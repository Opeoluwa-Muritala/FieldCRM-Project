-- Immutable field, document, and application activity for one tenant/application.
-- Params: $1=org_id, $2=loan_id, $3=limit, $4=offset

SELECT
    ae.id,
    ae.action,
    ae.user_id AS actor_id,
    ae.user_role AS actor_role,
    u.full_name AS actor_name,
    ae.field_name,
    ae.old_value,
    ae.new_value,
    ae.source,
    ae.notes,
    ae.created_at,
    ae.entity_type,
    ae.entity_id
FROM audit_entries ae
LEFT JOIN users u
  ON u.id = ae.user_id
 AND u.org_id = ae.org_id
WHERE ae.org_id = $1
  AND (
      (ae.entity_type = 'loan_application' AND ae.entity_id = $2)
      OR (
          ae.entity_type = 'document'
          AND EXISTS (
              SELECT 1
              FROM documents d
              WHERE d.id = ae.entity_id
                AND d.loan_id = $2
                AND d.org_id = $1
          )
      )
  )
ORDER BY ae.created_at DESC, ae.id DESC
LIMIT $3 OFFSET $4;
