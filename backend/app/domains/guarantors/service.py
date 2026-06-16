from uuid import UUID

from app.core.audit import AuditService
from app.domains.guarantors.repository import GuarantorRepository
from app.domains.loans.repository import LoanRepository


class GuarantorService:
    def __init__(self, repo: GuarantorRepository, loan_repo: LoanRepository, audit: AuditService):
        self.repo = repo
        self.loan_repo = loan_repo
        self.audit = audit

    async def get_wizard_data(self, loan_id: UUID, slot: int) -> dict:
        sd = await self.loan_repo.get_stage_data(loan_id, f"guarantor_{slot}")
        return sd["data_json"] if sd and sd.get("data_json") else {}

    async def save_wizard_step(self, loan_id: UUID, slot: int, step: int, form_data: dict, user_id: UUID) -> None:
        existing = await self.loan_repo.get_stage_data(loan_id, f"guarantor_{slot}")
        existing_data = existing["data_json"] if existing and existing.get("data_json") else {}
        for k, v in form_data.items():
            existing_data[k] = v
        await self.loan_repo.save_stage_data(loan_id, f"guarantor_{slot}", existing_data, user_id)

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

        g_data = await self.get_wizard_data(loan_id, slot)

        full_name = g_data.get("name")
        relationship = g_data.get("relationship")
        bvn = g_data.get("bvn")
        phone = g_data.get("phone")
        home_address = g_data.get("home_address")
        employment_type = g_data.get("employment_type")

        monthly_salary = None
        if g_data.get("monthly_salary"):
            try:
                monthly_salary = float(g_data["monthly_salary"])
            except ValueError:
                pass

        max_guarantee = None
        if g_data.get("max_guarantee"):
            try:
                max_guarantee = float(g_data["max_guarantee"])
            except ValueError:
                pass

        bank_name = g_data.get("bank_name")
        account_number = g_data.get("account_number")
        cheque_number = g_data.get("cheque_number")

        sig = g_data.get("guarantor_signature")
        signature_detected = bool(sig and len(sig) > 50)

        wit_sig = g_data.get("witness_signature")
        witness_signature_detected = bool(wit_sig and len(wit_sig) > 50)

        guarantor = await self.repo.upsert_submitted(
            loan_id=loan_id,
            org_id=org_id,
            slot=slot,
            full_name=full_name,
            relationship_to_client=relationship,
            bvn=bvn,
            phone=phone,
            home_address=home_address,
            employment_type=employment_type,
            monthly_salary=monthly_salary,
            max_guarantee_amount=max_guarantee,
            bank_name=bank_name,
            account_number=account_number,
            cheque_number=cheque_number,
            signature_detected=signature_detected,
            witness_signature_detected=witness_signature_detected,
        )

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
