from __future__ import annotations

from datetime import date

from pydantic import BaseModel, EmailStr, Field, field_validator


class CustomerInput(BaseModel):
    legal_name: str = Field(min_length=2, max_length=200)
    date_of_birth: date | None = None
    phone: str | None = Field(default=None, max_length=30)
    email: EmailStr | None = None
    bvn: str | None = Field(default=None, max_length=11)
    nin: str | None = Field(default=None, max_length=11)
    bank_account: str | None = Field(default=None, max_length=10)
    bank_name: str | None = Field(default=None, max_length=120)
    residential_address: str | None = Field(default=None, max_length=500)
    business_name: str | None = Field(default=None, max_length=200)
    external_customer_id: str | None = Field(default=None, max_length=200)
    cbs_provider: str | None = Field(default=None, max_length=80)

    @field_validator("legal_name", "residential_address", "business_name", "external_customer_id", "cbs_provider", mode="before")
    @classmethod
    def trim_text(cls, value):
        return value.strip() if isinstance(value, str) else value

    @field_validator("phone")
    @classmethod
    def validate_phone(cls, value):
        if not value:
            return None
        normalized = "+" + "".join(filter(str.isdigit, value)) if value.strip().startswith("+") else "".join(filter(str.isdigit, value))
        digits = "".join(filter(str.isdigit, normalized))
        if not 7 <= len(digits) <= 15:
            raise ValueError("Phone number must contain 7 to 15 digits")
        return normalized

    @field_validator("bvn", "nin")
    @classmethod
    def validate_identity_number(cls, value):
        if not value:
            return None
        digits = "".join(filter(str.isdigit, value))
        if len(digits) != 11:
            raise ValueError("BVN and NIN must contain exactly 11 digits")
        return digits

    @field_validator("bank_account")
    @classmethod
    def validate_account(cls, value):
        if not value:
            return None
        digits = "".join(filter(str.isdigit, value))
        if len(digits) != 10:
            raise ValueError("Bank account must contain exactly 10 digits")
        return digits

    @field_validator("date_of_birth")
    @classmethod
    def validate_dob(cls, value):
        if value and value >= date.today():
            raise ValueError("Date of birth must be in the past")
        return value


class CustomerCreate(CustomerInput):
    duplicate_override_reason: str | None = Field(default=None, max_length=1000)


class DuplicateMatch(BaseModel):
    customer_id: str
    customer_number: str
    legal_name: str
    matched_rules: list[str]


class DuplicateCheckResponse(BaseModel):
    probable_duplicates: list[DuplicateMatch]
    override_required: bool
