"""Authoritative loan-review order and role labels."""

WORKFLOW_STAGES = (
    ("intake", "account_officer"),
    ("branch_manager_review", "branch_manager"),
    ("branch_supervisor_review", "branch_supervisor"),
    ("credit_analyst_review", "credit_analyst"),
    ("crm_review", "crm"),
    ("head_crm_review", "head_crm"),
    ("audit_review", "auditor"),
    ("ed_approval", "ed"),
    ("md_approval", "md"),
    ("disbursement_ready", "crm"),
)

NEXT_STAGE = {stage: WORKFLOW_STAGES[index + 1][0] for index, (stage, _) in enumerate(WORKFLOW_STAGES[:-1])}
STAGE_ROLE = dict(WORKFLOW_STAGES)

ROLE_LABELS = {
    "account_officer": "Account Officer",
    "branch_manager": "Branch Manager",
    "branch_supervisor": "Branch Supervisor",
    "credit_analyst": "Credit Analyst",
    "crm": "CRM Officer",
    "head_crm": "Head CRM",
    "auditor": "Audit",
    "ed": "Executive Director",
    "md": "Managing Director",
    "system_admin": "System Admin",
}
