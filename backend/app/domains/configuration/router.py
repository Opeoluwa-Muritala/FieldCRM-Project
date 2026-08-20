import json
from pathlib import Path
from urllib.parse import quote
from uuid import UUID

from fastapi import APIRouter, Depends, Form, HTTPException, Request, Response
from fastapi.responses import RedirectResponse

from app.config import settings
from app.core.dependencies import authenticated_db_conn, get_current_user
from app.core.field_encryption import decrypt_sensitive, encrypt_sensitive
from app.core.templates import create_templates
from app.domains.configuration.access import require_restricted_configuration_access
from app.domains.configuration.catalog import FEATURE_DEFAULTS, FEATURE_GROUPS
from app.domains.configuration.mfa import (
    clear_mfa_failures,
    enforce_mfa_attempt_limit,
    new_secret,
    qr_code_data_url,
    record_mfa_failure,
    token_is_valid,
    verification_token,
    verify_totp,
)
from app.domains.configuration.repository import ConfigurationRepository
from app.domains.configuration.schemas import DraftCreate, DraftPatch, MfaCode
from app.domains.configuration.service import ConfigurationService

router = APIRouter()
templates = create_templates(str(Path(__file__).resolve().parents[4] / "frontend" / "templates"))


def _service(conn):
    return ConfigurationService(ConfigurationRepository(conn))


def _row(row):
    if not row:
        return None
    value = dict(row)
    value["payload"] = dict(value["payload"])
    return value


def _require_page_access(request: Request, current_user) -> None:
    require_restricted_configuration_access(request, current_user, require_mfa=False)
    if not token_is_valid(request.cookies.get("configuration_mfa"), current_user.id):
        raise HTTPException(status_code=428, detail="Configuration MFA verification is required")


async def _view_context(conn, current_user) -> dict:
    repo = ConfigurationRepository(conn)
    payload, current = await ConfigurationService(repo).effective(current_user.org_id)
    versions = [_row(row) for row in await repo.list(current_user.org_id)]
    effective_features = {**FEATURE_DEFAULTS, **payload.get("features", {})}
    active_draft = next((version for version in versions if version["status"] == "draft"), None)
    draft_features = (
        {**FEATURE_DEFAULTS, **active_draft["payload"].get("features", {})}
        if active_draft else effective_features
    )
    return {
        "current_user": current_user,
        "feature_groups": FEATURE_GROUPS,
        "effective": payload,
        "effective_features": effective_features,
        "draft_features": draft_features,
        "current": _row(current),
        "versions": versions,
        "active_draft": active_draft,
        "enabled_feature_count": sum(bool(value) for value in effective_features.values()),
        "pending_approval_count": sum(version["status"] == "pending_approval" for version in versions),
    }


@router.get("/configuration/mfa")
async def mfa_setup(request: Request, current_user=Depends(get_current_user), conn=Depends(authenticated_db_conn)):
    require_restricted_configuration_access(request, current_user, require_mfa=False)
    repo = ConfigurationRepository(conn)
    state = await repo.mfa_state(current_user.id, current_user.org_id)
    if not state:
        raise HTTPException(status_code=404, detail="User not found")
    encrypted = state["config_mfa_secret_encrypted"]
    enabled = bool(state["config_mfa_enabled"])
    secret = None
    if not encrypted:
        secret = new_secret()
        await repo.save_mfa(current_user.id, current_user.org_id,
                            encrypt_sensitive(secret, context=f"configuration:mfa:{current_user.id}"), False)
    elif not enabled:
        # Enrollment was started but not completed. The seed remains available
        # only until the first successful verification.
        secret = decrypt_sensitive(encrypted, context=f"configuration:mfa:{current_user.id}")

    enrollment_uri = None
    qr_data_url = None
    if secret:
        issuer = quote("FieldCRM Configuration")
        account = quote(current_user.email)
        enrollment_uri = f"otpauth://totp/{issuer}:{account}?secret={secret}&issuer={issuer}"
        qr_data_url = qr_code_data_url(enrollment_uri)
    return templates.TemplateResponse(request, "configuration/mfa.html", {
        "current_user": current_user, "secret": secret,
        "otpauth": enrollment_uri,
        "qr_code_data_url": qr_data_url,
        "enabled": enabled,
    }, headers={"Cache-Control": "no-store"})


@router.post("/configuration/mfa")
async def mfa_verify(request: Request, response: Response, code: str = Form(...),
                     current_user=Depends(get_current_user), conn=Depends(authenticated_db_conn)):
    require_restricted_configuration_access(request, current_user, require_mfa=False)
    enforce_mfa_attempt_limit(current_user.id)
    validated = MfaCode(code=code)
    repo = ConfigurationRepository(conn)
    state = await repo.mfa_state(current_user.id, current_user.org_id)
    if not state or not state["config_mfa_secret_encrypted"]:
        raise HTTPException(status_code=409, detail="Enroll MFA first")
    secret = decrypt_sensitive(state["config_mfa_secret_encrypted"], context=f"configuration:mfa:{current_user.id}")
    if not verify_totp(secret, validated.code):
        record_mfa_failure(current_user.id)
        raise HTTPException(status_code=400, detail="Invalid authentication code")
    clear_mfa_failures(current_user.id)
    await repo.save_mfa(current_user.id, current_user.org_id, state["config_mfa_secret_encrypted"], True)
    redirect = RedirectResponse("/configuration", status_code=303)
    redirect.set_cookie("configuration_mfa", verification_token(current_user.id), httponly=True,
                        secure=settings.COOKIE_SECURE or request.url.scheme == "https", samesite="strict",
                        max_age=15 * 60, path="/configuration")
    return redirect


