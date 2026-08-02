# FieldCRM Shared Module

The `:shared` module is a Kotlin library designed to share code between the Android mobile client and other Kotlin targets. It encapsulates the core data transfer objects (DTOs), serialization formats, local database schemas, and data structures used across the client application.

## Core Capabilities

- **Shared Data Models**: Defines serialization-ready models like `BorrowerModel`, `LoanApplicationModel`, and `SyncPayload` using `kotlinx.serialization`.
- **Offline Storage Schema**: Defines the local SQLite table definitions and offline database structure (`AppDatabase.sq`) utilizing **SQLDelight**.
- **Unified Sync Models**: Structures data schemas for local queue items, offline changesets, and syncing transactions between the client and the FastAPI backend.

## Directory Structure

```text
shared/
├── src/main/
│   ├── java/com/fieldcrm/shared/
│   │   └── model/             Kotlin serialization-compatible data models
│   └── sqldelight/com/fieldcrm/shared/db/
│       ├── migrations/        SQLite migration files (.sqm)
│       └── AppDatabase.sq     SQLDelight tables, indices, and queries
├── build.gradle.kts           Shared compilation & serialization dependencies
└── README.md                  Module documentation
```

## Setup and Integration

The module is integrated directly as a Gradle project dependency inside the Android app module:

```kotlin
// android/build.gradle.kts
dependencies {
    implementation(project(":shared"))
}
```

To compile or rebuild the SQLDelight database and shared targets:

```powershell
.\gradlew.bat :shared:assemble
```

## Adding Database Tables & Queries

1. Modify or add query files in `shared/src/main/sqldelight/com/fieldcrm/shared/db/AppDatabase.sq`.
2. To create structural migrations, add numbered migration scripts under `shared/src/main/sqldelight/com/fieldcrm/shared/db/migrations/`.
3. Re-run `./gradlew :shared:generateSqlDelightInterface` to regenerate type-safe Kotlin query definitions.
