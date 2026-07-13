-- MD provides advisory comments, then returns the file to ED for the
-- required final ED decision.
-- Params: $1=loan_id, $2=org_id, $3=md_notes

UPDATE loan_applications
SET
    md_notes   = $3,
    stage      = 'ed_approval',
    updated_at = NOW()
WHERE id       = $1
  AND org_id   = $2
  AND stage    = 'md_approval'
  AND deleted_at IS NULL
RETURNING id, ref_no, stage, md_notes, updated_at;
