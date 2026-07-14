-- Adds a second complete dossier to every canonical approval queue.
-- Apply after 014 and 015. Safe to run repeatedly.

WITH targets(stage, ref_no, owner_id) AS (
    VALUES
      ('branch_manager_review', 'MMFB-2026-QA-183', '7532cddd-2417-45f6-94cd-48ce7a06fb37'::uuid),
      ('branch_supervisor_review', 'MMFB-2026-QA-184', '11111111-1111-4111-8111-111111111111'::uuid),
      ('credit_analyst_review', 'MMFB-2026-QA-185', '12d9b785-0143-49da-a927-dd0a6b0eba0d'::uuid),
      ('crm_review', 'MMFB-2026-QA-186', 'f30ec19c-5153-4656-9182-89fbef3b11a2'::uuid),
      ('head_crm_review', 'MMFB-2026-QA-187', 'c028f31a-6cf3-42ff-a52a-b09244f037ea'::uuid),
      ('audit_review', 'MMFB-2026-QA-188', '22222222-2222-4222-8222-222222222222'::uuid),
      ('ed_approval', 'MMFB-2026-QA-189', '8b2a737f-cbe8-4b2d-95eb-5c4cc1a9307f'::uuid),
      ('md_approval', 'MMFB-2026-QA-190', '83feff50-caea-440c-b2e9-48b93ed4d184'::uuid),
      ('disbursement_ready', 'MMFB-2026-QA-191', 'f30ec19c-5153-4656-9182-89fbef3b11a2'::uuid)
), source AS (
    SELECT DISTINCT ON (la.stage) la.*, t.ref_no AS clone_ref, t.owner_id
    FROM targets t JOIN loan_applications la ON la.stage = t.stage
    WHERE la.ref_no LIKE 'MMFB-2026-QA-%' AND la.ref_no NOT LIKE '%-1__'
    ORDER BY la.stage, la.created_at
)
INSERT INTO loan_applications (org_id, ref_no, customer_type, loan_type, stage, applicant_name, bvn, phone, amount, tenor_months, purpose, repayment_mode, created_by, current_owner_id, branch_manager_id, created_at, updated_at)
SELECT org_id, clone_ref, customer_type, loan_type, stage, applicant_name || ' II',
       bvn || '9', phone, amount, tenor_months, purpose, repayment_mode,
       created_by, owner_id, branch_manager_id, NOW() - INTERVAL '2 days', NOW()
FROM source
ON CONFLICT (org_id, ref_no) DO NOTHING;

-- Every seeded review dossier has a complete intake payload, including all
-- signed consents, pledge/witness signatures, guarantors and account details.
WITH complete_intake AS (
    SELECT la.id, la.created_by, jsonb_build_object(
        'applicant_name',la.applicant_name,'phone',la.phone,'bvn',la.bvn,
        'residential_address','14 Unity Close, Ikeja, Lagos','spouse_name','Demo Spouse','spouse_signature','/static/img/logo.png',
        'business_name','Complete Demo Trading Enterprise','monthly_income',850000,
        'amount',la.amount,'tenor_months',la.tenor_months,'purpose',la.purpose,'repayment_mode',la.repayment_mode,
        'disbursement_bank','Mainstreet Microfinance Bank','disbursement_account_number','100000' || right(la.ref_no,4),
        'borrower_pledge_signature','/static/img/logo.png','witness_pledge_signature','/static/img/logo.png','applicant_signature','/static/img/logo.png',
        'consents',jsonb_build_object('credit_bureau','true','credit_check','true','cheque_authority','true','gsi_mandate','true'),
        'guarantors_complete',true,'documents_complete',true,'ocr_extraction_status','processed_automatically','verified',true,'seed','complete approval dossier'
    ) payload
    FROM loan_applications la WHERE la.ref_no LIKE 'MMFB-2026-QA-%'
      AND la.stage IN ('branch_manager_review','branch_supervisor_review','credit_analyst_review','crm_review','head_crm_review','audit_review','ed_approval','md_approval','disbursement_ready')
)
UPDATE stage_data sd SET data_json = ci.payload, saved_by = ci.created_by, saved_at = NOW()
FROM complete_intake ci WHERE sd.loan_id = ci.id AND sd.stage = 'intake';

