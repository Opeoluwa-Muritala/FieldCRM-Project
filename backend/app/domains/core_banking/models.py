from __future__ import annotations

from dataclasses import dataclass, field
from datetime import date, datetime, timezone
from decimal import Decimal
from enum import StrEnum


class SyncStatus(StrEnum):
    SUCCESS = "success"
    FAILED = "failed"
    UNMATCHED_CUSTOMER = "unmatched_customer"
    UNMATCHED_LOAN = "unmatched_loan"


class SyncTrigger(StrEnum):
    MANUAL = "manual"
    SCHEDULED = "scheduled"
    WEBHOOK = "webhook"


@dataclass(frozen=True)
class CoreBankingTransaction:
    external_transaction_id: str
    transaction_type: str
    amount: Decimal
    transaction_at: datetime
    value_date: date | None = None
    currency: str = "NGN"
    source_updated_at: datetime = field(default_factory=lambda: datetime.now(timezone.utc))


@dataclass(frozen=True)
class CoreBankingInstallment:
    external_installment_id: str
    installment_no: int
    due_date: date
    principal_due: Decimal
    interest_due: Decimal
    total_due: Decimal
    amount_paid: Decimal
    status: str
    source_updated_at: datetime = field(default_factory=lambda: datetime.now(timezone.utc))


@dataclass(frozen=True)
class CoreBankingLoan:
    external_customer_id: str
    external_loan_id: str
    outstanding_balance: Decimal
    principal_balance: Decimal
    arrears_amount: Decimal
    days_past_due: int
    loan_status: str
    disbursed_amount: Decimal | None
    disbursed_at: datetime | None
    source_updated_at: datetime
    transactions: tuple[CoreBankingTransaction, ...] = ()
    schedule: tuple[CoreBankingInstallment, ...] = ()


@dataclass(frozen=True)
class SyncResult:
    status: SyncStatus
    loan_id: str | None
    provider: str
    transactions_imported: int = 0
    schedules_imported: int = 0
    reconciliation_issues: int = 0
    message: str | None = None
