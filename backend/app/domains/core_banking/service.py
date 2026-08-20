from __future__ import annotations

import logging
from decimal import Decimal
from uuid import UUID

from app.domains.core_banking.models import CoreBankingLoan, SyncResult, SyncStatus, SyncTrigger
from app.domains.core_banking.provider import CoreBankingProviderError, ProviderRegistry
from app.domains.core_banking.repository import CoreBankingRepository

log = logging.getLogger("CoreBankingSync")


class CoreBankingDisabled(RuntimeError):
    pass


class CoreBankingMappingError(RuntimeError):
    pass


class CoreBankingService:
    def __init__(self, repo: CoreBankingRepository, providers: ProviderRegistry, *, enabled: bool):
        self.repo = repo
        self.providers = providers
        self.enabled = enabled

    @staticmethod
    def _validate_snapshot(loan: CoreBankingLoan, expected_customer_id: str, expected_loan_id: str) -> None:
        if loan.external_customer_id != expected_customer_id or loan.external_loan_id != expected_loan_id:
            raise CoreBankingProviderError("Core Banking returned identifiers that do not match the requested record")
        amounts = (
            loan.outstanding_balance, loan.principal_balance, loan.arrears_amount,
            loan.disbursed_amount if loan.disbursed_amount is not None else Decimal("0"),
        )
        if any(amount < 0 for amount in amounts) or loan.days_past_due < 0:
            raise CoreBankingProviderError("Core Banking returned invalid financial values")
        if not loan.loan_status.strip() or len(loan.loan_status) > 100:
            raise CoreBankingProviderError("Core Banking returned an invalid loan status")
        for transaction in loan.transactions:
            if not transaction.external_transaction_id.strip() or transaction.amount <= 0:
                raise CoreBankingProviderError("Core Banking returned an invalid transaction")
            if len(transaction.currency) != 3 or not transaction.currency.isalpha():
                raise CoreBankingProviderError("Core Banking returned an invalid transaction currency")
        for installment in loan.schedule:
            if installment.installment_no <= 0 or min(
                installment.principal_due,
                installment.interest_due,
                installment.total_due,
                installment.amount_paid,
            ) < 0:
                raise CoreBankingProviderError("Core Banking returned an invalid schedule")

    async def _unmatched(
        self, *, mapping: dict, run_id: UUID, provider: str, status: SyncStatus,
        requested_reference: str | None, message: str,
    ) -> SyncResult:
        loan_id = mapping["id"]
        org_id = mapping["org_id"]
        await self.repo.record_issue(
            loan_id=loan_id,
            org_id=org_id,
            provider=provider,
            issue_type=status.value,
            external_reference=requested_reference,
            details=message,
        )
        await self.repo.mark_failure(loan_id, org_id, status.value, message)
        await self.repo.finish_run(run_id, status=status.value, error_code=status.value, error_message=message)
        return SyncResult(status=status, loan_id=str(loan_id), provider=provider, message=message)

    async def sync_loan(
        self,
        *,
        org_id: UUID,
        loan_id: UUID,
        trigger: SyncTrigger,
        requested_by: UUID | None = None,
        external_event_id: str | None = None,
    ) -> SyncResult:
        if not self.enabled:
            raise CoreBankingDisabled("Core Banking integration is disabled")
        mapping = await self.repo.get_mapping(loan_id, org_id)
        if not mapping:
            raise CoreBankingMappingError("Loan application was not found")
        if not mapping.get("cbs_enabled"):
            raise CoreBankingDisabled("Core Banking is not enabled for this loan product")

        provider_name = str(mapping.get("cbs_provider") or "mock").strip().lower()
        run_id = await self.repo.start_run(
            loan_id=loan_id,
            org_id=org_id,
            provider=provider_name,
            trigger=trigger.value,
            requested_by=requested_by,
            external_event_id=external_event_id,
        )
        if run_id is None:
            return SyncResult(
                status=SyncStatus.SUCCESS,
                loan_id=str(loan_id),
                provider=provider_name,
                message="Webhook event was already processed",
            )

        external_customer_id = str(mapping.get("external_customer_id") or "").strip()
        if not external_customer_id:
            return await self._unmatched(
                mapping=mapping,
                run_id=run_id,
                provider=provider_name,
                status=SyncStatus.UNMATCHED_CUSTOMER,
                requested_reference=None,
                message="No Core Banking customer is mapped to this application",
            )
        external_loan_id = str(mapping.get("external_loan_id") or "").strip()
        if not external_loan_id:
            return await self._unmatched(
                mapping=mapping,
                run_id=run_id,
                provider=provider_name,
                status=SyncStatus.UNMATCHED_LOAN,
                requested_reference=None,
                message="No Core Banking loan is mapped to this application",
            )

        try:
            provider = self.providers.get(provider_name)
            loan = await provider.fetch_loan(
                external_customer_id=external_customer_id,
                external_loan_id=external_loan_id,
            )
            self._validate_snapshot(loan, external_customer_id, external_loan_id)
            await self.repo.apply_snapshot(loan_id, org_id, provider_name, loan)

            imported_transactions = 0
            reconciliation_issues = 0
            for transaction in loan.transactions:
                outcome = await self.repo.insert_transaction(loan_id, org_id, provider_name, transaction)
                if outcome == "inserted":
                    imported_transactions += 1
                elif outcome == "conflict":
                    reconciliation_issues += 1
                    await self.repo.record_issue(
                        loan_id=loan_id,
                        org_id=org_id,
                        provider=provider_name,
                        issue_type="transaction_conflict",
                        external_reference=transaction.external_transaction_id,
                        details="An existing CBS transaction has different immutable financial values",
                    )

            for installment in loan.schedule:
                await self.repo.upsert_installment(loan_id, org_id, provider_name, installment)

            await self.repo.mark_success(loan_id, org_id, provider_name, loan.source_updated_at)
            await self.repo.finish_run(
                run_id,
                status=SyncStatus.SUCCESS.value,
                transactions=imported_transactions,
                schedules=len(loan.schedule),
            )
            return SyncResult(
                status=SyncStatus.SUCCESS,
                loan_id=str(loan_id),
                provider=provider_name,
                transactions_imported=imported_transactions,
                schedules_imported=len(loan.schedule),
                reconciliation_issues=reconciliation_issues,
            )
        except CoreBankingProviderError as exc:
            safe_message = str(exc)[:500] or "Core Banking synchronization failed"
        except Exception:
            log.exception("Unexpected CBS sync failure for loan %s", loan_id)
            safe_message = "Core Banking synchronization failed"

        await self.repo.record_issue(
            loan_id=loan_id,
            org_id=org_id,
            provider=provider_name,
            issue_type="sync_failed",
            external_reference=external_loan_id,
            details=safe_message,
        )
        await self.repo.mark_failure(loan_id, org_id, SyncStatus.FAILED.value, safe_message)
        await self.repo.finish_run(
            run_id,
            status=SyncStatus.FAILED.value,
            error_code="provider_error",
            error_message=safe_message,
        )
        return SyncResult(
            status=SyncStatus.FAILED,
            loan_id=str(loan_id),
            provider=provider_name,
            message=safe_message,
        )
    async def sync_batch(self, *, org_id: UUID, limit: int = 100) -> list[SyncResult]:
        if not self.enabled:
            raise CoreBankingDisabled("Core Banking integration is disabled")
        candidates = await self.repo.list_candidates(org_id, min(max(limit, 1), 500))
        return [
            await self.sync_loan(
                org_id=org_id,
                loan_id=row["id"],
                trigger=SyncTrigger.SCHEDULED,
            )
            for row in candidates
        ]

    async def ingest_event(
        self,
        *,
        org_id: UUID,
        provider_name: str,
        external_event_id: str,
        event: dict,
    ) -> SyncResult:
        if not self.enabled:
            raise CoreBankingDisabled("Core Banking integration is disabled")
        provider_name = provider_name.strip().lower()
        provider = self.providers.get(provider_name)
        external_customer_id, external_loan_id = await provider.resolve_event(event)
        mapping = await self.repo.find_mapping_by_external_loan(org_id, provider_name, external_loan_id)
        if not mapping or mapping.get("external_customer_id") != external_customer_id:
            run_id = await self.repo.start_run(
                loan_id=mapping.get("id") if mapping else None,
                org_id=org_id,
                provider=provider_name,
                trigger=SyncTrigger.WEBHOOK.value,
                requested_by=None,
                external_event_id=external_event_id,
            )
            if run_id is None:
                return SyncResult(SyncStatus.SUCCESS, None, provider_name, message="Webhook event was already processed")
            status = SyncStatus.UNMATCHED_CUSTOMER if mapping else SyncStatus.UNMATCHED_LOAN
            message = "Webhook event does not match a configured FieldCRM CBS mapping"
            await self.repo.record_issue(
                loan_id=mapping.get("id") if mapping else None,
                org_id=org_id,
                provider=provider_name,
                issue_type=status.value,
                external_reference=external_loan_id,
                details=message,
            )
            await self.repo.finish_run(run_id, status=status.value, error_code=status.value, error_message=message)
            return SyncResult(status, str(mapping["id"]) if mapping else None, provider_name, message=message)
        return await self.sync_loan(
            org_id=org_id,
            loan_id=mapping["id"],
            trigger=SyncTrigger.WEBHOOK,
            external_event_id=external_event_id,
        )
