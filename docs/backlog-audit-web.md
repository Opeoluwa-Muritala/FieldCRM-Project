# FieldCRM Web Configurable Product Backlog Audit

Audit date: 20 August 2026  
Scope: FastAPI/Jinja2/PostgreSQL web application and the shared backend/API only. Android UI and on-device implementation are excluded.  
Source-of-truth filename clarification: the prompt's `agent.md` means the repository's root `AGENTS.md`.

## Mandatory pre-work result

### Existing `AGENTS.md` sections

The file was read in full before this audit was written. It currently contains one section only:

- `Project coding instructions` — requires `$vibesec-secure-coding`, makes security an acceptance criterion, and prohibits weakening authentication, authorization, tenant isolation, validation, auditability, secret handling, or other controls.

It does not yet contain the requested product/configuration sections: Organisation, Users & Access, Products, Forms, Workflow, Approval Matrix, Documents, Features, Integrations, Field Operations, SLA, Security, Branding, Audit, or System Health.

### Tooling and repository checks

- Migration tooling: `alembic==1.13.1` is installed; `backend/alembic.ini`, `backend/alembic/`, and `backend/migrations/` exist.
- Database access: raw parameterized asyncpg repositories are the prevailing domain pattern; there is no application ORM.
- Templates/design system: Jinja templates exist under `frontend/templates`; shared shells and CSS variables are present. `DESIGN.md` exists; there is no separately named design-token file assumed by the implementation.
- Test runner: the repository `.venv` points at a Microsoft Store interpreter that is inaccessible in this execution environment. The installed user Python 3.13.5 interpreter can run the suite and has pytest 9.0.3.
- Baseline: **195 passed, 0 failed, 2 warnings in 14.51s** using `python -m pytest`. Warnings are a Starlette 422 constant deprecation and an inaccessible `.pytest_cache` write; neither changes the pass count.
- Worktree: clean before audit creation.

### Backend/web module walk

| Module | Current responsibility | Backlog relevance |
|---|---|---|
| `aml` | Sanctions screening service | Identity/credit input only; not configurable |
| `audit` and `app/core/audit.py` | Tenant-scoped audit writes and audit views | Strong foundation; no configuration change versions yet |
| `auth` | Web/mobile sessions, refresh rotation, reset, revocation | Strong session foundation; no MFA/account-lockout policy |
| `branches` | Organisation-scoped branch create/list | No region/area/team hierarchy |
| `credit_bureau` | Direct provider adapters and persistence | Direct checks exist; manual-evidence default workflow is incomplete |
| `documents` | Validated uploads, direct-upload authorization/finalization, OCR hook | Contextual documents exist; no configurable work queue/quality scoring |
| `feasibility` | Officer-entered cashflow and ratios | Must remain manual; not CBS data |
| `guarantors` | Guarantor wizard persistence | No configurable counts or exposure tracking |
| `loans` | Origination, dossier, approval routes, servicing/PAR screens | Main scaffolding; workflow and servicing contain hardcoded behavior |
| `notifications` | User/application notifications | No mentions, category permissions, or full event coverage |
| `ocr` | OCR worker and exception queries | Specialist queues exist but remain primary navigation entries |
| `signing` | Versioned signing evidence and immutable payload hashes | Useful versioning pattern; not general record conflict handling |
| `users` | Organisation users, roles, branches, invitations/deactivation | System-admin foundation; role checks are not permission bundles |
| `verification` | BVN/NIN provider hooks | Partial identity verification only |
| `visitation` | Visit reports and manager sign-off | Existing manual field flow to preserve |
| `workflow` and `app/core/workflow.py` | Event log plus fixed authoritative stage/role tuple | Some product stage lists exist, but runtime flow remains substantially hardcoded |
| `app/api/v1/mobile.py` | Existing Android-compatible API v1 | Must remain backward compatible; `/mobile/config` is hardcoded and incomplete |
| `frontend/templates` | Role dashboards, loan dossier/wizard, documents, visits, approvals, OCR, PAR, audit | Large existing UI surface; no configuration control centre or Customer 360 |

## Requirement classification

`EXISTS` means the stated requirement is already complete enough that it must not be rebuilt. `PARTIAL` means reusable behavior exists but the acceptance criterion is not met. `MISSING` means no implementation was found.

