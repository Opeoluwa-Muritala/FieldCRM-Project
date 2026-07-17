-- Portfolio at Risk metrics for the org.
-- Returns total portfolio and overdue buckets (PAR-1, PAR-30, PAR-90) based on outstanding principal/interest balances.
-- Params: $1=org_id

WITH loan_balances AS (
    SELECT 
        la.id,
        la.org_id,
        la.classification,
        la.days_past_due,
        la.stage,
        la.deleted_at,
        la.disbursed_amount,
        -- Total amount paid by the customer
        COALESCE((SELECT SUM(amount_paid) FROM repayment_records WHERE loan_id = la.id), 0) AS total_paid,
        -- Remaining outstanding balance
        GREATEST(
            COALESCE((SELECT SUM(total_due) FROM repayment_schedule WHERE loan_id = la.id), la.disbursed_amount) - 
            COALESCE((SELECT SUM(amount_paid) FROM repayment_records WHERE loan_id = la.id), 0),
            0
        ) AS outstanding_balance
    FROM loan_applications la
)
SELECT
    COUNT(*)                                                                        AS total_loans,
    COALESCE(SUM(outstanding_balance), 0)                                           AS total_portfolio,
    COUNT(*) FILTER (WHERE days_past_due >= 1)                                      AS par1_count,
    COALESCE(SUM(outstanding_balance) FILTER (WHERE days_past_due >= 1), 0)         AS par1_amount,
    COUNT(*) FILTER (WHERE days_past_due >= 30)                                     AS par30_count,
    COALESCE(SUM(outstanding_balance) FILTER (WHERE days_past_due >= 30), 0)        AS par30_amount,
    COUNT(*) FILTER (WHERE days_past_due >= 90)                                     AS par90_count,
    COALESCE(SUM(outstanding_balance) FILTER (WHERE days_past_due >= 90), 0)        AS par90_amount,
    COUNT(*) FILTER (WHERE classification = 'olem')                                 AS olem_count,
    COUNT(*) FILTER (WHERE classification = 'substandard')                          AS substandard_count,
    COUNT(*) FILTER (WHERE classification = 'doubtful')                             AS doubtful_count,
    COUNT(*) FILTER (WHERE classification = 'lost')                                 AS lost_count
FROM loan_balances
WHERE org_id      = $1
  AND stage       = 'disbursed'
  AND deleted_at  IS NULL;
