-- Migration 009: Committee review, ED/MD split approval, board referrals
-- Run after 008_ocr_pdf.sql

-- =========================================================
-- 1. Add committee_review, ed_approval, md_approval stages
-- =========================================================
ALTER TABLE loan_applications
  DROP CONSTRAINT IF EXISTS loan_applications_stage_check;

ALTER TABLE loan_applications
  ADD CONSTRAINT loan_applications_stage_check CHECK (stage IN (
    'intake','ocr_review','credit_review','branch_approval',
    'crm_review','committee_review',
    'ed_approval','md_approval',
    'executive_approval',
    'disbursement_ready','disbursed','returned','rejected'
  ));

-- =========================================================
-- 2. Add committee role
-- =========================================================
ALTER TABLE users
  DROP CONSTRAINT IF EXISTS users_role_check;

ALTER TABLE users
  ADD CONSTRAINT users_role_check CHECK (role IN (
    'loan_officer','credit_officer','branch_manager',
    'auditor','system_admin','crm','md','ed','committee'
  ));

-- =========================================================
-- 3. Tracking columns on loan_applications
-- =========================================================
ALTER TABLE loan_applications
  ADD COLUMN IF NOT EXISTS committee_completed_at  TIMESTAMPTZ,
  ADD COLUMN IF NOT EXISTS committee_recommendation TEXT CHECK (committee_recommendation IN ('approve','return','reject')),
  ADD COLUMN IF NOT EXISTS ed_escalated_to_md      BOOLEAN NOT NULL DEFAULT FALSE,
  ADD COLUMN IF NOT EXISTS ed_approved_by          UUID REFERENCES users(id),
  ADD COLUMN IF NOT EXISTS ed_approved_at          TIMESTAMPTZ,
  ADD COLUMN IF NOT EXISTS md_approved_by          UUID REFERENCES users(id),
  ADD COLUMN IF NOT EXISTS md_approved_at          TIMESTAMPTZ,
  ADD COLUMN IF NOT EXISTS md_notes                TEXT;

-- =========================================================
-- 4. Committee votes table
-- =========================================================
CREATE TABLE IF NOT EXISTS committee_votes (
  id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  loan_id         UUID NOT NULL REFERENCES loan_applications(id) ON DELETE CASCADE,
  org_id          UUID NOT NULL REFERENCES organisations(id),
  member_id       UUID NOT NULL REFERENCES users(id),
  recommendation  TEXT NOT NULL CHECK (recommendation IN ('approve','return','reject')),
  notes           TEXT,
  voted_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  UNIQUE (loan_id, member_id)
);
CREATE INDEX IF NOT EXISTS ix_committee_votes_loan ON committee_votes (loan_id);
CREATE INDEX IF NOT EXISTS ix_committee_votes_org  ON committee_votes (org_id, loan_id);

-- =========================================================
-- 5. Board referrals table (MD ad-hoc referral to board members by email)
-- =========================================================
CREATE TABLE IF NOT EXISTS board_referrals (
  id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  loan_id             UUID NOT NULL REFERENCES loan_applications(id) ON DELETE CASCADE,
  org_id              UUID NOT NULL REFERENCES organisations(id),
  referred_by         UUID NOT NULL REFERENCES users(id),
  board_member_email  TEXT NOT NULL,
  board_member_name   TEXT,
  notes               TEXT,
  status              TEXT NOT NULL DEFAULT 'pending' CHECK (status IN ('pending','responded','closed')),
  created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS ix_board_referrals_loan ON board_referrals (loan_id);
