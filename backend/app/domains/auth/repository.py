from app.core.base_repository import BaseRepository
from app.domains.users.schemas import UserRow
from datetime import datetime
from uuid import UUID


class AuthRepository(BaseRepository):
    domain = "auth"

    async def get_user_by_email(self, email: str) -> UserRow | None:
        email_clean = email.strip().lower()
        rows = await self.conn.fetch(self.sql("get_user_by_email"), email_clean)
        
        if not rows:
            return None
            
        if "@" not in email_clean:
            if len(rows) > 1:
                return None
                
        return UserRow(**rows[0])

    async def get_user_by_id(self, user_id: str):
        return await self.conn.fetchrow("SELECT * FROM users WHERE id = $1", user_id)

    async def record_login(self, user_id: str) -> None:
        await self.conn.execute(
            "UPDATE users SET last_login_at = CURRENT_TIMESTAMP WHERE id = $1",
            str(user_id),
        )

    async def create_reset_token(self, user_id: str, token: str, expires_at) -> None:
        await self.conn.execute(
            "INSERT INTO password_reset_tokens (user_id, token, expires_at) VALUES ($1, $2, $3)",
            user_id, token, expires_at
        )

    async def get_valid_reset_token(self, token: str):
        return await self.conn.fetchrow(
            "SELECT user_id FROM password_reset_tokens WHERE token = $1 AND expires_at > NOW() AND used_at IS NULL",
            token
        )

    async def mark_token_used(self, token: str) -> None:
        await self.conn.execute(
            "UPDATE password_reset_tokens SET used_at = NOW() WHERE token = $1",
            token
        )

    async def update_password(self, user_id: str, hashed_password: str) -> None:
        await self.conn.execute(
            "UPDATE users SET password_hash = $1, active = TRUE WHERE id = $2",
            hashed_password, user_id
        )

    async def create_auth_session(self, *, user_id, org_id, family_id, token_hash: str, expires_at: datetime):
        return await self.conn.fetchrow(
            """INSERT INTO auth_sessions(user_id, org_id, family_id, refresh_token_hash, expires_at)
               VALUES($1,$2,$3,$4,$5) RETURNING id""",
            user_id, org_id, family_id, token_hash, expires_at,
        )

    async def lock_auth_session(self, token_hash: str):
        return await self.conn.fetchrow(
            "SELECT * FROM auth_sessions WHERE refresh_token_hash=$1 FOR UPDATE", token_hash
        )

    async def rotate_auth_session(self, old_id, new_id) -> None:
        await self.conn.execute(
            """UPDATE auth_sessions SET revoked_at=NOW(), revoked_reason='rotated',
                      last_used_at=NOW(), rotated_to=$2 WHERE id=$1""", old_id, new_id
        )

    async def revoke_family(self, family_id, reason: str) -> None:
        await self.conn.execute(
            """UPDATE auth_sessions SET revoked_at=coalesce(revoked_at,NOW()), revoked_reason=$2
               WHERE family_id=$1""", family_id, reason
        )

    async def revoke_by_hash(self, token_hash: str, reason: str = "logout") -> None:
        await self.conn.execute(
            """UPDATE auth_sessions SET revoked_at=coalesce(revoked_at,NOW()), revoked_reason=$2
               WHERE refresh_token_hash=$1""", token_hash, reason
        )
