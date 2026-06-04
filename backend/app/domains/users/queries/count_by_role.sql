-- users/queries/count_by_role.sql
-- Active user counts by role.
-- Params: $1=org_id

SELECT
    role,
    COUNT(*)::int AS count
FROM users
WHERE org_id = $1
  AND active = TRUE
GROUP BY role
ORDER BY role;
