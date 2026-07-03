# FieldCRM Android App

The `android` module contains FieldCRM's native Android application for field and back-office lending workflows. It is built with Kotlin, Jetpack Compose, and depends on the shared Kotlin Multiplatform module for models, API access, local storage, and synchronization foundations.

## What This App Does

The Android app supports core mobile workflows for FieldCRM:

- Authentication with password, passcode, and biometric sign-in
- Role-aware dashboards and queue entry points
- Borrower search, detail, and creation
- Loan application list, detail, and draft creation
- Loan intake form workflows
- Document upload and viewing
- Guarantor verification and pledge/trust capture
- Visitation report capture and signoff
- Review screens for credit officers, branch managers, auditors, and admins
- Audit event inspection
- Settings and offline queue management

Some flows are still under development. Certain backend API endpoints and offline sync behaviors are scaffolded or in progress.

## Technology

- Kotlin 1.9.22
- Android Gradle Plugin 8.2.2
- Jetpack Compose + Material 3
- Android ViewModel and lifecycle components
- WorkManager for background sync
- Ktor HTTP client
- SQLDelight local storage
- Shared Kotlin Multiplatform module

## Requirements

- Android Studio
- Android SDK 34
- JDK 17
- Physical device or emulator with API 24+

Current app configuration:

| Setting | Value |
| --- | --- |
| Application ID | `com.fieldcrm.android` |
| Minimum SDK | 24 |
| Target SDK | 34 |
| Compile SDK | 34 |
| Version | `1.0` (`versionCode` 1) |

## Project Structure

```text
android/
|-- build.gradle.kts
`-- src/main/
    |-- AndroidManifest.xml
    `-- java/com/fieldcrm/android/
        |-- MainActivity.kt
        |-- core/
        |   |-- network/
        |   `-- session/
        |-- data/repository/
        |-- sync/
        `-- ui/
            |-- components/
            |-- screens/
            |-- theme/
            `-- viewmodel/
```

## Navigation Model

Navigation is driven by a central `Screen` sealed class in `AppViewModel` and a back stack managed in `MainActivity.kt`. The app does not currently use Jetpack Navigation; instead, each screen transition is explicit and the visible back arrow follows the user-defined back stack.

## Screen Navigation

The app does not currently maintain a Jetpack Navigation back stack. Each
screen receives explicit callbacks, and `MainActivity.kt` changes
`AppViewModel.currentScreen`. The back arrow therefore goes to the fixed
destination documented below rather than automatically returning through
navigation history.

### Main Flow

```text
Login
  -> Dashboard
     -> Borrower List -> Borrower Detail -> Create Application
     |                `-> Create Borrower
     -> Application List -> Application Detail -> workflow task screens
     `-> Offline Queue
