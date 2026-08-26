# FieldCRM web UI role and element inventory

This document is the functional UI specification for the server-rendered FieldCRM web application. It records what each visible or interactive element does and how each screen is arranged. Repeated elements are defined once under **Shared UI** and referenced by the role sections.

Last verified: 26 August 2026

Companion references:

- [Design system and exact element colors](design-system.md)
- [Desktop, tablet, mobile, page, and fragment arrangement sketches](layout-sketches.md)
- [Complete catalog of all HTML templates, CSS assets, and JavaScript modules](source-catalog.md)

## Legend

| Kind | Meaning |
|---|---|
| Link/row | Navigates with GET. A linked card or row makes the entire record selectable. |
| Button | Performs a local interaction or submits a form. |
| Input | Captures data. Required inputs block submission when empty. |
| Badge | Read-only state such as Verified, Pending, Returned, or stage ownership. |
| Metric | Read-only count or amount summarising live records. |
| Preview | Opens the protected document preview modal without exposing storage credentials. |
| Form action | Sends a POST and may change workflow or persisted data. |

## Global authenticated shell

Arrangement: persistent role-specific sidebar on the left, desktop top bar above the workspace, and responsive content beneath it. On narrow screens the sidebar becomes a drawer.

| Element | Kind | Information/action |
|---|---|---|
| Bank logo | Link | Returns to `/dashboard`. |
| Sidebar navigation | Links | Shows only destinations assigned to the signed-in role; active page is highlighted. |
| Sidebar count badges | Badges | Live queue counts for role-owned work. |
| Sidebar toggle / mobile menu | Button | Collapses or opens the navigation drawer. |
| Page title | Information | Names the current workspace. |
| Global search field | Input | Searches applications, borrowers, references, and officer names through `/search`. |
| Search button | Button | Submits the global search. |
| Notification bell | Link/badge | Opens `/notifications`; badge indicates unread items. |
| Profile identity | Link/information | Displays user name and role and opens `/settings`. |
| Logout | Link | Ends the authenticated session. |
| Offline banner | Information | Appears when connectivity is lost. |
| Toast/confirmation panel | Information/buttons | Reports errors/successes or asks for confirmation before consequential actions. |
| Skeletons/retry control | Information/button | Shows progressive loading; Retry Loading repeats a failed section request. |

## Shared record and workflow UI

### Queue and list pattern

Arrangement: page heading and filters first, metrics when relevant, then table rows or cards.

| Element | Kind | Information/action |
|---|---|---|
| Record card/row | Link | Opens the application or the role-appropriate review screen. |
| Applicant name | Information | Primary record identity. |
| Reference number | Information | Human-readable application reference. |
| Product/loan type | Information | Facility category. |
| Amount | Information | Requested, recommended, approved, or outstanding amount according to context. |
| Stage/status badge | Badge | Current workflow owner or record condition. |
| Officer/branch/date | Information | Ownership and age/context of the record. |
| Search/filter controls | Inputs/buttons | Narrow by text, stage, product, officer, or date when present. |
| Pagination/load more | Button/automatic trigger | Fetches the next record page without duplicating rows. |
| Empty state | Information | Explains that no matching work exists. |

### Application dossier/detail workstation

Arrangement: sticky left evidence column and larger right work column.

Left column elements:

- Identity card: reference number, borrower name, product, amount, and workflow status.
- Application Timeline: Intake & Submission, OCR Verification, Credit Risk Review, Branch Concurrence, and Further Review; each item shows completed/current/future state and a date or assignment note.
- Documents Checklist: Passport Photograph, Valid ID Card, Utility Bill, Bank Statement, Guarantor Form 1, Guarantor Form 2, and Pledge Agreement. Available items are preview links; each row ends with verified, review, or missing state.
- Verification & Screening: Qore ID/BVN result, CreditRegistry/CRC result, and Youverify AML result. Each integration reports verified, failed, awaiting, or not configured.

