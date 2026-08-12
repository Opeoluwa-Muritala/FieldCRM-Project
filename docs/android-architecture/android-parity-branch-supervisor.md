# Android parity audit — Branch Supervisor

| Role | Destination | Web reference | Android reference | Web capability | Android capability | Gap | Evidence | Severity |
|---|---|---|---|---|---|---|---|---|
| Branch Supervisor | Dashboard | `branch_supervisor/dashboard.html`; `GET /dashboard` | `roles/supervisor/dashboard/SupervisorDashboard.kt`; `DashboardViewModel` | Post-manager files with applicant, reference, product and amount, linking to review. | Supervisory-review and approved-today metrics with queue shortcut. | Actionable file preview and per-file context are absent. | **Confirmed** | Medium |
| Branch Supervisor | Supervisory Review Queue | `branch_supervisor/review_queue.html`; `GET /supervisory-review-queue` | `SupervisorReviewQueue` through `RoleReviewQueueHost`; `CreditReviewQueueScreen.kt` | Files awaiting review after Team Lead clearance, with applicant, reference, product, amount and explicit supervisory status, linking to approval readiness. | Role-selected dashboard queue opening application detail. | Row detail is reduced; cards on web link to approval readiness review workspace, whereas Android links to shared application detail; server response authorization is present but offline/error/empty states are not fully distinguished. | **Confirmed** | Medium |
| Branch Supervisor | Application Detail | shared detail route/template selected by role; `GET /applications/{id}` | `ApplicationDetailScreen.kt`; application-detail endpoint | Full dossier evidence relevant to supervisory decision. | Shared dossier sections and audit history. | No supervisor-specific composition of structural readiness and prior Team Lead decision; generic dossier content is reused. | **Confirmed** | Medium |
| Branch Supervisor | Supervisor Review | approval/return routes and supervisor detail context | `roles/supervisor/review/SupervisorReview.kt` → `BranchManagerReviewScreen.kt` | Review after Team Lead, verify readiness, approve to credit or return with recorded reason. | Fixed supervisor role invokes shared attestations, comment, approve-to-credit and return. | Team Lead and Supervisor share the same evidence/control layout; prior concurrence evidence and supervisor-specific gates are not separately surfaced. | **Confirmed** | Medium |
| Branch Supervisor | Search | `shared/search_results.html`; `GET /search` | `roles/supervisor/search/SupervisorSearch.kt`; `SearchViewModel` | Search supervised applications in authorized organization/branch scope. | Role-labelled shared application search. | No supervisor-specific branch/team filter or scope indicator in the UI. | **Confirmed** | Medium |
| Branch Supervisor | Settings | `shared/settings.html`; settings routes | Existing `SettingsScreen.kt`, excluded by workspace policy | Account details and password change. | Not reachable for this workspace. | **Missing as a reachable destination.** Foundation touch: route policy. | **Confirmed** | High |

## Missing-entirely destinations

- **High — Confirmed:** reachable Settings/password page.

## Summary

- High: **1**
- Medium: **5**
- Low: **0**

The most consequential workflow gap is that the Supervisor review reuses the Team Lead evidence layout and does not expose the prior concurrence and supervisor-specific gates as a distinct review contract.
