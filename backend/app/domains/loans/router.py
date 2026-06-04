import os
from datetime import datetime
from uuid import UUID
from fastapi import APIRouter, Depends, Form, HTTPException, Request, Response, status
from fastapi.responses import RedirectResponse
from fastapi.templating import Jinja2Templates

from app.core.database import db_conn
from app.domains.loans.repository import LoanRepository
from app.domains.loans.service import LoanService
from app.core.audit import AuditService
from app.core.dependencies import get_current_user, RoleChecker
from app.core.template_utils import (
    build_template_context,
    detect_device_type,
    get_role_template,
)
from app.services.dashboard_service import DashboardService
from app.domains.documents.repository import DocumentRepository
from app.domains.documents.service import DocumentService
from app.domains.guarantors.repository import GuarantorRepository
from app.domains.guarantors.service import GuarantorService
from app.domains.visitation.repository import VisitationRepository
from app.domains.visitation.service import VisitationService

router = APIRouter()

# Resolve templates folder relatively
base_dir = os.path.dirname(os.path.abspath(__file__))
templates_dir = os.path.abspath(os.path.join(base_dir, "../../../../frontend/templates"))
templates = Jinja2Templates(directory=templates_dir)

def form_data_to_jsonable_dict(form_data) -> dict:
    payload = {}
    for key in form_data.keys():
        values = form_data.getlist(key)
        cleaned = []
        for value in values:
            if hasattr(value, "filename"):
                if value.filename:
                    cleaned.append(value.filename)
                continue
            cleaned.append(str(value))

        if not cleaned:
            continue

        normalized_key = key[:-2] if key.endswith("[]") else key
        if key.endswith("[]"):
            existing = payload.get(normalized_key, [])
            existing.extend([v for v in cleaned if v != ""])
            payload[normalized_key] = existing
        else:
            payload[normalized_key] = cleaned[-1]
    return payload

def get_loan_service(conn = Depends(db_conn)) -> LoanService:
    repo = LoanRepository(conn)
    audit = AuditService(conn)
    return LoanService(repo, audit)

def get_document_service(conn = Depends(db_conn)) -> DocumentService:
    return DocumentService(DocumentRepository(conn), AuditService(conn))

def get_guarantor_service(conn = Depends(db_conn)) -> GuarantorService:
    return GuarantorService(GuarantorRepository(conn), LoanRepository(conn), AuditService(conn))

def get_visitation_service(conn = Depends(db_conn)) -> VisitationService:
    return VisitationService(VisitationRepository(conn), AuditService(conn))

@router.get("/dashboard")
async def render_dashboard(
    request: Request,
    conn = Depends(db_conn),
    current_user = Depends(get_current_user)
):
    """Renders the role-specific dashboard.

    Each role gets a genuinely different template and data set.
    Mobile and desktop share the same template but extend different
    base shells via the 'shell' context variable.
    """
    dashboard_svc = DashboardService(conn)
    data = await dashboard_svc.get_dashboard_data(current_user)

    repo = LoanRepository(conn)
    if current_user.role.lower().replace(" ", "_") == "loan_officer":
        applications = data.get("queue", [])
    else:
        applications = await repo.list_recent(current_user.org_id, limit=10)

    # Resolve role-specific template (e.g. "loan_officer/dashboard.html")
    role = current_user.role.lower().replace(" ", "_")
    template_name = get_role_template(role, "dashboard.html")

    ctx = build_template_context(
        request,
        current_user,
        data=data,
        applications=applications,
        metrics=data.get("metrics", {}),
        today_label=datetime.now().strftime("%A, %d %B %Y"),
    )

    # Try role-specific template; fall back to shared dashboard
    try:
        return templates.TemplateResponse(request, template_name, ctx)
    except Exception:
        # Fallback to the existing dashboard.html during migration
        return templates.TemplateResponse(request, "dashboard.html", ctx)

