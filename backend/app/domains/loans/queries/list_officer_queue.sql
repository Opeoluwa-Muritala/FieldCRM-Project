-- loans/queries/list_officer_queue.sql
-- Returns a loan officer's personal queue with priority-based ordering.
-- Order: Returned → Missing Docs → OCR Review → Intake (drafts).
-- Params: $1=org_id, $2=officer_user_id, $3=stage_filter (nullable), $4=limit, $5=offset

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
        WHEN 'returned'   THEN 1
        WHEN 'ocr_review' THEN 2
        WHEN 'intake'     THEN 3
        ELSE 4
    END AS priority_order,
    COUNT(*) OVER () AS total_count
FROM loan_applications la
WHERE la.org_id       = $1
  AND la.created_by   = $2
  AND la.deleted_at   IS NULL
  AND la.stage NOT IN ('disbursed', 'rejected')
  AND ($3::text IS NULL OR la.stage = $3::text)
ORDER BY priority_order ASC, la.updated_at DESC
LIMIT $4 OFFSET $5;
