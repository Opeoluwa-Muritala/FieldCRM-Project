import logging
import mimetypes
import re
from datetime import date, datetime
from decimal import Decimal
from pathlib import Path
from typing import Any
from uuid import UUID, uuid4
from fastapi import APIRouter, Depends, File, Form, HTTPException, Request, UploadFile, status, Query
from fastapi.responses import RedirectResponse, FileResponse, StreamingResponse
import httpx

from app.config import settings
from app.core.database import get_connection
from app.core.dependencies import authenticated_db_conn as db_conn
from app.core.dependencies import get_current_user, RoleChecker
from app.core.audit import AuditService
from app.core.template_utils import build_template_context
from app.core.templates import create_templates
from app.domains.loans.repository import LoanRepository
from app.domains.feasibility.calculator import calculate_feasibility, calculate_cam_feasibility
from app.domains.feasibility.cam import CAMValidationError, parse_cam_form
from app.domains.feasibility.repository import FeasibilityRepository
from app.domains.documents.service import prepare_upload_file
from app.services.loan_servicing_service import generate_schedule
from app.services.cloud_storage_service import signed_preview_url

log = logging.getLogger(__name__)
router = APIRouter()

# Resolve templates folder relatively
base_dir = Path(__file__).resolve().parent
templates_dir = (base_dir.parents[3] / "frontend" / "templates").resolve()
templates = create_templates(str(templates_dir))
templates.env.globals.update(
    brand_logo_black="https://res.cloudinary.com/ddezxlqjr/image/upload/v1784551475/MMFB_Logo_Black_lnma0l.png",
    brand_logo_white="https://res.cloudinary.com/ddezxlqjr/image/upload/v1784551475/MMFB_logo_White_gzthxm.png",
)

def _verify_loan_scope(app, current_user):
    role = current_user.role.lower().replace(" ", "_")
    created_by = app.get("created_by") if isinstance(app, dict) else getattr(app, "created_by", None)
    if role in ("account_officer", "loan_officer") and created_by:
        if str(created_by) != str(current_user.id):
            raise HTTPException(
                status_code=status.HTTP_403_FORBIDDEN,
                detail="You do not have permission to access this application."
            )

async def get_loan_interest_rate(conn, app):
    if getattr(app, "interest_rate_snapshot", None):
        return float(app.interest_rate_snapshot)
    row = await conn.fetchrow(
        "SELECT rate FROM interest_rate_presets WHERE loan_type = $1 ORDER BY set_at DESC LIMIT 1;",
        app.loan_type
    )
    if row and row["rate"]:
        return float(row["rate"])
    return 24.0

# =============================================================================
# REPAYMENT FEASIBILITY SUMMARY SCREEN
# =============================================================================

