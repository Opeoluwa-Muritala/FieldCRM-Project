"""Report whether the configured PostgreSQL connection can run security migrations.

No credentials or protected field values are printed.
"""
from __future__ import annotations

import asyncio
import ssl
from urllib.parse import parse_qsl, urlencode, urlsplit, urlunsplit

import asyncpg

from app.config import settings


def connection_parameters() -> tuple[str, ssl.SSLContext | None]:
    if not settings.DATABASE_URL.startswith("postgresql"):
        raise RuntimeError("Security migrations require PostgreSQL")
    parts = urlsplit(settings.DATABASE_URL.replace("postgresql+asyncpg://", "postgresql://", 1))
    query = dict(parse_qsl(parts.query, keep_blank_values=True))
    sslmode = query.pop("sslmode", "").lower()
    query.pop("channel_binding", None)
    dsn = urlunsplit((parts.scheme, parts.netloc, parts.path, urlencode(query), parts.fragment))
    tls = ssl.create_default_context() if sslmode in {"require", "verify-ca", "verify-full"} else None
    return dsn, tls


async def main() -> None:
    dsn, tls = connection_parameters()
    conn = await asyncpg.connect(dsn, ssl=tls, timeout=15)
    try:
        state = await conn.fetchrow(
            """SELECT current_database() AS database_name, current_user AS role_name,
                      r.rolsuper, r.rolbypassrls
               FROM pg_roles r WHERE r.rolname = current_user"""
        )
        protected = await conn.fetchrow(
            """SELECT c.relowner::regrole::text AS owner,
                      c.relrowsecurity AS rls_enabled, c.relforcerowsecurity AS rls_forced
               FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace
               WHERE n.nspname='public' AND c.relname='loan_applications'"""
        )
        roles = await conn.fetch(
            "SELECT rolname, rolcanlogin, rolsuper, rolbypassrls FROM pg_roles WHERE rolname IN ('fieldcrm_app','fieldcrm_worker') ORDER BY rolname"
        )
        columns = await conn.fetchval(
            """SELECT count(*) FROM information_schema.columns
               WHERE table_schema='public' AND (
                 (table_name='loan_applications' AND column_name='bvn_lookup_hash') OR
                 (table_name='guarantors' AND column_name IN ('bvn_lookup_hash','account_lookup_hash'))
               )"""
        )
        print(f"database={state['database_name']}")
        print(f"connected_role={state['role_name']}")
        print(f"connected_role_superuser={state['rolsuper']}")
        print(f"connected_role_bypassrls={state['rolbypassrls']}")
        print(f"loan_table_owner={protected['owner'] if protected else 'missing'}")
        print(f"loan_rls_enabled={protected['rls_enabled'] if protected else 'missing'}")
        print(f"loan_rls_forced={protected['rls_forced'] if protected else 'missing'}")
        print(f"runtime_roles={','.join(row['rolname'] for row in roles) or 'missing'}")
        for runtime_role in roles:
            print(f"{runtime_role['rolname']}_can_login={runtime_role['rolcanlogin']}")
        print(f"sensitive_lookup_columns={columns}/3")
        can_migrate = bool(state["rolsuper"] or (protected and protected["owner"] == state["role_name"]))
        print(f"can_apply_schema_migrations={str(can_migrate).lower()}")
    finally:
        await conn.close()


if __name__ == "__main__":
    asyncio.run(main())
