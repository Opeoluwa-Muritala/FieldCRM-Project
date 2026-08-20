from fastapi import HTTPException, Request

from app.config import settings
from app.core.loan_authorization import canonical_role
from app.domains.configuration.mfa import token_is_valid


def require_restricted_configuration_access(request: Request, user, *, require_mfa: bool = True) -> None:
    # The Configuration Hub is deliberately a localhost-only control plane.
    # This remains a runtime guard in addition to production startup validation.
    if not settings.CONFIGURATION_HUB_ENABLED or settings.is_production:
        raise HTTPException(status_code=404, detail="Not found")
    host = (request.url.hostname or "").lower()
    client_host = request.client.host if request.client else ""
    local_host = host in {"localhost", "127.0.0.1", "[::1]", "testserver"}
    try:
        import ipaddress
        local_client = ipaddress.ip_address(client_host).is_loopback
    except ValueError:
        local_client = host == "testserver" and client_host == "testclient"
    if not (local_host and local_client):
        raise HTTPException(status_code=404, detail="Not found")
    if canonical_role(user.role) != "configuration_admin":
        raise HTTPException(status_code=403, detail="Configuration Admin access is required")
    if require_mfa and not token_is_valid(request.cookies.get("configuration_mfa"), user.id):
        raise HTTPException(status_code=428, detail="Configuration MFA verification is required")
