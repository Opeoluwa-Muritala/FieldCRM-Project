import os
import re
import sys
import unicodedata

# Add backend directory to sys.path to allow correct imports when running from the repository root (e.g. on Vercel)
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

import logging
from datetime import timedelta
from pathlib import Path
from typing import Any
from urllib.parse import quote, unquote, urlsplit
from fastapi import FastAPI, Depends, Form, HTTPException, Query, Request, Response, status
from fastapi.middleware.cors import CORSMiddleware
from fastapi.exception_handlers import request_validation_exception_handler
from fastapi.exceptions import RequestValidationError
from fastapi.responses import FileResponse, JSONResponse, PlainTextResponse, RedirectResponse, StreamingResponse
from fastapi.staticfiles import StaticFiles
from starlette.exceptions import HTTPException as StarletteHTTPException
from starlette.middleware.trustedhost import TrustedHostMiddleware

from contextlib import asynccontextmanager
from app.config import settings
from app.core import security
from app.core.database import db_conn, init_engine, dispose_engine, get_connection, verify_runtime_database_role
from app.core.exceptions import DomainException, domain_exception_handler
from app.core.middleware import (
    PendingResponseCookiesMiddleware,
    PerformanceTimingMiddleware,
    RequestIDMiddleware,
    SecurityHeadersMiddleware,
    CrossSiteRequestMiddleware,
)
from app.core.template_utils import csp_nonce_context
from app.core.templates import create_templates
from app.core.api_docs import (
    install_organised_openapi,
    redoc_response,
    require_local_docs_access,
    swagger_ui_response,
)
from app.core.dependencies import authenticated_db_conn, get_current_user, RoleChecker
from app.core.loan_authorization import require_view
from app.core.audit import AuditService
from app.domains.documents.repository import DocumentRepository
from app.domains.loans.repository import LoanRepository
from app.services.cloud_storage_service import signed_download_url, signed_preview_url
from app.core.rate_limit import close_rate_limiter, init_rate_limiter, enforce_login_limits, enforce_reset_limits
from app.core.cache import ResponseCacheInvalidationMiddleware, close_cache, init_cache
from uuid import UUID
import httpx

# HTTPX INFO logs include complete request URLs.  Preview URLs contain short-
# lived credentials and must never be written to application logs.
logging.getLogger("httpx").setLevel(logging.WARNING)

# Domain Routers
from app.domains.auth.router import router as auth_router
from app.domains.users.router import router as users_router
from app.domains.branches.router import router as branches_router
from app.domains.loans.router import router as loans_router
from app.domains.loans.collateral import router as collateral_router
from app.domains.ocr.router import router as ocr_router
from app.api.v1.mobile import router as mobile_api_router, warm_mobile_static_cache
from app.domains.core_banking.router import router as core_banking_router
from app.domains.customers.router import router as customers_router
from app.domains.configuration.router import router as configuration_router
from app.domains.configuration.context import ConfigurationContextMiddleware
from app.domains.products.router import router as products_router
from app.domains.workflow.router import router as workflow_router
from app.domains.tasks.router import router as tasks_router

@asynccontextmanager
async def lifespan(app: FastAPI):
    init_engine()
    if settings.is_production:
        db_url = settings.DATABASE_URL
        if "neon.tech" in db_url and "-pooler" not in db_url:
            logging.warning(
                "CRITICAL WARNING: The application is running in production but does NOT appear to use Neon's pooled connection string. "
                "This can lead to connection exhaustion under serverless scaling."
            )
    await init_rate_limiter()
    await init_cache()
    await warm_mobile_static_cache()
    try:
        async with get_connection() as conn:
            await verify_runtime_database_role(conn)
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

            # Verify or create ocr_jobs table
            if "postgresql" in settings.DATABASE_URL:
                row = await conn.fetchrow(
                    "SELECT 1 FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'ocr_jobs'"
                )
                table_exists = bool(row)
            else:
                row = await conn.fetchrow(
                    "SELECT 1 FROM sqlite_master WHERE type='table' AND name='ocr_jobs'"
                )
                table_exists = bool(row)

            if not table_exists:
                logging.info("Creating ocr_jobs table...")
                await conn.execute(
                    """
                    CREATE TABLE ocr_jobs (
                        id UUID PRIMARY KEY,
                        document_id UUID NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
                        status TEXT NOT NULL DEFAULT 'pending',
                        created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
                        updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
                        error TEXT
                    )
                    """
                )
    except Exception as e:
        logging.error(f"Failed to dynamically verify or create columns/tables: {e}")
    yield
    await close_rate_limiter()
    await close_cache()
    await dispose_engine()

