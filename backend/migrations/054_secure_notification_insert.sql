-- Internal workflow notifications are written through a constrained,
-- tenant-validated function. This avoids relying on request-local RLS
-- settings during nested audit work while preserving object isolation.
CREATE OR REPLACE FUNCTION public.app_create_notification(
  target_user_id uuid,
  target_org_id uuid,
  target_application_id uuid,
  target_title text,
  target_message text,
  target_type text
)
RETURNS TABLE (
  id uuid,
  title text,
  message text,
  created_at timestamptz,
  is_read boolean,
  application_id uuid,
  type text
)
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = pg_catalog, public, pg_temp
AS $$
BEGIN
  IF target_org_id IS NULL OR NOT EXISTS (
    SELECT 1 FROM public.organisations o WHERE o.id = target_org_id
  ) THEN
    RAISE EXCEPTION 'invalid notification organisation';
  END IF;
  IF NOT EXISTS (
    SELECT 1 FROM public.users u
    WHERE u.id = target_user_id
      AND u.org_id = target_org_id
      AND u.deleted_at IS NULL
  ) THEN
    RAISE EXCEPTION 'invalid notification recipient';
  END IF;
  IF target_application_id IS NOT NULL AND NOT EXISTS (
    SELECT 1 FROM public.loan_applications la
    WHERE la.id = target_application_id
      AND la.org_id = target_org_id
      AND la.deleted_at IS NULL
  ) THEN
    RAISE EXCEPTION 'invalid notification application';
  END IF;

  RETURN QUERY
  INSERT INTO public.notifications (user_id, org_id, application_id, title, message, type, is_read)
  VALUES (target_user_id, target_org_id, target_application_id, target_title, target_message, target_type, FALSE)
  RETURNING notifications.id, notifications.title, notifications.message,
            notifications.created_at, notifications.is_read,
            notifications.application_id, notifications.type;
END;
$$;
REVOKE ALL ON FUNCTION public.app_create_notification(uuid, uuid, uuid, text, text, text) FROM PUBLIC;
DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'fieldcrm_app') THEN
    GRANT EXECUTE ON FUNCTION public.app_create_notification(uuid, uuid, uuid, text, text, text) TO fieldcrm_app;
  END IF;
END $$;
