-- loans/queries/create.sql
-- Creates a new loan application draft and returns the full row.
-- Uses generate_loan_ref() for atomic ref_no generation.
-- Params: $1=org_id, $2=ref_no, $3=customer_type, $4=loan_type,
--         $5=applicant_name, $6=created_by, $7=branch_id, $8=client_request_id

INSERT INTO loan_applications (
    org_id,
    ref_no,
    customer_type,
    loan_type,
    applicant_name,
    created_by,
    current_owner_id,
    stage,
    branch_id,
    client_request_id,
    originated_config_version_id
)
VALUES ($1, $2, $3, $4, $5, $6, $6, 'intake', $7, $8,
  (SELECT id FROM configuration_versions
   WHERE org_id=$1 AND status='published' AND effective_at <= NOW()
   ORDER BY effective_at DESC, version_number DESC LIMIT 1))
RETURNING
    id, org_id, ref_no, customer_type, loan_type, stage, applicant_name,
    bvn, phone, amount, tenor_months, purpose, repayment_mode, created_by,
    current_owner_id, credit_officer_id, branch_manager_id, return_reason,
    returned_at, approved_by, approved_at, disbursed_at, created_at, updated_at, branch_id;
