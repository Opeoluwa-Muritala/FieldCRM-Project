"""Central object-level authorization for loan dossiers.

The helpers in this module are deliberately framework-light so the web, mobile,
document, and background entry points cannot drift into different policies.
Database RLS is a second enforcement layer; these checks remain the source of
truth for workflow-stage business rules and user-facing capability flags.
"""
from __future__ import annotations

from dataclasses import asdict, dataclass
from typing import Any

from fastapi import HTTPException, status


ROLE_ALIASES = {
    "admin": "system_admin",
    "loan_officer": "account_officer",
    "relationship_officer": "account_officer",
    "team_lead": "branch_manager",
    "supervisor": "branch_supervisor",
}


def canonical_role(value: str | None) -> str:
    normalized = (value or "").strip().lower().replace(" ", "_")
    return ROLE_ALIASES.get(normalized, normalized)


def _value(subject: Any, name: str):
    if isinstance(subject, dict):
        return subject.get(name)
    return getattr(subject, name, None)


def _same(left: Any, right: Any) -> bool:
    return left is not None and right is not None and str(left) == str(right)


def _same_org(user: Any, app: Any) -> bool:
    return _same(_value(user, "org_id"), _value(app, "org_id"))


def _same_branch(user: Any, app: Any) -> bool:
    user_branch = _value(user, "branch_id")
    app_branch = _value(app, "branch_id")
    # Old fixtures can lack branch data. Missing branch scope never authorizes
    # a mutation, but an assigned manager can still read the legacy record.
    return _same(user_branch, app_branch)


REVIEW_ROLES = {
    "branch_supervisor",
    "credit_analyst",
    "crm",
    "head_crm",
    "auditor",
    "ed",
    "md",
    "legal",
}


def can_view_loan(user: Any, app: Any) -> bool:
    if not _same_org(user, app) or not bool(_value(user, "is_active") if _value(user, "is_active") is not None else True):
        return False
    role = canonical_role(_value(user, "role"))
    if role == "account_officer":
        return _same(_value(app, "created_by"), _value(user, "id"))
    if role == "branch_manager":
        return _same_branch(user, app) or _same(_value(app, "branch_manager_id"), _value(user, "id"))
    if role in REVIEW_ROLES:
        return True
    # System administrators manage identities/configuration, not loan dossiers.
    return False


def can_edit_intake(user: Any, app: Any) -> bool:
    if not can_view_loan(user, app):
        return False
    role = canonical_role(_value(user, "role"))
    stage = _value(app, "stage")
    if role == "account_officer":
        return stage == "intake" and _same(_value(app, "created_by"), _value(user, "id"))
    if role == "branch_manager":
        return (
            stage == "branch_manager_review"
            and _same_branch(user, app)
            and _same(_value(app, "branch_manager_id"), _value(user, "id"))
        )
    return False


CRM_DOCUMENT_TYPES = {
    "offer_acceptance",
    "disbursement_mandate",
    "direct_debit_mandate",
    "insurance_certificate",
    "legal_clearance",
    "other_crm",
    "crm_memo",
}


def can_upload_document(user: Any, app: Any, document_type: str | None = None) -> bool:
    role = canonical_role(_value(user, "role"))
    if document_type in CRM_DOCUMENT_TYPES:
        return can_view_loan(user, app) and role in {"crm", "head_crm"}
    return can_edit_intake(user, app)


@dataclass(frozen=True)
class LoanCapabilities:
    can_view: bool
    can_edit_intake: bool
    can_upload_documents: bool
    can_download_documents: bool
    can_export: bool
    can_reveal_sensitive_data: bool

    def to_dict(self) -> dict[str, bool]:
        return asdict(self)


def capabilities_for(user: Any, app: Any) -> LoanCapabilities:
    view = can_view_loan(user, app)
    role = canonical_role(_value(user, "role"))
    return LoanCapabilities(
        can_view=view,
        can_edit_intake=can_edit_intake(user, app),
        can_upload_documents=can_upload_document(user, app),
        can_download_documents=view,
        can_export=view and role in {"auditor", "crm", "head_crm", "ed", "md"},
        can_reveal_sensitive_data=view and role in {"credit_analyst", "auditor"},
    )


def require_view(user: Any, app: Any) -> None:
    if not can_view_loan(user, app):
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="You do not have permission to access this application")


def require_intake_edit(user: Any, app: Any) -> None:
    if not can_edit_intake(user, app):
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="This intake is read-only for your role, assignment, branch, or workflow stage")


def require_document_upload(user: Any, app: Any, document_type: str | None = None) -> None:
    if not can_upload_document(user, app, document_type):
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="You do not have permission to upload documents for this application")
