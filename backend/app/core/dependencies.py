from fastapi import Depends, HTTPException, Request, status
from fastapi.security import OAuth2PasswordBearer
from app.core.database import get_connection
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
    user = UserRow(**cached) if cached else None
    if user is None and parsed_user_id:
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
    # Resolve token from OAuth2 authorization header or session cookies
    token = token or request.cookies.get("session") or request.cookies.get("__Host-session")
    user = await get_current_user_from_token(token)
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
        }
        self.allowed_roles = [
            role_aliases.get(r.lower().replace(" ", "_"), r.lower().replace(" ", "_"))
            for r in allowed_roles
        ]

    def __call__(self, current_user: UserRow = Depends(get_current_user)) -> UserRow:
        # UserRow.role is already stored as lowercase snake_case in the new schema
        user_role = current_user.role.lower().replace(" ", "_")
        if user_role not in self.allowed_roles:
            raise HTTPException(
                status_code=status.HTTP_403_FORBIDDEN,
                detail="Insufficient permissions for this action",
            )
        return current_user
