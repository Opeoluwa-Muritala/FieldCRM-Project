import asyncio
import os
import sys
import json
from datetime import date, timedelta
from decimal import Decimal
from pathlib import Path
import asyncpg
from dotenv import load_dotenv

# Set paths
BACKEND_DIR = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(BACKEND_DIR))
load_dotenv(BACKEND_DIR / ".env")

from app.services.loan_servicing_service import LoanServicingService

def make_mock_pdf_bytes(title: str, ref_no: str, applicant: str) -> bytes:
    html = f"""<!DOCTYPE html>
<html>
<head>
    <style>
        body {{ font-family: Arial, sans-serif; margin: 40px; line-height: 1.6; }}
        h1 {{ color: #2e0052; border-bottom: 2px solid #2e0052; padding-bottom: 8px; }}
        .meta {{ margin-bottom: 20px; font-size: 14px; color: #555; }}
    </style>
</head>
<body>
    <h1>{title}</h1>
    <div class="meta">
        <p><strong>Application Ref:</strong> {ref_no}</p>
        <p><strong>Applicant Name:</strong> {applicant}</p>
        <p><strong>Status:</strong> Verified &amp; Seeding Completed</p>
        <p><strong>Date:</strong> {date.today().strftime('%d %B %Y')}</p>
    </div>
    <p>This is a seeded mock file to serve as a placeholder for visual checks and document validation.</p>
</body>
</html>"""
    return html.encode("utf-8")

