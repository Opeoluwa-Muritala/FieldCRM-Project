-- Get the most recent previous disbursed loan for the same applicant (by phone or name match).
-- Params: $1=org_id, $2=applicant_name, $3=phone, $4=current_loan_id

SELECT
    la.id,
    la.ref_no,
    la.amount,
    la.disbursed_at,
    la.tenor_months,
    la.stage,
    la.loan_type,
    COALESCE(
        (SELECT SUM(rr.amount_paid)
         FROM repayment_records rr
         WHERE rr.loan_id = la.id),
        0
    ) AS total_paid
FROM loan_applications la
WHERE la.org_id  = $1
  AND la.id     != $4
  AND la.deleted_at IS NULL
  AND (
    la.applicant_name ILIKE $2
    OR (la.phone IS NOT NULL AND la.phone = $3)
  )
ORDER BY la.created_at DESC
LIMIT 1;
