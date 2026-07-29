# Vercel deployment map

## Confirmed configuration

- `pyproject.toml` declares `[tool.vercel] entrypoint = "backend.app.main:app"`.
- Root `requirements.txt` is the deployment dependency file.
- Root and backend `runtime.txt` specify `python-3.12.11`.
- There is no `vercel.json`, `.vercelignore`, Node manifest, package lock, static build output, build command, install override, or CI workflow.
- FastAPI uses paths derived from `__file__` to mount `frontend/static` and load `frontend/templates`.

## Local development

From the repository root, install Python dependencies and run Uvicorn with `backend` as the working directory or module path configured as documented. The single process serves HTML, `/static/*`, browser routes, `/api/v1/*`, and mobile APIs.

## Vercel build and deployment

**Confirmed:** Vercel is instructed to import `backend.app.main:app` and install root Python dependencies. **Inferred:** the function bundle includes `frontend/templates` and `frontend/static`, because runtime code resolves both within the checked-out repository. There is no independent Vercel static deployment.

**Unverified:** Vercel project root, dashboard build/install overrides, bundle inclusion, function duration/size, production and preview domains, routing behavior, and configured environment values. No deployment was performed.

## Runtime routing

With no repository `vercel.json`, application route matching is delegated to the Python/FastAPI entry point according to the Vercel project integration. FastAPI itself mounts `/static` and registers all HTML/API routes. Exact platform-generated rewrites are **Unverified**.

## Environment variables (names only)

Core: `APP_ENV`, `VERCEL_ENV`, `APP_BASE_URL`, `JWT_SECRET_KEY`, `CORS_ORIGINS`, `COOKIE_SECURE`, `ORG_REGISTRATION_SECRET`.

Database/cache: `DATABASE_URL`, `POSTGRES_SERVER`, `POSTGRES_USER`, `POSTGRES_PASSWORD`, `POSTGRES_DB`, `RATE_LIMIT_REDIS_URL`, `CACHE_REDIS_URL`.

Documents: `DOCUMENT_UPLOAD_DIR`, `DOCUMENT_MAX_PDF_BYTES`, `DOCUMENT_MAX_IMAGE_BYTES`, `DOCUMENT_MAX_IMAGE_COMPRESSED_BYTES`, `CLOUDINARY_CLOUD_NAME`, `CLOUDINARY_API_KEY`, `CLOUDINARY_API_SECRET`.

Email: `EMAIL_SERVICE_URL`, legacy `EMAIL_BASE_URL`, and SMTP variables (present in settings although EmailService uses the JSON gateway).

Providers: `QORE_API_KEY`, `QORE_BASE_URL`, `CREDIT_REGISTRY_USERNAME`, `CREDIT_REGISTRY_PASSWORD`, `CREDIT_REGISTRY_BASE_URL`, `CRC_API_KEY`, `CRC_BASE_URL`, `AML_YOUVERIFY_TOKEN`, `AML_BASE_URL`.

## Connectivity and deployment risks

1. **High, Confirmed:** detached OCR tasks may not finish after a Vercel response.
2. **Medium, Confirmed:** production startup rejects a non-pooled/non-Neon-looking PostgreSQL URL and requires Redis rate limiting; incorrect dashboard values prevent startup.
3. **Medium, Inferred:** CPU-heavy OCR/PDF/image dependencies may approach serverless bundle/time limits.
4. **Medium, Unverified:** templates/static inclusion and maximum duration have no explicit repo configuration.
5. **Medium, Confirmed:** Cloudinary is mandatory for production uploads; local Vercel filesystem persistence is deliberately rejected.
6. **Low, Unverified:** preview domains must be explicitly present in `CORS_ORIGINS` if cross-origin API access is used. Same-origin rendered UI does not require CORS.

## Post-deployment verification

Check `/api/v1/health`, login/cookie attributes, a protected HTML page, `/static/css/dashboard.css`, a JSON mobile endpoint with a test token, document upload/preview/download, Redis-backed rate limiting, DB pooling, and each configured external integration. These are manual checks; deployment health is not claimed here.
