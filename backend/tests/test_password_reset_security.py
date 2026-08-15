from datetime import datetime, timedelta, timezone
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
