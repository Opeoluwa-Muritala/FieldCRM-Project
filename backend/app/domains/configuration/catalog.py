FEATURE_DEFAULTS = {
    "external_applicant_portal": False,
    "ocr": True,
    "visits": True,
    "gps": True,
    "guarantors": True,
    "collateral": True,
    "credit_bureau_integration": False,
    "manual_credit_bureau_evidence": True,
    "cbs_integration": False,
    "manual_repayment": True,
    "repayment_schedule": True,
    "par": True,
    "collections": False,
    "committee_review": True,
    "legal_review": True,
    "ed_review": True,
    "md_review": True,
    "audit_intervention": True,
    "sms": False,
    "email": False,
    "push_notifications": False,
    "offline_mode": False,
    "my_work": True,
    "pipeline": True,
    "document_work_queue": True,
    "credit_reviews": True,
    "exceptions_centre": True,
}

FEATURE_GROUPS = (
    {
        "name": "Operational workspace",
        "description": "Show or remove optional day-to-day workspaces from staff navigation.",
        "features": (
            ("my_work", "My Work", "Show assigned work and task queues for operational users."),
            ("pipeline", "Pipeline", "Show the branch application pipeline workspace."),
            ("document_work_queue", "Document work queue", "Show missing, rejected, and document-quality exceptions."),
            ("credit_reviews", "Credit reviews", "Show underwriting and credit-review workspaces."),
            ("exceptions_centre", "Exceptions Centre", "Show the central operational exceptions workspace."),
        ),
    },
    {
        "name": "Origination & field capture",
        "description": "Control how staff collect application evidence and field information.",
        "features": (
            ("external_applicant_portal", "External applicant portal", "Allow applicants to start an application outside the staff workspace."),
            ("visits", "Field visits", "Enable visit planning, findings, and completion evidence."),
            ("gps", "GPS capture", "Allow location evidence on supported field activities."),
            ("guarantors", "Guarantors", "Collect and validate guarantor information for applicable products."),
            ("collateral", "Collateral", "Collect collateral details and supporting evidence."),
        ),
    },
    {
        "name": "Credit & approval workflow",
        "description": "Choose the evidence and approval stages available to configured workflows.",
        "features": (
            ("credit_bureau_integration", "Credit bureau integration", "Query an approved credit bureau provider directly."),
            ("manual_credit_bureau_evidence", "Manual bureau evidence", "Let analysts record a completed bureau check and attach evidence."),
            ("committee_review", "Credit committee", "Make committee review available as a configurable workflow stage."),
            ("legal_review", "Legal review", "Make legal review available for conditional workflow routing."),
            ("ed_review", "Executive Director review", "Allow workflows to route qualifying applications to the ED."),
            ("md_review", "Managing Director review", "Allow workflows to route qualifying applications to the MD."),
            ("audit_intervention", "Audit intervention", "Allow Audit to flag cases, add notes, and request investigation."),
        ),
    },
    {
        "name": "Portfolio & collections",
        "description": "Control financial data integrations and post-disbursement operations.",
        "features": (
            ("cbs_integration", "Core Banking integration", "Read balances, schedules, repayments, arrears, and DPD from CBS."),
            ("manual_repayment", "Manual repayment", "Keep legacy manual repayment entry available when CBS is not authoritative."),
            ("repayment_schedule", "Repayment schedule", "Show repayment schedules for supported loan products."),
            ("par", "Portfolio at risk", "Enable CBS-sourced PAR monitoring and queues."),
            ("collections", "Collections", "Enable collections queues and action recording."),
        ),
    },
    {
        "name": "Communications & continuity",
        "description": "Manage outbound channels and supported resilient-working capabilities.",
        "features": (
            ("sms", "SMS notifications", "Send approved workflow and customer notifications by SMS."),
            ("email", "Email notifications", "Send approved internal and customer email notifications."),
            ("push_notifications", "Push notifications", "Send mobile push notifications to staff devices."),
            ("offline_mode", "Offline mode", "Expose offline-capable workflows to supported clients."),
        ),
    },
)

SECTIONS = (
    "Organisation", "Users & Access", "Products", "Forms", "Workflow",
    "Approval Matrix", "Documents", "Features", "Integrations",
    "Field Operations", "SLA", "Security", "Branding", "Audit", "System Health",
)

PRESETS = {"microfinance_bank", "cooperative", "finance_company", "credit_union", "custom"}


def default_payload(org_name: str = "FieldCRM") -> dict:
    return {
        "organisation": {"name": org_name, "preset": "microfinance_bank"},
        "features": dict(FEATURE_DEFAULTS),
        "branding": {
            "institution_name": org_name,
            "logo_url": "",
            "login_logo_url": "",
            "report_logo_url": "",
            "report_header": org_name,
            "support_phone": "",
            "support_email": "",
            "brand_accent": "#2E0052",
        },
        "security": {"configuration_second_approver": True},
        "sla": {"default_hours": 48},
    }
