import io
from pathlib import Path
from types import SimpleNamespace
from uuid import uuid4

import pytest
from PIL import Image

from app.core.exceptions import DomainException
from app.domains.products.quality import assess_image_quality
from app.domains.products.schemas import ProductDefinition
from app.domains.products.service import ProductService


def product_payload():
    return {
        "code": "sme_green", "name": "SME Green Facility", "family": "business",
        "customer_segment": "corporate", "min_amount": 100000, "max_amount": 5000000,
        "interest_parameters": {"calculation_type": "reducing", "rate": 18},
        "min_tenor_months": 3, "max_tenor_months": 24, "repayment_frequency": "monthly",
        "guarantor_count": 2, "collateral_required": True,
        "workflow_stages": ["intake", "credit_analyst_review", "ed_approval"], "sla_hours": 48,
        "sections": [{"section_key": "business", "requirement": "required"}],
        "documents": [{"doc_type": "cac", "display_name": "CAC registration", "mandatory": True}],
        "fields": [{"section_key": "business", "field_key": "business_age", "label": "Business age",
                    "field_type": "number", "requirement": "required", "validation_rules": {"min": 1}}],
    }


class Tx:
    async def __aenter__(self): return self
    async def __aexit__(self, *args): return False


class Repo:
    def __init__(self, draft=True): self.draft=draft; self.created=None; self.conn=SimpleNamespace(transaction=lambda: Tx())
    async def draft_version(self, version_id, org_id): return {"id": version_id} if self.draft else None
    async def create(self, org_id, version_id, definition): self.created=definition; return {"code": definition.code}


@pytest.mark.asyncio
async def test_new_product_with_documents_and_fields_requires_no_code_change():
    repo = Repo()
    definition = ProductDefinition.model_validate(product_payload())
    row = await ProductService(repo).create(uuid4(), uuid4(), definition)
    assert row["code"] == "sme_green"
    assert repo.created.documents[0].doc_type == "cac"
    assert repo.created.fields[0].field_key == "business_age"


@pytest.mark.asyncio
async def test_product_writes_are_rejected_outside_draft_configuration():
    with pytest.raises(DomainException, match="draft configuration"):
        await ProductService(Repo(draft=False)).create(uuid4(), uuid4(), ProductDefinition.model_validate(product_payload()))


@pytest.mark.asyncio
async def test_hidden_section_cannot_contain_required_field():
    payload = product_payload()
    payload["sections"] = [{"section_key": "business", "requirement": "hidden"}]
    definition = ProductDefinition.model_validate(payload)
    with pytest.raises(DomainException, match="hidden section"):
        await ProductService(Repo()).create(uuid4(), uuid4(), definition)


def test_generic_renderer_supports_every_required_field_type():
    schema = (Path(__file__).resolve().parents[1] / "app/domains/products/schemas.py").read_text(encoding="utf-8")
    template = (Path(__file__).resolve().parents[2] / "frontend/templates/shared/new_application.html").read_text(encoding="utf-8")
    for kind in ("text","number","currency","date","dropdown","checkbox","yes_no","photo","file","signature","gps"):
        assert f'"{kind}"' in schema
    assert "configured-field" in template and "visibility_condition" in template
    assert "dynamic_uploads" in (Path(__file__).resolve().parents[1] / "app/domains/loans/router.py").read_text(encoding="utf-8")


def test_configuration_admin_gets_a_structured_product_form_not_raw_json():
    template = (Path(__file__).resolve().parents[2] / "frontend/templates/configuration/products.html").read_text(encoding="utf-8")
    for field in ("code", "name", "min_amount", "max_amount", "min_tenor_months",
                  "max_tenor_months", "repayment_frequency", "workflow_stages"):
        assert f'name="{field}"' in template
    assert 'name="definition_json"' not in template
    assert "Planned" not in template


def test_product_form_and_document_configuration_pages_are_editable():
    root = Path(__file__).resolve().parents[2] / "frontend/templates/configuration"
    forms = (root / "forms.html").read_text(encoding="utf-8")
    documents = (root / "documents.html").read_text(encoding="utf-8")
    product_edit = (root / "product_edit.html").read_text(encoding="utf-8")
    assert 'action="/configuration/forms"' in forms
    assert "/configuration/forms/{{ field.id }}" in forms
    assert "validation_key" in forms and "visibility_condition" in forms
    assert 'action="/configuration/documents"' in documents
    assert "/configuration/documents/{{ document.id }}" in documents
    assert "/configuration/products/{{ product.code }}/edit" in product_edit


def test_conditional_visibility_and_server_required_validation():
    service = ProductService(Repo())
    field = {"field_key": "cac_number", "label": "CAC number", "requirement": "required",
             "visibility_condition": {"field": "registered", "equals": "yes"}, "validation_rules": {"pattern": r"RC[0-9]+"}}
    assert service.validate_values([field], {"registered": "no"}) == []
    assert "required" in service.validate_values([field], {"registered": "yes"})[0]
    assert "invalid format" in service.validate_values([field], {"registered": "yes", "cac_number": "bad"})[0]


def test_image_quality_gate_detects_unreadable_capture():
    image = Image.new("RGB", (120, 120), "white")
    output = io.BytesIO(); image.save(output, "JPEG")
    result = assess_image_quality(output.getvalue(), "image/jpeg")
    assert result["status"] in {"needs_review", "rejected"}
    assert "blur" in result["issues"] and "cropping_or_low_resolution" in result["issues"]


def test_phase4_migration_down_and_submission_gate_are_present():
    root = Path(__file__).resolve().parents[1]
    up = (root / "migrations/045_configurable_products_forms.sql").read_text(encoding="utf-8")
    down = (root / "migrations/045_configurable_products_forms.rollback.sql").read_text(encoding="utf-8")
    routes = (root / "app/domains/loans/router.py").read_text(encoding="utf-8")
    assert "product_form_fields" in up and "document_quality_assessments" in up
    assert "DROP TABLE IF EXISTS product_form_fields" in down
    assert routes.count("DynamicReadinessService(conn).require_ready") == 2
