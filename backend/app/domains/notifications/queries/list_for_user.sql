-- Params: $1=user_id, $2=org_id

SELECT
    id,
    title,
    message,
    created_at,
    is_read,
    application_id,
    type
FROM notifications
WHERE user_id = $1
  AND org_id = $2
ORDER BY created_at DESC;

