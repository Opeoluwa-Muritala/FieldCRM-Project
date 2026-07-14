"""
Dashboard data aggregation service.

Each role gets a genuinely different dashboard with role-appropriate metrics,
task lists, and queue data. This service dispatches to role-specific methods
that run the corresponding SQL queries.
"""
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
        if role in ("account_officer", "loan_officer"):
            return await self._loan_officer_data(user)
        elif role == "branch_manager":
            return await self._branch_manager_data(user)
        elif role == "branch_supervisor":
            return await self._branch_supervisor_data(user)
        elif role == "credit_analyst":
            return await self._credit_analyst_data(user)
        elif role == "auditor":
            return await self._auditor_data(user)
        elif role == "system_admin":
            return await self._system_admin_data(user)
        elif role in ("crm", "head_crm"):
            return await self._crm_data(user)
        elif role == "ed":
            return await self._ed_data(user)
        elif role == "md":
            return await self._md_data(user)
        elif role in ("md", "ed"):
            return await self._executive_data(user)
        # Fallback
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
            "branch_manager_review": "Branch Manager Review",
            "branch_supervisor_review": "Branch Supervisor Review",
            "credit_analyst_review": "Credit Analyst Review",
            "crm_review": "CRM Review",
            "ed_approval": "ED Approval",
            "md_approval": "MD Approval",
            "executive_approval": "Executive Approval",
            "disbursement_ready": "Disbursement Ready",
            "disbursed": "Disbursed",
            "returned": "Returned",
            "rejected": "Rejected",
        }
        row["status"] = labels.get(row.get("stage"), "Draft")
        return row

    async def _branch_manager_data(self, user) -> dict:
        """Branch Manager dashboard: approvals, signoffs, and assigned pipeline."""
        metrics = await self._fetch_one("loans", "dashboard_branch_manager", user.org_id, user.id)
        queue = await self.get_awaiting_concurrence(user, limit=8)
        signoffs = await self.get_pending_signoffs(user, limit=6)
        pipeline = await self.get_branch_pipeline(user)

        return {
            "metrics": metrics,
            "queue": queue,
            "signoffs": signoffs,
            "pipeline": pipeline,
        }

    async def _credit_analyst_data(self, user) -> dict:
        """Credit Analyst dashboard: underwriting files and data-quality exceptions."""
        reviews = await self.get_credit_reviews(user, limit=8)
        exceptions = await self.get_credit_ocr_exceptions(user, limit=8)
        return {
            "metrics": {
                "reviews_due": len(reviews),
                "ocr_exceptions": len(exceptions),
                "reviewed_today": 0,
                "returned_this_week": 0,
            },
            "reviews": reviews,
            "exceptions": exceptions,
        }

    async def _branch_supervisor_data(self, user) -> dict:
        """Branch Supervisor dashboard: post-manager files awaiting supervisory review."""
        rows = await self.conn.fetch(
            load_sql("loans", "list_by_stage"),
            user.org_id,
            "branch_supervisor_review",
            None,
            10,
            0,
        )
        queue = [self._with_stage_display(dict(row)) for row in rows] if rows else []
        return {
            "metrics": {
                "supervisory_reviews": len(queue),
                "returned_this_week": 0,
            },
            "queue": queue,
        }

    async def get_supervisory_review_queue(self, user, limit: int = 50, offset: int = 0) -> list[dict]:
        rows = await self.conn.fetch(
            load_sql("loans", "list_by_stage"),
            user.org_id,
            "branch_supervisor_review",
            None,
            limit,
            offset,
        )
        return [self._with_stage_display(dict(row)) for row in rows] if rows else []

    async def _auditor_data(self, user) -> dict:
        """Auditor dashboard: read-only compliance flags and audit activity."""
        metrics = await self._fetch_one("loans", "dashboard_auditor", user.org_id)
        flags = await self.get_compliance_flags(user, limit=10)
        activity = await self.get_recent_audit_activity(user, limit=8)
        flag_counts = await self._fetch_one("audit", "count_compliance_flags", user.org_id)

        return {
            "metrics": metrics,
            "flags": flags,
            "activity": activity,
            "flag_counts": flag_counts,
        }

    async def _system_admin_data(self, user) -> dict:
        """System Admin dashboard: user and system control overview."""
        metrics = await self._fetch_one("loans", "dashboard_system_admin", user.org_id)
        users = await self.get_admin_users(user, limit=8)
        role_counts = await self.get_user_counts_by_role(user)
        control_queue = await self.get_system_control_queue(user, limit=8)
        activity = await self.get_recent_audit_activity(user, limit=8)

        return {
            "metrics": metrics,
            "users": users,
            "role_counts": role_counts,
            "control_queue": control_queue,
            "activity": activity,
        }

    async def get_awaiting_concurrence(self, user, limit: int = 50, offset: int = 0) -> list[dict]:
        rows = await self.conn.fetch(
            load_sql("loans", "list_awaiting_concurrence"),
            user.org_id,
            user.id,
            limit,
            offset,
        )
        return [self._with_stage_display(dict(r)) for r in rows] if rows else []

    async def get_branch_pipeline(self, user) -> list[dict]:
        rows = await self.conn.fetch(load_sql("loans", "branch_pipeline_counts"), user.org_id, user.id)
        return [dict(r) for r in rows] if rows else []

    async def get_pending_signoffs(self, user, limit: int = 50, offset: int = 0) -> list[dict]:
        rows = await self.conn.fetch(
            load_sql("visitation", "list_pending_signoffs"),
            user.org_id,
            user.id,
            limit,
            offset,
        )
        return [dict(r) for r in rows] if rows else []

    async def get_credit_reviews(self, user, limit: int = 50, offset: int = 0) -> list[dict]:
        rows = await self.conn.fetch(
            load_sql("loans", "list_credit_reviews"),
            user.org_id,
            user.id,
            limit,
            offset,
        )
        return [self._with_stage_display(dict(r)) for r in rows] if rows else []

    async def get_credit_ocr_exceptions(
        self,
        user,
        threshold: int = 70,
        limit: int = 50,
        offset: int = 0,
    ) -> list[dict]:
        rows = await self.conn.fetch(
            load_sql("ocr", "list_exceptions_by_credit_officer"),
            user.org_id,
            user.id,
            threshold,
            limit,
            offset,
        )
        return [dict(r) for r in rows] if rows else []

    async def get_compliance_flags(self, user, limit: int = 50, offset: int = 0) -> list[dict]:
        rows = await self.conn.fetch(
            load_sql("audit", "list_compliance_flags"),
            user.org_id,
            limit,
            offset,
        )
        return [dict(r) for r in rows] if rows else []

    async def get_recent_audit_activity(self, user, limit: int = 50, offset: int = 0) -> list[dict]:
        rows = await self.conn.fetch(
            load_sql("audit", "list_recent_activity"),
            user.org_id,
            limit,
            offset,
        )
        return [dict(r) for r in rows] if rows else []

    async def get_admin_users(self, user, limit: int = 50, offset: int = 0) -> list[dict]:
        rows = await self.conn.fetch(
            load_sql("users", "list_users_admin"),
            user.org_id,
            limit,
            offset,
        )
        return [dict(r) for r in rows] if rows else []

    async def get_user_counts_by_role(self, user) -> list[dict]:
        rows = await self.conn.fetch(load_sql("users", "count_by_role"), user.org_id)
        return [dict(r) for r in rows] if rows else []

    async def get_system_control_queue(self, user, limit: int = 50, offset: int = 0) -> list[dict]:
        rows = await self.conn.fetch(
            load_sql("loans", "list_system_control_queue"),
            user.org_id,
            limit,
            offset,
        )
        return [self._with_stage_display(dict(r)) for r in rows] if rows else []

    async def _crm_data(self, user) -> dict:
        """CRM dashboard: dossier review queue, recent disbursements, PAR."""
        from app.domains.loans.repository import LoanRepository
        repo = LoanRepository(self.conn)
        crm_queue = await (
            repo.list_head_crm_queue(user.org_id, limit=20)
            if user.role == "head_crm"
            else repo.list_crm_queue(user.org_id, limit=20)
        )
        disbursed = await repo.list_disbursed(user.org_id)
        par = await self.get_par_summary(user)
        return {
            "metrics": {
                "crm_queue": len(crm_queue),
                "disbursed_total": len(disbursed),
                "par30_pct": par.get("par30_pct", 0),
            },
            "crm_queue": crm_queue,
            "recent_disbursements": disbursed[:10],
            "par": par,
        }

    async def _executive_data(self, user) -> dict:
        """MD/ED dashboard: executive approval queue and PAR."""
        from app.domains.loans.repository import LoanRepository
        repo = LoanRepository(self.conn)
        exec_queue = await repo.list_executive_queue(user.org_id, limit=20)
        par = await self.get_par_summary(user)
        return {
            "metrics": {
                "exec_queue": len(exec_queue),
                "par1_pct": par.get("par1_pct", 0),
                "par30_pct": par.get("par30_pct", 0),
                "par90_pct": par.get("par90_pct", 0),
            },
            "exec_queue": exec_queue,
            "par": par,
        }

    async def get_par_summary(self, user) -> dict:
        from app.services.loan_servicing_service import LoanServicingService
        svc = LoanServicingService(self.conn)
        return await svc.get_par_summary(user.org_id)

    async def get_crm_queue(self, user, limit: int = 50, offset: int = 0) -> list[dict]:
        from app.domains.loans.repository import LoanRepository
        repo = LoanRepository(self.conn)
        if user.role == "head_crm":
            return await repo.list_head_crm_queue(user.org_id, limit, offset)
        return await repo.list_crm_queue(user.org_id, limit, offset)

    async def get_executive_queue(self, user, limit: int = 50, offset: int = 0) -> list[dict]:
        from app.domains.loans.repository import LoanRepository
        return await LoanRepository(self.conn).list_executive_queue(user.org_id, limit, offset)

    async def get_committee_queue(self, user, limit: int = 50, offset: int = 0) -> list[dict]:
        from app.domains.loans.repository import LoanRepository
        return await LoanRepository(self.conn).list_committee_queue(user.org_id, limit, offset)

    async def get_ed_queue(self, user, limit: int = 50, offset: int = 0) -> list[dict]:
        from app.domains.loans.repository import LoanRepository
        return await LoanRepository(self.conn).list_ed_queue(user.org_id, limit, offset)

    async def get_md_queue(self, user, limit: int = 50, offset: int = 0) -> list[dict]:
        from app.domains.loans.repository import LoanRepository
        return await LoanRepository(self.conn).list_md_queue(user.org_id, limit, offset)

    async def _committee_data(self, user) -> dict:
        queue = await self.get_committee_queue(user, limit=20)
        return {
            "metrics": {"committee_queue": len(queue)},
            "committee_queue": queue,
        }

    async def _ed_data(self, user) -> dict:
        queue = await self.get_ed_queue(user, limit=20)
        par = await self.get_par_summary(user)
        return {
            "metrics": {
                "ed_queue": len(queue),
                "par30_pct": par.get("par30_pct", 0),
            },
            "ed_queue": queue,
            "par": par,
        }

    async def _md_data(self, user) -> dict:
        queue = await self.get_md_queue(user, limit=20)
        par = await self.get_par_summary(user)
        return {
            "metrics": {
                "md_queue": len(queue),
                "par30_pct": par.get("par30_pct", 0),
            },
            "md_queue": queue,
            "par": par,
        }

    async def _fetch_one(self, domain: str, query: str, *args) -> dict:
        row = await self.conn.fetchrow(load_sql(domain, query), *args)
        return dict(row) if row else {}
