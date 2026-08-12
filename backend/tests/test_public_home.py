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


async def test_home_renders_public_product_and_preview_metadata(monkeypatch):
    monkeypatch.setattr(main.settings, "ANDROID_APK_URL", "")
    response = await main.root_view(make_request())
    body = await response_body(response)
    assert response.status_code == 200
    assert "One accountable process" in body
    assert "fieldcrm-social-preview.png" in body
    assert "Android release coming soon" in body
    assert "Staff login" in body


async def test_home_labels_debug_channel_as_preview(monkeypatch):
    monkeypatch.setattr(main.settings, "ANDROID_APK_URL", "https://github.com/example/app.apk")
    monkeypatch.setattr(main.settings, "ANDROID_APK_CHANNEL", "preview")
    response = await main.root_view(make_request())
    body = await response_body(response)
    assert "Download Preview APK" in body
    assert "Android debug certificate" in body


async def test_android_download_redirects_to_configured_release(monkeypatch):
    monkeypatch.setattr(main.settings, "ANDROID_APK_URL", "https://res.cloudinary.com/demo/raw/upload/fieldcrm.apk")
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


async def test_home_is_edge_cacheable(monkeypatch):
    monkeypatch.setattr(main.settings, "ANDROID_APK_CHANNEL", "preview")
    response = await main.root_view(make_request())
    assert "s-maxage=3600" in response.headers["cache-control"]
