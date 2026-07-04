"""
Server-side OCR extraction service.

Uses pdfplumber for text-based PDFs and pytesseract (via pdf2image) for
scanned/image PDFs. Extracts fields per document type and stores them in
ocr_results + ocr_fields, then auto-fills high-confidence values into stage_data.
"""
from __future__ import annotations

import asyncio
import json
import logging
import re
from pathlib import Path
from typing import Any
from uuid import UUID

log = logging.getLogger(__name__)

# ---------------------------------------------------------------------------
# Field maps per document type: (field_name, regex_or_label, is_critical)
# ---------------------------------------------------------------------------
_LOAN_APP_FIELDS: list[tuple[str, str, bool]] = [
    ("applicant_name",    r"(?:Full\s+Name|Applicant\s+Name)[:\s]+(.+)",        True),
    ("bvn",               r"BVN[:\s]+(\d{11})",                                 True),
    ("phone",             r"(?:Phone|Mobile)[:\s]+([+\d\s\-()]{7,15})",         False),
    ("amount",            r"(?:Loan\s+Amount|Amount\s+Requested)[:\s]+([\d,]+)", True),
    ("tenor_months",      r"(?:Tenor|Duration)[:\s]+(\d+)\s*(?:months?)?",      False),
    ("purpose",           r"(?:Purpose|Loan\s+Purpose)[:\s]+(.+)",              False),
    ("sector",            r"(?:Sector|Business\s+Sector)[:\s]+(.+)",            False),
]

_GUARANTOR_FIELDS: list[tuple[str, str, bool]] = [
    ("full_name",           r"(?:Full\s+Name|Guarantor\s+Name)[:\s]+(.+)",       True),
    ("bvn",                 r"BVN[:\s]+(\d{11})",                                True),
    ("phone",               r"(?:Phone|Mobile)[:\s]+([+\d\s\-()]{7,15})",        False),
    ("relationship",        r"Relationship[:\s]+(.+)",                            False),
    ("monthly_salary",      r"(?:Monthly\s+Salary|Income)[:\s]+([\d,]+)",        False),
    ("bank_name",           r"Bank[:\s]+(.+)",                                    False),
    ("account_number",      r"Account\s+Number[:\s]+(\d{10})",                   False),
    ("max_guarantee_amount",r"(?:Max\s+Guarantee|Guarantee\s+Amount)[:\s]+([\d,]+)", True),
]

_PLEDGE_FIELDS: list[tuple[str, str, bool]] = [
    ("item_name",        r"(?:Item|Pledged\s+Item|Property)[:\s]+(.+)",          True),
    ("serial_number",    r"(?:Serial\s+No|Serial\s+Number)[:\s]+([A-Z0-9\-]+)", False),
    ("estimated_value",  r"(?:Value|Estimated\s+Value)[:\s]+([\d,]+)",           True),
    ("ncr_reg_number",   r"(?:NCR|NCR\s+Reg|Registration)[:\s]+([A-Z0-9\-/]+)", False),
]

_FIELD_MAP: dict[str, tuple[list[tuple[str, str, bool]], str]] = {
    "loan_application_form": (_LOAN_APP_FIELDS,  "loan_application"),
    "loan":                  (_LOAN_APP_FIELDS,  "loan_application"),
    "guarantor_form":        (_GUARANTOR_FIELDS, "guarantor"),
    "guarantor":             (_GUARANTOR_FIELDS, "guarantor"),
    "pledge_form":           (_PLEDGE_FIELDS,    "pledge_receipt"),
    "pledge":                (_PLEDGE_FIELDS,    "pledge_receipt"),
}


def _clean(value: str) -> str:
    return " ".join(value.split()).strip()


def _extract_fields(
    text: str,
    field_specs: list[tuple[str, str, bool]],
    page_offset: int = 0,
) -> list[dict[str, Any]]:
    """Run regex extraction against raw text. Returns list of field dicts."""
    results = []
    for field_name, pattern, is_critical in field_specs:
        m = re.search(pattern, text, re.IGNORECASE | re.MULTILINE)
        if m:
            raw_val = _clean(m.group(1))
            # Confidence is 95 if BVN/account match exact digit patterns, else 80
            if field_name in ("bvn", "account_number") and raw_val.isdigit():
                confidence = 95.0
            elif raw_val:
                confidence = 80.0
            else:
                confidence = 0.0
            results.append({
                "field_name": field_name,
                "ocr_value": raw_val or None,
                "confidence": confidence,
                "is_critical": is_critical,
                "page_number": page_offset + 1 if page_offset >= 0 else None,
            })
        else:
            results.append({
                "field_name": field_name,
                "ocr_value": None,
                "confidence": 0.0,
                "is_critical": is_critical,
                "page_number": None,
            })
    return results


def _extract_text_pdfplumber(file_path: str) -> tuple[str, int]:
    """Extract text from a text-based PDF using pdfplumber. Returns (text, page_count)."""
    try:
        import pdfplumber
        with pdfplumber.open(file_path) as pdf:
            pages = []
            for page in pdf.pages:
                t = page.extract_text() or ""
                pages.append(t)
            return "\n".join(pages), len(pages)
    except Exception as e:
        log.warning("pdfplumber failed for %s: %s", file_path, e)
        return "", 0


def _extract_text_tesseract(file_path: str) -> tuple[str, int]:
    """OCR-extract from image-based PDF using pdf2image + pytesseract."""
    try:
        from pdf2image import convert_from_path
        import pytesseract
        images = convert_from_path(file_path, dpi=200)
        texts = [pytesseract.image_to_string(img) for img in images]
        return "\n".join(texts), len(images)
    except Exception as e:
        log.warning("Tesseract OCR failed for %s: %s", file_path, e)
        return "", 0


