# FieldCRM web configuration source of truth

This document covers the FastAPI/Jinja2 web application and shared backend. Android consumes the same versioned backend/configuration API but has a separate implementation prompt; do not infer Android UI work from this document.

## Dropped from scope

- Product-positioning prose belongs in `README.md`, not the implementation backlog.
- Promise-to-pay is a Collections Workbench record type, not a standalone module.
- Integration health appears only in the future Integration Hub, not a duplicate dashboard.
- Conflict resolution belongs to resilience/offline behavior, not a standalone module.
- Background document upload belongs to resilience/offline behavior, not a standalone module.

## Project coding instructions

- For every code creation, modification, refactor, review, or debugging task in this repository, invoke and follow the `$vibesec-secure-coding` skill.
- Treat security as an acceptance criterion and apply the skill proportionately to the affected risk surface.
- Do not weaken authentication, authorization, tenant isolation, validation, auditability, secret handling, or other security controls merely to make a feature or test pass.

## Organisation

- [implemented] Organisation and branch tenant scoping.
- [implemented] Phase 3 provides effective-dated institution profiles, organisation presets, settings, and immutable configuration versions through the separate Configuration Admin Hub.
- [planned — phase 7] Institution → Region → Area → Branch → Team → Officer hierarchy.

## Users & Access

- [implemented] The smaller System Admin area manages users, invitations, basic roles/branches, deactivation, password resets, session revocation, and system activity.
- [flagged off by default] Phase 3 provides the separate, larger Configuration Admin Hub (`CONFIGURATION_HUB_ENABLED=false`) as a localhost-only, non-production control plane with a dedicated role, encrypted TOTP, and institution-wide authority. System Admin cannot grant or alter Configuration Admin access.
- [flagged off by default] Phase 5 (`CONFIGURABLE_WORKFLOW_ENABLED=false`) adds permission bundles, maker-checker action separation, expiring delegation, and audited transactional portfolio reassignment while preserving legacy route compatibility.
- [flagged off by default] Phase 2 Customer Identity (`CUSTOMER_IDENTITY_ENABLED=false`) adds staff-only Customer 360 access with officer/branch/object scoping; System Admin cannot view customer PII.

## Products

- [implemented] Product catalog, limits, tenor, repayment frequency, basic guarantor/collateral requirements, document requirements, and product stage lists.
- [implemented] Configuration Admin has a dedicated product page where working-draft products can be added, opened, and edited without exposing published versions to in-place mutation.
- [flagged off by default] `loan_products.cbs_enabled` opts an individual product into CBS authority only when the deployment CBS flag is also enabled.
- [flagged off by default] Phase 4 (`CONFIGURABLE_PRODUCTS_ENABLED=false`) provides draft-bound admin product definitions for rules, sections, checks, documents, SLA, workflow, approval limits, CBS applicability, and effective publication.

## Forms

- [implemented] Existing code-defined web origination, business, employment, financial, guarantor, collateral, document, GPS/visit, recommendation, review-comment, and exception-reason flows remain supported.
- [implemented] Phase 4 persists required/optional/hidden sections and dynamic text, number, currency, date, dropdown, checkbox, yes/no, photo, file, signature, and GPS fields with conditional visibility, validation, help text, a generic web renderer, and shared versioned API definitions.
- [implemented] Configuration Admin has separate draft-bound form-field and document-checklist editors; validation patterns are selected from a server allowlist and tenant/version ownership is derived server-side.
- [implemented] Phase 2 customer creation validates identity fields server-side and stops before save on probable duplicates unless a typed override reason is supplied.

## Workflow

- [implemented] Existing staged origination and immutable workflow events.
- [implemented] Phase 5 provides versioned ordered/conditional stages, permission actors, return routes, and immutable application workflow/approval snapshots fixed at origination.
- [planned — phase 6] Formal tasks, dossier timeline, exceptions, notes, mentions, notifications, and conditions precedent.

## Approval Matrix

- [implemented] Existing explicit Branch Manager, Branch Supervisor, Credit Analyst, CRM, Head CRM, ED, and MD approval routes.
- [implemented] Phase 5 approval authority can depend on amount, product, risk, branch, collateral, customer type, and exceptions. Explicit ED/MD roles are authoritative; `EXECUTIVE` remains a compatibility alias.
- [flagged off by default] Credit Committee routes require the published `committee_review` flag. Audit remains independent of mandatory approval stages.

## Documents

- [implemented] Contextual application documents, secure upload/preview, basic type/content/size validation, OCR processing, and product document requirements.
- [implemented] Phase 4 provides product checklists, percentage/missing-item submission gates, application-contextual uploads, an exception-only document queue, and pre-OCR blur/lighting/crop/glare/readability assessment.

