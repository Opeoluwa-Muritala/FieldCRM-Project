# Adaptive UI verification record

This record covers the adaptive UI implementation commits from Phase A through
Phase E. It deliberately distinguishes source-level checks from runtime checks.
No Gradle task, APK build, emulator, or connected-device test was run for this
verification pass.

## Implementation checkpoints

| Phase | Commit | Source-level result |
|---|---|---|
| A — adaptive navigation and theme | `a64c3e4` | `NavigationSuiteScaffold` is driven by `currentWindowAdaptiveInfo`; Navigation 3 remains the only back stack. Light and dark schemes are centrally mapped. |
| B — list/detail and queue states | `e89f4dc` | Navigation 3 list/detail scene metadata is assigned to queue/detail destinations. Queue screens distinguish loading, empty, and error states. |
| C — review supporting panes | `c27bb20` | Evidence-heavy review destinations use supporting-pane scene metadata. Decision impact notices and primary/secondary action hierarchy are present. |
| D — dashboards, dossier, servicing, previews | `e7e7a78` | Dashboard metrics reflow, numeric styles use tabular figures, dossier sections use progressive disclosure, and document preview is edge-to-edge. |
| E — forms and administration | `123057c` | Non-sensitive wizard position and paging state are saveable. Sensitive entered values remain outside saved instance state. Missing OCR readiness resolves to incomplete, never verified. |

## Static security checks

- Navigation scene metadata changes do not introduce another navigation graph or HTTP client.
- Pane selection passes identifiers to the existing detail destination; it does not promote list summaries to authoritative detail records.
- No token, authorization header, borrower identifier, name, password, or other PII was added to logs or `SavedStateHandle`.
- User search and form-entered identity data are intentionally not stored with `rememberSaveable`.
- Skeletons are visually distinct from confirmed records and are replaced by loaded, empty, or error content.
- No backend endpoint, authorization rule, or API response contract was changed by Phases A–E.

## Device/configuration matrix

| Configuration | Static inspection | Runtime verification |
|---|---|---|
| Compact width / smallest phone | Compact destinations retain single-pane Navigation 3 behavior | Pending |
| Medium width | Navigation suite and list/detail scene strategies are wired | Pending |
| Expanded width | Navigation suite and supporting/list-detail scenes are wired | Pending |
| Portrait to landscape rotation | Non-sensitive wizard position and paging use saveable state | Pending |
| Folded / unfolded / live transition | Adaptive info is sourced from the platform adaptive API | Pending |
| Multi-window resize | Layout decisions consume current adaptive window information | Pending |
| Default and large font scale | Scrollable form/detail containers remain in source | Pending |
| Light and dark theme | Both color schemes are centralized in `FieldCRMTheme` | Contrast and visual review pending |
| Edge-to-edge document preview | Activity edge-to-edge and full-screen preview source is present | Pending |
| Session expiry in any pane | Existing API/session path remains unchanged | Pending |

## Required runtime gate

Before release, run a clean compilation and fresh APK installation, then exercise
all rows in the matrix on representative compact, medium, expanded, and foldable
configurations. Verify back-stack behavior, in-flight form handling, 401 handling,
logout/role-switch state clearing, document preview system bars, and WCAG AA color
contrast. Any failure at this gate blocks release even when the corresponding
source-level check above passed.

