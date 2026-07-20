import os
import logging
from pathlib import Path
from urllib.parse import urlparse
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


def default_email_service_url() -> str:
    """Support the legacy EMAIL_BASE_URL name used by existing deployments."""
    url = os.getenv("EMAIL_SERVICE_URL") or os.getenv("EMAIL_BASE_URL") or "https://emailope.vercel.app/"
    return url if "://" in url else f"https://{url}"


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
    APP_ENV: str = os.getenv("APP_ENV", os.getenv("VERCEL_ENV", "development"))
    JWT_SECRET_KEY: str = os.getenv("JWT_SECRET_KEY", "")
    JWT_ALGORITHM: str = "HS256"
    ACCESS_TOKEN_EXPIRE_MINUTES: int = 60  # 1 hour for web sessions; mobile uses 30-day tokens
    
    # Database
    POSTGRES_SERVER: str = os.getenv("POSTGRES_SERVER", "")
    POSTGRES_USER: str = os.getenv("POSTGRES_USER", "postgres")
    POSTGRES_PASSWORD: str = os.getenv("POSTGRES_PASSWORD", "postgres")
    POSTGRES_DB: str = os.getenv("POSTGRES_DB", "fieldcrm")
    DATABASE_URL: str = os.getenv("DATABASE_URL", default_database_url())

    
    # Security / CORS
    # Keep this as a string for compatibility with older pydantic-settings
    # releases; use ``cors_origins`` where FastAPI needs a list.
    CORS_ORIGINS: str = os.getenv("CORS_ORIGINS", "")
    COOKIE_SECURE: bool = os.getenv("COOKIE_SECURE", "false").lower() in ("true", "1", "yes")
    RATE_LIMIT_REDIS_URL: str = os.getenv("RATE_LIMIT_REDIS_URL", "")
    # May share the rate-limit Redis deployment. Caching remains optional and
    # always falls back to the database if Redis is unavailable.
    CACHE_REDIS_URL: str = os.getenv("CACHE_REDIS_URL", os.getenv("RATE_LIMIT_REDIS_URL", ""))

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
    EMAIL_SERVICE_URL: str = default_email_service_url()

    # Document uploads (local fallback)
    DOCUMENT_UPLOAD_DIR: str = os.getenv(
        "DOCUMENT_UPLOAD_DIR",
        str(ROOT_DIR / "frontend" / "static" / "uploads"),
    )
    # Upload limits are deliberately split by content type.  Images may be
    # larger at capture time, but must compress below the final 3 MB limit.
    DOCUMENT_MAX_PDF_BYTES: int = int(os.getenv("DOCUMENT_MAX_PDF_BYTES", str(3 * 1024 * 1024)))
    DOCUMENT_MAX_IMAGE_BYTES: int = int(os.getenv("DOCUMENT_MAX_IMAGE_BYTES", str(5 * 1024 * 1024)))
    DOCUMENT_MAX_IMAGE_COMPRESSED_BYTES: int = int(
        os.getenv("DOCUMENT_MAX_IMAGE_COMPRESSED_BYTES", str(3 * 1024 * 1024))
    )
    DOCUMENT_ALLOWED_MIME_TYPES: list[str] = ["application/pdf", "image/jpeg", "image/png"]

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

    @property
    def is_production(self) -> bool:
        return self.APP_ENV.lower() in {"production", "prod"}

    @property
    def cors_origins(self) -> list[str]:
        value = self.CORS_ORIGINS.strip()
        if not value:
            return []
        if value.startswith("["):
            import json
            parsed = json.loads(value)
            if not isinstance(parsed, list) or not all(isinstance(item, str) for item in parsed):
                raise ValueError("CORS_ORIGINS JSON value must be an array of origin strings.")
            return [item.strip() for item in parsed if item.strip()]
        return [origin.strip() for origin in value.split(",") if origin.strip()]

    @model_validator(mode="after")
    def normalize_database_url(self):
        if self.DATABASE_URL == "sqlite:///./fieldcrm.db":
            self.DATABASE_URL = f"sqlite:///{(ROOT_DIR / 'fieldcrm.db').as_posix()}"
        if not self.JWT_SECRET_KEY.strip():
            raise ValueError("JWT_SECRET_KEY is required; configure a fixed secret in the environment.")
        if not self.cors_origins or "*" in self.cors_origins:
            raise ValueError("CORS_ORIGINS must contain one or more explicit origins; wildcards are forbidden.")
        if self.is_production:
            host = urlparse(self.DATABASE_URL).hostname or ""
            if not self.DATABASE_URL.startswith("postgresql") or "-pooler" not in host:
                raise ValueError("Production DATABASE_URL must use Neon's pooled (-pooler) PostgreSQL host.")
            if not self.RATE_LIMIT_REDIS_URL:
                raise ValueError("RATE_LIMIT_REDIS_URL is required in production for distributed rate limiting.")
            if self.CACHE_REDIS_URL and not self.CACHE_REDIS_URL.startswith("rediss://"):
                raise ValueError("CACHE_REDIS_URL must use rediss:// in production.")
        return self

settings = Settings()
