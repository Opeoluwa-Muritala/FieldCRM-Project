-- Immutable audit/history records for FieldCRM.
--
-- Ownership remains with neondb_owner.  The Vercel application must connect
-- only as fieldcrm_app, which receives privileges but never ownership.
-- Create/update the two login roles separately before this migration:
--   CREATE ROLE fieldcrm_app LOGIN NOINHERIT PASSWORD '<strong password>';
--   CREATE ROLE fieldcrm_audit_maintenance LOGIN NOINHERIT PASSWORD '<strong password>';
-- Never configure fieldcrm_audit_maintenance in Vercel.

GRANT USAGE ON SCHEMA public TO fieldcrm_app;
GRANT USAGE ON SCHEMA public TO fieldcrm_audit_maintenance;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO fieldcrm_app;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO fieldcrm_app;

ALTER DEFAULT PRIVILEGES IN SCHEMA public
  GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO fieldcrm_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA public
  GRANT USAGE, SELECT ON SEQUENCES TO fieldcrm_app;

CREATE OR REPLACE FUNCTION public.prevent_history_mutation()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
  IF current_user = 'fieldcrm_audit_maintenance' THEN
    RETURN COALESCE(NEW, OLD);
  END IF;
  RAISE EXCEPTION '% is append-only', TG_TABLE_NAME USING ERRCODE = '55000';
END;
$$;

DO $$
DECLARE table_name text;
BEGIN
  FOREACH table_name IN ARRAY ARRAY[
    'audit_entries', 'workflow_events', 'committee_votes',
    'verification_checks', 'bureau_submissions', 'sanctions_checks',
    'repayment_records'
  ]
  LOOP
    IF to_regclass('public.' || table_name) IS NOT NULL THEN
      EXECUTE format('REVOKE UPDATE, DELETE ON public.%I FROM fieldcrm_app', table_name);
      EXECUTE format('GRANT SELECT, INSERT ON public.%I TO fieldcrm_app', table_name);
      EXECUTE format('GRANT SELECT, DELETE ON public.%I TO fieldcrm_audit_maintenance', table_name);
      EXECUTE format('DROP TRIGGER IF EXISTS %I ON public.%I', table_name || '_append_only', table_name);
      EXECUTE format(
        'CREATE TRIGGER %I BEFORE UPDATE OR DELETE ON public.%I '
        'FOR EACH ROW EXECUTE FUNCTION public.prevent_history_mutation()',
        table_name || '_append_only', table_name
      );
    END IF;
  END LOOP;
END $$;

-- Verification after switching Vercel DATABASE_URL to fieldcrm_app:
--   UPDATE audit_entries SET notes = 'test' WHERE false; -- permission denied
--   DELETE FROM workflow_events WHERE false;              -- permission denied
