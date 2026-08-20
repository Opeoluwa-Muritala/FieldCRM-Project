import asyncio
from contextlib import asynccontextmanager
from types import SimpleNamespace
from uuid import uuid4

import pytest
from starlette.requests import Request

from app.core import database
from app.core import dependencies


class FakeConnection:
    def __init__(self):
        self.calls = []

    async def execute(self, query, *args):
        self.calls.append((query, args))


def test_transaction_local_identity_is_bound_and_cleared(monkeypatch):
    monkeypatch.setattr(database, "_is_sqlite", False)
    conn = FakeConnection()
    identity = database.DatabaseIdentity(
        org_id=str(uuid4()), user_id=str(uuid4()), role="branch_manager",
        branch_id=str(uuid4()), request_id="request-123",
    )

    async def exercise():
        with database.database_identity(identity):
            await database._install_database_identity(conn)
            assert database._database_identity.get() == identity
        assert database._database_identity.get() is None

    asyncio.run(exercise())
    assert len(conn.calls) == 5
    assert all(call[0] == "SELECT set_config($1, $2, TRUE)" for call in conn.calls)
    assert {call[1][0] for call in conn.calls} == {
        "app.org_id", "app.user_id", "app.user_role", "app.branch_id", "app.request_id"
    }


def test_missing_identity_installs_nothing(monkeypatch):
    monkeypatch.setattr(database, "_is_sqlite", False)
    conn = FakeConnection()
    asyncio.run(database._install_database_identity(conn))
    assert conn.calls == []


def test_legacy_request_connection_installs_authenticated_rls_identity(monkeypatch):
    user = SimpleNamespace(
        id=uuid4(), org_id=uuid4(), role="account_officer", branch_id=uuid4()
    )
    observed = []

    async def fake_current_user(request, token=""):
        assert token == "signed-token"
        return user

    @asynccontextmanager
    async def fake_connection():
        observed.append(database._database_identity.get())
        yield object()

    monkeypatch.setattr(dependencies, "get_current_user", fake_current_user)
    monkeypatch.setattr(database, "get_connection", fake_connection)
    request = Request({
        "type": "http", "method": "GET", "path": "/applications", "scheme": "https",
        "server": ("fieldcrm.example", 443), "client": ("192.0.2.10", 5000),
        "headers": [(b"authorization", b"Bearer signed-token")], "state": {},
    })

    async def exercise():
        async for _ in database.db_conn(request):
            assert database._database_identity.get() is not None

    asyncio.run(exercise())
    assert observed[0].org_id == str(user.org_id)
    assert observed[0].user_id == str(user.id)
    assert database._database_identity.get() is None


def test_rls_runtime_role_preflight_rejects_owner_or_bypass(monkeypatch):
    monkeypatch.setattr(database, "_is_sqlite", False)
    monkeypatch.setattr(database.settings, "RLS_ENFORCED", True)

    class RoleConnection:
        async def fetchrow(self, _query):
            return {
                "role_name": "neondb_owner", "rolsuper": False,
                "rolbypassrls": True, "owns_loan_table": True,
            }

    with pytest.raises(RuntimeError, match="Unsafe database runtime role"):
        asyncio.run(database.verify_runtime_database_role(RoleConnection()))
