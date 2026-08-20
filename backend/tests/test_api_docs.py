from starlette.requests import Request

from collections import Counter

import pytest
from fastapi import HTTPException

from app.core.api_docs import (
    OPENAPI_TAG_GROUPS,
    OPENAPI_TAGS,
    redoc_response,
    require_local_docs_access,
    swagger_ui_response,
)
from app.main import app


def _docs_request(host: str, client: str) -> Request:
    return Request({
        "type": "http", "method": "GET", "path": "/api/docs", "scheme": "http",
        "server": (host, 8000), "client": (client, 5000),
        "headers": [(b"host", host.encode("ascii"))],
    })


def test_documentation_requires_loopback_host_and_peer():
    require_local_docs_access(_docs_request("localhost", "127.0.0.1"))
    with pytest.raises(HTTPException) as exc:
        require_local_docs_access(_docs_request("localhost", "192.0.2.10"))
    assert exc.value.status_code == 404
    with pytest.raises(HTTPException) as exc:
        require_local_docs_access(_docs_request("dev.example", "127.0.0.1"))
    assert exc.value.status_code == 404


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


def test_openapi_is_api_only_fully_grouped_and_easy_to_scan():
    app.openapi_schema = None
    schema = app.openapi()
    assert schema["paths"]
    assert all(path.startswith("/api/v1/") for path in schema["paths"])

    tag_counts = Counter()
    declared_tags = {tag["name"] for tag in OPENAPI_TAGS}
    for path_item in schema["paths"].values():
        for method, operation in path_item.items():
            if method.lower() not in {"get", "post", "put", "patch", "delete", "options", "head"}:
                continue
            assert len(operation.get("tags", [])) == 1
            assert operation["tags"][0] in declared_tags
            tag_counts[operation["tags"][0]] += 1

    assert "Untagged" not in tag_counts
    assert "Mobile API" not in tag_counts
    assert max(tag_counts.values()) <= 18

    grouped_tags = [tag for group in OPENAPI_TAG_GROUPS for tag in group["tags"]]
    assert len(grouped_tags) == len(set(grouped_tags))
    assert set(grouped_tags) == declared_tags
    assert max(len(group["tags"]) for group in OPENAPI_TAG_GROUPS) <= 4
    assert schema["x-tagGroups"] == OPENAPI_TAG_GROUPS
