from uuid import UUID

from app.core.base_repository import BaseRepository


class NotificationRepository(BaseRepository):
    domain = "notifications"

    async def list_for_user(self, *, user_id: UUID, org_id: UUID) -> list[dict]:
        rows = await self.conn.fetch(self.sql("list_for_user"), user_id, org_id)
        return [dict(row) for row in rows] if rows else []

    async def create(
        self,
        *,
        user_id: UUID,
        org_id: UUID,
        application_id: UUID | None,
        title: str,
        message: str,
        notification_type: str,
    ) -> dict:
        row = await self.conn.fetchrow(
            self.sql("create"),
            user_id,
            org_id,
            application_id,
            title,
            message,
            notification_type,
        )
        return dict(row)

    async def mark_read_for_user(self, *, notification_id: str, user_id: UUID, org_id: UUID) -> bool:
        result = await self.conn.execute(self.sql("mark_read_for_user"), notification_id, user_id, org_id)
        return result == "UPDATE 1"

    async def clear_for_user(self, *, user_id: UUID, org_id: UUID) -> int:
        result = await self.conn.execute(self.sql("clear_for_user"), user_id, org_id)
        try:
            return int(result.split()[-1])
        except (IndexError, ValueError):
            return 0

