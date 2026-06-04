from uuid import UUID

from app.core.audit import AuditService
from app.domains.guarantors.repository import GuarantorRepository
from app.domains.loans.repository import LoanRepository


class GuarantorService:
    def __init__(self, repo: GuarantorRepository, loan_repo: LoanRepository, audit: AuditService):
        self.repo = repo
        self.loan_repo = loan_repo
        self.audit = audit

    async def mark_slot_submitted(
        self,
        *,
        loan_id: UUID,
        org_id: UUID,
        slot: int,
        submitted_by: UUID,
        user_role: str,
    ) -> dict:
        existing = await self.loan_repo.get_stage_data(loan_id, "intake")
        data = existing["data_json"] if existing and existing.get("data_json") else {}
        data[f"guarantor_{slot}_status"] = "Submitted"
        await self.loan_repo.save_stage_data(loan_id, "intake", data, submitted_by)

        guarantor = await self.repo.upsert_submitted(loan_id=loan_id, org_id=org_id, slot=slot)
        await self.audit.insert(
            org_id=org_id,
            entity_type="loan_application",
            entity_id=loan_id,
            action="guarantor.submitted",
            user_id=submitted_by,
            user_role=user_role,
            field_name=f"guarantor_{slot}",
            new_value="submitted",
            source="manual",
        )
        return guarantor
