-- ocr/queries/insert_ocr_result.sql
-- Params: $1=document_id, $2=loan_id, $3=form_type, $4=overall_confidence, $5=raw_extraction (jsonb)
INSERT INTO ocr_results (document_id, loan_id, form_type, overall_confidence, raw_extraction)
VALUES ($1, $2, $3, $4, $5::jsonb)
RETURNING id, document_id, loan_id, form_type, overall_confidence, created_at;
