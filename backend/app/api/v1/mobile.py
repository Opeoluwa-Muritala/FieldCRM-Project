import json
import secrets
from datetime import date, datetime, timedelta, timezone
from decimal import Decimal
from typing import Any, Literal
from uuid import UUID

from fastapi import APIRouter, Depends, File, Form, HTTPException, Path, Query, UploadFile, status, Request
from pydantic import BaseModel, Field

from app.core.audit import AuditService
from app.core.database import db_conn
from app.core.dependencies import get_current_user
from app.core.workflow import NEXT_STAGE, ROLE_LABELS, STAGE_ROLE, WORKFLOW_STAGES
from app.domains.documents.repository import DocumentRepository
from app.domains.documents.service import DocumentService
from app.domains.documents.direct_upload import DirectDocumentUploadService
from app.domains.documents.schemas import DirectUploadAuthorizationRequest, DirectUploadFinalizeRequest
from app.domains.ocr.repository import OcrRepository
from app.domains.guarantors.repository import GuarantorRepository
from app.domains.guarantors.service import GuarantorService
from app.domains.loans.repository import LoanRepository
from app.domains.loans.service import LoanService
from app.domains.notifications.repository import NotificationRepository
from app.domains.notifications.service import NotificationService
from app.domains.visitation.repository import VisitationRepository
from app.domains.visitation.service import VisitationService
from app.domains.signing.repository import SigningRepository
from app.domains.branches.repository import BranchRepository
from app.domains.users.repository import UserRepository
from app.domains.users.service import UserService
from app.services.dashboard_service import DashboardService
from app.services.email_service import EmailService
from app.core.rate_limit import enforce_reset_limits
from app.core.cache import cache_response, get_json, set_json
from app.config import settings


router = APIRouter()


class MobileUserResponse(BaseModel):
    id: UUID
    org_id: UUID
    full_name: str
    email: str
    role: str
    display_role: str


class CreateApplicationRequest(BaseModel):
    customer_type: Literal["new", "existing"]
    loan_type: Literal["enterprise", "msef", "payee", "other"]
    applicant_name: str = "New Applicant"
    borrower_id: str | None = None
    amount: float | None = None
    tenure: int | None = None
    product_type: str | None = None
    client_request_id: UUID | None = None


PROFILE_FIELDS = (
    "applicant_name", "full_name", "phone", "alternative_phone", "email",
    "date_of_birth", "gender", "marital_status", "bvn", "nin",
    "residential_address", "state", "lga", "locality", "customer_reference",
    "account_reference", "employment_status", "employer_name",
)


def _mask_identifier(value: Any, *, visible_end: int = 2) -> str | None:
    text = str(value or "").strip()
    if not text:
        return None
    if len(text) <= visible_end * 2:
        return "*" * len(text)
    return f"{text[:visible_end]}{'*' * (len(text) - visible_end * 2)}{text[-visible_end:]}"


def _personal_profile(app: Any, intake: dict[str, Any] | None = None) -> dict[str, Any]:
    source = dict(intake or {})
    profile = {key: source.get(key) for key in PROFILE_FIELDS if source.get(key) not in (None, "")}
    def app_value(key: str, default=None):
        if isinstance(app, dict) or hasattr(app, "keys"):
            return app[key] if key in app.keys() else default
        return getattr(app, key, default)
    profile["applicant_name"] = profile.get("applicant_name") or profile.get("full_name") or app_value("applicant_name", "")
    for key in ("phone", "bvn"):
        if not profile.get(key) and app_value(key):
            profile[key] = app_value(key)
    profile["customer_reference"] = profile.get("customer_reference") or app_value("ref_no")
    return profile


async def _customer_record(conn, borrower_id: UUID, org_id: UUID):
    app = await LoanRepository(conn).get_by_id(borrower_id, org_id)
    if app:
        intake = await _loan_service(conn).get_wizard_data(borrower_id)
        return app, _personal_profile(app, intake)
    exists = await conn.fetchval(
        "SELECT EXISTS(SELECT 1 FROM loan_applications WHERE id=$1 AND deleted_at IS NULL)",
        borrower_id,
    )
    if exists:
        raise HTTPException(status_code=403, detail="Customer does not belong to your organization")
    raise HTTPException(status_code=404, detail="Customer not found")


class MobileBorrowerRequest(BaseModel):
    name: str
    phone: str = ""
    bvn: str = ""
    nin: str = ""
    gps_coordinates: str | None = None
    physical_address: str | None = None
    employment_status: str | None = None
    employer_name: str | None = None
    monthly_income: float | None = None
    bank_name: str | None = None
    account_number: str | None = None
    guarantor_name: str | None = None
    guarantor_phone: str | None = None


class SaveStepRequest(BaseModel):
    data: dict[str, Any] = Field(default_factory=dict)


class SaveGuarantorStepRequest(BaseModel):
    data: dict[str, Any] = Field(default_factory=dict)


class OcrReviewRequest(BaseModel):
    action: Literal["save", "verify"] = "save"
    corrections: dict[str, Any] = Field(default_factory=dict)


class VisitationReportRequest(BaseModel):
    met_with: str | None = None
    premises_description: str | None = None
    direction_from_branch: str | None = None


class VisitationSignoffRequest(BaseModel):
    decision: Literal["concurred", "returned"]
    notes: str = ""


class CreditReviewRequest(BaseModel):
    recommendation_decision: Literal[
        "Recommend Approval",
        "Recommend Rejection",
        "Return for Correction",
    ]
    recommendation_notes: str


class ReturnApplicationRequest(BaseModel):
    reason_category: str
    corrections: list[str] = Field(default_factory=list)
    notes: str


class AuditChecklistRequest(BaseModel):
    consent_verified: bool = False
    signature_matched: bool = False
    exhibits_verified: bool = False


class CreditChecklistItemRequest(BaseModel):
    item_key: str
    item_label: str = ""
    is_checked: bool
    context: str = "credit"


class ChangePasswordRequest(BaseModel):
    current_password: str
    new_password: str = Field(min_length=8)
    confirm_password: str


class DisbursementRequest(BaseModel):
    disbursed_amount: float = Field(gt=0)
    disbursement_method: Literal["bank_transfer", "cheque", "cash", "direct_debit"]
    disbursed_bank_ref: str | None = None
    payment_date: date
    interest_rate: float = Field(gt=0)
    repayment_frequency: str
    schedule_method: Literal["flat_rate", "reducing_balance"] = "flat_rate"


class ValuationItemRequest(BaseModel):
    item_id: UUID
    appraised_value: float = Field(ge=0)
    valuer_name: str | None = None
    valuer_license_no: str | None = None
    valuation_date: date | None = None


class ValuationRequest(BaseModel):
    items: list[ValuationItemRequest]


class MccVoteRequest(BaseModel):
    recommended_amount: float = Field(gt=0)
    notes: str = ""


class MccFinalizeRequest(BaseModel):
    final_amount: float = Field(gt=0)


class ReviewApprovalRequest(BaseModel):
    notes: str = Field(min_length=1)
    kyc_attested: bool
    collateral_attested: bool


class InterestPresetRequest(BaseModel):
    loan_type: str = Field(min_length=1, max_length=50)
    rate: float = Field(gt=0)
    rate_type: str = Field(min_length=1, max_length=30)


class BranchRequest(BaseModel):
    name: str = Field(min_length=2, max_length=100)
    code: str = Field(min_length=1, max_length=20)


class UserRoleRequest(BaseModel):
    role: str
    branch_id: UUID | None = None


def _role(user) -> str:
    return user.role.lower().replace(" ", "_")


def _mobile_role(user) -> str:
    role = _role(user)
    role_map = {
        "admin":     "system_admin",
    }
    return role_map.get(role, role)


def _stage_number(stage: str | None) -> int:
    return {
        "intake": 1,
        "branch_manager_review": 2,
        "branch_supervisor_review": 3,
        "credit_analyst_review": 4,
        "crm_review": 5,
        "head_crm_review": 6,
        "ed_approval": 7,
        "md_approval": 8,
        "executive_approval": 7,
        "disbursement_ready": 9,
        "disbursed": 10,
        "returned": 11,
        "rejected": 12,
        # Historical aliases retained for records created by the retired flow.
        "ocr_review": 1,
        "credit_review": 4,
        "branch_approval": 2,
        "committee_review": 7,
    }.get(stage or "intake", 1)


def _stage_status(stage: str | None) -> str:
    return {
        "intake": "Relationship Officer Intake",
        "branch_manager_review": "Team Lead Review",
        "branch_supervisor_review": "Supervisor Review",
        "credit_analyst_review": "Credit Analysis",
        "crm_review": "CRM Dossier Review",
        "head_crm_review": "Head CRM Approval",
        "ed_approval": "Executive Director Approval",
        "md_approval": "Managing Director Input",
        "executive_approval": "Executive Director Approval",
        "disbursement_ready": "CRM Disbursement",
        "disbursed": "Disbursed",
        "returned": "Returned",
        "rejected": "Rejected",
        "ocr_review": "Legacy OCR Review",
        "credit_review": "Legacy Credit Review",
        "branch_approval": "Legacy Branch Approval",
        "committee_review": "Legacy Committee Review",
    }.get(stage or "intake", "Relationship Officer Intake")


def _mobile_application(app: Any, current_user) -> dict[str, Any]:
    app_id = str(app.id)
    return {
        "id": app_id,
        "org_id": str(getattr(app, "org_id", current_user.org_id)),
        "borrower_id": str(getattr(app, "borrower_id", app_id)),
        "current_stage": getattr(app, "current_stage", _stage_number(getattr(app, "stage", None))),
        "current_owner_id": str(getattr(app, "current_owner_id", "") or getattr(app, "created_by", current_user.id)),
        "status": getattr(app, "status", _stage_status(getattr(app, "stage", None))),
        "amount": float(getattr(app, "amount", 0) or 0),
        "tenure": int(getattr(app, "tenure", getattr(app, "tenor_months", 0)) or 0),
        "product_type": getattr(app, "product_type", getattr(app, "loan_type", "other")),
        "interest_rate": getattr(app, "interest_rate", 15.0),
        "repayment_frequency": getattr(app, "repayment_frequency", getattr(app, "repayment_mode", "Monthly") or "Monthly"),
        "collateral_desc": getattr(app, "collateral_desc", getattr(app, "purpose", None)),
        "collateral_value": getattr(app, "collateral_value", 0.0),
        "officer_recommendation": getattr(app, "officer_recommendation", ""),
        "applicant_name": getattr(app, "applicant_name", "Applicant"),
        "stage": getattr(app, "stage", None),
        "created_at": getattr(app, "created_at", ""),
    }


def _mobile_borrower(app: Any, current_user, nin: str = "") -> dict[str, Any]:
    app_id = str(app.id)
    return {
        "id": app_id,
        "org_id": str(getattr(app, "org_id", current_user.org_id)),
        "loan_officer_id": str(getattr(app, "created_by", current_user.id) or current_user.id),
        "name": getattr(app, "applicant_name", "Applicant"),
        "phone": getattr(app, "phone", "") or "",
        "bvn": getattr(app, "bvn", "") or "",
        "nin": nin,
        "photo_url": None,
        "status": "ACTIVE" if getattr(app, "stage", None) != "rejected" else "INACTIVE",
        "gps_coordinates": None,
        "physical_address": None,
        "employment_status": None,
        "employer_name": None,
        "monthly_income": None,
        "bank_name": None,
        "account_number": None,
        "guarantor_name": None,
        "guarantor_phone": None,
        "created_at": getattr(app, "created_at", ""),
    }


def _mobile_dashboard_metrics(data: dict[str, Any]) -> dict[str, Any]:
    metrics = data.get("metrics", {}) if data else {}
    return {
        "apps_today": metrics.get("my_applications", metrics.get("total_applications", 0)),
        "pending_sync": metrics.get("pending_upload", 0),
        "visits_due": metrics.get("visits_due", 0),
        "missing_docs": metrics.get("returned", metrics.get("returned_count", 0)),
        "branch_disbursed": float(metrics.get("ready_amount", 0) or 0),
        "target_met_pct": int(metrics.get("target_met_pct", 0) or 0),
        "awaiting_signoff": metrics.get("pending_signoffs", metrics.get("awaiting_concurrence", 0)),
        "active_agents": metrics.get("active_assigned", 0),
        "underwriting_queue": metrics.get("underwriting_queue", metrics.get("credit_review_count", 0)),
        "avg_turnaround_mins": metrics.get("avg_turnaround_mins", 0),
        "high_risk_cases": metrics.get("high_risk_cases", 0),
        "approved_today": metrics.get("approved_today", 0),
        "flags_raised": metrics.get("flags_raised", 0),
        "policy_breaches": metrics.get("policy_breaches", 0),
        "audited_today": metrics.get("audited_today", 0),
        "board_tickets": metrics.get("board_tickets", 0),
        "alert_escalations": metrics.get("alert_escalations", 0),
        "decisions_signed": metrics.get("decisions_signed", metrics.get("approved_today", 0)),
    }


def _stage_from_query(stage: str | None) -> str | None:
    if not stage or stage == "all":
        return None
    stage_map = {
        "1": "intake",
        "2": "ocr_review",
        "3": "credit_review",
        "4": "branch_approval",
        "5": "disbursement_ready",
        "6": "disbursed",
        "7": "returned",
        "8": "rejected",
    }
    return stage_map.get(stage, stage)


async def _get_application_or_404(
    conn, application_id: UUID, current_user, *, enforce_officer_scope: bool = True
):
    repo = LoanRepository(conn)
    app = await repo.get_by_id(application_id, current_user.org_id)
    if not app:
        raise HTTPException(status_code=404, detail="Loan Application not found")
    if enforce_officer_scope and _role(current_user) == "system_admin":
        raise HTTPException(status_code=403, detail="System Admin does not have access to loan dossiers")
    if (
        enforce_officer_scope
        and _role(current_user) in {"account_officer", "loan_officer"}
        and str(app.created_by) != str(current_user.id)
    ):
        raise HTTPException(status_code=403, detail="You do not have permission to access this application")
    if (
        enforce_officer_scope
        and _role(current_user) == "branch_manager"
        and getattr(app, "branch_id", None) is not None
        and getattr(current_user, "branch_id", None) is not None
        and str(app.branch_id) != str(current_user.branch_id)
    ):
        raise HTTPException(status_code=403, detail="You do not have permission to access applications outside your branch")
    return app


