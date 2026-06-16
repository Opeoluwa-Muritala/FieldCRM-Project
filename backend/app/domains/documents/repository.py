from uuid import UUID

from app.core.base_repository import BaseRepository


class DocumentRepository(BaseRepository):
    domain = "documents"

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
        )
        return dict(row)

    async def get_by_loan(self, loan_id: UUID, org_id: UUID) -> list[dict]:
        rows = await self.conn.fetch(
            self.sql("get_by_loan"),
            loan_id,
            org_id,
        )
        return [dict(row) for row in rows] if rows else []

