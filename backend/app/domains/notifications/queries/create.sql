-- Params: $1=user_id, $2=org_id, $3=application_id,
--         $4=title, $5=message, $6=type

SELECT * FROM public.app_create_notification($1, $2, $3, $4, $5, $6);