app = FastAPI(
    title=settings.PROJECT_NAME,
    docs_url=None,
    redoc_url=None,
    openapi_url=None,
    lifespan=lifespan
)
install_organised_openapi(app)

if not settings.is_production:
    @app.get("/openapi.json", include_in_schema=False)
    async def local_openapi(request: Request):
        require_local_docs_access(request)
        return JSONResponse(app.openapi(), headers={"Cache-Control": "no-store"})

    @app.get("/api/docs", include_in_schema=False)
    async def swagger_docs(request: Request):
        require_local_docs_access(request)
        return swagger_ui_response(
            request,
            openapi_url="/openapi.json",
            title=f"{settings.PROJECT_NAME} - Swagger UI",
        )

    @app.get("/api/redoc", include_in_schema=False)
    async def redoc_docs(request: Request):
        require_local_docs_access(request)
        return redoc_response(
            request,
            openapi_url="/openapi.json",
            title=f"{settings.PROJECT_NAME} - ReDoc",
        )

# CORS Policy
app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.cors_origins,
    allow_credentials=True,
    allow_methods=["GET", "POST", "PUT", "PATCH", "DELETE"],
    allow_headers=["Authorization", "Content-Type", "X-Request-ID", "X-CSRF-Token"],
)
app.add_middleware(
    TrustedHostMiddleware,
    allowed_hosts=settings.trusted_hosts,
    www_redirect=False,
)

# Custom Middlewares
app.add_middleware(
    SecurityHeadersMiddleware,
    cookie_secure=settings.COOKIE_SECURE,
    csp_nonce_enforced=settings.CSP_NONCE_ENFORCED,
)
app.add_middleware(RequestIDMiddleware)
app.add_middleware(ConfigurationContextMiddleware)
app.add_middleware(
    CrossSiteRequestMiddleware,
    allowed_origins=settings.cors_origins,
    cookie_secure=settings.COOKIE_SECURE,
)
app.add_middleware(PendingResponseCookiesMiddleware)
app.add_middleware(ResponseCacheInvalidationMiddleware)
app.add_middleware(
    PerformanceTimingMiddleware,
    expose_server_timing=not settings.is_production,
)

# Exception handlers
app.add_exception_handler(DomainException, domain_exception_handler)

# Setup templates and static mount
base_dir = os.path.dirname(os.path.abspath(__file__))
templates_dir = os.path.abspath(os.path.join(base_dir, "../../frontend/templates"))
static_dir = os.path.abspath(os.path.join(base_dir, "../../frontend/static"))
templates = create_templates(templates_dir)
# Versioned Cloudinary URLs let the CDN and browser cache each brand image once
# while every template continues to use the same source of truth.
templates.env.globals.update(
    brand_logo_black="https://res.cloudinary.com/ddezxlqjr/image/upload/v1784551475/MMFB_Logo_Black_lnma0l.png",
    brand_logo_white="https://res.cloudinary.com/ddezxlqjr/image/upload/v1784551475/MMFB_logo_White_gzthxm.png",
)


def safe_relative_redirect(value: str | None) -> str | None:
    """Return a same-site path after rejecting parser-confusion bypasses."""
    candidate = (value or "").strip()
    if not candidate or any(ord(character) < 32 for character in candidate):
        return None
    decoded = candidate
    for _ in range(2):
        decoded = unquote(decoded)
    if "\\" in decoded or not decoded.startswith("/") or decoded.startswith("//"):
        return None
    parsed = urlsplit(decoded)
    if parsed.scheme or parsed.netloc:
        return None
    return candidate

