-- FieldCRM PostgreSQL RLS security foundation.
-- Apply as the schema owner. The deployed application must connect as
-- fieldcrm_app (NOINHERIT, NOBYPASSRLS), never as the owner.

CREATE OR REPLACE FUNCTION public.app_setting_uuid(setting_name text)
RETURNS uuid
LANGUAGE plpgsql
STABLE
SET search_path = pg_catalog, public, pg_temp
AS $$
DECLARE raw_value text;
BEGIN
  raw_value := current_setting(setting_name, true);
  IF raw_value IS NULL OR raw_value = '' THEN
    RETURN NULL;
  END IF;
  RETURN raw_value::uuid;
EXCEPTION WHEN invalid_text_representation THEN
  RETURN NULL;
END;
$$;

CREATE OR REPLACE FUNCTION public.app_current_org_id() RETURNS uuid
LANGUAGE sql STABLE SET search_path = pg_catalog, public, pg_temp
RETURN public.app_setting_uuid('app.org_id');

CREATE OR REPLACE FUNCTION public.app_current_user_id() RETURNS uuid
LANGUAGE sql STABLE SET search_path = pg_catalog, public, pg_temp
RETURN public.app_setting_uuid('app.user_id');

CREATE OR REPLACE FUNCTION public.app_current_branch_id() RETURNS uuid
LANGUAGE sql STABLE SET search_path = pg_catalog, public, pg_temp
RETURN public.app_setting_uuid('app.branch_id');

CREATE OR REPLACE FUNCTION public.app_current_role() RETURNS text
LANGUAGE sql STABLE SET search_path = pg_catalog, public, pg_temp
RETURN NULLIF(current_setting('app.user_role', true), '');

CREATE OR REPLACE FUNCTION public.app_can_view_loan_values(
  loan_org uuid, loan_creator uuid, loan_branch uuid, loan_manager uuid
)
RETURNS boolean
LANGUAGE sql
STABLE
SET search_path = pg_catalog, public, pg_temp
AS $$
  SELECT loan_org = public.app_current_org_id()
    AND CASE public.app_current_role()
      WHEN 'account_officer' THEN loan_creator = public.app_current_user_id()
      WHEN 'branch_manager' THEN
        loan_branch = public.app_current_branch_id()
        OR loan_manager = public.app_current_user_id()
      WHEN 'branch_supervisor' THEN
        loan_branch IS NULL OR loan_branch = public.app_current_branch_id()
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

CREATE OR REPLACE FUNCTION public.app_can_mutate_loan_values(
  loan_org uuid, loan_creator uuid, loan_branch uuid, loan_manager uuid,
  loan_credit_officer uuid, loan_owner uuid, loan_stage text
)
RETURNS boolean
LANGUAGE sql
STABLE
SET search_path = pg_catalog, public, pg_temp
AS $$
  SELECT loan_org = public.app_current_org_id()
    AND CASE public.app_current_role()
      WHEN 'account_officer' THEN
        loan_creator = public.app_current_user_id() AND loan_stage = 'intake'
      WHEN 'branch_manager' THEN
        loan_manager = public.app_current_user_id()
        AND loan_branch = public.app_current_branch_id()
        AND loan_stage = 'branch_manager_review'
      WHEN 'branch_supervisor' THEN
        loan_branch = public.app_current_branch_id()
        AND loan_stage = 'branch_supervisor_review'
      WHEN 'credit_analyst' THEN
        (loan_credit_officer = public.app_current_user_id() OR loan_owner = public.app_current_user_id())
        AND loan_stage IN ('credit_analyst_review', 'credit_review')
      WHEN 'crm' THEN loan_stage = 'crm_review'
      WHEN 'head_crm' THEN loan_stage = 'head_crm_review'
      WHEN 'ed' THEN loan_stage IN ('ed_approval', 'executive_approval')
      WHEN 'md' THEN loan_stage = 'md_approval'
      WHEN 'legal' THEN loan_stage = 'legal_review'
      ELSE FALSE
    END;
$$;

CREATE OR REPLACE FUNCTION public.app_can_view_loan(target_loan uuid)
RETURNS boolean
LANGUAGE sql STABLE SET search_path = pg_catalog, public, pg_temp
AS $$
  SELECT EXISTS (
    SELECT 1 FROM public.loan_applications la
    WHERE la.id = target_loan AND la.deleted_at IS NULL
      AND public.app_can_view_loan_values(la.org_id, la.created_by, la.branch_id, la.branch_manager_id)
  );
$$;

