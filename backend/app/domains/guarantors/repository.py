from uuid import UUID

from app.core.base_repository import BaseRepository


class GuarantorRepository(BaseRepository):
    domain = "guarantors"

    async def upsert_submitted(self, *, loan_id: UUID, org_id: UUID, slot: int) -> dict:
        row = await self.conn.fetchrow(self.sql("upsert_submitted"), loan_id, org_id, slot)
        return dict(row)
