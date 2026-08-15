from pathlib import Path
from uuid import UUID

import pytest
from fastapi import HTTPException, status
from starlette.requests import Request

from app.domains.demo import router as demo
from app import main


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


def test_demo_mode_never_uses_the_staff_login(monkeypatch):
    monkeypatch.setattr(main.settings, "DEMO_ENABLED", True)
    monkeypatch.setattr(main.settings, "VERCEL_ENV", "preview")

    assert main.browser_login_url() == "/demo"
    assert main.browser_login_url("/users") == "/demo"


def demo_request(path: str) -> Request:
    return Request({
        "type": "http",
        "method": "GET",
        "path": path,
        "root_path": "",
        "scheme": "https",
        "query_string": b"",
        "headers": [(b"host", b"demo.example")],
        "client": ("127.0.0.1", 1234),
        "server": ("demo.example", 443),
    })


@pytest.mark.asyncio
async def test_demo_login_route_and_expired_session_return_to_presenter(monkeypatch):
    monkeypatch.setattr(main.settings, "DEMO_ENABLED", True)
    monkeypatch.setattr(main.settings, "VERCEL_ENV", "preview")

    login_response = await main.render_login(demo_request("/login"))
    expired_response = await main.http_exception_handler(
        demo_request("/users"),
        HTTPException(status_code=status.HTTP_401_UNAUTHORIZED),
    )

    assert login_response.headers["location"] == "/demo"
    assert expired_response.headers["location"] == "/demo"


@pytest.mark.asyncio
async def test_ending_presenter_session_returns_to_demo():
    response = await demo.lock_presenter()

    assert response.headers["location"] == "/demo"


def test_seed_is_not_an_automatic_production_migration():
    from migrations import run_migration

    assert "demo_tenant_seed.sql" not in run_migration.MIGRATION_FILES


def test_demo_seed_contains_a_complete_single_intake():
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


def test_demo_seed_contains_two_complete_internal_guarantor_forms():
    seed = Path("backend/migrations/demo_tenant_seed.sql").read_text(encoding="utf-8")

    assert "'guarantor_1'" in seed
    assert "'guarantor_2'" in seed
    for field in (
        '"name"', '"relationship"', '"phone"', '"dob"', '"origin_lga"',
        '"home_address"', '"existing_loans"', '"marital_status"', '"dependants"',
        '"spouse_info"', '"employment_type"', '"employer_name"', '"monthly_salary"',
        '"employer_address"', '"business_sector"', '"business_turnover"',
        '"passport_photo_verified"', '"id_document_verified"',
        '"declaration_accept"', '"max_guarantee"', '"bank_name"',
    ):
        assert seed.count(field) >= 2

    # Restricted values are injected only through the encryption-aware seeder.
    assert '"bvn"' not in seed
    assert '"account_number"' not in seed
    assert '"cheque_number"' not in seed
