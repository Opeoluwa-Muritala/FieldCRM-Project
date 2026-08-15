from app.core.base_repository import BaseRepository
from app.domains.users.schemas import UserRow
from datetime import datetime
import hashlib
from uuid import UUID


class AuthRepository(BaseRepository):
    domain = "auth"

    @staticmethod
    def _reset_token_hash(token: str) -> str:
        return hashlib.sha256(token.encode("utf-8")).hexdigest()

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
            user_id, self._reset_token_hash(token), expires_at
        )

    async def get_valid_reset_token(self, token: str):
        return await self.conn.fetchrow(
            """SELECT user_id FROM password_reset_tokens
               WHERE token = ANY($1::text[]) AND expires_at > NOW() AND used_at IS NULL
               ORDER BY created_at DESC LIMIT 1""",
            [self._reset_token_hash(token), token],
        )

    async def consume_valid_reset_token(self, token: str):
        return await self.conn.fetchrow(
            """UPDATE password_reset_tokens
               SET used_at = NOW()
               WHERE id = (
                   SELECT id FROM password_reset_tokens
                   WHERE token = ANY($1::text[])
                     AND expires_at > NOW() AND used_at IS NULL
                   ORDER BY created_at DESC
                   LIMIT 1
                   FOR UPDATE
               )
               RETURNING user_id""",
            [self._reset_token_hash(token), token],
        )

    async def update_password(self, user_id: str, hashed_password: str) -> None:
        await self.conn.execute(
            "UPDATE users SET password_hash = $1, active = TRUE WHERE id = $2",
            hashed_password, user_id
        )

    async def revoke_all_sessions_for_user(self, user_id) -> None:
        await self.conn.execute(
            "UPDATE refresh_tokens SET revoked_at = NOW() WHERE user_id = $1 AND revoked_at IS NULL",
            user_id,
        )
        await self.conn.execute(
            """UPDATE auth_sessions
               SET revoked_at = COALESCE(revoked_at, NOW()), revoked_reason = 'credential_changed'
               WHERE user_id = $1 AND revoked_at IS NULL""",
            user_id,
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

    # ----------------------------------------------------
    # New single-use refresh_tokens table repository layer
    # ----------------------------------------------------
    async def create_refresh_token(
        self, *, user_id: UUID, token_hash: str, family_id: UUID, expires_at: datetime, user_agent: str | None = None, ip_address: str | None = None
    ):
        return await self.conn.fetchrow(
            """INSERT INTO refresh_tokens (user_id, token_hash, family_id, expires_at, user_agent, ip_address)
               VALUES ($1, $2, $3, $4, $5, $6) RETURNING id, family_id, expires_at""",
            user_id, token_hash, family_id, expires_at, user_agent, ip_address
        )

    async def get_refresh_token_by_hash_for_update(self, token_hash: str):
        return await self.conn.fetchrow(
            "SELECT * FROM refresh_tokens WHERE token_hash = $1 FOR UPDATE",
            token_hash
        )

    async def mark_refresh_token_used(self, token_id: UUID, replaced_by_id: UUID) -> None:
        await self.conn.execute(
            "UPDATE refresh_tokens SET used_at = NOW(), replaced_by = $2 WHERE id = $1",
            token_id, replaced_by_id
        )

    async def revoke_refresh_token_family(self, family_id: UUID) -> None:
        await self.conn.execute(
            "UPDATE refresh_tokens SET revoked_at = NOW() WHERE family_id = $1 AND revoked_at IS NULL",
            family_id
        )

    async def revoke_refresh_token_by_hash(self, token_hash: str) -> None:
        await self.conn.execute(
            "UPDATE refresh_tokens SET revoked_at = NOW() WHERE token_hash = $1 AND revoked_at IS NULL",
            token_hash
        )

    async def cleanup_expired_tokens(self, retention_days: int = 30) -> int:
        result = await self.conn.execute(
            """DELETE FROM refresh_tokens 
               WHERE expires_at < NOW() - $1 * INTERVAL '1 day' 
                  OR revoked_at < NOW() - $1 * INTERVAL '1 day'
                  OR (used_at < NOW() - $1 * INTERVAL '1 day' AND replaced_by IS NOT NULL)""",
            retention_days
        )
        try:
            return int(result.split()[-1])
        except Exception:
            return 0

    async def list_active_sessions_for_user(self, user_id: UUID) -> list[dict]:
        rows = await self.conn.fetch(
            """SELECT DISTINCT ON (family_id) id, family_id, issued_at, expires_at, user_agent, ip_address
               FROM refresh_tokens
               WHERE user_id = $1 AND revoked_at IS NULL AND expires_at > NOW() AND used_at IS NULL
               ORDER BY family_id, issued_at DESC""",
            user_id
        )
        return [dict(r) for r in rows]
