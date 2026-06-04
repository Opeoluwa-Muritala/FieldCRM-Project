import os
import logging
from datetime import timedelta
from fastapi import FastAPI, Depends, Form, HTTPException, Request, Response, status
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import RedirectResponse
from fastapi.staticfiles import StaticFiles
from fastapi.templating import Jinja2Templates

from contextlib import asynccontextmanager
from app.config import settings
from app.core import security
from app.core.database import db_conn, init_pool, close_pool
from app.core.exceptions import DomainException, domain_exception_handler
from app.core.middleware import RequestIDMiddleware
from app.core.dependencies import get_current_user, RoleChecker

# Domain Routers
from app.domains.auth.router import router as auth_router
from app.domains.users.router import router as users_router
from app.domains.loans.router import router as loans_router

@asynccontextmanager
async def lifespan(app: FastAPI):
    await init_pool()
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
app.add_middleware(RequestIDMiddleware)

@app.middleware("http")
async def add_security_headers(request: Request, call_next):
    response = await call_next(request)
    response.headers["X-Frame-Options"] = "DENY"
    response.headers["Content-Security-Policy"] = (
        "default-src 'self'; "
        "script-src 'self' 'unsafe-inline'; "
        "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; "
        "font-src 'self' https://fonts.gstatic.com; "
        "img-src 'self' data: https:; "
        "frame-ancestors 'none'; "
        "object-src 'none';"
    )
    # TODO(security): Replace 'unsafe-inline' in script-src with nonce-based CSP
    response.headers["X-Content-Type-Options"] = "nosniff"
    response.headers["X-XSS-Protection"] = "1; mode=block"
    response.headers["Cache-Control"] = "no-store, no-cache, must-revalidate, max-age=0"
    return response

# Exception handlers
app.add_exception_handler(DomainException, domain_exception_handler)

# Setup templates and static mount
base_dir = os.path.dirname(os.path.abspath(__file__))
templates_dir = os.path.abspath(os.path.join(base_dir, "../../frontend/templates"))
static_dir = os.path.abspath(os.path.join(base_dir, "../../frontend/static"))
templates = Jinja2Templates(directory=templates_dir)

app.mount("/static", StaticFiles(directory=static_dir), name="static")

# Mount Routers
app.include_router(auth_router, prefix=f"{settings.API_V1_STR}/auth", tags=["Authentication"])
app.include_router(users_router, prefix=f"{settings.API_V1_STR}/users", tags=["Users"])

# Mount Loan pages at root
app.include_router(loans_router)

logger = logging.getLogger("FieldCRMMain")

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
async def render_login(request: Request):
    return templates.TemplateResponse(request, "shared/login.html", {"error": None})

@app.post("/login")
async def login_web(
    request: Request,
    response: Response,
    username: str = Form(...),
    password: str = Form(...),
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
        return templates.TemplateResponse(
            request,
            "shared/login.html",
            {"error": "Incorrect email or password."},
            status_code=status.HTTP_401_UNAUTHORIZED,
        )

    redirect = RedirectResponse(url="/dashboard", status_code=status.HTTP_303_SEE_OTHER)
    redirect.set_cookie(
        key="session",
        value=token,
        httponly=True,
        secure=False,  # TODO(security): Set True in production with HTTPS
        samesite="strict",
        max_age=settings.ACCESS_TOKEN_EXPIRE_MINUTES * 60,
        path="/",
    )
    return redirect

@app.get("/logout")
async def logout_web():
    redirect = RedirectResponse(url="/login", status_code=status.HTTP_303_SEE_OTHER)
    redirect.delete_cookie(key="session", path="/")
    redirect.delete_cookie(key="__Host-session", path="/")
    return redirect

@app.get("/api/v1/health")
async def health_check():
    return {"status": "healthy", "database": "configured"}
