from app.core.sql import load_sql
from uuid import UUID


def _as_uuid(value):
    return value if isinstance(value, UUID) else UUID(str(value))


class AuditService:
    def __init__(self, conn):
        self.conn = conn

    async def insert(
        self,
        *,
        org_id,
        entity_type,
        entity_id,
        action,
        user_id,
        user_role,
        field_name=None,
        old_value=None,
        new_value=None,
        source=None,
        notes=None,
        request_id=None,
    ) -> None:
        sql = load_sql("audit", "insert_entry")
        db_role = user_role.lower().replace(" ", "_")
        await self.conn.execute(
            sql,
            _as_uuid(org_id), entity_type, _as_uuid(entity_id), action,
            _as_uuid(user_id), db_role, field_name, old_value,
            new_value, source, notes, request_id,
        )

    async def log(
        self,
        *,
        application_id: str,
        org_id: str = None,
        action: str,
        from_stage=None,
        to_stage,
        actor_id: str,
        actor_role: str = "loan_officer",
        reason: str = None,
    ) -> None:
        sql = load_sql("workflow", "log_event")

        # Stage mapper for legacy integer stage calls
        stage_map = {
            1: "intake",
            2: "ocr_review",
            3: "credit_review",
            4: "branch_approval",
            5: "disbursement_ready",
            6: "disbursed",
            7: "returned",
            8: "rejected",
        }

        str_from = stage_map.get(from_stage, from_stage) if from_stage is not None else None
        str_to = stage_map.get(to_stage, to_stage)

        # If org_id is not supplied, fetch it from the loan application
        if not org_id:
            row = await self.conn.fetchrow(
                "SELECT org_id FROM loan_applications WHERE id = $1",
                _as_uuid(application_id),
            )
            org_id = row["org_id"] if row else None

        db_role = actor_role.lower().replace(" ", "_")

        await self.conn.execute(
            sql,
            _as_uuid(application_id),
            _as_uuid(org_id),
            action,
            str_from,
            str_to,
            _as_uuid(actor_id),
            db_role,
            reason,
        )

        if org_id:
            await self.insert(
                org_id=org_id,
                entity_type="loan_application",
                entity_id=application_id,
                action=action,
                user_id=actor_id,
                user_role=db_role,
                field_name="stage",
                old_value=str_from,
                new_value=str_to,
                source="manual",
                notes=reason,
            )
            await self._notify_workflow_event(
                application_id=_as_uuid(application_id),
                org_id=_as_uuid(org_id),
                action=action,
                to_stage=str_to,
                actor_role=db_role,
            )

    async def _notify_workflow_event(
        self,
        *,
        application_id: UUID,
        org_id: UUID,
        action: str,
        to_stage: str,
        actor_role: str,
    ) -> None:
        row = await self.conn.fetchrow(
            """
            SELECT
                id,
                ref_no,
                applicant_name,
                created_by,
                branch_manager_id,
                credit_officer_id
            FROM loan_applications
            WHERE id = $1
              AND org_id = $2
            """,
            application_id,
            org_id,
        )
        if not row:
            return

        recipient_id = row["created_by"]
        ref_no = row["ref_no"]
        applicant_name = row["applicant_name"]

        title = None
        notification_type = None
        message = None

        if to_stage == "returned" or action.lower().startswith("return"):
            title = "Application Returned"
            notification_type = "application_returned"
            message = f"{ref_no} was returned by {actor_role.replace('_', ' ').title()} for correction"
        elif action == "Credit Underwriting Verdict":
            recipient_id = row["branch_manager_id"] or row["created_by"]
            title = "Credit Review Done"
            notification_type = "credit_review"
            message = f"Credit review completed for {applicant_name} ({ref_no})"
        elif to_stage == "disbursement_ready" or action == "Branch Final Approval":
            title = "Loan Approved"
            notification_type = "approved"
            message = f"{ref_no} was approved for disbursement"
        elif action == "Verify OCR Data":
            title = "Documents Verified"
            notification_type = "document_verified"
            message = f"Documents verified for {applicant_name} ({ref_no})"

        if not title:
            return

        from app.domains.notifications.repository import NotificationRepository
        from app.domains.notifications.service import NotificationService

        await NotificationService(NotificationRepository(self.conn)).create(
            user_id=recipient_id,
            org_id=org_id,
            application_id=application_id,
            title=title,
            message=message,
            notification_type=notification_type,
        )
