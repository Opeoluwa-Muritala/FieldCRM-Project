-- Advance loan from crm_review to committee_review.
-- Params: $1=loan_id, $2=org_id, $3=crm_user_id, $4=crm_notes

UPDATE loan_applications
SET
    stage           = 'committee_review',
    crm_reviewed_by = $3,
    crm_reviewed_at = NOW(),
    crm_notes       = $4,
    updated_at      = NOW()
WHERE id            = $1
  AND org_id        = $2
  AND stage         = 'crm_review'
  AND deleted_at    IS NULL
RETURNING
    id, org_id, ref_no, customer_type, loan_type, stage, applicant_name,
    bvn, phone, amount, tenor_months, purpose, repayment_mode, created_by,
    current_owner_id, credit_officer_id, branch_manager_id, return_reason,
    returned_at, approved_by, approved_at, disbursed_at, created_at, updated_at;
