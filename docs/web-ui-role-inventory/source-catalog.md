# FieldCRM web template, CSS, and JavaScript catalog

Last verified: 26 August 2026

Coverage at verification: 124 Jinja2 HTML templates, 13 CSS assets, and 13 JavaScript/module assets under `frontend`. “Sketch” refers to `layout-sketches.md`. Functional details for visible controls are in `README.md`.

## Shell templates (4)

| Template | Sketch | Responsibility |
|---|---|---|
| `frontend/templates/base.html` | W01/W02/W07/W09 | Legacy authenticated shell: sidebar, top bar, content block, shared static assets |
| `frontend/templates/base/shell.html` | W01–W10 | Responsive authenticated document shell, role attribute, static asset order |
| `frontend/templates/base/desktop_shell.html` | W01–W10 | Role sidebar, desktop/mobile headers, search, notifications, profile, drawer behavior |
| `frontend/templates/base/mobile_shell.html` | W01–W10 | Compatibility alias for the responsive desktop shell |

## Role-owned page templates (52)

### Auditor (4)

| Template | Sketch | Surface |
|---|---|---|
| `frontend/templates/auditor/dashboard.html` | W01 | Auditor metrics and recent activity |
| `frontend/templates/auditor/compliance_flags.html` | W02/W09 | Compliance flag queue |
| `frontend/templates/auditor/audit_trail.html` | W09 | Immutable audit event table |
| `frontend/templates/auditor/application_detail.html` | W03 | Read-only application evidence and transitions |

### Team Lead (5)

| Template | Sketch | Surface |
|---|---|---|
| `frontend/templates/branch_manager/dashboard.html` | W01 | Team Lead dashboard |
| `frontend/templates/branch_manager/awaiting_concurrence.html` | W02 | Awaiting Me queue |
| `frontend/templates/branch_manager/pending_signoffs.html` | W02 | Visit signoff queue |
| `frontend/templates/branch_manager/pipeline.html` | W02 | Branch pipeline stages and records |
| `frontend/templates/branch_manager/application_detail.html` | W03/W04 | Evidence workstation and concurrence decision |

### Supervisor (2)

| Template | Sketch | Surface |
|---|---|---|
| `frontend/templates/branch_supervisor/dashboard.html` | W01 | Supervisor metrics and review work |
| `frontend/templates/branch_supervisor/review_queue.html` | W02 | Supervisory review queue |

### Credit Analyst (3)

| Template | Sketch | Surface |
|---|---|---|
| `frontend/templates/credit_analyst/dashboard.html` | W01 | Underwriting dashboard |
| `frontend/templates/credit_analyst/review_queue.html` | W02 | Underwriting queue |
| `frontend/templates/credit_analyst/ocr_exceptions.html` | W02/W08 | OCR exception queue |

### Credit Officer compatibility surfaces (4)

| Template | Sketch | Surface |
|---|---|---|
| `frontend/templates/credit_officer/dashboard.html` | W01 | Credit review dashboard compatibility page |
| `frontend/templates/credit_officer/review_queue.html` | W02 | Credit review queue compatibility page |
| `frontend/templates/credit_officer/ocr_exceptions.html` | W02/W08 | OCR exceptions compatibility page |
| `frontend/templates/credit_officer/application_detail.html` | W03/W05 | Credit risk workstation compatibility page |

### CRM and Head CRM (5)

| Template | Sketch | Surface |
|---|---|---|
| `frontend/templates/crm/dashboard.html` | W01 | CRM metrics and work summary |
| `frontend/templates/crm/crm_queue.html` | W02 | Dossier/disbursement review queue |
| `frontend/templates/crm/crm_review.html` | W05 | CRM or Head CRM decision desk |
| `frontend/templates/crm/disburse.html` | W05/W07 | Offer and disbursement recording |
| `frontend/templates/crm/record_payment.html` | W07 | Manual payment recording |

### Customer Identity (2)

| Template | Sketch | Surface |
|---|---|---|
| `frontend/templates/customers/new.html` | W07 | Staff customer creation and duplicate handling |
| `frontend/templates/customers/360.html` | W10 | Customer 360 aggregation |

### Executive, ED, MD, and MCC (11)

| Template | Sketch | Surface |
|---|---|---|
| `frontend/templates/executive/dashboard.html` | W01 | Compatibility executive dashboard |
| `frontend/templates/executive/executive_queue.html` | W02 | Executive instruction queue |
| `frontend/templates/executive/executive_approve.html` | W05 | Disbursement-instruction decision |
| `frontend/templates/executive/ed_dashboard.html` | W01 | ED dashboard |
| `frontend/templates/executive/ed_queue.html` | W02 | ED approval queue |
| `frontend/templates/executive/ed_approve.html` | W05 | ED approval/request-MD-input decision |
| `frontend/templates/executive/md_dashboard.html` | W01 | MD dashboard |
| `frontend/templates/executive/md_queue.html` | W02 | MD approval queue |
| `frontend/templates/executive/md_approve.html` | W05 | MD input/final decision and board opinion request |
| `frontend/templates/executive/mcc_index.html` | W02 | MCC dossier directory |
| `frontend/templates/executive/mcc_summary.html` | W05/W09 | MCC recommendations and final amount |

