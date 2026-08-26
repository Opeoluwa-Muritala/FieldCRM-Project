from pathlib import Path

from jinja2 import Environment, FileSystemLoader

from app.core.workflow import STAGE_DESCRIPTIONS, STAGE_LABELS, WORKFLOW_STAGES


ROOT = Path(__file__).resolve().parents[2]


def test_demo_seed_covers_authoritative_workflow_and_terminal_state():
    seed_source = (ROOT / "scripts" / "reset_online_demo.py").read_text(encoding="utf-8")

    assert "from app.core.workflow import WORKFLOW_STAGES" in seed_source
    assert 'STAGES = [stage for stage, _ in WORKFLOW_STAGES] + ["disbursed"]' in seed_source


def test_shared_pipeline_uses_real_stage_keys_instead_of_legacy_stage_numbers():
    template = (ROOT / "frontend" / "templates" / "shared" / "pipeline.html").read_text(
        encoding="utf-8"
    )

    assert "app.stage == pipeline_stage.key" in template
    assert "app.current_stage == stage_num" not in template
    assert "stage_counts" not in template

    environment = Environment(loader=FileSystemLoader(ROOT / "frontend" / "templates"))
    environment.get_template("shared/pipeline.html")


def test_pipeline_labels_cover_every_seeded_stage_and_preserve_role_acronyms():
    expected_stages = [stage for stage, _ in WORKFLOW_STAGES] + ["disbursed"]

    assert all(stage in STAGE_LABELS for stage in expected_stages)
    assert all(stage in STAGE_DESCRIPTIONS for stage in expected_stages)
    assert STAGE_LABELS["crm_review"] == "CRM Review"
    assert STAGE_LABELS["ed_approval"] == "ED Approval"
    assert STAGE_LABELS["md_approval"] == "MD Approval"


def test_pipeline_queries_remain_tenant_scoped():
    router = (ROOT / "backend" / "app" / "domains" / "loans" / "router.py").read_text(
        encoding="utf-8"
    )

    assert "repo.count_by_stage(current_user.org_id)" in router
    assert "repo.list_recent(current_user.org_id, limit=500)" in router
