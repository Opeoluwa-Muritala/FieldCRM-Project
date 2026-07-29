import asyncio
from pathlib import Path
from types import SimpleNamespace
from uuid import uuid4

import httpx
from starlette.requests import Request

from app.core import database, dependencies
from app.core.templates import create_templates
from app.domains.auth.service import AuthService
from app.domains.loans.repository import LoanRepository
from app.domains.loans.router import render_dashboard
from app.main import app
from app.services.dashboard_service import DashboardService


def run(coro):
    return asyncio.run(coro)


async def get(path: str, **kwargs):
    async with httpx.AsyncClient(
        transport=httpx.ASGITransport(app=app),
        base_url="http://testserver",
        follow_redirects=False,
    ) as client:
        return await client.get(path, **kwargs)


def test_logged_out_login_does_not_acquire_database_connection(monkeypatch):
    def fail_init_engine():
        raise AssertionError("logged-out login must not initialize the database")

    monkeypatch.setattr(database, "init_engine", fail_init_engine)
    response = run(get("/login"))

    assert response.status_code == 200
    assert 'id="loginForm"' in response.text
    assert response.headers["x-query-count"] == "0"


def test_authenticated_login_visitor_is_still_redirected(monkeypatch):
    calls = []

    async def authenticated(token, conn=None):
        calls.append((token, conn))
        return SimpleNamespace(id=uuid4(), active=True)

    monkeypatch.setattr(dependencies, "get_current_user_from_token", authenticated)
    response = run(get("/login", cookies={"session": "valid-session"}))

    assert response.status_code == 303
    assert response.headers["location"] == "/dashboard"
    assert calls == [("valid-session", None)]


class RecordingConnection:
    def __init__(self):
        self.calls = []

    async def fetch(self, query, *args):
        self.calls.append((query, args))
        return []


def test_application_workflow_history_is_scoped_by_org_and_application():
    conn = RecordingConnection()
    org_id = uuid4()
    application_id = uuid4()

    run(
        LoanRepository(conn).list_workflow_events_for_application(
            org_id,
            application_id,
            limit=75,
        )
    )

    query, args = conn.calls[0]
    assert "WHERE org_id = $1" in query
    assert "AND loan_id = $2" in query
    assert "ORDER BY created_at DESC, id DESC" in query
    assert args == (org_id, application_id, 75)


def test_application_compliance_union_scopes_every_branch():
    conn = RecordingConnection()
    user = SimpleNamespace(org_id=uuid4())
    application_id = uuid4()

    run(
        DashboardService(conn).get_application_compliance_flags(
            user,
            application_id,
            limit=80,
        )
    )

    query, args = conn.calls[0]
    assert query.count("org_id = $1") >= 3
    assert query.count("loan_id = $2") == 3
    assert "ORDER BY created_at DESC, flag_type, flag_label" in query
    assert args == (user.org_id, application_id, 80, 0)


def make_request(path="/dashboard"):
    return Request(
        {
            "type": "http",
            "method": "GET",
            "path": path,
            "headers": [],
            "query_string": b"",
            "scheme": "http",
            "server": ("testserver", 80),
            "client": ("127.0.0.1", 1234),
        }
    )


def test_dedicated_dashboard_roles_redirect_before_loading_data(monkeypatch):
    async def fail_dashboard_data(self, user):
        raise AssertionError("dedicated role must redirect before dashboard queries")

    monkeypatch.setattr(DashboardService, "get_dashboard_data", fail_dashboard_data)
    expected = {
        "crm": "/crm-dashboard",
        "head_crm": "/crm-dashboard",
        "ed": "/ed-dashboard",
        "md": "/md-dashboard",
        "legal": "/legal-queue",
    }

    for role, location in expected.items():
        user = SimpleNamespace(role=role)
        response = run(render_dashboard(make_request(), user))
        assert response.status_code == 303
        assert response.headers["location"] == location


def test_login_animation_uses_short_non_load_blocking_delay():
    response = run(get("/login"))

    assert response.status_code == 200
    assert "<body>" in response.text
    assert "window.setTimeout(revealLogin, 600)" in response.text
    assert "window.addEventListener('load'" not in response.text


def test_scoped_query_files_do_not_use_tenant_wide_post_filtering():
    root = Path(__file__).resolve().parents[1] / "app" / "domains"
    workflow = (
        root / "loans" / "queries" / "list_workflow_events_for_application.sql"
    ).read_text(encoding="utf-8")
    compliance = (
        root / "audit" / "queries" / "list_application_compliance_flags.sql"
    ).read_text(encoding="utf-8")

    assert "WHERE org_id = $1" in workflow
    assert "AND loan_id = $2" in workflow
    assert compliance.count("loan_id = $2") == 3


def test_dashboard_bundle_uses_one_query_and_preserves_sections():
    class BundleConnection:
        calls = 0

        async def fetchrow(self, query, *args):
            self.calls += 1
            return {
                "metrics": {"my_applications": 2, "pending_upload": 1},
                "tasks": [],
                "queue": [],
                "visits_due": [],
            }

    conn = BundleConnection()
    user = SimpleNamespace(org_id=uuid4(), id=uuid4(), role="account_officer")
    result = run(DashboardService(conn).get_account_officer_bundle(user))

    assert conn.calls == 1
    assert result["metrics"]["my_applications"] == 2


def test_password_reset_invalidates_auth_cache(monkeypatch):
    calls = []

    class Repository:
        async def get_valid_reset_token(self, token):
            return {"user_id": uuid4()}

        async def update_password(self, user_id, hashed_password):
            calls.append(("password", str(user_id)))

        async def mark_token_used(self, token):
            calls.append(("token", token))

    async def invalidate(user_id):
        calls.append(("invalidate", str(user_id)))

    monkeypatch.setattr("app.core.cache.invalidate_auth_user", invalidate)
    assert run(AuthService(Repository()).reset_password("reset-token", "new-password"))
    assert [name for name, _ in calls] == ["password", "token", "invalidate"]


def test_template_factory_reuses_one_environment():
    template_dir = str(Path(__file__).resolve().parents[2] / "frontend" / "templates")
    first = create_templates(template_dir)
    second = create_templates(template_dir)

    assert first is second


def test_document_preview_script_is_conditionally_loaded():
    shell = (
        Path(__file__).resolve().parents[2]
        / "frontend"
        / "templates"
        / "base"
        / "shell.html"
    ).read_text(encoding="utf-8")

    assert '<script src="/static/js/document-preview.js' not in shell
    assert "document.querySelector('[data-document-preview]" in shell
    assert '<script defer src="/static/js/dashboard.js' in shell
