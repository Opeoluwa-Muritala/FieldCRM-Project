from pathlib import Path

import pytest

from app.api.v1.mobile import router as mobile_router
from app.domains.loans.router import router as loans_router
from app.services.pdf_service import _restricted_pdf_url_fetcher


ROOT = Path(__file__).resolve().parents[2]


def _paths(router):
    return {route.path for route in router.routes}


def test_customer_only_routes_are_removed_but_staff_intake_remains():
    loan_paths = _paths(loans_router)
    mobile_paths = _paths(mobile_router)

    forbidden_fragments = (
        "/client-form/",
        "/client-access/",
        "/guarantor-access/",
        "/share-intake/",
        "/generate-share-link",
        "/client-link",
        "/guarantor-link/",
    )
    assert not any(fragment in path for path in loan_paths | mobile_paths for fragment in forbidden_fragments)
    assert "/applications/{application_id}/step/{step}" in loan_paths
    assert "/applications/{application_id}/guarantors/{guarantor_index}/step/{step}" in loan_paths


def test_customer_only_templates_are_removed_but_staff_wizards_remain():
    templates = ROOT / "frontend" / "templates" / "shared"
    assert (templates / "application_wizard.html").is_file()
    assert (templates / "guarantor_wizard.html").is_file()
    assert not list(templates.glob("client_*.html"))
    assert not (templates / "guarantor_sign.html").exists()


@pytest.mark.parametrize("url", [
    "https://example.com/tracker.png",
    "http://169.254.169.254/latest/meta-data/",
    "file:///etc/passwd",
])
def test_generated_pdfs_cannot_fetch_external_or_local_resources(url):
    with pytest.raises(ValueError, match="External resources are disabled"):
        _restricted_pdf_url_fetcher(url)


def test_offer_letter_does_not_mark_database_content_safe():
    source = (ROOT / "frontend" / "templates" / "shared" / "offer_letter_template.html").read_text(
        encoding="utf-8"
    )
    assert "|safe" not in source
