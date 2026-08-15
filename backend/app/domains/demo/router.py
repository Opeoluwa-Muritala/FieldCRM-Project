from __future__ import annotations

import secrets
from datetime import UTC, datetime, timedelta
from pathlib import Path
from urllib.parse import urlsplit
from uuid import UUID

from fastapi import APIRouter, Depends, Form, HTTPException, Request, status
from fastapi.responses import RedirectResponse
from jose import jwt

from app.config import settings
from app.core.database import db_conn
from app.core.middleware import CrossSiteRequestMiddleware
from app.core.rate_limit import enforce_login_limits
from app.core.security import create_access_token, decode_access_token
from app.core.templates import create_templates

router = APIRouter(prefix="/demo", tags=["Demo Presenter"])
templates = create_templates(str(Path(__file__).resolve().parents[4] / "frontend" / "templates"))

ROLE_ORDER = {
    role: index for index, role in enumerate((
        "account_officer", "branch_manager", "branch_supervisor", "credit_analyst",
        "crm", "head_crm", "ed", "md", "legal", "auditor", "system_admin",
    ))
}
ROLE_LABELS = {
    "account_officer": "Relationship Officer",
    "branch_manager": "Team Lead",
    "branch_supervisor": "Supervisor",
    "credit_analyst": "Credit Analyst",
    "crm": "CRM Officer",
    "head_crm": "Head CRM",
    "ed": "Executive Director",
    "md": "Managing Director",
    "legal": "Legal",
    "auditor": "Auditor",
    "system_admin": "System Admin",
}
ROLE_SCREENS = {
    "account_officer": "Dashboard, completed intake, documents and visitation",
    "branch_manager": "Team Lead queue, evidence checklist and concurrence",
    "branch_supervisor": "Supervisory queue and branch recommendation",
    "credit_analyst": "Affordability, OCR, bureau, AML and credit decision",
    "crm": "Compliance review, offer letter and disbursement",
    "head_crm": "CRM oversight and executive recommendation",
    "ed": "Executive decision pack and MD escalation",
    "md": "MD advice, final decision and board referral",
    "legal": "Collateral valuation and pledged-item workspace",
    "auditor": "Immutable activity, change authors and compliance flags",
    "system_admin": "Users, roles, branches and system activity",
}
ROLE_LANDING = {
    "account_officer": "/dashboard",
    "branch_manager": "/awaiting-me",
    "branch_supervisor": "/supervisory-review-queue",
    "credit_analyst": "/my-reviews",
    "crm": "/crm-review-queue",
    "head_crm": "/crm-review-queue",
    "ed": "/ed-queue",
    "md": "/md-queue",
    "legal": "/legal-queue",
    "auditor": "/audit-trail",
    "system_admin": "/users",
}
STAGE_ROLE = {
    "intake": "account_officer",
    "branch_manager_review": "branch_manager",
    "branch_supervisor_review": "branch_supervisor",
    "credit_analyst_review": "credit_analyst",
    "crm_review": "crm",
    "head_crm_review": "head_crm",
    "ed_approval": "ed",
    "md_approval": "md",
    "legal_review": "legal",
    "disbursement_ready": "crm",
    "disbursed": "auditor",
}


def _require_enabled() -> UUID:
    if not settings.demo_mode:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Not found")
    return UUID(settings.DEMO_ORG_ID)


def _require_same_origin(request: Request) -> None:
    origin = request.headers.get("origin", "").rstrip("/").lower()
    if not origin and request.headers.get("referer"):
        parsed = urlsplit(request.headers["referer"])
        origin = f"{parsed.scheme}://{parsed.netloc}".lower()
    expected = CrossSiteRequestMiddleware._request_origin(request.scope)
    if not origin or origin != expected:
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="Demo origin validation failed")


def _presenter_payload(request: Request) -> dict | None:
    raw = request.cookies.get("demo_presenter")
    if not raw:
        return None
    try:
        payload = jwt.decode(raw, settings.JWT_SECRET_KEY, algorithms=[settings.JWT_ALGORITHM])
    except Exception:
        return None
    if payload.get("type") != "demo_presenter" or payload.get("org_id") != settings.DEMO_ORG_ID:
        return None
    return payload


def _secure_cookie(request: Request) -> bool:
    forwarded = request.headers.get("x-forwarded-proto", "").split(",", 1)[0].strip().lower()
    return settings.COOKIE_SECURE or request.url.scheme == "https" or forwarded == "https"


async def _install_demo_read_identity(conn, org_id: UUID) -> None:
    """Install an unlocked presenter's read-only, tenant-scoped RLS identity."""
    for name, value in (
        ("app.org_id", str(org_id)),
        ("app.user_id", "00000000-0000-4000-8000-000000000000"),
        ("app.user_role", "auditor"),
        ("app.branch_id", ""),
    ):
        await conn.execute("SELECT set_config($1, $2, TRUE)", name, value)


