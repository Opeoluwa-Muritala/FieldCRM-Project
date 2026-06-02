from typing import Optional
from datetime import datetime
from pydantic import BaseModel, Field

class RepaymentRecordCreate(BaseModel):
    schedule_id: str
    application_id: str
    amount_paid: float = Field(..., gt=0, description="Amount paid in the repayment installment")
    channel: str = Field(..., description="cash / transfer / mobile money / POS")

class RepaymentRecordResponse(BaseModel):
    id: str
    schedule_id: str
    application_id: str
    amount_paid: float
    date: datetime
    channel: str
    officer_id: str
    synced_at: Optional[datetime] = None

    class Config:
        from_attributes = True

class PromiseToPayCreate(BaseModel):
    application_id: str
    promised_amount: float = Field(..., gt=0, description="Committed repayment amount")
    promised_date: datetime = Field(..., description="Committed payment date")

class PromiseToPayResponse(BaseModel):
    id: str
    application_id: str
    borrower_id: str
    promised_amount: float
    promised_date: datetime
    status: str  # Pending, Met, Broken
    created_by: str
    created_at: datetime

    class Config:
        from_attributes = True
