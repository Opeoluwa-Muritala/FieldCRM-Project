import os
import logging
from datetime import timedelta
from fastapi import FastAPI, Depends, Form, HTTPException, Request, Response, status
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import RedirectResponse
from fastapi.staticfiles import StaticFiles
from fastapi.templating import Jinja2Templates
from app.core.config import settings
from app.core import security
from app.api.v1 import auth, borrowers, communication, applications, collections, groups, portal

app = FastAPI(
    title=settings.PROJECT_NAME,
    docs_url="/api/docs",
    redoc_url="/api/redoc"
)

app.include_router(auth.router, prefix=f"{settings.API_V1_STR}/auth", tags=["Authentication"])
app.include_router(borrowers.router, prefix=f"{settings.API_V1_STR}/borrowers", tags=["Borrowers"])
app.include_router(communication.router, prefix=f"{settings.API_V1_STR}/communication", tags=["Communication Logs"])
app.include_router(applications.router, prefix=f"{settings.API_V1_STR}/applications", tags=["Loan Applications Workflow"])
app.include_router(collections.router, prefix=f"{settings.API_V1_STR}/collections", tags=["Repayments & Collections"])
app.include_router(groups.router, prefix=f"{settings.API_V1_STR}/groups", tags=["Cooperative Groups"])
app.include_router(portal.router, prefix=f"{settings.API_V1_STR}/portal", tags=["Customer Portal"])






# CORS Policy configuration (restricted to configured origins, avoiding wildcards)
app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.CORS_ORIGINS,
    allow_credentials=True,
    allow_methods=["GET", "POST", "PUT", "PATCH", "DELETE"],
    allow_headers=["*"],
)

# Setup Jinja2 templates (Jinja2 automatically escapes HTML variables by default)
# We establish paths under the execution workspace
base_dir = os.path.dirname(os.path.abspath(__file__))
templates_dir = os.path.abspath(os.path.join(base_dir, "../../frontend/templates"))
static_dir = os.path.abspath(os.path.join(base_dir, "../../frontend/static"))

# Create directories if they do not exist
os.makedirs(templates_dir, exist_ok=True)
os.makedirs(static_dir, exist_ok=True)

templates = Jinja2Templates(directory=templates_dir)
app.mount("/static", StaticFiles(directory=static_dir), name="static")


@app.middleware("http")
async def add_security_headers(request: Request, call_next):
    """
    HTTP Middleware implementing strict security headers:
    1. Clickjacking Protection (X-Frame-Options & frame-ancestors)
    2. Content-Security-Policy (Restricting execution scopes strictly)
    3. X-Content-Type-Options: nosniff (Preventing mime-type sniffing)
    4. X-XSS-Protection (Legacy protection toggle)
    5. Cache-Control: no-store (Preventing dynamic browser caching data leaks)
    """
    response = await call_next(request)
    
    # Clickjacking protection
    response.headers["X-Frame-Options"] = "DENY"
    
    # Existing templates use inline scripts and styles for the MVP UI.
    response.headers["Content-Security-Policy"] = (
        "default-src 'self'; "
        "script-src 'self' 'unsafe-inline'; "
        "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; "
        "font-src 'self' https://fonts.gstatic.com; "
        "img-src 'self' data: https:; "
        "frame-ancestors 'none'; "
        "object-src 'none';"
    )
    
    # MIME-sniffing protection
    response.headers["X-Content-Type-Options"] = "nosniff"
    response.headers["X-XSS-Protection"] = "1; mode=block"
    
    # Sensitive view cache protection
    response.headers["Cache-Control"] = "no-store, no-cache, must-revalidate, max-age=0"
    
    return response

from sqlalchemy.orm import Session
from sqlalchemy.exc import SQLAlchemyError
from app.api import deps
from app.db.models import User, WorkflowEvent

logger = logging.getLogger("FieldCRMMain")

def raise_login_redirect():
    raise HTTPException(
        status_code=status.HTTP_303_SEE_OTHER,
        headers={"Location": "/login"}
    )

def get_current_user_web(
    request: Request,
    db: Session = Depends(deps.get_db)
) -> User:
    token = request.cookies.get("session") or request.cookies.get("__Host-session")
    if not token:
        raise_login_redirect()

    payload = security.decode_access_token(token)
    user_id = payload.get("sub")
    if not user_id:
        raise_login_redirect()

    user = db.query(User).filter(User.id == user_id).first()
    if not user or not user.is_active:
        raise_login_redirect()
    return user

