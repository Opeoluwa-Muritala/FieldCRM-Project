from __future__ import annotations

import hashlib
import hmac
import json
import time
from uuid import UUID

from fastapi import APIRouter, Depends, Header, HTTPException, Request, Response, status
from fastapi.responses import RedirectResponse
from pydantic import BaseModel, Field, ValidationError

from app.core.audit import AuditService
from app.core.config import settings
from app.core.database import DatabaseIdentity, database_identity, get_connection
from app.core.dependencies import authenticated_db_conn, get_current_user
from app.core.loan_authorization import require_view
from app.domains.core_banking.mock import MockCoreBankingProvider
from app.domains.core_banking.models import SyncStatus, SyncTrigger
from app.domains.core_banking.provider import CoreBankingProviderError, ProviderRegistry
from app.domains.core_banking.repository import CoreBankingRepository
from app.domains.core_banking.service import (
    CoreBankingDisabled,
    CoreBankingMappingError,
    CoreBankingService,
)
from app.domains.loans.repository import LoanRepository

router = APIRouter(tags=["Core Banking"])
_providers = ProviderRegistry([MockCoreBankingProvider()])
_SYSTEM_USER_ID = "00000000-0000-0000-0000-000000000000"
_MAX_WEBHOOK_BYTES = 64 * 1024


def _service(conn) -> CoreBankingService:
    return CoreBankingService(
        CoreBankingRepository(conn),
        _providers,
        enabled=settings.CBS_INTEGRATION_ENABLED,
    )


async def _feature_not_found(conn=None, org_id=None) -> None:
    if not settings.CBS_INTEGRATION_ENABLED:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Not found")
    if settings.CONFIGURATION_HUB_ENABLED and conn is not None and org_id is not None:
        from app.domains.configuration.repository import ConfigurationRepository
        from app.domains.configuration.service import ConfigurationService
        if not await ConfigurationService(ConfigurationRepository(conn)).feature_enabled(org_id, "cbs_integration"):
            raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Not found")


async def _authorized_loan(conn, loan_id: UUID, current_user):
    loan = await LoanRepository(conn).get_by_id(loan_id, current_user.org_id)
    if not loan:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Loan application not found")
    require_view(current_user, loan)
    return loan


@router.get("/api/v1/core-banking/loans/{loan_id}")
async def get_core_banking_loan(
    loan_id: UUID,
    conn=Depends(authenticated_db_conn),
    current_user=Depends(get_current_user),
):
    await _feature_not_found(conn, current_user.org_id)
    await _authorized_loan(conn, loan_id, current_user)
    view = await CoreBankingRepository(conn).get_view(loan_id, current_user.org_id)
    return {"source": "Core Banking", "data": view}


@router.post("/api/v1/core-banking/loans/{loan_id}/refresh")
async def refresh_core_banking_loan(
    loan_id: UUID,
    conn=Depends(authenticated_db_conn),
    current_user=Depends(get_current_user),
):
    await _feature_not_found(conn, current_user.org_id)
    await _authorized_loan(conn, loan_id, current_user)
    try:
        result = await _service(conn).sync_loan(
            org_id=current_user.org_id,
            loan_id=loan_id,
            trigger=SyncTrigger.MANUAL,
            requested_by=current_user.id,
        )
    except CoreBankingDisabled as exc:
        raise HTTPException(status_code=status.HTTP_409_CONFLICT, detail=str(exc)) from exc
    except CoreBankingMappingError as exc:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail=str(exc)) from exc
    await AuditService(conn).insert(
        org_id=current_user.org_id,
        entity_type="loan_application",
        entity_id=loan_id,
        action="cbs.sync",
        user_id=current_user.id,
        user_role=current_user.role,
        source="cbs",
        notes=f"status={result.status.value}; provider={result.provider}",
    )
    return result


