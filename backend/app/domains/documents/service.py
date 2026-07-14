import asyncio
import io
import logging
import mimetypes
import re
from pathlib import Path
from uuid import UUID, uuid4

from fastapi import HTTPException, UploadFile, status

from app.core.audit import AuditService
from app.core.config import settings
from app.domains.documents.repository import DocumentRepository

log = logging.getLogger(__name__)


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

_SIGNATURES = {
    "application/pdf": b"%PDF-",
    "image/jpeg": b"\xff\xd8\xff",
    "image/png": b"\x89PNG\r\n\x1a\n",
}


def validate_file(file_bytes: bytes, mimetype: str, allowed: set[str], filename: str, enforce_size: bool = True) -> str | None:
    """Validate declared type, extension, and binary signature before upload."""
    if not file_bytes:
        return "Uploaded file is empty."
    if enforce_size and len(file_bytes) > settings.DOCUMENT_MAX_UPLOAD_BYTES:
        return f"Document exceeds the {settings.DOCUMENT_MAX_UPLOAD_BYTES // (1024 * 1024)} MB limit."
    if mimetype not in allowed:
        return "Unsupported document type. Upload PDF, JPEG, or PNG."
    extension = Path(filename or "").suffix.lower()
    extension_map = {".pdf": "application/pdf", ".jpg": "image/jpeg", ".jpeg": "image/jpeg", ".png": "image/png"}
    expected = extension_map.get(extension)
    if not expected or expected not in allowed:
        return "File extension must be PDF, JPG, JPEG, or PNG."
    if expected != mimetype:
        return "The file extension does not match the declared document type."
    if not file_bytes.startswith(_SIGNATURES[mimetype]):
        return "The file contents do not match the declared document type."
    return None


def compress_image(file_bytes: bytes, mimetype: str) -> tuple[bytes, str]:
    """Optimise camera scans without altering PDFs or increasing file size."""
    if mimetype not in {"image/jpeg", "image/png"}:
        return file_bytes, mimetype
    try:
        from PIL import Image, ImageOps
    except ImportError:
        log.warning("Pillow is unavailable; image upload was not compressed")
        return file_bytes, mimetype
    try:
        with Image.open(io.BytesIO(file_bytes)) as image:
            image = ImageOps.exif_transpose(image)
            image.thumbnail((2200, 2200), Image.Resampling.LANCZOS)
            if image.mode in {"RGBA", "LA"} or (image.mode == "P" and "transparency" in image.info):
                background = Image.new("RGB", image.size, "white")
                background.paste(image.convert("RGBA"), mask=image.convert("RGBA").getchannel("A"))
                image = background
            elif image.mode != "RGB":
                image = image.convert("RGB")
            output = io.BytesIO()
            image.save(output, format="JPEG", quality=82, progressive=True, optimize=True)
            compressed = output.getvalue()
            return (compressed, "image/jpeg") if len(compressed) < len(file_bytes) else (file_bytes, mimetype)
    except Exception:
        log.warning("Image compression failed; using original upload", exc_info=True)
        return file_bytes, mimetype


def prepare_upload_file(file_bytes: bytes, mimetype: str, allowed: set[str], filename: str) -> tuple[bytes, str, str | None]:
    error = validate_file(file_bytes, mimetype, allowed, filename, enforce_size=False)
    if error:
        return file_bytes, mimetype, error
    prepared, prepared_type = compress_image(file_bytes, mimetype)
    if len(prepared) > settings.DOCUMENT_MAX_UPLOAD_BYTES:
        return prepared, prepared_type, (
            f"Document exceeds the {settings.DOCUMENT_MAX_UPLOAD_BYTES // (1024 * 1024)} MB limit after image optimisation."
        )
    return prepared, prepared_type, None


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
        content = await file.read(settings.DOCUMENT_MAX_UPLOAD_BYTES + 1)
        content, mime_type, error = prepare_upload_file(
            content, mime_type, set(settings.DOCUMENT_ALLOWED_MIME_TYPES), original_name
        )
        if error:
            raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail=error)

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

        # Attempt Cloudinary upload; fall back to local path if not configured
        from app.services.cloud_storage_service import upload_document as _cloud_upload
        cloud_result = _cloud_upload(
            file_bytes=content,
            mime_type=mime_type,
            org_id=str(org_id),
            loan_id=str(loan_id),
            doc_type=safe_doc_type,
            filename_stem=uuid4().hex,
        )
        stored_path = cloud_result.stored_path if cloud_result else (
            "/static/uploads/" + (relative_dir / stored_name).as_posix()
        )

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
            cloud_public_id=cloud_result.public_id if cloud_result else None,
            cloud_preview_url=cloud_result.preview_url if cloud_result else None,
        )
        await self._audit_upload(
            document=document,
            org_id=org_id,
            uploaded_by=uploaded_by,
            user_role=user_role,
            doc_type=doc_type,
        )
        # Trigger server-side OCR asynchronously (fire-and-forget)
        asyncio.create_task(self._run_ocr(document, mime_type))
        return document

    async def _run_ocr(self, document: dict, mime_type: str) -> None:
        try:
            from app.services.ocr_extraction_service import OcrExtractionService
            from uuid import UUID as _UUID
            await OcrExtractionService(self.repo.conn).process_document(
                document_id=_UUID(str(document["id"])),
                loan_id=_UUID(str(document["loan_id"])),
                doc_type=document["doc_type"],
                stored_path=document["stored_path"],
                mime_type=mime_type,
                upload_dir=settings.DOCUMENT_UPLOAD_DIR,
            )
        except Exception as e:
            log.warning("OCR background task failed for doc %s: %s", document.get("id"), e)

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
