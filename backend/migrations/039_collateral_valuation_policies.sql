-- Configurable collateral haircuts. CBN/NCR requires lender due diligence;
-- these percentages are bank policy defaults, not universal regulatory rates.

CREATE TABLE IF NOT EXISTS collateral_valuation_policies (
    asset_class TEXT PRIMARY KEY,
    display_name TEXT NOT NULL,
    retention_rate NUMERIC(5,4) NOT NULL CHECK (retention_rate > 0 AND retention_rate <= 1),
    max_valuation_age_days INTEGER NOT NULL CHECK (max_valuation_age_days > 0),
    manual_review_required BOOLEAN NOT NULL DEFAULT FALSE,
    policy_note TEXT NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

INSERT INTO collateral_valuation_policies
    (asset_class, display_name, retention_rate, max_valuation_age_days, manual_review_required, policy_note)
VALUES
    ('property', 'Land / Building', 0.7000, 180, TRUE, 'Use a current independent valuation and confirm title and saleability.'),
    ('equipment', 'Equipment / Vehicle', 0.6000, 90, TRUE, 'Allow for condition, removal cost, depreciation, and resale time.'),
    ('gold', 'Gold / Precious Metal', 0.8500, 30, TRUE, 'Use current tested weight, purity, and observable price; do not project appreciation.'),
    ('inventory', 'Durable Stock / Inventory', 0.5000, 60, TRUE, 'Exclude obsolete, damaged, slow-moving, or already pledged stock.'),
    ('fast_moving_goods', 'Fast-Moving Consumer Goods', 0.4500, 30, TRUE, 'Apply a volatility and bulk-sale discount to verified saleable stock.'),
    ('petty_perishable_goods', 'Petty / Perishable Goods', 0.2500, 14, TRUE, 'High spoilage, price-volatility, and recovery risk; require a fresh stock count.'),
    ('cash', 'Cash Collateral', 1.0000, 30, FALSE, 'Count only blocked cash under the bank''s control.')
ON CONFLICT (asset_class) DO UPDATE SET
    display_name = EXCLUDED.display_name,
    retention_rate = EXCLUDED.retention_rate,
    max_valuation_age_days = EXCLUDED.max_valuation_age_days,
    manual_review_required = EXCLUDED.manual_review_required,
    policy_note = EXCLUDED.policy_note,
    active = TRUE,
    updated_at = NOW();

ALTER TABLE collateral_items DROP CONSTRAINT IF EXISTS collateral_items_collateral_type_check;
ALTER TABLE collateral_items ADD CONSTRAINT collateral_items_collateral_type_check
    CHECK (collateral_type IN ('property','equipment','gold','inventory','fast_moving_goods','petty_perishable_goods','cash'));

ALTER TABLE collateral_items ADD COLUMN IF NOT EXISTS retention_rate NUMERIC(5,4);
ALTER TABLE collateral_items ADD COLUMN IF NOT EXISTS valuation_date DATE;
ALTER TABLE collateral_items ADD COLUMN IF NOT EXISTS valuation_source TEXT;
ALTER TABLE collateral_items ADD COLUMN IF NOT EXISTS manual_review_required BOOLEAN NOT NULL DEFAULT FALSE;

-- The original generated column forced every non-cash asset to 70%. Replace it
-- with the assessed value captured at the policy rate in force at assessment.
ALTER TABLE collateral_items DROP COLUMN IF EXISTS force_sale_value;
ALTER TABLE collateral_items ADD COLUMN force_sale_value NUMERIC(15,2) CHECK (force_sale_value >= 0);

UPDATE collateral_items ci
SET retention_rate = p.retention_rate,
    valuation_date = COALESCE(ci.valuation_date, ci.created_at::date),
    manual_review_required = p.manual_review_required,
    force_sale_value = ROUND(
        CASE
            WHEN ci.face_value IS NOT NULL AND ci.face_value > 0 THEN ci.face_value
            ELSE COALESCE(ci.loan_based_price, 0) * p.retention_rate
        END,
        2
    )
FROM collateral_valuation_policies p
WHERE p.asset_class = ci.collateral_type;

ALTER TABLE collateral_items ALTER COLUMN retention_rate SET NOT NULL;
ALTER TABLE collateral_items ALTER COLUMN force_sale_value SET NOT NULL;

