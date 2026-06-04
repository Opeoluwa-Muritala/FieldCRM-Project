-- loans/queries/dashboard_loan_officer.sql
-- Returns loan officer-specific dashboard metrics.
-- Scoped to the officer's own applications only (created_by = $2).
-- Params: $1=org_id, $2=user_id

SELECT
    -- My Applications (active, not terminal states)
    COUNT(*) FILTER (
        WHERE created_by = $2
          AND stage NOT IN ('disbursed', 'rejected')
    ) AS my_applications,

    -- Pending Upload (intake stage, officer's own apps)
    COUNT(*) FILTER (
        WHERE created_by = $2
          AND stage = 'intake'
    ) AS pending_upload,

    -- OCR Review (officer's apps awaiting OCR)
    COUNT(*) FILTER (
        WHERE created_by = $2
          AND stage = 'ocr_review'
    ) AS ocr_review_count,

    -- Returned (returned apps needing attention)
    COUNT(*) FILTER (
        WHERE created_by = $2
          AND stage = 'returned'
    ) AS returned_count,

    -- Drafts (intake stage only)
    COUNT(*) FILTER (
        WHERE created_by = $2
          AND stage = 'intake'
    ) AS drafts_count,

    -- Total active across org (for context)
    COUNT(*) FILTER (
        WHERE stage NOT IN ('disbursed', 'rejected')
    ) AS total_active

FROM loan_applications
WHERE org_id     = $1
  AND deleted_at IS NULL;
