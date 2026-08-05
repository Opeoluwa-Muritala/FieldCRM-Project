"""Seed paired performing and unpaid disbursed loans for the PAR report only."""

import asyncio
import os
from datetime import date, timedelta
from decimal import Decimal

import asyncpg
from dotenv import load_dotenv


load_dotenv("backend/.env")


async def main() -> None:
    database_url = os.getenv("DATABASE_URL")
    if not database_url:
        raise SystemExit("DATABASE_URL is not configured in backend/.env")

    conn = await asyncpg.connect(database_url)
    try:
        async with conn.transaction():
            await conn.execute("ALTER TABLE repayment_records DISABLE TRIGGER USER")
            await conn.execute("DELETE FROM repayment_records")
            await conn.execute("DELETE FROM repayment_schedule")
            await conn.execute("ALTER TABLE repayment_records ENABLE TRIGGER USER")

            loans = await conn.fetch(
                """SELECT la.id, la.org_id, la.ref_no, la.amount, la.tenor_months,
                          row_number() OVER (
                              PARTITION BY la.org_id, la.created_by, la.loan_type
                              ORDER BY la.ref_no
                          ) AS scenario_no,
                          COALESCE(
                              (SELECT id FROM users
                               WHERE org_id=la.org_id AND active=TRUE AND role='crm'
                               ORDER BY created_at LIMIT 1),
                              la.created_by
                          ) AS recorded_by
                   FROM loan_applications la
                   WHERE la.stage='disbursed'
                   ORDER BY la.org_id, la.created_by, la.loan_type, la.ref_no"""
            )
            if not loans:
                raise RuntimeError("No disbursed applications exist; run the main demo seeder first.")

            performing = 0
            unpaid = 0
            for loan in loans:
                principal = Decimal(str(loan["amount"] or 0))
                tenor = max(int(loan["tenor_months"] or 6), 6)
                principal_due = (principal / Decimal(tenor)).quantize(Decimal("0.01"))
                interest_due = (principal * Decimal("0.045")).quantize(Decimal("0.01"))
                total_due = principal_due + interest_due
                has_repayments = int(loan["scenario_no"]) % 2 == 1

                for installment in range(1, 7):
                    due_date = date.today() + timedelta(days=30 * (installment - 4))
                    await conn.execute(
                        """INSERT INTO repayment_schedule
                           (loan_id, org_id, installment_no, due_date, principal_due, interest_due, total_due)
                           VALUES ($1,$2,$3,$4,$5,$6,$7)""",
                        loan["id"], loan["org_id"], installment, due_date,
                        principal_due, interest_due, total_due,
                    )

                if has_repayments:
                    performing += 1
                    for installment in range(1, 4):
                        await conn.execute(
                            """INSERT INTO repayment_records
                               (loan_id, org_id, payment_date, amount_paid, channel, bank_ref, recorded_by)
                               VALUES ($1,$2,$3,$4,$5,$6,$7)""",
                            loan["id"], loan["org_id"],
                            date.today() + timedelta(days=30 * (installment - 4)),
                            total_due, "bank_transfer", f"PAR-PAID-{loan['ref_no']}-{installment}",
                            loan["recorded_by"],
                        )
                    await conn.execute(
                        """UPDATE loan_applications
                           SET classification='current', days_past_due=0, classification_updated_at=NOW()
                           WHERE id=$1""",
                        loan["id"],
                    )
                else:
                    unpaid += 1
                    await conn.execute(
                        """UPDATE loan_applications
                           SET classification='substandard', days_past_due=60, classification_updated_at=NOW()
                           WHERE id=$1""",
                        loan["id"],
                    )

        schedule_count = await conn.fetchval("SELECT count(*) FROM repayment_schedule")
        payment_count = await conn.fetchval("SELECT count(*) FROM repayment_records")
        print(
            f"PAR seed complete: performing={performing}, unpaid={unpaid}, "
            f"schedule_rows={schedule_count}, payment_rows={payment_count}"
        )
    finally:
        await conn.close()


if __name__ == "__main__":
    asyncio.run(main())
