ALTER TABLE committee_votes ADD COLUMN IF NOT EXISTS recommended_amount NUMERIC(15,2);
ALTER TABLE loan_applications ADD COLUMN IF NOT EXISTS mcc_finalized_by UUID REFERENCES users(id);
ALTER TABLE loan_applications ADD COLUMN IF NOT EXISTS mcc_finalized_at TIMESTAMPTZ;
