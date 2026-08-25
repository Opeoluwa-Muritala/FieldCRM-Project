-- Keep workflow notifications tenant/application scoped without depending on
-- recipient lifecycle state.  Workflow transitions may notify a role holder
-- whose user record is being changed concurrently; the application endpoint
-- already resolves an authorized recipient from the same tenant.
ALTER TABLE public.notifications ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.notifications FORCE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS notification_insert ON public.notifications;
CREATE POLICY notification_insert ON public.notifications FOR INSERT
  WITH CHECK (
    org_id = public.app_current_org_id()
    AND (application_id IS NULL OR public.app_application_in_current_org(application_id))
  );
