from pathlib import Path
from unittest.mock import AsyncMock, MagicMock, call

import pytest

from app.domains.loans.repository import LoanRepository
from app.core.templates import create_templates


@pytest.mark.asyncio
async def test_resolve_product_code_preserves_canonical_underscores():
    conn = MagicMock()
    conn.fetchrow = AsyncMock(return_value={"code": "salary_advance"})

    result = await LoanRepository(conn).resolve_product_code("salary_advance")

    assert result == "salary_advance"
    conn.fetchrow.assert_awaited_once_with(
        "SELECT code FROM loan_products WHERE LOWER(code) = $1 AND active = TRUE",
        "salary_advance",
    )


@pytest.mark.asyncio
async def test_resolve_product_code_normalizes_underscores_only_for_aliases():
    conn = MagicMock()
    conn.fetchrow = AsyncMock(
        side_effect=[
            None,
            {"product_code": "save_n_borrow_basic"},
            {"code": "save_n_borrow_basic"},
        ]
    )

    result = await LoanRepository(conn).resolve_product_code("save_and_borrow_basic")

    assert result == "save_n_borrow_basic"
    assert conn.fetchrow.await_args_list == [
        call(
            "SELECT code FROM loan_products WHERE LOWER(code) = $1 AND active = TRUE",
            "save_and_borrow_basic",
        ),
        call(
            "SELECT product_code FROM product_aliases WHERE LOWER(alias) = $1",
            "save and borrow basic",
        ),
        call(
            "SELECT code FROM loan_products WHERE code = $1 AND active = TRUE",
            "save_n_borrow_basic",
        ),
    ]


def test_new_application_renders_active_products_from_new_families():
    templates_dir = Path(__file__).resolve().parents[2] / "frontend" / "templates"
    template = create_templates(str(templates_dir)).env.get_template("shared/new_application.html")
    request = MagicMock()
    request.state.csp_nonce = "test-nonce"
    product = {
        "code": "save_n_borrow_basic",
        "name": "Save & Borrow Basic",
        "description": "Savings-linked facility",
        "family": "savings_personal",
        "min_amount": 50000,
        "max_amount": 500000,
        "min_tenor_months": 3,
        "max_tenor_months": 12,
    }

    body = template.render(
        request=request,
        shell="base/desktop_shell.html",
        user=MagicMock(role="account_officer", full_name="Test Officer"),
        db_role="account_officer",
        role_name="Relationship Officer",
        sidebar_component="components/desktop_sidebar_loan_officer.html",
        tabbar_component="components/mobile_tabbar_loan_officer.html",
        metrics={},
        products=[product],
        csp_nonce="test-nonce",
    )

    assert "Savings-Linked Personal Schemes" in body
    assert 'value="save_n_borrow_basic"' in body
    assert "Save &amp; Borrow Basic" in body
