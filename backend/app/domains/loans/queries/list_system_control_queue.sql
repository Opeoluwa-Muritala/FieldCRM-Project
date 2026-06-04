-- loans/queries/list_system_control_queue.sql
-- System admin final-control queue.
-- Params: $1=org_id, $2=limit, $3=offset

SELECT
    la.id,
    la.ref_no,
    la.loan_type,
    la.stage,
    la.amount,
    la.applicant_name,
    la.updated_at,
    officer.full_name AS officer_name,
    credit.full_name AS credit_officer_name,
    manager.full_name AS branch_manager_name,
    COUNT(*) OVER () AS total_count
FROM loan_applications la
LEFT JOIN users officer ON officer.id = la.created_by
LEFT JOIN users credit ON credit.id = la.credit_officer_id
LEFT JOIN users manager ON manager.id = la.branch_manager_id
WHERE la.org_id = $1
  AND la.deleted_at IS NULL
  AND la.stage IN ('branch_approval', 'disbursement_ready', 'returned', 'rejected')
ORDER BY
    CASE la.stage
        WHEN 'returned' THEN 1
        WHEN 'branch_approval' THEN 2
        WHEN 'disbursement_ready' THEN 3
        WHEN 'rejected' THEN 4
        ELSE 5
    END,
    la.updated_at DESC
LIMIT $2 OFFSET $3;
