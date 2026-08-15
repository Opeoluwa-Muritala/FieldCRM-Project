"""Apply the explicit, additive demo-tenant seed with an owner connection."""
from __future__ import annotations

import asyncio
import json
from pathlib import Path

import asyncpg

from migrations.security_migration_preflight import connection_parameters
from app.core.field_encryption import blind_index, encrypt_sensitive

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
            intake_data = await conn.fetchval(
                "SELECT data_json FROM stage_data WHERE id=$1",
                "de000000-0000-4000-8000-000000000401",
            )
            if isinstance(intake_data, str):
                intake_data = json.loads(intake_data)
            intake_data = dict(intake_data or {})
            intake_data["bvn"] = encrypt_sensitive("00000000000", context="intake:bvn")
            intake_data["account_number"] = encrypt_sensitive("0000000000", context="intake:account_number")
            await conn.execute(
                "UPDATE stage_data SET data_json=$1::jsonb,saved_at=NOW() WHERE id=$2",
                json.dumps(intake_data),
                "de000000-0000-4000-8000-000000000401",
            )
            await conn.execute(
                """UPDATE loan_applications SET bvn=$1,bvn_lookup_hash=$2,updated_at=NOW()
                   WHERE id=$3 AND org_id=$4""",
                encrypt_sensitive("00000000000", context="loan_application:bvn"),
                blind_index("00000000000", context="loan_application:bvn"),
                "de000000-0000-4000-8000-000000000301",
                DEMO_ORG_ID,
            )
        counts = await conn.fetchrow(
            """SELECT
              (SELECT count(*) FROM branches WHERE org_id=$1) AS branches,
              (SELECT count(*) FROM users WHERE org_id=$1 AND active=TRUE AND deleted_at IS NULL) AS users,
              (SELECT count(*) FROM loan_applications WHERE org_id=$1 AND deleted_at IS NULL) AS applications,
              (SELECT count(*) FROM documents WHERE org_id=$1 AND deleted_at IS NULL) AS documents,
              (SELECT count(*) FROM verification_checks WHERE loan_application_id='de000000-0000-4000-8000-000000000301') AS identity_checks,
              (SELECT count(*) FROM checklist_items WHERE loan_application_id='de000000-0000-4000-8000-000000000301') AS checklist_items,
              (SELECT count(*) FROM pledged_items WHERE loan_id='de000000-0000-4000-8000-000000000301') AS pledged_items,
              (SELECT count(*) FROM notifications WHERE org_id=$1 AND type='demo') AS role_guides""",
            DEMO_ORG_ID,
        )
        print(f"demo_org_id={DEMO_ORG_ID}")
        print(f"branches={counts['branches']} users={counts['users']} applications={counts['applications']}")
        print(
            f"documents={counts['documents']} identity_checks={counts['identity_checks']} "
            f"checklist_items={counts['checklist_items']} pledged_items={counts['pledged_items']} "
            f"role_guides={counts['role_guides']}"
        )
    finally:
        await conn.close()


if __name__ == "__main__":
    asyncio.run(main())
