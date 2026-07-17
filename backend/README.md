# FieldCRM backend

The backend is the system of record for FieldCRM. It is a FastAPI application that serves the role-aware web application, exposes a JWT-protected mobile API, owns loan-workflow decisions and audit events, and persists borrowers, applications, documents, OCR results, notifications, and configuration data.

It also mounts the sibling `frontend/` directory: Jinja templates are rendered by FastAPI and `/static` is served from `frontend/static`. The web frontend therefore does **not** run as a separate service.

## Capabilities

- Password login, web session cookies, bearer-token and mobile-token login; password resets and invitation links.
- Role-aware access control for account officers, branch managers and supervisors, credit analysts, CRM, auditors, executive roles, legal, and system administrators.
- Loan origination: borrower records, guided application and guarantor forms, submitted documents, visit reports, and shareable client-intake links.
- Controlled workflow actions: review, return, sign-off, committee voting, executive/ED/MD decisions, board referral, disbursement, repayment entry, and PAR reporting.
- Document intake for PDFs, JPEGs, and PNGs (10 MiB maximum by default), local storage fallback, optional Cloudinary storage, server-side PDF generation, and asynchronous OCR extraction/review.
- Audit trail, compliance flags, notifications, dashboards, queues, search, user administration, and interest presets.
- Optional BVN/NIN verification (QoreID), credit-bureau reporting, AML screening, transactional email, and Cloudinary integration. These remain disabled until their credentials are configured.

## Web pages and APIs

The primary web pages include login/reset flows; dashboards; my work, visits and pending-signoff queues; application creation, detail, intake, guarantors, documents, OCR, visitation, credit review, approval and return pages; borrower, search, notification, settings, audit, pipeline, repayment, CRM, committee, executive, ED, MD, legal, and administrative pages. The router chooses the correct role-specific template and rejects unauthorized actions.

HTTP APIs are rooted at `/api/v1`:

| Route area | Purpose |
| --- | --- |
| `/auth` | API and mobile authentication/logout. |
| `/users` | Organisation registration, user creation/invitations, roles, and deactivation. |
| `/mobile` | Android-facing dashboards, queues, borrowers, applications, documents, OCR, visits, review actions, notifications, configuration, audit, servicing, reports, and user administration. |
| `/health` | Unauthenticated health response. |

Interactive API documentation is available at `/api/docs` and `/api/redoc` while the service is running.

## Architecture

```text
backend/
├── app/
│   ├── main.py                 FastAPI setup, middleware, routes, static/templates
│   ├── core/                   configuration, database pools, security, dependencies, audit
│   ├── api/v1/mobile.py        Android API contract
│   ├── domains/                auth, users, loans, documents, OCR, visits, workflow, audit
│   └── services/               OCR, PDF, email, cloud storage, dashboards and servicing
├── migrations/                 SQL schema/data migrations and migration helper
├── requirements.txt
└── test_*.py                   route, HTTP and responsive smoke checks
```

Domain routers and services own application rules; repositories execute parameterized SQL from each domain's `queries/` directory. `app/main.py` applies CORS, request-ID and security-header middleware, mounts `/static`, and manages the connection pool over the application lifecycle.

## Prerequisites

- Python 3.10 or newer.
- PostgreSQL for the included migration runner and a production-like setup. SQLite is the local fallback when `DATABASE_URL` is omitted; the repository's `fieldcrm.db` is used.
- For image-based PDF OCR: Tesseract installed and on `PATH`; `pdf2image` may also require Poppler available on `PATH`.

## Configure and run

Run from the repository root:

```powershell
python -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -r backend\requirements.txt
```

Create `backend/.env`. This is a development-oriented example; never commit production credentials.

```env
# Required in a stable deployment
DATABASE_URL=postgresql://USER:PASSWORD@HOST:5432/fieldcrm
JWT_SECRET_KEY=replace-with-a-long-random-secret
COOKIE_SECURE=false

# Browser origins permitted by CORS (the application defaults to localhost:8000)
# CORS_ORIGINS=["https://your-web-origin.example"]

# Optional mail and public-link configuration
APP_BASE_URL=http://127.0.0.1:8000
SMTP_HOST=
SMTP_PORT=587
SMTP_USERNAME=
SMTP_PASSWORD=
SMTP_FROM_EMAIL=
SMTP_FROM_NAME=FieldCRM
SMTP_USE_TLS=true

# Optional: Cloudinary document storage. Without all three, uploads use frontend/static/uploads.
CLOUDINARY_CLOUD_NAME=
CLOUDINARY_API_KEY=
CLOUDINARY_API_SECRET=

# Optional verification and screening providers
QORE_API_KEY=
QORE_BASE_URL=https://api.qoreid.com
CREDIT_REGISTRY_USERNAME=
CREDIT_REGISTRY_PASSWORD=
CREDIT_REGISTRY_BASE_URL=https://api.creditregistry.com
AML_YOUVERIFY_TOKEN=
AML_BASE_URL=
```

`DOCUMENT_UPLOAD_DIR`, `DOCUMENT_MAX_UPLOAD_BYTES`, `DOCUMENT_ALLOWED_MIME_TYPES`, `EMAIL_SERVICE_URL`, and `ORG_REGISTRATION_SECRET` are additional supported settings. Defaults are defined in `app/core/config.py`. Set `COOKIE_SECURE=true`, a fixed `JWT_SECRET_KEY`, an HTTPS `APP_BASE_URL`, and `ORG_REGISTRATION_SECRET` before production. If no JWT secret is provided locally, the server generates one on startup, so existing sessions expire when it restarts.

For PostgreSQL, the included runner applies migrations `001` through `005` and demo data:

```powershell
python backend\migrations\run_migration.py
```

Later migration files are maintained separately; review and apply them deliberately for an existing deployment. Do not run demo/seed migrations against a production tenant.

Start the service:

```powershell
uvicorn app.main:app --app-dir backend --reload
```

Open `http://127.0.0.1:8000/login`, `http://127.0.0.1:8000/api/docs`, or `http://127.0.0.1:8000/api/v1/health`.

## Verification

With the virtual environment activated, run the repository checks from the root:

```powershell
python test_imports.py
python backend\test_http.py
python backend\test_routes_render.py
python backend\test_responsive_smoke.py
```

The HTTP and rendering checks require the expected local database/application setup. Inspect the repository root README for demo account details.

## Operational notes

- The backend is authoritative: clients can request actions but must not decide eligibility, permissions, approval thresholds, or workflow transitions locally.
- Local documents are placed under `frontend/static/uploads`; configure a durable volume or Cloudinary for deployed environments.
- Cloudinary credentials enable cloud upload; a cloud upload failure falls back to local storage.
- OCR is background work after upload. A document can be stored before extraction finishes, and reviewers must verify OCR output rather than treating it as fact.
- Keep API routers thin, put business rules in services, and use repository queries rather than interpolating user input into SQL.
