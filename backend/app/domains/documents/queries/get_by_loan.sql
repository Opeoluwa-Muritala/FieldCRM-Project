-- documents/queries/get_by_loan.sql
-- Fetches active documents for a specific loan application.
-- Params: $1=loan_id, $2=org_id

SELECT
    d.id,
    d.loan_id,
    d.org_id,
    d.doc_type,
    d.form_code,
    d.original_name,
    d.stored_path,
    d.mime_type,
    d.size_bytes,
    d.verified,
    d.uploaded_by,
    uploader.full_name AS uploaded_by_name,
    uploader.role AS uploaded_by_role,
    d.uploaded_at,
    d.cloud_public_id,
    d.cloud_preview_url,
    d.uploaded_at AS created_at,
    d.uploaded_at AS updated_at
FROM documents d
LEFT JOIN users uploader
  ON uploader.id = d.uploaded_by
 AND uploader.org_id = d.org_id
WHERE d.loan_id = $1
  AND d.org_id = $2
  AND d.deleted_at IS NULL
ORDER BY d.uploaded_at DESC;
