from uuid import UUID
from datetime import datetime
from typing import Optional
from pydantic import BaseModel, Field

class BranchBase(BaseModel):
    name: str = Field(..., min_length=2, max_length=100)
    code: str = Field(..., min_length=1, max_length=20)

class BranchCreate(BranchBase):
    pass

class BranchResponse(BranchBase):
    id: UUID
    org_id: UUID
    active: bool
    created_at: datetime

    class Config:
        from_attributes = True
