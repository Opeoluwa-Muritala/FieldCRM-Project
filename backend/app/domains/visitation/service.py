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
        visit_date: str | None = None,
        visit_time: str | None = None,
        relationship: str | None = None,
        business_condition: str | None = None,
        account_officer: str | None = None,
        gps_coordinates: str | None = None,
        site_photo_url: str | None = None,
        visiting_officer: str | None = None,
        visiting_officer_sig: str | None = None,
        account_officer_sig: str | None = None,
        submitted_by: UUID,
        user_role: str,
    ) -> dict:
        report = await self.repo.upsert_report(
            loan_id=loan_id,
            org_id=org_id,
            met_with=met_with,
            premises_description=premises_description,
            direction_from_branch=direction_from_branch,
            visit_date=visit_date,
            visit_time=visit_time,
            relationship=relationship,
            business_condition=business_condition,
            account_officer=account_officer,
            gps_coordinates=gps_coordinates,
            site_photo_url=site_photo_url,
            visiting_officer=visiting_officer,
            visiting_officer_sig=visiting_officer_sig,
            account_officer_sig=account_officer_sig,
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
        signature: str | None = None,
        return_reason: str | None = None,
    ) -> dict | None:
        report = await self.repo.manager_signoff(
            loan_id=loan_id,
            org_id=org_id,
            manager_id=manager_id,
            notes=notes,
            decision=decision,
            signature=signature,
            return_reason=return_reason,
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
            if decision == "concurred":
                loan = await self.repo.conn.fetchrow(
                    """
                    SELECT ref_no, applicant_name, created_by
                    FROM loan_applications
                    WHERE id = $1
                      AND org_id = $2
                    """,
                    loan_id,
                    org_id,
                )
                if loan:
                    from app.domains.notifications.repository import NotificationRepository
                    from app.domains.notifications.service import NotificationService

                    await NotificationService(NotificationRepository(self.repo.conn)).create(
                        user_id=report.get("visiting_officer_id") or loan["created_by"],
                        org_id=org_id,
                        application_id=loan_id,
                        title="Visitation Signed Off",
                        message=f"Branch Manager concurred on site visit for {loan['applicant_name']}",
                        notification_type="visitation_signoff",
                    )
        return report
