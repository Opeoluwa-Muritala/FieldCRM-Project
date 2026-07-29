import json
import logging
import secrets
import uuid
from time import perf_counter
from starlette.datastructures import MutableHeaders
from starlette.types import ASGIApp, Message, Receive, Scope, Send

from app.core.performance import finish_request_performance, start_request_performance


logger = logging.getLogger("FieldCRMPerformance")


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
    def __init__(
        self,
        app: ASGIApp,
        cookie_secure: bool = False,
        csp_nonce_enforced: bool = False,
    ):
        self.app = app
        self.cookie_secure = cookie_secure
        self.csp_nonce_enforced = csp_nonce_enforced

    async def __call__(self, scope: Scope, receive: Receive, send: Send) -> None:
        if scope["type"] != "http":
            await self.app(scope, receive, send)
            return

        csp_nonce = secrets.token_urlsafe(16)
        scope.setdefault("state", {})["csp_nonce"] = csp_nonce
        script_unsafe_inline = "" if self.csp_nonce_enforced else " 'unsafe-inline'"

        async def send_with_security_headers(message: Message) -> None:
            if message["type"] == "http.response.start":
                headers = MutableHeaders(scope=message)
                is_document_preview = scope.get("path", "").startswith("/api/v1/documents/") and scope.get("path", "").endswith("/preview")
                headers["X-Frame-Options"] = "SAMEORIGIN" if is_document_preview else "DENY"
                frame_ancestors = "'self'" if is_document_preview else "'none'"
                headers["Content-Security-Policy"] = (
                    "default-src 'self'; base-uri 'self'; object-src 'none'; "
                    f"frame-ancestors {frame_ancestors}; form-action 'self'; "
                    f"script-src 'self' 'nonce-{csp_nonce}'{script_unsafe_inline} https://cdnjs.cloudflare.com; "
                    "worker-src 'self' https://cdnjs.cloudflare.com; "
                    f"style-src 'self' 'nonce-{csp_nonce}' https://fonts.googleapis.com; "
                    "font-src 'self' https://fonts.gstatic.com; "
                    "img-src 'self' data: https://res.cloudinary.com;"
                )
                headers["X-Content-Type-Options"] = "nosniff"
                headers["Referrer-Policy"] = "strict-origin-when-cross-origin"
                headers["Permissions-Policy"] = "camera=(self), geolocation=(self), microphone=(), payment=(), usb=()"
                is_static = scope.get("path", "").startswith("/static/")
                if is_static:
                    headers["Cache-Control"] = "public, max-age=31536000, immutable"
                else:
                    headers["Cache-Control"] = "no-store, no-cache, must-revalidate, max-age=0"

                scheme = scope.get("scheme", "")
                forwarded_proto = _get_header(scope, b"x-forwarded-proto")
                if self.cookie_secure or scheme == "https" or forwarded_proto == "https":
                    headers["Strict-Transport-Security"] = "max-age=31536000; includeSubDomains"

            await send(message)

        await self.app(scope, receive, send_with_security_headers)


class PerformanceTimingMiddleware:
    """Measure request components and emit safe structured timing metadata."""

    def __init__(self, app: ASGIApp, expose_server_timing: bool = False):
        self.app = app
        self.expose_server_timing = expose_server_timing

    async def __call__(self, scope: Scope, receive: Receive, send: Send) -> None:
        if scope["type"] != "http":
            await self.app(scope, receive, send)
            return

        performance, token = start_request_performance()
        scope.setdefault("state", {})["performance"] = performance

        async def send_with_timing(message: Message) -> None:
            if message["type"] == "http.response.start" and self.expose_server_timing:
                headers = MutableHeaders(scope=message)
                components = [
                    f"{name};dur={duration:.1f}"
                    for name, duration in sorted(performance.durations_ms.items())
                ]
                if components:
                    headers["Server-Timing"] = ", ".join(components)
                headers["X-Query-Count"] = str(performance.query_count)
            await send(message)

        try:
            await self.app(scope, receive, send_with_timing)
        finally:
            total_ms = (perf_counter() - performance.started_at) * 1000
            logger.info(
                json.dumps(
                    {
                        "event": "request_performance",
                        "method": scope.get("method"),
                        "path": scope.get("path"),
                        "total_ms": round(total_ms, 1),
                        "query_count": performance.query_count,
                        "counters": performance.counters,
                        "durations_ms": {
                            name: round(duration, 1)
                            for name, duration in performance.durations_ms.items()
                        },
                    },
                    separators=(",", ":"),
                )
            )
            finish_request_performance(token)
