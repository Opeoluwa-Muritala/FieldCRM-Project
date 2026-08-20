import re
from pathlib import Path
from uuid import UUID

from asyncpg import UniqueViolationError
from fastapi import APIRouter, Depends, Form, HTTPException, Request
from fastapi.responses import RedirectResponse
from pydantic import ValidationError

from app.config import settings
from app.core.dependencies import authenticated_db_conn, get_current_user
from app.core.templates import create_templates
from app.domains.configuration.access import require_restricted_configuration_access
from app.domains.products.repository import ProductRepository
from app.domains.products.schemas import DocumentDefinition, FormFieldDefinition, ProductDefinition
from app.domains.products.service import ProductService

router = APIRouter()
templates = create_templates(str(Path(__file__).resolve().parents[4] / "frontend" / "templates"))


def _enabled():
    if not settings.CONFIGURABLE_PRODUCTS_ENABLED:
        raise HTTPException(status_code=404, detail="Not found")


@router.get("/configuration/products")
async def product_editor(request: Request, current_user=Depends(get_current_user), conn=Depends(authenticated_db_conn)):
    _enabled(); require_restricted_configuration_access(request, current_user)
    versions = await conn.fetch("SELECT id,version_number,status FROM configuration_versions WHERE org_id=$1 ORDER BY version_number DESC", current_user.org_id)
    products = await ProductRepository(conn).effective(current_user.org_id)
    drafts = await ProductRepository(conn).drafts(current_user.org_id)
    return templates.TemplateResponse(request, "configuration/products.html", {
        "current_user": current_user, "versions": versions, "products": products, "draft_products": drafts,
    }, headers={"Cache-Control": "no-store"})


def _product_from_fields(values: dict) -> ProductDefinition:
    stages = [stage.strip().lower() for stage in values.pop("workflow_stages").split(",") if stage.strip()]
    if not stages or any(not re.fullmatch(r"[a-z][a-z0-9_]{1,79}", stage) for stage in stages):
        raise HTTPException(status_code=422, detail="Workflow stages must use lowercase stage keys.")
    values["workflow_stages"] = stages
    try:
        return ProductDefinition.model_validate(values)
    except ValidationError as exc:
        raise HTTPException(status_code=422, detail="Check the product fields and try again.") from exc


@router.post("/configuration/products")
async def create_product_web(
    request: Request,
    version_id: UUID = Form(...),
    code: str = Form(..., min_length=2, max_length=50),
    name: str = Form(..., min_length=2, max_length=120),
    description: str = Form("", max_length=1000),
    family: str = Form(..., min_length=2, max_length=80),
    customer_segment: str = Form(..., min_length=2, max_length=80),
    min_amount: float = Form(..., ge=0),
    max_amount: float = Form(..., gt=0),
    min_tenor_months: int = Form(..., ge=1, le=360),
    max_tenor_months: int = Form(..., ge=1, le=360),
    repayment_frequency: str = Form(...),
    workflow_stages: str = Form(..., min_length=2, max_length=500),
    guarantor_count: int = Form(0, ge=0, le=20),
    collateral_required: bool = Form(False),
    cbs_enabled: bool = Form(False),
    sla_hours: int = Form(48, ge=1, le=8760),
    current_user=Depends(get_current_user),
    conn=Depends(authenticated_db_conn),
):
    _enabled(); require_restricted_configuration_access(request, current_user)
    definition = _product_from_fields({
            "code": code.strip().lower(), "name": name.strip(), "description": description.strip(),
            "family": family.strip(), "customer_segment": customer_segment.strip(),
            "min_amount": min_amount, "max_amount": max_amount,
            "min_tenor_months": min_tenor_months, "max_tenor_months": max_tenor_months,
            "repayment_frequency": repayment_frequency, "workflow_stages": workflow_stages,
            "guarantor_count": guarantor_count, "collateral_required": collateral_required,
            "cbs_enabled": cbs_enabled, "sla_hours": sla_hours,
        })
    await ProductService(ProductRepository(conn)).create(current_user.org_id, version_id, definition)
    return RedirectResponse("/configuration/products", status_code=303)


@router.get("/configuration/products/{code}/edit")
async def edit_product_page(code: str, request: Request, current_user=Depends(get_current_user), conn=Depends(authenticated_db_conn)):
    _enabled(); require_restricted_configuration_access(request, current_user)
    product = await ProductRepository(conn).draft_product(code, current_user.org_id)
    if not product:
        raise HTTPException(status_code=404, detail="Only products in a working draft can be edited.")
    return templates.TemplateResponse(request, "configuration/product_edit.html", {
        "current_user": current_user, "product": dict(product),
    }, headers={"Cache-Control": "no-store"})


