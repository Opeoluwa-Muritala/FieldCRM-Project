import asyncio
from pathlib import Path

import pytest
from fastapi import HTTPException
from fastapi.testclient import TestClient

from app.api.v1.mobile import router as mobile_router
from app.domains.loans.mcc_policy import require_mcc_quorum
from app.main import app


class QuorumConnection:
    def __init__(self, vote_count: int, executive_vote_count: int):
        self.row = {
            "vote_count": vote_count,
            "executive_vote_count": executive_vote_count,
        }

    async def fetchrow(self, *_args):
        return self.row


def test_customer_uploads_are_not_public_static_assets():
    response = TestClient(app).get("/static/uploads/demo/generated/example.pdf")
    assert response.status_code == 404


def test_mobile_legal_queue_has_one_exact_route():
    matches = [
        route for route in mobile_router.routes
        if getattr(route, "path", None) == "/queues/legal" and "GET" in getattr(route, "methods", set())
    ]
    assert len(matches) == 1


def test_mcc_finalization_requires_distinct_and_executive_votes():
    async def scenario():
        for connection in (QuorumConnection(1, 1), QuorumConnection(2, 0)):
            with pytest.raises(HTTPException) as exc:
                await require_mcc_quorum(connection, "loan", "org")
            assert exc.value.status_code == 409

        result = await require_mcc_quorum(QuorumConnection(2, 1), "loan", "org")
        assert result == {"vote_count": 2, "executive_vote_count": 1}

    asyncio.run(scenario())


def test_mobile_creation_sets_idempotency_key_in_initial_insert():
    query = Path("backend/app/domains/loans/queries/create.sql").read_text(encoding="utf-8")
    assert "client_request_id" in query
    assert "$8" in query


def test_runtime_dependencies_are_exactly_pinned():
    for filename in ("requirements.txt", "backend/requirements.txt"):
        requirements = Path(filename).read_text(encoding="utf-8")
        assert ">=" not in requirements
    lock = Path("requirements.lock").read_text(encoding="utf-8")
    assert "starlette==" in lock
    assert "cryptography==" in lock


def test_mobile_notifications_are_not_silently_swallowed():
    source = Path("backend/app/api/v1/mobile.py").read_text(encoding="utf-8")
    assert "except Exception:\n        pass" not in source
