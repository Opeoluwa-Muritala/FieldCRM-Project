from types import SimpleNamespace
from uuid import uuid4

import pytest

from app.domains.users.service import UserService
from app.core.exceptions import DomainException


class Repository:
    def __init__(self, user):
        self.user = user
        self.conn = self

    async def fetchval(self, query, *args):
        return True

    async def get_by_id(self, user_id):
        return self.user if user_id == self.user.id else None

    async def update_role(self, user_id, role):
        self.user.role = role

    async def update_branch(self, user_id, branch_id):
        self.user.branch_id = branch_id

    async def deactivate_user(self, user_id):
        self.user.active = False

    async def delete_user(self, user_id):
        self.user.active = False


def identities():
    org_id = uuid4()
    admin = SimpleNamespace(id=uuid4(), org_id=org_id, role="system_admin")
    user = SimpleNamespace(id=uuid4(), org_id=org_id, role="account_officer", branch_id=None, active=True)
    return admin, user


@pytest.mark.asyncio
async def test_branch_manager_cannot_create_a_privileged_user():
    org_id = uuid4()
    manager = SimpleNamespace(id=uuid4(), org_id=org_id, role="branch_manager")
    requested_user = SimpleNamespace(
        org_id=str(org_id),
        full_name="Escalated User",
        email="escalated@example.com",
        role="system_admin",
        password="correct horse battery staple",
        branch_id=None,
    )

    with pytest.raises(DomainException) as exc_info:
        await UserService(Repository(None)).register_user(manager, requested_user)

    assert exc_info.value.status_code == 403


@pytest.mark.asyncio
async def test_role_change_canonicalizes_legacy_admin_and_invalidates_cache(monkeypatch):
    admin, user = identities()
    invalidated = []
    async def invalidate(user_id):
        invalidated.append(user_id)
    monkeypatch.setattr("app.domains.users.service.invalidate_auth_user", invalidate)

    updated = await UserService(Repository(user)).update_user_role(admin, user.id, "admin")

    assert updated.role == "system_admin"
    assert invalidated == [user.id]


@pytest.mark.asyncio
@pytest.mark.parametrize("operation", ["branch", "deactivate", "delete"])
async def test_identity_changes_invalidate_affected_auth_profile(monkeypatch, operation):
    admin, user = identities()
    invalidated = []
    async def invalidate(user_id):
        invalidated.append(user_id)
    monkeypatch.setattr("app.domains.users.service.invalidate_auth_user", invalidate)
    service = UserService(Repository(user))

    if operation == "branch":
        await service.update_user_branch(admin, user.id, uuid4())
    elif operation == "deactivate":
        await service.deactivate_managed_user(admin, user.id)
    else:
        await service.delete_managed_user(admin, user.id)

    assert invalidated == [user.id]
