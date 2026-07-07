-- Committee chair completes review and routes to ED or MD based on amount.
-- Loans < 10,000,000 go to ed_approval; >= 10,000,000 go to md_approval.
-- Params: $1=loan_id, $2=org_id, $3=recommendation

UPDATE loan_applications
SET
    stage                    = CASE
                                 WHEN amount IS NULL OR amount < 10000000 THEN 'ed_approval'
                                 ELSE 'md_approval'
                               END,
    committee_recommendation = $3,
    committee_completed_at   = NOW(),
    updated_at               = NOW()
WHERE id         = $1
  AND org_id     = $2
  AND stage      = 'committee_review'
  AND deleted_at IS NULL
RETURNING
    id, org_id, ref_no, customer_type, loan_type, stage, applicant_name,
    bvn, phone, amount, tenor_months, purpose, repayment_mode, created_by,
    current_owner_id, credit_officer_id, branch_manager_id, return_reason,
    returned_at, approved_by, approved_at, disbursed_at, created_at, updated_at;
