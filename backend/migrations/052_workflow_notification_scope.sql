-- Allow workflow notifications to target the next role after a transition.
-- Keep both recipient and application strictly tenant-scoped.
CREATE OR REPLACE FUNCTION public.app_application_in_current_org(target_application uuid)
RETURNS boolean LANGUAGE sql STABLE SECURITY DEFINER
SET search_path = pg_catalog, public, pg_temp
AS $$
  SELECT EXISTS (
    SELECT 1 FROM public.loan_applications la
    WHERE la.id = target_application
      AND la.org_id = public.app_current_org_id()
      AND la.deleted_at IS NULL
  );
$$;
REVOKE ALL ON FUNCTION public.app_application_in_current_org(uuid) FROM PUBLIC;
DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'fieldcrm_app') THEN
    GRANT EXECUTE ON FUNCTION public.app_application_in_current_org(uuid) TO fieldcrm_app;
  END IF;
END $$;
ALTER TABLE public.notifications ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.notifications FORCE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS notification_insert ON public.notifications;
CREATE POLICY notification_insert ON public.notifications FOR INSERT
  WITH CHECK (
    org_id = public.app_current_org_id()
    AND public.app_user_in_current_org(user_id)
    AND (application_id IS NULL OR public.app_application_in_current_org(application_id))
  );
