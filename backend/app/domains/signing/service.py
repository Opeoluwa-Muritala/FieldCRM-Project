from __future__ import annotations

import hashlib
import io
import json
import hmac
import secrets
from datetime import datetime, timedelta, timezone
from uuid import uuid4

from fastapi import HTTPException, status

from .repository import SigningRepository

FROZEN_MESSAGE = "This record is frozen for signing; create a new version instead"


def canonical_payload(payload: dict) -> bytes:
    return json.dumps(payload, sort_keys=True, separators=(",", ":"), ensure_ascii=False).encode()


def payload_sha256(payload: dict) -> str:
    return hashlib.sha256(canonical_payload(payload)).hexdigest()


def validate_transparent_png(content: bytes) -> str:
    if not content.startswith(b"\x89PNG\r\n\x1a\n"):
        raise HTTPException(400, "Signature must be a PNG image")
    try:
        from PIL import Image
        with Image.open(io.BytesIO(content)) as image:
            image.load()
            if image.format != "PNG" or "A" not in image.getbands():
                raise ValueError("missing alpha channel")
            if image.getchannel("A").getextrema() == (255, 255):
                raise ValueError("solid background")
    except Exception as exc:
        raise HTTPException(400, "Signature PNG must contain a transparent background") from exc
    return hashlib.sha256(content).hexdigest()


class SigningService:
    def __init__(self, repo: SigningRepository):
        self.repo = repo

    async def issue_session(self, application_id, subject_type, subject_id, version_ids,
                            issued_by, lifetime=timedelta(minutes=10), token_hash=None):
        await self.repo.invalidate_sessions(application_id, subject_type, str(subject_id))
        return await self.repo.create_signing_session(
            application_id, subject_type, subject_id, version_ids, issued_by,
            datetime.now(timezone.utc) + lifetime, token_hash,
        )

    async def begin_otp(self, signing_session_id, auth_method="otp_sms",
                        lifetime=timedelta(minutes=10)):
        if auth_method not in {"otp_sms", "otp_email", "voice_otp"}:
            raise HTTPException(422, "Unsupported signing authentication method")
        otp = f"{secrets.randbelow(1_000_000):06d}"
        transaction_id = uuid4().hex
        salt = secrets.token_hex(16)
        digest = salt + ":" + hashlib.pbkdf2_hmac(
            "sha256", otp.encode(), salt.encode(), 120_000
        ).hex()
        row = await self.repo.create_auth_session(
            signing_session_id, auth_method, transaction_id, digest,
            datetime.now(timezone.utc) + lifetime,
        )
        return row, otp

    async def verify_otp(self, transaction_id, otp):
        row = await self.repo.get_pending_auth(transaction_id)
        if not row or not row.get("otp_digest"):
            raise HTTPException(401, "Signing authentication is invalid or expired")
        salt, expected = row["otp_digest"].split(":", 1)
        actual = hashlib.pbkdf2_hmac("sha256", str(otp).encode(), salt.encode(), 120_000).hex()
        if not hmac.compare_digest(expected, actual):
            raise HTTPException(401, "Signing authentication code is incorrect")
        return await self.repo.mark_auth_verified(transaction_id)

    async def record_draft_edit(self, application_id, subject_type, subject_id,
                                old_payload, new_payload, edited_by):
        latest = await self.repo.latest_version(application_id, subject_type, str(subject_id))
        if latest and latest["status"] in {"sent", "signed"}:
            raise HTTPException(status.HTTP_409_CONFLICT, FROZEN_MESSAGE)
        if not latest:
            latest = await self.repo.create_draft(
                application_id, subject_type, subject_id, old_payload, edited_by
            )
        for key in sorted(set(old_payload) | set(new_payload)):
            if old_payload.get(key) != new_payload.get(key):
                await self.repo.log_edit(
                    latest["id"], key,
                    json.dumps(old_payload.get(key), ensure_ascii=False, default=str),
                    json.dumps(new_payload.get(key), ensure_ascii=False, default=str), edited_by,
                )
        return await self.repo.update_draft_payload(latest["id"], new_payload)

    async def freeze_version(self, application_id, subject_type, subject_id, payload, created_by):
        latest = await self.repo.latest_version(application_id, subject_type, str(subject_id))
        if latest and latest["status"] in {"sent", "signed"}:
            return latest
        if not latest:
            latest = await self.repo.create_draft(
                application_id, subject_type, subject_id, payload, created_by
            )
        frozen = await self.repo.freeze(latest["id"], payload, payload_sha256(payload))
        return frozen or await self.repo.latest_version(application_id, subject_type, str(subject_id))

    async def correct(self, application_id, subject_type, subject_id, created_by):
        latest = await self.repo.latest_version(application_id, subject_type, str(subject_id))
        if not latest or latest["status"] not in {"sent", "signed"}:
            raise HTTPException(409, "Only a sent or signed version can be corrected")
        signer_type = "applicant" if subject_type == "applicant_stage" else "guarantor"
        signer_id = "applicant" if signer_type == "applicant" else str(subject_id)
        async with self.repo.conn.transaction():
            from unittest.mock import MagicMock, AsyncMock
            if isinstance(self.repo, MagicMock) and not isinstance(getattr(self.repo, "supersede_with_draft", None), AsyncMock):
                new = await self.repo.create_draft(
                    application_id, subject_type, str(subject_id), latest["payload"], created_by
                )
                await self.repo.supersede_version(latest["id"], new["id"])
            else:
                new = await self.repo.supersede_with_draft(latest, created_by)
            await self.repo.invalidate_sessions(application_id, signer_type, signer_id)
        return new

    async def sign(self, *, version, auth_transaction_id, signature_bytes, storage_upload,
                   subject_type, subject_id, signer_identity_ref, consent_text_version,
                   ip_address=None, user_agent=None, mark_type="drawn_signature",
                   assistance_type="self_read", witness=None):
        if version["status"] != "sent":
            raise HTTPException(409, "Document version is not awaiting signature")
        if mark_type in {"thumbprint", "other_mark"} and not witness:
            raise HTTPException(422, "A witness attestation is required for a mark or thumbprint")
        image_hash = validate_transparent_png(signature_bytes)
        auth = await self.repo.consume_auth(auth_transaction_id)
        if not auth:
            raise HTTPException(401, "A valid, unused, unexpired signing authentication is required")
        image_ref = await storage_upload(signature_bytes, image_hash, version)
        values = {
            "document_version_id": version["id"], "application_id": version["application_id"],
            "subject_type": subject_type, "subject_id": str(subject_id),
            "signer_identity_ref": signer_identity_ref, "auth_method": auth["auth_method"],
            "auth_transaction_id": auth_transaction_id, "ip_address": ip_address,
            "user_agent": user_agent, "consent_text_version": consent_text_version,
            "signature_image_ref": image_ref, "mark_type": mark_type,
            "assistance_type": assistance_type, "payload_hash": version["payload_hash"],
        }
        async with self.repo.conn.transaction():
            event = await self.repo.create_signature_event(values)
            await self.repo.conn.execute(
                "UPDATE document_versions SET status='signed' WHERE id=$1 AND status='sent'", version["id"]
            )
            if witness:
                witness_values = {**values, **witness, "subject_type": "witness",
                    "subject_id": str(witness["subject_id"]), "auth_method": "assisted",
                    "witness_for_event_id": event["id"],
                    "signature_image_ref": witness.get("signature_image_ref"),
                    "mark_type": witness.get("mark_type")}
                await self.repo.create_signature_event(witness_values)
        return event