Right column varies by role, but may show readiness, credit recommendation, audit history, exceptions, checklist controls, or decision forms.

### Approval readiness workstation

Arrangement: sticky identity/readiness sidebar, grouped evidence cards, then a decision card.

| Element | Kind | Information/action |
|---|---|---|
| Borrower/ID/amount | Information | Identifies the application being approved. |
| Readiness percentage/progress | Metric | Structural completion and critical-item count. |
| Review dots | Information | Count of completed review checks. |
| Loan Application row | Clickable row | Opens the read-only wizard at Step 1. |
| Guarantor rows | Clickable rows | Open submitted guarantor evidence; unavailable forms are disabled and labelled Not Available. |
| Pledge row | Clickable row | Opens the uploaded protected PDF when present, otherwise the structured Step 8 form. |
| Supporting-document rows | Clickable rows | Open protected previews; status appears at the row end. |
| OCR exception row | Clickable row | Opens OCR correction/review. |
| Visitation concurrence row | Clickable row | Opens the field visitation report. |
| KYC attestation | Required checkbox | Confirms physical KYC sighting. |
| Collateral attestation | Required checkbox | Confirms collateral registry entry. |
| Recommended amount | Required number input | Sets the amount carried into the next workflow stage. |
| Return to Relationship Officer | Link | Opens the structured return-reason page. |
| Concur & Forward | Form action | Records concurrence and advances the application. |

### Staff application wizard

Arrangement: horizontal step indicator, one form card, Back/Next or Save controls at the bottom. Reviewer mode locks every data field and adds Back to approval review plus read-only navigation.

| Step | Information and inputs | Clickable/actions |
|---|---|---|
| 1 Applicant Details | Name, DOB, gender, marital status, phone, email, BVN/NIN, ID type/number/expiry, residential details, photograph | Photo chooser, Back/Next, Save Draft; BVN/expiry validation. |
| 2 Spousal Consent | Spouse identity/contact and consent information | Back/Next; skipped for Single applicants. |
| 3 Guarantors | Required guarantor slots and completion/signing state | Open Guarantor 1/2, generate signing links where available. |
| 4 Employment & Business | Employment type, employer/business, occupation, income, address, supporting proof | Employment choice toggles relevant field groups. |
| 5 Existing Facilities | Lender, facility type, amount, outstanding balance, repayment and tenor | Add Facility and Remove row controls. |
| 6 Loan Request | Amount, amount in words, tenor, purpose, sector, repayment mode, security | Purpose/security choices reveal additional fields; amount words are generated. |
| 7 Disbursement Account | Bank, account name/number and payout details | Back/Next with account-name warning. |
| 8 Pledge & Trust Receipt | Date, borrower, amount, location, obligor, pledged-item schedule, witnesses and signatures | Fill Form/Upload Completed Form tabs; add/remove pledged item; choose PDF/image; Save uploads the document and runs OCR. |
| 9 Review & Sign | Read-only consolidated application and signing state | Generate/copy signing link; submit signed application to Team Lead when eligible. |

OCR-filled pledge values are marked for manual comparison with the original uploaded document and do not overwrite existing staff-entered values.

### Guarantor wizard

Arrangement: step indicator, form body, signature/evidence area, footer navigation.

| Stage | Information/inputs |
|---|---|
| Identity | Full name, date of birth, BVN/NIN, phone, email, address, ID and photo. |
| Obligations | Existing guarantees and liabilities. |
| Family | Relationship and family/contact context. |
| Employment | Employer, occupation, income and work address. |
| Business & Documents | Business details and supporting documents. |
| Declaration | Accuracy/consent confirmations. |
| Guarantee Limit | Maximum guaranteed amount and facility acknowledgement. |
| Signatures | Guarantor/witness evidence and signing status. |

Back/Next/Save controls persist steps. Reviewer mode disables editing and provides a return link to approval review.

