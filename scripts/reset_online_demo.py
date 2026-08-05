import asyncio
import json
import os
import random
import sys
import uuid
from datetime import date, datetime, timedelta, timezone
from decimal import Decimal
from pathlib import Path

import asyncpg
from dotenv import load_dotenv


load_dotenv("backend/.env")
sys.path.insert(0, str(Path(__file__).resolve().parents[1] / "backend"))

DATABASE_URL = os.getenv("DATABASE_URL")
DEMO_PASSWORD_HASH = "pbkdf2_sha256$260000$ZmllbGRjcm0tZGVtby1zYWx0$ditwuWjTVIp6hukjbeVVTR4M1YOImExIsrQd4OjY/aY="
MIGRATIONS_TO_APPLY = [
    "backend/migrations/032_single_executive_role_holders.sql",
]
DEMO_USERS = [
    ("MVP Admin", "admin", "system_admin"),
    ("Adebayo Johnson", "branch.manager", "branch_manager"),
    ("Chidi Obi", "loan.officer", "loan_officer"),
    ("Fatima Bello", "credit.officer", "credit_analyst"),
    ("Samuel Okafor", "auditor", "auditor"),
    ("Nora Eke", "crm", "crm"),
    ("Mariam Danjuma", "md", "md"),
    ("Emmanuel Cole", "ed", "ed"),
]

BUSINESS_TABLES = [
    "repayment_records",
    "repayment_schedule",
    "audit_entries",
    "notifications",
    "visitation_reports",
    "workflow_events",
    "ocr_fields",
    "ocr_results",
    "signature_event_pdfs",
    "signature_events",
    "signing_auth_sessions",
    "signing_sessions",
    "field_edit_log",
    "document_upload_jobs",
    "document_upload_intents",
    "document_versions",
    "committee_votes",
    "loan_recommendations",
    "collateral_documents",
    "collateral_items",
    "business_pnl",
    "business_locations",
    "checklist_items",
    "verification_checks",
    "documents",
    "pledged_items",
    "guarantors",
    "stage_data",
    "loan_applications",
]

IMMUTABLE_HISTORY_TABLES = (
    "audit_entries", "workflow_events", "committee_votes",
    "verification_checks", "bureau_submissions", "sanctions_checks",
    "repayment_records",
    "signature_events", "signature_event_pdfs",
)

STAGES = [
    "intake",
    "branch_manager_review",
    "branch_supervisor_review",
    "credit_analyst_review",
    "crm_review",
    "head_crm_review",
    "ed_approval",
    "md_approval",
    "disbursement_ready",
    "disbursed",
]

APPLICANTS = [
    ("Grace Omowunmi", "Fashion retail stock expansion", "msef", "direct_debit", "trade"),
    ("Ibrahim Musa", "Working capital for provisions shop", "enterprise", "cheque", "commerce"),
    ("Chidi Okafor", "Salary backed emergency facility", "payee", "standing_order", "salary"),
    ("Aisha Lawal", "School supplies trading cycle", "other", "cash_deposit", "education"),
    ("Tunde Balogun", "Phone accessories inventory", "msef", "direct_debit", "trade"),
    ("Ngozi Eze", "Bakery oven and mixer purchase", "enterprise", "cheque", "manufacturing"),
    ("Kunle Adeyemi", "Payroll backed personal facility", "payee", "standing_order", "salary"),
    ("Maryam Sani", "Provision store expansion", "msef", "direct_debit", "commerce"),
    ("Peter Nwosu", "Cold room installation", "enterprise", "cheque", "food_services"),
    ("Blessing Udo", "Agric produce trading", "other", "cash_deposit", "agriculture"),
]

DOC_TYPES = [
    "loan_application_form",
    "passport_photo",
    "id_card",
    "utility_bill",
    "bank_statement",
    "guarantor_form_1",
    "guarantor_form_2",
    "pledge_form",
]

FORM_CODES = {
    "loan_application_form": "MMFB/CRM/01",
    "passport_photo": "MMFB/KYC/PHOTO",
    "id_card": "MMFB/KYC/ID",
    "utility_bill": "MMFB/KYC/ADDRESS",
    "bank_statement": "MMFB/CRM/BS",
    "guarantor_form_1": "MMFB/CRM/G01",
    "guarantor_form_2": "MMFB/CRM/G02",
    "pledge_form": "MMFB/CRM/02",
}

GENERATED_UPLOAD_DIR = Path("frontend/static/uploads/demo/generated")


def html_to_pdf_bytes(html: str) -> bytes:
    from app.services.pdf_service import _to_pdf

    return _to_pdf(html)


def _pdf_escape(value: str) -> str:
    return value.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)")


def simple_pdf_bytes(lines: list[str]) -> bytes:
    commands = ["BT", "/F1 10 Tf", "50 780 Td", "14 TL"]
    for line in lines:
        commands.append(f"({_pdf_escape(line[:110])}) Tj")
        commands.append("T*")
    commands.append("ET")
    stream = "\n".join(commands).encode("latin-1", errors="replace")
    objects = [
        b"<< /Type /Catalog /Pages 2 0 R >>",
        b"<< /Type /Pages /Kids [3 0 R] /Count 1 >>",
        b"<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >>",
        b"<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>",
        b"<< /Length " + str(len(stream)).encode("ascii") + b" >>\nstream\n" + stream + b"\nendstream",
    ]
    pdf = bytearray(b"%PDF-1.4\n")
    offsets = []
    for number, obj in enumerate(objects, start=1):
        offsets.append(len(pdf))
        pdf.extend(f"{number} 0 obj\n".encode("ascii"))
        pdf.extend(obj)
        pdf.extend(b"\nendobj\n")
    xref_offset = len(pdf)
    pdf.extend(f"xref\n0 {len(objects) + 1}\n0000000000 65535 f \n".encode("ascii"))
    for offset in offsets:
        pdf.extend(f"{offset:010d} 00000 n \n".encode("ascii"))
    pdf.extend(
        f"trailer\n<< /Size {len(objects) + 1} /Root 1 0 R >>\nstartxref\n{xref_offset}\n%%EOF\n".encode("ascii")
    )
    return bytes(pdf)


