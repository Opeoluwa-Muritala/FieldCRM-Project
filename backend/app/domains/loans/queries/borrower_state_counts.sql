-- Current-loan state counts for one organisation.
-- Params: $1=org_id

SELECT
    COUNT(*) AS total,
    COUNT(*) FILTER (WHERE stage = 'intake') AS draft,
    COUNT(*) FILTER (
        WHERE stage IN (
            'ocr_review',
            'branch_manager_review',
            'branch_supervisor_review',
            'credit_analyst_review',
            'credit_review',
            'branch_approval'
        )
    ) AS review,
    COUNT(*) FILTER (WHERE stage = 'disbursement_ready') AS approved,
    COUNT(*) FILTER (WHERE stage = 'disbursed') AS active
FROM loan_applications
WHERE org_id = $1
  AND deleted_at IS NULL;
