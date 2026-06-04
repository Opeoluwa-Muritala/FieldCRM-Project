-- loans/queries/get_stage_data.sql
-- Gets stage data for a loan by stage name.
-- Params: $1=loan_id, $2=stage (text)

SELECT
    id,
    loan_id,
    stage,
    data_json,
    saved_by,
    saved_at
FROM stage_data
WHERE loan_id = $1
  AND stage   = $2
ORDER BY saved_at DESC
LIMIT 1;
