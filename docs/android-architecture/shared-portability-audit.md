# Shared module portability audit

## Finding

`shared/` is currently an Android/JVM library (`com.android.library` plus
`kotlin("android")`), not a Kotlin Multiplatform module. It cannot currently be
linked into an iOS target even though some source code is platform-neutral.

## Portable now

- `model/BorrowerModel.kt`: serializable borrower, application, and sync payload
  value types. These use Kotlin and kotlinx.serialization only.
- `db/BackendSchema.kt`: declarative schema metadata. It has no Android imports.

## Portable after extraction

- `api/FieldCRMClient.kt`: request mapping and JSON configuration can move to
  common code, but construction currently chooses a default `HttpClient`, keeps
  mutable token state inside the client, returns raw `HttpResponse`, and does
  manual JSON mapping. Inject the platform engine and `SessionManager`, and
  implement `FieldCrmApi` before moving it to `commonMain`.
- `repository/SyncRepository.kt`: retry policy and mutation dispatch are portable,
  but the class directly depends on generated `AppDatabase`, HTTP implementation
  details, and JVM `System.currentTimeMillis()`. Depend on `PendingMutationStore`,
  `FieldCrmApi`, and `EpochClock` instead.

## Not portable

- Android SQLDelight driver configuration in `shared/build.gradle.kts`.
- The OkHttp Ktor engine dependency.
- Android Gradle plugin/source layout (`src/main/java`).
- Android secure storage, WorkManager scheduling, CameraX, Credential Manager,
  Activity/ViewModel lifecycle, and Compose Android UI in `android/`.

## Recommended boundary (not implemented)

- A typed API interface should own remote operations and never expose Ktor responses.
- A secure session-store interface should own platform-secure credential persistence (Android
  Keystore or iOS Keychain).
- A session manager should own token validity, refresh, and logout.
- Domain gateways expose application, borrower, workspace, and queued-mutation
  behavior to either Android or iOS presentation code.
- Tokens remain outside presentation state and are supplied only by the session
  layer to the API implementation.

## Next migration step

If separately authorized, convert `shared` to `kotlin("multiplatform")` with `commonMain`, `androidMain`, and
`iosMain`. Move the portable files first without changing Android behavior. Add
an injected Ktor engine per platform, Android/iOS implementations of
`SecureSessionStore`, and SQLDelight drivers in their platform source sets.
