-- Phase 1: opt-in Core Banking source-of-truth storage.
-- All existing products remain local/manual because cbs_enabled defaults false.

ALTER TABLE loan_products
  ADD COLUMN IF NOT EXISTS cbs_enabled BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE loan_applications
  ADD COLUMN IF NOT EXISTS external_customer_id TEXT,
  ADD COLUMN IF NOT EXISTS external_loan_id TEXT,
  ADD COLUMN IF NOT EXISTS cbs_provider TEXT,
  ADD COLUMN IF NOT EXISTS cbs_last_successful_sync_at TIMESTAMPTZ,
  ADD COLUMN IF NOT EXISTS cbs_sync_status TEXT NOT NULL DEFAULT 'not_configured',
  ADD COLUMN IF NOT EXISTS cbs_sync_error TEXT;

CREATE UNIQUE INDEX IF NOT EXISTS ux_loan_external_cbs_id
  ON loan_applications (org_id, cbs_provider, external_loan_id)
  WHERE external_loan_id IS NOT NULL AND deleted_at IS NULL;

CREATE TABLE IF NOT EXISTS core_banking_loan_snapshots (
  loan_id UUID PRIMARY KEY REFERENCES loan_applications(id) ON DELETE CASCADE,
  org_id UUID NOT NULL REFERENCES organisations(id) ON DELETE CASCADE,
  provider TEXT NOT NULL,
  external_loan_id TEXT NOT NULL,
  outstanding_balance NUMERIC(15,2) NOT NULL CHECK (outstanding_balance >= 0),
  principal_balance NUMERIC(15,2) NOT NULL CHECK (principal_balance >= 0),
  arrears_amount NUMERIC(15,2) NOT NULL CHECK (arrears_amount >= 0),
  days_past_due INTEGER NOT NULL CHECK (days_past_due >= 0),
  loan_status TEXT NOT NULL,
  disbursed_amount NUMERIC(15,2) CHECK (disbursed_amount >= 0),
  disbursed_at TIMESTAMPTZ,
  source_updated_at TIMESTAMPTZ NOT NULL,
  received_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  UNIQUE (org_id, provider, external_loan_id)
);

CREATE INDEX IF NOT EXISTS ix_cbs_snapshot_org_dpd
  ON core_banking_loan_snapshots (org_id, days_past_due DESC);

CREATE TABLE IF NOT EXISTS core_banking_transactions (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  loan_id UUID NOT NULL REFERENCES loan_applications(id) ON DELETE CASCADE,
  org_id UUID NOT NULL REFERENCES organisations(id) ON DELETE CASCADE,
  provider TEXT NOT NULL,
  external_transaction_id TEXT NOT NULL,
  transaction_type TEXT NOT NULL,
  amount NUMERIC(15,2) NOT NULL CHECK (amount > 0),
  transaction_at TIMESTAMPTZ NOT NULL,
  value_date DATE,
  currency CHAR(3) NOT NULL DEFAULT 'NGN',
  source_updated_at TIMESTAMPTZ NOT NULL,
  received_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  UNIQUE (org_id, provider, external_transaction_id)
);

CREATE INDEX IF NOT EXISTS ix_cbs_transactions_loan_date
  ON core_banking_transactions (loan_id, transaction_at DESC);

CREATE TABLE IF NOT EXISTS core_banking_schedule (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  loan_id UUID NOT NULL REFERENCES loan_applications(id) ON DELETE CASCADE,
  org_id UUID NOT NULL REFERENCES organisations(id) ON DELETE CASCADE,
  provider TEXT NOT NULL,
  external_installment_id TEXT NOT NULL,
  installment_no INTEGER NOT NULL CHECK (installment_no > 0),
  due_date DATE NOT NULL,
  principal_due NUMERIC(15,2) NOT NULL CHECK (principal_due >= 0),
  interest_due NUMERIC(15,2) NOT NULL CHECK (interest_due >= 0),
  total_due NUMERIC(15,2) NOT NULL CHECK (total_due >= 0),
  amount_paid NUMERIC(15,2) NOT NULL DEFAULT 0 CHECK (amount_paid >= 0),
  status TEXT NOT NULL,
  source_updated_at TIMESTAMPTZ NOT NULL,
  received_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  UNIQUE (org_id, provider, external_installment_id),
  UNIQUE (loan_id, installment_no)
);

