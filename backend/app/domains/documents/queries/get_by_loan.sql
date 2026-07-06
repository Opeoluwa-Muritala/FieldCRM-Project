-- documents/queries/get_by_loan.sql
-- Fetches active documents for a specific loan application.
-- Params: $1=loan_id, $2=org_id

SELECT
    id,
    loan_id,
    org_id,
    doc_type,
    form_code,
    original_name,
    stored_path,
    mime_type,
    size_bytes,
    verified,
    uploaded_by,
    uploaded_at,
    uploaded_at AS created_at,
    uploaded_at AS updated_at
FROM documents
WHERE loan_id = $1
  AND org_id = $2
  AND deleted_at IS NULL
ORDER BY uploaded_at DESC;
