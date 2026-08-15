import secrets
import hashlib
from uuid import uuid4, UUID
from datetime import datetime, timedelta, timezone

from app.core.security import verify_password, create_access_token, get_password_hash, validate_password_strength
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
        token = create_access_token(
            user.id, role=user.role, org_id=user.org_id,
            password_hash=user.password_hash, session_type=session_type,
        )
        return token

    @staticmethod
    def _refresh_hash(token: str) -> str:
        return hashlib.sha256(token.encode("utf-8")).hexdigest()

    async def authenticate_web(self, email: str, password: str, user_agent: str | None = None, ip_address: str | None = None) -> dict:
        user = await self.repo.get_user_by_email(email)
        if not user or not user.is_active:
            raise DomainException("Incorrect email or password.", 401)

        if not verify_password(password, user.hashed_password):
            raise DomainException("Incorrect email or password.", 401)

        await self.repo.record_login(str(user.id))
        
        refresh_token = secrets.token_urlsafe(48)
        absolute_expiry = datetime.now(timezone.utc) + timedelta(days=2)
        family_id = uuid4()
        
        await self.repo.create_refresh_token(
            user_id=user.id,
            token_hash=self._refresh_hash(refresh_token),
            family_id=family_id,
            expires_at=absolute_expiry,
            user_agent=user_agent,
            ip_address=ip_address
        )
        
        access_token = create_access_token(
            user.id, role=user.role, org_id=user.org_id,
            password_hash=user.password_hash, session_type="web",
        )
        
        return {
            "access_token": access_token,
            "refresh_token": refresh_token,
            "expires_at": absolute_expiry
        }

    async def authenticate_mobile(self, email: str, password: str, user_agent: str | None = None, ip_address: str | None = None) -> dict:
        user = await self.repo.get_user_by_email(email)
        if not user or not user.is_active or not verify_password(password, user.hashed_password):
            raise DomainException("Incorrect email or password.", 401)
        await self.repo.record_login(str(user.id))
        
        refresh_token = secrets.token_urlsafe(48)
        absolute_expiry = datetime.now(timezone.utc) + timedelta(days=2)
        family_id = uuid4()
        
        await self.repo.create_refresh_token(
            user_id=user.id,
            token_hash=self._refresh_hash(refresh_token),
            family_id=family_id,
            expires_at=absolute_expiry,
            user_agent=user_agent,
            ip_address=ip_address
        )
        return {
            "access_token": create_access_token(
                user.id, role=user.role, org_id=user.org_id,
                password_hash=user.password_hash, session_type="mobile",
            ),
            "token_type": "bearer", "refresh_token": refresh_token,
            "access_expires_in": 600, "session_expires_at": absolute_expiry.isoformat(),
        }

    async def rotate_refresh_token(self, raw_token: str, client_type: str = "web", user_agent: str | None = None, ip_address: str | None = None) -> dict:
        token_hash = self._refresh_hash(raw_token)
        async with self.repo.conn.transaction():
            session = await self.repo.get_refresh_token_by_hash_for_update(token_hash)
            if not session:
                raise DomainException("Invalid refresh session.", 401)
            
            # Replay attack / Reuse detection
            if session["used_at"] is not None or session["revoked_at"] is not None:
                # Multiple browser requests can arrive with the same expired
                # access token. The first consumes the refresh token; allow a
                # very short same-device grace window for the others to obtain
                # a fresh access token without revoking the family. They do
                # not receive another refresh token, so rotation is preserved.
                same_device = (
                    session["used_at"] is not None
                    and bool(user_agent)
                    and bool(ip_address)
                    and session.get("user_agent") == user_agent
                    and str(session.get("ip_address")) == str(ip_address)
                    and session["used_at"] >= datetime.now(timezone.utc) - timedelta(seconds=10)
                )
                if same_device:
                    user = await self.repo.get_user_by_id(str(session["user_id"]))
                    if user and user["active"]:
                        return {
                            "access_token": create_access_token(
                                user["id"], role=user["role"], org_id=user["org_id"],
                                password_hash=user["password_hash"], session_type=client_type
                            ),
                            "refresh_token": None,
                            "expires_at": session["expires_at"],
                        }
                await self.repo.revoke_refresh_token_family(session["family_id"])
                import logging
                logger = logging.getLogger("SecurityAudit")
                logger.error(
                    f"Security Compromise: Refresh token reuse attempt detected! "
                    f"User ID: {session['user_id']}, Family ID: {session['family_id']}, "
                    f"IP: {ip_address}, UA: {user_agent}"
                )
                raise DomainException("Refresh token reuse detected.", 401)

            if session["expires_at"] <= datetime.now(timezone.utc):
                await self.repo.revoke_refresh_token_family(session["family_id"])
                raise DomainException("Refresh session expired.", 401)

            user = await self.repo.get_user_by_id(str(session["user_id"]))
            if not user or not user["active"]:
                await self.repo.revoke_refresh_token_family(session["family_id"])
                raise DomainException("Session is no longer active.", 401)

            replacement = secrets.token_urlsafe(48)
            new_row = await self.repo.create_refresh_token(
                user_id=session["user_id"],
                token_hash=self._refresh_hash(replacement),
                family_id=session["family_id"],
                expires_at=session["expires_at"],
                user_agent=user_agent,
                ip_address=ip_address
            )
            
            await self.repo.mark_refresh_token_used(session["id"], new_row["id"])
            
            access_token = create_access_token(
                user["id"], role=user["role"], org_id=user["org_id"],
                password_hash=user["password_hash"], session_type=client_type,
            )
            
            return {
                "access_token": access_token,
                "refresh_token": replacement,
                "expires_at": session["expires_at"]
            }


    async def rotate_mobile_session(self, refresh_token: str, user_agent: str | None = None, ip_address: str | None = None) -> dict:
        res = await self.rotate_refresh_token(refresh_token, client_type="mobile", user_agent=user_agent, ip_address=ip_address)
        return {
            "access_token": res["access_token"],
            "token_type": "bearer",
            "refresh_token": res["refresh_token"],
            "access_expires_in": 600,
            "session_expires_at": res["expires_at"].isoformat(),
        }

    async def revoke_mobile_session(self, refresh_token: str | None) -> None:
        if refresh_token:
            token_hash = self._refresh_hash(refresh_token)
            row = await self.repo.get_refresh_token_by_hash_for_update(token_hash)
            if row:
                await self.repo.revoke_refresh_token_family(row["family_id"])

    async def revoke_web_session(self, refresh_token: str | None) -> None:
        await self.revoke_mobile_session(refresh_token)

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
        try:
            validate_password_strength(new_password)
        except ValueError as exc:
            raise DomainException(str(exc), 400) from exc
        hashed = get_password_hash(new_password)
        async with self.repo.conn.transaction():
            row = await self.repo.consume_valid_reset_token(token)
            if not row:
                return False
            user_id = str(row["user_id"])
            await self.repo.update_password(user_id, hashed)
            await self.repo.revoke_all_sessions_for_user(user_id)
        from app.core.cache import invalidate_auth_user
        await invalidate_auth_user(user_id)
        return True

    async def change_password(self, user_id: str, current_password: str, new_password: str) -> bool:
        try:
            validate_password_strength(new_password)
        except ValueError as exc:
            raise DomainException(str(exc), 400) from exc
        user = await self.repo.get_user_by_id(user_id)
        if not user or not verify_password(current_password, user["password_hash"]):
            return False
        hashed = get_password_hash(new_password)
        await self.repo.update_password(str(user["id"]), hashed)
        await self.repo.revoke_all_sessions_for_user(str(user["id"]))
        from app.core.cache import invalidate_auth_user
        await invalidate_auth_user(user["id"])
        return True

    async def _require_managed_user(self, current_admin, user_id: UUID) -> dict:
        user = await self.repo.get_user_by_id(str(user_id))
        if not user or str(user["org_id"]) != str(current_admin.org_id):
            raise DomainException("User not found.", 404)
        return user

    async def list_active_sessions(self, current_admin, user_id: UUID) -> list[dict]:
        await self._require_managed_user(current_admin, user_id)
        return await self.repo.list_active_sessions_for_user(user_id)

    async def revoke_all_user_sessions(self, current_admin, user_id: UUID) -> None:
        await self._require_managed_user(current_admin, user_id)
        await self.repo.revoke_all_sessions_for_user(user_id)
