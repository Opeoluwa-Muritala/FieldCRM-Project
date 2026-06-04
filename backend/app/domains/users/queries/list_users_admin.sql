-- users/queries/list_users_admin.sql
-- System admin user management list.
-- Params: $1=org_id, $2=limit, $3=offset

SELECT
    id,
    org_id,
    full_name,
    email,
    role,
    active,
    last_login_at,
    created_at,
    COUNT(*) OVER () AS total_count
FROM users
WHERE org_id = $1
ORDER BY active DESC, role ASC, full_name ASC
LIMIT $2 OFFSET $3;
