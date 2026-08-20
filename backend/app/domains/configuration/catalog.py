FEATURE_DEFAULTS = {
    "external_applicant_portal": False,
    "ocr": False,
    "visits": True,
    "gps": True,
    "guarantors": True,
    "collateral": True,
    "credit_bureau_integration": False,
    "manual_credit_bureau_evidence": True,
    "cbs_integration": False,
    "manual_repayment": True,
    "repayment_schedule": True,
    "par": False,
    "collections": False,
    "committee_review": False,
    "legal_review": False,
    "ed_review": True,
    "md_review": True,
    "audit_intervention": True,
    "sms": False,
    "email": False,
    "push_notifications": False,
    "offline_mode": False,
}

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
