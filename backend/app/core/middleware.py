import uuid
from starlette.datastructures import MutableHeaders
from starlette.types import ASGIApp, Message, Receive, Scope, Send


def _get_header(scope: Scope, name: bytes) -> str:
    for key, value in scope.get("headers", []):
        if key.lower() == name:
            return value.decode("latin-1")
    return ""


class RequestIDMiddleware:
    def __init__(self, app: ASGIApp):
        self.app = app

    async def __call__(self, scope: Scope, receive: Receive, send: Send) -> None:
        if scope["type"] != "http":
            await self.app(scope, receive, send)
            return

        request_id = _get_header(scope, b"x-request-id") or str(uuid.uuid4())
        scope.setdefault("state", {})["request_id"] = request_id

        async def send_with_request_id(message: Message) -> None:
            if message["type"] == "http.response.start":
                headers = MutableHeaders(scope=message)
                headers["X-Request-ID"] = request_id
            await send(message)

        await self.app(scope, receive, send_with_request_id)


class SecurityHeadersMiddleware:
    def __init__(self, app: ASGIApp, cookie_secure: bool = False):
        self.app = app
        self.cookie_secure = cookie_secure

    async def __call__(self, scope: Scope, receive: Receive, send: Send) -> None:
        if scope["type"] != "http":
            await self.app(scope, receive, send)
            return

        async def send_with_security_headers(message: Message) -> None:
            if message["type"] == "http.response.start":
                headers = MutableHeaders(scope=message)
                is_document_preview = scope.get("path", "").startswith("/api/v1/documents/") and scope.get("path", "").endswith("/preview")
                headers["X-Frame-Options"] = "SAMEORIGIN" if is_document_preview else "DENY"
                frame_ancestors = "'self'" if is_document_preview else "'none'"
                headers["Content-Security-Policy"] = (
                    "default-src 'self'; base-uri 'self'; object-src 'none'; "
                    f"frame-ancestors {frame_ancestors}; form-action 'self'; "
                    # Existing server-rendered templates include inline
                    # bootstrap scripts; retain this until they are migrated
                    # to nonce-based scripts rather than breaking login/forms.
                    "script-src 'self' 'unsafe-inline' https://cdnjs.cloudflare.com; "
                    "worker-src 'self' https://cdnjs.cloudflare.com; "
                    "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; "
                    "font-src 'self' https://fonts.gstatic.com; "
                    "img-src 'self' data: https://res.cloudinary.com;"
                )
                headers["X-Content-Type-Options"] = "nosniff"
                headers["Referrer-Policy"] = "strict-origin-when-cross-origin"
                headers["Permissions-Policy"] = "camera=(self), geolocation=(self), microphone=(), payment=(), usb=()"
                headers["Cache-Control"] = "no-store, no-cache, must-revalidate, max-age=0"

                scheme = scope.get("scheme", "")
                forwarded_proto = _get_header(scope, b"x-forwarded-proto")
                if self.cookie_secure or scheme == "https" or forwarded_proto == "https":
                    headers["Strict-Transport-Security"] = "max-age=31536000; includeSubDomains"

            await send(message)

        await self.app(scope, receive, send_with_security_headers)
