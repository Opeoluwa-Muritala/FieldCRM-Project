-- loans/queries/dashboard_system_admin.sql
-- System Admin dashboard metrics.
-- Params: $1=org_id

SELECT
    (
        SELECT COUNT(*)
        FROM users u
        WHERE u.org_id = $1
          AND u.active = TRUE
    ) AS active_users,
    COUNT(*) FILTER (
        WHERE stage NOT IN ('disbursed', 'rejected')
    ) AS active_loans,
    COUNT(*) FILTER (
        WHERE stage = 'disbursement_ready'
    ) AS ready_for_disbursement,
    COUNT(*) FILTER (
        WHERE stage IN ('returned', 'rejected')
    ) AS blocked_files,
    (
        SELECT COUNT(*)
        FROM audit_entries ae
        WHERE ae.org_id = $1
          AND ae.created_at >= NOW() - INTERVAL '24 hours'
    ) AS audit_events_today
FROM loan_applications
WHERE org_id = $1
  AND deleted_at IS NULL;
