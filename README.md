# FieldCRM

FieldCRM is a loan origination and processing CRM for microfinance and retail lending teams. It manages the loan workflow from borrower intake through document review, OCR checks, credit review, branch approval, disbursement readiness, and audit.

The project contains:

- A FastAPI backend with REST endpoints and server-rendered Jinja pages
- Role-aware web dashboards for field and back-office staff
- A native Android app built with Kotlin and Jetpack Compose
- A Kotlin Multiplatform `shared` module for mobile models, API access, local storage, and sync foundations

## What This App Does

FieldCRM tracks applications through a controlled lending workflow:

1. Loan officers create borrowers and start loan applications.
2. Required borrower, guarantor, pledge, visitation, and document information is collected.
3. Uploaded documents can move through OCR review and correction.
4. Credit officers review risk, documents, and recommendations.
5. Branch managers review and approve or return applications.
6. Auditors and system admins review compliance, audit history, and operational activity.

The web app is role-aware and device-aware. The same backend renders different dashboard pages and navigation for loan officers, branch managers, credit officers, auditors, and system admins. Mobile web users receive mobile shells and tab bars; desktop users receive desktop shells and sidebars.

## Repository Layout

```text
FieldCRM/
|-- backend/                  FastAPI app, domain routers, services, repositories, migrations
|-- frontend/                 Jinja templates, static CSS, JavaScript, and images
|-- android/                  Android Jetpack Compose app
|-- shared/                   Kotlin Multiplatform module used by Android
|-- gradle/                   Gradle wrapper files
|-- build.gradle.kts          Root Gradle plugin configuration
|-- settings.gradle.kts       Gradle module includes
|-- test_imports.py           Backend import smoke test
`-- README.md
```

## Main Technologies

Backend:

- Python
- FastAPI
- Jinja2
- asyncpg / psycopg2
- SQL files grouped by domain
- Pydantic settings and schemas

Web frontend:

- Server-rendered Jinja templates
- Vanilla CSS and JavaScript
- Separate desktop and mobile shells
- Role-specific dashboards and navigation

Android:

- Kotlin
- Jetpack Compose
- Material 3
- ViewModels
- WorkManager
- Kotlin Multiplatform shared module
- SQLDelight in the shared module
- Ktor client scaffolding in the shared module

## Backend Setup

Run these commands from the repository root.

### 1. Create And Activate A Virtual Environment

```powershell
python -m venv .venv
.\.venv\Scripts\Activate.ps1
```

### 2. Install Backend Dependencies

```powershell
pip install -r backend\requirements.txt
```

### 3. Configure Environment Variables

Create `backend/.env`:

```env
DATABASE_URL=postgresql://postgres:postgres@localhost:5432/fieldcrm
JWT_SECRET_KEY=replace-with-a-long-random-secret
COOKIE_SECURE=false
```

The app can fall back to a local SQLite path if no database URL is provided, but the bundled migration and seed scripts are PostgreSQL-oriented. Use PostgreSQL for the full demo dataset.

### 4. Run Migrations And Seed Data

```powershell
python backend\migrations\run_migration.py
```

This runs:

- `001_full_schema.sql`
- `002_ref_no_sequence.sql`
- `003_seed_demo.sql`

### 5. Start The Web App And API

```powershell
uvicorn app.main:app --app-dir backend --reload
```

Open:

- Web app: `http://127.0.0.1:8000/login`
- API docs: `http://127.0.0.1:8000/api/docs`
- ReDoc: `http://127.0.0.1:8000/api/redoc`
- Health check: `http://127.0.0.1:8000/api/v1/health`

## Demo Login Accounts

Seeded users all use this password:

```text
password123
```

| Role | Email |
| --- | --- |
| System Admin | `admin@mmfb.com` |
| Branch Manager | `adebayo@mmfb.com` |
| Loan Officer | `chidi@mmfb.com` |
| Credit Officer | `fatima@mmfb.com` |
| Auditor | `samuel@mmfb.com` |

## Web Routes To Try

After logging in, useful routes include:

