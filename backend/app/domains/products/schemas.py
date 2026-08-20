from typing import Any, Literal

from pydantic import BaseModel, Field, field_validator


FieldType = Literal["text","number","currency","date","dropdown","checkbox","yes_no","photo","file","signature","gps"]
Requirement = Literal["required","optional","hidden"]


class FormFieldDefinition(BaseModel):
    section_key: str = Field(min_length=2, max_length=80, pattern=r"^[a-z][a-z0-9_]*$")
    field_key: str = Field(min_length=2, max_length=80, pattern=r"^[a-z][a-z0-9_]*$")
    label: str = Field(min_length=2, max_length=120)
    field_type: FieldType
    requirement: Requirement = "optional"
    options: list[str] = Field(default_factory=list, max_length=100)
    validation_rules: dict[str, Any] = Field(default_factory=dict)
    visibility_condition: dict[str, Any] = Field(default_factory=dict)
    help_text: str | None = Field(default=None, max_length=300)
    display_order: int = Field(default=0, ge=0, le=10000)

    @field_validator("options")
    @classmethod
    def dropdown_options(cls, value, info):
        if info.data.get("field_type") == "dropdown" and not value:
            raise ValueError("Dropdown fields require options")
        if any(not item.strip() or len(item) > 100 for item in value):
            raise ValueError("Options must be 1-100 characters")
        return value


class SectionDefinition(BaseModel):
    section_key: Literal["personal_details","employment","business","financials","guarantors","collateral","documents","visits","credit_assessment"]
    requirement: Requirement


class DocumentDefinition(BaseModel):
    doc_type: str = Field(min_length=2, max_length=80, pattern=r"^[a-z][a-z0-9_]*$")
    display_name: str = Field(min_length=2, max_length=120)
    mandatory: bool = True


class ProductDefinition(BaseModel):
    code: str = Field(min_length=2, max_length=50, pattern=r"^[a-z][a-z0-9_]*$")
    name: str = Field(min_length=2, max_length=120)
    description: str = Field(default="", max_length=1000)
    family: str = Field(min_length=2, max_length=80)
    customer_segment: str = Field(min_length=2, max_length=80)
    min_amount: float = Field(ge=0)
    max_amount: float = Field(gt=0)
    interest_parameters: dict[str, Any] = Field(default_factory=dict)
    min_tenor_months: int = Field(ge=1, le=360)
    max_tenor_months: int = Field(ge=1, le=360)
    repayment_frequency: Literal["daily","weekly","biweekly","monthly","quarterly","bullet"]
    guarantor_count: int = Field(default=0, ge=0, le=20)
    collateral_required: bool = False
    collateral_rules: dict[str, Any] = Field(default_factory=dict)
    workflow_stages: list[str] = Field(min_length=1, max_length=30)
    approval_limits: dict[str, Any] = Field(default_factory=dict)
    visit_requirements: dict[str, Any] = Field(default_factory=dict)
    credit_checks: dict[str, Any] = Field(default_factory=dict)
    sla_hours: int = Field(default=48, ge=1, le=8760)
    cbs_enabled: bool = False
    sections: list[SectionDefinition] = Field(default_factory=list)
    documents: list[DocumentDefinition] = Field(default_factory=list)
    fields: list[FormFieldDefinition] = Field(default_factory=list)

    @field_validator("max_amount")
    @classmethod
    def amount_range(cls, value, info):
        if info.data.get("min_amount") is not None and value < info.data["min_amount"]:
            raise ValueError("max_amount must be at least min_amount")
        return value

    @field_validator("max_tenor_months")
    @classmethod
    def tenor_range(cls, value, info):
        if info.data.get("min_tenor_months") is not None and value < info.data["min_tenor_months"]:
            raise ValueError("maximum tenor must be at least minimum tenor")
        return value
