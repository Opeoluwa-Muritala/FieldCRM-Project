-- users/queries/get_by_id.sql
-- Fetches a single user by UUID.
-- Targets the new 'users' table (plural).
-- Params: $1=user_id

SELECT
    id,
    org_id,
    full_name,
    email,
    password_hash,
    role,
    active,
    last_login_at,
    created_at
FROM users
WHERE id = $1;
