from datetime import UTC, datetime
import inspect
from pathlib import Path
from types import SimpleNamespace
from uuid import uuid4

import pytest
from fastapi import HTTPException
from starlette.requests import Request

from app.core.templates import create_templates
from app.domains.loans.router import _get_dossier_context, _overview_sections


def test_overview_summary_is_curated_and_excludes_sensitive_fields():
    sections = _overview_sections({
        "phone": "08012345678",
        "business_name": "Example Stores",
        "monthly_income": "650000",
        "bvn": "12345678901",
        "nin": "10987654321",
        "bank_account_number": "0123456789",
        "borrower_pledge_signature": "data:image/png;base64,secret",
        "nested": {"secret": "value"},
    })
    rendered = str(sections)
    assert "Example Stores" in rendered
    assert "650000" in rendered
    assert "12345678901" not in rendered
    assert "10987654321" not in rendered
    assert "0123456789" not in rendered
    assert "base64" not in rendered


@pytest.mark.asyncio
async def test_overview_rejects_malformed_application_id_before_database_access():
    request = Request({"type": "http", "method": "GET", "path": "/applications/not-a-uuid/view", "headers": []})
    with pytest.raises(HTTPException) as exc:
        await _get_dossier_context(request, "not-a-uuid", None, SimpleNamespace())
    assert exc.value.status_code == 404


def test_application_overview_renders_feature_aware_secure_dossier():
    template_root = Path(__file__).resolve().parents[2] / "frontend" / "templates"
    template = create_templates(str(template_root)).env.get_template("shared/application_overview.html")
    now = datetime.now(UTC)
    application_id = uuid4()
    user = SimpleNamespace(role="auditor", name="Audit User", full_name="Audit User")
    app = SimpleNamespace(
        id=application_id, ref_no="APP-1001", applicant_name="Ada Example",
        loan_type="sme", amount=1_250_000, tenor_months=12,
        stage="credit_analyst_review", status="Credit Analyst Review",
        created_by_name="Officer One", updated_at=now,
    )
    body = template.render(
        request=Request({"type": "http", "method": "GET", "path": f"/applications/{application_id}/view", "query_string": b"", "headers": []}),
        shell="base/desktop_shell.html", user=user, current_user=user,
        sidebar_component="components/desktop_sidebar_auditor.html",
        tabbar_component="components/mobile_tabbar_auditor.html",
        app=app, app_id=str(application_id), borrower_name=app.applicant_name,
        overview_sections=[{"title": "Financial profile", "items": [{"label": "Monthly income", "value": "650000"}]}],
        documents=[{"id": uuid4(), "original_name": "Statement.pdf", "category": "bank_statement", "doc_type": "bank_statement", "status": "verified", "created_at": now, "url": "https://untrusted.example/document"}],
        activity_events=[], dynamic_readiness=None, cbs_authoritative=False,
        feature_flags={"guarantors": False, "collateral": False, "visits": False, "audit_intervention": True},
        metrics={}, csp_nonce="test-nonce",
    )
    assert "APP-1001" in body and "Ada Example" in body
    assert "application-overview.css" in body
    assert "Officer provided" in body
    assert "data-document-preview" in body
    assert "untrusted.example" not in body
    assert "/guarantors/" not in body
    assert "/collateral" not in body
    assert "/visitation" not in body


def test_product_requirements_query_is_tenant_scoped():
    source = (Path(__file__).resolve().parents[1] / "app/domains/loans/router.py").read_text(encoding="utf-8")
    assert "WHERE product_code = $1 AND org_id = $2 AND is_mandatory = TRUE" in source


def test_dossier_context_reuses_request_connection_without_pool_fanout():
    source = inspect.getsource(_get_dossier_context)
    assert "async with get_connection()" not in source
    assert "asyncio.gather(" not in source
