-- users/queries/get_by_id.sql
-- Fetches a single user by UUID.
-- Targets the new 'users' table (plural).
-- Params: $1=user_id

SELECT
    u.id,
    u.org_id,
    u.full_name,
    u.email,
    u.password_hash,
    u.role,
    u.active,
    u.branch_id,
    b.name AS branch_name,
    u.last_login_at,
    u.created_at
FROM users u
LEFT JOIN branches b ON b.id = u.branch_id
WHERE u.id = $1;