def build_seed_document_pdf(
    *,
    org_name: str,
    ref_no: str,
    applicant_name: str,
    doc_type: str,
    form_code: str,
    amount: Decimal,
    tenor: int,
    stage: str,
    phone: str = "",
    bvn: str = "",
    nin: str = "",
    home_address: str = "",
    business_address: str = "",
) -> bytes:
    title = doc_type.replace("_", " ").title()
    return simple_pdf_bytes([
        org_name,
        f"{title} | Form {form_code}",
        "Generated seed document",
        "",
        f"Reference Number: {ref_no}",
        f"Applicant Name: {applicant_name}",
        f"Phone Number: {phone}",
        f"BVN: {bvn}",
        f"NIN: {nin}",
        f"Home Address: {home_address}",
        f"Business Address: {business_address}",
        f"Workflow Stage: {stage.replace('_', ' ').title()}",
        f"Requested Amount: NGN {amount:,.2f}",
        f"Tenor: {tenor} months",
        "Document Status: Complete and legible for demo review",
        "",
        "Applicant Signature: Seeded signed copy",
        "Officer Verification: Verified",
    ])


def store_seed_file(file_bytes: bytes, relative_name: str) -> str:
    GENERATED_UPLOAD_DIR.mkdir(parents=True, exist_ok=True)
    target_path = GENERATED_UPLOAD_DIR / relative_name
    target_path.write_bytes(file_bytes)
    return "/static/uploads/demo/generated/" + relative_name


def upload_seed_document(
    *,
    file_bytes: bytes,
    org_id: str,
    loan_id: str,
    ref_no: str,
    doc_type: str,
    filename_stem: str,
) -> tuple[str, str, str]:
    inline_upload = os.getenv("SEED_CLOUDINARY_INLINE", "false").lower() in {"1", "true", "yes"}
    if inline_upload:
        from app.core.config import settings
        from app.services.cloud_storage_service import upload_to_cloudinary

        if not settings.cloudinary_enabled:
            raise RuntimeError("SEED_CLOUDINARY_INLINE is enabled, but Cloudinary settings are incomplete.")

        folder = f"fieldcrm/{org_id}/{loan_id}"
        result = upload_to_cloudinary(
            file_bytes=file_bytes,
            mime_type="application/pdf",
            folder=folder,
            public_id=f"{folder}/{doc_type}_{filename_stem}",
        )
        return result.stored_path, result.public_id or "", result.preview_url or result.stored_path

    stored_path = store_seed_file(file_bytes, f"{ref_no}_{filename_stem}.pdf")
    cloud_public_id = f"fieldcrm/{org_id}/{loan_id}/{doc_type}_{filename_stem}"
    return stored_path, cloud_public_id, stored_path


def pick(users_by_role, role, fallback):
    rows = users_by_role.get(role) or []
    return random.choice(rows)["id"] if rows else fallback["id"]


def role_name(user_id, users):
    for user in users:
        if user["id"] == user_id:
            return user["role"]
    return "system_admin"


async def execute_delete(conn, table):
    exists = await conn.fetchval(
        "SELECT to_regclass($1) IS NOT NULL",
        f"public.{table}",
    )
    if exists:
        await conn.execute(f"DELETE FROM {table}")


async def ensure_demo_users(conn):
    orgs = await conn.fetch("SELECT id, code FROM organisations WHERE active = TRUE ORDER BY created_at")
    for org in orgs:
        code = (org["code"] or "fieldcrm").lower()
        for full_name, email_name, role in DEMO_USERS:
            role_exists = await conn.fetchval(
                "SELECT TRUE FROM users WHERE org_id = $1 AND role = $2 LIMIT 1",
                org["id"],
                role,
            )
            if role_exists:
                continue

            email = f"{email_name}@{code}.com"
            email_exists = await conn.fetchval(
                "SELECT TRUE FROM users WHERE org_id = $1 AND lower(email) = lower($2) LIMIT 1",
                org["id"],
                email,
            )
            if email_exists:
                email = f"{email_name}.{role}@{code}.com"

            await conn.execute(
                """
                INSERT INTO users (org_id, full_name, email, password_hash, role, active)
                VALUES ($1, $2, $3, $4, $5, TRUE)
                """,
                org["id"],
                full_name,
                email,
                DEMO_PASSWORD_HASH,
                role,
            )