@router.get("/applications/{application_id}/repayment-feasibility")
async def render_repayment_feasibility(
    request: Request,
    application_id: str,
    conn = Depends(db_conn),
    current_user = Depends(RoleChecker(["Relationship Officer", "Branch Manager", "Branch Supervisor", "Credit Analyst", "CRM", "Head CRM", "Auditor", "ED", "MD", "Legal"])),
):
    try:
        app_uuid = UUID(application_id)
    except ValueError:
        raise HTTPException(status_code=404, detail="Loan Application not found")

    repo = LoanRepository(conn)
    app = await repo.get_by_id(app_uuid, current_user.org_id)
    if not app:
        raise HTTPException(status_code=404, detail="Loan Application not found")

    _verify_loan_scope(app, current_user)

    collateral_items = await conn.fetch(
        """SELECT ci.*, p.display_name, p.policy_note, p.max_valuation_age_days
           FROM collateral_items ci
           LEFT JOIN collateral_valuation_policies p ON p.asset_class = ci.collateral_type
           WHERE ci.application_id = $1 ORDER BY ci.created_at, ci.id""",
        app_uuid,
    )
    total_market = sum((Decimal(str(row["loan_based_price"] or 0)) for row in collateral_items), Decimal("0"))
    total_pledged = sum(
        (
            Decimal(str(row["cam_forced_sale_value"] if row["cam_forced_sale_value"] is not None else row["force_sale_value"] or 0))
            for row in collateral_items
        ),
        Decimal("0"),
    )
    cashflows, financial_profile, obligations = await FeasibilityRepository(conn).get_inputs(app_uuid)
    total_pledged = Decimal(str(total_pledged or 0))
    
    # Fetch collateral documents and group them
    collateral_docs = await conn.fetch(
        """SELECT cd.* FROM collateral_documents cd
           JOIN collateral_items ci ON ci.id = cd.collateral_item_id
           WHERE ci.application_id = $1""",
        app_uuid
    )
    docs_by_item = {}
    for doc in collateral_docs:
        item_id = doc["collateral_item_id"]
        if item_id not in docs_by_item:
            docs_by_item[item_id] = []
        docs_by_item[item_id].append(dict(doc))

    collateral_list = []
    for row in collateral_items:
        d = dict(row)
        d["documents"] = docs_by_item.get(row["id"], [])
        collateral_list.append(d)

    # Fetch guarantors
    guarantors_rows = await conn.fetch(
        "SELECT * FROM guarantors WHERE loan_id = $1 ORDER BY slot",
        app_uuid
    )
    guarantors_list = [dict(r) for r in guarantors_rows]

    loan_amount = Decimal(str(app.amount or 0))
    coverage_ratio = Decimal("0")
    market_coverage_ratio = Decimal("0")
    if loan_amount > 0:
        coverage_ratio = (total_pledged / loan_amount)
        market_coverage_ratio = (total_market / loan_amount)

    product = await conn.fetchrow(
        """SELECT repayment_frequency, interest_calculation_type
           FROM loan_products WHERE code = $1 AND active = TRUE""",
        app.loan_type,
    )
    repayment_frequency = (product["repayment_frequency"] if product else None) or "monthly"
    interest_method = (product["interest_calculation_type"] if product else None) or "flat"
    schedule_method = "reducing_balance" if interest_method in {"reducing", "reducing_balance"} else "flat_rate"
    rate = await get_loan_interest_rate(conn, app)
    tenor = int(app.tenor_months or 12)

    intake_record = await conn.fetchrow(
        """SELECT data_json FROM stage_data
           WHERE loan_id = $1 AND stage = 'intake'
           ORDER BY saved_at DESC LIMIT 1""",
        app_uuid
    )
    intake_data = dict(intake_record["data_json"]) if (intake_record and intake_record["data_json"]) else {}

    if not financial_profile:
        financial_profile = {}
    else:
        financial_profile = dict(financial_profile)

    # Fallback to intake data if profile is not populated yet
    if not financial_profile.get("monthly_turnover"):
        financial_profile["monthly_turnover"] = intake_data.get("monthly_turnover") or intake_data.get("monthly_sales") or 0.0
    if not financial_profile.get("monthly_expenses"):
        financial_profile["monthly_expenses"] = intake_data.get("household_expenses") or intake_data.get("monthly_expenses") or 0.0
    if not financial_profile.get("shop_allocation"):
        financial_profile["shop_allocation"] = intake_data.get("shop_allocation") or ""
    if not financial_profile.get("shop_allowance"):
        financial_profile["shop_allowance"] = intake_data.get("shop_allowance") or 0.0
    if not financial_profile.get("recommended_amount"):
        financial_profile["recommended_amount"] = app.amount or 0.0
    if not financial_profile.get("proposed_tenor"):
        financial_profile["proposed_tenor"] = app.tenor_months or 12
    if not financial_profile.get("interest_rate"):
        financial_profile["interest_rate"] = rate or 30.0
    installment_amount = Decimal("0")
    if loan_amount > 0 and tenor > 0:
        try:
            rows = generate_schedule(
                principal=float(loan_amount),
                annual_rate=rate,
                tenor_months=tenor,
                frequency=repayment_frequency,
                method=schedule_method,
                disbursement_date=date.today()
            )
            if rows:
                installment_amount = max(Decimal(str(row["total_due"])) for row in rows)
        except Exception as e:
            log.error(f"Error computing proposed installment: {e}")

    # Standard cashflow feasibility
    feasibility = calculate_feasibility(
        cashflows,
        financial_profile,
        obligations,
        proposed_payment=installment_amount,
        proposed_payment_frequency=repayment_frequency,
    )

    # CAM-specific feasibility
    cam_feasibility = calculate_cam_feasibility(
        financial_profile,
        obligations,
        collateral_items,
    )

    internal_obligations = [row for row in obligations if row.get("source_type") == "internal"]
    external_obligations = [row for row in obligations if row.get("source_type") == "external"]
    bank_turnovers = [row for row in cashflows if row.get("source_type") == "bank_turnover"]
    application_header = await conn.fetchrow(
        """SELECT b.name AS branch_name, u.full_name AS officer_name
           FROM loan_applications la
           LEFT JOIN branches b ON b.id = la.branch_id AND b.org_id = la.org_id
           LEFT JOIN users u ON u.id = la.created_by AND u.org_id = la.org_id
           WHERE la.id = $1 AND la.org_id = $2""",
        app_uuid,
        current_user.org_id,
    )
    recommendations = await conn.fetch(
        """SELECT lr.role_at_submission, lr.recommended_amount, lr.notes, lr.created_at,
                  u.full_name AS submitted_by_name
           FROM loan_recommendations lr
           JOIN users u ON u.id = lr.submitted_by
           WHERE lr.application_id = $1
           ORDER BY lr.created_at, lr.id""",
        app_uuid,
    )
    recommendation_rows = [dict(row) for row in recommendations]
    normalized_recommendations: dict[str, list[dict]] = {}
    role_aliases = {
        "loan_officer": "relationship_officer",
        "account_officer": "relationship_officer",
        "team_lead": "branch_manager",
        "supervisor": "branch_supervisor",
    }
    for recommendation in recommendation_rows:
        role_key = str(recommendation.get("role_at_submission") or "").lower().replace(" ", "_")
        role_key = role_aliases.get(role_key, role_key)
        normalized_recommendations.setdefault(role_key, []).append(recommendation)
    recommendation_chain = []
    for role_key, label, slots in (
        ("relationship_officer", "Relationship Officer", 1),
        ("branch_manager", "Branch Manager", 1),
        ("branch_supervisor", "Branch Supervisor / Head SME", 1),
        ("credit_analyst", "Credit Analyst", 1),
        ("head_crm", "Head CRM", 1),
        ("mcc", "MCC", 3),
    ):
        rows = normalized_recommendations.get(role_key, [])
        for slot in range(slots):
            recommendation = rows[slot] if slot < len(rows) else {}
            if role_key == "credit_analyst" and not recommendation:
                recommendation = {
                    "submitted_by_name": financial_profile.get("analyst_name"),
                    "recommended_amount": financial_profile.get("recommended_amount"),
                    "notes": financial_profile.get("analyst_recommendation"),
                }
            recommendation_defaults = {
                "submitted_by_name": None,
                "recommended_amount": None,
                "notes": None,
            }
            recommendation_defaults.update(recommendation)
            recommendation_chain.append({
                "role": f"{label} {slot + 1}" if slots > 1 else label,
                **recommendation_defaults,
            })

    # Build response context
    ctx = build_template_context(
        request, current_user,
        app=app, app_id=application_id,
        cashflows=cashflows,
        financial_profile=financial_profile or {},
        obligations=obligations,
        feasibility=feasibility,
        cam_feasibility=cam_feasibility,
        collateral_items=collateral_list,
        guarantors=guarantors_list,
        internal_obligations=internal_obligations,
        external_obligations=external_obligations,
        bank_turnovers=bank_turnovers,
        application_header=dict(application_header) if application_header else {},
        intake_data=intake_data,
        recommendations=recommendation_rows,
        recommendation_chain=recommendation_chain,
        can_edit_cam=(current_user.role.lower().replace(" ", "_") == "credit_analyst" and app.stage == "credit_analyst_review"),
        total_market_value=total_market,
        total_pledged_value=total_pledged,
        market_coverage_ratio=market_coverage_ratio,
        coverage_ratio=coverage_ratio,
        proposed_installment=installment_amount,
        proposed_payment_frequency=repayment_frequency,
        proposed_interest_rate=rate,
        active_tab="queue", active_page="queue"
    )
    return templates.TemplateResponse(request, "shared/repayment_feasibility.html", ctx)


