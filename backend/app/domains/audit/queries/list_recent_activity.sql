-- audit/queries/list_recent_activity.sql
-- Recent immutable audit activity.
-- Params: $1=org_id, $2=limit, $3=offset

SELECT
    ae.id,
    ae.entity_type,
    ae.entity_id,
    ae.action,
    ae.user_id,
    ae.user_role,
    ae.field_name,
    ae.source,
    ae.notes,
    ae.created_at,
    u.full_name AS user_name,
    COUNT(*) OVER () AS total_count
FROM audit_entries ae
LEFT JOIN users u ON u.id = ae.user_id
WHERE ae.org_id = $1
ORDER BY ae.created_at DESC
LIMIT $2 OFFSET $3;
