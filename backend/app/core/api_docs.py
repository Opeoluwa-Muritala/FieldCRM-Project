"""Nonce-aware, loopback-only development API documentation responses."""

import ipaddress

from fastapi import HTTPException
from fastapi.openapi.docs import get_redoc_html, get_swagger_ui_html
from fastapi.openapi.utils import get_openapi
from starlette.requests import Request
from starlette.responses import HTMLResponse


OPENAPI_TAGS = [
    {"name": "Authentication", "description": "Sign-in, token refresh, logout, password recovery, and sessions."},
    {"name": "Users & Access", "description": "Staff accounts, invitations, roles, and account lifecycle."},
    {"name": "Organisation & Branches", "description": "Institution branches and organisational reference data."},
    {"name": "Customers", "description": "Customer creation, lookup, duplicate checks, and application profiles."},
    {"name": "Applications", "description": "Application creation, intake, retrieval, and submission data."},
    {"name": "Documents", "description": "Authorised uploads, finalisation, previews, and downloads."},
    {"name": "Field Operations", "description": "Visits, guarantors, collateral, and valuations."},
    {"name": "Credit Assessment", "description": "Credit rules, bureau evidence, checklists, and OCR review."},
    {"name": "Application Workflow", "description": "Operational review, return, readiness, and workflow transitions."},
    {"name": "Executive Approvals", "description": "ED, MD, legal, committee, offer, and disbursement actions."},
    {"name": "Repayments", "description": "Payments and repayment schedules."},
    {"name": "Workflow Configuration", "description": "Workflow definitions, delegations, and portfolio reassignment."},
    {"name": "Configuration", "description": "Published mobile, product, and workflow configuration contracts."},
    {"name": "Products & Forms", "description": "Loan product and configurable form definitions."},
    {"name": "Core Banking", "description": "CBS loan snapshots, refresh, webhooks, and internal synchronization."},
    {"name": "Portfolio & Reports", "description": "Portfolio-at-risk and portfolio reporting data."},
    {"name": "Notifications", "description": "User notifications and read state."},
    {"name": "Audit & Compliance", "description": "Audit history, checklists, and compliance flags."},
    {"name": "Search & Reference", "description": "Search, FAQs, onboarding, and reference lookups."},
    {"name": "Mobile Workspace", "description": "Mobile dashboards, queues, user profile, and workspace data."},
    {"name": "System & Internal", "description": "Health, system activity, and restricted worker endpoints."},
]

OPENAPI_TAG_GROUPS = [
    {"name": "Identity & Access", "tags": ["Authentication", "Users & Access", "Organisation & Branches", "Customers"]},
    {"name": "Origination", "tags": ["Applications", "Documents", "Field Operations", "Credit Assessment"]},
    {"name": "Decisioning", "tags": ["Application Workflow", "Executive Approvals", "Workflow Configuration"]},
    {"name": "Product Setup", "tags": ["Configuration", "Products & Forms", "Core Banking"]},
    {"name": "Servicing", "tags": ["Repayments", "Portfolio & Reports", "Notifications"]},
    {"name": "Governance", "tags": ["Audit & Compliance", "Search & Reference"]},
    {"name": "Channels & Platform", "tags": ["Mobile Workspace", "System & Internal"]},
]

_HTTP_METHODS = frozenset({"get", "post", "put", "patch", "delete", "options", "head"})


def require_local_docs_access(request: Request) -> None:
    """Hide development documentation from non-loopback clients.

    Host validation alone is insufficient when a development server is bound to
    a network interface.  Requiring both a loopback host and loopback peer keeps
    the schema local even in a non-production environment.
    """
    host = (request.url.hostname or "").rstrip(".").lower()
    client_host = request.client.host if request.client else ""
    if host == "testserver" and client_host == "testclient":
        return
    try:
        is_loopback_client = ipaddress.ip_address(client_host).is_loopback
    except ValueError:
        is_loopback_client = False
    if host not in {"localhost", "127.0.0.1", "::1"} or not is_loopback_client:
        raise HTTPException(status_code=404, detail="Not found")