@router.post("/applications/{application_id}/repayment-feasibility")
async def save_repayment_feasibility(
    request: Request,
    application_id: str,
    conn = Depends(db_conn),
    current_user = Depends(RoleChecker(["Credit Analyst"])),
):
    try:
        app_uuid = UUID(application_id)
    except ValueError:
        raise HTTPException(status_code=404, detail="Loan Application not found")

    app = await LoanRepository(conn).get_by_id(app_uuid, current_user.org_id)
    if not app:
        raise HTTPException(status_code=404, detail="Loan Application not found")
    if app.stage != "credit_analyst_review":
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail="Feasibility inputs can only be edited during Credit Analyst Review.",
        )

    form_data = await request.form()
    collateral_rows = await conn.fetch(
        """SELECT id FROM collateral_items
           WHERE application_id = $1
           ORDER BY created_at, id""",
        app_uuid,
    )
    try:
        payload = parse_cam_form(
            form_data,
            [row["id"] for row in collateral_rows],
        )
    except CAMValidationError as exc:
        raise HTTPException(
            status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
            detail=str(exc),
        ) from None

    if payload["profile"]["recommended_amount"] > Decimal(str(app.amount or 0)):
        raise HTTPException(
            status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
            detail="Recommended Amount cannot exceed the requested loan amount.",
        )

    async with conn.transaction():
        await FeasibilityRepository(conn).save_cam_details(
            app_uuid,
            payload,
            current_user.id,
        )
        await AuditService(conn).insert(
            org_id=current_user.org_id,
            entity_type="loan_application",
            entity_id=app_uuid,
            action="feasibility.cam_updated",
            user_id=current_user.id,
            user_role=current_user.role,
            source="manual",
            notes="Credit Analyst updated the CAM feasibility analysis.",
            request_id=request.headers.get("x-request-id"),
        )

    return RedirectResponse(
        url=f"/applications/{application_id}/repayment-feasibility?success=saved",
        status_code=status.HTTP_303_SEE_OTHER,
    )


