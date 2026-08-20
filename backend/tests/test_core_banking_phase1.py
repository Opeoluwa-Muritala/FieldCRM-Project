from __future__ import annotations

import hashlib
import hmac
import time
from datetime import datetime, timezone
from decimal import Decimal
from uuid import uuid4

import pytest
from fastapi import HTTPException

from app.domains.core_banking.mock import MockCoreBankingProvider
from app.domains.core_banking.models import CoreBankingLoan, SyncStatus, SyncTrigger
from app.domains.core_banking.provider import ProviderRegistry
from app.domains.core_banking.router import _verify_webhook
from app.domains.core_banking.service import CoreBankingDisabled, CoreBankingService


class FakeRepository:
    def __init__(self, *, cbs_enabled=True, external_customer_id="CBS-C-1", external_loan_id="CBS-L-1"):
        self.loan_id = uuid4()
        self.org_id = uuid4()
        self.mapping = {
            "id": self.loan_id,
            "org_id": self.org_id,
            "cbs_enabled": cbs_enabled,
            "cbs_provider": "mock",
            "external_customer_id": external_customer_id,
            "external_loan_id": external_loan_id,
        }
        self.transactions = {}
        self.issues = []
        self.snapshot = None
        self.failures = []
        self.successes = []
        self.finished = []

    async def get_mapping(self, loan_id, org_id):
        return self.mapping if loan_id == self.loan_id and org_id == self.org_id else None

    async def start_run(self, **kwargs):
        return uuid4()

    async def finish_run(self, run_id, **kwargs):
        self.finished.append(kwargs)

    async def record_issue(self, **kwargs):
        self.issues.append(kwargs)

    async def mark_failure(self, *args):
        self.failures.append(args)

    async def apply_snapshot(self, loan_id, org_id, provider, loan):
        self.snapshot = loan

    async def insert_transaction(self, loan_id, org_id, provider, transaction):
        previous = self.transactions.get(transaction.external_transaction_id)
        if previous is None:
            self.transactions[transaction.external_transaction_id] = transaction
            return "inserted"
        return "duplicate" if previous == transaction else "conflict"

    async def upsert_installment(self, *args):
        return None

    async def mark_success(self, *args):
        self.successes.append(args)


def service(repo, provider=None, *, enabled=True):
    return CoreBankingService(
        repo,
        ProviderRegistry([provider or MockCoreBankingProvider()]),
        enabled=enabled,
    )


@pytest.mark.asyncio
async def test_cbs_is_fail_closed_when_deployment_flag_is_off():
    repo = FakeRepository()
    with pytest.raises(CoreBankingDisabled):
        await service(repo, enabled=False).sync_loan(
            org_id=repo.org_id, loan_id=repo.loan_id, trigger=SyncTrigger.MANUAL
        )
    assert repo.snapshot is None


@pytest.mark.asyncio
async def test_cbs_is_fail_closed_when_product_flag_is_off():
    repo = FakeRepository(cbs_enabled=False)
    with pytest.raises(CoreBankingDisabled):
        await service(repo).sync_loan(
            org_id=repo.org_id, loan_id=repo.loan_id, trigger=SyncTrigger.MANUAL
        )
    assert repo.snapshot is None


@pytest.mark.asyncio
async def test_missing_customer_creates_explicit_reconciliation_issue():
    repo = FakeRepository(external_customer_id=None)
    result = await service(repo).sync_loan(
        org_id=repo.org_id, loan_id=repo.loan_id, trigger=SyncTrigger.MANUAL
    )
    assert result.status == SyncStatus.UNMATCHED_CUSTOMER
    assert repo.issues[0]["issue_type"] == "unmatched_customer"
    assert repo.failures[0][2] == "unmatched_customer"


@pytest.mark.asyncio
async def test_mock_sync_is_end_to_end_and_transaction_idempotent():
    repo = FakeRepository()
    first = await service(repo).sync_loan(
        org_id=repo.org_id, loan_id=repo.loan_id, trigger=SyncTrigger.MANUAL
    )
    second = await service(repo).sync_loan(
        org_id=repo.org_id, loan_id=repo.loan_id, trigger=SyncTrigger.SCHEDULED
    )
    assert first.status == SyncStatus.SUCCESS
    assert first.transactions_imported == 1
    assert first.schedules_imported == 6
    assert second.status == SyncStatus.SUCCESS
    assert second.transactions_imported == 0
    assert len(repo.transactions) == 1
    assert repo.snapshot.outstanding_balance >= 0
    assert not repo.failures


@pytest.mark.asyncio
async def test_provider_cannot_substitute_a_different_financial_record():
    repo = FakeRepository()
    wrong = CoreBankingLoan(
        external_customer_id="CBS-C-OTHER",
        external_loan_id="CBS-L-OTHER",
        outstanding_balance=Decimal("100"),
        principal_balance=Decimal("100"),
        arrears_amount=Decimal("0"),
        days_past_due=0,
        loan_status="active",
        disbursed_amount=Decimal("100"),
        disbursed_at=datetime.now(timezone.utc),
        source_updated_at=datetime.now(timezone.utc),
    )
    provider = MockCoreBankingProvider({"CBS-L-1": wrong})
    result = await service(repo, provider).sync_loan(
        org_id=repo.org_id, loan_id=repo.loan_id, trigger=SyncTrigger.MANUAL
    )
    assert result.status == SyncStatus.FAILED
    assert repo.snapshot is None
    assert repo.issues[0]["issue_type"] == "sync_failed"


def test_webhook_signature_rejects_replay_and_accepts_current_hmac(monkeypatch):
    secret = "s" * 32
    monkeypatch.setattr("app.domains.core_banking.router.settings.CBS_WEBHOOK_SECRET", secret)
    body = b'{"event_id":"evt-1"}'
    timestamp = int(time.time())
    digest = hmac.new(secret.encode(), str(timestamp).encode() + b"." + body, hashlib.sha256).hexdigest()
    _verify_webhook(body, str(timestamp), f"sha256={digest}")

    with pytest.raises(HTTPException) as replay:
        _verify_webhook(body, str(timestamp - 301), f"sha256={digest}")
    assert replay.value.status_code == 401

    with pytest.raises(HTTPException) as forged:
        _verify_webhook(body, str(timestamp), "sha256=bad")
    assert forged.value.status_code == 401


def test_phase1_migration_has_working_down_and_append_only_controls():
    from pathlib import Path

    root = Path(__file__).resolve().parents[1] / "migrations"
    up = (root / "042_core_banking.sql").read_text(encoding="utf-8")
    down = (root / "042_core_banking.rollback.sql").read_text(encoding="utf-8")
    assert "cbs_enabled BOOLEAN NOT NULL DEFAULT FALSE" in up
    assert "core_banking_transactions_append_only" in up
    assert "ENABLE ROW LEVEL SECURITY" in up
    assert "DROP TABLE IF EXISTS core_banking_transactions" in down
    assert "DROP COLUMN IF EXISTS external_customer_id" in down
