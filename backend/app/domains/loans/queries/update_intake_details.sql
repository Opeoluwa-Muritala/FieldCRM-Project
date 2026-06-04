-- loans/queries/update_intake_details.sql
-- Updates the denormalized applicant fields captured during intake.
-- Params: $1=applicant_name, $2=phone, $3=bvn, $4=amount,
--         $5=tenor_months, $6=loan_id, $7=org_id

UPDATE loan_applications
SET
    applicant_name = $1,
    phone = $2,
    bvn = $3,
    amount = $4,
    tenor_months = $5,
    updated_at = NOW()
WHERE id = $6
  AND org_id = $7
  AND deleted_at IS NULL
RETURNING
    id, org_id, ref_no, customer_type, loan_type, stage, applicant_name,
    bvn, phone, amount, tenor_months, purpose, repayment_mode, created_by,
    current_owner_id, credit_officer_id, branch_manager_id, return_reason,
    returned_at, approved_by, approved_at, disbursed_at, created_at, updated_at;
