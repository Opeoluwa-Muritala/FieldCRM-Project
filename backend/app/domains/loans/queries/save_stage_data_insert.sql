-- loans/queries/save_stage_data_insert.sql
-- Creates a new stage_data record.
-- Params: $1=loan_id, $2=stage (text), $3=data_json (jsonb), $4=saved_by

INSERT INTO stage_data (loan_id, stage, data_json, saved_by)
VALUES ($1, $2, $3, $4)
RETURNING id, loan_id, stage, data_json, saved_by, saved_at;