### Document preview and upload

- Upload chooser/drop zone opens the device file picker.
- Selected filename, validation, progress and completion/error states give upload feedback.
- Accepted source types are PDF, JPG/JPEG, and PNG within configured size limits.
- Previewable document rows open a modal; Close and Escape dismiss it.
- Multi-page PDFs load protected page images sequentially.
- Explicit download links use short-lived authorised URLs.

### OCR review

- Shows document/form type, extracted field, OCR value, confidence, critical flag, and verification state.
- Correction inputs capture final values.
- Verify/save controls persist reviewed values.
- Low-confidence and critical badges explain which fields require attention.

### Field visitation

- Displays borrower/application identity and existing report status.
- Inputs cover person met, premises, directions, visit date/time, relationship, business condition, officer names and signatures.
- Team Lead concurrence includes signature and return reason controls.
- Save/submit records the report; return/concur controls change its review condition.

### Return page

- Target role/title explains where the file will go.
- Reason category is required.
- Correction checklist records requested fixes.
- Notes are required.
- Cancel returns without mutation; Return Application advances the file backward and audits the reason.

## Relationship Officer (account_officer / loan_officer)

Sidebar arrangement: Dashboard, My Work, My Queue, New Application, Drafts, Returned, Visit Schedule, My Visitation Reports, Documents Needing Action, and feature-aware MCC.

| Screen | Informational elements | Clickable/editable elements and result |
|---|---|---|
| Dashboard | My applications, pending uploads, visits due, returned, OCR review and draft metrics; today’s tasks; queue; visits | Metric/task/record cards open filtered work, applications or visits; New Application starts intake. |
| My Queue | Applicant, reference, product, amount, stage, returned reason/age | Stage filter and application rows open the dossier. |
| New Application | New/existing customer choice, borrower selection, facility/product details | Customer and product cards select a path; Create/Generate Client Link starts an application or produces a share link. |
| Drafts/Returned | Same application list with stage context | Filters, application rows, resume/correction actions. |
| Application Detail | Identity, secure-link state, timeline, documents, screening, pending tasks, wizard progress, supporting intake | Resume step, upload document, preview evidence, generate/copy client or guarantor links, visitation, submit to Team Lead. |
| Visit Schedule | Due borrower, date, location, facility | Record row opens visitation form. |
| My Visitation Reports | Borrower, reference, visit date, amount, report status | Report row opens saved visitation evidence. |
| Upload Form | Eligible application cards | Selecting an application opens document upload. |
| OCR Review Queue | Applications with document count and OCR state | Row opens application/OCR results. |

## Team Lead (branch_manager)

Sidebar arrangement: Dashboard, Awaiting Me, Visit Signoffs, Exceptions Centre, Pipeline, Current Loans, and feature-aware MCC.

| Screen | Informational elements | Clickable/editable elements and result |
|---|---|---|
| Dashboard | Awaiting concurrence, pending signoffs and branch pipeline metrics; priority reviews and stage counts | Open/View all links, priority application cards, pipeline stage cards. |
| Awaiting Me | Applicant, reference, product, amount and review status | Application card opens full dossier/approval review. |
| Visit Signoffs | Borrower, visit date, officer and status | Row opens visitation concurrence. |
| Pipeline | Counts and applications grouped by assigned stage | Stage/record links open filtered pipeline or dossier. |
| Application Detail | Full shared dossier; structural readiness; recommendation; audit groups | Document previews, six checklist checkboxes, Return to Relationship Officer, Concur & Forward. |
| Approval Readiness | Full shared approval workstation | Read-only forms/evidence, attestations, amount, return and concurrence controls. |

## Supervisor (branch_supervisor)

Sidebar arrangement: Dashboard, Review Queue, Borrowers, and feature-aware MCC.

