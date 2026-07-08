import json
from typing import Any, Literal
from uuid import UUID

from fastapi import APIRouter, Depends, File, Form, HTTPException, Path, Query, UploadFile, status, Request
from pydantic import BaseModel, Field

from app.core.audit import AuditService
from app.core.database import db_conn
from app.core.dependencies import get_current_user
from app.domains.documents.repository import DocumentRepository
from app.domains.documents.service import DocumentService
from app.domains.guarantors.repository import GuarantorRepository
from app.domains.guarantors.service import GuarantorService
from app.domains.loans.repository import LoanRepository
from app.domains.loans.service import LoanService
from app.domains.notifications.repository import NotificationRepository
from app.domains.notifications.service import NotificationService
from app.domains.visitation.repository import VisitationRepository
from app.domains.visitation.service import VisitationService
from app.services.dashboard_service import DashboardService


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


def _role(user) -> str:
    return user.role.lower().replace(" ", "_")


def _mobile_role(user) -> str:
    role = _role(user)
    role_map = {
        "admin_mcr": "system_admin",
        "mcr":       "system_admin",
        "admin":     "system_admin",
    }
    return role_map.get(role, role)


def _stage_number(stage: str | None) -> int:
    return {
        "intake": 1,
        "ocr_review": 2,
        "credit_review": 3,
        "branch_approval": 4,
        "crm_review": 5,
        "committee_review": 6,
        "ed_approval": 7,
        "md_approval": 8,
        "executive_approval": 7,
        "disbursement_ready": 9,
        "disbursed": 10,
        "returned": 11,
        "rejected": 12,
    }.get(stage or "intake", 1)


def _stage_status(stage: str | None) -> str:
    return {
        "intake": "Draft",
        "ocr_review": "OCR Review",
        "credit_review": "Credit Review",
        "branch_approval": "Branch Approval",
        "crm_review": "CRM Review",
        "committee_review": "Committee Review",
        "ed_approval": "ED Approval",
        "md_approval": "MD Approval",
        "executive_approval": "Executive Approval",
        "disbursement_ready": "Disbursement Ready",
        "disbursed": "Disbursed",
        "returned": "Returned",
        "rejected": "Rejected",
    }.get(stage or "intake", "Draft")


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
        "pending_sync": 0,
        "visits_due": metrics.get("visits_due", 0),
        "missing_docs": metrics.get("pending_upload", 0),
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
        "mcr_disbursed": float(metrics.get("mcr_disbursed", 0) or 0),
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


async def _get_application_or_404(conn, application_id: UUID, current_user):
    repo = LoanRepository(conn)
    app = await repo.get_by_id(application_id, current_user.org_id)
    if not app:
        raise HTTPException(status_code=404, detail="Loan Application not found")
    return app


def _ensure_intake_writer(app, current_user) -> None:
    user_role = _role(current_user)
    if user_role not in ("system_admin", "loan_officer"):
        raise HTTPException(status_code=403, detail="Insufficient permissions for this action")
    if user_role == "loan_officer" and app.created_by != current_user.id:
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


