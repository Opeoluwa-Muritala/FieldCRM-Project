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
        pnl={"net_profit": Decimal("1200000.00"), "period_label": "Annual"},
        total_pledged_value=Decimal("400000.00"),
        coverage_ratio=Decimal("0.80"),
        proposed_installment=Decimal("50000.00"),
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
