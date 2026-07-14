from uuid import UUID
import datetime
import random
from app.domains.loans.repository import LoanRepository
from app.core.audit import AuditService
from app.core.exceptions import DomainException
from app.domains.loans.schemas import LoanRow
from app.core.database import get_transaction

CUSTOMER_TYPE_MAP = {
    "new": "new",
    "new customer": "new",
    "existing": "existing",
    "existing customer": "existing",
}

LOAN_TYPE_MAP = {
    "enterprise": "enterprise",
    "enterprise loan": "enterprise",
    "msef": "msef",
    "payee": "payee",
    "other": "other",
    "other option": "other",
}

REPAYMENT_MODE_MAP = {
    "cheque": "cheque",
    "standing order": "standing_order",
    "standing_order": "standing_order",
    "direct debit": "direct_debit",
    "direct_debit": "direct_debit",
    "cash deposit": "cash_deposit",
    "cash_deposit": "cash_deposit",
}


def _normalize_choice(value: str, allowed: dict[str, str], field_name: str) -> str:
    key = value.strip().lower().replace("_", " ")
    normalized = allowed.get(key)
    if not normalized:
        raise DomainException(f"Invalid {field_name}", 422)
    return normalized


def _optional_float(value):
    if value in (None, ""):
        return None
    if isinstance(value, (int, float)):
        return float(value)
    cleaned = str(value).replace(",", "").replace("NGN", "").replace("₦", "").strip()
    return float(cleaned) if cleaned else None


def _optional_int(value):
    parsed = _optional_float(value)
    return int(parsed) if parsed is not None else None


class LoanService:
    def __init__(self, repo: LoanRepository, audit: AuditService):
        self.repo = repo
        self.audit = audit

    async def create_loan(
        self,
        org_id: UUID,
        customer_type: str,
        loan_type: str,
        applicant_name: str,
        user_id: UUID
    ) -> LoanRow:
        customer_type = _normalize_choice(customer_type, CUSTOMER_TYPE_MAP, "customer type")
        loan_type = _normalize_choice(loan_type, LOAN_TYPE_MAP, "loan type")

        # Generate ref_no
        year = datetime.datetime.now().year
        rand_val = random.randint(10000, 99999)
        ref_no = f"MMFB-{year}-{rand_val}"
        
        async with get_transaction() as conn:
            tx_repo = LoanRepository(conn)
            tx_audit = AuditService(conn)
            
            created_app = await tx_repo.create(
                org_id=org_id,
                ref_no=ref_no,
                customer_type=customer_type,
                loan_type=loan_type,
                applicant_name=applicant_name,
                created_by=user_id
            )
            
            # Log audit
            await tx_audit.log(
                application_id=str(created_app.id),
                org_id=str(org_id),
                action="Create Loan Draft",
                from_stage=None,
                to_stage="intake",
                actor_id=str(user_id)
            )
            return created_app

    async def get_wizard_data(self, app_id: UUID) -> dict:
        sd = await self.repo.get_stage_data(app_id, "intake")
        return sd["data_json"] if sd and sd.get("data_json") else {}

    async def save_wizard_step(self, app_id: UUID, step: int, form_data: dict, user_id: UUID, org_id: UUID) -> None:
        async with get_transaction() as conn:
            tx_repo = LoanRepository(conn)
            tx_audit = AuditService(conn)
            
            app = await tx_repo.get_by_id(app_id, org_id)
            if not app:
                raise DomainException("Application not found", 404)
                
            existing = await tx_repo.get_stage_data(app_id, "intake")
            existing_data = existing["data_json"] if existing and existing.get("data_json") else {}
            
            # Merge form data
            for k, v in form_data.items():
                existing_data[k] = v

            if "repayment_mode" in existing_data and existing_data["repayment_mode"]:
                existing_data["repayment_mode"] = _normalize_choice(
                    existing_data["repayment_mode"], REPAYMENT_MODE_MAP, "repayment mode"
                )
                
            # If step 1, we can pre-populate applicant_name/phone/bvn onto loan_application
            if step == 1:
                applicant_name = form_data.get("applicant_name") or form_data.get("full_name") or app.applicant_name
                phone = form_data.get("phone") or app.phone
                bvn = form_data.get("bvn") or app.bvn
                amount = form_data.get("amount") or app.amount
                tenor_months = form_data.get("tenor_months") or form_data.get("tenor") or app.tenor_months

                await tx_repo.update_intake_details(
                    loan_id=app_id,
                    org_id=org_id,
                    applicant_name=applicant_name,
                    phone=phone,
                    bvn=bvn,
                    amount=_optional_float(amount),
                    tenor_months=_optional_int(tenor_months),
                )
                
            await tx_repo.save_stage_data(app_id, "intake", existing_data, user_id)

            if step == 8:
                # Account Officers complete field intake but do not formally
                # submit loan applications. The Branch Manager owns the first
                # review and submission decision.
                await tx_repo.advance_stage(app_id, org_id, "branch_manager_review")
                await tx_repo.assign_default_branch_manager(app_id, org_id)
                await tx_audit.log(
                    application_id=str(app_id),
                    org_id=str(org_id),
                    action="Complete Intake Form",
                    from_stage="intake",
                    to_stage="branch_manager_review",
                    actor_id=str(user_id)
                )
