from decimal import Decimal

from app.domains.feasibility.calculator import calculate_feasibility, monthly_equivalent


def test_monthly_equivalent_normalizes_common_frequencies():
    assert monthly_equivalent("1200", "annual") == Decimal("100")
    assert monthly_equivalent("300", "quarterly") == Decimal("100")
    assert monthly_equivalent("1200", "period_total", 6) == Decimal("200")


def test_feasibility_includes_household_and_all_debt_service():
    cashflows = [
        {
            "flow_direction": "inflow",
            "classification": "operating",
            "amount": "100000",
            "frequency": "monthly",
            "verification_status": "verified",
        },
        {
            "flow_direction": "outflow",
            "classification": "operating",
            "amount": "40000",
            "frequency": "monthly",
            "verification_status": "verified",
        },
        {
            "flow_direction": "outflow",
            "classification": "personal",
            "amount": "5000",
            "frequency": "monthly",
            "verification_status": "verified",
        },
    ]
    profile = {
        "essential_household_expenses": "10000",
        "verified_other_income": "5000",
        "maintenance_capex": "0",
        "inventory_value": "20000",
        "receivables_value": "10000",
        "payables_value": "5000",
    }
    obligations = [{"periodic_payment": "5000", "payment_frequency": "monthly"}]

    result = calculate_feasibility(
        cashflows,
        profile,
        obligations,
        proposed_payment="20000",
        proposed_payment_frequency="monthly",
    )

    assert result["monthly_cash_available"] == Decimal("50000.00")
    assert result["monthly_total_debt_service"] == Decimal("25000.00")
    assert result["dscr"] == Decimal("2.0000")
    assert result["monthly_residual_cash"] == Decimal("25000.00")
    assert result["data_quality_status"] == "verified"


def test_excluded_cash_movement_does_not_inflate_capacity():
    result = calculate_feasibility(
        [
            {
                "flow_direction": "inflow",
                "classification": "operating",
                "amount": "999999",
                "frequency": "monthly",
                "verification_status": "excluded",
            }
        ],
        None,
        [],
        proposed_payment="1000",
    )

    assert result["monthly_operating_inflows"] == Decimal("0.00")
    assert result["monthly_residual_cash"] == Decimal("-1000.00")

