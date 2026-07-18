from uuid import UUID

from app.core.base_repository import BaseRepository


class DocumentRepository(BaseRepository):
    domain = "documents"

    async def create(
        self,
        *,
        loan_id: UUID,
        org_id: UUID,
        doc_type: str,
        form_code: str | None,
        original_name: str,
        stored_path: str,
        mime_type: str,
        size_bytes: int,
        uploaded_by: UUID,
        cloud_public_id: str | None = None,
        cloud_preview_url: str | None = None,
    ) -> dict:
        row = await self.conn.fetchrow(
            self.sql("create"),
            loan_id,
            org_id,
            doc_type,
            form_code,
            original_name,
            stored_path,
            mime_type,
            size_bytes,
            uploaded_by,
            cloud_public_id,
            cloud_preview_url,
        )
        return dict(row)

    async def create_mock_upload(
        self,
        *,
        loan_id: UUID,
        org_id: UUID,
        doc_type: str,
        form_code: str | None,
        original_name: str,
        stored_path: str,
        mime_type: str,
        size_bytes: int,
        uploaded_by: UUID,
    ) -> dict:
        return await self.create(
            loan_id=loan_id,
            org_id=org_id,
            doc_type=doc_type,
            form_code=form_code,
            original_name=original_name,
            stored_path=stored_path,
            mime_type=mime_type,
            size_bytes=size_bytes,
            uploaded_by=uploaded_by,
        )

    async def get_by_loan(self, loan_id: UUID, org_id: UUID) -> list[dict]:
        rows = await self.conn.fetch(
            self.sql("get_by_loan"),
            loan_id,
            org_id,
        )
        return [dict(row) for row in rows] if rows else []

    async def get_by_id_for_org(self, document_id: UUID, org_id: UUID) -> dict | None:
        row = await self.conn.fetchrow(
            """
            SELECT id, org_id, mime_type, cloud_public_id
            FROM documents
            WHERE id = $1 AND org_id = $2 AND deleted_at IS NULL
            """,
            document_id,
            org_id,
        )
        return dict(row) if row else None
