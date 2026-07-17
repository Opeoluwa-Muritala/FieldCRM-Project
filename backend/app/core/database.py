import re
import json
import sqlite3
from contextlib import asynccontextmanager
from app.config import settings

_pool = None
_is_sqlite = settings.DATABASE_URL.startswith("sqlite")

class SQLiteTransactionContext:
    def __init__(self, conn):
        self.conn = conn
        self._in_transaction = False

    async def __aenter__(self):
        await self.conn.execute("BEGIN TRANSACTION;")
        self._in_transaction = True
        return self

    async def __aexit__(self, exc_type, exc_val, exc_tb):
        if not self._in_transaction:
            return
        if exc_type is not None:
            await self.conn.execute("ROLLBACK;")
        else:
            await self.conn.execute("COMMIT;")
        self._in_transaction = False

class SQLiteConnectionWrapper:
    def __init__(self, conn):
        self._conn = conn

    def _convert_query_and_args(self, query: str, args):
        ordered_args = []

        def replace_param(match):
            index = int(match.group(0)[1:]) - 1
            ordered_args.append(args[index])
            return "?"

        q = re.sub(r'\$\d+', replace_param, query)
        q = re.sub(r'\?::[A-Za-z_][A-Za-z0-9_]*', '?', q)
        q = q.replace("NOW()", "CURRENT_TIMESTAMP")
        q = q.replace("gen_random_uuid()", "lower(hex(randomblob(4))) || '-' || lower(hex(randomblob(2))) || '-4' || substr(lower(hex(randomblob(2))),2) || '-' || substr('89ab',abs(random()) % 4 + 1, 1) || substr(lower(hex(randomblob(2))),2) || '-' || lower(hex(randomblob(6)))")
        return q, self._convert_args(ordered_args)

    def _convert_args(self, args):
        new_args = []
        for arg in args:
            if isinstance(arg, (dict, list)):
                new_args.append(json.dumps(arg))
            else:
                new_args.append(arg)
        return new_args

    def _parse_row(self, row):
        if not row:
            return None
        d = dict(row)
        for k, v in d.items():
            if (k == 'data_json' or k.endswith('_json')) and isinstance(v, str):
                try:
                    d[k] = json.loads(v)
                except ValueError:
                    pass
        return d

    async def fetch(self, query: str, *args):
        q, converted_args = self._convert_query_and_args(query, args)
        async with self._conn.execute(q, converted_args) as cursor:
            rows = await cursor.fetchall()
            return [self._parse_row(row) for row in rows]

    async def fetchrow(self, query: str, *args):
        q, converted_args = self._convert_query_and_args(query, args)
        async with self._conn.execute(q, converted_args) as cursor:
            row = await cursor.fetchone()
            return self._parse_row(row)

    async def fetchval(self, query: str, *args):
        q, converted_args = self._convert_query_and_args(query, args)
        async with self._conn.execute(q, converted_args) as cursor:
            row = await cursor.fetchone()
            return row[0] if row else None

    async def execute(self, query: str, *args):
        q, converted_args = self._convert_query_and_args(query, args)
        await self._conn.execute(q, converted_args)
        return "UPDATE 1"

    def transaction(self):
        return SQLiteTransactionContext(self._conn)

class SQLitePool:
    def __init__(self, dsn: str):
        path = dsn.replace("sqlite:///", "").replace("sqlite://", "")
        self.dsn = path

    async def init(self):
        pass

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

    async def close(self):
        pass

async def init_pool() -> None:
    global _pool
    if _is_sqlite:
        _pool = SQLitePool(settings.DATABASE_URL)
        await _pool.init()
    else:
        import asyncpg
        _pool = await asyncpg.create_pool(
            dsn=settings.DATABASE_URL,
            min_size=5,
            max_size=20,
            max_inactive_connection_lifetime=300,
            command_timeout=30,
            timeout=60.0,
            statement_cache_size=100,
        )

async def close_pool() -> None:
    if _pool:
        await _pool.close()

@asynccontextmanager
async def get_connection():
    if _is_sqlite:
        async with _pool.acquire() as conn:
            yield conn
    else:
        conn = await _pool.acquire()
        try:
            yield conn
        finally:
            import asyncio
            await asyncio.shield(_pool.release(conn))

@asynccontextmanager
async def get_transaction():
    if _is_sqlite:
        async with _pool.acquire() as conn:
            async with conn.transaction():
                yield conn
    else:
        conn = await _pool.acquire()
        try:
            async with conn.transaction():
                yield conn
        finally:
            import asyncio
            await asyncio.shield(_pool.release(conn))

async def db_conn():
    if _is_sqlite:
        async with _pool.acquire() as conn:
            yield conn
    else:
        conn = await _pool.acquire()
        try:
            yield conn
        finally:
            import asyncio
            await asyncio.shield(_pool.release(conn))

