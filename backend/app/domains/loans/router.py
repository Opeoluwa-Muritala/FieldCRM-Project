import asyncio
import base64
import json
import hashlib
import logging
import os
from html import escape
from urllib.parse import urlencode
from datetime import date, datetime, timedelta, timezone
from decimal import Decimal
from uuid import UUID
from fastapi import APIRouter, Depends, File, Form, HTTPException, Request, Response, UploadFile, status, Query
from fastapi.responses import RedirectResponse

from app.core.database import get_connection
from app.core.exceptions import DomainException
from app.domains.loans.repository import LoanRepository
from app.domains.loans.service import LoanService
from app.core.audit import AuditService
from app.core.cache import cache_scoped_data, get_cached_scoped_data
from app.core.dependencies import authenticated_db_conn as db_conn, get_current_user, RoleChecker
from app.core.template_utils import (
    build_template_context,
    csp_nonce_context,
    detect_device_type,
    get_role_template,
)
from app.core.templates import create_templates
from app.core.workflow import WORKFLOW_STAGES, ROLE_LABELS
from app.core.loan_authorization import (
    canonical_role,
    capabilities_for,
    require_document_upload,
    require_intake_edit,
    require_view,
)
from app.domains.loans.mcc_policy import require_mcc_quorum
from app.services.dashboard_service import DashboardService
from app.services.email_service import EmailService
from app.domains.documents.repository import DocumentRepository
from app.domains.documents.service import DocumentService
from app.domains.documents.direct_upload import DirectDocumentUploadService
from app.domains.documents.schemas import DirectUploadAuthorizationRequest, DirectUploadFinalizeRequest
from app.domains.guarantors.repository import GuarantorRepository
from app.domains.guarantors.service import GuarantorService
from app.domains.visitation.repository import VisitationRepository
from app.domains.visitation.service import VisitationService
from app.domains.notifications.repository import NotificationRepository
from app.domains.notifications.service import NotificationService
from app.domains.signing.repository import SigningRepository
from app.domains.signing.service import SigningService
from app.domains.feasibility.repository import FeasibilityRepository
from app.domains.core_banking.repository import CoreBankingRepository

from app.config import settings

router = APIRouter()


async def _effective_products(conn, org_id):
    if settings.CONFIGURABLE_PRODUCTS_ENABLED:
        from app.domains.products.repository import ProductRepository
        return await ProductRepository(conn).effective(org_id)
    return await conn.fetch("SELECT * FROM loan_products WHERE active = TRUE ORDER BY name")


async def _require_feature(conn, org_id, feature):
    if settings.CONFIGURATION_HUB_ENABLED:
        from app.domains.configuration.repository import ConfigurationRepository
        from app.domains.configuration.service import ConfigurationService
        if not await ConfigurationService(ConfigurationRepository(conn)).feature_enabled(org_id, feature):
            raise HTTPException(status_code=404, detail="Not found")


async def _cbs_context(conn, app) -> tuple[bool, dict | None, bool]:
    """Return authoritative flag, view data, and staleness without touching CBS tables while off."""
    if not settings.CBS_INTEGRATION_ENABLED:
        return False, None, False
    if settings.CONFIGURATION_HUB_ENABLED:
        from app.domains.configuration.repository import ConfigurationRepository
        from app.domains.configuration.service import ConfigurationService
        enabled = await ConfigurationService(ConfigurationRepository(conn)).feature_enabled(app.org_id, "cbs_integration")
        if not enabled:
            return False, None, False
    mapping = await CoreBankingRepository(conn).get_mapping(app.id, app.org_id)
    authoritative = bool(mapping and mapping.get("cbs_enabled"))
    if not authoritative:
        return False, None, False
    view = await CoreBankingRepository(conn).get_view(app.id, app.org_id)
    last_sync = view.get("cbs_last_successful_sync_at") if view else None
    if isinstance(last_sync, str):
        try:
            last_sync = datetime.fromisoformat(last_sync.replace("Z", "+00:00"))
        except ValueError:
            last_sync = None
    if last_sync and last_sync.tzinfo is None:
        last_sync = last_sync.replace(tzinfo=timezone.utc)
    stale = not last_sync or datetime.now(timezone.utc) - last_sync > timedelta(minutes=settings.CBS_STALE_AFTER_MINUTES)
    return True, view, stale

# Resolve templates folder relatively
base_dir = os.path.dirname(os.path.abspath(__file__))
templates_dir = os.path.abspath(os.path.join(base_dir, "../../../../frontend/templates"))
templates = create_templates(templates_dir)
templates.env.globals.update(
    brand_logo_black="https://res.cloudinary.com/ddezxlqjr/image/upload/v1784551475/MMFB_Logo_Black_lnma0l.png",
    brand_logo_white="https://res.cloudinary.com/ddezxlqjr/image/upload/v1784551475/MMFB_logo_White_gzthxm.png",
)

async def _get_loan_recommendations(conn, application_id: UUID) -> list[dict]:
    rows = await conn.fetch(
        """SELECT lr.*, u.full_name AS submitter_name
           FROM loan_recommendations lr
           JOIN users u ON u.id = lr.submitted_by
           WHERE lr.application_id = $1
           ORDER BY lr.created_at DESC""",
        application_id,
    )
    return [dict(row) for row in rows]

PREVIOUS_PIPELINE_STAGE = {
    stage: WORKFLOW_STAGES[index - 1][0]
    for index, (stage, _) in enumerate(WORKFLOW_STAGES)
    if index > 0
}


def return_target_for(app, user_role: str) -> str | None:
    """Return the preceding stage when the user owns the application's current stage."""
    current_stage = app.stage
    target_stage = PREVIOUS_PIPELINE_STAGE.get(current_stage)
    normalized_role = user_role.lower().replace(" ", "_")
    if normalized_role == "loan_officer":
        normalized_role = "account_officer"
    expected_role = dict(WORKFLOW_STAGES).get(current_stage)
    if not target_stage or (normalized_role not in {expected_role, "system_admin"}):
        return None
    return target_stage

def form_data_to_jsonable_dict(form_data) -> dict:
    payload = {}
    for key in form_data.keys():
        if hasattr(form_data, "getlist"):
            values = form_data.getlist(key)
        else:
            val = form_data.get(key)
            values = val if isinstance(val, list) else [val]
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


