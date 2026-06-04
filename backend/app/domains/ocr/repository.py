from uuid import UUID

from app.core.base_repository import BaseRepository


class OcrRepository(BaseRepository):
    domain = "ocr"

    async def list_low_confidence(self, *, loan_id: UUID, threshold: int = 70) -> list[dict]:
        rows = await self.conn.fetch(self.sql("list_low_confidence"), loan_id, threshold)
        return [dict(row) for row in rows]
