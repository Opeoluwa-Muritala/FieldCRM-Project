-- loans/queries/advance_stage.sql
-- Moves a loan to a new workflow stage.
-- Params: $1=stage, $2=loan_id, $3=org_id

UPDATE loan_applications
SET
    stage = $1,
    updated_at = NOW()
WHERE id = $2
  AND org_id = $3
  AND deleted_at IS NULL
RETURNING
    id, org_id, ref_no, customer_type, loan_type, stage, applicant_name,
    bvn, phone, amount, tenor_months, purpose, repayment_mode, created_by,
    current_owner_id, credit_officer_id, branch_manager_id, return_reason,
    returned_at, approved_by, approved_at, disbursed_at, created_at, updated_at;