@router.get("/queues/{queue_name}")
async def get_mobile_queue(
    queue_name: Literal[
        "loan-officer",
        "visits-due",
        "awaiting-concurrence",
        "pending-signoffs",
        "credit-reviews",
        "ocr-exceptions",
        "compliance-flags",
        "system-control",
        "crm-review",
        "committee-review",
        "ed-approval",
        "md-approval",
        "executive-approval",
    ],
    stage: str | None = None,
    limit: int = Query(50, ge=1, le=100),
    offset: int = Query(0, ge=0),
    conn=Depends(db_conn),
    current_user=Depends(get_current_user),
):
    dashboard = DashboardService(conn)
    if queue_name == "loan-officer":
        _ensure_roles(current_user, {"loan_officer", "system_admin"})
        items = await dashboard.get_loan_officer_queue(current_user, stage=_stage_from_query(stage), limit=limit, offset=offset)
    elif queue_name == "visits-due":
        _ensure_roles(current_user, {"loan_officer", "system_admin"})
        items = await dashboard.get_visits_due_today(current_user)
    elif queue_name == "awaiting-concurrence":
        _ensure_roles(current_user, {"branch_manager", "system_admin"})
        items = await dashboard.get_awaiting_concurrence(current_user, limit=limit, offset=offset)
    elif queue_name == "pending-signoffs":
        _ensure_roles(current_user, {"branch_manager", "system_admin"})
        items = await dashboard.get_pending_signoffs(current_user, limit=limit, offset=offset)
    elif queue_name == "credit-reviews":
        _ensure_roles(current_user, {"branch_manager", "system_admin"})
        items = await dashboard.get_credit_reviews(current_user, limit=limit, offset=offset)
    elif queue_name == "ocr-exceptions":
        _ensure_roles(current_user, {"branch_manager", "system_admin"})
        items = await dashboard.get_credit_ocr_exceptions(current_user, limit=limit, offset=offset)
    elif queue_name == "compliance-flags":
        _ensure_roles(current_user, {"auditor", "system_admin"})
        items = await dashboard.get_compliance_flags(current_user, limit=limit, offset=offset)
    elif queue_name == "crm-review":
        _ensure_roles(current_user, {"crm", "system_admin"})
        items = await dashboard.get_crm_queue(current_user)
    elif queue_name == "committee-review":
        _ensure_roles(current_user, {"committee", "system_admin"})
        items = await dashboard.get_committee_queue(current_user)
    elif queue_name == "ed-approval":
        _ensure_roles(current_user, {"ed", "system_admin"})
        items = await dashboard.get_ed_queue(current_user)
    elif queue_name == "md-approval":
        _ensure_roles(current_user, {"md", "system_admin"})
        items = await dashboard.get_md_queue(current_user)
    elif queue_name == "executive-approval":
        _ensure_roles(current_user, {"md", "ed", "system_admin"})
        items = await dashboard.get_executive_queue(current_user)
    else:
        _ensure_roles(current_user, {"system_admin"})
        items = await dashboard.get_system_control_queue(current_user, limit=limit, offset=offset)

    return {"queue": queue_name, "items": items}


@router.get("/borrowers")
async def list_mobile_borrowers(
    page: int = Query(1, ge=1),
    size: int = Query(50, ge=1, le=100),
    conn=Depends(db_conn),
    current_user=Depends(get_current_user),
):
    repo = LoanRepository(conn)
    officer_id = current_user.id if _role(current_user) == "loan_officer" else None
    applications, total = await repo.list_by_stage(
        org_id=current_user.org_id,
        stage=None,
        officer_id=officer_id,
        page=page,
        size=size,
    )
    seen: set[str] = set()
    items = []
    for app in applications:
        full_app = await repo.get_by_id(app.id, current_user.org_id)
        borrower_id = str(full_app.id if full_app else app.id)
        if borrower_id in seen:
            continue
        seen.add(borrower_id)
        items.append(_mobile_borrower(full_app or app, current_user))
    return {
        "items": items,
        "total": total,
        "page": page,
        "size": size,
    }


@router.post("/borrowers", status_code=status.HTTP_201_CREATED)
async def create_mobile_borrower(
    payload: MobileBorrowerRequest,
    conn=Depends(db_conn),
    current_user=Depends(get_current_user),
):
    _ensure_roles(current_user, {"loan_officer", "system_admin"})
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
async def list_mobile_applications(
    stage: str | None = None,
    page: int = Query(1, ge=1),
    size: int = Query(50, ge=1, le=100),
    conn=Depends(db_conn),
    current_user=Depends(get_current_user),
):
    repo = LoanRepository(conn)
    officer_id = current_user.id if _role(current_user) == "loan_officer" else None
    applications, total = await repo.list_by_stage(
        org_id=current_user.org_id,
        stage=_stage_from_query(stage),
        officer_id=officer_id,
        page=page,
        size=size,
    )
    items = []
    for app in applications:
        full_app = await repo.get_by_id(app.id, current_user.org_id)
        items.append(_mobile_application(full_app or app, current_user))
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
    _ensure_roles(current_user, {"loan_officer", "system_admin"})
    applicant_name = payload.applicant_name
    if payload.borrower_id:
        try:
            borrower_app = await LoanRepository(conn).get_by_id(UUID(payload.borrower_id), current_user.org_id)
            if borrower_app:
                applicant_name = borrower_app.applicant_name
        except ValueError:
            pass
    app = await _loan_service(conn).create_loan(
        org_id=current_user.org_id,
        customer_type=payload.customer_type,
        loan_type=payload.loan_type,
        applicant_name=applicant_name,
        user_id=current_user.id,
    )
    if payload.amount is not None or payload.tenure is not None:
        updated = await LoanRepository(conn).update_intake_details(
            loan_id=app.id,
            org_id=current_user.org_id,
            applicant_name=applicant_name,
            phone=None,
            bvn=None,
            amount=payload.amount,
            tenor_months=payload.tenure,
        )
        app = updated or app
    return {"application": _mobile_application(app, current_user), "next": {"type": "intake_step", "step": 1}}


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
    workflow_events = await LoanRepository(conn).list_workflow_events(current_user.org_id)
    return {
        "application": app,
        "intake": await loan_service.get_wizard_data(application_id),
        "documents": documents,
        "visitation": visitation,
        "readiness": readiness,
        "workflow_events": [dict(event) for event in workflow_events if str(event.get("loan_id")) == str(application_id)],
    }


