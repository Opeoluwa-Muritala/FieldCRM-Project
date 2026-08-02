# Android parity audit — Branch Manager

Android presents this role as “Team Lead”; this report compares the underlying `branch_manager` role requested by the audit.

| Role | Destination | Web reference | Android reference | Web capability | Android capability | Gap | Evidence | Severity |
|---|---|---|---|---|---|---|---|---|
| Branch Manager | Dashboard | `branch_manager/dashboard.html`; `GET /dashboard` | `roles/teamlead/dashboard/TeamLeadDashboard.kt`; `DashboardViewModel` | Priority reviews with reference/product/amount/wait age and a branch-pipeline preview. | Awaiting-concurrence, pending-signoff and returned-this-week metrics plus destination links. | Priority row detail, wait age and branch-pipeline preview are absent. | **Confirmed** | Medium |
| Branch Manager | Awaiting Concurrence | `branch_manager/awaiting_concurrence.html`; `GET /awaiting-me` | `TeamLeadConcurrenceQueue` → `AwaitingConcurrenceScreen.kt` | Applicant, officer, amount, visit state and waiting age. | Dashboard queue cards open the dossier/review flow. | Waiting age and complete visit state are reduced, and network failure is not separated from an empty dashboard queue. | **Confirmed** | Medium |
| Branch Manager | Pending Visit Signoffs | `branch_manager/pending_signoffs.html`; `GET /pending-signoffs` | `TeamLeadSignoffQueue` → `PendingSignoffsScreen.kt`; `VisitationReportScreen.kt` | Applicant, visiting officer, met-with, status and updated time; opens report for signoff. | Lists signoffs and opens the field-verification report. | The mobile row omits some web audit context; whether the loaded report always exposes the server signoff capability is runtime-dependent. | **Unverified** | Medium |
| Branch Manager | Assigned Pipeline | `branch_manager/pipeline.html`; `GET /pipeline` | `TeamLeadPipeline` → `PipelineScreen.kt` | Assigned branch files grouped/presented by workflow status. | Locally maps all loaded applications into pipeline entries and stage labels. | Membership is derived from the general application list rather than a dedicated server eligibility response, risking out-of-scope or incomplete membership. | **Confirmed** | Medium |
| Branch Manager | Application Detail | `branch_manager/application_detail.html`; `GET /applications/{id}` | `ApplicationDetailScreen.kt`; `ApplicationViewModel` | Timeline, documents, screening, structural checklist, credit recommendation and verification audit groups. | Shared dossier tabs for identity, collateral, visit, documents and audit. | Structural checklist, grouped verification evidence and credit-risk recommendation are less explicit than web. | **Confirmed** | Medium |
| Branch Manager | Completeness and Concurrence Review | `branch_manager/application_detail.html`; approval/return routes | `roles/teamlead/review/TeamLeadConcurrenceReview.kt` → `BranchManagerReviewScreen.kt` | Server readiness, structural checklist, attestations, approve/return with reasons, correct transition. | Dossier summary, KYC/collateral attestations, comment, approve-to-supervisor and return action. | Review reasons include a hardcoded fallback list, and the evidence set is substantially thinner than the web structural/verification groups. | **Confirmed** | Medium |
| Branch Manager | Search | `shared/search_results.html`; `GET /search` | `roles/teamlead/search/TeamLeadSearch.kt`; `SearchViewModel` | Search team applications/officers within authorized scope. | Role-labelled shared result page backed by the general mobile search endpoint. | No distinct relationship-officer result row or confirmed officer filter/sort UI. | **Confirmed** | Medium |
| Branch Manager | Settings | `shared/settings.html`; settings routes | `SettingsScreen.kt` exists; route excluded by `WorkspaceRegistry.kt` | Account details and password change. | Not reachable under the role route policy. | **Missing as a reachable destination.** Foundation touch: route policy. | **Confirmed** | High |

## Missing-entirely destinations

- **High — Confirmed:** reachable Settings/password page.

## Summary

- High: **1**
- Medium: **7**
- Low: **0**

The most consequential operational degradation is that Assigned Pipeline membership is assembled from a general mobile application collection instead of the web’s server-scoped branch pipeline.