@router.get("/my-queue")
async def render_my_queue(
    request: Request,
    stage: str = None,
    conn = Depends(db_conn),
    current_user = Depends(RoleChecker(["Loan Officer"]))
):
    """Render the loan officer's personal queue for mobile tab bar and desktop sidebar."""
    dashboard_svc = DashboardService(conn)
    queue = await dashboard_svc.get_loan_officer_queue(current_user, stage=stage)
    metrics = (await dashboard_svc.get_dashboard_data(current_user)).get("metrics", {})
    ctx = build_template_context(
        request,
        current_user,
        queue=queue,
        metrics=metrics,
        active_tab="queue",
        active_page="queue",
        today_label=datetime.now().strftime("%A, %d %B %Y"),
    )
    return templates.TemplateResponse(request, "loan_officer/queue.html", ctx)

@router.get("/visits")
async def render_visits_due(
    request: Request,
    conn = Depends(db_conn),
    current_user = Depends(RoleChecker(["Loan Officer"]))
):
    """Render visits due today for field users."""
    dashboard_svc = DashboardService(conn)
    visits = await dashboard_svc.get_visits_due_today(current_user)
    metrics = (await dashboard_svc.get_dashboard_data(current_user)).get("metrics", {})
    ctx = build_template_context(
        request,
        current_user,
        visits=visits,
        metrics=metrics,
        active_tab="visits",
        active_page="visits",
        today_label=datetime.now().strftime("%A, %d %B %Y"),
    )
    return templates.TemplateResponse(request, "loan_officer/visits.html", ctx)

@router.get("/applications")
async def render_applications_list(
    request: Request,
    stage: str = None,
    loan_type: str = None,
    q: str = None,
    from_date: str = None,
    to_date: str = None,
    conn = Depends(db_conn),
    current_user = Depends(get_current_user)
):
    """Renders application queue page with inline filtering."""
    db_stage = None
    if stage and stage != "all":
        stage_map = {
            1: 'intake',
            2: 'ocr_review',
            3: 'credit_review',
            4: 'branch_approval',
            5: 'disbursement_ready',
            6: 'disbursed',
            7: 'returned',
            8: 'rejected'
        }
        try:
            db_stage = stage_map.get(int(stage), stage)
        except ValueError:
            db_stage = stage

    repo = LoanRepository(conn)
    applications, total = await repo.list_by_stage(
        org_id=current_user.org_id,
        stage=db_stage,
        officer_id=None,
        page=1,
        size=100
    )
    return templates.TemplateResponse(
        request,
            "shared/applications.html",
        {
            "current_user": current_user,
            "applications": applications,
            "current_stage": stage,
            "current_loan_type": loan_type,
            "search_query": q,
            "from_date": from_date,
            "to_date": to_date
        }
    )

@router.get("/applications/new")
async def render_new_application(
    request: Request,
    current_user = Depends(RoleChecker(["System Admin", "Loan Officer"]))
):
    """Renders Page 3 customer selection page."""
    return templates.TemplateResponse(
        request,
        "shared/new_application.html",
        {"current_user": current_user}
    )

@router.post("/applications/new")
async def process_new_application(
    request: Request,
    customer_type: str = Form(...),
    loan_type: str = Form(...),
    service: LoanService = Depends(get_loan_service),
    current_user = Depends(RoleChecker(["System Admin", "Loan Officer"]))
):
    """Initializes a new borrower and loan application in draft stage."""
    app = await service.create_loan(
        org_id=current_user.org_id,
        customer_type=customer_type,
        loan_type=loan_type,
        applicant_name="New Applicant",
        user_id=current_user.id
    )
    return RedirectResponse(url=f"/applications/{app.id}/step/1", status_code=status.HTTP_303_SEE_OTHER)

@router.get("/applications/{application_id}")
async def redirect_application_detail(
    application_id: str,
    conn = Depends(db_conn),
    current_user = Depends(get_current_user)
):
    """Routes detail request to correct wizard step or review page depending on current owner/stage."""
    repo = LoanRepository(conn)
    app = await repo.get_by_id(UUID(application_id), current_user.org_id)
    if not app:
        raise HTTPException(status_code=404, detail="Loan Application not found")
        
    if app.current_stage == 1:
        return RedirectResponse(url=f"/applications/{application_id}/step/1", status_code=status.HTTP_303_SEE_OTHER)
    elif app.current_stage == 2:
        return RedirectResponse(url=f"/applications/{application_id}/ocr-review", status_code=status.HTTP_303_SEE_OTHER)
    elif app.current_stage == 3:
        return RedirectResponse(url=f"/applications/{application_id}/credit-review", status_code=status.HTTP_303_SEE_OTHER)
    else:
        return RedirectResponse(url=f"/applications/{application_id}/approve", status_code=status.HTTP_303_SEE_OTHER)

