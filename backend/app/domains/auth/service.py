import secrets
from datetime import datetime, timedelta, timezone

from app.core.security import verify_password, create_access_token, get_password_hash
from app.domains.auth.repository import AuthRepository
from app.core.exceptions import DomainException


class AuthService:
    def __init__(self, repo: AuthRepository):
        self.repo = repo

    async def authenticate_user(self, email: str, password: str, session_type: str = "web") -> str:
        """Authenticate a user by email and password, return JWT token."""
        user = await self.repo.get_user_by_email(email)
        if not user or not user.is_active:
            raise DomainException("Incorrect email or password.", 401)

        if not verify_password(password, user.hashed_password):
            raise DomainException("Incorrect email or password.", 401)

        await self.repo.record_login(str(user.id))
        token = create_access_token(user.id, role=user.role, org_id=user.org_id, session_type=session_type)
        return token

    async def request_password_reset(self, email: str) -> None:
        user = await self.repo.get_user_by_email(email)
        if user:
            token = secrets.token_urlsafe(32)
            expires_at = datetime.now(timezone.utc) + timedelta(hours=1)
            await self.repo.create_reset_token(str(user.id), token, expires_at)

    async def validate_reset_token(self, token: str):
        row = await self.repo.get_valid_reset_token(token)
        return str(row["user_id"]) if row else None

    async def reset_password(self, token: str, new_password: str) -> bool:
        user_id = await self.validate_reset_token(token)
        if not user_id:
            return False
        hashed = get_password_hash(new_password)
        await self.repo.update_password(user_id, hashed)
        await self.repo.mark_token_used(token)
        return True

    async def change_password(self, user_id: str, current_password: str, new_password: str) -> bool:
        user = await self.repo.get_user_by_id(user_id)
        if not user or not verify_password(current_password, user["password_hash"]):
            return False
        hashed = get_password_hash(new_password)
        await self.repo.update_password(str(user["id"]), hashed)
        return True
