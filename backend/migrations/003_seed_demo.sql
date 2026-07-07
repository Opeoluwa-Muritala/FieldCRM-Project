-- Demo Seed Data for FieldCRM
-- Organisation: Mainstreet Microfinance Bank
-- 5 users with email-based login, password: password123

-- Organisation
INSERT INTO organisations (id, name, code, active)
VALUES (
    'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
    'Mainstreet Microfinance Bank',
    'MMFB',
    TRUE
);

-- Users (all with password: password123)
-- Hash format: pbkdf2_sha256$iterations$base64_salt$base64_hash
-- Hash: pbkdf2_sha256$260000$ZmllbGRjcm0tZGVtby1zYWx0$ditwuWjTVIp6hukjbeVVTR4M1YOImExIsrQd4OjY/aY=

INSERT INTO users (id, org_id, full_name, email, password_hash, role) VALUES
(
    'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a00',
    'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
    'MVP Admin',
    'admin@mmfb.com',
    'pbkdf2_sha256$260000$ZmllbGRjcm0tZGVtby1zYWx0$ditwuWjTVIp6hukjbeVVTR4M1YOImExIsrQd4OjY/aY=',
    'system_admin'
),
(
    'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a01',
    'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
    'Adebayo Johnson',
    'adebayo@mmfb.com',
    'pbkdf2_sha256$260000$ZmllbGRjcm0tZGVtby1zYWx0$ditwuWjTVIp6hukjbeVVTR4M1YOImExIsrQd4OjY/aY=',
    'branch_manager'
),
(
    'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a02',
    'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
    'Chidi Obi',
    'chidi@mmfb.com',
    'pbkdf2_sha256$260000$ZmllbGRjcm0tZGVtby1zYWx0$ditwuWjTVIp6hukjbeVVTR4M1YOImExIsrQd4OjY/aY=',
    'loan_officer'
),
(
    'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a03',
    'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
    'Fatima Bello',
    'fatima@mmfb.com',
    'pbkdf2_sha256$260000$ZmllbGRjcm0tZGVtby1zYWx0$ditwuWjTVIp6hukjbeVVTR4M1YOImExIsrQd4OjY/aY=',
    'loan_officer'
),
(
    'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a04',
    'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
    'Samuel Okafor',
    'samuel@mmfb.com',
    'pbkdf2_sha256$260000$ZmllbGRjcm0tZGVtby1zYWx0$ditwuWjTVIp6hukjbeVVTR4M1YOImExIsrQd4OjY/aY=',
    'auditor'
);

