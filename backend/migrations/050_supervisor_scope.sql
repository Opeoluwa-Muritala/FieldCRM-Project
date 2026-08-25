-- Some production supervisors are institution-wide (branch_id IS NULL). They
-- must be able to read every branch's supervisory queue while remaining
-- constrained to the current tenant.
CREATE OR REPLACE FUNCTION public.app_can_view_loan_values(
  loan_org uuid, loan_creator uuid, loan_branch uuid, loan_manager uuid
)
RETURNS boolean
LANGUAGE sql STABLE
SET search_path = pg_catalog, public, pg_temp
AS $$
  SELECT loan_org = public.app_current_org_id()
    AND CASE public.app_current_role()
      WHEN 'account_officer' THEN loan_creator = public.app_current_user_id()
      WHEN 'branch_manager' THEN loan_branch = public.app_current_branch_id()
        OR loan_manager = public.app_current_user_id()
      WHEN 'branch_supervisor' THEN public.app_current_branch_id() IS NULL
        OR loan_branch IS NULL OR loan_branch = public.app_current_branch_id()
      WHEN 'credit_analyst' THEN TRUE
      WHEN 'crm' THEN TRUE
      WHEN 'head_crm' THEN TRUE
      WHEN 'auditor' THEN TRUE
      WHEN 'ed' THEN TRUE
      WHEN 'md' THEN TRUE
      WHEN 'legal' THEN TRUE
      ELSE FALSE
    END;
$$;
