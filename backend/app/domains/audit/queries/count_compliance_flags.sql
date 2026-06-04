-- audit/queries/count_compliance_flags.sql
-- Compliance exception counts for auditor dashboards.
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
        FROM documents d
        WHERE d.org_id = $1
          AND d.deleted_at IS NULL
          AND d.quality_status IN ('blurry', 'cropped', 'unreadable')
    ) AS poor_quality_documents,
    (
        SELECT COUNT(*)
        FROM ocr_fields of
        JOIN loan_applications la ON la.id = of.loan_id
        WHERE la.org_id = $1
          AND la.deleted_at IS NULL
          AND of.is_critical = TRUE
          AND of.verified = FALSE
    ) AS unsigned_or_critical_forms,
    (
        SELECT COUNT(*)
        FROM workflow_events we
        WHERE we.org_id = $1
          AND we.event_type IN ('loan.returned', 'loan.rejected')
    ) AS returned_or_rejected_files;
