-- loans/queries/create.sql
-- Creates a new loan application draft and returns the full row.
-- Uses generate_loan_ref() for atomic ref_no generation.
-- Params: $1=org_id, $2=ref_no, $3=customer_type, $4=loan_type,
--         $5=applicant_name, $6=created_by

INSERT INTO loan_applications (
    org_id,
    ref_no,
    customer_type,
    loan_type,
    applicant_name,
    created_by,
    current_owner_id,
    stage
)
VALUES ($1, $2, $3, $4, $5, $6, $6, 'intake')
RETURNING
    id, org_id, ref_no, customer_type, loan_type, stage, applicant_name,
    bvn, phone, amount, tenor_months, purpose, repayment_mode, created_by,
    current_owner_id, credit_officer_id, branch_manager_id, return_reason,
    returned_at, approved_by, approved_at, disbursed_at, created_at, updated_at;
