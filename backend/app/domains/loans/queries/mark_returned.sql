-- loans/queries/mark_returned.sql
-- Marks a loan application as returned with a reason.
-- Params: $1=loan_id, $2=org_id, $3=return_reason, $4=returned_by

UPDATE loan_applications
SET
    stage         = 'returned',
    return_reason = $3,
    returned_at   = NOW(),
    current_owner_id = $4,
    updated_at    = NOW()
WHERE id         = $1
  AND org_id     = $2
  AND deleted_at IS NULL
RETURNING id;
