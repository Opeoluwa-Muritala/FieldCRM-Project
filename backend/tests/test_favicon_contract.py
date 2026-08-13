from pathlib import Path


def test_shared_shell_and_root_route_use_fieldcrm_favicon():
    root = Path(__file__).resolve().parents[2]
    shell = (root / "frontend/templates/base/shell.html").read_text(encoding="utf-8")
    main = (root / "backend/app/main.py").read_text(encoding="utf-8")

    assert "/static/icons/favicon.svg?v=20260813" in shell
    assert "/static/icons/favicon.ico?v=20260813" in shell
    assert '@app.get("/favicon.ico", include_in_schema=False)' in main
    assert (root / "frontend/static/icons/favicon.ico").stat().st_size > 0
