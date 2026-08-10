# FieldCRM

> This document covers the complete web, backend, Android, data, workflow, and role model for the project.

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
- Head CRM is the approving authority for the CRM desk and sends approved files to the Executive Director.
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
| Head CRM | CRM approval | Approve/reject CRM dossier review and route approved files to the Executive Director. |
| Auditor | Compliance | Review controls, exceptions, workflow history, and audit trail. |
| Executive Director | Executive approval | Review executive queue; request MD input when needed; retain final ED approval. |
| Managing Director | Escalations | Provide advice to ED, approve required high-value files, and manage board referrals. |
| Legal | Legal and collateral | Review the legal queue, pledged assets, supporting evidence, and valuations. |
| System Admin | Administration | Invite users, assign roles, deactivate access, and review system activity. |

## Detailed role functions

### Relationship Officer (`account_officer`)

- Search existing customers and prefill reusable customer information.
- Create applications and select active server-managed loan products.
- Capture personal, contact, employment, business, location, income, expense, and loan-purpose data.
- Capture guarantors, references, pledged assets, collateral, business P&L, and field-visitation information.
- Upload required documents, review OCR results, add a recommendation, and submit a complete file.
- Work personal, returned, and task queues from web or Android.
- Read previously pulled Android data while offline; later approval decisions remain outside this role.

### Team Lead (`branch_manager`)

- Work the branch/team review queue and inspect the full dossier.
- Review documents, recommendations, readiness information, and visitation evidence.
- Sign off permitted visitation reports and record branch concurrence.
- Return incomplete files with a reason or forward satisfactory files to the Supervisor.
- Monitor branch pipeline and pending signoffs.

### Supervisor (`branch_supervisor`)

- Perform the second branch-control review after Team Lead concurrence.
- Inspect the dossier, evidence, documents, and prior recommendations.
- Add a recommendation, return the file with a reason, or advance it to Credit Analyst review.
- View permitted current-loan and workflow-history information.

### Credit Analyst (`credit_analyst`)

- Work the underwriting queue and review affordability, repayment feasibility, P&L, and collateral.
- Pull or inspect configured credit-bureau data.
- Review OCR/data-quality exceptions and checklist requirements.
- Record a recommended amount and underwriting notes.
- Return an incomplete file or advance an acceptable file to CRM review.

### CRM Officer (`crm`)

- Validate the assembled dossier, readiness status, consents, documents, and recommendations.
- Upload CRM memos, add CRM notes, and return or advance the file to Head CRM.
- Generate configured offer letters where permitted.
- Record disbursement after final approval and maintain repayment/servicing information.
- Review CRM queues, recent disbursements, and portfolio-at-risk reports.
- Never submit an applicant signature on the applicant's behalf.

### Head CRM (`head_crm`)

- Review the CRM Officer's completed dossier and recommendations.
- Record Head CRM notes and approve or return the file.
- Advance approved files to Executive Director review.
- Monitor CRM dashboards, portfolio queues, disbursement readiness, and PAR information.

### Audit (`auditor`)

- Review audit history, compliance flags, workflow events, OCR/document exceptions, and authorized dossiers.
- Review reporting views made available to Audit.
- Confirm decision traceability without silently changing operational data.
- Operate as independent oversight rather than a mandatory approval stage.

### Executive Director (`ed`)

- Work the ED queue and review Head CRM-approved files, documents, recommendations, and committee evidence.
- Issue the final ED approval/disbursement instruction where applicable.
- Escalate eligible files to MD for input.
- Receive MD advice returned to ED and make the applicable final decision.
- Review executive dashboards and PAR information.

### Managing Director (`md`)

- Work the MD queue and review the complete executive dossier.
- Approve files for which MD is the final authority.
- Add comments or return an advisory file to ED.
- Record board referrals and preserve them in application history.
- Review executive dashboards and PAR information.

### Legal (`legal`)

- Work the legal queue and inspect authorized application/collateral information.
- Capture or update valuation information.
- Review pledged assets, ownership evidence, and supporting documents.
- Preserve valuation/legal actions in audit history.
- Provide legal/collateral review without replacing credit or executive approval.

### System Admin (`system_admin`)

- Invite users and manage registration flows.
- Assign roles and branches; activate, deactivate, or soft-delete managed users subject to service rules.
- Create and manage organisation branches.
- Review administrative activity and system metrics.
- Manage configured interest presets and related administrative settings.
- Operate within the authenticated organisation; the role does not grant cross-organisation access.

### Role identifiers and legacy names

| Display name | Current identifier | Compatibility name |
| --- | --- | --- |
| Relationship Officer | `account_officer` | `loan_officer` |
| Team Lead | `branch_manager` | Branch Manager |
| Supervisor | `branch_supervisor` | Branch Supervisor |
| Credit Analyst | `credit_analyst` | - |
| CRM Officer | `crm` | - |
| Head CRM | `head_crm` | - |
| Audit | `auditor` | Auditor |
| Executive Director | `ed` | - |
| Managing Director | `md` | - |
| Legal | `legal` | - |
| System Admin | `system_admin` | Admin |

The Android `EXECUTIVE` enum is retained only for compatibility with older saved sessions. New assignments should use ED or MD.

### Role capability matrix

| Capability | Account Officer | Branch Manager | Branch Supervisor | Credit Analyst | CRM Officer | Head CRM | Audit | ED | MD | System Admin |
| --- | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
| Create a loan application | Yes | — | — | — | — | — | — | — | — | — |
| Maintain intake, guarantors and supporting documents | Yes | — | — | — | — | — | Read | Read | Read | Read |
| Verify OCR / correct extracted data | Yes | — | — | Resolve exceptions | Read | Read | Read | Read | Read | Read |
| Review branch file | — | Yes | Yes | — | — | — | — | — | — | — |
| Complete underwriting recommendation | — | — | — | Yes | — | — | Read | Read | Read | Read |
| Review CRM dossier | — | — | — | — | Yes | — | Read | Read | Read | Read |
| Approve CRM dossier and route to ED | — | — | — | — | — | Yes | — | — | — | — |
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