### Phase 1 — Core Banking (CBS) integration domain

| Requirement | Status | Evidence / gap |
|---|---|---|
| Provider interface, mock adapter, future REST adapter | EXISTS | `core_banking/provider.py::CoreBankingProvider` defines the provider contract/registry; `mock.py::MockCoreBankingProvider` provides deterministic demo data and fixture injection. |
| Scheduled/batch imports and webhook ingestion | EXISTS | `CoreBankingService.sync_batch` plus secret-gated `/api/v1/internal/core-banking/sync`; signed, replay-windowed, idempotent `/api/v1/core-banking/webhooks/{provider_name}` ingestion. |
| External customer/loan/transaction IDs and sync state | EXISTS | Reversible migration `042_core_banking.sql` adds mappings/sync state and tenant-scoped snapshot, transaction, schedule, run, and reconciliation tables. |
| Import balances, repayments, schedules, arrears, DPD, status, disbursement | EXISTS | `CoreBankingService.sync_loan` validates and imports the normalized snapshot into dedicated server-authoritative tables. |
| CBS repayment records read-only | EXISTS | Database append-only trigger plus web/mobile/service-level write guards reject local repayment, schedule, and disbursement writes for CBS-enabled products. |
| Manual refresh and scheduled sync | EXISTS | Authenticated object-authorized API/web refresh routes, dossier button, and batch job endpoint are implemented. |
| Idempotency, reconciliation, unmatched/failed states | EXISTS | Provider transaction IDs and webhook event IDs are unique; immutable-value conflicts and unmatched/failed cases create explicit reconciliation records. |
| Stale-data warning with configurable threshold | EXISTS | `CBS_STALE_AFTER_MINUTES` is bounded and the shared CBS component/repayment page warn when a snapshot is stale or absent. |
| CBS/manual source labels and field provenance metadata | EXISTS | `field_value_metadata` supports all required sources; CBS fields are verified/provider-tagged, web/mobile/OCR intake changes can record sidecar provenance, and CBS cards show source/last-updated labels. |

### Phase 2 — Customer identity and lookup

| Requirement | Status | Evidence / gap |
|---|---|---|
| Customer 360 web page with full related-data timeline | EXISTS | Staff-only `/customers/{customer_id}` aggregates personal/account data, applications/loans, CBS exposure/arrears/DPD, repayments, visits, documents, guarantors, collateral, bureau evidence, communications, collection placeholder, and a canonical activity timeline. Restricted identifiers are masked outside Credit Analyst/Audit views. |
| Existing-customer search during origination | EXISTS | `/api/v1/customers/search` covers local/CBS identifiers and the new-application UI shows “Existing customer found → Use Existing Customer”; a scoped profile snapshot is copied to the new application and linked without changing the customer record. |
| Duplicate detection and typed override reason | EXISTS | `CustomerService.duplicates` checks BVN, NIN, phone, email, bank account, similar name+DOB, address, and CBS identity. API/web creation stops before insert and requires a 10+ character reason; overrides are append-only. |
| Global multi-identifier search | EXISTS | Feature-gated global search resolves name, customer/application reference, phone, account, CBS customer/loan IDs, BVN, NIN, and business name with role/branch scope and masked results. |
| Defined customer/application event taxonomy | EXISTS | `customers/service.py::EVENT_TAXONOMY`, append-only `customer_activity`, and dossier projections cover created/edited/submitted/returned/document/visit/credit/approval/CBS/repayment/collection/config/workflow events. |

### Phase 3 — Restricted Configuration Control Centre

