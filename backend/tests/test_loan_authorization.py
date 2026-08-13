from types import SimpleNamespace
from uuid import uuid4

import pytest

from app.core.loan_authorization import (
    can_edit_intake,
    can_upload_document,
    can_view_loan,
    canonical_role,
    require_intake_edit,
)


def user(*, role, org=None, branch=None, user_id=None):
    return SimpleNamespace(
        id=user_id or uuid4(), org_id=org or uuid4(), branch_id=branch,
        role=role, is_active=True,
    )


def app(*, org, created_by, stage="intake", branch=None, manager=None):
    return SimpleNamespace(
        org_id=org, created_by=created_by, stage=stage, branch_id=branch,
        branch_manager_id=manager,
    )


def test_role_aliases_are_canonicalized_at_one_boundary():
    assert canonical_role("Relationship Officer") == "account_officer"
    assert canonical_role("team_lead") == "branch_manager"


def test_officer_can_edit_only_owned_intake():
    org = uuid4()
    officer = user(role="account_officer", org=org)
    assert can_edit_intake(officer, app(org=org, created_by=officer.id))
    assert not can_edit_intake(officer, app(org=org, created_by=uuid4()))
    assert not can_edit_intake(officer, app(org=org, created_by=officer.id, stage="branch_manager_review"))


def test_assigned_team_lead_can_edit_only_own_branch_review():
    org, branch = uuid4(), uuid4()
    lead = user(role="team_lead", org=org, branch=branch)
    assigned = app(org=org, created_by=uuid4(), stage="branch_manager_review", branch=branch, manager=lead.id)
    assert can_view_loan(lead, assigned)
    assert can_edit_intake(lead, assigned)
    assert can_upload_document(lead, assigned, "bank_statement")
    assert not can_edit_intake(lead, app(org=org, created_by=uuid4(), stage="branch_manager_review", branch=branch, manager=uuid4()))
    assert not can_edit_intake(lead, app(org=org, created_by=uuid4(), stage="branch_manager_review", branch=uuid4(), manager=lead.id))
    assert not can_edit_intake(lead, app(org=uuid4(), created_by=uuid4(), stage="branch_manager_review", branch=branch, manager=lead.id))


def test_inactive_user_and_system_admin_are_denied_dossier_access():
    org = uuid4()
    officer = user(role="account_officer", org=org)
    loan = app(org=org, created_by=officer.id)
    officer.is_active = False
    assert not can_view_loan(officer, loan)
    assert not can_view_loan(user(role="system_admin", org=org), loan)


def test_denial_is_enforced_server_side():
    org = uuid4()
    lead = user(role="branch_manager", org=org, branch=uuid4())
    with pytest.raises(Exception) as exc:
        require_intake_edit(lead, app(org=org, created_by=uuid4(), stage="branch_manager_review", branch=uuid4(), manager=lead.id))
    assert exc.value.status_code == 403
