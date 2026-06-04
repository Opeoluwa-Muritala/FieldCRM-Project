-- visitation/queries/list_pending_signoffs.sql
-- Submitted visitation reports awaiting branch manager signoff.
-- Params: $1=org_id, $2=branch_manager_user_id, $3=limit, $4=offset

SELECT
    vr.id,
    vr.loan_id,
    la.ref_no,
    la.applicant_name,
    la.amount,
    la.stage,
    vr.visit_date,
    vr.met_with,
    vr.business_condition,
    vr.status,
    vr.updated_at,
    officer.full_name AS visiting_officer_name,
    COUNT(*) OVER () AS total_count
FROM visitation_reports vr
JOIN loan_applications la ON la.id = vr.loan_id
LEFT JOIN users officer ON officer.id = vr.visiting_officer_id
WHERE vr.org_id = $1
  AND la.branch_manager_id = $2
  AND la.deleted_at IS NULL
  AND vr.status = 'submitted'
ORDER BY vr.updated_at ASC
LIMIT $3 OFFSET $4;