CREATE OR REPLACE FUNCTION public.app_can_mutate_loan(target_loan uuid)
RETURNS boolean
LANGUAGE sql STABLE SET search_path = pg_catalog, public, pg_temp
AS $$
  SELECT EXISTS (
    SELECT 1 FROM public.loan_applications la
    WHERE la.id = target_loan AND la.deleted_at IS NULL
      AND public.app_can_mutate_loan_values(
        la.org_id, la.created_by, la.branch_id, la.branch_manager_id,
        la.credit_officer_id, la.current_owner_id, la.stage
      )
  );
$$;

CREATE OR REPLACE FUNCTION public.app_can_upload_to_loan(target_loan uuid, target_type text)
RETURNS boolean
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = pg_catalog, public, pg_temp
AS $$
  SELECT CASE
    WHEN target_type IN (
      'offer_acceptance','disbursement_mandate','direct_debit_mandate',
      'insurance_certificate','legal_clearance','other_crm','crm_memo'
    ) THEN public.app_current_role() IN ('crm', 'head_crm')
         AND public.app_can_view_loan(target_loan)
    ELSE public.app_can_mutate_loan(target_loan)
  END;
$$;

REVOKE ALL ON FUNCTION public.app_setting_uuid(text) FROM PUBLIC;
REVOKE ALL ON FUNCTION public.app_current_org_id() FROM PUBLIC;
REVOKE ALL ON FUNCTION public.app_current_user_id() FROM PUBLIC;
REVOKE ALL ON FUNCTION public.app_current_branch_id() FROM PUBLIC;
REVOKE ALL ON FUNCTION public.app_current_role() FROM PUBLIC;
REVOKE ALL ON FUNCTION public.app_can_view_loan_values(uuid,uuid,uuid,uuid) FROM PUBLIC;
REVOKE ALL ON FUNCTION public.app_can_mutate_loan_values(uuid,uuid,uuid,uuid,uuid,uuid,text) FROM PUBLIC;
REVOKE ALL ON FUNCTION public.app_can_view_loan(uuid) FROM PUBLIC;
REVOKE ALL ON FUNCTION public.app_can_mutate_loan(uuid) FROM PUBLIC;
REVOKE ALL ON FUNCTION public.app_can_upload_to_loan(uuid, text) FROM PUBLIC;

DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'fieldcrm_app') THEN
    ALTER ROLE fieldcrm_app NOINHERIT NOBYPASSRLS;
    GRANT EXECUTE ON FUNCTION public.app_setting_uuid(text) TO fieldcrm_app;
    GRANT EXECUTE ON FUNCTION public.app_current_org_id() TO fieldcrm_app;
    GRANT EXECUTE ON FUNCTION public.app_current_user_id() TO fieldcrm_app;
    GRANT EXECUTE ON FUNCTION public.app_current_branch_id() TO fieldcrm_app;
    GRANT EXECUTE ON FUNCTION public.app_current_role() TO fieldcrm_app;
    GRANT EXECUTE ON FUNCTION public.app_can_view_loan_values(uuid,uuid,uuid,uuid) TO fieldcrm_app;
    GRANT EXECUTE ON FUNCTION public.app_can_mutate_loan_values(uuid,uuid,uuid,uuid,uuid,uuid,text) TO fieldcrm_app;
    GRANT EXECUTE ON FUNCTION public.app_can_view_loan(uuid) TO fieldcrm_app;
    GRANT EXECUTE ON FUNCTION public.app_can_mutate_loan(uuid) TO fieldcrm_app;
    GRANT EXECUTE ON FUNCTION public.app_can_upload_to_loan(uuid, text) TO fieldcrm_app;
  END IF;
  IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'fieldcrm_worker') THEN
    ALTER ROLE fieldcrm_worker NOINHERIT NOBYPASSRLS;
  END IF;
END $$;

CREATE INDEX IF NOT EXISTS ix_loan_rls_org_created
  ON public.loan_applications(org_id, created_by, id) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS ix_loan_rls_org_branch
  ON public.loan_applications(org_id, branch_id, id) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS ix_loan_rls_org_manager_stage
  ON public.loan_applications(org_id, branch_manager_id, stage, id) WHERE deleted_at IS NULL;

ALTER TABLE public.loan_applications ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.loan_applications FORCE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS loan_select ON public.loan_applications;
DROP POLICY IF EXISTS loan_insert ON public.loan_applications;
DROP POLICY IF EXISTS loan_update ON public.loan_applications;
DROP POLICY IF EXISTS loan_delete ON public.loan_applications;
CREATE POLICY loan_select ON public.loan_applications FOR SELECT
  USING (
    deleted_at IS NULL
    AND public.app_can_view_loan_values(org_id, created_by, branch_id, branch_manager_id)
  );
