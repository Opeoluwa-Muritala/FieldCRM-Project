-- loans/queries/list_workflow_events.sql
-- Lists workflow events for a tenant audit screen.
-- Params: $1=org_id

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
ORDER BY created_at DESC;
