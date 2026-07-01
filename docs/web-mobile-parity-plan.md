# Web and Mobile Parity Plan

This document defines what needs to be done for the FieldCRM website and Android mobile app to match functionally, use the same backend database, and handle the same file and data formats.

## Goal

The website and mobile app should become two clients for one authoritative system:

- Same PostgreSQL backend tables and workflow rules.
- Same API contracts for users, applications, guarantors, documents, OCR review, visitation, credit review, approvals, returns, audit, notifications, and configuration.
- Same accepted document categories, form codes, file metadata, and physical file storage behavior.
- Same role permissions and stage transitions.
- Same offline/online sync rules for mobile without creating a second business model.

The web frontend can remain server-rendered Jinja. The mobile app can keep SQLDelight for offline caching. The key requirement is that mobile local storage mirrors the backend contract instead of inventing separate entities.

## Current State

### Backend and Web

- The backend is the source of truth and uses PostgreSQL-oriented migrations in `backend/migrations`.
- Main workflow tables include `organisations`, `users`, `loan_applications`, `stage_data`, `guarantors`, `pledged_items`, `documents`, `ocr_results`, `ocr_fields`, `workflow_events`, `visitation_reports`, `notifications`, and `audit_entries`.
- The web app uses Jinja templates under `frontend/templates` and domain services/repositories under `backend/app/domains`.
- Web routes use session cookies and server-rendered pages.
- Mobile API routes already exist under `/api/v1/mobile` in `backend/app/api/v1/mobile.py`.
- Existing docs define the mobile API surface in `docs/android-api.md` and the loan form fields in `docs/loan-forms-and-flow.md`.

### Android and Shared Module

- Android uses Kotlin, Jetpack Compose, ViewModels, and repositories.
- The shared module uses Ktor, SQLDelight, and local sync foundations.
- SQLDelight currently stores older mobile-shaped tables such as `Borrower`, `LoanApplication`, `RepaymentRecord`, `CommunicationLog`, and `SyncQueue`.
- Mobile code still references borrower-style APIs and offline models that do not fully match the current backend schema.
- Mobile upload code can attempt multipart upload, but the documented backend mobile document route currently records a document category rather than streaming and storing a physical file.

## Main Gaps To Close

1. **Data model mismatch**
   - Backend has `loan_applications` as the primary customer/application workflow record.
   - Mobile still has a separate `Borrower` table and older `LoanApplication` shape.
   - Mobile needs local tables that map directly to backend response contracts.

2. **API contract drift**
   - `docs/android-api.md`, `backend/app/api/v1/mobile.py`, `FieldCRMClient.kt`, and `MobileApiService.kt` must describe and consume the same routes, payloads, response fields, status codes, and error format.
   - Mobile currently has endpoints and fields that are not guaranteed by the backend contract.

3. **File handling mismatch**
   - Backend `documents` table supports file metadata: `doc_type`, `form_code`, `original_name`, `stored_path`, `mime_type`, `size_bytes`, quality status, verification state, uploader, and timestamps.
   - Mobile upload service can send binary multipart data, but the current mobile document route is documented as a mock/category record.
   - Website and mobile need one real upload format and one storage policy.

4. **Workflow parity**
   - Web supports intake, guarantor forms, document upload, OCR review, visitation, credit review, branch approval, return, audit, and dashboards.
   - Mobile has screens and ViewModels for much of this, but each role must be checked against the same stage rules and permissions as web.

5. **Offline sync risk**
   - Mobile sync queues server-authoritative application actions such as `CREATE_APPLICATION`.
   - Full parity requires queueing stage saves, guarantor saves, document metadata/uploads, visitation reports, credit review decisions, return actions, and notification state changes where allowed.
   - Workflow transitions must remain server-authoritative to prevent offline clients from bypassing approval rules.

## Target Architecture

### Backend

The backend remains the only authoritative write path.

- PostgreSQL is the source of truth.
- Domain services enforce workflow, role permissions, validation, audit logging, and notification creation.
- Mobile API routes call the same domain services as web routes.
- Web routes and mobile routes may have different presentation formats, but not different business rules.
- OpenAPI docs should be valid enough to generate or test mobile client models.

### Web

The web app continues to use:

- Session cookie authentication.
- Jinja templates.
- Existing role-specific shells and dashboards.
- Existing domain routers and services.

Required web changes should be limited to places where the web is using behavior that mobile cannot reproduce through the shared domain contract.

### Mobile

Mobile should use:

- Bearer-token authentication.
- `/api/v1/mobile` JSON APIs.
- SQLDelight as an offline cache and replay queue.
- Backend IDs, stage names, document category keys, form codes, and timestamps exactly as returned by the server.
- Server-wins conflict resolution for workflow stage and approval state.
- Client-wins or merge behavior only for draft form fields that have not been submitted into the workflow.

## Canonical Contracts To Define

Create or update a single canonical contract document, then implement from it.

### Entity Contracts

Define shared JSON schemas for:

