from fastapi import Depends, HTTPException, Request, status
from fastapi.security import OAuth2PasswordBearer
from time import perf_counter
from app.core.database import get_connection
from app.core.middleware import queue_response_cookie
from app.core.performance import record_counter, record_duration
from app.core.security import decode_access_token
from app.core.cache import cache_auth_user, get_cached_auth_user
from app.domains.users.repository import UserRepository
from app.domains.users.schemas import UserRow

oauth2_scheme = OAuth2PasswordBearer(tokenUrl="/api/v1/auth/login", auto_error=False)


async def get_current_user_from_token(token: str, conn=None) -> UserRow:
    if not token:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Not authenticated",
        )
    payload = decode_access_token(token)
    user_id = payload.get("sub")
    if not user_id:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid token payload",
        )
    try:
        from uuid import UUID
        parsed_user_id = UUID(user_id)
    except (ValueError, TypeError):
        parsed_user_id = None

    cached = await get_cached_auth_user(user_id) if parsed_user_id else None
    record_counter("auth_cache_hit" if cached else "auth_cache_miss")
    user = UserRow(**cached) if cached else None
    if user is None and parsed_user_id:
        record_counter("auth_db_fallback")
        if conn is None:
            async with get_connection() as direct_conn:
                user = await UserRepository(direct_conn).get_by_id(parsed_user_id)
        else:
            user = await UserRepository(conn).get_by_id(parsed_user_id)
        if user:
            await cache_auth_user(user)

    if not user or not user.is_active:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="User not found or inactive",
        )
    return user


async def get_current_user(
    request: Request,
    token: str = Depends(oauth2_scheme),
) -> UserRow:
    # Resolve token from OAuth2 authorization header, query params, or session cookies
    started_at = perf_counter()
    token = (
        token
        or request.query_params.get("token")
        or request.cookies.get("session")
        or request.cookies.get("__Host-session")
    )
    try:
        user = await get_current_user_from_token(token)
    except HTTPException as exc:
        if exc.status_code == 401:
            # Attempt transparent refresh if a refresh token is present
            refresh_token = request.cookies.get("refresh_token")
            if refresh_token:
                try:
                    from app.core.database import get_connection
                    from app.domains.auth.repository import AuthRepository
                    from app.domains.auth.service import AuthService
                    from app.config import settings

                    forwarded_proto = request.headers.get("x-forwarded-proto", "").split(",", 1)[0].strip().lower()
                    is_secure_request = request.url.scheme.lower() == "https" or forwarded_proto == "https"
                    if settings.is_production and not is_secure_request:
                        raise exc
                    
                    async with get_connection() as conn:
                        service = AuthService(AuthRepository(conn))
                        ip_address = request.client.host if request.client else None
                        user_agent = request.headers.get("user-agent")
                        
                        session_data = await service.rotate_refresh_token(
                            refresh_token, client_type="web", user_agent=user_agent, ip_address=ip_address
                        )
                        
                        new_token = session_data["access_token"]
                        user = await get_current_user_from_token(new_token, conn=conn)
                        
                        is_secure = settings.COOKIE_SECURE or (request.url.scheme == "https" or request.headers.get("x-forwarded-proto") == "https")
                        queue_response_cookie(
                            request.scope,
                            key="session",
                            value=new_token,
                            httponly=True,
                            secure=is_secure,
                            samesite="lax",
                            max_age=settings.ACCESS_TOKEN_EXPIRE_MINUTES * 60,
                            path="/",
                        )
                        if session_data.get("refresh_token"):
                            queue_response_cookie(
                                request.scope,
                                key="refresh_token",
                                value=session_data["refresh_token"],
                                httponly=True,
                                secure=is_secure,
                                samesite="strict",
                                expires=session_data["expires_at"],
                                path="/",
                            )
                except Exception:
                    # Propagate original 401 if refresh attempt fails
                    raise exc
            else:
                raise exc
        else:
            raise exc
    finally:
        record_duration("auth", started_at)
    # Response-cache invalidation uses this only after a successful write.
    # It does not change the authentication or direct database read path.
    request.state.cache_user = user
    return user


class RoleChecker:
    def __init__(self, allowed_roles: list[str]):
        # The web UI still calls the field role "Loan Officer", while the
        # canonical workflow and mobile API use "account_officer".  Treat
        # these as the same role at the authorization boundary so legacy web
        # links do not bounce a valid Account Officer back to the dashboard.
        role_aliases = {
            "loan_officer": "account_officer",
            "relationship_officer": "account_officer",
            "team_lead": "branch_manager",
            "supervisor": "branch_supervisor",
        }
        self.allowed_roles = [
            role_aliases.get(r.lower().replace(" ", "_"), r.lower().replace(" ", "_"))
            for r in allowed_roles
        ]

    def __call__(self, current_user: UserRow = Depends(get_current_user)) -> UserRow:
        # UserRow.role is already stored as lowercase snake_case in the new schema
        user_role = current_user.role.lower().replace(" ", "_")
        role_aliases = {
            "loan_officer": "account_officer",
            "relationship_officer": "account_officer",
            "team_lead": "branch_manager",
            "supervisor": "branch_supervisor",
        }
        user_role = role_aliases.get(user_role, user_role)
        if user_role not in self.allowed_roles:
            raise HTTPException(
                status_code=status.HTTP_403_FORBIDDEN,
                detail="Insufficient permissions for this action",
            )
        return current_user