CREATE POLICY loan_insert ON public.loan_applications FOR INSERT
  WITH CHECK (
    org_id = public.app_current_org_id()
    AND created_by = public.app_current_user_id()
    AND public.app_current_role() = 'account_officer'
    AND stage = 'intake'
  );
CREATE POLICY loan_update ON public.loan_applications FOR UPDATE
  USING (
    deleted_at IS NULL
    AND public.app_can_mutate_loan_values(
      org_id, created_by, branch_id, branch_manager_id,
      credit_officer_id, current_owner_id, stage
    )
  )
  WITH CHECK (
    org_id = public.app_current_org_id()
    AND (
      created_by = public.app_current_user_id()
      OR branch_id = public.app_current_branch_id()
      OR public.app_current_role() IN ('credit_analyst','crm','head_crm','ed','md','legal')
    )
  );
-- No DELETE policy: runtime deletion is denied by default.

-- Apply parent-loan policies to tables with a direct loan_id/application_id.
DO $$
DECLARE target record; link_column text; org_check text; read_expr text; write_expr text;
BEGIN
  FOR target IN
    SELECT t.table_name,
           bool_or(c.column_name = 'loan_id') AS has_loan_id,
           bool_or(c.column_name = 'application_id') AS has_application_id,
           bool_or(c.column_name = 'org_id') AS has_org_id
    FROM information_schema.tables t
    JOIN information_schema.columns c
      ON c.table_schema = t.table_schema AND c.table_name = t.table_name
    WHERE t.table_schema = 'public' AND t.table_type = 'BASE TABLE'
      AND t.table_name NOT IN (
        'loan_applications','documents','document_upload_intents','workflow_events',
        'audit_entries','notifications','users','organisations','auth_sessions',
        'refresh_tokens','password_reset_tokens','signing_sessions'
      )
    GROUP BY t.table_name
    HAVING bool_or(c.column_name = 'loan_id') OR bool_or(c.column_name = 'application_id')
  LOOP
    link_column := CASE WHEN target.has_loan_id THEN 'loan_id' ELSE 'application_id' END;
    org_check := CASE WHEN target.has_org_id THEN 'org_id = public.app_current_org_id() AND ' ELSE '' END;
    read_expr := org_check || format('public.app_can_view_loan(%I)', link_column);
    write_expr := org_check || format('public.app_can_mutate_loan(%I)', link_column);
    EXECUTE format('ALTER TABLE public.%I ENABLE ROW LEVEL SECURITY', target.table_name);
    EXECUTE format('ALTER TABLE public.%I FORCE ROW LEVEL SECURITY', target.table_name);
    EXECUTE format('DROP POLICY IF EXISTS fieldcrm_read ON public.%I', target.table_name);
    EXECUTE format('DROP POLICY IF EXISTS fieldcrm_insert ON public.%I', target.table_name);
    EXECUTE format('DROP POLICY IF EXISTS fieldcrm_update ON public.%I', target.table_name);
    EXECUTE format('CREATE POLICY fieldcrm_read ON public.%I FOR SELECT USING (%s)', target.table_name, read_expr);
    EXECUTE format('CREATE POLICY fieldcrm_insert ON public.%I FOR INSERT WITH CHECK (%s)', target.table_name, write_expr);
    EXECUTE format('CREATE POLICY fieldcrm_update ON public.%I FOR UPDATE USING (%s) WITH CHECK (%s)', target.table_name, write_expr, write_expr);
  END LOOP;
END $$;

ALTER TABLE public.documents ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.documents FORCE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS document_read ON public.documents;
DROP POLICY IF EXISTS document_insert ON public.documents;
CREATE POLICY document_read ON public.documents FOR SELECT
  USING (org_id = public.app_current_org_id() AND public.app_can_view_loan(loan_id));
CREATE POLICY document_insert ON public.documents FOR INSERT
  WITH CHECK (
    org_id = public.app_current_org_id()
    AND uploaded_by = public.app_current_user_id()
    AND public.app_can_upload_to_loan(loan_id, doc_type)
  );

ALTER TABLE public.document_upload_intents ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.document_upload_intents FORCE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS upload_intent_actor ON public.document_upload_intents;
CREATE POLICY upload_intent_actor ON public.document_upload_intents FOR ALL
  USING (
    organization_id = public.app_current_org_id()
    AND actor_id = public.app_current_user_id()
    AND actor_role = public.app_current_role()
    AND public.app_can_upload_to_loan(application_id, document_type)
  )
  WITH CHECK (
    organization_id = public.app_current_org_id()
    AND actor_id = public.app_current_user_id()
    AND actor_role = public.app_current_role()
    AND public.app_can_upload_to_loan(application_id, document_type)
  );

