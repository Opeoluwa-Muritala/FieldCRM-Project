-- Record a repayment collection.
-- Params: $1=loan_id, $2=org_id, $3=payment_date, $4=amount_paid,
--         $5=channel, $6=bank_ref, $7=recorded_by

INSERT INTO repayment_records
    (loan_id, org_id, payment_date, amount_paid, channel, bank_ref, recorded_by)
VALUES ($1, $2, $3, $4, $5, $6, $7)
RETURNING *;
