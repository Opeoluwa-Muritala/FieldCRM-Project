-- loans/queries/list_awaiting_concurrence.sql
-- Branch manager approval queue.
-- Params: $1=org_id, $2=branch_manager_user_id, $3=limit, $4=offset

SELECT
    la.id,
    la.ref_no,
    la.loan_type,
    la.stage,
    la.amount,
    la.applicant_name,
    la.created_at,
    la.updated_at,
    la.created_by,
    officer.full_name AS officer_name,
    credit.full_name AS credit_officer_name,
    vr.status AS visitation_status,
    vr.manager_concurrence,
    EXTRACT(DAY FROM (NOW() - la.updated_at))::int AS days_waiting,
    COUNT(*) OVER () AS total_count
FROM loan_applications la
LEFT JOIN users officer ON officer.id = la.created_by
LEFT JOIN users credit ON credit.id = la.credit_officer_id
LEFT JOIN visitation_reports vr ON vr.loan_id = la.id
WHERE la.org_id = $1
  AND la.branch_manager_id = $2
  AND la.stage = 'branch_manager_review'
  AND la.deleted_at IS NULL
ORDER BY
    CASE WHEN vr.status = 'submitted' THEN 0 ELSE 1 END,
    la.updated_at ASC
LIMIT $3 OFFSET $4;
