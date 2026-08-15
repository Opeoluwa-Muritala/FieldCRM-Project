from typing import Optional
from datetime import datetime
from uuid import UUID
from pydantic import BaseModel, ConfigDict, EmailStr, Field


class UserBase(BaseModel):
    full_name: str = Field(..., min_length=2, max_length=100)
    email: str = Field(..., description="Unique user email address")
    role: str = Field(..., description="account_officer, branch_manager, branch_supervisor, credit_analyst, crm, head_crm, auditor, ed, md, legal, system_admin")
    branch_id: Optional[UUID] = None


class UserCreate(UserBase):
    org_id: str
    password: str = Field(..., min_length=12, max_length=128, description="Strong user password")


class UserInvitationCreate(UserBase):
    """An invitee chooses their password after receiving the role-specific link."""
    email: EmailStr


class UserRoleUpdate(BaseModel):
    role: str = Field(..., description="The operational role to assign")
    branch_id: Optional[UUID] = None


class UserUpdate(BaseModel):
    full_name: Optional[str] = None
    email: Optional[str] = None
    role: Optional[str] = None
    active: Optional[bool] = None
    branch_id: Optional[UUID] = None


class UserResponse(UserBase):
    model_config = ConfigDict(from_attributes=True)

    id: str
    org_id: str
    active: bool
    branch_name: Optional[str] = None
    created_at: datetime

class UserRow(BaseModel):
    model_config = ConfigDict(from_attributes=True)

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
    branch_id: Optional[UUID] = None
    branch_name: Optional[str] = None
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
            "account_officer": "Relationship Officer",
            "loan_officer": "Relationship Officer",
            "relationship_officer": "Relationship Officer",
            "branch_manager": "Team Lead",
            "team_lead": "Team Lead",
            "branch_supervisor": "Supervisor",
            "supervisor": "Supervisor",
            "credit_analyst": "Credit Analyst",
            "auditor": "Auditor",
            "system_admin": "System Admin",
            "crm": "CRM Officer",
            "head_crm": "Head CRM",
            "md": "Managing Director",
            "ed": "Executive Director",
            "legal": "Legal",
        }
        key = self.role.lower().replace(" ", "_")
        return mapping.get(key, self.role)

class OrganisationRow(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: UUID
    name: str
    code: str
    active: bool
    created_at: datetime
