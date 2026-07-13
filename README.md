# FieldCRM

FieldCRM is a role-based loan origination, approval, disbursement, and servicing platform for Mainstreet Microfinance Bank workflows. It combines a FastAPI web backend, Jinja2 desktop interface, PostgreSQL data store, Cloudinary-backed private documents, and a Kotlin Android client foundation.

## What it does

FieldCRM manages a loan file from initial customer intake through credit and management approvals to disbursement and repayment monitoring. Every operational action is organization-scoped and recorded in workflow/audit history.

### Credit workflow

```text
Account Officer
  → Branch Manager
  → Branch Supervisor
  → Credit Analyst
  → CRM Officer
  → Head CRM
  → Audit
  → Executive Director
  → Managing Director (only where required)
  → CRM disbursement and servicing
```

Rules:

- CRM Officer completes dossier review and sends the file to Head CRM.
- Head CRM is the approving authority for the CRM desk and sends approved files to Audit.
- Executive Director may request MD input; the file returns to ED for ED’s final decision.
- Loans above the configured ₦10m threshold require ED and MD approval.
- CRM records disbursement and maintains repayment/portfolio information.

## Roles

| Role | Primary workspace | Main actions |
| --- | --- | --- |
| Account Officer | Intake | Create applications, capture borrower data, documents, guarantors, visits, OCR review. |
| Branch Manager | Branch review | Review branch submissions, sign off visits, monitor pipeline. |
| Branch Supervisor | Supervision | Review branch-manager submissions before credit analysis. |
| Credit Analyst | Underwriting | Review credit files, resolve OCR/data exceptions, submit recommendation. |
| CRM Officer | Dossier review | Validate dossier completeness and send it to Head CRM. |
| Head CRM | CRM approval | Approve/reject CRM dossier review and route approved files to Audit. |
| Auditor | Compliance | Review controls, exceptions, workflow history, and audit trail. |
| Executive Director | Executive approval | Review executive queue; request MD input when needed; retain final ED approval. |
| Managing Director | Escalations | Provide advice to ED, approve required high-value files, and manage board referrals. |
| System Admin | Administration | Invite users, assign roles, deactivate access, and review system activity. |

### Role capability matrix

| Capability | Account Officer | Branch Manager | Branch Supervisor | Credit Analyst | CRM Officer | Head CRM | Audit | ED | MD | System Admin |
| --- | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
| Create a loan application | Yes | — | — | — | — | — | — | — | — | — |
| Maintain intake, guarantors and supporting documents | Yes | — | — | — | — | — | Read | Read | Read | Read |
| Verify OCR / correct extracted data | Yes | — | — | Resolve exceptions | Read | Read | Read | Read | Read | Read |
| Review branch file | — | Yes | Yes | — | — | — | — | — | — | — |
| Complete underwriting recommendation | — | — | — | Yes | — | — | Read | Read | Read | Read |
| Review CRM dossier | — | — | — | — | Yes | — | Read | Read | Read | Read |
| Approve CRM dossier and route to Audit | — | — | — | — | — | Yes | — | — | — | — |
| Review controls and audit trail | — | — | — | — | — | — | Yes | Read | Read | Read |
| Executive approval / MD advice | — | — | — | — | — | — | — | Yes | Advice / required approval | — |
| Record disbursement and repayments | — | — | — | — | Yes | Oversight | Read | Read | Read | — |
| Invite, change role, deactivate user | — | — | — | — | — | — | — | — | — | Yes |

`Read` means the role may view the information when it is part of its authorized queue or audit/reporting view; it does not grant an approval or edit action.

## Design system

The complete visual specification is in [DESIGN.md](DESIGN.md). It defines the **Institutional Modernist** system used throughout FieldCRM:

- Shield Purple `#2E0052` as the primary brand color and MFB Purple `#89268B` as accent.
- Off-white `#F2F2F2` canvas, white elevated cards, and restrained semantic status colors.
- Playfair Display for headings and DM Sans for functional UI text.
- 4px spacing grid; standard inputs are 48px and primary buttons are 52px high.
- Soft 4–8px corner radii and low-contrast ambient elevation.
- Desktop sidebar, desktop toolbar, clear page hierarchy, and role-specific work queues.

`DESIGN.md` also documents component usage, status chips, section cards, label/value rows, document scanning, and Android design foundations. The current web shell is desktop-only; do not reintroduce mobile navigation or mobile-only templates without an approved design change.

## API design

The interactive API contract is available when the app is running:

- OpenAPI JSON: `/openapi.json`
- Swagger UI: `/api/docs`
- ReDoc: `/api/redoc`

