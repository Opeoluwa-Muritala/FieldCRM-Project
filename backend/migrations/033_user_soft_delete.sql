ALTER TABLE users ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ;
CREATE INDEX IF NOT EXISTS ix_users_org_active_not_deleted
    ON users(org_id, active) WHERE deleted_at IS NULL;
