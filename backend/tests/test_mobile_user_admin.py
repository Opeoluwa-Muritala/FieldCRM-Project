from types import SimpleNamespace
from uuid import UUID
from inspect import signature

import pytest
from starlette.requests import Request

from app.api.v1 import mobile
from app.core.dependencies import RoleChecker
from app.domains.users.schemas import UserInvitationCreate


@pytest.mark.asyncio
async def test_mobile_invitation_uses_real_invitation_flow_and_branch(monkeypatch):
    branch_id = UUID("11111111-1111-1111-1111-111111111111")
    captured = {}

    class FakeUserService:
        def __init__(self, repo):
            captured["repo"] = repo

        async def invite_user(self, current_admin, invitation):
            captured["admin"] = current_admin
            captured["invitation"] = invitation
            return (
                SimpleNamespace(
                    id=UUID("22222222-2222-2222-2222-222222222222"),
                    email=invitation.email,
                    full_name=invitation.full_name,
                    role=invitation.role,
                ),
                "invitation-token",
            )

    class FakeEmailService:
        def send_invitation(self, **kwargs):
            captured["email"] = kwargs
            return True

    monkeypatch.setattr(mobile, "UserService", FakeUserService)
    monkeypatch.setattr(mobile, "EmailService", FakeEmailService)

    request = Request({"type": "http", "scheme": "https", "server": ("example.test", 443), "path": "/"})
    current_user = SimpleNamespace(role="system_admin", org_id=UUID("33333333-3333-3333-3333-333333333333"))
    invitation = UserInvitationCreate(
        full_name="Ada Okafor",
        email="ada@example.com",
        role="account_officer",
        branch_id=branch_id,
    )

    response = await mobile.invite_mobile_user(request, invitation, current_user, object())

    assert response["email_sent"] is True
    assert response["message"] == "Invitation email sent."
    assert captured["invitation"].branch_id == branch_id
    assert captured["email"]["invitation_url"].endswith("/accept-invitation?token=invitation-token")


def test_mobile_admin_routes_authenticate_before_opening_database():
    for endpoint in (mobile.list_mobile_users, mobile.invite_mobile_user, mobile.create_mobile_user):
        parameter_names = list(signature(endpoint).parameters)
        assert parameter_names.index("current_user") < parameter_names.index("conn")


def test_legacy_admin_role_is_authorized_as_system_admin():
    legacy_admin = SimpleNamespace(role="admin")

    mobile._ensure_roles(legacy_admin, {"system_admin"})
    assert RoleChecker(["System Admin"])(legacy_admin) is legacy_admin