| Screen | Informational elements | Clickable/editable elements and result |
|---|---|---|
| Dashboard | Awaiting review and returned-this-week metrics; post-manager files | Open Review Queue, View all, application cards. |
| Review Queue | Files awaiting review after Team Lead clearance; applicant, reference, product, amount and Supervisory Review badge | Each card opens the approval readiness workstation. |
| Application Detail | Identity; complete timeline; seven-document checklist; Qore/CreditRegistry/Youverify states; structural checklist; credit recommendation; six verification audit items | Preview every available document; toggle audit items; Return to Relationship Officer; Concur & Forward. Layout is a sticky evidence sidebar plus review cards and decision footer. |
| Approval Readiness | Full shared approval workstation | Read-only forms/evidence, attestations, amount, return and concurrence controls. |
| Borrowers | Current-loan states and amounts | Search/filter and row links open read-only loan view. |

## Credit Analyst (credit_analyst)

Sidebar arrangement: Dashboard, Underwriting Queue, Document Exceptions, Exceptions Centre, Current Loans, and feature-aware MCC.

| Screen | Informational elements | Clickable/editable elements and result |
|---|---|---|
| Dashboard | Reviews due, OCR exceptions, reviewed today, returned this week; files and exceptions | Open/View all links and record cards. |
| Underwriting Queue | Borrower, reference, product, amount, exception count | Row opens Credit Risk Review. |
| OCR Exceptions | Borrower, document type, field and confidence | Row opens OCR review for that loan. |
| Credit Risk Review | Application summary, external screening, documents, DSR/affordability and exception evidence | Bureau pull when configured; recommendation decision, notes and recommended amount; submit advances or returns. |
| Current Loans | Read-only portfolio records and state | Filters and record links. |

## CRM Officer and Head CRM

Sidebar arrangement: Dashboard, Dossier Review Queue, Current Loans, Portfolio at Risk, and feature-aware MCC. Head CRM uses the same areas with final CRM authority labels.

| Screen | Informational elements | Clickable/editable elements and result |
|---|---|---|
| Dashboard | Queue counts, recent disbursements and portfolio information | Queue/view links, application/disbursement records. |
| Dossier Review Queue | Applicant, reference, amount, stage/age | Row opens CRM dossier review. |
| CRM Review | Applicant/reference, document checklist, previous recommendations and consent status | Document previews; supporting-document file/category upload; notes, amount, consent/decision controls; submit/return. |
| Disbursement | Applicant/reference and approval state | Generate Offer Letter; bank reference, amount/date and confirmation inputs; Disburse action. |
| Record Payment | Loan identity, schedule context and payment history | Amount/date/reference inputs; Record Collection. |
| Portfolio at Risk | PAR totals, CBN classifications and active-loan table | Classification/filter controls and loan links. |

## Executive Director (ed)

Sidebar arrangement: Dashboard, Approval Queue, MCC, PAR Report, Borrowers.

| Screen | Informational elements | Clickable/editable elements and result |
|---|---|---|
| Dashboard/Queue | Loans below ED threshold, applicant, reference and amount | Queue cards open ED approval. |
| ED Approval | Borrower/amount, MCC recommendations, document summary, earlier decisions | Preview documents; decision, notes and amount; approve, return or escalate according to workflow. |
| MCC Index/Summary | Approval dossiers, member recommendations, vote counts/final amount | Open dossier, submit recommendation, finalise amount when authorised. |
| PAR/Borrowers | Portfolio risk and current borrower states | Filters and record links. |

## Managing Director (md)

Sidebar arrangement: Dashboard, Approval Queue, MCC, PAR Report, Borrowers.

| Screen | Informational elements | Clickable/editable elements and result |
|---|---|---|
| Dashboard/Queue | MD approval count and dossier cards | Queue/application links open MD Approval. |
| MD Approval | Borrower, amount, MCC recommendations, document summary, requested input and board opinions | Document previews; notes/amount; final approval, return, or refer to board. |
| MCC | Dossiers, recommendations, member identity and final amount | Recommendation and finalisation actions. |
| PAR/Borrowers | Risk and portfolio state | Filters and record links. |

