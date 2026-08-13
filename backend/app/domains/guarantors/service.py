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
        data = dict(sd["data_json"]) if sd and sd.get("data_json") else {}
        from app.core.field_encryption import decrypt_sensitive
        for field in ("bvn", "account_number", "cheque_number"):
            if field in data:
                data[field] = decrypt_sensitive(data[field], context=f"guarantor_stage:{field}")
        return data

    async def save_wizard_step(self, loan_id: UUID, slot: int, step: int, form_data: dict, user_id: UUID) -> None:
        prod = await self.loan_repo.conn.fetchrow(
            """
            SELECT lp.guarantor_required, lp.name 
            FROM loan_applications la
            JOIN loan_products lp ON la.loan_type = lp.code
            WHERE la.id = $1
            """,
            loan_id
        )
        if prod and not prod["guarantor_required"]:
            from app.core.exceptions import DomainException
            raise DomainException(f"Guarantors are not required or accepted for {prod['name']}", 422)

        existing = await self.loan_repo.get_stage_data(loan_id, f"guarantor_{slot}")
        existing_data = existing["data_json"] if existing and existing.get("data_json") else {}
        for k, v in form_data.items():
            existing_data[k] = v
        from app.core.field_encryption import encrypt_sensitive
        # Guarantors no longer sign; silently discard obsolete clients' fields
        # so historical mobile/web versions cannot reintroduce signature data.
        existing_data.pop("guarantor_signature", None)
        existing_data.pop("witness_signature", None)
        form_data.pop("guarantor_signature", None)
        form_data.pop("witness_signature", None)
        for field in ("bvn", "account_number", "cheque_number"):
            if field in form_data:
                existing_data[field] = encrypt_sensitive(
                    existing_data.get(field), context=f"guarantor_stage:{field}"
                )
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
        g_data = await self.get_wizard_data(loan_id, slot)

        # Keep the parent wizard's summary in sync with the submitted
        # guarantor record.  The step-three card reads these fields directly.
        data.update({
            f"guarantor_{slot}_name": g_data.get("name", ""),
            f"guarantor_{slot}_relationship": g_data.get("relationship", ""),
            f"guarantor_{slot}_phone": g_data.get("phone", ""),
            f"guarantor_{slot}_status": "Submitted",
        })
        await self.loan_repo.save_stage_data(loan_id, "intake", data, submitted_by)

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
            signature_detected=False,
            witness_signature_detected=False,
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