### API conventions

- Base API prefix: `/api/v1`.
- Authentication: JWT bearer token or the HttpOnly `session` cookie created by login.
- Request bodies: JSON for API mutations unless the endpoint uploads files; file uploads use `multipart/form-data`.
- Authorization: every protected endpoint derives the authenticated user from the token/session and applies a role check server-side.
- Data scope: repositories filter organization-bound records using the authenticated user’s `org_id`.
- Errors: FastAPI validation errors use HTTP `422`; authorization failures use `401`/`403`; domain errors return a JSON error message and a request ID.

### Core API groups

| Group | Prefix / examples | Purpose |
| --- | --- | --- |
| Authentication | `/api/v1/auth/login`, `/login-bearer`, `/logout` | Session and bearer-token login/logout. |
| Users | `/api/v1/users/invitations`, `/{id}/role`, `/{id}/deactivate` | Invitation, role assignment, access deactivation. |
| Mobile/workflow API | `/api/v1/mobile/...` | JSON endpoints used by Android and workflow automation. |
| Web workflow | `/applications/...` | Server-rendered desktop pages and form submissions. |
| Health | `/api/v1/health` | Deployment health confirmation. |

### Authentication example

```http
POST /api/v1/auth/login
Content-Type: application/x-www-form-urlencoded

username=person@example.com&password=your-password
```

The response returns `access_token` and also sets the HttpOnly session cookie for browser access. Do not store browser session tokens in local storage.

### Authorization model

API/page authorization uses `RoleChecker`. A navigation item must always have a matching authorized backend endpoint; sidebar visibility alone is never authorization. When adding an endpoint, document its role, organization scope, success response, and failure responses in the OpenAPI schema.

## Architecture

```text
frontend/                Jinja2 templates, desktop CSS, JavaScript, static assets
backend/app/             FastAPI application and domain modules
backend/app/domains/     Routers, services, repositories, parameterized SQL
backend/migrations/      PostgreSQL schema and data migrations
android/                 Jetpack Compose Android app
shared/                  Kotlin Multiplatform shared models and sync foundation
```

### Backend stack

- Python 3.10+
- FastAPI and Uvicorn
- PostgreSQL with `asyncpg` / psycopg-compatible SQL access
- Pydantic settings and schemas
- Jinja2 server-rendered pages
- JWT session authentication
- Cloudinary for authenticated document storage
- Pillow, PDF/OCR utilities for document handling

## Database

The active application database is PostgreSQL. The currently expected public tables are:

```text
audit_entries                 document_upload_jobs        repayment_records
board_referrals               documents                   repayment_schedule
committee_votes               guarantors                  stage_data
loan_applications             notifications               users
ocr_fields                    ocr_results                 visitation_reports
organisations                 password_reset_tokens       workflow_events
pledged_items
```

Migrations live in [`backend/migrations`](backend/migrations). Apply them in numeric order. Migration `013_async_document_uploads.sql` adds document upload-job support and related document metadata.

> Never run seed/reset migrations against production unless data loss has been explicitly approved.

## Local setup

### 1. Create a virtual environment

```powershell
python -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -r requirements.txt
```

### 2. Configure environment variables

Create `backend/.env`:

```env
DATABASE_URL=postgresql://USER:PASSWORD@HOST:5432/fieldcrm
JWT_SECRET_KEY=replace-with-a-long-random-secret
COOKIE_SECURE=false
APP_BASE_URL=http://127.0.0.1:8000

# Emailope / transactional email
EMAIL_SERVICE_URL=https://emailope.vercel.app/

# Optional Cloudinary private document storage
CLOUDINARY_CLOUD_NAME=
CLOUDINARY_API_KEY=
CLOUDINARY_API_SECRET=
```

Important production settings:

- Use a managed PostgreSQL database and a stable, secret `JWT_SECRET_KEY`.
- Set `COOKIE_SECURE=true` behind HTTPS.
- Store Cloudinary and database credentials only in deployment secrets.
- Do not commit `backend/.env`.

### 3. Run migrations

```powershell
python backend\migrations\run_migration.py
```

Review the migration script/environment before running it against a shared database.

### 4. Start the web app

```powershell
uvicorn app.main:app --app-dir backend --reload
```

Open:

- `http://127.0.0.1:8000/login`
- `http://127.0.0.1:8000/dashboard`
- `http://127.0.0.1:8000/api/docs`
- `http://127.0.0.1:8000/api/v1/health`

## Key web routes

