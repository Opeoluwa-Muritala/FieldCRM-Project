-- users/queries/get_by_email.sql
-- Fetches a single user by email address.
-- Targets the new 'users' table (plural).
-- Params: $1=email

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
WHERE u.email = $1;
