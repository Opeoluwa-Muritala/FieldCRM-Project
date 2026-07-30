-- users/queries/list_users_admin.sql
-- System admin user management list.
-- Params: $1=org_id, $2=limit, $3=offset

SELECT
    u.id,
    u.org_id,
    u.full_name,
    u.email,
    u.role,
    u.active,
    u.branch_id,
    b.name AS branch_name,
    u.last_login_at,
    u.created_at,
    COUNT(*) OVER () AS total_count
FROM users u
LEFT JOIN branches b ON b.id = u.branch_id
WHERE u.org_id = $1
ORDER BY u.active DESC, u.role ASC, u.full_name ASC
LIMIT $2 OFFSET $3;
