"""PDF generation for disbursement documents, credit printouts, and CBN returns."""
from __future__ import annotations

import io
import os
import re
import zipfile
from datetime import datetime
from pathlib import Path
from typing import Optional
from html import unescape

if os.name == "nt":
    # WeasyPrint's Windows runtime is provided by MSYS2/Pango. Respect an
    # explicit deployment setting while supporting the standard local path.
    _dll_directory = os.environ.get(
        "WEASYPRINT_DLL_DIRECTORIES",
        r"C:\msys64\mingw64\bin",
    )
    if Path(_dll_directory).is_dir():
        os.environ["WEASYPRINT_DLL_DIRECTORIES"] = _dll_directory

def _to_pdf(html: str) -> bytes:
    """Render the supplied HTML and its CSS into real PDF bytes."""
    try:
        from weasyprint import HTML
        project_root = Path(__file__).resolve().parents[3]
        return HTML(string=html, base_url=str(project_root)).write_pdf()
    except Exception:
        # WeasyPrint depends on native Pango/Cairo libraries that are not
        # available in every serverless runtime (notably Vercel). Keep document
        # generation operational with a small, standards-compliant text PDF.
        # The full HTML renderer remains the preferred path where available.
        import logging
        logging.getLogger(__name__).warning(
            "WeasyPrint unavailable; using plain-text PDF fallback", exc_info=True
        )
        return _reportlab_pdf(html)


