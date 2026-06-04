-- workflow/queries/log_event.sql
-- Inserts an immutable workflow event.
-- Params: $1=loan_id, $2=org_id, $3=event_type, $4=from_stage,
--         $5=to_stage, $6=triggered_by, $7=triggered_role, $8=notes

INSERT INTO workflow_events (
    loan_id, org_id, event_type, from_stage,
    to_stage, triggered_by, triggered_role, notes
)
VALUES ($1, $2, $3, $4, $5, $6, $7, $8)
RETURNING id, created_at;
