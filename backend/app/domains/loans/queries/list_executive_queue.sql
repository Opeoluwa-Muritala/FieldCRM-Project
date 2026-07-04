-- Loans in executive_approval for the org, ordered by oldest first.
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
    u.full_name   AS officer_name,
    crm.full_name AS crm_name,
    la.crm_notes,
    EXTRACT(DAY FROM NOW() - la.updated_at)::INTEGER AS days_waiting
FROM loan_applications la
LEFT JOIN users u   ON u.id   = la.created_by
LEFT JOIN users crm ON crm.id = la.crm_reviewed_by
WHERE la.org_id     = $1
  AND la.stage      = 'executive_approval'
  AND la.deleted_at IS NULL
ORDER BY la.updated_at ASC
LIMIT $2 OFFSET $3;