def _reportlab_pdf(html: str) -> bytes:
    """Render the offer HTML into a structured PDF without native libraries."""
    from html.parser import HTMLParser
    from xml.sax.saxutils import escape
    from reportlab.lib import colors
    from reportlab.lib.enums import TA_CENTER, TA_JUSTIFY, TA_LEFT, TA_RIGHT
    from reportlab.lib.pagesizes import A4
    from reportlab.lib.styles import ParagraphStyle, getSampleStyleSheet
    from reportlab.lib.units import mm
    from reportlab.platypus import (
        BaseDocTemplate, Frame, PageTemplate, Paragraph, Spacer, Table, TableStyle, NextPageTemplate, PageBreak,
    )

    class DocumentParser(HTMLParser):
        block_tags = {"p", "h1", "h2", "h3", "li", "div"}

        def __init__(self):
            super().__init__(convert_charrefs=True)
            self.blocks: list[tuple[str, str]] = []
            self.elements: list[tuple[str, object]] = []
            self.tables: list[list[list[str]]] = []
            self._table: list[list[str]] | None = None
            self._row: list[str] | None = None
            self._text: list[str] = []
            self._tag_stack: list[str] = []
            self._ignored = 0
            self.list_index = 0
            self.table_count = 0

        def handle_starttag(self, tag, attrs):
            tag = tag.lower()
            self._tag_stack.append(tag)
            if tag in {"style", "script"}:
                self._ignored += 1
            elif tag == "ol":
                self.list_index = 0
            elif self._ignored == 0 and tag == "table":
                self._table = []
            elif self._ignored == 0 and tag == "tr" and self._table is not None:
                self._row = []
            elif self._ignored == 0 and tag in {"td", "th"}:
                self._text = []
            elif self._ignored == 0 and tag == "br":
                self._text.append("<br/>")

        def handle_endtag(self, tag):
            tag = tag.lower()
            if self._ignored and tag in {"style", "script"}:
                self._ignored -= 1
            elif self._ignored == 0 and tag in {"td", "th"} and self._row is not None:
                self._row.append(" ".join(self._text).strip())
                self._text = []
            elif self._ignored == 0 and tag == "tr" and self._row is not None and self._table is not None:
                self._table.append(self._row)
                self._row = None
            elif self._ignored == 0 and tag == "table" and self._table is not None:
                if self._table:
                    self.table_count += 1
                    self.tables.append(self._table)
                    self.elements.append(("table", (self.table_count, self._table)))
                self._table = None
            elif self._ignored == 0 and tag in self.block_tags:
                if self._table is None:
                    text = " ".join(self._text).strip()
                    if text:
                        if tag == "li" and "ol" in self._tag_stack:
                            self.list_index += 1
                            text = f"{self.list_index}. {text}"
                        self.blocks.append((tag, text))
                        self.elements.append(("block", (tag, text)))
                    self._text = []
            if tag == "ol":
                self.list_index = 0
            if self._tag_stack:
                self._tag_stack.pop()

        def handle_data(self, data):
            if self._ignored:
                return
            self._text.append(data)

    parser = DocumentParser()
    parser.feed(html)
    styles = getSampleStyleSheet()
    
    body = ParagraphStyle("offer-body", parent=styles["BodyText"], fontName="Helvetica", fontSize=9, leading=12, alignment=TA_LEFT, spaceAfter=6)
    h1_style = ParagraphStyle("offer-h1", parent=body, fontName="Helvetica-Bold", fontSize=14, leading=18, alignment=TA_CENTER, spaceBefore=12, spaceAfter=12)
    h2_style = ParagraphStyle("offer-h2", parent=body, fontName="Helvetica-Bold", fontSize=11, leading=15, alignment=TA_CENTER, spaceBefore=10, spaceAfter=10)
    h3_style = ParagraphStyle("offer-h3", parent=body, fontName="Helvetica-Bold", fontSize=10, leading=13, alignment=TA_LEFT, spaceBefore=8, spaceAfter=6)
    
    # Custom alignment helpers for tables
    th_style = ParagraphStyle("offer-th", parent=body, fontName="Helvetica-Bold", fontSize=8, leading=10, alignment=TA_CENTER)
    td_center = ParagraphStyle("offer-td-center", parent=body, fontSize=8, leading=10, alignment=TA_CENTER)
    td_right = ParagraphStyle("offer-td-right", parent=body, fontSize=8, leading=10, alignment=TA_RIGHT)
    small = ParagraphStyle("offer-small", parent=body, fontSize=8, leading=10)

    def para(text, style=body):
        escaped = escape(text).replace("&lt;br/&gt;", "<br/>")
        restored = escaped.replace("&lt;u&gt;", "<u>").replace("&lt;/u&gt;", "</u>")
        restored = restored.replace("&lt;b&gt;", "<b>").replace("&lt;/b&gt;", "</b>")
        return Paragraph(restored, style)

    # Initialize story with template switch command for subsequent pages
    story = [NextPageTemplate("LaterPages")]
    for kind, value in parser.elements:
        if kind == "block":
            tag, text = value
            
            if text.strip() in ("Securities:", "Conditions Precedent to Drawdown:", "REPAYMENT SCHEDULE;"):
                story.append(para(text, ParagraphStyle("section-hdr", parent=body, fontName="Helvetica-Bold", fontSize=9, leading=12, spaceBefore=10, spaceAfter=4)))
            elif text.upper().startswith("OFFER LETTER FOR"):
                # Centered, bold, underlined title
                title_text = f"<u><b>{text}</b></u>"
                story.append(para(title_text, ParagraphStyle("offer-title-style", parent=body, fontName="Helvetica-Bold", fontSize=10, leading=14, alignment=TA_CENTER, spaceBefore=12, spaceAfter=12)))
            elif tag == "h1":
                story.append(para(text, h1_style))
            elif tag == "h2":
                story.append(para(text, h2_style))
            elif tag == "h3":
                story.append(para(text, h3_style))
            elif tag == "li":
                import re
                list_item_style = ParagraphStyle("offer-list-item", parent=body, leftIndent=15, spaceAfter=4)
                if re.match(r"^\d+\.", text):
                    story.append(para(text, list_item_style))
                else:
                    story.append(para("• " + text, list_item_style))
            else:
                # Custom block text style overrides for signatures / acceptance sections
                if text.startswith("TERMS ACCEPTED BY ME") or text.startswith("WITNESSED BY"):
                    story.append(para(text, ParagraphStyle("offer-acc-title", parent=body, fontName="Helvetica-Bold", fontSize=9, leading=12, spaceBefore=10, spaceAfter=4)))
                elif text.startswith("(Please sign across"):
                    story.append(para(text, ParagraphStyle("offer-acc-stamp", parent=body, fontName="Helvetica-Oblique", fontSize=8, leading=10, spaceAfter=6)))
                elif text.startswith("NAME:"):
                    story.append(para(text, ParagraphStyle("offer-acc-line", parent=body, fontSize=8.5, leading=14, spaceBefore=6, spaceAfter=6)))
                else:
                    story.append(para(text, body))
            continue
            
        table_idx, rows = value
        cols_count = len(rows[0]) if rows else 0
        if table_idx == 1:
            # 1. Header table: Name/Address left-aligned, Date right-aligned on SAME line
            data = []
            for row in rows:
                if len(row) >= 2:
                    left_para = para(row[0], ParagraphStyle("header-left", parent=body, fontName="Helvetica-Bold", fontSize=9, leading=12, alignment=TA_LEFT))
                    right_para = para(row[1], ParagraphStyle("header-right", parent=body, fontName="Helvetica-Bold", fontSize=9, leading=12, alignment=TA_RIGHT))
                    data.append([left_para, right_para])
            if data:
                printable_width = A4[0] - 30 * mm
                table = Table(data, colWidths=[printable_width * 0.7, printable_width * 0.3])
                table.setStyle(TableStyle([
                    ("VALIGN", (0, 0), (-1, -1), "TOP"),
                    ("LEFTPADDING", (0, 0), (-1, -1), 0),
                    ("RIGHTPADDING", (0, 0), (-1, -1), 0),
                    ("TOPPADDING", (0, 0), (-1, -1), 0),
                    ("BOTTOMPADDING", (0, 0), (-1, -1), 10),
                ]))
                story.append(table)
        elif cols_count == 2:
            is_sig_table = any("authorised signatory" in str(cell).lower() for row in rows for cell in row)
            if is_sig_table:
                # Signature table - split into two columns with an empty gap in the middle
                data = []
                sig_style = ParagraphStyle("offer-sig", parent=body, fontName="Helvetica-Bold", fontSize=9, leading=12, alignment=TA_LEFT)
                for row in rows:
                    if len(row) >= 2:
                        sig1 = para(row[0].upper(), sig_style)
                        sig2 = para(row[1].upper(), sig_style)
                        data.append([sig1, "", sig2])
                if data:
                    printable_width = A4[0] - 30 * mm
                    col_widths = [printable_width * 0.42, printable_width * 0.16, printable_width * 0.42]
                    table = Table(data, colWidths=col_widths)
                    table.setStyle(TableStyle([
                        ("VALIGN", (0, 0), (-1, -1), "BOTTOM"),
                        ("ALIGN", (0, 0), (-1, -1), "LEFT"),
                        ("LINEABOVE", (0, 0), (0, -1), 0.75, colors.black),
                        ("LINEABOVE", (2, 0), (2, -1), 0.75, colors.black),
                        ("TOPPADDING", (0, 0), (-1, -1), 6), # sit close beneath the line
                        ("BOTTOMPADDING", (0, 0), (-1, -1), 4),
                        ("LEFTPADDING", (0, 0), (-1, -1), 0),
                        ("RIGHTPADDING", (0, 0), (-1, -1), 0),
                    ]))
                    story.extend([Spacer(1, 30), table, Spacer(1, 10)])
            else:
                # Terms and conditions table: borderless with 32% label width
                data = []
                for row in rows:
                    if len(row) >= 2:
                        label_para = para(row[0], ParagraphStyle("offer-td-bold", parent=body, fontName="Helvetica-Bold", fontSize=8.5, leading=11))
                        val_para = para(row[1], ParagraphStyle("offer-td-val", parent=body, fontSize=8.5, leading=11))
                        data.append([label_para, val_para])
                if data:
                    printable_width = A4[0] - 30 * mm
                    table = Table(data, colWidths=[printable_width * 0.32, printable_width * 0.68])
                    table.setStyle(TableStyle([
                        ("VALIGN", (0, 0), (-1, -1), "TOP"),
                        ("LEFTPADDING", (0, 0), (-1, -1), 0),
                        ("RIGHTPADDING", (0, 0), (-1, -1), 6),
                        ("TOPPADDING", (0, 0), (-1, -1), 3),
                        ("BOTTOMPADDING", (0, 0), (-1, -1), 3),
                    ]))
                    story.extend([Spacer(1, 4), table, Spacer(1, 8)])
        else:
            # Repayment schedule: keep light grey border grids and background with aligned cells
            data = []
            for r_idx, row in enumerate(rows):
                new_row = []
                for c_idx, cell in enumerate(row):
                    if r_idx == 0:
                        style = th_style
                    else:
                        style = td_right if c_idx in (2, 3, 4) else td_center
                    new_row.append(para(cell, style))
                data.append(new_row)
            if data:
                printable_width = A4[0] - 30 * mm
                col_widths = [printable_width * 0.12, printable_width * 0.22, printable_width * 0.22, printable_width * 0.22, printable_width * 0.22]
                table = Table(data, repeatRows=1, colWidths=col_widths)
                table.setStyle(TableStyle([
                    ("GRID", (0, 0), (-1, -1), 0.5, colors.black),
                    ("VALIGN", (0, 0), (-1, -1), "MIDDLE"),
                    ("BACKGROUND", (0, 0), (-1, 0), colors.HexColor("#f2f2f2")),
                    ("LEFTPADDING", (0, 0), (-1, -1), 4),
                    ("RIGHTPADDING", (0, 0), (-1, -1), 4),
                    ("TOPPADDING", (0, 0), (-1, -1), 4),
                    ("BOTTOMPADDING", (0, 0), (-1, -1), 4),
                ]))
                story.extend([Spacer(1, 4), table, Spacer(1, 8)])

    if len(story) <= 1:
        story.append(para("FieldCRM document"))

    buffer = io.BytesIO()
    class OfferDocTemplate(BaseDocTemplate):
        def __init__(self, filename, **kwargs):
            super().__init__(filename, **kwargs)
            # First page frame: starts lower to allow letterhead space (topMargin=50mm)
            frame_first = Frame(
                self.leftMargin,
                15 * mm,
                self.width,
                A4[1] - 50 * mm - 15 * mm,
                id="first",
                topPadding=0, bottomPadding=0, leftPadding=0, rightPadding=0
            )
            # Subsequent pages frame: standard top margin (topMargin=20mm)
            frame_later = Frame(
                self.leftMargin,
                15 * mm,
                self.width,
                A4[1] - 20 * mm - 15 * mm,
                id="later",
                topPadding=0, bottomPadding=0, leftPadding=0, rightPadding=0
            )
            self.addPageTemplates([
                PageTemplate(id="FirstPage", frames=frame_first, onPage=self._footer),
                PageTemplate(id="LaterPages", frames=frame_later, onPage=self._footer)
            ])

        def _footer(self, canvas, doc):
            canvas.saveState()
            canvas.setFont("Helvetica", 8)
            canvas.setFillColor(colors.grey)
            canvas.drawRightString(A4[0] - 15 * mm, 9 * mm, f"Page {doc.page}")
            canvas.restoreState()

    doc = OfferDocTemplate(buffer, pagesize=A4, leftMargin=15 * mm, rightMargin=15 * mm, topMargin=20 * mm, bottomMargin=15 * mm)
    doc.build(story)
    return buffer.getvalue()


