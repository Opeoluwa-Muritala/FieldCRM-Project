import asyncio
from types import SimpleNamespace
from uuid import uuid4

from app.services.dashboard_service import DashboardService


def test_legacy_team_lead_role_uses_branch_manager_dashboard(monkeypatch):
    observed = []

    async def no_cache(*args):
        return "cache-key", None

    async def ignore_cache(*args, **kwargs):
        return None

    async def branch_data(self, user):
        observed.append(user.id)
        return {"metrics": {"awaiting_concurrence": 2}, "queue": [{"id": "loan"}], "pipeline": []}

    async def wrong_data(self, user):
        raise AssertionError("legacy Team Lead role must not use the officer dashboard")

    monkeypatch.setattr("app.core.cache.get_cached_dashboard_data", no_cache)
    monkeypatch.setattr("app.core.cache.cache_dashboard_data", ignore_cache)
    monkeypatch.setattr(DashboardService, "_branch_manager_data", branch_data)
    monkeypatch.setattr(DashboardService, "_loan_officer_data", wrong_data)
    user = SimpleNamespace(id=uuid4(), org_id=uuid4(), role="team_lead")

    result = asyncio.run(DashboardService(object()).get_dashboard_data(user))

    assert result["metrics"]["awaiting_concurrence"] == 2
    assert observed == [user.id]


def test_team_lead_initial_dashboard_is_not_progressive_only():
    router_source = (
        __import__("pathlib").Path(__file__).resolve().parents[1]
        / "app" / "domains" / "loans" / "router.py"
    ).read_text(encoding="utf-8")
    assert 'progressive_role = role in ("account_officer", "loan_officer")' in router_source


def test_team_lead_tabs_render_non_actionable_branch_data():
    root = __import__("pathlib").Path(__file__).resolve().parents[2]
    pipeline = (root / "frontend" / "templates" / "branch_manager" / "pipeline.html").read_text(encoding="utf-8")
    awaiting = (root / "frontend" / "templates" / "branch_manager" / "awaiting_concurrence.html").read_text(encoding="utf-8")
    signoffs = (root / "frontend" / "templates" / "branch_manager" / "pending_signoffs.html").read_text(encoding="utf-8")

    assert "Branch Applications" in pipeline
    assert "Other Branch Files" in awaiting
    assert "Recent Branch Visits" in signoffs
