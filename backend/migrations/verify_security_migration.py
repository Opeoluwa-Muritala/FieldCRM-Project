"""Verify security migration state without printing protected values."""
from __future__ import annotations

import asyncio

import asyncpg

from migrations.security_migration_preflight import connection_parameters


async def main() -> None:
    dsn, tls = connection_parameters()
    conn = await asyncpg.connect(dsn, ssl=tls, timeout=15)
    try:
        loan_plain = await conn.fetchval(
            "SELECT count(*) FROM loan_applications WHERE NULLIF(bvn,'') IS NOT NULL AND bvn NOT LIKE 'enc:v1:%'"
        )
        loan_missing_index = await conn.fetchval(
            "SELECT count(*) FROM loan_applications WHERE NULLIF(bvn,'') IS NOT NULL AND bvn_lookup_hash IS NULL"
        )
        guarantor_plain = await conn.fetchval(
            """SELECT count(*) FROM guarantors WHERE
               (NULLIF(bvn,'') IS NOT NULL AND bvn NOT LIKE 'enc:v1:%') OR
               (NULLIF(account_number,'') IS NOT NULL AND account_number NOT LIKE 'enc:v1:%') OR
               (NULLIF(cheque_number,'') IS NOT NULL AND cheque_number NOT LIKE 'enc:v1:%')"""
        )
        role = await conn.fetchrow(
            "SELECT rolsuper, rolbypassrls, rolinherit FROM pg_roles WHERE rolname='fieldcrm_app'"
        )
        policies = await conn.fetchval(
            "SELECT count(*) FROM pg_policies WHERE schemaname='public'"
        )
        forced_tables = await conn.fetchval(
            """SELECT count(*) FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace
               WHERE n.nspname='public' AND c.relrowsecurity AND c.relforcerowsecurity"""
        )
        print(f"plaintext_loan_identifiers={loan_plain}")
        print(f"loan_rows_missing_blind_index={loan_missing_index}")
        print(f"plaintext_guarantor_identifiers={guarantor_plain}")
        print(f"rls_policies={policies}")
        print(f"rls_forced_tables={forced_tables}")
        print(f"fieldcrm_app_superuser={role['rolsuper'] if role else 'missing'}")
        print(f"fieldcrm_app_bypassrls={role['rolbypassrls'] if role else 'missing'}")
        print(f"fieldcrm_app_inherit={role['rolinherit'] if role else 'missing'}")
        if loan_plain or loan_missing_index or guarantor_plain or not role or role["rolsuper"] or role["rolbypassrls"]:
            raise SystemExit(1)
    finally:
        await conn.close()


if __name__ == "__main__":
    asyncio.run(main())