def _ensure_intake_writer(app, current_user) -> None:
    user_role = _role(current_user)
    if user_role not in ("system_admin", "account_officer", "loan_officer"):
        raise HTTPException(status_code=403, detail="Insufficient permissions for this action")
    if user_role in {"account_officer", "loan_officer"} and app.created_by != current_user.id:
        raise HTTPException(status_code=403, detail="You do not have permission to modify this application")


def _ensure_roles(current_user, allowed_roles: set[str]) -> None:
    if _role(current_user) not in allowed_roles:
        raise HTTPException(status_code=403, detail="Insufficient permissions for this action")


def _loan_service(conn) -> LoanService:
    return LoanService(LoanRepository(conn), AuditService(conn))


def _guarantor_service(conn) -> GuarantorService:
    return GuarantorService(GuarantorRepository(conn), LoanRepository(conn), AuditService(conn))


def _document_service(conn) -> DocumentService:
    return DocumentService(DocumentRepository(conn), AuditService(conn))


def _mobile_document(document: dict) -> dict:
    """Never expose a Cloudinary delivery URL directly to mobile clients."""
    payload = dict(document)
    if payload.get("cloud_public_id"):
        payload["preview_url"] = f"/api/v1/documents/{payload['id']}/preview"
        payload["download_url"] = f"/api/v1/documents/{payload['id']}/download"
        payload.pop("stored_path", None)
        payload.pop("cloud_preview_url", None)
    return payload


def _visitation_service(conn) -> VisitationService:
    return VisitationService(VisitationRepository(conn), AuditService(conn))


def _notification_service(conn) -> NotificationService:
    return NotificationService(NotificationRepository(conn))


@router.get("/me", response_model=MobileUserResponse)
async def get_mobile_user(current_user=Depends(get_current_user)):
    return {
        "id": current_user.id,
        "org_id": current_user.org_id,
        "full_name": current_user.full_name,
        "email": current_user.email,
        "role": _mobile_role(current_user),
        "display_role": current_user.display_role,
    }


@router.get("/dashboard")
@cache_response(ttl_seconds=30)
async def get_mobile_dashboard(conn=Depends(db_conn), current_user=Depends(get_current_user)):
    data = await DashboardService(conn).get_dashboard_data(current_user)
    metrics = _mobile_dashboard_metrics(data)
    return {
        **metrics,
        "user": {
            "id": current_user.id,
            "full_name": current_user.full_name,
            "role": _mobile_role(current_user),
            "display_role": current_user.display_role,
        },
        "data": data,
    }


@router.get("/notifications")
@cache_response(ttl_seconds=15, notification_scoped=True)
async def list_mobile_notifications(conn=Depends(db_conn), current_user=Depends(get_current_user)):
    return await _notification_service(conn).list_for_user(
        user_id=current_user.id,
        org_id=current_user.org_id,
    )


@router.patch("/notifications/{notification_id}/read")
async def mark_mobile_notification_read(
    notification_id: str,
    conn=Depends(db_conn),
    current_user=Depends(get_current_user),
):
    await _notification_service(conn).mark_read_for_user(
        notification_id=notification_id,
        user_id=current_user.id,
        org_id=current_user.org_id,
    )
    return {"ok": True}


@router.delete("/notifications")
async def clear_mobile_notifications(conn=Depends(db_conn), current_user=Depends(get_current_user)):
    await _notification_service(conn).clear_for_user(
        user_id=current_user.id,
        org_id=current_user.org_id,
    )
    return {"ok": True}


@router.get("/queues/legal")
async def get_mobile_legal_queue_exact(
    page: int = Query(1, ge=1),
    size: int = Query(50, ge=1, le=100),
    conn=Depends(db_conn),
    current_user=Depends(get_current_user),
):
    _ensure_roles(current_user, {"legal"})
    rows = await conn.fetch(
        """
        SELECT id, ref_no, applicant_name, amount, stage, updated_at,
               COUNT(*) OVER() AS total_count
        FROM loan_applications
        WHERE org_id=$1
          AND stage IN ('branch_manager_review','credit_analyst_review','crm_review')
          AND deleted_at IS NULL
        ORDER BY updated_at ASC
        LIMIT $2 OFFSET $3
        """,
        current_user.org_id, size, (page - 1) * size,
    )
    total = int(rows[0]["total_count"]) if rows else 0
    return {
        "items": [{k: v for k, v in dict(row).items() if k != "total_count"} for row in rows],
        "page": page, "size": size, "total": total,
    }


@router.get("/queues/{queue_name}")
@cache_response(ttl_seconds=30)
async def get_mobile_queue(
    queue_name: Literal[
        "loan-officer",
        "visits-due",
        "awaiting-concurrence",
        "pending-signoffs",
        "branch-supervisor-review",
        "credit-analyst-review",
        "head-crm-review",
        "credit-reviews",
        "ocr-exceptions",
        "compliance-flags",
        "system-control",
        "crm-review",
        "ed-approval",
        "md-approval",
        "executive-approval",
        "legal",
    ],
    stage: str | None = None,
    limit: int = Query(50, ge=1, le=100),
    offset: int = Query(0, ge=0),
    conn=Depends(db_conn),
    current_user=Depends(get_current_user),
):
    dashboard = DashboardService(conn)
    if queue_name == "loan-officer":
        _ensure_roles(current_user, {"account_officer"})
        items = await dashboard.get_loan_officer_queue(current_user, stage=_stage_from_query(stage), limit=limit, offset=offset)
    elif queue_name == "visits-due":
        _ensure_roles(current_user, {"account_officer"})
        items = await dashboard.get_visits_due_today(current_user)
    elif queue_name == "awaiting-concurrence":
        _ensure_roles(current_user, {"branch_manager"})
        items = await dashboard.get_awaiting_concurrence(current_user, limit=limit, offset=offset)
    elif queue_name == "pending-signoffs":
        _ensure_roles(current_user, {"branch_manager"})
        items = await dashboard.get_pending_signoffs(current_user, limit=limit, offset=offset)
    elif queue_name == "branch-supervisor-review":
        _ensure_roles(current_user, {"branch_supervisor"})
        items = await dashboard.get_supervisory_review_queue(current_user, limit=limit, offset=offset)
    elif queue_name == "credit-analyst-review":
        _ensure_roles(current_user, {"credit_analyst"})
        items = await dashboard.get_credit_reviews(current_user, limit=limit, offset=offset)
    elif queue_name == "head-crm-review":
        _ensure_roles(current_user, {"head_crm"})
        items = await dashboard.get_crm_queue(current_user, limit=limit, offset=offset)
    elif queue_name == "credit-reviews":
        _ensure_roles(current_user, {"branch_manager"})
        items = await dashboard.get_credit_reviews(current_user, limit=limit, offset=offset)
    elif queue_name == "ocr-exceptions":
        _ensure_roles(current_user, {"branch_manager"})
        items = await dashboard.get_credit_ocr_exceptions(current_user, limit=limit, offset=offset)
    elif queue_name == "compliance-flags":
        _ensure_roles(current_user, {"auditor"})
        items = await dashboard.get_compliance_flags(current_user, limit=limit, offset=offset)
    elif queue_name == "crm-review":
        _ensure_roles(current_user, {"crm", "head_crm"})
        items = await dashboard.get_crm_queue(current_user)
    elif queue_name == "ed-approval":
        _ensure_roles(current_user, {"ed"})
        items = await dashboard.get_ed_queue(current_user)
    elif queue_name == "md-approval":
        _ensure_roles(current_user, {"md"})
        items = await dashboard.get_md_queue(current_user)
    elif queue_name == "executive-approval":
        _ensure_roles(current_user, {"md", "ed"})
        items = await dashboard.get_executive_queue(current_user)
    elif queue_name == "legal":
        _ensure_roles(current_user, {"legal"})
        items = await conn.fetch(
            """
            SELECT id, ref_no, applicant_name, amount, stage, updated_at
            FROM loan_applications
            WHERE org_id=$1
              AND stage IN ('branch_manager_review','credit_analyst_review','crm_review')
              AND deleted_at IS NULL
            ORDER BY updated_at ASC
            LIMIT $2 OFFSET $3
            """,
            current_user.org_id, limit, offset,
        )
        items = [dict(item) for item in items]
    else:
        _ensure_roles(current_user, {"system_admin"})
        items = await dashboard.get_system_control_queue(current_user, limit=limit, offset=offset)

    return {"queue": queue_name, "items": items}


@router.get("/borrowers")
@cache_response(ttl_seconds=30)
async def list_mobile_borrowers(
    page: int = Query(1, ge=1),
    size: int = Query(20, ge=1, le=100),
    conn=Depends(db_conn),
    current_user=Depends(get_current_user),
):
    if _role(current_user) == "system_admin":
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="System Admin does not have access to borrower records",
        )
    repo = LoanRepository(conn)
    officer_id = current_user.id if _role(current_user) == "account_officer" else None
    applications, total = await repo.list_by_stage(
        org_id=current_user.org_id,
        stage=None,
        officer_id=officer_id,
        page=page,
        size=size,
        branch_id=getattr(current_user, "branch_id", None),
    )
    items = [_mobile_borrower(app, current_user) for app in applications]
    return {
        "items": items,
        "total": total,
        "page": page,
        "size": size,
    }


@router.get("/borrowers/search")
async def search_mobile_borrowers(
    q: str = Query(..., max_length=100),
    limit: int = Query(20, ge=1, le=50),
    conn=Depends(db_conn),
    current_user=Depends(get_current_user),
):
    _ensure_roles(current_user, {"account_officer"})
    query = q.strip()
    structured = query.isdigit() and len(query) >= 7
    if len(query) < 3 and not structured:
        raise HTTPException(status_code=422, detail="Enter at least 3 characters")
    pattern = f"%{query}%"
    rows = await conn.fetch(
        """
        SELECT DISTINCT ON (lower(a.applicant_name), coalesce(a.phone,''), coalesce(a.bvn,''))
               a.id, a.ref_no, a.applicant_name, a.phone, a.bvn, a.stage,
               u.full_name AS relationship_owner, b.name AS branch_name,
               coalesce(sd.data_json, '{}'::jsonb) AS intake
        FROM loan_applications a
        LEFT JOIN users u ON u.id=a.created_by AND u.org_id=a.org_id
        LEFT JOIN branches b ON b.id=u.branch_id AND b.org_id=a.org_id
        LEFT JOIN LATERAL (
            SELECT data_json FROM stage_data
            WHERE loan_id=a.id AND stage='intake' ORDER BY saved_at DESC LIMIT 1
        ) sd ON true
        WHERE a.org_id=$1 AND a.deleted_at IS NULL
          AND (a.applicant_name ILIKE $2 OR coalesce(a.phone,'') ILIKE $2
               OR coalesce(a.bvn,'') ILIKE $2 OR a.ref_no ILIKE $2
               OR coalesce(sd.data_json->>'nin','') ILIKE $2
               OR coalesce(sd.data_json->>'customer_reference','') ILIKE $2
               OR coalesce(sd.data_json->>'account_reference','') ILIKE $2)
        ORDER BY lower(a.applicant_name), coalesce(a.phone,''), coalesce(a.bvn,''), a.updated_at DESC
        LIMIT $3
        """,
        current_user.org_id, pattern, limit,
    )
    items = []
    for row in rows:
        profile = _personal_profile(row, dict(row["intake"] or {}))
        items.append({
            "id": str(row["id"]), "legal_name": profile["applicant_name"],
            "customer_reference": _mask_identifier(profile.get("customer_reference")),
            "masked_bvn": _mask_identifier(profile.get("bvn")),
            "masked_nin": _mask_identifier(profile.get("nin")),
            "masked_phone": _mask_identifier(profile.get("phone"), visible_end=3),
            "branch": row["branch_name"], "relationship_owner": row["relationship_owner"],
            "active": row["stage"] != "rejected",
        })
    return {"items": items, "query": query}


@router.get("/borrowers/{borrower_id}/application-profile")
async def get_mobile_borrower_application_profile(
    borrower_id: UUID,
    conn=Depends(db_conn),
    current_user=Depends(get_current_user),
):
    _ensure_roles(current_user, {"account_officer"})
    app, profile = await _customer_record(conn, borrower_id, current_user.org_id)
    return {"borrower": {"id": str(app.id), "legal_name": app.applicant_name}, "personal_profile": profile}


@router.post("/borrowers", status_code=status.HTTP_201_CREATED)
async def create_mobile_borrower(
    payload: MobileBorrowerRequest,
    conn=Depends(db_conn),
    current_user=Depends(get_current_user),
):
    _ensure_roles(current_user, {"account_officer", "loan_officer"})
    app = await _loan_service(conn).create_loan(
        org_id=current_user.org_id,
        customer_type="new",
        loan_type="other",
        applicant_name=payload.name,
        user_id=current_user.id,
    )
    updated = await LoanRepository(conn).update_intake_details(
        loan_id=app.id,
        org_id=current_user.org_id,
        applicant_name=payload.name,
        phone=payload.phone,
        bvn=payload.bvn,
        amount=None,
        tenor_months=None,
    )
    borrower = updated or app
    return {"borrower": _mobile_borrower(borrower, current_user, nin=payload.nin)}


@router.get("/applications")
@cache_response(ttl_seconds=30)
async def list_mobile_applications(
    stage: str | None = None,
    page: int = Query(1, ge=1),
    size: int = Query(50, ge=1, le=100),
    conn=Depends(db_conn),
    current_user=Depends(get_current_user),
):
    if _role(current_user) == "system_admin":
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="System Admin does not have access to loan applications",
        )
    repo = LoanRepository(conn)
    officer_id = current_user.id if _role(current_user) in {"account_officer", "loan_officer"} else None
    branch_id = current_user.branch_id if _role(current_user) == "branch_manager" else None
    applications, total = await repo.list_by_stage(
        org_id=current_user.org_id,
        stage=_stage_from_query(stage),
        officer_id=officer_id,
        branch_id=branch_id,
        page=page,
        size=size,
    )
    items = [_mobile_application(app, current_user) for app in applications]
    return {
        "items": items,
        "total": total,
        "page": page,
        "size": size,
    }


