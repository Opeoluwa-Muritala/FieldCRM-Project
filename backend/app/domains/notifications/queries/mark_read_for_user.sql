-- Params: $1=notification_id, $2=user_id, $3=org_id

UPDATE notifications
SET is_read = TRUE
WHERE id = $1
  AND user_id = $2
  AND org_id = $3;

