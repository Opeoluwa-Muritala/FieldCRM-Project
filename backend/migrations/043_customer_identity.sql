-- Phase 2: staff-only customer identity and duplicate-control domain.

CREATE TABLE IF NOT EXISTS customers (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  org_id UUID NOT NULL REFERENCES organisations(id) ON DELETE CASCADE,
  customer_number TEXT NOT NULL,
  legal_name TEXT NOT NULL,
  normalized_name TEXT NOT NULL,
  name_signature TEXT NOT NULL,
  date_of_birth DATE,
  phone_encrypted TEXT,
  phone_lookup_hash TEXT,
  email_encrypted TEXT,
  email_lookup_hash TEXT,
  bvn_encrypted TEXT,
  bvn_lookup_hash TEXT,
  nin_encrypted TEXT,
  nin_lookup_hash TEXT,
  residential_address TEXT,
  normalized_address TEXT,
  business_name TEXT,
  external_customer_id TEXT,
  cbs_provider TEXT,
  relationship_officer_id UUID REFERENCES users(id) ON DELETE SET NULL,
  branch_id UUID REFERENCES branches(id) ON DELETE SET NULL,
  created_by UUID NOT NULL REFERENCES users(id),
  active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  UNIQUE (org_id, customer_number)
);

CREATE INDEX IF NOT EXISTS ix_customers_org_name ON customers (org_id, normalized_name);
CREATE INDEX IF NOT EXISTS ix_customers_org_phone_hash ON customers (org_id, phone_lookup_hash) WHERE phone_lookup_hash IS NOT NULL;
CREATE INDEX IF NOT EXISTS ix_customers_org_bvn_hash ON customers (org_id, bvn_lookup_hash) WHERE bvn_lookup_hash IS NOT NULL;
CREATE INDEX IF NOT EXISTS ix_customers_org_nin_hash ON customers (org_id, nin_lookup_hash) WHERE nin_lookup_hash IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS ux_customers_org_cbs
  ON customers (org_id, cbs_provider, external_customer_id)
  WHERE external_customer_id IS NOT NULL;

CREATE TABLE IF NOT EXISTS customer_accounts (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  customer_id UUID NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
  org_id UUID NOT NULL REFERENCES organisations(id) ON DELETE CASCADE,
  account_number_encrypted TEXT NOT NULL,
  account_number_lookup_hash TEXT NOT NULL,
  bank_name TEXT,
  is_primary BOOLEAN NOT NULL DEFAULT FALSE,
  source TEXT NOT NULL DEFAULT 'manual_web',
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  UNIQUE (org_id, account_number_lookup_hash)
);

ALTER TABLE loan_applications
  ADD COLUMN IF NOT EXISTS customer_id UUID REFERENCES customers(id) ON DELETE SET NULL;
CREATE INDEX IF NOT EXISTS ix_loan_customer ON loan_applications (customer_id, updated_at DESC);

CREATE TABLE IF NOT EXISTS customer_duplicate_overrides (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  org_id UUID NOT NULL REFERENCES organisations(id) ON DELETE CASCADE,
  customer_id UUID NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
  probable_duplicate_id UUID NOT NULL REFERENCES customers(id),
  matched_rules TEXT[] NOT NULL,
  override_reason TEXT NOT NULL CHECK (char_length(trim(override_reason)) >= 10),
  overridden_by UUID NOT NULL REFERENCES users(id),
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS customer_activity (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  org_id UUID NOT NULL REFERENCES organisations(id) ON DELETE CASCADE,
  customer_id UUID NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
  application_id UUID REFERENCES loan_applications(id) ON DELETE SET NULL,
  event_type TEXT NOT NULL CHECK (event_type IN (
    'created','edited','submitted','returned','document_uploaded','visit_completed',
    'credit_reviewed','approved','cbs_sync','repayment_detected','collection_action',
    'application_linked','configuration_applied','workflow_transition'
  )),
  actor_id UUID REFERENCES users(id) ON DELETE SET NULL,
  source TEXT NOT NULL,
  summary TEXT NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS ix_customer_activity_timeline
  ON customer_activity (org_id, customer_id, created_at DESC);

CREATE OR REPLACE FUNCTION prevent_customer_history_mutation()
RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
  RAISE EXCEPTION 'Customer history is append-only';
END;
$$;
CREATE TRIGGER customer_activity_append_only BEFORE UPDATE OR DELETE ON customer_activity
  FOR EACH ROW EXECUTE FUNCTION prevent_customer_history_mutation();
CREATE TRIGGER customer_duplicate_overrides_append_only BEFORE UPDATE OR DELETE ON customer_duplicate_overrides
  FOR EACH ROW EXECUTE FUNCTION prevent_customer_history_mutation();

DO $$
DECLARE table_name text;
BEGIN
  FOREACH table_name IN ARRAY ARRAY['customers','customer_accounts','customer_duplicate_overrides','customer_activity'] LOOP
    EXECUTE format('ALTER TABLE public.%I ENABLE ROW LEVEL SECURITY', table_name);
    EXECUTE format(
      'CREATE POLICY %I ON public.%I USING (org_id = NULLIF(current_setting(''app.org_id'', true), '''')::uuid) WITH CHECK (org_id = NULLIF(current_setting(''app.org_id'', true), '''')::uuid)',
      table_name || '_tenant', table_name
    );
  END LOOP;
END $$;
DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname='fieldcrm_app') THEN
    GRANT SELECT, INSERT, UPDATE ON customers, customer_accounts TO fieldcrm_app;
    GRANT SELECT, INSERT ON customer_duplicate_overrides, customer_activity TO fieldcrm_app;
  END IF;
END $$;
