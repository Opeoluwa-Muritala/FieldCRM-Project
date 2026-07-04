-- Advance loan from branch_approval to crm_review.
-- Params: $1=loan_id, $2=org_id, $3=approved_by

UPDATE loan_applications
SET
    stage             = 'crm_review',
    approved_by       = $3,
    approved_at       = NOW(),
    updated_at        = NOW()
WHERE id              = $1
  AND org_id          = $2
  AND stage           = 'branch_approval'
  AND deleted_at      IS NULL
RETURNING
    id, org_id, ref_no, customer_type, loan_type, stage, applicant_name,
    bvn, phone, amount, tenor_months, purpose, repayment_mode, created_by,
    current_owner_id, credit_officer_id, branch_manager_id, return_reason,
    returned_at, approved_by, approved_at, disbursed_at, created_at, updated_at;