class ProtectedStaticFiles(StaticFiles):
    """Never expose customer uploads through the unauthenticated static mount."""

    async def get_response(self, path: str, scope):
        normalized = path.replace("\\", "/").lstrip("/")
        if normalized == "uploads" or normalized.startswith("uploads/"):
            raise StarletteHTTPException(status_code=404)
        response = await super().get_response(path, scope)
        if response.status_code == 200:
            response.headers["Cache-Control"] = "public, max-age=86400, stale-while-revalidate=604800"
        return response


app.mount("/static", ProtectedStaticFiles(directory=static_dir), name="static")


@app.get("/favicon.ico", include_in_schema=False)
async def favicon():
    """Serve the FieldCRM favicon for browsers that probe the conventional root path."""
    return FileResponse(
        os.path.join(static_dir, "icons", "favicon.ico"),
        media_type="image/x-icon",
        headers={"Cache-Control": "public, max-age=86400, stale-while-revalidate=604800"},
    )


@app.get("/api/v1/documents/{document_id}/download")
async def download_document(
    document_id: UUID,
    current_user=Depends(get_current_user),
    conn=Depends(authenticated_db_conn),
):
    """Authorise in FieldCRM before issuing a streamed file response with client name."""
    document = await DocumentRepository(conn).get_by_id_for_org(document_id, current_user.org_id)
    if not document:
        raise HTTPException(status_code=404, detail="Document not found")
    if document.get("loan_id"):
        loan = await LoanRepository(conn).get_by_id(document["loan_id"], current_user.org_id)
        if not loan:
            raise HTTPException(status_code=404, detail="Document not found")
        require_view(current_user, loan)
        await AuditService(conn).insert(
            org_id=current_user.org_id,
            entity_type="document",
            entity_id=document_id,
            action="document.downloaded",
            user_id=current_user.id,
            user_role=current_user.role,
            source="document_proxy",
        )

    # Determine a user-friendly filename containing the applicant name
    filename = document.get("original_name")
    if document.get("loan_id"):
        loan_app = await conn.fetchrow(
            "SELECT applicant_name FROM loan_applications WHERE id = $1;",
            document["loan_id"]
        )
        if loan_app and loan_app["applicant_name"]:
            applicant_name = loan_app["applicant_name"]
            if document.get("doc_type") == "offer_letter":
                filename = f"{applicant_name} - Offer Letter.pdf"
            else:
                orig_name = document.get("original_name", "")
                if applicant_name.lower() not in orig_name.lower():
                    # Format as e.g. "Oboh Cletus - utility_bill.pdf"
                    filename = f"{applicant_name} - {orig_name}"

    filename = sanitize_preview_filename(filename)
    local_path = resolve_local_document_path(document.get("stored_path"))
    if not document.get("cloud_public_id") and local_path:
        return FileResponse(
            local_path,
            media_type=document.get("mime_type"),
            filename=filename,
            headers={"Cache-Control": "private, no-store", "X-Content-Type-Options": "nosniff"},
        )

    if not document.get("cloud_public_id"):
        raise HTTPException(status_code=404, detail="Document not found")

    import httpx
    from fastapi.responses import StreamingResponse

    cloud_url = signed_download_url(document["cloud_public_id"], document["mime_type"])

    async def file_streamer():
        async with httpx.AsyncClient() as client:
            async with client.stream("GET", cloud_url) as r:
                if r.status_code != 200:
                    raise HTTPException(status_code=r.status_code, detail="Failed to fetch document from cloud storage")
                async for chunk in r.aiter_bytes():
                    yield chunk

    return StreamingResponse(
        file_streamer(),
        media_type=document.get("mime_type"),
        headers={
            "Content-Disposition": download_content_disposition(filename),
            "Cache-Control": "private, no-store",
            "X-Content-Type-Options": "nosniff",
        }
    )


