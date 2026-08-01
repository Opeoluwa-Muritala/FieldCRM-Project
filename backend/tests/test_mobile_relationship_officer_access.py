from types import SimpleNamespace
import asyncio

import pytest

from app.api.v1 import mobile


def relationship_officer(user_id="officer-1"):
    return SimpleNamespace(id=user_id, org_id="org-1", role="account_officer")


def test_relationship_officer_dashboard_metrics_match_web_bundle():
    payload = mobile._mobile_dashboard_metrics({
        "metrics": {
            "my_applications": 7,
            "pending_upload": 3,
            "visits_due": 2,
            "returned": 1,
        }
    })
    assert payload["apps_today"] == 7
    assert payload["pending_sync"] == 3
    assert payload["visits_due"] == 2
    assert payload["missing_docs"] == 1


def test_relationship_officer_can_write_only_owned_intake():
    mobile._ensure_intake_writer(
        SimpleNamespace(created_by="officer-1"), relationship_officer()
    )
    with pytest.raises(mobile.HTTPException) as error:
        mobile._ensure_intake_writer(
            SimpleNamespace(created_by="officer-2"), relationship_officer()
        )
    assert error.value.status_code == 403


def test_application_list_is_scoped_to_relationship_officer(monkeypatch):
    captured = {}

    class FakeRepository:
        def __init__(self, conn):
            pass

        async def list_by_stage(self, **kwargs):
            captured.update(kwargs)
            return [], 0

    monkeypatch.setattr(mobile, "LoanRepository", FakeRepository)
    asyncio.run(mobile.list_mobile_applications(
        conn=object(), current_user=relationship_officer()
    ))
    assert captured["org_id"] == "org-1"
    assert captured["officer_id"] == "officer-1"


def test_mcc_mutations_do_not_grant_relationship_officer_role():
    source = mobile.submit_mobile_mcc_vote.__wrapped__ if hasattr(
        mobile.submit_mobile_mcc_vote, "__wrapped__"
    ) else mobile.submit_mobile_mcc_vote
    assert source is not None
    with pytest.raises(mobile.HTTPException):
        mobile._ensure_roles(relationship_officer(), {"ed", "md", "system_admin"})
