from uuid import UUID

import pytest

from app.domains.demo import router as demo


class RecordingConnection:
    def __init__(self):
        self.calls = []

    async def execute(self, query, *args):
        self.calls.append((query, args))


@pytest.mark.asyncio
async def test_demo_read_identity_is_scoped_and_read_only():
    conn = RecordingConnection()
    org_id = UUID("de000000-0000-4000-8000-000000000001")

    await demo._install_demo_read_identity(conn, org_id)

    settings = {args[0]: args[1] for _, args in conn.calls}
    assert settings["app.org_id"] == str(org_id)
    assert settings["app.user_role"] == "auditor"
    assert settings["app.branch_id"] == ""


def test_demo_stage_role_follows_live_workflow():
    assert demo.STAGE_ROLE["intake"] == "account_officer"
    assert demo.STAGE_ROLE["branch_manager_review"] == "branch_manager"
    assert demo.STAGE_ROLE["branch_supervisor_review"] == "branch_supervisor"
    assert demo.STAGE_ROLE["credit_analyst_review"] == "credit_analyst"
    assert demo.STAGE_ROLE["crm_review"] == "crm"
    assert demo.STAGE_ROLE["head_crm_review"] == "head_crm"
    assert demo.STAGE_ROLE["ed_approval"] == "ed"
    assert demo.STAGE_ROLE["md_approval"] == "md"
    assert demo.STAGE_ROLE["disbursement_ready"] == "crm"


def test_every_demo_role_has_a_guided_landing_screen():
    assert set(demo.ROLE_ORDER) == set(demo.ROLE_LANDING)
    assert set(demo.ROLE_ORDER) == set(demo.ROLE_SCREENS)


def test_seed_is_not_an_automatic_production_migration():
    from migrations import run_migration

    assert "demo_tenant_seed.sql" not in run_migration.MIGRATION_FILES


def test_demo_seed_contains_a_complete_single_intake():
    from pathlib import Path

    seed = Path("backend/migrations/demo_tenant_seed.sql").read_text(encoding="utf-8")
    for field in (
        '"full_name"', '"spouse_name"', '"guarantor_1_name"',
        '"monthly_sales"', '"cashflow_amount"', '"facility_bank"',
        '"loan_purpose"', '"collateral_type"', '"account_name"',
        '"pledge_item_name"',
    ):
        assert field in seed
    assert '"guarantor_signature"' not in seed
    assert '"witness_signature"' not in seed
