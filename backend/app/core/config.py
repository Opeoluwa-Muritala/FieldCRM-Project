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
    ACCESS_TOKEN_EXPIRE_MINUTES: int = 60  # 1 hour for web sessions; mobile uses 30-day tokens
    
    # Database
    POSTGRES_SERVER: str = os.getenv("POSTGRES_SERVER", "")
    POSTGRES_USER: str = os.getenv("POSTGRES_USER", "postgres")
    POSTGRES_PASSWORD: str = os.getenv("POSTGRES_PASSWORD", "postgres")
    POSTGRES_DB: str = os.getenv("POSTGRES_DB", "fieldcrm")
    DATABASE_URL: str = os.getenv("DATABASE_URL", default_database_url())

    
    # Security / CORS
    CORS_ORIGINS: List[str] = ["http://localhost:8000", "http://127.0.0.1:8000"]
    COOKIE_SECURE: bool = os.getenv("COOKIE_SECURE", "false").lower() in ("true", "1", "yes")

    # Organisation registration guard — set this in production to a strong random string
    ORG_REGISTRATION_SECRET: str = os.getenv("ORG_REGISTRATION_SECRET", "")

    # Email invitations and password resets. Without SMTP, local development
    # logs links instead of attempting delivery.
    APP_BASE_URL: str = os.getenv("APP_BASE_URL", "")
    SMTP_HOST: str = os.getenv("SMTP_HOST", "")
    SMTP_PORT: int = int(os.getenv("SMTP_PORT", "587"))
    SMTP_USERNAME: str = os.getenv("SMTP_USERNAME", "")
    SMTP_PASSWORD: str = os.getenv("SMTP_PASSWORD", "")
    SMTP_FROM_EMAIL: str = os.getenv("SMTP_FROM_EMAIL", "")
    SMTP_FROM_NAME: str = os.getenv("SMTP_FROM_NAME", "FieldCRM")
    SMTP_USE_TLS: bool = os.getenv("SMTP_USE_TLS", "true").lower() in ("true", "1", "yes")

    # Transactional email delivery. Emailope accepts a JSON POST without SMTP
    # credentials; override this URL only when using a compatible mail gateway.
    EMAIL_SERVICE_URL: str = os.getenv("EMAIL_SERVICE_URL", "https://emailope.vercel.app/")

    # Document uploads (local fallback)
    DOCUMENT_UPLOAD_DIR: str = os.getenv(
        "DOCUMENT_UPLOAD_DIR",
        str(ROOT_DIR / "frontend" / "static" / "uploads"),
    )
    DOCUMENT_MAX_UPLOAD_BYTES: int = int(os.getenv("DOCUMENT_MAX_UPLOAD_BYTES", str(10 * 1024 * 1024)))
    DOCUMENT_ALLOWED_MIME_TYPES: List[str] = ["application/pdf", "image/jpeg", "image/png"]

    # Cloudinary (optional — set all three to enable cloud storage)
    CLOUDINARY_CLOUD_NAME: str = os.getenv("CLOUDINARY_CLOUD_NAME", "")
    CLOUDINARY_API_KEY: str = os.getenv("CLOUDINARY_API_KEY", "")
    CLOUDINARY_API_SECRET: str = os.getenv("CLOUDINARY_API_SECRET", "")

    # External Integrations settings
    QORE_API_KEY: str | None = os.getenv("QORE_API_KEY", None)
    QORE_BASE_URL: str = os.getenv("QORE_BASE_URL", "https://api.qoreid.com")

    CREDIT_REGISTRY_USERNAME: str | None = os.getenv("CREDIT_REGISTRY_USERNAME", None)
    CREDIT_REGISTRY_PASSWORD: str | None = os.getenv("CREDIT_REGISTRY_PASSWORD", None)
    CREDIT_REGISTRY_BASE_URL: str = os.getenv("CREDIT_REGISTRY_BASE_URL", "https://api.creditregistry.com")

    CRC_API_KEY: str | None = os.getenv("CRC_API_KEY", None)
    CRC_BASE_URL: str = os.getenv("CRC_BASE_URL", "https://api.crccreditbureau.com")

    AML_YOUVERIFY_TOKEN: str | None = os.getenv("AML_YOUVERIFY_TOKEN", None)
    AML_BASE_URL: str | None = os.getenv("AML_BASE_URL", None)

    @property
    def VERIFICATION_ENABLED(self) -> bool:
        return bool(self.QORE_API_KEY)

    @property
    def BUREAU_REPORTING_ENABLED(self) -> bool:
        return bool((self.CREDIT_REGISTRY_USERNAME and self.CREDIT_REGISTRY_PASSWORD) or self.CRC_API_KEY)

    @property
    def AML_SCREENING_ENABLED(self) -> bool:
        return bool(self.AML_YOUVERIFY_TOKEN)

    @property
    def cloudinary_enabled(self) -> bool:
        return bool(self.CLOUDINARY_CLOUD_NAME and self.CLOUDINARY_API_KEY and self.CLOUDINARY_API_SECRET)

    @model_validator(mode="after")
    def normalize_database_url(self):
        if self.DATABASE_URL == "sqlite:///./fieldcrm.db":
            self.DATABASE_URL = f"sqlite:///{(ROOT_DIR / 'fieldcrm.db').as_posix()}"
        return self

settings = Settings()