| Requirement | Status | Evidence / gap |
|---|---|---|
| Restricted configuration portal with localhost-only access, MFA, config-admin role | EXISTS | Separate `/configuration` control plane is unavailable in production and gated locally by `CONFIGURATION_HUB_ENABLED`, loopback host/client checks, the dedicated `configuration_admin` role, and encrypted TOTP with a 15-minute step-up cookie. It is not part of System Admin. |
| Split System Admin and Configuration Admin | EXISTS | System Admin retains users/branches/account controls and is explicitly forbidden from granting/changing Configuration Admin access; configuration-wide controls live in the separate domain and role. |
| Centrally managed feature switches affecting validation/UI/API | EXISTS | The dedicated `/configuration/features` page edits all working-draft switches. Published flags enter every Jinja context, remove affected role navigation, and reject disabled web/API routes server-side (including OCR workers, bureau actions, committee/legal/executive review, PAR, visits, work queues, and documents). `/api/v1/config/mobile` remains additive and dependency validation blocks invalid combinations. |
| Internal-only default; external applicant portal OFF | EXISTS | There is no customer application/self-service route. `backend/tests/test_customer_surfaces_removed.py` asserts removal. `shared/public_home.html` is informational rather than an applicant portal. Do not rebuild an external portal. |
| Branding/white-label and organisation presets | EXISTS | Versioned branding covers institution/login/report logos, header, support details, accent and the five presets. Effective branding is injected into web Jinja context and mobile config without code branching. |
| Draft → Validate → Publish, versions, effective dates, maker-checker | EXISTS | `configuration_versions` retains immutable published history, supports effective dates, and enforces Draft → Validate → optional different-person Approval → Publish. New applications snapshot the effective configuration version. |
| Dependency validation | EXISTS | Validation blocks disabling guarantors/collateral/CBS while an active product requires them and rejects conflicting direct/manual bureau modes. |
| Configuration diff audit with reason/approver | EXISTS | `configuration_change_log` is append-only and records old/new/path/actor/reason; high-risk approval is a separate immutable entry with approver, while versions retain validator/publisher timestamps. |

### Phase 4 — Configurable products, forms, and documents

| Requirement | Status | Evidence / gap |
|---|---|---|
| Admin-defined complete loan products | EXISTS | The separate `/configuration/products` page adds products to a selected working draft and opens draft product cards in an edit page. Definitions cover name/code, amounts, interest, tenor/frequency, guarantors, collateral, documents, sections, workflow/limits, visits, credit checks, SLA, and CBS applicability; published versions remain immutable. |
| Dynamic configurable forms shared by web/API | EXISTS | The separate `/configuration/forms` editor adds and edits draft-bound product fields. `product_form_fields`, validated schemas, `/api/v1/config/products/{code}`, and the generic Jinja renderer support all required types, required/hidden state, options, conditional visibility, allowlisted rules, help text, and secure contextual file capture. |
| Required/optional/hidden sections per product | EXISTS | `product_section_requirements` persists the nine section states and rejects required fields inside hidden sections. |
| Product document checklist and readiness blocking | EXISTS | Version-bound checklists feed a percentage/missing-items indicator; both web intake submission paths return a conflict until configured mandatory fields/documents are present. |
| Contextual documents and exception-only central queue | EXISTS | Uploads remain application-contextual. Primary OCR/upload links were replaced by `/document-work-queue`, which contains only missing, quality, OCR, rejection, and processing exceptions. |
| Pre-OCR document quality checks | EXISTS | Image uploads receive blur, lighting, crop/resolution, glare, and readability scores; non-passing captures are persisted for review and held out of the OCR pending queue. |

### Phase 5 — Workflow, approvals, and credit rules

