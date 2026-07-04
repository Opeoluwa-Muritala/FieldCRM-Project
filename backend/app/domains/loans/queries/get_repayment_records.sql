-- Fetch all repayment records for a loan, newest first.
-- Params: $1=loan_id, $2=org_id

SELECT
    rr.id, rr.loan_id, rr.org_id, rr.payment_date,
    rr.amount_paid, rr.channel, rr.bank_ref,
    rr.recorded_by, rr.created_at,
    u.full_name AS recorded_by_name
FROM repayment_records rr
LEFT JOIN users u ON u.id = rr.recorded_by
WHERE rr.loan_id = $1
  AND rr.org_id  = $2
ORDER BY rr.payment_date DESC;
