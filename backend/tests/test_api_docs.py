from starlette.requests import Request

from app.core.api_docs import swagger_ui_response


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