@router.get("/applications/{application_id}/step/{step}")
async def render_wizard_step(
    request: Request,
    application_id: str,
    step: int,
    service: LoanService = Depends(get_loan_service),
    conn = Depends(db_conn),
    current_user = Depends(get_current_user)
):
    """GET handler for Borrower intake wizard steps 1 to 9."""
    repo = LoanRepository(conn)
    app = await repo.get_by_id(UUID(application_id), current_user.org_id)
    if not app:
        raise HTTPException(status_code=404, detail="Loan Application not found")
        
    data = await service.get_wizard_data(UUID(application_id))
    
    if step == 2 and data.get("marital_status") == "Single":
        return RedirectResponse(url=f"/applications/{application_id}/step/3", status_code=status.HTTP_303_SEE_OTHER)

    return templates.TemplateResponse(
        request,
        "shared/application_wizard.html",
        {
            "current_user": current_user,
            "app_id": application_id,
            "step": step,
            "data": data
        }
    )

@router.post("/applications/{application_id}/step/{step}")
async def process_wizard_step(
    request: Request,
    application_id: str,
    step: int,
    service: LoanService = Depends(get_loan_service),
    current_user = Depends(get_current_user)
):
    """POST handler to persist wizard values and advance flow."""
    form_data = await request.form()
    data_dict = form_data_to_jsonable_dict(form_data)
    await service.save_wizard_step(UUID(application_id), step, data_dict, current_user.id, current_user.org_id)

    if step < 9:
        next_step = step + 1
        return RedirectResponse(url=f"/applications/{application_id}/step/{next_step}", status_code=status.HTTP_303_SEE_OTHER)
    else:
        return RedirectResponse(url=f"/applications/{application_id}/ocr-review", status_code=status.HTTP_303_SEE_OTHER)

@router.get("/applications/{application_id}/guarantors/{guarantor_index}/step/{step}")
async def render_guarantor_step(
    request: Request,
    application_id: str,
    guarantor_index: int,
    step: int,
    current_user = Depends(get_current_user)
):
    """GET handler for Guarantor intake flow steps 1 to 8."""
    return templates.TemplateResponse(
        request,
        "shared/guarantor_wizard.html",
        {
            "current_user": current_user,
            "app_id": application_id,
            "guarantor_index": guarantor_index,
            "step": step,
            "data": {}
        }
    )

@router.post("/applications/{application_id}/guarantors/{guarantor_index}/step/{step}")
async def process_guarantor_step(
    request: Request,
    application_id: str,
    guarantor_index: int,
    step: int,
    service: GuarantorService = Depends(get_guarantor_service),
    current_user = Depends(get_current_user)
):
    """POST handler for Guarantor flow."""
    if step < 8:
        return RedirectResponse(url=f"/applications/{application_id}/guarantors/{guarantor_index}/step/{step + 1}", status_code=status.HTTP_303_SEE_OTHER)
    else:
        await service.mark_slot_submitted(
            loan_id=UUID(application_id),
            org_id=current_user.org_id,
            slot=guarantor_index,
            submitted_by=current_user.id,
            user_role=current_user.role,
        )
        return RedirectResponse(url=f"/applications/{application_id}/step/3", status_code=status.HTTP_303_SEE_OTHER)

@router.get("/applications/{application_id}/documents/upload")
async def render_document_upload(
    request: Request,
    application_id: str,
    type: str = "other",
    conn = Depends(db_conn),
    current_user = Depends(get_current_user)
):
    """Page 13 Document Upload page."""
    repo = LoanRepository(conn)
    app = await repo.get_by_id(UUID(application_id), current_user.org_id)
    borrower_name = app.applicant_name if app else "Borrower"
    return templates.TemplateResponse(
        request,
        "shared/upload_document.html",
        {
            "current_user": current_user,
            "app_id": application_id,
            "doc_type": type,
            "borrower_name": borrower_name
        }
    )

