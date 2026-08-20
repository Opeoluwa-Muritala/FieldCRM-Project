import operator

from app.core.exceptions import DomainException

OPS = {"lte": operator.le, "lt": operator.lt, "gte": operator.ge, "gt": operator.gt,
       "eq": operator.eq, "neq": operator.ne}
INCOMPATIBLE = {"originate": {"recommend","approve","disburse","reverse"},
                "recommend": {"originate","approve","disburse","reverse"},
                "approve": {"originate","recommend","disburse","reverse"},
                "disburse": {"originate","recommend","approve","reverse"},
                "reverse": {"originate","recommend","approve","disburse"}}


def condition_matches(condition: dict, facts: dict) -> bool:
    if not condition: return True
    operation = OPS.get(condition.get("operator", "eq"))
    return bool(operation and condition.get("fact") in facts and operation(facts[condition["fact"]], condition.get("value")))


class WorkflowEngine:
    def __init__(self, conn): self.conn=conn

    async def permissions(self, user, permission: str) -> bool:
        direct = await self.conn.fetchval("SELECT EXISTS(SELECT 1 FROM role_permissions WHERE role=$1 AND permission_code=$2)", user.role, permission)
        if direct: return True
        return bool(await self.conn.fetchval("""SELECT EXISTS(SELECT 1 FROM staff_delegations WHERE org_id=$1 AND delegate_id=$2
          AND permission_code=$3 AND starts_at<=NOW() AND ends_at>=NOW())""", user.org_id, user.id, permission))

    async def require_permission(self, user, permission):
        if not await self.permissions(user, permission): raise DomainException("Permission denied.", 403)

    async def record_action(self, org_id, application_id, actor_id, action_type):
        conflicts = INCOMPATIBLE[action_type]
        prior = await self.conn.fetchval("""SELECT action_type FROM application_actor_actions
          WHERE org_id=$1 AND application_id=$2 AND actor_id=$3 AND action_type=ANY($4::text[]) LIMIT 1""",
          org_id, application_id, actor_id, list(conflicts))
        if prior: raise DomainException(f"Maker-checker policy prohibits {action_type} after {prior} by the same person.", 409)
        await self.conn.execute("INSERT INTO application_actor_actions(org_id,application_id,actor_id,action_type) VALUES($1,$2,$3,$4) ON CONFLICT DO NOTHING", org_id,application_id,actor_id,action_type)

    async def snapshot(self, application_id, org_id):
        app = await self.conn.fetchrow("SELECT loan_type,originated_config_version_id FROM loan_applications WHERE id=$1 AND org_id=$2", application_id,org_id)
        if not app: raise DomainException("Application not found.",404)
        stages = await self.conn.fetch("""SELECT wsd.* FROM workflow_definitions wd JOIN workflow_stage_definitions wsd ON wsd.workflow_id=wd.id
          WHERE wd.org_id=$1 AND wd.configuration_version_id=$2 AND wd.product_code=$3 ORDER BY wsd.position""",org_id,app["originated_config_version_id"],app["loan_type"])
        rules = await self.conn.fetch("""SELECT * FROM approval_matrix_rules WHERE org_id=$1 AND configuration_version_id=$2
          AND (product_code IS NULL OR product_code=$3) ORDER BY priority""",org_id,app["originated_config_version_id"],app["loan_type"])
        import json
        await self.conn.execute("""INSERT INTO application_workflow_snapshots(application_id,org_id,configuration_version_id,stages,approval_rules)
          VALUES($1,$2,$3,$4::jsonb,$5::jsonb) ON CONFLICT(application_id) DO NOTHING""",application_id,org_id,app["originated_config_version_id"],json.dumps([dict(x) for x in stages],default=str),json.dumps([dict(x) for x in rules],default=str))

    @staticmethod
    def required_stages(stages, facts):
        return [{**stage,"required":condition_matches(dict(stage.get("condition") or {}),facts)} for stage in stages]

    @staticmethod
    def approval_stages(rules, facts):
        required=[]
        for rule in sorted(rules,key=lambda item:item.get("priority",100)):
            checks=(rule.get("min_amount") is None or facts.get("amount",0)>=float(rule["min_amount"]),
                    rule.get("max_amount") is None or facts.get("amount",0)<float(rule["max_amount"]),
                    rule.get("risk_level") is None or facts.get("risk_level")==rule["risk_level"],
                    rule.get("branch_id") is None or str(facts.get("branch_id"))==str(rule["branch_id"]),
                    rule.get("collateral_type") is None or facts.get("collateral_type")==rule["collateral_type"],
                    rule.get("customer_type") is None or facts.get("customer_type")==rule["customer_type"],
                    rule.get("minimum_exception_count") is None or facts.get("exception_count",0)>=rule["minimum_exception_count"])
            if all(checks) and rule["required_stage_key"] not in required: required.append(rule["required_stage_key"])
        return required

    @staticmethod
    def evaluate_rule(rule, actual):
        return OPS[rule["operator"]](actual, rule["threshold"])

    async def required_approval_stages_for_application(self, application, org_id):
        snapshot=await self.conn.fetchrow("SELECT approval_rules FROM application_workflow_snapshots WHERE application_id=$1 AND org_id=$2",application.id,org_id)
        if not snapshot: return []
        return self.approval_stages(list(snapshot["approval_rules"] or []),{"amount":float(application.amount or 0),"branch_id":application.branch_id,"customer_type":application.customer_type,"exception_count":0})
