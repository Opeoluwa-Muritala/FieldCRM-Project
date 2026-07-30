-- Paginated disbursed loans for one organisation.
-- Params: $1=org_id, $2=limit, $3=offset

SELECT
    la.id,
    la.ref_no,
    la.applicant_name,
    la.bvn,
    la.amount,
    la.disbursed_amount,
    la.disbursed_at,
    la.interest_rate,
    la.repayment_frequency,
    la.tenor_months,
    la.loan_type,
    la.sector,
    la.classification,
    la.days_past_due,
    la.executive_approved_by,
    ex.full_name AS executive_name,
    COUNT(*) OVER () AS total_count
FROM loan_applications la
LEFT JOIN users ex ON ex.id = la.executive_approved_by
WHERE la.org_id = $1
  AND la.stage = 'disbursed'
  AND la.deleted_at IS NULL
ORDER BY la.disbursed_at DESC, la.id DESC
LIMIT $2 OFFSET $3;
