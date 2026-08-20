import json
from uuid import UUID


class ConfigurationRepository:
    def __init__(self, conn):
        self.conn = conn

    async def mfa_state(self, user_id: UUID, org_id: UUID):
        return await self.conn.fetchrow(
            "SELECT config_mfa_enabled, config_mfa_secret_encrypted FROM users WHERE id=$1 AND org_id=$2", user_id, org_id
        )

    async def save_mfa(self, user_id: UUID, org_id: UUID, encrypted: str, enabled: bool):
        return await self.conn.fetchrow(
            "UPDATE users SET config_mfa_secret_encrypted=$1, config_mfa_enabled=$2 WHERE id=$3 AND org_id=$4 RETURNING id",
            encrypted, enabled, user_id, org_id,
        )

    async def organisation_name(self, org_id):
        return await self.conn.fetchval("SELECT name FROM organisations WHERE id=$1", org_id)

    async def current(self, org_id):
        return await self.conn.fetchrow(
            """SELECT * FROM configuration_versions WHERE org_id=$1 AND status='published'
               AND effective_at <= NOW() ORDER BY effective_at DESC, version_number DESC LIMIT 1""", org_id
        )

    async def latest(self, org_id):
        return await self.conn.fetchrow(
            "SELECT * FROM configuration_versions WHERE org_id=$1 ORDER BY version_number DESC LIMIT 1", org_id
        )

    async def get(self, version_id, org_id, *, lock=False):
        suffix = " FOR UPDATE" if lock else ""
        return await self.conn.fetchrow(
            "SELECT * FROM configuration_versions WHERE id=$1 AND org_id=$2" + suffix, version_id, org_id
        )

    async def list(self, org_id):
        return await self.conn.fetch(
            "SELECT * FROM configuration_versions WHERE org_id=$1 ORDER BY version_number DESC LIMIT 100", org_id
        )

    async def create(self, *, org_id, payload, reason, effective_at, actor_id):
        return await self.conn.fetchrow(
            """INSERT INTO configuration_versions
               (org_id,version_number,payload,reason,effective_at,created_by)
               VALUES ($1,COALESCE((SELECT MAX(version_number)+1 FROM configuration_versions WHERE org_id=$1),1),$2::jsonb,$3,COALESCE($4,NOW()),$5)
               RETURNING *""", org_id, json.dumps(payload), reason, effective_at, actor_id,
        )

    async def patch(self, version_id, org_id, payload, high_risk):
        return await self.conn.fetchrow(
            "UPDATE configuration_versions SET payload=$1::jsonb, high_risk=$2 WHERE id=$3 AND org_id=$4 AND status='draft' RETURNING *",
            json.dumps(payload), high_risk, version_id, org_id,
        )

    async def log_change(self, *, org_id, version_id, path, old, new, actor_id, reason):
        await self.conn.execute(
            """INSERT INTO configuration_change_log(org_id,version_id,setting_path,old_value,new_value,changed_by,reason)
               VALUES($1,$2,$3,$4::jsonb,$5::jsonb,$6,$7)""", org_id, version_id, path, json.dumps(old), json.dumps(new), actor_id, reason,
        )

    async def validate(self, version_id, org_id, actor_id, needs_approval):
        status = "pending_approval" if needs_approval else "validated"
        return await self.conn.fetchrow(
            """UPDATE configuration_versions SET status=$1, validated_by=$2, validated_at=NOW(), requires_second_approval=$3
               WHERE id=$4 AND org_id=$5 AND status='draft' RETURNING *""", status, actor_id, needs_approval, version_id, org_id,
        )

    async def approve(self, version_id, org_id, actor_id):
        row = await self.conn.fetchrow(
            """UPDATE configuration_versions SET status='validated', approved_by=$1, approved_at=NOW()
               WHERE id=$2 AND org_id=$3 AND status='pending_approval' AND created_by<>$1 AND validated_by<>$1 RETURNING *""",
            actor_id, version_id, org_id,
        )
        if row:
            await self.conn.execute(
                """INSERT INTO configuration_change_log
                   (org_id,version_id,setting_path,old_value,new_value,changed_by,reason,approver_id)
                   VALUES($1,$2,'_approval','null'::jsonb,jsonb_build_object('approved',TRUE),$3,
                          'Second approval for high-risk configuration',$3)""",
                org_id, version_id, actor_id,
            )
        return row

    async def publish(self, version_id, org_id, actor_id):
        return await self.conn.fetchrow(
            """UPDATE configuration_versions SET status='published',published_by=$1,published_at=NOW()
               WHERE id=$2 AND org_id=$3 AND status='validated' RETURNING *""", actor_id, version_id, org_id,
        )

    async def product_dependencies(self, org_id):
        return await self.conn.fetch(
            "SELECT code,guarantor_required,collateral_required,cbs_enabled FROM loan_products WHERE active=TRUE"
        )