def _plain_text_pdf(html: str) -> bytes:
    """Build a dependency-free PDF containing the readable document text."""
    # CSS and JavaScript blocks are implementation details, not document text.
    # Remove their complete contents before stripping the remaining markup;
    # otherwise a serverless fallback PDF visibly prints the stylesheet.
    without_code = re.sub(r"<(?:style|script)\b[^>]*>.*?</(?:style|script)>", " ", html, flags=re.I | re.S)
    text = unescape(re.sub(r"<[^>]+>", " ", without_code))
    lines = [re.sub(r"\s+", " ", line).strip() for line in text.splitlines()]
    lines = [line for line in lines if line]
    wrapped = []
    for line in lines:
        while len(line) > 90:
            wrapped.append(line[:90])
            line = line[90:]
        if line:
            wrapped.append(line)
    lines = wrapped or ["FieldCRM document"]

    # Keep the fallback intentionally simple: one page with a readable
    # Helvetica text stream and escaped PDF literals.
    content_lines = ["BT", "/F1 9 Tf", "50 760 Td", "12 TL"]
    for line in lines[:55]:
        escaped = line.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)")
        content_lines.append(f"({escaped}) Tj T*")
    content_lines.append("ET")
    stream = "\n".join(content_lines).encode("latin-1", "replace")
    objects = [
        b"<< /Type /Catalog /Pages 2 0 R >>",
        b"<< /Type /Pages /Kids [3 0 R] /Count 1 >>",
        b"<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >>",
        b"<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>",
        b"<< /Length " + str(len(stream)).encode() + b" >>\nstream\n" + stream + b"\nendstream",
    ]
    pdf = bytearray(b"%PDF-1.4\n")
    offsets = [0]
    for index, obj in enumerate(objects, 1):
        offsets.append(len(pdf))
        pdf.extend(f"{index} 0 obj\n".encode())
        pdf.extend(obj)
        pdf.extend(b"\nendobj\n")
    xref = len(pdf)
    pdf.extend(f"xref\n0 {len(objects) + 1}\n".encode())
    pdf.extend(b"0000000000 65535 f \n")
    for offset in offsets[1:]:
        pdf.extend(f"{offset:010d} 00000 n \n".encode())
    pdf.extend(
        f"trailer\n<< /Size {len(objects) + 1} /Root 1 0 R >>\nstartxref\n{xref}\n%%EOF\n".encode()
    )
    return bytes(pdf)


