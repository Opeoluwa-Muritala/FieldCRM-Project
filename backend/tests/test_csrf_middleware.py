import asyncio

from app.core.middleware import CrossSiteRequestMiddleware


async def invoke(*, origin="", fetch_site="", cookie="session=token"):
    called = []

    async def downstream(scope, receive, send):
        called.append(True)
        await send({"type": "http.response.start", "status": 204, "headers": []})
        await send({"type": "http.response.body", "body": b""})

    headers = [(b"cookie", cookie.encode())]
    if origin:
        headers.append((b"origin", origin.encode()))
    if fetch_site:
        headers.append((b"sec-fetch-site", fetch_site.encode()))
    sent = []
    async def send(message):
        sent.append(message)
    middleware = CrossSiteRequestMiddleware(downstream, ["https://fieldcrm.example"])
    await middleware(
        {"type": "http", "method": "POST", "path": "/applications/1", "headers": headers},
        lambda: None,
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


def test_bearer_clients_are_not_treated_as_cookie_csrf():
    called, sent = asyncio.run(invoke(cookie=""))
    assert called and sent[0]["status"] == 204
