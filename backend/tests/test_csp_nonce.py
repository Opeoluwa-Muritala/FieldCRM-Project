import re
from pathlib import Path

from fastapi.templating import Jinja2Templates
from starlette.applications import Starlette
from starlette.requests import Request
from starlette.responses import JSONResponse
from starlette.routing import Route
from starlette.testclient import TestClient

from app.core.middleware import SecurityHeadersMiddleware
from app.core.template_utils import csp_nonce_context

TEMPLATE_DIR = Path(__file__).resolve().parents[2] / "frontend" / "templates"
templates = Jinja2Templates(directory=TEMPLATE_DIR, context_processors=[csp_nonce_context])


async def nonce_value(request: Request) -> JSONResponse:
    return JSONResponse(csp_nonce_context(request))


async def rendered_login(request: Request):
    return templates.TemplateResponse(
        request,
        "shared/login.html",
        {"error": None, "next_url": "/dashboard"},
    )


async def rendered_shell(request: Request):
    return templates.TemplateResponse(
        request,
        "base/desktop_shell.html",
        {
            "user": None,
            "active_page": "",
            "sidebar_component": "components/desktop_sidebar_loan_officer.html",
            "mobile_tabbar_component": "components/mobile_tabbar_loan_officer.html",
        },
    )


def build_client(*, enforced: bool) -> TestClient:
    app = Starlette(routes=[Route("/", nonce_value)])
    app.add_middleware(SecurityHeadersMiddleware, csp_nonce_enforced=enforced)
    return TestClient(app)


def extract_nonce(csp: str) -> str:
    match = re.search(r"'nonce-([^']+)'", csp)
    assert match is not None
    return match.group(1)


def test_nonce_is_unique_and_available_to_template_context():
    with build_client(enforced=False) as client:
        first = client.get("/")
        second = client.get("/")

    first_nonce = extract_nonce(first.headers["content-security-policy"])
    second_nonce = extract_nonce(second.headers["content-security-policy"])

    assert first.json()["csp_nonce"] == first_nonce
    assert second.json()["csp_nonce"] == second_nonce
    assert first_nonce != second_nonce


def test_strict_mode_removes_unsafe_inline_from_scripts_and_styles():
    with build_client(enforced=True) as client:
        response = client.get("/")

    csp = response.headers["content-security-policy"]
    script_src = csp.split("script-src ", 1)[1].split(";", 1)[0]
    style_src = csp.split("style-src ", 1)[1].split(";", 1)[0]

    assert "'unsafe-inline'" not in script_src
    assert "'unsafe-inline'" not in style_src
    assert f"'nonce-{extract_nonce(csp)}'" in style_src
    assert "https://cdnjs.cloudflare.com" in script_src
    assert "https://fonts.googleapis.com" in style_src


def test_rollout_mode_keeps_existing_inline_scripts_functional():
    with build_client(enforced=False) as client:
        response = client.get("/")

    script_src = response.headers["content-security-policy"].split("script-src ", 1)[1].split(";", 1)[0]
    assert "'unsafe-inline'" in script_src
    assert "'nonce-" in script_src


def test_swagger_cdn_is_allowed_only_on_the_local_docs_path():
    app = Starlette(routes=[Route("/api/docs", nonce_value), Route("/", nonce_value)])
    app.add_middleware(SecurityHeadersMiddleware, csp_nonce_enforced=True)

    with TestClient(app) as client:
        docs_response = client.get("/api/docs")
        normal_response = client.get("/")

    assert "https://cdn.jsdelivr.net" in docs_response.headers["content-security-policy"]
    assert "https://cdn.jsdelivr.net" not in normal_response.headers["content-security-policy"]


def test_strict_csp_nonce_matches_standalone_and_shell_template_scripts():
    app = Starlette(
        routes=[
            Route("/login", rendered_login),
            Route("/shell", rendered_shell),
        ]
    )
    app.add_middleware(SecurityHeadersMiddleware, csp_nonce_enforced=True)

    with TestClient(app) as client:
        responses = [client.get("/login"), client.get("/shell")]

    for response in responses:
        nonce = extract_nonce(response.headers["content-security-policy"])
        assert "'unsafe-inline'" not in response.headers["content-security-policy"].split(
            "script-src ", 1
        )[1].split(";", 1)[0]
        assert f'<script nonce="{nonce}">' in response.text


def test_every_inline_template_script_has_the_nonce_attribute():
    inline_script = re.compile(r"<script\b(?![^>]*\bsrc\s*=)[^>]*>", re.IGNORECASE)
    nonce_attribute = re.compile(
        r"""\bnonce\s*=\s*["']{{\s*csp_nonce\s*}}["']""",
        re.IGNORECASE,
    )
    missing = []

    for template_path in TEMPLATE_DIR.rglob("*.html"):
        for match in inline_script.finditer(template_path.read_text(encoding="utf-8")):
            if not nonce_attribute.search(match.group(0)):
                missing.append(f"{template_path.relative_to(TEMPLATE_DIR)}: {match.group(0)}")

    assert missing == []


def test_templates_have_no_inline_event_handler_attributes():
    event_attribute = re.compile(
        r"""<[^>]*\s(?:on[a-z]+)\s*=""",
        re.IGNORECASE,
    )
    matches = []

    for template_path in TEMPLATE_DIR.rglob("*.html"):
        text = template_path.read_text(encoding="utf-8")
        for match in event_attribute.finditer(text):
            matches.append(
                f"{template_path.relative_to(TEMPLATE_DIR)}: {match.group(0)}"
            )

    assert matches == []


def test_templates_have_no_literal_style_attributes_or_unnonced_style_blocks():
    style_attribute = re.compile(r"""<[^>]*\sstyle\s*=""", re.IGNORECASE)
    inline_style = re.compile(
        r"""<style\b(?![^>]*\bnonce\s*=\s*["']{{\s*csp_nonce\s*}}["'])""",
        re.IGNORECASE,
    )
    matches = []

    for template_path in TEMPLATE_DIR.rglob("*.html"):
        text = template_path.read_text(encoding="utf-8")
        if style_attribute.search(text):
            matches.append(f"{template_path.relative_to(TEMPLATE_DIR)}: style attribute")
        if inline_style.search(text):
            matches.append(f"{template_path.relative_to(TEMPLATE_DIR)}: unnonced style block")

    assert matches == []
