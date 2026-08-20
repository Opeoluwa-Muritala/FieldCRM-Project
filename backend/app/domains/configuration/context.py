from app.config import settings
from app.core.database import DatabaseIdentity, database_identity, get_connection
from app.core.security import decode_access_token
from uuid import UUID


class ConfigurationContextMiddleware:
    """Attach effective, non-secret branding/features to Jinja request context."""

    def __init__(self, app):
        self.app = app

    async def __call__(self, scope, receive, send):
        if scope.get("type") != "http" or not settings.CONFIGURATION_HUB_ENABLED:
            await self.app(scope, receive, send)
            return
        from http.cookies import SimpleCookie
        headers = {key.lower(): value for key, value in scope.get("headers", [])}
        cookies = SimpleCookie()
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
                    scope.setdefault("state", {})["branding"] = config.get("branding", {})
                    scope["state"]["feature_flags"] = config.get("features", {})
        except Exception:
            # Configuration context is presentation-only. Authentication and
            # route-level feature enforcement remain authoritative and fail closed.
            scope.setdefault("state", {})["branding"] = {}
            scope["state"]["feature_flags"] = {}
        await self.app(scope, receive, send)
