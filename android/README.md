# FieldCRM Android

The Android module is the native, offline-capable FieldCRM client for Mainstreet Microfinance Bank loan operations. It is a Kotlin and Jetpack Compose application for Android 8.0+ (`minSdk 26`). The module is a client of the existing backend workflow: it does not replace loan rules, authorization, OCR processing, Room persistence, Ktor networking, or synchronization policy.

## What this module is responsible for

- Native Compose screens, adaptive layouts, theming, navigation, and accessible interaction.
- Local screen state and user intents through the existing MVI-style ViewModels.
- Local biometric device gate, with password fallback messaging.
- On-device document capture, PDF assembly, upload, and an OCR review interface.
- Rendering local/offline and server synchronization status.
- Calling the established Ktor-backed `MobileApiService` and repositories.

## What this module is not responsible for

- Authorizing a user or a role. The backend remains the authority.
- Deciding loan eligibility, approval thresholds, or advancing a workflow without a backend action.
- Replacing Room, SQLDelight, Koin, Ktor, the OCR engine, or biometric security implementation.
- Performing WebAuthn/passkey verification. That requires backend relying-party endpoints and Digital Asset Links.
- Displaying borrower data outside the authenticated app, including in notifications or widgets.

## Build and run

From the repository root:

```powershell
.\gradlew.bat :android:compileDebugKotlin
.\gradlew.bat :android:assembleDebug
```

All external Android dependencies are declared in [`../gradle/libs.versions.toml`](../gradle/libs.versions.toml). `android/build.gradle.kts` uses the generated `libs.*` accessors. The only non-catalog dependency is the local shared module:

```kotlin
implementation(project(":shared"))
```

## Module map

| Location | Purpose |
| --- | --- |
| `src/main/java/com/fieldcrm/android/ui/navigation/` | Nav3 app entry point, screen destinations, and back-stack handling. |
| `ui/screens/` | Feature screens grouped by dashboard, application, document, queue, review, admin, audit, auth, onboarding, and borrower flows. |
| `ui/components/` | Reusable Compose building blocks: app bars, buttons, cards, navigation, sync status, capture UI, and approval sheets. |
| `ui/theme/` | Brand tokens, dark/light themes, typography, shapes, and the existing custom `FieldIcons`. |
| `ui/viewmodel/` | Existing screen state/actions and UI orchestration. |
| `data/api/` | Ktor `MobileApiService`, transport DTOs, and compatibility-safe API calls. |
| `data/repository/` | Local/cache and remote repository coordination. |
| `core/session/` | Session and role representation, including the canonical-to-legacy UI bridge. |

## Navigation

Navigation uses Navigation 3 only. The app retains the existing Nav3 back stack and decorators for saved state and ViewModel scope. Do not add `androidx.navigation:navigation-compose` or another navigation library.

Primary navigation adapts by window size:

- **Compact phones:** branded bottom navigation and thumb-reachable primary actions.
- **Medium/expanded windows:** navigation rail/shell treatment and wider content lanes; dashboards use the Compose Material 3 window-size class rather than a hand-built width breakpoint.

The current navigation structure intentionally preserves existing destinations and deep workflow links. A redesign must not create a parallel back stack.

## Shared design system

The Android design system follows the root [`../DESIGN.md`](../DESIGN.md) reference. The visual language is Shield Purple, neutral financial-document surfaces, deliberate hierarchy, and the app’s custom vector icon set.

### Core components

| Component | Does | Does not do |
| --- | --- | --- |
| `PrimaryButton` | Presents the one highest-priority action for a screen or decision. | Infer permissions or submit data itself. |
| `SecondaryButton` | Provides non-destructive alternatives such as retry, cancel, upload, or return. | Replace a primary action merely for visual variety. |
| `FieldCard` / `SectionCard` | Groups a loan summary, evidence set, review checklist, or status information. | Imply all content has equal priority. |
| `FieldTopAppBar` | Supplies consistent title, back navigation, and optional actions. | Own navigation state. |
| `FieldBottomNavigation` / rail | Provides app-level navigation using the current Nav3 destination model. | Use Material stock icons in place of `FieldIcons`. |
| `SyncStatusBar` | Shows persistent saved/syncing/failed state and exposes retry where supplied. | Claim server success for locally saved work. |
| `CameraOcrScanner` | Guides framing, scan capture, document page handling, retake, and local OCR hand-off. | Invent real-time glare/blur confidence when the OCR engine does not provide it. |
| `OcrFieldsCard` | Lets staff review and manually correct extracted identity fields. Flags per-field server confidence when available. | Treat a local text parse as a verified financial record. |
| `ReviewDecisionSheet` | Confirms consequential approve, forward, or return decisions with a clear outcome. | Alter approval logic, permissions, or backend workflow. |

