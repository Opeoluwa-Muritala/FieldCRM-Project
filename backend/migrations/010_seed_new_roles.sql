-- Migration 010: Seed demo users and loan data for new workflow roles
-- Adds committee, ed, md, crm users and loan applications spanning
-- crm_review, committee_review, ed_approval, and md_approval stages.
-- Run after 009_committee_workflow.sql

-- =============================================================
-- 1. NEW USERS (crm, ed, md, committee x3)
-- =============================================================
INSERT INTO users (id, org_id, full_name, email, password_hash, role) VALUES
(
    'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a05',
    'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
    'Ngozi Ike',
    'ngozi@mmfb.com',
    'pbkdf2_sha256$260000$ZmllbGRjcm0tZGVtby1zYWx0$ditwuWjTVIp6hukjbeVVTR4M1YOImExIsrQd4OjY/aY=',
    'crm'
),
(
    'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a06',
    'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
    'Amina Dikko',
    'amina@mmfb.com',
    'pbkdf2_sha256$260000$ZmllbGRjcm0tZGVtby1zYWx0$ditwuWjTVIp6hukjbeVVTR4M1YOImExIsrQd4OjY/aY=',
    'ed'
),
(
    'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a07',
    'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
    'Emeka Okonkwo',
    'emeka@mmfb.com',
    'pbkdf2_sha256$260000$ZmllbGRjcm0tZGVtby1zYWx0$ditwuWjTVIp6hukjbeVVTR4M1YOImExIsrQd4OjY/aY=',
    'md'
),
(
    'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a08',
    'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
    'Dr. Bola Adeyemi',
    'bola@mmfb.com',
    'pbkdf2_sha256$260000$ZmllbGRjcm0tZGVtby1zYWx0$ditwuWjTVIp6hukjbeVVTR4M1YOImExIsrQd4OjY/aY=',
    'committee'
),
(
    'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a09',
    'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
    'Hajiya Zainab Musa',
    'zainab@mmfb.com',
    'pbkdf2_sha256$260000$ZmllbGRjcm0tZGVtby1zYWx0$ditwuWjTVIp6hukjbeVVTR4M1YOImExIsrQd4OjY/aY=',
    'committee'
),
(
    'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a10',
    'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
    'Chief Emmanuel Obi',
    'emmanuel@mmfb.com',
    'pbkdf2_sha256$260000$ZmllbGRjcm0tZGVtby1zYWx0$ditwuWjTVIp6hukjbeVVTR4M1YOImExIsrQd4OjY/aY=',
    'committee'
)
ON CONFLICT (org_id, email) DO NOTHING;

-- =============================================================
-- 2. NEW LOAN APPLICATIONS
-- =============================================================
-- Columns supplied per loan:
--   crm_review loans (021, 022): no crm tracking cols set
--   committee_review loans (023, 024): crm_reviewed_by set
--   ed_approval loan (025): crm + committee cols set
--   md_approval loan (026): crm + committee + ed cols set

INSERT INTO loan_applications (
    id, org_id, ref_no, customer_type, loan_type, stage,
    applicant_name, bvn, phone, amount, tenor_months, purpose, repayment_mode,
    created_by, current_owner_id, credit_officer_id, branch_manager_id,
    crm_reviewed_by, crm_reviewed_at, crm_notes,
    committee_recommendation, committee_completed_at,
    ed_escalated_to_md, ed_approved_by, ed_approved_at,
    return_reason, returned_at, approved_by, approved_at, disbursed_at,
    created_at, updated_at
) VALUES

-- 021: crm_review — Funmilayo Adekoya — 4,500,000 — 24m — enterprise — cheque
(
    'c0000000-0000-4000-8000-000000000021',
    'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
    'MMFB-2026-01021',
    'new',
    'enterprise',
    'crm_review',
    'Funmilayo Adekoya',
    '22345678921',
    '08031234521',
    4500000.00,
    24,
    'Business expansion and working capital',
    'cheque',
    'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a02',
    'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a05',
    'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a03',
    'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a01',
    NULL, NULL, NULL,
    NULL, NULL,
    FALSE, NULL, NULL,
    NULL, NULL, NULL, NULL, NULL,
    NOW() - INTERVAL '4 days', NOW() - INTERVAL '3 hours'
),