- Current user and role.
- Dashboard metrics and role queues.
- Loan application list item.
- Loan application detail bundle.
- Intake stage data.
- Guarantor slot data.
- Pledged item data.
- Document metadata.
- OCR result and OCR field correction.
- Visitation report and signoff.
- Credit review.
- Approval readiness checklist.
- Return request.
- Notification.
- Audit/workflow event.
- App configuration and dropdown values.

### Enum Contracts

The same enum values must be used everywhere:

- Roles: `loan_officer`, `credit_officer`, `branch_manager`, `auditor`, `system_admin`.
- Stages: `intake`, `ocr_review`, `credit_review`, `branch_approval`, `disbursement_ready`, `disbursed`, `returned`, `rejected`.
- Customer types: `new`, `existing`.
- Loan types: `enterprise`, `msef`, `payee`, `other`.
- Repayment modes: `cheque`, `standing_order`, `direct_debit`, `cash_deposit`.
- Document quality statuses: `pending`, `clear`, `blurry`, `cropped`, `unreadable`.
- Guarantor form stages: `draft`, `submitted`, `ocr_review`, `verified`, `returned`.

### File Format Contracts

Define the accepted physical upload formats:

- Images: `image/jpeg`, `image/png`, optionally `image/webp`.
- Documents: `application/pdf`.
- Optional camera captures should be normalized before upload to JPEG or PDF.
- Every uploaded file must have `original_name`, `stored_path`, `mime_type`, `size_bytes`, `doc_type`, `form_code` where applicable, `loan_id`, `org_id`, `uploaded_by`, and `uploaded_at`.

Define document category keys:

- `loan_application_form` maps to `MMFB/CRM/01`.
- `pledge_form` maps to `MMFB/CRM/02`.
- `guarantor_form` maps to `MMFB/CRM/03`.
- Supporting categories include `payslip`, `id`, `statement`, `guarantor`, `other`, `passport_photo`, `id_card`, and `utility_bill`.

## Implementation Plan

### Phase 1: Contract Freeze

- Review `docs/android-api.md` against `backend/app/api/v1/mobile.py`.
- Add missing endpoints from the mobile service interface or remove unsupported mobile calls.
- Decide the exact response shape for application list and detail.
- Decide whether the backend exposes borrower compatibility endpoints or whether mobile removes borrower-first flows.
- Add explicit status codes and error response format.
- Add example payloads for every write route.
- Add document upload multipart specification.

Deliverables:

- Updated `docs/android-api.md`.
- API response examples for all role workflows.
- Backend/mobile contract checklist.

### Phase 2: Backend API Alignment

- Ensure every mobile route delegates to the same domain services used by web.
- Add or complete missing mobile routes for:
  - Application list filtering and pagination.
  - Full application detail bundle.
  - Intake save and submit behavior.
  - Guarantor get/save/submit behavior.
  - Real document multipart upload.
  - OCR corrections and verification.
  - Visitation report and manager signoff.
  - Credit review.
  - Approval readiness.
  - Approve and return.
  - Notifications.
  - Audit trail.
  - App config and dropdown values.
- Add role checks to mobile routes that exactly match web workflow permissions.
- Return consistent JSON errors with `detail` and `request_id`.

Deliverables:

- Backend route coverage for every web workflow needed on mobile.
- Tests for route permissions and stage transitions.
- Updated OpenAPI output.

### Phase 3: Database and Local Cache Alignment

- Treat PostgreSQL schema as canonical.
- Replace or evolve SQLDelight tables to cache backend-shaped records:
  - `MobileUserCache`
  - `LoanApplicationCache`
  - `StageDataCache`
  - `GuarantorCache`
  - `DocumentCache`
  - `VisitationCache`
  - `NotificationCache`
  - `WorkflowEventCache`
  - `SyncQueue`
- Store backend UUIDs as text in SQLDelight.
- Store stage names as text, not only numeric stage values.
- Store server timestamps and local dirty timestamps separately.
- Keep draft form payloads as JSON when the backend stores them in `stage_data.data_json`.
- Add SQLDelight migrations if existing installed users must keep local data.

Deliverables:

- New SQLDelight schema matching backend contracts.
- Mapping layer from API DTOs to local cache models.
- Offline cache migration plan.

### Phase 4: File Upload and Storage Parity

- Implement one backend upload endpoint used by both web and mobile.
- Accept multipart form data with:
  - `category`
  - optional `guarantor_id`
  - optional `slot`
  - `file`
- Validate MIME type, extension, size, and category.
- Generate deterministic server-side storage paths by org, loan, document category, and document ID.
- Store metadata in `documents`.
- Return the created document metadata as JSON.
- Update web upload templates to use the same service behavior if they still use mock uploads.
- Update mobile upload code to send the exact accepted multipart format.
- Decide how mobile queues uploads offline:
  - Store local URI and metadata in `SyncQueue`.
  - Upload binary file only when connectivity returns.
  - Mark failed uploads separately from failed JSON actions.

Deliverables:

- Real document upload endpoint.
- Web and Android upload parity.
- Document metadata tests.
- File size and MIME validation tests.

### Phase 5: Mobile Feature Parity