ALTER TABLE public.workflow_events ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.workflow_events FORCE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS workflow_read ON public.workflow_events;
DROP POLICY IF EXISTS workflow_insert ON public.workflow_events;
CREATE POLICY workflow_read ON public.workflow_events FOR SELECT
  USING (org_id = public.app_current_org_id() AND public.app_can_view_loan(loan_id));
CREATE POLICY workflow_insert ON public.workflow_events FOR INSERT
  WITH CHECK (
    org_id = public.app_current_org_id()
    AND triggered_by = public.app_current_user_id()
    AND public.app_can_view_loan(loan_id)
  );

ALTER TABLE public.audit_entries ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.audit_entries FORCE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS audit_read ON public.audit_entries;
DROP POLICY IF EXISTS audit_insert ON public.audit_entries;
CREATE POLICY audit_read ON public.audit_entries FOR SELECT
  USING (
    org_id = public.app_current_org_id()
    AND (
      public.app_current_role() = 'auditor'
      OR (entity_type = 'loan_application' AND public.app_can_view_loan(entity_id))
      OR (entity_type = 'document' AND EXISTS (
        SELECT 1 FROM public.documents d
        WHERE d.id = entity_id AND public.app_can_view_loan(d.loan_id)
      ))
    )
  );
CREATE POLICY audit_insert ON public.audit_entries FOR INSERT
  WITH CHECK (
    org_id = public.app_current_org_id()
    AND user_id = public.app_current_user_id()
    AND user_role = public.app_current_role()
  );

ALTER TABLE public.notifications ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.notifications FORCE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS notification_read ON public.notifications;
DROP POLICY IF EXISTS notification_insert ON public.notifications;
DROP POLICY IF EXISTS notification_update ON public.notifications;
CREATE POLICY notification_read ON public.notifications FOR SELECT
  USING (org_id = public.app_current_org_id() AND user_id = public.app_current_user_id());
CREATE POLICY notification_insert ON public.notifications FOR INSERT
  WITH CHECK (org_id = public.app_current_org_id());
CREATE POLICY notification_update ON public.notifications FOR UPDATE
  USING (org_id = public.app_current_org_id() AND user_id = public.app_current_user_id())
  WITH CHECK (org_id = public.app_current_org_id() AND user_id = public.app_current_user_id());

-- Indirect children that do not carry a direct application identifier.
DO $$
BEGIN
  IF to_regclass('public.field_edit_log') IS NOT NULL THEN
    ALTER TABLE public.field_edit_log ENABLE ROW LEVEL SECURITY;
    ALTER TABLE public.field_edit_log FORCE ROW LEVEL SECURITY;
    DROP POLICY IF EXISTS field_edit_parent ON public.field_edit_log;
    CREATE POLICY field_edit_parent ON public.field_edit_log FOR ALL
      USING (EXISTS (
        SELECT 1 FROM public.document_versions dv
        WHERE dv.id = document_version_id AND public.app_can_view_loan(dv.application_id)
      ))
      WITH CHECK (edited_by = public.app_current_user_id() AND EXISTS (
        SELECT 1 FROM public.document_versions dv
        WHERE dv.id = document_version_id AND public.app_can_mutate_loan(dv.application_id)
      ));
  END IF;
  IF to_regclass('public.collateral_documents') IS NOT NULL THEN
    ALTER TABLE public.collateral_documents ENABLE ROW LEVEL SECURITY;
    ALTER TABLE public.collateral_documents FORCE ROW LEVEL SECURITY;
    DROP POLICY IF EXISTS collateral_document_parent ON public.collateral_documents;
    CREATE POLICY collateral_document_parent ON public.collateral_documents FOR ALL
      USING (EXISTS (
        SELECT 1 FROM public.collateral_items ci
        WHERE ci.id = collateral_item_id AND public.app_can_view_loan(ci.application_id)
      ))
      WITH CHECK (uploaded_by = public.app_current_user_id() AND EXISTS (
        SELECT 1 FROM public.collateral_items ci
        WHERE ci.id = collateral_item_id AND public.app_can_mutate_loan(ci.application_id)
      ));
  END IF;
END $$;

-- History remains append-only even if a future policy is accidentally broadened.
REVOKE UPDATE, DELETE ON public.audit_entries, public.workflow_events FROM fieldcrm_app;
