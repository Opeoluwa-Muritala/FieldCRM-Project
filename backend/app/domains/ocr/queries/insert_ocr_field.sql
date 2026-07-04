-- ocr/queries/insert_ocr_field.sql
-- Params: $1=ocr_result_id, $2=loan_id, $3=field_name, $4=ocr_value, $5=confidence, $6=is_critical, $7=page_number
INSERT INTO ocr_fields (ocr_result_id, loan_id, field_name, ocr_value, confidence, is_critical, page_number)
VALUES ($1, $2, $3, $4, $5, $6, $7)
RETURNING id;