## Executive/disbursement role

| Screen | Informational elements | Clickable/editable elements and result |
|---|---|---|
| Dashboard | Awaiting disbursement instructions and PAR summary | Open queue/application/PAR links. |
| Executive Queue | Approved facility records awaiting instruction | Record opens Executive Approval. |
| Executive Approval | Applicant, amount and document summary | Preview evidence; instruction/reference/notes inputs; issue instruction or return. |

## Legal

Sidebar arrangement: Dashboard, Legal Review Queue, and feature-aware MCC.

| Screen | Informational elements | Clickable/editable elements and result |
|---|---|---|
| Legal Queue | Applications needing collateral valuation | Record opens valuation. |
| Valuation | Borrower, application details and pledged property rows | Appraised value, valuer, licence and date inputs per item; document preview; save/submit valuation. |

## Auditor

Sidebar arrangement: Dashboard, Compliance Flags, Audit Trail, Current Loans, and feature-aware MCC.

| Screen | Informational elements | Clickable/editable elements and result |
|---|---|---|
| Dashboard | Open flags, severity/count metrics and recent immutable activity | Open Flags, View all, Audit Trail and application links. |
| Compliance Flags | Application, rule/category, severity, status and creation time | Record opens read-only application evidence. |
| Audit Trail | Actor, role, action, entity/application, from/to state and timestamp | Search/filter controls and record links; no mutation controls. |
| Application Detail | Identity, timeline, documents, screening, compliance evidence, state transitions and upload verification log | Protected previews and navigation only. |
| Current Loans | Portfolio states | Search/filter and read-only record links. |

## System Administrator

Sidebar arrangement: Dashboard, Users, System Activity.

| Screen | Informational elements | Clickable/editable elements and result |
|---|---|---|
| Dashboard | User/activity/role counts and system-control queue | Manage Users, user/activity cards and application links. |
| Users | Team count; name, email, phone, role, branch and active state; workflow role reference | Invite fields/button; role selector and Save; two-step Deactivate; branch name/code/address/manager inputs and Create Branch. |
| System Activity | Login/activity summaries and recent audit events | Manage Users and linked entities where supplied. |
| Interest Presets | Existing rates, products, tenors and active state | Product/rate/tenor inputs; Create Preset; Delete preset. |
| Application Detail | Full evidence sidebar, final oversight and verification summary | Document preview and permitted system-control actions. |

## Customer Identity

Customer Identity is feature-flagged and staff-only. System Admin does not gain customer PII access from its administrative role.

| Screen | Informational elements | Clickable/editable elements and result |
|---|---|---|
| New Customer | Legal name, identity/contact/address/classification fields, probable duplicate results | Create customer; duplicate matches stop persistence unless the authorized staff member enters a typed override reason. |
| Customer 360 | Identity header, applications, current loans, CBS exposure, repayments, visits, documents, guarantors, collateral, credit evidence, communications and activity | Section navigation and tenant/object-authorized record/document links; no hidden-UI authorization assumptions. |

## My Work and Exceptions Centre

These Phase 6 surfaces are feature-aware and appear only when enabled for the role.

| Screen | Informational elements | Clickable/editable elements and result |
|---|---|---|
| My Work | Work buckets, task identity, case/application, ownership, due/age state | Bucket filters and authorized record links open the underlying work item. |
| Exceptions Centre | Exception category, severity/state, application, owner and age | Filters and authorized record links open contextual resolution surfaces. |

## Configuration Administrator

Configuration Admin is a separate localhost-only, non-production control plane with dedicated role, encrypted TOTP, short-lived step-up session and maker-checker controls.