### Runtime data flow

```text
Web browser / Android app
          |
          v
FastAPI authentication, authorization, and workflow services
          |
          +--> PostgreSQL (authoritative business data)
          +--> Redis (short-lived server cache and rate limiting)
          +--> private document storage / Cloudinary

Android also uses SQLDelight/SQLite for device cache and offline work.
```

## Caching, offline storage, and synchronization

### Web and API cache

- PostgreSQL remains the server system of record.
- Redis stores short-lived authenticated user profiles, role dashboard bundles, selected dossier/review data, branch/mobile reads, and long-lived fixed mobile content.
- Cache keys include organisation, user, role, parameters, and application scope where applicable.
- Successful writes increment scoped cache versions so affected reads are refreshed.
- If Redis is unavailable, the backend logs the problem and continues against PostgreSQL.
- `CACHE_REDIS_URL` may share `RATE_LIMIT_REDIS_URL`; production Redis connections must use `rediss://`.

### Android device cache

- Application summaries are stored in SQLDelight after the first successful list pull.
- A full review dossier is stored after its first successful detail pull.
- Stored content is rendered immediately on later opens while network refresh runs silently.
- A connected WorkManager job refreshes summaries and up to ten oldest stored dossiers per cycle.
- Failed pulls preserve the last successful local copy.
- Supported offline mutations remain queued until synchronization succeeds.
- Pending documents are encrypted locally before upload.
- Network, JSON parsing, and SQLDelight work run off the Compose UI thread.

### Android synchronization order

1. Push queued local application mutations.
2. Pull current application summaries and refresh eligible stored dossiers.
3. Decrypt and upload pending documents, then mark successful uploads synchronized.

The device database is a replaceable cache/offline store, not a second source of truth.

## Security and authorization summary

- Role, organisation, branch, assignment, and workflow-stage checks are enforced by the backend.
- A visible navigation item does not grant access; manually entered unauthorized routes must still return `403`.
- Browser sessions use HttpOnly cookies and production HTTPS settings.
- Android session material uses encrypted Android preferences.
- Refresh tokens are rotated, stored as hashes server-side, and checked for replay.
- Production Redis connections use TLS.
- Documents are validated and served through private or signed delivery.
- Material workflow and administrative changes produce audit and/or workflow records.
- Secrets belong in environment/deployment settings and must never be committed.

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

# Optional locally; required for production distributed rate limiting.
RATE_LIMIT_REDIS_URL=redis://127.0.0.1:6379/0
CACHE_REDIS_URL=redis://127.0.0.1:6379/0

# Keep nonce enforcement enabled unless a reviewed deployment requires otherwise.
CSP_NONCE_ENFORCED=true

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
- Configure `RATE_LIMIT_REDIS_URL` for production distributed rate limiting.
- Configure `CACHE_REDIS_URL` or let it share the rate-limit Redis deployment.
- Use `rediss://` for production Redis URLs.
- Keep `CSP_NONCE_ENFORCED=true` unless a reviewed deployment constraint requires otherwise.
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

Install the development test dependencies, then run these checks before release:

```powershell
pip install -r requirements-dev.txt
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

Before building, set the backend endpoint in `gradle.properties`:

```properties
FIELDCRM_API_BASE_URL=http://10.0.2.2:8000
```

`10.0.2.2` reaches the development machine from the standard Android emulator. Use a reachable LAN address for a physical device.

Android prerequisites:

- JDK 17.
- Android SDK 36.
- Android Studio for emulator/device workflows.

The first Android build can take several minutes while Gradle resolves and compiles dependencies.

The shared module contains models, API-client scaffolding, SQLDelight storage, and sync foundations.

## Development conventions

- Keep routers thin; place workflow/business rules in services.
- Keep SQL in domain query files and parameterize every value.
- Scope every database query by organization and authenticated user where applicable.
- Treat PostgreSQL as authoritative and caches as replaceable copies.
- Keep network calls, JSON parsing, and database work out of Compose UI code.
- Preserve last-known-good device data on transient network failure.
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

### Token Authentication System & Rotation Flow

The application implements a secure authentication system featuring short-lived access tokens and single-use rotating refresh tokens:

1. **Access Tokens**: Signed JWTs using HS256 algorithm. They expire every **10 minutes** and contain standard non-sensitive claims (`user_id`, `role`, `org_id`, `iat`, `exp`, `jti`).
2. **Refresh Tokens**: Opaque base64url-encoded random strings stored using a secure SHA-256 hash in the database.
3. **Session Lifetimes**: Expire after a strict timeline of **2 days**.
4. **Cookie Security (Web)**:
   - Delivered via `Set-Cookie` with `HttpOnly` and `Secure` flags.
   - Restrained to `SameSite=Strict` and scoped strictly to `path=/api/v1/auth/refresh`.
5. **JSON Delivery (Mobile)**:
   - For native client apps, refresh tokens are transmitted via request/response JSON payloads.
6. **Single-Use & Rotation**:
   - The token database tracking keeps a group chain via `family_id`.
   - Every invocation of `POST /auth/refresh` rotates the refresh token: the old token is marked as `used_at = NOW()`, and a new one is issued with the same `family_id`.
7. **Replay Attack / Reuse Detection**:
   - If a replayed refresh token is detected (where `used_at` is already populated), the system immediately revokes the **entire rotation chain** (`revoked_at = NOW()` on all tokens with matching `family_id`), logs a high-priority security event, and returns HTTP 401.
