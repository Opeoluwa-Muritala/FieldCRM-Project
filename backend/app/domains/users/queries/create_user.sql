-- users/queries/create_user.sql
-- Inserts a new user and returns the full row.
-- Targets the new 'users' table (plural).
-- Params: $1=org_id, $2=full_name, $3=email, $4=role, $5=password_hash, $6=branch_id

INSERT INTO users (org_id, full_name, email, role, password_hash, branch_id)
VALUES ($1, $2, $3, $4, $5, $6)
RETURNING
    id,
    org_id,
    full_name,
    email,
    password_hash,
    role,
    active,
    branch_id,
    last_login_at,
    created_at;
