from datetime import date
from decimal import Decimal
from app.services.loan_servicing_service import generate_schedule

def test_flat_rate_rounding_absorption_enterprise_loan():
    # OBOH CLETUS loan parameters
    principal = 500000.00
    annual_rate = 60.0  # 5% flat monthly * 12 months = 60% p.a.
    tenor_months = 6
    frequency = "monthly"
    method = "flat_rate"
    disbursement_date = date(2026, 3, 16)

    # Generate schedule
    rows = generate_schedule(
        principal=principal,
        annual_rate=annual_rate,
        tenor_months=tenor_months,
        frequency=frequency,
        method=method,
        disbursement_date=disbursement_date
    )

    # Assertions
    assert len(rows) == 6

    # Verify first 5 rows
    for i in range(5):
        assert rows[i]["installment_no"] == i + 1
        assert rows[i]["principal_due"] == 83333.33
        assert rows[i]["interest_due"] == 25000.00
        assert rows[i]["total_due"] == 108333.33

    # Verify final row has absorbed the rounding difference of 0.02
    assert rows[5]["installment_no"] == 6
    assert rows[5]["principal_due"] == 83333.35
    assert rows[5]["interest_due"] == 25000.00
    assert rows[5]["total_due"] == 108333.35

    # Verify sums reconcile to exact totals
    total_principal = sum(row["principal_due"] for row in rows)
    total_interest = sum(row["interest_due"] for row in rows)
    total_due = sum(row["total_due"] for row in rows)

    assert round(total_principal, 2) == 500000.00
    assert round(total_interest, 2) == 150000.00
    assert round(total_due, 2) == 650000.00

def test_flat_rate_weekly_schedule_save_n_borrow_basic():
    # RHODA OKORO loan parameters (if schedule is generated)
    principal = 500000.00
    annual_rate = 54.0  # 4.5% flat monthly * 12 months = 54% p.a.
    tenor_months = 3
    frequency = "weekly"
    method = "flat_rate"
    disbursement_date = date(2026, 6, 23)

    # Generate schedule (3 months * 4 = 12 weeks)
    rows = generate_schedule(
        principal=principal,
        annual_rate=annual_rate,
        tenor_months=tenor_months,
        frequency=frequency,
        method=method,
        disbursement_date=disbursement_date
    )

    assert len(rows) == 12

    # Verify final row absorbs rounding
    total_principal = sum(row["principal_due"] for row in rows)
    total_interest = sum(row["interest_due"] for row in rows)
    total_due = sum(row["total_due"] for row in rows)

    assert round(total_principal, 2) == 500000.00
    assert round(total_interest, 2) == 67500.00
    assert round(total_due, 2) == 567500.00
