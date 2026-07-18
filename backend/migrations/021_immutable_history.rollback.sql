-- Rollback only with an approved maintenance window.
DO $$
DECLARE table_name text;
BEGIN
  FOREACH table_name IN ARRAY ARRAY[
    'audit_entries', 'workflow_events', 'committee_votes',
    'verification_checks', 'bureau_submissions', 'sanctions_checks',
    'repayment_records'
  ]
  LOOP
    EXECUTE format('DROP TRIGGER IF EXISTS %I ON public.%I', table_name || '_append_only', table_name);
  END LOOP;
END $$;
DROP FUNCTION IF EXISTS public.prevent_history_mutation();
REVOKE SELECT, DELETE ON audit_entries, workflow_events, committee_votes,
  verification_checks, bureau_submissions, sanctions_checks, repayment_records
  FROM fieldcrm_audit_maintenance;
