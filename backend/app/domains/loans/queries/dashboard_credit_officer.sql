-- loans/queries/dashboard_credit_officer.sql
-- Credit Officer dashboard metrics.
-- Params: $1=org_id, $2=credit_officer_user_id

SELECT
    COUNT(*) FILTER (
        WHERE credit_officer_id = $2
          AND stage = 'credit_review'
    ) AS reviews_due,
    COUNT(*) FILTER (
        WHERE credit_officer_id = $2
          AND stage = 'returned'
          AND returned_at >= NOW() - INTERVAL '7 days'
    ) AS returned_this_week,
    COUNT(*) FILTER (
        WHERE credit_officer_id = $2
          AND stage = 'branch_approval'
          AND updated_at::date = CURRENT_DATE
    ) AS reviewed_today,
    (
        SELECT COUNT(DISTINCT of.loan_id)
        FROM ocr_fields of
        JOIN loan_applications la2 ON la2.id = of.loan_id
        WHERE la2.org_id = $1
          AND la2.credit_officer_id = $2
          AND la2.deleted_at IS NULL
          AND of.verified = FALSE
          AND (of.confidence < 70 OR of.is_critical = TRUE)
    ) AS ocr_exceptions,
    COUNT(*) FILTER (
        WHERE credit_officer_id = $2
          AND stage NOT IN ('disbursed', 'rejected')
    ) AS active_assigned
FROM loan_applications
WHERE org_id = $1
  AND deleted_at IS NULL;
