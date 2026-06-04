-- users/queries/create_user.sql
-- Inserts a new user and returns the full row.
-- Targets the new 'users' table (plural).
-- Params: $1=org_id, $2=full_name, $3=email, $4=role, $5=password_hash

INSERT INTO users (org_id, full_name, email, role, password_hash)
VALUES ($1, $2, $3, $4, $5)
RETURNING
    id,
    org_id,
    full_name,
    email,
    password_hash,
    role,
    active,
    last_login_at,
    created_at;
