-- Administrative health only. Never derive System Admin metrics from lending records.
-- Params: $1=org_id
SELECT
    (SELECT COUNT(*) FROM users WHERE org_id=$1 AND active=TRUE) AS active_users,
    (SELECT COUNT(*) FROM users WHERE org_id=$1) AS total_users,
    (SELECT COUNT(*) FROM users WHERE org_id=$1 AND active=FALSE) AS inactive_users,
    (SELECT COUNT(*) FROM audit_entries WHERE org_id=$1 AND created_at >= NOW() - INTERVAL '24 hours') AS system_events,
    0::bigint AS failed_jobs,
    0::bigint AS config_alerts;
