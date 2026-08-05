-- Keep executive authority singular within each organisation.
CREATE UNIQUE INDEX IF NOT EXISTS uq_users_one_active_ed_per_org
    ON users (org_id) WHERE active = TRUE AND role = 'ed';

CREATE UNIQUE INDEX IF NOT EXISTS uq_users_one_active_md_per_org
    ON users (org_id) WHERE active = TRUE AND role = 'md';
