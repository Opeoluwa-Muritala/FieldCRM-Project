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
    ACCESS_TOKEN_EXPIRE_MINUTES: int = 10
    
    # Database
    POSTGRES_SERVER: str = os.getenv("POSTGRES_SERVER", "")
    POSTGRES_USER: str = os.getenv("POSTGRES_USER", "postgres")
    POSTGRES_PASSWORD: str = os.getenv("POSTGRES_PASSWORD", "postgres")
    POSTGRES_DB: str = os.getenv("POSTGRES_DB", "fieldcrm")
    DATABASE_URL: str = os.getenv("DATABASE_URL", default_database_url())
    RLS_ENFORCED: bool = os.getenv("RLS_ENFORCED", "false").lower() in ("true", "1", "yes")
    DATABASE_EXPECTED_RUNTIME_USER: str = os.getenv("DATABASE_EXPECTED_RUNTIME_USER", "fieldcrm_app")

    # URL-safe base64 values that each decode to 32 random bytes. Encryption
    # and lookup keys are separate to avoid using ciphertext keys for search.
    FIELD_ENCRYPTION_KEY: str = os.getenv("FIELD_ENCRYPTION_KEY", "")
    FIELD_LOOKUP_KEY: str = os.getenv("FIELD_LOOKUP_KEY", "")

    # Demo presenter mode is permitted only outside Vercel Production. It uses
    # a synthetic tenant in the normal database and never bypasses RLS.
    DEMO_ENABLED: bool = os.getenv("DEMO_ENABLED", "false").lower() in ("true", "1", "yes")
    DEMO_ORG_ID: str = os.getenv("DEMO_ORG_ID", "")
    DEMO_ACCESS_SECRET: str = os.getenv("DEMO_ACCESS_SECRET", "")
    DEMO_SESSION_MINUTES: int = int(os.getenv("DEMO_SESSION_MINUTES", "120"))
    VERCEL_ENV: str = os.getenv("VERCEL_ENV", "")

    
    # Security / CORS
    # Keep this as a string for compatibility with older pydantic-settings
    # releases; use ``cors_origins`` where FastAPI needs a list.
    CORS_ORIGINS: str = os.getenv("CORS_ORIGINS", "")
    TRUSTED_HOSTS: str = os.getenv("TRUSTED_HOSTS", "")
    COOKIE_SECURE: bool = os.getenv("COOKIE_SECURE", "false").lower() in ("true", "1", "yes")
    # Strict script enforcement is the default now that inline scripts are
    # nonced and HTML event-handler attributes have been removed.
    CSP_NONCE_ENFORCED: bool = os.getenv("CSP_NONCE_ENFORCED", "true").lower() in ("true", "1", "yes")
    RATE_LIMIT_REDIS_URL: str = os.getenv("RATE_LIMIT_REDIS_URL", "")
    # May share the rate-limit Redis deployment. Caching remains optional and
    # always falls back to the database if Redis is unavailable.
    CACHE_REDIS_URL: str = os.getenv("CACHE_REDIS_URL", os.getenv("RATE_LIMIT_REDIS_URL", ""))
    AUTH_CACHE_TTL_SECONDS: int = int(os.getenv("AUTH_CACHE_TTL_SECONDS", "600"))
    DASHBOARD_CACHE_TTL_SECONDS: int = int(os.getenv("DASHBOARD_CACHE_TTL_SECONDS", "30"))

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

    # Public Android release metadata. The APK itself is stored as a public
    # Cloudinary raw asset; the application keeps the download URL stable.
    ANDROID_APK_URL: str = os.getenv(
        "ANDROID_APK_URL",
        "https://github.com/Opeoluwa-Muritala/FieldCRM-Project/releases/download/"
        "android-preview-v1.0-20260812/android-debug.apk",
    )
    ANDROID_APK_VERSION: str = os.getenv("ANDROID_APK_VERSION", "1.0 preview")
    ANDROID_APK_RELEASED_AT: str = os.getenv("ANDROID_APK_RELEASED_AT", "12 August 2026")
    ANDROID_APK_SIZE_BYTES: int = int(os.getenv("ANDROID_APK_SIZE_BYTES", "71365053"))
    ANDROID_APK_SHA256: str = os.getenv(
        "ANDROID_APK_SHA256",
        "73098ed8d4c61993fd17cf30048685aab7a63adf1056af17f0df237407efc21f",
    )
    ANDROID_APK_CHANNEL: str = os.getenv("ANDROID_APK_CHANNEL", "preview")

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

    # Core Banking remains opt-in at both the deployment and product levels.
    # The mock provider is intentionally limited to non-production use.
    CBS_INTEGRATION_ENABLED: bool = os.getenv("CBS_INTEGRATION_ENABLED", "false").lower() in ("true", "1", "yes")
    CBS_PROVIDER: str = os.getenv("CBS_PROVIDER", "mock").strip().lower()
    CBS_STALE_AFTER_MINUTES: int = int(os.getenv("CBS_STALE_AFTER_MINUTES", "240"))
    CBS_WEBHOOK_SECRET: str = os.getenv("CBS_WEBHOOK_SECRET", "")
    CBS_JOB_SECRET: str = os.getenv("CBS_JOB_SECRET", "")
    CUSTOMER_IDENTITY_ENABLED: bool = os.getenv("CUSTOMER_IDENTITY_ENABLED", "false").lower() in ("true", "1", "yes")
    CONFIGURATION_HUB_ENABLED: bool = os.getenv("CONFIGURATION_HUB_ENABLED", "false").lower() in ("true", "1", "yes")
    CONFIGURABLE_PRODUCTS_ENABLED: bool = os.getenv("CONFIGURABLE_PRODUCTS_ENABLED", "false").lower() in ("true", "1", "yes")
    CONFIGURABLE_WORKFLOW_ENABLED: bool = os.getenv("CONFIGURABLE_WORKFLOW_ENABLED", "false").lower() in ("true", "1", "yes")
    OPERATIONS_UI_ENABLED: bool = os.getenv("OPERATIONS_UI_ENABLED", "false").lower() in ("true", "1", "yes")

    @property
    def VERIFICATION_ENABLED(self) -> bool:
        return bool(self.QORE_API_KEY) and not self.demo_mode

    @property
    def BUREAU_REPORTING_ENABLED(self) -> bool:
        return bool((self.CREDIT_REGISTRY_USERNAME and self.CREDIT_REGISTRY_PASSWORD) or self.CRC_API_KEY) and not self.demo_mode

    @property
    def AML_SCREENING_ENABLED(self) -> bool:
        return bool(self.AML_YOUVERIFY_TOKEN) and not self.demo_mode

    @property
    def cloudinary_enabled(self) -> bool:
        # Demo documents still need durable storage on serverless previews.
        # Cloudinary object paths are already scoped by organisation and loan.
        return bool(self.CLOUDINARY_CLOUD_NAME and self.CLOUDINARY_API_KEY and self.CLOUDINARY_API_SECRET)

    @property
    def demo_mode(self) -> bool:
        return self.DEMO_ENABLED and self.VERCEL_ENV.lower() != "production"

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

    @property
    def trusted_hosts(self) -> list[str]:
        hosts = {host.strip().lower() for host in self.TRUSTED_HOSTS.split(",") if host.strip()}
        for url in [self.APP_BASE_URL, *self.cors_origins]:
            hostname = urlparse(url).hostname if url else None
            if hostname:
                hosts.add(hostname.lower())
        vercel_url = os.getenv("VERCEL_URL", "").strip()
        if vercel_url:
            hosts.add(vercel_url.lower())
        if self.VERCEL_ENV:
            # Preview deployment names are generated dynamically by Vercel.
            hosts.add("*.vercel.app")
        if not self.is_production:
            hosts.update({"localhost", "127.0.0.1", "testserver"})
        return sorted(hosts)

    @property
    def public_base_url(self) -> str:
        if self.APP_BASE_URL.strip():
            return self.APP_BASE_URL.rstrip("/")
        return self.cors_origins[0].rstrip("/")

    @model_validator(mode="after")
    def normalize_database_url(self):
        if self.DATABASE_URL == "sqlite:///./fieldcrm.db":
            self.DATABASE_URL = f"sqlite:///{(ROOT_DIR / 'fieldcrm.db').as_posix()}"
        if not self.JWT_SECRET_KEY.strip():
            raise ValueError("JWT_SECRET_KEY is required; configure a fixed secret in the environment.")
        if not self.cors_origins or "*" in self.cors_origins:
            raise ValueError("CORS_ORIGINS must contain one or more explicit origins; wildcards are forbidden.")
        if not self.trusted_hosts:
            raise ValueError("TRUSTED_HOSTS or a hostname in APP_BASE_URL/CORS_ORIGINS is required.")
        for origin in self.cors_origins:
            parsed_origin = urlparse(origin)
            if parsed_origin.scheme not in {"http", "https"} or not parsed_origin.hostname:
                raise ValueError("CORS_ORIGINS entries must be absolute HTTP(S) origins.")
        if self.APP_BASE_URL:
            parsed_base = urlparse(self.APP_BASE_URL)
            if parsed_base.scheme not in {"http", "https"} or not parsed_base.hostname:
                raise ValueError("APP_BASE_URL must be an absolute HTTP(S) URL.")
        if self.is_production:
            parsed_database = urlparse(self.DATABASE_URL)
            host = parsed_database.hostname or ""
            if not self.DATABASE_URL.startswith("postgresql") or "-pooler" not in host:
                raise ValueError("Production DATABASE_URL must use Neon's pooled (-pooler) PostgreSQL host.")
            if not self.RATE_LIMIT_REDIS_URL:
                raise ValueError("RATE_LIMIT_REDIS_URL is required in production for distributed rate limiting.")
            if self.CACHE_REDIS_URL and not self.CACHE_REDIS_URL.startswith("rediss://"):
                raise ValueError("CACHE_REDIS_URL must use rediss:// in production.")
            if not self.FIELD_ENCRYPTION_KEY or not self.FIELD_LOOKUP_KEY:
                raise ValueError("FIELD_ENCRYPTION_KEY and FIELD_LOOKUP_KEY are required in production.")
            if not self.public_base_url.startswith("https://"):
                raise ValueError("APP_BASE_URL or the primary CORS origin must use HTTPS in production.")
            if self.RLS_ENFORCED and parsed_database.username != self.DATABASE_EXPECTED_RUNTIME_USER:
                raise ValueError("RLS_ENFORCED requires the configured non-owner runtime database user.")
        for setting_name, encoded_key in (
            ("FIELD_ENCRYPTION_KEY", self.FIELD_ENCRYPTION_KEY),
            ("FIELD_LOOKUP_KEY", self.FIELD_LOOKUP_KEY),
        ):
            if encoded_key:
                import base64
                try:
                    decoded_key = base64.urlsafe_b64decode(encoded_key.encode("ascii"))
                except Exception as exc:
                    raise ValueError(f"{setting_name} must be URL-safe base64.") from exc
                if len(decoded_key) != 32:
                    raise ValueError(f"{setting_name} must decode to exactly 32 bytes.")
        if self.ANDROID_APK_URL and not self.ANDROID_APK_URL.startswith("https://"):
            raise ValueError("ANDROID_APK_URL must use https://.")
        if self.ANDROID_APK_SHA256 and (
            len(self.ANDROID_APK_SHA256) != 64
            or any(character not in "0123456789abcdefABCDEF" for character in self.ANDROID_APK_SHA256)
        ):
            raise ValueError("ANDROID_APK_SHA256 must be a 64-character hexadecimal digest.")
        if self.DEMO_ENABLED:
            from uuid import UUID
            try:
                UUID(self.DEMO_ORG_ID)
            except (TypeError, ValueError) as exc:
                raise ValueError("DEMO_ORG_ID must be a valid UUID when DEMO_ENABLED=true.") from exc
            if len(self.DEMO_ACCESS_SECRET) < 32:
                raise ValueError("DEMO_ACCESS_SECRET must contain at least 32 characters.")
            if not 10 <= self.DEMO_SESSION_MINUTES <= 240:
                raise ValueError("DEMO_SESSION_MINUTES must be between 10 and 240.")
        if not 5 <= self.CBS_STALE_AFTER_MINUTES <= 10080:
            raise ValueError("CBS_STALE_AFTER_MINUTES must be between 5 minutes and 7 days.")
        if self.CBS_INTEGRATION_ENABLED:
            if self.CBS_PROVIDER not in {"mock"}:
                raise ValueError("CBS_PROVIDER is not registered by this release.")
            if self.is_production and self.CBS_PROVIDER == "mock":
                raise ValueError("The mock CBS provider cannot be enabled in production.")
            if self.CBS_WEBHOOK_SECRET and len(self.CBS_WEBHOOK_SECRET) < 32:
                raise ValueError("CBS_WEBHOOK_SECRET must contain at least 32 characters when configured.")
            if self.CBS_JOB_SECRET and len(self.CBS_JOB_SECRET) < 32:
                raise ValueError("CBS_JOB_SECRET must contain at least 32 characters when configured.")
        if self.CUSTOMER_IDENTITY_ENABLED and (not self.FIELD_ENCRYPTION_KEY or not self.FIELD_LOOKUP_KEY):
            raise ValueError("Customer identity requires FIELD_ENCRYPTION_KEY and FIELD_LOOKUP_KEY.")
        if self.CONFIGURATION_HUB_ENABLED:
            if self.is_production:
                raise ValueError("The Configuration Hub is localhost-only and cannot be enabled in production.")
            if not self.FIELD_ENCRYPTION_KEY:
                raise ValueError("The Configuration Hub requires FIELD_ENCRYPTION_KEY for MFA secrets.")
        return self

settings = Settings()
