import secrets
import hashlib
from uuid import uuid4
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

    @staticmethod
    def _refresh_hash(token: str) -> str:
        return hashlib.sha256(token.encode("utf-8")).hexdigest()

    async def authenticate_mobile(self, email: str, password: str) -> dict:
        user = await self.repo.get_user_by_email(email)
        if not user or not user.is_active or not verify_password(password, user.hashed_password):
            raise DomainException("Incorrect email or password.", 401)
        await self.repo.record_login(str(user.id))
        refresh_token = secrets.token_urlsafe(48)
        absolute_expiry = datetime.now(timezone.utc) + timedelta(hours=48)
        await self.repo.create_auth_session(
            user_id=user.id, org_id=user.org_id, family_id=uuid4(),
            token_hash=self._refresh_hash(refresh_token), expires_at=absolute_expiry,
        )
        return {
            "access_token": create_access_token(user.id, role=user.role, org_id=user.org_id, session_type="mobile"),
            "token_type": "bearer", "refresh_token": refresh_token,
            "access_expires_in": 600, "session_expires_at": absolute_expiry.isoformat(),
        }

    async def rotate_mobile_session(self, refresh_token: str) -> dict:
        token_hash = self._refresh_hash(refresh_token)
        async with self.repo.conn.transaction():
            session = await self.repo.lock_auth_session(token_hash)
            if not session:
                raise DomainException("Invalid refresh session.", 401)
            if session["revoked_at"] is not None:
                await self.repo.revoke_family(session["family_id"], "refresh_token_reuse")
                raise DomainException("Refresh token reuse detected.", 401)
            if session["expires_at"] <= datetime.now(timezone.utc):
                await self.repo.revoke_family(session["family_id"], "expired")
                raise DomainException("Refresh session expired.", 401)
            user = await self.repo.get_user_by_id(str(session["user_id"]))
            if not user or not user["active"]:
                await self.repo.revoke_family(session["family_id"], "user_inactive")
                raise DomainException("Session is no longer active.", 401)
            replacement = secrets.token_urlsafe(48)
            new_row = await self.repo.create_auth_session(
                user_id=session["user_id"], org_id=session["org_id"], family_id=session["family_id"],
                token_hash=self._refresh_hash(replacement), expires_at=session["expires_at"],
            )
            await self.repo.rotate_auth_session(session["id"], new_row["id"])
        return {
            "access_token": create_access_token(user["id"], role=user["role"], org_id=user["org_id"], session_type="mobile"),
            "token_type": "bearer", "refresh_token": replacement,
            "access_expires_in": 600, "session_expires_at": session["expires_at"].isoformat(),
        }

    async def revoke_mobile_session(self, refresh_token: str | None) -> None:
        if refresh_token:
            await self.repo.revoke_by_hash(self._refresh_hash(refresh_token))

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
        from app.core.cache import invalidate_auth_user
        await invalidate_auth_user(user_id)
        return True

    async def change_password(self, user_id: str, current_password: str, new_password: str) -> bool:
        user = await self.repo.get_user_by_id(user_id)
        if not user or not verify_password(current_password, user["password_hash"]):
            return False
        hashed = get_password_hash(new_password)
        await self.repo.update_password(str(user["id"]), hashed)
        return True