def _naira(amount) -> str:
    try:
        return f"\u20a6{float(amount):,.2f}"
    except (TypeError, ValueError):
        return "—"


def generate_disbursement_instruction_sheet(loan: dict, org: dict, users: dict) -> bytes:
    """Pre-filled DIS PDF for core banking operator."""
    executive = users.get("executive", {})
    officer = users.get("officer", {})
    html = f"""<!DOCTYPE html><html><head><meta charset="utf-8">
<title>Disbursement Instruction Sheet</title>
<style>
  body {{ font-family: Arial, sans-serif; font-size: 12px; margin: 40px; }}
  h1 {{ font-size: 16px; text-align: center; text-transform: uppercase; }}
  .label {{ font-weight: bold; width: 200px; display: inline-block; }}
  .row {{ margin: 8px 0; border-bottom: 1px dotted #ccc; padding-bottom: 4px; }}
  .header {{ text-align: center; margin-bottom: 24px; }}
  .sig-block {{ margin-top: 40px; display: flex; gap: 80px; }}
  .sig-line {{ border-top: 1px solid #000; width: 200px; margin-top: 40px; }}
</style></head><body>
<div class="header">
  <h1>{org.get('name', 'FieldCRM Organisation')}</h1>
  <h1>Disbursement Instruction Sheet</h1>
  <p>FieldCRM Reference: <strong>{loan.get('ref_no', '—')}</strong> &nbsp;|&nbsp; Date: <strong>{datetime.now().strftime('%d %B %Y')}</strong></p>
</div>
<div class="row"><span class="label">Borrower Name:</span> {loan.get('applicant_name', '—')}</div>
<div class="row"><span class="label">BVN:</span> {loan.get('bvn', '—')}</div>
<div class="row"><span class="label">Phone:</span> {loan.get('phone', '—')}</div>
<div class="row"><span class="label">Loan Amount:</span> {_naira(loan.get('amount'))}</div>
<div class="row"><span class="label">Tenor:</span> {loan.get('tenor_months', '—')} months</div>
<div class="row"><span class="label">Loan Type:</span> {loan.get('loan_type', '—')}</div>
<div class="row"><span class="label">Repayment Mode:</span> {loan.get('repayment_mode', '—')}</div>
<div class="row"><span class="label">Interest Rate:</span> {loan.get('interest_rate', '—')}% p.a.</div>
<div class="row"><span class="label">Loan Officer:</span> {officer.get('full_name', '—')}</div>
<div class="row"><span class="label">Authorizing Executive:</span> {executive.get('full_name', '—')}</div>
<p style="margin-top:20px;"><em>The core banking operator is hereby instructed to process the above loan disbursement. All details above are as approved in FieldCRM.</em></p>
<div class="sig-block">
  <div><div class="sig-line"></div><p>Authorizing Executive Signature</p></div>
  <div><div class="sig-line"></div><p>CRM Officer Signature</p></div>
</div>
</body></html>"""
    return _to_pdf(html)


