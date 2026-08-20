from app.core.database import DatabaseIdentity, database_identity, get_connection
from app.core.security import decode_access_token
from app.domains.configuration.catalog import FEATURE_DEFAULTS
from app.domains.configuration.gates import required_feature_for_path
from starlette.responses import JSONResponse, Response
from uuid import UUID


class ConfigurationContextMiddleware:
    """Attach effective, non-secret branding/features to Jinja request context."""

    def __init__(self, app):
        self.app = app

    async def __call__(self, scope, receive, send):
        if scope.get("type") != "http":
            await self.app(scope, receive, send)
            return
        from http.cookies import SimpleCookie
        headers = {key.lower(): value for key, value in scope.get("headers", [])}
        cookies = SimpleCookie()
        state = scope.setdefault("state", {})
        state["branding"] = {}
        state["feature_flags"] = dict(FEATURE_DEFAULTS)
        try:
            cookies.load(headers.get(b"cookie", b"").decode("latin-1"))
            token = (cookies.get("session") or cookies.get("__Host-session"))
            payload = decode_access_token(token.value if token else "")
            org_id, user_id = payload.get("org_id"), payload.get("sub")
            if org_id and user_id and payload.get("type") == "access":
                identity = DatabaseIdentity(org_id=str(org_id), user_id=str(user_id),
                                            role=str(payload.get("role") or ""), request_id="configuration-context")
                with database_identity(identity):
                    async with get_connection() as conn:
                        row = await conn.fetchrow(
                            """SELECT payload FROM configuration_versions
                               WHERE org_id=$1 AND status='published' AND effective_at<=NOW()
                               ORDER BY effective_at DESC,version_number DESC LIMIT 1""", UUID(str(org_id)),
                        )
                if row:
                    config = dict(row["payload"])
                    state["branding"] = config.get("branding", {})
                    state["feature_flags"] = {**FEATURE_DEFAULTS, **config.get("features", {})}
        except Exception:
            # Configuration context is presentation-only. Authentication and
            # route-level feature enforcement remain authoritative and fail closed.
            state["branding"] = {}
            state["feature_flags"] = dict(FEATURE_DEFAULTS)

        required_feature = required_feature_for_path(scope.get("path", ""))
        if required_feature and not state["feature_flags"].get(required_feature, False):
            response = (
                JSONResponse({"detail": "Not found"}, status_code=404)
                if scope.get("path", "").startswith("/api/")
                else Response(status_code=404)
            )
            await response(scope, receive, send)
            return
        await self.app(scope, receive, send)