### Legal (2)

| Template | Sketch | Surface |
|---|---|---|
| `frontend/templates/legal/legal_queue.html` | W02 | Legal review queue |
| `frontend/templates/legal/valuation.html` | W07 | Collateral valuation form |

### Relationship Officer (7)

| Template | Sketch | Surface |
|---|---|---|
| `frontend/templates/loan_officer/dashboard.html` | W01 | Relationship Officer dashboard |
| `frontend/templates/loan_officer/queue.html` | W02 | My Queue |
| `frontend/templates/loan_officer/application_detail.html` | W03 | Application evidence, tasks, and links |
| `frontend/templates/loan_officer/document_upload_selector.html` | W02 | Eligible-application upload selector |
| `frontend/templates/loan_officer/ocr_review_queue.html` | W02/W08 | OCR review queue |
| `frontend/templates/loan_officer/visitation_reports.html` | W02/W09 | Saved visitation reports |
| `frontend/templates/loan_officer/visits.html` | W02 | Visit schedule |

### System Administrator (5)

| Template | Sketch | Surface |
|---|---|---|
| `frontend/templates/system_admin/dashboard.html` | W01 | System administration dashboard |
| `frontend/templates/system_admin/users.html` | W02/W07 | User, invitation, role, branch, deactivation management |
| `frontend/templates/system_admin/system_activity.html` | W09 | System activity reporting |
| `frontend/templates/system_admin/interest_presets.html` | W02/W07 | Interest preset directory/editor |
| `frontend/templates/system_admin/application_detail.html` | W03 | System-control application view |

### Operational tasks (2)

| Template | Sketch | Surface |
|---|---|---|
| `frontend/templates/tasks/my_work.html` | W02 | Phase 6 My Work buckets |
| `frontend/templates/tasks/exceptions.html` | W02/W09 | Central exception queue |

## Configuration Admin templates (9)

| Template | Sketch | Surface |
|---|---|---|
| `frontend/templates/configuration/base.html` | W11 | Dedicated configuration shell |
| `frontend/templates/configuration/mfa.html` | W12 | Localhost Configuration Admin TOTP step-up |
| `frontend/templates/configuration/hub.html` | W01/W11 | Configuration overview and version state |
| `frontend/templates/configuration/versions.html` | W02/W11 | Version history, validation, approval, publication |
| `frontend/templates/configuration/products.html` | W02/W11 | Draft product directory |
| `frontend/templates/configuration/product_edit.html` | W07/W11 | Draft product definition editor |
| `frontend/templates/configuration/forms.html` | W02/W11 | Dynamic form-field editor |
| `frontend/templates/configuration/documents.html` | W02/W11 | Document checklist editor |
| `frontend/templates/configuration/features.html` | W02/W11 | Feature-control groups and search |

## Shared page templates (30)

| Template | Sketch | Surface |
|---|---|---|
| `frontend/templates/shared/application_overview.html` | W10 | Read-only current-loan dossier |
| `frontend/templates/shared/application_wizard.html` | W06 | Nine-step application wizard and reviewer mode |
| `frontend/templates/shared/applications.html` | W02 | Shared application directory |
| `frontend/templates/shared/approve.html` | W04 | Approval readiness/concurrence workspace |
| `frontend/templates/shared/audit.html` | W09 | Shared audit report |
| `frontend/templates/shared/borrowers.html` | W02/W09 | Current Loans directory |
| `frontend/templates/shared/credit_review.html` | W05 | Credit analyst review and recommendation |
| `frontend/templates/shared/document_work_queue.html` | W02 | Document exception work queue |
| `frontend/templates/shared/error.html` | W12 | Generic safe error page |
| `frontend/templates/shared/forgot_password.html` | W12 | Password recovery request |
| `frontend/templates/shared/guarantor_wizard.html` | W06 | Guarantor stepped workflow |
| `frontend/templates/shared/loan_view.html` | W03/W10 | Read-only loan view |
| `frontend/templates/shared/login.html` | W12 | Staff login and onboarding hero |
| `frontend/templates/shared/new_application.html` | W07 | Customer/product selection before origination |
| `frontend/templates/shared/not_found.html` | W12 | Not-found status page |
| `frontend/templates/shared/notifications.html` | W02 | Notification list and read controls |
| `frontend/templates/shared/ocr_review.html` | W08 | Document/OCR correction workstation |
| `frontend/templates/shared/offer_letter_template.html` | W14 | Printable offer letter |
| `frontend/templates/shared/par_dashboard.html` | W09 | Portfolio-at-risk report |
| `frontend/templates/shared/pipeline.html` | W02 | Shared workflow pipeline |
| `frontend/templates/shared/public_home.html` | W13 | Public landing page |
| `frontend/templates/shared/public_terms.html` | W13 | Public legal/terms article |
| `frontend/templates/shared/repayment_feasibility.html` | W07/W09 | Collateral and repayment feasibility calculator |
| `frontend/templates/shared/repayment_schedule.html` | W09 | Repayment schedule/payment history |
| `frontend/templates/shared/reset_password.html` | W12 | Password/invitation completion |
| `frontend/templates/shared/return_page.html` | W07 | Structured workflow return |
| `frontend/templates/shared/search_results.html` | W02/W09 | Global search results |
| `frontend/templates/shared/settings.html` | W07 | Profile and password settings |
| `frontend/templates/shared/upload_document.html` | W07 | Protected document upload |
| `frontend/templates/shared/visitation.html` | W07 | Field visit report and concurrence |