@app.get("/api/v1/documents/{document_id}/preview")
async def preview_document(
    document_id: UUID,
    request: Request,
    page: int = Query(default=1, ge=1, le=100),
    current_user=Depends(get_current_user),
    conn=Depends(authenticated_db_conn),
):
    """Stream an authorised document inline so it can be safely embedded in the UI."""
    document = await DocumentRepository(conn).get_by_id_for_org(document_id, current_user.org_id)
    if not document or document.get("upload_status") not in (None, "done"):
        raise HTTPException(status_code=404, detail="Document not found")
    if document.get("loan_id"):
        loan = await LoanRepository(conn).get_by_id(document["loan_id"], current_user.org_id)
        if not loan:
            raise HTTPException(status_code=404, detail="Document not found")
        require_view(current_user, loan)
        await AuditService(conn).insert(
            org_id=current_user.org_id,
            entity_type="document",
            entity_id=document_id,
            action="document.previewed",
            user_id=current_user.id,
            user_role=current_user.role,
            source="document_proxy",
            notes=f"Page {page}",
        )

    stored_mime_type = normalize_mime_type(document.get("mime_type"))
    if stored_mime_type and stored_mime_type not in ALLOWED_PREVIEW_MIME_TYPES:
        raise HTTPException(status_code=415, detail="This document type cannot be previewed")

    # Images have a single page. PDFs are converted by Cloudinary into a PNG
    # for this page number, so the original PDF never reaches the browser.
    if stored_mime_type != "application/pdf" and page != 1:
        raise HTTPException(status_code=404, detail="Document preview page not found")

    local_path = resolve_local_document_path(document.get("stored_path"))
    if not document.get("cloud_public_id") and local_path:
        if page != 1:
            raise HTTPException(status_code=404, detail="Document preview page not found")
        return FileResponse(
            local_path,
            media_type=stored_mime_type,
            headers={
                "Content-Disposition": preview_content_disposition(document.get("original_name")),
                "Cache-Control": "private, no-store",
                "X-Content-Type-Options": "nosniff",
            },
        )
    if not document.get("cloud_public_id"):
        raise HTTPException(status_code=404, detail="Document not found")

    client: httpx.AsyncClient | None = None
    response: httpx.Response | None = None
    stream_context: Any | None = None
    try:
        # The public id is taken only from the already-authorised DB record.
        preview_url = signed_preview_url(document["cloud_public_id"], stored_mime_type or "", page=page)
        client = httpx.AsyncClient(timeout=30.0)
        stream_context = client.stream("GET", preview_url, headers={})
        response = await stream_context.__aenter__()
        response.raise_for_status()
    except httpx.HTTPStatusError as exc:
        # Cloudinary returns 400/404 when pg_N is beyond the final PDF page.
        # That is the normal end-of-document signal used by the image viewer.
        if page > 1 and exc.response.status_code in (400, 404):
            await close_cloudinary_stream(client, response, stream_context)
            raise HTTPException(status_code=404, detail="Document preview page not found") from exc
        if page == 1 and local_path:
            await close_cloudinary_stream(client, response, stream_context)
            return FileResponse(
                local_path,
                media_type=stored_mime_type,
                headers={"Content-Disposition": preview_content_disposition(document.get("original_name"))},
            )
        logger.warning("Cloudinary document preview request failed (%s)", type(exc).__name__)
        await close_cloudinary_stream(client, response, stream_context)
        raise HTTPException(status_code=502, detail="Document preview is temporarily unavailable") from exc
    except httpx.HTTPError as exc:
        logger.warning("Cloudinary document preview request failed (%s)", type(exc).__name__)
        await close_cloudinary_stream(client, response, stream_context)
        raise HTTPException(status_code=502, detail="Document preview is temporarily unavailable") from exc
    except Exception as exc:
        # Do not include exception text: it can contain a signed URL or token.
        logger.warning("Cloudinary document preview setup failed (%s)", type(exc).__name__)
        await close_cloudinary_stream(client, response, stream_context)
        raise HTTPException(status_code=502, detail="Document preview is temporarily unavailable") from exc

    mime_type = stored_mime_type or normalize_mime_type(response.headers.get("content-type"))
    if stored_mime_type == "application/pdf":
        mime_type = "image/png"
    if mime_type not in ALLOWED_PREVIEW_MIME_TYPES:
        await close_cloudinary_stream(client, response, stream_context)
        raise HTTPException(status_code=415, detail="This document type cannot be previewed")

    return StreamingResponse(
        iter_cloudinary_content(client, response, stream_context),
        media_type=mime_type,
        status_code=response.status_code,
        headers=preview_response_headers(document.get("original_name"), response),
    )