class WebRoleChecker:
    def __init__(self, allowed_roles: list[str]):
        self.allowed_roles = allowed_roles

    def __call__(self, current_user: User = Depends(get_current_user_web)) -> User:
        if current_user.role not in self.allowed_roles:
            raise HTTPException(
                status_code=status.HTTP_403_FORBIDDEN,
                detail="Access denied. Insufficient role permissions for this page."
            )
        return current_user

@app.get("/")
async def root_view(request: Request):
    """Routes browser users into the MVP login experience."""
    return RedirectResponse(url="/login", status_code=status.HTTP_303_SEE_OTHER)

@app.get("/login")
async def render_login(request: Request):
    """Renders staff login for the browser MVP."""
    return templates.TemplateResponse(request, "login.html", {"error": None})

@app.post("/login")
async def login_web(
    request: Request,
    response: Response,
    username: str = Form(...),
    password: str = Form(...),
    db: Session = Depends(deps.get_db)
):
    """Authenticates a staff user and sets the same HttpOnly session cookie used by the API."""
    try:
        user = db.query(User).filter(User.phone == username).first()
    except SQLAlchemyError as exc:
        logger.error("Login database lookup failed: %s", exc)
        return templates.TemplateResponse(
            request,
            "login.html",
            {"error": "Authentication database is unavailable. Please try again later."},
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE
        )

    if not user or not user.is_active or not security.verify_password(password, user.hashed_password):
        return templates.TemplateResponse(
            request,
            "login.html",
            {"error": "Incorrect phone number or password."},
            status_code=status.HTTP_401_UNAUTHORIZED
        )

    access_token = security.create_access_token(
        user.id,
        expires_delta=timedelta(minutes=settings.ACCESS_TOKEN_EXPIRE_MINUTES)
    )
    redirect_url = "/forms" if user.role == "Loan Officer" else "/dashboard"
    redirect = RedirectResponse(url=redirect_url, status_code=status.HTTP_303_SEE_OTHER)
    redirect.set_cookie(
        key="session",
        value=access_token,
        httponly=True,
        secure=False,
        samesite="strict",
        max_age=settings.ACCESS_TOKEN_EXPIRE_MINUTES * 60,
        path="/"
    )
    return redirect

@app.get("/dashboard")
async def render_dashboard(
    request: Request,
    current_user = Depends(get_current_user_web)
):
    """Renders the main CRM dashboard after staff authentication."""
    if current_user.role == "Loan Officer":
        return RedirectResponse(url="/forms", status_code=status.HTTP_303_SEE_OTHER)

    templates_by_role = {
        "Branch Manager": "dashboard_branch_manager.html",
        "Credit Officer": "dashboard_credit_officer.html",
        "Auditor": "dashboard_auditor.html",
        "System Admin": "dashboard_system_admin.html",
    }
    return templates.TemplateResponse(
        request,
        templates_by_role.get(current_user.role, "dashboard.html"),
        {"current_user": current_user}
    )

@app.get("/forms")
async def render_loan_forms(
    request: Request,
    db: Session = Depends(deps.get_db),
    current_user = Depends(WebRoleChecker(["Loan Officer"]))
):
    """Renders the dedicated loan form capture page for loan officers."""
    applications = db.query(LoanApplication).filter(
        LoanApplication.org_id == current_user.org_id,
        LoanApplication.current_owner_id == current_user.id,
        LoanApplication.current_stage == 1
    ).all()
    return templates.TemplateResponse(
        request,
        "loan_forms.html",
        {
            "current_user": current_user,
            "applications": applications
        }
    )

@app.get("/logout")
async def logout_web():
    redirect = RedirectResponse(url="/login", status_code=status.HTTP_303_SEE_OTHER)
    redirect.delete_cookie(key="session", path="/")
    redirect.delete_cookie(key="__Host-session", path="/")
    return redirect

@app.get("/api/v1/health")
async def health_check():
    """Health monitor standard endpoint."""
    return {"status": "healthy", "database": "configured"}

from app.db.models import LoanApplication

def get_pipeline_applications_for_user(db: Session, current_user: User):
    query = db.query(LoanApplication).filter(
        LoanApplication.org_id == current_user.org_id
    )

    if current_user.role == "System Admin":
        return query.filter(
            LoanApplication.current_owner_id == current_user.id
        ).all()

    if current_user.role == "Branch Manager":
        return query.filter(
            LoanApplication.current_owner_id == current_user.id,
            LoanApplication.current_stage.in_([2, 5, 6])
        ).all()

    if current_user.role == "Credit Officer":
        return query.filter(
            LoanApplication.current_owner_id == current_user.id,
            LoanApplication.current_stage == 3
        ).all()

    if current_user.role == "Auditor":
        return query.filter(
            LoanApplication.current_owner_id == current_user.id,
            LoanApplication.current_stage == 4
        ).all()

    if current_user.role == "Loan Officer":
        return query.filter(
            LoanApplication.current_owner_id == current_user.id,
            LoanApplication.current_stage == 1
        ).all()

    return []

