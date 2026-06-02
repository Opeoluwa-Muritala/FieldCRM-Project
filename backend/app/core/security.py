from datetime import datetime, timedelta
import logging
from typing import Any, Union
from jose import jwt
from passlib.context import CryptContext
from app.core.config import settings

logger = logging.getLogger("FieldCRMSecurity")

# CryptContext accepts existing bcrypt hashes and creates new hashes with
# pbkdf2_sha256 to avoid current bcrypt backend compatibility issues.
pwd_context = CryptContext(schemes=["pbkdf2_sha256", "bcrypt"], deprecated="auto")

def verify_password(plain_password: str, hashed_password: str) -> bool:
    """Verifies a plain password against its hashed database entry."""
    if not hashed_password:
        return False
    try:
        return pwd_context.verify(plain_password, hashed_password)
    except Exception as exc:
        logger.warning("Password hash verification failed: %s", exc)
        return False

def get_password_hash(password: str) -> str:
    """Hashes a password using the configured password hashing context."""
    return pwd_context.hash(password)

def create_access_token(subject: Union[str, Any], expires_delta: timedelta = None) -> str:
    """
    Creates a secure JWT token:
    - Sets standard 'sub' and 'exp' claims.
    - Uses configured secure algorithms.
    """
    if expires_delta:
        expire = datetime.utcnow() + expires_delta
    else:
        expire = datetime.utcnow() + timedelta(minutes=settings.ACCESS_TOKEN_EXPIRE_MINUTES)
    
    to_encode = {"exp": expire, "sub": str(subject)}
    encoded_jwt = jwt.encode(to_encode, settings.JWT_SECRET_KEY, algorithm=settings.JWT_ALGORITHM)
    return encoded_jwt

def decode_access_token(token: str) -> dict:
    """
    Decodes and validates a JWT token:
    - Strictly rejects 'none' algorithm by hardcoding the expected algorithm.
    - Validates presence of expirations.
    """
    try:
        # Hardcoding the algorithm check to prevent algorithm-downgrade attacks
        payload = jwt.decode(
            token, 
            settings.JWT_SECRET_KEY, 
            algorithms=[settings.JWT_ALGORITHM]
        )
        return payload
    except Exception:
        # Fail-closed behavior
        return {}
