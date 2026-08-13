"""Application-layer encryption and blind indexes for restricted identifiers."""
from __future__ import annotations

import base64
import hashlib
import hmac
import os

from cryptography.hazmat.primitives.ciphers.aead import AESGCM

from app.config import settings

PREFIX = "enc:v1:"


def _key(value: str, setting_name: str) -> bytes:
    try:
        decoded = base64.urlsafe_b64decode(value.encode("ascii"))
    except Exception as exc:
        raise RuntimeError(f"{setting_name} is not valid URL-safe base64") from exc
    if len(decoded) != 32:
        raise RuntimeError(f"{setting_name} must decode to exactly 32 bytes")
    return decoded


def encryption_configured() -> bool:
    return bool(settings.FIELD_ENCRYPTION_KEY and settings.FIELD_LOOKUP_KEY)


def encrypt_sensitive(value: str | None, *, context: str) -> str | None:
    if value in (None, "") or (isinstance(value, str) and value.startswith(PREFIX)):
        return value
    if not settings.FIELD_ENCRYPTION_KEY:
        if settings.is_production:
            raise RuntimeError("FIELD_ENCRYPTION_KEY is required in production")
        return value
    nonce = os.urandom(12)
    ciphertext = AESGCM(_key(settings.FIELD_ENCRYPTION_KEY, "FIELD_ENCRYPTION_KEY")).encrypt(
        nonce, str(value).encode("utf-8"), context.encode("utf-8")
    )
    return PREFIX + base64.urlsafe_b64encode(nonce + ciphertext).decode("ascii")


def decrypt_sensitive(value: str | None, *, context: str) -> str | None:
    if value in (None, "") or not isinstance(value, str) or not value.startswith(PREFIX):
        return value
    if not settings.FIELD_ENCRYPTION_KEY:
        raise RuntimeError("Encrypted field cannot be read without FIELD_ENCRYPTION_KEY")
    payload = base64.urlsafe_b64decode(value[len(PREFIX):].encode("ascii"))
    return AESGCM(_key(settings.FIELD_ENCRYPTION_KEY, "FIELD_ENCRYPTION_KEY")).decrypt(
        payload[:12], payload[12:], context.encode("utf-8")
    ).decode("utf-8")


def blind_index(value: str | None, *, context: str) -> str | None:
    if value in (None, ""):
        return None
    if not settings.FIELD_LOOKUP_KEY:
        if settings.is_production:
            raise RuntimeError("FIELD_LOOKUP_KEY is required in production")
        return None
    normalized = "".join(str(value).split()).lower()
    digest = hmac.new(
        _key(settings.FIELD_LOOKUP_KEY, "FIELD_LOOKUP_KEY"),
        f"{context}:{normalized}".encode("utf-8"),
        hashlib.sha256,
    ).hexdigest()
    return f"hmac:v1:{digest}"


def mask_sensitive(value: str | None, *, visible: int = 4) -> str:
    if value in (None, ""):
        return "empty"
    raw = str(value)
    tail = raw[-visible:] if len(raw) > visible else raw
    return f"{'*' * 7}{tail}"
