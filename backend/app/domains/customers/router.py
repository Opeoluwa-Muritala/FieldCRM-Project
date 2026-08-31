from __future__ import annotations

from datetime import date
from uuid import UUID

from fastapi import APIRouter, Depends, Form, HTTPException, Query, Request, status
from fastapi.responses import RedirectResponse
from pydantic import ValidationError

from app.config import settings
from app.core.dependencies import RoleChecker, authenticated_db_conn, get_current_user
from app.core.loan_authorization import canonical_role
from app.core.field_encryption import mask_sensitive
from app.core.template_utils import build_template_context
from app.core.templates import create_templates
from app.domains.customers.repository import CustomerRepository
from app.domains.customers.schemas import CustomerCreate, CustomerInput
from app.domains.customers.service import CustomerService, DuplicateOverrideRequired, can_view_customer

router = APIRouter(tags=["Customers"])


def _adult_dob_max() -> str:
    today = date.today()
    try:
        return today.replace(year=today.year - 18).isoformat()
    except ValueError:  # 29 February
        return today.replace(year=today.year - 18, day=28).isoformat()
templates = create_templates(str(settings.ROOT_DIR / "frontend" / "templates") if hasattr(settings, "ROOT_DIR") else str(__import__('pathlib').Path(__file__).resolve().parents[4] / "frontend" / "templates"))


def _enabled():
    if not settings.CUSTOMER_IDENTITY_ENABLED:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Not found")


def _service(conn):
    return CustomerService(CustomerRepository(conn))


@router.post("/api/v1/customers/duplicates")
async def check_duplicates(payload: CustomerInput, conn=Depends(authenticated_db_conn), current_user=Depends(RoleChecker(["Account Officer"]))):
    _enabled()
    matches = await _service(conn).duplicates(current_user.org_id, payload)
    return {"probable_duplicates": matches, "override_required": bool(matches)}


@router.post("/api/v1/customers", status_code=status.HTTP_201_CREATED)
async def create_customer_api(payload: CustomerCreate, conn=Depends(authenticated_db_conn), current_user=Depends(RoleChecker(["Account Officer"]))):
    _enabled()
    try:
        customer = await _service(conn).create(
            org_id=current_user.org_id, actor_id=current_user.id,
            branch_id=current_user.branch_id, payload=payload, source="manual_web",
        )
    except DuplicateOverrideRequired as exc:
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail={"code": "duplicate_override_required", "probable_duplicates": [match.model_dump() for match in exc.matches]},
        ) from exc
    return {"customer": customer}


@router.get("/api/v1/customers/search")
async def search_customers(q: str = Query(..., min_length=2, max_length=100), limit: int = Query(20, ge=1, le=50), conn=Depends(authenticated_db_conn), current_user=Depends(get_current_user)):
    _enabled()
    if canonical_role(current_user.role) == "system_admin":
        raise HTTPException(status_code=403, detail="System Admin does not have access to customer records")
    items = await _service(conn).search(
        org_id=current_user.org_id, query=q, role=canonical_role(current_user.role),
        user_id=current_user.id, branch_id=current_user.branch_id, limit=limit,
    )
    return {"items": items, "query": q}


@router.get("/api/v1/customers/{customer_id}/application-profile")
async def customer_application_profile(customer_id: UUID, conn=Depends(authenticated_db_conn), current_user=Depends(get_current_user)):
    _enabled()
    raw = await CustomerRepository(conn).get(customer_id, current_user.org_id)
    if not raw:
        raise HTTPException(status_code=404, detail="Customer not found")
    if not can_view_customer(current_user, raw):
        raise HTTPException(status_code=403, detail="Customer access denied")
    profile = await _service(conn).get_profile(customer_id, current_user.org_id)
    return {"borrower": {"id": str(customer_id), "legal_name": profile["legal_name"]}, "personal_profile": {
        "applicant_name": profile["legal_name"], "phone": profile.get("phone"), "bvn": profile.get("bvn"),
        "nin": profile.get("nin"), "dob": profile.get("date_of_birth"),
        "home_address": profile.get("residential_address"), "business_name": profile.get("business_name"),
        "customer_reference": profile.get("customer_number"),
        "account_number": profile["accounts"][0]["account_number"] if profile.get("accounts") else None,
    }}


