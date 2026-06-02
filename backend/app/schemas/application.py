from typing import Optional, List, Any, Dict
from datetime import datetime
from pydantic import BaseModel, Field

class LoanApplicationBase(BaseModel):
    borrower_id: str
    amount: float = Field(..., gt=0, description="Requested principal amount")
    tenure: int = Field(..., gt=0, description="Tenure value in weeks/months")
    product_type: str = Field(..., description="Individual / Group / SME / Emergency")
    interest_rate: float = Field(..., ge=0)
    repayment_frequency: str = Field(..., description="Weekly / Bi-weekly / Monthly")
    collateral_desc: Optional[str] = None
    collateral_value: Optional[float] = Field(None, ge=0)
    officer_recommendation: str = Field(..., min_length=10, description="Required officer recommendation note")

class LoanApplicationCreate(LoanApplicationBase):
    org_id: str

class LoanApplicationResponse(BaseModel):
    id: str
    org_id: str
    borrower_id: str
    current_stage: int
    current_owner_id: str
    status: str
    amount: float
    tenure: int
    product_type: str
    interest_rate: float
    repayment_frequency: str
    collateral_desc: Optional[str] = None
    collateral_value: Optional[float] = None
    officer_recommendation: Optional[str] = None
    created_at: datetime

    class Config:
        from_attributes = True

# Stage-specific data inputs
class ManagerReview(BaseModel):
    recommend_approve: bool
    note: str = Field(..., min_length=5)
    risk_flag_toggle: bool = False

class RiskAssessment(BaseModel):
    approve_risk: bool
    note: str = Field(..., min_length=5)
    debt_to_income_ratio: float = Field(..., ge=0, le=100)
    credit_score: int = Field(..., ge=0)
    bureau_check_result: str
    risk_rating: str  # Low, Medium, High

class AuditorComplianceCheck(BaseModel):
    compliance_pass: bool
    note: str = Field(..., min_length=5)
    document_checklist_verified: bool
    regulatory_flags_triggered: bool = False

class CommitteeApproval(BaseModel):
    approve: bool
    note: str = Field(..., min_length=5)
    approval_conditions: Optional[str] = None
    vote_record: str  # E.g. "3-0 Approved"

# Bidirectional returns
class WorkflowReturnRequest(BaseModel):
    target_stage: int = Field(..., ge=1, le=5, description="1: Loan Officer, 2: Branch Manager, 3: Credit Officer, 4: Auditor")
    reason: str = Field(..., min_length=10, description="Mandatory text log detailing reason for return")

# Document request (returns back to Loan Officer)
class DocumentRequest(BaseModel):
    requested_docs: List[str] = Field(..., min_items=1)
    reason: str = Field(..., min_length=10)

class WorkflowEventResponse(BaseModel):
    id: str
    application_id: str
    action: str
    from_stage: Optional[int] = None
    to_stage: int
    actor_id: str
    reason: Optional[str] = None
    timestamp: datetime

    class Config:
        from_attributes = True

class DisbursementFormUpsert(BaseModel):
    form_type: str = Field(..., description="guarantor, pledge_trust_receipt, or another configured disbursement form key")
    data: Dict[str, Any] = Field(..., description="Structured form answers captured from the disbursement document")
    completed: bool = False

class DisbursementFormResponse(BaseModel):
    application_id: str
    form_type: str
    data: Dict[str, Any]
    completed: bool
    submitted_by: str
    submitted_at: datetime