@router.post("/applications", status_code=status.HTTP_201_CREATED)
async def create_mobile_application(
    payload: CreateApplicationRequest,
    conn=Depends(db_conn),
    current_user=Depends(get_current_user),
):
    _ensure_roles(current_user, {"account_officer"})
    if payload.client_request_id:
        existing = await conn.fetchrow(
            """
            SELECT * FROM loan_applications
            WHERE org_id = $1 AND client_request_id = $2 AND deleted_at IS NULL
            """,
            current_user.org_id,
            payload.client_request_id,
        )
        if existing:
            existing_app = await LoanRepository(conn).get_by_id(
                existing["id"], current_user.org_id
            )
            saved_snapshot = await _loan_service(conn).get_wizard_data(existing["id"])
            linked_id = saved_snapshot.get("borrower_id")
            return {
                "application": _mobile_application(existing_app, current_user),
                "borrower": ({"id": linked_id, "legal_name": existing_app.applicant_name} if linked_id else None),
                "personal_profile_snapshot": {
                    key: saved_snapshot[key] for key in PROFILE_FIELDS if saved_snapshot.get(key) not in (None, "")
                } or None,
                "next": {"type": "intake_step", "step": 1},
                "replayed": True,
            }
    if payload.customer_type == "existing" and not payload.borrower_id:
        raise HTTPException(status_code=422, detail="borrower_id is required for an existing customer")
    if payload.customer_type == "new" and payload.borrower_id:
        raise HTTPException(status_code=422, detail="borrower_id must be empty for a new customer")
    applicant_name = payload.applicant_name
    borrower_app = None
    personal_profile = None
    if payload.borrower_id:
        try:
            selected_id = UUID(payload.borrower_id)
        except ValueError as exc:
            raise HTTPException(status_code=422, detail="Invalid borrower_id") from exc
        borrower_app, personal_profile = await _customer_record(conn, selected_id, current_user.org_id)
        applicant_name = personal_profile["applicant_name"]
    app = await _loan_service(conn).create_loan(
        org_id=current_user.org_id,
        customer_type=payload.customer_type,
        loan_type=payload.loan_type,
        applicant_name=applicant_name,
        user_id=current_user.id,
    )
    if payload.client_request_id:
        updated_idempotent = await conn.fetchrow(
            """
            UPDATE loan_applications
            SET client_request_id = $1
            WHERE id = $2 AND org_id = $3
            RETURNING *
            """,
            payload.client_request_id,
            app.id,
            current_user.org_id,
        )
        if updated_idempotent:
            app = await LoanRepository(conn).get_by_id(app.id, current_user.org_id) or app
    if personal_profile is not None:
        snapshot = dict(personal_profile)
        snapshot.update({
            "borrower_id": str(borrower_app.id),
            "profile_snapshot_source": "customer_profile",
            "profile_snapshot_created_at": datetime.now(timezone.utc).isoformat(),
        })
        await _loan_service(conn).save_wizard_step(
            app.id, 1, snapshot, current_user.id, current_user.org_id
        )
        app = await LoanRepository(conn).get_by_id(app.id, current_user.org_id) or app
    return {
        "application": _mobile_application(app, current_user),
        "borrower": ({"id": str(borrower_app.id), "legal_name": borrower_app.applicant_name} if borrower_app else None),
        "personal_profile_snapshot": personal_profile,
        "next": {"type": "intake_step", "step": 1},
        "replayed": False,
    }


@router.get("/applications/{application_id}")
async def get_mobile_application(
    application_id: UUID,
    conn=Depends(db_conn),
    current_user=Depends(get_current_user),
):
    app = await _get_application_or_404(conn, application_id, current_user)
    loan_service = _loan_service(conn)
    documents = await DocumentRepository(conn).get_by_loan(application_id, current_user.org_id)
    visitation = await VisitationRepository(conn).get_by_loan(loan_id=application_id, org_id=current_user.org_id) or {}
    readiness = await LoanRepository(conn).get_readiness_summary(application_id, current_user.org_id)
    workflow_events = await LoanRepository(conn).list_workflow_events_for_application(
        current_user.org_id, application_id
    )
    return {
        "application": app,
        "intake": await loan_service.get_wizard_data(application_id),
        "documents": documents,
        "visitation": visitation,
        "readiness": readiness,
        "workflow_events": [dict(event) for event in workflow_events],
    }


@router.get("/applications/{application_id}/intake")
@cache_response(ttl_seconds=60, application_scoped=True)
async def get_mobile_intake(
    application_id: UUID,
    conn=Depends(db_conn),
    current_user=Depends(get_current_user),
):
    await _get_application_or_404(conn, application_id, current_user)
    return {"data": await _loan_service(conn).get_wizard_data(application_id)}


@router.put("/applications/{application_id}/intake/steps/{step}")
async def save_mobile_intake_step(
    application_id: UUID,
    step: int = Path(..., ge=1, le=8),
    payload: SaveStepRequest = None,
    conn=Depends(db_conn),
    current_user=Depends(get_current_user),
):
    app = await _get_application_or_404(conn, application_id, current_user)
    _ensure_intake_writer(app, current_user)
    await _loan_service(conn).save_wizard_step(
        application_id,
        step,
        (payload or SaveStepRequest()).data,
        current_user.id,
        current_user.org_id,
    )
    updated = await _loan_service(conn).get_wizard_data(application_id)
    next_step = step + 1 if step < 8 else None
    return {
        "application_id": application_id,
        "stage": "branch_manager_review" if step == 8 else app.stage,
        "step": step,
        "next_step": next_step,
        "data": updated,
    }


@router.get("/applications/{application_id}/guarantors/{slot}")
@cache_response(ttl_seconds=60, application_scoped=True)
async def get_mobile_guarantor(
    application_id: UUID,
    slot: int = Path(..., ge=1, le=2),
    conn=Depends(db_conn),
    current_user=Depends(get_current_user),
):
    await _get_application_or_404(conn, application_id, current_user)
    return {"slot": slot, "data": await _guarantor_service(conn).get_wizard_data(application_id, slot)}


@router.put("/applications/{application_id}/guarantors/{slot}/steps/{step}")
async def save_mobile_guarantor_step(
    application_id: UUID,
    slot: int = Path(..., ge=1, le=2),
    step: int = Path(..., ge=1, le=8),
    payload: SaveGuarantorStepRequest = None,
    conn=Depends(db_conn),
    current_user=Depends(get_current_user),
):
    app = await _get_application_or_404(conn, application_id, current_user)
    _ensure_intake_writer(app, current_user)
    service = _guarantor_service(conn)
    await service.save_wizard_step(application_id, slot, step, (payload or SaveGuarantorStepRequest()).data, current_user.id)
    if step == 8:
        guarantor = await service.mark_slot_submitted(
            loan_id=application_id,
            org_id=current_user.org_id,
            slot=slot,
            submitted_by=current_user.id,
            user_role=current_user.role,
        )
    else:
        guarantor = None
    return {
        "slot": slot,
        "step": step,
        "next_step": step + 1 if step < 8 else None,
        "submitted": step == 8,
        "guarantor": guarantor,
        "data": await service.get_wizard_data(application_id, slot),
    }


@router.post("/applications/{application_id}/documents")
async def upload_mobile_document(
    application_id: UUID,
    file: UploadFile = File(...),
    doc_type: str = Form("other"),
    form_code: str | None = Form(None),
    category: str | None = Form(None),
    conn=Depends(db_conn),
    current_user=Depends(get_current_user),
):
    await _get_application_or_404(conn, application_id, current_user)
    document = await _document_service(conn).save_upload(
        loan_id=application_id,
        org_id=current_user.org_id,
        doc_type=doc_type or category or "other",
        form_code=form_code,
        file=file,
        uploaded_by=current_user.id,
        user_role=current_user.role,
    )
    return {"document": _mobile_document(document)}


@router.post("/applications/{application_id}/documents/upload-authorizations")
async def authorize_mobile_document_upload(
    application_id: UUID,
    payload: DirectUploadAuthorizationRequest,
    conn=Depends(db_conn),
    current_user=Depends(get_current_user),
):
    await _get_application_or_404(conn, application_id, current_user)
    crm_categories = {
        "offer_acceptance", "disbursement_mandate", "direct_debit_mandate",
        "insurance_certificate", "legal_clearance", "other_crm",
    }
    if payload.doc_type in crm_categories:
        _ensure_roles(current_user, {"crm", "head_crm"})
    authorization = await DirectDocumentUploadService(conn).authorize(
        application_id=application_id,
        org_id=current_user.org_id,
        actor_id=current_user.id,
        actor_role=current_user.role,
        doc_type=payload.doc_type,
        form_code=payload.form_code,
        original_name=payload.filename,
        mime_type=payload.mime_type,
        size_bytes=payload.size_bytes,
    )
    return {"authorization": authorization}


@router.post("/applications/{application_id}/documents/finalize")
async def finalize_mobile_document_upload(
    application_id: UUID,
    payload: DirectUploadFinalizeRequest,
    conn=Depends(db_conn),
    current_user=Depends(get_current_user),
):
    await _get_application_or_404(conn, application_id, current_user)
    document = await DirectDocumentUploadService(conn).finalize(
        intent_id=payload.intent_id,
        application_id=application_id,
        org_id=current_user.org_id,
        actor_id=current_user.id,
        public_id=payload.public_id,
        version=payload.version,
        signature=payload.signature,
    )
    return {"document": _mobile_document(document)}


@router.get("/applications/{application_id}/documents")
async def list_mobile_documents(
    application_id: UUID,
    conn=Depends(db_conn),
    current_user=Depends(get_current_user),
):
    await _get_application_or_404(conn, application_id, current_user)
    documents = await DocumentRepository(conn).get_by_loan(application_id, current_user.org_id)
    return {"items": [_mobile_document(document) for document in documents]}


@router.post("/applications/{application_id}/crm-documents")
async def upload_mobile_crm_document(
    application_id: UUID,
    file: UploadFile = File(...),
    category: Literal[
        "offer_acceptance", "disbursement_mandate", "direct_debit_mandate",
        "insurance_certificate", "legal_clearance", "other_crm"
    ] = Form(...),
    conn=Depends(db_conn),
    current_user=Depends(get_current_user),
):
    await _get_application_or_404(conn, application_id, current_user)
    _ensure_roles(current_user, {"crm", "head_crm"})
    document = await _document_service(conn).save_upload(
        loan_id=application_id, org_id=current_user.org_id,
        doc_type=category, form_code=None, file=file,
        uploaded_by=current_user.id, user_role=current_user.role,
    )
    return {"document": _mobile_document(document)}


@router.get("/applications/{application_id}/ocr-fields")
async def list_mobile_ocr_fields(
    application_id: UUID,
    conn=Depends(db_conn),
    current_user=Depends(get_current_user),
):
    """Return persisted per-field OCR confidence for an authorised dossier."""
    await _get_application_or_404(conn, application_id, current_user)
    fields = await OcrRepository(conn).list_fields_for_loan(loan_id=application_id)
    documents = await DocumentRepository(conn).get_by_loan(application_id, current_user.org_id)
    return {
        "items": fields,
        "processing": any(document.get("ocr_status") in {"pending", "processing"} for document in documents),
    }


@router.post("/applications/{application_id}/ocr-review")
async def submit_mobile_ocr_review(
    application_id: UUID,
    payload: OcrReviewRequest,
    conn=Depends(db_conn),
    current_user=Depends(get_current_user),
):
    app = await _get_application_or_404(conn, application_id, current_user)
    if payload.action == "verify":
        await LoanRepository(conn).advance_stage(application_id, current_user.org_id, "branch_approval")
        await AuditService(conn).log(
            application_id=str(application_id),
            org_id=str(current_user.org_id),
            action="Verify OCR Data",
            from_stage=app.stage,
            to_stage="branch_approval",
            actor_id=str(current_user.id),
            actor_role=current_user.role,
            reason=str(payload.corrections) if payload.corrections else None,
        )
        return {"application_id": application_id, "stage": "branch_approval", "verified": True}
    return {"application_id": application_id, "stage": app.stage, "verified": False, "corrections": payload.corrections}


@router.get("/applications/{application_id}/visitation")
@cache_response(ttl_seconds=60, application_scoped=True)
async def get_mobile_visitation(
    application_id: UUID,
    conn=Depends(db_conn),
    current_user=Depends(get_current_user),
):
    await _get_application_or_404(conn, application_id, current_user)
    report = await VisitationRepository(conn).get_by_loan(loan_id=application_id, org_id=current_user.org_id)
    return {"report": report}


@router.put("/applications/{application_id}/visitation")
async def submit_mobile_visitation(
    application_id: UUID,
    payload: VisitationReportRequest,
    conn=Depends(db_conn),
    current_user=Depends(get_current_user),
):
    await _get_application_or_404(conn, application_id, current_user)
    report = await _visitation_service(conn).submit_report(
        loan_id=application_id,
        org_id=current_user.org_id,
        met_with=payload.met_with,
        premises_description=payload.premises_description,
        direction_from_branch=payload.direction_from_branch,
        submitted_by=current_user.id,
        user_role=current_user.role,
    )
    return {"report": report}


@router.post("/applications/{application_id}/visitation/signoff")
async def submit_mobile_visitation_signoff(
    application_id: UUID,
    payload: VisitationSignoffRequest,
    conn=Depends(db_conn),
    current_user=Depends(get_current_user),
):
    _ensure_roles(current_user, {"branch_manager"})
    await _get_application_or_404(conn, application_id, current_user)
    report = await _visitation_service(conn).submit_manager_signoff(
        loan_id=application_id,
        org_id=current_user.org_id,
        manager_id=current_user.id,
        manager_role=current_user.role,
        notes=payload.notes,
        decision=payload.decision,
    )
    if not report:
        raise HTTPException(status_code=409, detail="No submitted visitation report is awaiting signoff")
    # Notify loan officer of visitation sign-off
    try:
        app_obj = await LoanRepository(conn).get_by_id(application_id, current_user.org_id)
        created_by = getattr(app_obj, "created_by", None) if app_obj else None
        if created_by and created_by != current_user.id:
            verb = "concurred with" if payload.decision == "concurred" else "returned"
            await _notification_service(conn).create(
                user_id=created_by,
                org_id=current_user.org_id,
                application_id=application_id,
                title="Visitation Sign-Off",
                message=f"Team Lead {verb} your visitation report.",
                notification_type="visitation_signoff",
            )
    except Exception:
        pass
    return {"report": report}


