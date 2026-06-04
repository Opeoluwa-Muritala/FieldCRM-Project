-- ocr/queries/list_low_confidence.sql
-- Returns unverified low-confidence or critical OCR fields for a loan.
-- Params: $1=loan_id, $2=confidence_threshold

SELECT
    f.id,
    f.field_name,
    f.ocr_value,
    f.corrected_value,
    f.final_value,
    f.confidence,
    f.source,
    f.is_critical,
    f.verified,
    r.form_type,
    r.id AS ocr_result_id,
    d.id AS document_id,
    d.doc_type,
    d.original_name
FROM ocr_fields f
JOIN ocr_results r ON r.id = f.ocr_result_id
JOIN documents d ON d.id = r.document_id
WHERE f.loan_id = $1
  AND f.verified = FALSE
  AND (
      f.confidence < $2
      OR f.is_critical = TRUE
  )
ORDER BY f.is_critical DESC, f.confidence ASC;