-- 022: crm_review — Haruna Yakubu — 2,800,000 — 18m — msef — direct_debit
(
    'c0000000-0000-4000-8000-000000000022',
    'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
    'MMFB-2026-01022',
    'existing',
    'msef',
    'crm_review',
    'Haruna Yakubu',
    '22345678922',
    '08031234522',
    2800000.00,
    18,
    'Livestock trading capital',
    'direct_debit',
    'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a02',
    'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a05',
    'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a03',
    'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a01',
    NULL, NULL, NULL,
    NULL, NULL,
    FALSE, NULL, NULL,
    NULL, NULL, NULL, NULL, NULL,
    NOW() - INTERVAL '3 days', NOW() - INTERVAL '5 hours'
),

-- 023: committee_review — Adaeze Nnaji — 6,000,000 — 30m — enterprise — cheque
(
    'c0000000-0000-4000-8000-000000000023',
    'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
    'MMFB-2026-01023',
    'new',
    'enterprise',
    'committee_review',
    'Adaeze Nnaji',
    '22345678923',
    '08031234523',
    6000000.00,
    30,
    'Manufacturing plant upgrade',
    'cheque',
    'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a02',
    'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a08',
    'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a03',
    'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a01',
    'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a05',
    NOW() - INTERVAL '1 day',
    'Dossier complete. Strong business case.',
    NULL, NULL,
    FALSE, NULL, NULL,
    NULL, NULL, NULL, NULL, NULL,
    NOW() - INTERVAL '6 days', NOW() - INTERVAL '1 day'
),

-- 024: committee_review — Olumide Fashola — 3,200,000 — 20m — msef — direct_debit
(
    'c0000000-0000-4000-8000-000000000024',
    'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
    'MMFB-2026-01024',
    'existing',
    'msef',
    'committee_review',
    'Olumide Fashola',
    '22345678924',
    '08031234524',
    3200000.00,
    20,
    'Agro-processing business expansion',
    'direct_debit',
    'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a02',
    'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a08',
    'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a03',
    'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a01',
    'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a05',
    NOW() - INTERVAL '2 days',
    'Verified all documents. Recommend committee review.',
    NULL, NULL,
    FALSE, NULL, NULL,
    NULL, NULL, NULL, NULL, NULL,
    NOW() - INTERVAL '5 days', NOW() - INTERVAL '2 days'
),

-- 025: ed_approval — Chioma Obi — 7,500,000 — 36m — enterprise — cheque
(
    'c0000000-0000-4000-8000-000000000025',
    'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
    'MMFB-2026-01025',
    'new',
    'enterprise',
    'ed_approval',
    'Chioma Obi',
    '22345678925',
    '08031234525',
    7500000.00,
    36,
    'Hotel renovation and furnishing',
    'cheque',
    'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a02',
    'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a06',
    'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a03',
    'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a01',
    'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a05',
    NOW() - INTERVAL '3 days',
    'All pre-conditions satisfied. Escalated for committee review.',
    'approve',
    NOW() - INTERVAL '12 hours',
    FALSE, NULL, NULL,
    NULL, NULL, NULL, NULL, NULL,
    NOW() - INTERVAL '8 days', NOW() - INTERVAL '12 hours'
),

-- 026: md_approval — Babatunde Martins — 15,000,000 — 48m — enterprise — cheque
(
    'c0000000-0000-4000-8000-000000000026',
    'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
    'MMFB-2026-01026',
    'existing',
    'enterprise',
    'md_approval',
    'Babatunde Martins',
    '22345678926',
    '08031234526',
    15000000.00,
    48,
    'Large-scale industrial facility acquisition',
    'cheque',
    'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a02',
    'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a07',
    'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a03',
    'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a01',
    'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a05',
    NOW() - INTERVAL '7 days',
    'Full dossier verified. Large facility requires senior escalation.',
    'approve',
    NOW() - INTERVAL '2 days',
    TRUE,
    'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a06',
    NOW() - INTERVAL '1 day',
    NULL, NULL, NULL, NULL, NULL,
    NOW() - INTERVAL '10 days', NOW() - INTERVAL '1 day'
);

-- =============================================================
-- 3. STAGE DATA
-- =============================================================
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
            'credit_bureau', TRUE,
            'credit_check', TRUE,
            'gsi_mandate', TRUE,
            'cheque_authority', repayment_mode = 'cheque'
        )
    ),
    created_by,
    created_at + INTERVAL '30 minutes'
