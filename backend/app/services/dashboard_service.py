"""
Dashboard data aggregation service.

Each role gets a genuinely different dashboard with role-appropriate metrics,
task lists, and queue data. This service dispatches to role-specific methods
that run the corresponding SQL queries.
"""
from datetime import datetime
from uuid import UUID

from app.core.sql import load_sql
from app.domains.loans.schemas import LoanRow


class DashboardService:
    """Aggregates dashboard data per role using raw SQL queries.

    Each role method returns a dict with keys matching the role's
    dashboard template variables.
    """

    def __init__(self, conn):
        self.conn = conn

    async def get_dashboard_data(self, user) -> dict:
        """Dispatch to role-specific data method."""
        role = user.role.lower().replace(" ", "_")
        if role == "loan_officer":
            return await self._loan_officer_data(user)
        elif role == "branch_manager":
            return await self._branch_manager_data(user)
        elif role == "credit_officer":
            return await self._credit_officer_data(user)
        elif role == "auditor":
            return await self._auditor_data(user)
        elif role == "system_admin":
            return await self._system_admin_data(user)
        # Fallback — should never reach here with proper RBAC
        return await self._loan_officer_data(user)

    async def _loan_officer_data(self, user) -> dict:
        """Loan Officer dashboard: task-focused, personal queue.

        Metrics: My Applications, Pending Upload, Visits Due, Missing Docs
        Sections: Today's Tasks, Recent Activity, Personal Queue
        """
        # Metrics
        metrics_sql = load_sql("loans", "dashboard_loan_officer")
        metrics_row = await self.conn.fetchrow(metrics_sql, user.org_id, user.id)
        metrics = dict(metrics_row) if metrics_row else {}

        # Today's tasks — prioritized actionable items
        tasks_sql = load_sql("loans", "list_officer_tasks")
        tasks_rows = await self.conn.fetch(tasks_sql, user.org_id, user.id)
        tasks = [dict(r) for r in tasks_rows] if tasks_rows else []

        # Personal queue — recent applications
        queue_sql = load_sql("loans", "list_officer_queue")
        queue_rows = await self.conn.fetch(
            queue_sql, user.org_id, user.id, None, 10, 0
        )
        queue = [self._with_stage_display(dict(r)) for r in queue_rows] if queue_rows else []

        # Visits due
        try:
            visits_sql = load_sql("visitation", "list_visits_due_today")
            visits_rows = await self.conn.fetch(visits_sql, user.org_id, user.id)
            visits_due = [dict(r) for r in visits_rows] if visits_rows else []
        except FileNotFoundError:
            visits_due = []

        return {
            "metrics": {
                "my_applications": metrics.get("my_applications", 0),
                "pending_upload": metrics.get("pending_upload", 0),
                "visits_due": len(visits_due),
                "returned": metrics.get("returned_count", 0),
                "ocr_review": metrics.get("ocr_review_count", 0),
                "drafts": metrics.get("drafts_count", 0),
            },
            "tasks": tasks,
            "queue": queue,
            "visits_due": visits_due,
        }

    async def get_loan_officer_queue(
        self,
        user,
        stage: str | None = None,
        limit: int = 50,
        offset: int = 0,
    ) -> list[dict]:
        """Return the loan officer's priority-ordered personal queue."""
        queue_sql = load_sql("loans", "list_officer_queue")
        rows = await self.conn.fetch(queue_sql, user.org_id, user.id, stage, limit, offset)
        return [self._with_stage_display(dict(r)) for r in rows] if rows else []

    async def get_visits_due_today(self, user) -> list[dict]:
        """Return loans due for field visitation for a loan officer."""
        visits_sql = load_sql("visitation", "list_visits_due_today")
        rows = await self.conn.fetch(visits_sql, user.org_id, user.id)
        return [dict(r) for r in rows] if rows else []

    @staticmethod
    def _with_stage_display(row: dict) -> dict:
        labels = {
            "intake": "Draft",
            "ocr_review": "OCR Review",
            "credit_review": "Credit Review",
            "branch_approval": "Branch Approval",
            "disbursement_ready": "Disbursement Ready",
            "disbursed": "Disbursed",
            "returned": "Returned",
            "rejected": "Rejected",
        }
        row["status"] = labels.get(row.get("stage"), "Draft")
        return row

    async def _branch_manager_data(self, user) -> dict:
        """Branch Manager: pipeline overview + awaiting concurrence."""
        # Reuse existing metrics query for now
        metrics_sql = load_sql("loans", "dashboard_metrics")
        metrics_row = await self.conn.fetchrow(
            metrics_sql, user.org_id, user.id, "branch_manager"
        )
        metrics = dict(metrics_row) if metrics_row else {}

        # Pipeline counts
        pipeline_sql = load_sql("loans", "count_by_stage")
        pipeline_rows = await self.conn.fetch(pipeline_sql, user.org_id)
        pipeline = [dict(r) for r in pipeline_rows] if pipeline_rows else []

        # Recent applications
        recent_sql = load_sql("loans", "list_recent")
        recent_rows = await self.conn.fetch(recent_sql, user.org_id, 10)
        recent = [LoanRow(**r) for r in recent_rows] if recent_rows else []

        return {
            "metrics": metrics,
            "pipeline": pipeline,
            "recent": recent,
        }

    async def _credit_officer_data(self, user) -> dict:
        """Credit Officer: analysis-heavy review queue."""
        metrics_sql = load_sql("loans", "dashboard_metrics")
        metrics_row = await self.conn.fetchrow(
            metrics_sql, user.org_id, user.id, "credit_officer"
        )
        metrics = dict(metrics_row) if metrics_row else {}

        recent_sql = load_sql("loans", "list_recent")
        recent_rows = await self.conn.fetch(recent_sql, user.org_id, 10)
        recent = [LoanRow(**r) for r in recent_rows] if recent_rows else []

        return {
            "metrics": metrics,
            "recent": recent,
        }

    async def _auditor_data(self, user) -> dict:
        """Auditor: compliance overview, read-only."""
        metrics_sql = load_sql("loans", "dashboard_metrics")
        metrics_row = await self.conn.fetchrow(
            metrics_sql, user.org_id, user.id, "auditor"
        )
        metrics = dict(metrics_row) if metrics_row else {}

        return {
            "metrics": metrics,
        }

    async def _system_admin_data(self, user) -> dict:
        """System Admin: user management overview."""
        metrics_sql = load_sql("loans", "dashboard_metrics")
        metrics_row = await self.conn.fetchrow(
            metrics_sql, user.org_id, user.id, "system_admin"
        )
        metrics = dict(metrics_row) if metrics_row else {}

        return {
            "metrics": metrics,
        }