ALLOWED_PREVIEW_MIME_TYPES = {"application/pdf", "image/jpeg", "image/png"}
_FILENAME_CONTROL_CHARS = re.compile(r"[\x00-\x1f\x7f]")
_FILENAME_ASCII_UNSAFE = re.compile(r"[^A-Za-z0-9._ -]")


def resolve_local_document_path(stored_path: str | None) -> Path | None:
    """Resolve a seeded/static document without allowing traversal outside uploads."""
    if not stored_path or not stored_path.startswith("/static/uploads/"):
        return None
    uploads_root = (Path(static_dir) / "uploads").resolve()
    candidate = (Path(static_dir) / stored_path.removeprefix("/static/")).resolve()
    if uploads_root not in candidate.parents or not candidate.is_file():
        return None
    return candidate


def normalize_mime_type(value: str | None) -> str:
    """Return a lower-cased media type without parameters."""
    return value.split(";", 1)[0].strip().lower() if value else ""


def sanitize_preview_filename(value: str | None) -> str:
    """Remove characters that are unsafe in Content-Disposition filenames."""
    filename = str(value or "document")
    filename = _FILENAME_CONTROL_CHARS.sub("_", filename)
    filename = filename.replace("/", "_").replace("\\", "_").replace('"', "")
    filename = filename.strip(" .")
    return filename[:180] or "document"


def preview_content_disposition(value: str | None) -> str:
    filename = sanitize_preview_filename(value)
    ascii_filename = unicodedata.normalize("NFKD", filename).encode("ascii", "ignore").decode("ascii")
    ascii_filename = _FILENAME_ASCII_UNSAFE.sub("_", ascii_filename).strip(" .") or "document"
    encoded_filename = quote(filename, safe="!#$&+-.^_`|~")
    return f"inline; filename=\"{ascii_filename}\"; filename*=UTF-8''{encoded_filename}"


def download_content_disposition(value: str | None) -> str:
    return preview_content_disposition(value).replace("inline;", "attachment;", 1)


def preview_response_headers(filename: str | None, response: httpx.Response) -> dict[str, str]:
    headers = {
        "Cache-Control": "private, no-store",
        "Pragma": "no-cache",
        "X-Content-Type-Options": "nosniff",
        "Content-Disposition": preview_content_disposition(filename),
    }
    # Deliberately do not proxy Cloudinary disposition, cookies, auth, or cache policy.
    for header in ("Content-Length", "Accept-Ranges", "ETag", "Last-Modified"):
        if value := get_upstream_header(response, header):
            headers[header] = value
    if response.status_code == status.HTTP_206_PARTIAL_CONTENT:
        if value := get_upstream_header(response, "Content-Range"):
            headers["Content-Range"] = value
    return headers


def get_upstream_header(response: httpx.Response, name: str) -> str | None:
    """Read an HTTP header, including from simple mapping-based test doubles."""
    return response.headers.get(name) or response.headers.get(name.lower())


async def close_cloudinary_stream(
    client: httpx.AsyncClient | None,
    response: httpx.Response | None,
    stream_context: Any | None,
) -> None:
    if stream_context is not None:
        await stream_context.__aexit__(None, None, None)
    elif response is not None:
        await response.aclose()
    if client is not None:
        await client.aclose()


async def iter_cloudinary_content(
    client: httpx.AsyncClient,
    response: httpx.Response,
    stream_context: Any,
):
    """Yield upstream bytes and always release Cloudinary resources."""
    try:
        async for chunk in response.aiter_bytes():
            yield chunk
    except httpx.HTTPError as exc:
        # The response has started, so it cannot be converted to a 502 here.
        logger.warning("Cloudinary document preview stream interrupted (%s)", type(exc).__name__)
        raise
    finally:
        await close_cloudinary_stream(client, response, stream_context)


