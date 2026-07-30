-- Workflow history for one application inside one tenant.
-- Params: $1=org_id, $2=loan_id, $3=limit, $4=offset

SELECT
    id,
    loan_id,
    org_id,
    event_type,
    from_stage,
    to_stage,
    triggered_by,
    triggered_role,
    notes,
    created_at
FROM workflow_events
WHERE org_id = $1
  AND loan_id = $2
ORDER BY created_at DESC, id DESC
LIMIT $3 OFFSET $4;
