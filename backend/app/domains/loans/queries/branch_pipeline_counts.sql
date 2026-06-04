-- loans/queries/branch_pipeline_counts.sql
-- Stage counts for applications assigned to a branch manager.
-- Params: $1=org_id, $2=branch_manager_user_id

SELECT
    stage,
    COUNT(*)::int AS count,
    COALESCE(SUM(amount), 0) AS total_amount
FROM loan_applications
WHERE org_id = $1
  AND branch_manager_id = $2
  AND deleted_at IS NULL
GROUP BY stage
ORDER BY
    CASE stage
        WHEN 'intake' THEN 1
        WHEN 'ocr_review' THEN 2
        WHEN 'credit_review' THEN 3
        WHEN 'branch_approval' THEN 4
        WHEN 'disbursement_ready' THEN 5
        WHEN 'disbursed' THEN 6
        WHEN 'returned' THEN 7
        WHEN 'rejected' THEN 8
        ELSE 9
    END;