| Route | Purpose | Access |
| --- | --- | --- |
| `/login` | Authentication | Public |
| `/dashboard` | Role-aware dashboard redirect/page | Authenticated |
| `/applications/new` | Start an application | Account Officer |
| `/applications/{id}` | Loan file hub | Authorized workflow user |
| `/applications/{id}/documents/upload` | Upload evidence | Authorized workflow user |
| `/applications/{id}/credit-review` | Underwriting decision | Credit Analyst |
| `/crm-review-queue` | CRM / Head CRM work queue | CRM, Head CRM |
| `/applications/{id}/crm-review` | CRM review or Head CRM approval | CRM, Head CRM |
| `/ed-queue` | ED approval queue | ED |
| `/md-queue` | MD escalation/approval queue | MD |
| `/applications/{id}/disburse` | Record disbursement | CRM |
| `/borrowers` | Current loans | Authorized operational roles |
| `/audit-trail` | Audit history | Audit, System Admin |
| `/users` | User administration | System Admin |

## User administration and invitations

System Admin creates a user invitation from **Users**. The system:

1. Creates an inactive user with the selected role.
2. Generates a time-limited registration token.
3. Sends the invitation email.
4. Lets the invitee set their password and activate their account.

The Users page supports role changes and account deactivation. Administrators cannot deactivate or change their own role through the page.

## Documents and privacy

Documents are private identity/employment/loan evidence and must not be published as public assets.

- Accepted formats: PDF, JPG/JPEG, PNG.
- Images are validated using declared MIME type, extension, and binary signature.
- Images can be normalized/compressed before upload; PDFs are not re-encoded.
- Cloudinary uploads use authenticated delivery when Cloudinary is configured.
- The `documents` table retains metadata, verification state, OCR state, and upload status.
- `document_upload_jobs` stores upload-job state for asynchronous upload processing.

## Email behavior

- Normal workflow notifications are sent as no-reply notifications.
- User invitations contain the registration link.
- MD board-referral messages use the sender’s name and email as the reply identity.

## Verification checklist

Run these checks before release:

```powershell
python test_imports.py
python backend\test_http.py
python backend\test_routes_render.py
```

Also perform a role-by-role smoke test:

1. Log in as every role.
2. Open every sidebar destination.
3. Create one test loan as Account Officer.
4. Advance it through every required workflow stage.
5. Verify CRM Officer → Head CRM → Audit routing.
6. Test ED/MD high-value approval and ED request-for-input flow.
7. Upload a valid document and attempt invalid file types/sizes.
8. Confirm workflow and audit records are written.
9. Confirm email delivery in the deployment environment.

## Deployment notes

The repository-level `requirements.txt` contains direct Python dependencies for Vercel’s Python builder. Ensure FastAPI is present there before deploying.

For every deployment:

- Configure all secrets in the hosting provider.
- Run/apply migrations separately and safely.
- Confirm `/api/v1/health` and `/login` respond.
- Verify static assets load without 404 errors.
- Review logs for import errors, failed email delivery, database errors, and authorization failures.

## Android and shared modules

The repository also contains a Kotlin/Jetpack Compose Android app and a Kotlin Multiplatform shared module.

Build Android:

```powershell
.\gradlew.bat :android:assembleDebug
```

The shared module contains models, API-client scaffolding, SQLDelight storage, and sync foundations.

## Development conventions

- Keep routers thin; place workflow/business rules in services.
- Keep SQL in domain query files and parameterize every value.
- Scope every database query by organization and authenticated user where applicable.
- Add audit/workflow events for every state-changing loan decision.
- Do not trust client-provided user, organization, or document ownership identifiers.
- Add migrations for schema changes; never alter production tables manually without a reviewed migration.

## Troubleshooting

| Symptom | Check |
| --- | --- |
| `ModuleNotFoundError: fastapi` on deploy | Ensure root `requirements.txt` is deployed and includes FastAPI. |
| Login redirects repeatedly | Check session cookie settings, JWT secret stability, and authorization of the target page. |
| Role dashboard fails | Confirm the role has dashboard data, template alias, sidebar component, and route authorization. |
| Document upload fails | Check allowed MIME type/signature, size, Cloudinary secrets, and `documents` columns. |
| 403 on a sidebar link | Align `RoleChecker` authorization with the role’s sidebar destination. |
| Missing table/column | Apply the applicable migration and verify live PostgreSQL schema. |

## Security

- Use HTTPS and secure cookies in production.
- Rotate JWT, database, Cloudinary, and email credentials if exposed.
- Keep document assets authenticated/private.
- Back up PostgreSQL and test restoration regularly.
- Restrict production database access to application and migration principals.
