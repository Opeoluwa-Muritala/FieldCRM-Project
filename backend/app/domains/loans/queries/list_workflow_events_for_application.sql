-- Workflow history for one application inside one tenant.
-- Params: $1=org_id, $2=loan_id, $3=limit, $4=offset

SELECT
    we.id,
    we.loan_id,
    we.org_id,
    we.event_type,
    we.from_stage,
    we.to_stage,
    we.triggered_by,
    we.triggered_role,
    we.notes,
    we.created_at,
    (
      SELECT u.full_name FROM users u
      WHERE u.id = we.triggered_by AND u.org_id = we.org_id
    ) AS actor_name
FROM workflow_events we
WHERE org_id = $1
  AND loan_id = $2
ORDER BY created_at DESC, id DESC
LIMIT $3 OFFSET $4;