@router.post("/applications/{loan_id}/cbs/refresh")
async def refresh_core_banking_loan_web(
    loan_id: UUID,
    conn=Depends(authenticated_db_conn),
    current_user=Depends(get_current_user),
):
    await refresh_core_banking_loan(loan_id, conn, current_user)
    return RedirectResponse(url=f"/applications/{loan_id}?cbs_refreshed=1", status_code=status.HTTP_303_SEE_OTHER)


class BatchRequest(BaseModel):
    org_id: UUID
    limit: int = Field(default=100, ge=1, le=500)


def _require_bearer_secret(authorization: str | None, expected: str) -> None:
    supplied = authorization.removeprefix("Bearer ") if authorization else ""
    if not expected or not supplied or not hmac.compare_digest(supplied, expected):
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Invalid job credential")


@router.post("/api/v1/internal/core-banking/sync")
async def run_core_banking_batch(
    payload: BatchRequest,
    authorization: str | None = Header(default=None),
):
    _require_bearer_secret(authorization, settings.CBS_JOB_SECRET)
    identity = DatabaseIdentity(
        org_id=str(payload.org_id), user_id=_SYSTEM_USER_ID, role="system", request_id="cbs-scheduled-sync"
    )
    with database_identity(identity):
        async with get_connection() as conn:
            await _feature_not_found(conn, payload.org_id)
            results = await _service(conn).sync_batch(org_id=payload.org_id, limit=payload.limit)
    return {"processed": len(results), "results": results}


def _verify_webhook(raw_body: bytes, timestamp_header: str | None, signature: str | None) -> None:
    if not settings.CBS_WEBHOOK_SECRET:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Not found")
    try:
        timestamp = int(timestamp_header or "")
    except ValueError as exc:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Invalid webhook signature") from exc
    if abs(int(time.time()) - timestamp) > 300:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Expired webhook signature")
    signed = str(timestamp).encode("ascii") + b"." + raw_body
    expected = hmac.new(settings.CBS_WEBHOOK_SECRET.encode("utf-8"), signed, hashlib.sha256).hexdigest()
    supplied = (signature or "").removeprefix("sha256=")
    if not supplied or not hmac.compare_digest(supplied, expected):
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Invalid webhook signature")


class WebhookEvent(BaseModel):
    event_id: str = Field(min_length=1, max_length=200)
    org_id: UUID
    external_customer_id: str = Field(min_length=1, max_length=200)
    external_loan_id: str = Field(min_length=1, max_length=200)


@router.post("/api/v1/core-banking/webhooks/{provider_name}")
async def ingest_core_banking_webhook(
    provider_name: str,
    request: Request,
    response: Response,
    x_cbs_timestamp: str | None = Header(default=None),
    x_cbs_signature: str | None = Header(default=None),
):
    raw_body = await request.body()
    if len(raw_body) > _MAX_WEBHOOK_BYTES:
        raise HTTPException(status_code=status.HTTP_413_REQUEST_ENTITY_TOO_LARGE, detail="Webhook payload is too large")
    _verify_webhook(raw_body, x_cbs_timestamp, x_cbs_signature)
    try:
        event = WebhookEvent.model_validate(json.loads(raw_body))
    except (json.JSONDecodeError, ValidationError) as exc:
        raise HTTPException(status_code=status.HTTP_422_UNPROCESSABLE_CONTENT, detail="Invalid webhook payload") from exc
    try:
        _providers.get(provider_name)
    except CoreBankingProviderError as exc:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Provider not found") from exc
    identity = DatabaseIdentity(
        org_id=str(event.org_id), user_id=_SYSTEM_USER_ID, role="system", request_id=event.event_id
    )
    with database_identity(identity):
        async with get_connection() as conn:
            await _feature_not_found(conn, event.org_id)
            result = await _service(conn).ingest_event(
                org_id=event.org_id,
                provider_name=provider_name,
                external_event_id=event.event_id,
                event=event.model_dump(mode="json"),
            )
    response.status_code = status.HTTP_200_OK if result.status == SyncStatus.SUCCESS else status.HTTP_202_ACCEPTED
    return {"result": result}
