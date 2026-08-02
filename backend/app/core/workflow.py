"""Authoritative loan-review order and role labels."""

WORKFLOW_STAGES = (
    ("intake", "account_officer"),
    ("branch_manager_review", "branch_manager"),
    ("branch_supervisor_review", "branch_supervisor"),
    ("credit_analyst_review", "credit_analyst"),
    ("crm_review", "crm"),
    ("head_crm_review", "head_crm"),
    ("ed_approval", "ed"),
    ("md_approval", "md"),
    ("disbursement_ready", "crm"),
)

NEXT_STAGE = {stage: WORKFLOW_STAGES[index + 1][0] for index, (stage, _) in enumerate(WORKFLOW_STAGES[:-1])}
STAGE_ROLE = dict(WORKFLOW_STAGES)

ROLE_LABELS = {
    "account_officer": "Relationship Officer",
    "branch_manager": "Team Lead",
    "branch_supervisor": "Supervisor",
    "credit_analyst": "Credit Analyst",
    "crm": "CRM Officer",
    "head_crm": "Head CRM",
    "auditor": "Audit",
    "ed": "Executive Director",
    "md": "Managing Director",
    "legal": "Legal",
    "system_admin": "System Admin",
}