@router.post("/applications/{application_id}/documents/upload")
async def process_document_upload(
    application_id: str,
    type: str,
    category: str = Form("other"),
    service: DocumentService = Depends(get_document_service),
    current_user = Depends(get_current_user)
):
    """POST handler to store documents on server side."""
    await service.save_mock_upload(
        loan_id=UUID(application_id),
        org_id=current_user.org_id,
        category=category,
        uploaded_by=current_user.id,
        user_role=current_user.role,
    )
    return RedirectResponse(url=f"/applications/{application_id}", status_code=status.HTTP_303_SEE_OTHER)

@router.get("/applications/{application_id}/ocr-review")
async def render_ocr_review(
    request: Request,
    application_id: str,
    doc: str = "loan",
    current_user = Depends(get_current_user)
):
    """Page 14 OCR Review Page."""
    return templates.TemplateResponse(
        request,
        "shared/ocr_review.html",
        {
            "current_user": current_user,
            "app_id": application_id,
            "doc_type": doc,
            "doc_name": "Loan Application Form" if doc == 'loan' else "Pledge Receipt Form"
        }
    )

@router.post("/applications/{application_id}/ocr-review")
async def process_ocr_review(
    application_id: str,
    action: str = Form(...),
    conn = Depends(db_conn),
    current_user = Depends(get_current_user)
):
    """POST processor for OCR validation overrides."""
    repo = LoanRepository(conn)
    app = await repo.get_by_id(UUID(application_id), current_user.org_id)
    if not app:
        raise HTTPException(status_code=404, detail="Loan Application not found")
        
    if action == "verify":
        await repo.advance_stage(UUID(application_id), current_user.org_id, "credit_review")
        
        audit = AuditService(conn)
        await audit.log(
            application_id=application_id,
            org_id=str(current_user.org_id),
            action="Verify OCR Data",
            from_stage="ocr_review",
            to_stage="credit_review",
            actor_id=str(current_user.id),
            actor_role=current_user.role
        )
        return RedirectResponse(url=f"/applications/{application_id}/credit-review", status_code=status.HTTP_303_SEE_OTHER)
    else:
        return RedirectResponse(url=f"/applications/{application_id}", status_code=status.HTTP_303_SEE_OTHER)

@router.get("/applications/{application_id}/visitation")
async def render_visitation_report(
    request: Request,
    application_id: str,
    conn = Depends(db_conn),
    current_user = Depends(get_current_user)
):
    """Page 16 Field Visitation Report Page."""
    repo = LoanRepository(conn)
    app = await repo.get_by_id(UUID(application_id), current_user.org_id)
    borrower_name = app.applicant_name if app else "Borrower"
    
    visitation_repo = VisitationRepository(conn)
    data = await visitation_repo.get_by_loan(loan_id=UUID(application_id), org_id=current_user.org_id) or {}
    
    return templates.TemplateResponse(
        request,
        "shared/visitation.html",
        {
            "current_user": current_user,
            "app_id": application_id,
            "borrower_name": borrower_name,
            "data": data
        }
    )

@router.post("/applications/{application_id}/visitation")
async def process_visitation_report(
    request: Request,
    application_id: str,
    action: str = Form(...),
    service: VisitationService = Depends(get_visitation_service),
    current_user = Depends(get_current_user)
):
    """POST processor for Field visitation report."""
    form_data = await request.form()
    
    await service.submit_report(
        loan_id=UUID(application_id),
        org_id=current_user.org_id,
        met_with=form_data.get("met_with"),
        premises_description=form_data.get("premises_description"),
        direction_from_branch=form_data.get("direction_from_branch"),
        submitted_by=current_user.id,
        user_role=current_user.role,
    )

    if action == "concur" and current_user.role == "branch_manager":
        await service.submit_manager_signoff(
            loan_id=UUID(application_id),
            org_id=current_user.org_id,
            manager_id=current_user.id,
            manager_role=current_user.role,
            notes="Branch Manager Concurred",
            decision="concurred",
        )
        
    return RedirectResponse(url=f"/applications/{application_id}", status_code=status.HTTP_303_SEE_OTHER)

