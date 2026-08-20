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

    assert 'src="https://cdn.jsdelivr.net/npm/swagger-ui-dist@5/swagger-ui-bundle.js"' in body
    assert body.count('nonce="test-nonce"') == 2
    assert "url: '/openapi.json'" in body
