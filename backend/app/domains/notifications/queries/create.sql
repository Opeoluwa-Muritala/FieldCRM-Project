-- Params: $1=user_id, $2=org_id, $3=application_id,
--         $4=title, $5=message, $6=type

INSERT INTO notifications (
    user_id,
    org_id,
    application_id,
    title,
    message,
    type,
    is_read
)
VALUES ($1, $2, $3, $4, $5, $6, FALSE)
RETURNING
    id,
    title,
    message,
    created_at,
    is_read,
    application_id,
    type;

