-- loans/queries/dashboard_metrics.sql
-- Returns role-specific metric card values for the dashboard.
-- Uses CASE to return role-appropriate counts in one trip.
-- Params: $1=org_id, $2=user_id, $3=role

SELECT
    -- Loan Officer: their own queue
    COUNT(*) FILTER (
        WHERE $3 = 'loan_officer'
          AND created_by = $2
          AND stage IN ('intake','returned')
    ) AS metric_1,

    -- Loan Officer: pending OCR
    COUNT(*) FILTER (
        WHERE $3 = 'loan_officer'
          AND created_by = $2
          AND stage = 'ocr_review'
    ) AS metric_2,

    -- Branch Manager: awaiting concurrence
    COUNT(*) FILTER (
        WHERE $3 = 'branch_manager'
          AND branch_manager_id = $2
          AND stage = 'branch_approval'
    ) AS metric_3,

    -- Branch Manager: awaiting credit review (credit_officer role removed)
    COUNT(*) FILTER (
        WHERE $3 = 'branch_manager'
          AND branch_manager_id = $2
          AND stage = 'credit_review'
    ) AS metric_4,

    -- All roles: total active
    COUNT(*) FILTER (
        WHERE stage NOT IN ('disbursed','rejected')
    ) AS total_active

FROM loan_applications
WHERE org_id     = $1
  AND deleted_at IS NULL;