def generate_disbursement_memo(loan: dict, org: dict, users: dict) -> bytes:
    """Formal disbursement record PDF generated after CRM confirms disbursement."""
    executive = users.get("executive", {})
    crm = users.get("crm", {})
    officer = users.get("officer", {})
    html = f"""<!DOCTYPE html><html><head><meta charset="utf-8">
<title>Disbursement Memo</title>
<style>
  body {{ font-family: Arial, sans-serif; font-size: 12px; margin: 40px; }}
  h1 {{ font-size: 15px; text-align: center; text-transform: uppercase; }}
  .label {{ font-weight: bold; width: 220px; display: inline-block; }}
  .row {{ margin: 8px 0; border-bottom: 1px dotted #ccc; padding-bottom: 4px; }}
  .header {{ text-align: center; margin-bottom: 24px; }}
  .badge {{ background: #e8f5e9; border: 1px solid #388e3c; padding: 4px 12px; border-radius: 4px; font-weight: bold; color: #1b5e20; }}
</style></head><body>
<div class="header">
  <h1>{org.get('name', 'FieldCRM Organisation')}</h1>
  <h1>Loan Disbursement Memo</h1>
  <p>Ref: <strong>{loan.get('disbursement_ref', loan.get('ref_no', '—'))}</strong> &nbsp;|&nbsp; <span class="badge">DISBURSED</span></p>
</div>
<div class="row"><span class="label">Borrower Name:</span> {loan.get('applicant_name', '—')}</div>
<div class="row"><span class="label">BVN:</span> {loan.get('bvn', '—')}</div>
<div class="row"><span class="label">Approved Loan Amount:</span> {_naira(loan.get('amount'))}</div>
<div class="row"><span class="label">Actual Disbursed Amount:</span> {_naira(loan.get('disbursed_amount'))}</div>
<div class="row"><span class="label">Disbursement Date:</span> {loan.get('disbursed_at', '—')}</div>
<div class="row"><span class="label">Payment Method:</span> {loan.get('disbursement_method', '—')}</div>
<div class="row"><span class="label">Bank Reference:</span> {loan.get('disbursed_bank_ref', '—')}</div>
<div class="row"><span class="label">Interest Rate:</span> {loan.get('interest_rate', '—')}% p.a.</div>
<div class="row"><span class="label">Repayment Frequency:</span> {loan.get('repayment_frequency', '—')}</div>
<div class="row"><span class="label">Tenor:</span> {loan.get('tenor_months', '—')} months</div>
<div class="row"><span class="label">Loan Officer:</span> {officer.get('full_name', '—')}</div>
<div class="row"><span class="label">Authorizing Executive:</span> {executive.get('full_name', '—')}</div>
<div class="row"><span class="label">CRM Officer (Executed):</span> {crm.get('full_name', '—')}</div>
<div class="row"><span class="label">Generated:</span> {datetime.now().strftime('%d %B %Y %H:%M')}</div>
</body></html>"""
    return _to_pdf(html)


def generate_credit_printout(loan: dict, org: dict, schedule: list, payments: list, collateral: list) -> bytes:
    """CBN §1.7 credit printout PDF."""
    total_due = sum(r.get("total_due", 0) for r in schedule)
    total_paid = sum(p.get("amount_paid", 0) for p in payments)
    outstanding = total_due - total_paid
    last_payment = payments[0].get("payment_date", "—") if payments else "—"

    collateral_rows = "".join(
        f"<tr><td>{c.get('item_name','—')}</td><td>{_naira(c.get('estimated_value'))}</td>"
        f"<td>{c.get('ncr_reg_number','—')}</td></tr>"
        for c in collateral
    ) or "<tr><td colspan='3'>No collateral recorded</td></tr>"

    html = f"""<!DOCTYPE html><html><head><meta charset="utf-8">
<title>Credit Printout</title>
<style>
  body {{ font-family: Arial, sans-serif; font-size: 11px; margin: 30px; }}
  h1 {{ font-size: 14px; text-align: center; }}
  table {{ width: 100%; border-collapse: collapse; margin: 10px 0; }}
  th, td {{ border: 1px solid #999; padding: 5px 8px; text-align: left; }}
  th {{ background: #f0f0f0; }}
  .label {{ font-weight: bold; width: 200px; }}
  .section {{ margin-top: 16px; font-weight: bold; font-size: 12px; border-bottom: 2px solid #333; }}
</style></head><body>
<h1>{org.get('name','—')} — Credit Printout (CBN §1.7)</h1>
<p style="text-align:center">Generated: {datetime.now().strftime('%d %B %Y %H:%M')}</p>
<div class="section">Borrower & Facility</div>
<table>
<tr><th class="label">Field</th><th>Value</th></tr>
<tr><td>Account/Ref No</td><td>{loan.get('ref_no','—')}</td></tr>
<tr><td>Borrower Name</td><td>{loan.get('applicant_name','—')}</td></tr>
<tr><td>BVN</td><td>{loan.get('bvn','—')}</td></tr>
<tr><td>Facility Type</td><td>{loan.get('loan_type','—')}</td></tr>
<tr><td>Date Granted</td><td>{loan.get('disbursed_at','—')}</td></tr>
<tr><td>Interest Rate</td><td>{loan.get('interest_rate','—')}% p.a.</td></tr>
<tr><td>Authorised Limit</td><td>{_naira(loan.get('amount'))}</td></tr>
<tr><td>Disbursed Amount</td><td>{_naira(loan.get('disbursed_amount'))}</td></tr>
<tr><td>Outstanding Balance</td><td>{_naira(outstanding)}</td></tr>
<tr><td>Tenor</td><td>{loan.get('tenor_months','—')} months</td></tr>
<tr><td>Repayment Frequency</td><td>{loan.get('repayment_frequency','—')}</td></tr>
<tr><td>Date of Last Payment</td><td>{last_payment}</td></tr>
<tr><td>Sector / Industry</td><td>{loan.get('sector','—')}</td></tr>
<tr><td>Classification</td><td>{loan.get('classification','current').upper()}</td></tr>
<tr><td>Days Past Due</td><td>{loan.get('days_past_due',0)}</td></tr>
</table>
<div class="section">Collateral</div>
<table><tr><th>Item</th><th>Value</th><th>NCR Reg No</th></tr>{collateral_rows}</table>
</body></html>"""
    return _to_pdf(html)


