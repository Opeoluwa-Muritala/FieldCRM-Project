# Android parity audit — System Admin

| Role | Destination | Web reference | Android reference | Web capability | Android capability | Gap | Evidence | Severity |
|---|---|---|---|---|---|---|---|---|
| System Admin | Dashboard | `system_admin/dashboard.html`; `GET /dashboard` | `roles/systemadmin/dashboard/SystemAdminDashboard.kt`; `DashboardViewModel` | Recent user login/activity rows and role distribution with links to user management. | Active-user, system-event, failed-job and configuration-alert metrics with Users/Activity links. | Web user/activity detail and role distribution are absent; Android shows different health counters. Whether this is an intended backend contract divergence is not documented. | **Confirmed** | Medium |
| System Admin | Users / Invitations | `system_admin/users.html`; `GET /users` and invitation/user mutation routes | `UsersScreen.kt`; direct `MobileApiService` calls | Invite by name/email/role/branch, directory with status/last login, edit role, deactivate, branch assignment. | Create user with name/email/password/role, search, typed states, detail, role/branch update and deactivate. | Android creates a password-bearing account rather than sending an invitation and does not offer reactivation despite the typed-state goal; invitation semantics are missing. | **Confirmed** | High |
| System Admin | Branch Management | Branch section in `system_admin/users.html`; branch creation routes | `BranchManagementScreen` exists in `SpecialistScreens.kt`, but `Screen.BranchManagement` is excluded from System Admin allowed routes and Users has no create-branch control | Create branch and view/use branches during assignment. | Can assign from loaded branches; separate create-branch composable is unreachable. | **Missing as a reachable destination/action:** cannot create a branch. Foundation touch: route policy. | **Confirmed** | High |
| System Admin | System Activity | `system_admin/system_activity.html`; `GET /system-activity` | `SystemActivityScreen.kt`; mobile `/system-activity` | User login/activity plus recent audit events. | Paged workflow events with actor, resource, stage, time and notes; typed loading/empty/403/session/error states. | Mobile endpoint queries only `workflow_events`, so user login activity shown on web is absent; filters are absent. | **Confirmed** | Medium |
| System Admin | Interest Rate Presets | `system_admin/interest_presets.html`; interest preset routes | `InterestPresetScreen` exists in `SpecialistScreens.kt`, but route is excluded from admin workspace | Create, list and delete product/rate/method presets. | Composable/API methods exist but cannot be reached under System Admin policy. | **Missing entirely as a reachable destination.** This conflicts with the verified web route even though the current Android workspace intentionally limits admin to three tabs. Foundation touch: route policy. | **Confirmed** | High |
| System Admin | Settings | `shared/settings.html`; settings routes | Existing screen excluded by admin policy | Account details and password change. | Not reachable. | **Missing as a reachable destination.** Foundation touch: route policy. | **Confirmed** | High |

## Missing-entirely destinations

- **High — Confirmed:** Branch Management/create branch.
- **High — Confirmed:** Interest Rate Presets.
- **High — Confirmed:** reachable Settings/password page.

## Summary

- High: **4**
- Medium: **2**
- Low: **0**

The most consequential gap is that Android’s Users flow creates password-bearing accounts rather than performing the web invitation workflow.
