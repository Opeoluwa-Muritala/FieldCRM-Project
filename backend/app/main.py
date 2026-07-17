import os
import logging
from datetime import timedelta
from urllib.parse import urlparse
from fastapi import FastAPI, Depends, Form, HTTPException, Request, Response, status
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import RedirectResponse
from fastapi.staticfiles import StaticFiles
from fastapi.templating import Jinja2Templates
from starlette.exceptions import HTTPException as StarletteHTTPException

from contextlib import asynccontextmanager
from app.config import settings
from app.core import security
from app.core.database import db_conn, init_pool, close_pool, get_connection
from app.core.exceptions import DomainException, domain_exception_handler
from app.core.middleware import RequestIDMiddleware, SecurityHeadersMiddleware
from app.core.dependencies import get_current_user, RoleChecker

# Domain Routers
from app.domains.auth.router import router as auth_router
from app.domains.users.router import router as users_router
from app.domains.loans.router import router as loans_router
from app.api.v1.mobile import router as mobile_api_router

@asynccontextmanager
async def lifespan(app: FastAPI):
    await init_pool()
    try:
        async with get_connection() as conn:
            if "postgresql" in settings.DATABASE_URL:
                row = await conn.fetchrow(
                    "SELECT 1 FROM information_schema.columns WHERE table_name = 'loan_applications' AND column_name = 'share_token'"
                )
                column_exists = bool(row)
            else:
                rows = await conn.fetch("PRAGMA table_info(loan_applications)")
                column_exists = any(r["name"] == "share_token" for r in rows)
            
            if not column_exists:
                logging.info("Adding share_token column to loan_applications table...")
                await conn.execute("ALTER TABLE loan_applications ADD COLUMN share_token TEXT")
    except Exception as e:
        logging.error(f"Failed to dynamically verify share_token column: {e}")
    yield
    await close_pool()

app = FastAPI(
    title=settings.PROJECT_NAME,
    docs_url="/api/docs",
    redoc_url="/api/redoc",
    lifespan=lifespan
)

# CORS Policy
app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.CORS_ORIGINS,
    allow_credentials=True,
    allow_methods=["GET", "POST", "PUT", "PATCH", "DELETE"],
    allow_headers=["*"],
)

# Custom Middlewares
app.add_middleware(SecurityHeadersMiddleware, cookie_secure=settings.COOKIE_SECURE)
app.add_middleware(RequestIDMiddleware)

# Exception handlers
app.add_exception_handler(DomainException, domain_exception_handler)

# Setup templates and static mount
base_dir = os.path.dirname(os.path.abspath(__file__))
templates_dir = os.path.abspath(os.path.join(base_dir, "../../frontend/templates"))
static_dir = os.path.abspath(os.path.join(base_dir, "../../frontend/static"))
templates = Jinja2Templates(directory=templates_dir)

app.mount("/static", StaticFiles(directory=static_dir), name="static")


def render_not_found_page(request: Request, message: str = "The page you requested could not be found."):
    return templates.TemplateResponse(
        request,
        "shared/not_found.html",
        {"message": message},
        status_code=status.HTTP_404_NOT_FOUND,
    )


def return_to_internal_referrer(request: Request):
    """Keep users on their previous app page when an internal link is unimplemented."""
    if request.method not in {"GET", "HEAD"}:
        return None

    referrer = request.headers.get("referer")
    if not referrer:
        return None

    parsed = urlparse(referrer)
    if parsed.scheme not in {"http", "https"} or parsed.netloc != request.url.netloc:
        return None
    if parsed.path == request.url.path and parsed.query == request.url.query:
        return None

    return RedirectResponse(url=referrer, status_code=status.HTTP_303_SEE_OTHER)


async def browser_domain_exception_handler(request: Request, exc: DomainException):
    if exc.status_code == status.HTTP_404_NOT_FOUND and not request.url.path.startswith("/api/"):
        redirect = return_to_internal_referrer(request)
        if redirect:
            return redirect
        return render_not_found_page(request, exc.message)
    return await domain_exception_handler(request, exc)


app.add_exception_handler(DomainException, browser_domain_exception_handler)

# Mount Routers
app.include_router(auth_router, prefix=f"{settings.API_V1_STR}/auth", tags=["Authentication"])
app.include_router(users_router, prefix=f"{settings.API_V1_STR}/users", tags=["Users"])
app.include_router(mobile_api_router, prefix=f"{settings.API_V1_STR}/mobile", tags=["Mobile API"])

# Mount Loan pages at root
app.include_router(loans_router)

logger = logging.getLogger("FieldCRMMain")

from fastapi.responses import JSONResponse
import urllib.parse

@app.exception_handler(StarletteHTTPException)
@app.exception_handler(HTTPException)
async def http_exception_handler(request: Request, exc: HTTPException):
    # Only redirect page requests (non-API) to login on 401/403
    is_api = request.url.path.startswith("/api/")
    if exc.status_code == status.HTTP_403_FORBIDDEN and not is_api:
        # A signed-in user does not become authorized by visiting /login again.
        # Send forbidden page requests to their dashboard instead of creating
        # a protected-page -> login -> protected-page redirect loop.
        return RedirectResponse(url="/dashboard", status_code=status.HTTP_303_SEE_OTHER)
    if exc.status_code == status.HTTP_401_UNAUTHORIZED and not is_api:
        next_url = str(request.url.path)
        if request.url.query:
            next_url += f"?{request.url.query}"
        encoded_next = urllib.parse.quote(next_url)
        return RedirectResponse(
            url=f"/login?next={encoded_next}",
            status_code=status.HTTP_303_SEE_OTHER
        )
    if exc.status_code == status.HTTP_404_NOT_FOUND and not is_api:
        redirect = return_to_internal_referrer(request)
        if redirect:
            return redirect
        return render_not_found_page(request, str(exc.detail))

    request_id = getattr(request.state, "request_id", "unknown")
    return JSONResponse(
        status_code=exc.status_code,
        content={"detail": exc.detail, "request_id": request_id}
    )