def generate_audit_package(loan: dict, document_paths: list[tuple[str, bytes]], memo_bytes: bytes) -> bytes:
    """Bundle all documents and disbursement memo into a zip archive."""
    buf = io.BytesIO()
    with zipfile.ZipFile(buf, "w", zipfile.ZIP_DEFLATED) as zf:
        zf.writestr("disbursement_memo.pdf", memo_bytes)
        for name, data in document_paths:
            zf.writestr(name, data)
    return buf.getvalue()


def generate_offer_letter_pdf(
    loan: dict | None = None,
    org: dict | None = None,
    rate: float | None = None,
    clauses: list[str] | None = None,
    context: dict | None = None
) -> bytes:
    if context:
        from app.core.templates import create_templates
        base_dir = os.path.dirname(os.path.abspath(__file__))
        templates_dir = os.path.abspath(os.path.join(base_dir, "../../../frontend/templates"))
        templates = create_templates(templates_dir)
        template = templates.get_template("shared/offer_letter_template.html")
        html = template.render(context)
        return _to_pdf(html)

    clause_paragraphs = "".join(f"<p style='margin: 12px 0;'>&bull; {c}</p>" for c in (clauses or []))
    html = f"""<!DOCTYPE html><html><head><meta charset="utf-8">
<title>Offer Letter</title>
<style>
  body {{ font-family: Arial, sans-serif; font-size: 12px; margin: 45px; line-height: 1.5; }}
  h1 {{ font-size: 16px; text-align: center; text-transform: uppercase; margin-bottom: 5px; }}
  h2 {{ font-size: 13px; text-align: center; text-transform: uppercase; margin-bottom: 24px; color: #333; }}
  .label {{ font-weight: bold; width: 220px; display: inline-block; }}
  .row {{ margin: 8px 0; border-bottom: 1px dotted #ccc; padding-bottom: 4px; }}
  .header {{ text-align: center; margin-bottom: 30px; }}
  .clauses-section {{ margin-top: 24px; border-top: 2px solid #000; padding-top: 12px; }}
</style></head><body>
<div class="header">
  <h1>{(org or {}).get('name', 'Mainstreet Microfinance Bank')}</h1>
  <h2>Letter of Offer for Credit Facility</h2>
  <p>Reference: <strong>{(loan or {}).get('ref_no', '—')}</strong> &nbsp;|&nbsp; Date: <strong>{datetime.now().strftime('%d %B %Y')}</strong></p>
</div>
<div class="row"><span class="label">Borrower Name:</span> {(loan or {}).get('applicant_name', '—')}</div>
<div class="row"><span class="label">Facility Limit:</span> {_naira((loan or {}).get('amount'))}</div>
<div class="row"><span class="label">Interest Rate Snapshot:</span> {rate}% p.a.</div>
<div class="row"><span class="label">Tenor:</span> {(loan or {}).get('tenor_months', '—')} months</div>
<div class="row"><span class="label">Facility Type:</span> {(loan or {}).get('loan_type', '—')}</div>
<div class="row"><span class="label">Repayment Frequency:</span> {(loan or {}).get('repayment_frequency', 'Monthly')}</div>

<div class="clauses-section">
  <h3>Terms &amp; Special Conditions</h3>
  {clause_paragraphs}
</div>

<p style="margin-top:30px;">This offer is subject to the terms and conditions outlined above. Please indicate your acceptance by signing below.</p>
<div style="margin-top: 40px; display: flex; justify-content: space-between;">
  <div>
    <div style="border-top: 1px solid #000; width: 200px; margin-top: 40px;"></div>
    <p>Authorized Bank Signatory</p>
  </div>
  <div>
    <div style="border-top: 1px solid #000; width: 200px; margin-top: 40px;"></div>
    <p>Borrower Acceptance &amp; Date</p>
  </div>
</div>
</body></html>"""
    return _to_pdf(html)