async def seed_org(conn, org, users):
    users_by_role = {}
    for user in users:
        users_by_role.setdefault(user["role"], []).append(user)

    fallback = users[0]
    account_officers = users_by_role.get("account_officer") or users_by_role.get("loan_officer") or [fallback]
    credit_officer = pick(users_by_role, "credit_analyst", fallback)
    branch_manager = pick(users_by_role, "branch_manager", fallback)
    branch_supervisor = pick(users_by_role, "branch_supervisor", branch_manager)
    crm = pick(users_by_role, "crm", fallback)
    head_crm = pick(users_by_role, "head_crm", crm)
    ed = pick(users_by_role, "ed", fallback)
    md = pick(users_by_role, "md", fallback)
    auditor = pick(users_by_role, "auditor", fallback)
    system_admin = pick(users_by_role, "system_admin", fallback)

    org_id = org["id"]
    prefix = (org["code"] or "CRM").upper()
    now = datetime.now(timezone.utc)

    loan_types = ("enterprise", "msef", "payee", "other")
    seed_cases = [
        (stage_index, stage, officer, seeded_loan_type, variant)
        for stage_index, stage in enumerate(STAGES, start=1)
        for officer in account_officers
        for seeded_loan_type in loan_types
        for variant in range(1, 3)
    ]

    for record_no, (idx, stage, officer, seeded_loan_type, variant) in enumerate(seed_cases, start=1):
            loan_officer = officer["id"]
            app = APPLICANTS[(record_no - 1) % len(APPLICANTS)]
            name, purpose, _, repayment_mode, sector = app
            loan_type = seeded_loan_type
            amount = Decimal(250000 + (idx * 175000) + (variant * 50000))
            tenor = 6 + ((idx + variant) % 18)
            loan_id = uuid.uuid4()
            created_at = now - timedelta(days=idx * 3 + variant)
            ref_no = f"{prefix}-2026-{record_no:04d}"

            owner = {
                "intake": loan_officer,
                "branch_manager_review": branch_manager,
                "branch_supervisor_review": branch_supervisor,
                "credit_analyst_review": credit_officer,
                "crm_review": crm,
                "head_crm_review": head_crm,
                "ed_approval": ed,
                "md_approval": md,
                "disbursement_ready": crm,
                "disbursed": crm,
            }.get(stage, loan_officer)

            stage_position = STAGES.index(stage)
            approved_by = branch_manager if stage_position > STAGES.index("branch_manager_review") else None
            approved_at = created_at + timedelta(hours=24) if approved_by else None
            crm_reviewed_by = crm if stage_position > STAGES.index("crm_review") else None
            crm_reviewed_at = created_at + timedelta(hours=30) if crm_reviewed_by else None
            executive_approved_by = md if stage_position > STAGES.index("md_approval") else None
            executive_approved_at = created_at + timedelta(hours=36) if executive_approved_by else None
            disbursed_at = created_at + timedelta(days=2) if stage == "disbursed" else None
            returned_at = None
            return_reason = None
            has_disbursement_details = stage in {"disbursement_ready", "disbursed"}

            days_past_due = 0
            classification = "current"
            if stage == "disbursed" and variant == 2:
                days_past_due = 18
                classification = "olem"
            elif stage == "disbursed" and variant == 3:
                days_past_due = 48
                classification = "substandard"

            applicant_name = f"{name} {record_no}"
            phone = f"0803{record_no:07d}"
            bvn = f"22{record_no:09d}"
            nin = f"12{record_no:09d}"
            email = f"{name.lower().replace(' ', '.')}.{record_no}@example.com"
            dob = date(1984 + (idx % 12), min(variant + 2, 12), min(idx + 8, 28))
            id_issue_date = date.today() - timedelta(days=365 * 2 + idx)
            id_expiry_date = date.today() + timedelta(days=365 * 3 + idx)
            home_address = f"House {idx}{variant}, Unity Estate, Lagos"
            business_address = f"Shop {idx}{variant}, Main Market Road, Lagos"
            landmark = f"Opposite Market Gate {idx}{variant}, Lagos"
            spouse_name = f"Spouse of {name}"
            spouse_phone = f"0705{idx:03d}{variant:03d}"
            employer_name = f"{name.split()[0]} Ventures"
            employer_address = f"Suite {idx}{variant}, Commerce Plaza, Lagos"
            account_number = f"20{record_no:08d}"
            guarantor_1_name = f"Primary Guarantor for {name}"
            guarantor_1_phone = f"0812{idx:03d}{variant:02d}1"
            guarantor_2_name = f"Secondary Guarantor for {name}"
            guarantor_2_phone = f"0812{idx:03d}{variant:02d}2"
            memo_bytes = build_seed_document_pdf(
                org_name=org["name"],
                ref_no=ref_no,
                applicant_name=applicant_name,
                doc_type="disbursement_memo",
                form_code="MMFB/CRM/DIS",
                amount=amount,
                tenor=tenor,
                stage=stage,
                phone=phone,
                bvn=bvn,
                nin=nin,
                home_address=home_address,
                business_address=business_address,
            )
            disbursement_memo_path, _, _ = upload_seed_document(
                file_bytes=memo_bytes,
                org_id=str(org_id),
                loan_id=str(loan_id),
                ref_no=ref_no,
                doc_type="disbursement_memo",
                filename_stem="disbursement_memo",
            )
            audit_bytes = build_seed_document_pdf(
                org_name=org["name"],
                ref_no=ref_no,
                applicant_name=applicant_name,
                doc_type="audit_package",
                form_code="MMFB/CRM/AUD",
                amount=amount,
                tenor=tenor,
                stage=stage,
                phone=phone,
                bvn=bvn,
                nin=nin,
                home_address=home_address,
                business_address=business_address,
            )
            audit_package_path, _, _ = upload_seed_document(
                file_bytes=audit_bytes,
                org_id=str(org_id),
                loan_id=str(loan_id),
                ref_no=ref_no,
                doc_type="audit_package",
                filename_stem="audit_package",
            )

            await conn.execute(
                """
                INSERT INTO loan_applications (
                    id, org_id, ref_no, customer_type, loan_type, stage, applicant_name,
                    bvn, phone, amount, tenor_months, purpose, repayment_mode, created_by,
                    current_owner_id, credit_officer_id, branch_manager_id, return_reason,
                    returned_at, approved_by, approved_at, disbursed_at, created_at, updated_at,
                    disbursement_ref, disbursement_method, disbursement_memo_path,
                    disbursed_amount, disbursed_bank_ref, audit_archived_at,
                    audit_package_path, executive_approved_by, executive_approved_at,
                    crm_reviewed_by, crm_reviewed_at, crm_notes, classification,
                    days_past_due, classification_updated_at, sector, interest_rate,
                    repayment_frequency, schedule_method, credit_bureau_1_date,
                    credit_bureau_2_date, crms_searched, crms_search_date
                ) VALUES (
                    $1,$2,$3,$4,$5,$6,$7,$8,$9,$10,$11,$12,$13,$14,$15,$16,$17,$18,
                    $19,$20,$21,$22,$23,$24,$25,$26,$27,$28,$29,$30,$31,$32,$33,$34,
                    $35,$36,$37,$38,$39,$40,$41,$42,$43,$44,$45,$46,$47
                )
                """,
                loan_id,
                org_id,
                ref_no,
                "existing" if idx % 2 == 0 else "new",
                loan_type,
                stage,
                applicant_name,
                bvn,
                phone,
                amount,
                tenor,
                purpose,
                repayment_mode,
                loan_officer,
                owner,
                credit_officer,
                branch_manager,
                return_reason,
                returned_at,
                approved_by,
                approved_at,
                disbursed_at,
                created_at,
                now - timedelta(hours=variant),
                f"DISB-{ref_no}" if has_disbursement_details else None,
                "bank_transfer" if has_disbursement_details else None,
                disbursement_memo_path if has_disbursement_details else None,
                amount if stage == "disbursed" else None,
                f"NIP-{idx}{variant}987654" if stage == "disbursed" else None,
                now - timedelta(days=1) if stage == "disbursed" else None,
                audit_package_path if stage == "disbursed" else None,
                executive_approved_by,
                executive_approved_at,
                crm_reviewed_by,
                crm_reviewed_at,
                "CRM review completed; documentation is internally consistent." if crm_reviewed_by else None,
                classification,
                days_past_due,
                now - timedelta(days=1),
                sector,
                Decimal("4.50"),
                "monthly",
                "flat_rate",
                date.today() - timedelta(days=4),
                date.today() - timedelta(days=3),
                True,
                date.today() - timedelta(days=2),
            )

            intake_json = {
                "applicant_name": applicant_name,
                "full_name": applicant_name,
                "applicant_full_name": applicant_name,
                "phone": phone,
                "phone_numbers": phone,
                "bvn": bvn,
                "nin": nin,
                "id_type": "National ID",
                "means_of_identification": "National ID",
                "id_number": nin,
                "id_issue_date": str(id_issue_date),
                "id_expiry": str(id_expiry_date),
                "id_expiry_date": str(id_expiry_date),
                "dob": str(dob),
                "date_of_birth": str(dob),
                "gender": "Female" if variant == 1 else "Male",
                "nationality": "Nigerian",
                "email_address": email,
                "state_of_origin": "Lagos",
                "lga": "Ikeja",
                "loan_amount": str(amount),
                "amount": str(amount),
                "tenor_months": tenor,
                "tenor": tenor,
                "loan_type": loan_type,
                "purpose": purpose,
                "loan_purpose": purpose,
                "loan_amount_requested": str(amount),
                "repayment_mode": repayment_mode,
                "repayment_method": repayment_mode,
                "marital_status": "Married" if variant == 1 else "Single",
                "employment_type": "Full-time" if loan_type == "payee" else "Self-employed",
                "employment_status": "Full-time" if loan_type == "payee" else "Self-employed",
                "sector": sector,
                "industry": sector,
                "interest_rate": "4.50",
                "repayment_frequency": "monthly",
                "business_address": business_address,
                "residential_address": home_address,
                "home_address": home_address,
                "landmark": landmark,
                "gps_coordinates": f"6.{450 + idx}{variant}, 3.{390 + idx}{variant}",
                "monthly_turnover": str(amount * Decimal("1.70")),
                "monthly_sales": str(amount * Decimal("1.45")),
                "monthly_expenses": str(amount * Decimal("0.62")),
                "net_monthly_income": str(amount * Decimal("0.35")),
                "monthly_salary": str(amount * Decimal("0.30")),
                "other_income": str(amount * Decimal("0.05")),
                "bank_name": "Mainstreet MFB",
                "account_number": account_number,
                "account_name": applicant_name,
                "salary_account_details": f"Mainstreet MFB / {account_number} / {applicant_name}",
                "proposed_disbursement_account": f"Mainstreet MFB - {account_number}",
                "proposed_disbursement_date": str(date.today() + timedelta(days=3)),
                "next_of_kin": f"Next of Kin for {name}",
                "next_of_kin_phone": f"0706{idx:03d}{variant:03d}",
                "spouse_name": spouse_name,
                "spouse_phone": spouse_phone,
                "spouse_phone_number": spouse_phone,
                "spouse_occupation": "Trader",
                "spouse_employer": f"{name.split()[0]} Family Store",
                "spouse_address": home_address,
                "spouse_business_address": f"Unit {idx}{variant}, Family Market Cluster, Lagos",
                "spouse_children": variant,
                "spouse_dependants": variant + 1,
                "spouse_signature": "signed",
                "employer_name": employer_name,
                "employer_address": employer_address,
                "position": "Business Owner" if loan_type != "payee" else "Account Officer",
                "staff_number": f"STF-{idx:02d}{variant:02d}",
                "date_employed": str(date.today() - timedelta(days=365 * (variant + 2))),
                "business_name": employer_name,
                "business_type": sector.replace("_", " ").title(),
                "nature_of_business": sector.replace("_", " ").title(),
                "business_registration_number": f"BN-{idx:02d}{variant:02d}9988",
                "years_in_business": variant + 2,
                "years_in_operation": variant + 2,
                "years_employed": variant + 2,
                "average_monthly_turnover": str(amount * Decimal("1.70")),
                "facility_bank": ["Access Bank"],
                "facility_amount": [str(amount * Decimal("0.15"))],
                "facility_tenor": [6],
                "guarantor_1_name": guarantor_1_name,
                "guarantor_1_relationship": "Business associate",
                "guarantor_1_phone": guarantor_1_phone,
                "guarantor_1_bvn": f"33{idx:02d}{variant:02d}17890",
                "guarantor_1_nin": f"44{idx:02d}{variant:02d}11223",
                "guarantor_1_address": "11 Broad Street, Lagos",
                "guarantor_1_status": "Verified",
                "guarantor_2_name": guarantor_2_name,
                "guarantor_2_relationship": "Family friend",
                "guarantor_2_phone": guarantor_2_phone,
                "guarantor_2_bvn": f"33{idx:02d}{variant:02d}27890",
                "guarantor_2_nin": f"44{idx:02d}{variant:02d}21223",
                "guarantor_2_address": "12 Broad Street, Lagos",
                "guarantor_2_status": "Verified",
                "pledge_amount_figs": str(amount),
                "pledge_amount_words": "Amount equal to requested facility",
                "shop_address": business_address,
                "house_address": home_address,
                "expected_sales_proceeds": str(amount * Decimal("1.20")),
                "collection_method": repayment_mode,
                "deposit_account": account_number,
                "applicant_signature": "signed",
                "signature_date": str(date.today()),
                "witness_name": f"Witness for {name}",
                "witness_address": f"15 Witness Close, Lagos",
                "witness_signature": "signed",
                "witness_date": str(date.today()),
                "consents": {
                    "credit_bureau": True,
                    "credit_check": True,
                    "gsi_mandate": True,
                    "cheque_authority": True,
                },
                "credit_bureau_consent": True,
                "cheque_authorisation": True,
                "gsi_mandate": True,
                "terms_acceptance": True,
            }
            await conn.execute(
                "INSERT INTO stage_data (loan_id, stage, data_json, saved_by, saved_at) VALUES ($1,$2,$3,$4,$5)",
                loan_id,
                "intake",
                json.dumps(intake_json),
                loan_officer,
                created_at + timedelta(minutes=30),
            )

            guarantor_ids = []
            for slot in (1, 2):
                guarantor_id = uuid.uuid4()
                guarantor_ids.append(guarantor_id)
                await conn.execute(
                    """
                    INSERT INTO guarantors (
                        id, loan_id, org_id, slot, full_name, relationship_to_client, bvn, phone,
                        home_address, employment_type, monthly_salary, max_guarantee_amount,
                        max_guarantee_amount_words, bank_name, account_number, cheque_number,
                        form_stage, signature_detected, witness_signature_detected
                    ) VALUES ($1,$2,$3,$4,$5,$6,$7,$8,$9,$10,$11,$12,$13,$14,$15,$16,$17,$18,$19)
                    """,
                    guarantor_id,
                    loan_id,
                    org_id,
                    slot,
                    guarantor_1_name if slot == 1 else guarantor_2_name,
                    "Business associate" if slot == 1 else "Family friend",
                    f"33{idx:02d}{variant:02d}{slot}7890",
                    guarantor_1_phone if slot == 1 else guarantor_2_phone,
                    f"{slot + 10} Broad Street, Lagos",
                    "Self-employed" if slot == 1 else "Full-time",
                    Decimal("280000.00") if slot == 1 else Decimal("220000.00"),
                    amount,
                    "Amount equal to requested facility",
                    "Mainstreet MFB",
                    f"10{idx:02d}{variant:02d}{slot:02d}890",
                    f"CHQ{idx:02d}{variant:02d}{slot:02d}",
                    "verified",
                    True,
                    True,
                )

            for item_no, item_name in enumerate(("Shop Stock", "Display Refrigerator", "POS Terminal"), start=1):
                await conn.execute(
                    """
                    INSERT INTO pledged_items (
                        loan_id, item_number, item_name, serial_number, description,
                        estimated_value, created_at, ncr_reg_number
                    ) VALUES ($1,$2,$3,$4,$5,$6,$7,$8)
                    """,
                    loan_id,
                    item_no,
                    item_name,
                    f"{ref_no}-PLG-{item_no}",
                    f"{item_name} pledged as collateral for {ref_no}",
                    amount * Decimal("0.45") if item_no == 1 else amount * Decimal("0.20"),
                    created_at + timedelta(hours=2),
                    f"NCR-{ref_no}-{item_no}",
                )

            # Populate the newer feasibility and application-overview screens.
            for address_line, city, state_name, location_function in (
                (business_address, "Lagos", "Lagos", "retail_outlet"),
                (f"Warehouse {record_no}, Industrial Estate, Lagos", "Ikeja", "Lagos", "warehouse"),
            ):
                await conn.execute(
                    """INSERT INTO business_locations
                       (application_id, address_line, city, state, function, created_by)
                       VALUES ($1,$2,$3,$4,$5,$6)""",
                    loan_id, address_line, city, state_name, location_function, loan_officer,
                )

            monthly_revenue = amount * Decimal("0.34")
            monthly_expenses = amount * (Decimal("0.25") if variant == 1 else Decimal("0.38"))
            await conn.execute(
                """INSERT INTO business_pnl
                   (application_id, revenue, expenses, period_label, created_by)
                   VALUES ($1,$2,$3,$4,$5)""",
                loan_id, monthly_revenue, monthly_expenses, "Average month", loan_officer,
            )

            # Inventory contributes 52.5% and cash contributes 20%, producing
            # 72.5% FSV coverage against the requested facility.
            await conn.execute(
                """INSERT INTO collateral_items
                   (application_id, collateral_type, narration, loan_based_price, face_value, created_by)
                   VALUES ($1,'inventory',$2,$3,NULL,$4)""",
                loan_id, f"Trading stock held at {business_address}", amount * Decimal("0.75"), loan_officer,
            )
            await conn.execute(
                """INSERT INTO collateral_items
                   (application_id, collateral_type, narration, loan_based_price, face_value, created_by)
                   VALUES ($1,'cash',$2,NULL,$3,$4)""",
                loan_id, f"Cash collateral reserved for {ref_no}", amount * Decimal("0.20"), loan_officer,
            )

            recommendation_steps = [
                (loan_officer, "account_officer", Decimal("0.95"), "Account Officer recommends the facility after customer assessment."),
            ]
            if stage_position > STAGES.index("branch_manager_review"):
                recommendation_steps.append((branch_manager, "branch_manager", Decimal("0.93"), "Branch review supports the proposed facility."))
            if stage_position > STAGES.index("credit_analyst_review"):
                recommendation_steps.append((credit_officer, "credit_analyst", Decimal("0.90"), "Credit analysis supports this risk-adjusted amount."))
            if stage_position > STAGES.index("crm_review"):
                recommendation_steps.append((crm, "crm", Decimal("0.92"), "CRM recommends approval subject to documented conditions."))
            if stage_position > STAGES.index("head_crm_review"):
                recommendation_steps.append((head_crm, "head_crm", Decimal("0.91"), "Head CRM concurs with the recommendation."))
            if stage_position > STAGES.index("ed_approval"):
                recommendation_steps.append((ed, "ed", Decimal("0.90"), "Executive Director recommends approval."))
            if stage_position > STAGES.index("md_approval"):
                recommendation_steps.append((md, "md", Decimal("0.90"), "Managing Director gives final approval concurrence."))

            for recommendation_index, (submitted_by, recommendation_role, factor, notes) in enumerate(recommendation_steps):
                recommended_amount = (amount * factor).quantize(Decimal("0.01"))
                await conn.execute(
                    """INSERT INTO loan_recommendations
                       (application_id, submitted_by, role_at_submission, recommended_amount, notes, created_at)
                       VALUES ($1,$2,$3,$4,$5,$6)""",
                    loan_id, submitted_by, recommendation_role, recommended_amount, notes,
                    created_at + timedelta(hours=3 + recommendation_index),
                )

            if stage_position > STAGES.index("ed_approval"):
                await conn.execute(
                    """INSERT INTO committee_votes
                       (loan_id, org_id, member_id, recommendation, notes, recommended_amount, voted_at)
                       VALUES ($1,$2,$3,'approve',$4,$5,$6)""",
                    loan_id, org_id, ed, "Approved based on feasibility and documented controls.",
                    amount * Decimal("0.90"), created_at + timedelta(hours=38),
                )
            if stage_position > STAGES.index("md_approval"):
                await conn.execute(
                    """INSERT INTO committee_votes
                       (loan_id, org_id, member_id, recommendation, notes, recommended_amount, voted_at)
                       VALUES ($1,$2,$3,'approve',$4,$5,$6)""",
                    loan_id, org_id, md, "Final executive concurrence recorded.",
                    amount * Decimal("0.90"), created_at + timedelta(hours=40),
                )
                await conn.execute(
                    """UPDATE loan_applications
                       SET amount=$1, mcc_finalized_by=$2, mcc_finalized_at=$3
                       WHERE id=$4""",
                    amount * Decimal("0.90"), crm, created_at + timedelta(hours=41), loan_id,
                )

            for doc_index, doc_type in enumerate(DOC_TYPES, start=1):
                doc_id = uuid.uuid4()
                verified = True
                quality = "clear"
                file_bytes = build_seed_document_pdf(
                    org_name=org["name"],
                    ref_no=ref_no,
                    applicant_name=applicant_name,
                    doc_type=doc_type,
                    form_code=FORM_CODES[doc_type],
                    amount=amount,
                    tenor=tenor,
                    stage=stage,
                    phone=phone,
                    bvn=bvn,
                    nin=nin,
                    home_address=home_address,
                    business_address=business_address,
                )
                stored_path, cloud_public_id, cloud_preview_url = upload_seed_document(
                    file_bytes=file_bytes,
                    org_id=str(org_id),
                    loan_id=str(loan_id),
                    ref_no=ref_no,
                    doc_type=doc_type,
                    filename_stem=doc_type,
                )
                await conn.execute(
                    """
                    INSERT INTO documents (
                        id, loan_id, org_id, guarantor_id, doc_type, form_code, original_name,
                        stored_path, mime_type, size_bytes, quality_status, verified,
                        verified_by, verified_at, uploaded_by, uploaded_at, zoho_file_id,
                        ocr_status, cloud_public_id, cloud_preview_url
                    ) VALUES ($1,$2,$3,$4,$5,$6,$7,$8,$9,$10,$11,$12,$13,$14,$15,$16,$17,$18,$19,$20)
                    """,
                    doc_id,
                    loan_id,
                    org_id,
                    guarantor_ids[0] if doc_type == "guarantor_form_1" else (guarantor_ids[1] if doc_type == "guarantor_form_2" else None),
                    doc_type,
                    FORM_CODES[doc_type],
                    f"{ref_no}_{doc_type}.pdf",
                    stored_path,
                    "application/pdf",
                    len(file_bytes),
                    quality,
                    verified,
                    credit_officer,
                    now - timedelta(hours=8),
                    loan_officer,
                    created_at + timedelta(hours=3),
                    f"zoho_{ref_no}_{doc_index}",
                    "done" if doc_type in {"loan_application_form", "guarantor_form_1", "guarantor_form_2", "pledge_form"} else "skipped",
                    cloud_public_id,
                    cloud_preview_url,
                )

                if doc_type in {"loan_application_form", "guarantor_form_1", "guarantor_form_2", "pledge_form"}:
                    ocr_id = uuid.uuid4()
                    confidence = Decimal("61.50") if quality == "blurry" else Decimal("91.25")
                    await conn.execute(
                        """
                        INSERT INTO ocr_results (id, document_id, loan_id, form_type, overall_confidence, raw_extraction, created_at)
                        VALUES ($1,$2,$3,$4,$5,$6,$7)
                        """,
                        ocr_id,
                        doc_id,
                        loan_id,
                        "guarantor" if doc_type.startswith("guarantor_form_") else ("pledge_receipt" if doc_type == "pledge_form" else "loan_application"),
                        confidence,
                        json.dumps({"source": "reset_online_demo", "doc_type": doc_type, "ref_no": ref_no}),
                        created_at + timedelta(hours=4),
                    )
                    fields = [
                        ("applicant_name", applicant_name, applicant_name, Decimal("93.00"), True),
                        ("phone", phone, phone, Decimal("93.00"), True),
                        ("bvn", bvn, bvn, confidence, True),
                        ("nin", nin, nin, Decimal("92.00"), True),
                        ("home_address", home_address, home_address, Decimal("88.00"), True),
                        ("business_address", business_address, business_address, Decimal("88.00"), False),
                        ("loan_amount", str(amount), str(amount), Decimal("89.00"), True),
                        ("signature", "detected", "detected", Decimal("74.00"), True),
                        ("document_date", str(date.today() - timedelta(days=idx)), str(date.today() - timedelta(days=idx)), Decimal("87.00"), False),
                    ]
                    for page, field in enumerate(fields, start=1):
                        field_name, value, corrected, field_conf, critical = field
                        verified_field = True
                        await conn.execute(
                            """
                            INSERT INTO ocr_fields (
                                ocr_result_id, loan_id, field_name, ocr_value, corrected_value,
                                confidence, source, is_critical, verified, verified_by, verified_at, page_number
                            ) VALUES ($1,$2,$3,$4,$5,$6,$7,$8,$9,$10,$11,$12)
                            """,
                            ocr_id,
                            loan_id,
                            field_name,
                            value,
                            corrected,
                            field_conf,
                            "corrected",
                            critical,
                            verified_field,
                            credit_officer,
                            now - timedelta(hours=5),
                            page,
                        )

            if stage_position >= STAGES.index("branch_supervisor_review"):
                await conn.execute(
                    """
                    INSERT INTO visitation_reports (
                        loan_id, org_id, visit_date, met_with, premises_description,
                        direction_from_branch, business_condition, visiting_officer_id,
                        visiting_officer_signature, account_officer_id, manager_concurrence,
                        manager_id, manager_notes, manager_concurred_at, status, created_at, updated_at
                    ) VALUES ($1,$2,$3,$4,$5,$6,$7,$8,$9,$10,$11,$12,$13,$14,$15,$16,$17)
                    """,
                    loan_id,
                    org_id,
                    date.today() - timedelta(days=2),
                    f"{name} {variant}",
                    "Business premises verified with stock records, customer traffic, and visible trading activity.",
                    "From branch office, proceed to market gate and turn right at the first junction.",
                    "Stable cashflow observed; inventory turnover aligns with requested facility.",
                    loan_officer,
                    True,
                    loan_officer,
                    True,
                    branch_manager,
                    "Concurred after site inspection.",
                    now - timedelta(days=1),
                    "concurred",
                    created_at + timedelta(hours=5),
                    now - timedelta(hours=2),
                )

            workflow_steps = [("loan.created", "created", "intake", loan_officer, "Application created by Relationship Officer.")]
            stage_path = [
                ("intake", "branch_manager_review", loan_officer, "Submitted to Team Lead."),
                ("branch_manager_review", "branch_supervisor_review", branch_manager, "Team Lead concurrence completed."),
                ("branch_supervisor_review", "credit_analyst_review", branch_supervisor, "Supervisor review completed."),
                ("credit_analyst_review", "crm_review", credit_officer, "Credit analysis completed."),
                ("crm_review", "head_crm_review", crm, "CRM review completed."),
                ("head_crm_review", "ed_approval", head_crm, "Head CRM review completed."),
                ("ed_approval", "md_approval", ed, "Executive Director approval completed."),
                ("md_approval", "disbursement_ready", md, "Managing Director approval completed."),
                ("disbursement_ready", "disbursed", crm, "Facility disbursed."),
            ]
            target_index = STAGES.index(stage)
            for transition_index, (from_stage, to_stage, actor, notes) in enumerate(stage_path):
                if transition_index >= target_index:
                    break
                workflow_steps.append((f"{from_stage}.completed", from_stage, to_stage, actor, notes))

            for step_no, (event_type, from_stage, to_stage, actor, notes) in enumerate(workflow_steps):
                event_time = created_at + timedelta(hours=step_no + 1)
                await conn.execute(
                    """
                    INSERT INTO workflow_events (
                        loan_id, org_id, event_type, from_stage, to_stage,
                        triggered_by, triggered_role, notes, created_at
                    ) VALUES ($1,$2,$3,$4,$5,$6,$7,$8,$9)
                    """,
                    loan_id,
                    org_id,
                    event_type,
                    from_stage,
                    to_stage,
                    actor,
                    role_name(actor, users),
                    notes,
                    event_time,
                )
                await conn.execute(
                    """
                    INSERT INTO audit_entries (
                        org_id, entity_type, entity_id, action, user_id, user_role,
                        field_name, old_value, new_value, source, notes, created_at
                    ) VALUES ($1,$2,$3,$4,$5,$6,$7,$8,$9,$10,$11,$12)
                    """,
                    org_id,
                    "loan_application",
                    loan_id,
                    event_type,
                    actor,
                    role_name(actor, users),
                    "stage",
                    from_stage,
                    to_stage,
                    "seed",
                    notes,
                    event_time,
                )

            notify_user = owner
            await conn.execute(
                """
                INSERT INTO notifications (
                    user_id, org_id, application_id, title, message, type, is_read, created_at
                ) VALUES ($1,$2,$3,$4,$5,$6,$7,$8)
                """,
                notify_user,
                org_id,
                loan_id,
                f"{ref_no} now in {stage.replace('_', ' ')}",
                f"{name} {variant}'s application is ready for {stage.replace('_', ' ')} action.",
                stage,
                variant == 3,
                now - timedelta(minutes=idx * variant),
            )

            if stage == "disbursed":
                monthly_principal = (amount / Decimal(tenor)).quantize(Decimal("0.01"))
                monthly_interest = (amount * Decimal("0.045")).quantize(Decimal("0.01"))
                for installment in range(1, tenor + 1):
                    due = date.today() + timedelta(days=30 * installment)
                    await conn.execute(
                        """
                        INSERT INTO repayment_schedule (
                            loan_id, org_id, installment_no, due_date,
                            principal_due, interest_due, total_due
                        ) VALUES ($1,$2,$3,$4,$5,$6,$7)
                        """,
                        loan_id,
                        org_id,
                        installment,
                        due,
                        monthly_principal,
                        monthly_interest,
                        monthly_principal + monthly_interest,
                    )
                for installment in range(1, min(3, tenor) + 1):
                    await conn.execute(
                        """
                        INSERT INTO repayment_records (
                            loan_id, org_id, payment_date, amount_paid, channel, bank_ref, recorded_by
                        ) VALUES ($1,$2,$3,$4,$5,$6,$7)
                        """,
                        loan_id,
                        org_id,
                        date.today() - timedelta(days=30 * (3 - installment)),
                        monthly_principal + monthly_interest,
                        "bank_transfer",
                        f"PAY-{ref_no}-{installment}",
                        crm,
                    )

    for user in users:
        await conn.execute(
            """
            INSERT INTO notifications (user_id, org_id, title, message, type, is_read, created_at)
            VALUES ($1,$2,$3,$4,$5,$6,$7)
            """,
            user["id"],
            org_id,
            "Demo data refreshed",
            "Business workflow data has been reset while auth credentials were preserved.",
            "system",
            False,
            now,
        )


