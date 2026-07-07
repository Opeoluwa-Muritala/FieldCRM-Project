"""Seed script with real org/user IDs from the configured database."""
import asyncio
import sys
from pathlib import Path

import asyncpg

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from app.core.config import settings

DB = settings.DATABASE_URL

ORG  = '6975c129-45de-4492-8c77-9b5d7a413b42'
LO   = 'a5834126-3e41-4ed7-abd4-57e71cb26c83'
BM   = '9899c7c2-3278-4b18-b73d-4487eec61854'
CO   = '59a9f5af-fc88-4b29-a8cc-b2987cc6053c'   # aisha — for legacy audit rows
CRM  = '76d1d0d7-15e1-477e-b08a-3ce5e86ea7cf'
ED   = 'f7164464-d510-4694-b12f-81f66a0cf2f5'
MD   = '64b1c527-b414-434f-9664-2a3920e43e7a'
C1   = 'cc010001-0000-4000-8000-000000000001'
C2   = 'cc010002-0000-4000-8000-000000000002'
C3   = 'cc010003-0000-4000-8000-000000000003'
L21  = 'cc000000-0000-4000-8000-000000000021'
L22  = 'cc000000-0000-4000-8000-000000000022'
L23  = 'cc000000-0000-4000-8000-000000000023'
L24  = 'cc000000-0000-4000-8000-000000000024'
L25  = 'cc000000-0000-4000-8000-000000000025'
L26  = 'cc000000-0000-4000-8000-000000000026'
PH   = 'pbkdf2_sha256$260000$ZmllbGRjcm0tZGVtby1zYWx0$ditwuWjTVIp6hukjbeVVTR4M1YOImExIsrQd4OjY/aY='

