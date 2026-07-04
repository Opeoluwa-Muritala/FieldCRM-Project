-- Portfolio at Risk metrics for the org.
-- Returns total portfolio and overdue buckets (PAR-1, PAR-30, PAR-90).
-- Params: $1=org_id

SELECT
    COUNT(*)                                                                        AS total_loans,
    COALESCE(SUM(disbursed_amount), 0)                                              AS total_portfolio,
    COUNT(*) FILTER (WHERE days_past_due >= 1)                                      AS par1_count,
    COALESCE(SUM(disbursed_amount) FILTER (WHERE days_past_due >= 1), 0)            AS par1_amount,
    COUNT(*) FILTER (WHERE days_past_due >= 30)                                     AS par30_count,
    COALESCE(SUM(disbursed_amount) FILTER (WHERE days_past_due >= 30), 0)           AS par30_amount,
    COUNT(*) FILTER (WHERE days_past_due >= 90)                                     AS par90_count,
    COALESCE(SUM(disbursed_amount) FILTER (WHERE days_past_due >= 90), 0)           AS par90_amount,
    COUNT(*) FILTER (WHERE classification = 'olem')                                 AS olem_count,
    COUNT(*) FILTER (WHERE classification = 'substandard')                          AS substandard_count,
    COUNT(*) FILTER (WHERE classification = 'doubtful')                             AS doubtful_count,
    COUNT(*) FILTER (WHERE classification = 'lost')                                 AS lost_count
FROM loan_applications
WHERE org_id      = $1
  AND stage       = 'disbursed'
  AND deleted_at  IS NULL;
