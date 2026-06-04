from app.core.base_repository import BaseRepository
from app.domains.users.schemas import UserRow


class AuthRepository(BaseRepository):
    domain = "auth"

    async def get_user_by_email(self, email: str) -> UserRow | None:
        row = await self.conn.fetchrow(self.sql("get_user_by_email"), email.strip().lower())
        return UserRow(**row) if row else None
