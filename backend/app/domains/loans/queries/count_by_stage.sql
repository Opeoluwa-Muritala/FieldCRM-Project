-- loans/queries/count_by_stage.sql
-- Returns count of active loans per stage for the pipeline widget.
-- Params: $1=org_id

SELECT
    stage,
    COUNT(*) AS count
FROM loan_applications
WHERE org_id     = $1
  AND deleted_at IS NULL
GROUP BY stage
ORDER BY
    CASE stage
        WHEN 'intake'               THEN 1
        WHEN 'ocr_review'           THEN 2
        WHEN 'credit_review'        THEN 3
        WHEN 'branch_approval'      THEN 4
        WHEN 'disbursement_ready'   THEN 5
        WHEN 'disbursed'            THEN 6
        WHEN 'returned'             THEN 7
        WHEN 'rejected'             THEN 8
    END;
