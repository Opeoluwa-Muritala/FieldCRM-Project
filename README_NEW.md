# FieldCRM

FieldCRM is a multi-platform loan origination and servicing workflow for microfinance and retail lending teams. The repository combines a FastAPI backend, server-rendered role-based web dashboards, and a Kotlin/Jetpack Compose Android app.

## What is in this repository

- backend/: FastAPI application, domain services, repositories, SQL migrations, authentication, and API routes.
- frontend/: Jinja2 templates, shared page shells, CSS, and JavaScript for the web experience.
- android/: Native Android app with explicit screen navigation and offline-friendly workflow support.
- shared/: Kotlin Multiplatform module for models, API client scaffolding, SQLDelight storage, and sync foundations.
- scripts/: Helper utilities for seeding demo data, checking database state, and scanning the backend.

## Core workflow

FieldCRM supports the full loan lifecycle:

- borrower intake and registration
- loan application creation and step-based form capture
- document upload and OCR review
- guarantor and pledge/trust capture
- visitation reporting and signoff
- credit review and branch approval
- auditor compliance checks and audit trail review
- system administration and role-based workflow management

## Quick start

### 1. Prerequisites

- Python 3.10+
- PostgreSQL
- JDK 17 for Android builds
- Android Studio and Android SDK

### 2. Create the Python environment

```powershell
python -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -r backend\requirements.txt
```

### 3. Configure environment variables

Create backend/.env with values such as:

```env
DATABASE_URL=postgresql://postgres:postgres@localhost:5432/fieldcrm
JWT_SECRET_KEY=replace-with-a-long-random-secret
COOKIE_SECURE=false
```

### 4. Create the schema and seed demo data

```powershell
python backend\migrations\run_migration.py
```

### 5. Start the backend

```powershell
uvicorn app.main:app --app-dir backend --reload
```

The web app should then be available at:

- http://127.0.0.1:8000/login
- http://127.0.0.1:8000/api/docs
- http://127.0.0.1:8000/api/redoc
- http://127.0.0.1:8000/api/v1/health

### 6. Build the Android app

If needed, create local.properties at the repository root with the Android SDK path:

```properties
sdk.dir=C\:\Users\YOUR_USER\AppData\Local\Android\Sdk
```

Then run:

```powershell
.\gradlew.bat :android:compileDebugKotlin
.\gradlew.bat :android:assembleDebug
```

## Demo accounts

The seeded demo users use the password password123.

| Role | Email |
| --- | --- |
| System Admin | admin@mmfb.com |
| Branch Manager | adebayo@mmfb.com |
| Loan Officer | chidi@mmfb.com |
| Credit Officer | fatima@mmfb.com |
| Auditor | samuel@mmfb.com |

## Useful repository scripts

- populate_db.py: clears and repopulates loan workflow demo data for the current database.
- reset_online_demo.py: applies the newer online-demo migrations and seeds users and loan records for the rendered demo environment.
- check_seed_state.py: prints a quick snapshot of seeded users and table counts.
- run_scans.py: calls the configured scanning backend for a set of core source files.

## Development notes

- Keep backend routes thin and delegate business rules to services.
- Put data access in repositories and domain SQL files.
- Keep Android screens separated by feature and keep state in ViewModels or repositories rather than composables.
- Do not commit local environment files such as backend/.env, local.properties, build outputs, or virtual environments.

## Documentation

- backend/README.md for backend-specific setup and API integration notes
- DESIGN.md for the product and UI design system reference
- docs/ for workflow and parity planning notes

## Troubleshooting

- If imports fail, activate the virtual environment and reinstall backend requirements.
- If migrations fail, confirm backend/.env exists and DATABASE_URL points to a reachable PostgreSQL database.
- If Android builds cannot find the SDK, fix local.properties with the correct sdk.dir.
- If Gradle uses the wrong Java version, make sure JAVA_HOME points to JDK 17.
