from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]


def test_guarantor_signing_endpoints_are_explicitly_retired():
    source = (ROOT / "backend/app/domains/loans/router.py").read_text(encoding="utf-8")
    assert '@router.get("/guarantor-access/{token}")' in source
    assert '@router.get("/client-form/apply/guarantors/sign")' in source
    assert '@router.post("/client-form/apply/guarantors/sign")' in source
    assert source.count("Guarantor signing is no longer part of the application process.") >= 3
    assert source.count("status_code=status.HTTP_410_GONE") >= 3


def test_active_guarantor_wizards_have_no_signature_controls():
    for relative in (
        "frontend/templates/shared/guarantor_wizard.html",
        "frontend/templates/shared/client_guarantor_wizard.html",
    ):
        source = (ROOT / relative).read_text(encoding="utf-8")
        assert 'name="guarantor_signature"' not in source
        assert 'name="witness_signature"' not in source
        assert "Generate Link" not in source