ERROR_PAGE_CONTENT = {
    400: ("We couldn't process that request", "Check the information you entered and try again."),
    403: ("You don't have access", "Your account does not have permission to view this page or perform this action."),
    404: ("Page not found", "The page you requested could not be found."),
    405: ("That action isn't allowed", "This page does not support the action you tried."),
    408: ("The request took too long", "Please try again in a moment."),
    409: ("We couldn't complete that action", "The information may have changed. Refresh the page and try again."),
    413: ("That file is too large", "Choose a smaller file and try again."),
    415: ("That file type isn't supported", "Choose a supported file format and try again."),
    422: ("Check the information you entered", "Some details are missing or invalid. Review them and try again."),
    429: ("Too many requests", "Please wait a moment before trying again."),
    500: ("Something went wrong", "We couldn't complete your request. Please try again."),
    502: ("A service is temporarily unavailable", "Please try again in a few moments."),
    503: ("Service temporarily unavailable", "We're unable to handle your request right now. Please try again shortly."),
    504: ("A service took too long to respond", "Please try again in a few moments."),
}


def render_error_page(
    request: Request,
    status_code: int,
    detail: Any = None,
    headers: dict[str, str] | None = None,
):
    """Render a safe, useful error screen for browser page requests."""
    title, default_message = ERROR_PAGE_CONTENT.get(
        status_code,
        ("We couldn't complete your request", "Please return to the dashboard and try again."),
    )
    # Validation and other client-error details help users correct their input.
    # Server-error details can expose implementation or infrastructure data.
    message = detail if status_code < 500 and isinstance(detail, str) and detail else default_message
    request_id = getattr(request.state, "request_id", None)
    return templates.TemplateResponse(
        request,
        "shared/error.html",
        {
            "status_code": status_code,
            "title": title,
            "message": message,
            "request_id": request_id,
            "retry_url": str(request.url.path),
        },
        status_code=status_code,
        headers=headers,
    )


async def browser_domain_exception_handler(request: Request, exc: DomainException):
    if not request.url.path.startswith("/api/"):
        return render_error_page(request, exc.status_code, exc.message)
    return await domain_exception_handler(request, exc)


app.add_exception_handler(DomainException, browser_domain_exception_handler)

# Mount Routers
app.include_router(auth_router, prefix=f"{settings.API_V1_STR}/auth", tags=["Authentication"])
app.include_router(users_router, prefix=f"{settings.API_V1_STR}/users", tags=["Users"])
app.include_router(branches_router, tags=["Branches"])
app.include_router(mobile_api_router, prefix=f"{settings.API_V1_STR}/mobile", tags=["Mobile API"])
app.include_router(ocr_router)
app.include_router(core_banking_router)
app.include_router(customers_router)
app.include_router(configuration_router)
app.include_router(products_router)
app.include_router(workflow_router)
app.include_router(tasks_router)

# Mount Loan pages at root
app.include_router(loans_router)
app.include_router(collateral_router)

logger = logging.getLogger("FieldCRMMain")

import urllib.parse


def browser_login_url(next_url: str | None = None) -> str:
    """Return the staff login URL with an optional same-site destination."""
    if next_url:
        return f"/login?next={urllib.parse.quote(next_url)}"
    return "/login"


def safe_login_destination(value: str | None) -> str | None:
    """Reject authentication endpoints as post-login redirect targets."""
    candidate = safe_relative_redirect(value)
    if candidate:
        path = urllib.parse.urlsplit(candidate).path.rstrip("/") or "/"
        if path in {"/login", "/logout", "/forgot-password", "/reset-password"}:
            return None
    return candidate


def browser_home_url(role: str | None) -> str:
    """Return the authenticated landing page for a server-trusted role."""
    normalized = (role or "").strip().lower().replace(" ", "_")
    if normalized == "configuration_admin":
        return "/configuration"
    return "/dashboard"


async def public_db_conn():
    """Unauthenticated connection for login and password-recovery forms.

    Do not let a stale session/refresh cookie trigger transparent auth before
    the user has a chance to submit fresh credentials.
    """
    async with get_connection() as conn:
        yield conn

