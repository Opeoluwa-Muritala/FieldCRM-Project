import ipaddress

from fastapi import HTTPException, Request

from app.config import settings
from app.core.loan_authorization import canonical_role
from app.domains.configuration.mfa import token_is_valid


def require_restricted_configuration_access(request: Request, user, *, require_mfa: bool = True) -> None:
    if not settings.CONFIGURATION_HUB_ENABLED:
        raise HTTPException(status_code=404, detail="Not found")
    if canonical_role(user.role) != "configuration_admin":
        raise HTTPException(status_code=403, detail="Configuration Admin access is required")
    host = (request.url.hostname or "").lower()
    allowed_hosts = {item.strip().lower() for item in settings.CONFIGURATION_ADMIN_HOSTS.split(",") if item.strip()}
    try:
        networks = [ipaddress.ip_network(item.strip()) for item in settings.CONFIGURATION_ADMIN_NETWORKS.split(",") if item.strip()]
        client_ip = ipaddress.ip_address(request.client.host if request.client else "0.0.0.0")
    except ValueError as exc:
        raise HTTPException(status_code=503, detail="Configuration access policy is invalid") from exc
    local = not settings.is_production and host in {"localhost", "127.0.0.1", "testserver"}
    if not (local or host in allowed_hosts or any(client_ip in network for network in networks)):
        raise HTTPException(status_code=404, detail="Not found")
    if require_mfa and not token_is_valid(request.cookies.get("configuration_mfa"), user.id):
        raise HTTPException(status_code=428, detail="Configuration MFA verification is required")
