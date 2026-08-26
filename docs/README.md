# FieldCRM Project Documentation Hub

Welcome to the FieldCRM documentation repository. This directory contains detailed system architecture maps, workflows, and developer manuals for the FieldCRM platform.

## Documentation Index

- [Web UI Functional Inventory](web-ui-role-inventory/README.md): Role-by-role screens, visible information, fields, and actions.
- [Web UI Design System](web-ui-role-inventory/design-system.md): Exact color assignments, semantic actions, element treatments, spacing, responsiveness, and accessibility.
- [Web UI Arrangement Sketches](web-ui-role-inventory/layout-sketches.md): Reusable desktop/tablet/mobile wireframes mapped to every web template.
- [Web UI Source Catalog](web-ui-role-inventory/source-catalog.md): Complete coverage of all 124 Jinja templates and every web CSS/JavaScript asset.
- [Architecture Overview](file:///C:/Users/LENOVO/Desktop/FieldCRM/docs/system-architecture/architecture-overview.md): High-level system design, data boundaries, and modular structure.
- [Current Component Map](file:///C:/Users/LENOVO/Desktop/FieldCRM/docs/system-architecture/current-component-map.md): Graph representation of systems interactions.
- [Deployment Map](file:///C:/Users/LENOVO/Desktop/FieldCRM/docs/system-architecture/deployment-map.md): Infrastructure hosting setup, TLS termination, and server specifications.
- [Frontend/Backend Contract Map](file:///C:/Users/LENOVO/Desktop/FieldCRM/docs/system-architecture/frontend-backend-contract-map.md): API routes, parameters, authentication flow, and data payload specifications.
- [Route Mapping Guide](file:///C:/Users/LENOVO/Desktop/FieldCRM/docs/system-architecture/route-map.md): Detailed web pages and REST endpoints hierarchy.
- [Verification Checklist](file:///C:/Users/LENOVO/Desktop/FieldCRM/docs/system-architecture/verification-checklist.md): Core workflow verification rules for developers.
- [Electronic Signing Structure](file:///C:/Users/LENOVO/Desktop/FieldCRM/docs/current-electronic-signing-structure.md): Legal framework, digital signature algorithms, and verification gates.

---

## Android Client: Architecture & Developer Operations

The Android client is a native Kotlin application built using Jetpack Compose, structured inside the [android/](file:///C:/Users/LENOVO/Desktop/FieldCRM/android) directory.

### 1. Developer Setup & Prerequisites
- **JDK**: Java Development Kit 17 or higher.
- **Android SDK**: `compileSdk 36`, `targetSdk 35`, and `minSdk 26` (Android 8.0+).
- **Gradle wrapper**: Compiles through the root project wrapper to resolve the `:shared` Multiplatform dependency.

### 2. Architecture Patterns
- **MVI (Model-View-Intent) ViewModels**: Screen states are collected reactively from ViewModels via Kotlin StateFlows.
- **Dependency Injection**: Powered by **Koin** for compile-safe constructor parameter mapping.
- **Local SQLite Cache**: Type-safe queries generated from the `:shared` module using **SQLDelight**.
- **Background Synchronization**: Managed via Android **WorkManager** to enable robust offline sync capabilities even when the app is suspended.
- **Camera OCR Processing**: Captures images using **CameraX** and runs local OCR detection via Google's **ML Kit Text Recognition**.

### 3. Developer Commands
All tasks must be run from the repository root:

*   **Compile Kotlin Sources**:
    ```powershell
    .\gradlew.bat :android:compileDebugKotlin
    ```
*   **Build Debug APK**:
    ```powershell
    .\gradlew.bat :android:assembleDebug
    ```
*   **Install App on Emulator/Device**:
    ```powershell
    .\gradlew.bat :android:installDebug
    ```
*   **Run Unit Tests**:
    ```powershell
    .\gradlew.bat :android:testDebugUnitTest
    ```

For detailed mobile client information, check the [android/README.md](file:///C:/Users/LENOVO/Desktop/FieldCRM/android/README.md).
