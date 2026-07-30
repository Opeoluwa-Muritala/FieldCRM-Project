-- Params: $1=org_id, $2=name, $3=code
INSERT INTO branches (org_id, name, code)
VALUES ($1, $2, $3)
RETURNING id, org_id, name, code, active, created_at;
