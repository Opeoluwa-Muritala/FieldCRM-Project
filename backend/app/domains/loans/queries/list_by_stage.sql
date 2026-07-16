-- loans/queries/list_by_stage.sql
-- Paginated loan list with total count using window function.
-- Nullable filters — pass NULL to skip.
-- Params: $1=org_id, $2=stage (nullable), $3=officer_id (nullable),
--         $4=loan_type (nullable), $5=search query (nullable),
--         $6=from_date (nullable), $7=to_date (nullable), $8=limit, $9=offset

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
  AND ($4::TEXT IS NULL OR LOWER(la.loan_type) = LOWER($4))
  AND (
      $5::TEXT IS NULL
      OR LOWER(la.applicant_name) LIKE '%' || LOWER($5) || '%'
      OR LOWER(la.ref_no) LIKE '%' || LOWER($5) || '%'
      OR LOWER(u.full_name) LIKE '%' || LOWER($5) || '%'
  )
  AND ($6::DATE IS NULL OR DATE(la.created_at) >= $6)
  AND ($7::DATE IS NULL OR DATE(la.created_at) <= $7)
ORDER BY la.updated_at DESC
LIMIT $8 OFFSET $9;