async def main():
    db_url = os.getenv("DATABASE_URL")
    if not db_url:
        print("DATABASE_URL is not set!")
        sys.exit(1)

    print("Connecting to database...")
    conn = await asyncpg.connect(db_url)
    
    try:
        # 1. Fetch all active loan applications
        loans = await conn.fetch(
            """
            SELECT id, org_id, ref_no, applicant_name, bvn, phone, amount, tenor_months, 
                   interest_rate, repayment_frequency, schedule_method, stage, created_at, created_by
            FROM loan_applications
            WHERE deleted_at IS NULL;
            """
        )
        print(f"Found {len(loans)} active loan applications to verify/seed.")
        
        for idx, loan in enumerate(loans, start=1):
            loan_id = loan["id"]
            ref_no = loan["ref_no"]
            org_id = loan["org_id"]
            
            # Default values if empty
            applicant_name = loan["applicant_name"] or f"Applicant {idx}"
            bvn = loan["bvn"] or f"22{idx:09d}"[:11]
            phone = loan["phone"] or f"0803{idx:06d}"
            amount = Decimal(str(loan["amount"] or 500000))
            tenor = loan["tenor_months"] or 12
            rate = Decimal(str(loan["interest_rate"] or 24.0))
            freq = loan["repayment_frequency"] or "monthly"
            method = loan["schedule_method"] or "flat_rate"
            stage = loan["stage"] or "intake"
            
            # Update application details in-place
            await conn.execute(
                """
                UPDATE loan_applications
                SET applicant_name = $1,
                    bvn = $2,
                    phone = $3,
                    amount = $4,
                    tenor_months = $5,
                    interest_rate = $6,
                    repayment_frequency = $7,
                    schedule_method = $8,
                    stage = $9,
                    disbursed_at = coalesce(disbursed_at, created_at),
                    disbursed_amount = coalesce(disbursed_amount, $4)
                WHERE id = $10;
                """,
                applicant_name, bvn, phone, amount, tenor, rate, freq, method, stage, loan_id
            )
            
            # 2. Seed repayment schedule if empty
            schedule_count = await conn.fetchval(
                "SELECT count(*) FROM repayment_schedule WHERE loan_id = $1;",
                loan_id
            )
            if schedule_count == 0:
                print(f"-> Seeding repayment schedule for {ref_no}...")
                servicing = LoanServicingService(conn)
                try:
                    await servicing.create_schedule(
                        loan_id=loan_id,
                        org_id=org_id,
                        principal=float(amount),
                        annual_rate=float(rate),
                        tenor_months=tenor,
                        frequency=freq,
                        method=method,
                        disbursement_date=date.today() - timedelta(days=30)
                    )
                except Exception as e:
                    print(f"Error creating schedule for {ref_no}: {e}")
            
            # 3. Seed default documents if none exist
            doc_count = await conn.fetchval(
                "SELECT count(*) FROM documents WHERE loan_id = $1 AND deleted_at IS NULL;",
                loan_id
            )
            if doc_count == 0:
                print(f"-> Adding default documents for {ref_no}...")
                doc_types = [
                    ("utility_bill", "Utility Bill", "MMFB/CRM/UTL"),
                    ("business_proof", "Business Proof", "MMFB/CRM/BUS"),
                    ("government_id", "Government ID", "MMFB/CRM/ID"),
                    ("bank_statement", "Bank Statement", "MMFB/CRM/BS")
                ]
                os.makedirs(os.path.join(BACKEND_DIR, "..", "frontend", "static", "uploads"), exist_ok=True)
                for doc_type, name, code in doc_types:
                    pdf_bytes = make_mock_pdf_bytes(f"Document: {name}", ref_no, applicant_name)
                    file_name = f"{ref_no}_{doc_type}.pdf"
                    local_path = os.path.join(BACKEND_DIR, "..", "frontend", "static", "uploads", file_name)
                    
                    with open(local_path, "wb") as f:
                        f.write(pdf_bytes)
                        
                    stored_path = f"/static/uploads/{file_name}"
                    await conn.execute(
                        """
                        INSERT INTO documents (
                            loan_id, org_id, doc_type, form_code, original_name, stored_path, mime_type, size_bytes,
                            quality_status, verified, verified_by, verified_at, uploaded_by, cloud_preview_url, ocr_status
                        )
                        VALUES ($1, $2, $3, $4, $5, $6, 'application/pdf', $7, 'clear', TRUE, $8, now(), $8, $6, 'done');
                        """,
                        loan_id, org_id, doc_type, code, f"{doc_type}.pdf", stored_path, len(pdf_bytes), loan["created_by"]
                    )
            
            # 4. Generate local PDF files for documents with empty stored_path
            docs = await conn.fetch(
                """
                SELECT id, doc_type, original_name 
                FROM documents 
                WHERE loan_id = $1 
                  AND (stored_path IS NULL OR stored_path = '' OR stored_path NOT LIKE '/%' AND stored_path NOT LIKE 'http%')
                  AND deleted_at IS NULL;
                """,
                loan_id
            )
            if docs:
                os.makedirs(os.path.join(BACKEND_DIR, "..", "frontend", "static", "uploads"), exist_ok=True)
                for doc in docs:
                    doc_id = doc["id"]
                    doc_type = doc["doc_type"]
                    
                    pdf_bytes = make_mock_pdf_bytes(f"Document: {doc_type.upper()}", ref_no, applicant_name)
                    file_name = f"{ref_no}_{doc_type}.pdf"
                    local_path = os.path.join(BACKEND_DIR, "..", "frontend", "static", "uploads", file_name)
                    
                    with open(local_path, "wb") as f:
                        f.write(pdf_bytes)
                        
                    stored_path = f"/static/uploads/{file_name}"
                    await conn.execute(
                        """
                        UPDATE documents
                        SET stored_path = $1,
                            size_bytes = $2,
                            quality_status = 'clear',
                            verified = TRUE,
                            ocr_status = 'done',
                            cloud_preview_url = $1
                        WHERE id = $3;
                        """,
                        stored_path, len(pdf_bytes), doc_id
                    )
                    
            # 5. Ensure verification tables are populated (Qore ID, AutoCred, AML)
            # verification_checks (BVN)
            check_bvn = await conn.fetchrow(
                "SELECT id FROM verification_checks WHERE loan_application_id = $1 AND subject_type = 'bvn';",
                loan_id
            )
            if not check_bvn:
                await conn.execute(
                    """
                    INSERT INTO verification_checks (loan_application_id, subject_type, provider, status, is_valid, raw_response)
                    VALUES ($1, 'bvn', 'qoreid', 'success', TRUE, '{}'::jsonb);
                    """,
                    loan_id
                )
                
            # bureau_submissions (CreditRegistry)
            check_bureau = await conn.fetchrow(
                "SELECT id FROM bureau_submissions WHERE loan_application_id = $1;",
                loan_id
            )
            if not check_bureau:
                await conn.execute(
                    """
                    INSERT INTO bureau_submissions (loan_application_id, registry_id, status, report_type, raw_response, provider)
                    VALUES ($1, 'mock_registry_999', 'success', 'AutoCred_v8_Summary', '{"status": "success", "registry_id": "mock_registry_999", "report_type": "AutoCred_v8_Summary", "data": {"is_approximate_placeholder": true, "score": 680, "active_loans_count": 2, "total_outstanding_balance": 1200000.0, "total_monthly_repayments": 85000.0, "total_delinquent_accounts": 0, "worst_payment_status": "performing"}}'::jsonb, 'creditregistry');
                    """,
                    loan_id
                )
                
            # sanctions_checks (AML / Youverify)
            check_aml = await conn.fetchrow(
                "SELECT id FROM sanctions_checks WHERE loan_application_id = $1;",
                loan_id
            )
            if not check_aml:
                await conn.execute(
                    """
                    INSERT INTO sanctions_checks (loan_application_id, subject_type, subject_name, status, category_count, raw_response)
                    VALUES ($1, 'individual', $2, 'clear', '{}'::jsonb, '{}'::jsonb);
                    """,
                    loan_id, applicant_name
                )
                
            # 6. Seed checklist_items
            checklist_count = await conn.fetchval(
                "SELECT count(*) FROM checklist_items WHERE loan_application_id = $1;",
                loan_id
            )
            if checklist_count == 0:
                items = [
                    ("branch_manager", "bm_signature", "Branch Manager Signature"),
                    ("branch_manager", "guarantor_check", "Guarantor Verification"),
                    ("branch_manager", "home_visit", "Physical Visitation"),
                    ("crm", "bureau_pull", "Credit Registry Report"),
                    ("crm", "crms_search", "CRMS Database Search"),
                    ("crm", "ncr_reg", "NCR Collateral Registration")
                ]
                for context, item_key, label in items:
                    await conn.execute(
                        """
                        INSERT INTO checklist_items (loan_application_id, context, item_key, item_label, is_checked)
                        VALUES ($1, $2, $3, $4, TRUE)
                        ON CONFLICT DO NOTHING;
                        """,
                        loan_id, context, item_key, label
                    )
                    
            print(f"[OK] Verification, documents, and repayment checks verified/seeded for {ref_no}.")
            
        print("Database in-place seeding completed successfully!")
    finally:
        await conn.close()

if __name__ == "__main__":
    asyncio.run(main())
