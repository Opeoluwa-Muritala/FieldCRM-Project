import os
from urllib.parse import urlencode
from datetime import datetime
from uuid import UUID
from fastapi import APIRouter, Depends, File, Form, HTTPException, Request, Response, UploadFile, status
from fastapi.responses import RedirectResponse
from fastapi.templating import Jinja2Templates

from app.core.database import db_conn
from app.core.exceptions import DomainException
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
from app.domains.notifications.repository import NotificationRepository
from app.domains.notifications.service import NotificationService

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
    """
    Each role gets a genuinely different template and data set.
    Mobile and desktop share the same template but extend different
    base shells via the 'shell' context variable.
    """
    dashboard_svc = DashboardService(conn)
    data = await dashboard_svc.get_dashboard_data(current_user)

    repo = LoanRepository(conn)
    if current_user.role.lower().replace(" ", "_") in ("account_officer", "loan_officer"):
        applications = data.get("queue", [])
    else:
        applications = await repo.list_recent(current_user.org_id, limit=10)

    role = current_user.role.lower().replace(" ", "_")

    # Roles with dedicated dashboard routes
    if role in ("crm", "head_crm"):
        return RedirectResponse(url="/crm-dashboard", status_code=status.HTTP_303_SEE_OTHER)
    if role == "ed":
        return RedirectResponse(url="/ed-dashboard", status_code=status.HTTP_303_SEE_OTHER)
    if role == "md":
        return RedirectResponse(url="/md-dashboard", status_code=status.HTTP_303_SEE_OTHER)
    if role in ("md", "ed"):
        return RedirectResponse(url="/executive-dashboard", status_code=status.HTTP_303_SEE_OTHER)
    template_name = get_role_template(role, "dashboard.html")

    ctx = build_template_context(
        request,
        current_user,
        data=data,
        applications=applications,
        metrics=data.get("metrics", {}),
        today_label=datetime.now().strftime("%A, %d %B %Y"),
    )

    return templates.TemplateResponse(request, template_name, ctx)

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


@router.get("/visitation-reports")
async def render_my_visitation_reports(
    request: Request,
    conn = Depends(db_conn),
    current_user = Depends(RoleChecker(["Loan Officer"])),
):
    """Completed visitation reports owned by the current Account Officer."""
    reports = await conn.fetch(
        """
        SELECT vr.loan_id, vr.visit_date, vr.met_with, vr.status, vr.updated_at,
               la.ref_no, la.applicant_name, la.amount
        FROM visitation_reports vr
        JOIN loan_applications la ON la.id = vr.loan_id
        WHERE vr.org_id = $1 AND la.created_by = $2 AND la.deleted_at IS NULL
        ORDER BY vr.updated_at DESC
        """,
        current_user.org_id, current_user.id,
    )
    data = await DashboardService(conn).get_dashboard_data(current_user)
    ctx = build_template_context(
        request, current_user, reports=[dict(row) for row in reports],
        metrics=data.get("metrics", {}), active_tab="visits", active_page="visit_reports",
    )
    return templates.TemplateResponse(request, "loan_officer/visitation_reports.html", ctx)


@router.get("/document-upload")
async def render_document_upload_selector(
    request: Request,
    conn = Depends(db_conn),
    current_user = Depends(RoleChecker(["Loan Officer"])),
):
    """Choose an Account Officer-owned application before uploading a document."""
    applications, _ = await LoanRepository(conn).list_by_stage(
        current_user.org_id, None, current_user.id, page=1, size=100
    )
    data = await DashboardService(conn).get_dashboard_data(current_user)
    ctx = build_template_context(
        request, current_user, applications=applications,
        metrics=data.get("metrics", {}), active_tab="upload", active_page="upload",
    )
    return templates.TemplateResponse(request, "loan_officer/document_upload_selector.html", ctx)


@router.get("/ocr-review-queue")
async def render_ocr_review_queue(
    request: Request,
    conn = Depends(db_conn),
    current_user = Depends(RoleChecker(["Loan Officer"])),
):
    """OCR worklist, limited to applications that have source documents."""
    rows = await conn.fetch(
        """
        SELECT la.id, la.ref_no, la.applicant_name, la.amount, la.stage,
               MAX(d.uploaded_at) AS last_document_at,
               COUNT(d.id)::int AS document_count
        FROM loan_applications la
        JOIN documents d ON d.loan_id = la.id AND d.org_id = la.org_id AND d.deleted_at IS NULL
        WHERE la.org_id = $1 AND la.created_by = $2 AND la.deleted_at IS NULL
        GROUP BY la.id, la.ref_no, la.applicant_name, la.amount, la.stage
        ORDER BY last_document_at DESC
        """,
        current_user.org_id, current_user.id,
    )
    data = await DashboardService(conn).get_dashboard_data(current_user)
    ctx = build_template_context(
        request, current_user, applications=[dict(row) for row in rows],
        metrics=data.get("metrics", {}), active_tab="upload", active_page="ocr_queue",
    )
    return templates.TemplateResponse(request, "loan_officer/ocr_review_queue.html", ctx)

@router.get("/awaiting-me")
async def render_awaiting_me(
    request: Request,
    conn = Depends(db_conn),
    current_user = Depends(RoleChecker(["Branch Manager", "Branch Supervisor"]))
):
    """Render applications awaiting branch manager concurrence."""
    dashboard_svc = DashboardService(conn)
    queue = await dashboard_svc.get_awaiting_concurrence(current_user)
    data = await dashboard_svc.get_dashboard_data(current_user)
    ctx = build_template_context(
        request,
        current_user,
        queue=queue,
        data=data,
        metrics=data.get("metrics", {}),
        active_tab="awaiting",
        active_page="awaiting",
        today_label=datetime.now().strftime("%A, %d %B %Y"),
    )
    return templates.TemplateResponse(request, "branch_manager/awaiting_concurrence.html", ctx)

@router.get("/pending-signoffs")
async def render_pending_signoffs(
    request: Request,
    conn = Depends(db_conn),
    current_user = Depends(RoleChecker(["Branch Manager"]))
):
    """Render visitation signoffs awaiting branch manager concurrence."""
    dashboard_svc = DashboardService(conn)
    signoffs = await dashboard_svc.get_pending_signoffs(current_user)
    data = await dashboard_svc.get_dashboard_data(current_user)
    ctx = build_template_context(
        request,
        current_user,
        signoffs=signoffs,
        data=data,
        metrics=data.get("metrics", {}),
        active_tab="signoffs",
        active_page="signoffs",
        today_label=datetime.now().strftime("%A, %d %B %Y"),
    )
    return templates.TemplateResponse(request, "branch_manager/pending_signoffs.html", ctx)

@router.get("/supervisory-review-queue")
async def render_supervisory_review_queue(
    request: Request,
    conn = Depends(db_conn),
    current_user = Depends(RoleChecker(["Branch Supervisor"])),
):
    """Render the post-manager queue assigned to Branch Supervisors."""
    dashboard_svc = DashboardService(conn)
    queue = await dashboard_svc.get_supervisory_review_queue(current_user)
    data = await dashboard_svc.get_dashboard_data(current_user)
    ctx = build_template_context(
        request,
        current_user,
        queue=queue,
        data=data,
        metrics=data.get("metrics", {}),
        active_page="supervisory_reviews",
        today_label=datetime.now().strftime("%A, %d %B %Y"),
    )
    return templates.TemplateResponse(request, "branch_supervisor/review_queue.html", ctx)

@router.get("/my-reviews")
async def render_my_reviews(
    request: Request,
    conn = Depends(db_conn),
    current_user = Depends(RoleChecker(["Branch Manager", "Credit Analyst"]))
):
    """Render the credit review queue (now handled by branch manager)."""
    dashboard_svc = DashboardService(conn)
    reviews = await dashboard_svc.get_credit_reviews(current_user)
    data = await dashboard_svc.get_dashboard_data(current_user)
    ctx = build_template_context(
        request,
        current_user,
        reviews=reviews,
        data=data,
        metrics=data.get("metrics", {}),
        active_tab="reviews",
        active_page="reviews",
        today_label=datetime.now().strftime("%A, %d %B %Y"),
    )
    template = "credit_analyst/review_queue.html" if current_user.role == "credit_analyst" else "branch_manager/awaiting_concurrence.html"
    return templates.TemplateResponse(request, template, ctx)

@router.get("/ocr-exceptions")
async def render_ocr_exceptions(
    request: Request,
    conn = Depends(db_conn),
    current_user = Depends(RoleChecker(["Branch Manager", "Credit Analyst"]))
):
    """Render OCR exceptions (now handled by branch manager)."""
    dashboard_svc = DashboardService(conn)
    exceptions = await dashboard_svc.get_credit_ocr_exceptions(current_user)
    data = await dashboard_svc.get_dashboard_data(current_user)
    ctx = build_template_context(
        request,
        current_user,
        exceptions=exceptions,
        data=data,
        metrics=data.get("metrics", {}),
        active_tab="exceptions",
        active_page="exceptions",
        today_label=datetime.now().strftime("%A, %d %B %Y"),
    )
    template = "credit_analyst/ocr_exceptions.html" if current_user.role == "credit_analyst" else "branch_manager/awaiting_concurrence.html"
    return templates.TemplateResponse(request, template, ctx)

