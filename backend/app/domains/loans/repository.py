from uuid import UUID
import json
from datetime import date
from app.core.base_repository import BaseRepository
from app.domains.loans.schemas import LoanRow, LoanListItem, StageCount, ReadinessSummary

class LoanRepository(BaseRepository):
    domain = "loans"

    async def resolve_product_code(self, input_val: str) -> str:
        if not input_val:
            from app.core.exceptions import DomainException
            raise DomainException("Invalid loan product code", 422)
            
        cleaned = input_val.strip().lower().replace("_", " ")
        row = await self.conn.fetchrow(
            "SELECT code FROM loan_products WHERE LOWER(code) = $1 AND active = TRUE",
            cleaned
        )
        if row:
            return row["code"]
            
        alias_row = await self.conn.fetchrow(
            "SELECT product_code FROM product_aliases WHERE LOWER(alias) = $1",
            cleaned
        )
        if alias_row:
            prod_row = await self.conn.fetchrow(
                "SELECT code FROM loan_products WHERE code = $1 AND active = TRUE",
                alias_row["product_code"]
            )
            if prod_row:
                return prod_row["code"]
                
        from app.core.exceptions import DomainException
        raise DomainException(f"Invalid or inactive loan product code: {input_val}", 422)

    async def get_next_stage_for_loan(self, loan_id: UUID, current_stage: str) -> str | None:
        row = await self.conn.fetchrow(
            """
            SELECT lp.workflow_stages 
            FROM loan_applications la
            JOIN loan_products lp ON la.loan_type = lp.code
            WHERE la.id = $1
            """,
            loan_id
        )
        if not row or not row["workflow_stages"]:
            from app.core.workflow import NEXT_STAGE
            return NEXT_STAGE.get(current_stage)
        
        stages = [s.strip() for s in row["workflow_stages"].split(",")]
        try:
            idx = stages.index(current_stage)
            if idx + 1 < len(stages):
                return stages[idx + 1]
        except ValueError:
            pass
        return None

    async def create(
        self,
        *,
        org_id: UUID,
        ref_no: str,
        customer_type: str,
        loan_type: str,
        applicant_name: str,
        created_by: UUID,
        client_request_id: UUID | None = None,
    ) -> LoanRow:
        user_row = await self.conn.fetchrow(
            "SELECT branch_id FROM users WHERE id = $1",
            str(created_by)
        )
        branch_id = user_row["branch_id"] if user_row else None

        row = await self.conn.fetchrow(
            self.sql("create"),
            org_id, ref_no, customer_type, loan_type, applicant_name, created_by,
            str(branch_id) if branch_id else None,
            str(client_request_id) if client_request_id else None,
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
        loan_type: str | None = None,
        query: str | None = None,
        from_date: date | None = None,
        to_date: date | None = None,
        branch_id: UUID | None = None,
    ) -> tuple[list[LoanListItem], int]:
        rows = await self.conn.fetch(
            self.sql("list_by_stage"),
            org_id, stage, officer_id, loan_type, query, from_date, to_date,
            size, (page - 1) * size,
            str(branch_id) if branch_id else None,
        )
        total = rows[0]["total_count"] if rows else 0
        return [LoanListItem(**r) for r in rows], total

    async def count_by_stage(self, org_id: UUID) -> list[StageCount]:
        rows = await self.conn.fetch(self.sql("count_by_stage"), org_id)
        return [StageCount(**r) for r in rows]

    async def get_readiness_summary(self, loan_id: UUID, org_id: UUID) -> ReadinessSummary:
        row = await self.conn.fetchrow(self.sql("readiness_summary"), loan_id, org_id)
        return ReadinessSummary(**row)

    async def get_application_detail_snapshot(
        self,
        loan_id: UUID,
        org_id: UUID,
    ) -> dict:
        row = await self.conn.fetchrow(
            self.sql("application_detail_snapshot"),
            loan_id,
            org_id,
        )
        return dict(row) if row else {}

    async def get_wizard_page_snapshot(
        self,
        loan_id: UUID,
        org_id: UUID,
    ) -> tuple[LoanRow, dict, dict | None, list[dict]] | None:
        row = await self.conn.fetchrow(
            self.sql("wizard_page_snapshot"),
            loan_id,
            org_id,
        )
        if not row:
            return None
        data = dict(row)
        wizard_data = data.pop("wizard_data", {}) or {}
        latest_version = data.pop("latest_version", None)
        signature_events = data.pop("signature_events", []) or []
        return LoanRow(**data), wizard_data, latest_version, signature_events

    async def approve(
        self,
        loan_id: UUID,
        org_id: UUID,
        approved_by: UUID,
        expected_stage: str = "branch_manager_review",
        next_stage: str = "branch_supervisor_review",
    ) -> LoanRow | None:
        row = await self.conn.fetchrow(
            self.sql("approve"),
            loan_id, org_id, approved_by, expected_stage, next_stage
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

    async def list_disbursed_page(self, org_id: UUID, limit: int = 10, offset: int = 0) -> tuple[list[LoanRow], int]:
        rows = await self.conn.fetch(
            """
            SELECT la.*, COUNT(*) OVER() AS total_count
            FROM loan_applications la
            WHERE la.org_id = $1
              AND la.stage = 'disbursed'
              AND la.deleted_at IS NULL
            ORDER BY la.updated_at DESC
            LIMIT $2 OFFSET $3
            """,
            org_id, limit, offset
        )
        if not rows:
            return [], 0
        total = rows[0]["total_count"]
        return [LoanRow(**r) for r in rows], total

    async def list_workflow_events_for_application(
        self, org_id: UUID, application_id: UUID, limit: int = 10, offset: int = 0
    ) -> list[dict]:
        rows = await self.conn.fetch(
            """
            SELECT * FROM workflow_events
            WHERE org_id = $1 AND loan_id = $2
            ORDER BY created_at DESC, id DESC
            LIMIT $3 OFFSET $4
            """,
            org_id, application_id, limit, offset
        )
        return [dict(r) for r in rows] if rows else []

    async def search(self, org_id: UUID, query: str) -> list[LoanRow]:
        rows = await self.conn.fetch(
            """
            SELECT * FROM loan_applications
            WHERE org_id = $1
              AND (applicant_name ILIKE '%' || $2 || '%'
                   OR ref_no ILIKE '%' || $2 || '%')
            ORDER BY updated_at DESC
            LIMIT 50
            """,
            org_id, query
        )
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

    async def assign_default_branch_manager(self, loan_id: UUID, org_id: UUID) -> None:
        await self.conn.execute(self.sql("assign_default_branch_manager"), loan_id, org_id)

    async def list_workflow_events(self, org_id: UUID):
        return await self.conn.fetch(self.sql("list_workflow_events"), org_id)

    async def list_workflow_events_for_application(
        self,
        org_id: UUID,
        loan_id: UUID,
        limit: int = 200,
        offset: int = 0,
    ):
        return await self.conn.fetch(
            self.sql("list_workflow_events_for_application"),
            org_id,
            loan_id,
            limit,
            offset,
        )

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

    async def advance_to_crm_review(self, loan_id: UUID, org_id: UUID, approved_by: UUID) -> LoanRow | None:
        row = await self.conn.fetchrow(
            self.sql("advance_to_crm_review"), loan_id, org_id, approved_by
        )
        return LoanRow(**row) if row else None

    async def advance_to_executive_approval(
        self, loan_id: UUID, org_id: UUID, crm_user_id: UUID, crm_notes: str
    ) -> LoanRow | None:
        row = await self.conn.fetchrow(
            self.sql("advance_to_executive_approval"), loan_id, org_id, crm_user_id, crm_notes
        )
        return LoanRow(**row) if row else None

    async def executive_approve(self, loan_id: UUID, org_id: UUID, executive_id: UUID) -> LoanRow | None:
        row = await self.conn.fetchrow(
            self.sql("executive_approve"), loan_id, org_id, executive_id
        )
        return LoanRow(**row) if row else None

    async def disburse(
        self,
        loan_id: UUID,
        org_id: UUID,
        disbursed_amount: float,
        disbursement_method: str,
        disbursed_bank_ref: str | None,
        disbursement_ref: str,
        interest_rate: float,
        repayment_frequency: str,
        schedule_method: str,
    ) -> LoanRow | None:
        row = await self.conn.fetchrow(
            self.sql("disburse"),
            loan_id, org_id, disbursed_amount, disbursement_method,
            disbursed_bank_ref, disbursement_ref, interest_rate,
            repayment_frequency, schedule_method,
        )
        return LoanRow(**row) if row else None

    async def ed_approve(self, loan_id: UUID, org_id: UUID, ed_user_id: UUID) -> LoanRow | None:
        row = await self.conn.fetchrow(self.sql("ed_approve"), loan_id, org_id, ed_user_id)
        return LoanRow(**row) if row else None

    async def ed_escalate_to_md(self, loan_id: UUID, org_id: UUID, ed_user_id: UUID) -> LoanRow | None:
        row = await self.conn.fetchrow(self.sql("ed_escalate_to_md"), loan_id, org_id, ed_user_id)
        return LoanRow(**row) if row else None

    async def md_approve(
        self, loan_id: UUID, org_id: UUID, md_user_id: UUID, notes: str
    ) -> LoanRow | None:
        row = await self.conn.fetchrow(self.sql("md_approve"), loan_id, org_id, md_user_id, notes)
        return LoanRow(**row) if row else None

    async def md_add_comment(self, loan_id: UUID, org_id: UUID, notes: str) -> bool:
        result = await self.conn.execute(self.sql("md_add_comment"), loan_id, org_id, notes)
        return result.startswith("UPDATE")

    async def list_ed_queue(self, org_id: UUID, limit: int = 50, offset: int = 0) -> list[dict]:
        rows = await self.conn.fetch(self.sql("list_ed_queue"), org_id, limit, offset)
        return [dict(r) for r in rows]

    async def list_md_queue(self, org_id: UUID, limit: int = 50, offset: int = 0) -> list[dict]:
        rows = await self.conn.fetch(self.sql("list_md_queue"), org_id, limit, offset)
        return [dict(r) for r in rows]

    async def insert_board_referral(
        self, loan_id: UUID, org_id: UUID, referred_by: UUID,
        board_member_email: str, board_member_name: str, notes: str
    ) -> dict:
        row = await self.conn.fetchrow(
            self.sql("insert_board_referral"),
            loan_id, org_id, referred_by, board_member_email, board_member_name, notes
        )
        return dict(row)

    async def get_board_referrals(self, loan_id: UUID, org_id: UUID) -> list[dict]:
        rows = await self.conn.fetch(self.sql("get_board_referrals"), loan_id, org_id)
        return [dict(r) for r in rows]

    async def get_last_loan(self, org_id: UUID, applicant_name: str, phone: str | None, current_loan_id: UUID) -> dict | None:
        row = await self.conn.fetchrow(
            self.sql("get_last_loan"), org_id, applicant_name, phone or "", current_loan_id
        )
        return dict(row) if row else None

    async def list_crm_queue(self, org_id: UUID, limit: int = 50, offset: int = 0) -> list[dict]:
        rows = await self.conn.fetch(self.sql("list_crm_queue"), org_id, limit, offset)
        return [dict(r) for r in rows]

    async def list_head_crm_queue(self, org_id: UUID, limit: int = 50, offset: int = 0) -> list[dict]:
        rows = await self.conn.fetch(self.sql("list_head_crm_queue"), org_id, limit, offset)
        return [dict(r) for r in rows]

    async def list_executive_queue(self, org_id: UUID, limit: int = 50, offset: int = 0) -> list[dict]:
        rows = await self.conn.fetch(self.sql("list_executive_queue"), org_id, limit, offset)
        return [dict(r) for r in rows]

    async def list_disbursed(self, org_id: UUID) -> list[dict]:
        rows = await self.conn.fetch(self.sql("list_disbursed"), org_id)
        return [dict(r) for r in rows]

    async def list_disbursed_page(
        self,
        org_id: UUID,
        limit: int = 50,
        offset: int = 0,
    ) -> tuple[list[dict], int]:
        rows = await self.conn.fetch(
            self.sql("list_disbursed_page"),
            org_id,
            limit,
            offset,
        )
        total = int(rows[0]["total_count"]) if rows else 0
        return [dict(row) for row in rows], total

    async def borrower_state_counts(self, org_id: UUID) -> dict:
        row = await self.conn.fetchrow(
            self.sql("borrower_state_counts"),
            org_id,
        )
        return dict(row) if row else {
            "total": 0,
            "draft": 0,
            "review": 0,
            "approved": 0,
            "active": 0,
        }

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
