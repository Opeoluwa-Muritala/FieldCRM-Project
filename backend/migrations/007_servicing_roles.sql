-- Migration 007: New roles (crm, md, ed), new stages, loan servicing tables, CBN compliance columns
-- Run after 006_password_reset.sql

-- =========================================================
-- 1. Extend stage CHECK constraint on loan_applications
-- =========================================================
ALTER TABLE loan_applications
  DROP CONSTRAINT IF EXISTS loan_applications_stage_check;

ALTER TABLE loan_applications
  ADD CONSTRAINT loan_applications_stage_check CHECK (stage IN (
    'intake','ocr_review','credit_review','branch_approval',
    'crm_review','executive_approval',
    'disbursement_ready','disbursed','returned','rejected'
  ));

-- =========================================================
-- 2. Extend role CHECK constraint on users
-- =========================================================
ALTER TABLE users
  DROP CONSTRAINT IF EXISTS users_role_check;

ALTER TABLE users
  ADD CONSTRAINT users_role_check CHECK (role IN (
    'loan_officer','credit_officer','branch_manager',
    'auditor','system_admin','crm','md','ed'
  ));

-- =========================================================
-- 3. Disbursement + archive columns on loan_applications
-- =========================================================
ALTER TABLE loan_applications
  ADD COLUMN IF NOT EXISTS disbursement_ref          TEXT,
  ADD COLUMN IF NOT EXISTS disbursement_method       TEXT CHECK (disbursement_method IN ('bank_transfer','cheque','cash','direct_debit')),
  ADD COLUMN IF NOT EXISTS disbursement_memo_path    TEXT,
  ADD COLUMN IF NOT EXISTS disbursed_amount          NUMERIC(15,2),
  ADD COLUMN IF NOT EXISTS disbursed_bank_ref        TEXT,
  ADD COLUMN IF NOT EXISTS audit_archived_at         TIMESTAMPTZ,
  ADD COLUMN IF NOT EXISTS audit_package_path        TEXT,
  ADD COLUMN IF NOT EXISTS executive_approved_by     UUID REFERENCES users(id),
  ADD COLUMN IF NOT EXISTS executive_approved_at     TIMESTAMPTZ,
  ADD COLUMN IF NOT EXISTS crm_reviewed_by           UUID REFERENCES users(id),
  ADD COLUMN IF NOT EXISTS crm_reviewed_at           TIMESTAMPTZ,
  ADD COLUMN IF NOT EXISTS crm_notes                 TEXT;

-- =========================================================
-- 4. Loan classification + CBN compliance columns
-- =========================================================
ALTER TABLE loan_applications
  ADD COLUMN IF NOT EXISTS classification            TEXT DEFAULT 'current'
                                                     CHECK (classification IN ('current','olem','substandard','doubtful','lost')),
  ADD COLUMN IF NOT EXISTS days_past_due             INTEGER NOT NULL DEFAULT 0,
  ADD COLUMN IF NOT EXISTS classification_updated_at TIMESTAMPTZ,
  ADD COLUMN IF NOT EXISTS sector                    TEXT,
  ADD COLUMN IF NOT EXISTS interest_rate             NUMERIC(5,2),
  ADD COLUMN IF NOT EXISTS repayment_frequency       TEXT CHECK (repayment_frequency IN ('daily','weekly','biweekly','monthly')),
  ADD COLUMN IF NOT EXISTS schedule_method           TEXT DEFAULT 'flat_rate'
                                                     CHECK (schedule_method IN ('flat_rate','reducing_balance')),
  ADD COLUMN IF NOT EXISTS credit_bureau_1_date      DATE,
  ADD COLUMN IF NOT EXISTS credit_bureau_2_date      DATE,
  ADD COLUMN IF NOT EXISTS crms_searched             BOOLEAN NOT NULL DEFAULT FALSE,
  ADD COLUMN IF NOT EXISTS crms_search_date          DATE;

-- =========================================================
-- 5. NCR registration on pledged_items
-- =========================================================
ALTER TABLE pledged_items
  ADD COLUMN IF NOT EXISTS ncr_reg_number            TEXT;

-- =========================================================
-- 6. Zoho file ID on documents
-- =========================================================
ALTER TABLE documents
  ADD COLUMN IF NOT EXISTS zoho_file_id              TEXT;

-- =========================================================
-- 7. Repayment schedule table
-- =========================================================
CREATE TABLE IF NOT EXISTS repayment_schedule (
  id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  loan_id           UUID NOT NULL REFERENCES loan_applications(id) ON DELETE CASCADE,
  org_id            UUID NOT NULL REFERENCES organisations(id),
  installment_no    INTEGER NOT NULL,
  due_date          DATE NOT NULL,
  principal_due     NUMERIC(15,2) NOT NULL,
  interest_due      NUMERIC(15,2) NOT NULL,
  total_due         NUMERIC(15,2) NOT NULL,
  created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  UNIQUE (loan_id, installment_no)
);
CREATE INDEX IF NOT EXISTS ix_schedule_loan ON repayment_schedule (loan_id, installment_no);

-- =========================================================
-- 8. Repayment records table
-- =========================================================
CREATE TABLE IF NOT EXISTS repayment_records (
  id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  loan_id           UUID NOT NULL REFERENCES loan_applications(id) ON DELETE CASCADE,
  org_id            UUID NOT NULL REFERENCES organisations(id),
  payment_date      DATE NOT NULL,
  amount_paid       NUMERIC(15,2) NOT NULL CHECK (amount_paid > 0),
  channel           TEXT NOT NULL CHECK (channel IN ('bank_transfer','cheque','cash','direct_debit','pos')),
  bank_ref          TEXT,
  recorded_by       UUID NOT NULL REFERENCES users(id),
  created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS ix_repayment_loan ON repayment_records (loan_id, payment_date DESC);
