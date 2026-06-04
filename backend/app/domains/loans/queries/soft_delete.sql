-- loans/queries/soft_delete.sql
-- Soft-deletes a loan application.
-- Params: $1=loan_id, $2=org_id

UPDATE loan_applications
SET deleted_at = NOW()
WHERE id        = $1
  AND org_id    = $2
  AND deleted_at IS NULL
RETURNING id;
