import inspect
from decimal import Decimal
from types import SimpleNamespace

from starlette.requests import Request

from app.core.template_utils import build_template_context
from app.domains.loans.collateral import (
    render_repayment_feasibility,
    save_repayment_feasibility,
    templates,
)


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
        stage="credit_analyst_review",
        applicant_name="Test Customer",
        ref_no="FIELDCRM-2026-TEST",
        amount=Decimal("500000.00"),
        tenor_months=12,
        bvn="12345678901",
        phone="08000000000",
        updated_at=None,
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
        financial_profile={},
        cam_feasibility={
            "total_assets": Decimal("500000.00"),
            "monthly_gross_profit": Decimal("150000.00"),
            "net_profit": Decimal("100000.00"),
            "mainstreet_installment": Decimal("50000.00"),
            "total_external_rental": Decimal("0.00"),
            "total_rental": Decimal("50000.00"),
            "dti_ratio": Decimal("50.00"),
            "asset_to_loan_ratio": None,
            "total_outstanding_loan": Decimal("0.00"),
            "gearing_ratio": Decimal("1.0000"),
            "total_collateral_valuation": Decimal("400000.00"),
            "collateral_coverage": Decimal("80.00"),
        },
        internal_obligations=[], external_obligations=[], bank_turnovers=[],
        guarantors=[], application_header={}, intake_data={}, recommendation_chain=[],
        can_edit_cam=True,
        active_tab="queue",
        active_page="queue",
    )

    response = templates.TemplateResponse(request, "shared/repayment_feasibility.html", context)
    body = response.body.decode("utf-8")

    assert response.status_code == 200
    assert "Feasibility Analysis" in body
    assert "80.00%" in body
    assert "Asset to Loan Ratio (min 2:1)" in body
    assert "Not configured" in body
    assert "Credit analyst assessment" in body
    assert 'name="metric_note_dti"' in body
    section_titles = [
        "Header block",
        "Credit History with Mainstreet MFB (Internal)",
        "External Borrower History",
        "Bank(s) Turnover",
        "Remita Bank Details",
        "Security / Collateral",
        "Appraisal &amp; Parameters",
        "Guarantor(s) Verified by Analyst",
        "Property",
        "Credit Analyst Comments",
        "Recommendation / Approval Chain",
    ]
    positions = [body.index(title) for title in section_titles]
    assert positions == sorted(positions)


def test_credit_analyst_can_open_repayment_feasibility_page():
    dependency = inspect.signature(render_repayment_feasibility).parameters["current_user"].default.dependency

    assert "credit_analyst" in dependency.allowed_roles


def test_only_credit_analyst_can_submit_feasibility_inputs():
    dependency = inspect.signature(save_repayment_feasibility).parameters["current_user"].default.dependency

    assert dependency.allowed_roles == ["credit_analyst"]
