import asyncio
from types import SimpleNamespace
from uuid import uuid4

import httpx
import pytest
from fastapi import HTTPException
from starlette.requests import Request

from app import main


class FakeUpstreamResponse:
    def __init__(self, *, status_code=200, headers=None, chunks=(b"preview",), error=None):
        self.status_code = status_code
        self.headers = headers or {}
        self.chunks = chunks
        self.error = error
        self.closed = False

    @property
    def content(self):
        raise AssertionError("preview must stream instead of reading response.content")

    def raise_for_status(self):
        if self.error:
            raise self.error

    async def aiter_bytes(self):
        for chunk in self.chunks:
            yield chunk

    async def aclose(self):
        self.closed = True


class FakeStream:
    def __init__(self, response=None, error=None):
        self.response = response
        self.error = error

    async def __aenter__(self):
        if self.error:
            raise self.error
        return self.response

    async def __aexit__(self, *_):
        if self.response is not None:
            await self.response.aclose()
        return False


class FakeClient:
    def __init__(self, response=None, error=None):
        self.response = response
        self.error = error
        self.closed = False
        self.requests = []

    def stream(self, method, url, headers):
        self.requests.append((method, url, headers))
        return FakeStream(self.response, self.error)

    async def aclose(self):
        self.closed = True


def make_request(range_header=None):
    headers = [] if not range_header else [(b"range", range_header.encode())]
    return Request({"type": "http", "method": "GET", "path": "/preview", "headers": headers})


async def invoke(monkeypatch, document, response=None, stream_error=None, range_header=None, page=1):
    seen = []
    client = FakeClient(response, stream_error)

    class Repository:
        async def get_by_id_for_org(self, document_id, org_id):
            seen.append((document_id, org_id))
            return document

    monkeypatch.setattr(main, "DocumentRepository", lambda _: Repository())
    monkeypatch.setattr(main.httpx, "AsyncClient", lambda **_: client)
    monkeypatch.setattr(main, "signed_preview_url", lambda public_id, mime_type, *, page: "https://signed.example/secret-token")
    result = await main.preview_document(
        uuid4(), make_request(range_header), page, SimpleNamespace(org_id="authorised-org"), object()
    )
    return result, client, seen


@pytest.mark.parametrize("mime_type", ["image/jpeg", "image/png"])
def test_authorised_allowed_previews_are_streamed(monkeypatch, mime_type):
    async def scenario():
        upstream = FakeUpstreamResponse(headers={"content-type": "text/html", "content-length": "7"})
        response, client, _ = await invoke(
            monkeypatch,
            {"cloud_public_id": "authorised/id", "mime_type": mime_type, "original_name": "résumé.pdf", "upload_status": "done"},
            upstream,
        )
        assert response.media_type == mime_type
        assert b"".join([part async for part in response.body_iterator]) == b"preview"
        assert upstream.closed and client.closed
        assert response.headers["content-disposition"].startswith("inline; filename=\"")
        assert "filename*=UTF-8''" in response.headers["content-disposition"]
        assert response.headers["cache-control"] == "private, no-store"
        assert response.headers["x-content-type-options"] == "nosniff"
    asyncio.run(scenario())


def test_pdf_preview_streams_a_cloudinary_rendered_png(monkeypatch):
    async def scenario():
        upstream = FakeUpstreamResponse(headers={"content-type": "image/png", "content-length": "7"})
        response, client, _ = await invoke(
            monkeypatch,
            {"cloud_public_id": "authorised/id", "mime_type": "application/pdf", "original_name": "statement.pdf", "upload_status": "done"},
            upstream,
            page=2,
        )
        assert response.media_type == "image/png"
        assert b"".join([part async for part in response.body_iterator]) == b"preview"
        assert client.requests[0][2] == {}
    asyncio.run(scenario())


def test_missing_next_pdf_page_is_a_normal_404(monkeypatch):
    async def scenario():
        upstream = FakeUpstreamResponse(error=httpx.HTTPStatusError(
            "page not found",
            request=httpx.Request("GET", "https://signed.example/redacted"),
            response=httpx.Response(404),
        ))
        with pytest.raises(HTTPException) as exc:
            await invoke(
                monkeypatch,
                {"cloud_public_id": "id", "mime_type": "application/pdf", "upload_status": "done"},
                upstream,
                page=2,
            )
        assert exc.value.status_code == 404
        assert exc.value.detail == "Document preview page not found"
    asyncio.run(scenario())