@app.exception_handler(StarletteHTTPException)
@app.exception_handler(HTTPException)
async def http_exception_handler(request: Request, exc: HTTPException):
    # Authentication failures lead to the staff login entry point.
    # All other browser failures render HTML; APIs retain the JSON contract.
    is_api = request.url.path.startswith("/api/")
    if exc.status_code == status.HTTP_401_UNAUTHORIZED and not is_api:
        next_url = str(request.url.path)
        if request.url.query:
            next_url += f"?{request.url.query}"
        return RedirectResponse(
            url=browser_login_url(safe_login_destination(next_url)),
            status_code=status.HTTP_303_SEE_OTHER
        )
    if not is_api:
        return render_error_page(request, exc.status_code, exc.detail, exc.headers)

    request_id = getattr(request.state, "request_id", "unknown")
    return JSONResponse(
        status_code=exc.status_code,
        content={"detail": exc.detail, "request_id": request_id},
        headers=exc.headers,
    )


@app.exception_handler(RequestValidationError)
async def validation_exception_handler(request: Request, exc: RequestValidationError):
    """Render form/page validation failures while retaining FastAPI's API JSON."""
    if request.url.path.startswith("/api/"):
        return await request_validation_exception_handler(request, exc)
    return render_error_page(request, status.HTTP_422_UNPROCESSABLE_CONTENT)


@app.exception_handler(Exception)
async def unexpected_exception_handler(request: Request, exc: Exception):
    """Give users a branded 500 page without leaking exception details."""
    request_id = getattr(request.state, "request_id", "unknown")
    logger.exception(
        "Unhandled request error path=%s request_id=%s",
        request.url.path,
        request_id,
        exc_info=(type(exc), exc, exc.__traceback__),
    )
    if request.url.path.startswith("/api/"):
        return JSONResponse(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            content={"detail": "Internal server error", "request_id": request_id},
        )
    return render_error_page(request, status.HTTP_500_INTERNAL_SERVER_ERROR)

def raise_login_redirect():
    raise HTTPException(
        status_code=status.HTTP_303_SEE_OTHER,
        headers={"Location": browser_login_url()}
    )

# Public product pages
@app.get("/")
async def root_view(request: Request):
    release = {
        "available": bool(settings.ANDROID_APK_URL),
        "version": settings.ANDROID_APK_VERSION,
        "released_at": settings.ANDROID_APK_RELEASED_AT,
        "size_bytes": settings.ANDROID_APK_SIZE_BYTES,
        "sha256": settings.ANDROID_APK_SHA256,
        "channel": settings.ANDROID_APK_CHANNEL,
    }
    response = templates.TemplateResponse(request, "shared/public_home.html", {"release": release})
    response.headers["Cache-Control"] = "public, max-age=300, s-maxage=3600, stale-while-revalidate=86400"
    return response


@app.get("/download/android")
async def download_android():
    if not settings.ANDROID_APK_URL:
        raise HTTPException(status_code=404, detail="The Android release is not available yet.")
    return RedirectResponse(
        url=settings.ANDROID_APK_URL,
        status_code=status.HTTP_307_TEMPORARY_REDIRECT,
        headers={"Cache-Control": "no-store"},
    )


@app.get("/terms")
async def terms_view(request: Request):
    response = templates.TemplateResponse(request, "shared/public_terms.html", {})
    response.headers["Cache-Control"] = "public, max-age=3600, s-maxage=86400, stale-while-revalidate=604800"
    return response


@app.get("/robots.txt", response_class=PlainTextResponse)
async def robots_txt(request: Request):
    origin = str(request.base_url).rstrip("/")
    return f"User-agent: *\nAllow: /\nDisallow: /api/\nDisallow: /dashboard\nSitemap: {origin}/sitemap.xml\n"


@app.get("/sitemap.xml")
async def sitemap_xml(request: Request):
    origin = str(request.base_url).rstrip("/")
    body = (
        '<?xml version="1.0" encoding="UTF-8"?>'
        '<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">'
        f'<url><loc>{origin}/</loc></url>'
        f'<url><loc>{origin}/terms</loc></url>'
        '</urlset>'
    )
    return Response(content=body, media_type="application/xml")

