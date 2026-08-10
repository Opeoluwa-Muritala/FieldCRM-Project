"""Pure, Decimal-based feasibility calculations.

The calculator deliberately has no database or HTTP dependencies so the web,
mobile, reporting, and approval paths all use the same financial result.
"""
from __future__ import annotations

from decimal import Decimal, ROUND_HALF_UP
from typing import Any, Iterable, Mapping


ZERO = Decimal("0")
MONEY = Decimal("0.01")


def as_decimal(value: Any) -> Decimal:
    if value in (None, ""):
        return ZERO
    return Decimal(str(value))


def monthly_equivalent(amount: Any, frequency: str, period_months: Any = 1) -> Decimal:
    value = as_decimal(amount)
    if frequency == "quarterly":
        return value / Decimal("3")
    if frequency == "annual":
        return value / Decimal("12")
    factors = {
        "daily": Decimal("365") / Decimal("12"),
        "weekly": Decimal("52") / Decimal("12"),
        "biweekly": Decimal("26") / Decimal("12"),
        "monthly": Decimal("1"),
        "one_off": ZERO,
    }
    if frequency == "period_total":
        months = max(as_decimal(period_months), Decimal("0.01"))
        return value / months
    return value * factors.get(frequency, Decimal("1"))


def _included(row: Mapping[str, Any]) -> bool:
    return row.get("verification_status", "declared") not in {"rejected", "excluded", "stale"}


def _sum_monthly(rows: Iterable[Mapping[str, Any]], *, direction: str, classifications: set[str]) -> Decimal:
    total = ZERO
    for row in rows:
        if not _included(row):
            continue
        if row.get("flow_direction") != direction or row.get("classification") not in classifications:
            continue
        total += monthly_equivalent(row.get("amount"), row.get("frequency", "monthly"), row.get("period_months", 1))
    return total


def calculate_feasibility(
    cashflows: Iterable[Mapping[str, Any]],
    profile: Mapping[str, Any] | None,
    obligations: Iterable[Mapping[str, Any]],
    *,
    proposed_payment: Any,
    proposed_payment_frequency: str = "monthly",
) -> dict[str, Any]:
    rows = list(cashflows)
    debts = list(obligations)
    profile = profile or {}

    operating_inflows = _sum_monthly(rows, direction="inflow", classifications={"operating"})
    operating_outflows = _sum_monthly(rows, direction="outflow", classifications={"operating"})
    personal_outflows = _sum_monthly(rows, direction="outflow", classifications={"personal"})
    household_expenses = as_decimal(profile.get("essential_household_expenses"))
    other_income = as_decimal(profile.get("verified_other_income"))
    maintenance_capex = as_decimal(profile.get("maintenance_capex"))

    operating_cashflow = operating_inflows - operating_outflows
    cash_available = operating_cashflow + other_income - personal_outflows - household_expenses - maintenance_capex

    existing_debt_service = sum(
        (
            monthly_equivalent(debt.get("periodic_payment"), debt.get("payment_frequency", "monthly"))
            for debt in debts
            if _included(debt)
        ),
        ZERO,
    )
    proposed_debt_service = monthly_equivalent(proposed_payment, proposed_payment_frequency)
    total_debt_service = existing_debt_service + proposed_debt_service
    dscr = cash_available / total_debt_service if total_debt_service > 0 else None
    residual_cash = cash_available - total_debt_service

    inventory = as_decimal(profile.get("inventory_value"))
    receivables = as_decimal(profile.get("receivables_value"))
    payables = as_decimal(profile.get("payables_value"))
    annual_sales = operating_inflows * Decimal("12")
    annual_cost = operating_outflows * Decimal("12")
    inventory_days = inventory / annual_cost * Decimal("365") if annual_cost > 0 else ZERO
    receivable_days = receivables / annual_sales * Decimal("365") if annual_sales > 0 else ZERO
    payable_days = payables / annual_cost * Decimal("365") if annual_cost > 0 else ZERO
    cash_conversion_cycle = inventory_days + receivable_days - payable_days
    working_capital_need = annual_cost / Decimal("365") * max(cash_conversion_cycle, ZERO)

    verified_count = sum(1 for row in rows if row.get("verification_status") == "verified")
    included_count = sum(1 for row in rows if _included(row))

    def money(value: Decimal) -> Decimal:
        return value.quantize(MONEY, rounding=ROUND_HALF_UP)

    return {
        "monthly_operating_inflows": money(operating_inflows),
        "monthly_operating_outflows": money(operating_outflows),
        "monthly_operating_cashflow": money(operating_cashflow),
        "monthly_personal_outflows": money(personal_outflows + household_expenses),
        "monthly_cash_available": money(cash_available),
        "monthly_existing_debt_service": money(existing_debt_service),
        "monthly_proposed_debt_service": money(proposed_debt_service),
        "monthly_total_debt_service": money(total_debt_service),
        "dscr": dscr.quantize(Decimal("0.0001"), rounding=ROUND_HALF_UP) if dscr is not None else None,
        "monthly_residual_cash": money(residual_cash),
        "inventory_days": inventory_days.quantize(Decimal("0.1"), rounding=ROUND_HALF_UP),
        "receivable_days": receivable_days.quantize(Decimal("0.1"), rounding=ROUND_HALF_UP),
        "payable_days": payable_days.quantize(Decimal("0.1"), rounding=ROUND_HALF_UP),
        "cash_conversion_cycle_days": cash_conversion_cycle.quantize(Decimal("0.1"), rounding=ROUND_HALF_UP),
        "working_capital_need": money(working_capital_need),
        "cashflow_entry_count": len(rows),
        "included_cashflow_count": included_count,
        "verified_cashflow_count": verified_count,
        "data_quality_status": "verified" if included_count > 0 and verified_count == included_count else "declared",
    }
