from uuid import UUID
import json

from app.core.base_repository import BaseRepository


class OcrRepository(BaseRepository):
    domain = "ocr"

    async def list_low_confidence(self, *, loan_id: UUID, threshold: int = 70) -> list[dict]:
        rows = await self.conn.fetch(self.sql("list_low_confidence"), loan_id, threshold)
        return [dict(row) for row in rows]

    async def insert_result(
        self,
        *,
        document_id: UUID,
        loan_id: UUID,
        form_type: str,
        overall_confidence: float,
        raw_extraction: dict,
    ) -> dict:
        row = await self.conn.fetchrow(
            self.sql("insert_ocr_result"),
            document_id,
            loan_id,
            form_type,
            overall_confidence,
            json.dumps(raw_extraction),
        )
        return dict(row)

    async def insert_field(
        self,
        *,
        ocr_result_id: UUID,
        loan_id: UUID,
        field_name: str,
        ocr_value: str | None,
        confidence: float,
        is_critical: bool,
        page_number: int | None = None,
    ) -> UUID:
        row = await self.conn.fetchrow(
            self.sql("insert_ocr_field"),
            ocr_result_id,
            loan_id,
            field_name,
            ocr_value,
            confidence,
            is_critical,
            page_number,
        )
        return row["id"]

    async def set_document_ocr_status(self, document_id: UUID, status: str) -> None:
        await self.conn.execute(
            "UPDATE documents SET ocr_status = $1 WHERE id = $2",
            status,
            document_id,
        )
