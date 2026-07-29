# Frontend function map

All entries are **Confirmed** unless marked otherwise.

## JavaScript functions

| Function | File | Trigger | Reads from | Calls | API endpoint | Updates | Errors |
|---|---|---|---|---|---|---|---|
| DOMContentLoaded bootstrap | `static/js/dashboard.js` | DOM ready | role shell, forms, badge elements | initialization functions | dynamic `data-badge-url` GET | icons, tour, drawers, loading states, badges | polling catches failures silently |
| `bindFormChoices` / `openFormWorkspace` | `dashboard.js` | `[data-form-choice]` click | card datasets | render manual/upload workspace | none | inline generated form UI | guarded missing nodes |
| `saveFormDraft` | `dashboard.js` | generated Save Draft click | generated workspace | local status update | none | `#dashboardInlineStatus` | no persistence; UI-only draft |
| `startTrainingTour` / navigation | `dashboard.js` | guide control / first visit | role and target IDs | tooltip positioning | none | tour DOM | filters missing targets |
| `startBadgePolling` | `dashboard.js` | DOM ready + interval | `data-badge-url` | `fetch` with same-origin credentials | URL supplied by HTML | badge text/visibility | caught; previous value remains |
| motion bootstrap | `motion.js` | DOM ready/form submit | forms/cards/status chips | validity check, observers | none | loading/reveal classes | none |
| `renderPreviewPages` | `document-preview.js` | delegated document click | preview URL/title | sequential image loads | `GET /api/v1/documents/{id}/preview?page=N` | modal and page images | user-visible failure text + console |
| `validateWizardForm` and step helpers | staff/client wizard inline JS | form submit/change/load | wizard inputs/canvas | field checks, signature serialization | native step POST; draft `fetch(form.action?draft=1)` | errors, computed words, dynamic rows | blocks submit and marks fields |
| upload handlers | `shared/upload_document.html` | file change/submit | `#fileInput`, form | `XMLHttpRequest`, `FormData` | current multipart form action | progress, filename, error/success | status and retry UI |
| `requestOtp` / `verifyOtp` | `shared/guarantor_sign.html` | buttons | OTP input/session | `fetch` | signing viewed/start/verify | OTP status and enabled signature form | response error status |
| invitation submit | `system_admin/users.html` | `#invite-user-form` submit | `FormData` | JSON `fetch` | `POST /api/v1/users/invitations` | status/reset/reload | error status |
| role edit/save | `system_admin/users.html` | data buttons | row user ID/select | JSON `fetch` | `PUT /api/v1/users/{id}/role` | select/reload | global toast |
| deactivate user | `system_admin/users.html` | data button twice | row user ID | `fetch` | `POST /api/v1/users/{id}/deactivate` | confirmation/reload | global toast |
| `generateClientLink` | `new_application.html` | `#generateLinkBtn` click | borrower/loan selections | JSON parsing | `POST /loans/generate-share-link` | share-link field | inline error |
| client link handlers | loan detail/view inline JS | generate buttons | rendered application ID | `fetch` | `POST /applications/{id}/client-link` | link display/clipboard | inline error |
| guarantor link handler | `loan_officer/application_detail.html` | guarantor link buttons | app ID/slot | `fetch` | missing `/applications/{id}/guarantor-link/{slot}` | intended link display | **Confirmed broken route** |
| checklist toggle | `branch_manager/application_detail.html` | checkbox change | app ID/check item | JSON `fetch` | `POST /applications/{id}/checklist` | checkbox state | rollback/message |
| shell confirmation | `base/shell.html` | delegated `[data-confirm]` click | data message | replays confirmed click | action-specific | confirmation panel | none |
| inactivity handler | `base/shell.html` | activity/15-minute timeout | current URL | location redirect | `GET /logout?next=...` | navigation | none |

## HTML interactions

| HTML page/family | Element | Event | Listener | Handler | Result |
|---|---|---|---|---|---|
| `shared/login.html` | `#loginForm` | submit | inline listener + motion listener | native form submission | POST login, loading animation |
| shell templates | navigation links, menu buttons | click | inline/external | drawer/navigation handlers | server-rendered GET or drawer state |
| dashboards/queues | table rows/links | click | inline `onclick` | `window.location` | detail/queue GET |
| `shared/new_application.html` | customer/loan cards; generate button | click | inline | selection + `generateClientLink` | create app/share intake path |
| staff/client application wizard | `#wizardForm`, inputs, canvas | submit/change/pointer | inline | validators, dynamic fields, signature drawing | step POST or local DOM update |
| guarantor wizard | wizard form/canvas | submit/pointer/load | inline | validator/signature/sessionStorage | step POST |
| upload pages | file input/drop zone/form | change/click/submit | inline | file checks + XHR/native form | multipart upload/progress |
| document-bearing tables | row/link | delegated click | `document-preview.js` | `renderPreviewPages` | protected modal preview |
| role review/approval pages | forms/buttons/radios | submit/change/click | native/inline | toggle panels and native POST | workflow transition/redirect |
| `system_admin/users.html` | invite/role/deactivate controls | submit/click | inline listeners | JSON handlers | status/reload |
| `shared/guarantor_sign.html` | confirmation, OTP, signature form | change/click/submit | inline | OTP/validation/signature handlers | signing evidence; final action currently missing |

## Template and asset inventory

Every `.html` file under `frontend/templates` was included in the inspection. There are 108 templates across `base`, `components`, `shared`, and the role directories listed in the overview. Five first-party stylesheets exist: `dashboard.css`, `role-themes.css`, `login.css`, `borrowers.css`, `motion.css`. Three first-party scripts exist: `dashboard.js`, `motion.js`, and `document-preview.js`; `pdf.min.mjs` is a vendored generated asset and was not treated as application logic.

No WebSocket use, XMLHttpRequest outside document upload, browser authentication-token storage, or JavaScript cookie parsing was found. `localStorage` stores the role usage-guide completion key; `sessionStorage` stores wizard previous-step keys. Authentication is cookie-based for the web UI.
