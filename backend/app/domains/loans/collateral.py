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
from app.core.database import db_conn, get_connection
from app.core.dependencies import get_current_user, RoleChecker
from app.core.audit import AuditService
from app.core.template_utils import build_template_context
from app.core.templates import create_templates
from app.domains.loans.repository import LoanRepository
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
    current_user = Depends(RoleChecker(["Relationship Officer", "Branch Manager", "Branch Supervisor", "CRM", "Head CRM", "Auditor", "ED", "MD", "Legal"])),
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

    # This screen is read-only analysis; data entry lives in the officer wizard.
    total_pledged = await conn.fetchval(
        "SELECT COALESCE(SUM(COALESCE(face_value, force_sale_value)), 0) FROM collateral_items WHERE application_id = $1",
        app_uuid,
    )
    pnl = await conn.fetchrow(
        "SELECT * FROM business_pnl WHERE application_id = $1",
        app_uuid
    )
    total_pledged = Decimal(str(total_pledged or 0))
    
    loan_amount = Decimal(str(app.amount or 0))
    coverage_ratio = Decimal("0")
    if loan_amount > 0:
        coverage_ratio = (total_pledged / loan_amount)

    rate = await get_loan_interest_rate(conn, app)
    tenor = int(app.tenor_months or 12)
    installment_amount = Decimal("0")
    if loan_amount > 0 and tenor > 0:
        try:
            rows = generate_schedule(
                principal=float(loan_amount),
                annual_rate=rate,
                tenor_months=tenor,
                frequency="monthly",
                method="flat_rate",
                disbursement_date=date.today()
            )
            if rows:
                installment_amount = Decimal(str(rows[0]["total_due"]))
        except Exception as e:
            log.error(f"Error computing proposed installment: {e}")

    # Build response context
    ctx = build_template_context(
        request, current_user,
        app=app, app_id=application_id,
        pnl=dict(pnl) if pnl else None,
        total_pledged_value=total_pledged,
        coverage_ratio=coverage_ratio,
        proposed_installment=installment_amount,
        proposed_interest_rate=rate,
        active_tab="queue", active_page="queue"
    )
    return templates.TemplateResponse(request, "shared/repayment_feasibility.html", ctx)

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
    safe_doc_type = re.sub(r"[^a-zA-Z0-9_.-]+", "_", document_type or "other").strip("._") or "other"
    
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
    current_user = Depends(get_current_user),
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
        await conn.execute(
            "DELETE FROM collateral_documents WHERE id = $1 AND collateral_item_id = $2",
            document_uuid, collateral_uuid
        )

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
        if path.startswith("/static/"):
            rel = path.replace("/static/uploads/", "")
            full_path = Path(settings.DOCUMENT_UPLOAD_DIR) / rel
            if full_path.exists():
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

    async with conn.transaction():
        row = await conn.fetchrow(
            """INSERT INTO loan_recommendations (application_id, submitted_by, role_at_submission, recommended_amount, notes)
               VALUES ($1, $2, $3, $4, $5) RETURNING id""",
            app_uuid, current_user.id, current_user.role, recommended_amount, notes.strip() or None
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