@router.post("/applications/{application_id}/credit-review")
async def submit_mobile_credit_review(
    application_id: UUID,
    payload: CreditReviewRequest,
    conn=Depends(db_conn),
    current_user=Depends(get_current_user),
):
    _ensure_roles(current_user, {"credit_analyst"})
    app = await _get_application_or_404(conn, application_id, current_user)
    if app.stage != "credit_analyst_review":
        raise HTTPException(status_code=409, detail="Application is not awaiting Credit Analyst review")
    notes = payload.recommendation_notes.strip()
    if not notes:
        raise HTTPException(status_code=422, detail="Provide underwriting recommendation notes")
    if payload.recommendation_decision == "Recommend Approval":
        stage = "crm_review"
    elif payload.recommendation_decision == "Return for Correction":
        stage = "returned"
    else:
        stage = "rejected"

    repo = LoanRepository(conn)
    await repo.save_stage_data(
        application_id,
        "credit_analyst_review",
        {
            "recommendation_decision": payload.recommendation_decision,
            "recommendation_notes": notes,
        },
        current_user.id,
    )
    updated = await repo.advance_stage(application_id, current_user.org_id, stage)
    if not updated:
        raise HTTPException(status_code=409, detail="Application could not be advanced")
    await AuditService(conn).log(
        application_id=str(application_id),
        org_id=str(current_user.org_id),
        action="Credit Underwriting Verdict",
        from_stage=app.stage,
        to_stage=stage,
        actor_id=str(current_user.id),
        actor_role=current_user.role,
        reason=notes,
    )
    # Notify loan officer of credit review outcome
    try:
        created_by = getattr(app, "created_by", None)
        if created_by and created_by != current_user.id:
            await _notification_service(conn).create(
                user_id=created_by,
                org_id=current_user.org_id,
                application_id=application_id,
                title="Credit Review Complete",
                message=f"Credit review verdict: {payload.recommendation_decision}",
                notification_type="credit_review",
            )
    except Exception:
        pass
    return {"application": updated, "stage": stage}


@router.get("/applications/{application_id}/approval-readiness")
@cache_response(ttl_seconds=60, application_scoped=True)
async def get_mobile_approval_readiness(
    application_id: UUID,
    conn=Depends(db_conn),
    current_user=Depends(get_current_user),
):
    await _get_application_or_404(conn, application_id, current_user)
    summary = await LoanRepository(conn).get_readiness_summary(application_id, current_user.org_id)
    return {"summary": summary}


@router.post("/applications/{application_id}/approve")
async def approve_mobile_application(
    application_id: UUID,
    payload: ReviewApprovalRequest,
    conn=Depends(db_conn),
    current_user=Depends(get_current_user),
):
    _ensure_roles(current_user, {"branch_manager", "branch_supervisor"})
    existing = await _get_application_or_404(conn, application_id, current_user)
    role = _role(current_user)
    expected_stage, next_stage, action = (
        ("branch_supervisor_review", "credit_analyst_review", "Supervisor Concurrence")
        if role == "branch_supervisor"
        else ("branch_manager_review", "branch_supervisor_review", "Team Lead Concurrence")
    )
    if existing.stage != expected_stage:
        raise HTTPException(status_code=409, detail="Application is not awaiting your review")
    if not payload.kyc_attested or not payload.collateral_attested:
        raise HTTPException(status_code=422, detail="Complete both review attestations before approval")
    readiness = await LoanRepository(conn).get_readiness_summary(application_id, current_user.org_id)
    base_file_ready = (
        readiness.loan_form_submitted
        and readiness.total_docs > 0
        and readiness.unverified_docs == 0
        and readiness.critical_unverified == 0
        and readiness.low_confidence_unverified == 0
        and readiness.guarantors_required > 0
        and readiness.guarantors_verified == readiness.guarantors_required
        and readiness.consent_credit_bureau
        and readiness.consent_gsi
        and readiness.officer_signed_visitation
    )
    if not base_file_ready:
        raise HTTPException(
            status_code=422,
            detail="The dossier is incomplete. Resolve document, guarantor, consent, OCR, and visitation readiness items before concurrence.",
        )
    context = "supervisor_review" if role == "branch_supervisor" else "team_lead_review"
    for key, label, checked in (
        ("kyc_attested", "KYC requirements verified", payload.kyc_attested),
        ("collateral_attested", "Collateral requirements verified", payload.collateral_attested),
    ):
        await conn.execute(
            """
            INSERT INTO checklist_items
                (loan_application_id, context, item_key, item_label, is_checked, checked_by, checked_at)
            VALUES ($1,$2,$3,$4,$5,$6,NOW())
            ON CONFLICT (loan_application_id, context, item_key)
            DO UPDATE SET is_checked=EXCLUDED.is_checked, checked_by=EXCLUDED.checked_by,
                          checked_at=NOW()
            """,
            application_id, context, key, label, checked, current_user.id,
        )
    app = await LoanRepository(conn).approve(
        application_id,
        current_user.org_id,
        current_user.id,
        expected_stage=expected_stage,
        next_stage=next_stage,
    )
    if not app:
        raise HTTPException(status_code=409, detail="Application is no longer awaiting your review")
    await AuditService(conn).log(
        application_id=str(application_id),
        org_id=str(current_user.org_id),
        action=action,
        from_stage=expected_stage,
        to_stage=next_stage,
        actor_id=str(current_user.id),
        actor_role=current_user.role,
        reason=payload.notes.strip(),
    )
    # Notify loan officer of approval
    try:
        created_by = getattr(app, "created_by", None)
        if created_by and created_by != current_user.id:
            await _notification_service(conn).create(
                user_id=created_by,
                org_id=current_user.org_id,
                application_id=application_id,
                title=f"{action} Complete",
                message=(
                    "Your application has been sent to the Credit Analyst."
                    if role == "branch_supervisor"
                    else "Your application has been sent to the Supervisor."
                ),
                notification_type="approval",
            )
    except Exception:
        pass
    return {"application": app}


@router.post("/applications/{application_id}/return")
async def return_mobile_application(
    application_id: UUID,
    payload: ReturnApplicationRequest,
    conn=Depends(db_conn),
    current_user=Depends(get_current_user),
):
    _ensure_roles(current_user, {"branch_manager", "branch_supervisor"})
    existing = await _get_application_or_404(conn, application_id, current_user)
    expected_stage = (
        "branch_supervisor_review"
        if _role(current_user) == "branch_supervisor"
        else "branch_manager_review"
    )
    if existing.stage != expected_stage:
        raise HTTPException(status_code=409, detail="Application is not awaiting your review")
    reason_parts = [payload.reason_category]
    if payload.corrections:
        reason_parts.append("Corrections: " + ", ".join(payload.corrections))
    reason_parts.append(payload.notes)
    return_reason = " | ".join(part for part in reason_parts if part)

    previous_stages = {
        stage: WORKFLOW_STAGES[index - 1][0]
        for index, (stage, _) in enumerate(WORKFLOW_STAGES)
        if index > 0
    }
    target_stage = previous_stages[expected_stage]
    returned = await LoanRepository(conn).advance_stage(
        application_id, current_user.org_id, target_stage
    )
    if not returned:
        raise HTTPException(status_code=400, detail="Unable to return the application")
    await conn.execute(
        "UPDATE loan_applications SET return_reason = $1, returned_at = NOW() WHERE id = $2 AND org_id = $3",
        return_reason,
        application_id,
        current_user.org_id,
    )
    await AuditService(conn).log(
        application_id=str(application_id),
        org_id=str(current_user.org_id),
        action="Return Application",
        from_stage=expected_stage,
        to_stage=target_stage,
        actor_id=str(current_user.id),
        actor_role=current_user.role,
        reason=return_reason,
    )
    app = await LoanRepository(conn).get_by_id(application_id, current_user.org_id)
    # Notify loan officer their application was returned
    try:
        created_by = getattr(app, "created_by", None) if app else None
        if created_by and created_by != current_user.id:
            await _notification_service(conn).create(
                user_id=created_by,
                org_id=current_user.org_id,
                application_id=application_id,
                title="Application Returned",
                message=f"Your loan application has been returned: {payload.reason_category}",
                notification_type="returned",
            )
    except Exception:
        pass
    return {"application": app, "return_reason": return_reason}


@router.get("/config")
async def get_mobile_config(conn=Depends(db_conn), current_user=Depends(get_current_user)):
    cache_key = f"fieldcrm:cache:mobile-config:v1:org:{current_user.org_id}"
    cached = await get_json(cache_key)
    if cached is not None:
        return cached
    row = await conn.fetchrow("SELECT name FROM organisations WHERE id = $1", current_user.org_id)
    org_name = row["name"] if row else "FieldCRM MFB"
    config = {
        "org_name": org_name,
        "support_phone": "+234 1 234 5678",
        "support_email": "helpdesk@mainstreetmfb.com",
        "node_id": "IKJ-SRV-049",
        "dti_limit": 0.40,
        "pledge_form_code": "MMFB/CRM/02",
        "dropdowns": {
            "marital_status": ["Single", "Married", "Widowed", "Divorced"],
            "employment_status": ["Public Service", "Private Sector", "Self Employed", "Unemployed"],
            "loan_products": [
                {"id": "WC", "name": "Working Capital"},
                {"id": "AP", "name": "Asset Purchase"},
                {"id": "MS", "name": "MSEF"},
                {"id": "PY", "name": "Payee"},
            ],
            "error_categories": ["Payment Failed", "Wrong Deduction", "Not Credited", "BankOne Issue", "Other"],
            "review_reasons": [
                "High Confidence Business Site Check",
                "Strong Co-Guarantor Attestation",
                "Collateral Evaluation Mismatch",
                "Insufficient Credit Score",
            ],
            "document_categories": [
                "National ID", "Utility Bill", "Bank Statement", "Business Permit", "Guarantor ID"
            ],
        },
    }
    await set_json(cache_key, config, ttl_seconds=10 * 60, only_if_absent=True)
    return config


@router.get("/search")
async def search_mobile(
    q: str = Query(""),
    conn=Depends(db_conn),
    current_user=Depends(get_current_user),
):
    if _role(current_user) == "system_admin":
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="System Admin does not have access to borrower or application search",
        )
    if len(q.strip()) < 2:
        return {"applications": [], "borrowers": []}
    term = f"%{q.strip()}%"
    role = _role(current_user)
    if role in {"account_officer", "loan_officer"}:
        apps = await conn.fetch(
            """
            SELECT id, ref_no, applicant_name, stage
            FROM loan_applications
            WHERE org_id = $1 AND deleted_at IS NULL AND created_by = $3
              AND (applicant_name ILIKE $2 OR ref_no ILIKE $2)
            ORDER BY updated_at DESC LIMIT 20
            """,
            current_user.org_id, term, current_user.id,
        )
        borrowers_raw = await conn.fetch(
            """
            SELECT DISTINCT ON (applicant_name) id, applicant_name AS name, phone
            FROM loan_applications
            WHERE org_id = $1 AND deleted_at IS NULL AND created_by = $3 AND applicant_name ILIKE $2
            ORDER BY applicant_name, created_at DESC LIMIT 20
            """,
            current_user.org_id, term, current_user.id,
        )
    else:
        apps = await conn.fetch(
            """
            SELECT id, ref_no, applicant_name, stage
            FROM loan_applications
            WHERE org_id = $1 AND deleted_at IS NULL
              AND (applicant_name ILIKE $2 OR ref_no ILIKE $2)
            ORDER BY updated_at DESC LIMIT 20
            """,
            current_user.org_id, term,
        )
        borrowers_raw = await conn.fetch(
            """
            SELECT DISTINCT ON (applicant_name) id, applicant_name AS name, phone
            FROM loan_applications
            WHERE org_id = $1 AND deleted_at IS NULL AND applicant_name ILIKE $2
            ORDER BY applicant_name, created_at DESC LIMIT 20
            """,
            current_user.org_id, term,
        )
    return {
        "applications": [
            {"id": str(r["id"]), "ref_no": r["ref_no"],
             "borrower_name": r["applicant_name"], "status": _stage_status(r["stage"])}
            for r in apps
        ],
        "borrowers": [
            {"id": str(r["id"]), "name": r["name"], "phone": r["phone"] or ""}
            for r in borrowers_raw
        ],
    }


@router.get("/audit-trail")
async def get_mobile_global_audit_trail(
    limit: int = Query(100, ge=1, le=200),
    offset: int = Query(0, ge=0),
    conn=Depends(db_conn),
    current_user=Depends(get_current_user),
):
    _ensure_roles(current_user, {"auditor"})
    rows = await DashboardService(conn).get_recent_audit_activity(current_user, limit=limit, offset=offset)
    return [
        {
            "id": str(row.get("id", "")),
            "timestamp": row.get("created_at").isoformat() if row.get("created_at") else "",
            "actor_name": row.get("user_name") or "",
            "actor_role": row.get("user_role") or "",
            "action": row.get("action") or "",
            "state_diff": row.get("field_name") or row.get("entity_type") or "",
            "notes": row.get("notes") or "",
            "is_mine": str(row.get("user_id", "")) == str(current_user.id),
        }
        for row in rows
    ]


@router.get("/applications/{application_id}/audit")
@cache_response(ttl_seconds=60, application_scoped=True)
async def get_mobile_audit_trail(
    application_id: UUID,
    page: int = Query(1, ge=1),
    size: int = Query(50, ge=1, le=100),
    conn=Depends(db_conn),
    current_user=Depends(get_current_user),
):
    await _get_application_or_404(conn, application_id, current_user)
    events = await conn.fetch(
        """
        SELECT we.id, we.triggered_by, we.event_type, we.from_stage, we.to_stage,
               we.triggered_role, we.notes, we.created_at,
               u.full_name AS actor_name
        FROM workflow_events we
        JOIN users u ON u.id = we.triggered_by
        WHERE we.loan_id = $1 AND we.org_id = $2
        ORDER BY we.created_at DESC, we.id DESC
        LIMIT $3 OFFSET $4
        """,
        application_id, current_user.org_id, size, (page - 1) * size,
    )

    stage_labels = {
        "intake": "Intake",
        "ocr_review": "OCR Review",
        "credit_review": "Credit Review",
        "branch_approval": "Branch Approval",
        "crm_review": "CRM Review",
        "ed_approval": "ED Approval",
        "md_approval": "MD Approval",
        "disbursement_ready": "Disbursement Ready",
        "disbursed": "Disbursed",
        "returned": "Returned",
        "rejected": "Rejected",
    }

    def fmt_stage(s):
        if not s:
            return "—"
        return stage_labels.get(s, s.replace("_", " ").title())

    return [
        {
            "id": str(e["id"]),
            "timestamp": e["created_at"].isoformat(),
            "actor_name": e["actor_name"],
            "actor_role": e["triggered_role"],
            "action": e["event_type"],
            "state_diff": f"{fmt_stage(e['from_stage'])} → {fmt_stage(e['to_stage'])}",
            "notes": e["notes"] or "",
            "is_mine": str(e["triggered_by"]) == str(current_user.id),
        }
        for e in events
    ]


