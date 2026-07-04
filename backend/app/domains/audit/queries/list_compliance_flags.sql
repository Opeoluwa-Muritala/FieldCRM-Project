-- audit/queries/list_compliance_flags.sql
-- Concrete compliance flags across documents, OCR fields, and workflow exceptions.
-- Params: $1=org_id, $2=limit, $3=offset

SELECT *
FROM (
    SELECT
        d.loan_id,
        la.ref_no,
        la.applicant_name,
        'document' AS flag_type,
        d.doc_type AS flag_label,
        d.quality_status AS flag_status,
        d.uploaded_at AS created_at
    FROM documents d
    JOIN loan_applications la ON la.id = d.loan_id
    WHERE d.org_id = $1
      AND d.deleted_at IS NULL
      AND (d.verified = FALSE OR d.quality_status IN ('blurry', 'cropped', 'unreadable'))

    UNION ALL

    SELECT
        of.loan_id,
        la.ref_no,
        la.applicant_name,
        'ocr' AS flag_type,
        of.field_name AS flag_label,
        CASE WHEN of.is_critical THEN 'critical' ELSE 'low_confidence' END AS flag_status,
        of.created_at
    FROM ocr_fields of
    JOIN loan_applications la ON la.id = of.loan_id
    WHERE la.org_id = $1
      AND la.deleted_at IS NULL
      AND of.verified = FALSE
      AND (of.confidence < 70 OR of.is_critical = TRUE)

    UNION ALL

    SELECT
        we.loan_id,
        la.ref_no,
        la.applicant_name,
        'workflow' AS flag_type,
        we.event_type AS flag_label,
        COALESCE(we.to_stage, we.from_stage, 'event') AS flag_status,
        we.created_at
    FROM workflow_events we
    JOIN loan_applications la ON la.id = we.loan_id
    WHERE we.org_id = $1
      AND we.event_type IN ('loan.returned', 'loan.rejected')
) flags
ORDER BY 7 DESC
LIMIT $2 OFFSET $3;
