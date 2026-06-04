-- loans/queries/dashboard_auditor.sql
-- Auditor dashboard metrics.
-- Params: $1=org_id

SELECT
    (
        SELECT COUNT(*)
        FROM documents d
        WHERE d.org_id = $1
          AND d.deleted_at IS NULL
          AND d.verified = FALSE
    ) AS unverified_documents,
    (
        SELECT COUNT(*)
        FROM ocr_fields of
        JOIN loan_applications la ON la.id = of.loan_id
        WHERE la.org_id = $1
          AND la.deleted_at IS NULL
          AND of.verified = FALSE
          AND of.is_critical = TRUE
    ) AS critical_ocr_gaps,
    (
        SELECT COUNT(*)
        FROM workflow_events we
        WHERE we.org_id = $1
          AND we.event_type IN ('loan.returned', 'loan.rejected')
    ) AS workflow_exceptions,
    (
        SELECT COUNT(*)
        FROM audit_entries ae
        WHERE ae.org_id = $1
          AND ae.created_at >= NOW() - INTERVAL '24 hours'
    ) AS audit_events_today,
    COUNT(*) FILTER (
        WHERE stage IN ('branch_approval', 'disbursement_ready')
    ) AS files_at_approval
FROM loan_applications
WHERE org_id = $1
  AND deleted_at IS NULL;
