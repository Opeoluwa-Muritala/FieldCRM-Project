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
import tempfile
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
    ("pledge_date",        r"(?:Date)[:\s]+(\d{1,2}[\-/]\d{1,2}[\-/]\d{2,4}|\d{4}-\d{2}-\d{2})", False),
    ("pledge_borrower",    r"(?:Name\s+of\s+(?:Borrower|Association)|Borrower)[:\s]+(.+)", True),
    ("pledge_amount_figs", r"(?:Facility\s+Amount|Amount\s+in\s+Figures|Loan\s+Amount)[:\s₦N]+([\d,]+(?:\.\d{1,2})?)", True),
    ("pledge_location",    r"(?:Goods\s+(?:are\s+)?Located|Shop/House\s+Address|Location)[:\s]+(.+)", False),
    ("pledge_obligor",     r"(?:Name\s+of\s+Obligor|Obligor)[:\s]+(.+)", False),
    ("item_name",          r"(?:Item|Pledged\s+Item|Property)[:\s]+(.+)", True),
    ("item_quantity",      r"(?:Quantity|Qty)[:\s]+(\d+)", False),
    ("item_description",   r"(?:Description)[:\s]+(.+)", False),
    ("serial_number",      r"(?:Serial\s+No|Serial\s+Number)[:\s]+([A-Z0-9\-]+)", False),
    ("estimated_value",    r"(?:Value|Estimated\s+Value)[:\s₦N]+([\d,]+(?:\.\d{1,2})?)", True),
    ("ncr_reg_number",     r"(?:NCR|NCR\s+Reg|Registration)[:\s]+([A-Z0-9\-/]+)", False),
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

        temp_path: Path | None = None
        if stored_path.startswith("cloudinary://"):
            try:
                import httpx
                from app.services.cloud_storage_service import signed_download_url

                public_id = stored_path.removeprefix("cloudinary://")
                download_url = signed_download_url(public_id, mime_type)
                async with httpx.AsyncClient(timeout=60.0) as client:
                    response = await client.get(download_url)
                    response.raise_for_status()
                suffix = ".pdf" if mime_type == "application/pdf" else ".png" if mime_type == "image/png" else ".jpg"
                with tempfile.NamedTemporaryFile(delete=False, suffix=suffix) as temp_file:
                    temp_file.write(response.content)
                    temp_path = Path(temp_file.name)
                abs_path = str(temp_path)
            except Exception as exc:
                log.warning("Unable to retrieve cloud document for OCR: %s", type(exc).__name__)
                await self.conn.execute(
                    "UPDATE documents SET ocr_status = 'failed' WHERE id = $1", document_id
                )
                return None
        else:
            rel = stored_path.lstrip("/").replace("static/uploads/", "")
            abs_path = str(Path(upload_dir) / rel)

        await self.conn.execute(
            "UPDATE documents SET ocr_status = 'processing' WHERE id = $1", document_id
        )

        try:
            text, page_count = await asyncio.get_event_loop().run_in_executor(None, extract_text_from_file, abs_path, mime_type)
        except Exception as e:
            log.error("OCR text extraction failed: %s", e)
            await self.conn.execute(
                "UPDATE documents SET ocr_status = 'failed' WHERE id = $1", document_id
            )
            return None
        finally:
            if temp_path:
                temp_path.unlink(missing_ok=True)

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
        elif form_type == "pledge_receipt":
            await self._autofill_pledge(loan_id, document_id, fields)

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

    async def _autofill_pledge(self, loan_id: UUID, document_id: UUID, fields: list[dict]) -> None:
        """Merge high-confidence pledge OCR values into the Step 8 intake fields."""
        extracted = {
            field["field_name"]: field["ocr_value"]
            for field in fields
            if field["confidence"] >= 80.0 and field["ocr_value"]
        }
        if not extracted:
            return

        scalar_fields = {
            "pledge_date", "pledge_borrower", "pledge_amount_figs",
            "pledge_location", "pledge_obligor", "ncr_reg_number",
        }
        pledge_data = {key: extracted[key] for key in scalar_fields if key in extracted}
        if raw_date := pledge_data.get("pledge_date"):
            date_match = re.fullmatch(r"(\d{1,2})[\-/](\d{1,2})[\-/](\d{2,4})", raw_date)
            if date_match:
                day, month, year = date_match.groups()
                pledge_data["pledge_date"] = f"{('20' + year) if len(year) == 2 else year}-{int(month):02d}-{int(day):02d}"
        if "pledge_amount_figs" in pledge_data:
            pledge_data["pledge_amount_figs"] = pledge_data["pledge_amount_figs"].replace(",", "")
        repeated_fields = {
            "item_name": "pledge_item_name",
            "item_quantity": "pledge_item_qty",
            "item_description": "pledge_item_desc",
            "estimated_value": "pledge_item_val",
        }
        for source, target in repeated_fields.items():
            if source in extracted:
                value = extracted[source]
                if source == "estimated_value":
                    value = value.replace(",", "")
                pledge_data[target] = [value]
        if "serial_number" in extracted and "pledge_item_desc" not in pledge_data:
            pledge_data["pledge_item_desc"] = [f"Serial number: {extracted['serial_number']}"]

        pledge_data["_pledge_ocr_source"] = str(document_id)
        pledge_data["_pledge_ocr_requires_review"] = True
        existing = await self.conn.fetchrow(
            "SELECT id, data_json FROM stage_data WHERE loan_id = $1 AND stage = 'intake' ORDER BY saved_at DESC LIMIT 1",
            loan_id,
        )
        if not existing:
            return
        merged = dict(existing["data_json"] or {})
        for key, value in pledge_data.items():
            if key.startswith("_") or not merged.get(key):
                merged[key] = value
        await self.conn.execute(
            "UPDATE stage_data SET data_json = $1::jsonb, saved_at = CURRENT_TIMESTAMP WHERE id = $2",
            json.dumps(merged), existing["id"],
        )
