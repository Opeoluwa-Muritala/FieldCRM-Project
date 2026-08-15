import pytest
from datetime import datetime, timezone, timedelta
from uuid import uuid4, UUID
from unittest.mock import AsyncMock, MagicMock
from app.domains.auth.service import AuthService
from app.core.exceptions import DomainException

@pytest.mark.asyncio
async def test_normal_rotation():
    repo = MagicMock()
    service = AuthService(repo)
    
    user_id = uuid4()
    org_id = uuid4()
    session_id = uuid4()
    family_id = uuid4()
    expires_at = datetime.now(timezone.utc) + timedelta(days=2)
    
    mock_session = {
        "id": session_id,
        "user_id": user_id,
        "org_id": org_id,
        "family_id": family_id,
        "expires_at": expires_at,
        "used_at": None,
        "revoked_at": None,
        "replaced_by": None
    }
    
    mock_user = {
        "id": user_id,
        "org_id": org_id,
        "role": "account_officer",
        "active": True,
        "password_hash": "test-password-hash",
    }
    
    # Configure mock repository responses
    repo.get_refresh_token_by_hash_for_update = AsyncMock(return_value=mock_session)
    repo.get_user_by_id = AsyncMock(return_value=mock_user)
    repo.create_refresh_token = AsyncMock(return_value={"id": uuid4()})
    repo.mark_refresh_token_used = AsyncMock()
    repo.cleanup_expired_tokens = AsyncMock(return_value=0)
    
    # Mock transaction manager
    transaction = MagicMock()
    transaction.__aenter__ = AsyncMock()
    transaction.__aexit__ = AsyncMock(return_value=False)
    repo.conn.transaction = MagicMock(return_value=transaction)
    
    raw_token = "mock_raw_refresh_token"
    res = await service.rotate_refresh_token(raw_token, client_type="web")
    
    assert "access_token" in res
    assert "refresh_token" in res
    assert res["expires_at"] == expires_at
    
    # Verify DB interactions
    repo.get_refresh_token_by_hash_for_update.assert_called_once()
    repo.get_user_by_id.assert_called_once_with(str(user_id))
    repo.create_refresh_token.assert_called_once()
    repo.mark_refresh_token_used.assert_called_once()


@pytest.mark.asyncio
async def test_reuse_detection_and_family_revocation():
    repo = MagicMock()
    service = AuthService(repo)
    
    user_id = uuid4()
    org_id = uuid4()
    session_id = uuid4()
    family_id = uuid4()
    expires_at = datetime.now(timezone.utc) + timedelta(days=2)
    
    # Case: used_at is already set (token has been consumed)
    mock_session = {
        "id": session_id,
        "user_id": user_id,
        "org_id": org_id,
        "family_id": family_id,
        "expires_at": expires_at,
        "used_at": datetime.now(timezone.utc) - timedelta(minutes=1),
        "revoked_at": None,
        "replaced_by": uuid4()
    }
    
    repo.get_refresh_token_by_hash_for_update = AsyncMock(return_value=mock_session)
    repo.revoke_refresh_token_family = AsyncMock()
    repo.cleanup_expired_tokens = AsyncMock(return_value=0)
    
    # Mock transaction manager
    transaction = MagicMock()
    transaction.__aenter__ = AsyncMock()
    transaction.__aexit__ = AsyncMock(return_value=False)
    repo.conn.transaction = MagicMock(return_value=transaction)
    
    raw_token = "replayed_token"
    
    with pytest.raises(DomainException) as exc_info:
        await service.rotate_refresh_token(raw_token, client_type="web")
        
    assert exc_info.value.status_code == 401
    assert "reuse detected" in exc_info.value.message.lower()
    
    # Verify that the entire family was immediately revoked
    repo.revoke_refresh_token_family.assert_called_once_with(family_id)