# =============================================================================
# BUSINESS LOCATIONS ENDPOINTS
# =============================================================================

@router.post("/applications/{application_id}/business-locations")
async def update_business_locations(
    request: Request,
    application_id: str,
    conn = Depends(db_conn),
    current_user = Depends(get_current_user),
):
    try:
        app_uuid = UUID(application_id)
    except ValueError:
        raise HTTPException(status_code=404, detail="Loan Application not found")

    repo = LoanRepository(conn)
    app = await repo.get_by_id(app_uuid, current_user.org_id)
    if not app:
        raise HTTPException(status_code=404, detail="Loan Application not found")

    _verify_loan_scope(app, current_user)

    form_data = await request.form()
    addresses = form_data.getlist("address_line[]")
    cities = form_data.getlist("city[]")
    states = form_data.getlist("state[]")
    functions = form_data.getlist("function[]")

    async with conn.transaction():
        await conn.execute("DELETE FROM business_locations WHERE application_id = $1", app_uuid)
        
        for address, city, state, func in zip(addresses, cities, states, functions):
            if address.strip() and city.strip() and state.strip() and func.strip():
                await conn.execute(
                    """INSERT INTO business_locations (application_id, address_line, city, state, function, created_by)
                       VALUES ($1, $2, $3, $4, $5, $6)""",
                    app_uuid, address.strip(), city.strip(), state.strip(), func.strip(), current_user.id
                )

        audit = AuditService(conn)
        await audit.insert(
            org_id=current_user.org_id,
            entity_type="loan_application",
            entity_id=app_uuid,
            action="business_locations.update",
            user_id=current_user.id,
            user_role=current_user.role,
            notes="Updated business locations in Feasibility screen"
        )

    return RedirectResponse(
        url=f"/applications/{application_id}/repayment-feasibility",
        status_code=status.HTTP_303_SEE_OTHER
    )