## Components and partial templates (29)

### Components (25)

| Template | Sketch | Responsibility |
|---|---|---|
| `frontend/templates/components/application_document_checklist.html` | F02 | Required-document rows and preview states |
| `frontend/templates/components/application_flags.html` | F02 | Application exception/flag summary |
| `frontend/templates/components/application_readiness.html` | F02 | Readiness score, gates, and missing items |
| `frontend/templates/components/core_banking_summary.html` | F02 | CBS source, balances, timestamps, stale state |
| `frontend/templates/components/loan_recommendations.html` | F02 | Approval recommendation trail |
| `frontend/templates/components/mcc_sidebar_link.html` | F01 | Feature-aware MCC navigation link |
| `frontend/templates/components/desktop_sidebar_auditor.html` | F01 | Auditor navigation |
| `frontend/templates/components/desktop_sidebar_branch_manager.html` | F01 | Team Lead navigation |
| `frontend/templates/components/desktop_sidebar_branch_supervisor.html` | F01 | Supervisor navigation |
| `frontend/templates/components/desktop_sidebar_credit_analyst.html` | F01 | Credit Analyst navigation |
| `frontend/templates/components/desktop_sidebar_credit_officer.html` | F01 | Credit Officer compatibility navigation |
| `frontend/templates/components/desktop_sidebar_crm.html` | F01 | CRM/Head CRM navigation |
| `frontend/templates/components/desktop_sidebar_ed.html` | F01 | ED navigation |
| `frontend/templates/components/desktop_sidebar_legal.html` | F01 | Legal navigation |
| `frontend/templates/components/desktop_sidebar_loan_officer.html` | F01 | Relationship Officer navigation |
| `frontend/templates/components/desktop_sidebar_md.html` | F01 | MD navigation |
| `frontend/templates/components/desktop_sidebar_system_admin.html` | F01 | System Admin navigation |
| `frontend/templates/components/mobile_tabbar_auditor.html` | F01 | Auditor mobile tabs |
| `frontend/templates/components/mobile_tabbar_branch_manager.html` | F01 | Team Lead mobile tabs |
| `frontend/templates/components/mobile_tabbar_credit_officer.html` | F01 | Credit mobile tabs |
| `frontend/templates/components/mobile_tabbar_ed.html` | F01 | ED mobile tabs |
| `frontend/templates/components/mobile_tabbar_legal.html` | F01 | Legal mobile tabs |
| `frontend/templates/components/mobile_tabbar_loan_officer.html` | F01 | Relationship Officer mobile tabs |
| `frontend/templates/components/mobile_tabbar_md.html` | F01 | MD mobile tabs |
| `frontend/templates/components/mobile_tabbar_system_admin.html` | F01 | System Admin mobile tabs |

### Progressive response partials (4)

| Template | Sketch | Responsibility |
|---|---|---|
| `frontend/templates/partials/borrower_metrics.html` | F03 | Current-loan metric fragment |
| `frontend/templates/partials/borrower_rows.html` | F03 | Current-loan table rows |
| `frontend/templates/partials/par_summary.html` | F03 | PAR summary fragment |
| `frontend/templates/partials/par_loan_rows.html` | F03 | PAR table rows |

## CSS asset catalog (13)

