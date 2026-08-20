import base64
from collections import defaultdict, deque
from io import BytesIO
import hashlib
import hmac
import secrets
import struct
import time
from datetime import UTC, datetime, timedelta

from fastapi import HTTPException
from jose import jwt
import qrcode
from qrcode.constants import ERROR_CORRECT_M

from app.config import settings


_MFA_ATTEMPT_WINDOW_SECONDS = 60
_MFA_MAX_FAILURES = 5
_failed_attempts: dict[str, deque[float]] = defaultdict(deque)


def _recent_failures(user_id) -> deque[float]:
    key = str(user_id)
    failures = _failed_attempts[key]
    cutoff = time.monotonic() - _MFA_ATTEMPT_WINDOW_SECONDS
    while failures and failures[0] <= cutoff:
        failures.popleft()
    if not failures:
        _failed_attempts.pop(key, None)
        return deque()
    return failures


def enforce_mfa_attempt_limit(user_id) -> None:
    if len(_recent_failures(user_id)) >= _MFA_MAX_FAILURES:
        raise HTTPException(
            status_code=429,
            detail="Too many authentication attempts. Try again in one minute.",
            headers={"Retry-After": str(_MFA_ATTEMPT_WINDOW_SECONDS)},
        )


def record_mfa_failure(user_id) -> None:
    failures = _failed_attempts[str(user_id)]
    failures.append(time.monotonic())
    enforce_mfa_attempt_limit(user_id)


def clear_mfa_failures(user_id) -> None:
    _failed_attempts.pop(str(user_id), None)


def new_secret() -> str:
    return base64.b32encode(secrets.token_bytes(20)).decode("ascii").rstrip("=")


def qr_code_data_url(enrollment_uri: str) -> str:
    """Encode an MFA enrollment URI locally without disclosing it to a QR service."""
    if not enrollment_uri.startswith("otpauth://totp/") or len(enrollment_uri) > 1024:
        raise ValueError("Invalid authenticator enrollment URI")
    qr = qrcode.QRCode(
        version=None,
        error_correction=ERROR_CORRECT_M,
        box_size=7,
        border=4,
    )
    qr.add_data(enrollment_uri)
    qr.make(fit=True)
    image = qr.make_image(fill_color="black", back_color="white")
    output = BytesIO()
    image.save(output, format="PNG")
    encoded = base64.b64encode(output.getvalue()).decode("ascii")
    return f"data:image/png;base64,{encoded}"


def totp(secret: str, at: int | None = None) -> str:
    counter = int((at if at is not None else time.time()) // 30)
    padded = secret + "=" * ((8 - len(secret) % 8) % 8)
    digest = hmac.new(base64.b32decode(padded, casefold=True), struct.pack(">Q", counter), hashlib.sha1).digest()
    offset = digest[-1] & 15
    value = (struct.unpack(">I", digest[offset:offset + 4])[0] & 0x7FFFFFFF) % 1_000_000
    return f"{value:06d}"


def verify_totp(secret: str, code: str) -> bool:
    now = int(time.time())
    return bool(code) and any(hmac.compare_digest(totp(secret, now + drift * 30), code) for drift in (-1, 0, 1))


def verification_token(user_id) -> str:
    return jwt.encode({
        "sub": str(user_id), "purpose": "configuration_mfa",
        "iat": datetime.now(UTC), "exp": datetime.now(UTC) + timedelta(minutes=15),
    }, settings.JWT_SECRET_KEY, algorithm=settings.JWT_ALGORITHM)


def token_is_valid(token: str | None, user_id) -> bool:
    try:
        payload = jwt.decode(token or "", settings.JWT_SECRET_KEY, algorithms=[settings.JWT_ALGORITHM])
        return payload.get("purpose") == "configuration_mfa" and hmac.compare_digest(str(payload.get("sub")), str(user_id))
    except Exception:
        return False