FROM loan_applications
WHERE id IN (
    'c0000000-0000-4000-8000-000000000021',
    'c0000000-0000-4000-8000-000000000022',
    'c0000000-0000-4000-8000-000000000023',
    'c0000000-0000-4000-8000-000000000024',
    'c0000000-0000-4000-8000-000000000025',
    'c0000000-0000-4000-8000-000000000026'
);

-- CRM review stage data for loans that have passed crm_review
INSERT INTO stage_data (loan_id, stage, data_json, saved_by, saved_at)
SELECT
    id,
    'crm_review',
    jsonb_build_object(
        'crm_reviewer', crm_reviewed_by,
        'crm_notes', crm_notes,
        'reviewed_at', crm_reviewed_at
    ),
    crm_reviewed_by,
    crm_reviewed_at
FROM loan_applications
WHERE id IN (
    'c0000000-0000-4000-8000-000000000023',
    'c0000000-0000-4000-8000-000000000024',
    'c0000000-0000-4000-8000-000000000025',
    'c0000000-0000-4000-8000-000000000026'
)
  AND crm_reviewed_by IS NOT NULL;

-- Committee review stage data for loans that have passed committee_review
INSERT INTO stage_data (loan_id, stage, data_json, saved_by, saved_at)
SELECT
    id,
    'committee_review',
    jsonb_build_object(
        'committee_recommendation', committee_recommendation,
        'completed_at', committee_completed_at
    ),
    'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a08',
    committee_completed_at
FROM loan_applications
WHERE id IN (
    'c0000000-0000-4000-8000-000000000025',
    'c0000000-0000-4000-8000-000000000026'
)
  AND committee_completed_at IS NOT NULL;

-- ED approval stage data for loan that has passed ed_approval
INSERT INTO stage_data (loan_id, stage, data_json, saved_by, saved_at)
VALUES (
    'c0000000-0000-4000-8000-000000000026',
    'ed_approval',
    jsonb_build_object(
        'ed_approved_by', 'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a06',
        'escalated_to_md', TRUE,
        'escalation_notes', 'Facility above ED approval threshold. Referred to MD.'
    ),
    'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a06',
    NOW() - INTERVAL '1 day'
);

-- =============================================================
-- 4. GUARANTORS (2 per loan)
-- =============================================================
INSERT INTO guarantors (
    loan_id, org_id, slot, full_name, relationship_to_client,
    bvn, phone, home_address, employment_type,
    monthly_salary, max_guarantee_amount, max_guarantee_amount_words,
    bank_name, account_number, cheque_number,
    form_stage, signature_detected, witness_signature_detected
)
SELECT
    la.id,
    la.org_id,
    slot_no,
    CASE slot_no
        WHEN 1 THEN 'Primary Guarantor for ' || la.applicant_name
        ELSE       'Secondary Guarantor for ' || la.applicant_name
    END,
    CASE slot_no WHEN 1 THEN 'Trader Association Member' ELSE 'Family Friend' END,
    '33345678' || lpad((row_number() OVER (ORDER BY la.ref_no, slot_no))::text, 3, '0'),
    CASE slot_no
        WHEN 1 THEN '0812456' || lpad((row_number() OVER (ORDER BY la.ref_no, slot_no))::text, 4, '0')
        ELSE        '0819876' || lpad((row_number() OVER (ORDER BY la.ref_no, slot_no))::text, 4, '0')
    END,
    'Lagos business district',
    CASE WHEN la.loan_type = 'payee' THEN 'Full-time' ELSE 'Self-employed' END,
    250000.00,
    la.amount,
    'Amount equal to requested facility',
    'Mainstreet MFB',
    '10' || lpad((row_number() OVER (ORDER BY la.ref_no, slot_no))::text, 8, '0'),
    'CHQ' || lpad((row_number() OVER (ORDER BY la.ref_no, slot_no))::text, 6, '0'),
    'submitted',
    TRUE,
    FALSE
FROM loan_applications la
CROSS JOIN (VALUES (1), (2)) AS slots(slot_no)
WHERE la.id IN (
    'c0000000-0000-4000-8000-000000000021',
    'c0000000-0000-4000-8000-000000000022',
    'c0000000-0000-4000-8000-000000000023',
    'c0000000-0000-4000-8000-000000000024',
    'c0000000-0000-4000-8000-000000000025',
    'c0000000-0000-4000-8000-000000000026'
);

