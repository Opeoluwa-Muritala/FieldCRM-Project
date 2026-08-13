"""One-time authorisations and verified finalization for direct Cloudinary uploads."""
from __future__ import annotations

import asyncio
import mimetypes
import re
import time
from datetime import datetime, timedelta, timezone
from pathlib import Path
from uuid import UUID, uuid4

from fastapi import HTTPException, status

from app.core.audit import AuditService
from app.core.config import settings
from app.domains.documents.repository import DocumentRepository
from app.domains.documents.service import ALLOWED_EXTENSIONS, FORM_CODES
from app.services.cloud_storage_service import _configure_cloudinary
from app.core.loan_authorization import canonical_role


ALLOWED_MIME_TYPES = {"application/pdf", "image/jpeg", "image/png"}
FORMAT_TO_MIME = {"pdf": "application/pdf", "jpg": "image/jpeg", "jpeg": "image/jpeg", "png": "image/png"}


def _validate_metadata(filename: str, mime_type: str, size_bytes: int) -> None:
    if mime_type not in ALLOWED_MIME_TYPES or mime_type not in settings.DOCUMENT_ALLOWED_MIME_TYPES:
        raise HTTPException(status_code=400, detail="Unsupported document type. Upload PDF, JPEG, or PNG.")
    expected = mimetypes.guess_type(filename)[0]
    if expected != mime_type:
        raise HTTPException(status_code=400, detail="The file extension does not match the declared document type.")
    maximum = settings.DOCUMENT_MAX_PDF_BYTES if mime_type == "application/pdf" else settings.DOCUMENT_MAX_IMAGE_BYTES
    if size_bytes <= 0 or size_bytes > maximum:
        raise HTTPException(status_code=400, detail=f"Document size must be between 1 byte and {maximum} bytes.")


