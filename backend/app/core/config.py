import os
import secrets
import logging
from typing import List
from pathlib import Path
from pydantic import model_validator
from pydantic_settings import BaseSettings, SettingsConfigDict

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger("FieldCRMConfig")

BACKEND_DIR = Path(__file__).resolve().parents[2]
ROOT_DIR = BACKEND_DIR.parent


def default_database_url() -> str:
    postgres_server = os.getenv("POSTGRES_SERVER", "")
    postgres_user = os.getenv("POSTGRES_USER", "postgres")
    postgres_password = os.getenv("POSTGRES_PASSWORD", "postgres")
    postgres_db = os.getenv("POSTGRES_DB", "fieldcrm")
    if postgres_server:
        return f"postgresql://{postgres_user}:{postgres_password}@{postgres_server}/{postgres_db}"

    db_path = (ROOT_DIR / "fieldcrm.db").as_posix()
    return f"sqlite:///{db_path}"


def resolve_jwt_secret() -> str:
    """
    Secure resolution of JWT Secret Key:
    1. Resolve from environment variables (Production/Staging standard)
    2. Query from local secrets file (jwt_secret.txt)
    3. Generate dynamic secure fallback + raise severe warning (Dev environment fallback)
    """
    env_secret = os.getenv("JWT_SECRET_KEY")
    if env_secret:
        return env_secret
        
    secret_path = os.path.join(os.path.dirname(os.path.abspath(__file__)), "../../../jwt_secret.txt")
    if os.path.exists(secret_path):
        try:
            with open(secret_path, "r", encoding="utf-8") as f:
                return f.read().strip()
        except Exception as e:
            logger.error("Failed to read JWT secret from file: %s", e)
            
    # Dev-only secure dynamic fallback
    logger.warning("Generating dynamic ephemeral JWT secret. Session invalidation will occur on server restart!")
    fallback_secret = secrets.token_hex(32)
    return fallback_secret

class Settings(BaseSettings):
    model_config = SettingsConfigDict(
        env_file=str(BACKEND_DIR / ".env"),
        env_file_encoding="utf-8",
        case_sensitive=True,
        extra="ignore",
    )

    PROJECT_NAME: str = "FieldCRM"
    API_V1_STR: str = "/api/v1"
    
    # Secrets
    JWT_SECRET_KEY: str = resolve_jwt_secret()
    JWT_ALGORITHM: str = "HS256"
    ACCESS_TOKEN_EXPIRE_MINUTES: int = 60 * 24 * 7  # 7 Days for field operations
    
    # Database
    POSTGRES_SERVER: str = os.getenv("POSTGRES_SERVER", "")
    POSTGRES_USER: str = os.getenv("POSTGRES_USER", "postgres")
    POSTGRES_PASSWORD: str = os.getenv("POSTGRES_PASSWORD", "postgres")
    POSTGRES_DB: str = os.getenv("POSTGRES_DB", "fieldcrm")
    DATABASE_URL: str = os.getenv("DATABASE_URL", default_database_url())

    
    # Security / CORS
    CORS_ORIGINS: List[str] = ["http://localhost:8000", "http://127.0.0.1:8000"]
    COOKIE_SECURE: bool = os.getenv("COOKIE_SECURE", "false").lower() in ("true", "1", "yes")

    # Document uploads
    DOCUMENT_UPLOAD_DIR: str = os.getenv(
        "DOCUMENT_UPLOAD_DIR",
        str(ROOT_DIR / "frontend" / "static" / "uploads"),
    )
    DOCUMENT_MAX_UPLOAD_BYTES: int = int(os.getenv("DOCUMENT_MAX_UPLOAD_BYTES", str(10 * 1024 * 1024)))
    DOCUMENT_ALLOWED_MIME_TYPES: List[str] = ["application/pdf", "image/jpeg", "image/png"]

    @model_validator(mode="after")
    def normalize_database_url(self):
        if self.DATABASE_URL == "sqlite:///./fieldcrm.db":
            self.DATABASE_URL = f"sqlite:///{(ROOT_DIR / 'fieldcrm.db').as_posix()}"
        return self

settings = Settings()
