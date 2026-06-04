-- guarantors/queries/upsert_submitted.sql
-- Marks a guarantor slot as submitted, creating the slot if needed.
-- Params: $1=loan_id, $2=org_id, $3=slot

INSERT INTO guarantors (loan_id, org_id, slot, form_stage)
VALUES ($1, $2, $3, 'submitted')
ON CONFLICT (loan_id, slot)
DO UPDATE SET
    form_stage = 'submitted',
    updated_at = NOW()
RETURNING
    id, loan_id, org_id, slot, full_name, relationship_to_client, bvn,
    phone, home_address, employment_type, monthly_salary,
    max_guarantee_amount, max_guarantee_amount_words, bank_name,
    account_number, cheque_number, form_stage, signature_detected,
    witness_signature_detected, created_at, updated_at;
