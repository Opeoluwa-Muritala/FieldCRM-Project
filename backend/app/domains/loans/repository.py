from uuid import UUID
import json
from app.core.base_repository import BaseRepository
from app.domains.loans.schemas import LoanRow, LoanListItem, StageCount, ReadinessSummary

class LoanRepository(BaseRepository):
    domain = "loans"

    async def create(
        self,
        *,
        org_id: UUID,
        ref_no: str,
        customer_type: str,
        loan_type: str,
        applicant_name: str,
        created_by: UUID,
    ) -> LoanRow:
        row = await self.conn.fetchrow(
            self.sql("create"),
            org_id, ref_no, customer_type, loan_type, applicant_name, created_by
        )
        return LoanRow(**row)

    async def get_by_id(self, loan_id: UUID, org_id: UUID) -> LoanRow | None:
        row = await self.conn.fetchrow(self.sql("get_by_id"), loan_id, org_id)
        return LoanRow(**row) if row else None

    async def list_by_stage(
        self,
        org_id: UUID,
        stage: str | None,
        officer_id: UUID | None,
        page: int,
        size: int,
    ) -> tuple[list[LoanListItem], int]:
        rows = await self.conn.fetch(
            self.sql("list_by_stage"),
            org_id, stage, officer_id, size, (page - 1) * size
        )
        total = rows[0]["total_count"] if rows else 0
        return [LoanListItem(**r) for r in rows], total

    async def count_by_stage(self, org_id: UUID) -> list[StageCount]:
        rows = await self.conn.fetch(self.sql("count_by_stage"), org_id)
        return [StageCount(**r) for r in rows]

    async def get_readiness_summary(self, loan_id: UUID, org_id: UUID) -> ReadinessSummary:
        row = await self.conn.fetchrow(self.sql("readiness_summary"), loan_id, org_id)
        return ReadinessSummary(**row)

    async def approve(
        self,
        loan_id: UUID,
        org_id: UUID,
        approved_by: UUID,
    ) -> LoanRow | None:
        row = await self.conn.fetchrow(
            self.sql("approve"),
            loan_id, org_id, approved_by
        )
        return LoanRow(**row) if row else None

    async def mark_returned(
        self,
        loan_id: UUID,
        org_id: UUID,
        return_reason: str,
        returned_by: UUID,
    ) -> bool:
        result = await self.conn.execute(
            self.sql("mark_returned"),
            loan_id, org_id, return_reason, returned_by
        )
        return result == "UPDATE 1"

    async def dashboard_metrics(self, org_id: UUID, user_id: UUID, role: str) -> dict:
        db_role = role.lower().replace(" ", "_")
        row = await self.conn.fetchrow(self.sql("dashboard_metrics"), org_id, user_id, db_role)
        return dict(row) if row else {}

    async def list_recent(self, org_id: UUID, limit: int = 10) -> list[LoanRow]:
        rows = await self.conn.fetch(self.sql("list_recent"), org_id, limit)
        return [LoanRow(**r) for r in rows]

    async def update_intake_details(
        self,
        *,
        loan_id: UUID,
        org_id: UUID,
        applicant_name: str,
        phone: str | None,
        bvn: str | None,
        amount: float | None,
        tenor_months: int | None,
    ) -> LoanRow | None:
        row = await self.conn.fetchrow(
            self.sql("update_intake_details"),
            applicant_name,
            phone,
            bvn,
            amount,
            tenor_months,
            loan_id,
            org_id,
        )
        return LoanRow(**row) if row else None

    async def advance_stage(self, loan_id: UUID, org_id: UUID, stage: str) -> LoanRow | None:
        row = await self.conn.fetchrow(self.sql("advance_stage"), stage, loan_id, org_id)
        return LoanRow(**row) if row else None

    async def list_workflow_events(self, org_id: UUID):
        return await self.conn.fetch(self.sql("list_workflow_events"), org_id)

    async def soft_delete(self, loan_id: UUID, org_id: UUID) -> UUID | None:
        row = await self.conn.fetchrow(self.sql("soft_delete"), loan_id, org_id)
        return row["id"] if row else None

    async def get_stage_data(self, loan_id: UUID, stage: str) -> dict | None:
        row = await self.conn.fetchrow(self.sql("get_stage_data"), loan_id, stage)
        if not row:
            return None
        data = dict(row)
        if isinstance(data.get("data_json"), str):
            data["data_json"] = json.loads(data["data_json"])
        return data

    async def save_stage_data(self, loan_id: UUID, stage: str, data: dict, user_id: UUID) -> dict:
        existing = await self.get_stage_data(loan_id, stage)
        payload = json.dumps(data)
        if not existing:
            row = await self.conn.fetchrow(
                self.sql("save_stage_data_insert"),
                loan_id, stage, payload, user_id
            )
        else:
            row = await self.conn.fetchrow(
                self.sql("save_stage_data_update"),
                payload, user_id, existing["id"]
            )
        result = dict(row)
        if isinstance(result.get("data_json"), str):
            result["data_json"] = json.loads(result["data_json"])
        return result
