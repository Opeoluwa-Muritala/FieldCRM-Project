import asyncio
from uuid import uuid4

from app.core import database


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