-- =============================================================
-- 5. DOCUMENTS (5 types per loan)
-- =============================================================
INSERT INTO documents (
    loan_id, org_id, doc_type, form_code, original_name, stored_path,
    mime_type, size_bytes, quality_status,
    verified, verified_by, verified_at,
    uploaded_by, uploaded_at
)
SELECT
    la.id,
    la.org_id,
    doc_type,
    CASE doc_type
        WHEN 'loan_application_form' THEN 'MMFB/CRM/01'
        WHEN 'pledge_form'           THEN 'MMFB/CRM/02'
        WHEN 'guarantor_form'        THEN 'MMFB/CRM/03'
        ELSE NULL
    END,
    la.ref_no || '_' || doc_type || '.pdf',
    '/static/uploads/demo/' || la.ref_no || '_' || doc_type || '.pdf',
    'application/pdf',
    204800,
    'clear',
    TRUE,
    'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a03',
    la.created_at + INTERVAL '2 hours',
    la.created_by,
    la.created_at + INTERVAL '1 hour'
FROM loan_applications la
CROSS JOIN (VALUES
    ('loan_application_form'),
    ('valid_id'),
    ('bank_statement'),
    ('guarantor_form'),
    ('pledge_form')
) AS required_docs(doc_type)
WHERE la.id IN (
    'c0000000-0000-4000-8000-000000000021',
    'c0000000-0000-4000-8000-000000000022',
    'c0000000-0000-4000-8000-000000000023',
    'c0000000-0000-4000-8000-000000000024',
    'c0000000-0000-4000-8000-000000000025',
    'c0000000-0000-4000-8000-000000000026'
);

-- =============================================================
-- 6. COMMITTEE VOTES
-- =============================================================

-- Loan 023 (committee_review — 2 votes so far: bola=approve, zainab=approve)
INSERT INTO committee_votes (loan_id, org_id, member_id, recommendation, notes, voted_at) VALUES
(
    'c0000000-0000-4000-8000-000000000023',
    'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
    'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a08',  -- Dr. Bola Adeyemi
    'approve',
    'Strong financials',
    NOW() - INTERVAL '18 hours'
),
(
    'c0000000-0000-4000-8000-000000000023',
    'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
    'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a09',  -- Hajiya Zainab Musa
    'approve',
    'Good track record',
    NOW() - INTERVAL '16 hours'
);

-- Loan 024 (committee_review — 3 votes: bola=approve, zainab=return, emmanuel=approve)
INSERT INTO committee_votes (loan_id, org_id, member_id, recommendation, notes, voted_at) VALUES
(
    'c0000000-0000-4000-8000-000000000024',
    'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
    'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a08',  -- Dr. Bola Adeyemi
    'approve',
    'Business viable',
    NOW() - INTERVAL '36 hours'
),
(
    'c0000000-0000-4000-8000-000000000024',
    'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
    'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a09',  -- Hajiya Zainab Musa
    'return',
    'Needs more collateral',
    NOW() - INTERVAL '34 hours'
),
(
    'c0000000-0000-4000-8000-000000000024',
    'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
    'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a10',  -- Chief Emmanuel Obi
    'approve',
    'Overall positive',
    NOW() - INTERVAL '32 hours'
);

-- Loan 025 (committee already completed — all three approved)
INSERT INTO committee_votes (loan_id, org_id, member_id, recommendation, notes, voted_at) VALUES
(
    'c0000000-0000-4000-8000-000000000025',
    'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
    'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a08',
    'approve',
    'Solid repayment capacity demonstrated',
    NOW() - INTERVAL '15 hours'
),
(
    'c0000000-0000-4000-8000-000000000025',
    'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
    'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a09',
    'approve',
    'Business is established and profitable',
    NOW() - INTERVAL '14 hours'
),
(
    'c0000000-0000-4000-8000-000000000025',
    'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
    'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a10',
    'approve',
    'Collateral and cash flow adequate',
    NOW() - INTERVAL '13 hours'
);

