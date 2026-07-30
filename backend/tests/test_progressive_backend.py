import asyncio
from datetime import datetime, timezone
from types import SimpleNamespace
from uuid import uuid4

from app.api.v1 import mobile
from app.domains.loans.repository import LoanRepository
from app.domains.loans.router import (
    list_web_application_compliance_flags,
    list_web_application_workflow_events,
)
from app.services.dashboard_service import DashboardService


def run(coro):
    return asyncio.run(coro)


class RecordingConnection:
    def __init__(self, rows=None):
        self.rows = rows or []
        self.calls = []

    async def fetch(self, query, *args):
        self.calls.append((query, args))
        return self.rows


def test_disbursed_page_is_tenant_scoped_and_paginated():
    conn = RecordingConnection(
        [{"id": uuid4(), "total_count": 123, "disbursed_at": datetime.now(timezone.utc)}]
    )
    org_id = uuid4()

    items, total = run(
        LoanRepository(conn).list_disbursed_page(org_id, limit=25, offset=50)
    )

    query, args = conn.calls[0]
    assert "WHERE la.org_id = $1" in query
    assert "LIMIT $2 OFFSET $3" in query
    assert args == (org_id, 25, 50)
    assert total == 123
    assert len(items) == 1


def test_workflow_event_page_uses_deterministic_offset():
    conn = RecordingConnection()
    org_id = uuid4()
    application_id = uuid4()

    run(
        LoanRepository(conn).list_workflow_events_for_application(
            org_id,
            application_id,
            limit=20,
            offset=40,
        )
    )

    query, args = conn.calls[0]
    assert "WHERE org_id = $1" in query
    assert "AND loan_id = $2" in query
    assert "ORDER BY created_at DESC, id DESC" in query
    assert args == (org_id, application_id, 20, 40)


def test_mobile_borrowers_no_longer_perform_n_plus_one(monkeypatch):
    calls = {"list": 0, "get": 0}
    application = SimpleNamespace(
        id=uuid4(),
        org_id=uuid4(),
        created_by=uuid4(),
        applicant_name="Borrower",
        phone="08000000000",
        bvn="00000000000",
        stage="intake",
    )

    class Repository:
        def __init__(self, conn):
            pass

        async def list_by_stage(self, **kwargs):
            calls["list"] += 1
            return [application], 1

        async def get_by_id(self, *args, **kwargs):
            calls["get"] += 1
            raise AssertionError("borrower pagination must not issue per-row lookups")

    monkeypatch.setattr(mobile, "LoanRepository", Repository)
    user = SimpleNamespace(
        id=application.created_by,
        org_id=application.org_id,
        role="account_officer",
    )

    result = run(
        mobile.list_mobile_borrowers(
            page=1,
            size=50,
            conn=object(),
            current_user=user,
        )
    )

    assert result["total"] == 1
    assert len(result["items"]) == 1
    assert calls == {"list": 1, "get": 0}


def test_application_section_endpoints_reject_cross_tenant_application(monkeypatch):
    async def missing(self, application_id, org_id):
        return None

    monkeypatch.setattr(LoanRepository, "get_by_id", missing)
    user = SimpleNamespace(org_id=uuid4())

    for endpoint in (
        list_web_application_workflow_events,
        list_web_application_compliance_flags,
    ):
        try:
            run(
                endpoint(
                    application_id=uuid4(),
                    page=1,
                    size=50,
                    conn=object(),
                    current_user=user,
                )
            )
        except Exception as exc:
            assert getattr(exc, "status_code", None) == 404
        else:
            raise AssertionError("cross-tenant application must not return section data")


def test_compliance_section_pagination_passes_bounded_offset(monkeypatch):
    application_id = uuid4()
    user = SimpleNamespace(org_id=uuid4())
    observed = {}

    async def found(self, requested_id, org_id):
        return SimpleNamespace(id=requested_id)

    async def flags(self, current_user, requested_id, limit, offset):
        observed.update(limit=limit, offset=offset, application_id=requested_id)
        return []

    monkeypatch.setattr(LoanRepository, "get_by_id", found)
    monkeypatch.setattr(DashboardService, "get_application_compliance_flags", flags)

    result = run(
        list_web_application_compliance_flags(
            application_id=application_id,
            page=3,
            size=25,
            conn=object(),
            current_user=user,
        )
    )

    assert observed == {
        "limit": 25,
        "offset": 50,
        "application_id": application_id,
    }
    assert result["has_more"] is False
