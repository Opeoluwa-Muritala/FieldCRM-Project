from app.services.pdf_service import _plain_text_pdf


def test_plain_text_pdf_excludes_stylesheet_contents():
    pdf = _plain_text_pdf(
        "<style>@page { size: A4; } .terms-table { color: red; }</style>"
        "<h1>Offer Letter</h1><p>Borrower name</p>"
    )

    assert pdf.startswith(b"%PDF-1.4")
    assert b"@page" not in pdf
    assert b"terms-table" not in pdf
    assert b"Offer Letter" in pdf