-- Loan 026 (committee already completed — all three approved)
INSERT INTO committee_votes (loan_id, org_id, member_id, recommendation, notes, voted_at) VALUES
(
    'c0000000-0000-4000-8000-000000000026',
    'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
    'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a08',
    'approve',
    'Large facility but well-secured',
    NOW() - INTERVAL '55 hours'
),
(
    'c0000000-0000-4000-8000-000000000026',
    'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
    'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a09',
    'approve',
    'Borrower history is clean',
    NOW() - INTERVAL '53 hours'
),
(
    'c0000000-0000-4000-8000-000000000026',
    'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
    'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a10',
    'approve',
    'Risk appetite within acceptable limits',
    NOW() - INTERVAL '51 hours'
);

-- =============================================================
-- 7. BOARD REFERRALS (loan 026 only)
-- =============================================================
INSERT INTO board_referrals (
    loan_id, org_id, referred_by,
    board_member_email, board_member_name,
    notes, status, created_at
) VALUES (
    'c0000000-0000-4000-8000-000000000026',
    'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
    'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a07',  -- Emeka Okonkwo (MD)
    'boardchair@mmfb.com',
    'Board Chairman',
    'Please advise on this large facility',
    'pending',
    NOW() - INTERVAL '20 hours'
);

-- =============================================================
-- 8. WORKFLOW EVENTS (full journey for each loan)
-- =============================================================

-- Loan 021: intake → ocr_review → credit_review → branch_approval → crm_review (current)
INSERT INTO workflow_events (loan_id, org_id, event_type, from_stage, to_stage, triggered_by, triggered_role, notes, created_at) VALUES
('c0000000-0000-4000-8000-000000000021','a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11','loan.created',            NULL,              'intake',       'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a02','loan_officer',  'Application created',                        NOW() - INTERVAL '4 days'),
('c0000000-0000-4000-8000-000000000021','a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11','intake.submitted',        'intake',          'ocr_review',   'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a02','loan_officer',  'Submitted for OCR review',                   NOW() - INTERVAL '4 days' + INTERVAL '1 hour'),
('c0000000-0000-4000-8000-000000000021','a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11','ocr.verified',            'ocr_review',      'credit_review','b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a03','credit_officer','OCR exceptions cleared',                     NOW() - INTERVAL '3 days' + INTERVAL '10 hours'),
('c0000000-0000-4000-8000-000000000021','a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11','credit_review.completed', 'credit_review',   'branch_approval','b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a03','credit_officer','Credit analysis complete',                   NOW() - INTERVAL '3 days'),
('c0000000-0000-4000-8000-000000000021','a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11','branch.approved',         'branch_approval', 'crm_review',   'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a01','branch_manager','Branch approved. Referred to CRM.',          NOW() - INTERVAL '2 days');

-- Loan 022: intake → ocr_review → credit_review → branch_approval → crm_review (current)
INSERT INTO workflow_events (loan_id, org_id, event_type, from_stage, to_stage, triggered_by, triggered_role, notes, created_at) VALUES
('c0000000-0000-4000-8000-000000000022','a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11','loan.created',            NULL,              'intake',       'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a02','loan_officer',  'Application created',                        NOW() - INTERVAL '3 days'),
('c0000000-0000-4000-8000-000000000022','a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11','intake.submitted',        'intake',          'ocr_review',   'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a02','loan_officer',  'Submitted for OCR review',                   NOW() - INTERVAL '3 days' + INTERVAL '1 hour'),
('c0000000-0000-4000-8000-000000000022','a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11','ocr.verified',            'ocr_review',      'credit_review','b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a03','credit_officer','OCR review passed',                          NOW() - INTERVAL '2 days' + INTERVAL '8 hours'),
('c0000000-0000-4000-8000-000000000022','a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11','credit_review.completed', 'credit_review',   'branch_approval','b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a03','credit_officer','Credit analysis complete',                   NOW() - INTERVAL '2 days'),
('c0000000-0000-4000-8000-000000000022','a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11','branch.approved',         'branch_approval', 'crm_review',   'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a01','branch_manager','Branch approved. Referred to CRM.',          NOW() - INTERVAL '1 day');

