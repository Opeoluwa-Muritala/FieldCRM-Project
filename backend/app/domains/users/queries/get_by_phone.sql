-- users/queries/get_by_phone.sql
-- Compatibility lookup for older callers; treats the value as an email.
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
