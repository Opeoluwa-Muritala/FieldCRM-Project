# FieldCRM

FieldCRM is a multi-platform loan origination and servicing workflow for microfinance and retail lending teams. The repository combines a FastAPI backend, server-rendered role-based web dashboards, and a Kotlin/Jetpack Compose Android app.

## What is in this repository

- backend/: FastAPI application, domain services, repositories, SQL migrations, authentication, and API routes.
- frontend/: Jinja2 templates, shared page shells, CSS, and JavaScript for the web experience.
- android/: Native Android app with explicit screen navigation and offline-friendly workflow support.
- shared/: Kotlin Multiplatform module for models, API client scaffolding, SQLDelight storage, and sync foundations.
- scripts/: Helper utilities for seeding demo data, checking database state, and scanning the backend.

## Core workflow

FieldCRM supports the full loan workflow:

- Borrower intake and registration
- Loan application creation and intake forms
- Document upload and OCR review
- Guarantor verification and pledge/trust capture
- Visitation reporting and signoff
- Credit officer review and recommendation
- Branch manager approval readiness and decisioning
- Auditor compliance checks and audit trail review
- System administrator user and activity management

## User Roles and Navigation

### Loan Officer

Main screens:
- Dashboard: personal queue, drafts, returned items, visits, and upload shortcuts.
- My Queue: active applications assigned to the officer.
- Applications: list and filter all applications.
- New Application: starts the loan intake wizard.
- Drafts / Returned: resume incomplete or corrected applications.
- Visit Schedule: view and complete visitation tasks.
- OCR Review Queue: view applications in OCR review.

### Credit Officer

Main screens:
- Dashboard: review queue, OCR exception alerts, and loan summaries.
- My Reviews: credit review queue for active applications.
- OCR Exceptions: exceptions from OCR extraction needing validation.
- Borrowers / Current Loans: view borrower and loan details.

### Branch Manager

Main screens:
- Dashboard: approval readiness and branch workflow summaries.
- Awaiting Me: applications needing branch manager approval.
- Visit Signoffs: pending visit signoffs requiring concurrence.
- Pipeline: stage-based application pipeline view.
- Borrowers: current loans and borrower details.

### Auditor

Main screens:
- Dashboard: compliance flags and audit activity.
- Compliance Flags: flagged applications and documents.
- Audit Trail: historical action review.
- Borrowers: loan details and related applications.

### System Admin

Main screens:
- Dashboard: system activity and final control summaries.
- Users: manage staff accounts and roles.
- System Activity: audit and control queue.
- Audit Trail: read-only history for compliance review.

## Web Screen Flow

### Authentication and entry

- `/login`: shared login flow for all users.
- `/dashboard`: role-aware home page after login.
- `/logout`: clears session and returns to login.

### Application workflow

- `/applications`: application list with stage filters.
- `/applications/new`: create a new loan application draft.
- `/applications/{id}`: application details and task hub.
- `/applications/{id}/step/{1-9}`: loan intake wizard for draft applications.
- `/applications/{id}/guarantors/{slot}/step/{1-8}`: guarantor wizard.
- `/applications/{id}/documents/upload`: upload files and trigger OCR.
- `/applications/{id}/ocr-review`: OCR review and correction screens.
- `/applications/{id}/credit-review`: credit review and recommendation.
- `/applications/{id}/approve`: approval readiness for branch managers.
- `/applications/{id}/return`: return application workflow.

### Supporting screens

- `/pipeline`: branch manager pipeline view.
- `/borrowers`: borrower and loan list view.
- `/audit`: auditor workflow page.
- `/audit-trail`: audit history.
- `/compliance-flags`: auditor/system admin flag list.
- `/users`: system admin user management.
- `/system-activity`: system admin activity and control view.

## Android Screen and Navigation Flow

The Android app uses a central `Screen` sealed class state and back stack in `MainActivity.kt`. Navigation is explicit, and each screen generally returns to the previous screen.

### Main mobile flow

- Login → Dashboard
- Dashboard → Borrower List / Application List / Offline Queue / Settings
- Borrower List → Borrower Detail → Create Borrower / Create Application
- Application List → Application Detail → child task screens
- Application Detail → Loan Application Form / Document Upload / Document Viewer / Guarantors / Pledge & Trust / Visitation Report / Review / Audit Trail

### Key mobile screens

- `LoginScreen`: user login with password, passcode, or biometric options.
- `DashboardScreen`: home screen with role-specific shortcuts.
- `BorrowerListScreen`: list registered borrowers.
- `BorrowerDetailScreen`: borrower summary and create application.
- `CreateBorrowerScreen`: add a new borrower.
- `ApplicationListScreen`: list active applications.
- `ApplicationDetailScreen`: application hub with workflows and review actions.
- `CreateApplicationScreen`: create a new draft application.
- `LoanApplicationFormScreen`: process loan intake in wizard steps.
- `DocumentUploadScreen`: upload application documents.
- `DocumentViewerScreen`: view uploaded documents.
- `GuarantorsFormScreen`: add or edit guarantor details.
- `PledgeTrustScreen`: manage pledge/trust requirements.
- `VisitationReportScreen`: capture visit reports.
- `CreditOfficerReviewScreen`: credit review flow.
- `BranchManagerReviewScreen`: branch approval flow.
- `AuditorComplianceScreen`: auditor compliance review.
- `AdminMcrApprovalScreen`: admin final approval flow.
- `WorkflowEventAuditScreen`: audit event history.
- `SettingsScreen`: theme, passcode, and sign-out.
- `OfflineQueueScreen`: local queue and retry management.

## Project Structure

```text
FieldCRM/
|-- backend/
|-- frontend/
|-- android/
|-- shared/
|-- gradle/
|-- build.gradle.kts
|-- settings.gradle.kts
|-- test_imports.py
`-- README.md
```

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

Run migrations and seed demo data:

```powershell
python backend\migrations\run_migration.py
```

Start the backend:

```powershell
uvicorn app.main:app --app-dir backend --reload
```

Open:

- `http://127.0.0.1:8000/login`
- `http://127.0.0.1:8000/api/docs`
- `http://127.0.0.1:8000/api/redoc`
- `http://127.0.0.1:8000/api/v1/health`

## Android Setup

Recommended tools:

- Android Studio
- Android SDK
- JDK 17

Create `local.properties` in the repository root if needed:

```properties
sdk.dir=C\:\\Users\\YOUR_USER\\AppData\\Local\\Android\\Sdk
```

Compile the Android app:

```powershell
.\gradlew.bat :android:compileDebugKotlin
```

Build a debug APK:

```powershell
.\gradlew.bat :android:assembleDebug
```

## Shared Module

The `shared` module provides:

- Borrower and loan application models
- Ktor API client scaffolding
- SQLDelight local storage
- Sync and repository foundations

## Running Checks

```powershell
python test_imports.py
python backend\test_http.py
python backend\test_routes_render.py
```

## Demo Login Accounts

Seeded users all use password `password123`.

| Role | Email |
| --- | --- |
| System Admin | `admin@mmfb.com` |
| Branch Manager | `adebayo@mmfb.com` |
| Loan Officer | `chidi@mmfb.com` |
| Credit Officer | `fatima@mmfb.com` |
| Auditor | `samuel@mmfb.com` |

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