| Asset | Scope and ownership |
|---|---|
| `frontend/static/css/web-ui-system.css` | Final semantic design system: action colors, fields, approval hierarchy, confirmation dialogs, accessibility, responsive behavior |
| `frontend/static/css/role-themes.css` | Unified plum variables for all role shells, sidebar state, role badge, focus, tab and metric accents |
| `frontend/static/css/dashboard.css` | Primary authenticated layout, shell, cards, tables, queues, dossier and approval foundations |
| `frontend/static/css/dashboard_legacy.css` | Compatibility copy used by older CRM/credit/executive/borrower templates; loaded before the final UI system |
| `frontend/static/css/csp-utilities.css` | Generated static replacements for CSP-safe template style declarations |
| `frontend/static/css/configuration-hub.css` | Configuration Admin shell, version/product/form/document/feature editors and MFA |
| `frontend/static/css/application-overview.css` | Current Loans dossier hero, tabs, two-column overview, documents, quick links, activity |
| `frontend/static/css/borrowers.css` | Borrower/current-loan directory filters, metrics, table/card responsive behavior |
| `frontend/static/css/login.css` | Login/onboarding split page, recovery cards, authentication states |
| `frontend/static/css/home.css` | Public landing and public terms layout |
| `frontend/static/css/motion.css` | Page/card/status transitions and reduced-motion overrides |
| `frontend/static/css/public-icons.css` | Small public icon primitives |
| `frontend/static/css/swagger-ui.css` | Vendored Swagger UI bundle styling; generated third-party asset, not a design-system editing target |

## JavaScript/module asset catalog (13)

| Asset | Pages/hooks | Responsibility and safety contract |
|---|---|---|
| `frontend/static/js/ui-system.js` | All authenticated shells; `data-confirm`, `data-set-recommendation` | Toasts, semantic confirmation dialog, focus trap, recommendation choice, inactivity logout; messages use `textContent` |
| `frontend/static/js/csrf.js` | All state-changing form/fetch surfaces | Injects session CSRF token into unsafe forms and same-origin fetch requests; never remove for visual work |
| `frontend/static/js/dashboard.js` | Authenticated shell; `data-href`, `data-form-choice`, sidebar/guide hooks | Keyboard/card navigation, sidebar icons, role guide, form chooser/workspace, submit feedback |
| `frontend/static/js/csp-styles.js` | Templates with `data-csp-style` | Applies server-generated allowlisted static style strings without enabling inline script |
| `frontend/static/js/motion.js` | `data-motion-page`, forms, status chips | Entry/reveal/loading/status animation with reduced-motion support |
| `frontend/static/js/progressive-loader.js` | `data-section-src`, `data-paginate-url` | Section fetch, sanitized insertion, cache, retries, pagination, performance measurement; strips unsafe markup/attributes |
| `frontend/static/js/document-preview.js` | `data-document-preview`, protected preview URLs | Accessible preview modal, protected PDF/image page streaming, cancellation, Escape/close behavior |
| `frontend/static/js/direct-document-upload.js` | Upload form authorization/finalize data attributes | Authorized direct cloud upload, progress, same-origin fallback, finalize call, safe errors |
| `frontend/static/js/configuration-hub.js` | `data-feature-search`, `data-feature-card/group` | Client-side feature-card filtering and empty state |
| `frontend/static/js/pdf.min.mjs` | PDF-capable pages | Vendored PDF.js runtime; third-party generated asset |
| `frontend/static/js/pdf.worker.min.mjs` | PDF-capable pages | Vendored PDF.js worker; third-party generated asset |
| `frontend/static/js/swagger-ui-bundle.js` | API documentation | Vendored Swagger UI runtime; third-party generated asset |
| `frontend/static/js/redoc.standalone.js` | API documentation | Vendored ReDoc runtime; third-party generated asset |

## Template-to-script loading rules

- `base/shell.html` always loads `csrf.js`, `dashboard.js`, `motion.js`, and `ui-system.js`.
- `base.html` loads `csp-styles.js`, `csrf.js`, document preview, motion, and `ui-system.js`.
- `base/shell.html` conditionally loads document preview when protected preview links exist.
- `base/shell.html` conditionally loads progressive loading when section/pagination data hooks exist.
- Upload pages attach `direct-document-upload.js` only when the configured upload contract is present.
- Configuration feature search loads `configuration-hub.js`.
- Template-local scripts still exist for page-specific dynamic fields and tabs; new shared behavior should move to an external asset.

## Coverage maintenance checklist

Run these from the repository root after UI changes:

```powershell
rg --files frontend/templates | Measure-Object
rg --files frontend/static/css
rg --files frontend/static/js
rg -n "web-ui-system.css|role-themes.css|dashboard.css" frontend/templates
```

Then verify the catalog count, assign every new template a sketch, document new colors/classes, parse all Jinja templates, and run focused rendering/responsive tests. Never run Vercel checks for this UI documentation workflow.
