"""Apply the reviewed FieldCRM security migrations as one transaction."""
from __future__ import annotations

import asyncio
from pathlib import Path

import asyncpg

from migrations.security_migration_preflight import connection_parameters

MIGRATIONS = (
    "040_rls_security_foundation.sql",
    "041_sensitive_field_encryption.sql",
)


async def main() -> None:
    dsn, tls = connection_parameters()
    conn = await asyncpg.connect(dsn, ssl=tls, timeout=15)
    root = Path(__file__).resolve().parent
    try:
        async with conn.transaction():
            await conn.execute("SET LOCAL lock_timeout = '10s'")
            await conn.execute("SET LOCAL statement_timeout = '120s'")
            for filename in MIGRATIONS:
                await conn.execute((root / filename).read_text(encoding="utf-8"))
                print(f"applied={filename}")
    finally:
        await conn.close()


if __name__ == "__main__":
    asyncio.run(main())