@pytest.mark.asyncio
async def test_expired_token_rejection():
    repo = MagicMock()
    service = AuthService(repo)
    
    user_id = uuid4()
    org_id = uuid4()
    session_id = uuid4()
    family_id = uuid4()
    # Expired 1 hour ago
    expires_at = datetime.now(timezone.utc) - timedelta(hours=1)
    
    mock_session = {
        "id": session_id,
        "user_id": user_id,
        "org_id": org_id,
        "family_id": family_id,
        "expires_at": expires_at,
        "used_at": None,
        "revoked_at": None,
        "replaced_by": None
    }
    
    repo.get_refresh_token_by_hash_for_update = AsyncMock(return_value=mock_session)
    repo.revoke_refresh_token_family = AsyncMock()
    repo.cleanup_expired_tokens = AsyncMock(return_value=0)
    
    # Mock transaction manager
    transaction = MagicMock()
    transaction.__aenter__ = AsyncMock()
    transaction.__aexit__ = AsyncMock(return_value=False)
    repo.conn.transaction = MagicMock(return_value=transaction)
    
    raw_token = "expired_token"
    
    with pytest.raises(DomainException) as exc_info:
        await service.rotate_refresh_token(raw_token, client_type="web")
        
    assert exc_info.value.status_code == 401
    assert "expired" in exc_info.value.message.lower()
    repo.revoke_refresh_token_family.assert_called_once_with(family_id)


@pytest.mark.asyncio
async def test_inactive_user_token_rejection():
    repo = MagicMock()
    service = AuthService(repo)
    
    user_id = uuid4()
    org_id = uuid4()
    session_id = uuid4()
    family_id = uuid4()
    expires_at = datetime.now(timezone.utc) + timedelta(days=2)
    
    mock_session = {
        "id": session_id,
        "user_id": user_id,
        "org_id": org_id,
        "family_id": family_id,
        "expires_at": expires_at,
        "used_at": None,
        "revoked_at": None,
        "replaced_by": None
    }
    
    mock_user = {
        "id": user_id,
        "org_id": org_id,
        "role": "account_officer",
        "active": False # User is inactive
    }
    
    repo.get_refresh_token_by_hash_for_update = AsyncMock(return_value=mock_session)
    repo.get_user_by_id = AsyncMock(return_value=mock_user)
    repo.revoke_refresh_token_family = AsyncMock()
    repo.cleanup_expired_tokens = AsyncMock(return_value=0)
    
    # Mock transaction manager
    transaction = MagicMock()
    transaction.__aenter__ = AsyncMock()
    transaction.__aexit__ = AsyncMock(return_value=False)
    repo.conn.transaction = MagicMock(return_value=transaction)
    
    raw_token = "inactive_user_token"
    
    with pytest.raises(DomainException) as exc_info:
        await service.rotate_refresh_token(raw_token, client_type="web")
        
    assert exc_info.value.status_code == 401
    assert "no longer active" in exc_info.value.message.lower()
    repo.revoke_refresh_token_family.assert_called_once_with(family_id)


@pytest.mark.asyncio
async def test_concurrent_refresh_reuses_access_renewal_without_family_revocation():
    repo = MagicMock()
    service = AuthService(repo)
    user_id = uuid4()
    expires_at = datetime.now(timezone.utc) + timedelta(days=2)
    session = {
        "user_id": user_id,
        "expires_at": expires_at,
        "used_at": datetime.now(timezone.utc) - timedelta(seconds=1),
        "revoked_at": None,
        "user_agent": "browser",
        "ip_address": "127.0.0.1",
    }
    repo.get_refresh_token_by_hash_for_update = AsyncMock(return_value=session)
    repo.get_user_by_id = AsyncMock(return_value={
        "id": user_id,
        "org_id": uuid4(),
        "role": "account_officer",
        "active": True,
        "password_hash": "test-password-hash",
    })
    transaction = MagicMock()
    transaction.__aenter__ = AsyncMock()
    transaction.__aexit__ = AsyncMock(return_value=False)
    repo.conn.transaction = MagicMock(return_value=transaction)

    result = await service.rotate_refresh_token(
        "stable-refresh-token", user_agent="browser", ip_address="127.0.0.1"
    )

    assert result["access_token"]
    assert result["expires_at"] == expires_at
    assert result["refresh_token"] is None
    repo.revoke_refresh_token_family.assert_not_called()
