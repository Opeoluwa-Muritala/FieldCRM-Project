-- Returns the newest extracted value for every field in a loan dossier.
SELECT DISTINCT ON (f.field_name, r.form_type)
    f.field_name,
    f.ocr_value,
    f.final_value,
    f.confidence,
    f.is_critical,
    f.verified,
    f.page_number,
    r.form_type,
    d.ocr_status,
    d.doc_type
FROM ocr_fields f
JOIN ocr_results r ON r.id = f.ocr_result_id
JOIN documents d ON d.id = r.document_id
WHERE f.loan_id = $1
ORDER BY f.field_name, r.form_type, r.created_at DESC, f.created_at DESC;
