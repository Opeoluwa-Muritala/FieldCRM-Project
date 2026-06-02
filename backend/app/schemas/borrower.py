from typing import Optional
from datetime import datetime
from pydantic import BaseModel, Field, field_validator

class BorrowerBase(BaseModel):
    name: str = Field(..., min_length=2, max_length=100)
    phone: str = Field(..., description="Mobile number of the borrower")
    bvn: str = Field(..., min_length=11, max_length=11, description="11-digit Bank Verification Number")
    nin: str = Field(..., min_length=11, max_length=11, description="11-digit National Identification Number")
    photo_url: Optional[str] = None
    gps_coordinates: Optional[str] = None
    physical_address: Optional[str] = None
    employment_status: Optional[str] = None
    employer_name: Optional[str] = None
    monthly_income: Optional[float] = Field(None, ge=0)
    bank_name: Optional[str] = None
    account_number: Optional[str] = None
    guarantor_name: Optional[str] = None
    guarantor_phone: Optional[str] = None

    @field_validator("bvn", "nin")
    @classmethod
    def validate_numeric_identifiers(cls, value: str) -> str:
        """Enforces that regulatory IDs contain strictly digits."""
        if not value.isdigit():
            raise ValueError("Identifier must consist strictly of digits")
        return value

class BorrowerCreate(BorrowerBase):
    org_id: str

class BorrowerUpdate(BaseModel):
    name: Optional[str] = None
    phone: Optional[str] = None
    bvn: Optional[str] = None
    nin: Optional[str] = None
    photo_url: Optional[str] = None
    status: Optional[str] = None  # Active, Applicant, Blacklisted, Dormant
    gps_coordinates: Optional[str] = None
    physical_address: Optional[str] = None
    employment_status: Optional[str] = None
    employer_name: Optional[str] = None
    monthly_income: Optional[float] = None
    bank_name: Optional[str] = None
    account_number: Optional[str] = None
    guarantor_name: Optional[str] = None
    guarantor_phone: Optional[str] = None

class BorrowerResponse(BorrowerBase):
    id: str
    org_id: str
    loan_officer_id: str
    status: str
    created_at: datetime

    class Config:
        from_attributes = True