# =============================================================================
# COLLATERAL ENDPOINTS
# =============================================================================

@router.post("/applications/{application_id}/collateral")
async def add_collateral_item(
    application_id: str,
    collateral_type: str = Form(...),
    narration: str = Form(...),
    loan_based_price: Decimal = Form(None),
    face_value: Decimal = Form(None),
    conn = Depends(db_conn),
    current_user = Depends(get_current_user),
):
    try:
        app_uuid = UUID(application_id)
    except ValueError:
        raise HTTPException(status_code=404, detail="Loan Application not found")

    repo = LoanRepository(conn)
    app = await repo.get_by_id(app_uuid, current_user.org_id)
    if not app:
        raise HTTPException(status_code=404, detail="Loan Application not found")

    _verify_loan_scope(app, current_user)

    if collateral_type not in ('property', 'equipment', 'inventory', 'cash'):
        raise HTTPException(status_code=400, detail="Invalid collateral type")

    if not narration.strip():
        raise HTTPException(status_code=400, detail="Narration is required")

    async with conn.transaction():
        row = await conn.fetchrow(
            """INSERT INTO collateral_items (application_id, collateral_type, narration, loan_based_price, face_value, created_by)
               VALUES ($1, $2, $3, $4, $5, $6) RETURNING id""",
            app_uuid, collateral_type, narration.strip(), loan_based_price, face_value, current_user.id
        )
        
        audit = AuditService(conn)
        await audit.insert(
            org_id=current_user.org_id,
            entity_type="collateral_item",
            entity_id=row["id"],
            action="collateral.create",
            user_id=current_user.id,
            user_role=current_user.role,
            notes=f"Added collateral item: {collateral_type}"
        )

    return RedirectResponse(
        url=f"/applications/{application_id}/repayment-feasibility",
        status_code=status.HTTP_303_SEE_OTHER
    )

@router.post("/applications/{application_id}/collateral/{collateral_id}/delete")
async def delete_collateral_item(
    application_id: str,
    collateral_id: str,
    conn = Depends(db_conn),
    current_user = Depends(get_current_user),
):
    try:
        app_uuid = UUID(application_id)
        collateral_uuid = UUID(collateral_id)
    except ValueError:
        raise HTTPException(status_code=404, detail="Not found")

    repo = LoanRepository(conn)
    app = await repo.get_by_id(app_uuid, current_user.org_id)
    if not app:
        raise HTTPException(status_code=404, detail="Loan Application not found")

    _verify_loan_scope(app, current_user)

    async with conn.transaction():
        await conn.execute(
            "DELETE FROM collateral_items WHERE id = $1 AND application_id = $2",
            collateral_uuid, app_uuid
        )
        
        audit = AuditService(conn)
        await audit.insert(
            org_id=current_user.org_id,
            entity_type="collateral_item",
            entity_id=collateral_uuid,
            action="collateral.delete",
            user_id=current_user.id,
            user_role=current_user.role,
            notes="Deleted collateral item"
        )

    return RedirectResponse(
        url=f"/applications/{application_id}/repayment-feasibility",
        status_code=status.HTTP_303_SEE_OTHER
    )