Every custom icon must have a meaningful `contentDescription` where it is actionable or communicates state. Status is always text plus color. Touch targets must remain at least 48dp.

## Authentication and security screens

### Login and biometric gate

The login experience is the authenticated app boundary. It supports the existing local biometric gate and password fallback.

- **Does:** explain unavailable hardware, unenrolled biometrics, cancellation, lockout, and keystore invalidation recovery as a stable unauthenticated state.
- **Does not:** bypass Android `BiometricPrompt`, store secrets in Compose state, or present the device as authenticated when the prompt is cancelled.

### New-device passkey entry and passkey settings

The UI includes a clearly labelled **Create a passkey** affordance and a manage-passkeys settings entry.

- **Does:** explain that passkeys are for new-device sign-in and retain the existing daily biometric gate.
- **Does not yet do:** create, list, delete, register, or authenticate a real passkey. Credential Manager integration must wait for a confirmed backend WebAuthn contract, relying-party verification endpoints, and hosted `assetlinks.json`. API 26–27 devices must remain on the biometric/password fallback path.

## Dashboard

`DashboardScreen` is the authenticated starting surface. It displays role-aware metrics, short actions, queue items, recent activity, and persistent sync state.

### Dashboard behavior

- Displays server metrics when available without pretending local drafts are server-synced.
- Shows individual record state in relevant lists and a global sync summary with a retry action.
- Uses tablet layout when the Compose window size class is not compact.
- Keeps navigation callbacks outside deeply nested UI components.

### Dashboard limitations

- The dashboard does not determine role permissions; it reflects the current session/backend role.
- It does not silently overwrite a conflict. A backend conflict must surface a dedicated resolution flow.
- It does not use dynamic color; dark mode is a tonal version of the same brand.

## Roles and workflow ownership

The canonical server workflow is:

```text
Account Officer → Branch Manager → Branch Supervisor → Credit Analyst
→ CRM Officer → Head CRM → Auditor → Executive Director
→ Managing Director → CRM disbursement
```

The Android app keeps some legacy screen names while the backend/API moves to canonical routing. `UserRole.legacyUiRole` is a temporary presentation bridge:

| Canonical role | Current UI bridge | Primary responsibility |
| --- | --- | --- |
| Account Officer | Loan Officer | Capture borrowers, complete intake, field visits, documents, OCR review. |
| Branch Manager | Branch Manager | Review branch-level dossiers and submit the branch decision. |
| Branch Supervisor | Branch Manager presentation | Supervisor-stage review until its dedicated screen is separated. |
| Credit Analyst | Committee presentation | Analyst-stage review until its dedicated screen is separated. |
| CRM Officer | CRM | Credit-file completeness, compliance evidence, disbursement readiness. |
| Head CRM | Executive presentation | Head-CRM-stage review until its dedicated screen is separated. |
| Auditor | Auditor | Consent, signature, exhibit, and audit evidence checks. |
| Executive Director | ED | Executive approval or forward to MD. |
| Managing Director | MD | Final senior approval or board referral. |
| System Admin | System Admin | User administration, system activity, and configuration surfaces. |

### Role matrix

| Role | Main Android surfaces | Can do | Must not do |
| --- | --- | --- | --- |
| Account Officer | Dashboard, borrowers, intake wizard, visits, documents, OCR, offline queue | Create and update assigned dossiers; capture documents; correct OCR values; see local sync status. | Approve a loan or present locally saved work as server-approved. |
| Branch Manager | Dashboard, queues, branch review, visitation concurrence | Review assigned branch work and return/advance using existing actions. | Replace CRM, audit, or executive decision-making. |
| Branch Supervisor | Dashboard/branch-review bridge | Review supervisor-stage records via the existing compatible review surface. | Gain a new backend permission solely from the UI bridge. |
| Credit Analyst | Dashboard/review bridge | Review analyst-stage files through existing compatible review surface. | Vote/approve as a separate committee role unless backend authorizes it. |
| CRM Officer | CRM queue, CRM review, document viewer, disbursement surfaces | Verify file completeness, add notes/documents, return for correction, advance approved work. | Bypass required review stages. |
| Head CRM | Executive-review bridge | Inspect and action Head CRM work through compatible routing. | Gain direct ED/MD authority. |
| Auditor | Auditor verification, audit trail, compliance flags | Record checklist evidence and audit observations. | Change borrower financial data without an existing authorized workflow. |
| Executive Director | ED queue and ED approval | Approve within existing backend authority or forward to MD. | Issue a result without backend confirmation. |
| Managing Director | MD queue and MD approval | Give final decision and use existing board-referral action. | Override history silently. |
| System Admin | Users, system activity, configuration/audit surfaces | Manage users through existing admin endpoint and review system state. | Access or mutate records outside backend authorization. |

