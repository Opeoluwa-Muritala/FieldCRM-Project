"""Database access shared across FastAPI invocations.

Postgres runs through SQLAlchemy's async engine with a small process-local pool
in front of Neon's PgBouncer pooler. The small adapter preserves
the existing asyncpg-like repository interface while the application migrates
incrementally to SQLAlchemy statements.
"""
from __future__ import annotations

import asyncio
import json
import re
import sqlite3
import ssl
from contextlib import asynccontextmanager
from urllib.parse import parse_qsl, urlencode, urlsplit, urlunsplit

from sqlalchemy import text
from sqlalchemy.ext.asyncio import AsyncConnection, AsyncEngine, create_async_engine

from app.config import settings

_engine: AsyncEngine | None = None
_sqlite_pool = None
_is_sqlite = settings.DATABASE_URL.startswith("sqlite")


def _async_database_url(url: str) -> tuple[str, dict[str, object]]:
    """Translate libpq SSL URL options to asyncpg connect arguments."""
    if url.startswith("postgresql+asyncpg://"):
        async_url = url
    elif url.startswith("postgresql://"):
        async_url = url.replace("postgresql://", "postgresql+asyncpg://", 1)
    else:
        return url, {"timeout": 10}

    parts = urlsplit(async_url)
    query = dict(parse_qsl(parts.query, keep_blank_values=True))
    sslmode = query.pop("sslmode", "").lower()
    # Neon examples may include this libpq-only option. asyncpg performs TLS
    # verification through the SSLContext instead.
    query.pop("channel_binding", None)
    clean_url = urlunsplit((parts.scheme, parts.netloc, parts.path, urlencode(query), parts.fragment))
    connect_args: dict[str, object] = {"timeout": 10}
    if sslmode in {"require", "verify-ca", "verify-full"}:
        connect_args["ssl"] = ssl.create_default_context()
    return clean_url, connect_args


def init_engine() -> None:
    """Initialise one module-level engine; never create one per request."""
    global _engine, _sqlite_pool
    if _is_sqlite:
        if _sqlite_pool is None:
            _sqlite_pool = SQLitePool(settings.DATABASE_URL)
        return
    if _engine is None:
        database_url, connect_args = _async_database_url(settings.DATABASE_URL)
        _engine = create_async_engine(
            database_url,
            pool_size=5,
            max_overflow=5,
            pool_timeout=5,
            pool_pre_ping=True,
            pool_recycle=280,
            connect_args=connect_args,
        )


async def dispose_engine() -> None:
    global _engine
    if _engine is not None:
        await _engine.dispose()
        _engine = None


# Compatibility aliases for existing scripts while callers move to the engine
# terminology.  They deliberately retain the one-engine-per-process invariant.
async def init_pool() -> None:
    init_engine()


async def close_pool() -> None:
    await dispose_engine()


def _bind(query: str, args: tuple) -> tuple[str, dict[str, object]]:
    """Translate legacy asyncpg ``$1`` parameters for SQLAlchemy text().

    SQLAlchemy's text parser does not recognise ``:name::type`` as a bind
    parameter followed by PostgreSQL's type-cast operator.  Convert typed
    legacy placeholders to standard SQL ``CAST(:name AS type)`` as well.
    """
    params: dict[str, object] = {}

    def replace(match: re.Match[str]) -> str:
        index = int(match.group(1)) - 1
        if index >= len(args):
            raise ValueError(f"Query references ${index + 1}, but only {len(args)} values were supplied")
        key = f"p{index + 1}"
        params[key] = args[index]
        return f":{key}"

    statement = re.sub(r"\$(\d+)", replace, query)
    statement = re.sub(
        r"(:p\d+)::([A-Za-z_][A-Za-z0-9_]*(?:\s*\[\])?)",
        r"CAST(\1 AS \2)",
        statement,
    )
    return statement, params


class SQLAlchemyTransactionContext:
    def __init__(self, conn: "SQLAlchemyConnection"):
        self.conn = conn
        self.transaction = None

    async def __aenter__(self):
        self.transaction = await self.conn._conn.begin()
        return self.conn

    async def __aexit__(self, exc_type, exc_val, exc_tb):
        if exc_type is not None:
            await self.transaction.rollback()
        else:
            await self.transaction.commit()