@router.get("/applications/{application_id}/intake")
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
    step: int = Path(..., ge=1, le=9),
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
    next_step = step + 1 if step < 9 else None
    return {
        "application_id": application_id,
        "stage": "ocr_review" if step == 9 else app.stage,
        "step": step,
        "next_step": next_step,
        "data": updated,
    }


@router.get("/applications/{application_id}/guarantors/{slot}")
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
    return {"document": document}


@router.get("/applications/{application_id}/documents")
async def list_mobile_documents(
    application_id: UUID,
    conn=Depends(db_conn),
    current_user=Depends(get_current_user),
):
    await _get_application_or_404(conn, application_id, current_user)
    documents = await DocumentRepository(conn).get_by_loan(application_id, current_user.org_id)
    return {"items": documents}


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
    _ensure_roles(current_user, {"branch_manager", "system_admin"})
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
                message=f"Branch manager {verb} your visitation report.",
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
    _ensure_roles(current_user, {"loan_officer", "branch_manager", "system_admin"})
    app = await _get_application_or_404(conn, application_id, current_user)
    if payload.recommendation_decision == "Recommend Approval":
        stage = "branch_approval"
    elif payload.recommendation_decision == "Return for Correction":
        stage = "returned"
    else:
        stage = "rejected"

    updated = await LoanRepository(conn).advance_stage(application_id, current_user.org_id, stage)
    await AuditService(conn).log(
        application_id=str(application_id),
        org_id=str(current_user.org_id),
        action="Credit Underwriting Verdict",
        from_stage=app.stage,
        to_stage=stage,
        actor_id=str(current_user.id),
        actor_role=current_user.role,
        reason=payload.recommendation_notes,
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
    conn=Depends(db_conn),
    current_user=Depends(get_current_user),
):
    _ensure_roles(current_user, {"branch_manager", "system_admin"})
    app = await LoanRepository(conn).approve(application_id, current_user.org_id, current_user.id)
    if not app:
        raise HTTPException(status_code=404, detail="Loan Application not found or not in branch_approval stage")
    await AuditService(conn).log(
        application_id=str(application_id),
        org_id=str(current_user.org_id),
        action="Branch Final Approval",
        from_stage="branch_approval",
        to_stage="disbursement_ready",
        actor_id=str(current_user.id),
        actor_role=current_user.role,
    )
    # Notify loan officer of approval
    try:
        created_by = getattr(app, "created_by", None)
        if created_by and created_by != current_user.id:
            await _notification_service(conn).create(
                user_id=created_by,
                org_id=current_user.org_id,
                application_id=application_id,
                title="Application Approved",
                message="Your loan application has been approved and is ready for disbursement.",
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
    await _get_application_or_404(conn, application_id, current_user)
    reason_parts = [payload.reason_category]
    if payload.corrections:
        reason_parts.append("Corrections: " + ", ".join(payload.corrections))
    reason_parts.append(payload.notes)
    return_reason = " | ".join(part for part in reason_parts if part)

    returned = await LoanRepository(conn).mark_returned(
        application_id,
        current_user.org_id,
        return_reason,
        current_user.id,
    )
    if not returned:
        raise HTTPException(status_code=404, detail="Loan Application not found")
    await AuditService(conn).log(
        application_id=str(application_id),
        org_id=str(current_user.org_id),
        action="Return Application",
        from_stage=None,
        to_stage="returned",
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
    row = await conn.fetchrow("SELECT name FROM organisations WHERE id = $1", current_user.org_id)
    org_name = row["name"] if row else "FieldCRM MFB"
    return {
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


@router.get("/search")
async def search_mobile(
    q: str = Query(""),
    conn=Depends(db_conn),
    current_user=Depends(get_current_user),
):
    if len(q.strip()) < 2:
        return {"applications": [], "borrowers": []}
    term = f"%{q.strip()}%"
    role = _role(current_user)
    if role == "loan_officer":
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


@router.get("/applications/{application_id}/audit")
async def get_mobile_audit_trail(
    application_id: UUID,
    conn=Depends(db_conn),
    current_user=Depends(get_current_user),
):
    await _get_application_or_404(conn, application_id, current_user)
    events = await conn.fetch(
        """
        SELECT we.id, we.event_type, we.from_stage, we.to_stage,
               we.triggered_role, we.notes, we.created_at,
               u.full_name AS actor_name
        FROM workflow_events we
        JOIN users u ON u.id = we.triggered_by
        WHERE we.loan_id = $1 AND we.org_id = $2
        ORDER BY we.created_at DESC
        """,
        application_id, current_user.org_id,
    )
    return [
        {
            "id": str(e["id"]),
            "timestamp": e["created_at"].isoformat(),
            "actor_name": e["actor_name"],
            "actor_role": e["triggered_role"],
            "action": e["event_type"],
            "state_diff": f"{e['from_stage'] or '-'} → {e['to_stage'] or '-'}",
            "notes": e["notes"] or "",
            "is_mine": str(e.get("triggered_by", "")) == str(current_user.id),
        }
        for e in events
    ]


@router.get("/applications/{application_id}/bureau")
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


@router.get("/applications/{application_id}/committee-votes")
async def get_mobile_committee_votes(
    application_id: UUID,
    conn=Depends(db_conn),
    current_user=Depends(get_current_user),
):
    await _get_application_or_404(conn, application_id, current_user)
    rows = await conn.fetch(
        """
        SELECT notes FROM workflow_events
        WHERE loan_id = $1 AND org_id = $2 AND event_type = 'committee_vote'
        """,
        application_id, current_user.org_id,
    )
    total = len(rows)
    yes_votes = sum(1 for r in rows if (r["notes"] or "").lower().startswith("yes"))
    return {"yes_votes": yes_votes, "total_votes": total, "quorum": 3}


@router.get("/applications/{application_id}/audit-checklist")
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


@router.patch("/applications/{application_id}/audit-checklist")
async def save_mobile_audit_checklist(
    application_id: UUID,
    payload: AuditChecklistRequest,
    conn=Depends(db_conn),
    current_user=Depends(get_current_user),
):
    _ensure_roles(current_user, {"auditor", "system_admin"})
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
    return [
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
            "answer": "The Debt-to-Income ratio limit is 40%. Applications above this threshold require additional review and committee approval before disbursement.",
        },
        {
            "question": "How do I escalate a compliance flag?",
            "answer": "Navigate to the application audit trail, review workflow events, and use the Report Problem option to escalate to the compliance officer via the platform.",
        },
    ]


_ONBOARDING_SLIDES: dict[str, list[dict]] = {
    "loan_officer": [
        {"title": "Welcome to FieldCRM", "subtitle": "Your Field Operations Hub",
         "body": "Manage loan applications, conduct visits, and process borrower records from your mobile device — anytime, anywhere."},
        {"title": "Create Applications", "subtitle": "Start a New Loan Dossier",
         "body": "Tap the + button on your Work Queue to begin a new loan application. Fill in borrower details step by step and capture documents with OCR scanning."},
        {"title": "Schedule Visits", "subtitle": "Track Your Field Visits",
         "body": "View visits due today on your dashboard. Complete visitation reports in the field and submit them for branch manager sign-off."},
        {"title": "Offline Mode", "subtitle": "Work Without Internet",
         "body": "All your changes are saved locally when offline. When you reconnect, the sync queue automatically uploads completed records to the server."},
    ],
    "branch_manager": [
        {"title": "Manager Dashboard", "subtitle": "Branch Oversight Console",
         "body": "Monitor all applications awaiting your sign-off, review concurrence requests from field officers, and track branch disbursement targets."},
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
         "body": "Review loan files approved by the branch manager. Verify credit file completeness — bureau evidence, CRMS search, NCR registration — before advancing to the Executive."},
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


@router.get("/onboarding")
async def get_mobile_onboarding(
    role: str = Query("loan_officer"),
    current_user=Depends(get_current_user),
):
    mapped = _mobile_role(current_user) if not role else role
    slides = _ONBOARDING_SLIDES.get(mapped, _ONBOARDING_SLIDES["loan_officer"])
    return slides


class ForgotPasswordRequest(BaseModel):
    email: str


class ResetPasswordRequest(BaseModel):
    token: str
    new_password: str


@router.post("/auth/forgot-password")
async def mobile_forgot_password(req: ForgotPasswordRequest, conn=Depends(db_conn)):
    from app.domains.auth.repository import AuthRepository
    from app.domains.auth.service import AuthService
    await AuthService(AuthRepository(conn)).request_password_reset(req.email)
    return {"message": "If that email is registered, a reset link has been sent."}


@router.post("/auth/reset-password")
async def mobile_reset_password(req: ResetPasswordRequest, conn=Depends(db_conn)):
    from app.domains.auth.repository import AuthRepository
    from app.domains.auth.service import AuthService
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


@router.get("/applications/{application_id}/crm-review")
async def get_mobile_crm_review(
    application_id: UUID,
    conn=Depends(db_conn),
    current_user=Depends(get_current_user),
):
    _ensure_roles(current_user, {"crm", "system_admin"})
    app = await _get_application_or_404(conn, application_id, current_user)
    documents = await DocumentRepository(conn).get_by_loan(application_id, current_user.org_id)
    return {"application": app, "documents": documents}


@router.post("/applications/{application_id}/crm-review")
async def submit_mobile_crm_review(
    application_id: UUID,
    payload: CrmReviewRequest,
    conn=Depends(db_conn),
    current_user=Depends(get_current_user),
):
    _ensure_roles(current_user, {"crm", "system_admin"})
    app = await _get_application_or_404(conn, application_id, current_user)
    repo = LoanRepository(conn)
    if payload.decision == "advance":
        updated = await repo.advance_to_committee_review(
            loan_id=application_id,
            org_id=current_user.org_id,
            crm_user_id=current_user.id,
            crm_notes=payload.notes,
        )
        next_stage = "committee_review"
    else:
        updated = await repo.mark_returned(application_id, current_user.org_id, payload.notes, current_user.id)
        next_stage = "returned"
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
    _ensure_roles(current_user, {"md", "ed", "system_admin"})
    app = await _get_application_or_404(conn, application_id, current_user)
    documents = await DocumentRepository(conn).get_by_loan(application_id, current_user.org_id)
    return {"application": app, "documents": documents}


@router.post("/applications/{application_id}/executive-approve")
async def submit_mobile_executive_approve(
    application_id: UUID,
    conn=Depends(db_conn),
    current_user=Depends(get_current_user),
):
    _ensure_roles(current_user, {"md", "ed", "system_admin"})
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
# Committee review endpoints
# ---------------------------------------------------------------------------

class CommitteeVoteRequest(BaseModel):
    recommendation: Literal["approve", "return", "reject"]
    notes: str = ""


class CommitteeCompleteRequest(BaseModel):
    recommendation: Literal["approve", "return", "reject"]


@router.get("/applications/{application_id}/committee-votes-full")
async def get_mobile_committee_votes(
    application_id: UUID,
    conn=Depends(db_conn),
    current_user=Depends(get_current_user),
):
    _ensure_roles(current_user, {"committee", "ed", "md", "system_admin"})
    repo = LoanRepository(conn)
    votes = await repo.get_committee_votes(application_id, current_user.org_id)
    last_loan = await repo.get_last_loan(
        current_user.org_id,
        (await _get_application_or_404(conn, application_id, current_user)).applicant_name,
        None, application_id
    )
    return {"votes": votes, "last_loan": last_loan}


@router.post("/applications/{application_id}/committee-vote")
async def submit_mobile_committee_vote(
    application_id: UUID,
    payload: CommitteeVoteRequest,
    conn=Depends(db_conn),
    current_user=Depends(get_current_user),
):
    _ensure_roles(current_user, {"committee", "system_admin"})
    await _get_application_or_404(conn, application_id, current_user)
    vote = await LoanRepository(conn).insert_committee_vote(
        application_id, current_user.org_id, current_user.id,
        payload.recommendation, payload.notes
    )
    await AuditService(conn).log(
        application_id=str(application_id),
        org_id=str(current_user.org_id),
        action=f"Committee Vote: {payload.recommendation}",
        from_stage="committee_review", to_stage="committee_review",
        actor_id=str(current_user.id), actor_role=current_user.role,
        reason=payload.notes,
    )
    return {"vote": vote}


@router.post("/applications/{application_id}/committee-complete")
async def submit_mobile_committee_complete(
    application_id: UUID,
    payload: CommitteeCompleteRequest,
    conn=Depends(db_conn),
    current_user=Depends(get_current_user),
):
    _ensure_roles(current_user, {"committee", "system_admin"})
    app = await _get_application_or_404(conn, application_id, current_user)
    updated = await LoanRepository(conn).complete_committee_review(
        application_id, current_user.org_id, payload.recommendation
    )
    if not updated:
        raise HTTPException(status_code=409, detail="Application not in committee_review stage")
    await AuditService(conn).log(
        application_id=str(application_id),
        org_id=str(current_user.org_id),
        action=f"Committee Review Complete — {payload.recommendation}",
        from_stage="committee_review", to_stage=updated.stage,
        actor_id=str(current_user.id), actor_role=current_user.role,
    )
    return {"application": updated, "stage": updated.stage}


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
    _ensure_roles(current_user, {"ed", "system_admin"})
    app = await _get_application_or_404(conn, application_id, current_user)
    votes = await LoanRepository(conn).get_committee_votes(application_id, current_user.org_id)
    documents = await DocumentRepository(conn).get_by_loan(application_id, current_user.org_id)
    return {"application": app, "votes": votes, "documents": documents}


@router.post("/applications/{application_id}/ed-approve")
async def submit_mobile_ed_approve(
    application_id: UUID,
    payload: EdApproveRequest,
    conn=Depends(db_conn),
    current_user=Depends(get_current_user),
):
    _ensure_roles(current_user, {"ed", "system_admin"})
    app = await _get_application_or_404(conn, application_id, current_user)
    repo = LoanRepository(conn)
    if payload.action == "approve":
        updated = await repo.ed_approve(application_id, current_user.org_id, current_user.id)
        next_stage = "disbursement_ready"
        action_label = "ED Final Approval — Disbursement Instruction"
    else:
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


@router.get("/applications/{application_id}/md-review")
async def get_mobile_md_review(
    application_id: UUID,
    conn=Depends(db_conn),
    current_user=Depends(get_current_user),
):
    _ensure_roles(current_user, {"md", "system_admin"})
    app = await _get_application_or_404(conn, application_id, current_user)
    repo = LoanRepository(conn)
    votes = await repo.get_committee_votes(application_id, current_user.org_id)
    board_referrals = await repo.get_board_referrals(application_id, current_user.org_id)
    documents = await DocumentRepository(conn).get_by_loan(application_id, current_user.org_id)
    return {"application": app, "votes": votes, "board_referrals": board_referrals, "documents": documents}


@router.post("/applications/{application_id}/md-approve")
async def submit_mobile_md_approve(
    application_id: UUID,
    payload: MdApproveRequest,
    conn=Depends(db_conn),
    current_user=Depends(get_current_user),
):
    _ensure_roles(current_user, {"md", "system_admin"})
    app = await _get_application_or_404(conn, application_id, current_user)
    repo = LoanRepository(conn)
    if payload.action == "approve":
        updated = await repo.md_approve(application_id, current_user.org_id, current_user.id, payload.notes)
        if not updated:
            raise HTTPException(status_code=409, detail="Application not in md_approval stage")
        next_stage = "disbursement_ready"
        action_label = "MD Final Approval — Disbursement Instruction"
    else:
        await repo.md_add_comment(application_id, current_user.org_id, payload.notes)
        next_stage = app.stage
        action_label = "MD Comment Added"
        updated = app
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
    _ensure_roles(current_user, {"md", "system_admin"})
    await _get_application_or_404(conn, application_id, current_user)
    referral = await LoanRepository(conn).insert_board_referral(
        application_id, current_user.org_id, current_user.id,
        payload.board_member_email, payload.board_member_name, payload.notes
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


# ---------------------------------------------------------------------------
# Repayment schedule + payments
# ---------------------------------------------------------------------------

@router.get("/applications/{application_id}/repayment-schedule")
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
    _ensure_roles(current_user, {"crm", "system_admin"})
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
async def get_mobile_par_dashboard(
    conn=Depends(db_conn),
    current_user=Depends(get_current_user),
):
    _ensure_roles(current_user, {"crm", "md", "ed", "auditor", "system_admin"})
    from app.services.loan_servicing_service import LoanServicingService
    svc = LoanServicingService(conn)
    par = await svc.get_par_summary(org_id=current_user.org_id)
    loans = await LoanRepository(conn).list_disbursed(org_id=current_user.org_id)
    return {"par": par, "loans": [dict(l) if hasattr(l, 'keys') else l for l in loans]}


@router.post("/generate-share-link")
async def generate_share_link_mobile(
    request: Request,
    current_user=Depends(get_current_user),
):
    """Mobile endpoint to generate a client shareable link."""
    _ensure_roles(current_user, {"loan_officer", "system_admin"})
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
