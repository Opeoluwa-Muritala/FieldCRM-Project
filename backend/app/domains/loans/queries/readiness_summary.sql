-- loans/queries/readiness_summary.sql
-- Single query that computes the full approval readiness state for a loan.
-- Returns one row with counts and boolean flags.
-- Params: $1=loan_id, $2=org_id

WITH loan AS (
    SELECT id, stage, org_id
    FROM loan_applications
    WHERE id = $1 AND org_id = $2 AND deleted_at IS NULL
),
forms AS (
    SELECT
        COUNT(*) FILTER (WHERE form_code = 'MMFB/CRM/01') AS loan_form_count,
        COUNT(*) FILTER (WHERE form_code = 'MMFB/CRM/02') AS pledge_form_count,
        COUNT(*) FILTER (WHERE form_code = 'MMFB/CRM/03') AS guarantor_form_count
    FROM documents
    WHERE loan_id = $1 AND deleted_at IS NULL
),
docs AS (
    SELECT
        COUNT(*) FILTER (WHERE verified = TRUE)  AS verified_docs,
        COUNT(*) FILTER (WHERE verified = FALSE) AS unverified_docs,
        COUNT(*)                                  AS total_docs
    FROM documents
    WHERE loan_id = $1 AND deleted_at IS NULL
),
ocr AS (
    SELECT
        COALESCE(COUNT(*) FILTER (WHERE confidence < 70 AND verified = FALSE), 0)   AS low_confidence_unverified,
        COALESCE(COUNT(*) FILTER (WHERE is_critical = TRUE AND verified = FALSE), 0) AS critical_unverified
    FROM ocr_fields
    WHERE loan_id = $1
),
guar AS (
    SELECT
        COALESCE(COUNT(*) FILTER (WHERE form_stage = 'verified'), 0)  AS verified_guarantors,
        COALESCE(COUNT(*), 0)                                          AS total_guarantors
    FROM guarantors
    WHERE loan_id = $1
),
visit AS (
    SELECT
        COALESCE(status, 'pending')                        AS status,
        COALESCE(manager_concurrence, FALSE)               AS manager_concurrence,
        COALESCE(visiting_officer_signature, FALSE)        AS visiting_officer_signature
    FROM visitation_reports
    WHERE loan_id = $1
),
wf AS (
    SELECT
        MAX(created_at) FILTER (WHERE event_type = 'credit_review.completed') IS NOT NULL AS credit_reviewed,
        MAX(created_at) FILTER (WHERE event_type = 'branch.approved')         IS NOT NULL AS branch_approved
    FROM workflow_events
    WHERE loan_id = $1
),
consents AS (
    SELECT
        data_json -> 'consents' ->> 'credit_bureau'   AS credit_bureau,
        data_json -> 'consents' ->> 'credit_check'    AS credit_check,
        data_json -> 'consents' ->> 'gsi_mandate'     AS gsi_mandate,
        data_json -> 'consents' ->> 'cheque_authority' AS cheque_authority
    FROM stage_data
    WHERE loan_id = $1 AND stage = 'intake'
    ORDER BY saved_at DESC
    LIMIT 1
)
SELECT
    COALESCE(f.loan_form_count, 0)      > 0             AS loan_form_submitted,
    COALESCE(f.pledge_form_count, 0)    > 0             AS pledge_form_submitted,
    COALESCE(f.guarantor_form_count, 0) > 0             AS guarantor_form_submitted,
    COALESCE(g.verified_guarantors, 0)                  AS guarantors_verified,
    COALESCE(g.total_guarantors, 0)                     AS guarantors_required,
    COALESCE(d.verified_docs, 0)                        AS verified_docs,
    COALESCE(d.unverified_docs, 0)                      AS unverified_docs,
    COALESCE(d.total_docs, 0)                           AS total_docs,
    COALESCE(o.low_confidence_unverified, 0)            AS low_confidence_unverified,
    COALESCE(o.critical_unverified, 0)                  AS critical_unverified,
    COALESCE(c.credit_bureau,    'false') = 'true'      AS consent_credit_bureau,
    COALESCE(c.credit_check,     'false') = 'true'      AS consent_credit_check,
    COALESCE(c.gsi_mandate,      'false') = 'true'      AS consent_gsi,
    COALESCE(c.cheque_authority, 'false') = 'true'      AS consent_cheque,
    COALESCE(v.status, 'pending')                       AS visitation_status,
    COALESCE(v.manager_concurrence, FALSE)              AS manager_concurred,
    COALESCE(v.visiting_officer_signature, FALSE)       AS officer_signed_visitation,
    COALESCE(wf.credit_reviewed, FALSE)                 AS credit_reviewed,
    COALESCE(wf.branch_approved, FALSE)                 AS branch_approved,
    -- Readiness gate
    (
        COALESCE(f.loan_form_count, 0) > 0
        AND COALESCE(g.verified_guarantors, 0) = COALESCE(g.total_guarantors, 0)
        AND COALESCE(g.total_guarantors, 0) > 0
        AND COALESCE(d.unverified_docs, 0) = 0
        AND COALESCE(o.critical_unverified, 0) = 0
        AND COALESCE(o.low_confidence_unverified, 0) = 0
        AND COALESCE(c.gsi_mandate, 'false') = 'true'
        AND COALESCE(c.credit_bureau, 'false') = 'true'
        AND COALESCE(v.manager_concurrence, FALSE) = TRUE
        AND COALESCE(wf.credit_reviewed, FALSE) = TRUE
    ) AS ready_for_approval
FROM forms f
CROSS JOIN docs d
CROSS JOIN ocr o
CROSS JOIN guar g
LEFT JOIN visit v ON TRUE
CROSS JOIN wf
LEFT JOIN consents c ON TRUE;
