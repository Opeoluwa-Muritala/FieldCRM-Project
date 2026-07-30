-- Params: $1=org_id
SELECT id, org_id, name, code, active, created_at
FROM branches
WHERE org_id = $1
ORDER BY name;
