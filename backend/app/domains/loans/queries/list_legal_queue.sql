-- Loans that need valuation/legal review
-- Params: $1=org_id, $2=limit, $3=offset

SELECT
    la.id,
    la.ref_no,
    la.applicant_name,
    la.amount,
    la.loan_type,
    la.stage,
    la.created_at,
    la.updated_at,
    u.full_name AS officer_name,
    bm.full_name AS branch_manager_name,
    EXTRACT(DAY FROM NOW() - la.updated_at)::INTEGER AS days_waiting
FROM loan_applications la
LEFT JOIN users u  ON u.id  = la.created_by
LEFT JOIN users bm ON bm.id = la.branch_manager_id
WHERE la.org_id     = $1
  AND la.stage      IN ('branch_manager_review', 'credit_analyst_review', 'crm_review')
  AND la.deleted_at IS NULL
ORDER BY la.updated_at ASC
LIMIT $2 OFFSET $3;
