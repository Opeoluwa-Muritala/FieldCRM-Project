-- Phase 3: restricted, versioned Configuration Admin control plane.
ALTER TABLE users DROP CONSTRAINT IF EXISTS users_role_check;
ALTER TABLE users ADD CONSTRAINT users_role_check CHECK (role IN (
  'account_officer','branch_manager','branch_supervisor','credit_analyst',
  'crm','head_crm','auditor','ed','md','system_admin','legal','configuration_admin'
));

ALTER TABLE users ADD COLUMN IF NOT EXISTS config_mfa_secret_encrypted TEXT;
ALTER TABLE users ADD COLUMN IF NOT EXISTS config_mfa_enabled BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE configuration_versions (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  org_id UUID NOT NULL REFERENCES organisations(id) ON DELETE CASCADE,
  version_number INTEGER NOT NULL,
  status TEXT NOT NULL DEFAULT 'draft' CHECK (status IN ('draft','validated','pending_approval','published','superseded')),
  payload JSONB NOT NULL,
  reason TEXT NOT NULL,
  effective_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  high_risk BOOLEAN NOT NULL DEFAULT FALSE,
  requires_second_approval BOOLEAN NOT NULL DEFAULT FALSE,
  created_by UUID NOT NULL REFERENCES users(id),
  validated_by UUID REFERENCES users(id),
  approved_by UUID REFERENCES users(id),
  published_by UUID REFERENCES users(id),
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  validated_at TIMESTAMPTZ,
  approved_at TIMESTAMPTZ,
  published_at TIMESTAMPTZ,
  UNIQUE (org_id, version_number)
);

CREATE INDEX configuration_versions_effective_idx
  ON configuration_versions (org_id, status, effective_at DESC, version_number DESC);

CREATE TABLE configuration_change_log (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  org_id UUID NOT NULL REFERENCES organisations(id) ON DELETE CASCADE,
  version_id UUID NOT NULL REFERENCES configuration_versions(id) ON DELETE RESTRICT,
  setting_path TEXT NOT NULL,
  old_value JSONB,
  new_value JSONB,
  changed_by UUID NOT NULL REFERENCES users(id),
  reason TEXT NOT NULL,
  approver_id UUID REFERENCES users(id),
  changed_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

ALTER TABLE loan_applications
  ADD COLUMN IF NOT EXISTS originated_config_version_id UUID REFERENCES configuration_versions(id) ON DELETE RESTRICT;

ALTER TABLE configuration_versions ENABLE ROW LEVEL SECURITY;
ALTER TABLE configuration_versions FORCE ROW LEVEL SECURITY;
ALTER TABLE configuration_change_log ENABLE ROW LEVEL SECURITY;
ALTER TABLE configuration_change_log FORCE ROW LEVEL SECURITY;

CREATE POLICY configuration_versions_tenant ON configuration_versions
  USING (org_id = NULLIF(current_setting('app.current_org_id', TRUE), '')::uuid)
  WITH CHECK (org_id = NULLIF(current_setting('app.current_org_id', TRUE), '')::uuid);
CREATE POLICY configuration_change_log_tenant ON configuration_change_log
  USING (org_id = NULLIF(current_setting('app.current_org_id', TRUE), '')::uuid)
  WITH CHECK (org_id = NULLIF(current_setting('app.current_org_id', TRUE), '')::uuid);

CREATE OR REPLACE FUNCTION reject_configuration_history_mutation() RETURNS trigger AS $$
BEGIN
  RAISE EXCEPTION 'Published configuration history is immutable';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER configuration_change_log_immutable
  BEFORE UPDATE OR DELETE ON configuration_change_log
  FOR EACH ROW EXECUTE FUNCTION reject_configuration_history_mutation();

CREATE OR REPLACE FUNCTION protect_published_configuration() RETURNS trigger AS $$
BEGIN
  IF OLD.status IN ('published','superseded') THEN
    RAISE EXCEPTION 'Published configuration versions are immutable';
  END IF;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER configuration_versions_published_immutable
  BEFORE UPDATE OR DELETE ON configuration_versions
  FOR EACH ROW EXECUTE FUNCTION protect_published_configuration();

DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'fieldcrm_app') THEN
    GRANT SELECT, INSERT, UPDATE, DELETE ON configuration_versions TO fieldcrm_app;
    GRANT SELECT, INSERT ON configuration_change_log TO fieldcrm_app;
  END IF;
END $$;
