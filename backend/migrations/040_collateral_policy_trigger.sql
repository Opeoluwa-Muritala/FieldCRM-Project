-- Keep collateral valuation authoritative in PostgreSQL and compatible with
-- older application instances during rolling deployments.

CREATE OR REPLACE FUNCTION apply_collateral_valuation_policy()
RETURNS TRIGGER AS $$
DECLARE
    policy collateral_valuation_policies%ROWTYPE;
BEGIN
    SELECT * INTO policy
    FROM collateral_valuation_policies
    WHERE asset_class = NEW.collateral_type AND active = TRUE;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'No active collateral valuation policy for %', NEW.collateral_type;
    END IF;

    NEW.retention_rate := policy.retention_rate;
    NEW.force_sale_value := ROUND(COALESCE(NEW.loan_based_price, 0) * policy.retention_rate, 2);
    NEW.face_value := NEW.force_sale_value;
    NEW.valuation_date := COALESCE(NEW.valuation_date, CURRENT_DATE);
    NEW.valuation_source := COALESCE(NEW.valuation_source, 'policy_calculated');
    NEW.manual_review_required := policy.manual_review_required;
    NEW.updated_at := NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_apply_collateral_valuation_policy ON collateral_items;
CREATE TRIGGER trg_apply_collateral_valuation_policy
BEFORE INSERT OR UPDATE OF collateral_type, loan_based_price
ON collateral_items
FOR EACH ROW EXECUTE FUNCTION apply_collateral_valuation_policy();

