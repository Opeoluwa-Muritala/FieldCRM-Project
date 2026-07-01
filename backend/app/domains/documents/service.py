import mimetypes
import re
from pathlib import Path
from uuid import UUID, uuid4

from fastapi import HTTPException, UploadFile, status

from app.core.audit import AuditService
from app.core.config import settings
from app.domains.documents.repository import DocumentRepository


FORM_CODES = {
    "loan_application_form": "MMFB/CRM/01",
    "loan": "MMFB/CRM/01",
    "pledge_form": "MMFB/CRM/02",
    "pledge": "MMFB/CRM/02",
    "guarantor_form": "MMFB/CRM/03",
    "guarantor": "MMFB/CRM/03",
}

ALLOWED_EXTENSIONS = {
    "application/pdf": ".pdf",
    "image/jpeg": ".jpg",
    "image/png": ".png",
}


class DocumentService:
    def __init__(self, repo: DocumentRepository, audit: AuditService):
        self.repo = repo
        self.audit = audit

    async def save_upload(
        self,
        *,
        loan_id: UUID,
        org_id: UUID,
        doc_type: str,
        file: UploadFile,
        uploaded_by: UUID,
        user_role: str,
        form_code: str | None = None,
    ) -> dict:
        original_name = file.filename or "document"
        mime_type = file.content_type or mimetypes.guess_type(original_name)[0] or ""
        if mime_type not in settings.DOCUMENT_ALLOWED_MIME_TYPES:
            raise HTTPException(
                status_code=status.HTTP_415_UNSUPPORTED_MEDIA_TYPE,
                detail="Unsupported document type. Upload PDF, JPEG, or PNG.",
            )

        content = await file.read(settings.DOCUMENT_MAX_UPLOAD_BYTES + 1)
        if not content:
            raise HTTPException(status_code=400, detail="Uploaded file is empty")
        if len(content) > settings.DOCUMENT_MAX_UPLOAD_BYTES:
            max_mb = settings.DOCUMENT_MAX_UPLOAD_BYTES // (1024 * 1024)
            raise HTTPException(status_code=413, detail=f"Document exceeds {max_mb} MB limit")

        safe_doc_type = re.sub(r"[^a-zA-Z0-9_.-]+", "_", doc_type or "other").strip("._") or "other"
        extension = Path(original_name).suffix.lower() or ALLOWED_EXTENSIONS[mime_type]
        if extension not in {".pdf", ".jpg", ".jpeg", ".png"}:
            extension = ALLOWED_EXTENSIONS[mime_type]

        upload_root = Path(settings.DOCUMENT_UPLOAD_DIR)
        relative_dir = Path(str(org_id)) / str(loan_id)
        target_dir = upload_root / relative_dir
        target_dir.mkdir(parents=True, exist_ok=True)

        stored_name = f"{safe_doc_type}_{uuid4().hex}{extension}"
        target_path = target_dir / stored_name
        target_path.write_bytes(content)

        stored_path = "/static/uploads/" + (relative_dir / stored_name).as_posix()
        document = await self.repo.create(
            loan_id=loan_id,
            org_id=org_id,
            doc_type=doc_type,
            form_code=form_code or FORM_CODES.get(doc_type),
            original_name=original_name,
            stored_path=stored_path,
            mime_type=mime_type,
            size_bytes=len(content),
            uploaded_by=uploaded_by,
        )
        await self._audit_upload(
            document=document,
            org_id=org_id,
            uploaded_by=uploaded_by,
            user_role=user_role,
            doc_type=doc_type,
        )
        return document

    async def save_mock_upload(
        self,
        *,
        loan_id: UUID,
        org_id: UUID,
        category: str,
        uploaded_by: UUID,
        user_role: str,
    ) -> dict:
        document = await self.repo.create_mock_upload(
            loan_id=loan_id,
            org_id=org_id,
            doc_type=category,
            form_code=FORM_CODES.get(category),
            original_name=f"mock_{category}.pdf",
            stored_path=f"/static/uploads/mock_{category}.pdf",
            mime_type="application/pdf",
            size_bytes=1024,
            uploaded_by=uploaded_by,
        )
        await self._audit_upload(
            document=document,
            org_id=org_id,
            uploaded_by=uploaded_by,
            user_role=user_role,
            doc_type=category,
        )
        return document

    async def _audit_upload(
        self,
        *,
        document: dict,
        org_id: UUID,
        uploaded_by: UUID,
        user_role: str,
        doc_type: str,
    ) -> None:
        await self.audit.insert(
            org_id=org_id,
            entity_type="document",
            entity_id=document["id"],
            action="document.uploaded",
            user_id=uploaded_by,
            user_role=user_role,
            field_name="doc_type",
            new_value=doc_type,
            source="manual",
        )