@router.post("/applications/{application_id}/collateral/{collateral_id}/documents")
async def upload_collateral_document(
    application_id: str,
    collateral_id: str,
    worth: Decimal = Form(...),
    document_type: str = Form(...),
    file: UploadFile = File(...),
    conn = Depends(db_conn),
    current_user = Depends(RoleChecker(["Credit Analyst"])),
):
    try:
        app_uuid = UUID(application_id)
        collateral_uuid = UUID(collateral_id)
    except ValueError:
        raise HTTPException(status_code=404, detail="Not found")

    repo = LoanRepository(conn)
    app = await repo.get_by_id(app_uuid, current_user.org_id)
    if not app:
        raise HTTPException(status_code=404, detail="Loan Application not found")

    _verify_loan_scope(app, current_user)

    collateral_exists = await conn.fetchval(
        "SELECT EXISTS(SELECT 1 FROM collateral_items WHERE id = $1 AND application_id = $2)",
        collateral_uuid,
        app_uuid,
    )
    if not collateral_exists:
        raise HTTPException(status_code=404, detail="Collateral item not found")
    allowed_document_types = {
        "vehicle_document", "purchase_receipt", "stock_hypothecation",
        "title_document", "other_collateral_evidence",
    }
    if document_type not in allowed_document_types:
        raise HTTPException(status_code=422, detail="Invalid collateral document type")
    if not worth.is_finite() or worth < 0 or worth > Decimal("9999999999999.99"):
        raise HTTPException(status_code=422, detail="Supported worth is invalid")

    # 1. Read file bytes and validate size & mime
    original_name = file.filename or "document"
    mime_type = file.content_type or mimetypes.guess_type(original_name)[0] or ""
    content = await file.read(settings.DOCUMENT_MAX_IMAGE_BYTES + 1)
    
    content, mime_type, error = prepare_upload_file(
        content, mime_type, set(settings.DOCUMENT_ALLOWED_MIME_TYPES), original_name
    )
    if error:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail=error)

    # 2. Upload to Cloudinary or fall back to local disk saving
    safe_doc_type = document_type
    
    from app.services.cloud_storage_service import upload_document as _cloud_upload
    cloud_result = _cloud_upload(
        file_bytes=content,
        mime_type=mime_type,
        org_id=str(current_user.org_id),
        loan_id=str(app_uuid),
        doc_type=safe_doc_type,
        filename_stem=uuid4().hex,
    )

    if cloud_result:
        cloudinary_public_id = cloud_result.public_id
        cloudinary_url = cloud_result.preview_url or f"https://res.cloudinary.com/{settings.CLOUDINARY_CLOUD_NAME}/image/upload/{cloud_result.public_id}"
    else:
        # Local fallback
        upload_root = Path(settings.DOCUMENT_UPLOAD_DIR)
        relative_dir = Path(str(current_user.org_id)) / str(app_uuid)
        target_dir = upload_root / relative_dir
        target_dir.mkdir(parents=True, exist_ok=True)
        extension = Path(original_name).suffix.lower()
        stored_name = f"{safe_doc_type}_{uuid4().hex}{extension}"
        (target_dir / stored_name).write_bytes(content)
        cloudinary_url = "/static/uploads/" + (relative_dir / stored_name).as_posix()
        cloudinary_public_id = "local"

    # 3. Store document link in collateral_documents
    async with conn.transaction():
        row = await conn.fetchrow(
            """INSERT INTO collateral_documents (collateral_item_id, cloudinary_public_id, cloudinary_url, document_type, worth, uploaded_by)
               VALUES ($1, $2, $3, $4, $5, $6) RETURNING id""",
            collateral_uuid, cloudinary_public_id, cloudinary_url, safe_doc_type, worth, current_user.id
        )

        audit = AuditService(conn)
        await audit.insert(
            org_id=current_user.org_id,
            entity_type="collateral_document",
            entity_id=row["id"],
            action="collateral_document.uploaded",
            user_id=current_user.id,
            user_role=current_user.role,
            notes=f"Uploaded collateral document: {safe_doc_type}"
        )

    return RedirectResponse(
        url=f"/applications/{application_id}/repayment-feasibility",
        status_code=status.HTTP_303_SEE_OTHER
    )

