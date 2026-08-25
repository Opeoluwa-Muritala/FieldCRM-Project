-- Institution-wide supervisors must be able to advance/return files from
-- any branch while remaining tenant-scoped and stage-scoped.
CREATE OR REPLACE FUNCTION public.app_can_mutate_loan_values(
  loan_org uuid, loan_creator uuid, loan_branch uuid, loan_manager uuid,
  loan_credit_officer uuid, loan_owner uuid, loan_stage text
)
RETURNS boolean
LANGUAGE sql STABLE
SET search_path = pg_catalog, public, pg_temp
AS $$
  SELECT loan_org = public.app_current_org_id()
    AND CASE public.app_current_role()
      WHEN 'account_officer' THEN loan_creator = public.app_current_user_id() AND loan_stage = 'intake'
      WHEN 'branch_manager' THEN loan_branch = public.app_current_branch_id() AND loan_stage = 'branch_manager_review'
      WHEN 'branch_supervisor' THEN (public.app_current_branch_id() IS NULL OR loan_branch = public.app_current_branch_id())
        AND loan_stage = 'branch_supervisor_review'
      WHEN 'credit_analyst' THEN (loan_credit_officer = public.app_current_user_id() OR loan_owner = public.app_current_user_id())
        AND loan_stage IN ('credit_analyst_review', 'credit_review')
      WHEN 'crm' THEN loan_stage = 'crm_review'
      WHEN 'head_crm' THEN loan_stage = 'head_crm_review'
      WHEN 'ed' THEN loan_stage IN ('ed_approval', 'executive_approval')
      WHEN 'md' THEN loan_stage = 'md_approval'
      WHEN 'legal' THEN loan_stage = 'legal_review'
      ELSE FALSE
    END;
$$;