-- Loan applications: applicant/customer details live on loan_applications.
INSERT INTO loan_applications (
    id, org_id, ref_no, customer_type, loan_type, stage, applicant_name,
    bvn, phone, amount, tenor_months, purpose, repayment_mode, created_by,
    current_owner_id, credit_officer_id, branch_manager_id, return_reason,
    returned_at, approved_by, approved_at, disbursed_at, created_at, updated_at
) VALUES
('c0000000-0000-4000-8000-000000000001','a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11','MMFB-2026-01001','new','msef','intake','Grace Omowunmi','22345678901','08031234501',500000,10,'Market stock purchase','direct_debit','b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a02','b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a02',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NOW() - INTERVAL '9 days',NOW() - INTERVAL '2 hours'),
('c0000000-0000-4000-8000-000000000002','a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11','MMFB-2026-01002','existing','enterprise','intake','Ibrahim Musa','22345678902','08031234502',1200000,12,'Working capital expansion','cheque','b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a02','b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a02',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NOW() - INTERVAL '8 days',NOW() - INTERVAL '3 hours'),
('c0000000-0000-4000-8000-000000000003','a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11','MMFB-2026-01003','new','payee','intake','Chidi Okafor','22345678903','08031234503',300000,6,'Salary bridge facility','standing_order','b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a02','b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a02',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NOW() - INTERVAL '7 days',NOW() - INTERVAL '1 day'),
('c0000000-0000-4000-8000-000000000004','a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11','MMFB-2026-01004','existing','other','intake','Aisha Lawal','22345678904','08031234504',750000,9,'School fee business support','cash_deposit','b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a02','b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a02',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NOW() - INTERVAL '7 days',NOW() - INTERVAL '4 hours'),
('c0000000-0000-4000-8000-000000000005','a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11','MMFB-2026-01005','new','msef','ocr_review','Tunde Balogun','22345678905','08031234505',650000,8,'Inventory replenishment','direct_debit','b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a02','b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a02',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NOW() - INTERVAL '6 days',NOW() - INTERVAL '5 hours'),
('c0000000-0000-4000-8000-000000000006','a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11','MMFB-2026-01006','existing','enterprise','ocr_review','Ngozi Eze','22345678906','08031234506',2000000,18,'Bakery equipment purchase','cheque','b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a02','b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a02',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NOW() - INTERVAL '6 days',NOW() - INTERVAL '6 hours'),
('c0000000-0000-4000-8000-000000000007','a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11','MMFB-2026-01007','new','payee','ocr_review','Kunle Adeyemi','22345678907','08031234507',450000,10,'Personal emergency facility','standing_order','b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a02','b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a02',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NOW() - INTERVAL '5 days',NOW() - INTERVAL '8 hours'),
('c0000000-0000-4000-8000-000000000008','a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11','MMFB-2026-01008','existing','msef','credit_review','Maryam Sani','22345678908','08031234508',900000,12,'Provision store expansion','direct_debit','b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a02','b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a03','b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a03',NULL,NULL,NULL,NULL,NULL,NULL,NOW() - INTERVAL '5 days',NOW() - INTERVAL '1 hour'),
('c0000000-0000-4000-8000-000000000009','a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11','MMFB-2026-01009','new','enterprise','credit_review','Peter Nwosu','22345678909','08031234509',3500000,24,'Cold room installation','cheque','b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a02','b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a03','b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a03',NULL,NULL,NULL,NULL,NULL,NULL,NOW() - INTERVAL '5 days',NOW() - INTERVAL '3 hours'),
('c0000000-0000-4000-8000-000000000010','a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11','MMFB-2026-01010','existing','other','credit_review','Blessing Udo','22345678910','08031234510',800000,9,'Agric trading cycle','cash_deposit','b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a02','b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a03','b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a03',NULL,NULL,NULL,NULL,NULL,NULL,NOW() - INTERVAL '4 days',NOW() - INTERVAL '7 hours'),
('c0000000-0000-4000-8000-000000000011','a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11','MMFB-2026-01011','new','payee','credit_review','Victor Essien','22345678911','08031234511',550000,11,'Payroll backed facility','standing_order','b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a02','b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a03','b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a03',NULL,NULL,NULL,NULL,NULL,NULL,NOW() - INTERVAL '4 days',NOW() - INTERVAL '9 hours'),
('c0000000-0000-4000-8000-000000000012','a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11','MMFB-2026-01012','existing','msef','branch_approval','Halima Yusuf','22345678912','08031234512',1100000,14,'Textile stock expansion','direct_debit','b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a02','b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a01','b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a03','b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a01',NULL,NULL,NULL,NULL,NULL,NOW() - INTERVAL '3 days',NOW() - INTERVAL '30 minutes'),
('c0000000-0000-4000-8000-000000000013','a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11','MMFB-2026-01013','new','enterprise','branch_approval','Emeka Anozie','22345678913','08031234513',4500000,30,'Manufacturing inputs','cheque','b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a02','b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a01','b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a03','b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a01',NULL,NULL,NULL,NULL,NULL,NOW() - INTERVAL '3 days',NOW() - INTERVAL '2 hours'),
('c0000000-0000-4000-8000-000000000014','a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11','MMFB-2026-01014','existing','msef','branch_approval','Rasheed Bello','22345678914','08031234514',700000,8,'Phone accessories stock','cash_deposit','b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a02','b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a01','b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a03','b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a01',NULL,NULL,NULL,NULL,NULL,NOW() - INTERVAL '3 days',NOW() - INTERVAL '4 hours'),
('c0000000-0000-4000-8000-000000000015','a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11','MMFB-2026-01015','new','payee','disbursement_ready','Janet Akpan','22345678915','08031234515',400000,10,'Medical expense support','standing_order','b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a02','b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a00','b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a03','b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a01',NULL,NULL,'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a01',NOW() - INTERVAL '1 day',NULL,NOW() - INTERVAL '2 days',NOW() - INTERVAL '20 minutes'),
('c0000000-0000-4000-8000-000000000016','a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11','MMFB-2026-01016','existing','enterprise','disbursement_ready','Sola Martins','22345678916','08031234516',2800000,20,'Restaurant renovation','direct_debit','b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a02','b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a00','b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a03','b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a01',NULL,NULL,'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a01',NOW() - INTERVAL '1 day',NULL,NOW() - INTERVAL '2 days',NOW() - INTERVAL '1 hour'),
('c0000000-0000-4000-8000-000000000017','a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11','MMFB-2026-01017','existing','msef','disbursed','Kemi Olatunji','22345678917','08031234517',600000,12,'Cosmetics wholesale','cash_deposit','b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a02','b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a02','b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a03','b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a01',NULL,NULL,'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a01',NOW() - INTERVAL '5 days',NOW() - INTERVAL '4 days',NOW() - INTERVAL '10 days',NOW() - INTERVAL '4 days'),
('c0000000-0000-4000-8000-000000000018','a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11','MMFB-2026-01018','new','enterprise','disbursed','Daniel Ojo','22345678918','08031234518',5000000,36,'Logistics vehicle purchase','cheque','b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a02','b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a02','b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a03','b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a01',NULL,NULL,'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a01',NOW() - INTERVAL '8 days',NOW() - INTERVAL '7 days',NOW() - INTERVAL '15 days',NOW() - INTERVAL '7 days'),
('c0000000-0000-4000-8000-000000000019','a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11','MMFB-2026-01019','existing','msef','returned','Bola Akin','22345678919','08031234519',850000,12,'Trading capital','direct_debit','b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a02','b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a02','b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a03','b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a01','Missing signed guarantor page',NOW() - INTERVAL '12 hours',NULL,NULL,NULL,NOW() - INTERVAL '2 days',NOW() - INTERVAL '12 hours'),
('c0000000-0000-4000-8000-000000000020','a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11','MMFB-2026-01020','new','other','rejected','Uche Nnamdi','22345678920','08031234520',950000,10,'Import trading support','cheque','b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a02','b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a03','b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a03','b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a01','Adverse credit bureau result',NOW() - INTERVAL '1 day',NULL,NULL,NULL,NOW() - INTERVAL '3 days',NOW() - INTERVAL '1 day');

