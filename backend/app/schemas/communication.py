from typing import Optional, List
from datetime import datetime
from pydantic import BaseModel, Field

class CommunicationLogBase(BaseModel):
    borrower_id: str
    type: str = Field(..., description="Phone Call / Field Visit / SMS / WhatsApp / Walk-in")
    outcome: str = Field(..., min_length=2, max_length=100)
    note: str = Field(..., min_length=5, description="Summary details of the discussion")
    photo_urls: Optional[List[str]] = Field(default=None, description="Static image links from field visit")

class CommunicationLogCreate(CommunicationLogBase):
    pass

class CommunicationLogResponse(CommunicationLogBase):
    id: str
    officer_id: str
    timestamp: datetime

    class Config:
        from_attributes = True
