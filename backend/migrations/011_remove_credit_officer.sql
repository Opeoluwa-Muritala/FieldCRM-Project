-- Migration 011: Remove credit_officer role from the system.
-- Workflow is: Loan Officer → Branch Manager → CRM → Committee → ED → MD.
-- Run after 010_seed_new_roles.sql

-- =========================================================
-- 1. Migrate any existing credit_officer users to loan_officer
-- =========================================================
UPDATE users
SET role = 'loan_officer'
WHERE role = 'credit_officer';

-- =========================================================
-- 2. Update users role CHECK constraint — drop credit_officer
-- =========================================================
ALTER TABLE users
  DROP CONSTRAINT IF EXISTS users_role_check;

ALTER TABLE users
  ADD CONSTRAINT users_role_check CHECK (role IN (
    'loan_officer','branch_manager',
    'auditor','system_admin','crm','md','ed','committee'
  ));

-- =========================================================
-- 3. Remove credit_review stage from loan_applications
--    (loans stuck in credit_review are returned to branch_approval
--     so they can be re-routed through the new workflow)
-- =========================================================
UPDATE loan_applications
SET stage = 'branch_approval',
    updated_at = NOW()
WHERE stage = 'credit_review';

-- =========================================================
-- 4. Update loan_applications stage CHECK constraint
--    — remove credit_review stage
-- =========================================================
ALTER TABLE loan_applications
  DROP CONSTRAINT IF EXISTS loan_applications_stage_check;

ALTER TABLE loan_applications
  ADD CONSTRAINT loan_applications_stage_check CHECK (stage IN (
    'intake','ocr_review','branch_approval',
    'crm_review','committee_review',
    'ed_approval','md_approval',
    'executive_approval',
    'disbursement_ready','disbursed','returned','rejected'
  ));
