from __future__ import annotations

import base64
from datetime import date
from types import SimpleNamespace
from uuid import uuid4

import pytest
from fastapi import HTTPException
from pydantic import ValidationError

from app.config import settings
from app.domains.customers.router import _enabled, router
from app.domains.customers.schemas import CustomerCreate, CustomerInput
from app.domains.customers.service import (
    EVENT_TAXONOMY,
    CustomerService,
    DuplicateOverrideRequired,
    can_view_customer,
    name_signature,
)


@pytest.fixture(autouse=True)
def identity_keys(monkeypatch):
    key = base64.urlsafe_b64encode(b"k" * 32).decode("ascii")
    lookup = base64.urlsafe_b64encode(b"l" * 32).decode("ascii")
    monkeypatch.setattr(settings, "FIELD_ENCRYPTION_KEY", key)
    monkeypatch.setattr(settings, "FIELD_LOOKUP_KEY", lookup)


class FakeCustomerRepository:
    def __init__(self):
        self.candidates = []
        self.created = None
        self.accounts = []
        self.overrides = []
        self.activities = []

    async def duplicate_candidates(self, org_id, values):
        return self.candidates

    async def create(self, values):
        self.created = dict(values)
        self.created.update({
            "id": uuid4(), "created_at": None, "updated_at": None, "active": True,
            "relationship_officer_id": values["created_by"],
        })
        return dict(self.created)

    async def add_account(self, **values):
        self.accounts.append(values)

    async def add_override(self, **values):
        self.overrides.append(values)

    async def add_activity(self, **values):
        self.activities.append(values)


def test_customer_must_be_at_least_eighteen():
    today = date.today()
    adult_dob = today.replace(year=today.year - 18)
    CustomerInput(legal_name="Adult Applicant", date_of_birth=adult_dob)
    with pytest.raises(ValidationError, match="Applicant must be at least 18 years old"):
        CustomerInput(legal_name="Minor Applicant", date_of_birth=adult_dob.replace(year=adult_dob.year + 1))


@pytest.mark.asyncio
async def test_bvn_nin_or_phone_match_requires_typed_override_before_create():
    repo = FakeCustomerRepository()
    service = CustomerService(repo)
    payload = CustomerCreate(
        legal_name="Ada Nwosu", date_of_birth=date(1990, 1, 2),
        phone="08031234567", bvn="12345678901", nin="10987654321",
    )
    prepared = service._prepared(payload)
    duplicate_id = uuid4()
    repo.candidates = [{
        "id": duplicate_id, "customer_number": "CUST-EXISTING", "legal_name": "Ada Nwosu",
        "name_signature": name_signature("Ada Nwosu"), "date_of_birth": payload.date_of_birth,
        "bvn_lookup_hash": prepared["bvn_hash"], "nin_lookup_hash": prepared["nin_hash"],
        "phone_lookup_hash": prepared["phone_hash"], "email_lookup_hash": None,
        "normalized_address": None, "external_customer_id": None, "cbs_provider": None,
        "account_match": False,
    }]
    with pytest.raises(DuplicateOverrideRequired) as exc:
        await service.create(org_id=uuid4(), actor_id=uuid4(), branch_id=uuid4(), payload=payload)
    assert set(exc.value.matches[0].matched_rules) >= {"same_bvn", "same_nin", "same_phone"}
    assert repo.created is None


@pytest.mark.asyncio
async def test_override_reason_is_append_only_evidence_and_customer_is_created():
    repo = FakeCustomerRepository()
    service = CustomerService(repo)
    base = CustomerCreate(legal_name="Kemi Adeyemi", phone="08030000000")
    prepared = service._prepared(base)
    repo.candidates = [{
        "id": uuid4(), "customer_number": "CUST-1", "legal_name": "Kemi Adeyemi",
        "name_signature": name_signature("Kemi Adeyemi"), "date_of_birth": None,
        "phone_lookup_hash": prepared["phone_hash"], "bvn_lookup_hash": None,
        "nin_lookup_hash": None, "email_lookup_hash": None, "normalized_address": None,
        "external_customer_id": None, "cbs_provider": None, "account_match": False,
    }]
    payload = CustomerCreate(
        legal_name="Kemi Adeyemi", phone="08030000000",
        duplicate_override_reason="Confirmed distinct person after reviewing original ID.",
    )
    customer = await service.create(org_id=uuid4(), actor_id=uuid4(), branch_id=uuid4(), payload=payload)
    assert customer["legal_name"] == "Kemi Adeyemi"
    assert len(repo.overrides) == 1
    assert repo.overrides[0]["reason"].startswith("Confirmed distinct")
    assert repo.activities[0]["event_type"] == "created"


def test_customer_object_authorization_excludes_system_admin_and_cross_branch_access():
    org_id, actor_id, branch_id = uuid4(), uuid4(), uuid4()
    customer = {"org_id": org_id, "created_by": actor_id, "relationship_officer_id": actor_id, "branch_id": branch_id}
    officer = SimpleNamespace(org_id=org_id, id=actor_id, branch_id=branch_id, role="account_officer", is_active=True)
    other_manager = SimpleNamespace(org_id=org_id, id=uuid4(), branch_id=uuid4(), role="branch_manager", is_active=True)
    system_admin = SimpleNamespace(org_id=org_id, id=uuid4(), branch_id=branch_id, role="system_admin", is_active=True)
    assert can_view_customer(officer, customer)
    assert not can_view_customer(other_manager, customer)
    assert not can_view_customer(system_admin, customer)


def test_customer_timeline_taxonomy_is_explicit_and_complete():
    assert {
        "created", "edited", "submitted", "returned", "document_uploaded",
        "visit_completed", "credit_reviewed", "approved", "cbs_sync",
        "repayment_detected", "collection_action", "configuration_applied",
        "workflow_transition",
    } <= EVENT_TAXONOMY


def test_customer_routes_are_staff_only_feature_gated(monkeypatch):
    paths = {route.path for route in router.routes}
    assert "/customers/{customer_id}" in paths
    assert "/api/v1/customers/duplicates" in paths
    monkeypatch.setattr(settings, "CUSTOMER_IDENTITY_ENABLED", False)
    with pytest.raises(HTTPException) as exc:
        _enabled()
    assert exc.value.status_code == 404


def test_phase2_migration_is_reversible_and_protects_override_history():
    from pathlib import Path

    root = Path(__file__).resolve().parents[1] / "migrations"
    up = (root / "043_customer_identity.sql").read_text(encoding="utf-8")
    down = (root / "043_customer_identity.rollback.sql").read_text(encoding="utf-8")
    assert "customer_duplicate_overrides_append_only" in up
    assert "ENABLE ROW LEVEL SECURITY" in up
    assert "ADD COLUMN IF NOT EXISTS customer_id" in up
    assert "DROP COLUMN IF EXISTS customer_id" in down
    assert "DROP TABLE IF EXISTS customers" in down
