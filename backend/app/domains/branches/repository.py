from uuid import UUID
from app.core.base_repository import BaseRepository
from app.domains.branches.schemas import BranchResponse

class BranchRepository(BaseRepository):
    domain = "branches"

    async def create(self, org_id: UUID, name: str, code: str) -> BranchResponse:
        row = await self.conn.fetchrow(
            self.sql("create"),
            str(org_id), name, code.upper()
        )
        return BranchResponse(**row)

    async def list_by_org(self, org_id: UUID) -> list[BranchResponse]:
        rows = await self.conn.fetch(
            self.sql("list_by_org"),
            str(org_id)
        )
        return [BranchResponse(**r) for r in rows]
