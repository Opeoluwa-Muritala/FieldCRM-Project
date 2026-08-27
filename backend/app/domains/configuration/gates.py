import re


_ROUTE_FEATURES = (
    (re.compile(r"^/my-work(?:/|$)"), "my_work"),
    (re.compile(r"^/my-queue(?:/|$)"), "my_work"),
    (re.compile(r"^/pipeline(?:/|$)"), "pipeline"),
    (re.compile(r"^/document-work-queue(?:/|$)"), "document_work_queue"),
    (re.compile(r"^/my-reviews(?:/|$)"), "credit_reviews"),
    (re.compile(r"^/exceptions(?:/|$)"), "exceptions_centre"),
    (re.compile(r"^/visits(?:/|$)"), "visits"),
    (re.compile(r"^/visitation-reports(?:/|$)"), "visits"),
    (re.compile(r"^/pending-signoffs(?:/|$)"), "visits"),
    (re.compile(r"^/applications/[^/]+/visitation(?:/|$)"), "visits"),
    (re.compile(r"^/api/v1/internal/ocr-worker(?:/|$)"), "ocr"),
    (re.compile(r"^/applications/[^/]+/guarantors(?:/|$)"), "guarantors"),
    (re.compile(r"^/applications/[^/]+/(?:collateral|repayment-feasibility)(?:/|$)"), "collateral"),
    (re.compile(r"^/applications/[^/]+/repayment-schedule(?:/|$)"), "repayment_schedule"),
    (re.compile(r"^/reports/par(?:/|$)"), "par"),
    (re.compile(r"^/api/v1/web/reports/par(?:/|$)"), "par"),
    (re.compile(r"^/mcc(?:/|$)"), "committee_review"),
    (re.compile(r"^/applications/[^/]+/(?:mcc|mcc-vote|mcc-finalize)(?:/|$)"), "committee_review"),
    (re.compile(r"^/api/v1/mobile/applications/[^/]+/(?:mcc-vote|mcc-finalize)(?:/|$)"), "committee_review"),
    (re.compile(r"^/applications/[^/]+/credit-bureau-pull(?:/|$)"), "credit_bureau_integration"),
    (re.compile(r"^/api/v1/mobile/applications/[^/]+/credit-bureau-pull(?:/|$)"), "credit_bureau_integration"),
    (re.compile(r"^/api/v1/workflow/applications/[^/]+/bureau-evidence(?:/|$)"), "manual_credit_bureau_evidence"),
    (re.compile(r"^/legal-queue(?:/|$)"), "legal_review"),
    (re.compile(r"^/ed-queue(?:/|$)"), "ed_review"),
    (re.compile(r"^/md-queue(?:/|$)"), "md_review"),
    (re.compile(r"^/(?:audit-trail|compliance-flags)(?:/|$)"), "audit_intervention"),
)


def required_feature_for_path(path: str) -> str | None:
    """Return the published feature required by an optional application route."""
    for pattern, feature in _ROUTE_FEATURES:
        if pattern.match(path):
            return feature
    return None
