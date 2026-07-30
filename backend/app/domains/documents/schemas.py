from uuid import UUID

from pydantic import BaseModel, Field


class DirectUploadAuthorizationRequest(BaseModel):
    filename: str = Field(min_length=1, max_length=255)
    mime_type: str
    size_bytes: int = Field(gt=0)
    doc_type: str = Field(default="other", max_length=100)
    form_code: str | None = Field(default=None, max_length=100)


class DirectUploadFinalizeRequest(BaseModel):
    intent_id: UUID
    public_id: str = Field(min_length=1, max_length=500)
    version: int = Field(gt=0)
    signature: str = Field(min_length=20, max_length=200)

