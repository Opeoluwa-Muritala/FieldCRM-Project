-- Loan Reference Number Sequence and Generator Function
-- Generates references like: MMFB-2026-01000

CREATE SEQUENCE loan_ref_seq START 1000 INCREMENT 1;

CREATE OR REPLACE FUNCTION generate_loan_ref(org_code TEXT)
RETURNS TEXT AS $$
BEGIN
    RETURN org_code || '-' || TO_CHAR(NOW(), 'YYYY') || '-' || LPAD(nextval('loan_ref_seq')::TEXT, 5, '0');
END;
$$ LANGUAGE plpgsql;