@app.get("/pipeline")
async def render_loan_pipeline(
    request: Request,
    db: Session = Depends(deps.get_db),
    current_user = Depends(get_current_user_web)
):
    """
    Renders secure, server-side rendered Jinja2 template for the loan pipeline board:
    - Filters applications strictly to the current user's actionable queue.
    - Loan officers only see their own active draft/returned applications.
    - Approval roles only see applications assigned to them for approval, return, or release.
    """
    applications = get_pipeline_applications_for_user(db, current_user)
    
    stage_counts = {
        "stage_1": sum(1 for app in applications if app.current_stage == 1),
        "stage_2": sum(1 for app in applications if app.current_stage == 2),
        "stage_3": sum(1 for app in applications if app.current_stage == 3),
        "stage_4": sum(1 for app in applications if app.current_stage == 4),
        "stage_5": sum(1 for app in applications if app.current_stage == 5),
        "stage_6": sum(1 for app in applications if app.current_stage == 6)
    }
    
    return templates.TemplateResponse(
        request,
        "pipeline.html", 
        {
            "applications": applications, 
            "stage_counts": stage_counts,
            "current_user": current_user
        }
    )

@app.get("/borrowers")
async def render_current_loans(
    request: Request,
    db: Session = Depends(deps.get_db),
    current_user = Depends(WebRoleChecker(["Branch Manager", "Credit Officer", "Auditor", "System Admin"]))
):
    """
    Renders a top-chain current loan state view.
    Borrower directory/profile access is not exposed to loan officers from this page.
    """
    applications = db.query(LoanApplication).filter(
        LoanApplication.org_id == current_user.org_id
    ).order_by(LoanApplication.created_at.desc()).all()

    state_counts = {
        "total": len(applications),
        "draft": sum(1 for app in applications if app.current_stage == 1),
        "review": sum(1 for app in applications if app.current_stage in [2, 3, 4, 5]),
        "approved": sum(1 for app in applications if app.current_stage == 6),
        "active": sum(1 for app in applications if app.status == "Active"),
    }

    return templates.TemplateResponse(
        request,
        "borrowers.html",
        {
            "applications": applications,
            "state_counts": state_counts,
            "current_user": current_user
        }
    )

@app.get("/audit")
async def render_compliance_audit(
    request: Request,
    db: Session = Depends(deps.get_db),
    current_admin = Depends(WebRoleChecker(["System Admin"]))
):
    """
    Renders secure, server-side rendered Jinja2 template for regulatory compliance:
    - Restricts access to the main system admin role.
    - Fetches the complete immutable event log chronologically.
    """
    events = db.query(WorkflowEvent).order_by(WorkflowEvent.timestamp.desc()).all()
    return templates.TemplateResponse(
        request,
        "audit.html", 
        {"events": events, "current_user": current_admin}
    )

from app.db.models import Borrower, RepaymentSchedule
from app.core.security import decode_access_token

@app.get("/portal")
async def render_customer_portal(
    request: Request,
    db: Session = Depends(deps.get_db)
):
    """
    Renders secure, server-side rendered Jinja2 template for customer loan tracking:
    - Automatically maps the active borrower session.
    - Limits visibility exclusively to their own loan progress.
    """
    token = request.cookies.get("borrower_session")
    if not token:
        return templates.TemplateResponse(
            request,
            "portal.html", 
            {"borrower": None, "loan": None}
        )
        
    payload = decode_access_token(token)
    borrower_id = payload.get("sub")
    if not borrower_id:
        return templates.TemplateResponse(
            request,
            "portal.html", 
            {"borrower": None, "loan": None}
        )
        
    borrower = db.query(Borrower).filter(Borrower.id == borrower_id).first()
    if not borrower:
        return templates.TemplateResponse(
            request,
            "portal.html", 
            {"borrower": None, "loan": None}
        )
        
    # Get active loan
    loan = db.query(LoanApplication).filter(
        LoanApplication.borrower_id == borrower.id
    ).order_by(LoanApplication.created_at.desc()).first()
    
    # If active and has repayment schedule, load it
    if loan and loan.status == "Active":
        loan.repayment_schedule = db.query(RepaymentSchedule).filter(
            RepaymentSchedule.application_id == loan.id
        ).order_by(RepaymentSchedule.due_date.asc()).all()
        
    return templates.TemplateResponse(
        request,
        "portal.html", 
        {"borrower": borrower, "loan": loan}
    )