@router.post("/applications/{application_id}/collateral/{collateral_id}/documents/{document_id}/delete")
async def delete_collateral_document(
    application_id: str,
    collateral_id: str,
    document_id: str,
    conn = Depends(db_conn),
    current_user = Depends(RoleChecker(["Credit Analyst"])),
):
    try:
        app_uuid = UUID(application_id)
        collateral_uuid = UUID(collateral_id)
        document_uuid = UUID(document_id)
    except ValueError:
        raise HTTPException(status_code=404, detail="Not found")

    repo = LoanRepository(conn)
    app = await repo.get_by_id(app_uuid, current_user.org_id)
    if not app:
        raise HTTPException(status_code=404, detail="Loan Application not found")

    _verify_loan_scope(app, current_user)

    async with conn.transaction():
        result = await conn.execute(
            """DELETE FROM collateral_documents cd
               USING collateral_items ci
               WHERE cd.id = $1 AND cd.collateral_item_id = $2
                 AND ci.id = cd.collateral_item_id AND ci.application_id = $3""",
            document_uuid, collateral_uuid, app_uuid
        )
        if result == "DELETE 0":
            raise HTTPException(status_code=404, detail="Collateral document not found")

        audit = AuditService(conn)
        await audit.insert(
            org_id=current_user.org_id,
            entity_type="collateral_document",
            entity_id=document_uuid,
            action="collateral_document.deleted",
            user_id=current_user.id,
            user_role=current_user.role,
            notes="Deleted collateral document"
        )

    return RedirectResponse(
        url=f"/applications/{application_id}/repayment-feasibility",
        status_code=status.HTTP_303_SEE_OTHER
    )

# =============================================================================
# SECURE DOCUMENT PREVIEW PROXY
# =============================================================================

@router.get("/api/v1/documents/collateral/{document_id}/preview")
async def preview_collateral_document(
    document_id: UUID,
    request: Request,
    page: int = Query(default=1, ge=1, le=100),
    conn = Depends(db_conn),
    current_user = Depends(get_current_user),
):
    # Fetch document metadata
    doc = await conn.fetchrow(
        """SELECT cd.*, ci.application_id, la.org_id 
           FROM collateral_documents cd
           JOIN collateral_items ci ON ci.id = cd.collateral_item_id
           JOIN loan_applications la ON la.id = ci.application_id
           WHERE cd.id = $1""",
        document_id
    )
    if not doc:
        raise HTTPException(status_code=404, detail="Document not found")

    if doc["org_id"] != current_user.org_id:
        raise HTTPException(status_code=403, detail="Forbidden")

    # If it is local file, serve directly
    if doc["cloudinary_public_id"] == "local":
        path = doc["cloudinary_url"]
        if path.startswith("/static/uploads/"):
            rel = path.removeprefix("/static/uploads/")
            upload_root = Path(settings.DOCUMENT_UPLOAD_DIR).resolve()
            full_path = (upload_root / rel).resolve()
            if full_path.is_relative_to(upload_root) and full_path.is_file():
                return FileResponse(str(full_path))
        raise HTTPException(status_code=404, detail="Local document file not found")

    # If Cloudinary is configured, use signed_preview_url to proxy
    stored_mime_type = "image/png"
    if doc["document_type"].endswith(".pdf") or "pdf" in doc["cloudinary_url"].lower():
        stored_mime_type = "application/pdf"
    elif "png" in doc["cloudinary_url"].lower():
        stored_mime_type = "image/png"
    else:
        stored_mime_type = "image/jpeg"

    preview_url = signed_preview_url(doc["cloudinary_public_id"], stored_mime_type, page=page)
    
    client = httpx.AsyncClient(timeout=30.0)
    try:
        req = client.build_request("GET", preview_url)
        resp = await client.send(req, stream=True)
        resp.raise_for_status()
    except Exception as exc:
        await client.aclose()
        log.warning(f"Cloudinary preview proxy failed: {exc}")
        raise HTTPException(status_code=502, detail="Document preview is temporarily unavailable")

    async def generate_chunks():
        try:
            async for chunk in resp.aiter_bytes():
                yield chunk
        finally:
            await resp.aclose()
            await client.aclose()

    headers = {
        "Content-Disposition": f'inline; filename="{doc["document_type"]}"',
        "Cache-Control": "private, no-transform, max-age=86400"
    }
    
    # If original document is PDF, the signed preview URL returns a PNG thumbnail of page N
    media_type = resp.headers.get("content-type", "image/png")
    if stored_mime_type == "application/pdf":
        media_type = "image/png"

    return StreamingResponse(
        generate_chunks(),
        media_type=media_type,
        status_code=resp.status_code,
        headers=headers
    )

# =============================================================================
# BUSINESS PNL ENDPOINTS
# =============================================================================

