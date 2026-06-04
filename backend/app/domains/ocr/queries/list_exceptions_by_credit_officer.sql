-- ocr/queries/list_exceptions_by_credit_officer.sql
-- OCR exceptions assigned to a credit officer.
-- Params: $1=org_id, $2=credit_officer_user_id, $3=threshold, $4=limit, $5=offset

SELECT
    of.id,
    of.loan_id,
    la.ref_no,
    la.applicant_name,
    la.amount,
    of.field_name,
    of.ocr_value,
    of.corrected_value,
    of.final_value,
    of.confidence,
    of.is_critical,
    of.verified,
    ocr.form_type,
    d.doc_type,
    d.original_name,
    COUNT(*) OVER () AS total_count
FROM ocr_fields of
JOIN loan_applications la ON la.id = of.loan_id
JOIN ocr_results ocr ON ocr.id = of.ocr_result_id
JOIN documents d ON d.id = ocr.document_id
WHERE la.org_id = $1
  AND la.credit_officer_id = $2
  AND la.deleted_at IS NULL
  AND of.verified = FALSE
  AND (of.confidence < $3 OR of.is_critical = TRUE)
ORDER BY of.is_critical DESC, of.confidence ASC, la.updated_at ASC
LIMIT $4 OFFSET $5;
