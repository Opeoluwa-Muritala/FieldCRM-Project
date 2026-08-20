from __future__ import annotations

import hashlib
from datetime import date, datetime, timedelta, timezone
from decimal import Decimal

from app.domains.core_banking.models import (
    CoreBankingInstallment,
    CoreBankingLoan,
    CoreBankingTransaction,
)
from app.domains.core_banking.provider import CoreBankingProviderError


class MockCoreBankingProvider:
    """Deterministic development/demo adapter.

    Fixtures can be supplied by tests. Without a fixture, IDs prefixed with
    `missing-` simulate a provider miss and all other IDs produce stable data.
    """

    name = "mock"

    def __init__(self, fixtures: dict[str, CoreBankingLoan] | None = None):
        self._fixtures = fixtures or {}

    async def fetch_loan(self, *, external_customer_id: str, external_loan_id: str) -> CoreBankingLoan:
        if external_loan_id in self._fixtures:
            return self._fixtures[external_loan_id]
        if external_customer_id.startswith("missing-") or external_loan_id.startswith("missing-"):
            raise CoreBankingProviderError("Core Banking record was not found")

        seed = int(hashlib.sha256(external_loan_id.encode("utf-8")).hexdigest()[:8], 16)
        principal = Decimal(100_000 + seed % 4_900_001).quantize(Decimal("0.01"))
        paid = (principal * Decimal("0.20")).quantize(Decimal("0.01"))
        outstanding = principal - paid
        dpd = seed % 46
        arrears = (outstanding * Decimal("0.05")).quantize(Decimal("0.01")) if dpd else Decimal("0")
        now = datetime.now(timezone.utc).replace(microsecond=0)
        disbursed_at = now - timedelta(days=120)
        transaction = CoreBankingTransaction(
            external_transaction_id=f"MOCK-TXN-{external_loan_id}-001",
            transaction_type="repayment",
            amount=paid,
            transaction_at=now - timedelta(days=15),
            value_date=(now - timedelta(days=15)).date(),
            source_updated_at=now,
        )
        installments = tuple(
            CoreBankingInstallment(
                external_installment_id=f"MOCK-SCH-{external_loan_id}-{number:03d}",
                installment_no=number,
                due_date=date.today() + timedelta(days=30 * (number - 1)),
                principal_due=(principal / Decimal(6)).quantize(Decimal("0.01")),
                interest_due=(principal * Decimal("0.01")).quantize(Decimal("0.01")),
                total_due=((principal / Decimal(6)) + principal * Decimal("0.01")).quantize(Decimal("0.01")),
                amount_paid=paid if number == 1 else Decimal("0"),
                status="paid" if number == 1 else "pending",
                source_updated_at=now,
            )
            for number in range(1, 7)
        )
        return CoreBankingLoan(
            external_customer_id=external_customer_id,
            external_loan_id=external_loan_id,
            outstanding_balance=outstanding,
            principal_balance=outstanding,
            arrears_amount=arrears,
            days_past_due=dpd,
            loan_status="active",
            disbursed_amount=principal,
            disbursed_at=disbursed_at,
            source_updated_at=now,
            transactions=(transaction,),
            schedule=installments,
        )

    async def resolve_event(self, event: dict) -> tuple[str, str]:
        customer_id = str(event.get("external_customer_id") or "").strip()
        loan_id = str(event.get("external_loan_id") or "").strip()
        if not customer_id or not loan_id:
            raise CoreBankingProviderError("Webhook event is missing Core Banking identifiers")
        return customer_id, loan_id