@router.get("/applications/{application_id}/bureau")
@cache_response(ttl_seconds=60, application_scoped=True)
async def get_mobile_bureau(
    application_id: UUID,
    conn=Depends(db_conn),
    current_user=Depends(get_current_user),
):
    await _get_application_or_404(conn, application_id, current_user)
    row = await conn.fetchrow(
        """
        SELECT data_json FROM stage_data
        WHERE loan_id = $1 AND stage = 'credit_review'
        ORDER BY saved_at DESC LIMIT 1
        """,
        application_id,
    )
    data = dict(row["data_json"]) if row else {}
    return {
        "credit_score": int(data.get("credit_score", 0)),
        "dti_ratio": float(data.get("dti_ratio", 0.0)),
        "income_verified": bool(data.get("income_verified", False)),
        "source": data.get("bureau_source", "Bureau Pull — Lagos Node"),
    }


@router.get("/applications/{application_id}/audit-checklist")
@cache_response(ttl_seconds=60, application_scoped=True)
async def get_mobile_audit_checklist(
    application_id: UUID,
    conn=Depends(db_conn),
    current_user=Depends(get_current_user),
):
    await _get_application_or_404(conn, application_id, current_user)
    row = await conn.fetchrow(
        """
        SELECT data_json FROM stage_data
        WHERE loan_id = $1 AND stage = 'audit_checklist'
        ORDER BY saved_at DESC LIMIT 1
        """,
        application_id,
    )
    data = dict(row["data_json"]) if row else {}
    return {
        "consent_verified": bool(data.get("consent_verified", False)),
        "signature_matched": bool(data.get("signature_matched", False)),
        "exhibits_verified": bool(data.get("exhibits_verified", False)),
    }


async def save_mobile_audit_checklist(
    application_id: UUID,
    payload: AuditChecklistRequest,
    conn=Depends(db_conn),
    current_user=Depends(get_current_user),
):
    _ensure_roles(current_user, {"system_admin"})
    await _get_application_or_404(conn, application_id, current_user)
    data = json.dumps({
        "consent_verified": payload.consent_verified,
        "signature_matched": payload.signature_matched,
        "exhibits_verified": payload.exhibits_verified,
    })
    await conn.execute(
        "INSERT INTO stage_data (loan_id, stage, data_json, saved_by) VALUES ($1, 'audit_checklist', $2::jsonb, $3)",
        application_id, data, current_user.id,
    )
    return {"ok": True}


@router.get("/faqs")
async def get_mobile_faqs(current_user=Depends(get_current_user)):
    cache_key = "fieldcrm:cache:mobile-faqs:v1"
    cached = await get_json(cache_key)
    if cached is not None:
        return cached
    faqs = [
        {
            "question": "How does the camera OCR parser work?",
            "answer": "Align the NIN/BVN identity document inside the viewfinder scanner box. Click Scan & Extract; standard ML Kit extracts and matches text values locally without remote delays.",
        },
        {
            "question": "GPS Coordinates lock timeout?",
            "answer": "Ensure location sensors are enabled on your device. Click the refresh location button to trigger an active ACCESS_FINE_LOCATION provider query.",
        },
        {
            "question": "Managing the Offline Sync Queue?",
            "answer": "When working offline, completed dossiers are queued locally. Tap the Sync button on the main tab once network coverage is restored to upload cached records.",
        },
        {
            "question": "What is the DTI limit for loan approval?",
            "answer": "The Debt-to-Income ratio limit is 40%. Applications above this threshold require the additional review configured in the production approval workflow.",
        },
        {
            "question": "How do I escalate a compliance flag?",
            "answer": "Navigate to the application audit trail, review workflow events, and use the Report Problem option to escalate to the compliance officer via the platform.",
        },
    ]
    await set_json(cache_key, faqs, ttl_seconds=24 * 60 * 60, only_if_absent=True)
    return faqs


_ONBOARDING_SLIDES: dict[str, list[dict]] = {
    "loan_officer": [
        {"title": "Welcome to FieldCRM", "subtitle": "Your Field Operations Hub",
         "body": "Manage loan applications, conduct visits, and process borrower records from your mobile device — anytime, anywhere."},
        {"title": "Create Applications", "subtitle": "Start a New Loan Dossier",
         "body": "Tap the + button on your Work Queue to begin a new loan application. Fill in borrower details step by step and capture documents with OCR scanning."},
        {"title": "Schedule Visits", "subtitle": "Track Your Field Visits",
         "body": "View visits due today on your dashboard. Complete visitation reports in the field and submit them for Team Lead sign-off."},
        {"title": "Offline Mode", "subtitle": "Work Without Internet",
         "body": "All your changes are saved locally when offline. When you reconnect, the sync queue automatically uploads completed records to the server."},
    ],
    "branch_manager": [
        {"title": "Team Lead Dashboard", "subtitle": "Branch Oversight Console",
         "body": "Monitor all applications awaiting your sign-off, review concurrence requests from Relationship Officers, and track branch disbursement targets."},
        {"title": "Approval Workflow", "subtitle": "Final Branch Decision",
         "body": "Review credit officer recommendations and complete the final approval attestation. Applications you approve move directly to disbursement ready status."},
        {"title": "Visitation Sign-Off", "subtitle": "Concur or Return Reports",
         "body": "Review visitation reports submitted by your field officers. Concur with findings or return for corrections before final approval."},
    ],
    "auditor": [
        {"title": "Compliance Audit", "subtitle": "Regulatory Oversight Tools",
         "body": "Review loan files for policy compliance, verify audit checklists, and flag applications with potential regulatory breaches."},
        {"title": "Audit Checklist", "subtitle": "Structured Compliance Review",
         "body": "Complete the audit checklist for each reviewed application. Verify consent documentation, signature matching, and exhibit compliance."},
    ],
    "system_admin": [
        {"title": "System Administration", "subtitle": "Technical Support Console",
         "body": "Manage users, correct data errors, monitor system health, and provide technical support across all branches. You do not participate in the loan approval pipeline."},
        {"title": "User Management", "subtitle": "Create & Manage Accounts",
         "body": "Create officer accounts, assign roles, reset passwords, and deactivate users. All changes are logged in the audit trail."},
    ],
    "crm": [
        {"title": "CRM Review Console", "subtitle": "Pre-Disbursement Review",
         "body": "Review loan files approved by the Team Lead. Verify credit file completeness — bureau evidence, CRMS search, NCR registration — before advancing to the Executive."},
        {"title": "Disbursement Processing", "subtitle": "Record & Schedule",
         "body": "Once executive instruction is issued, record the disbursement details, generate the repayment schedule, and track collections."},
        {"title": "Portfolio Tracking", "subtitle": "PAR & Loan Classification",
         "body": "Monitor the PAR dashboard daily. Record repayments, track overdue accounts, and ensure CBN classification is up to date."},
    ],
    "executive": [
        {"title": "Executive Dashboard", "subtitle": "Portfolio Overview",
         "body": "Review the PAR dashboard, disbursement queue, and portfolio health metrics. Your approval is required to release disbursement instructions to the CRM."},
        {"title": "Disbursement Instruction", "subtitle": "Issue or Decline",
         "body": "Review the CRM-prepared loan file and issue the disbursement instruction. This action is logged, irreversible, and triggers the CRM to process payment."},
    ],
}


async def warm_mobile_static_cache() -> None:
    """Deployment warm-up: add shared onboarding data without replacing it."""
    for role, slides in _ONBOARDING_SLIDES.items():
        await set_json(
            f"fieldcrm:cache:mobile-onboarding:v1:role:{role}",
            slides,
            ttl_seconds=24 * 60 * 60,
            only_if_absent=True,
        )


@router.get("/onboarding")
async def get_mobile_onboarding(
    role: str = Query("loan_officer"),
    current_user=Depends(get_current_user),
):
    mapped = _mobile_role(current_user) if not role else role
    cache_key = f"fieldcrm:cache:mobile-onboarding:v1:role:{mapped}"
    cached = await get_json(cache_key)
    if cached is not None:
        return cached
    slides = _ONBOARDING_SLIDES.get(mapped, _ONBOARDING_SLIDES["loan_officer"])
    await set_json(cache_key, slides, ttl_seconds=24 * 60 * 60, only_if_absent=True)
    return slides


class ForgotPasswordRequest(BaseModel):
    email: str


class ResetPasswordRequest(BaseModel):
    token: str
    new_password: str


@router.post("/auth/forgot-password")
async def mobile_forgot_password(request: Request, req: ForgotPasswordRequest, conn=Depends(db_conn)):
    from app.domains.auth.repository import AuthRepository
    from app.domains.auth.service import AuthService
    await enforce_reset_limits(request, req.email)
    await AuthService(AuthRepository(conn)).request_password_reset(req.email)
    return {"message": "If that email is registered, a reset link has been sent."}


@router.post("/auth/reset-password")
async def mobile_reset_password(request: Request, req: ResetPasswordRequest, conn=Depends(db_conn)):
    from app.domains.auth.repository import AuthRepository
    from app.domains.auth.service import AuthService
    await enforce_reset_limits(request, req.token)
    ok = await AuthService(AuthRepository(conn)).reset_password(req.token, req.new_password)
    if not ok:
        raise HTTPException(status_code=400, detail="Invalid or expired reset token.")
    return {"message": "Password reset successful."}


class OcrCorrectionsRequest(BaseModel):
    corrections: dict[str, str] = Field(default_factory=dict)


@router.post("/applications/{application_id}/ocr-corrections")
async def save_mobile_ocr_corrections(
    application_id: str,
    req: OcrCorrectionsRequest,
    conn=Depends(db_conn),
    current_user=Depends(get_current_user),
):
    svc = LoanService(LoanRepository(conn), AuditService(conn))
    await svc.save_wizard_step(
        app_id=UUID(application_id),
        org_id=current_user.org_id,
        step=0,
        form_data={"ocr_corrections": req.corrections, "correction_source": "mobile"},
        user_id=current_user.id,
    )
    return {"message": "OCR corrections saved."}


# ---------------------------------------------------------------------------
# CRM Review endpoints
# ---------------------------------------------------------------------------

class CrmReviewRequest(BaseModel):
    decision: Literal["advance", "return"]
    notes: str = ""
    bureau_1_verified: bool = False
    bureau_2_verified: bool = False
    crms_verified: bool = False
    ncr_verified: bool = False


@router.get("/applications/{application_id}/crm-review")
async def get_mobile_crm_review(
    application_id: UUID,
    conn=Depends(db_conn),
    current_user=Depends(get_current_user),
):
    _ensure_roles(current_user, {"crm", "head_crm"})
    app = await _get_application_or_404(conn, application_id, current_user)
    documents = await DocumentRepository(conn).get_by_loan(application_id, current_user.org_id)
    return {"application": app, "documents": [_mobile_document(document) for document in documents]}


@router.post("/applications/{application_id}/crm-review")
async def submit_mobile_crm_review(
    application_id: UUID,
    payload: CrmReviewRequest,
    conn=Depends(db_conn),
    current_user=Depends(get_current_user),
):
    _ensure_roles(current_user, {"crm", "head_crm"})
    app = await _get_application_or_404(conn, application_id, current_user)
    role = _role(current_user)
    expected_stage = "head_crm_review" if role == "head_crm" else "crm_review"
    if app.stage != expected_stage:
        raise HTTPException(status_code=409, detail="Application is not awaiting your review")
    repo = LoanRepository(conn)
    if payload.decision == "advance":
        if role == "crm":
            checks = {
                "bureau_1_verified": payload.bureau_1_verified,
                "bureau_2_verified": payload.bureau_2_verified,
                "crms_verified": payload.crms_verified,
                "ncr_verified": payload.ncr_verified,
            }
            if not all(checks.values()):
                raise HTTPException(status_code=422, detail="Complete the CRM credit-file checklist before advancing")
            for key, checked in checks.items():
                await conn.execute(
                    """
                    INSERT INTO checklist_items
                        (loan_application_id, context, item_key, item_label, is_checked, checked_by, checked_at)
                    VALUES ($1,'crm_review',$2,$3,$4,$5,NOW())
                    ON CONFLICT (loan_application_id, context, item_key)
                    DO UPDATE SET is_checked=EXCLUDED.is_checked, checked_by=EXCLUDED.checked_by,
                                  checked_at=NOW()
                    """,
                    application_id, key, key.replace("_", " ").title(), checked, current_user.id,
                )
        else:
            completed_checks = await conn.fetchval(
                """
                SELECT COUNT(*) FROM checklist_items
                WHERE loan_application_id=$1 AND context='crm_review'
                  AND item_key = ANY($2::text[]) AND is_checked=TRUE
                """,
                application_id,
                ["bureau_1_verified", "bureau_2_verified", "crms_verified", "ncr_verified"],
            )
            if completed_checks != 4:
                raise HTTPException(status_code=422, detail="The CRM credit-file checklist is incomplete")
        next_stage = "ed_approval" if role == "head_crm" else "head_crm_review"
        updated = await repo.advance_stage(application_id, current_user.org_id, next_stage)
    else:
        next_stage = "crm_review" if role == "head_crm" else "credit_analyst_review"
        updated = await repo.advance_stage(application_id, current_user.org_id, next_stage)
        await conn.execute(
            "UPDATE loan_applications SET return_reason = $1, returned_at = NOW() WHERE id = $2 AND org_id = $3",
            payload.notes,
            application_id,
            current_user.org_id,
        )
    await AuditService(conn).log(
        application_id=str(application_id),
        org_id=str(current_user.org_id),
        action="CRM Review",
        from_stage=app.stage,
        to_stage=next_stage,
        actor_id=str(current_user.id),
        actor_role=current_user.role,
        reason=payload.notes,
    )
    return {"application": updated, "stage": next_stage}


# ---------------------------------------------------------------------------
# Executive approval endpoints
# ---------------------------------------------------------------------------

@router.get("/applications/{application_id}/executive-review")
async def get_mobile_executive_review(
    application_id: UUID,
    conn=Depends(db_conn),
    current_user=Depends(get_current_user),
):
    _ensure_roles(current_user, {"md", "ed"})
    app = await _get_application_or_404(conn, application_id, current_user)
    documents = await DocumentRepository(conn).get_by_loan(application_id, current_user.org_id)
    return {"application": app, "documents": documents}


