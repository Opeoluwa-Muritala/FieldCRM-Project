DROP TABLE IF EXISTS field_value_metadata;
DROP TABLE IF EXISTS core_banking_reconciliation_issues;
DROP TABLE IF EXISTS core_banking_sync_runs;
DROP TABLE IF EXISTS core_banking_schedule;
DROP TRIGGER IF EXISTS core_banking_transactions_append_only ON core_banking_transactions;
DROP TABLE IF EXISTS core_banking_transactions;
DROP FUNCTION IF EXISTS prevent_cbs_transaction_mutation();
DROP TABLE IF EXISTS core_banking_loan_snapshots;

DROP INDEX IF EXISTS ux_loan_external_cbs_id;
ALTER TABLE loan_applications
  DROP COLUMN IF EXISTS cbs_sync_error,
  DROP COLUMN IF EXISTS cbs_sync_status,
  DROP COLUMN IF EXISTS cbs_last_successful_sync_at,
  DROP COLUMN IF EXISTS cbs_provider,
  DROP COLUMN IF EXISTS external_loan_id,
  DROP COLUMN IF EXISTS external_customer_id;
ALTER TABLE loan_products DROP COLUMN IF EXISTS cbs_enabled;
