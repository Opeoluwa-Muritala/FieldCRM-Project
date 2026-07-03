-- auth/queries/get_user_by_email.sql
-- Lookup user by email address for authentication.
-- Targets the new 'users' table (plural).
-- Params: $1=email or staff_id

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
WHERE (
    lower(email) = lower($1)
    OR (
        position('@' in $1) = 0
        AND lower(email) LIKE lower($1) || '@%'
    )
)
AND active = TRUE;
