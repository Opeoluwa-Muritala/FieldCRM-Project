from typing import Any, Literal
from uuid import UUID

from fastapi import APIRouter, Depends, HTTPException, Path, Query, status
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


class SaveStepRequest(BaseModel):
    data: dict[str, Any] = Field(default_factory=dict)


class SaveGuarantorStepRequest(BaseModel):
    data: dict[str, Any] = Field(default_factory=dict)


class DocumentUploadRequest(BaseModel):
    category: str = "other"


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


def _role(user) -> str:
    return user.role.lower().replace(" ", "_")


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


@router.get("/me", response_model=MobileUserResponse)
async def get_mobile_user(current_user=Depends(get_current_user)):
    return {
        "id": current_user.id,
        "org_id": current_user.org_id,
        "full_name": current_user.full_name,
        "email": current_user.email,
        "role": current_user.role,
        "display_role": current_user.display_role,
    }


@router.get("/dashboard")
async def get_mobile_dashboard(conn=Depends(db_conn), current_user=Depends(get_current_user)):
    data = await DashboardService(conn).get_dashboard_data(current_user)
    return {
        "user": {
            "id": current_user.id,
            "full_name": current_user.full_name,
            "role": current_user.role,
            "display_role": current_user.display_role,
        },
        "data": data,
    }


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
        _ensure_roles(current_user, {"credit_officer", "system_admin"})
        items = await dashboard.get_credit_reviews(current_user, limit=limit, offset=offset)
    elif queue_name == "ocr-exceptions":
        _ensure_roles(current_user, {"credit_officer", "system_admin"})
        items = await dashboard.get_credit_ocr_exceptions(current_user, limit=limit, offset=offset)
    elif queue_name == "compliance-flags":
        _ensure_roles(current_user, {"auditor", "system_admin"})
        items = await dashboard.get_compliance_flags(current_user, limit=limit, offset=offset)
    else:
        _ensure_roles(current_user, {"system_admin"})
        items = await dashboard.get_system_control_queue(current_user, limit=limit, offset=offset)

    return {"queue": queue_name, "items": items}


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
    return {
        "items": applications,
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
    app = await _loan_service(conn).create_loan(
        org_id=current_user.org_id,
        customer_type=payload.customer_type,
        loan_type=payload.loan_type,
        applicant_name=payload.applicant_name,
        user_id=current_user.id,
    )
    return {"application": app, "next": {"type": "intake_step", "step": 1}}


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
    app = await _get_application_or_404(conn, application_id, current_user)
    _ensure_intake_writer(app, current_user)
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
    payload: DocumentUploadRequest,
    conn=Depends(db_conn),
    current_user=Depends(get_current_user),
):
    await _get_application_or_404(conn, application_id, current_user)
    document = await _document_service(conn).save_mock_upload(
        loan_id=application_id,
        org_id=current_user.org_id,
        category=payload.category,
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
        await LoanRepository(conn).advance_stage(application_id, current_user.org_id, "credit_review")
        await AuditService(conn).log(
            application_id=str(application_id),
            org_id=str(current_user.org_id),
            action="Verify OCR Data",
            from_stage=app.stage,
            to_stage="credit_review",
            actor_id=str(current_user.id),
            actor_role=current_user.role,
            reason=str(payload.corrections) if payload.corrections else None,
        )
        return {"application_id": application_id, "stage": "credit_review", "verified": True}
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
    return {"report": report}


@router.post("/applications/{application_id}/credit-review")
async def submit_mobile_credit_review(
    application_id: UUID,
    payload: CreditReviewRequest,
    conn=Depends(db_conn),
    current_user=Depends(get_current_user),
):
    _ensure_roles(current_user, {"credit_officer", "system_admin"})
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
    return {"application": app, "return_reason": return_reason}
