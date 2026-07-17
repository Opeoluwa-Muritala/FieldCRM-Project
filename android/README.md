# FieldCRM Android

`android/` is the native FieldCRM client for field and credit operations. It is a Kotlin/Jetpack Compose application for Android 8.0+ (minSdk 26) that calls the FastAPI mobile API. The backend remains authoritative for authentication, permissions, approval decisions, workflow transitions, audit history, and server-side document/OCR processing.

## Features

- Credential login with a mobile JWT, session restoration, biometric gate, passcode/password recovery, session-expiry handling, onboarding, and permission education.
- Role-aware dashboard metrics, quick actions, work queues, notifications, settings/help, application search, and adaptive phone/tablet navigation.
- Borrower list, borrower detail, and borrower creation.
- Loan application creation, application list/detail, guided intake, guarantors, pledge/trust details, repayment schedule, PAR dashboard, and visitation reports.
- Document upload, preview/viewing, CameraX capture, on-device ML Kit text scanning, server document upload, OCR-field review/correction, and OCR-exception queues.
- Branch, credit, CRM, committee, auditor, executive, ED, and MD review screens, with readiness/checklists, workflow decisions, and audit/event history.
- Encrypted session/document storage, background WorkManager sync/notification work, connection-aware errors, and sync-status UI.
- System-administration screens for users and system activity.

## Screen catalogue

| Family | Screens |
| --- | --- |
| Authentication/onboarding | Login, forgot/reset password, passcode, session expired, biometric enrollment, permissions primer, onboarding, and passkey-unavailable guidance. |
| Dashboard | Role-aware dashboard, notifications, settings, and search results. |
| Borrowers | Borrower list, detail, and creation. |
| Applications | List/detail/create, loan application form, guarantors, pledge/trust, visitation report, OCR review, offline queue, repayment schedule, and PAR dashboard. |
| Documents | Upload/camera scan, preview, viewer, and OCR review. |
| Queues | My queue, branch concurrence, credit/CRM/committee/ED/MD/executive queues, OCR exceptions, pending sign-offs, visits due, and pipeline. |
| Review/audit | Branch manager, credit officer, CRM, committee, auditor compliance, administrator MCR, executive, ED, MD, workflow-event audit, audit trail, and compliance flags. |
| Administration/common | Users, system activity, confirmation, detail fields, and full-screen error. |

Navigation uses Navigation 3 in `ui/navigation/` and adapts primary navigation for compact and larger windows. Preserve its existing single back stack; do not add a second navigation library.

## Architecture

```text
android/
├── src/main/java/com/fieldcrm/android/
│   ├── core/              biometric, encrypted storage, session, notifications, networking
│   ├── data/              Ktor API client, DTOs, and repositories
│   ├── di/                Koin dependency configuration
│   ├── sync/              WorkManager sync worker
│   └── ui/                Compose components, navigation, screens, theme, ViewModels
├── src/main/AndroidManifest.xml
└── build.gradle.kts
```

Compose screens receive state and callbacks from MVI-style ViewModels. Koin builds the API/repository/ViewModel graph. The application uses Ktor with Kotlin serialization, SQLDelight through the shared module, WorkManager, CameraX/ML Kit, AndroidX Biometric, and encrypted storage.

## Requirements and app configuration

- A compatible JDK, Android SDK Platform 36 (`compileSdk 36`), and an emulator/device running API 26 or later.
- The root Gradle wrapper: the module depends on `:shared` and is not a standalone Gradle project.

| Setting | Value |
| --- | --- |
| Application ID | `com.fieldcrm.android` |
| min / target / compile SDK | 26 / 35 / 36 |
| Version | `1.0` (`versionCode` 1) |
| Permissions | Internet, camera, biometric, network state, and Android 13+ notifications. |

Dependencies are versioned centrally in [`../gradle/libs.versions.toml`](../gradle/libs.versions.toml). Android-specific dependencies are declared in `android/build.gradle.kts`; the local shared module is declared as `implementation(project(":shared"))`.

## Configure the API host

The API host is currently hard-coded, not exposed as a build setting:

- `di/AppModule.kt` creates `FieldCRMClient` and `MobileApiServiceImpl` for `https://fieldcrm.onrender.com`.
- `sync/AndroidSyncWorker.kt` creates a client for the same host.
- `data/api/MobileApiService.kt` has the same default host.

For a local Android emulator, update **all three locations** to `http://10.0.2.2:8000`; `localhost` inside an emulator points to the emulator itself. For a physical device, use an HTTPS-reachable development host (or a reachable LAN address only where your network-security policy permits cleartext). The client calls `/api/v1/auth/login-mobile` and `/api/v1/mobile/...`; start/configure the backend before signing in.

The app receives a mobile token from the backend and sends it with protected calls. Never place backend credentials, provider secrets, or database access in the APK.

## Build, install, and test

From the repository root:

```powershell
.\gradlew.bat :android:compileDebugKotlin
.\gradlew.bat :android:assembleDebug
.\gradlew.bat :android:testDebugUnitTest
```

The debug APK is written to `android/build/outputs/apk/debug/android-debug.apk`. To install on a connected device/emulator:

```powershell
.\gradlew.bat :android:installDebug
```

Run instrumentation tests, when available, with:

```powershell
.\gradlew.bat :android:connectedDebugAndroidTest
```

## Backend contract and boundaries

The mobile API provides session/user data, dashboards, queues, borrowers, applications and intake steps, documents, OCR fields/review, visits/sign-off, review actions, notifications, search, configuration, audit, servicing, PAR, and user administration.

The client can show locally cached data and sync status, but it must not label a local change as synced or approved until the backend confirms it. Camera scanning may use local ML Kit text recognition for staff assistance. The backend independently stores documents, runs its OCR pipeline, returns persisted confidence values, and records verified/corrected fields. UI approval/return actions request backend workflow changes; they do not grant permissions or make decisions by themselves.

## Development conventions

- Keep feature screens in their folder, reusable UI in `ui/components/`, state/actions in ViewModels, and navigation keys in `ui/navigation/`.
- Use `FieldTheme` and `FieldIcons`; provide meaningful content descriptions and 48dp-or-larger touch targets.
- Keep network/persistence work outside composables. Expose loading, failure, offline, conflict, and retry states.
- Do not add `androidx.navigation:navigation-compose`; this project uses Navigation 3.
- Check compact, dark, and expanded/tablet layouts after screen changes.
