-- loans/queries/dashboard_branch_manager.sql
-- Branch Manager dashboard metrics.
-- Params: $1=org_id, $2=branch_manager_user_id

SELECT
    COUNT(*) FILTER (
        WHERE branch_manager_id = $2
          AND stage = 'branch_approval'
    ) AS awaiting_concurrence,
    COUNT(*) FILTER (
        WHERE branch_manager_id = $2
          AND stage = 'disbursement_ready'
          AND approved_at::date = CURRENT_DATE
    ) AS approved_today,
    COUNT(*) FILTER (
        WHERE branch_manager_id = $2
          AND stage = 'returned'
          AND returned_at >= NOW() - INTERVAL '7 days'
    ) AS returned_this_week,
    COUNT(*) FILTER (
        WHERE branch_manager_id = $2
          AND stage = 'disbursement_ready'
    ) AS ready_for_disbursement,
    COALESCE(SUM(amount) FILTER (
        WHERE branch_manager_id = $2
          AND stage = 'disbursement_ready'
    ), 0) AS ready_amount,
    COUNT(*) FILTER (
        WHERE branch_manager_id = $2
          AND stage NOT IN ('disbursed', 'rejected')
    ) AS active_assigned,
    (
        SELECT COUNT(*)
        FROM visitation_reports vr
        JOIN loan_applications vla ON vla.id = vr.loan_id
        WHERE vr.org_id = $1
          AND vla.branch_manager_id = $2
          AND vr.status = 'submitted'
          AND vla.deleted_at IS NULL
    ) AS pending_signoffs
FROM loan_applications
WHERE org_id = $1
  AND deleted_at IS NULL;
