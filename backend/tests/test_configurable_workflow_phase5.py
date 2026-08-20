from pathlib import Path
from types import SimpleNamespace
from uuid import uuid4
import pytest
from app.core.exceptions import DomainException
from app.core.loan_authorization import canonical_role
from app.domains.workflow.engine import WorkflowEngine,condition_matches

def test_conditions_and_multi_factor_approval_matrix_are_data_driven():
    assert condition_matches({"fact":"amount","operator":"gte","value":10_000_000},{"amount":12_000_000})
    rules=[{"min_amount":0,"max_amount":10_000_000,"required_stage_key":"ed_approval","priority":10},
           {"min_amount":10_000_000,"max_amount":None,"required_stage_key":"md_approval","priority":10}]
    assert WorkflowEngine.approval_stages(rules,{"amount":9_999_999})==["ed_approval"]
    assert WorkflowEngine.approval_stages(rules,{"amount":10_000_000})==["md_approval"]

def test_explicit_executive_director_compatibility_alias():
    assert canonical_role("EXECUTIVE")=="ed"

class Conn:
    def __init__(self,prior=None):self.prior=prior;self.inserted=[]
    async def fetchval(self,*args):return self.prior
    async def execute(self,*args):self.inserted.append(args);return "INSERT 0 1"

@pytest.mark.asyncio
async def test_maker_checker_rejects_same_actor_originating_and_approving():
    with pytest.raises(DomainException,match="Maker-checker"):
        await WorkflowEngine(Conn("originate")).record_action(uuid4(),uuid4(),uuid4(),"approve")

@pytest.mark.asyncio
async def test_maker_checker_records_compatible_first_action():
    conn=Conn();await WorkflowEngine(conn).record_action(uuid4(),uuid4(),uuid4(),"recommend")
    assert conn.inserted

def test_phase5_schema_is_reversible_and_effective_version_is_snapshotted():
    root=Path(__file__).resolve().parents[1]
    up=(root/"migrations/046_configurable_workflow_permissions.sql").read_text(encoding="utf-8")
    down=(root/"migrations/046_configurable_workflow_permissions.rollback.sql").read_text(encoding="utf-8")
    engine=(root/"app/domains/workflow/engine.py").read_text(encoding="utf-8")
    assert "application_workflow_snapshots" in up and "staff_delegations" in up and "credit_rules" in up
    assert "DROP TABLE IF EXISTS permissions" in down
    assert "originated_config_version_id" in engine

def test_permission_bundles_cover_required_examples():
    sql=(Path(__file__).resolve().parents[1]/"migrations/046_configurable_workflow_permissions.sql").read_text(encoding="utf-8")
    for permission in ("customer:create","application:submit","credit:review","loan:approve","document:verify","visit:create","audit:view","config:manage"):
        assert permission in sql
