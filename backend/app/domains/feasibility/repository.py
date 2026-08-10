"""Persistence for feasibility inputs."""
from __future__ import annotations

from uuid import UUID

from app.domains.feasibility.calculator import as_decimal


class FeasibilityRepository:
    def __init__(self, conn):
        self.conn = conn

    async def get_inputs(self, application_id: UUID) -> tuple[list[dict], dict | None, list[dict]]:
        cashflows = await self.conn.fetch(
            """SELECT * FROM cashflow_entries WHERE application_id = $1
               ORDER BY entry_date NULLS LAST, created_at, id""",
            application_id,
        )
        profile = await self.conn.fetchrow(
            "SELECT * FROM borrower_financial_profiles WHERE application_id = $1",
            application_id,
        )
        obligations = await self.conn.fetch(
            """SELECT * FROM credit_obligations WHERE application_id = $1
               ORDER BY created_at, id""",
            application_id,
        )
        return [dict(row) for row in cashflows], dict(profile) if profile else None, [dict(row) for row in obligations]

    async def replace_declared_cashflows(self, application_id: UUID, rows: list[dict], captured_by: UUID) -> None:
        await self.conn.execute(
            """DELETE FROM cashflow_entries
               WHERE application_id = $1
                 AND source_type IN ('manual', 'legacy_pnl_seed', 'legacy_salary_seed')""",
            application_id,
        )
        for row in rows:
            await self.conn.execute(
                """INSERT INTO cashflow_entries
                       (application_id, flow_direction, classification, category,
                        amount, frequency, period_months, description, channel,
                        source_type, is_recurring, verification_status, captured_by)
                   VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9,
                           'manual', $10, 'declared', $11)""",
                application_id,
                row["flow_direction"],
                row["classification"],
                row["category"],
                as_decimal(row["amount"]),
                row["frequency"],
                as_decimal(row.get("period_months") or 1),
                row.get("description") or None,
                row.get("channel") or None,
                bool(row.get("is_recurring", True)),
                captured_by,
            )

    async def upsert_profile(self, application_id: UUID, values: dict, captured_by: UUID) -> None:
        await self.conn.execute(
            """INSERT INTO borrower_financial_profiles
                   (application_id, essential_household_expenses, verified_other_income,
                    dependants, inventory_value, receivables_value, payables_value,
                    maintenance_capex, source_type, verification_status, captured_by)
               VALUES ($1, $2, $3, $4, $5, $6, $7, $8, 'manual', 'declared', $9)
               ON CONFLICT (application_id) DO UPDATE SET
                   essential_household_expenses = EXCLUDED.essential_household_expenses,
                   verified_other_income = EXCLUDED.verified_other_income,
                   dependants = EXCLUDED.dependants,
                   inventory_value = EXCLUDED.inventory_value,
                   receivables_value = EXCLUDED.receivables_value,
                   payables_value = EXCLUDED.payables_value,
                   maintenance_capex = EXCLUDED.maintenance_capex,
                   source_type = 'manual',
                   verification_status = CASE
                       WHEN borrower_financial_profiles.verification_status = 'verified'
                       THEN 'under_review' ELSE 'declared' END,
                   verified_by = NULL,
                   verified_at = NULL,
                   updated_at = NOW()""",
            application_id,
            as_decimal(values.get("household_expenses")),
            as_decimal(values.get("verified_other_income")),
            int(as_decimal(values.get("dependants"))),
            as_decimal(values.get("inventory_value")),
            as_decimal(values.get("receivables_value")),
            as_decimal(values.get("payables_value")),
            as_decimal(values.get("maintenance_capex")),
            captured_by,
        )

    async def replace_declared_obligations(self, application_id: UUID, rows: list[dict], captured_by: UUID) -> None:
        await self.conn.execute(
            "DELETE FROM credit_obligations WHERE application_id = $1 AND source_type = 'declared'",
            application_id,
        )
        for row in rows:
            await self.conn.execute(
                """INSERT INTO credit_obligations
                       (application_id, lender_name, source_type, outstanding_balance,
                        periodic_payment, payment_frequency, remaining_tenor_months,
                        status, verification_status, captured_by)
                   VALUES ($1, $2, 'declared', $3, $4, $5, $6, $7, 'declared', $8)""",
                application_id,
                row["lender_name"],
                as_decimal(row.get("outstanding_balance")),
                as_decimal(row.get("periodic_payment")),
                row.get("payment_frequency", "monthly"),
                int(as_decimal(row["remaining_tenor_months"])) if row.get("remaining_tenor_months") else None,
                row.get("status", "current"),
                captured_by,
            )
