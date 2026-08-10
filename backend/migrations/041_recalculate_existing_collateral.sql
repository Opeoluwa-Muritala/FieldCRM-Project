-- Re-run every existing collateral row through the authoritative policy
-- trigger so legacy user-entered FSVs cannot survive as calculation inputs.
UPDATE collateral_items
SET loan_based_price = loan_based_price;

