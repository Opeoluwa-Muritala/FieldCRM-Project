# Android parity audit — CRM Officer

| Role | Destination | Web reference | Android reference | Web capability | Android capability | Gap | Evidence | Severity |
|---|---|---|---|---|---|---|---|---|
| CRM Officer | Dashboard | `crm/dashboard.html`; `GET /crm-dashboard`/role dashboard | `roles/crm/dashboard/CrmDashboard.kt`; `DashboardViewModel` | Dossier queue plus recent disbursements with borrower/reference/amount/classification/actions. | CRM queue, PAR percentage and ready amount metrics with review/PAR shortcuts. | Recent-disbursement rows and their servicing actions are absent. | **Confirmed** | Medium |
| CRM Officer | Dossier Review Queue | `crm/crm_queue.html`; `GET /crm-review-queue` | `CrmReviewQueue` via `RoleCrmQueueHost`; `CrmQueueScreen.kt` | Applicant, reference, amount, officer, Branch Manager, wait age and review action. | Applicant/amount queue cards sourced from dashboard `crm_queue`. | Officer/manager ownership, wait age and complete reference context are reduced. | **Confirmed** | Medium |
| CRM Officer | CRM Dossier Review | `crm/crm_review.html`; CRM review GET/POST and CRM document upload routes | `CrmDossierReview.kt` → `CrmReviewScreen.kt`; `CrmReviewViewModel` | Document quality/verification, CRM uploads, four consent/declaration gates, approved/recommended amount, executive notes, advance and return. | Loan summary, four checklist booleans, notes, supporting-document upload shortcut, advance and return. | The checklist labels differ from the web consent/declaration contract; approved/recommended amount is absent; document quality/uploader table is reduced. Equivalent dossier review data cannot be captured. | **Confirmed** | High |
| CRM Officer | Disbursement | `crm/disburse.html`; `GET/POST /applications/{id}/disburse`, offer route | No `Screen.Disbursement`; application detail only exposes “Generate offer letter”; `ApplicationViewModel.recordDisbursement` is not wired to a screen | Generate offer, actual amount, date, method, bank reference, interest rate, frequency and schedule method; creates schedule. | Can generate an offer; a repository/ViewModel method exists for disbursement but no form/destination invokes it. | **Missing entirely:** CRM cannot record disbursement on Android. Foundation touch: Navigation 3 route/policy plus role-owned form. | **Confirmed** | High |
| CRM Officer | PAR Dashboard | `shared/par_dashboard.html`; `GET /reports/par` | `ParDashboardScreen.kt`; `ServicingViewModel`; allowed by CRM workspace | Classification breakdown and active portfolio with reference, borrower, disbursed amount, sector, class, days overdue and schedule action. | Breakdown and portfolio lists backed by mobile PAR APIs. | Live layout/detail completeness and error-state behavior require runtime verification; source contains the main web data groups. | **Unverified** | Low |
| CRM Officer | Repayment Schedule / Record Payment | `shared/repayment_schedule.html`, `crm/record_payment.html`; schedule/payment routes | `RepaymentScheduleScreen.kt`; `ServicingViewModel` | Due date, principal, interest, total, status; payment date, amount, channel, bank reference, recorder and history. | Schedule and history plus payment dialog for amount/channel/reference. | Payment date is not captured, recorder detail is reduced, and `Screen.RepaymentSchedule` is not included in the CRM workspace allow-list even though PAR navigates to it. This blocks the action through guarded navigation. Foundation touch: route policy. | **Confirmed** | High |
| CRM Officer | Search | `shared/search_results.html`; `GET /search` | `roles/crm/search/CrmSearch.kt`; `SearchViewModel` | Application/customer/payment-reference lookup. | Shared application-shaped search with CRM wording. | No typed disbursement/payment result row or payment-specific filters. | **Confirmed** | Medium |
| CRM Officer | Settings | `shared/settings.html`; settings routes | Existing screen excluded by workspace policy | Account details and password change. | Not reachable. | **Missing as a reachable destination.** Foundation touch: route policy. | **Confirmed** | High |

## Missing-entirely destinations

- **High — Confirmed:** Disbursement form.
- **High — Confirmed:** reachable Settings/password page.

## Summary

- High: **4**
- Medium: **3**
- Low: **1**

The most consequential gap is the complete absence of the disbursement form despite a mobile API/ViewModel method already existing.
