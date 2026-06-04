-- documents/queries/create.sql
-- Creates a document metadata row after upload/storage.
-- Params: $1=loan_id, $2=org_id, $3=doc_type, $4=form_code,
--         $5=original_name, $6=stored_path, $7=mime_type,
--         $8=size_bytes, $9=uploaded_by

INSERT INTO documents (
    loan_id,
    org_id,
    doc_type,
    form_code,
    original_name,
    stored_path,
    mime_type,
    size_bytes,
    quality_status,
    uploaded_by
)
VALUES ($1, $2, $3, $4, $5, $6, $7, $8, 'clear', $9)
RETURNING
    id, loan_id, org_id, guarantor_id, doc_type, form_code, original_name,
    stored_path, mime_type, size_bytes, quality_status, verified,
    verified_by, verified_at, uploaded_by, uploaded_at, deleted_at;