@pytest.mark.parametrize("document", [None, {"mime_type": "application/pdf"}])
def test_missing_or_unavailable_document_is_404(monkeypatch, document):
    async def scenario():
        with pytest.raises(HTTPException) as exc:
            await invoke(monkeypatch, document)
        assert exc.value.status_code == 404
    asyncio.run(scenario())


def test_other_org_is_404_when_authorised_query_returns_no_document(monkeypatch):
    async def scenario():
        with pytest.raises(HTTPException) as exc:
            await invoke(monkeypatch, None)
        assert exc.value.status_code == 404
    asyncio.run(scenario())


def test_non_ready_document_is_404(monkeypatch):
    async def scenario():
        with pytest.raises(HTTPException) as exc:
            await invoke(monkeypatch, {"cloud_public_id": "id", "mime_type": "application/pdf", "upload_status": "pending"})
        assert exc.value.status_code == 404
    asyncio.run(scenario())


def test_mime_allowlist_uses_stored_value_first_and_upstream_as_fallback(monkeypatch):
    async def scenario():
        with pytest.raises(HTTPException) as exc:
            await invoke(monkeypatch, {"cloud_public_id": "id", "mime_type": "text/html", "upload_status": "done"})
        assert exc.value.status_code == 415

        response, _, _ = await invoke(
            monkeypatch,
            {"cloud_public_id": "id", "mime_type": None, "upload_status": "done"},
            FakeUpstreamResponse(headers={"content-type": "image/png; charset=binary"}),
        )
        assert response.media_type == "image/png"
    asyncio.run(scenario())


@pytest.mark.parametrize("failure", ["connect", "status"])
def test_upstream_failures_are_generic_502_without_signed_url_in_logs(monkeypatch, caplog, failure):
    async def scenario():
        if failure == "connect":
            stream_error = httpx.ConnectError("https://signed.example/secret-token")
            upstream = None
        else:
            stream_error = None
            upstream = FakeUpstreamResponse(error=httpx.HTTPStatusError(
                "https://signed.example/secret-token",
                request=httpx.Request("GET", "https://signed.example/secret-token"),
                response=httpx.Response(503),
            ))
        with pytest.raises(HTTPException) as exc:
            await invoke(monkeypatch, {"cloud_public_id": "id", "mime_type": "application/pdf", "upload_status": "done"}, upstream, stream_error)
        assert exc.value.status_code == 502
        assert exc.value.detail == "Document preview is temporarily unavailable"
    asyncio.run(scenario())
    assert "secret-token" not in caplog.text


def test_filename_sanitisation_and_single_image_page_limit(monkeypatch):
    async def scenario():
        upstream = FakeUpstreamResponse(
            status_code=206,
            headers={"content-length": "7", "etag": '"v1"'},
        )
        response, client, _ = await invoke(
            monkeypatch,
            {"cloud_public_id": "id", "mime_type": "application/pdf", "original_name": 'bad\r\n/\\"name.pdf', "upload_status": "done"},
            upstream,
            page=1,
        )
        assert client.requests[0][2] == {}
        assert response.status_code == 206
        disposition = response.headers["content-disposition"]
        assert "\r" not in disposition and "\n" not in disposition and "/" not in disposition and "\\" not in disposition
        assert await anext(response.body_iterator) == b"preview"
        await response.body_iterator.aclose()
        assert upstream.closed and client.closed

        with pytest.raises(HTTPException) as exc:
            await invoke(
                monkeypatch,
                {"cloud_public_id": "id", "mime_type": "image/png", "upload_status": "done"},
                page=2,
            )
        assert exc.value.status_code == 404
    asyncio.run(scenario())


def test_preview_helpers_normalise_and_produce_safe_rfc5987_filenames():
    assert main.normalize_mime_type(" Application/PDF; charset=utf-8 ") == "application/pdf"
    assert main.sanitize_preview_filename('a\x00b\r\n/c\\d".pdf') == "a_b___c_d.pdf"
    assert main.preview_content_disposition("doc.pdf") == "inline; filename=\"doc.pdf\"; filename*=UTF-8''doc.pdf"