STEPS = [

# 1. Committee users
f"""INSERT INTO users (id, org_id, full_name, email, password_hash, role) VALUES
('{C1}','{ORG}','Dr. Bola Adeyemi','bola@mmfb.com','{PH}','committee'),
('{C2}','{ORG}','Hajiya Zainab Musa','zainab@mmfb.com','{PH}','committee'),
('{C3}','{ORG}','Chief Emmanuel Obi','emmanuel@mmfb.com','{PH}','committee')
ON CONFLICT (org_id, email) DO NOTHING""",

# 2. Loans
f"""INSERT INTO loan_applications (
    id, org_id, ref_no, customer_type, loan_type, stage,
    applicant_name, bvn, phone, amount, tenor_months, purpose, repayment_mode,
    created_by, current_owner_id, branch_manager_id,
    crm_reviewed_by, crm_reviewed_at, crm_notes,
    committee_recommendation, committee_completed_at,
    ed_escalated_to_md, ed_approved_by, ed_approved_at,
    created_at, updated_at
) VALUES
('{L21}','{ORG}','MMFB-2026-01021','new','enterprise','crm_review',
 'Funmilayo Adekoya','22345678921','08031234521',4500000,24,'Business expansion','cheque',
 '{LO}','{CRM}','{BM}',NULL,NULL,NULL,NULL,NULL,FALSE,NULL,NULL,
 NOW()-INTERVAL '4 days',NOW()-INTERVAL '3 hours'),
('{L22}','{ORG}','MMFB-2026-01022','existing','msef','crm_review',
 'Haruna Yakubu','22345678922','08031234522',2800000,18,'Livestock trading','direct_debit',
 '{LO}','{CRM}','{BM}',NULL,NULL,NULL,NULL,NULL,FALSE,NULL,NULL,
 NOW()-INTERVAL '3 days',NOW()-INTERVAL '5 hours'),
('{L23}','{ORG}','MMFB-2026-01023','new','enterprise','committee_review',
 'Adaeze Nnaji','22345678923','08031234523',6000000,30,'Manufacturing upgrade','cheque',
 '{LO}','{C1}','{BM}',
 '{CRM}',NOW()-INTERVAL '1 day','Dossier complete. Strong business case.',
 NULL,NULL,FALSE,NULL,NULL,
 NOW()-INTERVAL '6 days',NOW()-INTERVAL '1 day'),
('{L24}','{ORG}','MMFB-2026-01024','existing','msef','committee_review',
 'Olumide Fashola','22345678924','08031234524',3200000,20,'Agro-processing expansion','direct_debit',
 '{LO}','{C1}','{BM}',
 '{CRM}',NOW()-INTERVAL '2 days','Verified docs. Recommend committee.',
 NULL,NULL,FALSE,NULL,NULL,
 NOW()-INTERVAL '5 days',NOW()-INTERVAL '2 days'),
('{L25}','{ORG}','MMFB-2026-01025','new','enterprise','ed_approval',
 'Chioma Obi','22345678925','08031234525',7500000,36,'Hotel renovation','cheque',
 '{LO}','{ED}','{BM}',
 '{CRM}',NOW()-INTERVAL '3 days','All pre-conditions satisfied.',
 'approve',NOW()-INTERVAL '12 hours',FALSE,NULL,NULL,
 NOW()-INTERVAL '8 days',NOW()-INTERVAL '12 hours'),
('{L26}','{ORG}','MMFB-2026-01026','existing','enterprise','md_approval',
 'Babatunde Martins','22345678926','08031234526',15000000,48,'Industrial facility','cheque',
 '{LO}','{MD}','{BM}',
 '{CRM}',NOW()-INTERVAL '7 days','Full dossier verified. Senior escalation needed.',
 'approve',NOW()-INTERVAL '2 days',TRUE,'{ED}',NOW()-INTERVAL '1 day',
 NOW()-INTERVAL '10 days',NOW()-INTERVAL '1 day')
ON CONFLICT (id) DO NOTHING""",

# 3. Stage data
f"""INSERT INTO stage_data (loan_id, stage, data_json, saved_by, saved_at)
SELECT id,'intake',
  jsonb_build_object('applicant_name',applicant_name,'phone',phone,
    'loan_amount',amount,'tenor_months',tenor_months,'loan_type',loan_type),
  created_by, created_at+INTERVAL '30 minutes'
FROM loan_applications
WHERE id IN ('{L21}','{L22}','{L23}','{L24}','{L25}','{L26}')
ON CONFLICT DO NOTHING""",

# 4. Guarantors
f"""INSERT INTO guarantors (loan_id, org_id, slot, full_name, relationship_to_client,
  bvn, phone, home_address, employment_type, monthly_salary, max_guarantee_amount,
  max_guarantee_amount_words, bank_name, account_number, cheque_number,
  form_stage, signature_detected, witness_signature_detected)
SELECT la.id, la.org_id, s.slot_no,
  CASE s.slot_no WHEN 1 THEN 'Primary Guarantor for '||la.applicant_name
                 ELSE 'Secondary Guarantor for '||la.applicant_name END,
  CASE s.slot_no WHEN 1 THEN 'Association Member' ELSE 'Family Friend' END,
  '333456'||right(la.ref_no,3)||s.slot_no::text,
  '0812'||right(la.ref_no,6)||s.slot_no::text,
  'Lagos business district',
  CASE WHEN la.loan_type='payee' THEN 'Full-time' ELSE 'Self-employed' END,
  250000, la.amount, 'Amount equal to facility',
  'Mainstreet MFB','10'||right(la.ref_no,6)||s.slot_no::text,
  'CHQ'||right(la.ref_no,6)||s.slot_no::text,
  'submitted',TRUE,FALSE
FROM loan_applications la
CROSS JOIN (VALUES (1),(2)) AS s(slot_no)
WHERE la.id IN ('{L21}','{L22}','{L23}','{L24}','{L25}','{L26}')
ON CONFLICT DO NOTHING""",

# 5. Documents
f"""INSERT INTO documents (loan_id, org_id, doc_type, original_name, stored_path,
  mime_type, size_bytes, quality_status, verified, verified_by, verified_at,
  uploaded_by, uploaded_at)
SELECT la.id, la.org_id, d.doc_type,
  la.ref_no||'_'||d.doc_type||'.pdf',
  '/static/uploads/demo/'||la.ref_no||'_'||d.doc_type||'.pdf',
  'application/pdf',204800,'clear',TRUE,
  '{CO}',la.created_at+INTERVAL '2 hours',
  la.created_by,la.created_at+INTERVAL '1 hour'
FROM loan_applications la
CROSS JOIN (VALUES ('loan_application_form'),('valid_id'),('bank_statement'),
                   ('guarantor_form'),('pledge_form')) AS d(doc_type)
WHERE la.id IN ('{L21}','{L22}','{L23}','{L24}','{L25}','{L26}')
ON CONFLICT DO NOTHING""",

# 6. Committee votes
f"""INSERT INTO committee_votes (loan_id, org_id, member_id, recommendation, notes, voted_at) VALUES
('{L23}','{ORG}','{C1}','approve','Strong financials',NOW()-INTERVAL '18 hours'),
('{L23}','{ORG}','{C2}','approve','Good track record',NOW()-INTERVAL '16 hours'),
('{L24}','{ORG}','{C1}','approve','Business viable',NOW()-INTERVAL '36 hours'),
('{L24}','{ORG}','{C2}','return','Needs more collateral',NOW()-INTERVAL '34 hours'),
('{L24}','{ORG}','{C3}','approve','Overall positive',NOW()-INTERVAL '32 hours'),
('{L25}','{ORG}','{C1}','approve','Solid repayment capacity',NOW()-INTERVAL '15 hours'),
('{L25}','{ORG}','{C2}','approve','Profitable business',NOW()-INTERVAL '14 hours'),
('{L25}','{ORG}','{C3}','approve','Cash flow adequate',NOW()-INTERVAL '13 hours'),
('{L26}','{ORG}','{C1}','approve','Well-secured',NOW()-INTERVAL '55 hours'),
('{L26}','{ORG}','{C2}','approve','Clean history',NOW()-INTERVAL '53 hours'),
('{L26}','{ORG}','{C3}','approve','Within risk appetite',NOW()-INTERVAL '51 hours')
ON CONFLICT (loan_id, member_id) DO NOTHING""",

# 7. Board referral
f"""INSERT INTO board_referrals (loan_id, org_id, referred_by, board_member_email,
  board_member_name, notes, status, created_at)
VALUES ('{L26}','{ORG}','{MD}','boardchair@mmfb.com','Board Chairman',
  'Please advise on this large facility','pending',NOW()-INTERVAL '20 hours')
ON CONFLICT DO NOTHING""",

# 8. Workflow events
f"""INSERT INTO workflow_events (loan_id, org_id, event_type, from_stage, to_stage,
  triggered_by, triggered_role, notes, created_at) VALUES
('{L21}','{ORG}','loan.created',NULL,'intake','{LO}','loan_officer','Application created',NOW()-INTERVAL '4 days'),
('{L21}','{ORG}','intake.submitted','intake','ocr_review','{LO}','loan_officer','Submitted',NOW()-INTERVAL '4 days'+INTERVAL '1 hour'),
('{L21}','{ORG}','branch.approved','branch_approval','crm_review','{BM}','branch_manager','Branch signed off',NOW()-INTERVAL '2 days'),
('{L22}','{ORG}','loan.created',NULL,'intake','{LO}','loan_officer','Application created',NOW()-INTERVAL '3 days'),
('{L22}','{ORG}','branch.approved','branch_approval','crm_review','{BM}','branch_manager','Branch signed off',NOW()-INTERVAL '1 day'),
('{L23}','{ORG}','loan.created',NULL,'intake','{LO}','loan_officer','Application created',NOW()-INTERVAL '6 days'),
('{L23}','{ORG}','branch.approved','branch_approval','crm_review','{BM}','branch_manager','Branch signed off',NOW()-INTERVAL '3 days'),
('{L23}','{ORG}','crm.reviewed','crm_review','committee_review','{CRM}','crm','Strong business case',NOW()-INTERVAL '1 day'),
('{L24}','{ORG}','loan.created',NULL,'intake','{LO}','loan_officer','Application created',NOW()-INTERVAL '5 days'),
('{L24}','{ORG}','branch.approved','branch_approval','crm_review','{BM}','branch_manager','Branch signed off',NOW()-INTERVAL '2 days'),
('{L24}','{ORG}','crm.reviewed','crm_review','committee_review','{CRM}','crm','Verified documents',NOW()-INTERVAL '2 days'),
('{L25}','{ORG}','loan.created',NULL,'intake','{LO}','loan_officer','Application created',NOW()-INTERVAL '8 days'),
('{L25}','{ORG}','branch.approved','branch_approval','crm_review','{BM}','branch_manager','Branch signed off',NOW()-INTERVAL '5 days'),
('{L25}','{ORG}','crm.reviewed','crm_review','committee_review','{CRM}','crm','All pre-conditions satisfied',NOW()-INTERVAL '3 days'),
('{L25}','{ORG}','committee.completed','committee_review','ed_approval','{C1}','committee','Unanimous approval',NOW()-INTERVAL '12 hours'),
('{L26}','{ORG}','loan.created',NULL,'intake','{LO}','loan_officer','Application created',NOW()-INTERVAL '10 days'),
('{L26}','{ORG}','branch.approved','branch_approval','crm_review','{BM}','branch_manager','Branch signed off',NOW()-INTERVAL '7 days'),
('{L26}','{ORG}','crm.reviewed','crm_review','committee_review','{CRM}','crm','Full dossier verified',NOW()-INTERVAL '7 days'+INTERVAL '8 hours'),
('{L26}','{ORG}','committee.completed','committee_review','ed_approval','{C1}','committee','Unanimous approval',NOW()-INTERVAL '2 days'),
('{L26}','{ORG}','ed.escalated_to_md','ed_approval','md_approval','{ED}','ed','Facility exceeds ED threshold',NOW()-INTERVAL '1 day')
ON CONFLICT DO NOTHING""",
]

async def main():
    conn = await asyncpg.connect(DB)
    try:
        for i, sql in enumerate(STEPS, 1):
            await conn.execute(sql)
            print(f"  Step {i}: OK")
        print("Seed complete.")
    except Exception as e:
        print(f"ERR at step {i}: {e}")
    finally:
        await conn.close()

asyncio.run(main())
