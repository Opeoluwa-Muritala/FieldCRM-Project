-- Update loan classification and days past due.
-- Params: $1=classification, $2=days_past_due, $3=loan_id, $4=org_id

UPDATE loan_applications
SET
    classification            = $1,
    days_past_due             = $2,
    classification_updated_at = NOW(),
    updated_at                = NOW()
WHERE id      = $3
  AND org_id  = $4
  AND deleted_at IS NULL;