@router.get("/configuration")
async def hub(request: Request, current_user=Depends(get_current_user), conn=Depends(authenticated_db_conn)):
    require_restricted_configuration_access(request, current_user, require_mfa=False)
    if not token_is_valid(request.cookies.get("configuration_mfa"), current_user.id):
        return RedirectResponse("/configuration/mfa", status_code=303)
    return templates.TemplateResponse(
        request, "configuration/hub.html", await _view_context(conn, current_user),
        headers={"Cache-Control": "no-store"},
    )


@router.get("/configuration/features")
async def feature_controls(request: Request, current_user=Depends(get_current_user), conn=Depends(authenticated_db_conn)):
    _require_page_access(request, current_user)
    return templates.TemplateResponse(
        request, "configuration/features.html", await _view_context(conn, current_user),
        headers={"Cache-Control": "no-store"},
    )


@router.get("/configuration/versions")
async def version_history(request: Request, current_user=Depends(get_current_user), conn=Depends(authenticated_db_conn)):
    _require_page_access(request, current_user)
    return templates.TemplateResponse(
        request, "configuration/versions.html", await _view_context(conn, current_user),
        headers={"Cache-Control": "no-store"},
    )


@router.post("/configuration/drafts")
async def create_draft(request: Request, reason: str = Form(...), effective_at: str | None = Form(None),
                       current_user=Depends(get_current_user), conn=Depends(authenticated_db_conn)):
    require_restricted_configuration_access(request, current_user)
    draft = DraftCreate(reason=reason, effective_at=effective_at or None)
    await _service(conn).create_draft(current_user.org_id, current_user.id, draft)
    return RedirectResponse("/configuration", status_code=303)


@router.post("/configuration/versions/{version_id}/settings")
async def update_setting(version_id: UUID, request: Request, setting_path: str = Form(...),
                         value: str = Form(...), reason: str = Form(...), return_to: str = Form("/configuration"),
                         current_user=Depends(get_current_user), conn=Depends(authenticated_db_conn)):
    require_restricted_configuration_access(request, current_user)
    try:
        parsed_value = json.loads(value)
    except json.JSONDecodeError:
        parsed_value = value
    await _service(conn).patch(version_id, current_user.org_id, current_user.id,
                               DraftPatch(setting_path=setting_path, value=parsed_value, reason=reason))
    safe_return = return_to if return_to in {
        "/configuration", "/configuration/features", "/configuration/versions",
        "/configuration/products",
    } else "/configuration"
    return RedirectResponse(safe_return, status_code=303)


async def _transition(version_id, request, current_user, conn, action):
    require_restricted_configuration_access(request, current_user)
    await getattr(_service(conn), action)(version_id, current_user.org_id, current_user.id)
    return RedirectResponse("/configuration/versions", status_code=303)


@router.post("/configuration/versions/{version_id}/validate")
async def validate_version(version_id: UUID, request: Request, current_user=Depends(get_current_user), conn=Depends(authenticated_db_conn)):
    return await _transition(version_id, request, current_user, conn, "validate")


@router.post("/configuration/versions/{version_id}/approve")
async def approve_version(version_id: UUID, request: Request, current_user=Depends(get_current_user), conn=Depends(authenticated_db_conn)):
    return await _transition(version_id, request, current_user, conn, "approve")


@router.post("/configuration/versions/{version_id}/publish")
async def publish_version(version_id: UUID, request: Request, current_user=Depends(get_current_user), conn=Depends(authenticated_db_conn)):
    return await _transition(version_id, request, current_user, conn, "publish")


@router.get("/api/v1/config/mobile")
async def mobile_configuration(current_user=Depends(get_current_user), conn=Depends(authenticated_db_conn)):
    payload, row = await _service(conn).effective(current_user.org_id)
    return {"schema_version": 1, "config_version": row["version_number"] if row else 0, **payload}


@router.get("/api/v1/config/products")
async def product_configuration(current_user=Depends(get_current_user), conn=Depends(authenticated_db_conn)):
    if settings.CONFIGURABLE_PRODUCTS_ENABLED:
        from app.domains.products.repository import ProductRepository
        rows = await ProductRepository(conn).effective(current_user.org_id)
    else:
        rows = await conn.fetch("SELECT * FROM loan_products WHERE active=TRUE ORDER BY name")
    return {"schema_version": 1, "products": [dict(row) for row in rows]}


@router.get("/api/v1/config/workflow")
async def workflow_configuration(current_user=Depends(get_current_user), conn=Depends(authenticated_db_conn)):
    payload, row = await _service(conn).effective(current_user.org_id)
    return {"schema_version": 1, "config_version": row["version_number"] if row else 0,
            "workflow": payload.get("workflow", {}), "features": payload.get("features", FEATURE_DEFAULTS)}
