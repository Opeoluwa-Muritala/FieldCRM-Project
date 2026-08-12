import json
from pathlib import Path


def test_vercel_function_bundles_jinja_templates_and_static_assets():
    root = Path(__file__).resolve().parents[2]
    config = json.loads((root / "vercel.json").read_text(encoding="utf-8"))
    function = config["functions"]["backend/app/main.py"]
    includes = function["includeFiles"]
    assert "frontend/templates/**" in includes
    assert "frontend/static/**" in includes
    assert (root / "frontend/templates/shared/public_home.html").is_file()
