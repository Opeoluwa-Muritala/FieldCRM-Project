-- Development-only review queue seed for the canonical FieldCRM workflow.
-- Creates one complete demo dossier for every active review/disbursement stage.
-- Re-runnable: all rows use stable reference numbers and conflict guards.

INSERT INTO users (id, org_id, full_name, email, password_hash, role)
SELECT '11111111-1111-4111-8111-111111111111', org_id, 'Demo Branch Supervisor',
       'demo.branch.supervisor@mainstreetmfb.test', password_hash, 'branch_supervisor'
FROM users WHERE id = '06c53b4e-d9d8-47c2-8efb-8e637fe935c7'
ON CONFLICT (org_id, email) DO NOTHING;

INSERT INTO users (id, org_id, full_name, email, password_hash, role)
SELECT '22222222-2222-4222-8222-222222222222', org_id, 'Demo Auditor',
       'demo.auditor@mainstreetmfb.test', password_hash, 'auditor'
FROM users WHERE id = '06c53b4e-d9d8-47c2-8efb-8e637fe935c7'
ON CONFLICT (org_id, email) DO NOTHING;

WITH stages(stage, sequence, owner_id, ref_suffix) AS (
    VALUES
      ('intake', 1, '06c53b4e-d9d8-47c2-8efb-8e637fe935c7', '81'),
      ('ocr_review', 2, '06c53b4e-d9d8-47c2-8efb-8e637fe935c7', '82'),
      ('branch_manager_review', 3, '7532cddd-2417-45f6-94cd-48ce7a06fb37', '83'),
      ('branch_supervisor_review', 4, '11111111-1111-4111-8111-111111111111', '84'),
      ('credit_analyst_review', 5, '12d9b785-0143-49da-a927-dd0a6b0eba0d', '85'),
      ('crm_review', 6, 'f30ec19c-5153-4656-9182-89fbef3b11a2', '86'),
      ('head_crm_review', 7, 'c028f31a-6cf3-42ff-a52a-b09244f037ea', '87'),
      ('audit_review', 8, '22222222-2222-4222-8222-222222222222', '88'),
      ('ed_approval', 9, '8b2a737f-cbe8-4b2d-95eb-5c4cc1a9307f', '89'),
      ('md_approval', 10, '83feff50-caea-440c-b2e9-48b93ed4d184', '90'),
      ('disbursement_ready', 11, 'f30ec19c-5153-4656-9182-89fbef3b11a2', '91'),
      ('disbursed', 12, 'f30ec19c-5153-4656-9182-89fbef3b11a2', '92')
)
INSERT INTO loan_applications (
    org_id, ref_no, customer_type, loan_type, stage, applicant_name, bvn, phone,
    amount, tenor_months, purpose, repayment_mode, created_by, current_owner_id,
    branch_manager_id, approved_by, approved_at, disbursed_at, created_at, updated_at
)
SELECT ao.org_id, 'MMFB-2026-QA-' || s.ref_suffix, 'existing', 'enterprise', s.stage,
       'Review Dossier ' || s.ref_suffix || ' (Demo)', '2234567' || lpad(s.ref_suffix, 4, '0'),
       '0803123' || lpad(s.ref_suffix, 4, '0'), 5000000 + (s.sequence * 250000), 24,
       'Complete development seed for ' || replace(s.stage, '_', ' '), 'direct_debit',
       ao.id, s.owner_id::uuid, '7532cddd-2417-45f6-94cd-48ce7a06fb37',
       CASE WHEN s.sequence >= 11 THEN '83feff50-caea-440c-b2e9-48b93ed4d184'::uuid END,
       CASE WHEN s.sequence >= 11 THEN NOW() - INTERVAL '2 days' END,
       CASE WHEN s.stage = 'disbursed' THEN NOW() - INTERVAL '1 day' END,
       NOW() - (s.sequence + 3) * INTERVAL '1 day', NOW() - s.sequence * INTERVAL '1 hour'
FROM stages s
JOIN users ao ON ao.id = '06c53b4e-d9d8-47c2-8efb-8e637fe935c7'
ON CONFLICT (org_id, ref_no) DO NOTHING;

