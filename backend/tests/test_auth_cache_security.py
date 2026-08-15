from datetime import datetime, timezone
from types import SimpleNamespace
from uuid import uuid4

import pytest

from app.core import cache, dependencies
from app.core.security import create_access_token, credential_fingerprint


def auth_profile(*, password_hash: str) -> dict:
    return {
        "id": uuid4(),
        "org_id": uuid4(),
        "full_name": "Cached User",
        "email": "cached@example.com",
        "password_hash": "",
        "credential_fingerprint": credential_fingerprint(password_hash),
        "role": "account_officer",
        "branch_id": None,
        "active": True,
        "last_login_at": None,
        "created_at": datetime.now(timezone.utc),
    }


@pytest.mark.asyncio
async def test_cache_stores_credential_marker_without_password_hash(monkeypatch):
    stored = {}

    async def capture(key, value, ttl_seconds, **kwargs):
        stored.update(value)

    monkeypatch.setattr(cache, "set_json", capture)
    user = SimpleNamespace(**auth_profile(password_hash="stored-password-hash"))
    user.password_hash = "stored-password-hash"

    await cache.cache_auth_user(user)

    assert stored["password_hash"] == ""
    assert stored["credential_fingerprint"] == credential_fingerprint("stored-password-hash")


@pytest.mark.asyncio
async def test_cached_auth_profile_accepts_only_matching_credential(monkeypatch):
    password_hash = "stored-password-hash"
    profile = auth_profile(password_hash=password_hash)

    async def cached(_user_id):
        return profile

    monkeypatch.setattr(dependencies, "get_cached_auth_user", cached)
    token = create_access_token(
        profile["id"],
        role=profile["role"],
        org_id=profile["org_id"],
        password_hash=password_hash,
    )

    user = await dependencies.get_current_user_from_token(token)

    assert user.id == profile["id"]


@pytest.mark.asyncio
async def test_stale_cached_credential_is_rejected(monkeypatch):
    profile = auth_profile(password_hash="new-password-hash")

    async def cached(_user_id):
        return profile

    monkeypatch.setattr(dependencies, "get_cached_auth_user", cached)
    token = create_access_token(
        profile["id"],
        role=profile["role"],
        org_id=profile["org_id"],
        password_hash="old-password-hash",
    )

    with pytest.raises(Exception) as exc_info:
        await dependencies.get_current_user_from_token(token)

    assert getattr(exc_info.value, "status_code", None) == 401

