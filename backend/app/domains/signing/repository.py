from __future__ import annotations

import json
from uuid import UUID, uuid4


class SigningRepository:
    def __init__(self, conn):
        self.conn = conn

    async def latest_version(self, application_id: UUID, subject_type: str, subject_id: str):
        return await self.conn.fetchrow(
            """SELECT * FROM document_versions
               WHERE application_id=$1 AND subject_type=$2 AND subject_id=$3
                 AND status IN ('draft','sent','signed')
               ORDER BY version_number DESC LIMIT 1""",
            application_id, subject_type, str(subject_id),
        )

    async def create_draft(self, application_id, subject_type, subject_id, payload, created_by, version_number=1):
        return await self.conn.fetchrow(
            """INSERT INTO document_versions
               (application_id,subject_type,subject_id,version_number,payload,created_by)
               VALUES ($1,$2,$3,$4,$5::jsonb,$6) RETURNING *""",
            application_id, subject_type, str(subject_id), version_number,
            json.dumps(payload), created_by,
        )

    async def update_draft_payload(self, version_id, payload):
        return await self.conn.fetchrow(
            "UPDATE document_versions SET payload=$2::jsonb WHERE id=$1 AND status='draft' RETURNING *",
            version_id, json.dumps(payload),
        )

    async def log_edit(self, version_id, field_name, old_value, new_value, edited_by):
        return await self.conn.fetchrow(
            """INSERT INTO field_edit_log(document_version_id,field_name,old_value,new_value,edited_by)
               VALUES ($1,$2,$3,$4,$5) RETURNING *""",
            version_id, field_name, old_value, new_value, edited_by,
        )

    async def freeze(self, version_id, payload, payload_hash):
        return await self.conn.fetchrow(
            """UPDATE document_versions SET payload=$2::jsonb,payload_hash=$3,status='sent',frozen_at=now()
               WHERE id=$1 AND status='draft' RETURNING *""",
            version_id, json.dumps(payload), payload_hash,
        )

    async def supersede_with_draft(self, old, created_by):
        new_id = uuid4()
        await self.conn.execute(
            "UPDATE document_versions SET status='superseded',superseded_by=$2 WHERE id=$1",
            old["id"], new_id,
        )
        return await self.conn.fetchrow(
            """INSERT INTO document_versions
               (id,application_id,subject_type,subject_id,version_number,payload,created_by)
               VALUES ($1,$2,$3,$4,$5,$6::jsonb,$7) RETURNING *""",
            new_id, old["application_id"], old["subject_type"], old["subject_id"],
            int(old["version_number"]) + 1, json.dumps(old["payload"]), created_by,
        )

    async def invalidate_sessions(self, application_id, subject_type, subject_id):
        return await self.conn.execute(
            """UPDATE signing_sessions SET invalidated_at=now()
               WHERE application_id=$1 AND subject_type=$2 AND subject_id=$3
                 AND invalidated_at IS NULL AND used_at IS NULL AND expires_at>now()""",
            application_id, subject_type, str(subject_id),
        )

    async def create_signing_session(self, application_id, subject_type, subject_id,
                                     version_ids, issued_by, expires_at, token_hash=None):
        return await self.conn.fetchrow(
            """INSERT INTO signing_sessions
               (application_id,subject_type,subject_id,document_version_ids,issued_by,expires_at,token_hash)
               VALUES ($1,$2,$3,$4::jsonb,$5,$6,$7) RETURNING *""",
            application_id, subject_type, str(subject_id),
            json.dumps([str(value) for value in version_ids]), issued_by, expires_at, token_hash,
        )

    async def redeem_link(self, token_hash, session_expires_at):
        return await self.conn.fetchrow(
            """UPDATE signing_sessions
               SET redeemed_at=now(), session_expires_at=$2
               WHERE token_hash=$1 AND redeemed_at IS NULL AND invalidated_at IS NULL
                 AND used_at IS NULL AND expires_at>now()
               RETURNING *""", token_hash, session_expires_at,
        )

    async def authenticate_redeemed_link(self, signing_session_id, transaction_id, expires_at):
        return await self.conn.fetchrow(
            """INSERT INTO signing_auth_sessions
               (signing_session_id,auth_method,transaction_id,expires_at,verified_at)
               VALUES ($1,'single_use_link',$2,$3,now()) RETURNING *""",
            signing_session_id, transaction_id, expires_at,
        )

    async def mark_viewed(self, token_jti):
        return await self.conn.fetchrow(
            """UPDATE signing_sessions SET document_viewed_at=COALESCE(document_viewed_at,now())
               WHERE token_jti=$1 AND invalidated_at IS NULL AND used_at IS NULL AND expires_at>now()
               RETURNING *""", token_jti,
        )

    async def create_auth_session(self, signing_session_id, auth_method, transaction_id,
                                  otp_digest, expires_at):
        return await self.conn.fetchrow(
            """INSERT INTO signing_auth_sessions
               (signing_session_id,auth_method,transaction_id,otp_digest,expires_at)
               VALUES ($1,$2,$3,$4,$5) RETURNING *""",
            signing_session_id, auth_method, transaction_id, otp_digest, expires_at,
        )

    async def get_pending_auth(self, transaction_id):
        return await self.conn.fetchrow(
            """SELECT * FROM signing_auth_sessions
               WHERE transaction_id=$1 AND verified_at IS NULL AND used_at IS NULL AND expires_at>now()""",
            transaction_id,
        )

    async def mark_auth_verified(self, transaction_id):
        return await self.conn.fetchrow(
            """UPDATE signing_auth_sessions SET verified_at=now()
               WHERE transaction_id=$1 AND verified_at IS NULL AND used_at IS NULL AND expires_at>now()
               RETURNING *""", transaction_id,
        )

    async def consume_auth(self, transaction_id: str):
        return await self.conn.fetchrow(
            """UPDATE signing_auth_sessions SET used_at=now()
               WHERE transaction_id=$1 AND verified_at IS NOT NULL AND used_at IS NULL AND expires_at>now()
               RETURNING *""", transaction_id,
        )

    async def create_signature_event(self, values: dict):
        return await self.conn.fetchrow(
            """INSERT INTO signature_events
               (document_version_id,application_id,subject_type,subject_id,signer_identity_ref,
                auth_method,auth_transaction_id,ip_address,user_agent,consent_text_version,
                signature_image_ref,mark_type,assistance_type,reader_witness_user_id,
                reader_witness_attestation_text,reader_witness_signed_at,payload_hash,witness_for_event_id)
               VALUES ($1,$2,$3,$4,$5,$6,$7,$8,$9,$10,$11,$12,$13,$14,$15,$16,$17,$18)
               RETURNING *""",
            values["document_version_id"], values["application_id"], values["subject_type"],
            values["subject_id"], values["signer_identity_ref"], values["auth_method"],
            values["auth_transaction_id"], values.get("ip_address"), values.get("user_agent"),
            values["consent_text_version"], values.get("signature_image_ref"), values.get("mark_type"),
            values.get("assistance_type"), values.get("reader_witness_user_id"),
            values.get("reader_witness_attestation_text"), values.get("reader_witness_signed_at"),
            values["payload_hash"], values.get("witness_for_event_id"),
        )

    async def evidence_package(self, application_id):
        versions = await self.conn.fetch(
            "SELECT id,subject_type,subject_id,version_number,payload_hash,status,frozen_at FROM document_versions WHERE application_id=$1 ORDER BY subject_type,subject_id,version_number",
            application_id,
        )
        events = await self.conn.fetch(
            "SELECT * FROM signature_events WHERE application_id=$1 ORDER BY signed_at", application_id,
        )
        return {"application_id": str(application_id), "document_versions": [dict(v) for v in versions], "signature_events": [dict(e) for e in events]}
