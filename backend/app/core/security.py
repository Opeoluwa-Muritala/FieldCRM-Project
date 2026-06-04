from datetime import datetime, timedelta
import base64
import hashlib
import hmac
import secrets
from typing import Any, Union
from jose import jwt
from passlib.context import CryptContext
from app.config import settings

pwd_context = CryptContext(schemes=["bcrypt"], deprecated="auto")

PBKDF2_PREFIX = "pbkdf2_sha256"
PBKDF2_ITERATIONS = 260000


def _verify_pbkdf2_sha256(plain_password: str, encoded: str) -> bool:
    try:
        prefix, iterations, salt, expected = encoded.split("$", 3)
        if prefix != PBKDF2_PREFIX:
            return False
        salt_bytes = base64.b64decode(salt)
        expected_bytes = base64.b64decode(expected)
        actual = hashlib.pbkdf2_hmac(
            "sha256",
            plain_password.encode("utf-8"),
            salt_bytes,
            int(iterations),
            dklen=len(expected_bytes),
        )
        return hmac.compare_digest(actual, expected_bytes)
    except Exception:
        return False


def verify_password(plain_password: str, hashed_password: str) -> bool:
    if not hashed_password:
        return False
    if hashed_password.startswith(f"{PBKDF2_PREFIX}$"):
        return _verify_pbkdf2_sha256(plain_password, hashed_password)
    if hashed_password.startswith(("$2a$", "$2b$", "$2y$")):
        try:
            import bcrypt

            return bcrypt.checkpw(
                plain_password.encode("utf-8"),
                hashed_password.encode("utf-8"),
            )
        except Exception:
            pass
    try:
        if pwd_context.verify(plain_password, hashed_password):
            return True
    except Exception:
        pass
    return False

def get_password_hash(password: str) -> str:
    salt = secrets.token_bytes(16)
    digest = hashlib.pbkdf2_hmac(
        "sha256",
        password.encode("utf-8"),
        salt,
        PBKDF2_ITERATIONS,
        dklen=32,
    )
    return (
        f"{PBKDF2_PREFIX}${PBKDF2_ITERATIONS}$"
        f"{base64.b64encode(salt).decode('ascii')}$"
        f"{base64.b64encode(digest).decode('ascii')}"
    )

def create_access_token(
    subject: Union[str, Any], 
    role: str = "loan_officer", 
    org_id: str = "",
    expires_delta: Union[timedelta, None] = None
) -> str:
    if expires_delta:
        expire = datetime.utcnow() + expires_delta
    else:
        expire = datetime.utcnow() + timedelta(minutes=settings.ACCESS_TOKEN_EXPIRE_MINUTES)
        
    to_encode = {
        "sub": str(subject),
        "role": role,
        "org_id": str(org_id) if org_id else "",
        "exp": expire,
        "iat": datetime.utcnow(),
        "type": "access"
    }
    encoded_jwt = jwt.encode(to_encode, settings.JWT_SECRET_KEY, algorithm=settings.JWT_ALGORITHM)
    return encoded_jwt

def decode_access_token(token: str) -> dict:
    try:
        payload = jwt.decode(token, settings.JWT_SECRET_KEY, algorithms=[settings.JWT_ALGORITHM])
        return payload
    except Exception:
        return {}
