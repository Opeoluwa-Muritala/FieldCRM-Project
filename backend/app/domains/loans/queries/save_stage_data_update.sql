-- loans/queries/save_stage_data_update.sql
-- Updates an existing stage_data record.
-- Params: $1=data_json (jsonb), $2=saved_by, $3=stage_data_id

UPDATE stage_data
SET data_json = $1,
    saved_by  = $2,
    saved_at  = NOW()
WHERE id = $3
RETURNING id, loan_id, stage, data_json, saved_by, saved_at;