- `/dashboard`
- `/applications`
- `/applications/new`
- `/pipeline`
- `/borrowers`
- `/audit`
- `/audit-trail`
- `/compliance-flags`
- `/users`
- `/system-activity`

Some routes are role-gated. If a user does not have the required role, the backend redirects or returns an authorization error.

## Android Setup

The Android app is in the `android` module and depends on the `shared` Kotlin Multiplatform module.

Recommended local tools:

- Android Studio
- Android SDK installed locally
- JDK 17
- Gradle wrapper from this repository

Create a local `local.properties` file if Android Studio has not already created one:

```properties
sdk.dir=C\:\\Users\\YOUR_USER\\AppData\\Local\\Android\\Sdk
```

Do not commit `local.properties`; it is machine-specific.

### Compile The Android App

On Windows:

```powershell
.\gradlew.bat :android:compileDebugKotlin
```

Build a debug APK:

```powershell
.\gradlew.bat :android:assembleDebug
```

Open the project in Android Studio if you want to run it on an emulator or physical device.

## Android App Structure

```text
android/src/main/java/com/fieldcrm/android/
|-- MainActivity.kt
|-- core/
|   |-- network/
|   `-- session/
|-- data/repository/
|-- sync/
`-- ui/
    |-- screens/
    `-- viewmodel/
```

Current mobile screens include:

- Login
- Dashboard
- Borrower list
- Borrower details
- Create borrower
- Application list
- Application details
- Create application

The Android implementation is still a work in progress. Some repositories, sync paths, and authentication flows are scaffolded or mocked while the backend API contract is being aligned.

## Shared Module

The `shared` module contains cross-platform pieces used by the Android app:

- Borrower and loan application models
- Ktor API client scaffolding
- SQLDelight schema for local/offline storage
- Sync repository foundations

The module is configured for Android and has iOS target scaffolding for future use.

## Running Checks

Backend import smoke test:

```powershell
python test_imports.py
```

HTTP check after starting the backend:

```powershell
python backend\test_http.py
```

Role and mobile/desktop render check after starting the backend with seeded data:

```powershell
python backend\test_routes_render.py
```

Android Kotlin compile:

```powershell
.\gradlew.bat :android:compileDebugKotlin
```

## Configuration Reference

Common backend variables:

| Variable | Purpose |
| --- | --- |
| `DATABASE_URL` | Full database connection string |
| `POSTGRES_SERVER` | PostgreSQL server used when building a default URL |
| `POSTGRES_USER` | PostgreSQL username |
| `POSTGRES_PASSWORD` | PostgreSQL password |
| `POSTGRES_DB` | PostgreSQL database name |
| `JWT_SECRET_KEY` | JWT signing secret |
| `COOKIE_SECURE` | Set to `true` for secure HTTPS cookies |

For production or shared environments, always set a stable `JWT_SECRET_KEY`. If no key is provided, the app can generate an ephemeral development fallback, which invalidates sessions after restart.

## Development Notes

- Backend routes should stay thin and delegate business rules to services.
- Runtime data access should go through repositories and domain SQL files.
- Templates are split by role and by shared workflow pages.
- Android screens should stay in separate files under `ui/screens`.
- Android state should live in ViewModels, repositories, or workers, not directly in composables.
- Local database files, build outputs, virtual environments, `.env`, and `local.properties` should not be committed.

## Troubleshooting

If backend imports fail, make sure the virtual environment is active and dependencies are installed:

```powershell
.\.venv\Scripts\Activate.ps1
pip install -r backend\requirements.txt
```

If migrations fail, check that `backend/.env` exists and `DATABASE_URL` points to a reachable PostgreSQL database.

If Android builds cannot find the SDK, create or fix `local.properties` with the correct `sdk.dir`.

If Gradle uses the wrong Java version, run it from a shell where `JAVA_HOME` points to JDK 17.

If the first Android build is slow, Gradle may be downloading the wrapper distribution, Android Gradle Plugin, Kotlin dependencies, and Compose dependencies.