def raise_login_redirect():
    raise HTTPException(
        status_code=status.HTTP_303_SEE_OTHER,
        headers={"Location": "/login"}
    )

# Root redirects
@app.get("/")
async def root_view(request: Request):
    return RedirectResponse(url="/login", status_code=status.HTTP_303_SEE_OTHER)

@app.get("/login")
async def render_login(request: Request, conn=Depends(db_conn)):
    token = request.cookies.get("session") or request.cookies.get("__Host-session")
    if token:
        try:
            from app.core.dependencies import get_current_user_from_token
            await get_current_user_from_token(token, conn)
            # Avoid re-sending an already authenticated user to a route that
            # may be forbidden for their role. The POST login flow still
            # honours a validated `next` value after fresh authentication.
            return RedirectResponse(url="/dashboard", status_code=status.HTTP_303_SEE_OTHER)
        except Exception:
            # Token invalid, allow login page to render
            pass

    next_url = request.query_params.get("next", "")
    return templates.TemplateResponse(request, "shared/login.html", {"error": None, "next_url": next_url})

@app.post("/login")
async def login_web(
    request: Request,
    response: Response,
    username: str = Form(...),
    password: str = Form(...),
    next: str = Form(None),
    conn=Depends(db_conn),
):
    """Authenticate user by email and password, set session cookie."""
    from app.domains.auth.repository import AuthRepository
    from app.domains.auth.service import AuthService

    repo = AuthRepository(conn)
    service = AuthService(repo)
    try:
        token = await service.authenticate_user(username, password)
    except Exception as exc:
        logger.error("Login authentication failed for email: [REDACTED]")
        next_url = next or ""
        return templates.TemplateResponse(
            request,
            "shared/login.html",
            {"error": "Incorrect email or password.", "next_url": next_url},
            status_code=status.HTTP_401_UNAUTHORIZED,
        )

    is_secure = settings.COOKIE_SECURE or (request.url.scheme == "https" or request.headers.get("x-forwarded-proto") == "https")
    
    # Safe redirect validation: prevent open redirect to external domains
    redirect_url = "/dashboard"
    if next and next.strip() and next.startswith("/") and not next.startswith("//"):
        redirect_url = next.strip()

    redirect = RedirectResponse(url=redirect_url, status_code=status.HTTP_303_SEE_OTHER)
    redirect.set_cookie(
        key="session",
        value=token,
        httponly=True,
        secure=is_secure,
        # Account Officers commonly open upload and visitation tasks from
        # notification/deep links. Lax keeps the authenticated session on
        # safe top-level navigation while retaining CSRF protection for
        # cross-site form submissions.
        samesite="lax",
        max_age=settings.ACCESS_TOKEN_EXPIRE_MINUTES * 60,
        path="/",
    )
    return redirect

@app.get("/logout")
async def logout_web(next: str = None):
    redirect_url = "/login"
    if next and next.strip() and next.startswith("/") and not next.startswith("//"):
        redirect_url += f"?next={urllib.parse.quote(next.strip())}"

    redirect = RedirectResponse(url=redirect_url, status_code=status.HTTP_303_SEE_OTHER)
    redirect.delete_cookie(key="session", path="/")
    redirect.delete_cookie(key="__Host-session", path="/")
    return redirect

@app.get("/forgot-password")
async def render_forgot_password(request: Request):
    return templates.TemplateResponse(request, "shared/forgot_password.html", {"submitted": False, "error": None})

@app.post("/forgot-password")
async def process_forgot_password(request: Request, email: str = Form(...), conn=Depends(db_conn)):
    from app.domains.auth.repository import AuthRepository
    from app.domains.auth.service import AuthService
    await AuthService(AuthRepository(conn)).request_password_reset(email)
    return templates.TemplateResponse(request, "shared/forgot_password.html", {"submitted": True, "error": None})

@app.get("/reset-password")
async def render_reset_password(request: Request, token: str = None):
    return templates.TemplateResponse(request, "shared/reset_password.html", {"token": token, "error": None, "success": False})

@app.get("/accept-invitation")
async def render_accept_invitation(request: Request, token: str = None):
    return templates.TemplateResponse(
        request, "shared/reset_password.html",
        {"token": token, "error": None, "success": False, "invitation": True},
    )

@app.post("/reset-password")
async def process_reset_password(
    request: Request,
    token: str = Form(...),
    new_password: str = Form(...),
    confirm_password: str = Form(...),
    invitation: bool = Form(False),
    conn=Depends(db_conn),
):
    from app.domains.auth.repository import AuthRepository
    from app.domains.auth.service import AuthService
    if new_password != confirm_password:
        return templates.TemplateResponse(request, "shared/reset_password.html", {"token": token, "error": "Passwords do not match.", "success": False, "invitation": invitation})
    ok = await AuthService(AuthRepository(conn)).reset_password(token, new_password)
    if not ok:
        return templates.TemplateResponse(request, "shared/reset_password.html", {"token": token, "error": "Invalid or expired reset link.", "success": False, "invitation": invitation})
    return RedirectResponse(url="/login?reset=1", status_code=status.HTTP_303_SEE_OTHER)

@app.get("/api/v1/health")
async def health_check():
    return {"status": "healthy", "database": "configured"}
