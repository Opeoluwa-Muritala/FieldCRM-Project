import asyncio

from app.core.middleware import CrossSiteRequestMiddleware


async def invoke(
    *,
    origin="",
    fetch_site="",
    cookie="session=token",
    host="fieldcrm.example",
    forwarded_host="",
    csrf_token="csrf-value",
    csrf_header=True,
    content_type="",
    body=b"",
):
    called = []

    async def downstream(scope, receive, send):
        called.append(True)
        await send({"type": "http.response.start", "status": 204, "headers": []})
        await send({"type": "http.response.body", "body": b""})

    if cookie and csrf_token:
        cookie = f"{cookie}; csrf_token={csrf_token}"
    headers = [(b"cookie", cookie.encode()), (b"host", host.encode())]
    if origin:
        headers.append((b"origin", origin.encode()))
    if fetch_site:
        headers.append((b"sec-fetch-site", fetch_site.encode()))
    if forwarded_host:
        headers.append((b"x-forwarded-host", forwarded_host.encode()))
    if cookie and csrf_token and csrf_header:
        headers.append((b"x-csrf-token", csrf_token.encode()))
    if content_type:
        headers.append((b"content-type", content_type.encode()))
    sent = []
    async def send(message):
        sent.append(message)
    received = False

    async def receive():
        nonlocal received
        if received:
            return {"type": "http.request", "body": b"", "more_body": False}
        received = True
        return {"type": "http.request", "body": body, "more_body": False}

    middleware = CrossSiteRequestMiddleware(downstream, ["https://fieldcrm.example"])
    await middleware(
        {"type": "http", "method": "POST", "path": "/applications/1", "scheme": "https", "headers": headers},
        receive,
        send,
    )
    return called, sent


def test_cookie_mutation_requires_allowed_origin():
    called, sent = asyncio.run(invoke())
    assert not called and sent[0]["status"] == 403
    called, sent = asyncio.run(invoke(origin="https://fieldcrm.example", fetch_site="same-origin"))
    assert called and sent[0]["status"] == 204


def test_fetch_metadata_blocks_cross_site_even_with_forged_origin():
    called, sent = asyncio.run(invoke(origin="https://fieldcrm.example", fetch_site="cross-site"))
    assert not called and sent[0]["status"] == 403


def test_cookie_mutation_accepts_exact_deployment_same_origin():
    called, sent = asyncio.run(invoke(
        origin="https://field-crm-project.vercel.app",
        fetch_site="same-origin",
        host="field-crm-project.vercel.app",
    ))
    assert called and sent[0]["status"] == 204


def test_cookie_mutation_rejects_different_origin_from_request_host():
    called, sent = asyncio.run(invoke(
        origin="https://attacker.example",
        fetch_site="same-site",
        host="field-crm-project.vercel.app",
    ))
    assert not called and sent[0]["status"] == 403


def test_forwarded_host_cannot_expand_the_csrf_origin_allowlist():
    called, sent = asyncio.run(invoke(
        origin="https://attacker.example",
        fetch_site="same-site",
        host="field-crm-project.vercel.app",
        forwarded_host="attacker.example",
    ))
    assert not called and sent[0]["status"] == 403


def test_bearer_clients_are_not_treated_as_cookie_csrf():
    called, sent = asyncio.run(invoke(cookie=""))
    assert called and sent[0]["status"] == 204


def test_cookie_mutation_rejects_missing_csrf_token():
    called, sent = asyncio.run(invoke(
        origin="https://fieldcrm.example",
        fetch_site="same-origin",
        csrf_token="",
    ))
    assert not called and sent[0]["status"] == 403


def test_standard_form_can_supply_csrf_token_in_body():
    called, sent = asyncio.run(invoke(
        origin="https://fieldcrm.example",
        fetch_site="same-origin",
        csrf_header=False,
        content_type="application/x-www-form-urlencoded",
        body=b"csrf_token=csrf-value&action=save",
    ))
    assert called and sent[0]["status"] == 204
