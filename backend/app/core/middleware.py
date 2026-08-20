import json
import logging
import secrets
import uuid
from time import perf_counter
from starlette.datastructures import MutableHeaders
from starlette.requests import Request
from starlette.responses import Response
from starlette.types import ASGIApp, Message, Receive, Scope, Send
from urllib.parse import urlsplit

from app.core.performance import finish_request_performance, start_request_performance


logger = logging.getLogger("FieldCRMPerformance")


class CrossSiteRequestMiddleware:
    """Reject cross-site state changes that rely on browser cookies.

    API clients using Authorization headers are not subject to browser CSRF.
    Cookie-authenticated mutations must originate from an explicitly allowed
    origin, and modern Fetch Metadata is used as an additional deny signal.
    """
    _UNSAFE = {"POST", "PUT", "PATCH", "DELETE"}

    _CSRF_COOKIE = "csrf_token"
    _MAX_FORM_BYTES = 8 * 1024 * 1024
    _PRE_AUTH_BROWSER_PATHS = {
        "/login",
        "/forgot-password",
        "/reset-password",
        "/api/v1/auth/login",
    }

    def __init__(self, app: ASGIApp, allowed_origins: list[str], cookie_secure: bool = False):
        self.app = app
        self.allowed_origins = {origin.rstrip("/").lower() for origin in allowed_origins}
        self.cookie_secure = cookie_secure

    @staticmethod
    def _cookies(scope: Scope) -> dict[str, str]:
        from http.cookies import SimpleCookie

        parsed = SimpleCookie()
        try:
            parsed.load(_get_header(scope, b"cookie"))
        except Exception:
            return {}
        return {name: morsel.value for name, morsel in parsed.items()}

    @staticmethod
    async def _buffer_body(receive: Receive, maximum: int) -> bytes | None:
        body = bytearray()
        while True:
            message = await receive()
            if message["type"] == "http.disconnect":
                return None
            if message["type"] != "http.request":
                continue
            body.extend(message.get("body", b""))
            if len(body) > maximum:
                return None
            if not message.get("more_body", False):
                return bytes(body)

    @staticmethod
    def _replay_body(body: bytes) -> Receive:
        sent = False

        async def receive() -> Message:
            nonlocal sent
            if sent:
                return {"type": "http.request", "body": b"", "more_body": False}
            sent = True
            return {"type": "http.request", "body": body, "more_body": False}

        return receive

    async def _form_csrf_token(self, scope: Scope, receive: Receive) -> tuple[str, Receive] | None:
        body = await self._buffer_body(receive, self._MAX_FORM_BYTES)
        if body is None:
            return None
        replay = self._replay_body(body)
        request = Request(scope, receive=self._replay_body(body))
        try:
            form = await request.form(
                max_files=100,
                max_fields=1000,
                max_part_size=self._MAX_FORM_BYTES,
            )
            token = str(form.get("csrf_token") or "")
        except Exception:
            token = ""
        finally:
            await request.close()
        return token, replay

    async def _send_with_csrf_cookie(
        self,
        scope: Scope,
        receive: Receive,
        send: Send,
        *,
        token: str,
    ) -> None:
        async def send_with_cookie(message: Message) -> None:
            if message["type"] == "http.response.start":
                headers = MutableHeaders(scope=message)
                cookie_response = Response()
                cookie_response.set_cookie(
                    key=self._CSRF_COOKIE,
                    value=token,
                    httponly=False,
                    secure=self.cookie_secure or scope.get("scheme") == "https",
                    samesite="strict",
                    max_age=2 * 24 * 60 * 60,
                    path="/",
                )
                cookie_header = next(
                    value for name, value in cookie_response.raw_headers if name == b"set-cookie"
                )
                headers.append("set-cookie", cookie_header.decode("latin-1"))
            await send(message)

        await self.app(scope, receive, send_with_cookie)

    @staticmethod
    def _request_origin(scope: Scope) -> str:
        """Return the exact origin serving this request behind a trusted proxy.

        A browser's same-origin request is safe even when a deployment hostname
        was not duplicated in CORS_ORIGINS. Cross-site callers cannot make their
        Origin match the victim Host while retaining the victim's cookies.
        """
        # The ASGI server is responsible for accepting proxy headers only from
        # the trusted edge. Host validation happens independently in
        # TrustedHostMiddleware, so client-supplied forwarded-host values never
        # become an accepted CSRF origin here.
        scheme = str(scope.get("scheme") or "http").lower()
        host = _get_header(scope, b"host").strip().lower()
        return f"{scheme}://{host}" if host else ""

    async def __call__(self, scope: Scope, receive: Receive, send: Send) -> None:
        if scope.get("type") != "http":
            await self.app(scope, receive, send)
            return
        cookies = self._cookies(scope)
        csrf_cookie = cookies.get(self._CSRF_COOKIE, "")
        if scope.get("method") not in self._UNSAFE:
            if csrf_cookie:
                await self.app(scope, receive, send)
            else:
                await self._send_with_csrf_cookie(
                    scope, receive, send, token=secrets.token_urlsafe(32)
                )
            return

        uses_auth_cookie = any(
            name in cookies for name in ("session", "__Host-session", "refresh_token")
        )
        browser_pre_auth = scope.get("path") in self._PRE_AUTH_BROWSER_PATHS and bool(
            _get_header(scope, b"origin") or _get_header(scope, b"referer")
        )
        if not uses_auth_cookie and not browser_pre_auth:
            await self.app(scope, receive, send)
            return

        fetch_site = _get_header(scope, b"sec-fetch-site").lower()
        origin = _get_header(scope, b"origin").rstrip("/").lower()
        referer = _get_header(scope, b"referer")
        if fetch_site == "cross-site":
            response = Response("Cross-site request blocked", status_code=403)
            await response(scope, receive, send)
            return
        if not origin and referer:
            parsed = urlsplit(referer)
            origin = f"{parsed.scheme}://{parsed.netloc}".lower()
        accepted_origins = self.allowed_origins | {self._request_origin(scope)}
        accepted_origins.discard("")
        if not origin or origin not in accepted_origins:
            response = Response("CSRF origin validation failed", status_code=403)
            await response(scope, receive, send)
            return

        supplied_token = _get_header(scope, b"x-csrf-token")
        downstream_receive = receive
        content_type = _get_header(scope, b"content-type").lower()
        if not supplied_token and content_type.startswith(("application/x-www-form-urlencoded", "multipart/form-data")):
            parsed = await self._form_csrf_token(scope, receive)
            if parsed is None:
                response = Response("Request body is too large", status_code=413)
                await response(scope, receive, send)
                return
            supplied_token, downstream_receive = parsed
        if not csrf_cookie or not supplied_token or not secrets.compare_digest(csrf_cookie, supplied_token):
            response = Response("CSRF token validation failed", status_code=403)
            await response(scope, downstream_receive, send)
            return

        rotate_token = secrets.token_urlsafe(32) if scope.get("path") in {"/login", "/logout"} else ""
        if rotate_token:
            await self._send_with_csrf_cookie(
                scope, downstream_receive, send, token=rotate_token
            )
        else:
            await self.app(scope, downstream_receive, send)


