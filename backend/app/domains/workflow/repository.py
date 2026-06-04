from uuid import UUID

from app.core.base_repository import BaseRepository


class WorkflowRepository(BaseRepository):
    domain = "workflow"

    async def log_event(
        self,
        *,
        loan_id: UUID,
        org_id: UUID,
        event_type: str,
        from_stage: str | None,
        to_stage: str | None,
        triggered_by: UUID,
        triggered_role: str,
        notes: str | None = None,
    ) -> dict:
        row = await self.conn.fetchrow(
            self.sql("log_event"),
            loan_id,
            org_id,
            event_type,
            from_stage,
            to_stage,
            triggered_by,
            triggered_role.lower().replace(" ", "_"),
            notes,
        )
        return dict(row)
