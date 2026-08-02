# Android parity audit — Head CRM

| Role | Destination | Web reference | Android reference | Web capability | Android capability | Gap | Evidence | Severity |
|---|---|---|---|---|---|---|---|---|
| Head CRM | Dashboard | `crm/dashboard.html`; role dashboard route | `roles/headcrm/dashboard/HeadCrmDashboard.kt`; `DashboardViewModel` | Head CRM-labelled dossier queue and recent disbursements. | CRM queue, unverified-document and workflow-exception metrics with queue/PAR links. | Recent disbursement list and actionable oversight detail are absent. | **Confirmed** | Medium |
| Head CRM | Oversight Queue | `crm/crm_queue.html`; `GET /crm-review-queue` | `HeadCrmOversightQueue` via `RoleCrmQueueHost`; shared `CrmQueueScreen.kt` | Applicant, reference, amount, officer, Branch Manager, wait age and review action for Head CRM-eligible files. | Same generic CRM queue layout, fed from dashboard queue data. | Head CRM ownership/stage context is not distinct in the row; officer/manager/wait data is reduced. | **Confirmed** | Medium |
| Head CRM | Head CRM Approval | `crm/crm_review.html`; CRM review GET/POST | `HeadCrmOversightReview.kt` → `CrmReviewScreen.kt`; `CrmReviewViewModel` | Document evidence, consents/declaration, amount, notes, approve-and-send-to-Audit or return-to-CRM behavior. | Role-specific title/return wording over a shared four-checkbox checklist, notes, advance and return callbacks. | Approved amount is absent; declarations differ from web; document evidence is thinner. Source verification of the mobile transition shows a shared CRM review endpoint, but exact resulting stage requires live/API-state validation. | **Unverified** | High |
| Head CRM | Dossier / Workflow History | role-authorized application detail and audit routes | `ApplicationDetailScreen.kt`, `WorkflowEventAuditScreen.kt` | Read full dossier, prior CRM work, uploaded documents, approval conditions, workflow history and servicing information. | Shared dossier and workflow-event screens. | No Head CRM-specific prior-review summary or explicit approval-condition composition. | **Confirmed** | Medium |
| Head CRM | PAR / Servicing Oversight | `shared/par_dashboard.html`, schedule/payment views; reports routes | `ParDashboardScreen.kt`; `ServicingViewModel` | Portfolio classifications, loan detail, schedule and payment history as oversight. | PAR route is allowed, but repayment schedule is not in the Head CRM allow-list. | Portfolio can be opened, but drill-down to repayment schedule is rejected by the centralized route guard. Foundation touch: route policy. | **Confirmed** | High |
| Head CRM | Search | `shared/search_results.html`; `GET /search` | `roles/headcrm/search/HeadCrmSearch.kt`; `SearchViewModel` | Search CRM dossiers, disbursements and payment references. | Shared application-shaped search with Head CRM wording. | No typed disbursement/payment result or oversight filters. | **Confirmed** | Medium |
| Head CRM | Settings | `shared/settings.html`; settings routes | Existing screen excluded by workspace policy | Account details and password change. | Not reachable. | **Missing as a reachable destination.** Foundation touch: route policy. | **Confirmed** | High |

## Missing-entirely destinations

- **High — Confirmed:** reachable Settings/password page.

## Summary

- High: **3**
- Medium: **4**
- Low: **0**

The most consequential gap is that Head CRM approval lacks the amount and full declarations/evidence contract used by web before routing a dossier onward.
