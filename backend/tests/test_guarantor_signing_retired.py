from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]


def test_guarantor_signing_endpoints_are_explicitly_retired():
    source = (ROOT / "backend/app/domains/loans/router.py").read_text(encoding="utf-8")
    assert "/guarantor-access/" not in source
    assert "/client-form/" not in source


def test_active_guarantor_wizards_have_no_signature_controls():
    for relative in ("frontend/templates/shared/guarantor_wizard.html",):
        source = (ROOT / relative).read_text(encoding="utf-8")
        assert 'name="guarantor_signature"' not in source
        assert 'name="witness_signature"' not in source
        assert "Generate Link" not in source
