from fastapi import Depends, HTTPException, Request, status
from fastapi.security import OAuth2PasswordBearer
from app.core.database import db_conn
from app.core.security import decode_access_token
from app.domains.users.repository import UserRepository
from app.domains.users.schemas import UserRow

oauth2_scheme = OAuth2PasswordBearer(tokenUrl="/api/v1/auth/login", auto_error=False)


async def get_current_user_from_token(token: str, conn) -> UserRow:
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
    repo = UserRepository(conn)
    try:
        from uuid import UUID
        user = await repo.get_by_id(UUID(user_id))
    except (ValueError, TypeError):
        user = None

    if not user or not user.is_active:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="User not found or inactive",
        )
    return user


async def get_current_user(
    request: Request,
    token: str = Depends(oauth2_scheme),
    conn=Depends(db_conn),
) -> UserRow:
    # Resolve token from OAuth2 authorization header or session cookies
    token = token or request.cookies.get("session") or request.cookies.get("__Host-session")
    return await get_current_user_from_token(token, conn)


class RoleChecker:
    def __init__(self, allowed_roles: list[str]):
        self.allowed_roles = [r.lower().replace(" ", "_") for r in allowed_roles]

    def __call__(self, current_user: UserRow = Depends(get_current_user)) -> UserRow:
        # UserRow.role is already stored as lowercase snake_case in the new schema
        user_role = current_user.role.lower().replace(" ", "_")
        if user_role not in self.allowed_roles:
            raise HTTPException(
                status_code=status.HTTP_403_FORBIDDEN,
                detail="Insufficient permissions for this action",
            )
        return current_user