WITH docs(doc_type, form_code, original_name, stored_path, mime_type, size_bytes) AS (
    VALUES
      ('loan_application_form','MMFB/CRM/01','MMFB-2026-0101_loan_application_form.pdf','/static/uploads/demo/generated/MMFB-2026-0101_loan_application_form.pdf','application/pdf',1997),
      ('pledge_form','MMFB/CRM/02','MMFB-2026-0101_pledge_form.pdf','/static/uploads/demo/generated/MMFB-2026-0101_pledge_form.pdf','application/pdf',1977),
      ('guarantor_form','MMFB/CRM/03','MMFB-2026-0101_guarantor_form.pdf','/static/uploads/demo/generated/MMFB-2026-0101_guarantor_form.pdf','application/pdf',1983),
      ('valid_id',NULL,'MMFB-2026-0101_valid_id.pdf','/static/uploads/demo/generated/MMFB-2026-0101_valid_id.pdf','application/pdf',1969),
      ('bank_statement',NULL,'MMFB-2026-0101_bank_statement.pdf','/static/uploads/demo/generated/MMFB-2026-0101_bank_statement.pdf','application/pdf',1983),
      ('utility_bill',NULL,'MMFB-2026-0101_utility_bill.pdf','/static/uploads/demo/generated/MMFB-2026-0101_utility_bill.pdf','application/pdf',1979),
      ('disbursement_memo',NULL,'MMFB-2026-0101_disbursement_memo.pdf','/static/uploads/demo/generated/MMFB-2026-0101_disbursement_memo.pdf','application/pdf',1990),
      ('borrower_photo',NULL,'demo_borrower_photo.png','/static/img/logo.png','image/png',67566),
      ('signature_image',NULL,'demo_signature.png','/static/img/logo.png','image/png',67566)
)
INSERT INTO documents (loan_id, org_id, doc_type, form_code, original_name, stored_path, mime_type, size_bytes, quality_status, verified, verified_by, verified_at, uploaded_by, uploaded_at)
SELECT la.id, la.org_id, d.doc_type, d.form_code, d.original_name, d.stored_path, d.mime_type, d.size_bytes,
       'clear', TRUE, '12d9b785-0143-49da-a927-dd0a6b0eba0d', NOW() - INTERVAL '1 day', la.created_by, la.created_at
FROM loan_applications la CROSS JOIN docs d
WHERE la.ref_no LIKE 'MMFB-2026-QA-%'
  AND NOT EXISTS (SELECT 1 FROM documents existing WHERE existing.loan_id = la.id AND existing.doc_type = d.doc_type);

INSERT INTO guarantors (loan_id, org_id, slot, full_name, relationship_to_client, bvn, phone, home_address, employment_type, monthly_salary, max_guarantee_amount, max_guarantee_amount_words, bank_name, account_number, cheque_number, form_stage, signature_detected, witness_signature_detected)
SELECT la.id, la.org_id, slots.slot, 'Demo Guarantor ' || slots.slot || ' for ' || la.ref_no,
       'Business associate', '3334567' || right(la.ref_no, 4) || slots.slot,
       '0812456' || right(la.ref_no, 4), 'Lagos, Nigeria', 'Self-employed', 350000, la.amount,
       'Guaranteed amount equals requested facility', 'Mainstreet MFB', '1000' || right(la.ref_no, 6) || slots.slot,
       'CHQ' || right(la.ref_no, 4) || slots.slot, 'verified', TRUE, TRUE
FROM loan_applications la CROSS JOIN (VALUES (1),(2)) AS slots(slot)
WHERE la.ref_no LIKE 'MMFB-2026-QA-%'
ON CONFLICT (loan_id, slot) DO NOTHING;

