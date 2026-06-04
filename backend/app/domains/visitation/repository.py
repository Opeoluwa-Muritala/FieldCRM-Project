from uuid import UUID

from app.core.base_repository import BaseRepository


class VisitationRepository(BaseRepository):
    domain = "visitation"

    async def get_by_loan(self, *, loan_id: UUID, org_id: UUID) -> dict | None:
        row = await self.conn.fetchrow(self.sql("get_by_loan"), loan_id, org_id)
        return dict(row) if row else None

    async def upsert_report(
        self,
        *,
        loan_id: UUID,
        org_id: UUID,
        met_with: str | None,
        premises_description: str | None,
        direction_from_branch: str | None,
    ) -> dict:
        row = await self.conn.fetchrow(
            self.sql("upsert_report"),
            loan_id,
            org_id,
            met_with,
            premises_description,
            direction_from_branch,
        )
        return dict(row)

    async def manager_signoff(
        self,
        *,
        loan_id: UUID,
        org_id: UUID,
        manager_id: UUID,
        notes: str,
        decision: str,
    ) -> dict | None:
        row = await self.conn.fetchrow(
            self.sql("manager_signoff"),
            loan_id,
            org_id,
            manager_id,
            notes,
            decision,
        )
        return dict(row) if row else None
