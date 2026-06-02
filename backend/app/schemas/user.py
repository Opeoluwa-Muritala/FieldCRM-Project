from typing import Optional
from datetime import datetime
from pydantic import BaseModel, Field

class UserBase(BaseModel):
    name: str = Field(..., min_length=2, max_length=100)
    phone: str = Field(..., description="Unique user mobile number")
    role: str = Field(..., description="Loan Officer, Branch Manager, Credit Officer, Auditor, MCR")
    branch_id: Optional[str] = None

class UserCreate(UserBase):
    org_id: str
    password: str = Field(..., min_length=8, description="Strong user password")

class UserUpdate(BaseModel):
    name: Optional[str] = None
    phone: Optional[str] = None
    role: Optional[str] = None
    branch_id: Optional[str] = None
    is_active: Optional[bool] = None

class UserResponse(UserBase):
    id: str
    org_id: str
    is_active: bool
    created_at: datetime

    class Config:
        from_attributes = True

class Token(BaseModel):
    access_token: str
    token_type: str = "bearer"

class TokenPayload(BaseModel):
    sub: Optional[str] = None