@router.post("/configuration/products/{code}/edit")
async def edit_product(
    code: str, request: Request, name: str = Form(..., min_length=2, max_length=120),
    description: str = Form("", max_length=1000), family: str = Form(..., min_length=2, max_length=80),
    customer_segment: str = Form(..., min_length=2, max_length=80), min_amount: float = Form(..., ge=0),
    max_amount: float = Form(..., gt=0), min_tenor_months: int = Form(..., ge=1, le=360),
    max_tenor_months: int = Form(..., ge=1, le=360), repayment_frequency: str = Form(...),
    workflow_stages: str = Form(..., min_length=2, max_length=500), guarantor_count: int = Form(0, ge=0, le=20),
    collateral_required: bool = Form(False), cbs_enabled: bool = Form(False),
    sla_hours: int = Form(48, ge=1, le=8760), current_user=Depends(get_current_user),
    conn=Depends(authenticated_db_conn),
):
    _enabled(); require_restricted_configuration_access(request, current_user)
    product = await ProductRepository(conn).draft_product(code, current_user.org_id)
    if not product:
        raise HTTPException(status_code=404, detail="Only products in a working draft can be edited.")
    definition = _product_from_fields({
        "code": code.split("_", 1)[-1], "name": name.strip(), "description": description.strip(),
        "family": family.strip(), "customer_segment": customer_segment.strip(), "min_amount": min_amount,
        "max_amount": max_amount, "min_tenor_months": min_tenor_months,
        "max_tenor_months": max_tenor_months, "repayment_frequency": repayment_frequency,
        "workflow_stages": workflow_stages, "guarantor_count": guarantor_count,
        "collateral_required": collateral_required, "cbs_enabled": cbs_enabled, "sla_hours": sla_hours,
    })
    updated = await ProductRepository(conn).update_draft(code, current_user.org_id, definition)
    if not updated:
        raise HTTPException(status_code=409, detail="The product is no longer editable.")
    return RedirectResponse("/configuration/products", status_code=303)


_VALIDATION_PATTERNS = {
    "none": None,
    "email": r"[^\s@]+@[^\s@]+\.[^\s@]+",
    "phone": r"\+?[0-9]{10,15}",
    "bvn": r"[0-9]{11}",
    "nin": r"[0-9]{11}",
    "account_number": r"[0-9]{10}",
}


def _form_field(values: dict) -> FormFieldDefinition:
    validation_key = values.pop("validation_key")
    options_text = values.pop("options_text")
    condition_field = values.pop("condition_field")
    condition_equals = values.pop("condition_equals")
    options = [item.strip() for item in options_text.split(",") if item.strip()]
    values["options"] = options
    pattern = _VALIDATION_PATTERNS.get(validation_key)
    if validation_key not in _VALIDATION_PATTERNS:
        raise HTTPException(status_code=422, detail="Select a supported validation rule.")
    values["validation_rules"] = {"pattern": pattern} if pattern else {}
    values["visibility_condition"] = (
        {"field": condition_field.strip(), "equals": condition_equals.strip()}
        if condition_field.strip() and condition_equals.strip() else {}
    )
    try:
        return FormFieldDefinition.model_validate(values)
    except ValidationError as exc:
        raise HTTPException(status_code=422, detail="Check the form field definition and try again.") from exc


@router.get("/configuration/forms")
async def form_fields_page(request: Request, current_user=Depends(get_current_user), conn=Depends(authenticated_db_conn)):
    _enabled(); require_restricted_configuration_access(request, current_user)
    repo = ProductRepository(conn)
    return templates.TemplateResponse(request, "configuration/forms.html", {
        "current_user": current_user, "draft_products": await repo.drafts(current_user.org_id),
        "fields": await repo.draft_fields(current_user.org_id), "validation_rules": _VALIDATION_PATTERNS,
    }, headers={"Cache-Control": "no-store"})


@router.post("/configuration/forms")
async def add_form_field(
    request: Request, product_code: str = Form(...), section_key: str = Form(...),
    field_key: str = Form(...), label: str = Form(...), field_type: str = Form(...), requirement: str = Form("optional"),
    options_text: str = Form(""), validation_key: str = Form("none"), condition_field: str = Form(""),
    condition_equals: str = Form(""), help_text: str = Form("", max_length=300), display_order: int = Form(0, ge=0, le=10000),
    current_user=Depends(get_current_user), conn=Depends(authenticated_db_conn),
):
    _enabled(); require_restricted_configuration_access(request, current_user)
    field = _form_field({
        "section_key": section_key, "field_key": field_key, "label": label,
        "field_type": field_type, "requirement": requirement, "options_text": options_text,
        "validation_key": validation_key, "condition_field": condition_field,
        "condition_equals": condition_equals, "help_text": help_text or None,
        "display_order": display_order,
    })
    try:
        row = await ProductRepository(conn).add_field(current_user.org_id, product_code, field)
    except UniqueViolationError as exc:
        raise HTTPException(status_code=409, detail="That field key already exists for this product draft.") from exc
    if not row:
        raise HTTPException(status_code=409, detail="Select a product from an active configuration draft.")
    return RedirectResponse("/configuration/forms", status_code=303)


