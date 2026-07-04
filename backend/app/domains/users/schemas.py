from typing import Optional
from datetime import datetime
from uuid import UUID
from pydantic import BaseModel, Field


class UserBase(BaseModel):
    full_name: str = Field(..., min_length=2, max_length=100)
    email: str = Field(..., description="Unique user email address")
    role: str = Field(..., description="loan_officer, branch_manager, credit_officer, auditor, system_admin")


class UserCreate(UserBase):
    org_id: str
    password: str = Field(..., min_length=8, description="Strong user password")


class UserUpdate(BaseModel):
    full_name: Optional[str] = None
    email: Optional[str] = None
    role: Optional[str] = None
    active: Optional[bool] = None


class UserResponse(UserBase):
    id: str
    org_id: str
    active: bool
    created_at: datetime

    class Config:
        from_attributes = True


class UserRow(BaseModel):
    """Maps directly to the new 'users' table columns.
    No aliasing needed — column names match field names exactly.
    """
    id: UUID
    org_id: UUID
    full_name: str
    email: str
    password_hash: str
    role: str  # stored as lowercase snake_case in DB
    active: bool
    last_login_at: Optional[datetime] = None
    created_at: datetime

    @property
    def name(self) -> str:
        """Display name for templates."""
        return self.full_name

    @property
    def is_active(self) -> bool:
        return self.active

    @property
    def hashed_password(self) -> str:
        return self.password_hash

    @property
    def db_role(self) -> str:
        """Raw database role value (lowercase snake_case)."""
        return self.role

    @property
    def display_role(self) -> str:
        """Human-readable role for UI display."""
        mapping = {
            "loan_officer": "Loan Officer",
            "credit_officer": "Credit Officer",
            "branch_manager": "Branch Manager",
            "auditor": "Auditor",
            "system_admin": "System Admin",
            "crm": "CRM Officer",
            "md": "Managing Director",
            "ed": "Executive Director",
        }
        key = self.role.lower().replace(" ", "_")
        return mapping.get(key, self.role)

    class Config:
        from_attributes = True


class OrganisationRow(BaseModel):
    id: UUID
    name: str
    code: str
    active: bool
    created_at: datetime

    class Config:
        from_attributes = True
