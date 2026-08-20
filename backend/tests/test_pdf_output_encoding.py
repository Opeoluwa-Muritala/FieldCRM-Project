import io
import zipfile

from app.services import pdf_service


def _capture_html(monkeypatch):
    monkeypatch.setattr(pdf_service, "_to_pdf", lambda html: html.encode("utf-8"))


def test_application_and_guarantor_pdf_values_are_html_encoded(monkeypatch):
    _capture_html(monkeypatch)
    attack = '<img src=x onerror="alert(1)">'

    application = pdf_service.generate_application_form_pdf(
        {"ref_no": attack},
        {"name": attack},
        {"full_name": attack, "loan_amount": 1000},
        {"signature_image_ref": 'data:image/png;base64,x" onerror="alert(1)'},
        evidential_text=attack,
    ).decode("utf-8")
    guarantor = pdf_service.generate_guarantor_pledge_pdf(
        {"applicant_name": attack},
        {"name": attack},
        {"name": attack, "monthly_salary": 1000},
        None,
        evidential_text=attack,
    ).decode("utf-8")

    assert attack not in application
    assert attack not in guarantor
    assert "&lt;img src=x onerror=" in application
    assert "&lt;img src=x onerror=" in guarantor
    assert 'src="data:image/png;base64,x&quot; onerror=&quot;alert(1)"' in application


def test_offer_and_disbursement_pdf_values_are_html_encoded(monkeypatch):
    _capture_html(monkeypatch)
    attack = "<script>alert(1)</script>"

    offer = pdf_service.generate_offer_letter_pdf(
        {"applicant_name": attack, "amount": 1000},
        {"name": attack},
        rate=12,
        clauses=[attack],
    ).decode("utf-8")
    instruction = pdf_service.generate_disbursement_instruction_sheet(
        {"applicant_name": attack, "amount": 1000},
        {"name": attack},
        {"officer": {"full_name": attack}},
    ).decode("utf-8")

    assert attack not in offer
    assert attack not in instruction
    assert "&lt;script&gt;alert(1)&lt;/script&gt;" in offer


def test_audit_package_flattens_untrusted_archive_names():
    archive = pdf_service.generate_audit_package(
        {},
        [("../../outside.txt", b"safe"), ("folder/evidence.pdf", b"pdf")],
        b"memo",
    )

    with zipfile.ZipFile(io.BytesIO(archive)) as package:
        assert package.namelist() == ["disbursement_memo.pdf", "outside.txt", "evidence.pdf"]
