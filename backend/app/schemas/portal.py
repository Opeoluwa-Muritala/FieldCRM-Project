from pydantic import BaseModel, Field, field_validator

class PortalLoginRequest(BaseModel):
    phone: str = Field(..., description="Phone number associated with borrower profile")
    bvn: str = Field(..., min_length=11, max_length=11, description="11-digit BVN associated with borrower profile")

    @field_validator("bvn")
    @classmethod
    def validate_bvn_numeric(cls, value: str) -> str:
        if not value.isdigit():
            raise ValueError("BVN must contain strictly digits")
        return value
