from uuid import UUID
import datetime
import random
import json
import math
from app.domains.loans.repository import LoanRepository
from app.core.audit import AuditService
from app.core.exceptions import DomainException
from app.domains.loans.schemas import LoanRow
from app.core.database import get_transaction
from app.config import settings

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
        user_id: UUID,
        officer_id: UUID | None = None,
        client_request_id: UUID | None = None,
    ) -> LoanRow:
        customer_type = _normalize_choice(customer_type, CUSTOMER_TYPE_MAP, "customer type")

        # Generate ref_no
        year = datetime.datetime.now().year
        rand_val = random.randint(10000, 99999)
        ref_no = f"MMFB-{year}-{rand_val}"
        
        async with get_transaction() as conn:
            tx_repo = LoanRepository(conn)
            tx_audit = AuditService(conn)
            
            loan_type = await tx_repo.resolve_product_code(loan_type, org_id)
            
            created_app = await tx_repo.create(
                org_id=org_id,
                ref_no=ref_no,
                customer_type=customer_type,
                loan_type=loan_type,
                applicant_name=applicant_name,
                created_by=officer_id or user_id,
                client_request_id=client_request_id,
            )
            if settings.CONFIGURABLE_WORKFLOW_ENABLED:
                from app.domains.workflow.engine import WorkflowEngine
                engine = WorkflowEngine(conn)
                await engine.record_action(org_id, created_app.id, user_id, "originate")
                await engine.snapshot(created_app.id, org_id)
            
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
        data = dict(sd["data_json"]) if sd and sd.get("data_json") else {}
        from app.core.field_encryption import decrypt_sensitive
        for field in ("bvn", "nin", "account_number", "bank_account_number", "spouse_bvn"):
            if field in data:
                data[field] = decrypt_sensitive(data[field], context=f"intake:{field}")
        return data

    async def save_wizard_step(
        self, app_id: UUID, step: int, form_data: dict, user_id: UUID, org_id: UUID,
        capture_source: str = "manual_web",
    ) -> None:
        async with get_transaction() as conn:
            tx_repo = LoanRepository(conn)
            tx_audit = AuditService(conn)
            
            app = await tx_repo.get_by_id(app_id, org_id)
            if not app:
                raise DomainException("Application not found", 404)
                
            existing = await tx_repo.get_stage_data(app_id, "intake")
            existing_data = dict(existing["data_json"]) if existing and existing.get("data_json") else {}
            before = dict(existing_data)

            protected_fields = {
                "id", "org_id", "created_by", "created_at", "updated_at",
                "branch_id", "branch_manager_id", "current_owner_id", "stage",
                "approved_by", "approved_at", "disbursed_at", "deleted_at",
                "saved_by", "saved_at",
            }
            attempted = protected_fields.intersection(form_data)
            if attempted:
                raise DomainException(
                    f"Protected intake fields cannot be changed: {', '.join(sorted(attempted))}",
                    422,
                )
            
            # Business/SME applications never accept employment-only fields,
            # including values forged by bypassing the conditional web form.
            if step == 4:
                product_family = await conn.fetchval(
                    "SELECT family FROM loan_products WHERE code = $1",
                    app.loan_type,
                )
                if product_family == "corporate_business":
                    form_data = dict(form_data)
                    for field in (
                        "employment_type", "industry", "years_employed",
                        "employer_name", "monthly_salary", "employer_address",
                    ):
                        form_data.pop(field, None)
                        existing_data.pop(field, None)

            # National ID (NIN) does not expire. Discard a forged or stale
            # expiry value so downstream views cannot imply otherwise.
            if step == 1 and form_data.get("id_type") == "National ID":
                form_data = dict(form_data)
                form_data.pop("id_expiry", None)
                existing_data.pop("id_expiry", None)

            # Merge form data
            for k, v in form_data.items():
                existing_data[k] = v

            if "repayment_mode" in existing_data and existing_data["repayment_mode"]:
                existing_data["repayment_mode"] = _normalize_choice(
                    existing_data["repayment_mode"], REPAYMENT_MODE_MAP, "repayment mode"
                )
                
            # Validate the requested amount, but do not impose product amount
            # ceilings/floors. Approval routing can still use the amount.
            if step == 6:
                amount = form_data.get("amount") or existing_data.get("amount")
                tenor = form_data.get("tenor") or existing_data.get("tenor")
                f_amount = _optional_float(amount)
                if f_amount is None or not math.isfinite(f_amount) or f_amount <= 0:
                    raise DomainException("Loan amount must be greater than zero", 422)
                
                prod = await conn.fetchrow(
                    "SELECT min_tenor_months, max_tenor_months, name FROM loan_products WHERE code = $1",
                    app.loan_type
                )
                if prod:
                    if tenor:
                        i_tenor = _optional_int(tenor)
                        if i_tenor is not None:
                            if i_tenor < prod["min_tenor_months"]:
                                raise DomainException(f"Tenor is below the minimum limit of {prod['min_tenor_months']} months for {prod['name']}", 422)
                            if i_tenor > prod["max_tenor_months"]:
                                raise DomainException(f"Tenor exceeds the maximum limit of {prod['max_tenor_months']} months for {prod['name']}", 422)
                                
                # Sync amount and tenor_months to the main table
                await tx_repo.update_intake_details(
                    loan_id=app_id,
                    org_id=org_id,
                    applicant_name=app.applicant_name,
                    phone=app.phone,
                    bvn=app.bvn,
                    amount=f_amount,
                    tenor_months=_optional_int(tenor),
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
                if bvn:
                    try:
                        import logging
                        logger = logging.getLogger("LoansService")
                        from app.domains.verification.service import verify_bvn
                        await verify_bvn(bvn, loan_application_id=app_id, conn=tx_repo.conn)
                    except Exception as e:
                        logger.error(f"Failed to run BVN verification hook: {e}")
                
            from app.core.field_encryption import decrypt_sensitive, encrypt_sensitive, mask_sensitive
            restricted_intake_fields = {
                "bvn", "nin", "account_number", "bank_account_number", "spouse_bvn",
            }
            for field in restricted_intake_fields.intersection(form_data):
                existing_data[field] = encrypt_sensitive(
                    existing_data.get(field), context=f"intake:{field}"
                )

            await tx_repo.save_stage_data(app_id, "intake", existing_data, user_id)

            actor = await conn.fetchrow(
                "SELECT role FROM users WHERE id = $1 AND org_id = $2 AND active = TRUE",
                user_id,
                org_id,
            )
            actor_role = (actor["role"] if actor else "unknown").lower().replace(" ", "_")
            actor_role = {
                "loan_officer": "account_officer",
                "relationship_officer": "account_officer",
                "team_lead": "branch_manager",
                "supervisor": "branch_supervisor",
            }.get(actor_role, actor_role)
            source = "team_lead_correction" if actor_role == "branch_manager" else "relationship_officer_intake"

            sensitive_fields = {
                "bvn", "nin", "account_number", "bank_account_number",
                "spouse_bvn", "guarantor_bvn", "signature", "password", "token",
            }

            def audit_value(field: str, value) -> str | None:
                if value is None:
                    return None
                serialized = json.dumps(value, ensure_ascii=True, sort_keys=True) if isinstance(value, (dict, list)) else str(value)
                lowered = field.lower()
                if any(secret in lowered for secret in sensitive_fields):
                    if isinstance(value, str) and value.startswith("enc:v1:"):
                        try:
                            serialized = decrypt_sensitive(value, context=f"intake:{field}") or ""
                        except Exception:
                            return "masked"
                    return f"masked:{mask_sensitive(serialized)}"
                return serialized[:1000]

            changed_fields = []
            for field in sorted(form_data):
                old_value = before.get(field)
                new_value = existing_data.get(field)
                def comparable(value):
                    if field in restricted_intake_fields and isinstance(value, str) and value.startswith("enc:v1:"):
                        return decrypt_sensitive(value, context=f"intake:{field}")
                    return value
                if comparable(old_value) == comparable(new_value):
                    continue
                changed_fields.append(field)
                await tx_audit.insert(
                    org_id=org_id,
                    entity_type="loan_application",
                    entity_id=app_id,
                    action="intake.field_changed",
                    user_id=user_id,
                    user_role=actor_role,
                    field_name=field,
                    old_value=audit_value(field, old_value),
                    new_value=audit_value(field, new_value),
                    source=source,
                    notes=f"Intake step {step}",
                )
            if changed_fields:
                await tx_audit.insert(
                    org_id=org_id,
                    entity_type="loan_application",
                    entity_id=app_id,
                    action="intake.updated",
                    user_id=user_id,
                    user_role=actor_role,
                    source=source,
                    notes=f"Updated {len(changed_fields)} field(s) in intake step {step}",
                )
                # Provenance is an additive sidecar and never changes an officer-entered value.
                # It activates with Phase 1 so default-off deployments retain identical writes.
                from app.config import settings
                if settings.CBS_INTEGRATION_ENABLED:
                    from app.domains.core_banking.repository import CoreBankingRepository
                    if capture_source not in {"manual_web", "manual_android", "ocr"}:
                        raise DomainException("Invalid field capture source", 422)
                    metadata_repo = CoreBankingRepository(conn)
                    for field in changed_fields:
                        await metadata_repo.upsert_field_metadata(
                            org_id=org_id,
                            entity_type="loan_application",
                            entity_id=app_id,
                            field_name=field,
                            source=capture_source,
                            captured_by=user_id,
                        )
