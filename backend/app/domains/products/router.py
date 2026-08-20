import re
from pathlib import Path
from uuid import UUID

from fastapi import APIRouter, Depends, Form, HTTPException, Request
from fastapi.responses import RedirectResponse
from pydantic import ValidationError

from app.config import settings
from app.core.dependencies import authenticated_db_conn, get_current_user
from app.core.templates import create_templates
from app.domains.configuration.access import require_restricted_configuration_access
from app.domains.products.repository import ProductRepository
from app.domains.products.schemas import ProductDefinition
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
    return templates.TemplateResponse(request, "configuration/products.html", {"current_user": current_user, "versions": versions, "products": products})


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
    stages = [stage.strip().lower() for stage in workflow_stages.split(",") if stage.strip()]
    if not stages or any(not re.fullmatch(r"[a-z][a-z0-9_]{1,79}", stage) for stage in stages):
        raise HTTPException(status_code=422, detail="Workflow stages must use lowercase stage keys.")
    try:
        definition = ProductDefinition.model_validate({
            "code": code.strip().lower(), "name": name.strip(), "description": description.strip(),
            "family": family.strip(), "customer_segment": customer_segment.strip(),
            "min_amount": min_amount, "max_amount": max_amount,
            "min_tenor_months": min_tenor_months, "max_tenor_months": max_tenor_months,
            "repayment_frequency": repayment_frequency, "workflow_stages": stages,
            "guarantor_count": guarantor_count, "collateral_required": collateral_required,
            "cbs_enabled": cbs_enabled, "sla_hours": sla_hours,
        })
    except ValidationError as exc:
        raise HTTPException(status_code=422, detail="Check the product fields and try again.") from exc
    await ProductService(ProductRepository(conn)).create(current_user.org_id, version_id, definition)
    return RedirectResponse("/configuration/products", status_code=303)


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