| Requirement | Status | Evidence / gap |
|---|---|---|
| Configurable ordered/conditional stages, actors, and returns | EXISTS | Draft-bound workflow definitions persist ordered stages, permission actors, safe declarative conditions and return stages; applications snapshot the definition tied to their origination config version. |
| Multi-factor approval matrix | EXISTS | Effective rules match amount, product, risk, branch, collateral, customer type and exception count. Web/mobile ED/MD decisions consult the application snapshot rather than the former hardcoded threshold. |
| Optional direct bureau plus manual evidence default | EXISTS | Direct adapters remain optional; the default evidence endpoint requires provider, date, controlled result, application-owned report document and analyst assessment. |
| Explicit ED/MD roles; generic EXECUTIVE compatibility; optional Committee | EXISTS | ED/MD remain explicit, `EXECUTIVE` canonicalizes to ED for compatibility, and every MCC web mutation/view is backend-gated by the published `committee_review` switch. |
| Audit independent of mandatory workflow | EXISTS | Auditor routes are read/flag/note-oriented and `app/core/workflow.py::WORKFLOW_STAGES` does not insert Audit into every approval path. Preserve this behavior. |
| Permission bundles composed into roles | EXISTS | Persisted permission codes/bundles and `PermissionChecker` support direct and currently valid delegated authority; new workflow/config operations use permissions rather than raw role names. |
| Maker-checker transaction and configuration controls | EXISTS | Action evidence prevents one actor combining originate/recommend/approve/disburse/reverse; critical config versions already require a different approver. Hooks cover origination, credit recommendation, branch/ED/MD approval and disbursement. |
| Temporary delegation with expiry/audit | EXISTS | Tenant-scoped delegations have explicit permission, start/end, different users, creator and automatic time-window expiry. |
| Bulk portfolio reassignment | EXISTS | Permission-gated transactional reassignment moves applications/current ownership and visits, records per-entity counts, scope, reason and actor, and is ready for Phase 6/7 task/collection extensions. |
| Credit rules/checklist engine | EXISTS | Version-bound rules use allowlisted operators (no eval), persist pass/fail/not-applicable outcomes, and leave final judgment with the permitted analyst. |
| Collateral register | EXISTS | Existing multi-item/document register now includes owner, forced-sale value, valuer/date, insurance, legal status and expiry while retaining linked application/loan semantics. |
| Guarantor exposure tracking | EXISTS | Permission-scoped exposure aggregates loans, total amount and active guarantees by protected BVN blind index and flags unusually high counts/value. |

### Phase 6 — Day-to-day operational UI (web)

| Requirement | Status | Evidence / gap |
|---|---|---|
| My Work landing plus formal task engine | PARTIAL | `/my-queue`, `list_officer_tasks.sql`, and multiple role queues exist, but tasks are derived from loans rather than persisted with type/assignee/priority/due/status lifecycle. `/dashboard` remains the landing page. |
| Simplified role navigation | PARTIAL | Role-specific desktop sidebars exist under `frontend/templates/components`; specialist OCR/document/queue links remain prominent and there is no config-driven navigation. |
| Tabbed dossier, persistent header, workflow timeline | PARTIAL | The read-only Current Loans overview now has a persistent dossier header, feature-aware section navigation, curated officer-provided summary, readiness, protected document previews, recent activity, and CBS position. The full role workstations still need one shared conditional-stage timeline with explicit `Not Required` states. |
| Compact ED/MD executive summary with approve/return | PARTIAL | `ed_approve.html`, `md_approve.html`, and their GET/POST routes support decisions without opening the wizard. Existing/proposed exposure, full exceptions, completeness, and all requested summary fields are incomplete. |
| Central Exceptions Centre and CBS discrepancy resolution | MISSING | OCR and audit compliance flags are separate queues; no unified exception domain or CBS discrepancy workflow exists. |
| SLA/TAT engine with pauses and bottlenecks | MISSING | Timestamps exist, but no stage targets, pause records/reasons, or bottleneck engine exists. |
| Conditions precedent block disbursement | PARTIAL | `offer_letter_product_configs.conditions_precedent` exists, but `process_disburse` does not enforce a configurable satisfaction register. |
| Policy exception/waiver capture | PARTIAL | Compliance flags and return reasons exist, but the required policy/expected/actual/requestor/reason/approver/date model and controlled waiver flow do not. |
| Categorized notes, visibility, mentions | MISSING | Approval/review comments exist; there is no categorized note domain, visibility policy, or mention parsing. |
| Notification centre with required events | PARTIAL | `notifications` domain and `/notifications` web page exist for several workflow events; missing documents, CBS failures, SLA, mentions, and collection reminders are not covered. |

### Phase 7 — Collections, PAR, and reporting

| Requirement | Status | Evidence / gap |
|---|---|---|
| CBS-driven collections workbench, DPD buckets, actions/PTP | MISSING | No collections domain, case/action/PTP schema, or CBS-driven queues exist. |
| CBS-sourced PAR1/7/30/60/90 dashboard and filters | PARTIAL | `/reports/par`, `par_dashboard.html`, and `list_par.sql` exist. Values derive from FieldCRM loan amount and locally entered repayments, not CBS; requested buckets/filters are incomplete. Acceptance is not met. |
| Institution → Region → Area → Branch → Team → Officer hierarchy | PARTIAL | Organisations, branches, branch assignment, and officer ownership exist; region/area/team hierarchy does not. |
| Full reporting suite with Excel/CSV/PDF | MISSING | PAR screens and individual PDF/offer generation exist, but no general report catalog or controlled export suite covers the listed reports. |
| Controlled Excel import/export with validation | MISSING | No supported admin import/export pipeline exists. |
| Scheduled internal report delivery | MISSING | Email service exists, but no report schedule/approved-recipient engine exists. |

