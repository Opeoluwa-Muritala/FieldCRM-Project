"""Apply the explicit, additive demo-tenant seed with an owner connection."""
from __future__ import annotations

import asyncio
from pathlib import Path

import asyncpg

from migrations.security_migration_preflight import connection_parameters

DEMO_ORG_ID = "de000000-0000-4000-8000-000000000001"


async def main() -> None:
    dsn, tls = connection_parameters()
    conn = await asyncpg.connect(dsn, ssl=tls, timeout=15)
    try:
        role = await conn.fetchrow(
            "SELECT current_user AS name, rolsuper, rolbypassrls FROM pg_roles WHERE rolname=current_user"
        )
        if not role or not (role["rolsuper"] or role["rolbypassrls"]):
            raise RuntimeError("Demo seeding requires the owner/maintenance connection, never fieldcrm_app")
        async with conn.transaction():
            await conn.execute("SET LOCAL lock_timeout='10s'")
            await conn.execute("SET LOCAL statement_timeout='120s'")
            await conn.execute(Path(__file__).with_name("demo_tenant_seed.sql").read_text(encoding="utf-8"))
        counts = await conn.fetchrow(
            """SELECT
              (SELECT count(*) FROM branches WHERE org_id=$1) AS branches,
              (SELECT count(*) FROM users WHERE org_id=$1 AND active=TRUE AND deleted_at IS NULL) AS users,
              (SELECT count(*) FROM loan_applications WHERE org_id=$1 AND deleted_at IS NULL) AS applications""",
            DEMO_ORG_ID,
        )
        print(f"demo_org_id={DEMO_ORG_ID}")
        print(f"branches={counts['branches']} users={counts['users']} applications={counts['applications']}")
    finally:
        await conn.close()


if __name__ == "__main__":
    asyncio.run(main())
