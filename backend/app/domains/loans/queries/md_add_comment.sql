-- MD adds comment/notes to loans in md_approval stage only.
-- Params: $1=loan_id, $2=org_id, $3=md_notes

UPDATE loan_applications
SET
    md_notes   = $3,
    updated_at = NOW()
WHERE id       = $1
  AND org_id   = $2
  AND stage    = 'md_approval'
  AND deleted_at IS NULL
RETURNING id, ref_no, stage, md_notes, updated_at;