def generate_application_form_pdf(
    loan: dict,
    org: dict,
    wizard_data: dict,
    signature_event: dict | None,
    witness_event: dict | None = None,
    evidential_text: str | None = None
) -> bytes:
    # Build signature HTML
    sig_html = ""
    if signature_event and signature_event.get("signature_image_ref"):
        sig_ref = signature_event["signature_image_ref"]
        sig_html = f'<img src="{sig_ref}" style="max-height:80px; display:block;" alt="Signature">'
    else:
        sig_html = '<div style="height:80px; border-bottom:1px solid #000;"></div>'

    # Build witness signature HTML if assisted
    witness_html = ""
    if witness_event and witness_event.get("signature_image_ref"):
        wit_ref = witness_event["signature_image_ref"]
        witness_html = f"""
        <div style="margin-top: 20px; width: 250px;">
          <img src="{wit_ref}" style="max-height:80px; display:block;" alt="Witness Signature">
          <p><strong>Witness Attestation</strong></p>
          <p style="font-size:10px; color:#555;">{witness_event.get('reader_witness_attestation_text', '')}</p>
          <p>Witness: {witness_event.get('signer_identity_ref', '')}</p>
        </div>
        """

    # Evidential wording block
    evidence_block = ""
    if evidential_text:
        evidence_block = f"""
        <div style="margin-top:40px; padding:15px; border:1px solid #333; background-color:#f9f9f9; font-size:10px; line-height:1.4;">
          {evidential_text}
        </div>
        """

    html = f"""<!DOCTYPE html><html><head><meta charset="utf-8">
<title>Loan Application Form</title>
<style>
  body {{ font-family: Arial, sans-serif; font-size: 11px; margin: 40px; line-height: 1.5; }}
  h1 {{ font-size: 16px; text-align: center; text-transform: uppercase; margin-bottom: 5px; }}
  h2 {{ font-size: 13px; text-align: center; text-transform: uppercase; margin-bottom: 20px; color: #333; }}
  .section {{ margin-top: 15px; font-weight: bold; font-size: 12px; border-bottom: 1.5px solid #333; padding-bottom: 3px; }}
  table {{ width: 100%; border-collapse: collapse; margin: 10px 0; }}
  td {{ padding: 5px 8px; border: 1px solid #ccc; }}
  .label {{ font-weight: bold; width: 30%; background-color: #fcfcfc; }}
  .value {{ width: 70%; }}
</style></head><body>
<div style="text-align: center; margin-bottom: 25px;">
  <h1>{org.get('name', 'Mainstreet Microfinance Bank')}</h1>
  <h2>Loan Application Form (MMFB/CRM/01)</h2>
  <p>Reference: <strong>{loan.get('ref_no', '—')}</strong></p>
</div>

<div class="section">Personal & Contact Details</div>
<table>
  <tr><td class="label">Full Name</td><td class="value">{wizard_data.get('full_name', '—')}</td></tr>
  <tr><td class="label">Gender / Marital Status</td><td class="value">{wizard_data.get('gender', '—')} / {wizard_data.get('marital_status', '—')}</td></tr>
  <tr><td class="label">Phone / Email</td><td class="value">{wizard_data.get('phone', '—')} / {wizard_data.get('email', '—')}</td></tr>
  <tr><td class="label">BVN</td><td class="value">{wizard_data.get('bvn', '—')}</td></tr>
  <tr><td class="label">Residential Address</td><td class="value">{wizard_data.get('residential_address', '—')}</td></tr>
</table>

<div class="section">Employment / Business Details</div>
<table>
  <tr><td class="label">Employment Type</td><td class="value">{wizard_data.get('employment_type', '—')}</td></tr>
  <tr><td class="label">Employer Name / Business Name</td><td class="value">{wizard_data.get('employer_name', '') or wizard_data.get('business_name', '—')}</td></tr>
  <tr><td class="label">Monthly Income / Sales</td><td class="value">{_naira(wizard_data.get('monthly_salary') or wizard_data.get('monthly_sales'))}</td></tr>
</table>

<div class="section">Loan Details Requested</div>
<table>
  <tr><td class="label">Requested Amount</td><td class="value">{_naira(wizard_data.get('loan_amount'))}</td></tr>
  <tr><td class="label">Loan Purpose</td><td class="value">{wizard_data.get('loan_purpose', '—')}</td></tr>
  <tr><td class="label">Tenor</td><td class="value">{wizard_data.get('loan_tenor', '—')} months</td></tr>
  <tr><td class="label">Payout Bank Details</td><td class="value">{wizard_data.get('payout_bank_name', '—')} - {wizard_data.get('payout_account_number', '—')}</td></tr>
</table>

<div class="section">Declarations & Consents</div>
<table>
  <tr><td class="label">Credit Bureau Disclosure</td><td class="value">{"CONSENTED" if wizard_data.get('consent_credit_bureau') else "NOT SPECIFIED"}</td></tr>
  <tr><td class="label">Credit Check Authorisation</td><td class="value">{"CONSENTED" if wizard_data.get('consent_credit_check') else "NOT SPECIFIED"}</td></tr>
  <tr><td class="label">Cheque Recovery Authorisation</td><td class="value">{"CONSENTED" if wizard_data.get('consent_cheque') else "NOT SPECIFIED"}</td></tr>
  <tr><td class="label">GSI Mandate</td><td class="value">{"CONSENTED" if wizard_data.get('consent_gsi') else "NOT SPECIFIED"}</td></tr>
</table>

<div style="margin-top: 30px; display: flex; justify-content: space-between; align-items: flex-start; flex-wrap: wrap;">
  <div style="width: 250px;">
    {sig_html}
    <p><strong>Applicant Signature</strong></p>
    <p>Date: {datetime.now().strftime('%d %B %Y')}</p>
  </div>
  {witness_html}
</div>

{evidence_block}
</body></html>"""
    return _to_pdf(html)


