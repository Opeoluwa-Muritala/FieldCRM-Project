from uuid import UUID

from app.core.audit import AuditService
from app.domains.visitation.repository import VisitationRepository


class VisitationService:
    def __init__(self, repo: VisitationRepository, audit: AuditService):
        self.repo = repo
        self.audit = audit

    async def submit_report(
        self,
        *,
        loan_id: UUID,
        org_id: UUID,
        met_with: str | None,
        premises_description: str | None,
        direction_from_branch: str | None,
        submitted_by: UUID,
        user_role: str,
    ) -> dict:
        report = await self.repo.upsert_report(
            loan_id=loan_id,
            org_id=org_id,
            met_with=met_with,
            premises_description=premises_description,
            direction_from_branch=direction_from_branch,
        )
        await self.audit.insert(
            org_id=org_id,
            entity_type="loan_application",
            entity_id=loan_id,
            action="visitation.submitted",
            user_id=submitted_by,
            user_role=user_role,
            field_name="visitation_status",
            new_value="submitted",
            source="manual",
        )
        return report

    async def submit_manager_signoff(
        self,
        *,
        loan_id: UUID,
        org_id: UUID,
        manager_id: UUID,
        manager_role: str,
        notes: str,
        decision: str,
    ) -> dict | None:
        report = await self.repo.manager_signoff(
            loan_id=loan_id,
            org_id=org_id,
            manager_id=manager_id,
            notes=notes,
            decision=decision,
        )
        if report:
            await self.audit.insert(
                org_id=org_id,
                entity_type="loan_application",
                entity_id=loan_id,
                action="visitation.manager_signoff",
                user_id=manager_id,
                user_role=manager_role,
                field_name="manager_concurrence",
                new_value=decision,
                source="manual",
                notes=notes,
            )
        return report