```

### Every Screen

| Screen | How to open it | Actions and destination |
| --- | --- | --- |
| Login | Initial app screen or Logout | Successful login saves the session and opens Dashboard. |
| Dashboard | Successful login; back from Borrower List, Application List, Settings, or Offline Queue | Opens Borrower List, Application List, or Offline Queue. Logout clears the session and returns to Login. |
| Borrower List | Dashboard borrower module/action | Select a borrower to open Borrower Detail. Add opens Create Borrower. Back opens Dashboard. |
| Borrower Detail | Select a borrower in Borrower List | Create Application opens Create Application with that borrower preselected. Back clears the selected borrower and opens Borrower List. |
| Create Borrower | Add action in Borrower List | Successful creation opens Borrower List. Back cancels and opens Borrower List. |
| Application List | Dashboard application module/action | Select an application to open Application Detail. Add opens Create Application. Back opens Dashboard. |
| Create Application | Add action in Application List, or Create Application from Borrower Detail | Successful creation opens Application List. Back cancels and opens Application List. |
| Application Detail | Select an application in Application List | Opens Loan Application Form, Document Upload, Document Viewer, Guarantors Form, Pledge & Trust, Visitation Report, role-specific Review, or Workflow Event Audit. Back clears the selected application and opens Application List. |
| Loan Application Form | Form/wizard action in Application Detail | Back or Finish returns to Application Detail. Internal form steps remain inside this screen. |
| Document Upload | Identity/document requirement in Application Detail | Back or Complete returns to Application Detail. |
| Document Viewer | Select a displayed application document | Back returns to Application Detail. |
| Guarantors Form | Guarantor verification requirement in Application Detail | Back or Save returns to Application Detail. |
| Pledge & Trust | Pledge document requirement in Application Detail | Back or signature completion returns to Application Detail. |
| Visitation Report | GPS/visitation requirement in Application Detail | Back or Submit returns to Application Detail. |
| Branch Manager Review | Review action from Application Detail when the session role is Branch Manager | Back or decision submission returns to Application Detail. |
| Credit Officer Review | Review action from Application Detail when the role is Credit Officer; also the current fallback for unrecognized roles | Back or completed review returns to Application Detail. |
| Auditor Compliance | Review action from Application Detail when the role is Auditor | Back or completed audit returns to Application Detail. |
| Admin MCR Approval | Review action from Application Detail when the role is `ADMIN_MCR` | Back or disbursement trigger returns to Application Detail. |
| Workflow Event Audit | Audit-trail action in Application Detail | Back returns to Application Detail. |
| Settings | Settings state or the settings area rendered inside Dashboard | Back opens Dashboard. Its Offline Queue row opens Offline Queue. |
| Offline Queue | Dashboard offline action or Settings | Back opens Dashboard. Retry/remove actions currently update the local mock queue without leaving the screen. |

### Role-Based Review Routing

The Review action on Application Detail chooses a destination from the active
session role:

| Session role | Review screen |
| --- | --- |
| `BRANCH_MANAGER` | Branch Manager Review |
| `CREDIT_OFFICER` | Credit Officer Review |
| `AUDITOR` | Auditor Compliance |
| `ADMIN_MCR` | Admin MCR Approval |
| Any other or missing role | Credit Officer Review fallback |

### Dashboard Navigation

Dashboard content changes with the user role, but its top-level callbacks are
shared:

- Borrower-related cards and modules open Borrower List.
- Application, queue, review, and pipeline cards open Application List.
- The offline/settings area can open Offline Queue.
- The logout icon clears all `AppUiState`, including the session and selected
  borrower/application, then displays Login.

Dashboard also uses internal tab state for its main and settings content. That
tab state is local to `DashboardScreen`; it is separate from the app-level
`Screen.Settings` state.

### Application Detail Navigation

Application Detail is the Android app's central workflow hub:

- The application-form card opens Loan Application Form.
- A displayed document opens Document Viewer.
- Identity verification opens Document Upload.
- Pledge status opens Pledge & Trust.
- GPS visitation status opens Visitation Report.
- Guarantor verification opens Guarantors Form.
- Approve, return, and reject actions all open the role-based Review screen.
- The audit-history action opens Workflow Event Audit.

All these child screens explicitly return to Application Detail. They depend on
`selectedApplication` remaining in `AppUiState`; only leaving Application
Detail for Application List clears that selection.

### Android System Back

There is not yet a centralized handler that maps the Android system Back button
to the same destinations as each screen's top-app-bar arrow. Until navigation
is migrated to Jetpack Navigation or a custom back handler is added, test both
the visible back arrow and the device Back gesture when changing navigation.

## Setup

Run Gradle commands from the repository root because `android` depends on the
sibling `shared` module.

### 1. Configure the Android SDK

Android Studio usually creates `local.properties` automatically. If needed,
create it in the repository root:

```properties
sdk.dir=C\:\\Users\\YOUR_USER\\AppData\\Local\\Android\\Sdk
```

`local.properties` is machine-specific and must not be committed.

### 2. Compile the app

```powershell
.\gradlew.bat :android:compileDebugKotlin
```

### 3. Build a debug APK

```powershell
.\gradlew.bat :android:assembleDebug
```

The APK is generated under:

```text
android/build/outputs/apk/debug/
```

### 4. Run from Android Studio

Open the repository root in Android Studio, allow Gradle synchronization to
finish, select the `android` run configuration, and choose an emulator or
connected device.

## Connect to the Backend

Start the FastAPI backend on the development machine:

```powershell
uvicorn app.main:app --app-dir backend --host 0.0.0.0 --port 8000 --reload
```

The current ViewModels use:

- `http://10.0.2.2:8000` for a standard Android emulator.
- `http://localhost:8000` for non-emulator detection.

`localhost` on a physical device means the device itself, not the development
computer. For a physical device, change the base URL to the computer's LAN IP,
for example `http://192.168.1.20:8000`, and ensure the firewall allows the
connection.

The manifest currently permits internet access. Because development uses plain
HTTP, verify the emulator/device network policy if requests are blocked; a
production build should use HTTPS.

## Shared Module Dependency

The `android` module imports `project(":shared")`. The shared module provides:

- `FieldCRMClient`
- Borrower and loan application models
- SQLDelight `AppDatabase`
- Offline synchronization foundations

Both modules must remain included in the root `settings.gradle.kts`:

```kotlin
include(":shared")
include(":android")
```

## Architecture Notes

- Composables belong under `ui/screens` or `ui/components`.
- Screen state and user actions belong in ViewModels.
- Network and persistence logic belong in repositories or the shared module.
- Long-running synchronization belongs in WorkManager workers.
- Session and role information belongs under `core/session`.

Avoid performing network calls directly from composables.

## Tests and Verification

Compile after making changes:

```powershell
.\gradlew.bat :android:compileDebugKotlin
```

Run unit tests:

```powershell
.\gradlew.bat :android:testDebugUnitTest
```

Run connected instrumentation tests with an emulator or device available:

```powershell
.\gradlew.bat :android:connectedDebugAndroidTest
```

Manually verify affected screens with at least one narrow and one larger device
profile, and test offline/reconnection behavior when changing repositories or
workers.

## Known Development Gaps

- Authentication in `AuthRepository` is currently mocked.
- Some camera and offline queue behavior is placeholder UI.
- API URLs are constructed in multiple ViewModels and the sync worker.
- The mobile API contract is not fully aligned with the mounted backend routes.
- Release minification and production signing are not configured.

Before production release, centralize environment-specific URLs, complete the
API contract, replace mocks, use HTTPS, configure signing, and add automated
coverage for critical lending workflows.

## Troubleshooting

If Gradle uses an unsupported Java version, set `JAVA_HOME` to JDK 17 and
restart Android Studio.

If the SDK is not found, correct `sdk.dir` in the root `local.properties`.

If the emulator cannot reach the backend, confirm that the backend is running
on port 8000 and use `10.0.2.2`, not `127.0.0.1`.

If dependency resolution fails on the first build, check internet access and
retry after Gradle has downloaded the Android, Kotlin, Compose, Ktor, and
SQLDelight dependencies.