@router.post("/applications/{application_id}/executive-approve")
async def submit_mobile_executive_approve(
    application_id: UUID,
    conn=Depends(db_conn),
    current_user=Depends(get_current_user),
):
    _ensure_roles(current_user, {"md", "ed"})
    app = await _get_application_or_404(conn, application_id, current_user)
    updated = await LoanRepository(conn).executive_approve(
        loan_id=application_id,
        org_id=current_user.org_id,
        executive_id=current_user.id,
    )
    if not updated:
        raise HTTPException(status_code=409, detail="Application not in executive_approval stage")
    await AuditService(conn).log(
        application_id=str(application_id),
        org_id=str(current_user.org_id),
        action="Executive Disbursement Instruction",
        from_stage=app.stage,
        to_stage="disbursement_ready",
        actor_id=str(current_user.id),
        actor_role=current_user.role,
    )
    return {"application": updated, "stage": "disbursement_ready"}


# ---------------------------------------------------------------------------
# ED approval endpoints
# ---------------------------------------------------------------------------

class EdApproveRequest(BaseModel):
    action: Literal["approve", "escalate_md"]


@router.get("/applications/{application_id}/ed-review")
async def get_mobile_ed_review(
    application_id: UUID,
    conn=Depends(db_conn),
    current_user=Depends(get_current_user),
):
    _ensure_roles(current_user, {"ed"})
    app = await _get_application_or_404(conn, application_id, current_user)
    documents = await DocumentRepository(conn).get_by_loan(application_id, current_user.org_id)
    return {"application": app, "documents": documents}


@router.post("/applications/{application_id}/ed-approve")
async def submit_mobile_ed_approve(
    application_id: UUID,
    payload: EdApproveRequest,
    conn=Depends(db_conn),
    current_user=Depends(get_current_user),
):
    _ensure_roles(current_user, {"ed"})
    app = await _get_application_or_404(conn, application_id, current_user)
    repo = LoanRepository(conn)
    if payload.action == "approve":
        updated = await repo.ed_approve(application_id, current_user.org_id, current_user.id)
        next_stage = "disbursement_ready"
        action_label = "ED Final Approval — Disbursement Instruction"
    else:
        if (app.amount or 0) > 10_000_000:
            raise HTTPException(status_code=409, detail="MD input can only be requested for loans of ₦10,000,000 or less")
        updated = await repo.ed_escalate_to_md(application_id, current_user.org_id, current_user.id)
        next_stage = "md_approval"
        action_label = "ED Escalated to MD"
    if not updated:
        raise HTTPException(status_code=409, detail="Application not in ed_approval stage")
    await AuditService(conn).log(
        application_id=str(application_id),
        org_id=str(current_user.org_id),
        action=action_label,
        from_stage=app.stage, to_stage=next_stage,
        actor_id=str(current_user.id), actor_role=current_user.role,
    )
    return {"application": updated, "stage": next_stage}


# ---------------------------------------------------------------------------
# MD approval endpoints
# ---------------------------------------------------------------------------

class MdApproveRequest(BaseModel):
    action: Literal["approve", "comment"]
    notes: str = ""


class BoardReferralRequest(BaseModel):
    board_member_email: str
    board_member_name: str = ""
    notes: str = ""


class WorkflowTransitionRequest(BaseModel):
    notes: str = ""


@router.get("/applications/{application_id}/md-review")
async def get_mobile_md_review(
    application_id: UUID,
    conn=Depends(db_conn),
    current_user=Depends(get_current_user),
):
    _ensure_roles(current_user, {"md"})
    app = await _get_application_or_404(conn, application_id, current_user)
    repo = LoanRepository(conn)
    board_referrals = await repo.get_board_referrals(application_id, current_user.org_id)
    documents = await DocumentRepository(conn).get_by_loan(application_id, current_user.org_id)
    return {"application": app, "board_referrals": board_referrals, "documents": [_mobile_document(document) for document in documents]}


@router.post("/applications/{application_id}/md-approve")
async def submit_mobile_md_approve(
    application_id: UUID,
    payload: MdApproveRequest,
    conn=Depends(db_conn),
    current_user=Depends(get_current_user),
):
    _ensure_roles(current_user, {"md"})
    app = await _get_application_or_404(conn, application_id, current_user)
    repo = LoanRepository(conn)
    if payload.action == "approve":
        updated = await repo.md_approve(application_id, current_user.org_id, current_user.id, payload.notes)
        if not updated:
            raise HTTPException(status_code=409, detail="Application not in md_approval stage")
        next_stage = "disbursement_ready"
        action_label = "MD Final Approval — Disbursement Instruction"
    else:
        if not await repo.md_add_comment(application_id, current_user.org_id, payload.notes):
            raise HTTPException(status_code=409, detail="Application not in md_approval stage")
        updated = await _get_application_or_404(conn, application_id, current_user)
        next_stage = "ed_approval"
        action_label = "MD Comment Added — Returned to ED"
    await AuditService(conn).log(
        application_id=str(application_id),
        org_id=str(current_user.org_id),
        action=action_label,
        from_stage=app.stage, to_stage=next_stage,
        actor_id=str(current_user.id), actor_role=current_user.role,
        reason=payload.notes,
    )
    return {"application": updated, "stage": next_stage}


@router.post("/applications/{application_id}/md-refer-board")
async def submit_mobile_board_referral(
    application_id: UUID,
    payload: BoardReferralRequest,
    conn=Depends(db_conn),
    current_user=Depends(get_current_user),
):
    _ensure_roles(current_user, {"md"})
    app = await _get_application_or_404(conn, application_id, current_user)
    referral = await LoanRepository(conn).insert_board_referral(
        application_id, current_user.org_id, current_user.id,
        payload.board_member_email, payload.board_member_name, payload.notes
    )
    from app.services.email_service import EmailService
    member_name = payload.board_member_name.strip() or "Board Member"
    EmailService().send_notification(
        recipient=payload.board_member_email,
        subject=f"Board advice requested — {app.ref_no}",
        text=(
            f"Hello {member_name},\n\n{current_user.full_name} has requested your board advice on "
            f"FieldCRM application {app.ref_no} for {app.applicant_name}.\n\n"
            f"Notes: {payload.notes or 'No additional notes provided.'}"
        ),
        html_content=(
            f"<p>Hello {payload.board_member_name or 'Board Member'},</p><p><strong>{current_user.full_name}</strong> "
            f"has requested your board advice on FieldCRM application <strong>{app.ref_no}</strong> "
            f"for {app.applicant_name}.</p><p>{payload.notes or 'No additional notes provided.'}</p>"
        ),
        sender_name=current_user.full_name,
        reply_email=current_user.email,
    )
    await AuditService(conn).log(
        application_id=str(application_id),
        org_id=str(current_user.org_id),
        action=f"MD Board Referral — {payload.board_member_email}",
        from_stage="md_approval", to_stage="md_approval",
        actor_id=str(current_user.id), actor_role=current_user.role,
        reason=payload.notes,
    )
    return {"referral": referral}


@router.post("/applications/{application_id}/workflow/advance")
async def advance_review_workflow(
    application_id: UUID,
    payload: WorkflowTransitionRequest,
    conn=Depends(db_conn),
    current_user=Depends(get_current_user),
):
    """Advance a file through the operational review chain before ED/MD review."""
    app = await _get_application_or_404(conn, application_id, current_user)
    role = current_user.role.lower().replace(" ", "_")
    required_role = STAGE_ROLE.get(app.stage)
    if required_role is None:
        raise HTTPException(status_code=409, detail="This application is not in the operational review workflow")
    if app.stage == "intake":
        raise HTTPException(
            status_code=409,
            detail="Relationship Officers complete intake only; the Team Lead submits the application for review.",
        )
    if role != required_role:
        raise HTTPException(status_code=403, detail=f"This stage is assigned to {ROLE_LABELS[required_role]}")
    if app.stage in {"ed_approval", "md_approval"}:
        raise HTTPException(status_code=409, detail="Use the existing ED or MD decision endpoint for this stage")

    next_stage = NEXT_STAGE.get(app.stage)
    if not next_stage:
        raise HTTPException(status_code=409, detail="This application is already with CRM for disbursement")

    updated = await LoanRepository(conn).advance_stage(application_id, current_user.org_id, next_stage)
    if not updated:
        raise HTTPException(status_code=409, detail="Application could not be advanced")

    await AuditService(conn).log(
        application_id=str(application_id), org_id=str(current_user.org_id),
        action=f"{ROLE_LABELS.get(role, role.title())} review complete",
        from_stage=app.stage, to_stage=next_stage,
        actor_id=str(current_user.id), actor_role=current_user.role, reason=payload.notes,
    )

    next_role = STAGE_ROLE[next_stage]
    recipients = await conn.fetch(
        "SELECT email FROM users WHERE org_id = $1 AND role = $2 AND active = TRUE",
        current_user.org_id, next_role,
    )
    subject = f"FieldCRM action required — {updated.ref_no}"
    text = (
        f"Application {updated.ref_no} for {updated.applicant_name} is ready for your "
        f"{ROLE_LABELS[next_role]} review. This is an automated notification; please do not reply."
    )
    html_content = (
        f"<p>Application <strong>{updated.ref_no}</strong> for {updated.applicant_name} is ready for your "
        f"<strong>{ROLE_LABELS[next_role]}</strong> review.</p><p>This is an automated notification; please do not reply.</p>"
    )
    mailer = EmailService()
    for recipient in recipients:
        mailer.send_notification(recipient=recipient["email"], subject=subject, text=text, html_content=html_content)
    return {"application": updated, "stage": next_stage, "notified_role": next_role}


@router.post("/applications/{application_id}/submit-to-branch-manager")
async def submit_mobile_signed_intake_to_branch_manager(
    application_id: UUID,
    conn=Depends(db_conn),
    current_user=Depends(get_current_user),
):
    """JSON equivalent of the canonical signed-intake web transition."""
    app = await _get_application_or_404(conn, application_id, current_user)
    _ensure_roles(current_user, {"account_officer"})
    if app.created_by != current_user.id:
        raise HTTPException(status_code=403, detail="You do not have permission to submit this application")
    if app.stage != "intake":
        raise HTTPException(status_code=409, detail="Only an intake application can be submitted to the branch manager")

    repo = LoanRepository(conn)
    await repo.assign_default_branch_manager(application_id, current_user.org_id)
    updated = await repo.advance_stage(application_id, current_user.org_id, "branch_manager_review")
    if not updated:
        raise HTTPException(status_code=409, detail="Application could not be submitted")

    await AuditService(conn).log(
        application_id=str(application_id),
        org_id=str(current_user.org_id),
        action="Intake submitted to Branch Manager",
        from_stage="intake",
        to_stage="branch_manager_review",
        actor_id=str(current_user.id),
        actor_role=current_user.role,
    )
    return {"application": _mobile_application(updated, current_user), "stage": updated.stage}


@router.post("/applications/{application_id}/credit-bureau-pull")
async def pull_mobile_credit_bureau_report(
    application_id: UUID,
    conn=Depends(db_conn),
    current_user=Depends(get_current_user),
):
    app = await _get_application_or_404(conn, application_id, current_user)
    _ensure_roles(current_user, {"credit_analyst"})
    from app.domains.credit_bureau.service import CreditBureauService

    service = CreditBureauService(conn)
    session_code = await service.get_session_code()
    if not session_code:
        raise HTTPException(status_code=503, detail="Credit bureau authentication is unavailable")
    registry_id = await service.find_customer(
        session_code=session_code, bvn=app.bvn, phone=app.phone, name=app.applicant_name
    )
    if not registry_id:
        raise HTTPException(status_code=404, detail="Applicant was not found by the credit bureau")
    report = await service.get_report(
        loan_application_id=str(application_id),
        registry_id=registry_id,
        session_code=session_code,
    )
    return {"application_id": application_id, "report": report}


@router.get("/applications/{application_id}/credit-checklist")
async def get_mobile_credit_checklist(
    application_id: UUID,
    context: str = Query("credit", min_length=1, max_length=40),
    conn=Depends(db_conn),
    current_user=Depends(get_current_user),
):
    await _get_application_or_404(conn, application_id, current_user)
    rows = await conn.fetch(
        """
        SELECT item_key, item_label, is_checked, checked_at
        FROM checklist_items
        WHERE loan_application_id = $1 AND context = $2
        ORDER BY item_key
        """,
        application_id, context,
    )
    return {"application_id": application_id, "context": context, "items": [dict(row) for row in rows]}


@router.patch("/applications/{application_id}/credit-checklist")
async def update_mobile_credit_checklist(
    application_id: UUID,
    payload: CreditChecklistItemRequest,
    conn=Depends(db_conn),
    current_user=Depends(get_current_user),
):
    app = await _get_application_or_404(conn, application_id, current_user)
    _ensure_roles(current_user, {"credit_analyst"})
    row = await conn.fetchrow(
        """
        INSERT INTO checklist_items
            (loan_application_id, context, item_key, item_label, is_checked, checked_by, checked_at)
        VALUES ($1, $2, $3, $4, $5, $6, NOW())
        ON CONFLICT (loan_application_id, context, item_key)
        DO UPDATE SET item_label = EXCLUDED.item_label,
                      is_checked = EXCLUDED.is_checked,
                      checked_by = EXCLUDED.checked_by,
                      checked_at = NOW()
        RETURNING item_key, item_label, is_checked, checked_at
        """,
        application_id, payload.context, payload.item_key, payload.item_label,
        payload.is_checked, current_user.id,
    )
    await AuditService(conn).log(
        application_id=str(application_id), org_id=str(current_user.org_id),
        action="Credit checklist updated", from_stage=app.stage, to_stage=app.stage,
        actor_id=str(current_user.id), actor_role=current_user.role,
        reason=f"{payload.item_label or payload.item_key}: {payload.is_checked}",
    )
    return dict(row)


def _signing_link(request: Request, path: str, claims: dict[str, Any], minutes: int) -> dict[str, Any]:
    from jose import jwt
    expires_at = datetime.now(timezone.utc) + timedelta(minutes=minutes)
    token = jwt.encode(
        {**claims, "exp": expires_at, "nonce": secrets.token_hex(16)},
        settings.JWT_SECRET_KEY,
        algorithm=settings.JWT_ALGORITHM,
    )
    return {
        "share_url": f"{str(request.base_url).rstrip('/')}/{path}/{token}",
        "expires_at": expires_at,
    }


@router.post("/applications/{application_id}/client-link")
async def generate_mobile_client_link(
    request: Request,
    application_id: UUID,
    conn=Depends(db_conn),
    current_user=Depends(get_current_user),
):
    raise HTTPException(
        status_code=status.HTTP_410_GONE,
        detail="Client signing and link generation is deactivated."
    )