### Phase 8 — Integrations, backend resilience, and production hardening

| Requirement | Status | Evidence / gap |
|---|---|---|
| Single Integration Hub with health/retry/timeout/secure credentials | MISSING | Identity, bureau, OCR, email, and storage services are separately configured; there is no integration registry/admin hub. |
| Autosave, draft recovery, write retry, version conflicts | PARTIAL | Wizard stages save drafts, direct uploads use authorization/finalization, application creation has request idempotency, and signing has immutable versions. General record versions, 409 conflict payloads, retry policy, and Keep server/local/review UI are absent. |
| Financial/CBS data server-authoritative | MISSING | CBS data does not exist, and local manual payment/schedule paths remain active. |
| Production security hardening | PARTIAL | Existing controls include CSRF middleware, CSP nonce/security headers, parameterized SQL, tenant/org checks plus optional RLS, encrypted sensitive intake fields, secure refresh rotation/reuse detection, session revocation, validated document access, rate limiting, and production-setting validation. Missing privileged-role MFA, account lockout, config-admin network/host gate, permission bundles/tests, and complete credential-vault integration. |
| Immutable audit records | EXISTS | `backend/migrations/021_immutable_history.sql` revokes mutation and installs an exception-raising trigger on `audit_entries` and `workflow_events`. No application edit/delete route exists. Preserve this control. |
| `/api/v1` compatibility and modular-monolith domains | PARTIAL | API v1 and multiple domains exist. Required `core_banking`, `config`, `tasks`, `exceptions`, `collections`, `products`, `forms`, and `integrations` domains are absent. |
| Central authoritative configuration APIs | PARTIAL | `/api/v1/mobile/config` and `/products` exist, but configuration is hardcoded/read-only and there are no stable `/config/mobile`, `/config/products`, `/config/workflow` contracts backed by published versions. |
| Required automated and end-to-end tests | PARTIAL | 195 backend tests cover current auth, tenant scope, documents, products, workflow slices, and security. The new CBS/config/duplicates/tasks/collections/maker-checker/effective-date/E2E scenarios do not exist. |
| CI for lint/tests/migrations/secrets | MISSING | No `.github/workflows` or equivalent repository CI pipeline was found. |
| Observability across required services | PARTIAL | Request/security logging and health endpoint exist; no consolidated metrics/tracing/alerts for CBS, jobs, OCR/documents, workflow, DB, and slow endpoints. |
| Backup, PITR/retention, restore procedure and tested restore | PARTIAL | `backend/migrations/backup_data.py`, `restore_data.py`, and `db_backup.json` exist. No production backup schedule/PITR policy or dated staging restore-test evidence was found. |

## Existing behavior explicitly not to rebuild

- Internal staff-only operation with no external applicant/self-service portal.
- Audit as an independent monitoring capability rather than a mandatory approval stage.
- Immutable audit/workflow history.
- Existing manual origination, financial input, guarantor, collateral, document, OCR, visitation, recommendation, approval-comment, return-reason, repayment-entry, and notification flows. New phases must preserve them unless a product has CBS enabled, at which point only the CBS-owned financial fields become read-only.

## Baseline gate

Every phase must retain at least **195 passing backend tests**, add focused positive and negative tests, and leave the full suite green. A phase is not complete until its audit rows move to `EXISTS`, its matching `AGENTS.md` sections are updated, evidence is recorded, and a rollback plan is supplied.

## Phase evidence and rollback

### Phase 1 — Core Banking (CBS) integration domain

**Evidence:** `backend/migrations/042_core_banking.sql` and `042_core_banking.rollback.sql`; `backend/tests/test_core_banking_phase1.py` (7 focused tests); full suite **202 passed, 0 failed, 2 warnings**. The mock provider test proves snapshot/schedule import and transaction idempotency without writing officer-entered fields. No migration was applied to an external database during this coding run.

