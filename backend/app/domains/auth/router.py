from fastapi import APIRouter, Depends, Response, Request, HTTPException, status
from uuid import UUID
from fastapi.security import OAuth2PasswordRequestForm
from app.core.database import db_conn
from app.domains.auth.repository import AuthRepository
from app.domains.auth.service import AuthService
from app.domains.auth.schemas import Token, RefreshRequest, LogoutRequest
from app.config import settings
from app.core.rate_limit import enforce_login_limits

router = APIRouter()


def _request_is_secure(request: Request) -> bool:
    """Honor direct TLS and the HTTPS signal from the trusted deployment proxy."""
    forwarded_proto = request.headers.get("x-forwarded-proto", "").split(",", 1)[0].strip().lower()
    return request.url.scheme.lower() == "https" or forwarded_proto == "https"


def _require_secure_token_transport(request: Request) -> bool:
    """Require HTTPS for token issuance/rotation in production.

    Local development may continue to use HTTP; production never sends bearer or
    refresh credentials over a clear-text request. The bool is reused for the
    Secure cookie attribute so transport enforcement and cookie policy cannot
    drift apart.
    """
    secure = _request_is_secure(request)
    if settings.is_production and not secure:
        raise HTTPException(status_code=400, detail="HTTPS is required for authentication.")
    return secure or settings.COOKIE_SECURE


def _set_no_store(response: Response) -> None:
    """Prevent browsers and intermediary caches from retaining token responses."""
    response.headers["Cache-Control"] = "no-store"
    response.headers["Pragma"] = "no-cache"

def get_auth_service(conn = Depends(db_conn)) -> AuthService:
    repo = AuthRepository(conn)
    return AuthService(repo)

@router.post("/login", response_model=Token)
async def login_cookie(
    request: Request,
    response: Response,
    form_data: OAuth2PasswordRequestForm = Depends(),
    service: AuthService = Depends(get_auth_service)
):
    is_secure = _require_secure_token_transport(request)
    _set_no_store(response)
    await enforce_login_limits(request, form_data.username)
    ip_address = request.client.host if request.client else None
    user_agent = request.headers.get("user-agent")
    
    session_data = await service.authenticate_web(
        form_data.username, form_data.password, user_agent=user_agent, ip_address=ip_address
    )
    
    response.set_cookie(
        key="refresh_token",
        value=session_data["refresh_token"],
        httponly=True,
        secure=is_secure,
        samesite="strict",
        expires=session_data["expires_at"],
        path="/api/v1/auth/refresh"
    )
    return {"access_token": session_data["access_token"], "token_type": "bearer"}

@router.post("/refresh", response_model=Token)
async def refresh_cookie(
    request: Request,
    response: Response,
    service: AuthService = Depends(get_auth_service)
):
    is_secure = _require_secure_token_transport(request)
    _set_no_store(response)
    # CSRF protection: Origin and Referer checks
    origin = request.headers.get("origin")
    referer = request.headers.get("referer")
    allowed_origins = [o.strip().lower() for o in settings.CORS_ORIGINS.split(",") if o.strip()]
    
    if origin:
        if origin.lower() not in allowed_origins:
            raise HTTPException(status_code=403, detail="CSRF Protection: Origin not allowed")
    elif referer:
        from urllib.parse import urlparse
        parsed = urlparse(referer)
        ref_origin = f"{parsed.scheme}://{parsed.netloc}"
        if ref_origin.lower() not in allowed_origins:
            raise HTTPException(status_code=403, detail="CSRF Protection: Referer not allowed")
    else:
        if allowed_origins:
            raise HTTPException(status_code=403, detail="CSRF Protection: Missing Origin/Referer headers")

    raw_token = request.cookies.get("refresh_token")
    if not raw_token:
        raise HTTPException(status_code=401, detail="Refresh token cookie missing")
        
    ip_address = request.client.host if request.client else None
    user_agent = request.headers.get("user-agent")
    
    session_data = await service.rotate_refresh_token(
        raw_token, client_type="web", user_agent=user_agent, ip_address=ip_address
    )
    
    response.set_cookie(
        key="refresh_token",
        value=session_data["refresh_token"],
        httponly=True,
        secure=is_secure,
        samesite="strict",
        expires=session_data["expires_at"],
        path="/api/v1/auth/refresh"
    )
    return {"access_token": session_data["access_token"], "token_type": "bearer"}

@router.post("/login-bearer", response_model=Token)
async def login_bearer(
    request: Request,
    response: Response,
    form_data: OAuth2PasswordRequestForm = Depends(),
    service: AuthService = Depends(get_auth_service)
):
    _require_secure_token_transport(request)
    _set_no_store(response)
    await enforce_login_limits(request, form_data.username)
    token = await service.authenticate_user(form_data.username, form_data.password)
    return {"access_token": token, "token_type": "bearer"}

@router.post("/login-mobile", response_model=Token)
async def login_mobile(
    request: Request,
    response: Response,
    form_data: OAuth2PasswordRequestForm = Depends(),
    service: AuthService = Depends(get_auth_service)
):
    """Mobile credential login that issues a token stored in encrypted device storage."""
    _require_secure_token_transport(request)
    _set_no_store(response)
    await enforce_login_limits(request, form_data.username)
    ip_address = request.client.host if request.client else None
    user_agent = request.headers.get("user-agent")
    return await service.authenticate_mobile(
        form_data.username, form_data.password, user_agent=user_agent, ip_address=ip_address
    )

@router.post("/refresh-mobile", response_model=Token)
async def refresh_mobile(
    request: Request,
    response: Response,
    payload: RefreshRequest,
    service: AuthService = Depends(get_auth_service)
):
    _require_secure_token_transport(request)
    _set_no_store(response)
    ip_address = request.client.host if request.client else None
    user_agent = request.headers.get("user-agent")
    return await service.rotate_mobile_session(
        payload.refresh_token, user_agent=user_agent, ip_address=ip_address
    )

@router.post("/logout-mobile")
async def logout_mobile(payload: LogoutRequest, service: AuthService = Depends(get_auth_service)):
    await service.revoke_mobile_session(payload.refresh_token)
    return {"status": "logged_out"}

@router.post("/logout")
async def logout(
    request: Request,
    response: Response,
    service: AuthService = Depends(get_auth_service)
):
    raw_token = request.cookies.get("refresh_token")
    if raw_token:
        await service.revoke_web_session(raw_token)
        
    response.delete_cookie(key="refresh_token", path="/api/v1/auth/refresh")
    response.delete_cookie(key="session", path="/")
    response.delete_cookie(key="__Host-session", path="/")
    return {"status": "logged_out"}

from app.core.dependencies import RoleChecker

@router.get("/sessions/{user_id}")
async def list_user_sessions(
    user_id: UUID,
    current_user = Depends(RoleChecker(["System Admin"])),
    service: AuthService = Depends(get_auth_service)
):
    """Admin-only endpoint to list active sessions for a user."""
    return await service.list_active_sessions(user_id)

@router.post("/sessions/{user_id}/revoke")
async def revoke_all_user_sessions(
    user_id: UUID,
    current_user = Depends(RoleChecker(["System Admin"])),
    service: AuthService = Depends(get_auth_service)
):
    """Admin-only endpoint to revoke all active sessions for a user."""
    await service.revoke_all_user_sessions(user_id)
    return {"status": "all_sessions_revoked"}
