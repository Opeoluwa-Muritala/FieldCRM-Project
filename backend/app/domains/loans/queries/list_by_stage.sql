-- loans/queries/list_by_stage.sql
-- Paginated loan list with total count using window function.
-- Nullable filters — pass NULL to skip.
-- Params: $1=org_id, $2=stage (nullable), $3=officer_id (nullable),
--         $4=limit, $5=offset

SELECT
    la.id,
    la.ref_no,
    la.loan_type,
    la.stage,
    la.amount,
    la.applicant_name,
    la.created_at,
    la.updated_at,
    u.full_name             AS officer_name,
    COUNT(*) OVER ()        AS total_count
FROM loan_applications la
JOIN users u ON u.id = la.created_by
WHERE la.org_id       = $1
  AND la.deleted_at   IS NULL
  AND ($2::TEXT IS NULL OR la.stage = $2)
  AND ($3::UUID IS NULL OR la.created_by = $3)
ORDER BY la.updated_at DESC
LIMIT $4 OFFSET $5;