@router.get("/customers/new")
async def new_customer_page(request: Request, current_user=Depends(RoleChecker(["Account Officer"]))):
    _enabled()
    return templates.TemplateResponse(request, "customers/new.html", build_template_context(request, current_user, values={}, duplicates=[], error=None, adult_dob_max=_adult_dob_max(), active_page="customers"))


@router.post("/customers/new")
async def create_customer_web(
    request: Request, legal_name: str = Form(...), date_of_birth: str = Form(""), phone: str = Form(""),
    email: str = Form(""), bvn: str = Form(""), nin: str = Form(""), bank_account: str = Form(""),
    bank_name: str = Form(""), residential_address: str = Form(""), business_name: str = Form(""),
    external_customer_id: str = Form(""), cbs_provider: str = Form(""), duplicate_override_reason: str = Form(""),
    conn=Depends(authenticated_db_conn), current_user=Depends(RoleChecker(["Account Officer"])),
):
    _enabled()
    values = dict(locals())
    values.pop("request", None); values.pop("conn", None); values.pop("current_user", None)
    try:
        payload = CustomerCreate(
            legal_name=legal_name, date_of_birth=date.fromisoformat(date_of_birth) if date_of_birth else None,
            phone=phone or None, email=email or None, bvn=bvn or None, nin=nin or None,
            bank_account=bank_account or None, bank_name=bank_name or None,
            residential_address=residential_address or None, business_name=business_name or None,
            external_customer_id=external_customer_id or None, cbs_provider=cbs_provider or None,
            duplicate_override_reason=duplicate_override_reason or None,
        )
        customer = await _service(conn).create(
            org_id=current_user.org_id, actor_id=current_user.id, branch_id=current_user.branch_id,
            payload=payload, source="manual_web",
        )
    except DuplicateOverrideRequired as exc:
        ctx = build_template_context(request, current_user, values=values, duplicates=exc.matches, error="Probable duplicate found. Type a specific override reason to proceed.", adult_dob_max=_adult_dob_max(), active_page="customers")
        return templates.TemplateResponse(request, "customers/new.html", ctx, status_code=409)
    except (ValidationError, ValueError) as exc:
        ctx = build_template_context(request, current_user, values=values, duplicates=[], error=str(exc), adult_dob_max=_adult_dob_max(), active_page="customers")
        return templates.TemplateResponse(request, "customers/new.html", ctx, status_code=422)
    return RedirectResponse(url=f"/customers/{customer['id']}", status_code=status.HTTP_303_SEE_OTHER)


@router.get("/customers/{customer_id}")
async def customer_360(request: Request, customer_id: UUID, conn=Depends(authenticated_db_conn), current_user=Depends(get_current_user)):
    _enabled()
    raw = await CustomerRepository(conn).get(customer_id, current_user.org_id)
    if not raw:
        raise HTTPException(status_code=404, detail="Customer not found")
    if not can_view_customer(current_user, raw):
        raise HTTPException(status_code=403, detail="Customer access denied")
    customer = await _service(conn).get_profile(customer_id, current_user.org_id)
    if canonical_role(current_user.role) not in {"credit_analyst", "auditor"}:
        customer["bvn"] = mask_sensitive(customer.get("bvn")) if customer.get("bvn") else None
        customer["nin"] = mask_sensitive(customer.get("nin")) if customer.get("nin") else None
        for account in customer.get("accounts", []):
            account["account_number"] = mask_sensitive(account.get("account_number"))
    dossier = await CustomerRepository(conn).dossier(customer_id, current_user.org_id)
    return templates.TemplateResponse(request, "customers/360.html", build_template_context(request, current_user, customer=customer, **dossier, active_page="customers"))
