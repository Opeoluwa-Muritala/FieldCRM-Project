# Android parity audit — Auditor

| Role | Destination | Web reference | Android reference | Web capability | Android capability | Gap | Evidence | Severity |
|---|---|---|---|---|---|---|---|---|
| Auditor | Dashboard | `auditor/dashboard.html`; `GET /dashboard` | `roles/audit/dashboard/AuditDashboard.kt`; `DashboardViewModel` | Live compliance-flag cards and recent audit-activity cards with links. | Counts for unverified documents, critical OCR gaps, workflow exceptions and events today. | Actionable flag/event previews, actors, resources and timestamps are absent. | **Confirmed** | Medium |
| Auditor | Open Compliance Flags | `auditor/compliance_flags.html`; `GET /compliance-flags` | `ComplianceFlagsScreen.kt`; direct mobile queue call | Organization flags with application context and open action. | Loads typed display fields from JSON, shows loading/error/empty and opens an audit destination. | Filtering, paging and flag-detail/status context are absent; the screen stores only a small field subset. | **Confirmed** | Medium |
| Auditor | Immutable Audit Trail | `auditor/audit_trail.html`; `GET /audit-trail` | `AuditTrailScreen.kt`; `AuditTrailViewModel.loadGlobal/load` | Global immutable activity with actor/action/resource/time/outcome. | Event list supports global or selected-application loading. | Source does not expose web-equivalent filters or paging, and row detail is narrower. | **Confirmed** | Medium |
| Auditor | Compliance Application Detail | `auditor/application_detail.html`; `GET /applications/{id}` | shared `ApplicationDetailScreen.kt`; application/audit ViewModels | Timeline, document checklist, screening, compliance evidence log, state transitions with actor/outcome/notes, and uploaded-file verification log. | Shared identity/collateral/visit/document/audit dossier sections. | Dedicated compliance evidence and uploaded-file verification logs are not composed as distinct auditor sections; the generic dossier omits some actor/verifier/outcome fields. | **Confirmed** | Medium |
| Auditor | Search Auditable Records | `shared/search_results.html`; `GET /search` and audit routes | `roles/audit/search/AuditSearch.kt` → shared `SearchResultsScreen.kt`; `SearchViewModel` | Search authorized audit events, applications/documents and open the corresponding record. | Audit wording over the same application-result UI used by operational roles. | Audit events and documents are not typed result rows; actor/action/resource searches cannot be represented as their authorized destinations. | **Confirmed** | High |
| Auditor | Workflow Event Detail | application workflow-event API/routes | `WorkflowEventAuditScreen.kt`; `AuditTrailViewModel` | Per-application transitions with actor, role, prior/new stage, outcome and notes. | Per-application event list with loading and empty state. | Failure/permission/session states are not separately rendered and field presentation is reduced. | **Confirmed** | Medium |
| Auditor | Settings | `shared/settings.html`; settings routes | Existing screen excluded by workspace policy | Account details and password change. | Not reachable. | **Missing as a reachable destination.** Foundation touch: route policy. | **Confirmed** | High |

## Missing-entirely destinations

- **High — Confirmed:** reachable Settings/password page.

## Summary

- High: **2**
- Medium: **5**
- Low: **0**

The most consequential gap is that Audit Search is only a relabelled application search and cannot return audit-event or document result types.