@router.post("/configuration/forms/{field_id}")
async def edit_form_field(
    field_id: UUID, request: Request, section_key: str = Form(...), field_key: str = Form(...),
    label: str = Form(...), field_type: str = Form(...), requirement: str = Form("optional"),
    options_text: str = Form(""), validation_key: str = Form("none"), condition_field: str = Form(""),
    condition_equals: str = Form(""), help_text: str = Form("", max_length=300),
    display_order: int = Form(0, ge=0, le=10000), current_user=Depends(get_current_user),
    conn=Depends(authenticated_db_conn),
):
    _enabled(); require_restricted_configuration_access(request, current_user)
    field = _form_field({
        "section_key": section_key, "field_key": field_key, "label": label,
        "field_type": field_type, "requirement": requirement, "options_text": options_text,
        "validation_key": validation_key, "condition_field": condition_field,
        "condition_equals": condition_equals, "help_text": help_text or None,
        "display_order": display_order,
    })
    try:
        row = await ProductRepository(conn).update_field(field_id, current_user.org_id, field)
    except UniqueViolationError as exc:
        raise HTTPException(status_code=409, detail="That field key already exists for this product draft.") from exc
    if not row:
        raise HTTPException(status_code=404, detail="The draft form field was not found.")
    return RedirectResponse("/configuration/forms", status_code=303)


@router.get("/configuration/documents")
async def documents_page(request: Request, current_user=Depends(get_current_user), conn=Depends(authenticated_db_conn)):
    _enabled(); require_restricted_configuration_access(request, current_user)
    repo = ProductRepository(conn)
    return templates.TemplateResponse(request, "configuration/documents.html", {
        "current_user": current_user, "draft_products": await repo.drafts(current_user.org_id),
        "documents": await repo.draft_documents(current_user.org_id),
    }, headers={"Cache-Control": "no-store"})


@router.post("/configuration/documents")
async def add_document_requirement(
    request: Request, product_code: str = Form(...), doc_type: str = Form(...),
    display_name: str = Form(...), mandatory: bool = Form(False), current_user=Depends(get_current_user),
    conn=Depends(authenticated_db_conn),
):
    _enabled(); require_restricted_configuration_access(request, current_user)
    try:
        document = DocumentDefinition(doc_type=doc_type.strip().lower(), display_name=display_name.strip(), mandatory=mandatory)
        row = await ProductRepository(conn).add_document(current_user.org_id, product_code, document)
    except ValidationError as exc:
        raise HTTPException(status_code=422, detail="Check the document requirement and try again.") from exc
    except UniqueViolationError as exc:
        raise HTTPException(status_code=409, detail="That document type already exists for this product draft.") from exc
    if not row:
        raise HTTPException(status_code=409, detail="Select a product from an active configuration draft.")
    return RedirectResponse("/configuration/documents", status_code=303)


@router.post("/configuration/documents/{document_id}")
async def edit_document_requirement(
    document_id: UUID, request: Request, doc_type: str = Form(...), display_name: str = Form(...),
    mandatory: bool = Form(False), current_user=Depends(get_current_user), conn=Depends(authenticated_db_conn),
):
    _enabled(); require_restricted_configuration_access(request, current_user)
    try:
        document = DocumentDefinition(doc_type=doc_type.strip().lower(), display_name=display_name.strip(), mandatory=mandatory)
        row = await ProductRepository(conn).update_document(document_id, current_user.org_id, document)
    except ValidationError as exc:
        raise HTTPException(status_code=422, detail="Check the document requirement and try again.") from exc
    except UniqueViolationError as exc:
        raise HTTPException(status_code=409, detail="That document type already exists for this product draft.") from exc
    if not row:
        raise HTTPException(status_code=404, detail="The draft document requirement was not found.")
    return RedirectResponse("/configuration/documents", status_code=303)


@router.post("/api/v1/config/admin/products", status_code=201)
async def create_product_api(version_id: UUID, definition: ProductDefinition, request: Request,
                             current_user=Depends(get_current_user), conn=Depends(authenticated_db_conn)):
    _enabled(); require_restricted_configuration_access(request, current_user)
    row = await ProductService(ProductRepository(conn)).create(current_user.org_id, version_id, definition)
    return dict(row)


@router.get("/api/v1/config/products/{code}")
async def product_definition(code: str, current_user=Depends(get_current_user), conn=Depends(authenticated_db_conn)):
    _enabled()
    definition = await ProductRepository(conn).definition(code, current_user.org_id)
    if not definition: raise HTTPException(status_code=404, detail="Product not found")
    return {"schema_version": 1, **definition}