WITH complete_intake AS (
    SELECT la.id, la.created_by, jsonb_build_object('applicant_name',la.applicant_name,'phone',la.phone,'bvn',la.bvn,'amount',la.amount,'tenor_months',la.tenor_months,'applicant_signature','/static/img/logo.png','consents',jsonb_build_object('credit_bureau','true','credit_check','true','cheque_authority','true','gsi_mandate','true'),'guarantors_complete',true,'documents_complete',true,'ocr_extraction_status','processed_automatically','verified',true,'seed','complete approval dossier') payload
    FROM loan_applications la WHERE la.ref_no LIKE 'MMFB-2026-QA-%'
      AND la.stage IN ('branch_manager_review','branch_supervisor_review','credit_analyst_review','crm_review','head_crm_review','audit_review','ed_approval','md_approval','disbursement_ready')
)
INSERT INTO stage_data (loan_id,stage,data_json,saved_by,saved_at)
SELECT id,'intake',payload,created_by,NOW() FROM complete_intake ci
WHERE NOT EXISTS (SELECT 1 FROM stage_data sd WHERE sd.loan_id=ci.id AND sd.stage='intake');

INSERT INTO documents (loan_id,org_id,doc_type,form_code,original_name,stored_path,mime_type,size_bytes,quality_status,verified,verified_by,verified_at,uploaded_by,uploaded_at)
SELECT clone.id, clone.org_id, d.doc_type,d.form_code,d.original_name,d.stored_path,d.mime_type,d.size_bytes,'clear',true,d.verified_by,COALESCE(d.verified_at,NOW()),clone.created_by,NOW()
FROM loan_applications clone
JOIN loan_applications original ON original.stage=clone.stage AND original.ref_no LIKE 'MMFB-2026-QA-%' AND original.ref_no NOT LIKE '%-1__'
JOIN documents d ON d.loan_id=original.id
WHERE clone.ref_no LIKE 'MMFB-2026-QA-1__'
  AND NOT EXISTS (SELECT 1 FROM documents e WHERE e.loan_id=clone.id AND e.doc_type=d.doc_type);

INSERT INTO guarantors (loan_id,org_id,slot,full_name,relationship_to_client,bvn,phone,home_address,employment_type,monthly_salary,max_guarantee_amount,max_guarantee_amount_words,bank_name,account_number,cheque_number,form_stage,signature_detected,witness_signature_detected)
SELECT clone.id,clone.org_id,g.slot,g.full_name || ' II',g.relationship_to_client,g.bvn || '9',g.phone,g.home_address,g.employment_type,g.monthly_salary,g.max_guarantee_amount,g.max_guarantee_amount_words,g.bank_name,g.account_number,g.cheque_number,'verified',true,true
FROM loan_applications clone
JOIN loan_applications original ON original.stage=clone.stage AND original.ref_no LIKE 'MMFB-2026-QA-%' AND original.ref_no NOT LIKE '%-1__'
JOIN guarantors g ON g.loan_id=original.id
WHERE clone.ref_no LIKE 'MMFB-2026-QA-1__'
  AND NOT EXISTS (SELECT 1 FROM guarantors e WHERE e.loan_id=clone.id AND e.slot=g.slot);

INSERT INTO visitation_reports (loan_id,org_id,visit_date,met_with,premises_description,direction_from_branch,business_condition,visiting_officer_id,visiting_officer_signature,account_officer_id,manager_concurrence,manager_id,manager_notes,manager_concurred_at,status,created_at,updated_at)
SELECT clone.id,clone.org_id,v.visit_date,v.met_with,v.premises_description,v.direction_from_branch,v.business_condition,v.visiting_officer_id,true,v.account_officer_id,true,v.manager_id,v.manager_notes,NOW()-INTERVAL '1 hour','concurred',NOW()-INTERVAL '2 days',NOW()
FROM loan_applications clone
JOIN loan_applications original ON original.stage=clone.stage AND original.ref_no LIKE 'MMFB-2026-QA-%' AND original.ref_no NOT LIKE '%-1__'
JOIN visitation_reports v ON v.loan_id=original.id
WHERE clone.ref_no LIKE 'MMFB-2026-QA-1__'
  AND NOT EXISTS (SELECT 1 FROM visitation_reports e WHERE e.loan_id=clone.id);
