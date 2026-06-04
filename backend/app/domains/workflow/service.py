from uuid import UUID

from app.core.audit import AuditService
from app.domains.workflow.repository import WorkflowRepository


class WorkflowService:
    def __init__(self, repo: WorkflowRepository, audit: AuditService):
        self.repo = repo
        self.audit = audit

    async def record_stage_change(
        self,
        *,
        loan_id: UUID,
        org_id: UUID,
        event_type: str,
        from_stage: str | None,
        to_stage: str,
        user_id: UUID,
        user_role: str,
        notes: str | None = None,
    ) -> None:
        await self.repo.log_event(
            loan_id=loan_id,
            org_id=org_id,
            event_type=event_type,
            from_stage=from_stage,
            to_stage=to_stage,
            triggered_by=user_id,
            triggered_role=user_role,
            notes=notes,
        )
        await self.audit.insert(
            org_id=org_id,
            entity_type="loan_application",
            entity_id=loan_id,
            action=event_type,
            user_id=user_id,
            user_role=user_role,
            field_name="stage",
            old_value=from_stage,
            new_value=to_stage,
            source="manual",
            notes=notes,
        )
