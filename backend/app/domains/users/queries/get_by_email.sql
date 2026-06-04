-- users/queries/get_by_email.sql
-- Fetches a single user by email address.
-- Targets the new 'users' table (plural).
-- Params: $1=email

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
WHERE email = $1;
