-- MD/ED issues disbursement instruction: executive_approval -> disbursement_ready.
-- Params: $1=loan_id, $2=org_id, $3=executive_user_id

UPDATE loan_applications
SET
    stage                   = 'disbursement_ready',
    executive_approved_by   = $3,
    executive_approved_at   = NOW(),
    updated_at              = NOW()
WHERE id                    = $1
  AND org_id                = $2
  AND stage                 = 'executive_approval'
  AND deleted_at            IS NULL
RETURNING
    id, org_id, ref_no, customer_type, loan_type, stage, applicant_name,
    bvn, phone, amount, tenor_months, purpose, repayment_mode, created_by,
    current_owner_id, credit_officer_id, branch_manager_id, return_reason,
    returned_at, approved_by, approved_at, disbursed_at, created_at, updated_at;