@router.get("")
async def presenter(request: Request, conn=Depends(db_conn)):
    org_id = _require_enabled()
    payload = _presenter_payload(request)
    users = []
    application = None
    selected_user_id = None
    session_payload = decode_access_token(request.cookies.get("session", ""))
    if session_payload.get("org_id") == settings.DEMO_ORG_ID:
        selected_user_id = session_payload.get("sub")
    if payload:
        await _install_demo_read_identity(conn, org_id)
        rows = await conn.fetch(
            """SELECT u.id, u.full_name, u.role, b.name AS branch_name
               FROM users u LEFT JOIN branches b ON b.id=u.branch_id AND b.org_id=u.org_id
               WHERE u.org_id=$1 AND u.active=TRUE AND u.deleted_at IS NULL""",
            org_id,
        )
        users = sorted(
            (dict(row) for row in rows),
            key=lambda row: (ROLE_ORDER.get(row["role"], 99), row.get("branch_name") or "", row["full_name"]),
        )
        application = await conn.fetchrow(
            """SELECT id, ref_no, applicant_name, stage FROM loan_applications
               WHERE org_id=$1 AND deleted_at IS NULL ORDER BY created_at LIMIT 1""",
            org_id,
        )
        application = dict(application) if application else None
    return templates.TemplateResponse(request, "shared/demo_presenter.html", {
        "unlocked": bool(payload),
        "csrf_token": payload.get("csrf") if payload else "",
        "users": users,
        "selected_user_id": selected_user_id,
        "application": application,
        "next_role": STAGE_ROLE.get(application.get("stage")) if application else None,
        "role_labels": ROLE_LABELS,
        "role_screens": ROLE_SCREENS,
    })


@router.post("/unlock")
async def unlock_presenter(request: Request, access_secret: str = Form(...)):
    _require_enabled()
    _require_same_origin(request)
    await enforce_login_limits(request, "demo-presenter")
    if not secrets.compare_digest(access_secret, settings.DEMO_ACCESS_SECRET):
        return templates.TemplateResponse(
            request, "shared/demo_presenter.html",
            {"unlocked": False, "users": [], "application": None, "error": "Incorrect demo access secret."},
            status_code=status.HTTP_401_UNAUTHORIZED,
        )
    csrf = secrets.token_urlsafe(24)
    expires = datetime.now(UTC) + timedelta(hours=4)
    token = jwt.encode(
        {"type": "demo_presenter", "org_id": settings.DEMO_ORG_ID, "csrf": csrf, "exp": expires},
        settings.JWT_SECRET_KEY,
        algorithm=settings.JWT_ALGORITHM,
    )
    response = RedirectResponse("/demo", status_code=status.HTTP_303_SEE_OTHER)
    response.set_cookie(
        "demo_presenter", token, httponly=True, secure=_secure_cookie(request),
        samesite="strict", max_age=4 * 60 * 60, path="/demo",
    )
    return response


@router.post("/switch")
async def switch_demo_role(
    request: Request,
    user_id: UUID = Form(...),
    csrf_token: str = Form(...),
    conn=Depends(db_conn),
):
    org_id = _require_enabled()
    _require_same_origin(request)
    payload = _presenter_payload(request)
    if not payload or not secrets.compare_digest(csrf_token, str(payload.get("csrf") or "")):
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="Demo presenter session is invalid")
    user = await conn.fetchrow(
        """SELECT id, org_id, role FROM users
           WHERE id=$1 AND org_id=$2 AND active=TRUE AND deleted_at IS NULL""",
        user_id, org_id,
    )
    if not user or user["role"] not in ROLE_ORDER:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Demo role account not found")
    await _install_demo_read_identity(conn, org_id)
    application = await conn.fetchrow(
        """SELECT id, stage FROM loan_applications
           WHERE org_id=$1 AND deleted_at IS NULL ORDER BY created_at LIMIT 1""",
        org_id,
    )
    destination = ROLE_LANDING[user["role"]]
    if application and STAGE_ROLE.get(application["stage"]) == user["role"]:
        app_id = application["id"]
        destination = {
            "intake": f"/applications/{app_id}/step/1",
            "branch_manager_review": f"/applications/{app_id}/approve",
            "branch_supervisor_review": f"/applications/{app_id}/approve",
            "credit_analyst_review": f"/applications/{app_id}/credit-review",
            "crm_review": f"/applications/{app_id}/crm-review",
            "head_crm_review": f"/applications/{app_id}/crm-review",
            "ed_approval": f"/applications/{app_id}/ed-approve",
            "md_approval": f"/applications/{app_id}/md-approve",
            "disbursement_ready": f"/applications/{app_id}/disburse",
            "disbursed": "/audit-trail",
        }.get(application["stage"], destination)
    token = create_access_token(
        user["id"], role=user["role"], org_id=user["org_id"],
        expires_delta=timedelta(minutes=settings.DEMO_SESSION_MINUTES), session_type="demo",
    )
    response = RedirectResponse(destination, status_code=status.HTTP_303_SEE_OTHER)
    response.set_cookie(
        "session", token, httponly=True, secure=_secure_cookie(request), samesite="lax",
        max_age=settings.DEMO_SESSION_MINUTES * 60, path="/",
    )
    response.delete_cookie("refresh_token", path="/")
    return response


@router.get("/lock")
async def lock_presenter():
    response = RedirectResponse("/login", status_code=status.HTTP_303_SEE_OTHER)
    response.delete_cookie("demo_presenter", path="/demo")
    response.delete_cookie("session", path="/")
    response.delete_cookie("refresh_token", path="/")
    return response
