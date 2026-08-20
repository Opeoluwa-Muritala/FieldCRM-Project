from __future__ import annotations

from typing import Protocol, runtime_checkable

from app.domains.core_banking.models import CoreBankingLoan


class CoreBankingProviderError(RuntimeError):
    """Safe provider failure whose message may be recorded for staff."""


class CoreBankingRecordNotFound(CoreBankingProviderError):
    pass


@runtime_checkable
class CoreBankingProvider(Protocol):
    name: str

    async def fetch_loan(
        self,
        *,
        external_customer_id: str,
        external_loan_id: str,
    ) -> CoreBankingLoan:
        """Return a provider-normalized financial snapshot."""

    async def resolve_event(self, event: dict) -> tuple[str, str]:
        """Return `(external_customer_id, external_loan_id)` for a webhook event."""


class ProviderRegistry:
    def __init__(self, providers: list[CoreBankingProvider]):
        self._providers = {provider.name: provider for provider in providers}

    def get(self, name: str) -> CoreBankingProvider:
        provider = self._providers.get(name.strip().lower())
        if provider is None:
            raise CoreBankingProviderError("Configured Core Banking provider is unavailable")
        return provider
