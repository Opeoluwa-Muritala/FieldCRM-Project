"""
Cloud storage service — Cloudinary backend for PDF/image documents.

Production documents are private Cloudinary assets.  The database stores the
Cloudinary public id and the application issues short-lived download URLs only
after its own tenant and role checks have succeeded.
"""
from __future__ import annotations

import io
import logging
import time
from typing import NamedTuple

from fastapi import HTTPException, status

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
    overwrite: bool = True,
) -> UploadResult | None:
    """
    Upload file bytes to Cloudinary.

    PDFs are uploaded with resource_type='image' so Cloudinary can render
    page thumbnails. The `preview_url` replaces the .pdf extension with .jpg
    to get a page-1 thumbnail; page N is accessed via the pg_N URL transform.
    """
    _configure_cloudinary()
    import cloudinary.uploader

    # Send a binary stream, not a base64 data URI. Data URIs are submitted as
    # a multipart form field and Cloudinary rejects fields above 1024 KB before
    # its normal upload handling can accept the document.
    upload_stream = io.BytesIO(file_bytes)

    upload_opts: dict = {
        "folder": folder,
        # PDFs must be stored as image resources for Cloudinary's pg_N page
        # transformations. `auto` classifies authenticated PDFs as raw files.
        "resource_type": "image" if mime_type in {"application/pdf", "image/jpeg", "image/png"} else "auto",
        "type": "authenticated",
        "overwrite": overwrite,
    }
    if public_id:
        upload_opts["public_id"] = public_id
        # The high-level caller already supplies a fully-qualified public ID.
        # Passing both folder and that ID duplicates the path in Cloudinary.
        upload_opts.pop("folder", None)

    result = cloudinary.uploader.upload(
        upload_stream,
        **{k: v for k, v in upload_opts.items() if v is not None},
    )

    secure_url: str = result["secure_url"]
    pid: str = result["public_id"]

    preview_url = secure_url.replace(".pdf", ".jpg") if mime_type == "application/pdf" else secure_url

    return UploadResult(stored_path=secure_url, public_id=pid, preview_url=preview_url)


def upload_immutable_evidence(file_bytes: bytes, mime_type: str, *, org_id: str,
                              loan_id: str, doc_type: str, version: int,
                              content_sha256: str) -> UploadResult:
    """Store evidence under its content hash; existing objects are never replaced."""
    public_id = f"fieldcrm/{org_id}/{loan_id}/{doc_type}/{version}/{content_sha256}"
    result = upload_to_cloudinary(
        file_bytes, mime_type, public_id=public_id, overwrite=False
    )
    if result is None:
        raise HTTPException(status_code=503, detail="Immutable evidence storage is unavailable")
    return result


def upload_document(
    file_bytes: bytes,
    mime_type: str,
    org_id: str,
    loan_id: str,
    doc_type: str,
    filename_stem: str,
) -> UploadResult:
    """
    High-level entry point called from DocumentService.
    Local disk is permitted only outside production, where it remains useful
    for development and tests.  Vercel's filesystem is ephemeral.
    """
    from app.core.config import settings
    if not settings.cloudinary_enabled:
        if settings.is_production:
            raise HTTPException(
                status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
                detail="Document uploads are unavailable: Cloudinary is not configured.",
            )
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
    except Exception as exc:
        log.exception("Cloudinary upload failed")
        if settings.is_production:
            raise HTTPException(
                status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
                detail="Document upload failed; please retry.",
            ) from exc
        return None


def signed_download_url(public_id: str, mime_type: str, expires_in: int = 300) -> str:
    """Return a five-minute API download URL for an authenticated Cloudinary asset."""
    _configure_cloudinary()
    import cloudinary.utils

    options: dict[str, object] = {
        "resource_type": "image",
        "type": "authenticated",
        # This endpoint is used by explicit download actions, including offer
        # letters, so Cloudinary must send an attachment response.
        "attachment": True,
        "expires_at": int(time.time()) + expires_in,
    }
    file_format = "pdf" if mime_type == "application/pdf" else None
    return cloudinary.utils.private_download_url(public_id, file_format, **options)


def signed_preview_url(
    public_id: str,
    mime_type: str,
    *,
    page: int = 1,
    expires_in: int = 300,
) -> str:
    """Return an authenticated preview URL without delivering a PDF to the client."""
    _configure_cloudinary()
    import cloudinary.utils

    if mime_type == "application/pdf":
        # Cloudinary renders only the requested PDF page as PNG.  The original
        # PDF is never proxied to, or downloaded by, the browser.
        url, _ = cloudinary.utils.cloudinary_url(
            public_id,
            resource_type="image",
            type="authenticated",
            sign_url=True,
            format="png",
            transformation=[
                {"page": page},
                {"width": 1600, "crop": "limit", "quality": "auto"},
            ],
        )
        return url

    options: dict[str, object] = {
        "resource_type": "image",
        "type": "authenticated",
        # Explicitly avoid Cloudinary's attachment transformation.
        "attachment": False,
        "expires_at": int(time.time()) + expires_in,
    }
    file_format = "pdf" if mime_type == "application/pdf" else None
    return cloudinary.utils.private_download_url(public_id, file_format, **options)
