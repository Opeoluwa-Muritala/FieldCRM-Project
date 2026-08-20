import json
from app.core.exceptions import DomainException
from app.domains.workflow.engine import OPS


class WorkflowOperations:
    def __init__(self, conn): self.conn=conn

    async def delegate(self, *, org_id, delegator_id, delegate_id, permission, starts_at, ends_at, actor_id):
        if delegator_id == delegate_id or ends_at <= starts_at: raise DomainException("Invalid delegation period or recipient.",422)
        users = await self.conn.fetchval("SELECT COUNT(*) FROM users WHERE org_id=$1 AND id=ANY($2::uuid[]) AND active=TRUE",org_id,[delegator_id,delegate_id])
        if users != 2: raise DomainException("Both delegation users must be active in this institution.",422)
        return await self.conn.fetchrow("""INSERT INTO staff_delegations(org_id,delegator_id,delegate_id,permission_code,starts_at,ends_at,created_by)
          VALUES($1,$2,$3,$4,$5,$6,$7) RETURNING *""",org_id,delegator_id,delegate_id,permission,starts_at,ends_at,actor_id)

    async def reassign(self, *, org_id, from_user, to_user, actor_id, reason):
        if from_user == to_user or len(reason.strip()) < 10: raise DomainException("Select distinct users and provide a reason.",422)
        async with self.conn.transaction():
            application_count = int((await self.conn.execute("""UPDATE loan_applications SET created_by=$1,current_owner_id=CASE WHEN current_owner_id=$2 THEN $1 ELSE current_owner_id END
              WHERE org_id=$3 AND created_by=$2 AND deleted_at IS NULL""",to_user,from_user,org_id)).split()[-1])
            visit_count = int((await self.conn.execute("UPDATE visitation_reports SET visiting_officer_id=$1 WHERE org_id=$2 AND visiting_officer_id=$3",to_user,org_id,from_user)).split()[-1])
            counts={"applications":application_count,"visits":visit_count}
            row=await self.conn.fetchrow("""INSERT INTO portfolio_reassignments(org_id,from_user_id,to_user_id,scope,counts,reason,created_by)
              VALUES($1,$2,$3,$4::jsonb,$5::jsonb,$6,$7) RETURNING *""",org_id,from_user,to_user,json.dumps(["applications","visits"]),json.dumps(counts),reason.strip(),actor_id)
        return row

    async def evaluate_credit(self, application_id, org_id, facts):
        app=await self.conn.fetchrow("SELECT loan_type,originated_config_version_id FROM loan_applications WHERE id=$1 AND org_id=$2",application_id,org_id)
        if not app: raise DomainException("Application not found.",404)
        rules=await self.conn.fetch("""SELECT * FROM credit_rules WHERE org_id=$1 AND configuration_version_id=$2
          AND (product_code IS NULL OR product_code=$3)""",org_id,app["originated_config_version_id"],app["loan_type"])
        results=[]
        for rule in rules:
            actual=facts.get(rule["fact_key"]); threshold=rule["threshold"]
            outcome="not_applicable" if actual is None else ("pass" if OPS[rule["operator"]](actual,threshold) else "fail")
            await self.conn.execute("""INSERT INTO credit_rule_results(org_id,application_id,rule_id,outcome,actual_value)
              VALUES($1,$2,$3,$4,$5::jsonb) ON CONFLICT(application_id,rule_id) DO UPDATE SET outcome=EXCLUDED.outcome,actual_value=EXCLUDED.actual_value,evaluated_at=NOW()""",org_id,application_id,rule["id"],outcome,json.dumps(actual))
            results.append({"rule_key":rule["rule_key"],"label":rule["label"],"outcome":outcome,"actual":actual,"threshold":threshold})
        return results

    async def guarantor_exposure(self, org_id, guarantor_id):
        row=await self.conn.fetchrow("SELECT bvn_lookup_hash FROM guarantors WHERE id=$1 AND org_id=$2",guarantor_id,org_id)
        if not row: raise DomainException("Guarantor not found.",404)
        exposure=await self.conn.fetchrow("""SELECT COUNT(DISTINCT g.loan_id)::int loan_count,COALESCE(SUM(la.amount),0) total_exposure,
          COUNT(DISTINCT g.loan_id) FILTER(WHERE la.stage NOT IN('rejected','returned'))::int active_guarantees
          FROM guarantors g JOIN loan_applications la ON la.id=g.loan_id WHERE g.org_id=$1 AND g.bvn_lookup_hash=$2""",org_id,row["bvn_lookup_hash"])
        result=dict(exposure); result["flagged_unusually_high"]=result["active_guarantees"]>=3 or float(result["total_exposure"])>=10_000_000
        return result
