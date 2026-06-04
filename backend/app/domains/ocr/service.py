from uuid import UUID

from app.domains.ocr.repository import OcrRepository


class OcrService:
    def __init__(self, repo: OcrRepository):
        self.repo = repo

    async def list_review_exceptions(self, *, loan_id: UUID) -> list[dict]:
        return await self.repo.list_low_confidence(loan_id=loan_id)
