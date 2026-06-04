-- loans/queries/list_officer_tasks.sql
-- Returns actionable task items for a loan officer's "Today's Tasks" panel.
-- Each row represents one task with a type indicator for the UI.
-- Tasks: returned apps, missing docs, OCR review needed, pending visits.
-- Params: $1=org_id, $2=officer_user_id

SELECT
    la.id AS loan_id,
    la.ref_no,
    la.applicant_name,
    la.amount,
    la.stage,
    la.updated_at,
    la.return_reason,
    CASE
        WHEN la.stage = 'returned' THEN 'returned'
        WHEN la.stage = 'ocr_review' THEN 'ocr_review'
        WHEN la.stage = 'intake' THEN 'draft'
        ELSE 'other'
    END AS task_type,
    CASE
        WHEN la.stage = 'returned' THEN 'Application returned — needs correction'
        WHEN la.stage = 'ocr_review' THEN 'OCR review pending'
        WHEN la.stage = 'intake' THEN 'Draft application — continue intake'
        ELSE 'Action required'
    END AS task_description,
    CASE
        WHEN la.stage = 'returned' THEN 1
        WHEN la.stage = 'ocr_review' THEN 2
        WHEN la.stage = 'intake' THEN 3
        ELSE 4
    END AS priority_order
FROM loan_applications la
WHERE la.org_id       = $1
  AND la.created_by   = $2
  AND la.deleted_at   IS NULL
  AND la.stage IN ('returned', 'ocr_review', 'intake')
ORDER BY priority_order ASC, la.updated_at DESC
LIMIT 10;
