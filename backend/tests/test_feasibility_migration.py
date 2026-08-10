from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]


def test_feasibility_migration_creates_inputs_and_backfills_drafts():
    sql = (ROOT / "migrations" / "038_feasibility_cashflow.sql").read_text(encoding="utf-8")

    assert "CREATE TABLE IF NOT EXISTS cashflow_entries" in sql
    assert "CREATE TABLE IF NOT EXISTS borrower_financial_profiles" in sql
    assert "CREATE TABLE IF NOT EXISTS credit_obligations" in sql
    assert "la.stage = 'intake'" in sql
    assert "legacy_pnl_seed" in sql
    assert "legacy_salary_seed" in sql
    assert "stage_data:intake:facility:" in sql
    assert "ON CONFLICT DO NOTHING" in sql


def test_migration_runner_includes_feasibility_migration():
    runner = (ROOT / "migrations" / "run_migration.py").read_text(encoding="utf-8")
    assert '"038_feasibility_cashflow.sql"' in runner


def test_collateral_policy_migration_has_distinct_asset_haircuts():
    sql = (ROOT / "migrations" / "039_collateral_valuation_policies.sql").read_text(encoding="utf-8")
    assert "collateral_valuation_policies" in sql
    assert "('property', 'Land / Building', 0.7000" in sql
    assert "('gold', 'Gold / Precious Metal', 0.8500" in sql
    assert "('petty_perishable_goods', 'Petty / Perishable Goods', 0.2500" in sql
    assert "do not project appreciation" in sql
    assert '"039_collateral_valuation_policies.sql"' in (
        ROOT / "migrations" / "run_migration.py"
    ).read_text(encoding="utf-8")


def test_collateral_trigger_keeps_database_valuation_authoritative():
    sql = (ROOT / "migrations" / "040_collateral_policy_trigger.sql").read_text(encoding="utf-8")
    assert "BEFORE INSERT OR UPDATE" in sql
    assert "NEW.force_sale_value := ROUND" in sql
    assert "NEW.face_value := NEW.force_sale_value" in sql
    assert '"040_collateral_policy_trigger.sql"' in (
        ROOT / "migrations" / "run_migration.py"
    ).read_text(encoding="utf-8")
    assert "SET loan_based_price = loan_based_price" in (
        ROOT / "migrations" / "041_recalculate_existing_collateral.sql"
    ).read_text(encoding="utf-8")
