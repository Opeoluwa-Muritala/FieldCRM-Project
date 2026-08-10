from unittest.mock import AsyncMock, MagicMock, call

import pytest

from app.domains.loans.repository import LoanRepository


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