def queue_response_cookie(scope: Scope, **cookie_options) -> None:
    """Queue a cookie for the final response produced by the endpoint.

    Authentication runs in a dependency, but HTML endpoints commonly return
    their own TemplateResponse or RedirectResponse. Cookies set on FastAPI's
    temporary dependency response are not copied to those explicit responses,
    so queue the rendered Set-Cookie header on the request scope instead.
    """
    cookie_response = Response()
    cookie_response.set_cookie(**cookie_options)
    cookie_header = next(
        value for name, value in cookie_response.raw_headers if name == b"set-cookie"
    )
    scope.setdefault("state", {}).setdefault("pending_response_cookies", []).append(
        cookie_header
    )


class PendingResponseCookiesMiddleware:
    """Attach cookies queued by dependencies to the actual endpoint response."""

    def __init__(self, app: ASGIApp):
        self.app = app

    async def __call__(self, scope: Scope, receive: Receive, send: Send) -> None:
        if scope["type"] != "http":
            await self.app(scope, receive, send)
            return

        async def send_with_pending_cookies(message: Message) -> None:
            if message["type"] == "http.response.start":
                headers = MutableHeaders(scope=message)
                for cookie in scope.get("state", {}).pop("pending_response_cookies", []):
                    headers.append("set-cookie", cookie.decode("latin-1"))
            await send(message)

        await self.app(scope, receive, send_with_pending_cookies)


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
                is_redoc = scope.get("path", "") == "/api/redoc"
                style_authorization = "'unsafe-inline'" if is_redoc else f"'nonce-{csp_nonce}'"
                is_document_preview = scope.get("path", "").startswith("/api/v1/documents/") and scope.get("path", "").endswith("/preview")
                headers["X-Frame-Options"] = "SAMEORIGIN" if is_document_preview else "DENY"
                frame_ancestors = "'self'" if is_document_preview else "'none'"
                headers["Content-Security-Policy"] = (
                    "default-src 'self'; base-uri 'self'; object-src 'none'; "
                    f"frame-ancestors {frame_ancestors}; form-action 'self'; "
                    f"script-src 'self' 'nonce-{csp_nonce}'{script_unsafe_inline} https://cdnjs.cloudflare.com; "
                    "worker-src 'self' https://cdnjs.cloudflare.com; "
                    f"style-src 'self' {style_authorization} https://fonts.googleapis.com https://cdnjs.cloudflare.com; "
                    "font-src 'self' https://fonts.gstatic.com; "
                    "img-src 'self' data: https://res.cloudinary.com; "
                    "connect-src 'self' https://api.cloudinary.com;"
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
        response_status = None

        async def send_with_timing(message: Message) -> None:
            nonlocal response_status
            if message["type"] == "http.response.start":
                response_status = message["status"]
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
                        "status": response_status,
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
