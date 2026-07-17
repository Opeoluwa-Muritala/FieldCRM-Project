"""
PDF generation for disbursement documents, credit printouts, and CBN returns.
Uses weasyprint if available, falls back to plain HTML bytes.
"""
from __future__ import annotations

import io
import zipfile
from datetime import datetime
from typing import Optional


def _to_pdf(html: str) -> bytes:
    """Convert HTML string to PDF bytes. Falls back to HTML if weasyprint absent."""
    try:
        from weasyprint import HTML
        return HTML(string=html).write_pdf()
    except ImportError:
        return html.encode("utf-8")


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


def generate_offer_letter_pdf(loan: dict, org: dict, rate: float, clauses: list[str]) -> bytes:
    clause_paragraphs = "".join(f"<p style='margin: 12px 0;'>&bull; {c}</p>" for c in clauses)
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
  <h1>{org.get('name', 'Mainstreet Microfinance Bank')}</h1>
  <h2>Letter of Offer for Credit Facility</h2>
  <p>Reference: <strong>{loan.get('ref_no', '—')}</strong> &nbsp;|&nbsp; Date: <strong>{datetime.now().strftime('%d %B %Y')}</strong></p>
</div>
<div class="row"><span class="label">Borrower Name:</span> {loan.get('applicant_name', '—')}</div>
<div class="row"><span class="label">Facility Limit:</span> {_naira(loan.get('amount'))}</div>
<div class="row"><span class="label">Interest Rate Snapshot:</span> {rate}% p.a.</div>
<div class="row"><span class="label">Tenor:</span> {loan.get('tenor_months', '—')} months</div>
<div class="row"><span class="label">Facility Type:</span> {loan.get('loan_type', '—')}</div>
<div class="row"><span class="label">Repayment Frequency:</span> {loan.get('repayment_frequency', 'Monthly')}</div>

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