INSERT INTO stage_data (loan_id, stage, data_json, saved_by, saved_at)
SELECT
    id,
    'intake',
    jsonb_build_object(
        'applicant_name', applicant_name,
        'phone', phone,
        'bvn', bvn,
        'loan_amount', amount,
        'tenor_months', tenor_months,
        'loan_type', loan_type,
        'repayment_mode', repayment_mode,
        'marital_status', CASE WHEN right(ref_no, 1) IN ('1','4','7','0') THEN 'Married' ELSE 'Single' END,
        'employment_type', CASE WHEN loan_type = 'payee' THEN 'Full-time' ELSE 'Self-employed' END,
        'consents', jsonb_build_object(
            'credit_bureau', stage NOT IN ('intake','returned'),
            'credit_check', stage NOT IN ('intake','returned'),
            'gsi_mandate', stage NOT IN ('intake','returned'),
            'cheque_authority', repayment_mode = 'cheque' OR stage NOT IN ('intake','returned')
        )
    ),
    created_by,
    updated_at
FROM loan_applications
WHERE org_id = 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11';

INSERT INTO guarantors (
    loan_id, org_id, slot, full_name, relationship_to_client, bvn, phone,
    home_address, employment_type, monthly_salary, max_guarantee_amount,
    max_guarantee_amount_words, bank_name, account_number, cheque_number,
    form_stage, signature_detected, witness_signature_detected
)
SELECT
    la.id,
    la.org_id,
    slot_no,
    CASE slot_no WHEN 1 THEN 'Primary Guarantor for ' ELSE 'Secondary Guarantor for ' END || la.applicant_name,
    CASE slot_no WHEN 1 THEN 'Trader Association Member' ELSE 'Family Friend' END,
    '33345678' || lpad((row_number() OVER (ORDER BY la.ref_no, slot_no))::text, 3, '0'),
    CASE slot_no WHEN 1 THEN '0812456' ELSE '0819876' END || lpad((row_number() OVER (ORDER BY la.ref_no, slot_no))::text, 4, '0'),
    'Lagos business district',
    CASE WHEN la.loan_type = 'payee' THEN 'Full-time' ELSE 'Self-employed' END,
    250000,
    la.amount,
    'Amount equal to requested facility',
    'Mainstreet MFB',
    '10' || lpad((row_number() OVER (ORDER BY la.ref_no, slot_no))::text, 8, '0'),
    'CHQ' || lpad((row_number() OVER (ORDER BY la.ref_no, slot_no))::text, 6, '0'),
    CASE
        WHEN la.stage IN ('branch_approval','disbursement_ready','disbursed') THEN 'verified'
        WHEN la.stage = 'returned' THEN 'returned'
        ELSE 'submitted'
    END,
    la.stage <> 'intake',
    la.stage IN ('branch_approval','disbursement_ready','disbursed')
