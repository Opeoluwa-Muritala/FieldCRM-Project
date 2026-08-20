from contextlib import asynccontextmanager
from datetime import UTC, datetime
from pathlib import Path
from types import SimpleNamespace
from uuid import uuid4

import pytest
from fastapi import HTTPException
from starlette.requests import Request

from app.config import settings
from app.core.config import Settings
from app.core.exceptions import DomainException
from app.domains.configuration.access import require_restricted_configuration_access
from app.domains.configuration.catalog import FEATURE_DEFAULTS, FEATURE_GROUPS, SECTIONS, default_payload
from app.domains.configuration.mfa import qr_code_data_url, token_is_valid, totp, verification_token, verify_totp
from app.domains.configuration.gates import required_feature_for_path
from app.domains.configuration.schemas import DraftPatch
from app.domains.configuration.service import ConfigurationService


class Transaction:
    async def __aenter__(self): return self
    async def __aexit__(self, *args): return False


class FakeRepo:
    def __init__(self, payload=None):
        self.row = {
            "id": uuid4(), "status": "draft", "payload": payload or default_payload("MMFB"),
            "high_risk": False, "requires_second_approval": False, "approved_by": None,
        }
        self.products = []
        self.changes = []
        self.conn = SimpleNamespace(transaction=lambda: Transaction())

    async def get(self, *args, **kwargs): return self.row
    async def patch(self, version_id, org_id, payload, high_risk):
        self.row.update(payload=payload, high_risk=high_risk)
        return self.row
    async def log_change(self, **values): self.changes.append(values)
    async def product_dependencies(self, org_id): return self.products
    async def validate(self, version_id, org_id, actor_id, needs_approval):
        self.row.update(status="pending_approval" if needs_approval else "validated",
                        requires_second_approval=needs_approval)
        return self.row
    async def approve(self, version_id, org_id, actor_id):
        if actor_id == self.row.get("created_by") or actor_id == self.row.get("validated_by"):
            return None
        self.row.update(status="validated", approved_by=actor_id)
        return self.row


def test_hub_is_separate_and_defaults_external_surfaces_off():
    assert "Users & Access" in SECTIONS and "System Health" in SECTIONS
    assert FEATURE_DEFAULTS["external_applicant_portal"] is False
    assert FEATURE_DEFAULTS["cbs_integration"] is False
    assert FEATURE_DEFAULTS["offline_mode"] is False


def test_configuration_ui_catalog_only_contains_working_feature_controls():
    catalog_keys = [key for group in FEATURE_GROUPS for key, _label, _description in group["features"]]
    assert len(catalog_keys) == len(set(catalog_keys))
    assert set(catalog_keys) == set(FEATURE_DEFAULTS)

    template_root = Path(__file__).resolve().parents[2] / "frontend/templates/configuration"
    template = (
        template_root / "base.html"
    ).read_text(encoding="utf-8")
    assert "Configuration pages" in template
    assert "Product configuration" in template
    assert 'href="/configuration/features"' in template
    assert 'href="/configuration/versions"' in template
    assert "Feature controls" in template
    assert "Version history" in template
    assert "Planned" not in template
    assert "Advanced setting editor" not in template


def test_disabled_feature_routes_have_server_side_gate_mappings():
    assert required_feature_for_path("/my-work") == "my_work"
    assert required_feature_for_path("/applications/abc/ocr-review") == "ocr"
    assert required_feature_for_path("/applications/abc/guarantors/1/step/2") == "guarantors"
    assert required_feature_for_path("/applications/abc/collateral") == "collateral"
    assert required_feature_for_path("/reports/par") == "par"
    assert required_feature_for_path("/mcc") == "committee_review"
    assert required_feature_for_path("/configuration/features") is None


@pytest.mark.asyncio
async def test_feature_change_is_high_risk_versioned_and_dependency_checked():
    repo = FakeRepo()
    service = ConfigurationService(repo)
    await service.patch(repo.row["id"], uuid4(), uuid4(), DraftPatch(
        setting_path="features.guarantors", value=False,
        reason="Institution policy disables guarantor capture.",
    ))
    assert repo.row["payload"]["features"]["guarantors"] is False
    assert repo.row["high_risk"] is True
    assert repo.changes[0]["old"] is True
    repo.products = [{"code": "SME", "guarantor_required": True, "collateral_required": False, "cbs_enabled": False}]
    with pytest.raises(DomainException, match="requires guarantors"):
        await service.validate(repo.row["id"], uuid4(), uuid4())


@pytest.mark.asyncio
async def test_high_risk_configuration_requires_different_approver():
    creator = uuid4()
    repo = FakeRepo()
    repo.row.update(high_risk=True, created_by=creator, validated_by=creator)
    service = ConfigurationService(repo)
    await service.validate(repo.row["id"], uuid4(), creator)
    assert repo.row["status"] == "pending_approval"
    with pytest.raises(DomainException, match="different Configuration Admin"):
        await service.approve(repo.row["id"], uuid4(), creator)
    approver = uuid4()
    await service.approve(repo.row["id"], uuid4(), approver)
    assert repo.row["approved_by"] == approver


