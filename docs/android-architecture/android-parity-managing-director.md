# Android parity audit — Managing Director

| Role | Destination | Web reference | Android reference | Web capability | Android capability | Gap | Evidence | Severity |
|---|---|---|---|---|---|---|---|---|
| Managing Director | Dashboard | `executive/md_dashboard.html`; `GET /md-dashboard` | `roles/managingdirector/dashboard/ManagingDirectorDashboard.kt`; `DashboardViewModel` | MD approval queue with file references and amounts. | Queue count and shortcut to decisions/MCC. | Actionable per-file preview is absent. | **Confirmed** | Medium |
| Managing Director | MD Approval Queue | `executive/md_queue.html`; `GET /md-queue` | `ManagingDirectorDecisionQueue` → `MdQueueScreen.kt` | Applicant, reference, amount, source, waiting age, review and direct return-to-ED action. | Queue cards open MD approval. | Source, waiting age and direct return action are absent; queue rows depend on a separate locally loaded application list. | **Confirmed** | Medium |
| Managing Director | MD Decision / Advice | `executive/md_approve.html`; MD approval/return routes | `ManagingDirectorDecisionReview.kt` → `MdApprovalScreen.kt`; `CrmReviewViewModel` | MCC recommendations, document summary, return-to-ED input, editable amount, notes, final approval, board request and board-request history/status. | Loan summary, CRM notes, final approve, return-to-ED callback, and board email/name/message request. | MCC/document evidence, editable final amount, approval notes and board-opinion history/status are absent. Equivalent MD approval cannot be recorded. | **Confirmed** | High |
| Managing Director | MCC Dossiers / Recommendation | `executive/mcc_index.html`, `mcc_summary.html`; MCC routes | `MccWorkspaceScreen`; mobile MCC APIs | List, prior recommendations, vote amount/notes and authorized finalization. | List/detail, vote and finalize controls. | Live authorization and role-specific finalization rules remain unverified. | **Unverified** | Low |
| Managing Director | Dossier / PAR | authorized dossier and `/reports/par` routes | shared `ApplicationDetailScreen.kt`, `ParDashboardScreen.kt` | Read full supporting dossier, decisions, conditions, servicing and history. | Shared dossier/PAR. | MD-specific recommendation chronology and approval conditions are not explicitly composed; schedule drill-down is not allow-listed. | **Confirmed** | Medium |
| Managing Director | Decision Search | `shared/search_results.html`; `GET /search` | `roles/managingdirector/search/ManagingDirectorSearch.kt`; `SearchViewModel` | Decision-ready and historical decision lookup. | Shared application-shaped results. | No historical-decision row, source/escalation filter or board-referral search. | **Confirmed** | Medium |
| Managing Director | Settings | `shared/settings.html`; settings routes | Existing screen excluded by workspace policy | Account details and password change. | Not reachable. | **Missing as a reachable destination.** Foundation touch: route policy. | **Confirmed** | High |

## Missing-entirely destinations

- **High — Confirmed:** reachable Settings/password page.

## Summary

- High: **2**
- Medium: **4**
- Low: **1**

The most consequential gap is the MD decision page’s inability to capture the web’s final amount, approval notes, supporting recommendations and board-opinion history.