@router.post("/applications/{application_id}/guarantor-link/{slot}")
async def generate_mobile_guarantor_link(
    request: Request,
    application_id: UUID,
    slot: int = Path(..., ge=1, le=2),
    conn=Depends(db_conn),
    current_user=Depends(get_current_user),
):
    raise HTTPException(
        status_code=status.HTTP_410_GONE,
        detail="Client signing and link generation is deactivated."
    )


@router.get("/applications/{application_id}/offer")
async def get_mobile_offer_readiness(
    application_id: UUID,
    conn=Depends(db_conn),
    current_user=Depends(get_current_user),
):
    app = await _get_application_or_404(conn, application_id, current_user)
    offer = await conn.fetchrow(
        """
        SELECT id, status, interest_rate_snapshot, generated_pdf_url, created_at
        FROM offer_letters
        WHERE loan_application_id = $1
        ORDER BY created_at DESC LIMIT 1
        """,
        application_id,
    )
    return {
        "ready": app.stage == "disbursement_ready",
        "stage": app.stage,
        "offer": dict(offer) if offer else None,
    }


@router.post("/applications/{application_id}/offer", status_code=status.HTTP_201_CREATED)
async def generate_mobile_offer(
    application_id: UUID,
    conn=Depends(db_conn),
    current_user=Depends(get_current_user),
):
    app = await _get_application_or_404(conn, application_id, current_user)
    _ensure_roles(current_user, {"crm"})
    if app.stage != "disbursement_ready":
        raise HTTPException(status_code=409, detail="Application is not ready for an offer letter")
    existing = await conn.fetchrow(
        "SELECT * FROM offer_letters WHERE loan_application_id = $1 ORDER BY created_at DESC LIMIT 1",
        application_id,
    )
    if existing:
        return {"offer": dict(existing), "replayed": True}

    preset = await conn.fetchrow(
        "SELECT rate FROM interest_rate_presets WHERE loan_type = $1 ORDER BY set_at DESC LIMIT 1",
        app.loan_type,
    )
    rate = preset["rate"] if preset else Decimal("24.0")
    clause_row = await conn.fetchrow(
        "SELECT clause_keys FROM offer_letter_clause_sets WHERE loan_type = $1", app.loan_type
    )
    clauses = clause_row["clause_keys"] if clause_row else [
        "Interest is subject to market review.",
        "Penalty fees apply to past due amounts.",
        "Global Standing Instruction applies to the facility.",
    ]
    if isinstance(clauses, str):
        clauses = json.loads(clauses)
    from app.services.pdf_service import generate_offer_letter_pdf
    from app.services.cloud_storage_service import upload_document
    org_name = await conn.fetchval("SELECT name FROM organisations WHERE id = $1", current_user.org_id)
    pdf_bytes = generate_offer_letter_pdf(
        loan={"ref_no": app.ref_no, "applicant_name": app.applicant_name,
              "amount": app.amount or 0, "tenor_months": app.tenor_months or 12,
              "loan_type": app.loan_type, "repayment_frequency": "Monthly"},
        org={"name": org_name or "Organisation"}, rate=float(rate), clauses=list(clauses),
    )
    cloud = upload_document(
        file_bytes=pdf_bytes, mime_type="application/pdf", org_id=str(current_user.org_id),
        loan_id=str(application_id), doc_type="offer_letter", filename_stem=application_id.hex,
    )
    if not cloud or not cloud.public_id:
        raise HTTPException(status_code=503, detail="Offer letter storage is unavailable")
    pdf_url = f"cloudinary://{cloud.public_id}"
    offer = await conn.fetchrow(
        """
        INSERT INTO offer_letters
            (loan_application_id, loan_type, clause_set_version, clauses_included,
             interest_rate_snapshot, generated_pdf_url, generated_by, status)
        VALUES ($1, $2, 'v1', $3, $4, $5, $6, 'issued')
        RETURNING *
        """,
        application_id, app.loan_type, json.dumps(clauses), rate, pdf_url, current_user.id,
    )
    await DocumentRepository(conn).create(
        loan_id=application_id, org_id=current_user.org_id, doc_type="offer_letter",
        form_code=None, original_name="offer_letter.pdf", stored_path=pdf_url,
        mime_type="application/pdf", size_bytes=len(pdf_bytes), uploaded_by=current_user.id,
        cloud_public_id=cloud.public_id, cloud_preview_url=cloud.preview_url,
    )
    return {"offer": dict(offer)}


@router.get("/applications/{application_id}/disbursement")
async def get_mobile_disbursement(
    application_id: UUID,
    conn=Depends(db_conn),
    current_user=Depends(get_current_user),
):
    app = await _get_application_or_404(conn, application_id, current_user)
    offer_exists = await conn.fetchval(
        """
        SELECT EXISTS(
            SELECT 1 FROM documents
            WHERE loan_id = $1 AND org_id = $2 AND doc_type = 'offer_letter'
              AND deleted_at IS NULL
        )
        """,
        application_id, current_user.org_id,
    )
    return {
        "application": _mobile_application(app, current_user),
        "can_record": _role(current_user) == "crm"
                      and app.stage == "disbursement_ready" and bool(offer_exists),
        "offer_generated": bool(offer_exists),
    }


@router.post("/applications/{application_id}/disbursement")
async def record_mobile_disbursement(
    application_id: UUID,
    payload: DisbursementRequest,
    conn=Depends(db_conn),
    current_user=Depends(get_current_user),
):
    await _get_application_or_404(conn, application_id, current_user)
    _ensure_roles(current_user, {"crm"})
    offer_exists = await conn.fetchval(
        """
        SELECT EXISTS(
            SELECT 1 FROM documents
            WHERE loan_id = $1 AND org_id = $2 AND doc_type = 'offer_letter'
              AND deleted_at IS NULL
        )
        """,
        application_id, current_user.org_id,
    )
    if not offer_exists:
        raise HTTPException(status_code=409, detail="Generate the offer letter before recording disbursement")
    disbursement_ref = f"DIS-{datetime.now().strftime('%Y%m%d')}-{secrets.token_hex(4).upper()}"
    app = await LoanRepository(conn).disburse(
        loan_id=application_id, org_id=current_user.org_id,
        disbursed_amount=payload.disbursed_amount,
        disbursement_method=payload.disbursement_method,
        disbursed_bank_ref=payload.disbursed_bank_ref,
        disbursement_ref=disbursement_ref,
        interest_rate=payload.interest_rate,
        repayment_frequency=payload.repayment_frequency,
        schedule_method=payload.schedule_method,
    )
    if not app:
        raise HTTPException(status_code=409, detail="Application is not ready for disbursement")
    from app.services.loan_servicing_service import LoanServicingService
    service = LoanServicingService(conn)
    await service.create_schedule(
        loan_id=application_id, org_id=current_user.org_id,
        principal=payload.disbursed_amount, annual_rate=payload.interest_rate,
        tenor_months=app.tenor_months or 12, frequency=payload.repayment_frequency,
        method=payload.schedule_method, disbursement_date=payload.payment_date,
    )
    await AuditService(conn).log(
        application_id=str(application_id), org_id=str(current_user.org_id),
        action="Disbursement Recorded", from_stage="disbursement_ready", to_stage="disbursed",
        actor_id=str(current_user.id), actor_role=current_user.role,
    )
    schedule = await service.get_schedule(loan_id=application_id, org_id=current_user.org_id)
    return {
        "application": _mobile_application(app, current_user),
        "disbursement_ref": disbursement_ref,
        "schedule": schedule,
    }


# ---------------------------------------------------------------------------
# Repayment schedule + payments
# ---------------------------------------------------------------------------

@router.get("/applications/{application_id}/repayment-schedule")
@cache_response(ttl_seconds=60, application_scoped=True)
async def get_mobile_repayment_schedule(
    application_id: UUID,
    conn=Depends(db_conn),
    current_user=Depends(get_current_user),
):
    await _get_application_or_404(conn, application_id, current_user)
    from app.services.loan_servicing_service import LoanServicingService
    svc = LoanServicingService(conn)
    schedule = await svc.get_schedule(loan_id=application_id, org_id=current_user.org_id)
    payments = await svc.get_payments(loan_id=application_id, org_id=current_user.org_id)
    total_due = sum(float(r.get("total_due", 0)) for r in schedule)
    total_paid = sum(float(p.get("amount_paid", 0)) for p in payments)
    return {
        "schedule": schedule,
        "payments": payments,
        "total_due": total_due,
        "total_paid": total_paid,
        "outstanding": max(0.0, total_due - total_paid),
    }


class RecordPaymentRequest(BaseModel):
    amount_paid: float
    channel: Literal["cash", "bank_transfer", "pos", "mobile_money", "other"] = "cash"
    bank_ref: str | None = None
    payment_date: str | None = None


@router.post("/applications/{application_id}/payments")
async def record_mobile_payment(
    application_id: UUID,
    payload: RecordPaymentRequest,
    conn=Depends(db_conn),
    current_user=Depends(get_current_user),
):
    _ensure_roles(current_user, {"crm"})
    await _get_application_or_404(conn, application_id, current_user)
    from app.services.loan_servicing_service import LoanServicingService
    from datetime import date
    payment_date = date.today()
    if payload.payment_date:
        try:
            payment_date = date.fromisoformat(payload.payment_date)
        except ValueError:
            pass
    svc = LoanServicingService(conn)
    record = await svc.record_payment(
        loan_id=application_id,
        org_id=current_user.org_id,
        amount_paid=payload.amount_paid,
        channel=payload.channel,
        bank_ref=payload.bank_ref,
        recorded_by=current_user.id,
        payment_date=payment_date,
    )
    return {"payment": record}


# ---------------------------------------------------------------------------
# PAR dashboard
# ---------------------------------------------------------------------------

@router.get("/reports/par")
@cache_response(ttl_seconds=30)
async def get_mobile_par_dashboard(
    conn=Depends(db_conn),
    current_user=Depends(get_current_user),
):
    _ensure_roles(current_user, {"crm", "head_crm", "md", "ed", "auditor"})
    from app.services.loan_servicing_service import LoanServicingService
    svc = LoanServicingService(conn)
    par = await svc.get_par_summary(org_id=current_user.org_id)
    loans = await LoanRepository(conn).list_disbursed(org_id=current_user.org_id)
    return {"par": par, "loans": [dict(l) if hasattr(l, 'keys') else l for l in loans]}


@router.post("/settings/change-password")
async def change_mobile_password(
    payload: ChangePasswordRequest,
    conn=Depends(db_conn),
    current_user=Depends(get_current_user),
):
    if payload.new_password != payload.confirm_password:
        raise HTTPException(status_code=400, detail="Passwords do not match")
    if payload.new_password == payload.current_password:
        raise HTTPException(status_code=400, detail="New password must differ from the current password")
    from app.domains.auth.repository import AuthRepository
    from app.domains.auth.service import AuthService
    changed = await AuthService(AuthRepository(conn)).change_password(
        str(current_user.id), payload.current_password, payload.new_password
    )
    if not changed:
        raise HTTPException(status_code=400, detail="Current password is incorrect")
    return {"changed": True}


@router.get("/system-activity")
async def get_mobile_system_activity(
    page: int = Query(1, ge=1),
    size: int = Query(50, ge=1, le=100),
    conn=Depends(db_conn),
    current_user=Depends(get_current_user),
):
    _ensure_roles(current_user, {"auditor", "system_admin"})
    rows = await conn.fetch(
        """
        SELECT we.*, u.full_name AS actor_name, COUNT(*) OVER() AS total_count
        FROM workflow_events we
        LEFT JOIN users u ON u.id = we.triggered_by AND u.org_id = we.org_id
        WHERE we.org_id = $1
        ORDER BY we.created_at DESC, we.id DESC
        LIMIT $2 OFFSET $3
        """,
        current_user.org_id, size, (page - 1) * size,
    )
    total = int(rows[0]["total_count"]) if rows else 0
    return {
        "items": [
            {key: value for key, value in dict(row).items() if key != "total_count"}
            for row in rows
        ],
        "page": page, "size": size, "total": total,
    }


@router.get("/queues/legal")
async def get_mobile_legal_queue(
    page: int = Query(1, ge=1),
    size: int = Query(50, ge=1, le=100),
    conn=Depends(db_conn),
    current_user=Depends(get_current_user),
):
    _ensure_roles(current_user, {"legal"})
    rows = await conn.fetch(
        """
        SELECT la.id, la.ref_no, la.applicant_name, la.amount, la.stage, la.updated_at,
               u.full_name AS officer_name, bm.full_name AS branch_manager_name,
               COUNT(*) OVER() AS total_count
        FROM loan_applications la
        LEFT JOIN users u ON u.id = la.created_by AND u.org_id = la.org_id
        LEFT JOIN users bm ON bm.id = la.branch_manager_id AND bm.org_id = la.org_id
        WHERE la.org_id = $1
          AND la.stage IN ('branch_manager_review', 'credit_analyst_review', 'crm_review')
          AND la.deleted_at IS NULL
        ORDER BY la.updated_at ASC
        LIMIT $2 OFFSET $3
        """,
        current_user.org_id, size, (page - 1) * size,
    )
    total = int(rows[0]["total_count"]) if rows else 0
    return {
        "items": [{k: v for k, v in dict(row).items() if k != "total_count"} for row in rows],
        "page": page, "size": size, "total": total,
    }


@router.get("/applications/{application_id}/valuation")
async def get_mobile_valuation(
    application_id: UUID,
    conn=Depends(db_conn),
    current_user=Depends(get_current_user),
):
    app = await _get_application_or_404(conn, application_id, current_user)
    _ensure_roles(current_user, {"legal"})
    items = await conn.fetch(
        """
        SELECT id, item_number, item_name, serial_number, description, estimated_value,
               appraised_value, valuer_name, valuer_license_no, valuation_date,
               loan_to_value_ratio
        FROM pledged_items
        WHERE loan_id = $1
        ORDER BY item_number
        """,
        application_id,
    )
    return {"application": _mobile_application(app, current_user), "items": [dict(item) for item in items]}


