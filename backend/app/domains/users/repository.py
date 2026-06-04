from uuid import UUID
from app.core.base_repository import BaseRepository
from app.domains.users.schemas import UserRow, OrganisationRow


class UserRepository(BaseRepository):
    domain = "users"

    async def get_by_id(self, user_id: UUID) -> UserRow | None:
        row = await self.conn.fetchrow(self.sql("get_by_id"), str(user_id))
        return UserRow(**row) if row else None

    async def get_by_email(self, email: str) -> UserRow | None:
        row = await self.conn.fetchrow(self.sql("get_by_email"), email)
        return UserRow(**row) if row else None

    async def create_organisation(self, name: str, code: str) -> OrganisationRow:
        row = await self.conn.fetchrow(self.sql("create_organisation"), name, code)
        return OrganisationRow(**row)

    async def create_user(
        self,
        org_id: UUID,
        full_name: str,
        email: str,
        role: str,
        password_hash: str,
    ) -> UserRow:
        row = await self.conn.fetchrow(
            self.sql("create_user"),
            str(org_id), full_name, email, role, password_hash,
        )
        return UserRow(**row)
