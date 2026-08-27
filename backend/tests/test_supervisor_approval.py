import asyncio
from types import SimpleNamespace
from uuid import uuid4

from fastapi import HTTPException

from app.domains.loans import router
from app.services.dashboard_service import DashboardService


class Request:
    async def form(self):
        return {"amount": "250000", "kyc_attested": "on", "collateral_attested": "on"}


class Connection:
    def __init__(self):
        self.executions = []

    async def execute(self, query, *args):
        self.executions.append((query, args))


class QueueConnection:
    def __init__(self):
        self.calls = []

    async def fetch(self, query, *args):
        self.calls.append((query, args))
        return [{
            "id": uuid4(),
            "ref_no": "SUPERVISOR-CHECK",
            "stage": "branch_supervisor_review",
            "applicant_name": "Queue Verification",
            "loan_type": "msef",
            "amount": 250000,
        }]


def test_branchless_supervisor_dashboard_and_queue_use_institution_scope():
    conn = QueueConnection()
    user = SimpleNamespace(
        id=uuid4(), org_id=uuid4(), branch_id=None, role="branch_supervisor"
    )
    service = DashboardService(conn)

    dashboard = asyncio.run(service._branch_supervisor_data(user))
    queue = asyncio.run(service.get_supervisory_review_queue(user))

    assert dashboard["metrics"]["supervisory_reviews"] == 1
    assert dashboard["queue"][0]["status"] == "Supervisor Review"
    assert queue[0]["ref_no"] == "SUPERVISOR-CHECK"
    assert len(conn.calls) == 2
    for _query, args in conn.calls:
        assert args[0] == user.org_id
        assert args[1] == "branch_supervisor_review"
        assert args[9] is None


def test_branched_supervisor_dashboard_keeps_branch_filter():
    conn = QueueConnection()
    branch_id = uuid4()
    user = SimpleNamespace(
        id=uuid4(), org_id=uuid4(), branch_id=branch_id, role="branch_supervisor"
    )

    asyncio.run(DashboardService(conn)._branch_supervisor_data(user))

    assert conn.calls[0][1][9] == branch_id


def test_supervisor_advances_own_branch_file_to_credit_analyst(monkeypatch):
    # This fixture verifies the retained legacy workflow path. Configurable
    # permission enforcement has its own database-backed Phase 5 tests.
    monkeypatch.setattr(router.settings, "CONFIGURABLE_WORKFLOW_ENABLED", False)
    application_id = uuid4()
    org_id = uuid4()
    branch_id = uuid4()
    calls = {}

    class Repository:
        def __init__(self, conn):
            pass

        async def get_by_id(self, requested_id, requested_org):
            assert (requested_id, requested_org) == (application_id, org_id)
            return SimpleNamespace(stage="branch_supervisor_review", branch_id=branch_id)

        async def approve(self, requested_id, requested_org, user_id, **kwargs):
            calls.update(kwargs)
            return SimpleNamespace(stage="credit_analyst_review")

    class Audit:
        def __init__(self, conn):
            pass

        async def log(self, **kwargs):
            calls["audit"] = kwargs

    monkeypatch.setattr(router, "LoanRepository", Repository)
    monkeypatch.setattr(router, "AuditService", Audit)
    user = SimpleNamespace(id=uuid4(), org_id=org_id, branch_id=branch_id, role="branch_supervisor")
    conn = Connection()

    response = asyncio.run(
        router.process_approval_readiness(
            request=Request(),
            application_id=str(application_id),
            conn=conn,
            current_user=user,
        )
    )

    assert response.status_code == 303
    assert calls["expected_stage"] == "branch_supervisor_review"
    assert calls["next_stage"] == "credit_analyst_review"
    assert calls["audit"]["action"] == "Supervisor Concurrence — Forwarded to Credit Analyst"
    assert "AND org_id = $3" in conn.executions[0][0]


def test_supervisor_cannot_approve_another_branch_file(monkeypatch):
    class Repository:
        def __init__(self, conn):
            pass

        async def get_by_id(self, application_id, org_id):
            return SimpleNamespace(stage="branch_supervisor_review", branch_id=uuid4())

    monkeypatch.setattr(router, "LoanRepository", Repository)
    user = SimpleNamespace(id=uuid4(), org_id=uuid4(), branch_id=uuid4(), role="branch_supervisor")

    try:
        asyncio.run(
            router.process_approval_readiness(
                request=Request(),
                application_id=str(uuid4()),
                conn=Connection(),
                current_user=user,
            )
        )
    except HTTPException as exc:
        assert exc.status_code == 403
    else:
        raise AssertionError("cross-branch supervisor approval must be rejected")


def test_supervisor_queue_links_to_approval_screen():
    root = __import__("pathlib").Path(__file__).resolve().parents[2]
    template = (
        root / "frontend" / "templates" / "branch_supervisor" / "review_queue.html"
    ).read_text(encoding="utf-8")
    assert 'href="/applications/{{ item.id }}/approve"' in template
