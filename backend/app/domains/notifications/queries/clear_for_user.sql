-- Params: $1=user_id, $2=org_id

DELETE FROM notifications
WHERE user_id = $1
  AND org_id = $2;