@router.get("/applications/{application_id}/credit-review")
async def render_credit_review(
    request: Request,
    application_id: str,
    conn = Depends(db_conn),
    current_user = Depends(get_current_user)
):
    """Page 17 Credit Underwriter Review."""
    repo = LoanRepository(conn)
    app = await repo.get_by_id(UUID(application_id), current_user.org_id)
    borrower_name = app.applicant_name if app else "Borrower"
    
    return templates.TemplateResponse(
        request,
        "shared/credit_review.html",
        {
            "current_user": current_user,
            "app_id": application_id,
            "borrower_name": borrower_name,
            "amount": app.amount if app else 500000,
            "tenure": app.tenure if app else 12,
            "product_type": app.product_type if app else "MSEF"
        }
    )

@router.post("/applications/{application_id}/credit-review")
async def process_credit_review(
    application_id: str,
    recommendation_decision: str = Form(...),
    recommendation_notes: str = Form(...),
    conn = Depends(db_conn),
    current_user = Depends(get_current_user)
):
    """POST processor for credit underwriting recommendation."""
    repo = LoanRepository(conn)
    app = await repo.get_by_id(UUID(application_id), current_user.org_id)
    if not app:
        raise HTTPException(status_code=404, detail="Loan Application not found")
        
    if recommendation_decision == "Recommend Approval":
        stage_val = 'branch_approval'
    elif recommendation_decision == "Return for Correction":
        stage_val = 'returned'
    else:
        stage_val = 'rejected'
        
    await repo.advance_stage(UUID(application_id), current_user.org_id, stage_val)
    
    # Save recommendation to workflow
    audit = AuditService(conn)
    await audit.log(
        application_id=application_id,
        org_id=str(current_user.org_id),
        action="Credit Underwriting Verdict",
        from_stage="credit_review",
        to_stage=stage_val,
        actor_id=str(current_user.id),
        actor_role=current_user.role,
        reason=recommendation_notes
    )
    return RedirectResponse(url="/dashboard", status_code=status.HTTP_303_SEE_OTHER)

@router.get("/applications/{application_id}/approve")
async def render_approval_readiness(
    request: Request,
    application_id: str,
    conn = Depends(db_conn),
    current_user = Depends(get_current_user)
):
    """Page 18 Approval Readiness Review Page."""
    repo = LoanRepository(conn)
    app = await repo.get_by_id(UUID(application_id), current_user.org_id)
    borrower_name = app.applicant_name if app else "Borrower"
    
    summary = await repo.get_readiness_summary(UUID(application_id), current_user.org_id)
    
    return templates.TemplateResponse(
        request,
        "shared/approve.html",
        {
            "current_user": current_user,
            "app_id": application_id,
            "borrower_name": borrower_name,
            "summary": summary
        }
    )

@router.post("/applications/{application_id}/approve")
async def process_approval_readiness(
    application_id: str,
    conn = Depends(db_conn),
    current_user = Depends(get_current_user)
):
    """POST processor to complete branch approval."""
    repo = LoanRepository(conn)
    app = await repo.approve(UUID(application_id), current_user.org_id, current_user.id)
    if not app:
        raise HTTPException(status_code=404, detail="Loan Application not found or not in branch_approval stage")
        
    audit = AuditService(conn)
    await audit.log(
        application_id=application_id,
        org_id=str(current_user.org_id),
        action="Branch Final Approval",
        from_stage="branch_approval",
        to_stage="disbursement_ready",
        actor_id=str(current_user.id),
        actor_role=current_user.role
    )
    return RedirectResponse(url="/dashboard", status_code=status.HTTP_303_SEE_OTHER)

@router.get("/applications/{application_id}/return")
async def render_return_page(
    request: Request,
    application_id: str,
    conn = Depends(db_conn),
    current_user = Depends(get_current_user)
):
    """Page 20 Return Reason Page."""
    return templates.TemplateResponse(
        request,
        "shared/return_page.html",
        {
            "current_user": current_user,
            "app_id": application_id,
            "title": "Return Loan Application"
        }
    )