def test_totp_and_short_lived_verification_token(monkeypatch):
    secret = "JBSWY3DPEHPK3PXP"
    code = totp(secret, 1_777_000_000)
    monkeypatch.setattr("app.domains.configuration.mfa.time.time", lambda: 1_777_000_000)
    assert verify_totp(secret, code)
    user_id = uuid4()
    assert token_is_valid(verification_token(user_id), user_id)
    assert not token_is_valid(verification_token(user_id), uuid4())


def test_mfa_enrollment_qr_is_generated_locally_as_png():
    data_url = qr_code_data_url(
        "otpauth://totp/FieldCRM%20Configuration:admin%40fieldcrm.com"
        "?secret=JBSWY3DPEHPK3PXP&issuer=FieldCRM%20Configuration"
    )
    encoded = data_url.removeprefix("data:image/png;base64,")
    import base64
    assert base64.b64decode(encoded).startswith(b"\x89PNG\r\n\x1a\n")
    with pytest.raises(ValueError, match="Invalid authenticator"):
        qr_code_data_url("https://example.com/secret")


def _configuration_request(*, host="localhost", client="127.0.0.1"):
    return Request({"type": "http", "method": "GET", "path": "/configuration",
                    "scheme": "http", "server": (host, 8000),
                    "headers": [(b"host", host.encode("ascii"))], "client": (client, 5000)})


def test_configuration_access_gate_is_role_and_localhost_restricted(monkeypatch):
    monkeypatch.setattr(settings, "CONFIGURATION_HUB_ENABLED", True)
    monkeypatch.setattr(settings, "APP_ENV", "development")
    request = _configuration_request()
    wrong_role = SimpleNamespace(role="system_admin", id=uuid4())
    with pytest.raises(HTTPException) as exc:
        require_restricted_configuration_access(request, wrong_role, require_mfa=False)
    assert exc.value.status_code == 403
    config_admin = SimpleNamespace(role="configuration_admin", id=uuid4())
    require_restricted_configuration_access(request, config_admin, require_mfa=False)

    with pytest.raises(HTTPException) as exc:
        require_restricted_configuration_access(
            _configuration_request(host="admin.example", client="203.0.113.9"),
            config_admin,
            require_mfa=False,
        )
    assert exc.value.status_code == 404


def test_configuration_hub_is_rejected_in_production(monkeypatch):
    monkeypatch.setattr(settings, "CONFIGURATION_HUB_ENABLED", True)
    monkeypatch.setattr(settings, "APP_ENV", "production")
    config_admin = SimpleNamespace(role="configuration_admin", id=uuid4())
    with pytest.raises(HTTPException) as exc:
        require_restricted_configuration_access(_configuration_request(), config_admin, require_mfa=False)
    assert exc.value.status_code == 404

    key = "MDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDA="
    with pytest.raises(ValueError, match="localhost-only"):
        Settings(
            APP_ENV="production",
            DATABASE_URL="postgresql://fieldcrm_app:secret@example-pooler.neon.tech/neondb",
            JWT_SECRET_KEY="a-fixed-production-secret-that-is-long-enough",
            CORS_ORIGINS="https://fieldcrm.example",
            TRUSTED_HOSTS="fieldcrm.example",
            APP_BASE_URL="https://fieldcrm.example",
            RATE_LIMIT_REDIS_URL="rediss://redis.example/0",
            CACHE_REDIS_URL="rediss://redis.example/0",
            FIELD_ENCRYPTION_KEY=key,
            FIELD_LOOKUP_KEY=key,
            CONFIGURATION_HUB_ENABLED=True,
        )


def test_phase3_migration_is_reversible_immutable_and_effective_dated():
    migrations = Path(__file__).resolve().parents[1] / "migrations"
    up = (migrations / "044_configuration_hub.sql").read_text(encoding="utf-8")
    down = (migrations / "044_configuration_hub.rollback.sql").read_text(encoding="utf-8")
    create_sql = (Path(__file__).resolve().parents[1] / "app/domains/loans/queries/create.sql").read_text(encoding="utf-8")
    assert "configuration_admin" in up
    assert "configuration_change_log_immutable" in up
    repository = (Path(__file__).resolve().parents[1] / "app/domains/configuration/repository.py").read_text(encoding="utf-8")
    assert "Second approval for high-risk configuration" in repository
    assert "configuration_versions_published_immutable" in up
    assert "originated_config_version_id" in create_sql and "effective_at <= NOW()" in create_sql
    assert "DROP TABLE IF EXISTS configuration_versions" in down


def test_mobile_contract_is_additive_and_system_admin_cannot_grant_config_admin():
    root = Path(__file__).resolve().parents[1] / "app"
    mobile = (root / "api/v1/mobile.py").read_text(encoding="utf-8")
    users = (root / "domains/users/service.py").read_text(encoding="utf-8")
    assert '"features": payload.get("features", {})' in mobile
    assert '"config_version": version["version_number"]' in mobile
    assert "System Admin cannot grant or change Configuration Admin access" in users
