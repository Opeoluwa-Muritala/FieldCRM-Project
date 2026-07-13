-- Replace the former committee workflow with the operational review chain.
-- Account Officer -> BM -> Branch Supervisor -> Credit Analyst -> CRM ->
-- Head CRM -> Audit -> ED -> (optional MD advice) -> CRM disbursement.

ALTER TABLE users DROP CONSTRAINT IF EXISTS users_role_check;
ALTER TABLE users ADD CONSTRAINT users_role_check CHECK (role IN (
  'account_officer','branch_manager','branch_supervisor','credit_analyst',
  'crm','head_crm','auditor','ed','md','system_admin'
));

ALTER TABLE loan_applications DROP CONSTRAINT IF EXISTS loan_applications_stage_check;
ALTER TABLE loan_applications ADD CONSTRAINT loan_applications_stage_check CHECK (stage IN (
  'intake','ocr_review','branch_manager_review','branch_supervisor_review',
  'credit_analyst_review','crm_review','head_crm_review','audit_review',
  'ed_approval','md_approval','disbursement_ready','disbursed','returned','rejected'
));
