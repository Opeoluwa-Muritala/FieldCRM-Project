DROP TRIGGER IF EXISTS customer_duplicate_overrides_append_only ON customer_duplicate_overrides;
DROP TRIGGER IF EXISTS customer_activity_append_only ON customer_activity;
DROP FUNCTION IF EXISTS prevent_customer_history_mutation();
DROP TABLE IF EXISTS customer_activity;
DROP TABLE IF EXISTS customer_duplicate_overrides;
DROP INDEX IF EXISTS ix_loan_customer;
ALTER TABLE loan_applications DROP COLUMN IF EXISTS customer_id;
DROP TABLE IF EXISTS customer_accounts;
DROP TABLE IF EXISTS customers;
