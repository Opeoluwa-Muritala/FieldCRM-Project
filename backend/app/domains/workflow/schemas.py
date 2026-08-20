from datetime import datetime
from typing import Any
from uuid import UUID
from pydantic import BaseModel,Field

class StageInput(BaseModel):
    key:str=Field(pattern=r"^[a-z][a-z0-9_]*$",max_length=80); label:str=Field(min_length=2,max_length=120)
    permission:str; condition:dict[str,Any]=Field(default_factory=dict); return_stage:str|None=None
class ApprovalRuleInput(BaseModel):
    min_amount:float|None=None; max_amount:float|None=None; risk_level:str|None=None; branch_id:UUID|None=None
    collateral_type:str|None=None; customer_type:str|None=None; minimum_exception_count:int|None=None
    required_stage:str; priority:int=100
class CreditRuleInput(BaseModel):
    key:str; label:str; fact_key:str; operator:str=Field(pattern="^(lte|lt|gte|gt|eq|neq)$"); threshold:Any; severity:str="fail"
class WorkflowDefinitionInput(BaseModel):
    version_id:UUID; product_code:str; name:str=Field(min_length=2,max_length=120)
    stages:list[StageInput]=Field(min_length=1,max_length=30); approval_rules:list[ApprovalRuleInput]=Field(default_factory=list)
    credit_rules:list[CreditRuleInput]=Field(default_factory=list)
class DelegationInput(BaseModel):
    delegator_id:UUID; delegate_id:UUID; permission:str; starts_at:datetime; ends_at:datetime
class ReassignmentInput(BaseModel):
    from_user_id:UUID; to_user_id:UUID; reason:str=Field(min_length=10,max_length=500)
class CreditFacts(BaseModel): facts:dict[str,Any]
class BureauEvidenceInput(BaseModel):
    provider:str=Field(min_length=2,max_length=120);checked_at:datetime;result:str=Field(pattern="^(pass|pass_with_conditions|review_required|fail)$")
    document_id:UUID;assessment:str=Field(min_length=10,max_length=2000)