## Document and OCR flow

1. Staff selects a supported file or opens the camera scanner.
2. The capture surface gives framing and retake guidance. It does not claim live glare/blur detection unless provided by the capture engine.
3. The document is uploaded through the existing document service and Ktor client.
4. The backend processes supported documents asynchronously and persists OCR fields with confidence.
5. Android requests optional field-level OCR data from `GET /api/v1/mobile/applications/{id}/ocr-fields`.
6. The review screen populates empty identity values when data is ready and marks low-confidence or unverified critical values for checking.
7. Staff can manually correct values and submit the existing OCR review action.

### OCR compatibility

The OCR-fields endpoint is additive. An older online backend can return `404`; `MobileApiService` treats that as unavailable data and the manual review UI remains functional. No existing mobile endpoint or response contract is changed.

## Offline and synchronization behavior

The app is offline-first through the existing Room/SQLDelight cache and sync layer.

- **Saved locally:** display as saved on device/pending sync.
- **Syncing:** display a persistent syncing state.
- **Synced:** display only when the established sync layer confirms server completion.
- **Failed:** display a human-readable failure and one retry action.
- **Conflict:** must be surfaced as a separate resolution UI when supplied by the backend; never silently overwrite.

The UI does not implement sync policy. It renders `SyncUiState` and invokes the existing sync action supplied by the parent.

## Review and approval screens

| Screen area | Purpose | Boundary |
| --- | --- | --- |
| Branch review | Dossier/visit review and branch decision. | Uses existing ViewModel/repository action contracts. |
| CRM review | CBN-oriented credit-file checklist, notes, supporting document action, return or advance. | Checklist UI does not itself grant approval. |
| Audit verification | Consent, signature, and exhibit checklist with auditor sign-off. | Does not alter unrelated borrower data. |
| ED approval | Shows authority context, approve/issue instruction or forward to MD. | Final workflow transition remains backend-owned. |
| MD approval | Final senior decision and board referral surfaces. | Does not bypass audit trail or backend authorization. |
| Committee/executive legacy screens | Compatibility screens kept while canonical API routing is being completed. | Do not treat legacy labels as a different backend role. |

## Queues and pipeline

Queue screens are lists backed by current application data. They use `LazyColumn`, do not create a desktop table on a phone, and preserve the existing callback that opens the selected dossier.

The Pipeline screen groups canonical stages as **Intake**, **OCR Review**, **Review**, **Approval**, and **Disbursed**. It does not invent a workflow state; it maps backend stage values for scanning.

## Adding or changing a screen

1. Check `DESIGN.md`, existing screen folder conventions, and `FieldIcons` first.
2. Preserve the existing screen State/Action/ViewModel contract. Stop and ask before a visual change requires a new business contract.
3. Create one component/state/action class per file in the feature folder; put reusable UI in `ui/components/`.
4. Keep leaf composables stateless: receive state and callbacks rather than obtaining a ViewModel deep in the tree.
5. Use `LazyColumn`/`LazyVerticalGrid` for repeated content.
6. Add compact, dark, and expanded/tablet `@Preview` functions for redesigned screens/components.
7. Use string resources for new production copy and provide meaningful icon descriptions.
8. Use purpose-driven Compose motion only for transition, progress, sync state, or confirmation.

## Backend/API additions made for the OCR review UI

The Android client can consume the optional response:

```json
{
  "items": [
    {
      "field_name": "bvn",
      "ocr_value": "22345678927",
      "final_value": "22345678927",
      "confidence": 95.0,
      "is_critical": true,
      "verified": false,
      "ocr_status": "done"
    }
  ],
  "processing": false
}
```

This is intentionally optional so the Android app continues to work against older deployed backends.

## Development seed data

`../backend/migrations/014_seed_review_stage_dossiers.sql` seeds one complete, clearly labelled development dossier for each canonical workflow stage. It includes forms, verified documents, image/signature records, guarantors, signed field visits, stage data, and workflow events. It is re-runnable through conflict guards.

Do not run development seeds against a production tenant without explicit approval.

## Current follow-up work

- Replace canonical-to-legacy UI bridges with dedicated Branch Supervisor, Credit Analyst, and Head CRM surfaces after backend/API routing is fully confirmed.
- Implement real Credential Manager passkeys only after the WebAuthn backend contract and Digital Asset Links are available.
- Complete preview, string-resource, and accessibility cleanup across older pre-redesign screens.
- Run Android compile/build checks after dependency or API-interface changes.
