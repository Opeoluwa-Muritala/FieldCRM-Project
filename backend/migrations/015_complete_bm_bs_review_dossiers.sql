-- Complete the two canonical review fixtures used by the Branch Manager and
-- Branch Supervisor queues. Re-runnable and limited to the QA seed records.
-- OCR extraction remains automatic; it is not a workflow stage.

WITH intake_payload AS (
    SELECT jsonb_build_object(
        'applicant_name', la.applicant_name,
        'phone', la.phone,
        'bvn', la.bvn,
        'residential_address', '14 Unity Close, Ikeja, Lagos',
        'marital_status', 'married',
        'spouse_name', 'Demo Spouse',
        'spouse_phone', '08035550123',
        'spouse_signature', '/static/img/logo.png',
        'employment_type', 'self_employed',
        'business_name', 'Complete Demo Trading Enterprise',
        'business_address', '14 Unity Close, Ikeja, Lagos',
        'monthly_income', 850000,
        'existing_facilities', jsonb_build_array(jsonb_build_object(
            'institution', 'Mainstreet MFB', 'outstanding_balance', 0, 'status', 'cleared'
        )),
        'amount', la.amount,
        'tenor_months', la.tenor_months,
        'purpose', la.purpose,
        'repayment_mode', la.repayment_mode,
        'disbursement_bank', 'Mainstreet Microfinance Bank',
        'disbursement_account_name', la.applicant_name,
        'disbursement_account_number', '100000' || right(la.ref_no, 4),
        'pledge_legal_ack', '1',
        'borrower_pledge_signature', '/static/img/logo.png',
        'witness_pledge_signature', '/static/img/logo.png',
        'witness_name', 'Demo Independent Witness',
        'witness_address', '10 Allen Avenue, Ikeja, Lagos',
        'applicant_signature', '/static/img/logo.png',
        'consent_credit_bureau', '1',
        'consent_credit_check', '1',
        'consent_cheque', '1',
        'consent_gsi', '1',
        'consents', jsonb_build_object(
            'credit_bureau', 'true', 'credit_check', 'true',
            'cheque_authority', 'true', 'gsi_mandate', 'true'
        ),
        'guarantors_complete', true,
        'documents_complete', true,
        'ocr_extraction_status', 'processed_automatically',
        'seed', 'complete BM/BS review dossier',
        'verified', true
    ) AS data_json, la.id AS loan_id, la.created_by
    FROM loan_applications la
    WHERE la.ref_no IN ('MMFB-2026-QA-83', 'MMFB-2026-QA-84')
)
UPDATE stage_data sd
SET data_json = payload.data_json,
    saved_by = payload.created_by,
    saved_at = NOW() - INTERVAL '30 minutes'
FROM intake_payload payload
WHERE sd.loan_id = payload.loan_id AND sd.stage = 'intake';

WITH intake_payload AS (
    SELECT jsonb_build_object(
        'applicant_name', la.applicant_name, 'phone', la.phone, 'bvn', la.bvn,
        'residential_address', '14 Unity Close, Ikeja, Lagos',
        'spouse_name', 'Demo Spouse', 'spouse_signature', '/static/img/logo.png',
        'business_name', 'Complete Demo Trading Enterprise', 'monthly_income', 850000,
        'amount', la.amount, 'tenor_months', la.tenor_months, 'purpose', la.purpose,
        'repayment_mode', la.repayment_mode, 'applicant_signature', '/static/img/logo.png',
        'consents', jsonb_build_object('credit_bureau','true','credit_check','true','cheque_authority','true','gsi_mandate','true'),
        'guarantors_complete', true, 'documents_complete', true,
        'ocr_extraction_status', 'processed_automatically', 'seed', 'complete BM/BS review dossier', 'verified', true
    ) AS data_json, la.id AS loan_id, la.created_by
    FROM loan_applications la
    WHERE la.ref_no IN ('MMFB-2026-QA-83', 'MMFB-2026-QA-84')
)
INSERT INTO stage_data (loan_id, stage, data_json, saved_by, saved_at)
SELECT payload.loan_id, 'intake', payload.data_json, payload.created_by, NOW() - INTERVAL '30 minutes'
FROM intake_payload payload
WHERE NOT EXISTS (
    SELECT 1 FROM stage_data sd WHERE sd.loan_id = payload.loan_id AND sd.stage = 'intake'
);

-- The supervisor file has a completed Branch Manager review. The Branch
-- Manager file intentionally does not: it is waiting for that decision.
WITH supervisor AS (
    SELECT id, created_by FROM loan_applications WHERE ref_no = 'MMFB-2026-QA-84'
)
UPDATE stage_data sd
SET data_json = jsonb_build_object(
        'decision', 'approved_for_supervisory_review',
        'manager_notes', 'Intake, documents, guarantors, field visit and consents verified.',
        'readiness_verified', true,
        'verified', true,
        'seed', 'complete Branch Supervisor review dossier'
    ),
    saved_at = NOW() - INTERVAL '20 minutes'
FROM supervisor
WHERE sd.loan_id = supervisor.id AND sd.stage = 'branch_manager_review';

WITH supervisor AS (
    SELECT id, created_by FROM loan_applications WHERE ref_no = 'MMFB-2026-QA-84'
)
INSERT INTO stage_data (loan_id, stage, data_json, saved_by, saved_at)
SELECT id, 'branch_manager_review', jsonb_build_object(
    'decision', 'approved_for_supervisory_review',
    'manager_notes', 'Intake, documents, guarantors, field visit and consents verified.',
    'readiness_verified', true, 'verified', true,
    'seed', 'complete Branch Supervisor review dossier'
), created_by, NOW() - INTERVAL '20 minutes'
FROM supervisor
WHERE NOT EXISTS (
    SELECT 1 FROM stage_data sd WHERE sd.loan_id = supervisor.id AND sd.stage = 'branch_manager_review'
);

UPDATE loan_applications la
SET current_owner_id = CASE la.stage
        WHEN 'branch_manager_review' THEN '7532cddd-2417-45f6-94cd-48ce7a06fb37'::uuid
        WHEN 'branch_supervisor_review' THEN '11111111-1111-4111-8111-111111111111'::uuid
    END,
    updated_at = NOW()
WHERE la.ref_no IN ('MMFB-2026-QA-83', 'MMFB-2026-QA-84');

UPDATE documents d
SET quality_status = 'clear', verified = TRUE,
    verified_at = COALESCE(d.verified_at, NOW() - INTERVAL '1 hour')
FROM loan_applications la
WHERE d.loan_id = la.id
  AND la.ref_no IN ('MMFB-2026-QA-83', 'MMFB-2026-QA-84');
