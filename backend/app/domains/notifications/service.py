from uuid import UUID

from app.domains.notifications.repository import NotificationRepository


class NotificationService:
    def __init__(self, repo: NotificationRepository):
        self.repo = repo

    async def list_for_user(self, *, user_id: UUID, org_id: UUID) -> list[dict]:
        return await self.repo.list_for_user(user_id=user_id, org_id=org_id)

    async def create(
        self,
        *,
        user_id: UUID | None,
        org_id: UUID | None,
        application_id: UUID | None,
        title: str,
        message: str,
        notification_type: str,
    ) -> dict | None:
        if not user_id or not org_id:
            return None
        return await self.repo.create(
            user_id=user_id,
            org_id=org_id,
            application_id=application_id,
            title=title,
            message=message,
            notification_type=notification_type,
        )

    async def mark_read_for_user(self, *, notification_id: str, user_id: UUID, org_id: UUID) -> bool:
        return await self.repo.mark_read_for_user(
            notification_id=notification_id,
            user_id=user_id,
            org_id=org_id,
        )

    async def clear_for_user(self, *, user_id: UUID, org_id: UUID) -> int:
        return await self.repo.clear_for_user(user_id=user_id, org_id=org_id)