async def main():
    if not DATABASE_URL:
        raise SystemExit("DATABASE_URL is not configured in backend/.env")

    conn = await asyncpg.connect(DATABASE_URL)
    try:
        for migration_path in MIGRATIONS_TO_APPLY:
            with open(migration_path, "r", encoding="utf-8") as migration_file:
                await conn.execute(migration_file.read())

        async with conn.transaction():
            # await ensure_demo_users(conn)

            immutable_tables = []
            for table in IMMUTABLE_HISTORY_TABLES:
                if await conn.fetchval("SELECT to_regclass($1) IS NOT NULL", f"public.{table}"):
                    await conn.execute(f"ALTER TABLE {table} DISABLE TRIGGER USER")
                    immutable_tables.append(table)
            for table in BUSINESS_TABLES:
                await execute_delete(conn, table)
            for table in immutable_tables:
                await conn.execute(f"ALTER TABLE {table} ENABLE TRIGGER USER")

            orgs = await conn.fetch("SELECT id, name, code FROM organisations WHERE active = TRUE ORDER BY created_at")
            if not orgs:
                raise SystemExit("No organisations found. Auth data was preserved; no seed data inserted.")

            seeded_orgs = 0
            for org in orgs:
                has_head_crm = await conn.fetchval(
                    "SELECT EXISTS(SELECT 1 FROM users WHERE org_id=$1 AND active=TRUE AND role='head_crm')",
                    org["id"],
                )
                if not has_head_crm:
                    crm_candidate = await conn.fetchrow(
                        """SELECT id FROM users
                           WHERE org_id=$1 AND active=TRUE AND role='crm'
                           ORDER BY created_at DESC OFFSET 1 LIMIT 1""",
                        org["id"],
                    )
                    if crm_candidate:
                        await conn.execute("UPDATE users SET role='head_crm' WHERE id=$1", crm_candidate["id"])
                users = await conn.fetch(
                    "SELECT id, full_name, email, role FROM users WHERE org_id = $1 AND active = TRUE ORDER BY created_at",
                    org["id"],
                )
                if users:
                    await seed_org(conn, org, users)
                    seeded_orgs += 1

            if seeded_orgs == 0:
                raise SystemExit("No active users found. Auth data was preserved; no seed data inserted.")

        counts = {}
        counts["users"] = await conn.fetchval("SELECT count(*) FROM users WHERE active = TRUE")
        for table in BUSINESS_TABLES:
            exists = await conn.fetchval("SELECT to_regclass($1) IS NOT NULL", f"public.{table}")
            if exists:
                counts[table] = await conn.fetchval(f"SELECT count(*) FROM {table}")
        print(json.dumps(counts, indent=2, default=str))
    finally:
        await conn.close()


if __name__ == "__main__":
    asyncio.run(main())
