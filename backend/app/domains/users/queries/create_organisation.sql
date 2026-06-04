-- users/queries/create_organisation.sql
-- Inserts a new organization and returns the full row.
-- Targets the new 'organisations' table (plural).
-- Params: $1=name, $2=code

INSERT INTO organisations (name, code)
VALUES ($1, $2)
RETURNING id, name, code, active, created_at;
