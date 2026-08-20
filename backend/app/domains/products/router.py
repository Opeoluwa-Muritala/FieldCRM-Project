import json
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
async def create_product_web(request: Request, version_id: UUID = Form(...), definition_json: str = Form(...),
                             current_user=Depends(get_current_user), conn=Depends(authenticated_db_conn)):
    _enabled(); require_restricted_configuration_access(request, current_user)
    try:
        definition = ProductDefinition.model_validate(json.loads(definition_json))
    except (json.JSONDecodeError, ValidationError) as exc:
        raise HTTPException(status_code=422, detail="Product definition is invalid") from exc
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
