DROP INDEX IF EXISTS uq_loan_applications_org_client_request;
ALTER TABLE loan_applications DROP COLUMN IF EXISTS client_request_id;
