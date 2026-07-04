-- Insert a single repayment schedule installment.
-- Params: $1=loan_id, $2=org_id, $3=installment_no, $4=due_date,
--         $5=principal_due, $6=interest_due, $7=total_due

INSERT INTO repayment_schedule
    (loan_id, org_id, installment_no, due_date, principal_due, interest_due, total_due)
VALUES ($1, $2, $3, $4, $5, $6, $7)
ON CONFLICT (loan_id, installment_no) DO UPDATE SET
    due_date      = EXCLUDED.due_date,
    principal_due = EXCLUDED.principal_due,
    interest_due  = EXCLUDED.interest_due,
    total_due     = EXCLUDED.total_due
RETURNING *;