def documentation_tag(path: str) -> str:
    """Assign one stable, conventional tag to an API resource path."""
    if path.startswith("/api/v1/auth/") or "/mobile/auth/" in path or path.endswith("/settings/change-password"):
        return "Authentication"
    if "/users" in path:
        return "Users & Access"
    if path.endswith("/branches") or "/branches/" in path:
        return "Organisation & Branches"
    if path.startswith("/api/v1/config/admin/products") or "/mobile/products" in path:
        return "Products & Forms"
    if path.startswith("/api/v1/config/") or path.endswith("/mobile/config"):
        return "Configuration"
    if "core-banking" in path:
        return "Core Banking"
    if path.startswith("/api/v1/customers") or "/mobile/borrowers" in path:
        return "Customers"
    if "/documents" in path:
        return "Documents"
    if any(part in path for part in ("/visitation", "/valuation", "/collateral", "/guarantors/")):
        return "Field Operations"
    if any(part in path for part in ("/credit-", "/bureau", "/ocr-")):
        return "Credit Assessment"
    if any(part in path for part in ("/ed-", "/md-", "/executive-", "/mcc", "/offer", "/disbursement")):
        return "Executive Approvals"
    if any(part in path for part in ("/approval-readiness", "/approve", "/crm-review", "/return", "/submit-to-", "/workflow/advance")):
        return "Application Workflow"
    if "/payments" in path or "/repayment-schedule" in path:
        return "Repayments"
    if path.startswith("/api/v1/workflow/"):
        return "Workflow Configuration"
    if "/reports/" in path:
        return "Portfolio & Reports"
    if "/notifications" in path:
        return "Notifications"
    if any(part in path for part in ("/audit", "/compliance-flags")):
        return "Audit & Compliance"
    if any(part in path for part in ("/search", "/faqs", "/onboarding")):
        return "Search & Reference"
    if path.startswith("/api/v1/mobile/applications"):
        return "Applications"
    if path.startswith("/api/v1/mobile/"):
        return "Mobile Workspace"
    if any(part in path for part in ("/health", "/internal/", "/system-activity")):
        return "System & Internal"
    return "Applications"


def install_organised_openapi(app) -> None:
    """Expose a concise API-only OpenAPI schema with one tag per operation."""
    def organised_openapi():
        if app.openapi_schema:
            return app.openapi_schema
        schema = get_openapi(
            title=app.title,
            version=app.version,
            description=app.description,
            routes=app.routes,
            tags=OPENAPI_TAGS,
        )
        schema["paths"] = {
            path: path_item
            for path, path_item in schema.get("paths", {}).items()
            if path.startswith("/api/v1/")
        }
        schema["x-tagGroups"] = OPENAPI_TAG_GROUPS
        for path, path_item in schema["paths"].items():
            for method, operation in path_item.items():
                if method in _HTTP_METHODS and isinstance(operation, dict):
                    operation["tags"] = [documentation_tag(path)]
        app.openapi_schema = schema
        return schema

    app.openapi = organised_openapi


def swagger_ui_response(request: Request, *, openapi_url: str, title: str) -> HTMLResponse:
    """Render FastAPI's Swagger UI without weakening the strict CSP."""
    nonce = request.scope.get("state", {}).get("csp_nonce")
    if not nonce:
        raise RuntimeError("Swagger UI requires the request CSP nonce")

    generated = get_swagger_ui_html(
        openapi_url=openapi_url,
        title=title,
        swagger_js_url="/static/js/swagger-ui-bundle.js",
        swagger_css_url="/static/css/swagger-ui.css",
        swagger_favicon_url="/static/icons/favicon.svg",
    )
    body = generated.body.decode("utf-8")
    body = body.replace("<script ", f'<script nonce="{nonce}" ')
    body = body.replace("<script>", f'<script nonce="{nonce}">')
    body = body.replace("<style ", f'<style nonce="{nonce}" ')
    body = body.replace("<style>", f'<style nonce="{nonce}">')
    return HTMLResponse(body, headers={"Cache-Control": "no-store"})


def redoc_response(request: Request, *, openapi_url: str, title: str) -> HTMLResponse:
    """Render local ReDoc assets for the non-production documentation route."""
    nonce = request.scope.get("state", {}).get("csp_nonce")
    if not nonce:
        raise RuntimeError("ReDoc requires the request CSP nonce")

    generated = get_redoc_html(
        openapi_url=openapi_url,
        title=title,
        redoc_js_url="/static/js/redoc.standalone.js",
        redoc_favicon_url="/static/icons/favicon.svg",
        with_google_fonts=False,
    )
    body = generated.body.decode("utf-8")
    body = body.replace("<script ", f'<script nonce="{nonce}" ')
    body = body.replace("<script>", f'<script nonce="{nonce}">')
    body = body.replace("<style ", f'<style nonce="{nonce}" ')
    body = body.replace("<style>", f'<style nonce="{nonce}">')
    return HTMLResponse(body, headers={"Cache-Control": "no-store"})