@router.get("/audit-trail")
async def render_audit_trail(
    request: Request,
    conn = Depends(db_conn),
    current_user = Depends(RoleChecker(["Auditor"]))
):
    """Render the read-only audit trail for auditors."""
    dashboard_svc = DashboardService(conn)
    activity = await dashboard_svc.get_recent_audit_activity(current_user)
    data = await dashboard_svc.get_dashboard_data(current_user)
    ctx = build_template_context(
        request,
        current_user,
        activity=activity,
        data=data,
        metrics=data.get("metrics", {}),
        active_tab="audit",
        active_page="audit",
        today_label=datetime.now().strftime("%A, %d %B %Y"),
    )
    return templates.TemplateResponse(request, "auditor/audit_trail.html", ctx)

@router.get("/compliance-flags")
async def render_compliance_flags(
    request: Request,
    conn = Depends(db_conn),
    current_user = Depends(RoleChecker(["Auditor"]))
):
    """Render compliance flags from documents, OCR fields, and workflow events."""
    dashboard_svc = DashboardService(conn)
    flags = await dashboard_svc.get_compliance_flags(current_user)
    data = await dashboard_svc.get_dashboard_data(current_user)
    ctx = build_template_context(
        request,
        current_user,
        flags=flags,
        data=data,
        metrics=data.get("metrics", {}),
        active_tab="flags",
        active_page="flags",
        today_label=datetime.now().strftime("%A, %d %B %Y"),
    )
    return templates.TemplateResponse(request, "auditor/compliance_flags.html", ctx)

@router.get("/users")
async def render_user_management(
    request: Request,
    conn = Depends(db_conn),
    current_user = Depends(RoleChecker(["System Admin"]))
):
    """Render system admin user management view."""
    dashboard_svc = DashboardService(conn)
    users = await dashboard_svc.get_admin_users(current_user)
    data = await dashboard_svc.get_dashboard_data(current_user)
    ctx = build_template_context(
        request,
        current_user,
        users=users,
        data=data,
        role_counts=data.get("role_counts", []),
        metrics=data.get("metrics", {}),
        active_tab="users",
        active_page="users",
        today_label=datetime.now().strftime("%A, %d %B %Y"),
    )
    return templates.TemplateResponse(request, "system_admin/users.html", ctx)

@router.get("/system-activity")
async def render_system_activity(
    request: Request,
    conn = Depends(db_conn),
    current_user = Depends(RoleChecker(["System Admin"]))
):
    """Render system admin activity and final-control queue."""
    dashboard_svc = DashboardService(conn)
    activity = await dashboard_svc.get_recent_audit_activity(current_user)
    control_queue = await dashboard_svc.get_system_control_queue(current_user)
    data = await dashboard_svc.get_dashboard_data(current_user)
    ctx = build_template_context(
        request,
        current_user,
        activity=activity,
        control_queue=control_queue,
        data=data,
        metrics=data.get("metrics", {}),
        active_tab="activity",
        active_page="activity",
        today_label=datetime.now().strftime("%A, %d %B %Y"),
    )
    return templates.TemplateResponse(request, "system_admin/system_activity.html", ctx)

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
    role_name = current_user.role.lower().replace(" ", "_")
    officer_id = current_user.id if role_name in ("account_officer", "loan_officer") else None
    applications, total = await repo.list_by_stage(
        org_id=current_user.org_id,
        stage=db_stage,
        officer_id=officer_id,
        page=1,
        size=100
    )
    ctx = build_template_context(
        request,
        current_user,
        applications=applications,
        current_stage=stage,
        current_loan_type=loan_type,
        search_query=q,
        from_date=from_date,
        to_date=to_date,
        active_tab="applications",
        active_page="applications",
    )
    return templates.TemplateResponse(request, "shared/applications.html", ctx)

@router.get("/applications/new")
async def render_new_application(
    request: Request,
    current_user = Depends(RoleChecker(["Account Officer"]))
):
    """Renders Page 3 customer selection page."""
    ctx = build_template_context(
        request,
        current_user,
        active_tab="new_application",
        active_page="new_application",
    )
    return templates.TemplateResponse(request, "shared/new_application.html", ctx)

