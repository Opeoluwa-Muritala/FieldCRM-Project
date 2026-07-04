-- Fetch full repayment schedule for a loan, ordered by installment.
-- Params: $1=loan_id, $2=org_id

SELECT
    id, loan_id, org_id, installment_no,
    due_date, principal_due, interest_due, total_due, created_at
FROM repayment_schedule
WHERE loan_id = $1
  AND org_id  = $2
ORDER BY installment_no ASC;
