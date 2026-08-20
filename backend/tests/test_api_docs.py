from starlette.requests import Request

from app.core.api_docs import redoc_response, swagger_ui_response


def test_swagger_ui_bootstrap_scripts_use_request_nonce():
    request = Request({
        "type": "http",
        "method": "GET",
        "path": "/api/docs",
        "headers": [],
        "state": {"csp_nonce": "test-nonce"},
    })

    response = swagger_ui_response(
        request,
        openapi_url="/openapi.json",
        title="FieldCRM - Swagger UI",
    )
    body = response.body.decode("utf-8")

    assert 'src="/static/js/swagger-ui-bundle.js"' in body
    assert 'href="/static/css/swagger-ui.css"' in body
    assert "cdn.jsdelivr.net" not in body
    assert body.count('nonce="test-nonce"') == 2
    assert "url: '/openapi.json'" in body


def test_redoc_uses_local_bundle_and_request_nonce():
    request = Request({
        "type": "http",
        "method": "GET",
        "path": "/api/redoc",
        "headers": [],
        "state": {"csp_nonce": "test-nonce"},
    })
    response = redoc_response(request, openapi_url="/openapi.json", title="FieldCRM - ReDoc")
    body = response.body.decode("utf-8")

    assert 'src="/static/js/redoc.standalone.js"' in body
    assert 'nonce="test-nonce"' in body
    assert "cdn.jsdelivr.net" not in body
    assert "fonts.googleapis.com" not in body
    assert 'spec-url="/openapi.json"' in body
