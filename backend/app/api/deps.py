from typing import Generator, Optional
from fastapi import Depends, HTTPException, Request, status
from fastapi.security import OAuth2PasswordBearer
from jose import jwt
from sqlalchemy.orm import Session
from app.core.config import settings
from app.core.security import decode_access_token
from app.db.session import get_db
from app.db.models import User

# OAuth2PasswordBearer acts as fallback for bearer headers (like KMP mobile calls)
reusable_oauth2 = OAuth2PasswordBearer(
    tokenUrl=f"{settings.API_V1_STR}/auth/login-bearer",
    auto_error=False
)

def get_token_from_request(request: Request, header_token: Optional[str] = Depends(reusable_oauth2)) -> str:
    """
    Highly secure token extraction hierarchy:
    1. Check secure HttpOnly, SameSite cookie '__Host-session' or 'session'
    2. Fallback to OAuth2 bearer tokens (supporting Android KMP client headers)
    3. If neither present, throw 401 Unauthorized
    """
    # Cookie standard (safest against XSS token harvesting)
    cookie_token = request.cookies.get("session") or request.cookies.get("__Host-session")
    if cookie_token:
        return cookie_token
        
    if header_token:
        return header_token
        
    raise HTTPException(
        status_code=status.HTTP_401_UNAUTHORIZED,
        detail="Not authenticated. Missing secure session token or authorization header."
    )

def get_current_user(
    db: Session = Depends(get_db),
    token: str = Depends(get_token_from_request)
) -> User:
    """
    Decodes secure token, verifies active status, and loads user profile:
    - Enforces fail-closed validation check.
    """
    payload = decode_access_token(token)
    token_data_sub = payload.get("sub")
    if not token_data_sub:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Could not validate session. Invalid or expired token payload.",
        )
        
    user = db.query(User).filter(User.id == token_data_sub).first()
    if not user:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND, 
            detail="User account no longer exists."
        )
        
    if not user.is_active:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN, 
            detail="User account is deactivated."
        )
        
    return user

class RoleChecker:
    """
    Role-Based Access Control (RBAC) helper:
    - Restricts endpoint execution strictly to lists of allowed corporate roles.
    """
    def __init__(self, allowed_roles: list[str]):
        self.allowed_roles = allowed_roles

    def __call__(self, current_user: User = Depends(get_current_user)) -> User:
        if current_user.role not in self.allowed_roles:
            raise HTTPException(
                status_code=status.HTTP_403_FORBIDDEN,
                detail="Access denied. Insufficient role permissions for this operational stage."
            )
        return current_user
