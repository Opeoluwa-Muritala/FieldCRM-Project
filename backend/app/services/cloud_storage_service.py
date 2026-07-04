"""
Cloud storage service — Cloudinary backend for PDF/image documents.

When CLOUDINARY_CLOUD_NAME, CLOUDINARY_API_KEY, and CLOUDINARY_API_SECRET are
set in the environment, uploaded documents are stored on Cloudinary and the
returned `secure_url` is persisted in `documents.stored_path` instead of a
local file path.

Falls back to local disk storage when credentials are absent.
"""
from __future__ import annotations

import base64
import logging
from typing import NamedTuple

log = logging.getLogger(__name__)


class UploadResult(NamedTuple):
    stored_path: str        # URL (Cloudinary) or local static path
    public_id: str | None   # Cloudinary public_id for future transforms; None for local
    preview_url: str | None # First-page preview image URL (Cloudinary only)


def _configure_cloudinary():
    """Initialise the cloudinary SDK from settings. Call once before any upload."""
    import cloudinary
    from app.core.config import settings
    cloudinary.config(
        cloud_name=settings.CLOUDINARY_CLOUD_NAME,
        api_key=settings.CLOUDINARY_API_KEY,
        api_secret=settings.CLOUDINARY_API_SECRET,
        secure=True,
    )


def upload_to_cloudinary(
    file_bytes: bytes,
    mime_type: str,
    folder: str = "fieldcrm/documents",
    public_id: str | None = None,
) -> UploadResult:
    """
    Upload file bytes to Cloudinary.

    PDFs are uploaded with resource_type='image' so Cloudinary can render
    page thumbnails. The `preview_url` replaces the .pdf extension with .jpg
    to get a page-1 thumbnail; page N is accessed via the pg_N URL transform.
    """
    _configure_cloudinary()
    import cloudinary.uploader

    data_uri = f"data:{mime_type};base64,{base64.b64encode(file_bytes).decode()}"

    upload_opts: dict = {
        "folder": folder,
        "resource_type": "image",   # lets Cloudinary handle PDF page rendering
        "format": "pdf" if mime_type == "application/pdf" else None,
        "overwrite": True,
    }
    if public_id:
        upload_opts["public_id"] = public_id

    result = cloudinary.uploader.upload(data_uri, **{k: v for k, v in upload_opts.items() if v is not None})

    secure_url: str = result["secure_url"]
    pid: str = result["public_id"]

    preview_url = secure_url.replace(".pdf", ".jpg") if mime_type == "application/pdf" else secure_url

    return UploadResult(stored_path=secure_url, public_id=pid, preview_url=preview_url)


def upload_document(
    file_bytes: bytes,
    mime_type: str,
    org_id: str,
    loan_id: str,
    doc_type: str,
    filename_stem: str,
) -> UploadResult | None:
    """
    High-level entry point called from DocumentService.
    Returns UploadResult if Cloudinary is configured, None to fall through to local storage.
    """
    from app.core.config import settings
    if not settings.cloudinary_enabled:
        return None

    folder = f"fieldcrm/{org_id}/{loan_id}"
    public_id = f"{folder}/{doc_type}_{filename_stem}"

    try:
        return upload_to_cloudinary(
            file_bytes=file_bytes,
            mime_type=mime_type,
            folder=folder,
            public_id=public_id,
        )
    except Exception as e:
        log.error("Cloudinary upload failed, falling back to local storage: %s", e)
        return None