def generate_guarantor_pledge_pdf(
    loan: dict,
    org: dict,
    guarantor_data: dict,
    signature_event: dict | None,
    witness_event: dict | None = None,
    evidential_text: str | None = None
) -> bytes:
    # Build signature HTML
    sig_html = ""
    if signature_event and signature_event.get("signature_image_ref"):
        sig_ref = signature_event["signature_image_ref"]
        sig_html = f'<img src="{sig_ref}" style="max-height:80px; display:block;" alt="Signature">'
    else:
        sig_html = '<div style="height:80px; border-bottom:1px solid #000;"></div>'

    # Build witness signature HTML if assisted
    witness_html = ""
    if witness_event and witness_event.get("signature_image_ref"):
        wit_ref = witness_event["signature_image_ref"]
        witness_html = f"""
        <div style="margin-top: 20px; width: 250px;">
          <img src="{wit_ref}" style="max-height:80px; display:block;" alt="Witness Signature">
          <p><strong>Witness Attestation</strong></p>
          <p style="font-size:10px; color:#555;">{witness_event.get('reader_witness_attestation_text', '')}</p>
          <p>Witness: {witness_event.get('signer_identity_ref', '')}</p>
        </div>
        """

    # Evidential wording block
    evidence_block = ""
    if evidential_text:
        evidence_block = f"""
        <div style="margin-top:40px; padding:15px; border:1px solid #333; background-color:#f9f9f9; font-size:10px; line-height:1.4;">
          {evidential_text}
        </div>
        """

    html = f"""<!DOCTYPE html><html><head><meta charset="utf-8">
<title>Guarantor Pledge Form</title>
<style>
  body {{ font-family: Arial, sans-serif; font-size: 11px; margin: 40px; line-height: 1.5; }}
  h1 {{ font-size: 16px; text-align: center; text-transform: uppercase; margin-bottom: 5px; }}
  h2 {{ font-size: 13px; text-align: center; text-transform: uppercase; margin-bottom: 20px; color: #333; }}
  .section {{ margin-top: 15px; font-weight: bold; font-size: 12px; border-bottom: 1.5px solid #333; padding-bottom: 3px; }}
  table {{ width: 100%; border-collapse: collapse; margin: 10px 0; }}
  td {{ padding: 5px 8px; border: 1px solid #ccc; }}
  .label {{ font-weight: bold; width: 30%; background-color: #fcfcfc; }}
  .value {{ width: 70%; }}
</style></head><body>
<div style="text-align: center; margin-bottom: 25px;">
  <h1>{org.get('name', 'Mainstreet Microfinance Bank')}</h1>
  <h2>Guarantor Verification & Pledge Form (MMFB/CRM/04)</h2>
  <p>Reference: <strong>{loan.get('ref_no', '—')}</strong></p>
</div>

<div class="section">Guarantor Personal Details</div>
<table>
  <tr><td class="label">Full Name</td><td class="value">{guarantor_data.get('name', '—')}</td></tr>
  <tr><td class="label">Relationship to Client</td><td class="value">{guarantor_data.get('relationship', '—')}</td></tr>
  <tr><td class="label">Phone / BVN</td><td class="value">{guarantor_data.get('phone', '—')} / {guarantor_data.get('bvn', '—')}</td></tr>
  <tr><td class="label">Residential Address</td><td class="value">{guarantor_data.get('home_address', '—')}</td></tr>
</table>

<div class="section">Guarantor Employment / Income Details</div>
<table>
  <tr><td class="label">Employment Type</td><td class="value">{guarantor_data.get('employment_type', '—')}</td></tr>
  <tr><td class="label">Employer Name / Position</td><td class="value">{guarantor_data.get('employer_name', '—')} / {guarantor_data.get('position', '—')}</td></tr>
  <tr><td class="label">Monthly Income</td><td class="value">{_naira(guarantor_data.get('monthly_salary'))}</td></tr>
</table>

<div class="section">Guarantee Pledge Terms</div>
<p style="font-size:12px; line-height:1.6; margin: 15px 0;">
  I, <strong>{guarantor_data.get('name', '[Guarantor Name]')}</strong>, hereby guarantee the repayment of the loan facility of up to 
  <strong>{_naira(guarantor_data.get('max_guarantee'))}</strong> granted to the borrower <strong>{loan.get('applicant_name', '—')}</strong>. 
  In the event of default by the borrower, I authorise Mainstreet Microfinance Bank to recover any outstanding balance from my 
  bank account <strong>{guarantor_data.get('account_number', '—')}</strong> at <strong>{guarantor_data.get('bank_name', '—')}</strong> using my cheque number 
  <strong>{guarantor_data.get('cheque_number', '—')}</strong> or any other legal means.
</p>

<div style="margin-top: 30px; display: flex; justify-content: space-between; align-items: flex-start; flex-wrap: wrap;">
  <div style="width: 250px;">
    {sig_html}
    <p><strong>Guarantor Signature</strong></p>
    <p>Date: {datetime.now().strftime('%d %B %Y')}</p>
  </div>
  {witness_html}
</div>

{evidence_block}
</body></html>"""
    return _to_pdf(html)