FROM loan_applications la
CROSS JOIN (VALUES (1), (2)) AS slots(slot_no)
WHERE la.org_id = 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11';

INSERT INTO documents (
    loan_id, org_id, doc_type, form_code, original_name, stored_path,
    mime_type, size_bytes, quality_status, verified, verified_by, verified_at,
    uploaded_by, uploaded_at
)
SELECT
    la.id,
    la.org_id,
    doc_type,
    CASE doc_type
        WHEN 'loan_application_form' THEN 'MMFB/CRM/01'
        WHEN 'pledge_form' THEN 'MMFB/CRM/02'
        WHEN 'guarantor_form' THEN 'MMFB/CRM/03'
        ELSE NULL
    END,
    la.ref_no || '_' || doc_type || '.pdf',
    '/static/uploads/demo/' || la.ref_no || '_' || doc_type || '.pdf',
    'application/pdf',
    204800,
    CASE WHEN la.stage = 'ocr_review' AND doc_type = 'loan_application_form' THEN 'blurry' ELSE 'clear' END,
    CASE WHEN la.stage IN ('intake','ocr_review','returned') AND doc_type IN ('bank_statement','guarantor_form') THEN FALSE ELSE TRUE END,
    CASE WHEN la.stage IN ('intake','ocr_review','returned') AND doc_type IN ('bank_statement','guarantor_form') THEN NULL ELSE 'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a03'::uuid END,
    CASE WHEN la.stage IN ('intake','ocr_review','returned') AND doc_type IN ('bank_statement','guarantor_form') THEN NULL ELSE NOW() - INTERVAL '1 day' END,
    la.created_by,
    la.updated_at - INTERVAL '1 day'
FROM loan_applications la
CROSS JOIN (VALUES
    ('loan_application_form'),
    ('valid_id'),
    ('bank_statement'),
    ('guarantor_form'),
    ('pledge_form')
) AS required_docs(doc_type)
WHERE la.org_id = 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11'
  AND (la.stage <> 'intake' OR doc_type IN ('valid_id','bank_statement'));

INSERT INTO ocr_results (document_id, loan_id, form_type, overall_confidence, raw_extraction)
SELECT
    d.id,
    d.loan_id,
    CASE d.doc_type
        WHEN 'guarantor_form' THEN 'guarantor'
        WHEN 'pledge_form' THEN 'pledge_receipt'
        ELSE 'loan_application'
    END,
    CASE WHEN la.stage = 'ocr_review' THEN 62.00 ELSE 88.00 END,
    jsonb_build_object('source', 'demo_seed', 'document', d.doc_type)
FROM documents d
JOIN loan_applications la ON la.id = d.loan_id
WHERE d.doc_type IN ('loan_application_form','guarantor_form','pledge_form')
  AND la.stage IN ('ocr_review','credit_review','branch_approval','disbursement_ready','disbursed','returned','rejected');

INSERT INTO ocr_fields (
    ocr_result_id, loan_id, field_name, ocr_value, corrected_value,
    confidence, source, is_critical, verified, verified_by, verified_at
)
SELECT
    r.id,
    r.loan_id,
    field_name,
    ocr_value,
    corrected_value,
    confidence,
    CASE WHEN corrected_value IS NULL THEN 'ocr' ELSE 'corrected' END,
    is_critical,
    verified,
    CASE WHEN verified THEN 'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a03'::uuid ELSE NULL END,
    CASE WHEN verified THEN NOW() - INTERVAL '6 hours' ELSE NULL END
FROM ocr_results r
JOIN loan_applications la ON la.id = r.loan_id
CROSS JOIN LATERAL (
    VALUES
        ('applicant_name', la.applicant_name, NULL, 92.00, FALSE, la.stage NOT IN ('ocr_review','returned')),
        ('bvn', la.bvn, NULL, CASE WHEN la.stage = 'ocr_review' THEN 58.00 ELSE 84.00 END, TRUE, la.stage NOT IN ('ocr_review','returned')),
        ('loan_amount', la.amount::text, CASE WHEN la.stage = 'credit_review' THEN la.amount::text ELSE NULL END, CASE WHEN la.stage = 'ocr_review' THEN 64.00 ELSE 90.00 END, TRUE, la.stage NOT IN ('ocr_review','returned')),
        ('signature', 'detected', NULL, CASE WHEN la.stage = 'returned' THEN 35.00 ELSE 78.00 END, TRUE, la.stage NOT IN ('ocr_review','returned'))
) AS fields(field_name, ocr_value, corrected_value, confidence, is_critical, verified);

