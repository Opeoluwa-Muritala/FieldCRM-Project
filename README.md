# FieldCRM

FieldCRM is a loan workflow, document review, and field-operations CRM for microfinance teams. It combines a FastAPI web application, role-aware server-rendered dashboards, an Android Jetpack Compose app, and a Kotlin Multiplatform shared module for mobile data models, API access, and offline sync foundations.

## What It Does

FieldCRM supports the operational path of a microfinance loan from intake through review, approval, and audit:

- Borrower and loan application intake
- Role-based dashboards for system admins, branch managers, loan officers, credit officers, and auditors
- Loan pipeline tracking across intake, OCR review, credit review, branch approval, disbursement readiness, disbursed, returned, and rejected stages
- Guarantor, visitation, document upload, OCR review, credit review, approval, return, and audit workflows
- Device-aware web rendering with separate desktop shells and mobile shells
- Android screens for login, dashboard, borrowers, and applications
- Shared Kotlin models and API client scaffolding for mobile sync

## Repository Layout

```text
FieldCRM/
|-- backend/                  FastAPI app, domain services, raw SQL repositories, migrations
|-- frontend/                 Jinja templates, static CSS, JavaScript, and images
|-- android/                  Native Android app built with Jetpack Compose
|-- shared/                   Kotlin Multiplatform module used by Android and future clients
|-- gradle/                   Gradle wrapper files
|-- build.gradle.kts          Root Gradle plugin configuration
|-- settings.gradle.kts       Gradle modules: :shared and :android
|-- test_imports.py           Backend import smoke test
`-- run_scans.py              Optional external security scanner helper
```

## Architecture

### Backend

The backend is a FastAPI application under `backend/app`.

Key backend areas:

- `backend/app/main.py` wires the FastAPI app, static files, templates, CORS, security headers, login/logout routes, and mounted routers.
- `backend/app/core/` contains configuration, security, database access, middleware, audit helpers, pagination, and template utilities.
- `backend/app/domains/*/` contains feature domains. Each domain typically has:
  - `router.py` for HTTP routes
  - `service.py` for business logic
  - `repository.py` for data access
  - `queries/*.sql` for raw SQL statements
- `backend/migrations/` contains PostgreSQL schema and seed SQL.
- `frontend/templates/` contains the server-rendered web UI.

The backend is intentionally domain-oriented. Keep route handling, business rules, and SQL access separated.

### Web UI

The web app is rendered with Jinja templates and static assets from `frontend/`.

Important template groups:

- `frontend/templates/base/` contains desktop and mobile shells.
- `frontend/templates/components/` contains role-specific navigation components.
- `frontend/templates/shared/` contains reusable workflow pages.
- `frontend/templates/{role}/` contains role-specific dashboards and application detail pages.

The backend detects device type and role to choose the right shell/template combination.

### Android App

The Android app lives in `android/` and uses Jetpack Compose.

Important Android areas:

- `android/src/main/java/com/fieldcrm/android/MainActivity.kt` contains app-level screen routing.
- `android/src/main/java/com/fieldcrm/android/ui/screens/` contains one file per Compose screen.
- `android/src/main/java/com/fieldcrm/android/ui/viewmodel/` contains app, login, borrower, and application view models.
- `android/src/main/java/com/fieldcrm/android/data/repository/` contains mobile repository scaffolding.
- `android/src/main/java/com/fieldcrm/android/sync/` contains WorkManager sync scaffolding.

### Shared Kotlin Module

The shared module lives in `shared/` and is configured as a Kotlin Multiplatform library.

It currently includes:

- Shared borrower and loan application models
- Ktor API client scaffolding
- SQLDelight database schema
- Sync repository foundations
- Android and iOS targets

## Prerequisites

Install these before running the project:

- Python 3.11 or newer
- Java Development Kit compatible with Android Gradle Plugin 8.2.x
- Android Studio for Android development
- PostgreSQL for the seeded backend workflow data
- PowerShell or a POSIX-compatible shell for local commands

Gradle is provided through the wrapper scripts:

- Windows: `gradlew.bat`
- macOS/Linux: `./gradlew`

The first Gradle run downloads the configured Gradle distribution.

## Backend Setup

From the repository root:

```powershell
python -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -r backend\requirements.txt
```

Create `backend/.env`:

```env
DATABASE_URL=postgresql://postgres:postgres@localhost:5432/fieldcrm
JWT_SECRET_KEY=replace-with-a-long-random-secret
COOKIE_SECURE=false
```

Then run the PostgreSQL migrations and seed data:

```powershell
python backend\migrations\run_migration.py
```

Start the backend:

```powershell
uvicorn app.main:app --app-dir backend --reload
```

Open:

- Web app: `http://127.0.0.1:8000/login`
- API docs: `http://127.0.0.1:8000/api/docs`
- Health check: `http://127.0.0.1:8000/api/v1/health`

## Demo Users

The seed data creates one organization, demo loan applications, and these users. All seeded users use the password `password123`.

| Role | Email |
| --- | --- |
| System Admin | `admin@mmfb.com` |
| Branch Manager | `adebayo@mmfb.com` |
| Loan Officer | `chidi@mmfb.com` |
| Credit Officer | `fatima@mmfb.com` |
| Auditor | `samuel@mmfb.com` |

## Database Notes

`backend/app/core/config.py` can fall back to a local `fieldcrm.db` SQLite URL when no PostgreSQL settings are supplied. That fallback is useful for lightweight local experiments, but the provided migration runner and seed SQL are PostgreSQL-oriented.

For the full seeded workflow, use PostgreSQL and set `DATABASE_URL` in `backend/.env`.

Local database files and secrets should not be committed.

## Running Tests And Checks

Backend import smoke test:

```powershell
python test_imports.py
```

Basic HTTP connectivity check, after starting the backend:

```powershell
python backend\test_http.py
```

Role and mobile/desktop render checks, after starting the backend with seeded data:

```powershell
python backend\test_routes_render.py
```

Compile the Android Kotlin sources:

```powershell
.\gradlew.bat :android:compileDebugKotlin
```

Build all Gradle modules:

```powershell
.\gradlew.bat build
```

## Android Development

Open the repository in Android Studio and use the `android` run configuration, or run Gradle from the command line:

```powershell
.\gradlew.bat :android:assembleDebug
```

The mobile app currently uses a simple in-memory screen router in `AppViewModel`. Compose screens are separated by concern:

- `LoginScreen.kt`
- `DashboardScreen.kt`
- `BorrowerListScreen.kt`
- `BorrowerDetailScreen.kt`
- `CreateBorrowerScreen.kt`
- `ApplicationListScreen.kt`
- `ApplicationDetailScreen.kt`
- `CreateApplicationScreen.kt`
- `DetailField.kt`

Keep new screens in their own files under `ui/screens/`, and keep business or data-fetching logic in view models or repositories.

## Configuration

Common backend environment variables:

| Variable | Purpose | Default |
| --- | --- | --- |
| `DATABASE_URL` | Full database connection URL | Local SQLite path if unset |
| `POSTGRES_SERVER` | PostgreSQL host used to build a URL when `DATABASE_URL` is unset | Empty |
| `POSTGRES_USER` | PostgreSQL username | `postgres` |
| `POSTGRES_PASSWORD` | PostgreSQL password | `postgres` |
| `POSTGRES_DB` | PostgreSQL database name | `fieldcrm` |
| `JWT_SECRET_KEY` | JWT signing secret | Generated ephemeral fallback |
| `COOKIE_SECURE` | Marks cookies secure for HTTPS deployments | `false` |

Use a stable `JWT_SECRET_KEY` in any environment where sessions should survive a restart.

## Development Guidelines

- Keep backend code organized by domain.
- Put reusable business logic in services, not routers.
- Keep raw SQL in `queries/*.sql` when adding repository queries.
- Avoid committing generated local database files, secrets, or build outputs.
- Keep web templates role-aware and device-aware.
- Keep Android screens focused on UI composition.
- Keep Android state and side effects in view models, repositories, or workers.
- Prefer shared Kotlin models when data crosses mobile boundaries.

## Common Workflows

### Add A Backend Domain Route

1. Add or update SQL in `backend/app/domains/<domain>/queries/`.
2. Add repository methods in `repository.py`.
3. Add business rules in `service.py`.
4. Add request/response handling in `router.py`.
5. Mount the router in `backend/app/main.py` if it is a new domain.
6. Add or update tests/check scripts.

### Add A Web Page

1. Add a route in the relevant domain router.
2. Build the context with `build_template_context`.
3. Add the template under the matching role or shared folder.
4. Reuse base shells and navigation components.
5. Verify desktop and mobile rendering paths.

### Add An Android Screen

1. Create a new file in `android/src/main/java/com/fieldcrm/android/ui/screens/`.
2. Add view state and actions to the appropriate view model.
3. Add the screen route to `Screen` in `AppViewModel.kt`.
4. Wire navigation in `MainActivity.kt`.
5. Compile with `.\gradlew.bat :android:compileDebugKotlin`.

## Security Notes

The backend sets several response headers by default, including frame protection, content security policy, content type sniffing protection, referrer policy, and cache-control headers. Session cookies are HTTP-only and can be marked secure with `COOKIE_SECURE=true`.

For production:

- Set a strong `JWT_SECRET_KEY`.
- Use HTTPS and set `COOKIE_SECURE=true`.
- Restrict CORS origins to trusted hosts.
- Store secrets outside source control.
- Use a managed PostgreSQL instance or a properly backed-up database.

## Current Limitations

- The Android repositories and authentication are partially scaffolded and still contain mock behavior.
- The provided migration runner targets PostgreSQL.
- Some mobile API client routes are scaffolded ahead of full backend API coverage.
- File upload and OCR behavior currently includes demo/mock paths in parts of the workflow.

## Useful URLs

When running locally on port `8000`:

- `/login`
- `/dashboard`
- `/applications`
- `/applications/new`
- `/pipeline`
- `/borrowers`
- `/audit`
- `/api/docs`
- `/api/redoc`
- `/api/v1/health`