@router.post("/applications/new")
async def process_new_application(
    request: Request,
    customer_type: str = Form(...),
    loan_type: str = Form(...),
    service: LoanService = Depends(get_loan_service),
    current_user = Depends(RoleChecker(["Account Officer"]))
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
async def render_application_detail(
    request: Request,
    application_id: str,
    conn = Depends(db_conn),
    current_user = Depends(get_current_user)
):
    """
    On mobile: redirects to the correct wizard step or review page depending on current owner/stage.
    On desktop: renders the role-specific detail workstation layout page.
    """
    repo = LoanRepository(conn)
    app = await repo.get_by_id(UUID(application_id), current_user.org_id)
    if not app:
        raise HTTPException(status_code=404, detail="Loan Application not found")
        
    device = detect_device_type(request)
    if device == "mobile":
        if app.current_stage == 1:
            return RedirectResponse(url=f"/applications/{application_id}/step/1", status_code=status.HTTP_303_SEE_OTHER)
        elif app.current_stage == 2:
            # Legacy OCR-review records no longer have a manual review page.
            return RedirectResponse(url=f"/applications/{application_id}/approve", status_code=status.HTTP_303_SEE_OTHER)
        elif app.current_stage == 3:
            return RedirectResponse(url=f"/applications/{application_id}/credit-review", status_code=status.HTTP_303_SEE_OTHER)
        else:
            return RedirectResponse(url=f"/applications/{application_id}/approve", status_code=status.HTTP_303_SEE_OTHER)

    # On desktop, render role-specific detail workstation
    role = current_user.role.lower().replace(" ", "_")
    template_name = get_role_template(role, "application_detail.html")
    # Branch Supervisor and Credit Analyst use their own dashboards/queues,
    # but share the Branch Manager's read-only application review layout.
    # Those role folders intentionally do not contain a duplicate detail view.
    if role in ("branch_supervisor", "credit_analyst"):
        template_name = "branch_manager/application_detail.html"
    
    # Load all data needed for the detail view
    loan_svc = get_loan_service(conn)
    doc_svc = get_document_service(conn)
    visitation_repo = VisitationRepository(conn)
    
    wizard_data = await loan_svc.get_wizard_data(UUID(application_id))
    documents = await doc_svc.repo.get_by_loan(UUID(application_id), current_user.org_id)
    visitation_data = await visitation_repo.get_by_loan(loan_id=UUID(application_id), org_id=current_user.org_id) or {}
    readiness_summary = await repo.get_readiness_summary(UUID(application_id), current_user.org_id)
    
    # Load and filter audit events
    all_audit_events = await repo.list_workflow_events(current_user.org_id)
    audit_events = [e for e in all_audit_events if str(e.get("loan_id")) == application_id]
    
    # Load and filter compliance flags
    dashboard_svc = DashboardService(conn)
    all_flags = await dashboard_svc.get_compliance_flags(current_user, limit=1000)
    flags = [f for f in all_flags if str(f.get("loan_id")) == application_id]

    ctx = build_template_context(
        request,
        current_user,
        app=app,
        app_id=application_id,
        borrower_name=app.applicant_name,
        amount=app.amount or 500000,
        tenure=app.tenure or 12,
        product_type=app.product_type or "MSEF",
        wizard_data=wizard_data,
        documents=documents,
        visitation_data=visitation_data,
        summary=readiness_summary,
        audit_events=audit_events,
        flags=flags,
        active_tab="applications",
        active_page="applications",
    )
    return templates.TemplateResponse(request, template_name, ctx)

@router.get("/applications/{application_id}/step/{step}")
async def render_wizard_step(
    request: Request,
    application_id: str,
    step: int,
    service: LoanService = Depends(get_loan_service),
    conn = Depends(db_conn),
    current_user = Depends(get_current_user)
):
    """GET handler for Borrower intake wizard steps 1 to 8."""
    if step not in range(1, 9):
        raise HTTPException(status_code=404, detail="Unknown intake step")
    repo = LoanRepository(conn)
    app = await repo.get_by_id(UUID(application_id), current_user.org_id)
    if not app:
        raise HTTPException(status_code=404, detail="Loan Application not found")
        
    user_role = current_user.role.lower().replace(" ", "_")
    if user_role not in ("account_officer", "loan_officer"):
        raise HTTPException(status_code=403, detail="Insufficient permissions for this action")
    if app.created_by != current_user.id:
        raise HTTPException(status_code=403, detail="You do not have permission to view/modify this application")

    data = await service.get_wizard_data(UUID(application_id))

    ctx = build_template_context(
        request,
        current_user,
        app_id=application_id,
        step=step,
        data=data,
        active_tab="queue",
        active_page="queue",
    )
    return templates.TemplateResponse(request, "shared/application_wizard.html", ctx)

@router.post("/applications/{application_id}/step/{step}")
async def process_wizard_step(
    request: Request,
    application_id: str,
    step: int,
    service: LoanService = Depends(get_loan_service),
    conn = Depends(db_conn),
    current_user = Depends(get_current_user)
):
    """POST handler to persist wizard values and advance flow."""
    repo = LoanRepository(conn)
    app = await repo.get_by_id(UUID(application_id), current_user.org_id)
    if not app:
        raise HTTPException(status_code=404, detail="Loan Application not found")
        
    user_role = current_user.role.lower().replace(" ", "_")
    if user_role not in ("account_officer", "loan_officer"):
        raise HTTPException(status_code=403, detail="Insufficient permissions for this action")
    if app.created_by != current_user.id:
        raise HTTPException(status_code=403, detail="You do not have permission to view/modify this application")
    form_data = await request.form()
    data_dict = form_data_to_jsonable_dict(form_data)
    if step not in range(1, 9):
        raise HTTPException(status_code=404, detail="Unknown intake step")
    try:
        await service.save_wizard_step(UUID(application_id), step, data_dict, current_user.id, current_user.org_id)
    except DomainException as exc:
        # Browser form posts should return users to the relevant field, not
        # the API's JSON exception response.
        query = urlencode({"error": exc.message, "focus": "wizardForm"})
        return RedirectResponse(
            url=f"/applications/{application_id}/step/{step}?{query}",
            status_code=status.HTTP_303_SEE_OTHER,
        )

    if step < 8:
        next_step = step + 1
        return RedirectResponse(url=f"/applications/{application_id}/step/{next_step}", status_code=status.HTTP_303_SEE_OTHER)
    else:
        # OCR extraction continues in the background. There is no manual OCR
        # review stage; completed intake is handed to the Branch Manager.
        return RedirectResponse(url=f"/applications/{application_id}", status_code=status.HTTP_303_SEE_OTHER)

@router.get("/applications/{application_id}/guarantors/{guarantor_index}/step/{step}")
async def render_guarantor_step(
    request: Request,
    application_id: str,
    guarantor_index: int,
    step: int,
    service: GuarantorService = Depends(get_guarantor_service),
    current_user = Depends(get_current_user)
):
    """GET handler for Guarantor intake flow steps 1 to 8."""
    data = await service.get_wizard_data(UUID(application_id), guarantor_index)
    ctx = build_template_context(
        request,
        current_user,
        app_id=application_id,
        guarantor_index=guarantor_index,
        step=step,
        data=data,
        hide_tabbar=True,
        mobile_title_text=f"Guarantor: Step {step}"
    )
    return templates.TemplateResponse(request, "shared/guarantor_wizard.html", ctx)

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
    form_data = await request.form()
    data_dict = form_data_to_jsonable_dict(form_data)
    await service.save_wizard_step(UUID(application_id), guarantor_index, step, data_dict, current_user.id)

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
    if not app:
        raise HTTPException(status_code=404, detail="Loan Application not found")

    ctx = build_template_context(
        request,
        current_user,
        app_id=application_id,
        doc_type=type,
        borrower_name=app.applicant_name,
        active_tab="upload",
        active_page="upload",
    )
    return templates.TemplateResponse(request, "shared/upload_document.html", ctx)

@router.post("/applications/{application_id}/documents/upload")
async def process_document_upload(
    application_id: str,
    type: str = "other",
    category: str = Form("other"),
    file: UploadFile = File(...),
    service: DocumentService = Depends(get_document_service),
    current_user = Depends(get_current_user)
):
    """POST handler to store documents on server side."""
    doc_type = category or type or "other"
    await service.save_upload(
        loan_id=UUID(application_id),
        org_id=current_user.org_id,
        doc_type=doc_type,
        file=file,
        uploaded_by=current_user.id,
        user_role=current_user.role,
    )
    return RedirectResponse(url=f"/applications/{application_id}", status_code=status.HTTP_303_SEE_OTHER)

@router.post("/applications/{application_id}/crm-upload")
async def process_crm_upload(
    application_id: str,
    doc_label: str = Form("crm_memo"),
    file: UploadFile = File(...),
    service: DocumentService = Depends(get_document_service),
    current_user=Depends(RoleChecker(["crm", "head_crm"])),
):
    """CRM uploads a supporting document (memo, recommendation, etc.) and returns to CRM review."""
    await service.save_upload(
        loan_id=UUID(application_id),
        org_id=current_user.org_id,
        doc_type=doc_label,
        file=file,
        uploaded_by=current_user.id,
        user_role=current_user.role,
    )
    return RedirectResponse(
        url=f"/applications/{application_id}/crm-review",
        status_code=status.HTTP_303_SEE_OTHER,
    )


@router.get("/applications/{application_id}/ocr-review")
async def render_ocr_review(
    request: Request,
    application_id: str,
    doc: str = "loan",
    conn = Depends(db_conn),
    current_user = Depends(get_current_user)
):
    """Compatibility redirect for retired manual OCR-review links."""
    return RedirectResponse(
        url=f"/applications/{application_id}",
        status_code=status.HTTP_303_SEE_OTHER,
    )

@router.post("/applications/{application_id}/ocr-review")
async def process_ocr_review(
    application_id: str,
    action: str = Form(...),
    conn = Depends(db_conn),
    current_user = Depends(RoleChecker(["Account Officer"]))
):
    """Compatibility handler for clients that still post the retired screen."""
    repo = LoanRepository(conn)
    app = await repo.get_by_id(UUID(application_id), current_user.org_id)
    if not app:
        raise HTTPException(status_code=404, detail="Loan Application not found")
        
    if action == "verify":
        await repo.advance_stage(UUID(application_id), current_user.org_id, "branch_manager_review")
        
        audit = AuditService(conn)
        await audit.log(
            application_id=application_id,
            org_id=str(current_user.org_id),
            action="Verify OCR Data",
            from_stage="ocr_review",
            to_stage="branch_manager_review",
            actor_id=str(current_user.id),
            actor_role=current_user.role
        )
        return RedirectResponse(url=f"/applications/{application_id}", status_code=status.HTTP_303_SEE_OTHER)
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
    
    ctx = build_template_context(
        request,
        current_user,
        app_id=application_id,
        borrower_name=borrower_name,
        data=data,
        active_tab="visits",
        active_page="visits",
    )
    return templates.TemplateResponse(request, "shared/visitation.html", ctx)

@router.post("/applications/{application_id}/visitation")
async def process_visitation_report(
    request: Request,
    application_id: str,
    action: str = Form(...),
    service: VisitationService = Depends(get_visitation_service),
    current_user = Depends(RoleChecker(["Loan Officer", "Branch Manager"]))
):
    """POST processor for Field visitation report."""
    form_data = await request.form()
    
    await service.submit_report(
        loan_id=UUID(application_id),
        org_id=current_user.org_id,
        met_with=form_data.get("person_met") or form_data.get("met_with"),
        premises_description=form_data.get("premises_description"),
        direction_from_branch=form_data.get("direction_from_branch"),
        visit_date=form_data.get("visit_date"),
        visit_time=form_data.get("visit_time"),
        relationship=form_data.get("relationship"),
        business_condition=form_data.get("business_condition"),
        account_officer=form_data.get("account_officer"),
        visiting_officer=form_data.get("visiting_officer"),
        visiting_officer_sig=form_data.get("visiting_officer_sig"),
        account_officer_sig=form_data.get("account_officer_sig"),
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
            signature=form_data.get("bm_sig"),
            return_reason=form_data.get("concurrence_return_reason"),
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
    
    ctx = build_template_context(
        request,
        current_user,
        app_id=application_id,
        borrower_name=borrower_name,
        amount=app.amount if app else 500000,
        tenure=app.tenure if app else 12,
        product_type=app.product_type if app else "MSEF",
        active_tab="reviews",
        active_page="reviews",
    )
    return templates.TemplateResponse(request, "shared/credit_review.html", ctx)

@router.post("/applications/{application_id}/credit-review")
async def process_credit_review(
    application_id: str,
    recommendation_decision: str = Form(...),
    recommendation_notes: str = Form(...),
    conn = Depends(db_conn),
    current_user = Depends(RoleChecker(["Credit Analyst"]))
):
    """POST processor for credit underwriting recommendation."""
    repo = LoanRepository(conn)
    app = await repo.get_by_id(UUID(application_id), current_user.org_id)
    if not app:
        raise HTTPException(status_code=404, detail="Loan Application not found")
        
    if app.stage != "credit_analyst_review":
        raise HTTPException(status_code=400, detail="Application is not awaiting Credit Analyst review")

    if recommendation_decision == "Recommend Approval":
        stage_val = 'crm_review'
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
        from_stage="credit_analyst_review",
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
    
    ctx = build_template_context(
        request,
        current_user,
        app_id=application_id,
        borrower_name=borrower_name,
        app=app,
        summary=summary,
        active_tab="awaiting",
        active_page="awaiting",
    )
    return templates.TemplateResponse(request, "shared/approve.html", ctx)

@router.post("/applications/{application_id}/approve")
async def process_approval_readiness(
    request: Request,
    application_id: str,
    conn = Depends(db_conn),
    current_user = Depends(RoleChecker(["Branch Manager", "Branch Supervisor"]))
):
    """Record the applicable branch concurrence and forward for further review."""
    form_data = await request.form()
    kyc_attested = form_data.get("kyc_attested")
    collateral_attested = form_data.get("collateral_attested")
    import logging
    logger = logging.getLogger(__name__)
    logger.info(
        "Approval attestations for application %s — kyc_attested=%s, collateral_attested=%s",
        application_id, kyc_attested, collateral_attested
    )

    role = current_user.role.lower().replace(" ", "_")
    expected_stage, next_stage, audit_action = {
        "branch_manager": ("branch_manager_review", "branch_supervisor_review", "Branch Manager Concurrence — Forwarded to Branch Supervisor"),
        "branch_supervisor": ("branch_supervisor_review", "credit_analyst_review", "Branch Supervisor Concurrence — Forwarded to Credit Analyst"),
    }[role]
    repo = LoanRepository(conn)
    app = await repo.approve(
        UUID(application_id), current_user.org_id, current_user.id,
        expected_stage=expected_stage, next_stage=next_stage,
    )
    if not app:
        raise HTTPException(status_code=404, detail="Loan Application is not awaiting your review")
        
    audit = AuditService(conn)
    await audit.log(
        application_id=application_id,
        org_id=str(current_user.org_id),
        action="Branch Manager Concurrence — Forwarded for Further Review",
        from_stage=expected_stage,
        to_stage=next_stage,
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
    ctx = build_template_context(
        request,
        current_user,
        app_id=application_id,
        title="Return Loan Application",
        active_tab="awaiting",
        active_page="awaiting",
    )
    return templates.TemplateResponse(request, "shared/return_page.html", ctx)

@router.post("/applications/{application_id}/return")
async def process_return_page(
    application_id: str,
    reason_category: str = Form(...),
    notes: str = Form(...),
    conn = Depends(db_conn),
    current_user = Depends(RoleChecker(["Branch Manager", "Credit Analyst"]))
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
    if current_user.role.lower().replace(" ", "_") == "branch_manager":
        dashboard_svc = DashboardService(conn)
        pipeline = await dashboard_svc.get_branch_pipeline(current_user)
        data = await dashboard_svc.get_dashboard_data(current_user)
        ctx = build_template_context(
            request,
            current_user,
            pipeline=pipeline,
            data=data,
            metrics=data.get("metrics", {}),
            active_tab="pipeline",
            active_page="pipeline",
            today_label=datetime.now().strftime("%A, %d %B %Y"),
        )
        return templates.TemplateResponse(request, "branch_manager/pipeline.html", ctx)

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

    ctx = build_template_context(
        request,
        current_user,
        applications=applications,
        stage_counts=stage_counts,
        active_tab="pipeline",
        active_page="pipeline",
    )
    return templates.TemplateResponse(request, "shared/pipeline.html", ctx)

@router.get("/borrowers")
async def render_current_loans(
    request: Request,
    conn = Depends(db_conn),
    current_user = Depends(RoleChecker(["Branch Manager", "Branch Supervisor", "Credit Analyst", "CRM", "Head CRM", "Auditor", "ED", "MD"]))
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
    ctx = build_template_context(
        request,
        current_user,
        applications=applications,
        state_counts=state_counts,
        active_tab="borrowers",
        active_page="borrowers",
    )
    return templates.TemplateResponse(request, "shared/borrowers.html", ctx)

@router.get("/notifications")
async def render_notifications(
    request: Request,
    conn=Depends(db_conn),
    current_user=Depends(get_current_user),
):
    svc = NotificationService(NotificationRepository(conn))
    notifications = await svc.list_for_user(user_id=current_user.id, org_id=current_user.org_id)
    ctx = build_template_context(request, current_user, notifications=notifications, active_page="notifications")
    return templates.TemplateResponse(request, "shared/notifications.html", ctx)

@router.post("/notifications/{notification_id}/read")
async def mark_notification_read(
    request: Request,
    notification_id: str,
    conn=Depends(db_conn),
    current_user=Depends(get_current_user),
):
    svc = NotificationService(NotificationRepository(conn))
    await svc.mark_read_for_user(notification_id=notification_id, user_id=str(current_user.id), org_id=str(current_user.org_id))
    return RedirectResponse(url="/notifications", status_code=status.HTTP_303_SEE_OTHER)

@router.post("/notifications/clear")
async def clear_notifications(
    request: Request,
    conn=Depends(db_conn),
    current_user=Depends(get_current_user),
):
    svc = NotificationService(NotificationRepository(conn))
    await svc.clear_for_user(user_id=str(current_user.id), org_id=str(current_user.org_id))
    return RedirectResponse(url="/notifications", status_code=status.HTTP_303_SEE_OTHER)

@router.get("/settings")
async def render_settings(
    request: Request,
    current_user=Depends(get_current_user),
):
    ctx = build_template_context(request, current_user, active_page="settings", success=None, error=None)
    return templates.TemplateResponse(request, "shared/settings.html", ctx)

@router.post("/settings/change-password")
async def change_password(
    request: Request,
    current_password: str = Form(...),
    new_password: str = Form(...),
    confirm_password: str = Form(...),
    conn=Depends(db_conn),
    current_user=Depends(get_current_user),
):
    from app.domains.auth.repository import AuthRepository
    from app.domains.auth.service import AuthService
    if new_password != confirm_password:
        ctx = build_template_context(request, current_user, active_page="settings", success=None, error="Passwords do not match.")
        return templates.TemplateResponse(request, "shared/settings.html", ctx)
    ok = await AuthService(AuthRepository(conn)).change_password(str(current_user.id), current_password, new_password)
    if not ok:
        ctx = build_template_context(request, current_user, active_page="settings", success=None, error="Current password is incorrect.")
        return templates.TemplateResponse(request, "shared/settings.html", ctx)
    ctx = build_template_context(request, current_user, active_page="settings", success="Password updated successfully.", error=None)
    return templates.TemplateResponse(request, "shared/settings.html", ctx)

@router.get("/search")
async def render_search(
    request: Request,
    q: str = "",
    conn=Depends(db_conn),
    current_user=Depends(get_current_user),
):
    repo = LoanRepository(conn)
    role_name = current_user.role.lower().replace(" ", "_")
    apps = await repo.search(org_id=current_user.org_id, query=q) if q else []
    if role_name in ("account_officer", "loan_officer"):
        applications = [app for app in apps if app.created_by == current_user.id]
    else:
        applications = apps
    ctx = build_template_context(request, current_user, query=q, applications=applications, active_page="search")
    return templates.TemplateResponse(request, "shared/search_results.html", ctx)

@router.get("/audit")
async def render_compliance_audit(
    request: Request,
    conn = Depends(db_conn),
    current_user = Depends(RoleChecker(["Auditor"]))
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
        
    ctx = build_template_context(
        request,
        current_user,
        events=events,
        active_tab="audit",
        active_page="audit",
    )
    return templates.TemplateResponse(request, "shared/audit.html", ctx)


# =============================================================================
# CRM QUEUE
# =============================================================================

@router.get("/crm-review-queue")
async def render_crm_queue(
    request: Request,
    conn=Depends(db_conn),
    current_user=Depends(RoleChecker(["crm", "head_crm"])),
):
    dashboard_svc = DashboardService(conn)
    queue = await dashboard_svc.get_crm_queue(current_user)
    par = await dashboard_svc.get_par_summary(current_user)
    ctx = build_template_context(
        request, current_user,
        queue=queue, par=par,
        active_tab="crm_queue", active_page="crm_queue",
        today_label=datetime.now().strftime("%A, %d %B %Y"),
    )
    return templates.TemplateResponse(request, "crm/crm_queue.html", ctx)


@router.get("/applications/{application_id}/crm-review")
async def render_crm_review(
    request: Request,
    application_id: str,
    conn=Depends(db_conn),
    current_user=Depends(RoleChecker(["crm", "head_crm"])),
):
    repo = LoanRepository(conn)
    app = await repo.get_by_id(UUID(application_id), current_user.org_id)
    if not app:
        raise HTTPException(status_code=404, detail="Loan Application not found")
    doc_svc = get_document_service(conn)
    documents = await doc_svc.repo.get_by_loan(UUID(application_id), current_user.org_id)
    readiness = await repo.get_readiness_summary(UUID(application_id), current_user.org_id)
    consent_stage = await repo.get_stage_data(UUID(application_id), "crm_review")
    ctx = build_template_context(
        request, current_user,
        app=app, app_id=application_id,
        documents=documents, summary=readiness,
        consent_data=(consent_stage or {}).get("data_json", {}),
        active_tab="crm_queue", active_page="crm_queue",
    )
    return templates.TemplateResponse(request, "crm/crm_review.html", ctx)


@router.post("/applications/{application_id}/crm-review")
async def process_crm_review(
    request: Request,
    application_id: str,
    action: str = Form(...),
    crm_notes: str = Form(""),
    conn=Depends(db_conn),
    current_user=Depends(RoleChecker(["crm", "head_crm"])),
):
    repo = LoanRepository(conn)
    application = await repo.get_by_id(UUID(application_id), current_user.org_id)
    if not application:
        raise HTTPException(status_code=404, detail="Loan Application not found")
    role = current_user.role
    if role == "crm" and application.stage != "crm_review":
        raise HTTPException(status_code=400, detail="Application is not awaiting CRM review")
    if role == "head_crm" and application.stage != "head_crm_review":
        raise HTTPException(status_code=400, detail="Application is not awaiting Head CRM approval")

    if action == "advance":
        if role == "crm":
            form_data = await request.form()
            required = {
                "consent_credit_bureau": "Credit Bureau Disclosure",
                "consent_credit_check": "Credit Check Authorisation",
                "consent_cheque": "Cheque Recovery Authorisation",
                "consent_gsi": "Global Standing Instruction (GSI) Mandate",
                "applicant_signature": "Applicant Signature",
                "final_declaration": "Final declaration",
            }
            missing = [label for key, label in required.items() if not form_data.get(key)]
            # Preserve the reviewer’s completed consents/signature even when
            # another required item is still missing on this attempt.
            existing_consent_stage = await repo.get_stage_data(UUID(application_id), "crm_review")
            consent_data = (existing_consent_stage or {}).get("data_json", {})
            for key in ("consent_credit_bureau", "consent_credit_check", "consent_cheque", "consent_gsi", "final_declaration"):
                if form_data.get(key):
                    consent_data[key] = "true"
            if form_data.get("applicant_signature"):
                consent_data["applicant_signature"] = str(form_data.get("applicant_signature"))
            if missing:
                await repo.save_stage_data(UUID(application_id), "crm_review", consent_data, current_user.id)
                query = urlencode({"error": "Please complete: " + ", ".join(missing), "focus": "crm-consents"})
                return RedirectResponse(
                    url=f"/applications/{application_id}/crm-review?{query}",
                    status_code=status.HTTP_303_SEE_OTHER,
                )
            await repo.save_stage_data(UUID(application_id), "crm_review", consent_data, current_user.id)
        next_stage = "audit_review" if role == "head_crm" else "head_crm_review"
        app = await repo.advance_stage(UUID(application_id), current_user.org_id, next_stage)
        if not app:
            raise HTTPException(status_code=400, detail="Unable to advance application")
        audit = AuditService(conn)
        await audit.log(
            application_id=application_id,
            org_id=str(current_user.org_id),
            action="CRM Dossier Review Complete — Sent to Committee",
            from_stage=application.stage,
            to_stage=next_stage,
            actor_id=str(current_user.id),
            actor_role=current_user.role,
            reason=crm_notes,
        )
    elif action == "return":
        await repo.mark_returned(UUID(application_id), current_user.org_id, crm_notes or "Returned by CRM", current_user.id)
    return RedirectResponse(url="/crm-review-queue", status_code=status.HTTP_303_SEE_OTHER)


# =============================================================================
# EXECUTIVE QUEUE
# =============================================================================

@router.get("/executive-queue")
async def render_executive_queue(
    request: Request,
    conn=Depends(db_conn),
    current_user=Depends(RoleChecker(["md", "ed"])),
):
    dashboard_svc = DashboardService(conn)
    queue = await dashboard_svc.get_executive_queue(current_user)
    par = await dashboard_svc.get_par_summary(current_user)
    ctx = build_template_context(
        request, current_user,
        queue=queue, par=par,
        active_tab="exec_queue", active_page="exec_queue",
        today_label=datetime.now().strftime("%A, %d %B %Y"),
    )
    return templates.TemplateResponse(request, "executive/executive_queue.html", ctx)


@router.get("/applications/{application_id}/executive-approve")
async def render_executive_approve(
    request: Request,
    application_id: str,
    conn=Depends(db_conn),
    current_user=Depends(RoleChecker(["md", "ed"])),
):
    repo = LoanRepository(conn)
    app = await repo.get_by_id(UUID(application_id), current_user.org_id)
    if not app:
        raise HTTPException(status_code=404, detail="Loan Application not found")
    doc_svc = get_document_service(conn)
    documents = await doc_svc.repo.get_by_loan(UUID(application_id), current_user.org_id)
    ctx = build_template_context(
        request, current_user,
        app=app, app_id=application_id, documents=documents,
        active_tab="exec_queue", active_page="exec_queue",
    )
    return templates.TemplateResponse(request, "executive/executive_approve.html", ctx)


@router.post("/applications/{application_id}/executive-approve")
async def process_executive_approve(
    application_id: str,
    conn=Depends(db_conn),
    current_user=Depends(RoleChecker(["md", "ed"])),
):
    repo = LoanRepository(conn)
    app = await repo.executive_approve(UUID(application_id), current_user.org_id, current_user.id)
    if not app:
        raise HTTPException(status_code=400, detail="Application not in executive_approval stage")
    audit = AuditService(conn)
    await audit.log(
        application_id=application_id,
        org_id=str(current_user.org_id),
        action="Executive Disbursement Instruction",
        from_stage="executive_approval",
        to_stage="disbursement_ready",
        actor_id=str(current_user.id),
        actor_role=current_user.role,
    )
    return RedirectResponse(url="/executive-queue", status_code=status.HTTP_303_SEE_OTHER)


# =============================================================================
# COMMITTEE REVIEW QUEUE
# =============================================================================

@router.get("/committee-queue")
async def render_committee_queue(
    request: Request,
    conn=Depends(db_conn),
    current_user=Depends(RoleChecker(["committee"])),
):
    dashboard_svc = DashboardService(conn)
    queue = await dashboard_svc.get_committee_queue(current_user)
    ctx = build_template_context(
        request, current_user,
        queue=queue,
        active_tab="committee_queue", active_page="committee_queue",
        today_label=datetime.now().strftime("%A, %d %B %Y"),
    )
    return templates.TemplateResponse(request, "committee/committee_queue.html", ctx)


@router.get("/applications/{application_id}/committee-review")
async def render_committee_review(
    request: Request,
    application_id: str,
    conn=Depends(db_conn),
    current_user=Depends(RoleChecker(["committee"])),
):
    repo = LoanRepository(conn)
    app = await repo.get_by_id(UUID(application_id), current_user.org_id)
    if not app:
        raise HTTPException(status_code=404, detail="Loan Application not found")
    votes = await repo.get_committee_votes(UUID(application_id), current_user.org_id)
    last_loan = await repo.get_last_loan(
        current_user.org_id, app.applicant_name, app.phone, UUID(application_id)
    )
    repayments = []
    if last_loan:
        from app.services.loan_servicing_service import LoanServicingService
        svc = LoanServicingService(conn)
        repayments = await svc.get_payments(UUID(str(last_loan["id"])), current_user.org_id)
    my_vote = next((v for v in votes if str(v["member_id"]) == str(current_user.id)), None)
    ctx = build_template_context(
        request, current_user,
        app=app, app_id=application_id,
        votes=votes, my_vote=my_vote,
        last_loan=last_loan, repayments=repayments,
        active_tab="committee_queue", active_page="committee_queue",
    )
    return templates.TemplateResponse(request, "committee/committee_review.html", ctx)


@router.post("/applications/{application_id}/committee-vote")
async def process_committee_vote(
    application_id: str,
    recommendation: str = Form(...),
    notes: str = Form(""),
    conn=Depends(db_conn),
    current_user=Depends(RoleChecker(["committee"])),
):
    repo = LoanRepository(conn)
    await repo.insert_committee_vote(
        UUID(application_id), current_user.org_id, current_user.id, recommendation, notes
    )
    audit = AuditService(conn)
    await audit.log(
        application_id=application_id,
        org_id=str(current_user.org_id),
        action=f"Committee Vote: {recommendation}",
        from_stage="committee_review",
        to_stage="committee_review",
        actor_id=str(current_user.id),
        actor_role=current_user.role,
        reason=notes,
    )
    return RedirectResponse(
        url=f"/applications/{application_id}/committee-review",
        status_code=status.HTTP_303_SEE_OTHER,
    )


@router.post("/applications/{application_id}/committee-complete")
async def process_committee_complete(
    application_id: str,
    recommendation: str = Form(...),
    conn=Depends(db_conn),
    current_user=Depends(RoleChecker(["committee"])),
):
    repo = LoanRepository(conn)
    app = await repo.complete_committee_review(
        UUID(application_id), current_user.org_id, recommendation
    )
    if not app:
        raise HTTPException(status_code=400, detail="Application not in committee_review stage")
    audit = AuditService(conn)
    await audit.log(
        application_id=application_id,
        org_id=str(current_user.org_id),
        action=f"Committee Review Complete — {recommendation}",
        from_stage="committee_review",
        to_stage=app.stage,
        actor_id=str(current_user.id),
        actor_role=current_user.role,
    )
    return RedirectResponse(url="/committee-queue", status_code=status.HTTP_303_SEE_OTHER)


# =============================================================================
# ED APPROVAL QUEUE
# =============================================================================

@router.get("/ed-queue")
async def render_ed_queue(
    request: Request,
    conn=Depends(db_conn),
    current_user=Depends(RoleChecker(["ed"])),
):
    dashboard_svc = DashboardService(conn)
    queue = await dashboard_svc.get_ed_queue(current_user)
    par = await dashboard_svc.get_par_summary(current_user)
    ctx = build_template_context(
        request, current_user,
        queue=queue, par=par,
        active_tab="ed_queue", active_page="ed_queue",
        today_label=datetime.now().strftime("%A, %d %B %Y"),
    )
    return templates.TemplateResponse(request, "executive/ed_queue.html", ctx)


@router.get("/applications/{application_id}/ed-approve")
async def render_ed_approve(
    request: Request,
    application_id: str,
    conn=Depends(db_conn),
    current_user=Depends(RoleChecker(["ed"])),
):
    repo = LoanRepository(conn)
    app = await repo.get_by_id(UUID(application_id), current_user.org_id)
    if not app:
        raise HTTPException(status_code=404, detail="Loan Application not found")
    votes = await repo.get_committee_votes(UUID(application_id), current_user.org_id)
    doc_svc = get_document_service(conn)
    documents = await doc_svc.repo.get_by_loan(UUID(application_id), current_user.org_id)
    ctx = build_template_context(
        request, current_user,
        app=app, app_id=application_id,
        votes=votes, documents=documents,
        active_tab="ed_queue", active_page="ed_queue",
    )
    return templates.TemplateResponse(request, "executive/ed_approve.html", ctx)


@router.post("/applications/{application_id}/ed-approve")
async def process_ed_approve(
    application_id: str,
    action: str = Form(...),
    conn=Depends(db_conn),
    current_user=Depends(RoleChecker(["ed"])),
):
    repo = LoanRepository(conn)
    if action == "approve":
        app = await repo.ed_approve(UUID(application_id), current_user.org_id, current_user.id)
        if not app:
            raise HTTPException(status_code=400, detail="Application not in ed_approval stage")
        audit = AuditService(conn)
        await audit.log(
            application_id=application_id,
            org_id=str(current_user.org_id),
            action="ED Final Approval — Disbursement Instruction",
            from_stage="ed_approval",
            to_stage="disbursement_ready",
            actor_id=str(current_user.id),
            actor_role=current_user.role,
        )
    elif action == "escalate_md":
        app = await repo.ed_escalate_to_md(UUID(application_id), current_user.org_id, current_user.id)
        if not app:
            raise HTTPException(status_code=400, detail="Application not in ed_approval stage")
        audit = AuditService(conn)
        await audit.log(
            application_id=application_id,
            org_id=str(current_user.org_id),
            action="ED Escalated to MD",
            from_stage="ed_approval",
            to_stage="md_approval",
            actor_id=str(current_user.id),
            actor_role=current_user.role,
        )
    else:
        raise HTTPException(status_code=400, detail="Invalid action")
    return RedirectResponse(url="/ed-queue", status_code=status.HTTP_303_SEE_OTHER)


# =============================================================================
# MD APPROVAL QUEUE
# =============================================================================

@router.get("/md-queue")
async def render_md_queue(
    request: Request,
    conn=Depends(db_conn),
    current_user=Depends(RoleChecker(["md"])),
):
    dashboard_svc = DashboardService(conn)
    queue = await dashboard_svc.get_md_queue(current_user)
    par = await dashboard_svc.get_par_summary(current_user)
    ctx = build_template_context(
        request, current_user,
        queue=queue, par=par,
        active_tab="md_queue", active_page="md_queue",
        today_label=datetime.now().strftime("%A, %d %B %Y"),
    )
    return templates.TemplateResponse(request, "executive/md_queue.html", ctx)


@router.get("/applications/{application_id}/md-approve")
async def render_md_approve(
    request: Request,
    application_id: str,
    conn=Depends(db_conn),
    current_user=Depends(RoleChecker(["md"])),
):
    repo = LoanRepository(conn)
    app = await repo.get_by_id(UUID(application_id), current_user.org_id)
    if not app:
        raise HTTPException(status_code=404, detail="Loan Application not found")
    votes = await repo.get_committee_votes(UUID(application_id), current_user.org_id)
    board_referrals = await repo.get_board_referrals(UUID(application_id), current_user.org_id)
    doc_svc = get_document_service(conn)
    documents = await doc_svc.repo.get_by_loan(UUID(application_id), current_user.org_id)
    ctx = build_template_context(
        request, current_user,
        app=app, app_id=application_id,
        votes=votes, documents=documents, board_referrals=board_referrals,
        active_tab="md_queue", active_page="md_queue",
    )
    return templates.TemplateResponse(request, "executive/md_approve.html", ctx)


@router.post("/applications/{application_id}/md-approve")
async def process_md_approve(
    application_id: str,
    action: str = Form(...),
    md_notes: str = Form(""),
    conn=Depends(db_conn),
    current_user=Depends(RoleChecker(["md"])),
):
    repo = LoanRepository(conn)
    if action == "approve":
        app = await repo.md_approve(UUID(application_id), current_user.org_id, current_user.id, md_notes)
        if not app:
            raise HTTPException(status_code=400, detail="Application not in md_approval stage")
        audit = AuditService(conn)
        await audit.log(
            application_id=application_id,
            org_id=str(current_user.org_id),
            action="MD Final Approval — Disbursement Instruction",
            from_stage="md_approval",
            to_stage="disbursement_ready",
            actor_id=str(current_user.id),
            actor_role=current_user.role,
            reason=md_notes,
        )
    elif action == "comment":
        if not await repo.md_add_comment(UUID(application_id), current_user.org_id, md_notes):
            raise HTTPException(status_code=400, detail="Application not in md_approval stage")
        audit = AuditService(conn)
        await audit.log(
            application_id=application_id,
            org_id=str(current_user.org_id),
            action="MD Comment Added — Returned to ED",
            from_stage="md_approval",
            to_stage="ed_approval",
            actor_id=str(current_user.id),
            actor_role=current_user.role,
            reason=md_notes,
        )
    else:
        raise HTTPException(status_code=400, detail="Invalid action")
    return RedirectResponse(url="/md-queue", status_code=status.HTTP_303_SEE_OTHER)


@router.post("/applications/{application_id}/md-refer-board")
async def process_md_refer_board(
    application_id: str,
    board_member_email: str = Form(...),
    board_member_name: str = Form(""),
    notes: str = Form(""),
    conn=Depends(db_conn),
    current_user=Depends(RoleChecker(["md"])),
):
    repo = LoanRepository(conn)
    await repo.insert_board_referral(
        UUID(application_id), current_user.org_id, current_user.id,
        board_member_email, board_member_name, notes
    )
    audit = AuditService(conn)
    await audit.log(
        application_id=application_id,
        org_id=str(current_user.org_id),
        action=f"MD Board Referral — {board_member_email}",
        from_stage="md_approval",
        to_stage="md_approval",
        actor_id=str(current_user.id),
        actor_role=current_user.role,
        reason=notes,
    )
    return RedirectResponse(
        url=f"/applications/{application_id}/md-approve",
        status_code=status.HTTP_303_SEE_OTHER,
    )


# =============================================================================
# DISBURSEMENT (CRM records and schedule generated)
# =============================================================================

@router.get("/applications/{application_id}/disburse")
async def render_disburse(
    request: Request,
    application_id: str,
    conn=Depends(db_conn),
    current_user=Depends(RoleChecker(["crm", "head_crm"])),
):
    repo = LoanRepository(conn)
    app = await repo.get_by_id(UUID(application_id), current_user.org_id)
    if not app:
        raise HTTPException(status_code=404, detail="Loan Application not found")
    ctx = build_template_context(
        request, current_user,
        app=app, app_id=application_id,
        can_record_disbursement=current_user.role == "crm",
        active_tab="crm_queue", active_page="crm_queue",
    )
    return templates.TemplateResponse(request, "crm/disburse.html", ctx)


@router.post("/applications/{application_id}/disburse")
async def process_disburse(
    application_id: str,
    disbursed_amount: float = Form(...),
    disbursement_method: str = Form(...),
    disbursed_bank_ref: str = Form(""),
    payment_date: str = Form(...),
    interest_rate: float = Form(...),
    repayment_frequency: str = Form(...),
    schedule_method: str = Form("flat_rate"),
    conn=Depends(db_conn),
    current_user=Depends(RoleChecker(["crm"])),
):
    import secrets
    from datetime import datetime as dt
    from app.services.loan_servicing_service import LoanServicingService

    repo = LoanRepository(conn)
    loan_uuid = UUID(application_id)

    # Generate unique disbursement ref
    disbursement_ref = f"DIS-{datetime.now().strftime('%Y%m%d')}-{secrets.token_hex(4).upper()}"

    app = await repo.disburse(
        loan_id=loan_uuid,
        org_id=current_user.org_id,
        disbursed_amount=disbursed_amount,
        disbursement_method=disbursement_method,
        disbursed_bank_ref=disbursed_bank_ref or None,
        disbursement_ref=disbursement_ref,
        interest_rate=interest_rate,
        repayment_frequency=repayment_frequency,
        schedule_method=schedule_method,
    )
    if not app:
        raise HTTPException(status_code=400, detail="Application not in disbursement_ready stage")

    # Generate repayment schedule internally
    try:
        disbursement_date = dt.strptime(payment_date, "%Y-%m-%d").date()
    except ValueError:
        disbursement_date = dt.today().date()

    svc = LoanServicingService(conn)
    await svc.create_schedule(
        loan_id=loan_uuid,
        org_id=current_user.org_id,
        principal=disbursed_amount,
        annual_rate=interest_rate,
        tenor_months=app.tenor_months or 12,
        frequency=repayment_frequency,
        method=schedule_method,
        disbursement_date=disbursement_date,
    )

    audit = AuditService(conn)
    await audit.log(
        application_id=application_id,
        org_id=str(current_user.org_id),
        action="Disbursement Recorded",
        from_stage="disbursement_ready",
        to_stage="disbursed",
        actor_id=str(current_user.id),
        actor_role=current_user.role,
    )
    return RedirectResponse(
        url=f"/applications/{application_id}/repayment-schedule",
        status_code=status.HTTP_303_SEE_OTHER,
    )


# =============================================================================
# REPAYMENT SCHEDULE
# =============================================================================

@router.get("/applications/{application_id}/repayment-schedule")
async def render_repayment_schedule(
    request: Request,
    application_id: str,
    conn=Depends(db_conn),
    current_user=Depends(get_current_user),
):
    from app.services.loan_servicing_service import LoanServicingService
    repo = LoanRepository(conn)
    app = await repo.get_by_id(UUID(application_id), current_user.org_id)
    if not app:
        raise HTTPException(status_code=404, detail="Loan Application not found")
    svc = LoanServicingService(conn)
    schedule = await svc.get_schedule(UUID(application_id), current_user.org_id)
    payments = await svc.get_payments(UUID(application_id), current_user.org_id)
    total_paid = sum(p["amount_paid"] for p in payments)
    total_due = sum(r["total_due"] for r in schedule)
    ctx = build_template_context(
        request, current_user,
        app=app, app_id=application_id,
        schedule=schedule, payments=payments,
        total_paid=total_paid, total_due=total_due,
        outstanding=total_due - total_paid,
        active_page="applications",
    )
    return templates.TemplateResponse(request, "shared/repayment_schedule.html", ctx)


# =============================================================================
# REPAYMENT COLLECTIONS
# =============================================================================

@router.get("/applications/{application_id}/payments")
async def render_record_payment(
    request: Request,
    application_id: str,
    conn=Depends(db_conn),
    current_user=Depends(RoleChecker(["crm", "head_crm"])),
):
    from app.services.loan_servicing_service import LoanServicingService
    repo = LoanRepository(conn)
    app = await repo.get_by_id(UUID(application_id), current_user.org_id)
    if not app:
        raise HTTPException(status_code=404, detail="Loan Application not found")
    svc = LoanServicingService(conn)
    payments = await svc.get_payments(UUID(application_id), current_user.org_id)
    ctx = build_template_context(
        request, current_user,
        app=app, app_id=application_id, payments=payments,
        can_record_payment=current_user.role == "crm",
        active_page="applications",
    )
    return templates.TemplateResponse(request, "crm/record_payment.html", ctx)


@router.post("/applications/{application_id}/payments")
async def process_record_payment(
    application_id: str,
    payment_date: str = Form(...),
    amount_paid: float = Form(...),
    channel: str = Form(...),
    bank_ref: str = Form(""),
    conn=Depends(db_conn),
    current_user=Depends(RoleChecker(["crm"])),
):
    from datetime import datetime as dt
    from app.services.loan_servicing_service import LoanServicingService
    try:
        pdate = dt.strptime(payment_date, "%Y-%m-%d").date()
    except ValueError:
        pdate = dt.today().date()
    svc = LoanServicingService(conn)
    await svc.record_payment(
        loan_id=UUID(application_id),
        org_id=current_user.org_id,
        payment_date=pdate,
        amount_paid=amount_paid,
        channel=channel,
        bank_ref=bank_ref or None,
        recorded_by=current_user.id,
    )
    return RedirectResponse(
        url=f"/applications/{application_id}/repayment-schedule",
        status_code=status.HTTP_303_SEE_OTHER,
    )


# =============================================================================
# PAR DASHBOARD
# =============================================================================

@router.get("/reports/par")
async def render_par_dashboard(
    request: Request,
    conn=Depends(db_conn),
    current_user=Depends(RoleChecker(["md", "ed", "auditor", "crm", "head_crm"])),
):
    dashboard_svc = DashboardService(conn)
    par = await dashboard_svc.get_par_summary(current_user)
    repo = LoanRepository(conn)
    disbursed = await repo.list_disbursed(current_user.org_id)
    ctx = build_template_context(
        request, current_user,
        par=par, loans=disbursed,
        active_page="par",
        today_label=datetime.now().strftime("%A, %d %B %Y"),
    )
    return templates.TemplateResponse(request, "shared/par_dashboard.html", ctx)


# =============================================================================
# DASHBOARDS for new roles
# =============================================================================

@router.get("/crm-dashboard")
async def render_crm_dashboard(
    request: Request,
    conn=Depends(db_conn),
    current_user=Depends(RoleChecker(["crm", "head_crm"])),
):
    dashboard_svc = DashboardService(conn)
    data = await dashboard_svc.get_dashboard_data(current_user)
    ctx = build_template_context(
        request, current_user,
        data=data,
        crm_queue=data.get("crm_queue", []),
        recent_disbursements=data.get("recent_disbursements", []),
        par=data.get("par", {}),
        metrics=data.get("metrics", {}),
        active_tab="dashboard", active_page="dashboard",
        today_label=datetime.now().strftime("%A, %d %B %Y"),
    )
    return templates.TemplateResponse(request, "crm/dashboard.html", ctx)


@router.get("/executive-dashboard")
async def render_executive_dashboard(
    request: Request,
    conn=Depends(db_conn),
    current_user=Depends(RoleChecker(["md", "ed"])),
):
    dashboard_svc = DashboardService(conn)
    data = await dashboard_svc.get_dashboard_data(current_user)
    ctx = build_template_context(
        request, current_user,
        data=data,
        exec_queue=data.get("exec_queue", []),
        par=data.get("par", {}),
        metrics=data.get("metrics", {}),
        active_tab="dashboard", active_page="dashboard",
        today_label=datetime.now().strftime("%A, %d %B %Y"),
    )
    return templates.TemplateResponse(request, "executive/dashboard.html", ctx)


@router.get("/ed-dashboard")
async def render_ed_dashboard(
    request: Request,
    conn=Depends(db_conn),
    current_user=Depends(RoleChecker(["ed"])),
):
    dashboard_svc = DashboardService(conn)
    data = await dashboard_svc.get_dashboard_data(current_user)
    ctx = build_template_context(
        request, current_user,
        data=data,
        ed_queue=data.get("ed_queue", []),
        par=data.get("par", {}),
        metrics=data.get("metrics", {}),
        active_tab="dashboard", active_page="dashboard",
        today_label=datetime.now().strftime("%A, %d %B %Y"),
    )
    return templates.TemplateResponse(request, "executive/ed_dashboard.html", ctx)


@router.get("/md-dashboard")
async def render_md_dashboard(
    request: Request,
    conn=Depends(db_conn),
    current_user=Depends(RoleChecker(["md"])),
):
    dashboard_svc = DashboardService(conn)
    data = await dashboard_svc.get_dashboard_data(current_user)
    ctx = build_template_context(
        request, current_user,
        data=data,
        md_queue=data.get("md_queue", []),
        par=data.get("par", {}),
        metrics=data.get("metrics", {}),
        active_tab="dashboard", active_page="dashboard",
        today_label=datetime.now().strftime("%A, %d %B %Y"),
    )
    return templates.TemplateResponse(request, "executive/md_dashboard.html", ctx)


@router.get("/committee-dashboard")
async def render_committee_dashboard(
    request: Request,
    conn=Depends(db_conn),
    current_user=Depends(RoleChecker(["committee"])),
):
    dashboard_svc = DashboardService(conn)
    data = await dashboard_svc.get_dashboard_data(current_user)
    ctx = build_template_context(
        request, current_user,
        data=data,
        committee_queue=data.get("committee_queue", []),
        metrics=data.get("metrics", {}),
        active_tab="dashboard", active_page="dashboard",
        today_label=datetime.now().strftime("%A, %d %B %Y"),
    )
    return templates.TemplateResponse(request, "committee/dashboard.html", ctx)


# =============================================================================
# CLIENT INTAKE FORM SHARING & NO-LOGIN FLOW
# =============================================================================

from jose import jwt
from app.config import settings
from app.core.security import decode_access_token
import secrets

@router.post("/loans/generate-share-link")
async def generate_share_link(
    request: Request,
    current_user = Depends(RoleChecker(["Loan Officer"]))
):
    """Generates a cryptographically signed link for client intake."""
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


@router.get("/share-intake/{token}")
async def render_share_intake(
    request: Request,
    token: str,
    conn = Depends(db_conn)
):
    """Introductory screen for client intake. Handles draft resumption."""
    payload = decode_access_token(token)
    if not payload or payload.get("type") != "client_intake":
        return templates.TemplateResponse(
            request, "shared/client_error.html", 
            {"error_message": "The sharing link is invalid, malformed, or has expired. Please contact your loan officer for a new link."}
        )
        
    officer_id = payload.get("sub")
    org_id = payload.get("org_id")
    
    # Check if this token was already used to start an application
    row = await conn.fetchrow(
        "SELECT id, stage FROM loan_applications WHERE share_token = $1 AND org_id = $2",
        token, UUID(org_id)
    )
    if row:
        app_id = str(row["id"])
        stage = row["stage"]
        if stage == "intake":
            # Resume existing draft
            from datetime import datetime, timedelta
            expire = datetime.utcnow() + timedelta(days=3)
            session_payload = {
                "type": "client_session",
                "app_id": app_id,
                "org_id": org_id,
                "officer_id": officer_id,
                "exp": expire
            }
            session_token = jwt.encode(session_payload, settings.JWT_SECRET_KEY, algorithm=settings.JWT_ALGORITHM)
            
            is_secure = settings.COOKIE_SECURE or (request.url.scheme == "https" or request.headers.get("x-forwarded-proto") == "https")
            redirect = RedirectResponse(url="/client-form/apply/step/1", status_code=status.HTTP_303_SEE_OTHER)
            redirect.set_cookie(
                key="client_session",
                value=session_token,
                httponly=True,
                secure=is_secure,
                samesite="lax",
                max_age=3 * 24 * 60 * 60,
                path="/"
            )
            return redirect
        else:
            # Already submitted application (block reuse)
            return templates.TemplateResponse(
                request, "shared/client_error.html", 
                {"error_message": "This link has already been used to submit a loan application. Sharing links are single-use only."}
            )

    from app.domains.users.repository import UserRepository
    user_repo = UserRepository(conn)
    officer = await user_repo.get_by_id(UUID(officer_id))
    officer_name = officer.full_name if officer else "Mainstreet Officer"
    
    return templates.TemplateResponse(
        request, "shared/client_start.html",
        {"token": token, "officer_name": officer_name, "error": None}
    )


@router.post("/share-intake/{token}/start")
async def process_share_intake_start(
    request: Request,
    token: str,
    customer_type: str = Form(...),
    loan_type: str = Form(...),
    full_name: str = Form(...),
    conn = Depends(db_conn)
):
    """Creates the application and sets the client session cookie."""
    payload = decode_access_token(token)
    if not payload or payload.get("type") != "client_intake":
        raise HTTPException(status_code=403, detail="Invalid token")
        
    officer_id = payload.get("sub")
    org_id = payload.get("org_id")

    row = await conn.fetchrow(
        "SELECT id, stage FROM loan_applications WHERE share_token = $1 AND org_id = $2",
        token, UUID(org_id)
    )
    if row:
        if row["stage"] != "intake":
            raise HTTPException(status_code=400, detail="Application already submitted")
        app_id = str(row["id"])
    else:
        service = get_loan_service(conn)
        app = await service.create_loan(
            org_id=UUID(org_id),
            customer_type=customer_type,
            loan_type=loan_type,
            applicant_name=full_name,
            user_id=UUID(officer_id)
        )
        app_id = str(app.id)
        await conn.execute(
            "UPDATE loan_applications SET share_token = $1 WHERE id = $2 AND org_id = $3",
            token, UUID(app_id), UUID(org_id)
        )

    from datetime import datetime, timedelta
    expire = datetime.utcnow() + timedelta(days=3)
    session_payload = {
        "type": "client_session",
        "app_id": app_id,
        "org_id": org_id,
        "officer_id": officer_id,
        "exp": expire
    }
    session_token = jwt.encode(session_payload, settings.JWT_SECRET_KEY, algorithm=settings.JWT_ALGORITHM)
    
    is_secure = settings.COOKIE_SECURE or (request.url.scheme == "https" or request.headers.get("x-forwarded-proto") == "https")
    redirect = RedirectResponse(url="/client-form/apply/step/1", status_code=status.HTTP_303_SEE_OTHER)
    redirect.set_cookie(
        key="client_session",
        value=session_token,
        httponly=True,
        secure=is_secure,
        samesite="lax",
        max_age=3 * 24 * 60 * 60,
        path="/"
    )
    return redirect


def get_client_session_data(request: Request) -> dict:
    cookie_val = request.cookies.get("client_session")
    if not cookie_val:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Session not found. Please open the link sent by your officer."
        )
    payload = decode_access_token(cookie_val)
    if not payload or payload.get("type") != "client_session":
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Session expired or invalid. Please open the link sent by your officer."
        )
    return payload


@router.get("/client-form/apply/step/{step}")
async def render_client_wizard_step(
    request: Request,
    step: int,
    session = Depends(get_client_session_data),
    conn = Depends(db_conn)
):
    if step not in range(1, 9):
        raise HTTPException(status_code=404, detail="Unknown intake step")
    app_id = session.get("app_id")
    org_id = session.get("org_id")
    officer_id = session.get("officer_id")
    
    repo = LoanRepository(conn)
    app = await repo.get_by_id(UUID(app_id), UUID(org_id))
    if not app:
        raise HTTPException(status_code=404, detail="Loan Application not found")
        
    if app.stage != "intake":
        return RedirectResponse(url="/client-form/success", status_code=status.HTTP_303_SEE_OTHER)

    service = get_loan_service(conn)
    data = await service.get_wizard_data(UUID(app_id))
    
    from app.domains.users.repository import UserRepository
    officer = await UserRepository(conn).get_by_id(UUID(officer_id))
    officer_name = officer.full_name if officer else "Your Loan Officer"

    ctx = build_template_context(
        request,
        user=None,
        app_id=app_id,
        step=step,
        data=data,
        officer_name=officer_name,
        hide_tabbar=True,
    )
    return templates.TemplateResponse(request, "shared/client_wizard.html", ctx)


@router.post("/client-form/apply/step/{step}")
async def process_client_wizard_step(
    request: Request,
    step: int,
    session = Depends(get_client_session_data),
    conn = Depends(db_conn)
):
    if step not in range(1, 9):
        raise HTTPException(status_code=404, detail="Unknown intake step")
    app_id = session.get("app_id")
    org_id = session.get("org_id")
    officer_id = session.get("officer_id")

    repo = LoanRepository(conn)
    app = await repo.get_by_id(UUID(app_id), UUID(org_id))
    if not app:
        raise HTTPException(status_code=404, detail="Loan Application not found")
        
    if app.stage != "intake":
        return RedirectResponse(url="/client-form/success", status_code=status.HTTP_303_SEE_OTHER)

    form_data = await request.form()
    data_dict = form_data_to_jsonable_dict(form_data)
    
    service = get_loan_service(conn)
    await service.save_wizard_step(UUID(app_id), step, data_dict, UUID(officer_id), UUID(org_id))

    if step < 8:
        next_step = step + 1
        return RedirectResponse(url=f"/client-form/apply/step/{next_step}", status_code=status.HTTP_303_SEE_OTHER)
    else:
        return RedirectResponse(url="/client-form/success", status_code=status.HTTP_303_SEE_OTHER)


@router.get("/client-form/apply/documents/upload")
async def render_client_document_upload(
    request: Request,
    type: str,
    session = Depends(get_client_session_data),
    conn = Depends(db_conn)
):
    app_id = session.get("app_id")
    org_id = session.get("org_id")

    repo = LoanRepository(conn)
    app = await repo.get_by_id(UUID(app_id), UUID(org_id))
    if not app:
        raise HTTPException(status_code=404, detail="Loan Application not found")

    ctx = build_template_context(
        request,
        user=None,
        app_id=app_id,
        borrower_name=app.applicant_name,
        doc_type=type,
        hide_tabbar=True
    )
    return templates.TemplateResponse(request, "shared/client_upload_document.html", ctx)


@router.post("/client-form/apply/documents/upload")
async def process_client_document_upload(
    request: Request,
    category: str = Form(...),
    file: UploadFile = File(...),
    session = Depends(get_client_session_data),
    conn = Depends(db_conn)
):
    app_id = session.get("app_id")
    org_id = session.get("org_id")
    officer_id = session.get("officer_id")

    repo = LoanRepository(conn)
    app = await repo.get_by_id(UUID(app_id), UUID(org_id))
    if not app:
        raise HTTPException(status_code=404, detail="Loan Application not found")

    doc_service = get_document_service(conn)
    await doc_service.save_upload(
        loan_id=UUID(app_id),
        org_id=UUID(org_id),
        doc_type=category,
        file=file,
        uploaded_by=UUID(officer_id),
        user_role="client",
    )

    return {"redirect": "/client-form/apply/step/4"}


@router.get("/client-form/apply/guarantors/{guarantor_index}/step/{step}")
async def render_client_guarantor_step(
    request: Request,
    guarantor_index: int,
    step: int,
    session = Depends(get_client_session_data),
    conn = Depends(db_conn)
):
    app_id = session.get("app_id")
    org_id = session.get("org_id")

    repo = LoanRepository(conn)
    app = await repo.get_by_id(UUID(app_id), UUID(org_id))
    if not app:
        raise HTTPException(status_code=404, detail="Loan Application not found")

    service = get_guarantor_service(conn)
    data = await service.get_wizard_data(UUID(app_id), guarantor_index)
    
    ctx = build_template_context(
        request,
        user=None,
        app_id=app_id,
        guarantor_index=guarantor_index,
        step=step,
        data=data,
        hide_tabbar=True,
    )
    return templates.TemplateResponse(request, "shared/client_guarantor_wizard.html", ctx)


@router.post("/client-form/apply/guarantors/{guarantor_index}/step/{step}")
async def process_client_guarantor_step(
    request: Request,
    guarantor_index: int,
    step: int,
    session = Depends(get_client_session_data),
    conn = Depends(db_conn)
):
    app_id = session.get("app_id")
    org_id = session.get("org_id")
    officer_id = session.get("officer_id")

    repo = LoanRepository(conn)
    app = await repo.get_by_id(UUID(app_id), UUID(org_id))
    if not app:
        raise HTTPException(status_code=404, detail="Loan Application not found")

    form_data = await request.form()
    data_dict = form_data_to_jsonable_dict(form_data)
    
    service = get_guarantor_service(conn)
    await service.save_wizard_step(UUID(app_id), guarantor_index, step, data_dict, UUID(officer_id))

    if step < 8:
        return RedirectResponse(
            url=f"/client-form/apply/guarantors/{guarantor_index}/step/{step + 1}", 
            status_code=status.HTTP_303_SEE_OTHER
        )
    else:
        await service.mark_slot_submitted(
            loan_id=UUID(app_id),
            org_id=UUID(org_id),
            slot=guarantor_index,
            submitted_by=UUID(officer_id),
            user_role="client"
        )
        return RedirectResponse(url="/client-form/apply/step/3", status_code=status.HTTP_303_SEE_OTHER)


@router.get("/client-form/success")
async def render_client_success(
    request: Request,
    session = Depends(get_client_session_data),
    conn = Depends(db_conn)
):
    officer_id = session.get("officer_id")
    
    from app.domains.users.repository import UserRepository
    officer = await UserRepository(conn).get_by_id(UUID(officer_id))
    officer_name = officer.full_name if officer else "Your Loan Officer"

    ctx = build_template_context(
        request,
        user=None,
        officer_name=officer_name,
        hide_tabbar=True,
    )
    
    response = templates.TemplateResponse(request, "shared/client_success.html", ctx)
    response.delete_cookie("client_session")
    return response