INSERT INTO visitation_reports (
    loan_id, org_id, visit_date, met_with, premises_description,
    direction_from_branch, business_condition, visiting_officer_id,
    visiting_officer_signature, account_officer_id, manager_concurrence,
    manager_id, manager_notes, manager_concurred_at, status, created_at, updated_at
)
SELECT
    id,
    org_id,
    CURRENT_DATE - 2,
    applicant_name,
    'Business premises verified with visible trading activity and stock records.',
    'From branch office, proceed to market entrance and ask for applicant shop line.',
    'Stable business activity observed.',
    created_by,
    TRUE,
    created_by,
    stage IN ('branch_approval','disbursement_ready','disbursed'),
    CASE WHEN stage IN ('branch_approval','disbursement_ready','disbursed') THEN branch_manager_id ELSE NULL END,
    CASE WHEN stage IN ('branch_approval','disbursement_ready','disbursed') THEN 'Concurrence granted from seeded review.' ELSE NULL END,
    CASE WHEN stage IN ('branch_approval','disbursement_ready','disbursed') THEN NOW() - INTERVAL '1 day' ELSE NULL END,
    CASE WHEN stage IN ('branch_approval','disbursement_ready','disbursed') THEN 'concurred' ELSE 'submitted' END,
    updated_at - INTERVAL '2 days',
    updated_at
FROM loan_applications
WHERE stage IN ('credit_review','branch_approval','disbursement_ready','disbursed','returned','rejected');

INSERT INTO workflow_events (
    loan_id, org_id, event_type, from_stage, to_stage, triggered_by, triggered_role, notes, created_at
)
SELECT id, org_id, 'loan.created', NULL, 'intake', created_by, 'loan_officer', 'Seeded application created', created_at
FROM loan_applications
UNION ALL
SELECT id, org_id, 'intake.submitted', 'intake', 'ocr_review', created_by, 'loan_officer', 'Submitted for OCR review', created_at + INTERVAL '1 hour'
FROM loan_applications WHERE stage NOT IN ('intake')
UNION ALL
SELECT id, org_id, 'ocr.verified', 'ocr_review', 'credit_review', credit_officer_id, 'credit_officer', 'OCR exceptions reviewed', created_at + INTERVAL '2 hours'
FROM loan_applications WHERE stage IN ('credit_review','branch_approval','disbursement_ready','disbursed','returned','rejected')
UNION ALL
SELECT id, org_id, 'credit_review.completed', 'credit_review', 'branch_approval', credit_officer_id, 'credit_officer', 'Credit review completed', created_at + INTERVAL '3 hours'
FROM loan_applications WHERE stage IN ('branch_approval','disbursement_ready','disbursed')
UNION ALL
SELECT id, org_id, 'branch.approved', 'branch_approval', 'disbursement_ready', branch_manager_id, 'branch_manager', 'Branch approval completed', created_at + INTERVAL '4 hours'
FROM loan_applications WHERE stage IN ('disbursement_ready','disbursed')
UNION ALL
SELECT id, org_id, 'loan.disbursed', 'disbursement_ready', 'disbursed', branch_manager_id, 'branch_manager', 'Facility disbursed', disbursed_at
FROM loan_applications WHERE stage = 'disbursed'
UNION ALL
SELECT id, org_id, 'loan.returned', 'credit_review', 'returned', credit_officer_id, 'credit_officer', return_reason, returned_at
FROM loan_applications WHERE stage = 'returned'
UNION ALL
SELECT id, org_id, 'loan.rejected', 'credit_review', 'rejected', credit_officer_id, 'credit_officer', return_reason, returned_at
FROM loan_applications WHERE stage = 'rejected';

INSERT INTO audit_entries (
    org_id, entity_type, entity_id, action, user_id, user_role,
    field_name, old_value, new_value, source, notes, created_at
)
SELECT
    org_id,
    'loan_application',
    loan_id,
    event_type,
    triggered_by,
    triggered_role,
    'stage',
    from_stage,
    to_stage,
    'manual',
    notes,
    created_at
FROM workflow_events
WHERE org_id = 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11';
