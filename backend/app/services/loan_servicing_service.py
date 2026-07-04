"""
Loan servicing: repayment schedule generation, collection recording,
CBN loan classification, and PAR computation.
"""
from __future__ import annotations

import uuid
from datetime import date, timedelta
from decimal import Decimal, ROUND_HALF_UP
from typing import List

from app.core.sql import load_sql


# CBN classification thresholds (days past due)
_CLASSIFICATION_THRESHOLDS = [
    (0,   "current"),
    (1,   "olem"),
    (31,  "substandard"),
    (91,  "doubtful"),
    (181, "lost"),
]


def classify_loan(days_past_due: int) -> str:
    """Return CBN classification string based on days past due."""
    result = "current"
    for threshold, label in _CLASSIFICATION_THRESHOLDS:
        if days_past_due >= threshold:
            result = label
    return result


def _add_months(d: date, months: int) -> date:
    month = d.month - 1 + months
    year = d.year + month // 12
    month = month % 12 + 1
    import calendar
    day = min(d.day, calendar.monthrange(year, month)[1])
    return date(year, month, day)


def generate_schedule(
    *,
    principal: float,
    annual_rate: float,
    tenor_months: int,
    frequency: str,
    method: str,
    disbursement_date: date,
) -> list[dict]:
    """
    Generate repayment schedule rows.

    frequency: 'daily' | 'weekly' | 'biweekly' | 'monthly'
    method:    'flat_rate' | 'reducing_balance'

    Returns list of dicts with keys:
        installment_no, due_date, principal_due, interest_due, total_due
    """
    P = Decimal(str(principal))

    # Derive period rate and number of installments from frequency
    freq_map = {
        "daily":    (365, tenor_months * 30),
        "weekly":   (52,  tenor_months * 4),
        "biweekly": (26,  tenor_months * 2),
        "monthly":  (12,  tenor_months),
    }
    periods_per_year, n_installments = freq_map.get(frequency, (12, tenor_months))
    period_rate = Decimal(str(annual_rate)) / Decimal("100") / Decimal(str(periods_per_year))

    def next_due(start: date, i: int) -> date:
        if frequency == "monthly":
            return _add_months(start, i)
        elif frequency == "weekly":
            return start + timedelta(weeks=i)
        elif frequency == "biweekly":
            return start + timedelta(weeks=i * 2)
        else:  # daily
            return start + timedelta(days=i)

    rows = []

    if method == "flat_rate":
        total_interest = P * period_rate * Decimal(str(n_installments))
        interest_per_period = (total_interest / Decimal(str(n_installments))).quantize(
            Decimal("0.01"), rounding=ROUND_HALF_UP
        )
        principal_per_period = (P / Decimal(str(n_installments))).quantize(
            Decimal("0.01"), rounding=ROUND_HALF_UP
        )
        for i in range(1, n_installments + 1):
            rows.append({
                "installment_no": i,
                "due_date": next_due(disbursement_date, i),
                "principal_due": float(principal_per_period),
                "interest_due": float(interest_per_period),
                "total_due": float(principal_per_period + interest_per_period),
            })

    else:  # reducing_balance
        if period_rate == 0:
            principal_per_period = (P / Decimal(str(n_installments))).quantize(
                Decimal("0.01"), rounding=ROUND_HALF_UP
            )
            for i in range(1, n_installments + 1):
                rows.append({
                    "installment_no": i,
                    "due_date": next_due(disbursement_date, i),
                    "principal_due": float(principal_per_period),
                    "interest_due": 0.0,
                    "total_due": float(principal_per_period),
                })
        else:
            # EMI = P * r * (1+r)^n / ((1+r)^n - 1)
            r = period_rate
            n = Decimal(str(n_installments))
            emi = (P * r * (1 + r) ** int(n)) / ((1 + r) ** int(n) - 1)
            emi = emi.quantize(Decimal("0.01"), rounding=ROUND_HALF_UP)
            balance = P
            for i in range(1, n_installments + 1):
                interest = (balance * r).quantize(Decimal("0.01"), rounding=ROUND_HALF_UP)
                principal_part = emi - interest
                if i == n_installments:
                    principal_part = balance  # clear residual
                    emi = principal_part + interest
                balance -= principal_part
                rows.append({
                    "installment_no": i,
                    "due_date": next_due(disbursement_date, i),
                    "principal_due": float(principal_part),
                    "interest_due": float(interest),
                    "total_due": float(principal_part + interest),
                })

    return rows


