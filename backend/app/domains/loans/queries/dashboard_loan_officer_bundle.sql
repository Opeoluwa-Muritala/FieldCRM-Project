-- Complete account-officer dashboard in one database round trip.
-- Params: $1=org_id, $2=user_id

WITH metrics AS (
    SELECT
        COUNT(*) FILTER (
            WHERE created_by = $2 AND stage NOT IN ('disbursed', 'rejected')
        ) AS my_applications,
        COUNT(*) FILTER (
            WHERE created_by = $2 AND stage = 'intake'
        ) AS pending_upload,
        COUNT(*) FILTER (
            WHERE created_by = $2 AND stage = 'ocr_review'
        ) AS ocr_review_count,
        COUNT(*) FILTER (
            WHERE created_by = $2 AND stage = 'returned'
        ) AS returned_count,
        COUNT(*) FILTER (
            WHERE created_by = $2 AND stage = 'intake'
        ) AS drafts_count
    FROM loan_applications
    WHERE org_id = $1
      AND deleted_at IS NULL
),
tasks AS (
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
    WHERE la.org_id = $1
      AND la.created_by = $2
      AND la.deleted_at IS NULL
      AND la.stage IN ('returned', 'ocr_review', 'intake')
    ORDER BY priority_order, la.updated_at DESC
    LIMIT 10
),
queue AS (
    SELECT
        la.id,
        la.ref_no,
        la.loan_type,
        la.stage,
        la.amount,
        la.applicant_name,
        la.return_reason,
        la.returned_at,
        la.created_at,
        la.updated_at,
        CASE la.stage
            WHEN 'returned' THEN 1
            WHEN 'ocr_review' THEN 2
            WHEN 'intake' THEN 3
            ELSE 4
        END AS priority_order,
        COUNT(*) OVER () AS total_count
    FROM loan_applications la
    WHERE la.org_id = $1
      AND la.created_by = $2
      AND la.deleted_at IS NULL
      AND la.stage NOT IN ('disbursed', 'rejected')
    ORDER BY priority_order, la.updated_at DESC
    LIMIT 10
),
visits AS (
    SELECT
        la.id AS loan_id,
        la.ref_no,
        la.applicant_name,
        la.amount,
        la.stage,
        la.created_at AS application_date
    FROM loan_applications la
    LEFT JOIN visitation_reports vr ON vr.loan_id = la.id
    WHERE la.org_id = $1
      AND la.created_by = $2
      AND la.deleted_at IS NULL
      AND la.stage IN ('intake', 'branch_manager_review', 'branch_supervisor_review', 'credit_analyst_review')
      AND vr.id IS NULL
    ORDER BY la.created_at
    LIMIT 20
)
SELECT
    to_jsonb(metrics) AS metrics,
    COALESCE((SELECT jsonb_agg(to_jsonb(tasks) ORDER BY priority_order, updated_at DESC) FROM tasks), '[]'::jsonb) AS tasks,
    COALESCE((SELECT jsonb_agg(to_jsonb(queue) ORDER BY priority_order, updated_at DESC) FROM queue), '[]'::jsonb) AS queue,
    COALESCE((SELECT jsonb_agg(to_jsonb(visits) ORDER BY application_date) FROM visits), '[]'::jsonb) AS visits_due
FROM metrics;
