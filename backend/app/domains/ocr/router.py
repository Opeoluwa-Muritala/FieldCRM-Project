from fastapi import APIRouter, Depends, Header, HTTPException, status
from app.core.database import db_conn
from app.config import settings
from app.services.ocr_extraction_service import OcrExtractionService
from datetime import datetime, timezone
import logging
from uuid import UUID

router = APIRouter()
logger = logging.getLogger("ocr_worker")

@router.get("/api/v1/internal/ocr-worker")
async def run_ocr_worker(
    x_ocr_secret: str = Header(None, alias="X-OCR-Secret"),
    conn = Depends(db_conn),
):
    if not x_ocr_secret or x_ocr_secret != settings.ORG_REGISTRATION_SECRET:
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="Forbidden")

    # 1. Reset stale locks (processing for > 5 minutes)
    if "postgresql" in settings.DATABASE_URL:
        await conn.execute(
            "UPDATE ocr_jobs SET status = 'pending', updated_at = NOW() WHERE status = 'processing' AND updated_at < NOW() - INTERVAL '5 minutes'"
        )
    else:
        await conn.execute(
            "UPDATE ocr_jobs SET status = 'pending', updated_at = CURRENT_TIMESTAMP WHERE status = 'processing' AND datetime(updated_at) < datetime('now', '-5 minutes')"
        )

    # 2. Get pending jobs
    jobs = await conn.fetch(
        "SELECT j.id, j.document_id, d.loan_id, d.doc_type, d.stored_path, d.mime_type "
        "FROM ocr_jobs j "
        "JOIN documents d ON j.document_id = d.id "
        "WHERE j.status = 'pending' "
        "ORDER BY j.created_at ASC LIMIT 5"
    )

    processed_count = 0
    for job in jobs:
        job_id = job["id"]
        doc_id = job["document_id"]
        
        # Mark as processing
        if "postgresql" in settings.DATABASE_URL:
            await conn.execute(
                "UPDATE ocr_jobs SET status = 'processing', updated_at = NOW() WHERE id = $1", job_id
            )
        else:
            await conn.execute(
                "UPDATE ocr_jobs SET status = 'processing', updated_at = CURRENT_TIMESTAMP WHERE id = $1", job_id
            )

        try:
            # Process document OCR
            await OcrExtractionService(conn).process_document(
                document_id=doc_id,
                loan_id=job["loan_id"],
                doc_type=job["doc_type"],
                stored_path=job["stored_path"],
                mime_type=job["mime_type"],
                upload_dir=settings.DOCUMENT_UPLOAD_DIR,
            )

            # Mark as done
            if "postgresql" in settings.DATABASE_URL:
                await conn.execute(
                    "UPDATE ocr_jobs SET status = 'done', updated_at = NOW() WHERE id = $1", job_id
                )
            else:
                await conn.execute(
                    "UPDATE ocr_jobs SET status = 'done', updated_at = CURRENT_TIMESTAMP WHERE id = $1", job_id
                )
            processed_count += 1
        except Exception as e:
            logger.error(f"OCR job failed for job_id {job_id}: {e}")
            # Mark as failed
            if "postgresql" in settings.DATABASE_URL:
                await conn.execute(
                    "UPDATE ocr_jobs SET status = 'failed', error = $1, updated_at = NOW() WHERE id = $2",
                    str(e), job_id
                )
            else:
                await conn.execute(
                    "UPDATE ocr_jobs SET status = 'failed', error = $1, updated_at = CURRENT_TIMESTAMP WHERE id = $2",
                    str(e), job_id
                )

    return {"status": "ok", "processed_jobs": processed_count}
