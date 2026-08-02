# FieldCRM Android role-screen parity — Phase 0 summary

This index ranks the ten roles requested in the Phase 0 scope by confirmed/inferred High-severity gap count. Web templates and authorized web routes were treated as the reference; Android catalogues were verified against `WorkspaceRegistry`, `FieldCRMApp`, composables, ViewModels, repositories and mobile API routes.

| Rank | Role | High | Medium | Low | Most consequential gap | Report |
|---:|---|---:|---:|---:|---|---|
| 1 | CRM Officer | 4 | 3 | 1 | No reachable disbursement form; repayment drill-down is also route-blocked. | [android-parity-crm-officer.md](android-parity-crm-officer.md) |
| 1 | System Admin | 4 | 2 | 0 | User creation does not implement the web invitation flow; branch and interest-preset administration are unreachable. | [android-parity-system-admin.md](android-parity-system-admin.md) |
| 3 | Account Officer | 3 | 6 | 1 | No role-owned OCR review queue. | [android-parity-account-officer.md](android-parity-account-officer.md) |
| 3 | Credit Analyst | 3 | 4 | 0 | Credit review lacks major affordability, document, OCR-override, checklist and amount inputs. | [android-parity-credit-analyst.md](android-parity-credit-analyst.md) |
| 3 | Head CRM | 3 | 4 | 0 | Approval lacks the web amount and declarations/evidence contract. | [android-parity-head-crm.md](android-parity-head-crm.md) |
| 6 | Auditor | 2 | 5 | 0 | Audit Search cannot represent audit-event or document results. | [android-parity-auditor.md](android-parity-auditor.md) |
| 6 | Executive Director | 2 | 4 | 1 | Decision page lacks MCC/document evidence and editable approval amount. | [android-parity-executive-director.md](android-parity-executive-director.md) |
| 6 | Managing Director | 2 | 4 | 1 | Decision page lacks final amount, approval notes and board-opinion history. | [android-parity-managing-director.md](android-parity-managing-director.md) |
| 9 | Branch Manager | 1 | 7 | 0 | Assigned pipeline uses a general local application collection instead of the web’s scoped pipeline response. | [android-parity-branch-manager.md](android-parity-branch-manager.md) |
| 9 | Branch Supervisor | 1 | 5 | 0 | Review reuses the Team Lead evidence layout rather than exposing supervisor-specific gates and prior concurrence. | [android-parity-branch-supervisor.md](android-parity-branch-supervisor.md) |

## Aggregate findings

- High: **25**
- Medium: **44**
- Low: **4**
- Missing-entirely/reachability findings are listed separately inside every role report and are always classified High.
- The most common cross-role gap is role-specific naming applied over a shared application-shaped result or review surface without equivalent role-specific fields, filters, evidence or destinations.
- Several Android capabilities exist at the API, ViewModel or composable layer but are excluded by `WorkspaceRegistry`; these are recorded as reachability gaps rather than claiming the implementation is wholly absent.
- Differences in portrait bottom navigation, landscape rail behavior and the single Navigation 3 back stack were not classified as gaps.

## Evidence interpretation

- **Confirmed:** directly observed in both web and Android/backend source.
- **Inferred:** strongly supported by source structure, but one side was not directly demonstrable end to end.
- **Unverified:** requires a running client, authenticated role session or live API data to establish behavior.

No remediation approach, effort estimate or visual redesign is proposed in these Phase 0 reports.
