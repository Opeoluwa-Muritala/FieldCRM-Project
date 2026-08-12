from types import SimpleNamespace

from fastapi import HTTPException
from fastapi.exceptions import RequestValidationError
from starlette.requests import Request

from app.main import http_exception_handler, unexpected_exception_handler, validation_exception_handler


def make_request(path: str, request_id: str = "test-request-id", headers=None) -> Request:
    request = Request(
        {
            "type": "http",
            "method": "GET",
            "path": path,
            "raw_path": path.encode(),
            "query_string": b"",
            "headers": headers or [],
            "scheme": "http",
            "server": ("testserver", 80),
            "client": ("127.0.0.1", 1234),
            "root_path": "",
        }
    )
    request.state.request_id = request_id
    request.state.csp_nonce = "test-nonce"
    return request


async def response_body(response) -> str:
    return response.body.decode("utf-8")


async def test_browser_400_renders_user_facing_page():
    response = await http_exception_handler(
        make_request("/applications/new"),
        HTTPException(status_code=400, detail="Enter a valid loan amount."),
    )

    body = await response_body(response)
    assert response.status_code == 400
    assert response.media_type == "text/html"
    assert "Enter a valid loan amount." in body
    assert "Return home" in body


async def test_browser_403_renders_access_denied_page():
    response = await http_exception_handler(
        make_request("/admin/users"),
        HTTPException(status_code=403, detail="Forbidden"),
    )

    body = await response_body(response)
    assert response.status_code == 403
    assert "You don&#39;t have access" in body


async def test_browser_404_renders_page_instead_of_redirecting_to_referrer():
    response = await http_exception_handler(
        make_request(
            "/applications/missing",
            headers=[(b"referer", b"http://testserver/dashboard")],
        ),
        HTTPException(status_code=404, detail="Loan Application not found"),
    )

    body = await response_body(response)
    assert response.status_code == 404
    assert "Page not found" in body
    assert "Loan Application not found" in body


async def test_api_errors_remain_json():
    response = await http_exception_handler(
        make_request("/api/v1/example"),
        HTTPException(status_code=400, detail="Invalid value"),
    )

    assert response.status_code == 400
    assert response.media_type == "application/json"
    assert b'"request_id":"test-request-id"' in response.body


async def test_browser_validation_error_renders_html():
    response = await validation_exception_handler(
        make_request("/login"),
        RequestValidationError([]),
    )

    body = await response_body(response)
    assert response.status_code == 422
    assert response.media_type == "text/html"
    assert "Check the information you entered" in body


async def test_api_validation_error_keeps_fastapi_json_contract():
    response = await validation_exception_handler(
        make_request("/api/v1/example"),
        RequestValidationError([]),
    )

    assert response.status_code == 422
    assert response.media_type == "application/json"
    assert response.body == b'{"detail":[]}'


async def test_unhandled_browser_error_renders_safe_500_page():
    response = await unexpected_exception_handler(
        make_request("/dashboard"),
        RuntimeError("database password must not leak"),
    )

    body = await response_body(response)
    assert response.status_code == 500
    assert "Something went wrong" in body
    assert "database password" not in body
    assert "test-request-id" in body