@router.post("/applications/{application_id}/return")
async def process_return_page(
    application_id: str,
    reason_category: str = Form(...),
    notes: str = Form(...),
    conn = Depends(db_conn),
    current_user = Depends(get_current_user)
):
    """POST processor to return application to draft stage."""
    repo = LoanRepository(conn)
    success = await repo.mark_returned(
        UUID(application_id), 
        current_user.org_id, 
        f"Category: {reason_category}. Notes: {notes}", 
        current_user.id
    )
    if not success:
        raise HTTPException(status_code=404, detail="Loan Application not found")
        
    audit = AuditService(conn)
    await audit.log(
        application_id=application_id,
        org_id=str(current_user.org_id),
        action="Return Application",
        from_stage="branch_approval",
        to_stage="returned",
        actor_id=str(current_user.id),
        actor_role=current_user.role,
        reason=f"Category: {reason_category}. Notes: {notes}"
    )
    return RedirectResponse(url="/dashboard", status_code=status.HTTP_303_SEE_OTHER)

@router.get("/forms")
async def render_loan_forms_re(request: Request):
    """Deprecated forms view, forward to wizard dashboard page."""
    return RedirectResponse(url="/dashboard", status_code=status.HTTP_303_SEE_OTHER)

@router.get("/pipeline")
async def render_loan_pipeline(
    request: Request,
    conn = Depends(db_conn),
    current_user = Depends(get_current_user)
):
    """Renders the standard CRM pipeline board."""
    repo = LoanRepository(conn)
    counts = await repo.count_by_stage(current_user.org_id)
    
    stage_counts = {
        "stage_1": 0,
        "stage_2": 0,
        "stage_3": 0,
        "stage_4": 0,
        "stage_5": 0,
        "stage_6": 0
    }
    
    mapping = {
        'intake': 1,
        'ocr_review': 2,
        'credit_review': 3,
        'branch_approval': 4,
        'disbursement_ready': 5,
        'disbursed': 6
    }
    
    for c in counts:
        num = mapping.get(c.stage)
        if num:
            stage_counts[f"stage_{num}"] = c.count

    applications = await repo.list_recent(current_user.org_id, limit=500)

    return templates.TemplateResponse(
        request,
        "shared/pipeline.html", 
        {
            "applications": applications, 
            "stage_counts": stage_counts,
            "current_user": current_user
        }
    )

@router.get("/borrowers")
async def render_current_loans(
    request: Request,
    conn = Depends(db_conn),
    current_user = Depends(RoleChecker(["Branch Manager", "Credit Officer", "Auditor", "System Admin"]))
):
    """Renders borrower loans view."""
    repo = LoanRepository(conn)
    applications = await repo.list_recent(current_user.org_id, limit=500)
    state_counts = {
        "total": len(applications),
        "draft": sum(1 for app in applications if app.current_stage == 1),
        "review": sum(1 for app in applications if app.current_stage in [2, 3, 4, 5]),
        "approved": sum(1 for app in applications if app.current_stage == 6),
        "active": sum(1 for app in applications if app.stage == "disbursed"),
    }
    return templates.TemplateResponse(
        request,
        "shared/borrowers.html",
        {
            "applications": applications,
            "state_counts": state_counts,
            "current_user": current_user
        }
    )

@router.get("/audit")
async def render_compliance_audit(
    request: Request,
    conn = Depends(db_conn),
    current_user = Depends(get_current_user)
):
    """Renders regulatory compliance trail."""
    repo = LoanRepository(conn)
    rows = await repo.list_workflow_events(current_user.org_id)
    events = []
    for r in rows:
        class MockEvent:
            def __init__(self, row):
                self.timestamp = row["created_at"]
                self.application_id = str(row["loan_id"])
                self.action = row["event_type"]
                self.reason = row["notes"]
                self.from_stage = row["from_stage"]
                self.to_stage = row["to_stage"]
                self.actor_id = row["triggered_by"]
                class MockActor:
                    name = "Officer"
                self.actor = MockActor()
        events.append(MockEvent(r))
        
    return templates.TemplateResponse(
        request,
        "shared/audit.html", 
        {"events": events, "current_user": current_user}
    )