| Screen | Informational elements | Clickable/editable elements and result |
|---|---|---|
| MFA | Control-centre identity and step-up guidance | TOTP input verifies the dedicated Configuration Admin session. |
| Hub | Current/effective version, working draft, readiness and configuration areas | Open Versions, Products, Forms, Documents or Features; Exit returns to the operational shell. |
| Versions | Creator, validator, approver, publisher, reasons, effective timestamps and immutable history | Create/validate/approve/publish actions appear only when policy allows; high-risk approval requires a different person. |
| Products | Draft product cards, status, limits and configuration completeness | Add/open a working-draft product; published definitions are not edited in place. |
| Product Editor | Rules, sections, checks, SLA, workflow, approval limits and CBS applicability | Edit allowlisted draft-bound fields and save the working draft. |
| Form Fields | Field type, label, help, validation, visibility and required state | Add/edit/reorder draft-bound fields; patterns come from the server allowlist. |
| Document Checklists | Document type, required state, percentage gates and quality expectations | Add/edit/reorder draft-bound checklist entries. |
| Feature Controls | Grouped operational switches and current values | Search feature cards and update only the current working draft. |

## Public/client UI

| Screen | Informational elements | Clickable/editable elements and result |
|---|---|---|
| Staff Login | Bank identity, sign-in guidance and error state | Email/phone and password; Sign In; Forgot Password. |
| Forgot/Reset Password | Recovery or invitation context and validation guidance | Identifier/password/confirmation inputs; submit; back to login. |
| Client Start | Borrower/application context, privacy and process guidance | Start/continue application. |
| Client Wizard | Same intake sections needed from the borrower, review/consent summary and signature | Step navigation, inputs, uploads, consent checkboxes, OTP/signing controls and final submit. |
| Client Upload | Requested document type and upload constraints | Choose file and Upload. |
| Client Guarantor Wizard | Guarantor identity and step progress | Guarantor inputs, step controls and signing actions. |
| Guarantor Sign | Document identity, viewing confirmation, OTP and signature status | Confirm viewed, request/verify OTP, signature canvas and submit. |
| Success/Error | Submission receipt or inactive-link explanation | Return/close/retry/contact guidance links when supplied. |

## Other shared information screens

| Screen | Information | Actions |
|---|---|---|
| Applications | Application table, stages, borrower, amount, dates | Search/filter, row open, pagination. |
| Current Loans/Borrowers | State metrics and loan list | Search/filter, read-only row open. |
| Loan View | Applicant/facility summary and documents | Protected document preview. |
| Pipeline | Stage columns, counts and application cards | Stage/application navigation. |
| Search Results | Query and grouped application/borrower results | Result links. |
| Notifications | Message, type, read state and timestamp | Open target, mark read, clear notifications. |
| Settings | Profile identity/role and security status | Current/new/confirm password; Change Password; logout navigation. |
| Workflow Events | Chronological stage/action/actor/reason log | Application links where present; otherwise read-only. |
| Repayment Schedule | Instalment number, due date, principal, interest, total, paid/balance/status and payment history | Record Payment when role permits. |
| Not Found | Missing-page explanation | Return to dashboard/back link. |

## Source template coverage

This inventory covers the web templates under:

- `frontend/templates/base`: authenticated shells.
- `frontend/templates/components`: every desktop sidebar, mobile tab bar and application flag component.
- `frontend/templates/loan_officer`, `branch_manager`, `branch_supervisor`, `credit_analyst`, `credit_officer`, `crm`, `executive`, `legal`, `auditor`, and `system_admin`: role-owned pages.
- `frontend/templates/shared`: applications, wizards, approval, audit, borrowers, client flows, credit/OCR review, loan view, authentication, notifications, PAR, pipeline, repayment, return, search, settings, upload and visitation.
- `frontend/templates/partials`: borrower and PAR metric/row fragments.

When a new clickable or informational element is added to a template, update the relevant screen row here. When a shared component changes, update the shared definition and verify every role that references it.