class SQLAlchemyConnection:
    def __init__(self, conn: AsyncConnection):
        self._conn = conn

    async def fetch(self, query: str, *args):
        statement, params = _bind(query, args)
        result = await self._conn.execute(text(statement), params)
        return [dict(row) for row in result.mappings().all()]

    async def fetchrow(self, query: str, *args):
        statement, params = _bind(query, args)
        result = await self._conn.execute(text(statement), params)
        row = result.mappings().first()
        return dict(row) if row else None

    async def fetchval(self, query: str, *args):
        statement, params = _bind(query, args)
        result = await self._conn.execute(text(statement), params)
        row = result.first()
        return row[0] if row else None

    async def execute(self, query: str, *args):
        statement, params = _bind(query, args)
        result = await self._conn.execute(text(statement), params)
        command = statement.lstrip().split(None, 1)[0].upper()
        return f"{command} {max(result.rowcount or 0, 0)}"

    def transaction(self):
        return SQLAlchemyTransactionContext(self)


class SQLiteTransactionContext:
    def __init__(self, conn):
        self.conn = conn

    async def __aenter__(self):
        await self.conn._conn.execute("BEGIN TRANSACTION;")
        return self.conn

    async def __aexit__(self, exc_type, exc_val, exc_tb):
        await self.conn._conn.execute("ROLLBACK;" if exc_type else "COMMIT;")


class SQLiteConnectionWrapper:
    def __init__(self, conn):
        self._conn = conn

    def _convert_query_and_args(self, query: str, args):
        ordered_args = []

        def replace_param(match):
            ordered_args.append(args[int(match.group(0)[1:]) - 1])
            return "?"

        query = re.sub(r"\$\d+", replace_param, query)
        query = re.sub(r"\?::[A-Za-z_][A-Za-z0-9_]*", "?", query)
        query = query.replace("NOW()", "CURRENT_TIMESTAMP")
        query = query.replace("gen_random_uuid()", "lower(hex(randomblob(16)))")
        return query, [json.dumps(arg) if isinstance(arg, (dict, list)) else arg for arg in ordered_args]

    @staticmethod
    def _parse_row(row):
        if not row:
            return None
        result = dict(row)
        for key, value in result.items():
            if (key == "data_json" or key.endswith("_json")) and isinstance(value, str):
                try:
                    result[key] = json.loads(value)
                except ValueError:
                    pass
        return result

    async def fetch(self, query: str, *args):
        query, args = self._convert_query_and_args(query, args)
        async with self._conn.execute(query, args) as cursor:
            return [self._parse_row(row) for row in await cursor.fetchall()]

    async def fetchrow(self, query: str, *args):
        query, args = self._convert_query_and_args(query, args)
        async with self._conn.execute(query, args) as cursor:
            return self._parse_row(await cursor.fetchone())

    async def fetchval(self, query: str, *args):
        row = await self.fetchrow(query, *args)
        return next(iter(row.values())) if row else None

    async def execute(self, query: str, *args):
        query, args = self._convert_query_and_args(query, args)
        cursor = await self._conn.execute(query, args)
        return f"{query.lstrip().split(None, 1)[0].upper()} {max(cursor.rowcount, 0)}"

    def transaction(self):
        return SQLiteTransactionContext(self)


class SQLitePool:
    def __init__(self, dsn: str):
        self.dsn = dsn.replace("sqlite:///", "").replace("sqlite://", "")

    @asynccontextmanager
    async def acquire(self):
        import aiosqlite
        conn = await aiosqlite.connect(self.dsn)
        conn.row_factory = sqlite3.Row
        await conn.execute("PRAGMA foreign_keys = ON;")
        try:
            yield SQLiteConnectionWrapper(conn)
        finally:
            await conn.close()


@asynccontextmanager
async def get_connection():
    init_engine()
    if _is_sqlite:
        async with _sqlite_pool.acquire() as conn:
            yield conn
        return

    conn = None
    for attempt in range(2):
        try:
            conn = await _engine.connect()
            break
        except (TimeoutError, OSError):
            if attempt == 1:
                raise
            # A brief Neon/DNS interruption should not discard a submitted
            # form. Retry acquisition once; endpoint work has not begun yet.
            await asyncio.sleep(0.25)

    try:
        yield SQLAlchemyConnection(conn)
    except Exception:
        if conn.in_transaction():
            await conn.rollback()
        raise
    else:
        # Existing repositories execute writes without an explicit
        # transaction, matching asyncpg's per-statement commit behaviour.
        if conn.in_transaction():
            await conn.commit()
    finally:
        await conn.close()


@asynccontextmanager
async def get_transaction():
    async with get_connection() as conn:
        async with conn.transaction():
            yield conn


async def db_conn():
    async with get_connection() as conn:
        yield conn
