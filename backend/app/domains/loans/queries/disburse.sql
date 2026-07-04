-- CRM records disbursement: disbursement_ready -> disbursed.
-- Params: $1=loan_id, $2=org_id, $3=disbursed_amount, $4=disbursement_method,
--         $5=disbursed_bank_ref, $6=disbursement_ref, $7=interest_rate,
--         $8=repayment_frequency, $9=schedule_method

UPDATE loan_applications
SET
    stage                = 'disbursed',
    disbursed_at         = NOW(),
    disbursed_amount     = $3,
    disbursement_method  = $4,
    disbursed_bank_ref   = $5,
    disbursement_ref     = $6,
    interest_rate        = $7,
    repayment_frequency  = $8,
    schedule_method      = $9,
    classification       = 'current',
    days_past_due        = 0,
    updated_at           = NOW()
WHERE id                 = $1
  AND org_id             = $2
  AND stage              = 'disbursement_ready'
  AND deleted_at         IS NULL
RETURNING
    id, org_id, ref_no, customer_type, loan_type, stage, applicant_name,
    bvn, phone, amount, tenor_months, purpose, repayment_mode, created_by,
    current_owner_id, credit_officer_id, branch_manager_id, return_reason,
    returned_at, approved_by, approved_at, disbursed_at, disbursed_amount,
    disbursement_method, disbursed_bank_ref, disbursement_ref,
    interest_rate, repayment_frequency, schedule_method,
    executive_approved_by, executive_approved_at,
    crm_reviewed_by, crm_reviewed_at, crm_notes,
    classification, days_past_due,
    created_at, updated_at;
