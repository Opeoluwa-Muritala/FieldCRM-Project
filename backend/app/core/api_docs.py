"""Nonce-aware development API documentation responses."""

from fastapi.openapi.docs import get_swagger_ui_html
from starlette.requests import Request
from starlette.responses import HTMLResponse


def swagger_ui_response(request: Request, *, openapi_url: str, title: str) -> HTMLResponse:
    """Render FastAPI's Swagger UI without weakening the strict CSP."""
    nonce = request.scope.get("state", {}).get("csp_nonce")
    if not nonce:
        raise RuntimeError("Swagger UI requires the request CSP nonce")

    generated = get_swagger_ui_html(
        openapi_url=openapi_url,
        title=title,
        swagger_js_url="/static/js/swagger-ui-bundle.js",
        swagger_css_url="/static/css/swagger-ui.css",
        swagger_favicon_url="/static/icons/favicon.svg",
    )
    body = generated.body.decode("utf-8")
    body = body.replace("<script ", f'<script nonce="{nonce}" ')
    body = body.replace("<script>", f'<script nonce="{nonce}">')
    body = body.replace("<style ", f'<style nonce="{nonce}" ')
    body = body.replace("<style>", f'<style nonce="{nonce}">')
    return HTMLResponse(body, headers={"Cache-Control": "no-store"})
