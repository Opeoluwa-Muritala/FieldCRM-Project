from typing import Optional, List
from datetime import datetime
from pydantic import BaseModel, Field

class GroupCreate(BaseModel):
    name: str = Field(..., min_length=2, max_length=100)
    type: str = Field(..., description="ajo / esusu / solidarity / coop")
    meeting_schedule: Optional[str] = None
    territory: Optional[str] = None

class GroupMemberAdd(BaseModel):
    borrower_id: str

class GroupResponse(BaseModel):
    id: str
    org_id: str
    name: str
    type: str
    leader_id: Optional[str] = None
    meeting_schedule: Optional[str] = None
    territory: Optional[str] = None

    class Config:
        from_attributes = True

class GroupCycleCreate(BaseModel):
    group_id: str
    start_date: datetime
    end_date: datetime
    contribution_amount: float = Field(..., gt=0)

class GroupCycleContribution(BaseModel):
    cycle_id: str
    borrower_id: str
    amount: float = Field(..., gt=0)