def _extract_text_image(file_path: str) -> tuple[str, int]:
    """OCR a single image (JPEG/PNG) using pytesseract."""
    try:
        import pytesseract
        from PIL import Image
        img = Image.open(file_path)
        text = pytesseract.image_to_string(img)
        return text, 1
    except Exception as e:
        log.warning("Image OCR failed for %s: %s", file_path, e)
        return "", 0


def extract_text_from_file(file_path: str, mime_type: str) -> tuple[str, int]:
    """Extract raw text from a document file. Returns (text, page_count)."""
    if mime_type == "application/pdf":
        text, pages = _extract_text_pdfplumber(file_path)
        if len(text.strip()) < 50:
            text, pages = _extract_text_tesseract(file_path)
        return text, pages
    elif mime_type in ("image/jpeg", "image/png", "image/jpg"):
        return _extract_text_image(file_path)
    return "", 0


class OcrExtractionService:
    """Runs OCR extraction on a document and persists results to DB."""

    def __init__(self, conn):
        self.conn = conn

    async def process_document(
        self,
        *,
        document_id: UUID,
        loan_id: UUID,
        doc_type: str,
        stored_path: str,
        mime_type: str,
        upload_dir: str,
    ) -> dict[str, Any] | None:
        """
        Main entry point. Called after a document is saved.
        Returns summary dict or None if doc_type is not a known form.
        """
        from app.domains.ocr.repository import OcrRepository

        spec = _FIELD_MAP.get(doc_type.lower())
        if not spec:
            await self.conn.execute(
                "UPDATE documents SET ocr_status = 'skipped' WHERE id = $1", document_id
            )
            return None

        field_specs, form_type = spec

        # Build absolute path from stored_path ("/static/uploads/...")
        rel = stored_path.lstrip("/").replace("static/uploads/", "")
        abs_path = str(Path(upload_dir) / rel)

        await self.conn.execute(
            "UPDATE documents SET ocr_status = 'processing' WHERE id = $1", document_id
        )

        try:
            text, page_count = await asyncio.get_event_loop().run_in_executor(
                None, extract_text_from_file, abs_path, mime_type
            )
        except Exception as e:
            log.error("OCR text extraction failed: %s", e)
            await self.conn.execute(
                "UPDATE documents SET ocr_status = 'failed' WHERE id = $1", document_id
            )
            return None

        if not text.strip():
            await self.conn.execute(
                "UPDATE documents SET ocr_status = 'failed' WHERE id = $1", document_id
            )
            return {"document_id": document_id, "fields": [], "page_count": page_count}

        fields = _extract_fields(text, field_specs)
        valid_fields = [f for f in fields if f["ocr_value"]]
        overall_confidence = (
            sum(f["confidence"] for f in valid_fields) / len(valid_fields)
            if valid_fields else 0.0
        )

        repo = OcrRepository(self.conn)
        result_row = await repo.insert_result(
            document_id=document_id,
            loan_id=loan_id,
            form_type=form_type,
            overall_confidence=overall_confidence,
            raw_extraction={"text_preview": text[:500], "page_count": page_count},
        )
        result_id = result_row["id"]

        for field in fields:
            try:
                await repo.insert_field(
                    ocr_result_id=result_id,
                    loan_id=loan_id,
                    field_name=field["field_name"],
                    ocr_value=field["ocr_value"],
                    confidence=field["confidence"],
                    is_critical=field["is_critical"],
                    page_number=field["page_number"],
                )
            except Exception as e:
                log.warning("Failed to insert OCR field %s: %s", field["field_name"], e)

        # Auto-fill high-confidence fields into stage_data for loan_application forms
        if form_type == "loan_application":
            await self._autofill_intake(loan_id, fields)

        await self.conn.execute(
            "UPDATE documents SET ocr_status = 'done' WHERE id = $1", document_id
        )
        return {
            "document_id": document_id,
            "form_type": form_type,
            "overall_confidence": overall_confidence,
            "field_count": len(valid_fields),
            "page_count": page_count,
        }

    async def _autofill_intake(self, loan_id: UUID, fields: list[dict]) -> None:
        """Write high-confidence OCR values into stage_data for OCR review."""
        high_conf = {
            f["field_name"]: f["ocr_value"]
            for f in fields
            if f["confidence"] >= 80.0 and f["ocr_value"]
        }
        if not high_conf:
            return
        high_conf["_ocr_source"] = "pdf_scan"
        existing = await self.conn.fetchrow(
            "SELECT id, data_json FROM stage_data WHERE loan_id = $1 AND stage = 'ocr_review' ORDER BY saved_at DESC LIMIT 1",
            loan_id,
        )
        if existing:
            merged = dict(existing["data_json"])
            merged.update(high_conf)
            await self.conn.execute(
                "UPDATE stage_data SET data_json = $1::jsonb WHERE id = $2",
                json.dumps(merged), existing["id"],
            )
        else:
            system_user = await self.conn.fetchval(
                "SELECT id FROM users WHERE role = 'system_admin' ORDER BY created_at LIMIT 1"
            )
            if system_user:
                await self.conn.execute(
                    "INSERT INTO stage_data (loan_id, stage, data_json, saved_by) VALUES ($1, 'ocr_review', $2::jsonb, $3)",
                    loan_id, json.dumps(high_conf), system_user,
                )