@app.get("/login")
async def render_login(request: Request):
    token = request.cookies.get("session") or request.cookies.get("__Host-session")
    if token:
        try:
            from app.core.dependencies import get_current_user_from_token
            current_user = await get_current_user_from_token(token)
            # Avoid re-sending an already authenticated user to a route that
            # may be forbidden for their role. The POST login flow still
            # honours a validated `next` value after fresh authentication.
            return RedirectResponse(
                url=browser_home_url(getattr(current_user, "role", None)),
                status_code=status.HTTP_303_SEE_OTHER,
            )
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
    conn=Depends(public_db_conn),
):
    """Authenticate user by email and password, set session cookie."""
    from app.domains.auth.repository import AuthRepository
    from app.domains.auth.service import AuthService

    await enforce_login_limits(request, username)
    repo = AuthRepository(conn)
    service = AuthService(repo)
    try:
        ip_address = request.client.host if request.client else None
        user_agent = request.headers.get("user-agent")
        session_data = await service.authenticate_web(
            username, password, user_agent=user_agent, ip_address=ip_address
        )
        token = session_data["access_token"]
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
    redirect_url = browser_home_url(session_data.get("role"))
    validated_next = safe_login_destination(next)
    if validated_next:
        redirect_url = validated_next

    redirect = RedirectResponse(url=redirect_url, status_code=status.HTTP_303_SEE_OTHER)
    redirect.set_cookie(
        key="session",
        value=token,
        httponly=True,
        secure=is_secure,
        samesite="lax",
        max_age=settings.ACCESS_TOKEN_EXPIRE_MINUTES * 60,
        path="/",
    )
    redirect.set_cookie(
        key="refresh_token",
        value=session_data["refresh_token"],
        httponly=True,
        secure=is_secure,
        samesite="lax",
        expires=session_data["expires_at"],
        path="/",
    )
    return redirect

@app.post("/logout")
async def logout_web(request: Request, next: str = Form(None), conn=Depends(db_conn)):
    from app.domains.auth.repository import AuthRepository
    from app.domains.auth.service import AuthService

    await AuthService(AuthRepository(conn)).revoke_web_session(
        request.cookies.get("refresh_token")
    )
    redirect_url = browser_login_url()
    validated_next = safe_relative_redirect(next)
    if validated_next:
        redirect_url += f"?next={urllib.parse.quote(validated_next)}"

    redirect = RedirectResponse(url=redirect_url, status_code=status.HTTP_303_SEE_OTHER)
    redirect.delete_cookie(key="session", path="/")
    redirect.delete_cookie(key="refresh_token", path="/")
    redirect.delete_cookie(key="__Host-session", path="/")
    redirect.delete_cookie(key="configuration_mfa", path="/configuration")
    return redirect

@app.get("/forgot-password")
async def render_forgot_password(request: Request):
    return templates.TemplateResponse(request, "shared/forgot_password.html", {"submitted": False, "error": None})

@app.post("/forgot-password")
async def process_forgot_password(request: Request, email: str = Form(...), conn=Depends(db_conn)):
    from app.domains.auth.repository import AuthRepository
    from app.domains.auth.service import AuthService
    await enforce_reset_limits(request, email)
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
    await enforce_reset_limits(request, token)
    from app.domains.auth.repository import AuthRepository
    from app.domains.auth.service import AuthService
    from app.core.security import validate_password_strength
    if new_password != confirm_password:
        return templates.TemplateResponse(request, "shared/reset_password.html", {"token": token, "error": "Passwords do not match.", "success": False, "invitation": invitation})
    try:
        validate_password_strength(new_password)
    except ValueError as exc:
        return templates.TemplateResponse(
            request,
            "shared/reset_password.html",
            {"token": token, "error": str(exc), "success": False, "invitation": invitation},
            status_code=status.HTTP_400_BAD_REQUEST,
        )
    ok = await AuthService(AuthRepository(conn)).reset_password(token, new_password)
    if not ok:
        return templates.TemplateResponse(request, "shared/reset_password.html", {"token": token, "error": "Invalid or expired reset link.", "success": False, "invitation": invitation})
    return RedirectResponse(url="/login?reset=1", status_code=status.HTTP_303_SEE_OTHER)

@app.get("/api/v1/health")
async def health_check():
    return {"status": "healthy", "database": "configured"}
