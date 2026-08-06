-- Migration: 034_loan_products.sql
-- Create canonical loan_products catalog and associate tables.

CREATE TABLE IF NOT EXISTS loan_products (
    code                        TEXT PRIMARY KEY,
    name                        TEXT NOT NULL,
    description                 TEXT,
    family                      TEXT NOT NULL,
    customer_segment            TEXT NOT NULL,
    active                      BOOLEAN NOT NULL DEFAULT TRUE,
    min_amount                  NUMERIC(15,2),
    max_amount                  NUMERIC(15,2),
    min_tenor_months            INTEGER,
    max_tenor_months            INTEGER,
    repayment_frequency         TEXT NOT NULL DEFAULT 'monthly',
    interest_calculation_type   TEXT NOT NULL DEFAULT 'flat',
    collateral_required         BOOLEAN NOT NULL DEFAULT FALSE,
    guarantor_required          BOOLEAN NOT NULL DEFAULT FALSE,
    approval_route              TEXT NOT NULL DEFAULT 'standard',
    employer_sector_restriction TEXT,
    supports_repeat             BOOLEAN NOT NULL DEFAULT TRUE,
    effective_date              DATE NOT NULL DEFAULT CURRENT_DATE,
    retirement_date             DATE,
    workflow_stages             TEXT NOT NULL DEFAULT 'intake,branch_manager_review,branch_supervisor_review,credit_analyst_review,crm_review,head_crm_review,ed_approval,md_approval,disbursement_ready',
    is_legacy                   BOOLEAN NOT NULL DEFAULT FALSE,
    created_at                  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at                  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS product_aliases (
    alias        TEXT PRIMARY KEY,
    product_code TEXT NOT NULL REFERENCES loan_products(code) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS product_document_requirements (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_code TEXT NOT NULL REFERENCES loan_products(code) ON DELETE CASCADE,
    doc_type     TEXT NOT NULL,
    is_mandatory BOOLEAN NOT NULL DEFAULT TRUE,
    UNIQUE (product_code, doc_type)
);

-- Seed approved products
INSERT INTO loan_products (code, name, description, family, customer_segment, min_amount, max_amount, min_tenor_months, max_tenor_months, repayment_frequency, interest_calculation_type, collateral_required, guarantor_required, workflow_stages) VALUES
('elms2', 'E-LMS 2', 'Education Loan Management Scheme Version 2', 'education', 'individual', 50000.00, 1000000.00, 3, 12, 'monthly', 'flat', FALSE, TRUE, 'intake,branch_manager_review,branch_supervisor_review,credit_analyst_review,crm_review,head_crm_review,ed_approval,disbursement_ready'),
('elms3', 'E-LMS 3', 'Education Loan Management Scheme Version 3', 'education', 'individual', 100000.00, 2000000.00, 3, 18, 'monthly', 'flat', FALSE, TRUE, 'intake,branch_manager_review,branch_supervisor_review,credit_analyst_review,crm_review,head_crm_review,ed_approval,disbursement_ready'),
('elms4', 'E-LMS 4', 'Brewery Support Loan Scheme', 'corporate_business', 'corporate', 500000.00, 10000000.00, 6, 36, 'monthly', 'reducing', TRUE, TRUE, 'intake,branch_manager_review,branch_supervisor_review,credit_analyst_review,crm_review,head_crm_review,ed_approval,md_approval,disbursement_ready'),
('elms5', 'E-LMS 5', 'Education Loan Management Scheme Version 5', 'education', 'individual', 150000.00, 3000000.00, 3, 24, 'monthly', 'flat', FALSE, TRUE, 'intake,branch_manager_review,branch_supervisor_review,credit_analyst_review,crm_review,head_crm_review,ed_approval,disbursement_ready'),
('elms6', 'E-LMS 6', 'FFS Business Development Loan', 'corporate_business', 'corporate', 1000000.00, 15000000.00, 6, 24, 'monthly', 'reducing', TRUE, TRUE, 'intake,branch_manager_review,branch_supervisor_review,credit_analyst_review,crm_review,head_crm_review,ed_approval,md_approval,disbursement_ready'),
('elms8', 'E-LMS 8', 'FMC General Commercial Facility', 'corporate_business', 'corporate', 2000000.00, 20000000.00, 6, 24, 'monthly', 'reducing', TRUE, TRUE, 'intake,branch_manager_review,branch_supervisor_review,credit_analyst_review,crm_review,head_crm_review,ed_approval,md_approval,disbursement_ready'),
('elms10', 'E-LMS 10', 'Education Loan Management Scheme Version 10', 'education', 'individual', 250000.00, 5000000.00, 6, 36, 'monthly', 'flat', TRUE, TRUE, 'intake,branch_manager_review,branch_supervisor_review,credit_analyst_review,crm_review,head_crm_review,ed_approval,md_approval,disbursement_ready'),
('asset_finance', 'Main Asset Finance', 'Facility for asset acquisition and lease finance', 'corporate_business', 'individual', 500000.00, 10000000.00, 6, 36, 'monthly', 'flat', TRUE, TRUE, 'intake,branch_manager_review,branch_supervisor_review,credit_analyst_review,crm_review,head_crm_review,ed_approval,disbursement_ready'),
('corporate_sme', 'Corporate/SME Facility', 'Working capital and asset acquisition for SMEs', 'corporate_business', 'corporate', 1000000.00, 50000000.00, 6, 60, 'monthly', 'reducing', TRUE, TRUE, 'intake,branch_manager_review,branch_supervisor_review,credit_analyst_review,crm_review,head_crm_review,ed_approval,md_approval,disbursement_ready'),
('overdraft', 'Overdraft', 'Short-term credit facility to fund cash flow deficits', 'corporate_business', 'corporate', 200000.00, 5000000.00, 1, 12, 'monthly', 'reducing', TRUE, TRUE, 'intake,branch_manager_review,branch_supervisor_review,credit_analyst_review,crm_review,head_crm_review,ed_approval,md_approval,disbursement_ready'),
('equipment_procurement', 'Equipment Procurement / Bundled Facility', 'Finance for specialized machinery and equipment', 'corporate_business', 'corporate', 1000000.00, 30000000.00, 12, 48, 'monthly', 'reducing', TRUE, TRUE, 'intake,branch_manager_review,branch_supervisor_review,credit_analyst_review,crm_review,head_crm_review,ed_approval,md_approval,disbursement_ready'),
('plasa', 'Personal Loan Against Salary (PLASA)', 'Consumer credit facility for salary earners', 'salary_personal', 'salary_earner', 50000.00, 2000000.00, 3, 24, 'monthly', 'flat', FALSE, TRUE, 'intake,branch_manager_review,crm_review,ed_approval,disbursement_ready'),
('msas', 'Mainstreet Salary Advance Scheme (MSAS)', 'Short-term salary advance facility', 'salary_personal', 'salary_earner', 20000.00, 500000.00, 1, 6, 'monthly', 'flat', FALSE, FALSE, 'intake,branch_manager_review,crm_review,disbursement_ready'),
('spats', 'Special Payroll Advance & Treasury Scheme (SPATS)', 'Treasury backed payroll advance scheme', 'salary_personal', 'salary_earner', 100000.00, 3000000.00, 3, 12, 'monthly', 'flat', FALSE, TRUE, 'intake,branch_manager_review,crm_review,ed_approval,disbursement_ready'),
('mcas', 'Microfinance Cash Advance Scheme (MCAS)', 'Urgent short-term cash advance', 'salary_personal', 'salary_earner', 10000.00, 200000.00, 1, 3, 'monthly', 'flat', FALSE, FALSE, 'intake,branch_manager_review,crm_review,disbursement_ready'),
('msef', 'SME/MSE Facility', 'MSE and SME Micro Loan Facility', 'corporate_business', 'corporate', 100000.00, 5000000.00, 3, 24, 'monthly', 'flat', TRUE, TRUE, 'intake,branch_manager_review,branch_supervisor_review,credit_analyst_review,crm_review,head_crm_review,ed_approval,disbursement_ready'),
('other', 'Legacy/Uncategorized', 'Legacy unmapped loan products', 'salary_personal', 'individual', 1000.00, 100000000.00, 1, 120, 'monthly', 'flat', FALSE, FALSE, 'intake,branch_manager_review,branch_supervisor_review,credit_analyst_review,crm_review,head_crm_review,ed_approval,md_approval,disbursement_ready');

-- Set 'other' as inactive/legacy
UPDATE loan_products SET active = FALSE, is_legacy = TRUE WHERE code = 'other';

-- Seed aliases
INSERT INTO product_aliases (alias, product_code) VALUES
('enterprise', 'corporate_sme'),
('enterprise loan', 'corporate_sme'),
('msef', 'msef'),
('payee', 'plasa'),
('other', 'other'),
('other option', 'other'),
('plasa', 'plasa'),
('msas', 'msas'),
('spats', 'spats'),
('mcas', 'mcas'),
('elms2', 'elms2'),
('elms3', 'elms3'),
('elms4', 'elms4'),
('elms5', 'elms5'),
('elms6', 'elms6'),
('elms8', 'elms8'),
('elms10', 'elms10'),
('asset_finance', 'asset_finance'),
('corporate_sme', 'corporate_sme'),
('overdraft', 'overdraft'),
('equipment_procurement', 'equipment_procurement');

-- Seed required documents
INSERT INTO product_document_requirements (product_code, doc_type, is_mandatory) VALUES
('plasa', 'payslip', TRUE),
('plasa', 'employment_confirmation', TRUE),
('plasa', 'salary_account_statement', TRUE),
('msas', 'payslip', TRUE),
('msas', 'salary_account_statement', TRUE),
('spats', 'payslip', TRUE),
('spats', 'employment_confirmation', TRUE),
('spats', 'salary_account_statement', TRUE),
('mcas', 'payslip', TRUE),
('asset_finance', 'quotation', TRUE),
('asset_finance', 'pro_forma_invoice', TRUE),
('asset_finance', 'asset_valuation', TRUE),
('asset_finance', 'ownership_evidence', TRUE),
('overdraft', 'business_statements', TRUE),
('overdraft', 'cash_flow_evidence', TRUE),
('overdraft', 'existing_obligations', FALSE),
('corporate_sme', 'registration_documents', TRUE),
('corporate_sme', 'business_statements', TRUE),
('corporate_sme', 'tax_operating_evidence', TRUE),
('elms2', 'school_institution_documentation', TRUE),
('elms2', 'purpose_evidence', TRUE),
('elms3', 'school_institution_documentation', TRUE),
('elms3', 'purpose_evidence', TRUE),
('elms5', 'school_institution_documentation', TRUE),
('elms5', 'purpose_evidence', TRUE),
('elms10', 'school_institution_documentation', TRUE),
('elms10', 'purpose_evidence', TRUE),
('equipment_procurement', 'supplier_quotation', TRUE),
('equipment_procurement', 'procurement_documentation', TRUE);

-- Drop the old check constraint on loan_type column of loan_applications table first to allow updates
ALTER TABLE loan_applications DROP CONSTRAINT IF EXISTS loan_applications_loan_type_check;

-- Update existing database records (explicit mappings)
UPDATE loan_applications SET loan_type = 'corporate_sme' WHERE loan_type = 'enterprise';
UPDATE loan_applications SET loan_type = 'plasa' WHERE loan_type = 'payee';

UPDATE interest_rate_presets SET loan_type = 'corporate_sme' WHERE loan_type = 'enterprise';
UPDATE interest_rate_presets SET loan_type = 'plasa' WHERE loan_type = 'payee';

UPDATE offer_letters SET loan_type = 'corporate_sme' WHERE loan_type = 'enterprise';
UPDATE offer_letters SET loan_type = 'plasa' WHERE loan_type = 'payee';

UPDATE offer_letter_clause_sets SET loan_type = 'corporate_sme' WHERE loan_type = 'enterprise';
UPDATE offer_letter_clause_sets SET loan_type = 'plasa' WHERE loan_type = 'payee';

-- Alter loan_applications to enforce foreign key constraint
ALTER TABLE loan_applications ADD CONSTRAINT fk_loan_applications_product FOREIGN KEY (loan_type) REFERENCES loan_products(code);

-- Alter rest of references to enforce FK
ALTER TABLE interest_rate_presets ADD CONSTRAINT fk_interest_rate_presets_product FOREIGN KEY (loan_type) REFERENCES loan_products(code) ON DELETE CASCADE;
ALTER TABLE offer_letters ADD CONSTRAINT fk_offer_letters_product FOREIGN KEY (loan_type) REFERENCES loan_products(code) ON DELETE CASCADE;
ALTER TABLE offer_letter_clause_sets ADD CONSTRAINT fk_offer_letter_clause_sets_product FOREIGN KEY (loan_type) REFERENCES loan_products(code) ON DELETE CASCADE;
