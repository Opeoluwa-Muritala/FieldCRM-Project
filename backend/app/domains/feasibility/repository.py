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

    async def save_cam_details(self, application_id: UUID, data: dict, captured_by: UUID) -> None:
        # 1. Replace internal obligations
        await self.conn.execute(
            "DELETE FROM credit_obligations WHERE application_id = $1 AND source_type = 'internal'",
            application_id,
        )
        for row in data.get("internal_obligations", []):
            await self.conn.execute(
                """INSERT INTO credit_obligations
                       (application_id, lender_name, source_type, facility_amount, outstanding_balance,
                        periodic_payment, payment_frequency, remaining_tenor_months,
                        status, verification_status, start_date, end_date, classification, captured_by)
                   VALUES ($1, $2, 'internal', $3, $4, $5, $6, $7, $8, 'verified', $9, $10, $11, $12)""",
                application_id,
                row["lender_name"],
                as_decimal(row.get("facility_amount")),
                as_decimal(row.get("outstanding_balance")),
                as_decimal(row.get("periodic_payment")),
                row.get("payment_frequency", "monthly"),
                int(as_decimal(row["remaining_tenor_months"])) if row.get("remaining_tenor_months") else None,
                row.get("status", "current"),
                row.get("start_date") or None,
                row.get("end_date") or None,
                row.get("classification") or None,
                captured_by,
            )

        # 2. Replace external obligations
        await self.conn.execute(
            "DELETE FROM credit_obligations WHERE application_id = $1 AND source_type = 'external'",
            application_id,
        )
        for row in data.get("external_obligations", []):
            await self.conn.execute(
                """INSERT INTO credit_obligations
                       (application_id, lender_name, source_type, facility_amount, outstanding_balance,
                        periodic_payment, payment_frequency, remaining_tenor_months,
                        status, verification_status, start_date, end_date, classification, captured_by)
                   VALUES ($1, $2, 'external', $3, $4, $5, $6, $7, $8, 'verified', $9, $10, $11, $12)""",
                application_id,
                row["lender_name"],
                as_decimal(row.get("facility_amount")),
                as_decimal(row.get("outstanding_balance")),
                as_decimal(row.get("periodic_payment")),
                row.get("payment_frequency", "monthly"),
                int(as_decimal(row["remaining_tenor_months"])) if row.get("remaining_tenor_months") else None,
                row.get("status", "current"),
                row.get("start_date") or None,
                row.get("end_date") or None,
                row.get("classification") or None,
                captured_by,
            )

        # 3. Replace bank turnover entries
        await self.conn.execute(
            "DELETE FROM cashflow_entries WHERE application_id = $1 AND source_type = 'bank_turnover'",
            application_id,
        )
        for row in data.get("bank_turnovers", []):
            await self.conn.execute(
                """INSERT INTO cashflow_entries
                       (application_id, flow_direction, classification, category,
                        amount, frequency, period_months, entry_date, description, channel,
                        source_type, is_recurring, verification_status, transaction_count, captured_by)
                   VALUES ($1, 'inflow', 'operating', 'bank_turnover', $2, 'monthly', 1, $3, $4, $5,
                           'bank_turnover', TRUE, 'verified', $6, $7)""",
                application_id,
                as_decimal(row["amount"]),
                row.get("entry_date"),
                row.get("description") or None,
                row.get("channel") or None,
                int(as_decimal(row.get("transaction_count") or 0)),
                captured_by,
            )

        # 4. Upsert financial profile
        prof = data.get("profile", {})
        await self.conn.execute(
            """INSERT INTO borrower_financial_profiles
                   (application_id, cash_at_bank, stock, prepayment, fixed_assets,
                    monthly_turnover, margin, monthly_expenses, recommended_amount,
                    interest_rate, proposed_tenor, remita_email, remita_account_no,
                    remita_account_name, remita_bank, property_coordinates_link,
                    property_description, analyst_name, analyst_recommendation,
                    pre_disbursement_conditions, shop_allocation, shop_allowance,
                    shop_allowance_verified, source_type, verification_status, captured_by,
                    verified_by, verified_at)
               VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, $13, $14, $15, $16, $17, $18, $19, $20, $21, $22, $23, 'manual', 'verified', $24, $24, NOW())
               ON CONFLICT (application_id) DO UPDATE SET
                   cash_at_bank = EXCLUDED.cash_at_bank,
                   stock = EXCLUDED.stock,
                   prepayment = EXCLUDED.prepayment,
                   fixed_assets = EXCLUDED.fixed_assets,
                   monthly_turnover = EXCLUDED.monthly_turnover,
                   margin = EXCLUDED.margin,
                   monthly_expenses = EXCLUDED.monthly_expenses,
                   recommended_amount = EXCLUDED.recommended_amount,
                   interest_rate = EXCLUDED.interest_rate,
                   proposed_tenor = EXCLUDED.proposed_tenor,
                   remita_email = EXCLUDED.remita_email,
                   remita_account_no = EXCLUDED.remita_account_no,
                   remita_account_name = EXCLUDED.remita_account_name,
                   remita_bank = EXCLUDED.remita_bank,
                   property_coordinates_link = EXCLUDED.property_coordinates_link,
                   property_description = EXCLUDED.property_description,
                   analyst_name = EXCLUDED.analyst_name,
                   analyst_recommendation = EXCLUDED.analyst_recommendation,
                   pre_disbursement_conditions = EXCLUDED.pre_disbursement_conditions,
                   shop_allocation = EXCLUDED.shop_allocation,
                   shop_allowance = EXCLUDED.shop_allowance,
                   shop_allowance_verified = EXCLUDED.shop_allowance_verified,
                   verification_status = 'verified',
                   captured_by = EXCLUDED.captured_by,
                   verified_by = EXCLUDED.verified_by,
                   verified_at = NOW(),
                   updated_at = NOW()""",
            application_id,
            as_decimal(prof.get("cash_at_bank")),
            as_decimal(prof.get("stock")),
            as_decimal(prof.get("prepayment")),
            as_decimal(prof.get("fixed_assets")),
            as_decimal(prof.get("monthly_turnover")),
            as_decimal(prof.get("margin")),
            as_decimal(prof.get("monthly_expenses")),
            as_decimal(prof.get("recommended_amount")),
            as_decimal(prof.get("interest_rate")),
            int(as_decimal(prof.get("proposed_tenor") or 12)),
            prof.get("remita_email") or None,
            prof.get("remita_account_no") or None,
            prof.get("remita_account_name") or None,
            prof.get("remita_bank") or None,
            prof.get("property_coordinates_link") or None,
            prof.get("property_description") or None,
            prof.get("analyst_name") or None,
            prof.get("analyst_recommendation") or None,
            prof.get("pre_disbursement_conditions") or None,
            prof.get("shop_allocation") or None,
            as_decimal(prof.get("shop_allowance")),
            as_decimal(prof.get("shop_allowance_verified")),
            captured_by,
        )

        # 5. Update Guarantors verification details
        for g_data in data.get("guarantors", []):
            slot = int(g_data["slot"])
            await self.conn.execute(
                """INSERT INTO guarantors
                       (loan_id, org_id, slot, full_name, relationship_to_client, bvn, phone,
                        business_name, business_address, description_landmark, form_stage)
                   VALUES ($1, (SELECT org_id FROM loan_applications WHERE id = $1), $2, $3, $4, $5, $6, $7, $8, $9, 'verified')
                   ON CONFLICT (loan_id, slot) DO UPDATE SET
                       full_name = COALESCE(NULLIF(EXCLUDED.full_name, ''), guarantors.full_name),
                       relationship_to_client = COALESCE(NULLIF(EXCLUDED.relationship_to_client, ''), guarantors.relationship_to_client),
                       bvn = COALESCE(NULLIF(EXCLUDED.bvn, ''), guarantors.bvn),
                       phone = COALESCE(NULLIF(EXCLUDED.phone, ''), guarantors.phone),
                       business_name = EXCLUDED.business_name,
                       business_address = EXCLUDED.business_address,
                       description_landmark = EXCLUDED.description_landmark,
                       form_stage = 'verified'""",
                application_id,
                slot,
                g_data.get("full_name") or "",
                g_data.get("relationship_to_client") or "",
                g_data.get("bvn") or "",
                g_data.get("phone") or "",
                g_data.get("business_name") or None,
                g_data.get("business_address") or None,
                g_data.get("description_landmark") or None,
            )

        # 6. Update Collateral items details
        for c_data in data.get("collateral_items", []):
            c_id = UUID(c_data["id"])
            await self.conn.execute(
                """UPDATE collateral_items
                   SET owner_type = $1,
                       chassis_no = $2,
                       registration_no = $3,
                       colour = $4,
                       year = $5,
                       cam_forced_sale_value = $6,
                       updated_at = NOW()
                   WHERE id = $7 AND application_id = $8""",
                c_data.get("owner_type") or None,
                c_data.get("chassis_no") or None,
                c_data.get("registration_no") or None,
                c_data.get("colour") or None,
                int(as_decimal(c_data.get("year"))) if c_data.get("year") else None,
                as_decimal(c_data.get("forced_sale_value")),
                c_id,
                application_id,
            )
