from typing import Optional, List, Any, Dict
from datetime import datetime
from uuid import UUID
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
    officer_recommendation: Optional[str] = None

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

class LoanRow(BaseModel):
    id: UUID
    org_id: UUID
    ref_no: str
    customer_type: str
    loan_type: str
    stage: str
    applicant_name: str
    bvn: Optional[str] = None
    phone: Optional[str] = None
    amount: Optional[float] = None
    tenor_months: Optional[int] = None
    purpose: Optional[str] = None
    repayment_mode: Optional[str] = None
    created_by: UUID
    current_owner_id: Optional[UUID] = None
    credit_officer_id: Optional[UUID] = None
    branch_manager_id: Optional[UUID] = None
    return_reason: Optional[str] = None
    returned_at: Optional[datetime] = None
    approved_by: Optional[UUID] = None
    approved_at: Optional[datetime] = None
    disbursed_at: Optional[datetime] = None
    ed_escalated_to_md: bool = False
    ed_approved_by: Optional[UUID] = None
    md_approved_by: Optional[UUID] = None
    md_approved_at: Optional[datetime] = None
    md_notes: Optional[str] = None
    created_at: datetime
    updated_at: datetime

    # Join fields
    created_by_name: Optional[str] = None
    created_by_role: Optional[str] = None
    current_owner_name: Optional[str] = None
    credit_officer_name: Optional[str] = None
    branch_manager_name: Optional[str] = None

    # Compatibility layer properties
    @property
    def borrower_id(self) -> str:
        return str(self.id)

    @property
    def borrower(self) -> Any:
        class MockBorrower:
            def __init__(self, name, phone):
                self.name = name
                self.phone = phone
        return MockBorrower(self.applicant_name, self.phone or "")

    @property
    def owner(self) -> Any:
        class MockOwner:
            def __init__(self, name):
                self.name = name
        return MockOwner(self.current_owner_name) if self.current_owner_name else None

    @property
    def current_owner(self) -> Any:
        return self.owner

    @property
    def current_stage(self) -> int:
        mapping = {
            'intake': 1,
            'ocr_review': 2,
            'credit_review': 3,
            'branch_approval': 4,
            'crm_review': 5,
            'ed_approval': 7,
            'md_approval': 8,
            'executive_approval': 7,
            'disbursement_ready': 9,
            'disbursed': 10,
            'returned': 11,
            'rejected': 12,
        }
        return mapping.get(self.stage, 1)

    @property
    def status(self) -> str:
        mapping = {
            'intake': 'Draft',
            'ocr_review': 'OCR Review',
            'credit_review': 'Credit Review',
            'branch_approval': 'Branch Approval',
            'crm_review': 'CRM Review',
            'ed_approval': 'ED Approval',
            'md_approval': 'MD Approval',
            'executive_approval': 'Executive Approval',
            'disbursement_ready': 'Disbursement Ready',
            'disbursed': 'Disbursed',
            'returned': 'Returned',
            'rejected': 'Rejected',
        }
        return mapping.get(self.stage, 'Draft')

    @property
    def product_type(self) -> str:
        return self.loan_type

    @property
    def tenure(self) -> int:
        return self.tenor_months or 0

    @property
    def interest_rate(self) -> float:
        return 15.0

    @property
    def repayment_frequency(self) -> str:
        return "Monthly"

    @property
    def collateral_desc(self) -> str:
        return self.purpose or ""

    @property
    def collateral_value(self) -> float:
        return 0.0

    @property
    def officer_recommendation(self) -> str:
        return ""

    class Config:
        from_attributes = True

class LoanListItem(BaseModel):
    id: UUID
    ref_no: str
    loan_type: str
    stage: str
    amount: Optional[float]
    applicant_name: str
    created_at: datetime
    updated_at: datetime
    officer_name: Optional[str] = None
    total_count: int

    @property
    def current_stage(self) -> int:
        mapping = {
            'intake': 1,
            'ocr_review': 2,
            'credit_review': 3,
            'branch_approval': 4,
            'crm_review': 5,
            'ed_approval': 7,
            'md_approval': 8,
            'executive_approval': 7,
            'disbursement_ready': 9,
            'disbursed': 10,
            'returned': 11,
            'rejected': 12,
        }
        return mapping.get(self.stage, 1)

    @property
    def status(self) -> str:
        mapping = {
            'intake': 'Draft',
            'ocr_review': 'OCR Review',
            'credit_review': 'Credit Review',
            'branch_approval': 'Branch Approval',
            'crm_review': 'CRM Review',
            'ed_approval': 'ED Approval',
            'md_approval': 'MD Approval',
            'executive_approval': 'Executive Approval',
            'disbursement_ready': 'Disbursement Ready',
            'disbursed': 'Disbursed',
            'returned': 'Returned',
            'rejected': 'Rejected',
        }
        return mapping.get(self.stage, 'Draft')

    @property
    def product_type(self) -> str:
        return self.loan_type

    @property
    def borrower(self) -> Any:
        class MockBorrower:
            def __init__(self, name):
                self.name = name
        return MockBorrower(self.applicant_name)

    @property
    def owner(self) -> Any:
        return None

    @property
    def current_owner(self) -> Any:
        return None

    class Config:
        from_attributes = True

class RepaymentScheduleRow(BaseModel):
    id: UUID
    loan_id: UUID
    org_id: UUID
    installment_no: int
    due_date: Any
    principal_due: float
    interest_due: float
    total_due: float
    created_at: datetime

    class Config:
        from_attributes = True

class RepaymentRecordRow(BaseModel):
    id: UUID
    loan_id: UUID
    org_id: UUID
    payment_date: Any
    amount_paid: float
    channel: str
    bank_ref: Optional[str] = None
    recorded_by: UUID
    created_at: datetime

    class Config:
        from_attributes = True

class RecordPaymentRequest(BaseModel):
    payment_date: str
    amount_paid: float
    channel: str
    bank_ref: Optional[str] = None

class DisburseRequest(BaseModel):
    disbursed_amount: float
    disbursement_method: str
    disbursed_bank_ref: Optional[str] = None
    payment_date: str
    interest_rate: float
    repayment_frequency: str
    schedule_method: str = "flat_rate"

class StageCount(BaseModel):
    stage: str
    count: int

class ReadinessSummary(BaseModel):
    loan_form_submitted: bool
    pledge_form_submitted: bool
    guarantor_form_submitted: bool
    guarantors_verified: int
    guarantors_required: int
    verified_docs: int
    unverified_docs: int
    total_docs: int
    low_confidence_unverified: int
    critical_unverified: int
    consent_credit_bureau: bool
    consent_credit_check: bool
    consent_gsi: bool
    consent_cheque: bool
    visitation_status: str
    manager_concurred: bool
    officer_signed_visitation: bool
    credit_reviewed: bool
    branch_approved: bool
    ready_for_approval: bool

    class Config:
        from_attributes = True
