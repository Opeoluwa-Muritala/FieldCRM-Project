-- Reversible support for exactly-once replay of offline mobile application creation.
ALTER TABLE loan_applications
    ADD COLUMN IF NOT EXISTS client_request_id UUID;

CREATE UNIQUE INDEX IF NOT EXISTS uq_loan_applications_org_client_request
    ON loan_applications (org_id, client_request_id)
    WHERE client_request_id IS NOT NULL AND deleted_at IS NULL;
