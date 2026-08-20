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
from contextlib import asynccontextmanager, contextmanager
from contextvars import ContextVar
from dataclasses import dataclass
from time import perf_counter
from urllib.parse import parse_qsl, urlencode, urlsplit, urlunsplit

from sqlalchemy import text
from sqlalchemy.ext.asyncio import AsyncConnection, AsyncEngine, create_async_engine
from starlette.requests import Request

from app.config import settings
from app.core.performance import record_duration, record_query

_engine: AsyncEngine | None = None
_sqlite_pool = None
_is_sqlite = settings.DATABASE_URL.startswith("sqlite")


@dataclass(frozen=True)
class DatabaseIdentity:
    org_id: str
    user_id: str
    role: str
    branch_id: str | None = None
    request_id: str | None = None


_database_identity: ContextVar[DatabaseIdentity | None] = ContextVar(
    "fieldcrm_database_identity", default=None
)


@contextmanager
def database_identity(identity: DatabaseIdentity):
    """Bind trusted identity to this request/task, including child tasks."""
    token = _database_identity.set(identity)
    try:
        yield
    finally:
        _database_identity.reset(token)


async def _install_database_identity(conn) -> None:
    """Install transaction-local PostgreSQL variables used by RLS policies."""
    identity = _database_identity.get()
    if _is_sqlite or identity is None:
        return
    values = {
        "app.org_id": identity.org_id,
        "app.user_id": identity.user_id,
        "app.user_role": identity.role,
        "app.branch_id": identity.branch_id or "",
        "app.request_id": (identity.request_id or "")[:128],
    }
    for setting_name, setting_value in values.items():
        # set_config with is_local=true cannot leak identity through a pooled
        # connection after commit/rollback. Both values are bound parameters.
        await conn.execute("SELECT set_config($1, $2, TRUE)", setting_name, setting_value)


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
        # SQLAlchemy automatically starts a transaction on the first SELECT.
        # Repository services may then need an atomic mutation block on that
        # same request connection, which must use a savepoint rather than
        # attempting a second top-level transaction.
        if self.conn._conn.in_transaction():
            self.transaction = await self.conn._conn.begin_nested()
        else:
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
        started_at = perf_counter()
        try:
            statement, params = _bind(query, args)
            result = await self._conn.execute(text(statement), params)
            return [dict(row) for row in result.mappings().all()]
        finally:
            record_query(started_at)

    async def fetchrow(self, query: str, *args):
        started_at = perf_counter()
        try:
            statement, params = _bind(query, args)
            result = await self._conn.execute(text(statement), params)
            row = result.mappings().first()
            return dict(row) if row else None
        finally:
            record_query(started_at)

    async def fetchval(self, query: str, *args):
        started_at = perf_counter()
        try:
            statement, params = _bind(query, args)
            result = await self._conn.execute(text(statement), params)
            row = result.first()
            return row[0] if row else None
        finally:
            record_query(started_at)

    async def execute(self, query: str, *args):
        started_at = perf_counter()
        try:
            statement, params = _bind(query, args)
            result = await self._conn.execute(text(statement), params)
            command = statement.lstrip().split(None, 1)[0].upper()
            return f"{command} {max(result.rowcount or 0, 0)}"
        finally:
            record_query(started_at)

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
    acquisition_started_at = perf_counter()
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
    record_duration("db_acquire", acquisition_started_at)

    try:
        wrapped = SQLAlchemyConnection(conn)
        await _install_database_identity(wrapped)
        yield wrapped
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


async def db_conn(request: Request):
    """Yield a connection with RLS identity whenever the request is authenticated.

    A small number of public/authentication routes intentionally use this
    compatibility dependency without credentials. Legacy protected routes also
    use it, so a valid bearer/cookie session must install the same identity as
    ``authenticated_db_conn`` before any repository query executes.
    """
    authorization = request.headers.get("authorization", "")
    scheme, _, header_token = authorization.partition(" ")
    token = header_token.strip() if scheme.lower() == "bearer" else ""
    token = token or request.cookies.get("session") or request.cookies.get("__Host-session")
    identity = None
    if token or request.cookies.get("refresh_token"):
        from app.core.dependencies import get_current_user
        from app.core.loan_authorization import canonical_role

        current_user = await get_current_user(request, token=token)
        request_id = request.headers.get("x-request-id") or getattr(request.state, "request_id", None)
        identity = DatabaseIdentity(
            org_id=str(current_user.org_id),
            user_id=str(current_user.id),
            role=canonical_role(current_user.role),
            branch_id=str(current_user.branch_id) if getattr(current_user, "branch_id", None) else None,
            request_id=str(request_id) if request_id else None,
        )

    if identity is None:
        async with get_connection() as conn:
            yield conn
        return
    with database_identity(identity):
        async with get_connection() as conn:
            yield conn


async def verify_runtime_database_role(conn) -> None:
    """Fail closed when an RLS deployment connects as an owner/bypass role."""
    if _is_sqlite or not settings.RLS_ENFORCED:
        return
    role = await conn.fetchrow(
        """
        SELECT current_user AS role_name, r.rolsuper, r.rolbypassrls,
               pg_get_userbyid(c.relowner) = current_user AS owns_loan_table
        FROM pg_roles r
        JOIN pg_class c ON c.oid = 'public.loan_applications'::regclass
        WHERE r.rolname = current_user
        """
    )
    if (
        not role
        or role["role_name"] != settings.DATABASE_EXPECTED_RUNTIME_USER
        or role["rolsuper"]
        or role["rolbypassrls"]
        or role["owns_loan_table"]
    ):
        raise RuntimeError(
            "Unsafe database runtime role: use the non-owner, NOSUPERUSER, NOBYPASSRLS fieldcrm_app role"
        )