class DirectDocumentUploadService:
    def __init__(self, conn):
        self.conn = conn
        self.documents = DocumentRepository(conn)
        self.audit = AuditService(conn)

    async def authorize(
        self,
        *,
        application_id: UUID,
        org_id: UUID,
        actor_id: UUID,
        actor_role: str,
        doc_type: str,
        original_name: str,
        mime_type: str,
        size_bytes: int,
        form_code: str | None = None,
    ) -> dict:
        if not settings.cloudinary_enabled:
            raise HTTPException(status_code=503, detail="Direct document upload is unavailable.")
        _validate_metadata(original_name, mime_type, size_bytes)
        safe_type = re.sub(r"[^a-zA-Z0-9_.-]+", "_", doc_type or "other").strip("._") or "other"
        intent_id = uuid4()
        public_id = f"fieldcrm/{org_id}/{application_id}/{safe_type}_{uuid4().hex}"
        expires_at = datetime.now(timezone.utc) + timedelta(minutes=5)
        await self.conn.execute(
            """
            INSERT INTO document_upload_intents (
                id, organization_id, application_id, actor_id, actor_role,
                document_type, form_code, original_name, mime_type,
                expected_size_bytes, cloud_public_id, expires_at
            ) VALUES ($1,$2,$3,$4,$5,$6,$7,$8,$9,$10,$11,$12)
            """,
            intent_id, org_id, application_id, actor_id, canonical_role(actor_role), doc_type or "other",
            form_code, Path(original_name).name, mime_type, size_bytes, public_id, expires_at,
        )
        _configure_cloudinary()
        import cloudinary.utils

        timestamp = int(time.time())
        signed_params = {
            "timestamp": timestamp,
            "public_id": public_id,
            "type": "authenticated",
            "overwrite": "false",
        }
        signature = cloudinary.utils.api_sign_request(signed_params, settings.CLOUDINARY_API_SECRET)
        return {
            "intent_id": str(intent_id),
            "upload_url": (
                f"https://api.cloudinary.com/v1_1/{settings.CLOUDINARY_CLOUD_NAME}/image/upload"
            ),
            "fields": {
                **{key: str(value) for key, value in signed_params.items()},
                "api_key": settings.CLOUDINARY_API_KEY,
                "signature": signature,
            },
            "expires_at": expires_at.isoformat(),
            "fallback_available": True,
        }

    async def finalize(
        self,
        *,
        intent_id: UUID,
        application_id: UUID,
        org_id: UUID,
        actor_id: UUID,
        public_id: str,
        version: int,
        signature: str,
    ) -> dict:
        intent_row = await self.conn.fetchrow(
            """
            SELECT * FROM document_upload_intents
            WHERE id=$1 AND application_id=$2 AND organization_id=$3 AND actor_id=$4
            """,
            intent_id, application_id, org_id, actor_id,
        )
        if not intent_row:
            raise HTTPException(status_code=404, detail="Upload authorization not found.")
        intent = dict(intent_row)
        if intent["status"] == "finalized":
            document = await self.conn.fetchrow("SELECT * FROM documents WHERE id=$1", intent["document_id"])
            return dict(document)
        if intent["expires_at"] <= datetime.now(timezone.utc):
            await self.conn.execute(
                "UPDATE document_upload_intents SET status='expired' WHERE id=$1 AND status='pending'", intent_id
            )
            raise HTTPException(status_code=409, detail="Upload authorization expired.")
        if public_id != intent["cloud_public_id"]:
            raise HTTPException(status_code=400, detail="Uploaded asset does not match this authorization.")

        _configure_cloudinary()
        import cloudinary.api
        import cloudinary.utils

        expected_signature = cloudinary.utils.api_sign_request(
            {"public_id": public_id, "version": version}, settings.CLOUDINARY_API_SECRET
        )
        if not __import__("hmac").compare_digest(signature, expected_signature):
            raise HTTPException(status_code=400, detail="Invalid Cloudinary upload response.")
        try:
            asset = await asyncio.to_thread(
                cloudinary.api.resource,
                public_id,
                resource_type="image",
                type="authenticated",
            )
        except Exception as exc:
            raise HTTPException(status_code=409, detail="Uploaded asset could not be verified.") from exc

        asset_mime = FORMAT_TO_MIME.get(str(asset.get("format", "")).lower())
        valid = (
            asset.get("public_id") == public_id
            and asset.get("resource_type") == "image"
            and asset.get("type") == "authenticated"
            and asset_mime == intent["mime_type"]
            and int(asset.get("bytes") or 0) == intent["expected_size_bytes"]
        )
        if not valid:
            try:
                import cloudinary.uploader
                await asyncio.to_thread(
                    cloudinary.uploader.destroy, public_id, resource_type="image", type="authenticated"
                )
            finally:
                raise HTTPException(status_code=400, detail="Uploaded asset metadata failed verification.")

        preview_url = asset["secure_url"].replace(".pdf", ".jpg") if asset_mime == "application/pdf" else asset["secure_url"]
        async with self.conn.transaction():
            claimed = await self.conn.fetchrow(
                """
                UPDATE document_upload_intents SET status='finalized', finalized_at=NOW()
                WHERE id=$1 AND status='pending'
                RETURNING id
                """,
                intent_id,
            )
            if not claimed:
                completed = await self.conn.fetchrow(
                    "SELECT document_id FROM document_upload_intents WHERE id=$1", intent_id
                )
                document = await self.conn.fetchrow("SELECT * FROM documents WHERE id=$1", completed["document_id"])
                return dict(document)
            document = await self.documents.create(
                loan_id=intent["application_id"],
                org_id=intent["organization_id"],
                doc_type=intent["document_type"],
                form_code=intent["form_code"] or FORM_CODES.get(intent["document_type"]),
                original_name=intent["original_name"],
                stored_path=f"cloudinary://{public_id}",
                mime_type=intent["mime_type"],
                size_bytes=intent["expected_size_bytes"],
                uploaded_by=intent["actor_id"],
                cloud_public_id=public_id,
                cloud_preview_url=preview_url,
            )
            await self.audit.insert(
                org_id=intent["organization_id"], entity_type="document", entity_id=document["id"],
                action="document.uploaded", user_id=intent["actor_id"], user_role=intent["actor_role"],
                field_name="doc_type", new_value=intent["document_type"], source="direct_cloudinary",
            )
            await self.conn.execute(
                "INSERT INTO ocr_jobs (id, document_id, status) VALUES ($1,$2,'pending')",
                uuid4(), document["id"],
            )
            await self.conn.execute(
                "UPDATE document_upload_intents SET document_id=$2 WHERE id=$1", intent_id, document["id"]
            )
        return document
