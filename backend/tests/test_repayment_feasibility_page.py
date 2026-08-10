import inspect
from decimal import Decimal
from types import SimpleNamespace

from starlette.requests import Request

from app.core.template_utils import build_template_context
from app.domains.loans.collateral import render_repayment_feasibility, templates


def make_request() -> Request:
    request = Request(
        {
            "type": "http",
            "method": "GET",
            "path": "/applications/7687f464-f782-423e-83c8-4ee08eaa6853/repayment-feasibility",
            "raw_path": b"/applications/7687f464-f782-423e-83c8-4ee08eaa6853/repayment-feasibility",
            "query_string": b"",
            "headers": [],
            "scheme": "https",
            "server": ("testserver", 443),
            "client": ("127.0.0.1", 1234),
            "root_path": "",
        }
    )
    request.state.csp_nonce = "test-nonce"
    return request


def test_repayment_feasibility_renders_decimal_database_values():
    request = make_request()
    user = SimpleNamespace(role="credit_analyst", name="Ada Analyst")
    app = SimpleNamespace(
        stage="credit_review",
        applicant_name="Test Customer",
        ref_no="FIELDCRM-2026-TEST",
        amount=Decimal("500000.00"),
        tenor_months=12,
    )
    context = build_template_context(
        request,
        user,
        app=app,
        app_id="7687f464-f782-423e-83c8-4ee08eaa6853",
        feasibility={
            "monthly_operating_inflows": Decimal("150000.00"),
            "monthly_operating_outflows": Decimal("50000.00"),
            "monthly_cash_available": Decimal("100000.00"),
            "monthly_total_debt_service": Decimal("50000.00"),
            "monthly_residual_cash": Decimal("50000.00"),
            "working_capital_need": Decimal("25000.00"),
            "dscr": Decimal("2.0000"),
            "data_quality_status": "verified",
            "verified_cashflow_count": 2,
            "included_cashflow_count": 2,
            "cash_conversion_cycle_days": Decimal("30.0"),
        },
        obligations=[],
        collateral_items=[],
        total_market_value=Decimal("600000.00"),
        market_coverage_ratio=Decimal("1.20"),
        total_pledged_value=Decimal("400000.00"),
        coverage_ratio=Decimal("0.80"),
        proposed_installment=Decimal("50000.00"),
        proposed_payment_frequency="monthly",
        proposed_interest_rate=24.0,
        active_tab="queue",
        active_page="queue",
    )

    response = templates.TemplateResponse(request, "shared/repayment_feasibility.html", context)
    body = response.body.decode("utf-8")

    assert response.status_code == 200
    assert "Repayment Feasibility" in body
    assert "80.00%" in body
    assert "2.00x" in body


def test_credit_analyst_can_open_repayment_feasibility_page():
    dependency = inspect.signature(render_repayment_feasibility).parameters["current_user"].default.dependency

    assert "credit_analyst" in dependency.allowed_roles
