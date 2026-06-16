-- guarantors/queries/upsert_submitted.sql
-- Marks a guarantor slot as submitted, creating the slot if needed.
-- Params: $1=loan_id, $2=org_id, $3=slot, $4=full_name, $5=relationship_to_client, $6=bvn, $7=phone, $8=home_address, $9=employment_type, $10=monthly_salary, $11=max_guarantee_amount, $12=bank_name, $13=account_number, $14=cheque_number, $15=signature_detected, $16=witness_signature_detected

INSERT INTO guarantors (
    loan_id, org_id, slot, full_name, relationship_to_client, bvn,
    phone, home_address, employment_type, monthly_salary,
    max_guarantee_amount, bank_name, account_number, cheque_number,
    signature_detected, witness_signature_detected, form_stage
)
VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, $13, $14, $15, $16, 'submitted')
ON CONFLICT (loan_id, slot)
DO UPDATE SET
    full_name = EXCLUDED.full_name,
    relationship_to_client = EXCLUDED.relationship_to_client,
    bvn = EXCLUDED.bvn,
    phone = EXCLUDED.phone,
    home_address = EXCLUDED.home_address,
    employment_type = EXCLUDED.employment_type,
    monthly_salary = EXCLUDED.monthly_salary,
    max_guarantee_amount = EXCLUDED.max_guarantee_amount,
    bank_name = EXCLUDED.bank_name,
    account_number = EXCLUDED.account_number,
    cheque_number = EXCLUDED.cheque_number,
    signature_detected = EXCLUDED.signature_detected,
    witness_signature_detected = EXCLUDED.witness_signature_detected,
    form_stage = 'submitted',
    updated_at = NOW()
RETURNING
    id, loan_id, org_id, slot, full_name, relationship_to_client, bvn,
    phone, home_address, employment_type, monthly_salary,
    max_guarantee_amount, max_guarantee_amount_words, bank_name,
    account_number, cheque_number, form_stage, signature_detected,
    witness_signature_detected, created_at, updated_at;
