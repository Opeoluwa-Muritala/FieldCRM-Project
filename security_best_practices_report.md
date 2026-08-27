# FieldCRM VibeSec Security Review

## Executive summary

This review covered the current uncommitted FastAPI/Jinja/browser worktree, with Android explicitly excluded. The repaired self-service password-recovery path follows the expected security controls. The separate System Admin password-reset feature originally introduced a predictable shared credential; VS-001 is now remediated. Organization-wide visitation-signoff access for branchless Credit Analysts was confirmed as intentional policy and is recorded as accepted.

VS-001 was fixed after review and must remain covered by the focused regression tests.

## High severity

### VS-001 — System Admin reset installs a shared, publicly visible password

- **Status:** Remediated

- **Rule ID:** FASTAPI-AUTH-003 / VIBESEC-CREDENTIALS
- **Severity:** High
- **Location:** `backend/app/domains/users/service.py:169-177`; `frontend/templates/system_admin/users.html:159`; `backend/app/domains/users/router.py:132-139`
- **Evidence:** `default_password = "Password@012345"` is used for every reset, and the same password is displayed in browser JavaScript after the operation.
- **Impact:** Anyone who knows or guesses a reset user's email can authenticate with a predictable credential after an administrator performs the reset. Reuse across every reset increases the blast radius. The implementation invalidates the auth-user cache but does not call `revoke_all_sessions_for_user`, so existing refresh/auth sessions are not explicitly revoked as they are in the secure self-service reset path.
- **Fix:** Remove direct assignment of a default password. Have the administrator initiate the existing random, one-hour, single-use reset-token flow and send the link to the user's registered email. Revoke all active refresh/auth sessions when the credential changes, and append an audit event containing actor, target, organization, time, and outcome—but never the token.
- **Mitigation:** Until fixed, remove or feature-disable the admin reset control. Do not replace the constant with a returned temporary password; emailed single-use enrollment is safer and avoids exposing credentials in UI responses.
- **False-positive notes:** None. The predictable credential is present directly in source and rendered to the administrator.

**Resolution:** The admin action now initiates the random, one-hour, single-use emailed recovery flow instead of changing the password. The predictable password and browser disclosure were removed. Inactive accounts are rejected, and successful token consumption continues to revoke refresh tokens and server-side auth sessions.

## Medium severity

### VS-002 — Branchless Credit Analysts receive organization-wide visitation signoffs

- **Status:** Accepted intentional policy

- **Rule ID:** FASTAPI-AUTHZ-001 / VIBESEC-TENANT-OBJECT-SCOPE
- **Severity:** Medium
- **Location:** `backend/app/domains/loans/router.py:466-487`; `backend/app/domains/visitation/queries/list_pending_signoffs.sql:22-27`
- **Evidence:** The recent-visits query uses `(la.branch_id = $2 OR $2 IS NULL)`, while the queue query grants access when `(SELECT branch_id FROM users WHERE id = $2) IS NULL`.
- **Impact:** Any Credit Analyst whose `branch_id` is null can view visitation records and signoff work across every branch in the organization. This weakens the documented branch/object-scoping model and can expose customer/application data outside the analyst's assigned portfolio.
- **Fix:** Define an explicit institution-wide permission or assignment rule and check it server-side. Otherwise fail closed when `branch_id` is null. Keep the organization predicate and scope both the queue and recent-visits query identically. Add negative tests proving an analyst cannot read or concur on an application outside their authorized branch/portfolio.
- **Mitigation:** Ensure all Credit Analyst accounts have a valid branch assignment until explicit global authority exists.
- **False-positive notes:** If a null branch is an intentionally documented marker for institution-wide Credit Analyst authority, this may be accepted—but that authority should be represented by an explicit permission rather than an ambiguous missing branch.

## Verified controls

- Self-service recovery generates a cryptographically random token with a one-hour expiry.
- Reset tokens are stored hashed and consumed once within a transaction.
- Password policy validation and password hashing remain active.
- Successful self-service reset revokes refresh tokens and server-side auth sessions and invalidates cached identity data.
- Forgot-password responses remain neutral for existing and non-existing accounts.
- Email HTML escapes the user's name and reset URL.
- CSRF middleware requires exact-origin validation and a matching token for cookie-authenticated unsafe requests.
- SQL changes reviewed here remain parameterized and tenant predicates remain present.

## Verification performed

- `36 passed`: password-reset security, user privilege cache, user administration, loan authorization, database RLS context, and redirect-security tests.
- `git diff --check`: only pre-existing trailing whitespace was reported in the user-service/test changes.
- Manual review of authentication, password reset, session revocation, CSRF middleware, tenant/branch SQL, Jinja rendering, and changed browser JavaScript.
- `bandit` and `pip-audit` were unavailable in the installed Python environment, so no claim is made about full static-analysis or dependency-vulnerability coverage.

## Residual risk

This was a current-worktree review, not a penetration test. Dependency advisories were not verified because `pip-audit` is not installed, and high-signal DOM-sink searches found existing `innerHTML` usage outside the immediate changed recovery flow that warrants a separate end-to-end data-flow review.