@router.post("/applications/{application_id}/pnl")
async def update_business_pnl(
    application_id: str,
    revenue: Decimal = Form(...),
    expenses: Decimal = Form(...),
    period_label: str = Form(...),
    conn = Depends(db_conn),
    current_user = Depends(get_current_user),
):
    try:
        app_uuid = UUID(application_id)
    except ValueError:
        raise HTTPException(status_code=404, detail="Loan Application not found")

    repo = LoanRepository(conn)
    app = await repo.get_by_id(app_uuid, current_user.org_id)
    if not app:
        raise HTTPException(status_code=404, detail="Loan Application not found")

    _verify_loan_scope(app, current_user)

    if not period_label.strip():
        raise HTTPException(status_code=400, detail="Period label is required")

    async with conn.transaction():
        await conn.execute(
            """INSERT INTO business_pnl (application_id, revenue, expenses, period_label, created_by)
               VALUES ($1, $2, $3, $4, $5)
               ON CONFLICT (application_id) DO UPDATE SET
                   revenue = EXCLUDED.revenue,
                   expenses = EXCLUDED.expenses,
                   period_label = EXCLUDED.period_label,
                   updated_at = NOW()""",
            app_uuid, revenue, expenses, period_label.strip(), current_user.id
        )

        audit = AuditService(conn)
        await audit.insert(
            org_id=current_user.org_id,
            entity_type="business_pnl",
            entity_id=app_uuid,
            action="business_pnl.update",
            user_id=current_user.id,
            user_role=current_user.role,
            notes="Saved business PNL details"
        )

    return RedirectResponse(
        url=f"/applications/{application_id}/repayment-feasibility",
        status_code=status.HTTP_303_SEE_OTHER
    )

# =============================================================================
# RECOMMENDATIONS ENDPOINTS
# =============================================================================

@router.post("/applications/{application_id}/recommendations")
async def add_loan_recommendation(
    application_id: str,
    recommended_amount: Decimal = Form(...),
    notes: str = Form(""),
    return_url: str = Form(""),
    conn = Depends(db_conn),
    current_user = Depends(RoleChecker([
        "account_officer", "loan_officer", "relationship_officer", "branch_manager",
        "branch_supervisor", "credit_analyst", "crm", "head_crm", "ed", "md"
    ])),
):
    try:
        app_uuid = UUID(application_id)
    except ValueError:
        raise HTTPException(status_code=404, detail="Loan Application not found")

    repo = LoanRepository(conn)
    app = await repo.get_by_id(app_uuid, current_user.org_id)
    if not app:
        raise HTTPException(status_code=404, detail="Loan Application not found")

    _verify_loan_scope(app, current_user)

    if not recommended_amount.is_finite() or recommended_amount <= 0:
        raise HTTPException(status_code=422, detail="Recommended amount must be greater than zero")
    if recommended_amount > app.amount:
        raise HTTPException(status_code=422, detail="Recommended amount cannot exceed the requested amount")
    clean_notes = notes.strip()
    if len(clean_notes) > 2000:
        raise HTTPException(status_code=422, detail="Recommendation notes cannot exceed 2000 characters")

    async with conn.transaction():
        row = await conn.fetchrow(
            """INSERT INTO loan_recommendations (application_id, submitted_by, role_at_submission, recommended_amount, notes)
               VALUES ($1, $2, $3, $4, $5) RETURNING id""",
            app_uuid, current_user.id, current_user.role, recommended_amount, clean_notes or None
        )

        audit = AuditService(conn)
        await audit.insert(
            org_id=current_user.org_id,
            entity_type="loan_recommendation",
            entity_id=row["id"],
            action="recommendation.create",
            user_id=current_user.id,
            user_role=current_user.role,
            notes=f"Submitted recommendation amount: {recommended_amount}"
        )

    safe_prefix = f"/applications/{application_id}/"
    destination = return_url if return_url.startswith(safe_prefix) else f"{safe_prefix}repayment-feasibility"
    return RedirectResponse(url=destination, status_code=status.HTTP_303_SEE_OTHER)
