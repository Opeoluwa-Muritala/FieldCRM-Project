-- audit/queries/insert_entry.sql
-- Appends one immutable audit record.
-- No UPDATE or DELETE is ever run on this table — enforced at DB level.
-- Params: $1=org_id, $2=entity_type, $3=entity_id, $4=action,
--         $5=user_id, $6=user_role, $7=field_name, $8=old_value,
--         $9=new_value, $10=source, $11=notes, $12=request_id

INSERT INTO audit_entries (
    org_id, entity_type, entity_id, action,
    user_id, user_role, field_name, old_value,
    new_value, source, notes, request_id
)
VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12);
