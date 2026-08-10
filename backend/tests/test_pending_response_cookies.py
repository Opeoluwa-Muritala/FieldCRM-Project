from datetime import datetime, timezone

import pytest

from app.core.middleware import PendingResponseCookiesMiddleware, queue_response_cookie


@pytest.mark.asyncio
async def test_queued_auth_cookies_reach_explicit_endpoint_response():
    async def endpoint(scope, receive, send):
        queue_response_cookie(
            scope,
            key="session",
            value="new-access-token",
            httponly=True,
            secure=True,
            samesite="lax",
            max_age=600,
            path="/",
        )
        queue_response_cookie(
            scope,
            key="refresh_token",
            value="new-refresh-token",
            httponly=True,
            secure=True,
            samesite="strict",
            expires=datetime(2030, 1, 1, tzinfo=timezone.utc),
            path="/",
        )
        await send(
            {
                "type": "http.response.start",
                "status": 303,
                "headers": [(b"location", b"/crm-review-queue")],
            }
        )
        await send({"type": "http.response.body", "body": b""})

    sent = []

    async def receive():
        return {"type": "http.request", "body": b"", "more_body": False}

    async def send(message):
        sent.append(message)

    scope = {
        "type": "http",
        "method": "GET",
        "path": "/crm-review-queue",
        "headers": [],
        "state": {},
    }
    await PendingResponseCookiesMiddleware(endpoint)(scope, receive, send)

    response_start = next(message for message in sent if message["type"] == "http.response.start")
    cookie_headers = [
        value.decode("latin-1")
        for name, value in response_start["headers"]
        if name.lower() == b"set-cookie"
    ]

    assert len(cookie_headers) == 2
    assert any("session=new-access-token" in value for value in cookie_headers)
    assert any("refresh_token=new-refresh-token" in value for value in cookie_headers)
    assert all("HttpOnly" in value and "Secure" in value for value in cookie_headers)