@router.put("/applications/{application_id}/valuation")
async def update_mobile_valuation(
    application_id: UUID,
    payload: ValuationRequest,
    conn=Depends(db_conn),
    current_user=Depends(get_current_user),
):
    app = await _get_application_or_404(conn, application_id, current_user)
    _ensure_roles(current_user, {"legal"})
    updated = []
    for item in payload.items:
        value = Decimal(str(item.appraised_value))
        ltv = Decimal(str(app.amount)) / value if value > 0 and app.amount else None
        row = await conn.fetchrow(
            """
            UPDATE pledged_items
            SET appraised_value=$1, valuer_name=$2, valuer_license_no=$3,
                valuation_date=$4, loan_to_value_ratio=$5
            WHERE id=$6 AND loan_id=$7
            RETURNING id, item_number, item_name, appraised_value, valuer_name,
                      valuer_license_no, valuation_date, loan_to_value_ratio
            """,
            value, item.valuer_name, item.valuer_license_no, item.valuation_date,
            ltv, item.item_id, application_id,
        )
        if not row:
            raise HTTPException(status_code=404, detail=f"Pledged item {item.item_id} was not found")
        updated.append(dict(row))
    await AuditService(conn).log(
        application_id=str(application_id), org_id=str(current_user.org_id),
        action="Collateral Valuation Recorded", from_stage=app.stage, to_stage=app.stage,
        actor_id=str(current_user.id), actor_role=current_user.role,
        reason=f"{len(updated)} pledged item(s) valued",
    )
    return {"items": updated}


@router.get("/mcc")
async def get_mobile_mcc_queue(
    page: int = Query(1, ge=1), size: int = Query(50, ge=1, le=100),
    conn=Depends(db_conn), current_user=Depends(get_current_user),
):
    _ensure_roles(current_user, {"ed", "md", "branch_manager", "branch_supervisor", "credit_analyst", "crm", "head_crm", "auditor", "system_admin", "account_officer", "loan_officer"})
    rows = await conn.fetch(
        """SELECT id, ref_no, applicant_name, amount, stage, updated_at, COUNT(*) OVER() AS total_count
           FROM loan_applications WHERE org_id=$1 AND deleted_at IS NULL
             AND stage IN ('ed_approval','md_approval')
           ORDER BY updated_at DESC LIMIT $2 OFFSET $3""",
        current_user.org_id, size, (page - 1) * size,
    )
    total = int(rows[0]["total_count"]) if rows else 0
    return {"items": [{k: v for k, v in dict(row).items() if k != "total_count"} for row in rows],
            "page": page, "size": size, "total": total}


@router.get("/applications/{application_id}/mcc")
async def get_mobile_mcc_application(
    application_id: UUID, conn=Depends(db_conn), current_user=Depends(get_current_user),
):
    _ensure_roles(current_user, {"ed", "md", "branch_manager", "branch_supervisor", "credit_analyst", "crm", "head_crm", "auditor", "system_admin", "account_officer", "loan_officer"})
    app = await _get_application_or_404(conn, application_id, current_user, enforce_officer_scope=False)
    if app.stage not in {"ed_approval", "md_approval"}:
        raise HTTPException(status_code=409, detail="This dossier is not available for MCC review")
    votes = await conn.fetch(
        """SELECT cv.id, cv.member_id, u.full_name AS member_name, cv.recommendation,
                  cv.recommended_amount, cv.notes, cv.created_at
           FROM committee_votes cv JOIN users u ON u.id=cv.member_id AND u.org_id=cv.org_id
           WHERE cv.loan_id=$1 AND cv.org_id=$2 ORDER BY cv.created_at""",
        application_id, current_user.org_id,
    )
    valuation = await conn.fetch("SELECT * FROM pledged_items WHERE loan_id=$1 ORDER BY item_number", application_id)
    return {"application": _mobile_application(app, current_user), "votes": [dict(v) for v in votes],
            "valuation": [dict(item) for item in valuation]}


@router.post("/applications/{application_id}/mcc-vote", status_code=status.HTTP_201_CREATED)
async def submit_mobile_mcc_vote(
    application_id: UUID, payload: MccVoteRequest,
    conn=Depends(db_conn), current_user=Depends(get_current_user),
):
    _ensure_roles(current_user, {"ed", "md", "branch_manager", "branch_supervisor", "credit_analyst", "crm", "head_crm", "auditor", "system_admin", "account_officer", "loan_officer"})
    app = await _get_application_or_404(conn, application_id, current_user, enforce_officer_scope=False)
    if app.stage not in {"ed_approval", "md_approval"}:
        raise HTTPException(status_code=409, detail="This dossier is not available for MCC voting")
    try:
        vote = await conn.fetchrow(
            """INSERT INTO committee_votes
                   (loan_id, org_id, member_id, recommendation, notes, recommended_amount)
               VALUES ($1,$2,$3,'approve',$4,$5) RETURNING *""",
            application_id, current_user.org_id, current_user.id,
            payload.notes.strip(), Decimal(str(payload.recommended_amount)),
        )
    except Exception as exc:
        raise HTTPException(status_code=409, detail="You have already submitted an MCC recommendation") from exc
    return {"vote": dict(vote)}


@router.post("/applications/{application_id}/mcc-finalize")
async def finalize_mobile_mcc(
    application_id: UUID, payload: MccFinalizeRequest,
    conn=Depends(db_conn), current_user=Depends(get_current_user),
):
    _ensure_roles(current_user, {"ed", "md"})
    await _get_application_or_404(conn, application_id, current_user, enforce_officer_scope=False)
    row = await conn.fetchrow(
        """UPDATE loan_applications SET amount=$1, mcc_finalized_by=$2,
                  mcc_finalized_at=NOW(), updated_at=NOW()
           WHERE id=$3 AND org_id=$4 AND stage IN ('ed_approval','md_approval') RETURNING *""",
        Decimal(str(payload.final_amount)), current_user.id, application_id, current_user.org_id,
    )
    if not row:
        raise HTTPException(status_code=409, detail="Final MCC amount could not be set")
    return {"application": _mobile_application(
        await LoanRepository(conn).get_by_id(application_id, current_user.org_id), current_user)}


@router.get("/admin/interest-presets")
async def get_mobile_interest_presets(
    conn=Depends(db_conn),
    current_user=Depends(get_current_user),
):
    _ensure_roles(current_user, {"system_admin"})
    rows = await conn.fetch(
        """
        SELECT id, loan_type, rate, rate_type, effective_from, set_at
        FROM interest_rate_presets
        ORDER BY set_at DESC
        """
    )
    return {"items": [dict(row) for row in rows]}


@router.post("/admin/interest-presets", status_code=status.HTTP_201_CREATED)
async def create_mobile_interest_preset(
    payload: InterestPresetRequest,
    conn=Depends(db_conn),
    current_user=Depends(get_current_user),
):
    _ensure_roles(current_user, {"system_admin"})
    row = await conn.fetchrow(
        """
        INSERT INTO interest_rate_presets (loan_type, rate, rate_type, set_by)
        VALUES ($1,$2,$3,$4)
        RETURNING id, loan_type, rate, rate_type, effective_from, set_at
        """,
        payload.loan_type, Decimal(str(payload.rate)), payload.rate_type, current_user.id,
    )
    return dict(row)


@router.delete("/admin/interest-presets/{preset_id}")
async def delete_mobile_interest_preset(
    preset_id: UUID,
    conn=Depends(db_conn),
    current_user=Depends(get_current_user),
):
    _ensure_roles(current_user, {"system_admin"})
    result = await conn.execute(
        """
        DELETE FROM interest_rate_presets
        WHERE id=$1 AND set_by IN (SELECT id FROM users WHERE org_id=$2)
        """,
        preset_id, current_user.org_id,
    )
    if result != "DELETE 1":
        raise HTTPException(status_code=404, detail="Interest preset not found")
    return {"deleted": str(preset_id)}


@router.put("/admin/interest-presets/{preset_id}")
async def update_mobile_interest_preset(
    preset_id: UUID,
    payload: InterestPresetRequest,
    conn=Depends(db_conn),
    current_user=Depends(get_current_user),
):
    _ensure_roles(current_user, {"system_admin"})
    row = await conn.fetchrow(
        """
        UPDATE interest_rate_presets
        SET loan_type=$1, rate=$2, rate_type=$3, set_by=$4, set_at=NOW()
        WHERE id=$5 AND set_by IN (SELECT id FROM users WHERE org_id=$6)
        RETURNING id, loan_type, rate, rate_type, effective_from, set_at
        """,
        payload.loan_type, Decimal(str(payload.rate)), payload.rate_type,
        current_user.id, preset_id, current_user.org_id,
    )
    if not row:
        raise HTTPException(status_code=404, detail="Interest preset not found")
    return dict(row)


@router.get("/branches")
async def get_mobile_branches(
    conn=Depends(db_conn),
    current_user=Depends(get_current_user),
):
    return {"items": await BranchRepository(conn).list_by_org(current_user.org_id)}


@router.post("/branches", status_code=status.HTTP_201_CREATED)
async def create_mobile_branch(
    payload: BranchRequest,
    conn=Depends(db_conn),
    current_user=Depends(get_current_user),
):
    _ensure_roles(current_user, {"system_admin"})
    return await BranchRepository(conn).create(current_user.org_id, payload.name, payload.code)


@router.put("/users/{user_id}/role")
async def update_mobile_user_role(
    user_id: UUID,
    payload: UserRoleRequest,
    conn=Depends(db_conn),
    current_user=Depends(get_current_user),
):
    _ensure_roles(current_user, {"system_admin"})
    service = UserService(UserRepository(conn))
    user = await service.update_user_role(current_user, user_id, payload.role)
    if payload.branch_id is not None:
        user = await service.update_user_branch(current_user, user_id, payload.branch_id)
    return {"id": user.id, "role": user.role, "branch_id": user.branch_id}


@router.post("/users/{user_id}/deactivate")
async def deactivate_mobile_user(
    user_id: UUID,
    conn=Depends(db_conn),
    current_user=Depends(get_current_user),
):
    _ensure_roles(current_user, {"system_admin"})
    await UserService(UserRepository(conn)).deactivate_managed_user(current_user, user_id)
    return {"id": user_id, "active": False}


@router.get("/reports/par/loans")
async def get_mobile_par_loans(
    page: int = Query(1, ge=1),
    size: int = Query(25, ge=1, le=100),
    conn=Depends(db_conn),
    current_user=Depends(get_current_user),
):
    _ensure_roles(current_user, {"crm", "head_crm", "ed", "md", "auditor"})
    loans, total = await LoanRepository(conn).list_disbursed_page(
        current_user.org_id, limit=size, offset=(page - 1) * size
    )
    return {
        "items": [_mobile_application(loan, current_user) for loan in loans],
        "page": page, "size": size, "total": total,
    }


@router.get("/dashboards/{role_name}")
async def get_mobile_role_dashboard(
    role_name: str,
    conn=Depends(db_conn),
    current_user=Depends(get_current_user),
):
    current_role = _role(current_user)
    if role_name != current_role and current_role != "system_admin":
        raise HTTPException(status_code=403, detail="You may only access your assigned dashboard")
    data = await DashboardService(conn).get_dashboard_data(current_user)
    return {"role": current_role, "metrics": _mobile_dashboard_metrics(data), "data": data}


@router.post("/generate-share-link")
async def generate_share_link_mobile(
    request: Request,
    current_user=Depends(get_current_user),
):
    """Mobile endpoint to generate a client shareable link."""
    _ensure_roles(current_user, {"account_officer"})
    from jose import jwt
    from app.config import settings
    import secrets
    from datetime import datetime, timedelta

    expire = datetime.utcnow() + timedelta(days=7)
    to_encode = {
        "sub": str(current_user.id),
        "org_id": str(current_user.org_id),
        "exp": expire,
        "iat": datetime.utcnow(),
        "type": "client_intake",
        "random_salt": secrets.token_hex(8)
    }
    token = jwt.encode(to_encode, settings.JWT_SECRET_KEY, algorithm=settings.JWT_ALGORITHM)
    
    base_url = str(request.base_url).rstrip("/")
    share_url = f"{base_url}/share-intake/{token}"
    return {"share_url": share_url, "token": token}


# ---------------------------------------------------------------------------
# Admin: User Management
# ---------------------------------------------------------------------------

class MobileCreateUserRequest(BaseModel):
    full_name: str
    email: str
    role: str
    password: str = Field(..., min_length=8)


@router.get("/users")
@cache_response(ttl_seconds=5 * 60)
async def list_mobile_users(
    conn=Depends(db_conn),
    current_user=Depends(get_current_user),
):
    _ensure_roles(current_user, {"system_admin"})
    rows = await conn.fetch(
        """
        SELECT u.id, u.full_name, u.email, u.role, u.active, u.created_at,
               u.last_login_at, o.name AS organization_name, b.name AS branch_name
        FROM users u
        JOIN organisations o ON o.id=u.org_id
        LEFT JOIN branches b ON b.id=u.branch_id AND b.org_id=u.org_id
        WHERE u.org_id = $1
        ORDER BY u.active DESC, u.role ASC, u.full_name ASC
        """,
        current_user.org_id,
    )
    return [
        {
            "id": str(r["id"]),
            "full_name": r["full_name"],
            "email": r["email"],
            "role": r["role"],
            "display_role": {
                "loan_officer": "Relationship Officer",
                "branch_manager": "Team Lead",
                "auditor": "Audit",
                "system_admin": "System Admin",
                "crm": "CRM Officer",
                "md": "Managing Director",
                "ed": "Executive Director",
            }.get(r["role"], r["role"].replace("_", " ").title()),
            "active": r["active"],
            "organization_name": r["organization_name"],
            "branch_name": r["branch_name"],
            "last_activity_at": r["last_login_at"],
        }
        for r in rows
    ]


@router.post("/users", status_code=status.HTTP_201_CREATED)
async def create_mobile_user(
    payload: MobileCreateUserRequest,
    conn=Depends(db_conn),
    current_user=Depends(get_current_user),
):
    _ensure_roles(current_user, {"system_admin"})
    from app.domains.users.service import UserService
    from app.domains.users.repository import UserRepository
    from app.domains.users.schemas import UserCreate
    from app.core.exceptions import DomainException

    try:
        svc = UserService(UserRepository(conn))
        user = await svc.register_user(
            current_admin=current_user,
            user_in=UserCreate(
                org_id=str(current_user.org_id),
                full_name=payload.full_name,
                email=payload.email,
                role=payload.role.lower().replace(" ", "_"),
                password=payload.password,
            ),
        )
        return {
            "id": str(user.id),
            "full_name": user.full_name,
            "email": user.email,
            "role": user.role,
        }
    except DomainException as exc:
        raise HTTPException(status_code=exc.status_code, detail=str(exc))
