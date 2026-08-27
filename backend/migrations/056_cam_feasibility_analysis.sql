-- CAM-grounded feasibility analysis fields.
-- Additive and idempotent so existing feasibility and credit evidence remains intact.

ALTER TABLE credit_obligations ADD COLUMN IF NOT EXISTS facility_amount NUMERIC(15,2) NOT NULL DEFAULT 0 CHECK (facility_amount >= 0);
ALTER TABLE credit_obligations ADD COLUMN IF NOT EXISTS start_date DATE;
ALTER TABLE credit_obligations ADD COLUMN IF NOT EXISTS end_date DATE;
ALTER TABLE credit_obligations ADD COLUMN IF NOT EXISTS classification TEXT;

ALTER TABLE cashflow_entries ADD COLUMN IF NOT EXISTS transaction_count INTEGER NOT NULL DEFAULT 0 CHECK (transaction_count >= 0);

ALTER TABLE guarantors ADD COLUMN IF NOT EXISTS business_name TEXT;
ALTER TABLE guarantors ADD COLUMN IF NOT EXISTS business_address TEXT;
ALTER TABLE guarantors ADD COLUMN IF NOT EXISTS description_landmark TEXT;

ALTER TABLE collateral_items ADD COLUMN IF NOT EXISTS owner_type TEXT CHECK (owner_type IN ('client', 'guarantor', 'client_asset'));
ALTER TABLE collateral_items ADD COLUMN IF NOT EXISTS chassis_no TEXT;
ALTER TABLE collateral_items ADD COLUMN IF NOT EXISTS registration_no TEXT;
ALTER TABLE collateral_items ADD COLUMN IF NOT EXISTS colour TEXT;
ALTER TABLE collateral_items ADD COLUMN IF NOT EXISTS year INTEGER CHECK (year IS NULL OR year BETWEEN 1900 AND 2200);
ALTER TABLE collateral_items ADD COLUMN IF NOT EXISTS cam_forced_sale_value NUMERIC(15,2) CHECK (cam_forced_sale_value IS NULL OR cam_forced_sale_value >= 0);

ALTER TABLE borrower_financial_profiles ADD COLUMN IF NOT EXISTS cash_at_bank NUMERIC(15,2) NOT NULL DEFAULT 0 CHECK (cash_at_bank >= 0);
ALTER TABLE borrower_financial_profiles ADD COLUMN IF NOT EXISTS stock NUMERIC(15,2) NOT NULL DEFAULT 0 CHECK (stock >= 0);
ALTER TABLE borrower_financial_profiles ADD COLUMN IF NOT EXISTS prepayment NUMERIC(15,2) NOT NULL DEFAULT 0 CHECK (prepayment >= 0);
ALTER TABLE borrower_financial_profiles ADD COLUMN IF NOT EXISTS fixed_assets NUMERIC(15,2) NOT NULL DEFAULT 0 CHECK (fixed_assets >= 0);
ALTER TABLE borrower_financial_profiles ADD COLUMN IF NOT EXISTS monthly_turnover NUMERIC(15,2) NOT NULL DEFAULT 0 CHECK (monthly_turnover >= 0);
ALTER TABLE borrower_financial_profiles ADD COLUMN IF NOT EXISTS margin NUMERIC(7,6) NOT NULL DEFAULT 0 CHECK (margin BETWEEN 0 AND 1);
ALTER TABLE borrower_financial_profiles ADD COLUMN IF NOT EXISTS monthly_expenses NUMERIC(15,2) NOT NULL DEFAULT 0 CHECK (monthly_expenses >= 0);
ALTER TABLE borrower_financial_profiles ADD COLUMN IF NOT EXISTS recommended_amount NUMERIC(15,2) NOT NULL DEFAULT 0 CHECK (recommended_amount >= 0);
ALTER TABLE borrower_financial_profiles ADD COLUMN IF NOT EXISTS interest_rate NUMERIC(7,4) NOT NULL DEFAULT 0 CHECK (interest_rate BETWEEN 0 AND 100);
ALTER TABLE borrower_financial_profiles ADD COLUMN IF NOT EXISTS proposed_tenor INTEGER NOT NULL DEFAULT 12 CHECK (proposed_tenor BETWEEN 1 AND 120);
ALTER TABLE borrower_financial_profiles ADD COLUMN IF NOT EXISTS remita_email TEXT;
ALTER TABLE borrower_financial_profiles ADD COLUMN IF NOT EXISTS remita_account_no TEXT;
ALTER TABLE borrower_financial_profiles ADD COLUMN IF NOT EXISTS remita_account_name TEXT;
ALTER TABLE borrower_financial_profiles ADD COLUMN IF NOT EXISTS remita_bank TEXT;
ALTER TABLE borrower_financial_profiles ADD COLUMN IF NOT EXISTS property_coordinates_link TEXT;
ALTER TABLE borrower_financial_profiles ADD COLUMN IF NOT EXISTS property_description TEXT;
ALTER TABLE borrower_financial_profiles ADD COLUMN IF NOT EXISTS analyst_name TEXT;
ALTER TABLE borrower_financial_profiles ADD COLUMN IF NOT EXISTS analyst_recommendation TEXT;
ALTER TABLE borrower_financial_profiles ADD COLUMN IF NOT EXISTS pre_disbursement_conditions TEXT;
ALTER TABLE borrower_financial_profiles ADD COLUMN IF NOT EXISTS shop_allocation TEXT;
ALTER TABLE borrower_financial_profiles ADD COLUMN IF NOT EXISTS shop_allowance NUMERIC(15,2) NOT NULL DEFAULT 0 CHECK (shop_allowance >= 0);
ALTER TABLE borrower_financial_profiles ADD COLUMN IF NOT EXISTS shop_allowance_verified NUMERIC(15,2) NOT NULL DEFAULT 0 CHECK (shop_allowance_verified >= 0);

CREATE INDEX IF NOT EXISTS ix_cam_credit_history
    ON credit_obligations(application_id, source_type, created_at);
CREATE INDEX IF NOT EXISTS ix_cam_bank_turnover
    ON cashflow_entries(application_id, entry_date, channel)
    WHERE source_type = 'bank_turnover';
