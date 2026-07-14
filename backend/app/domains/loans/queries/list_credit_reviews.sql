-- loans/queries/list_credit_reviews.sql
-- Credit Analyst review queue.
-- Params: $1=org_id, $2=credit_analyst_user_id, $3=limit, $4=offset

SELECT
    la.id,
    la.ref_no,
    la.loan_type,
    la.stage,
    la.amount,
    la.tenor_months,
    la.applicant_name,
    la.created_at,
    la.updated_at,
    officer.full_name AS officer_name,
    COUNT(of.id) FILTER (
        WHERE of.verified = FALSE
          AND (of.confidence < 70 OR of.is_critical = TRUE)
    ) AS exception_count,
    COUNT(*) OVER () AS total_count
FROM loan_applications la
LEFT JOIN users officer ON officer.id = la.created_by
LEFT JOIN ocr_fields of ON of.loan_id = la.id
WHERE la.org_id = $1
  -- The branch workflow has a shared Credit Analyst queue; applications
  -- are not assigned to the former credit_officer_id field.
  AND $2::UUID IS NOT NULL
  AND la.stage = 'credit_analyst_review'
  AND la.deleted_at IS NULL
GROUP BY la.id, officer.full_name
ORDER BY exception_count DESC, la.updated_at ASC
LIMIT $3 OFFSET $4;
