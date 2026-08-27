from datetime import datetime, timedelta, timezone
from types import SimpleNamespace
from uuid import uuid4

import pytest

from app.domains.auth.repository import AuthRepository
from app.core.security import (
    create_access_token,
    decode_access_token,
    get_password_hash,
    validate_password_strength,
    verify_password,
)


class RecordingConnection:
    def __init__(self):
        self.calls = []

    async def execute(self, query, *args):
        self.calls.append((query, args))

    async def fetchrow(self, query, *args):
        self.calls.append((query, args))
        return None


def test_password_hashing_round_trip():
    encoded = get_password_hash("correct horse battery staple")
    assert encoded.startswith("pbkdf2_sha256$")
    assert verify_password("correct horse battery staple", encoded)
    assert not verify_password("wrong password", encoded)


@pytest.mark.parametrize("password", ["", "short", "password1234", "x" * 129])
def test_password_policy_rejects_weak_or_oversized_credentials(password):
    with pytest.raises(ValueError):
        validate_password_strength(password)
    with pytest.raises(ValueError):
        get_password_hash(password)


def test_access_tokens_are_bound_to_credentials():
    token = create_access_token(
        uuid4(), role="system_admin", org_id=uuid4(), password_hash="stored-password-hash"
    )
    payload = decode_access_token(token)
    assert payload["type"] == "access"
    assert payload["credential"]
    with pytest.raises(ValueError, match="credential hash"):
        create_access_token(uuid4(), role="system_admin", org_id=uuid4())


@pytest.mark.asyncio
async def test_new_reset_tokens_are_stored_as_hashes():
    conn = RecordingConnection()
    repo = AuthRepository(conn)
    raw_token = "secret-reset-token"

    await repo.create_reset_token(
        str(uuid4()), raw_token, datetime.now(timezone.utc) + timedelta(hours=1)
    )

    stored_token = conn.calls[0][1][1]
    assert stored_token != raw_token
    assert len(stored_token) == 64


@pytest.mark.asyncio
async def test_reset_consumption_queries_hash_and_legacy_token_once():
    conn = RecordingConnection()
    repo = AuthRepository(conn)
    raw_token = "secret-reset-token"

    await repo.consume_valid_reset_token(raw_token)

    query, args = conn.calls[0]
    assert "UPDATE password_reset_tokens" in query
    assert "used_at = NOW()" in query
    assert args[0] == [repo._reset_token_hash(raw_token), raw_token]


@pytest.mark.asyncio
async def test_password_reset_request_delivers_the_single_use_link(monkeypatch):
    from app.config import settings
    from app.domains.auth.service import AuthService
    from app.services.email_service import EmailService

    user = SimpleNamespace(
        id=uuid4(),
        email="officer@example.com",
        full_name="Example Officer",
        active=True,
    )
    captured = {}

    class ResetRepository:
        async def get_user_by_email(self, email):
            captured["lookup"] = email
            return user

        async def create_reset_token(self, user_id, token, expires_at):
            captured.update(user_id=user_id, token=token, expires_at=expires_at)

    def deliver(_service, **message):
        captured["message"] = message
        return True

    monkeypatch.setattr(settings, "APP_BASE_URL", "https://fieldcrm.example")
    monkeypatch.setattr(EmailService, "send_password_reset", deliver)

    await AuthService(ResetRepository()).request_password_reset(user.email)

    assert captured["lookup"] == user.email
    assert captured["user_id"] == str(user.id)
    assert captured["expires_at"] > datetime.now(timezone.utc)
    assert captured["message"]["recipient"] == user.email
    assert captured["message"]["full_name"] == user.full_name
    assert captured["message"]["reset_url"] == (
        f"https://fieldcrm.example/reset-password?token={captured['token']}"
    )


@pytest.mark.asyncio
async def test_inactive_account_cannot_request_self_service_reactivation():
    from app.domains.auth.service import AuthService

    class InactiveRepository:
        async def get_user_by_email(self, _email):
            return SimpleNamespace(active=False)

        async def create_reset_token(self, *_args):
            pytest.fail("Inactive accounts must not receive reset tokens")

    assert not await AuthService(InactiveRepository()).request_password_reset("inactive@example.com")


@pytest.mark.asyncio
async def test_resending_invitation_invalidates_prior_unused_tokens():
    conn = RecordingConnection()
    await AuthRepository(conn).invalidate_reset_tokens(str(uuid4()))

    query, _args = conn.calls[0]
    assert "UPDATE password_reset_tokens" in query
    assert "used_at = COALESCE(used_at, NOW())" in query
    assert "used_at IS NULL" in query