INSERT INTO visitation_reports (loan_id, org_id, visit_date, met_with, premises_description, direction_from_branch, business_condition, visiting_officer_id, visiting_officer_signature, account_officer_id, manager_concurrence, manager_id, manager_notes, manager_concurred_at, status, created_at, updated_at)
SELECT la.id, la.org_id, CURRENT_DATE - 7, la.applicant_name, 'Verified operating business premises with stock and records.',
       'From branch to the recorded business address.', 'Stable operational conditions.', la.created_by, TRUE, la.created_by,
       TRUE, '7532cddd-2417-45f6-94cd-48ce7a06fb37', 'Concurred for demo review dossier.', NOW() - INTERVAL '6 days', 'concurred', la.created_at, NOW() - INTERVAL '6 days'
FROM loan_applications la
WHERE la.ref_no LIKE 'MMFB-2026-QA-%'
ON CONFLICT (loan_id) DO NOTHING;

WITH ordered(stage, sequence) AS (
    VALUES ('intake',1),('ocr_review',2),('branch_manager_review',3),('branch_supervisor_review',4),
           ('credit_analyst_review',5),('crm_review',6),('head_crm_review',7),('audit_review',8),
           ('ed_approval',9),('md_approval',10),('disbursement_ready',11),('disbursed',12)
)
INSERT INTO stage_data (loan_id, stage, data_json, saved_by, saved_at)
SELECT la.id, prior.stage, jsonb_build_object('seed','complete evidence set','stage',prior.stage,'verified',TRUE),
       la.created_by, la.created_at + prior.sequence * INTERVAL '2 hours'
FROM loan_applications la
JOIN ordered current ON current.stage = la.stage
JOIN ordered prior ON prior.sequence <= current.sequence
WHERE la.ref_no LIKE 'MMFB-2026-QA-%'
  AND NOT EXISTS (SELECT 1 FROM stage_data existing WHERE existing.loan_id = la.id AND existing.stage = prior.stage);

WITH ordered(stage, sequence, role) AS (
    VALUES ('intake',1,'account_officer'),('ocr_review',2,'account_officer'),('branch_manager_review',3,'branch_manager'),
           ('branch_supervisor_review',4,'branch_supervisor'),('credit_analyst_review',5,'credit_analyst'),('crm_review',6,'crm'),
           ('head_crm_review',7,'head_crm'),('audit_review',8,'auditor'),('ed_approval',9,'ed'),('md_approval',10,'md'),
           ('disbursement_ready',11,'crm'),('disbursed',12,'crm')
), role_users(role, user_id) AS (
    VALUES ('account_officer','06c53b4e-d9d8-47c2-8efb-8e637fe935c7'),('branch_manager','7532cddd-2417-45f6-94cd-48ce7a06fb37'),
           ('branch_supervisor','11111111-1111-4111-8111-111111111111'),('credit_analyst','12d9b785-0143-49da-a927-dd0a6b0eba0d'),
           ('crm','f30ec19c-5153-4656-9182-89fbef3b11a2'),('head_crm','c028f31a-6cf3-42ff-a52a-b09244f037ea'),
           ('auditor','22222222-2222-4222-8222-222222222222'),('ed','8b2a737f-cbe8-4b2d-95eb-5c4cc1a9307f'),('md','83feff50-caea-440c-b2e9-48b93ed4d184')
)
INSERT INTO workflow_events (loan_id, org_id, event_type, from_stage, to_stage, triggered_by, triggered_role, notes, created_at)
SELECT la.id, la.org_id, CASE WHEN step.sequence = 1 THEN 'loan.created' ELSE 'review.completed' END,
       previous.stage, step.stage, actor.user_id::uuid, step.role,
       'Complete demo dossier advanced to ' || replace(step.stage, '_', ' ') || '.', la.created_at + step.sequence * INTERVAL '3 hours'
FROM loan_applications la
JOIN ordered current ON current.stage = la.stage
JOIN ordered step ON step.sequence <= current.sequence
LEFT JOIN ordered previous ON previous.sequence = step.sequence - 1
JOIN role_users actor ON actor.role = step.role
WHERE la.ref_no LIKE 'MMFB-2026-QA-%'
  AND NOT EXISTS (SELECT 1 FROM workflow_events existing WHERE existing.loan_id = la.id AND existing.to_stage = step.stage);
