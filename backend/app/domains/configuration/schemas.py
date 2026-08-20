from datetime import datetime
from typing import Any
from uuid import UUID

from pydantic import BaseModel, Field, field_validator


class DraftCreate(BaseModel):
    reason: str = Field(min_length=10, max_length=500)
    effective_at: datetime | None = None


class DraftPatch(BaseModel):
    setting_path: str = Field(min_length=3, max_length=120, pattern=r"^[a-z][a-z0-9_]*(\.[a-z][a-z0-9_]*)+$")
    value: Any
    reason: str = Field(min_length=10, max_length=500)


class MfaCode(BaseModel):
    code: str = Field(pattern=r"^[0-9]{6}$")


class ConfigVersion(BaseModel):
    id: UUID
    version_number: int
    status: str
    payload: dict
    reason: str
    effective_at: datetime
    high_risk: bool
    requires_second_approval: bool
