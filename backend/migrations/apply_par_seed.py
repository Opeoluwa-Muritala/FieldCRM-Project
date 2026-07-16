"""Apply the idempotent development PAR-report seed to the configured database."""
import asyncio
import sys
from pathlib import Path

import asyncpg

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from app.core.config import settings


async def main() -> None:
    seed_sql = (Path(__file__).parent / "019_seed_par_report.sql").read_text(encoding="utf-8")
    connection = await asyncpg.connect(settings.DATABASE_URL, timeout=10)
    try:
        org_id = await connection.fetchval(
            "SELECT id FROM organisations WHERE active = TRUE ORDER BY name LIMIT 1"
        )
        if not org_id:
            raise RuntimeError("No active organisation is available for the PAR development seed.")

        user_id = await connection.fetchval(
            """
            SELECT id FROM users
            WHERE org_id = $1 AND active = TRUE
            ORDER BY CASE WHEN role IN ('loan_officer', 'account_officer') THEN 0 ELSE 1 END,
                     full_name
            LIMIT 1
            """,
            org_id,
        )
        if not user_id:
            raise RuntimeError("The active organisation has no active user for the PAR development seed.")

        seed_sql = seed_sql.replace(
            "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11", str(org_id)
        ).replace(
            "b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a02", str(user_id)
        ).replace(
            "b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a01", str(user_id)
        )
        await connection.execute(seed_sql)
        seeded_count = await connection.fetchval(
            """
            SELECT COUNT(*) FROM loan_applications
            WHERE org_id = $1 AND ref_no LIKE 'MMFB-2026-PAR-%'
            """,
            org_id,
        )
        print(f"PAR development seed applied: {seeded_count} loans.")
    finally:
        await connection.close()


if __name__ == "__main__":
    asyncio.run(main())