class LoanServicingService:
    def __init__(self, conn):
        self.conn = conn

    async def create_schedule(
        self,
        *,
        loan_id: uuid.UUID,
        org_id: uuid.UUID,
        principal: float,
        annual_rate: float,
        tenor_months: int,
        frequency: str,
        method: str,
        disbursement_date: date,
    ) -> list[dict]:
        """Generate and persist the repayment schedule for a loan."""
        rows = generate_schedule(
            principal=principal,
            annual_rate=annual_rate,
            tenor_months=tenor_months,
            frequency=frequency,
            method=method,
            disbursement_date=disbursement_date,
        )
        sql = load_sql("loans", "insert_repayment_schedule")
        for row in rows:
            await self.conn.execute(
                sql,
                loan_id,
                org_id,
                row["installment_no"],
                row["due_date"],
                row["principal_due"],
                row["interest_due"],
                row["total_due"],
            )
        return rows

    async def get_schedule(self, loan_id: uuid.UUID, org_id: uuid.UUID) -> list[dict]:
        rows = await self.conn.fetch(
            load_sql("loans", "get_repayment_schedule"), loan_id, org_id
        )
        return [dict(r) for r in rows]

    async def record_payment(
        self,
        *,
        loan_id: uuid.UUID,
        org_id: uuid.UUID,
        payment_date: date,
        amount_paid: float,
        channel: str,
        bank_ref: str | None,
        recorded_by: uuid.UUID,
    ) -> dict:
        row = await self.conn.fetchrow(
            load_sql("loans", "insert_repayment_record"),
            loan_id, org_id, payment_date, amount_paid, channel, bank_ref, recorded_by,
        )
        # Recompute classification after each payment
        await self._reclassify(loan_id, org_id)
        return dict(row)

    async def get_payments(self, loan_id: uuid.UUID, org_id: uuid.UUID) -> list[dict]:
        rows = await self.conn.fetch(
            load_sql("loans", "get_repayment_records"), loan_id, org_id
        )
        return [dict(r) for r in rows]

    async def _reclassify(self, loan_id: uuid.UUID, org_id: uuid.UUID) -> None:
        """Compute days past due from schedule vs. payments and update classification."""
        today = date.today()
        schedule = await self.get_schedule(loan_id, org_id)
        payments = await self.get_payments(loan_id, org_id)

        total_paid = sum(p["amount_paid"] for p in payments)
        cumulative_due = Decimal("0")
        days_past_due = 0

        for inst in schedule:
            cumulative_due += Decimal(str(inst["total_due"]))
            if Decimal(str(total_paid)) >= cumulative_due:
                continue
            # This installment is not fully covered
            due_date = inst["due_date"]
            if isinstance(due_date, str):
                from datetime import datetime
                due_date = datetime.strptime(due_date, "%Y-%m-%d").date()
            if today > due_date:
                days_past_due = max(days_past_due, (today - due_date).days)

        classification = classify_loan(days_past_due)
        await self.conn.execute(
            load_sql("loans", "update_classification"),
            classification, days_past_due, loan_id, org_id,
        )

    async def get_par_summary(self, org_id: uuid.UUID) -> dict:
        row = await self.conn.fetchrow(load_sql("loans", "list_par"), org_id)
        if not row:
            return {}
        d = dict(row)
        total = float(d.get("total_portfolio") or 0)

        def pct(amount):
            return round(float(amount) / total * 100, 2) if total else 0.0

        return {
            "total_loans": d.get("total_loans", 0),
            "total_portfolio": total,
            "par1_count": d.get("par1_count", 0),
            "par1_amount": float(d.get("par1_amount") or 0),
            "par1_pct": pct(d.get("par1_amount") or 0),
            "par30_count": d.get("par30_count", 0),
            "par30_amount": float(d.get("par30_amount") or 0),
            "par30_pct": pct(d.get("par30_amount") or 0),
            "par90_count": d.get("par90_count", 0),
            "par90_amount": float(d.get("par90_amount") or 0),
            "par90_pct": pct(d.get("par90_amount") or 0),
            "olem_count": d.get("olem_count", 0),
            "substandard_count": d.get("substandard_count", 0),
            "doubtful_count": d.get("doubtful_count", 0),
            "lost_count": d.get("lost_count", 0),
        }

    async def reclassify_all_disbursed(self, org_id: uuid.UUID) -> int:
        """Batch reclassify all disbursed loans. Run nightly."""
        rows = await self.conn.fetch(
            load_sql("loans", "list_disbursed"), org_id
        )
        count = 0
        for row in rows:
            await self._reclassify(row["id"], org_id)
            count += 1
        return count