CREATE TABLE IF NOT EXISTS core_banking_sync_runs (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  loan_id UUID REFERENCES loan_applications(id) ON DELETE SET NULL,
  org_id UUID NOT NULL REFERENCES organisations(id) ON DELETE CASCADE,
  provider TEXT NOT NULL,
  trigger_type TEXT NOT NULL CHECK (trigger_type IN ('manual','scheduled','webhook')),
  status TEXT NOT NULL CHECK (status IN ('started','success','failed','unmatched_customer','unmatched_loan')),
  external_event_id TEXT,
  transactions_imported INTEGER NOT NULL DEFAULT 0,
  schedules_imported INTEGER NOT NULL DEFAULT 0,
  error_code TEXT,
  error_message TEXT,
  requested_by UUID REFERENCES users(id) ON DELETE SET NULL,
  started_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  completed_at TIMESTAMPTZ
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_cbs_sync_event
  ON core_banking_sync_runs (org_id, provider, external_event_id)
  WHERE external_event_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS ix_cbs_sync_org_date
  ON core_banking_sync_runs (org_id, started_at DESC);

CREATE TABLE IF NOT EXISTS core_banking_reconciliation_issues (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  loan_id UUID REFERENCES loan_applications(id) ON DELETE SET NULL,
  org_id UUID NOT NULL REFERENCES organisations(id) ON DELETE CASCADE,
  provider TEXT NOT NULL,
  issue_type TEXT NOT NULL CHECK (issue_type IN ('unmatched_customer','unmatched_loan','transaction_conflict','sync_failed')),
  external_reference TEXT,
  details TEXT NOT NULL,
  status TEXT NOT NULL DEFAULT 'open' CHECK (status IN ('open','resolved','ignored')),
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  resolved_at TIMESTAMPTZ,
  resolved_by UUID REFERENCES users(id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS ix_cbs_reconciliation_open
  ON core_banking_reconciliation_issues (org_id, status, created_at DESC);

CREATE TABLE IF NOT EXISTS field_value_metadata (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  org_id UUID NOT NULL REFERENCES organisations(id) ON DELETE CASCADE,
  entity_type TEXT NOT NULL,
  entity_id UUID NOT NULL,
  field_name TEXT NOT NULL,
  source TEXT NOT NULL CHECK (source IN (
    'manual_web','manual_android','cbs','ocr','bvn','nin',
    'credit_bureau','imported_file','system_calculated'
  )),
  captured_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  captured_by UUID REFERENCES users(id) ON DELETE SET NULL,
  verified BOOLEAN NOT NULL DEFAULT FALSE,
  verified_by UUID REFERENCES users(id) ON DELETE SET NULL,
  verified_at TIMESTAMPTZ,
  verification_source TEXT,
  UNIQUE (org_id, entity_type, entity_id, field_name)
);

CREATE OR REPLACE FUNCTION prevent_cbs_transaction_mutation()
RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
  RAISE EXCEPTION 'CBS transactions are append-only';
END;
$$;

DROP TRIGGER IF EXISTS core_banking_transactions_append_only ON core_banking_transactions;
CREATE TRIGGER core_banking_transactions_append_only
BEFORE UPDATE OR DELETE ON core_banking_transactions
FOR EACH ROW EXECUTE FUNCTION prevent_cbs_transaction_mutation();

DO $$
DECLARE table_name text;
BEGIN
  FOREACH table_name IN ARRAY ARRAY[
    'core_banking_loan_snapshots', 'core_banking_transactions',
    'core_banking_schedule', 'core_banking_sync_runs',
    'core_banking_reconciliation_issues', 'field_value_metadata'
  ] LOOP
    EXECUTE format('ALTER TABLE public.%I ENABLE ROW LEVEL SECURITY', table_name);
    EXECUTE format('DROP POLICY IF EXISTS %I ON public.%I', table_name || '_tenant', table_name);
    EXECUTE format(
      'CREATE POLICY %I ON public.%I USING (org_id = NULLIF(current_setting(''app.org_id'', true), '''')::uuid) WITH CHECK (org_id = NULLIF(current_setting(''app.org_id'', true), '''')::uuid)',
      table_name || '_tenant', table_name
    );
  END LOOP;
END $$;

DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'fieldcrm_app') THEN
    GRANT SELECT, INSERT, UPDATE ON
      core_banking_loan_snapshots, core_banking_schedule,
      core_banking_sync_runs, core_banking_reconciliation_issues,
      field_value_metadata
    TO fieldcrm_app;
    GRANT SELECT, INSERT ON core_banking_transactions TO fieldcrm_app;
  END IF;
END $$;
