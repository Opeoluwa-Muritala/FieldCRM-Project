from starlette.requests import Request

from app import main


def make_request(path: str = "/") -> Request:
    request = Request({
        "type": "http", "method": "GET", "path": path,
        "raw_path": path.encode(), "query_string": b"", "headers": [],
        "scheme": "https", "server": ("fieldcrm.example", 443),
        "client": ("127.0.0.1", 1234), "root_path": "",
        "app": main.app,
    })
    request.state.csp_nonce = "test-nonce"
    return request


async def response_body(response) -> str:
    return response.body.decode("utf-8")


def test_demo_route_is_not_registered():
    assert not any(
        getattr(route, "path", "") == "/demo" for route in main.app.routes
    )


async def test_home_renders_public_product_and_preview_metadata(monkeypatch):
    monkeypatch.setattr(main.settings, "ANDROID_APK_URL", "")
    response = await main.root_view(make_request())
    body = await response_body(response)
    assert response.status_code == 200
    assert "One accountable process" in body
    assert "fieldcrm-social-preview.png" in body
    assert "Android release coming soon" in body
    assert "Staff login" in body
    assert "Field-ready capture" in body
    assert "Contextual documents" in body
    assert "â" not in body


async def test_home_labels_debug_channel_as_preview(monkeypatch):
    monkeypatch.setattr(main.settings, "ANDROID_APK_URL", "https://github.com/example/app.apk")
    monkeypatch.setattr(main.settings, "ANDROID_APK_CHANNEL", "preview")
    response = await main.root_view(make_request())
    body = await response_body(response)
    assert "Download Preview APK" in body
    assert "Android debug certificate" in body


async def test_android_download_redirects_to_configured_release(monkeypatch):
    monkeypatch.setattr(main.settings, "ANDROID_APK_URL", "https://res.cloudinary.com/fieldcrm-test/raw/upload/fieldcrm.apk")
    response = await main.download_android()
    assert response.status_code == 307
    assert response.headers["location"].startswith("https://res.cloudinary.com/")
    assert response.headers["cache-control"] == "no-store"


async def test_android_download_is_404_until_release_exists(monkeypatch):
    monkeypatch.setattr(main.settings, "ANDROID_APK_URL", "")
    try:
        await main.download_android()
    except Exception as exc:
        assert exc.status_code == 404
    else:
        raise AssertionError("Expected unavailable release to return 404")


async def test_robots_and_sitemap_reference_public_origin():
    robots = await main.robots_txt(make_request("/robots.txt"))
    assert "Allow: /" in robots
    assert "https://fieldcrm.example/sitemap.xml" in robots

    sitemap = await main.sitemap_xml(make_request("/sitemap.xml"))
    assert sitemap.media_type == "application/xml"
    assert "https://fieldcrm.example/" in sitemap.body.decode()


async def test_terms_page_is_reachable_and_uses_home_header():
    response = await main.terms_view(make_request("/terms"))
    body = await response_body(response)
    assert response.status_code == 200
    assert "Terms for using FieldCRM" in body
    assert "Mainstreet Microfinance Bank" in body
    assert 'href="/">Home</a>' in body
    assert response.headers["cache-control"].startswith("public")


async def test_public_information_pages_are_reachable_and_documented():
    pages = (
        (main.platform_view, "/platform", "One dossier"),
            (main.controls_view, "/controls", "Controls recorded"),
        (main.privacy_view, "/privacy", "Nigeria Data Protection Act 2023"),
    )
    for view, path, marker in pages:
        response = await view(make_request(path))
        body = await response_body(response)
        assert response.status_code == 200
        assert marker in body
        assert response.headers["cache-control"].startswith("public")


async def test_terms_cover_nigerian_legal_and_operational_requirements():
    response = await main.terms_view(make_request("/terms"))
    body = await response_body(response)
    for marker in (
        "Nigeria Data Protection Act 2023",
        "Cybercrimes",
        "CBN Consumer Protection Regulations 2019",
        "Governing law",
        "Electronic records",
        "Prohibited conduct",
    ):
        assert marker in body
    assert "Legal review notice" in body


async def test_public_pages_share_navigation_footer_and_scroll_motion():
    rendered = []
    for view, path in (
        (main.root_view, "/"),
        (main.platform_view, "/platform"),
        (main.controls_view, "/controls"),
        (main.privacy_view, "/privacy"),
        (main.terms_view, "/terms"),
    ):
        response = await view(make_request(path))
        rendered.append(await response_body(response))

    expected_nav = '<nav aria-label="Public navigation"><a href="/">Home</a><a href="/platform">Platform</a><a href="/#workflow">Workflow</a><a href="/#mobile-app">Mobile</a><a href="/controls">Controls</a><a href="/#help">Help</a></nav>'
    expected_footer_links = '<a href="/">Home</a><a href="/platform">Platform</a><a href="/controls">Controls</a><a href="/privacy">Privacy</a><a href="/terms">Terms</a><a href="/login">Staff login</a>'
    for body in rendered:
        assert expected_nav in body
        assert expected_footer_links in body
        assert "public-site.js" in body


async def test_home_is_edge_cacheable(monkeypatch):
    monkeypatch.setattr(main.settings, "ANDROID_APK_CHANNEL", "preview")
    response = await main.root_view(make_request())
    assert "s-maxage=3600" in response.headers["cache-control"]
