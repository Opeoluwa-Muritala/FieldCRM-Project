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
from app.api.v1.mobile import router as mobile_api_router

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
    response.headers["X-Content-Type-Options"] = "nosniff"
    response.headers["X-XSS-Protection"] = "1; mode=block"
    response.headers["Referrer-Policy"] = "strict-origin-when-cross-origin"
    response.headers["Cache-Control"] = "no-store, no-cache, must-revalidate, max-age=0"
    
    is_secure = settings.COOKIE_SECURE or (request.url.scheme == "https" or request.headers.get("x-forwarded-proto") == "https")
    if is_secure:
        response.headers["Strict-Transport-Security"] = "max-age=31536000; includeSubDomains"
        
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
app.include_router(mobile_api_router, prefix=f"{settings.API_V1_STR}/mobile", tags=["Mobile API"])

# Mount Loan pages at root
app.include_router(loans_router)

logger = logging.getLogger("FieldCRMMain")

from fastapi.responses import JSONResponse
import urllib.parse

@app.exception_handler(HTTPException)
async def http_exception_handler(request: Request, exc: HTTPException):
    # Only redirect page requests (non-API) to login on 401/403
    is_api = request.url.path.startswith("/api/")
    if exc.status_code in (status.HTTP_401_UNAUTHORIZED, status.HTTP_403_FORBIDDEN) and not is_api:
        next_url = str(request.url.path)
        if request.url.query:
            next_url += f"?{request.url.query}"
        encoded_next = urllib.parse.quote(next_url)
        return RedirectResponse(
            url=f"/login?next={encoded_next}",
            status_code=status.HTTP_303_SEE_OTHER
        )

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
            # User is already logged in, redirect to next URL or dashboard
            next_url = request.query_params.get("next", "")
            redirect_url = "/dashboard"
            if next_url and next_url.strip() and next_url.startswith("/") and not next_url.startswith("//"):
                redirect_url = next_url.strip()
            return RedirectResponse(url=redirect_url, status_code=status.HTTP_303_SEE_OTHER)
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
        samesite="strict",
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

@app.get("/api/v1/health")
async def health_check():
    return {"status": "healthy", "database": "configured"}