## Features

- [flagged off by default] External Applicant Portal/customer self-service is unavailable to normal deployments.
- [flagged off by default] CBS integration is controlled by `CBS_INTEGRATION_ENABLED=false` plus a per-product flag.
- [implemented] Phase 3 versions switches for the operational workspace, external portal, OCR, visits, GPS, guarantors, collateral, bureau modes, CBS, manual repayments/schedules, PAR, collections, committee/legal/ED/MD/audit stages, messaging, push, and offline mode. Published flags control both navigation and server routes, including mobile/API actions; existing operational modules retain enabled compatibility defaults while new external-portal and CBS capabilities remain off.

## Integrations

- [implemented] Phase 1 provides a generic CBS provider contract, deterministic mock adapter, validated snapshots, signed/idempotent webhooks, secret-gated batch sync, manual refresh, reconciliation, and tenant-scoped sync history.
- [implemented] CBS imports balances, repayments, schedules, arrears, DPD, status, and disbursement data into separate read-only financial tables; transaction IDs are idempotent and append-only.
- [implemented] CBS web displays use `Source: Core Banking`, last-updated timestamps, and a configurable stale-data warning.
- [planned — phase 8] A single Configuration Admin Integration Hub for CBS, Identity, Credit Bureau, Documents, Email, and SMS with provider health, retry, timeout, and secure credentials.

## Field Operations

- [implemented] Existing visits, GPS/location, visitation findings/sign-off, officer recommendations, and manual collection notes remain unchanged.
- [planned — phase 6] My Work task engine, role-focused navigation, central exceptions, categorized notes, and notification coverage.
- [planned — phase 7] CBS-driven collections cases, actions, promise-to-pay records, and DPD queues.
- [implemented] Phase 2 Customer 360 aggregates current applications/loans, CBS exposure, repayments, visits, documents, guarantors, collateral, credit evidence, communications, and the customer/application activity timeline.
- [implemented] The Current Loans application overview is a tenant-authorized, read-only dossier with a persistent summary header, feature-aware section links, curated officer-provided fields, readiness, protected document previews, recent activity, and CBS financial context.

## SLA

- [implemented] Phase 4 product definitions include version-bound SLA hours; Phase 6 consumes them for stage TAT tracking.
- [planned — phase 6] Stage-age/TAT engine, targets, pauses with controlled reasons, overdue cases, and bottleneck reporting.

## Security

- [implemented] CSRF defense, CSP/security headers, tenant object authorization and optional PostgreSQL RLS, parameterized SQL, encrypted sensitive fields, refresh rotation/reuse detection, session revocation, secure document access, rate limiting, and production configuration checks.
- [implemented] Phase 1 CBS endpoints use object authorization; new tables have tenant RLS; webhooks use HMAC, a five-minute replay window, payload limits, and event idempotency; batch sync uses a separate strong secret.
- [implemented] Phase 3 Configuration Admin access is rejected in production and requires localhost, a dedicated role, encrypted TOTP secrets, short-lived HttpOnly step-up sessions, CSRF protection, and different-person approval for high-risk versions.
- [implemented] Phase 5 permission and maker-checker tests cover policy conditions, authority thresholds, compatibility and prohibited action combinations.
- [planned — phase 8] Account lockout, privileged MFA coverage, credential vaulting, device/session controls, and commercial-deployment hardening.

## Branding

- [implemented] Phase 3 versions institution/login/report logos, report header, support information, and brand accent; effective values feed web templates and the additive mobile config contract.

## Audit

- [implemented] Audit and workflow history are append-only/immutable; Audit monitors independently rather than acting as a mandatory loan stage.
- [implemented] CBS sync attempts, failures, reconciliation issues, actors/triggers, and source metadata are retained.
- [implemented] Phase 3 configuration diffs and second approvals are append-only; published versions are immutable and retain creator, validator, approver, publisher, reasons, and effective timestamps.
- [implemented] Phase 5 retains maker-checker actions, delegations, portfolio reassignment counts/reasons, credit outcomes, manual bureau evidence, collateral register details, and guarantor exposure flags.
- [implemented] Customer duplicate overrides and customer activity are append-only. The canonical taxonomy includes created, edited, submitted, returned, document uploaded, visit completed, credit reviewed, approved, CBS sync, repayment detected, collection action, configuration applied, application linked, and workflow transition.

## System Health

- [implemented] Basic API health and request/security logging.
- [planned — phase 8] Consolidated integration/API/database/job/OCR/document/login/workflow/slow-endpoint observability, CI gates, backup/PITR/retention, restore procedure, and dated staging restore evidence.
