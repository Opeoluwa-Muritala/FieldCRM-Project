-- Migration 020: BVN/NIN Verification, Credit Bureau, AML, Legal valuation, checklists, interest rates, offer letters.

-- 1. Update users check constraint to include 'legal' role
ALTER TABLE users DROP CONSTRAINT IF EXISTS users_role_check;
ALTER TABLE users ADD CONSTRAINT users_role_check CHECK (role IN (
  'account_officer','branch_manager','branch_supervisor','credit_analyst',
  'crm','head_crm','auditor','ed','md','system_admin','legal'
));

-- 2. verification_checks table
CREATE TABLE IF NOT EXISTS verification_checks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    loan_application_id UUID NOT NULL REFERENCES loan_applications(id) ON DELETE CASCADE,
    subject_type TEXT NOT NULL,
    provider TEXT NOT NULL DEFAULT 'qoreid',
    status TEXT NOT NULL,
    is_valid BOOLEAN,
    raw_response JSONB,
    checked_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 3. bureau_submissions table
CREATE TABLE IF NOT EXISTS bureau_submissions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    loan_application_id UUID NOT NULL REFERENCES loan_applications(id) ON DELETE CASCADE,
    registry_id TEXT,
    status TEXT NOT NULL,
    report_type TEXT,
    raw_response JSONB,
    submitted_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 4. sanctions_checks table
CREATE TABLE IF NOT EXISTS sanctions_checks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    loan_application_id UUID NOT NULL REFERENCES loan_applications(id) ON DELETE CASCADE,
    subject_type TEXT NOT NULL,
    subject_name TEXT NOT NULL,
    status TEXT NOT NULL,
    category_count JSONB,
    raw_response JSONB,
    checked_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 5. checklist_items table
CREATE TABLE IF NOT EXISTS checklist_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    loan_application_id UUID NOT NULL REFERENCES loan_applications(id) ON DELETE CASCADE,
    context TEXT NOT NULL,
    item_key TEXT NOT NULL,
    item_label TEXT NOT NULL,
    is_checked BOOLEAN NOT NULL DEFAULT FALSE,
    checked_by UUID REFERENCES users(id) ON DELETE SET NULL,
    checked_at TIMESTAMPTZ,
    UNIQUE (loan_application_id, context, item_key)
);

-- 6. interest_rate_presets table
CREATE TABLE IF NOT EXISTS interest_rate_presets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    loan_type TEXT NOT NULL,
    rate NUMERIC(15,4) NOT NULL,
    rate_type TEXT NOT NULL CHECK (rate_type IN ('flat', 'reducing')),
    effective_from TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    set_by UUID REFERENCES users(id) ON DELETE SET NULL,
    set_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 7. offer_letters table
CREATE TABLE IF NOT EXISTS offer_letters (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    loan_application_id UUID NOT NULL REFERENCES loan_applications(id) ON DELETE CASCADE,
    loan_type TEXT NOT NULL,
    clause_set_version TEXT,
    clauses_included JSONB,
    interest_rate_snapshot NUMERIC(15,4),
    generated_pdf_url TEXT,
    generated_by UUID REFERENCES users(id) ON DELETE SET NULL,
    generated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    status TEXT NOT NULL CHECK (status IN ('draft', 'issued'))
);

-- 8. offer_letter_clause_sets table
CREATE TABLE IF NOT EXISTS offer_letter_clause_sets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    loan_type TEXT NOT NULL UNIQUE,
    clause_keys JSONB NOT NULL
);

-- 9. Add nullable columns to pledged_items
ALTER TABLE pledged_items ADD COLUMN IF NOT EXISTS appraised_value NUMERIC(15,2);
ALTER TABLE pledged_items ADD COLUMN IF NOT EXISTS valuer_name TEXT;
ALTER TABLE pledged_items ADD COLUMN IF NOT EXISTS valuer_license_no TEXT;
ALTER TABLE pledged_items ADD COLUMN IF NOT EXISTS valuation_date DATE;
ALTER TABLE pledged_items ADD COLUMN IF NOT EXISTS loan_to_value_ratio NUMERIC(15,4);

-- 10. Add nullable interest_rate_snapshot to loan_applications
ALTER TABLE loan_applications ADD COLUMN IF NOT EXISTS interest_rate_snapshot NUMERIC(15,4);