**Revert plan:** Set `CBS_INTEGRATION_ENABLED=false` first so all new routes/UI and provenance side writes fail closed and every product returns to its unchanged local/manual behavior. Apply `backend/migrations/042_core_banking.rollback.sql` in an approved maintenance window after exporting any CBS snapshots/reconciliation evidence that must be retained, then revert the Phase 1 domain/router/template changes. The rollback drops only Phase 1 tables, columns, indexes, trigger, and the `loan_products.cbs_enabled` flag; it does not modify existing manual repayment, schedule, application, document, or audit records.

### Phase 2 — Customer identity and lookup

**Evidence:** `backend/migrations/043_customer_identity.sql` and `.rollback.sql`; `backend/tests/test_customer_identity_phase2.py` (6 focused tests); full suite **208 passed, 0 failed, 2 warnings**. Focused tests prove that matching BVN/NIN/phone prevents creation, a typed override creates append-only evidence, object authorization excludes System Admin/cross-branch access, and the taxonomy/migration contract is present. No external database migration was applied during this coding run.

**Revert plan:** Set `CUSTOMER_IDENTITY_ENABLED=false` first; web/API customer routes then return 404 and the existing application-backed borrower/search/origination flows continue unchanged. Export customer/override/activity records if retention is required, apply `043_customer_identity.rollback.sql`, and revert the Phase 2 domain/templates/integration changes. The down migration removes the nullable application link and Phase 2 tables only; existing applications and their manual intake snapshots are retained.

### Phase 3 — Restricted Configuration Control Centre

**Evidence:** `Phase 3 — Restricted Configuration Control Centre — Evidence: backend/migrations/044_configuration_hub.sql + 044_configuration_hub.rollback.sql; separate Overview/Features/Versions pages and published route/UI gates; backend/tests/test_configuration_phase3.py (11 passed); full maintained pytest suite (235 passed, 2 warnings).` No external database migration was applied from this workspace.

**Revert plan:** Set `CONFIGURATION_HUB_ENABLED=false` first, which removes the control plane and returns CBS/web/mobile behavior to the deployment-level compatibility path. Export version/change history if retention is required, revert the configuration domain/router/context integrations and application-origin snapshot query, then apply `044_configuration_hub.rollback.sql`. The rollback removes Configuration Admin MFA/configuration tables and restores the prior role constraint without changing existing operational loan data.

### Phase 4 — Configurable products, forms, and documents

**Evidence:** `Phase 4 — Configurable products, forms, and documents — Evidence: backend/migrations/045_configurable_products_forms.sql + .rollback.sql; separate draft Product/Form fields/Document checklist editors; backend/tests/test_configurable_products_phase4.py (9 passed); full maintained pytest suite (235 passed, 2 warnings).` No external database migration was applied from this workspace.

**Revert plan:** Set `CONFIGURABLE_PRODUCTS_ENABLED=false` first so legacy product queries, code-defined forms, and existing readiness behavior resume unchanged. Export any draft/published product definitions and dynamic values requiring retention, revert the products domain/routes/templates/readiness and quality integrations, then apply `045_configurable_products_forms.rollback.sql`. The rollback removes Phase 4 definition/value/quality tables and additive product columns but preserves legacy products, applications, stage data, and documents.

### Phase 5 — Workflow, approvals, and credit rules

**Evidence:** `Phase 5 — Workflow, approvals, and credit rules — Evidence: backend/migrations/046_configurable_workflow_permissions.sql + .rollback.sql; backend/tests/test_configurable_workflow_phase5.py (6 passed); full maintained pytest suite (228 passed, 2 warnings).` No external database migration was applied from this workspace.

**Revert plan:** Set `CONFIGURABLE_WORKFLOW_ENABLED=false` first so existing hardcoded workflow routes and role compatibility continue unchanged. Export workflow snapshots, action separation, delegations, reassignment and credit outcomes if required, revert the workflow domain and guarded hooks, then apply `046_configurable_workflow_permissions.rollback.sql`. The rollback removes only Phase 5 policy/control tables and additive collateral fields; existing applications, workflow/audit history, guarantors, collateral items and approval records remain.
