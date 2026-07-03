import asyncio
import os
import uuid
import random
from datetime import datetime, timedelta
import asyncpg
from dotenv import load_dotenv

load_dotenv("backend/.env")

DATABASE_URL = os.getenv("DATABASE_URL")

async def clear_and_populate():
    conn = await asyncpg.connect(DATABASE_URL)
    
    print("Clearing tables...", flush=True)
    tables_to_clear = [
        "audit_entries", "notifications", "visitation_reports", "workflow_events",
        "ocr_fields", "ocr_results", "documents", "pledged_items", "guarantors",
        "stage_data", "loan_applications"
    ]
    for t in tables_to_clear:
        await conn.execute(f"DELETE FROM {t};")
        print(f"Cleared {t}", flush=True)
        
    print("Cleared.", flush=True)

    orgs = await conn.fetch("SELECT id FROM organisations;")
    if not orgs:
        print("No organisations found!")
        return
    org_id = orgs[0]['id']
    
    users = await conn.fetch("SELECT id, role FROM users WHERE org_id = $1;", org_id)
    if not users:
        print("No users found!")
        return

    users_by_role = {}
    for u in users:
        users_by_role.setdefault(u['role'], []).append(u['id'])
        
    loan_officer_id = users_by_role.get("loan_officer", [users[0]['id']])[0]
    credit_officer_id = users_by_role.get("credit_officer", [users[0]['id']])[0]
    branch_manager_id = users_by_role.get("branch_manager", [users[0]['id']])[0]
    auditor_id = users_by_role.get("auditor", [users[0]['id']])[0]
    
    states = [
        "intake", "ocr_review", "credit_review", "branch_approval",
        "disbursement_ready", "disbursed", "returned", "rejected"
    ]
    
    print("Populating data...")
    count = 0
    now = datetime.utcnow()

    for state in states:
        for i in range(10):
            count += 1
            app_id = str(uuid.uuid4())
            ref_no = f"LN-{state[:3].upper()}-{count:04d}"
            
            applicant_name = f"Applicant {state.capitalize()} {i+1}"
            
            approved_by = branch_manager_id if state in ("disbursement_ready", "disbursed") else None
            approved_at = now - timedelta(days=1) if approved_by else None
            disbursed_at = now if state == "disbursed" else None
            return_reason = "Missing signature" if state == "returned" else None
            returned_at = now if state == "returned" else None

            # Create loan
            await conn.execute("""
                INSERT INTO loan_applications (
                    id, org_id, ref_no, customer_type, loan_type, stage, 
                    applicant_name, bvn, phone, amount, tenor_months, purpose, repayment_mode,
                    created_by, current_owner_id, credit_officer_id, branch_manager_id,
                    return_reason, returned_at, approved_by, approved_at, disbursed_at
                ) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, $13, $14, $15, $16, $17, $18, $19, $20, $21, $22)
            """, app_id, org_id, ref_no, "new", "enterprise", state, 
                applicant_name, "22233344455", "08012345678", 500000.0, 12, "Business expansion", "direct_debit",
                loan_officer_id, loan_officer_id, credit_officer_id, branch_manager_id,
                return_reason, returned_at, approved_by, approved_at, disbursed_at)
                
            # stage_data
            await conn.execute("""
                INSERT INTO stage_data (id, loan_id, stage, data_json, saved_by)
                VALUES ($1, $2, $3, $4, $5)
            """, str(uuid.uuid4()), app_id, "intake", '{"step": 1, "completed": true, "business_name": "Test Business"}', loan_officer_id)
            
            # guarantors
            g1_id = str(uuid.uuid4())
            await conn.execute("""
                INSERT INTO guarantors (id, loan_id, org_id, slot, full_name, relationship_to_client, bvn, phone, home_address, employment_type, monthly_salary, max_guarantee_amount, max_guarantee_amount_words, bank_name, account_number, cheque_number, form_stage, signature_detected, witness_signature_detected)
                VALUES ($1, $2, $3, 1, $4, 'Brother', '22211100011', '08098765432', '123 Test St', 'Employed', 200000.0, 100000.0, 'One Hundred Thousand', 'Test Bank', '0123456789', 'CHQ001', 'verified', TRUE, TRUE)
            """, g1_id, app_id, org_id, f"Guarantor 1 for {count}")
            
            g2_id = str(uuid.uuid4())
            await conn.execute("""
                INSERT INTO guarantors (id, loan_id, org_id, slot, full_name, relationship_to_client, bvn, phone, home_address, employment_type, monthly_salary, max_guarantee_amount, max_guarantee_amount_words, bank_name, account_number, cheque_number, form_stage, signature_detected, witness_signature_detected)
                VALUES ($1, $2, $3, 2, $4, 'Sister', '22211100022', '08098765433', '124 Test St', 'Self-Employed', 150000.0, 100000.0, 'One Hundred Thousand', 'Test Bank 2', '0123456780', 'CHQ002', 'verified', TRUE, TRUE)
            """, g2_id, app_id, org_id, f"Guarantor 2 for {count}")
            
            # pledged_items
            await conn.execute("""
                INSERT INTO pledged_items (id, loan_id, item_number, item_name, serial_number, description, estimated_value)
                VALUES ($1, $2, 1, 'Car', 'VIN1234567890', 'Toyota Corolla 2015', 2000000.0)
            """, str(uuid.uuid4()), app_id)

            # documents
            doc_id = str(uuid.uuid4())
            await conn.execute("""
                INSERT INTO documents (id, loan_id, org_id, doc_type, original_name, stored_path, mime_type, size_bytes, quality_status, verified, verified_by, uploaded_by)
                VALUES ($1, $2, $3, 'id_card', 'id.pdf', '/path/to/id.pdf', 'application/pdf', 102400, 'clear', TRUE, $4, $5)
            """, doc_id, app_id, org_id, credit_officer_id, loan_officer_id)

            # ocr_results
            ocr_id = str(uuid.uuid4())
            await conn.execute("""
                INSERT INTO ocr_results (id, document_id, loan_id, form_type, overall_confidence, raw_extraction)
                VALUES ($1, $2, $3, 'loan_application', 95.5, '{"extracted": true}')
            """, ocr_id, doc_id, app_id)

            # ocr_fields
            await conn.execute("""
                INSERT INTO ocr_fields (id, ocr_result_id, loan_id, field_name, ocr_value, corrected_value, confidence, source, is_critical, verified, verified_by)
                VALUES ($1, $2, $3, 'Name', 'Applicant Test', 'Applicant Test Corrected', 98.0, 'ocr', TRUE, TRUE, $4)
            """, str(uuid.uuid4()), ocr_id, app_id, credit_officer_id)

            # workflow_events
            await conn.execute("""
                INSERT INTO workflow_events (id, loan_id, org_id, event_type, triggered_by, triggered_role, notes)
                VALUES ($1, $2, $3, 'application_created', $4, 'loan_officer', 'Created application')
            """, str(uuid.uuid4()), app_id, org_id, loan_officer_id)

            # visitation_reports (if past credit_review)
            if state in ("branch_approval", "disbursement_ready", "disbursed"):
                await conn.execute("""
                    INSERT INTO visitation_reports (id, loan_id, org_id, visit_date, met_with, premises_description, direction_from_branch, business_condition, visiting_officer_id, visiting_officer_signature, manager_concurrence, manager_id, status)
                    VALUES ($1, $2, $3, $4, 'Owner', 'Small shop', 'Turn left', 'Good', $5, TRUE, TRUE, $6, 'concurred')
                """, str(uuid.uuid4()), app_id, org_id, now.date(), loan_officer_id, branch_manager_id)

            # notifications
            await conn.execute("""
                INSERT INTO notifications (id, user_id, org_id, application_id, title, message, type)
                VALUES ($1, $2, $3, $4, 'App Updated', 'Stage is now ' || $5, 'update')
            """, 'notif_' + str(uuid.uuid4()).replace('-', ''), loan_officer_id, org_id, app_id, state)

            # audit_entries
            await conn.execute("""
                INSERT INTO audit_entries (org_id, entity_type, entity_id, action, user_id, user_role, notes)
                VALUES ($1, 'loan_application', $2, 'created', $3, 'loan_officer', 'Audit log for creation')
            """, org_id, app_id, loan_officer_id)

    print("Done!")
    await conn.close()

if __name__ == "__main__":
    asyncio.run(clear_and_populate())