-- Loan 023: intake → ocr_review → credit_review → branch_approval → crm_review → committee_review (current)
INSERT INTO workflow_events (loan_id, org_id, event_type, from_stage, to_stage, triggered_by, triggered_role, notes, created_at) VALUES
('c0000000-0000-4000-8000-000000000023','a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11','loan.created',            NULL,              'intake',         'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a02','loan_officer',  'Application created',                        NOW() - INTERVAL '6 days'),
('c0000000-0000-4000-8000-000000000023','a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11','intake.submitted',        'intake',          'ocr_review',     'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a02','loan_officer',  'Submitted for OCR review',                   NOW() - INTERVAL '6 days' + INTERVAL '1 hour'),
('c0000000-0000-4000-8000-000000000023','a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11','ocr.verified',            'ocr_review',      'credit_review',  'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a03','credit_officer','OCR review passed',                          NOW() - INTERVAL '5 days'),
('c0000000-0000-4000-8000-000000000023','a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11','credit_review.completed', 'credit_review',   'branch_approval','b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a03','credit_officer','Credit analysis complete',                   NOW() - INTERVAL '4 days'),
('c0000000-0000-4000-8000-000000000023','a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11','branch.approved',         'branch_approval', 'crm_review',     'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a01','branch_manager','Branch approved. Referred to CRM.',          NOW() - INTERVAL '3 days'),
('c0000000-0000-4000-8000-000000000023','a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11','crm.reviewed',            'crm_review',      'committee_review','b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a05','crm',          'Dossier complete. Strong business case.',     NOW() - INTERVAL '1 day');

-- Loan 024: intake → ocr_review → credit_review → branch_approval → crm_review → committee_review (current)
INSERT INTO workflow_events (loan_id, org_id, event_type, from_stage, to_stage, triggered_by, triggered_role, notes, created_at) VALUES
('c0000000-0000-4000-8000-000000000024','a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11','loan.created',            NULL,              'intake',         'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a02','loan_officer',  'Application created',                        NOW() - INTERVAL '5 days'),
('c0000000-0000-4000-8000-000000000024','a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11','intake.submitted',        'intake',          'ocr_review',     'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a02','loan_officer',  'Submitted for OCR review',                   NOW() - INTERVAL '5 days' + INTERVAL '1 hour'),
('c0000000-0000-4000-8000-000000000024','a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11','ocr.verified',            'ocr_review',      'credit_review',  'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a03','credit_officer','OCR review passed',                          NOW() - INTERVAL '4 days'),
('c0000000-0000-4000-8000-000000000024','a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11','credit_review.completed', 'credit_review',   'branch_approval','b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a03','credit_officer','Credit analysis complete',                   NOW() - INTERVAL '3 days'),
('c0000000-0000-4000-8000-000000000024','a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11','branch.approved',         'branch_approval', 'crm_review',     'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a01','branch_manager','Branch approved. Referred to CRM.',          NOW() - INTERVAL '2 days' + INTERVAL '6 hours'),
('c0000000-0000-4000-8000-000000000024','a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11','crm.reviewed',            'crm_review',      'committee_review','b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a05','crm',          'Verified all documents. Recommend committee review.',NOW() - INTERVAL '2 days');

-- Loan 025: intake → ocr_review → credit_review → branch_approval → crm_review → committee_review → ed_approval (current)
INSERT INTO workflow_events (loan_id, org_id, event_type, from_stage, to_stage, triggered_by, triggered_role, notes, created_at) VALUES
('c0000000-0000-4000-8000-000000000025','a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11','loan.created',            NULL,              'intake',          'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a02','loan_officer',  'Application created',                         NOW() - INTERVAL '8 days'),
('c0000000-0000-4000-8000-000000000025','a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11','intake.submitted',        'intake',          'ocr_review',      'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a02','loan_officer',  'Submitted for OCR review',                    NOW() - INTERVAL '8 days' + INTERVAL '1 hour'),
('c0000000-0000-4000-8000-000000000025','a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11','ocr.verified',            'ocr_review',      'credit_review',   'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a03','credit_officer','OCR review passed',                           NOW() - INTERVAL '7 days'),
('c0000000-0000-4000-8000-000000000025','a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11','credit_review.completed', 'credit_review',   'branch_approval', 'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a03','credit_officer','Credit analysis complete',                    NOW() - INTERVAL '6 days'),
('c0000000-0000-4000-8000-000000000025','a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11','branch.approved',         'branch_approval', 'crm_review',      'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a01','branch_manager','Branch approved. Referred to CRM.',           NOW() - INTERVAL '5 days'),
('c0000000-0000-4000-8000-000000000025','a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11','crm.reviewed',            'crm_review',      'committee_review', 'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a05','crm',          'All pre-conditions satisfied.',               NOW() - INTERVAL '3 days'),
('c0000000-0000-4000-8000-000000000025','a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11','committee.completed',     'committee_review','ed_approval',      'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a08','committee',    'Unanimous approval. Escalated to ED.',        NOW() - INTERVAL '12 hours');

