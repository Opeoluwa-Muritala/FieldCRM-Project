# Android parity audit — Executive Director

| Role | Destination | Web reference | Android reference | Web capability | Android capability | Gap | Evidence | Severity |
|---|---|---|---|---|---|---|---|---|
| Executive Director | Dashboard | `executive/ed_dashboard.html`; `GET /ed-dashboard` | `roles/executivedirector/dashboard/ExecutiveDirectorDashboard.kt`; `DashboardViewModel` | Loans awaiting ED approval with below-₦10M context, reference, stage and amount. | ED queue count and PAR metric with queue/MCC links. | Per-file decision preview and threshold explanation are absent. | **Confirmed** | Medium |
| Executive Director | ED Approval Queue | `executive/ed_queue.html`; `GET /ed-queue` | `ExecutiveDirectorDecisionQueue` → `EdQueueScreen.kt` | Applicant, reference, amount, waiting age and open action, with eligibility threshold. | Eligible dashboard queue mapped back to loaded applications and shown as approval cards. | Waiting age is absent; items missing from the locally loaded application list disappear even if returned by the queue API. | **Confirmed** | Medium |
| Executive Director | ED Decision | `executive/ed_approve.html`; ED approval GET/POST | `ExecutiveDirectorDecisionReview.kt` → `EdApprovalScreen.kt`; `CrmReviewViewModel` | MCC recommendations, document summary, editable approved/recommended amount, approve, and optional request for MD input. | Loan summary, CRM notes, approve and forward-to-MD actions. | MCC evidence, document summary and editable amount are absent. The role cannot make the same amount-bearing decision record as web. | **Confirmed** | High |
| Executive Director | MCC Dossiers / Recommendation | `executive/mcc_index.html`, `mcc_summary.html`; MCC routes | `MccWorkspaceScreen` in `SpecialistScreens.kt`; direct mobile API calls | Dossier list, existing member recommendations, submit recommendation amount/notes and authorized final amount. | Dossier list/detail, votes, amount/notes, vote and finalize controls. | The source largely matches; authorization nuances and final-amount eligibility require live role testing. | **Unverified** | Low |
| Executive Director | Dossier / PAR | authorized application detail, `/reports/par` | `ApplicationDetailScreen.kt`, `ParDashboardScreen.kt` | Read dossier, credit/CRM evidence, documents, decisions, portfolio and history. | Shared dossier and PAR pages. | ED-specific recommendation/approval-condition composition is not distinct; repayment drill-down is not allow-listed. | **Confirmed** | Medium |
| Executive Director | Decision Search | `shared/search_results.html`; `GET /search` | `roles/executivedirector/search/ExecutiveDirectorSearch.kt`; `SearchViewModel` | Decision-eligible and permitted historical dossiers. | Shared application results with ED wording. | No typed historical-decision result, decision status filter or amount threshold filter. | **Confirmed** | Medium |
| Executive Director | Settings | `shared/settings.html`; settings routes | Existing screen excluded by workspace policy | Account details and password change. | Not reachable. | **Missing as a reachable destination.** Foundation touch: route policy. | **Confirmed** | High |

## Missing-entirely destinations

- **High — Confirmed:** reachable Settings/password page.

## Summary

- High: **2**
- Medium: **4**
- Low: **1**

The most consequential gap is the ED decision page’s lack of MCC/document evidence and an editable approved amount.
