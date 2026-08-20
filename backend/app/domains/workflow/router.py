import json
from uuid import UUID
from fastapi import APIRouter,Depends,HTTPException,Request
from app.config import settings
from app.core.dependencies import authenticated_db_conn,get_current_user
from app.domains.configuration.access import require_restricted_configuration_access
from app.domains.workflow.dependencies import PermissionChecker
from app.domains.workflow.operations import WorkflowOperations
from app.domains.workflow.schemas import WorkflowDefinitionInput,DelegationInput,ReassignmentInput,CreditFacts,BureauEvidenceInput

router=APIRouter(prefix="/api/v1/workflow",tags=["Workflow"])
def enabled():
    if not settings.CONFIGURABLE_WORKFLOW_ENABLED: raise HTTPException(404,"Not found")

@router.post("/definitions",status_code=201)
async def create_definition(payload:WorkflowDefinitionInput,request:Request,current_user=Depends(get_current_user),conn=Depends(authenticated_db_conn)):
    enabled();require_restricted_configuration_access(request,current_user)
    async with conn.transaction():
        draft=await conn.fetchval("SELECT status FROM configuration_versions WHERE id=$1 AND org_id=$2",payload.version_id,current_user.org_id)
        if draft!="draft": raise HTTPException(409,"Workflow definitions require a draft configuration")
        workflow=await conn.fetchrow("INSERT INTO workflow_definitions(org_id,configuration_version_id,product_code,name) VALUES($1,$2,$3,$4) RETURNING *",current_user.org_id,payload.version_id,payload.product_code,payload.name)
        for position,stage in enumerate(payload.stages):
            await conn.execute("""INSERT INTO workflow_stage_definitions(workflow_id,stage_key,label,position,permission_code,condition,return_stage_key)
              VALUES($1,$2,$3,$4,$5,$6::jsonb,$7)""",workflow["id"],stage.key,stage.label,position,stage.permission,json.dumps(stage.condition),stage.return_stage)
        for rule in payload.approval_rules:
            await conn.execute("""INSERT INTO approval_matrix_rules(org_id,configuration_version_id,product_code,min_amount,max_amount,risk_level,branch_id,collateral_type,customer_type,minimum_exception_count,required_stage_key,priority)
              VALUES($1,$2,$3,$4,$5,$6,$7,$8,$9,$10,$11,$12)""",current_user.org_id,payload.version_id,payload.product_code,rule.min_amount,rule.max_amount,rule.risk_level,rule.branch_id,rule.collateral_type,rule.customer_type,rule.minimum_exception_count,rule.required_stage,rule.priority)
        for rule in payload.credit_rules:
            await conn.execute("""INSERT INTO credit_rules(org_id,configuration_version_id,product_code,rule_key,label,fact_key,operator,threshold,severity)
              VALUES($1,$2,$3,$4,$5,$6,$7,$8::jsonb,$9)""",current_user.org_id,payload.version_id,payload.product_code,rule.key,rule.label,rule.fact_key,rule.operator,json.dumps(rule.threshold),rule.severity)
    return dict(workflow)

@router.post("/delegations",status_code=201)
async def delegate(payload:DelegationInput,current_user=Depends(PermissionChecker("portfolio:reassign")),conn=Depends(authenticated_db_conn)):
    enabled();return dict(await WorkflowOperations(conn).delegate(org_id=current_user.org_id,actor_id=current_user.id,**payload.model_dump()))
@router.post("/reassign",status_code=201)
async def reassign(payload:ReassignmentInput,current_user=Depends(PermissionChecker("portfolio:reassign")),conn=Depends(authenticated_db_conn)):
    enabled();return dict(await WorkflowOperations(conn).reassign(org_id=current_user.org_id,actor_id=current_user.id,from_user=payload.from_user_id,to_user=payload.to_user_id,reason=payload.reason))
@router.post("/applications/{application_id}/credit-rules")
async def credit_rules(application_id:UUID,body:CreditFacts,current_user=Depends(PermissionChecker("credit:review")),conn=Depends(authenticated_db_conn)):
    enabled();return await WorkflowOperations(conn).evaluate_credit(application_id,current_user.org_id,body.facts)
@router.get("/guarantors/{guarantor_id}/exposure")
async def exposure(guarantor_id:UUID,current_user=Depends(PermissionChecker("credit:review")),conn=Depends(authenticated_db_conn)):
    enabled();return await WorkflowOperations(conn).guarantor_exposure(current_user.org_id,guarantor_id)
@router.post("/applications/{application_id}/bureau-evidence",status_code=201)
async def bureau_evidence(application_id:UUID,body:BureauEvidenceInput,current_user=Depends(PermissionChecker("credit:review")),conn=Depends(authenticated_db_conn)):
    enabled();document=await conn.fetchval("SELECT id FROM documents WHERE id=$1 AND loan_id=$2 AND org_id=$3 AND deleted_at IS NULL",body.document_id,application_id,current_user.org_id)
    if not document: raise HTTPException(422,"Attach a report from this application")
    row=await conn.fetchrow("""INSERT INTO manual_credit_bureau_evidence(org_id,application_id,provider,checked_at,result,document_id,assessment,recorded_by)
      VALUES($1,$2,$3,$4,$5,$6,$7,$8) RETURNING *""",current_user.org_id,application_id,body.provider,body.checked_at.date(),body.result,body.document_id,body.assessment,current_user.id)
    return dict(row)
