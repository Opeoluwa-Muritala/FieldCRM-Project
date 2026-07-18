"""
Template utilities for role-based and device-aware template resolution.

Resolves the correct Jinja2 template path based on the authenticated user's role
and the requesting device type (mobile vs desktop).
"""
import re
from app.core.workflow import WORKFLOW_STAGES, ROLE_LABELS


# Pre-compiled mobile User-Agent pattern — avoids regex compilation per request
_MOBILE_UA_PATTERN = re.compile(
    r"Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini|Mobile|mobile",
    re.IGNORECASE,
)

# Valid roles that map to template subdirectories
_VALID_ROLES = frozenset({
    "account_officer",
    "loan_officer",
    "branch_manager",
    "branch_supervisor",
    "credit_analyst",
    "auditor",
    "system_admin",
    "crm",
    "head_crm",
    "md",
    "ed",
    "legal",
})

_ROLE_TEMPLATE_ALIASES = {
    "account_officer": "loan_officer",
    "head_crm": "crm",
}


def detect_device_type(request) -> str:
    """Determine device type from User-Agent header.

    Returns 'mobile' for phones/tablets, 'desktop' for everything else.
    Server-side detection is a heuristic — the frontend CSS also uses
    media queries as the authoritative breakpoint.
    """
    return "desktop"


def get_base_shell(device_type: str) -> str:
    """Return the base shell template path for the given device type.

    The shell provides the layout grid (mobile: topbar+content+tabbar,
    desktop: sidebar+topbar+content).
    """
    return "base/desktop_shell.html"


def get_role_template(role: str, template_name: str) -> str:
    """Resolve a role-specific template path.

    Falls back to shared/ if no role-specific template exists.
    Example: get_role_template("loan_officer", "dashboard.html")
             -> "loan_officer/dashboard.html"
    """
    # Normalize role to lowercase snake_case
    normalized_role = _ROLE_TEMPLATE_ALIASES.get(role.lower().replace(" ", "_"), role.lower().replace(" ", "_"))
    if normalized_role in _VALID_ROLES:
        return f"{normalized_role}/{template_name}"
    # Fallback for unknown roles — should never happen with proper RBAC
    return f"shared/{template_name}"


def get_role_sidebar_component(role: str) -> str:
    """Return the sidebar component template path for the given role.

    Each role has a genuinely different sidebar — not filtered, but separate.
    """
    normalized_role = _ROLE_TEMPLATE_ALIASES.get(role.lower().replace(" ", "_"), role.lower().replace(" ", "_"))
    if normalized_role in _VALID_ROLES:
        return f"components/desktop_sidebar_{normalized_role}.html"
    return "components/desktop_sidebar_loan_officer.html"


def get_role_tabbar_component(role: str) -> str:
    """Return the mobile tab bar component template path for the given role.

    Each role gets a different set of 5 bottom tabs.
    """
    normalized_role = _ROLE_TEMPLATE_ALIASES.get(role.lower().replace(" ", "_"), role.lower().replace(" ", "_"))
    if normalized_role in _VALID_ROLES:
        return f"components/mobile_tabbar_{normalized_role}.html"
    return "components/mobile_tabbar_loan_officer.html"


def build_template_context(request, user, **kwargs) -> dict:
    """Build the standard template context with device and role info.

    Every template response should use this to ensure consistent
    context variables across all role-specific templates.
    """
    device = detect_device_type(request)
    normalized_role = user.role.lower().replace(" ", "_") if user else "loan_officer"

    if "documents" in kwargs and isinstance(kwargs["documents"], list):
        mapped_docs = []
        for doc in kwargs["documents"]:
            if isinstance(doc, dict):
                doc_copy = dict(doc)
                if "doc_type" in doc_copy and "category" not in doc_copy:
                    doc_copy["category"] = doc_copy["doc_type"]
                if "verified" in doc_copy and "status" not in doc_copy:
                    doc_copy["status"] = "verified" if doc_copy["verified"] else "needs_review"
                
                if doc_copy.get("cloud_public_id"):
                    url = f"/api/v1/documents/{doc_copy['id']}/preview"
                else:
                    url = doc_copy.get("stored_path") or doc_copy.get("cloud_preview_url") or ""
                if url:
                    # Clean up Cloudinary URLs to allow inline rendering
                    url = url.replace("fl_attachment,", "").replace(",fl_attachment", "").replace("fl_attachment", "")
                    url = url.replace("/raw/upload/", "/image/upload/").replace("/raw/authenticated/", "/image/authenticated/")
                doc_copy["url"] = url
                
                # Prefer the MIME type stored at upload time. Protected document
                # URLs do not expose their file extension, so URL inference alone
                # incorrectly classifies PDFs as application/octet-stream.
                mime_type = doc_copy.get("mime_type") or "application/octet-stream"
                lower_url = url.lower()
                if mime_type == "application/octet-stream" and (".pdf" in lower_url or "pdf" in lower_url):
                    mime_type = "application/pdf"
                elif mime_type == "application/octet-stream" and (".jpg" in lower_url or ".jpeg" in lower_url or "jpg" in lower_url or "jpeg" in lower_url):
                    mime_type = "image/jpeg"
                elif mime_type == "application/octet-stream" and (".png" in lower_url or "png" in lower_url):
                    mime_type = "image/png"
                elif mime_type == "application/octet-stream" and doc_copy.get("category") in ("bank_statement", "pledge_form", "guarantor_form_1", "guarantor_form_2"):
                    mime_type = "application/pdf"
                doc_copy["mime_type"] = mime_type
                
                mapped_docs.append(doc_copy)
            else:
                mapped_docs.append(doc)
        kwargs["documents"] = mapped_docs

    app = kwargs.get("app")
    app_stage = app.get("stage") if isinstance(app, dict) else getattr(app, "stage", None)
    workflow_roles = dict(WORKFLOW_STAGES)
    prior_stages = {
        stage: WORKFLOW_STAGES[index - 1][0]
        for index, (stage, _) in enumerate(WORKFLOW_STAGES)
        if index > 0
    }
    previous_stage = prior_stages.get(app_stage)
    role_for_stage = workflow_roles.get(app_stage)
    can_return_previous = bool(
        user and previous_stage and normalized_role in {role_for_stage, "system_admin"}
    )

    ctx = {
        "request": request,
        "current_user": user,
        "user": user,
        "device": device,
        "shell": get_base_shell(device),
        "sidebar_component": get_role_sidebar_component(user.role) if user else "",
        "tabbar_component": get_role_tabbar_component(user.role) if user else "",
        "user_role": normalized_role,
        "can_return_previous": can_return_previous,
        "return_target_label": ROLE_LABELS.get(
            workflow_roles.get(previous_stage, ""),
            (previous_stage or "").replace("_", " ").title(),
        ),
    }
    ctx.update(kwargs)
    return ctx