Implement or align mobile screens with web workflows by role.

Loan officer:

- Login.
- Dashboard.
- Application list and detail.
- Create application.
- Intake steps 1-9.
- Two guarantor slots and 8-step guarantor forms.
- Document upload.
- OCR review visibility where allowed.
- Visitation report.
- Returned application correction.
- Notifications.

Credit officer:

- Dashboard.
- Credit review queue.
- Application detail.
- OCR exceptions.
- Credit review submission.
- Return for correction.
- Notifications.

Branch manager:

- Dashboard.
- Awaiting concurrence.
- Pending signoffs.
- Pipeline.
- Visitation signoff.
- Approval readiness.
- Approve or return.
- Notifications.

Auditor:

- Dashboard.
- Audit trail.
- Compliance flags.
- Application detail.
- Checklist where supported.

System admin:

- Dashboard.
- User/admin views where mobile needs them.
- System control queue.
- System activity visibility.

Deliverables:

- Role-by-role mobile parity checklist.
- Screen-level acceptance tests.
- Manual QA scripts using seeded demo users.

### Phase 6: Offline Sync Rules

Define supported offline actions:

- Draft intake step saves.
- Draft guarantor step saves.
- Draft visitation report.
- Draft document metadata and pending file upload.
- Notification read state.

Keep these server-authoritative:

- Stage advancement.
- OCR verification.
- Credit review final submission.
- Branch approval.
- Return/rejection.
- Audit entries.
- User and role management.

Sync behavior:

- Queue actions in chronological order.
- Use idempotency keys for write requests.
- Replay queued actions after token validation.
- Pull latest server state after successful replay.
- Detect conflicts by server `updated_at` or workflow event version.
- Surface conflicts to the user when a draft cannot be safely merged.

Deliverables:

- `SyncQueue` action list and payload schema.
- Idempotency strategy.
- Conflict handling rules.
- WorkManager sync tests.

### Phase 7: Shared Validation and Formatting

- Move reusable enum lists, document category maps, and display labels into a shared backend config endpoint.
- Mobile should fetch app config after login and cache it.
- Web should use the same backend constants rather than duplicating option lists in templates where practical.
- Normalize dates and timestamps as ISO 8601 strings.
- Normalize currency values as numbers in JSON and format only in the UI.
- Normalize phone, BVN, account number, and ID number validation in backend services.

Deliverables:

- `/api/v1/mobile/config` contract and implementation.
- Reduced duplicated dropdown values.
- Validation test matrix.

### Phase 8: Testing and Quality Gates

Backend:

- Unit tests for domain services.
- API tests for every mobile route.
- Permission tests for every role.
- Workflow transition tests.
- Document upload validation tests.
- Migration smoke test.

Web:

- Route smoke tests for major pages.
- Form submission tests for application, guarantor, upload, OCR, visitation, credit review, approval, return.
- Role navigation checks.

Android:

- Compile checks.
- ViewModel tests for each workflow.
- Repository tests with fake API.
- SQLDelight migration tests.
- Offline queue replay tests.
- Manual emulator QA for each role.

End-to-end:

- Same seeded application is visible and actionable from both web and mobile.
- A document uploaded on web appears on mobile.
- A document uploaded on mobile appears on web.
- A return issued on web appears on mobile notifications.
- A mobile intake save is visible on web.
- A branch approval updates both clients.

Deliverables:

- Automated test suite.
- Manual QA checklist.
- Release readiness checklist.

## Suggested Work Order

1. Freeze API and file contracts.
2. Complete backend mobile routes and document upload behavior.
3. Align SQLDelight schema and Kotlin DTOs to backend contracts.
4. Update mobile repositories to use one API client path.
5. Implement missing role workflows in Android screens.
6. Add offline queue support for safe draft actions.
7. Add test coverage and seeded QA scripts.
8. Run full web/mobile parity QA.

## Acceptance Criteria

The work is complete when:

- Web and Android authenticate against the same backend user table.
- Web and Android read/write the same `loan_applications`, `stage_data`, `guarantors`, `documents`, `visitation_reports`, `workflow_events`, `notifications`, and audit records.
- No mobile-only borrower/application model is required for persisted business state.
- All file uploads use the same accepted MIME types, categories, form codes, and backend storage service.
- Every workflow transition has the same role rules on web and mobile.
- Offline mobile writes replay without bypassing server workflow authority.
- A seeded demo workflow can be started on one client and completed on the other.

## Resolved Defaults

- Android/shared sync should remove borrower-first server calls and persisted borrower business state. Temporary borrower UI models may be derived from canonical loan application data while screens are refactored.
- Physical document files are stored on the backend filesystem for this release.
- Accepted upload MIME types are `application/pdf`, `image/jpeg`, and `image/png`.
- Maximum upload size is 10 MB per file.
- Existing Android SQLDelight data can be cleared on the next development install; no local migration from the old borrower-shaped cache is required.

## Remaining Open Decisions

- Whether OCR processing is synchronous, asynchronous, or mocked for the next release.
- Whether Android must support every admin function or only field/back-office operational workflows.
