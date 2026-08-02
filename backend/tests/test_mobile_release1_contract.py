from pathlib import Path

from app.api.v1.mobile import CreateApplicationRequest, _role, router


ROOT = Path(__file__).resolve().parents[2]


def test_mobile_signed_intake_submission_is_json_endpoint():
    routes = {
        (method, route.path)
        for route in router.routes
        for method in getattr(route, "methods", set())
    }
    assert (
        "POST",
        "/applications/{application_id}/submit-to-branch-manager",
    ) in routes


def test_release2_mobile_json_contracts_are_registered():
    routes = {
        (method, route.path)
        for route in router.routes
        for method in getattr(route, "methods", set())
    }
    expected = {
        ("POST", "/applications/{application_id}/credit-bureau-pull"),
        ("GET", "/applications/{application_id}/credit-checklist"),
        ("PATCH", "/applications/{application_id}/credit-checklist"),
        ("POST", "/applications/{application_id}/client-link"),
        ("POST", "/applications/{application_id}/guarantor-link/{slot}"),
        ("GET", "/applications/{application_id}/offer"),
        ("POST", "/applications/{application_id}/offer"),
        ("GET", "/applications/{application_id}/disbursement"),
        ("POST", "/applications/{application_id}/disbursement"),
        ("POST", "/settings/change-password"),
        ("GET", "/system-activity"),
    }
    assert expected <= routes


def test_release3_mobile_json_contracts_are_registered():
    routes = {
        (method, route.path)
        for route in router.routes
        for method in getattr(route, "methods", set())
    }
    expected = {
        ("GET", "/queues/legal"),
        ("GET", "/applications/{application_id}/valuation"),
        ("PUT", "/applications/{application_id}/valuation"),
        ("GET", "/mcc"),
        ("GET", "/applications/{application_id}/mcc"),
        ("POST", "/applications/{application_id}/mcc-vote"),
        ("POST", "/applications/{application_id}/mcc-finalize"),
        ("GET", "/admin/interest-presets"),
        ("POST", "/admin/interest-presets"),
        ("DELETE", "/admin/interest-presets/{preset_id}"),
        ("PUT", "/admin/interest-presets/{preset_id}"),
        ("GET", "/branches"),
        ("POST", "/branches"),
        ("PUT", "/users/{user_id}/role"),
        ("POST", "/users/{user_id}/deactivate"),
        ("POST", "/applications/{application_id}/crm-documents"),
        ("GET", "/reports/par/loans"),
        ("GET", "/dashboards/{role_name}"),
    }
    assert expected <= routes


def test_mobile_creation_accepts_client_request_id():
    field = CreateApplicationRequest.model_fields["client_request_id"]
    assert field.is_required() is False


def test_existing_customer_application_contracts_are_registered():
    routes = {
        (method, route.path)
        for route in router.routes
        for method in getattr(route, "methods", set())
    }
    assert ("GET", "/borrowers/search") in routes
    assert ("GET", "/borrowers/{borrower_id}/application-profile") in routes
    assert CreateApplicationRequest.model_fields["borrower_id"].is_required() is False


def test_current_roles_are_not_remapped_to_legacy_roles():
    user = type("User", (), {"role": "Head CRM"})()
    assert _role(user) == "head_crm"


def test_mobile_idempotency_migration_is_reversible():
    up = (ROOT / "backend/migrations/025_mobile_creation_idempotency.sql").read_text()
    down = (ROOT / "backend/migrations/025_mobile_creation_idempotency.rollback.sql").read_text()
    assert "org_id, client_request_id" in up
    assert "DROP COLUMN IF EXISTS client_request_id" in down
