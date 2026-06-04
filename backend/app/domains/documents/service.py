from uuid import UUID

from app.core.audit import AuditService
from app.domains.documents.repository import DocumentRepository


FORM_CODES = {
    "loan_application_form": "MMFB/CRM/01",
    "pledge_form": "MMFB/CRM/02",
    "guarantor_form": "MMFB/CRM/03",
}


class DocumentService:
    def __init__(self, repo: DocumentRepository, audit: AuditService):
        self.repo = repo
        self.audit = audit

    async def save_mock_upload(
        self,
        *,
        loan_id: UUID,
        org_id: UUID,
        category: str,
        uploaded_by: UUID,
        user_role: str,
    ) -> dict:
        document = await self.repo.create_mock_upload(
            loan_id=loan_id,
            org_id=org_id,
            doc_type=category,
            form_code=FORM_CODES.get(category),
            original_name=f"mock_{category}.pdf",
            stored_path=f"/static/uploads/mock_{category}.pdf",
            mime_type="application/pdf",
            size_bytes=1024,
            uploaded_by=uploaded_by,
        )
        await self.audit.insert(
            org_id=org_id,
            entity_type="document",
            entity_id=document["id"],
            action="document.uploaded",
            user_id=uploaded_by,
            user_role=user_role,
            field_name="doc_type",
            new_value=category,
            source="manual",
        )
        return document
