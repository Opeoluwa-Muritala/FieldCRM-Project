import pytest

from app.config import settings
from app.domains.aml.service import screen_entity
from app.domains.credit_bureau.service import CreditBureauService
from app.domains.verification.service import verify_bvn


@pytest.mark.asyncio
async def test_demo_identity_and_aml_fixtures_are_not_live_in_normal_development(monkeypatch):
    monkeypatch.setattr(settings, "DEMO_ENABLED", False)
    monkeypatch.setattr(settings, "QORE_API_KEY", None)
    monkeypatch.setattr(settings, "AML_YOUVERIFY_TOKEN", None)

    bvn_result = await verify_bvn("22216142222")
    aml_result = await screen_entity("Jane Doe")

    assert bvn_result["status"] == "not_configured"
    assert bvn_result["is_valid"] is False
    assert aml_result["match_status"] == "not_configured"


@pytest.mark.asyncio
async def test_unconfigured_credit_bureau_never_returns_mock_success(monkeypatch):
    monkeypatch.setattr(settings, "DEMO_ENABLED", False)
    monkeypatch.setattr(settings, "CRC_API_KEY", None)
    monkeypatch.setattr(settings, "CREDIT_REGISTRY_USERNAME", None)
    monkeypatch.setattr(settings, "CREDIT_REGISTRY_PASSWORD", None)
    service = CreditBureauService()

    assert await service.get_session_code() == ""
    assert await service.find_customer("mock_session_code_12345", "22216142222") == ""
    report = await service.get_report("application-id", "mock_registry", "mock_session_code_12345")
    assert report == {"status": "not_configured", "data": {}}


@pytest.mark.asyncio
async def test_demo_credit_bureau_fixture_remains_available_only_in_demo(monkeypatch):
    monkeypatch.setattr(settings, "DEMO_ENABLED", True)
    monkeypatch.setattr(settings, "VERCEL_ENV", "preview")
    monkeypatch.setattr(settings, "CRC_API_KEY", None)
    monkeypatch.setattr(settings, "CREDIT_REGISTRY_USERNAME", None)
    monkeypatch.setattr(settings, "CREDIT_REGISTRY_PASSWORD", None)
    service = CreditBureauService()

    session_code = await service.get_session_code()
    assert session_code.startswith("mock_")
    assert await service.find_customer(session_code, "22216142222") == "mock_registry_id_999888"