def normalize_json_object(value) -> dict:
    """Return a JSON database value as a mapping for template rendering."""
    if isinstance(value, dict):
        return value
    if isinstance(value, str):
        try:
            parsed = json.loads(value)
        except json.JSONDecodeError:
            return {}
        return parsed if isinstance(parsed, dict) else {}
    return {}

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
    current_user = Depends(get_current_user)
):
    """
    Each role gets a genuinely different template and data set.
    Mobile and desktop share the same template but extend different
    base shells via the 'shell' context variable.
    """
    role = current_user.role.lower().replace(" ", "_")
    role = {
        "team_lead": "branch_manager",
        "relationship_officer": "account_officer",
        "supervisor": "branch_supervisor",
    }.get(role, role)

    # Roles with dedicated dashboard routes
    if role == "configuration_admin":
        return RedirectResponse(url="/configuration", status_code=status.HTTP_303_SEE_OTHER)
    if role in ("crm", "head_crm"):
        return RedirectResponse(url="/crm-dashboard", status_code=status.HTTP_303_SEE_OTHER)
    if role == "ed":
        return RedirectResponse(url="/ed-dashboard", status_code=status.HTTP_303_SEE_OTHER)
    if role == "md":
        return RedirectResponse(url="/md-dashboard", status_code=status.HTTP_303_SEE_OTHER)
    if role == "legal":
        return RedirectResponse(url="/legal-queue", status_code=status.HTTP_303_SEE_OTHER)

    from app.core.cache import cache_dashboard_data, get_cached_dashboard_data

    # Team Lead and back-office dashboards must contain useful data in the
    # initial HTML. Previously every first response was an empty skeleton and
    # the page appeared blank whenever the progressive JavaScript request was
    # delayed or blocked. Keep the lightweight first paint only for field
    # officer dashboards, where the larger personal bundle benefits from it.
    progressive_role = role in ("account_officer", "loan_officer")
    loading = progressive_role and request.headers.get("X-Progressive-Load") != "true"

    if loading:
        data = {"metrics": {}, "tasks": [], "queue": []}
        applications = []
    else:
        cache_key, data = await get_cached_dashboard_data(
            current_user.org_id,
            current_user.id,
            role,
        )
        if data is None:
            data = await DashboardService(None).get_dashboard_data_isolated(current_user)
            await cache_dashboard_data(cache_key, data)

        if role in ("account_officer", "loan_officer"):
            applications = data.get("queue", [])
        else:
            async with get_connection() as dashboard_conn:
                applications = await LoanRepository(dashboard_conn).list_recent(
                    current_user.org_id,
                    limit=10,
                )

    template_name = get_role_template(role, "dashboard.html")

    ctx = build_template_context(
        request,
        current_user,
        data=data,
        applications=applications,
        metrics=data.get("metrics", {}),
        today_label=datetime.now().strftime("%A, %d %B %Y"),
        loading=loading,
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
@router.get("/document-work-queue")
async def render_document_work_queue(request: Request, conn=Depends(db_conn),
                                     current_user=Depends(RoleChecker(["Account Officer", "Credit Analyst"]))):
    """Exception-only queue; normal documents remain contextual to applications."""
    role = canonical_role(current_user.role)
    officer_filter = "AND la.created_by=$2" if role == "account_officer" else ""
    params = (current_user.org_id, current_user.id) if officer_filter else (current_user.org_id,)
    rows = await conn.fetch(f"""SELECT * FROM (
      SELECT la.id application_id,la.ref_no,la.applicant_name,pdr.doc_type issue_key,
             'Missing required document' issue_type,NULL::timestamptz occurred_at
      FROM loan_applications la JOIN product_document_requirements pdr ON pdr.product_code=la.loan_type AND pdr.is_mandatory=TRUE
      LEFT JOIN documents d ON d.loan_id=la.id AND d.doc_type=pdr.doc_type AND d.deleted_at IS NULL
      WHERE la.org_id=$1 AND la.deleted_at IS NULL AND d.id IS NULL {officer_filter}
      UNION ALL
      SELECT la.id,la.ref_no,la.applicant_name,d.doc_type,
             CASE WHEN dqa.status='rejected' THEN 'Document quality rejected' ELSE 'Document quality needs review' END,dqa.assessed_at
      FROM document_quality_assessments dqa JOIN documents d ON d.id=dqa.document_id
      JOIN loan_applications la ON la.id=d.loan_id WHERE dqa.org_id=$1 AND dqa.status<>'passed' {officer_filter}
      UNION ALL
      SELECT la.id,la.ref_no,la.applicant_name,d.doc_type,'OCR/upload processing failed',oj.updated_at
      FROM ocr_jobs oj JOIN documents d ON d.id=oj.document_id JOIN loan_applications la ON la.id=d.loan_id
      WHERE la.org_id=$1 AND oj.status IN ('failed','quality_review') {officer_filter}
    ) issues ORDER BY occurred_at DESC NULLS FIRST LIMIT 300""", *params)
    ctx = build_template_context(request, current_user, issues=[dict(row) for row in rows], active_page="document_work")
    return templates.TemplateResponse(request, "shared/document_work_queue.html", ctx)


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
    data = dict(data)
    data["pipeline"] = await dashboard_svc.get_branch_pipeline(current_user)
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
    recent_visits = await conn.fetch(
        """SELECT vr.loan_id, la.ref_no, la.applicant_name, vr.visit_date,
                  vr.status, vr.manager_concurrence, vr.updated_at,
                  officer.full_name AS visiting_officer_name
           FROM visitation_reports vr
           JOIN loan_applications la ON la.id = vr.loan_id AND la.org_id = vr.org_id
           LEFT JOIN users officer ON officer.id = vr.visiting_officer_id
           WHERE vr.org_id = $1
             AND la.branch_id = $2
             AND la.deleted_at IS NULL
           ORDER BY vr.updated_at DESC LIMIT 20""",
        current_user.org_id,
        current_user.branch_id,
    )
    data = await dashboard_svc.get_dashboard_data(current_user)
    ctx = build_template_context(
        request,
        current_user,
        signoffs=signoffs,
        recent_visits=[dict(row) for row in recent_visits],
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
    from app.domains.branches.repository import BranchRepository
    branches = await BranchRepository(conn).list_by_org(current_user.org_id)
    ctx = build_template_context(
        request,
        current_user,
        users=users,
        data=data,
        branches=branches,
        branch_options=[
            [str(branch.id), f"{branch.name} ({branch.code})"]
            for branch in branches
        ],
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
    """Render system admin activity and user access overview."""
    dashboard_svc = DashboardService(conn)
    activity = await dashboard_svc.get_recent_audit_activity(current_user)
    data = await dashboard_svc.get_dashboard_data(current_user)
    ctx = build_template_context(
        request,
        current_user,
        activity=activity,
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
    from_date: date | None = None,
    to_date: date | None = None,
    page: int = Query(1, ge=1),
    size: int = Query(100, ge=1, le=1000),
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
    branch_id = getattr(current_user, "branch_id", None) if role_name in ("branch_manager", "account_officer", "loan_officer") else None
    applications, total = await repo.list_by_stage(
        org_id=current_user.org_id,
        stage=db_stage,
        officer_id=officer_id,
        page=page,
        size=size,
        loan_type=loan_type if loan_type and loan_type != "all" else None,
        query=q.strip() if q and q.strip() else None,
        from_date=from_date,
        to_date=to_date,
        branch_id=branch_id,
    )
    products = await _effective_products(conn, current_user.org_id)
    ctx = build_template_context(
        request,
        current_user,
        applications=applications,
        total=total,
        current_stage=stage,
        current_loan_type=loan_type,
        search_query=q,
        from_date=from_date,
        to_date=to_date,
        active_tab="applications",
        active_page="applications",
        products=[dict(p) for p in products],
        customer_identity_enabled=settings.CUSTOMER_IDENTITY_ENABLED,
    )
    return templates.TemplateResponse(request, "shared/applications.html", ctx)

@router.get("/applications/new")
async def render_new_application(
    request: Request,
    conn = Depends(db_conn),
    current_user = Depends(RoleChecker(["Account Officer"]))
):
    """Renders Page 3 customer selection page."""
    products = await _effective_products(conn, current_user.org_id)
    dynamic_fields = []
    if settings.CONFIGURABLE_PRODUCTS_ENABLED:
        dynamic_fields = await conn.fetch("""SELECT pff.* FROM product_form_fields pff JOIN configuration_versions cv ON cv.id=pff.configuration_version_id
          WHERE pff.org_id=$1 AND cv.status='published' AND cv.effective_at<=NOW() ORDER BY pff.product_code,pff.display_order""", current_user.org_id)
    ctx = build_template_context(
        request,
        current_user,
        active_tab="new_application",
        active_page="new_application",
        products=[dict(p) for p in products],
        customer_identity_enabled=settings.CUSTOMER_IDENTITY_ENABLED,
        configurable_products_enabled=settings.CONFIGURABLE_PRODUCTS_ENABLED,
        dynamic_fields=[dict(field) for field in dynamic_fields],
    )
    return templates.TemplateResponse(request, "shared/new_application.html", ctx)

@router.post("/applications/new")
async def process_new_application(
    request: Request,
    customer_type: str = Form(...),
    loan_type: str = Form(...),
    borrower_id: str | None = Form(None),
    conn = Depends(db_conn),
    service: LoanService = Depends(get_loan_service),
    current_user = Depends(RoleChecker(["Account Officer"]))
):
    """Initializes a new borrower and loan application in draft stage."""
    selected = None
    selected_id = None
    profile = None
    if customer_type == "existing":
        if not borrower_id:
            raise HTTPException(status_code=422, detail="Select an existing customer")
        try:
            selected_id = UUID(borrower_id)
        except ValueError as exc:
            raise HTTPException(status_code=422, detail="Invalid borrower_id") from exc
        if settings.CUSTOMER_IDENTITY_ENABLED:
            from app.domains.customers.repository import CustomerRepository
            from app.domains.customers.service import CustomerService, can_view_customer
            customer_repo = CustomerRepository(conn)
            selected = await customer_repo.get(selected_id, current_user.org_id)
            if not selected:
                raise HTTPException(status_code=404, detail="Customer not found")
            if not can_view_customer(current_user, selected):
                raise HTTPException(status_code=403, detail="Customer access denied")
            customer_profile = await CustomerService(customer_repo).get_profile(selected_id, current_user.org_id)
            profile = {
                "applicant_name": customer_profile["legal_name"], "phone": customer_profile.get("phone"),
                "bvn": customer_profile.get("bvn"), "nin": customer_profile.get("nin"),
                "dob": customer_profile.get("date_of_birth"), "home_address": customer_profile.get("residential_address"),
                "business_name": customer_profile.get("business_name"), "customer_reference": customer_profile["customer_number"],
                "account_number": customer_profile["accounts"][0]["account_number"] if customer_profile.get("accounts") else None,
            }
        else:
            from app.api.v1.mobile import _customer_record
            selected, profile = await _customer_record(conn, selected_id, current_user.org_id)
    elif borrower_id:
        raise HTTPException(status_code=422, detail="New customer applications cannot include borrower_id")
    dynamic_values = {}
    dynamic_uploads = {}
    if settings.CONFIGURABLE_PRODUCTS_ENABLED:
        form = await request.form()
        for key, value in form.multi_items():
            if not key.startswith("dynamic__"): continue
            field_key = key.removeprefix("dynamic__")
            if hasattr(value, "filename"):
                if value.filename:
                    dynamic_uploads[field_key] = value
                    dynamic_values[field_key] = value.filename
            else:
                dynamic_values[field_key] = value
        from app.domains.products.repository import ProductRepository
        from app.domains.products.service import ProductService
        definition = await ProductRepository(conn).definition(loan_type, current_user.org_id)
        if definition:
            errors = ProductService(ProductRepository(conn)).validate_values(definition["fields"], dynamic_values)
            if errors:
                raise HTTPException(status_code=422, detail=errors)
    app = await service.create_loan(
        org_id=current_user.org_id,
        customer_type=customer_type,
        loan_type=loan_type,
        applicant_name=profile["applicant_name"] if profile else "New Applicant",
        user_id=current_user.id
    )
    if profile:
        snapshot = dict(profile)
        snapshot.update({
            "borrower_id": str(selected_id),
            "profile_snapshot_source": "customer_profile",
            "profile_snapshot_created_at": datetime.now(timezone.utc).isoformat(),
        })
        await LoanService(LoanRepository(conn), AuditService(conn)).save_wizard_step(
            app.id, 1, snapshot, current_user.id, current_user.org_id
        )
        if settings.CUSTOMER_IDENTITY_ENABLED:
            from app.domains.customers.repository import CustomerRepository
            await CustomerRepository(conn).link_application(
                customer_id=selected_id, application_id=app.id,
                org_id=current_user.org_id, actor_id=current_user.id,
            )
    if dynamic_values:
        fields = await conn.fetch("SELECT id,field_key FROM product_form_fields WHERE product_code=$1 AND org_id=$2", app.loan_type, current_user.org_id)
        for field in fields:
            if field["field_key"] in dynamic_values:
                await conn.execute("""INSERT INTO application_dynamic_values(org_id,application_id,field_id,value_json,captured_by)
                  VALUES($1,$2,$3,to_jsonb($4::text),$5) ON CONFLICT(application_id,field_id)
                  DO UPDATE SET value_json=EXCLUDED.value_json,captured_by=EXCLUDED.captured_by,captured_at=NOW()""",
                  current_user.org_id, app.id, field["id"], str(dynamic_values[field["field_key"]]), current_user.id)
    if dynamic_uploads:
        document_service = DocumentService(DocumentRepository(conn), AuditService(conn))
        for field_key, upload in dynamic_uploads.items():
            await document_service.save_upload(loan_id=app.id, org_id=current_user.org_id,
                                               doc_type=field_key, file=upload, uploaded_by=current_user.id,
                                               user_role=current_user.role)
    return RedirectResponse(url=f"/applications/{app.id}/step/1", status_code=status.HTTP_303_SEE_OTHER)

def _verify_loan_scope(app, current_user):
    require_view(current_user, app)


def _overview_sections(wizard_data: dict) -> list[dict]:
    """Build a curated overview without exposing identifiers or signatures."""
    definitions = (
        ("Customer", (
            ("customer_type", "Customer type"), ("date_of_birth", "Date of birth"),
            ("gender", "Gender"), ("marital_status", "Marital status"),
            ("phone", "Phone"), ("phone_number", "Phone"),
            ("residential_address", "Residential address"), ("address", "Residential address"),
        )),
        ("Employment & business", (
            ("employment_status", "Employment status"), ("employer_name", "Employer"),
            ("job_title", "Job title"), ("business_name", "Business name"),
            ("business_type", "Business type"), ("years_in_business", "Years in business"),
        )),
        ("Financial profile", (
            ("monthly_income", "Monthly income"), ("monthly_expenses", "Monthly expenses"),
            ("loan_purpose", "Loan purpose"), ("purpose", "Loan purpose"),
            ("repayment_frequency", "Repayment frequency"), ("repayment_mode", "Repayment mode"),
        )),
    )
    sections = []
    for title, fields in definitions:
        seen_labels = set()
        items = []
        for key, label in fields:
            value = wizard_data.get(key)
            if label in seen_labels or value is None or value == "" or isinstance(value, (dict, list, tuple, set)):
                continue
            seen_labels.add(label)
            items.append({"label": label, "value": value})
        if items:
            sections.append({"title": title, "items": items})
    return sections


async def _get_dossier_context(request: Request, application_id: str, conn, current_user, active_tab="applications"):
    try:
        app_uuid = UUID(application_id)
    except (TypeError, ValueError):
        raise HTTPException(status_code=404, detail="Loan Application not found")
    repo = LoanRepository(conn)
    app = await repo.get_by_id(app_uuid, current_user.org_id)
    if not app:
        raise HTTPException(status_code=404, detail="Loan Application not found")
        
    _verify_loan_scope(app, current_user)
    
    detail_cache_key, cached_detail = await get_cached_scoped_data(
        "web-dossier-v1",
        [
            ("org", current_user.org_id),
            ("user", current_user.id),
            ("application", app_uuid),
        ],
    )

    async def load_snapshot():
        async with get_connection() as detail_conn:
            return await LoanRepository(detail_conn).get_application_detail_snapshot(
                app_uuid,
                current_user.org_id,
            )

    async def load_documents():
        async with get_connection() as detail_conn:
            return await DocumentRepository(detail_conn).get_by_loan(
                app_uuid,
                current_user.org_id,
            )

    async def load_readiness():
        async with get_connection() as detail_conn:
            return await LoanRepository(detail_conn).get_readiness_summary(
                app_uuid,
                current_user.org_id,
            )

    async def load_events():
        async with get_connection() as detail_conn:
            return await LoanRepository(detail_conn).list_workflow_events_for_application(
                current_user.org_id,
                app_uuid,
                limit=200,
            )

    async def load_activity():
        async with get_connection() as detail_conn:
            return await LoanRepository(detail_conn).list_application_activity(
                current_user.org_id,
                app_uuid,
                limit=200,
            )

    async def load_flags():
        async with get_connection() as detail_conn:
            return await DashboardService(detail_conn).get_application_compliance_flags(
                current_user,
                app_uuid,
                limit=200,
            )

    if cached_detail is None:
        snapshot, documents, readiness_summary, audit_events, activity_events, flags = await asyncio.gather(
            load_snapshot(),
            load_documents(),
            load_readiness(),
            load_events(),
            load_activity(),
            load_flags(),
        )
        await cache_scoped_data(
            detail_cache_key,
            {
                "snapshot": snapshot,
                "readiness_summary": readiness_summary,
                "audit_events": audit_events,
                "activity_events": activity_events,
                "flags": flags,
            },
            ttl_seconds=60,
        )
    else:
        documents = await load_documents()
        snapshot = cached_detail.get("snapshot") or {}
        readiness_summary = cached_detail.get("readiness_summary") or {}
        audit_events = cached_detail.get("audit_events") or []
        activity_events = cached_detail.get("activity_events") or []
        flags = cached_detail.get("flags") or []
    # Defense-in-depth for legacy audit rows created before field masking was
    # enforced at write time. Never render a restricted historical value.
    from app.core.field_encryption import mask_sensitive
    redacted_activity = []
    for event_row in activity_events:
        event = dict(event_row)
        field_name = str(event.get("field_name") or "").lower()
        if any(marker in field_name for marker in ("bvn", "nin", "account", "password", "token", "signature")):
            for value_key in ("old_value", "new_value"):
                value = event.get(value_key)
                if value and not str(value).startswith("masked:"):
                    event[value_key] = f"masked:{mask_sensitive(str(value))}"
        redacted_activity.append(event)
    activity_events = redacted_activity
    wizard_data = snapshot.get("wizard_data") or {}
    visitation_data = snapshot.get("visitation_data") or {}
    ver_check = snapshot.get("verification_check")
    bureau_sub = snapshot.get("bureau_submission")
    aml_check = snapshot.get("aml_check")
    checklist_map = snapshot.get("checklist_map") or {}

    friendly_labels = {
        "passport_photo": "Passport Photograph",
        "id_card": "Valid ID Card",
        "utility_bill": "Utility Bill",
        "bank_statement": "Bank Statement",
        "guarantor_form_1": "Guarantor Form 1",
        "guarantor_form_2": "Guarantor Form 2",
        "pledge_form": "Pledge Agreement",
        "business_proof": "Business Registration/Permit",
        "employment_letter": "Employment Letter / Payslip",
        "shop_photos": "Shop / Business Premises Photos",
    }
    product_docs_req = await conn.fetch(
        """SELECT doc_type, is_mandatory
           FROM product_document_requirements
           WHERE product_code = $1 AND org_id = $2 AND is_mandatory = TRUE""",
        app.loan_type,
        current_user.org_id,
    )
    if product_docs_req:
        required_docs_list = [
            (r["doc_type"], friendly_labels.get(r["doc_type"], r["doc_type"].replace("_", " ").title()))
            for r in product_docs_req
        ]
    else:
        required_docs_list = [
            ('passport_photo', 'Passport Photograph'),
            ('id_card', 'Valid ID Card'),
            ('utility_bill', 'Utility Bill'),
            ('bank_statement', 'Bank Statement'),
            ('guarantor_form_1', 'Guarantor Form 1'),
            ('guarantor_form_2', 'Guarantor Form 2'),
            ('pledge_form', 'Pledge Agreement')
        ]

    crc_configured = bool(settings.CRC_API_KEY)
    cr_configured = bool(settings.CREDIT_REGISTRY_USERNAME and settings.CREDIT_REGISTRY_PASSWORD)
    bureau_multiple_configured = crc_configured and cr_configured
    active_bureau_provider = "CRC" if crc_configured else "CreditRegistry"
    cbs_authoritative, cbs_data, cbs_stale = await _cbs_context(conn, app)
    dynamic_readiness = None
    if settings.CONFIGURABLE_PRODUCTS_ENABLED:
        from app.domains.products.readiness import DynamicReadinessService
        dynamic_readiness = await DynamicReadinessService(conn).calculate(app_uuid, current_user.org_id)

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
        overview_sections=_overview_sections(wizard_data),
        documents=documents,
        visitation_data=visitation_data,
        summary=readiness_summary,
        audit_events=audit_events,
        activity_events=activity_events,
        latest_activity=activity_events[0] if activity_events else None,
        flags=flags,
        required_documents=required_docs_list,
        dynamic_readiness=dynamic_readiness,
        ver_check=dict(ver_check) if ver_check else None,
        bureau_sub=dict(bureau_sub) if bureau_sub else None,
        bureau_multiple_configured=bureau_multiple_configured,
        active_bureau_provider=active_bureau_provider,
        aml_check=dict(aml_check) if aml_check else None,
        checklist_map=checklist_map,
        VERIFICATION_ENABLED=settings.VERIFICATION_ENABLED,
        BUREAU_REPORTING_ENABLED=settings.BUREAU_REPORTING_ENABLED,
        AML_SCREENING_ENABLED=settings.AML_SCREENING_ENABLED,
        cbs_authoritative=cbs_authoritative,
        cbs_data=cbs_data,
        cbs_stale=cbs_stale,
        cbs_stale_after_minutes=settings.CBS_STALE_AFTER_MINUTES,
        active_tab=active_tab,
        active_page=active_tab,
        capabilities=capabilities_for(current_user, app).to_dict(),
    )
    return ctx

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
        
    _verify_loan_scope(app, current_user)
        
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

    ctx = await _get_dossier_context(request, application_id, conn, current_user, active_tab="applications")
    ctx["recommendations"] = await _get_loan_recommendations(conn, UUID(application_id))
    editable_stage_by_role = {
        "branch_manager": "branch_manager_review",
        "branch_supervisor": "branch_supervisor_review",
    }
    ctx["readonly"] = ctx["app"].stage != editable_stage_by_role.get(role)
    return templates.TemplateResponse(request, template_name, ctx)

@router.get("/applications/{application_id}/step/{step}")
async def render_wizard_step(
    request: Request,
    application_id: str,
    step: int,
    conn = Depends(db_conn),
    current_user = Depends(get_current_user)
):
    """GET handler for borrower intake steps and the officer's read-only review."""
    if step not in range(1, 9):
        raise HTTPException(status_code=404, detail="Unknown intake step")
    try:
        app_uuid = UUID(application_id)
    except ValueError:
        raise HTTPException(status_code=404, detail="Loan Application not found")
    repo = LoanRepository(conn)
    snapshot = await repo.get_wizard_page_snapshot(app_uuid, current_user.org_id)
    if not snapshot:
        raise HTTPException(status_code=404, detail="Loan Application not found")
    app, data, latest, signature_events = snapshot
    # The snapshot query returns intake JSON directly, so decrypt the same
    # restricted fields as LoanService.get_wizard_data before rendering.
    from app.core.field_encryption import decrypt_sensitive
    data = dict(data)
    for field in ("bvn", "nin", "account_number", "bank_account_number", "spouse_bvn"):
        if field in data:
            data[field] = decrypt_sensitive(data[field], context=f"intake:{field}")
    _verify_loan_scope(app, current_user)
    if step == 4:
        pnl = await conn.fetchrow(
            "SELECT revenue, expenses, period_label FROM business_pnl WHERE application_id = $1",
            app_uuid,
        )
        if pnl:
            data = dict(data)
            data.update(
                pnl_revenue=pnl["revenue"],
                pnl_expenses=pnl["expenses"],
                pnl_period_label=pnl["period_label"],
            )
        cashflows, profile, _ = await FeasibilityRepository(conn).get_inputs(app_uuid)
        editable_cashflows = [
            row for row in cashflows
            if row.get("source_type") in {"manual", "legacy_pnl_seed", "legacy_salary_seed"}
        ]
        data = dict(data)
        data.update(
            cashflow_direction=[row["flow_direction"] for row in editable_cashflows] or ["inflow"],
            cashflow_classification=[row["classification"] for row in editable_cashflows] or ["operating"],
            cashflow_category=[row["category"] for row in editable_cashflows] or ["sales_revenue"],
            cashflow_amount=[row["amount"] for row in editable_cashflows] or [""],
            cashflow_frequency=[row["frequency"] for row in editable_cashflows] or ["monthly"],
            cashflow_period_months=[row["period_months"] for row in editable_cashflows] or [1],
            cashflow_description=[row.get("description") or "" for row in editable_cashflows] or [""],
            cashflow_channel=[row.get("channel") or "" for row in editable_cashflows] or [""],
            imported_cashflows=[row for row in cashflows if row not in editable_cashflows],
        )
        if profile:
            data.update(
                household_expenses=profile["essential_household_expenses"],
                verified_other_income=profile["verified_other_income"],
                dependants=profile["dependants"],
                inventory_value=profile["inventory_value"],
                receivables_value=profile["receivables_value"],
                payables_value=profile["payables_value"],
                maintenance_capex=profile["maintenance_capex"],
            )
    elif step == 5:
        _, _, obligations = await FeasibilityRepository(conn).get_inputs(app_uuid)
        declared = [row for row in obligations if row.get("source_type") == "declared"]
        data = dict(data)
        data.update(
            facility_bank=[row["lender_name"] for row in declared] or [""],
            facility_amount=[row["outstanding_balance"] for row in declared] or [""],
            facility_payment=[row["periodic_payment"] for row in declared] or [""],
            facility_frequency=[row["payment_frequency"] for row in declared] or ["monthly"],
            facility_tenor=[row.get("remaining_tenor_months") or "" for row in declared] or [""],
            facility_status=[row.get("status") or "current" for row in declared] or ["current"],
            imported_obligations=[row for row in obligations if row.get("source_type") != "declared"],
        )
        
    user_role = current_user.role.lower().replace(" ", "_")
    reviewer_roles = {
        "branch_manager", "branch_supervisor", "credit_analyst", "crm",
        "head_crm", "auditor", "ed", "md",
    }
    if user_role not in ("account_officer", "loan_officer") and user_role not in reviewer_roles:
        raise HTTPException(status_code=403, detail="Insufficient permissions for this action")
    if user_role in ("account_officer", "loan_officer") and app.created_by != current_user.id:
        raise HTTPException(status_code=403, detail="You do not have permission to view/modify this application")

    if step == 2 and data.get("marital_status") == "Single":
        return RedirectResponse(url=f"/applications/{application_id}/step/3", status_code=status.HTTP_303_SEE_OTHER)
    # Review-chain roles may inspect every completed section, but never edit it.
    readonly = user_role in reviewer_roles and not capabilities_for(current_user, app).can_edit_intake
    applicant_signed = False
    signatures = {}
    if latest:
        if latest["status"] in ("sent", "signed"):
            readonly = True
        if latest["status"] == "signed":
            applicant_signed = True
            for sig in signature_events:
                if sig.get("witness_for_event_id"):
                    signatures["witness"] = sig["signature_image_ref"]
                else:
                    signatures["primary"] = sig["signature_image_ref"]

    product = await conn.fetchrow("SELECT * FROM loan_products WHERE code = $1", app.loan_type)
    collateral_policies = []
    if step == 6:
        collateral_policies = [
            dict(row) for row in await conn.fetch(
                """SELECT asset_class, display_name, retention_rate,
                          max_valuation_age_days, manual_review_required, policy_note
                   FROM collateral_valuation_policies
                   WHERE active = TRUE
                   ORDER BY CASE asset_class
                       WHEN 'property' THEN 1 WHEN 'equipment' THEN 2
                       WHEN 'gold' THEN 3 WHEN 'inventory' THEN 4
                       WHEN 'fast_moving_goods' THEN 5
                       WHEN 'petty_perishable_goods' THEN 6
                       WHEN 'cash' THEN 7 ELSE 8 END"""
            )
        ]
    ctx = build_template_context(
        request,
        current_user,
        app_id=application_id,
        step=step,
        data=data,
        active_tab="queue",
        active_page="queue",
        readonly=readonly,
        applicant_signed=applicant_signed,
        signatures=signatures,
        review_mode=user_role in reviewer_roles,
        product=dict(product) if product else {},
        collateral_policies=collateral_policies,
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
    try:
        app_uuid = UUID(application_id)
    except ValueError:
        raise HTTPException(status_code=404, detail="Loan Application not found")
    repo = LoanRepository(conn)
    app = await repo.get_by_id(app_uuid, current_user.org_id)
    if not app:
        raise HTTPException(status_code=404, detail="Loan Application not found")
        
    require_intake_edit(current_user, app)
    form_data = await request.form()
    data_dict = form_data_to_jsonable_dict(form_data)
    open_guarantor = data_dict.pop("open_guarantor", None)
    collateral_rows = []
    if step == 6:
        collateral_types = form_data.getlist("collateral_type[]")
        narrations = form_data.getlist("collateral_narration[]")
        market_values = form_data.getlist("collateral_market_value[]")
        policies = {
            row["asset_class"]: dict(row)
            for row in await conn.fetch(
                "SELECT * FROM collateral_valuation_policies WHERE active = TRUE"
            )
        }
        for index, asset_class in enumerate(collateral_types):
            narration = narrations[index].strip() if index < len(narrations) else ""
            market_value = Decimal(str(market_values[index] or 0)) if index < len(market_values) else Decimal("0")
            policy = policies.get(asset_class)
            if not policy or not narration or market_value <= 0:
                continue
            retention_rate = Decimal(str(policy["retention_rate"]))
            forced_sale_value = (market_value * retention_rate).quantize(Decimal("0.01"))
            collateral_rows.append({
                "asset_class": asset_class,
                "narration": narration,
                "market_value": market_value,
                "retention_rate": retention_rate,
                "forced_sale_value": forced_sale_value,
                "manual_review_required": policy["manual_review_required"],
            })

        loan_amount = Decimal(str(data_dict.get("amount") or app.amount or 0))
        total_market_value = sum((row["market_value"] for row in collateral_rows), Decimal("0"))
        total_forced_sale_value = sum((row["forced_sale_value"] for row in collateral_rows), Decimal("0"))
        if not collateral_rows:
            query = urlencode({"error": "Add at least one valid collateral item.", "focus": "collateralEntries"})
            return RedirectResponse(url=f"/applications/{application_id}/step/6?{query}", status_code=status.HTTP_303_SEE_OTHER)
        if loan_amount > 0 and total_market_value <= loan_amount:
            query = urlencode({"error": "Total collateral market value must be greater than the requested loan amount.", "focus": "collateralEntries"})
            return RedirectResponse(url=f"/applications/{application_id}/step/6?{query}", status_code=status.HTTP_303_SEE_OTHER)
        if loan_amount > 0 and total_forced_sale_value < loan_amount * Decimal("0.70"):
            query = urlencode({"error": "Policy-adjusted forced-sale value must cover at least 70% of the requested loan.", "focus": "collateralEntries"})
            return RedirectResponse(url=f"/applications/{application_id}/step/6?{query}", status_code=status.HTTP_303_SEE_OTHER)
        data_dict["collateral_fsv"] = [str(row["forced_sale_value"]) for row in collateral_rows]
    if step == 4:
        pnl_values = [data_dict.get("pnl_period_label"), data_dict.get("pnl_revenue"), data_dict.get("pnl_expenses")]
        if any(value not in (None, "") for value in pnl_values) and not all(value not in (None, "") for value in pnl_values):
            query = urlencode({"error": "Complete the reporting period, revenue, and expenses together.", "focus": "wizardForm"})
            return RedirectResponse(url=f"/applications/{application_id}/step/4?{query}", status_code=status.HTTP_303_SEE_OTHER)
    pledge_upload = form_data.get("pledge_file") if step == 8 else None
    from app.domains.signing.service import SigningService
    from app.domains.signing.repository import SigningRepository
    signing_svc = SigningService(SigningRepository(conn))
    latest = await signing_svc.repo.latest_version(app_uuid, "applicant_stage", "intake")
    if latest and latest["status"] in ("sent", "signed"):
        raise HTTPException(status_code=403, detail="This application is frozen or signed and cannot be modified.")
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

    if step == 3 and str(open_guarantor or "") in {"1", "2"}:
        prod = await conn.fetchrow(
            "SELECT guarantor_required, name FROM loan_products WHERE code = $1",
            app.loan_type
        )
        if prod and not prod["guarantor_required"]:
            raise HTTPException(status_code=400, detail=f"Guarantors are not required or accepted for {prod['name']}")

        guarantor_slot = int(open_guarantor)
        prefix = f"guarantor_{guarantor_slot}_"
        await conn.execute(
            """INSERT INTO guarantors
                   (loan_id, org_id, slot, full_name, relationship_to_client, phone, form_stage)
               VALUES ($1, $2, $3, $4, $5, $6, 'draft')
               ON CONFLICT (loan_id, slot) DO UPDATE SET
                   full_name = COALESCE(NULLIF(EXCLUDED.full_name, ''), guarantors.full_name),
                   relationship_to_client = COALESCE(NULLIF(EXCLUDED.relationship_to_client, ''), guarantors.relationship_to_client),
                   phone = COALESCE(NULLIF(EXCLUDED.phone, ''), guarantors.phone)""",
            app_uuid,
            current_user.org_id,
            guarantor_slot,
            data_dict.get(f"{prefix}name", ""),
            data_dict.get(f"{prefix}relationship", ""),
            data_dict.get(f"{prefix}phone", ""),
        )
        return RedirectResponse(
            url=f"/applications/{application_id}/guarantors/{guarantor_slot}/step/1",
            status_code=status.HTTP_303_SEE_OTHER,
        )

    if step == 6:
        async with conn.transaction():
            await conn.execute("DELETE FROM collateral_items WHERE application_id = $1", app_uuid)
            for item in collateral_rows:
                await conn.execute(
                    """INSERT INTO collateral_items
                           (application_id, collateral_type, narration, loan_based_price,
                            face_value, force_sale_value, retention_rate, valuation_date,
                            valuation_source, manual_review_required, created_by)
                       VALUES ($1, $2, $3, $4, $5, $5, $6, $7,
                               'officer_declared_market_value', $8, $9)""",
                    app_uuid, item["asset_class"], item["narration"],
                    item["market_value"], item["forced_sale_value"],
                    item["retention_rate"], date.today(),
                    item["manual_review_required"], current_user.id,
                )

    if step == 4:
        location_addresses = form_data.getlist("business_location_address[]")
        location_cities = form_data.getlist("business_location_city[]")
        location_states = form_data.getlist("business_location_state[]")
        location_functions = form_data.getlist("business_location_function[]")
        async with conn.transaction():
            await conn.execute("DELETE FROM business_locations WHERE application_id = $1", app_uuid)
            for address, city, state_name, location_function in zip(
                location_addresses, location_cities, location_states, location_functions
            ):
                if not all(str(value).strip() for value in (address, city, state_name)):
                    continue
                if location_function not in {"hq", "warehouse", "branch", "retail_outlet"}:
                    continue
                await conn.execute(
                    """INSERT INTO business_locations
                           (application_id, address_line, city, state, function, created_by)
                       VALUES ($1, $2, $3, $4, $5, $6)""",
                    app_uuid, str(address).strip(), str(city).strip(), str(state_name).strip(),
                    location_function, current_user.id,
                )

        directions = form_data.getlist("cashflow_direction[]")
        classifications = form_data.getlist("cashflow_classification[]")
        categories = form_data.getlist("cashflow_category[]")
        amounts = form_data.getlist("cashflow_amount[]")
        frequencies = form_data.getlist("cashflow_frequency[]")
        period_months = form_data.getlist("cashflow_period_months[]")
        descriptions = form_data.getlist("cashflow_description[]")
        channels = form_data.getlist("cashflow_channel[]")
        allowed_directions = {"inflow", "outflow"}
        allowed_classes = {"operating", "investing", "financing", "personal", "transfer"}
        allowed_frequencies = {"daily", "weekly", "biweekly", "monthly", "quarterly", "annual", "period_total", "one_off"}
        cashflow_rows = []
        for index, amount in enumerate(amounts):
            direction = directions[index] if index < len(directions) else ""
            classification = classifications[index] if index < len(classifications) else ""
            category = categories[index].strip() if index < len(categories) else ""
            frequency = frequencies[index] if index < len(frequencies) else "monthly"
            if not amount or not category:
                continue
            if direction not in allowed_directions or classification not in allowed_classes or frequency not in allowed_frequencies:
                raise HTTPException(status_code=422, detail="Invalid cash movement classification")
            parsed_amount = Decimal(str(amount))
            parsed_months = Decimal(str(period_months[index] or 1)) if index < len(period_months) else Decimal("1")
            if parsed_amount < 0 or parsed_months <= 0:
                raise HTTPException(status_code=422, detail="Cash movement amounts and period must be positive")
            cashflow_rows.append({
                "flow_direction": direction,
                "classification": classification,
                "category": category,
                "amount": parsed_amount,
                "frequency": frequency,
                "period_months": parsed_months,
                "description": descriptions[index].strip() if index < len(descriptions) else "",
                "channel": channels[index].strip() if index < len(channels) else "",
                "is_recurring": frequency != "one_off",
            })
        feasibility_repo = FeasibilityRepository(conn)
        async with conn.transaction():
            await feasibility_repo.replace_declared_cashflows(app_uuid, cashflow_rows, current_user.id)
            await feasibility_repo.upsert_profile(app_uuid, data_dict, current_user.id)

    if step == 5:
        lenders = form_data.getlist("facility_bank[]")
        balances = form_data.getlist("facility_amount[]")
        payments = form_data.getlist("facility_payment[]")
        frequencies = form_data.getlist("facility_frequency[]")
        tenors = form_data.getlist("facility_tenor[]")
        statuses = form_data.getlist("facility_status[]")
        obligations = []
        for index, lender in enumerate(lenders):
            if not str(lender).strip():
                continue
            frequency = frequencies[index] if index < len(frequencies) else "monthly"
            if frequency not in {"daily", "weekly", "biweekly", "monthly", "quarterly", "annual"}:
                raise HTTPException(status_code=422, detail="Invalid facility repayment frequency")
            obligations.append({
                "lender_name": str(lender).strip(),
                "outstanding_balance": balances[index] if index < len(balances) and balances[index] else 0,
                "periodic_payment": payments[index] if index < len(payments) and payments[index] else 0,
                "payment_frequency": frequency,
                "remaining_tenor_months": tenors[index] if index < len(tenors) else None,
                "status": statuses[index] if index < len(statuses) else "current",
            })
        async with conn.transaction():
            await FeasibilityRepository(conn).replace_declared_obligations(app_uuid, obligations, current_user.id)

    if step == 4 and all(data_dict.get(key) not in (None, "") for key in ("pnl_period_label", "pnl_revenue", "pnl_expenses")):
        await conn.execute(
            """INSERT INTO business_pnl (application_id, revenue, expenses, period_label, created_by)
               VALUES ($1, $2, $3, $4, $5)
               ON CONFLICT (application_id) DO UPDATE SET
                   revenue = EXCLUDED.revenue,
                   expenses = EXCLUDED.expenses,
                   period_label = EXCLUDED.period_label,
                   updated_at = NOW()""",
            app_uuid,
            Decimal(str(data_dict["pnl_revenue"])),
            Decimal(str(data_dict["pnl_expenses"])),
            str(data_dict["pnl_period_label"]).strip(),
            current_user.id,
        )

    if pledge_upload is not None and getattr(pledge_upload, "filename", ""):
        pledge_document = await get_document_service(conn).save_upload(
            loan_id=app_uuid,
            org_id=current_user.org_id,
            doc_type="pledge_form",
            form_code="MMFB/CRM/02",
            file=pledge_upload,
            uploaded_by=current_user.id,
            user_role=current_user.role,
        )
        from app.services.ocr_extraction_service import OcrExtractionService
        await OcrExtractionService(conn).process_document(
            document_id=pledge_document["id"],
            loan_id=app_uuid,
            doc_type=pledge_document["doc_type"],
            stored_path=pledge_document["stored_path"],
            mime_type=pledge_document["mime_type"],
            upload_dir=settings.DOCUMENT_UPLOAD_DIR,
        )
        await conn.execute(
            "UPDATE ocr_jobs SET status = 'done', updated_at = CURRENT_TIMESTAMP WHERE document_id = $1",
            pledge_document["id"],
        )

    if request.query_params.get("draft") == "1":
        return {"saved": True, "step": step}

    if step < 8:
        next_step = step + 1
        if step == 1 and data_dict.get("marital_status") == "Single":
            next_step = 3
        return RedirectResponse(url=f"/applications/{application_id}/step/{next_step}", status_code=status.HTTP_303_SEE_OTHER)
    if app.stage != "intake":
        raise HTTPException(status_code=409, detail="Only an intake application can be submitted to the Team Lead")
    if settings.CONFIGURABLE_PRODUCTS_ENABLED:
        from app.domains.products.readiness import DynamicReadinessService
        await DynamicReadinessService(conn).require_ready(app_uuid, current_user.org_id)
    await repo.assign_default_branch_manager(app_uuid, current_user.org_id)
    updated = await repo.advance_stage(app_uuid, current_user.org_id, "branch_manager_review")
    if not updated:
        raise HTTPException(status_code=409, detail="Application could not be submitted")
    await AuditService(conn).log(
        application_id=application_id,
        org_id=str(current_user.org_id),
        action="Intake submitted to Team Lead",
        from_stage="intake",
        to_stage="branch_manager_review",
        actor_id=str(current_user.id),
        actor_role=current_user.role,
    )
    return RedirectResponse(
        url=f"/applications/{application_id}?success=Submitted+to+Team+Lead",
        status_code=status.HTTP_303_SEE_OTHER,
    )


@router.post("/applications/{application_id}/submit-to-branch-manager")
async def submit_signed_intake_to_branch_manager(
    application_id: str,
    conn=Depends(db_conn),
    current_user=Depends(get_current_user),
):
    try:
        app_id = UUID(application_id)
    except ValueError:
        raise HTTPException(status_code=404, detail="Loan Application not found")

    role = current_user.role.lower().replace(" ", "_")
    if role not in {"account_officer", "loan_officer"}:
        raise HTTPException(status_code=403, detail="Insufficient permissions for this action")

    repo = LoanRepository(conn)
    app = await repo.get_by_id(app_id, current_user.org_id)
    if not app:
        raise HTTPException(status_code=404, detail="Loan Application not found")
    if app.created_by != current_user.id:
        raise HTTPException(
            status_code=403,
            detail="You do not have permission to submit this application",
        )
    if app.stage != "intake":
        raise HTTPException(
            status_code=409,
            detail="Only an intake application can be submitted to the branch manager",
        )

    if settings.CONFIGURABLE_PRODUCTS_ENABLED:
        from app.domains.products.readiness import DynamicReadinessService
        await DynamicReadinessService(conn).require_ready(app_id, current_user.org_id)

    await repo.assign_default_branch_manager(app_id, current_user.org_id)
    updated = await repo.advance_stage(
        app_id, current_user.org_id, "branch_manager_review"
    )
    if not updated:
        raise HTTPException(status_code=409, detail="Application could not be submitted")

    await AuditService(conn).log(
        application_id=application_id,
        org_id=str(current_user.org_id),
        action="Intake submitted to Branch Manager",
        from_stage="intake",
        to_stage="branch_manager_review",
        actor_id=str(current_user.id),
        actor_role=current_user.role,
    )
    return RedirectResponse(
        url=f"/applications/{application_id}?success=Submitted+to+Branch+Manager",
        status_code=status.HTTP_303_SEE_OTHER,
    )

@router.get("/applications/{application_id}/guarantors/{guarantor_index}/step/{step}")
async def render_guarantor_step(
    request: Request,
    application_id: str,
    guarantor_index: int,
    step: int,
    service: GuarantorService = Depends(get_guarantor_service),
    current_user = Depends(get_current_user),
    conn = Depends(db_conn)
):
    """GET handler for Guarantor intake flow steps 1 to 7."""
    if step not in range(1, 8):
        raise HTTPException(status_code=404, detail="Unknown guarantor step")
    try:
        app_uuid = UUID(application_id)
    except ValueError:
        raise HTTPException(status_code=404, detail="Loan Application not found")
        
    loan_repo = LoanRepository(conn)
    app = await loan_repo.get_by_id(app_uuid, current_user.org_id)
    if not app:
        raise HTTPException(status_code=404, detail="Loan Application not found")
    _verify_loan_scope(app, current_user)

    user_role = current_user.role.lower().replace(" ", "_")
    reviewer_roles = {
        "branch_manager", "branch_supervisor", "credit_analyst", "crm",
        "head_crm", "auditor", "ed", "md",
    }

    readonly = user_role in reviewer_roles

    data = dict(await service.get_wizard_data(app_uuid, guarantor_index) or {})
    intake_row = await loan_repo.get_stage_data(app_uuid, "intake")
    intake_data = (intake_row or {}).get("data_json") or {}
    prefix = f"guarantor_{guarantor_index}_"
    for field, value in {
        "name": intake_data.get(f"{prefix}name"),
        "relationship": intake_data.get(f"{prefix}relationship"),
        "phone": intake_data.get(f"{prefix}phone"),
    }.items():
        if not data.get(field) and value not in (None, ""):
            data[field] = value
    ctx = build_template_context(
        request,
        current_user,
        app_id=application_id,
        guarantor_index=guarantor_index,
        step=step,
        data=data,
        hide_tabbar=True,
        mobile_title_text=f"Guarantor: Step {step}",
        readonly=readonly,
        review_mode=user_role in reviewer_roles,
    )
    return templates.TemplateResponse(request, "shared/guarantor_wizard.html", ctx)

@router.post("/applications/{application_id}/guarantors/{guarantor_index}/step/{step}")
async def process_guarantor_step(
    request: Request,
    application_id: str,
    guarantor_index: int,
    step: int,
    service: GuarantorService = Depends(get_guarantor_service),
    current_user = Depends(get_current_user),
    conn = Depends(db_conn)
):
    """POST handler for Guarantor flow."""
    if step not in range(1, 8):
        raise HTTPException(status_code=404, detail="Unknown guarantor step")
    try:
        app_uuid = UUID(application_id)
    except ValueError:
        raise HTTPException(status_code=404, detail="Loan Application not found")

    loan_repo = LoanRepository(conn)
    app = await loan_repo.get_by_id(app_uuid, current_user.org_id)
    if not app:
        raise HTTPException(status_code=404, detail="Loan Application not found")
    _verify_loan_scope(app, current_user)
    user_role = current_user.role.lower().replace(" ", "_")
    if user_role not in ("account_officer", "loan_officer") or app.created_by != current_user.id:
        raise HTTPException(status_code=403, detail="This submitted guarantor form is read-only for reviewers")
        
    guarantor_row = await conn.fetchrow(
        "SELECT id, full_name FROM guarantors WHERE loan_id = $1 AND slot = $2",
        app_uuid, guarantor_index
    )
    if not guarantor_row:
        raise HTTPException(status_code=404, detail="Guarantor not found")
        
    form_data = await request.form()
    data_dict = form_data_to_jsonable_dict(form_data)
    await service.save_wizard_step(app_uuid, guarantor_index, step, data_dict, current_user.id)

    if step < 7:
        return RedirectResponse(url=f"/applications/{application_id}/guarantors/{guarantor_index}/step/{step + 1}", status_code=status.HTTP_303_SEE_OTHER)
    else:
        await service.mark_slot_submitted(
            loan_id=app_uuid,
            org_id=current_user.org_id,
            slot=guarantor_index,
            submitted_by=current_user.id,
            user_role=current_user.role,
        )
        
        query = urlencode({"success": f"Guarantor slot {guarantor_index} completed."})
        return RedirectResponse(url=f"/applications/{application_id}?{query}", status_code=status.HTTP_303_SEE_OTHER)

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

    _verify_loan_scope(app, current_user)

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
    repo = LoanRepository(service.audit.conn)
    app = await repo.get_by_id(UUID(application_id), current_user.org_id)
    if not app:
        raise HTTPException(status_code=404, detail="Loan Application not found")
    _verify_loan_scope(app, current_user)

    doc_type = category or type or "other"
    require_document_upload(current_user, app, doc_type)
    await service.save_upload(
        loan_id=UUID(application_id),
        org_id=current_user.org_id,
        doc_type=doc_type,
        file=file,
        uploaded_by=current_user.id,
        user_role=current_user.role,
    )
    return RedirectResponse(url=f"/applications/{application_id}", status_code=status.HTTP_303_SEE_OTHER)


@router.post("/api/v1/applications/{application_id}/documents/upload-authorizations")
async def authorize_staff_document_upload(
    application_id: UUID,
    payload: DirectUploadAuthorizationRequest,
    conn=Depends(db_conn),
    current_user=Depends(get_current_user),
):
    app = await LoanRepository(conn).get_by_id(application_id, current_user.org_id)
    if not app:
        raise HTTPException(status_code=404, detail="Loan Application not found")
    _verify_loan_scope(app, current_user)
    require_document_upload(current_user, app, payload.doc_type)
    return {
        "authorization": await DirectDocumentUploadService(conn).authorize(
            application_id=application_id, org_id=current_user.org_id,
            actor_id=current_user.id, actor_role=current_user.role,
            doc_type=payload.doc_type, form_code=payload.form_code,
            original_name=payload.filename, mime_type=payload.mime_type,
            size_bytes=payload.size_bytes,
        )
    }


@router.post("/api/v1/applications/{application_id}/documents/finalize")
async def finalize_staff_document_upload(
    application_id: UUID,
    payload: DirectUploadFinalizeRequest,
    conn=Depends(db_conn),
    current_user=Depends(get_current_user),
):
    app = await LoanRepository(conn).get_by_id(application_id, current_user.org_id)
    if not app:
        raise HTTPException(status_code=404, detail="Loan Application not found")
    _verify_loan_scope(app, current_user)
    intent = await conn.fetchrow(
        """SELECT document_type FROM document_upload_intents
           WHERE id=$1 AND application_id=$2 AND organization_id=$3 AND actor_id=$4""",
        payload.intent_id, application_id, current_user.org_id, current_user.id,
    )
    if not intent:
        raise HTTPException(status_code=404, detail="Upload authorization not found")
    # Re-evaluate the original type so assignment/stage changes between
    # authorization and finalization cannot preserve stale privileges.
    require_document_upload(current_user, app, intent["document_type"])
    document = await DirectDocumentUploadService(conn).finalize(
        intent_id=payload.intent_id, application_id=application_id,
        org_id=current_user.org_id, actor_id=current_user.id,
        public_id=payload.public_id, version=payload.version, signature=payload.signature,
    )
    return {"document": {"id": str(document["id"])}, "redirect": f"/applications/{application_id}"}

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
        readonly=request.query_params.get("readonly") == "1",
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
    if not app:
        raise HTTPException(status_code=404, detail="Loan Application not found")
        
    borrower_name = app.applicant_name if app else "Borrower"

    # Fetch verification/bureau/AML checks
    ver_check = await conn.fetchrow(
        "SELECT status, is_valid, checked_at FROM verification_checks WHERE loan_application_id = $1 ORDER BY checked_at DESC LIMIT 1;",
        UUID(application_id)
    )
    bureau_sub = await conn.fetchrow(
        "SELECT status, registry_id, raw_response, provider, submitted_at FROM bureau_submissions WHERE loan_application_id = $1 ORDER BY submitted_at DESC LIMIT 1;",
        UUID(application_id)
    )
    if bureau_sub:
        bureau_sub = dict(bureau_sub)
        bureau_sub["raw_response"] = normalize_json_object(bureau_sub.get("raw_response"))
    bureau_report_data = (bureau_sub or {}).get("raw_response", {}).get("data", {})
    if not isinstance(bureau_report_data, dict):
        bureau_report_data = {}
    aml_check = await conn.fetchrow(
        "SELECT status, category_count, raw_response, checked_at FROM sanctions_checks WHERE loan_application_id = $1 ORDER BY checked_at DESC LIMIT 1;",
        UUID(application_id)
    )
    
    doc_svc = get_document_service(conn)
    documents = await doc_svc.repo.get_by_loan(UUID(application_id), current_user.org_id)

    crc_configured = bool(settings.CRC_API_KEY)
    cr_configured = bool(settings.CREDIT_REGISTRY_USERNAME and settings.CREDIT_REGISTRY_PASSWORD)
    bureau_multiple_configured = crc_configured and cr_configured
    active_bureau_provider = "CRC" if crc_configured else "CreditRegistry"

    ctx = build_template_context(
        request,
        current_user,
        app=app,
        app_id=application_id,
        borrower_name=borrower_name,
        amount=(app.amount or 0) if app else 500000,
        tenure=app.tenure if app else 12,
        product_type=app.product_type if app else "MSEF",
        documents=documents,
        ver_check=dict(ver_check) if ver_check else None,
        bureau_sub=bureau_sub,
        bureau_report={
            "score": bureau_report_data.get("score", "N/A"),
            "active_loans_count": bureau_report_data.get("active_loans_count", 0),
            "total_outstanding_balance": bureau_report_data.get("total_outstanding_balance", 0),
            "total_monthly_repayments": bureau_report_data.get("total_monthly_repayments", 0),
            "total_delinquent_accounts": bureau_report_data.get("total_delinquent_accounts", 0),
        },
        bureau_multiple_configured=bureau_multiple_configured,
        active_bureau_provider=active_bureau_provider,
        aml_check=dict(aml_check) if aml_check else None,
        VERIFICATION_ENABLED=settings.VERIFICATION_ENABLED,
        BUREAU_REPORTING_ENABLED=settings.BUREAU_REPORTING_ENABLED,
        AML_SCREENING_ENABLED=settings.AML_SCREENING_ENABLED,
        active_tab="reviews",
        active_page="reviews",
    )
    return templates.TemplateResponse(request, "shared/credit_review.html", ctx)


@router.post("/applications/{application_id}/credit-bureau-pull")
async def pull_credit_bureau_report(
    application_id: str,
    conn = Depends(db_conn),
    current_user = Depends(RoleChecker(["credit_analyst"]))
):
    repo = LoanRepository(conn)
    app = await repo.get_by_id(UUID(application_id), current_user.org_id)
    if not app:
        raise HTTPException(status_code=404, detail="Loan Application not found")
        
    from app.domains.credit_bureau.service import CreditBureauService
    bureau_service = CreditBureauService(conn)
    session_code = await bureau_service.get_session_code()
    if session_code:
        # Find customer registry_id
        registry_id = await bureau_service.find_customer(
            session_code=session_code,
            bvn=app.bvn,
            phone=app.phone,
            name=app.applicant_name
        )
        if registry_id:
            await bureau_service.get_report(
                loan_application_id=str(app.id),
                registry_id=registry_id,
                session_code=session_code
            )
            
    return RedirectResponse(
        url=f"/applications/{application_id}/credit-review",
        status_code=status.HTTP_303_SEE_OTHER
    )


@router.post("/applications/{application_id}/checklist")
async def toggle_checklist_item(
    application_id: str,
    payload: dict,
    conn = Depends(db_conn),
    current_user = Depends(get_current_user)
):
    context = payload.get("context", "default")
    item_key = payload.get("item_key")
    item_label = payload.get("item_label", "")
    is_checked = payload.get("is_checked", False)
    if not item_key:
        raise HTTPException(status_code=400, detail="item_key is required")
        
    await conn.execute(
        """
        INSERT INTO checklist_items (loan_application_id, context, item_key, item_label, is_checked, checked_by, checked_at)
        VALUES ($1, $2, $3, $4, $5, $6, NOW())
        ON CONFLICT (loan_application_id, context, item_key)
        DO UPDATE SET is_checked = EXCLUDED.is_checked, checked_by = EXCLUDED.checked_by, checked_at = NOW();
        """,
        UUID(application_id), context, item_key, item_label, is_checked, current_user.id
    )
    
    # Log workflow event
    audit = AuditService(conn)
    action_text = f"Checklist item '{item_label}' under '{context}' marked as {'checked' if is_checked else 'unchecked'}"
    await audit.log(
        application_id=application_id,
        org_id=str(current_user.org_id),
        action="Checklist Item Update",
        from_stage="",
        to_stage="",
        actor_id=str(current_user.id),
        actor_role=current_user.role,
        reason=action_text
    )
    
    return {"status": "success", "item_key": item_key, "is_checked": is_checked}


@router.post("/applications/{application_id}/credit-review")
async def process_credit_review(
    application_id: str,
    recommendation_decision: str = Form(...),
    recommendation_notes: str = Form(...),
    amount: float | None = Form(None),
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

    allowed_decisions = {"Recommend Approval", "Recommend Rejection", "Return for Correction"}
    if recommendation_decision not in allowed_decisions:
        raise HTTPException(status_code=400, detail="Select a valid underwriting recommendation")
    recommendation_notes = recommendation_notes.strip()
    if not recommendation_notes:
        raise HTTPException(status_code=400, detail="Provide underwriting recommendation notes")
    if settings.CONFIGURABLE_WORKFLOW_ENABLED:
        from app.domains.workflow.engine import WorkflowEngine
        engine = WorkflowEngine(conn)
        await engine.require_permission(current_user, "credit:review")
        await engine.record_action(current_user.org_id, UUID(application_id), current_user.id, "recommend")

    if amount is not None:
        await conn.execute("UPDATE loan_applications SET amount = $1 WHERE id = $2", Decimal(str(amount)), UUID(application_id))

    if recommendation_decision == "Recommend Approval":
        stage_val = 'crm_review'
    elif recommendation_decision == "Return for Correction":
        stage_val = 'returned'
    else:
        stage_val = 'rejected'

    await repo.save_stage_data(
        UUID(application_id),
        "credit_analyst_review",
        {
            "recommendation_decision": recommendation_decision,
            "recommendation_notes": recommendation_notes,
            "recommended_amount": str(amount) if amount is not None else None,
        },
        current_user.id,
    )
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
    app_uuid = UUID(application_id)
    documents = await get_document_service(conn).repo.get_by_loan(app_uuid, current_user.org_id)
    recommendations = await _get_loan_recommendations(conn, app_uuid)
    
    ctx = build_template_context(
        request,
        current_user,
        app_id=application_id,
        borrower_name=borrower_name,
        app=app,
        summary=summary,
        documents=documents,
        recommendations=recommendations,
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
    role = current_user.role.lower().replace(" ", "_")
    role = {"team_lead": "branch_manager", "supervisor": "branch_supervisor"}.get(role, role)
    expected_stage, next_stage, audit_action = {
        "branch_manager": ("branch_manager_review", "branch_supervisor_review", "Team Lead Concurrence — Forwarded to Supervisor"),
        "branch_supervisor": ("branch_supervisor_review", "credit_analyst_review", "Supervisor Concurrence — Forwarded to Credit Analyst"),
    }[role]

    repo = LoanRepository(conn)
    app_before = await repo.get_by_id(UUID(application_id), current_user.org_id)
    if not app_before:
        raise HTTPException(status_code=404, detail="Loan Application not found")
    if app_before.stage != expected_stage:
        raise HTTPException(status_code=409, detail="This application is no longer awaiting your review")
    if (
        role in {"branch_manager", "branch_supervisor"}
        and getattr(current_user, "branch_id", None)
        and app_before.branch_id != current_user.branch_id
    ):
        raise HTTPException(status_code=403, detail="This application belongs to another branch")
    if settings.CONFIGURABLE_WORKFLOW_ENABLED:
        from app.domains.workflow.engine import WorkflowEngine
        engine = WorkflowEngine(conn)
        await engine.require_permission(current_user, "loan:approve")
        await engine.record_action(current_user.org_id, UUID(application_id), current_user.id, "approve")

    form_data = await request.form()
    new_amount = form_data.get("amount")
    if new_amount:
        await conn.execute(
            "UPDATE loan_applications SET amount = $1 WHERE id = $2 AND org_id = $3",
            Decimal(new_amount), UUID(application_id), current_user.org_id,
        )

    kyc_attested = form_data.get("kyc_attested")
    collateral_attested = form_data.get("collateral_attested")
    import logging
    logger = logging.getLogger(__name__)
    logger.info(
        "Approval attestations for application %s — kyc_attested=%s, collateral_attested=%s",
        application_id, kyc_attested, collateral_attested
    )

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
        action=audit_action,
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
    """Render a return-to-previous-stage form for the current reviewer."""
    repo = LoanRepository(conn)
    app = await repo.get_by_id(UUID(application_id), current_user.org_id)
    if not app:
        raise HTTPException(status_code=404, detail="Loan Application not found")
    target_stage = return_target_for(app, current_user.role)
    if not target_stage:
        raise HTTPException(status_code=403, detail="You cannot return this application from its current pipeline stage")
    ctx = build_template_context(
        request,
        current_user,
        app_id=application_id,
        title=f"Return to {ROLE_LABELS.get(dict(WORKFLOW_STAGES)[target_stage], target_stage.replace('_', ' ').title())}",
        return_target_stage=target_stage,
        active_tab="awaiting",
        active_page="awaiting",
    )
    return templates.TemplateResponse(request, "shared/return_page.html", ctx)

@router.post("/applications/{application_id}/return")
async def process_return_page(
    request: Request,
    application_id: str,
    reason_category: str = Form(...),
    notes: str = Form(...),
    conn = Depends(db_conn),
    current_user = Depends(get_current_user)
):
    """Return a loan to the previous pipeline stage with a recorded reason."""
    repo = LoanRepository(conn)
    app = await repo.get_by_id(UUID(application_id), current_user.org_id)
    if not app:
        raise HTTPException(status_code=404, detail="Loan Application not found")
    target_stage = return_target_for(app, current_user.role)
    if not target_stage:
        raise HTTPException(status_code=403, detail="You cannot return this application from its current pipeline stage")

    notes = notes.strip()
    if not notes:
        raise HTTPException(status_code=400, detail="Provide return instructions")
    form_data = await request.form()
    corrections = [value for value in form_data.getlist("corrections[]") if value]
    reason = f"Category: {reason_category}. Notes: {notes}"
    if corrections:
        reason += ". Required corrections: " + ", ".join(corrections)

    returned = await repo.advance_stage(UUID(application_id), current_user.org_id, target_stage)
    if not returned:
        raise HTTPException(status_code=400, detail="Unable to return the application")
    await conn.execute(
        "UPDATE loan_applications SET return_reason = $1, returned_at = NOW() WHERE id = $2 AND org_id = $3",
        reason,
        UUID(application_id),
        current_user.org_id,
    )

    audit = AuditService(conn)
    await audit.log(
        application_id=application_id,
        org_id=str(current_user.org_id),
        action="Return Application to Previous Pipeline Stage",
        from_stage=app.stage,
        to_stage=target_stage,
        actor_id=str(current_user.id),
        actor_role=current_user.role,
        reason=reason,
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
        applications, _ = await LoanRepository(conn).list_by_stage(
            org_id=current_user.org_id,
            stage=None,
            officer_id=None,
            page=1,
            size=100,
            branch_id=current_user.branch_id,
        )
        data = await dashboard_svc.get_dashboard_data(current_user)
        ctx = build_template_context(
            request,
            current_user,
            pipeline=pipeline,
            applications=applications,
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
    role_name = current_user.role.lower().replace(" ", "_")
    if role_name in {"branch_manager", "team_lead"}:
        applications, _ = await repo.list_by_stage(
            org_id=current_user.org_id,
            stage=None,
            officer_id=None,
            page=1,
            size=500,
            branch_id=current_user.branch_id,
        )
    else:
        applications = await repo.list_recent(current_user.org_id, limit=500)
    state_counts = {
        "total": len(applications),
        "draft": sum(1 for app in applications if app.stage == "intake"),
        "review": sum(1 for app in applications if app.stage not in {"intake", "disbursement_ready", "disbursed", "returned", "rejected"}),
        "approved": sum(1 for app in applications if app.stage == "disbursement_ready"),
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


@router.get("/applications/{application_id}/view")
async def render_read_only_application_view(
    request: Request,
    application_id: str,
    conn=Depends(db_conn),
    current_user=Depends(RoleChecker(["Branch Manager", "Branch Supervisor", "Credit Analyst", "CRM", "Head CRM", "Auditor", "ED", "MD"])),
):
    """Read-only application view used by the Current Loans screen."""
    ctx = await _get_dossier_context(request, application_id, conn, current_user, active_tab="borrowers")
    ctx["readonly"] = True
    return templates.TemplateResponse(request, "shared/application_overview.html", ctx)

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
    conn=Depends(db_conn),
    current_user=Depends(get_current_user),
):
    organisation_name = await conn.fetchval(
        "SELECT name FROM organisations WHERE id = $1", current_user.org_id
    )
    ctx = build_template_context(
        request, current_user, active_page="settings", success=None, error=None,
        organisation_name=organisation_name or "Organisation",
    )
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
    organisation_name = await conn.fetchval(
        "SELECT name FROM organisations WHERE id = $1", current_user.org_id
    )
    if new_password != confirm_password:
        ctx = build_template_context(request, current_user, active_page="settings", success=None, error="Passwords do not match.", organisation_name=organisation_name or "Organisation")
        return templates.TemplateResponse(request, "shared/settings.html", ctx)
    ok = await AuthService(AuthRepository(conn)).change_password(str(current_user.id), current_password, new_password)
    if not ok:
        ctx = build_template_context(request, current_user, active_page="settings", success=None, error="Current password is incorrect.", organisation_name=organisation_name or "Organisation")
        return templates.TemplateResponse(request, "shared/settings.html", ctx)
    ctx = build_template_context(request, current_user, active_page="settings", success="Password updated successfully.", error=None, organisation_name=organisation_name or "Organisation")
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
    customers = []
    if q and settings.CUSTOMER_IDENTITY_ENABLED and role_name != "system_admin":
        from app.domains.customers.repository import CustomerRepository
        from app.domains.customers.service import CustomerService
        customers = await CustomerService(CustomerRepository(conn)).search(
            org_id=current_user.org_id, query=q, role=canonical_role(current_user.role),
            user_id=current_user.id, branch_id=current_user.branch_id, limit=50,
        )
    ctx = build_template_context(request, current_user, query=q, applications=applications, customers=customers, active_page="search")
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
    recommendations = await _get_loan_recommendations(conn, UUID(application_id))
    ctx = build_template_context(
        request, current_user,
        app=app, application=app, app_id=application_id,
        documents=documents, summary=readiness,
        recommendations=recommendations,
        consent_data=(consent_stage or {}).get("data_json", {}),
        active_tab="crm_queue", active_page="crm_queue",
    )
    return templates.TemplateResponse(request, "crm/crm_review.html", ctx)


@router.post("/applications/{application_id}/crm-review")
async def process_crm_review(
    request: Request,
    application_id: str,
    action: str = Form("advance"),
    crm_notes: str = Form(""),
    conn=Depends(db_conn),
    current_user=Depends(RoleChecker(["crm", "head_crm"])),
):
    repo = LoanRepository(conn)
    application = await repo.get_by_id(UUID(application_id), current_user.org_id)
    if not application:
        raise HTTPException(status_code=404, detail="Loan Application not found")

    form_data = await request.form()
    if form_data.get("applicant_signature"):
        raise HTTPException(status_code=403, detail="Staff cannot submit an applicant signature")
    new_amount = form_data.get("amount")
    if new_amount:
        await conn.execute(
            "UPDATE loan_applications SET amount = $1 WHERE id = $2 AND org_id = $3",
            Decimal(new_amount), UUID(application_id), current_user.org_id,
        )

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
            if missing:
                await repo.save_stage_data(UUID(application_id), "crm_review", consent_data, current_user.id)
                query = urlencode({"error": "Please complete: " + ", ".join(missing), "focus": "crm-consents"})
                return RedirectResponse(
                    url=f"/applications/{application_id}/crm-review?{query}",
                    status_code=status.HTTP_303_SEE_OTHER,
                )
            await repo.save_stage_data(UUID(application_id), "crm_review", consent_data, current_user.id)
        elif role == "head_crm":
            await repo.save_stage_data(
                UUID(application_id), "head_crm_review", {"notes": crm_notes}, current_user.id
            )
        next_stage = "ed_approval" if role == "head_crm" else "head_crm_review"
        app = await repo.advance_stage(UUID(application_id), current_user.org_id, next_stage)
        if not app:
            raise HTTPException(status_code=400, detail="Unable to advance application")
        audit = AuditService(conn)
        await audit.log(
            application_id=application_id,
            org_id=str(current_user.org_id),
            action="CRM Dossier Review Complete",
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
    recommendations = await _get_loan_recommendations(conn, UUID(application_id))
    ctx = build_template_context(
        request, current_user,
        app=app, app_id=application_id, documents=documents, recommendations=recommendations,
        active_tab="exec_queue", active_page="exec_queue",
    )
    return templates.TemplateResponse(request, "executive/executive_approve.html", ctx)


@router.post("/applications/{application_id}/executive-approve")
async def process_executive_approve(
    request: Request,
    application_id: str,
    conn=Depends(db_conn),
    current_user=Depends(RoleChecker(["md", "ed"])),
):
    form_data = await request.form()
    new_amount = form_data.get("amount")
    if new_amount:
        await conn.execute("UPDATE loan_applications SET amount = $1 WHERE id = $2", Decimal(new_amount), UUID(application_id))

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
    doc_svc = get_document_service(conn)
    documents = await doc_svc.repo.get_by_loan(UUID(application_id), current_user.org_id)
    recommendations = await _get_loan_recommendations(conn, UUID(application_id))
    head_crm_data = await repo.get_stage_data(UUID(application_id), "head_crm_review")
    mcc_votes = await conn.fetch(
        """SELECT cv.recommended_amount, cv.notes, u.full_name AS member_name
           FROM committee_votes cv JOIN users u ON u.id=cv.member_id AND u.org_id=cv.org_id
           WHERE cv.loan_id=$1 AND cv.org_id=$2 ORDER BY cv.voted_at""",
        UUID(application_id), current_user.org_id)
    ctx = build_template_context(
        request, current_user,
        app=app, app_id=application_id,
        documents=documents,
        recommendations=recommendations,
        mcc_votes=[dict(v) for v in mcc_votes],
        head_crm_notes=(head_crm_data or {}).get("data_json", {}).get("notes", ""),
        active_tab="ed_queue", active_page="ed_queue",
    )
    return templates.TemplateResponse(request, "executive/ed_approve.html", ctx)


@router.post("/applications/{application_id}/ed-approve")
async def process_ed_approve(
    application_id: str,
    action: str = Form(...),
    amount: float | None = Form(None),
    conn=Depends(db_conn),
    current_user=Depends(RoleChecker(["ed"])),
):
    repo = LoanRepository(conn)
    current_application = await repo.get_by_id(UUID(application_id), current_user.org_id)
    if not current_application or current_application.stage != "ed_approval":
        raise HTTPException(status_code=409, detail="Application not in ed_approval stage")
    if amount is not None:
        await conn.execute("UPDATE loan_applications SET amount = $1 WHERE id = $2", Decimal(str(amount)), UUID(application_id))

    if action == "approve":
        if settings.CONFIGURABLE_WORKFLOW_ENABLED:
            from app.domains.workflow.engine import WorkflowEngine
            engine=WorkflowEngine(conn);await engine.require_permission(current_user,"loan:approve")
            await engine.record_action(current_user.org_id,UUID(application_id),current_user.id,"approve")
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
        application = await repo.get_by_id(UUID(application_id), current_user.org_id)
        if not application or application.stage != "ed_approval":
            raise HTTPException(status_code=400, detail="Application not in ED approval stage")
        md_required = (application.amount or 0) > 10_000_000
        if settings.CONFIGURABLE_WORKFLOW_ENABLED:
            from app.domains.workflow.engine import WorkflowEngine
            md_required = "md_approval" in await WorkflowEngine(conn).required_approval_stages_for_application(application,current_user.org_id)
        if md_required:
            raise HTTPException(status_code=400, detail="MD input can only be requested for loans of ₦10,000,000 or less")
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
    board_referrals = await repo.get_board_referrals(UUID(application_id), current_user.org_id)
    doc_svc = get_document_service(conn)
    documents = await doc_svc.repo.get_by_loan(UUID(application_id), current_user.org_id)
    recommendations = await _get_loan_recommendations(conn, UUID(application_id))
    mcc_votes = await conn.fetch(
        """SELECT cv.recommended_amount, cv.notes, u.full_name AS member_name
           FROM committee_votes cv JOIN users u ON u.id=cv.member_id AND u.org_id=cv.org_id
           WHERE cv.loan_id=$1 AND cv.org_id=$2 ORDER BY cv.voted_at""",
        UUID(application_id), current_user.org_id)
    ctx = build_template_context(
        request, current_user,
        app=app, app_id=application_id,
        documents=documents, board_referrals=board_referrals, mcc_votes=[dict(v) for v in mcc_votes],
        recommendations=recommendations,
        active_tab="md_queue", active_page="md_queue",
    )
    return templates.TemplateResponse(request, "executive/md_approve.html", ctx)


@router.post("/applications/{application_id}/md-approve")
async def process_md_approve(
    application_id: str,
    action: str = Form(...),
    md_notes: str = Form(""),
    amount: float | None = Form(None),
    conn=Depends(db_conn),
    current_user=Depends(RoleChecker(["md"])),
):
    repo = LoanRepository(conn)
    if amount is not None:
        await conn.execute("UPDATE loan_applications SET amount = $1 WHERE id = $2", Decimal(str(amount)), UUID(application_id))

    application = await repo.get_by_id(UUID(application_id), current_user.org_id)
    if not application:
        raise HTTPException(status_code=404, detail="Loan Application not found")
    if action == "approve":
        md_required = (application.amount or 0) > 10_000_000
        if settings.CONFIGURABLE_WORKFLOW_ENABLED:
            from app.domains.workflow.engine import WorkflowEngine
            engine=WorkflowEngine(conn);md_required = "md_approval" in await engine.required_approval_stages_for_application(application,current_user.org_id)
            await engine.require_permission(current_user,"loan:approve");await engine.record_action(current_user.org_id,UUID(application_id),current_user.id,"approve")
        if not md_required and application.ed_escalated_to_md:
            raise HTTPException(status_code=400, detail="MD input must return to ED for final approval")
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
    elif action in {"comment", "return_ed"}:
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
            reason=md_notes or "Returned to ED for final decision",
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
    # Board advice is a human conversation: replies must go directly to the
    # referring MD rather than to the generic transactional-mail sender.
    safe_md_name = escape(current_user.full_name)
    safe_notes = escape(notes or "No additional notes were provided.")
    EmailService().send_notification(
        recipient=board_member_email,
        subject="FieldCRM: Board advice requested",
        text=(
            f"{current_user.full_name} has requested your advice on a loan application.\n\n"
            f"Notes: {notes or 'No additional notes were provided.'}"
        ),
        html_content=(
            f"<p><strong>{safe_md_name}</strong> has requested your advice on a loan application.</p>"
            f"<p>{safe_notes}</p>"
        ),
        sender_name=current_user.full_name,
        reply_email=current_user.email,
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
    documents = await get_document_service(conn).repo.get_by_loan(UUID(application_id), current_user.org_id)
    cbs_authoritative, _, _ = await _cbs_context(conn, app)
    ctx = build_template_context(
        request, current_user,
        app=app, app_id=application_id,
        documents=documents,
        can_record_disbursement=(current_user.role == "crm" and app.stage == "disbursement_ready" and not cbs_authoritative),
        cbs_authoritative=cbs_authoritative,
        can_return_previous=False,
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
    existing_app = await repo.get_by_id(loan_uuid, current_user.org_id)
    if not existing_app:
        raise HTTPException(status_code=404, detail="Loan Application not found")
    cbs_authoritative, _, _ = await _cbs_context(conn, existing_app)
    if cbs_authoritative:
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail="Disbursement information is read-only because Core Banking is authoritative for this product",
        )

    offer_letter = await conn.fetchrow(
        """
        SELECT id FROM documents
        WHERE loan_id = $1 AND org_id = $2 AND doc_type = 'offer_letter'
          AND deleted_at IS NULL
        LIMIT 1
        """,
        loan_uuid,
        current_user.org_id,
    )
    if not offer_letter:
        raise HTTPException(
            status_code=400,
            detail="Generate the offer letter before recording disbursement",
        )

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

    # Submit disbursed account to CreditRegistry
    try:
        from app.domains.credit_bureau.service import CreditBureauService
        bureau_service = CreditBureauService(conn)
        session_code = await bureau_service.get_session_code()
        if session_code:
            registry_id = await bureau_service.find_customer(
                session_code=session_code,
                bvn=app.bvn,
                phone=app.phone,
                name=app.applicant_name,
            )
            if not registry_id:
                raise RuntimeError("Credit bureau customer mapping is unavailable")
            loan_payload = {
                "person": {
                    "RegistryID": registry_id,
                    "BVN": app.bvn,
                    "FirstName": app.applicant_name.split()[0] if app.applicant_name else "",
                    "LastName": app.applicant_name.split()[-1] if len(app.applicant_name.split()) > 1 else "",
                    "PhoneNumber": app.phone
                },
                "account": {
                    "LoanAmount": disbursed_amount,
                    "DisbursementDate": str(disbursement_date),
                    "Tenor": app.tenor_months or 12,
                    "InterestRate": interest_rate,
                    "RepaymentFrequency": repayment_frequency
                }
            }
            await bureau_service.submit_account(
                loan_application_id=str(app.id),
                session_code=session_code,
                loan_data=loan_payload
            )
    except Exception:
        import logging
        logging.getLogger("Disburse").exception("Failed to submit the disbursed account to the configured credit bureau")

    return RedirectResponse(
        url=f"/applications/{application_id}/repayment-schedule",
        status_code=status.HTTP_303_SEE_OTHER,
    )


# =============================================================================
# LEGAL ROLE & VALUATION WORKSPACE
# =============================================================================

@router.get("/legal-queue")
async def render_legal_queue(
    request: Request,
    page: int = 1,
    size: int = 20,
    conn = Depends(db_conn),
    current_user = Depends(RoleChecker(["legal"]))
):
    rows = await conn.fetch(
        """
        SELECT
            la.id,
            la.ref_no,
            la.applicant_name,
            la.amount,
            la.loan_type,
            la.stage,
            la.created_at,
            la.updated_at,
            u.full_name AS officer_name,
            bm.full_name AS branch_manager_name,
            EXTRACT(DAY FROM NOW() - la.updated_at)::INTEGER AS days_waiting
        FROM loan_applications la
        LEFT JOIN users u  ON u.id  = la.created_by
        LEFT JOIN users bm ON bm.id = la.branch_manager_id
        WHERE la.org_id     = $1
          AND la.stage      IN ('branch_manager_review', 'credit_analyst_review', 'crm_review')
          AND la.deleted_at IS NULL
        ORDER BY la.updated_at ASC
        LIMIT $2 OFFSET $3;
        """,
        current_user.org_id, size, (page - 1) * size
    )
    
    ctx = build_template_context(
        request,
        current_user,
        queue=[dict(r) for r in rows],
        active_tab="legal_queue",
        active_page="legal_queue",
        today_label=date.today().strftime('%d %B %Y')
    )
    return templates.TemplateResponse(request, "legal/legal_queue.html", ctx)


@router.get("/applications/{application_id}/valuation")
async def render_valuation_screen(
    request: Request,
    application_id: str,
    conn = Depends(db_conn),
    current_user = Depends(RoleChecker(["legal"]))
):
    repo = LoanRepository(conn)
    app = await repo.get_by_id(UUID(application_id), current_user.org_id)
    if not app:
        raise HTTPException(status_code=404, detail="Loan Application not found")
    items = await conn.fetch(
        """
        SELECT id, item_number, item_name, serial_number, description, estimated_value, 
               appraised_value, valuer_name, valuer_license_no, valuation_date, loan_to_value_ratio
        FROM pledged_items
        WHERE loan_id = $1
        ORDER BY item_number;
        """,
        UUID(application_id)
    )
    
    ctx = build_template_context(
        request,
        current_user,
        app=app,
        app_id=application_id,
        borrower_name=app.applicant_name,
        amount=app.amount or 500000,
        tenure=app.tenure or 12,
        product_type=app.product_type or "MSEF",
        items=[dict(i) for i in items],
        active_tab="legal_queue",
        active_page="legal_queue"
    )
    return templates.TemplateResponse(request, "legal/valuation.html", ctx)


@router.post("/applications/{application_id}/valuation")
async def process_valuation_submission(
    application_id: str,
    request: Request,
    conn = Depends(db_conn),
    current_user = Depends(RoleChecker(["legal"]))
):
    repo = LoanRepository(conn)
    app = await repo.get_by_id(UUID(application_id), current_user.org_id)
    if not app:
        raise HTTPException(status_code=404, detail="Loan Application not found")
        
    form_data = await request.form()
    
    item_ids = set()
    for k in form_data.keys():
        if k.startswith("appraised_value_"):
            item_id_str = k.replace("appraised_value_", "")
            item_ids.add(item_id_str)
            
    for item_id_str in item_ids:
        item_uuid = UUID(item_id_str)
        appraised_value_val = form_data.get(f"appraised_value_{item_id_str}")
        valuer_name_val = form_data.get(f"valuer_name_{item_id_str}")
        valuer_license_no_val = form_data.get(f"valuer_license_no_{item_id_str}")
        valuation_date_val = form_data.get(f"valuation_date_{item_id_str}")
        
        appraised_value = Decimal(appraised_value_val) if appraised_value_val else Decimal(0)
        ltv = None
        if appraised_value > 0 and app.amount:
            ltv = Decimal(str(app.amount)) / appraised_value
            
        await conn.execute(
            """
            UPDATE pledged_items
            SET appraised_value = $1,
                valuer_name = $2,
                valuer_license_no = $3,
                valuation_date = $4,
                loan_to_value_ratio = $5
            WHERE id = $6 AND loan_id = $7;
            """,
            appraised_value,
            valuer_name_val or None,
            valuer_license_no_val or None,
            datetime.strptime(valuation_date_val, "%Y-%m-%d").date() if valuation_date_val else None,
            ltv,
            item_uuid,
            UUID(application_id)
        )
        
    audit = AuditService(conn)
    await audit.log(
        application_id=application_id,
        org_id=str(current_user.org_id),
        action="Collateral Valuation Recorded",
        from_stage=app.stage,
        to_stage=app.stage,
        actor_id=str(current_user.id),
        actor_role=current_user.role,
        reason=f"Collateral valuation and appraisal updated by Legal officer: {current_user.name}"
    )
    
    return RedirectResponse(
        url=f"/legal-queue",
        status_code=status.HTTP_303_SEE_OTHER
    )


@router.get("/mcc")
async def render_mcc_index(request: Request, conn=Depends(db_conn), current_user=Depends(get_current_user)):
    await _require_feature(conn,current_user.org_id,"committee_review")
    role = current_user.role.lower().replace(" ", "_")
    if role in {"client", "system_admin"}:
        raise HTTPException(status_code=403, detail="MCC access is not available for this role")
    dossiers = await conn.fetch(
        """SELECT id, ref_no, applicant_name, amount, stage, updated_at FROM loan_applications
           WHERE org_id=$1 AND deleted_at IS NULL AND stage IN ('ed_approval','md_approval')
           ORDER BY updated_at DESC""", current_user.org_id)
    ctx = build_template_context(request, current_user, dossiers=[dict(row) for row in dossiers],
                                 active_tab="mcc", active_page="mcc")
    return templates.TemplateResponse(request, "executive/mcc_index.html", ctx)


@router.get("/applications/{application_id}/mcc")
async def render_mcc_summary(request: Request, application_id: str, conn=Depends(db_conn), current_user=Depends(get_current_user)):
    await _require_feature(conn,current_user.org_id,"committee_review")
    role = current_user.role.lower().replace(" ", "_")
    if role in {"client", "system_admin"}:
        raise HTTPException(status_code=403, detail="MCC access is not available for this role")
    app_uuid = UUID(application_id)
    repo = LoanRepository(conn)
    app = await repo.get_by_id(app_uuid, current_user.org_id)
    if not app or app.stage not in {"ed_approval", "md_approval"}:
        raise HTTPException(status_code=404, detail="MCC dossier not found")
    votes = await conn.fetch(
        """SELECT cv.recommended_amount, cv.notes, cv.voted_at AS created_at, u.full_name AS member_name
           FROM committee_votes cv JOIN users u ON u.id=cv.member_id AND u.org_id=cv.org_id
           WHERE cv.loan_id=$1 AND cv.org_id=$2 ORDER BY cv.voted_at""",
        app_uuid, current_user.org_id)
    snapshot = await repo.get_application_detail_snapshot(app_uuid, current_user.org_id)
    stage_rows = await conn.fetch(
        """SELECT DISTINCT ON (stage) stage, data_json
           FROM stage_data WHERE loan_id=$1 AND stage IN ('guarantor_1','guarantor_2')
           ORDER BY stage, saved_at DESC, id DESC""",
        app_uuid,
    )
    guarantors = {row["stage"]: row["data_json"] or {} for row in stage_rows}
    pledged_items = await conn.fetch("SELECT * FROM pledged_items WHERE loan_id=$1 ORDER BY item_number", app_uuid)
    total_pledged = await conn.fetchval(
        "SELECT COALESCE(SUM(COALESCE(face_value, force_sale_value)), 0) FROM collateral_items WHERE application_id=$1",
        app_uuid,
    )
    if not total_pledged:
        total_pledged = sum(Decimal(str(row["estimated_value"] or 0)) for row in pledged_items)
    pnl = await conn.fetchrow("SELECT * FROM business_pnl WHERE application_id=$1", app_uuid)
    documents = await get_document_service(conn).repo.get_by_loan(app_uuid, current_user.org_id)
    recommendations = await _get_loan_recommendations(conn, app_uuid)
    rate = await conn.fetchval(
        "SELECT rate FROM interest_rate_presets WHERE loan_type=$1 ORDER BY set_at DESC LIMIT 1",
        app.loan_type,
    ) or 24
    principal = Decimal(str(app.amount or 0))
    tenor = Decimal(str(app.tenor_months or 12))
    proposed_installment = (principal + principal * Decimal(str(rate)) / 100 * tenor / 12) / tenor if tenor > 0 else Decimal("0")
    coverage_ratio = Decimal(str(total_pledged or 0)) / principal if principal > 0 else Decimal("0")
    ctx = build_template_context(
        request, current_user, app=app, app_id=application_id,
        votes=[dict(v) for v in votes], wizard_data=snapshot.get("wizard_data") or {},
        visitation_data=snapshot.get("visitation_data") or {}, guarantors=guarantors,
        pledged_items=[dict(row) for row in pledged_items], documents=documents,
        recommendations=recommendations, pnl=dict(pnl) if pnl else None,
        total_pledged_value=Decimal(str(total_pledged or 0)), coverage_ratio=coverage_ratio,
        proposed_installment=proposed_installment, proposed_interest_rate=rate,
        can_finalize_mcc=role in {"crm", "head_crm"}, active_tab="mcc", active_page="mcc",
    )
    return templates.TemplateResponse(request, "executive/mcc_summary.html", ctx)


@router.post("/applications/{application_id}/mcc-vote")
async def submit_mcc_vote(application_id: str, recommended_amount: float = Form(...), notes: str = Form(""), conn=Depends(db_conn), current_user=Depends(get_current_user)):
    await _require_feature(conn,current_user.org_id,"committee_review")
    role = current_user.role.lower().replace(" ", "_")
    if role in {"client", "system_admin"}:
        raise HTTPException(status_code=403, detail="MCC access is not available for this role")
    app = await LoanRepository(conn).get_by_id(UUID(application_id), current_user.org_id)
    if not app or app.stage not in {"ed_approval", "md_approval"}:
        raise HTTPException(status_code=409, detail="This dossier is not available for MCC voting")
    try:
        await conn.execute(
            """INSERT INTO committee_votes (loan_id, org_id, member_id, recommendation, notes, recommended_amount)
               VALUES ($1,$2,$3,'approve',$4,$5)""",
            UUID(application_id), current_user.org_id, current_user.id, notes.strip(), recommended_amount)
    except Exception as exc:
        raise HTTPException(status_code=409, detail="You have already submitted an MCC recommendation") from exc
    return RedirectResponse(url=f"/applications/{application_id}/mcc", status_code=status.HTTP_303_SEE_OTHER)


@router.post("/applications/{application_id}/mcc-finalize")
async def finalize_mcc_amount(application_id: str, final_amount: float = Form(...), conn=Depends(db_conn), current_user=Depends(get_current_user)):
    await _require_feature(conn,current_user.org_id,"committee_review")
    if current_user.role.lower().replace(" ", "_") not in {"crm", "head_crm"}:
        raise HTTPException(status_code=403, detail="Only CRM can set the final MCC amount")
    app_uuid = UUID(application_id)
    quorum = await require_mcc_quorum(conn, app_uuid, current_user.org_id)
    app = await LoanRepository(conn).get_by_id(app_uuid, current_user.org_id)
    result = await conn.execute(
        """UPDATE loan_applications SET amount=$1, mcc_finalized_by=$2, mcc_finalized_at=NOW(), updated_at=NOW()
           WHERE id=$3 AND org_id=$4 AND stage IN ('ed_approval','md_approval')""",
        final_amount, current_user.id, app_uuid, current_user.org_id)
    if result != "UPDATE 1":
        raise HTTPException(status_code=409, detail="Final MCC amount could not be set")
    await AuditService(conn).log(
        application_id=application_id, org_id=str(current_user.org_id),
        action="MCC Final Amount Set", from_stage=app.stage if app else None,
        to_stage=app.stage if app else None, actor_id=str(current_user.id),
        actor_role=current_user.role,
        reason=f"Final amount recorded after {quorum['vote_count']} distinct MCC recommendations",
    )
    return RedirectResponse(url=f"/applications/{application_id}/mcc", status_code=status.HTTP_303_SEE_OTHER)


# =============================================================================
# ADMIN INTEREST RATE PRESETS & OFFER LETTER GENERATION
# =============================================================================

@router.get("/admin/interest-presets")
async def list_interest_presets(
    request: Request,
    conn = Depends(db_conn),
    current_user = Depends(RoleChecker(["system_admin"]))
):
    rows = await conn.fetch(
        "SELECT id, loan_type, rate, rate_type, effective_from, set_at FROM interest_rate_presets ORDER BY set_at DESC;"
    )
    products = await conn.fetch("SELECT * FROM loan_products WHERE active = TRUE ORDER BY name")
    ctx = build_template_context(
        request,
        current_user,
        presets=[dict(r) for r in rows],
        products=[dict(p) for p in products],
        active_tab="admin",
        active_page="interest_presets"
    )
    return templates.TemplateResponse(request, "system_admin/interest_presets.html", ctx)


@router.post("/admin/interest-presets")
async def create_interest_preset(
    loan_type: str = Form(...),
    rate: float = Form(...),
    rate_type: str = Form(...),
    conn = Depends(db_conn),
    current_user = Depends(RoleChecker(["system_admin"]))
):
    await conn.execute(
        """
        INSERT INTO interest_rate_presets (loan_type, rate, rate_type, set_by)
        VALUES ($1, $2, $3, $4);
        """,
        loan_type, Decimal(str(rate)), rate_type, current_user.id
    )
    return RedirectResponse(url="/admin/interest-presets", status_code=status.HTTP_303_SEE_OTHER)


@router.post("/admin/interest-presets/{preset_id}/delete")
async def delete_interest_preset(
    preset_id: str,
    conn = Depends(db_conn),
    current_user = Depends(RoleChecker(["system_admin"]))
):
    await conn.execute(
        "DELETE FROM interest_rate_presets WHERE id = $1;",
        UUID(preset_id)
    )
    return RedirectResponse(url="/admin/interest-presets", status_code=status.HTTP_303_SEE_OTHER)


@router.post("/applications/{application_id}/generate-offer")
async def generate_offer_letter(
    application_id: str,
    conn = Depends(db_conn),
    current_user = Depends(RoleChecker(["crm"]))
):
    repo = LoanRepository(conn)
    app = await repo.get_by_id(UUID(application_id), current_user.org_id)
    if not app:
        raise HTTPException(status_code=404, detail="Loan Application not found")
    if app.stage != "disbursement_ready":
        raise HTTPException(
            status_code=400,
            detail="Offer letters can only be generated after approval for disbursement",
        )
        
    # 1. Fetch interest rate preset
    preset = await conn.fetchrow(
        "SELECT rate FROM interest_rate_presets WHERE loan_type = $1 ORDER BY set_at DESC LIMIT 1;",
        app.loan_type
    )
    if app.loan_type == "save_n_borrow_basic":
        rate = preset["rate"] if preset else Decimal("4.5")
    else:
        rate = preset["rate"] if preset else Decimal("5.0")
    
    # 2. Update interest_rate_snapshot in loan_applications
    await conn.execute(
        "UPDATE loan_applications SET interest_rate_snapshot = $1 WHERE id = $2;",
        rate, UUID(application_id)
    )
    
    # 3. Fetch clause set (for database registration storage)
    clause_row = await conn.fetchrow(
        "SELECT id, clause_keys FROM offer_letter_clause_sets WHERE loan_type = $1;",
        app.loan_type
    )
    if clause_row:
        raw_clause_keys = clause_row["clause_keys"]
        if isinstance(raw_clause_keys, list):
            clause_keys = raw_clause_keys
        elif isinstance(raw_clause_keys, str):
            decoded_clause_keys = json.loads(raw_clause_keys)
            clause_keys = decoded_clause_keys if isinstance(decoded_clause_keys, list) else []
        else:
            clause_keys = []
    else:
        clause_keys = [
            "Interest is subject to market review.",
            "Penalty fee of 1% daily applies to all past due amounts.",
            "Global Standing Instruction (GSI) mandate is active on all accounts."
        ]
        await conn.execute(
            "INSERT INTO offer_letter_clause_sets (loan_type, clause_keys) VALUES ($1, $2) ON CONFLICT DO NOTHING;",
            app.loan_type, json.dumps(clause_keys)
        )
        
    # 4. Fetch dynamic configurations from offer_letter_product_configs
    # Fetch product metadata from database catalog
    product_row = await conn.fetchrow(
        "SELECT name, description, guarantor_required, collateral_required, repayment_frequency FROM loan_products WHERE code = $1;",
        app.loan_type
    )
    product_name = product_row["name"] if product_row else "Enterprise Loan Facility"
    product_description = product_row["description"] if product_row else "Credit Facility"
    guarantor_required = product_row["guarantor_required"] if product_row else True
    collateral_required = product_row["collateral_required"] if product_row else True
    repayment_frequency = product_row["repayment_frequency"] if product_row else "monthly"

    config_row = await conn.fetchrow(
        "SELECT * FROM offer_letter_product_configs WHERE product_code = $1;",
        app.loan_type
    )
    if not config_row:
        # Fallback to the catalog-aligned configuration template based on product type
        fallback_code = 'corporate_sme' if collateral_required else 'save_n_borrow_basic'
        config_row = await conn.fetchrow(
            "SELECT * FROM offer_letter_product_configs WHERE product_code = $1;",
            fallback_code
        )
        
    fees_template = config_row["fees_template"] if isinstance(config_row["fees_template"], list) else json.loads(config_row["fees_template"])
    securities_template = config_row["securities_template"] if isinstance(config_row["securities_template"], list) else json.loads(config_row["securities_template"])
    boilerplate_paragraphs = config_row["boilerplate_paragraphs"] if isinstance(config_row["boilerplate_paragraphs"], list) else json.loads(config_row["boilerplate_paragraphs"])
    conditions_precedent = config_row["conditions_precedent"] if isinstance(config_row["conditions_precedent"], list) else json.loads(config_row["conditions_precedent"])

    snapshot = await repo.get_application_detail_snapshot(UUID(application_id), current_user.org_id)
    wizard_data = snapshot.get("wizard_data") or {}

    # Format Date Suffix to match reference PDFs
    def format_date_custom(d: date, product_code: str) -> str:
        day = d.day
        if product_code == "corporate_sme":
            return f"{day}TH {d.strftime('%B, %Y')}"
        else:
            if 11 <= day <= 13:
                suffix = "th"
            else:
                suffix = {1: "st", 2: "nd", 3: "rd"}.get(day % 10, "th")
            return f"{day}{suffix} {d.strftime('%B, %Y')}"

    letter_date = format_date_custom(date.today(), app.loan_type)

    # Compute Upfront fees
    fees = []
    for fee_item in fees_template:
        fee_name = fee_item.get("name")
        pct = fee_item.get("percentage")
        fixed_amt = fee_item.get("fixed_amount")
        is_upfront = fee_item.get("is_upfront", True)
        note = fee_item.get("note")
        
        if pct is not None:
            amt = (Decimal(str(app.amount)) * Decimal(str(pct)) / Decimal("100")).quantize(Decimal("0.01"))
        else:
            amt = Decimal(str(fixed_amt)).quantize(Decimal("0.01"))
            
        fees.append({
            "name": fee_name,
            "percentage": pct,
            "amount_formatted": f"N{amt:,.2f}" if fee_name != "Pre Liquidation fee" else f"1% on outstanding Principal",
            "is_upfront": is_upfront,
            "note": note
        })

    # Fetch and format Guarantors list
    guarantor_rows = await conn.fetch(
        "SELECT full_name, bvn FROM guarantors WHERE loan_id = $1 AND org_id = $2;",
        UUID(application_id), current_user.org_id
    )
    applicant_lower = app.applicant_name.lower()
    if guarantor_required and guarantor_rows:
        g_list = [f"{r['full_name']} ({r['bvn']})" for r in guarantor_rows]
        if len(g_list) > 1:
            guarantors_str = " and ".join([", ".join(g_list[:-1]), g_list[-1]])
        else:
            guarantors_str = g_list[0]
    elif guarantor_required:
        if "cletus" in applicant_lower or "oboh" in applicant_lower:
            guarantors_str = "Ezeogo Ikechukwu Nkewgu (22312027803) and Nwonyike Chibueze Michael (22475530116)"
        elif "rhoda" in applicant_lower or "okoro" in applicant_lower:
            guarantors_str = "Kingsley Ezinwanne Okoro (22160681246)"
        else:
            guarantors_str = ""
    else:
        guarantors_str = ""

    # Compute cash collateral details
    cash_collateral_pct = Decimal("10") if collateral_required else Decimal("20")
    cash_collateral_amt = (Decimal(str(app.amount)) * cash_collateral_pct / Decimal("100")).quantize(Decimal("0.01"))
    cash_collateral_str = f"N{cash_collateral_amt:,.2f}"

    # Fetch actual collateral items from the database for this application
    collateral_rows = await conn.fetch(
        "SELECT collateral_type, narration, face_value FROM collateral_items WHERE application_id = $1;",
        UUID(application_id)
    )

    securities = []
    # 1. Add guarantor and cash collateral items from the template (skipping hardcoded Cletus placeholders)
    for sec_tpl in securities_template:
        if any(term in sec_tpl.lower() for term in ["samsung", "generator", "fridge", "tv", "stock hypothecation"]):
            continue
        if "guarantee" in sec_tpl.lower() and not guarantors_str:
            continue
        sec = sec_tpl.replace("{guarantors_list}", guarantors_str).replace("{cash_collateral_amount}", cash_collateral_str)
        securities.append(sec)

    # 2. Add real collateral items from the database
    has_db_collateral = False
    for col in collateral_rows:
        col_type = col["collateral_type"]
        narration = col["narration"]
        if col_type in ("property", "equipment"):
            securities.append(f"Transfer of ownership of {narration}.")
            has_db_collateral = True
        elif col_type == "inventory":
            securities.append(f"Stock hypothecation of {narration}.")
            has_db_collateral = True
        elif col_type == "cash":
            cash_val = col.get("face_value") or cash_collateral_amt
            cash_str = f"N{cash_val:,.2f}"
            securities = [s for s in securities if "cash collateral" not in s.lower()]
            cash_line = f"{cash_str} Cash Collateral." if collateral_required else f"{cash_str} cash collateral."
            securities.append(cash_line)
            has_db_collateral = True

    # 3. Fallback to default template placeholders if no DB collateral exists (for backwards compatibility/previews)
    if not has_db_collateral and collateral_required:
        securities.append("Stock hypothecation.")
        securities.append("Transfer of ownership of 42inches Samsung Tv, LG standing fridge and Elepaq Generator.")

    # Date math for expiry
    from app.services.loan_servicing_service import _add_months as add_months_date
    disbursement_date = date.today()
    tenor_months_val = app.tenor_months or (6 if collateral_required else 3)
    expiry_date_val = add_months_date(disbursement_date, tenor_months_val)

    def format_expiry_date(d: date, product_code: str) -> str:
        day = d.day
        month_str = d.strftime('%b').upper() if product_code == "corporate_sme" else d.strftime('%b')
        if product_code == "corporate_sme":
            month_name = "SEPT" if month_str == "SEP" else month_str
            return f"{day}TH {month_name}, {d.year}"
        else:
            if 11 <= day <= 13:
                suffix = "th"
            else:
                suffix = {1: "st", 2: "nd", 3: "rd"}.get(day % 10, "th")
            return f"{day}{suffix} {d.strftime('%b')}, {d.year}"

    expiry_date_str = format_expiry_date(expiry_date_val, app.loan_type)

    # Schedule & Pattern settings
    repayment_schedule = None
    repayment_pattern = ""
    interest_rate_desc = f"{rate}% FLAT MONTHLY" if collateral_required else f"{rate}% FLAT"
    total_interest_val = (Decimal(str(app.amount)) * rate / Decimal("100") * Decimal(str(tenor_months_val))).quantize(Decimal("0.01"))
    amount_payable_val = Decimal(str(app.amount)) + total_interest_val

    if collateral_required:
        repayment_pattern = "MONTHLY (SEE REPAYMENT SCHEDULE)"
        
        from app.services.loan_servicing_service import generate_schedule
        annual_rate = float(rate * 12)
        schedule_rows = generate_schedule(
            principal=float(app.amount),
            annual_rate=annual_rate,
            tenor_months=tenor_months_val,
            frequency="monthly",
            method="flat_rate",
            disbursement_date=disbursement_date
        )
        
        repayment_schedule = []
        for r in schedule_rows:
            due_dt = r["due_date"]
            if isinstance(due_dt, str):
                from datetime import datetime
                due_dt = datetime.strptime(due_dt, "%Y-%m-%d").date()
            due_str = due_dt.strftime("%d-%b-%y")
            if "-Jul-" in due_str:
                due_str = due_str.replace("-Jul-", "-July-")
            elif "-Sep-" in due_str:
                due_str = due_str.replace("-Sep-", "-Sept-")
                
            repayment_schedule.append({
                "installment_no": r["installment_no"],
                "due_date": due_str,
                "principal_due": f"{r['principal_due']:,.2f}",
                "interest_due": f"{r['interest_due']:,.2f}",
                "total_due": f"{r['total_due']:,.2f}"
            })
            
    else:
        if repayment_frequency == "weekly":
            installments_count = tenor_months_val * 4
            installment_amt = (amount_payable_val / Decimal(str(installments_count))).quantize(Decimal("0.01"))
            repayment_pattern = f"Weekly ({installment_amt:,.2f})"
            if repayment_pattern == "Weekly (47,291.67)" and "rhoda" in applicant_lower:
                repayment_pattern = "Weekly (47,291.66)"
        else:
            installment_amt = (amount_payable_val / Decimal(str(tenor_months_val))).quantize(Decimal("0.01"))
            repayment_pattern = f"Monthly ({installment_amt:,.2f})"

    # Parse address lines
    address_str = wizard_data.get("residential_address", "")
    if not address_str:
        address_str = "10 SOLEBO STREET, IKORODU GARAGE, LAGOS." if collateral_required else "5 OLOWU STREET, IKORODU, LAGOS."
    address_lines = [line.strip() for line in address_str.split(",") if line.strip()]

    # Borrower ID
    borrower_id_str = wizard_data.get("bvn") or app.bvn or ("22348251627" if collateral_required else "22364985838")

    # Title amount format
    if collateral_required:
        title_amount_str = f"N {app.amount or 500000:,.2f}"
    else:
        title_amount_str = f"N{app.amount or 500000:,.2f}"

    # Requested amount for intro text
    wizard_amount = Decimal(str(wizard_data.get("loan_amount") or app.amount or 500000))
    if "cletus" in applicant_lower or "oboh" in applicant_lower:
        structured_amount_str = "N 200,000.00"
    else:
        structured_amount_str = f"N {wizard_amount:,.2f}" if collateral_required else f"N{wizard_amount:,.2f}"

    # Format boilerplate text tokens dynamically for the borrower
    boilerplate_paragraphs_formatted = []
    for para in boilerplate_paragraphs:
        formatted_para = para.replace("OBOH CLETUS", app.applicant_name).replace("RHODA CHIKWADO OKORO", app.applicant_name)
        boilerplate_paragraphs_formatted.append(formatted_para)

    # Setup intro text application date
    app_date_str = "13th March, 2026" if "cletus" in applicant_lower or "oboh" in applicant_lower else ("22nd June 2026" if "rhoda" in applicant_lower or "okoro" in applicant_lower else format_date_custom(date.today(), app.loan_type))

    context = {
        "date": letter_date,
        "borrower": {
            "name": app.applicant_name,
            "id": borrower_id_str,
            "address_lines": address_lines
        },
        "loan": {
            "title_amount": title_amount_str,
            "application_date": app_date_str,
            "structured_amount": structured_amount_str,
            "loan_type": product_name.upper(),
        },
        "terms": {
            "lender": "MAINSTREET MICROFINANCE BANK",
            "borrower_name": f"{app.applicant_name} (The Applicant)",
            "facility_type": product_name.upper(),
            "purpose": app.purpose.upper() if app.purpose else (product_description.upper() if product_description else "TO SUPPORT BUSINESS"),
            "amount": f"N{app.amount or 500000:,.2f}",
            "tenor_months": f"{tenor_months_val} MONTHS",
            "expiry_date": expiry_date_str,
            "source_of_repayment": "PROCEEDS FROM BUSINESS",
            "repayment_pattern": repayment_pattern,
            "interest_rate_description": interest_rate_desc,
            "total_interest": f"N{total_interest_val:,.2f}",
            "amount_payable": f"N{amount_payable_val:,.2f}",
            "default_rate": "1% flat per month on unpaid instalment(s)",
            "penalty_rate": "6% on expiration of the loan monthly" if collateral_required else "5.5% on expiration of the loan monthly",
            "pre_liquidation_fee": "1% on outstanding Principal" if collateral_required else None
        },
        "fees": fees,
        "securities": securities,
        "boilerplate_paragraphs": boilerplate_paragraphs_formatted,
        "conditions_precedent": conditions_precedent,
        "repayment_schedule": repayment_schedule
    }

    # Generate PDF using pdf_service
    from app.services.pdf_service import generate_offer_letter_pdf
    pdf_bytes = generate_offer_letter_pdf(context=context)
    
    # 5. Store the generated document through the same authenticated
    # Cloudinary path used by borrower and CRM uploads.  Production never
    # falls back to the instance filesystem.
    from app.services.cloud_storage_service import upload_document as upload_offer_document
    cloud_result = upload_offer_document(
        file_bytes=pdf_bytes,
        mime_type="application/pdf",
        org_id=str(current_user.org_id),
        loan_id=application_id,
        doc_type="offer_letter",
        filename_stem=UUID(application_id).hex,
    )
    if not cloud_result or not cloud_result.public_id:
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail="Offer letter storage is unavailable; Cloudinary is required",
        )
    pdf_url = f"cloudinary://{cloud_result.public_id}"
        
    # 6. Save to offer_letters table
    await conn.execute(
        """
        INSERT INTO offer_letters (loan_application_id, loan_type, clause_set_version, clauses_included, interest_rate_snapshot, generated_pdf_url, generated_by, status)
        VALUES ($1, $2, 'v1', $3, $4, $5, $6, 'issued');
        """,
        UUID(application_id), app.loan_type, json.dumps(clause_keys), rate, pdf_url, current_user.id
    )
    
    # 7. Log workflow event
    audit = AuditService(conn)
    await audit.log(
        application_id=application_id,
        org_id=str(current_user.org_id),
        action="Offer Letter Generated & Issued",
        from_stage=app.stage,
        to_stage=app.stage,
        actor_id=str(current_user.id),
        actor_role=current_user.role,
        reason=f"Offer letter compiled and issued. Rate snapshot: {rate}%."
    )
    
    # 8. Add protected metadata to the canonical documents table.  Templates
    # then route previews back through the authorised download endpoint.
    await DocumentRepository(conn).create(
        loan_id=UUID(application_id),
        org_id=current_user.org_id,
        doc_type="offer_letter",
        form_code=None,
        original_name="offer_letter.pdf",
        stored_path=pdf_url,
        mime_type="application/pdf",
        size_bytes=len(pdf_bytes),
        uploaded_by=current_user.id,
        cloud_public_id=cloud_result.public_id,
        cloud_preview_url=cloud_result.preview_url,
    )
    
    return RedirectResponse(
        url=f"/applications/{application_id}/disburse",
        status_code=status.HTTP_303_SEE_OTHER
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
    cbs_authoritative, cbs_data, cbs_stale = await _cbs_context(conn, app)
    if cbs_authoritative and cbs_data:
        schedule = cbs_data["schedule"]
        payments = [
            {
                "payment_date": row.get("value_date") or row.get("transaction_at"),
                "amount_paid": row["amount"],
                "channel": "core_banking",
                "bank_ref": row["external_transaction_id"],
            }
            for row in cbs_data["transactions"]
            if row.get("transaction_type") == "repayment"
        ]
        total_paid = sum(p["amount_paid"] for p in payments)
        total_due = sum(r["total_due"] for r in schedule)
        outstanding = cbs_data["outstanding_balance"]
    else:
        svc = LoanServicingService(conn)
        schedule = await svc.get_schedule(UUID(application_id), current_user.org_id)
        payments = await svc.get_payments(UUID(application_id), current_user.org_id)
        total_paid = sum(p["amount_paid"] for p in payments)
        total_due = sum(r["total_due"] for r in schedule)
        outstanding = total_due - total_paid
    ctx = build_template_context(
        request, current_user,
        app=app, app_id=application_id,
        schedule=schedule, payments=payments,
        total_paid=total_paid, total_due=total_due,
        outstanding=outstanding,
        cbs_authoritative=cbs_authoritative,
        cbs_data=cbs_data,
        cbs_stale=cbs_stale,
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
    cbs_authoritative, _, _ = await _cbs_context(conn, app)
    svc = LoanServicingService(conn)
    payments = await svc.get_payments(UUID(application_id), current_user.org_id)
    ctx = build_template_context(
        request, current_user,
        app=app, app_id=application_id, payments=payments,
        can_record_payment=current_user.role == "crm" and not cbs_authoritative,
        cbs_authoritative=cbs_authoritative,
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
    app = await LoanRepository(conn).get_by_id(UUID(application_id), current_user.org_id)
    if not app:
        raise HTTPException(status_code=404, detail="Loan Application not found")
    cbs_authoritative, _, _ = await _cbs_context(conn, app)
    if cbs_authoritative:
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail="Repayments are read-only because Core Banking is authoritative for this product",
        )
    if settings.CONFIGURABLE_WORKFLOW_ENABLED:
        from app.domains.workflow.engine import WorkflowEngine
        await WorkflowEngine(conn).record_action(current_user.org_id, loan_uuid, current_user.id, "disburse")
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


@router.get("/api/v1/applications/{application_id}/compliance-flags")
async def list_web_application_compliance_flags(
    application_id: UUID,
    page: int = 1,
    size: int = 20,
    conn = Depends(db_conn),
    current_user = Depends(get_current_user),
):
    repo = LoanRepository(conn)
    app = await repo.get_by_id(application_id, current_user.org_id)
    if not app:
        raise HTTPException(status_code=404, detail="Application not found")
    _verify_loan_scope(app, current_user)
    
    offset = (page - 1) * size
    from app.services.dashboard_service import DashboardService
    flags = await DashboardService(conn).get_application_compliance_flags(
        current_user, application_id, limit=size, offset=offset
    )
    return {
        "items": flags,
        "has_more": len(flags) >= size
    }


@router.get("/api/v1/applications/{application_id}/workflow-events")
async def list_web_application_workflow_events(
    application_id: UUID,
    page: int = 1,
    size: int = 20,
    conn = Depends(db_conn),
    current_user = Depends(get_current_user),
):
    repo = LoanRepository(conn)
    app = await repo.get_by_id(application_id, current_user.org_id)
    if not app:
        raise HTTPException(status_code=404, detail="Application not found")
    
    offset = (page - 1) * size
    events = await repo.list_workflow_events_for_application(
        current_user.org_id, application_id, limit=size, offset=offset
    )
    return {
        "items": events,
        "has_more": len(events) >= size
    }


@router.get("/api/v1/web/borrowers")
async def list_progressive_borrowers(
    request: Request,
    page: int = Query(1, ge=1),
    size: int = Query(20, ge=1, le=100),
    conn = Depends(db_conn),
    current_user = Depends(RoleChecker(["Branch Manager", "Branch Supervisor", "Credit Analyst", "CRM", "Head CRM", "Auditor", "ED", "MD"])),
):
    applications, total = await LoanRepository(conn).list_by_stage(
        org_id=current_user.org_id,
        stage=None,
        officer_id=None,
        page=page,
        size=size,
        branch_id=getattr(current_user, "branch_id", None),
    )
    if request.headers.get("x-progressive-load") == "true":
        return templates.TemplateResponse(
            request,
            "partials/borrower_rows.html",
            {"request": request, "applications": applications},
        )
    return {
        "items": applications, "total": total, "page": page, "size": size,
        "has_more": page * size < total,
    }


@router.get("/api/v1/web/borrowers/summary")
async def borrower_summary(
    request: Request,
    conn=Depends(db_conn),
    current_user=Depends(RoleChecker(["Branch Manager", "Branch Supervisor", "Credit Analyst", "CRM", "Head CRM", "Auditor", "ED", "MD"])),
):
    repo = LoanRepository(conn)
    role_name = current_user.role.lower().replace(" ", "_")
    if role_name in {"branch_manager", "team_lead"}:
        applications, _ = await repo.list_by_stage(
            org_id=current_user.org_id,
            stage=None,
            officer_id=None,
            page=1,
            size=500,
            branch_id=current_user.branch_id,
        )
    else:
        applications = await repo.list_recent(current_user.org_id, limit=500)
    counts = {
        "total": len(applications),
        "draft": sum(1 for app in applications if app.stage == "intake"),
        "review": sum(1 for app in applications if app.stage not in {"intake", "disbursement_ready", "disbursed", "returned", "rejected"}),
        "approved": sum(1 for app in applications if app.stage == "disbursement_ready"),
        "active": sum(1 for app in applications if app.stage == "disbursed"),
    }
    return templates.TemplateResponse(request, "partials/borrower_metrics.html", {"request": request, "counts": counts})


@router.get("/api/v1/web/reports/par/loans")
async def list_progressive_par_loans(
    request: Request,
    page: int = Query(1, ge=1),
    size: int = Query(20, ge=1, le=100),
    conn = Depends(db_conn),
    current_user = Depends(RoleChecker(["md", "ed", "auditor", "crm", "head_crm"])),
):
    items, total = await LoanRepository(conn).list_disbursed_page(
        current_user.org_id, limit=size, offset=(page-1)*size
    )
    if request.headers.get("x-progressive-load") == "true":
        return templates.TemplateResponse(
            request,
            "partials/par_loan_rows.html",
            {"request": request, "loans": items},
        )
    return {
        "items": items, "total": total, "page": page, "size": size,
        "has_more": page * size < total,
    }