-- Loan 026: intake → ocr_review → credit_review → branch_approval → crm_review → committee_review → ed_approval → md_approval (current)
INSERT INTO workflow_events (loan_id, org_id, event_type, from_stage, to_stage, triggered_by, triggered_role, notes, created_at) VALUES
('c0000000-0000-4000-8000-000000000026','a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11','loan.created',            NULL,              'intake',          'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a02','loan_officer',  'Application created',                         NOW() - INTERVAL '10 days'),
('c0000000-0000-4000-8000-000000000026','a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11','intake.submitted',        'intake',          'ocr_review',      'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a02','loan_officer',  'Submitted for OCR review',                    NOW() - INTERVAL '10 days' + INTERVAL '1 hour'),
('c0000000-0000-4000-8000-000000000026','a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11','ocr.verified',            'ocr_review',      'credit_review',   'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a03','credit_officer','OCR review passed',                           NOW() - INTERVAL '9 days'),
('c0000000-0000-4000-8000-000000000026','a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11','credit_review.completed', 'credit_review',   'branch_approval', 'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a03','credit_officer','Credit analysis complete',                    NOW() - INTERVAL '8 days'),
('c0000000-0000-4000-8000-000000000026','a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11','branch.approved',         'branch_approval', 'crm_review',      'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a01','branch_manager','Branch approved. Referred to CRM.',           NOW() - INTERVAL '7 days'),
('c0000000-0000-4000-8000-000000000026','a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11','crm.reviewed',            'crm_review',      'committee_review', 'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a05','crm',          'Full dossier verified. Escalated for committee.', NOW() - INTERVAL '7 days' + INTERVAL '8 hours'),
('c0000000-0000-4000-8000-000000000026','a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11','committee.completed',     'committee_review','ed_approval',      'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a08','committee',    'Unanimous approval. Escalated to ED.',        NOW() - INTERVAL '2 days'),
('c0000000-0000-4000-8000-000000000026','a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11','ed.escalated_to_md',      'ed_approval',     'md_approval',      'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a06','ed',           'Facility exceeds ED threshold. Referred to MD.', NOW() - INTERVAL '1 day');

-- =============================================================
-- 9. AUDIT ENTRIES (mirror all workflow events)
-- =============================================================
INSERT INTO audit_entries (
    org_id, entity_type, entity_id, action,
    user_id, user_role, field_name, old_value, new_value,
    source, notes, created_at
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
WHERE loan_id IN (
    'c0000000-0000-4000-8000-000000000021',
    'c0000000-0000-4000-8000-000000000022',
    'c0000000-0000-4000-8000-000000000023',
    'c0000000-0000-4000-8000-000000000024',
    'c0000000-0000-4000-8000-000000000025',
    'c0000000-0000-4000-8000-000000000026'
);

-- Audit entries for committee votes
INSERT INTO audit_entries (
    org_id, entity_type, entity_id, action,
    user_id, user_role, field_name, old_value, new_value,
    source, notes, created_at
)
SELECT
    cv.org_id,
    'loan_application',
    cv.loan_id,
    'committee.vote',
    cv.member_id,
    'committee',
    'committee_vote',
    NULL,
    cv.recommendation,
    'manual',
    cv.notes,
    cv.voted_at
FROM committee_votes cv
WHERE cv.loan_id IN (
    'c0000000-0000-4000-8000-000000000023',
    'c0000000-0000-4000-8000-000000000024',
    'c0000000-0000-4000-8000-000000000025',
    'c0000000-0000-4000-8000-000000000026'
);

-- Audit entry for board referral (loan 026)
INSERT INTO audit_entries (
    org_id, entity_type, entity_id, action,
    user_id, user_role, field_name, old_value, new_value,
    source, notes, created_at
) VALUES (
    'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
    'loan_application',
    'c0000000-0000-4000-8000-000000000026',
    'board.referred',
    'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a07',
    'md',
    'board_referral',
    NULL,
    'boardchair@mmfb.com',
    'manual',
    'Please advise on this large facility',
    NOW() - INTERVAL '20 hours'
);
