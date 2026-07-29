# FastAPI route map

Evidence: **Confirmed** from decorators and router registration. `loans_router` has no prefix; auth/users/mobile prefixes are registered in `main.py`.

## Core, authentication and users

| Method | Route | Handler/file | Authentication | Request model | Main dependencies | Response |
|---|---|---|---|---|---|---|
| GET | `/`, `/login`, `/forgot-password`, `/reset-password`, `/accept-invitation` | `main.py` handlers | mixed/public | query | DB where needed | 303 or HTML |
| POST | `/login`, `/forgot-password`, `/reset-password` | `main.py` handlers | public/rate-limited | Form | DB, Auth/User service | HTML or 303/cookie |
| GET | `/logout` | `main.py:logout` | cookie optional | query | none | 303, delete cookie |
| GET | `/api/v1/health` | `main.py:health_check` | public | none | none | JSON |
| GET | `/api/v1/documents/{document_id}/download`, `/preview` | `main.py` | required | path/query | user + DB + Cloudinary | redirect / PNG stream |
| POST | `/api/v1/auth/login`, `/login-bearer`, `/login-mobile` | auth router | public | OAuth2 form | AuthService | Token JSON |
| POST | `/api/v1/auth/logout` | auth router | required | none | current user | JSON |
| POST | `/api/v1/users/register-org` | users router | registration secret | JSON | UserService | `UserResponse` |
| POST | `/api/v1/users/register-user`, `/invitations` | users router | role restricted | JSON | UserService/email | user or JSON 201 |
| PUT | `/api/v1/users/{user_id}/role` | users router | System Admin | `UserRoleUpdate` | UserService | JSON |
| POST | `/api/v1/users/{user_id}/deactivate` | users router | System Admin | path | UserService | JSON |

## Server-rendered web and workflow routes

All protected routes use `get_current_user`, `RoleChecker`, or client-session dependencies as declared. GET routes usually return `TemplateResponse`; POST routes usually return 303 redirects, with a few JSON link/signing actions.

| Methods | Routes | Handler area | Main calls | Response |
|---|---|---|---|---|
| GET | `/dashboard`, `/my-queue`, `/visits`, `/visitation-reports`, `/document-upload`, `/ocr-review-queue` | role dashboards/queues | dashboard SQL/services | HTML/303 |
| GET | `/awaiting-me`, `/pending-signoffs`, `/supervisory-review-queue`, `/my-reviews`, `/ocr-exceptions` | review queues | scoped SQL | HTML |
| GET | `/audit-trail`, `/compliance-flags`, `/users`, `/system-activity` | audit/admin | audit/user queries | HTML |
| GET/POST | `/applications`, `/applications/new`, `/applications/{application_id}` | application list/create/dispatch | LoanService/repository | HTML/303 |
| GET/POST | `/applications/{application_id}/step/{step}` | staff wizard | LoanService, verification/AML | HTML/303 |
| POST | `/applications/{application_id}/submit-to-branch-manager` | workflow submit | workflow SQL/audit | 303 |
| GET/POST | `/applications/{application_id}/guarantors/{guarantor_index}/step/{step}` | guarantor wizard | GuarantorService | HTML/303 |
| GET/POST | `/applications/{application_id}/documents/upload` | document upload | DocumentService | HTML/303 |
| POST | `/applications/{application_id}/crm-upload` | CRM upload | DocumentService | 303 |
| GET/POST | `/applications/{application_id}/ocr-review` | OCR review | OCR SQL | redirect/303 |
| GET/POST | `/applications/{application_id}/visitation` | visit/signoff | VisitationService | HTML/303 |
| GET/POST | `/applications/{application_id}/credit-review` | credit analysis | bureau/AML/workflow SQL | HTML/303 |
| POST | `/applications/{application_id}/credit-bureau-pull`, `/checklist` | bureau/checklist | credit providers/SQL | 303 or JSON |
| GET/POST | `/applications/{application_id}/approve`, `/return` | branch decision | workflow logic | HTML/303 |
| GET | `/forms`, `/pipeline`, `/borrowers`, `/applications/{application_id}/view` | shared views | scoped queries | 303/HTML |
| GET/POST | `/notifications`, `/notifications/{notification_id}/read`, `/notifications/clear` | notifications | notification SQL | HTML/303 |
| GET/POST | `/settings`, `/settings/change-password` | settings | security/user SQL | HTML |
| GET | `/search`, `/audit` | search/audit | scoped SQL | HTML |
| GET/POST | `/crm-review-queue`, `/applications/{id}/crm-review` | CRM review | workflow SQL | HTML/303 |
| GET/POST | `/executive-queue`, `/applications/{id}/executive-approve` | executive review | workflow SQL | HTML/303 |
| GET/POST | `/committee-queue`, `/applications/{id}/committee-review`, `/committee-vote`, `/committee-complete` | committee | vote/workflow SQL | HTML/303 |
| GET/POST | `/ed-queue`, `/applications/{id}/ed-approve` | ED | workflow SQL | HTML/303 |
| GET/POST | `/md-queue`, `/applications/{id}/md-approve`; POST `/md-refer-board` | MD | workflow SQL | HTML/303 |
| GET/POST | `/applications/{id}/disburse` | disbursement | loan servicing SQL | HTML/303 |
| GET/POST | `/legal-queue`, `/applications/{id}/valuation` | legal | valuation SQL | HTML/303 |
| GET/POST | `/mcc`, `/applications/{id}/mcc`, `/mcc-vote`, `/mcc-finalize` | MCC | vote/final amount SQL | HTML/303 |
| GET/POST | `/admin/interest-presets`, `/admin/interest-presets/{id}/delete` | admin pricing | SQL | HTML/303 |
| POST | `/applications/{id}/generate-offer` | offer generation | PDF/Cloudinary/SQL | 303 |
| GET/POST | `/applications/{id}/repayment-schedule`, `/payments` | servicing | servicing service/SQL | HTML/303 |
| GET | `/reports/par`, `/crm-dashboard`, `/executive-dashboard`, `/ed-dashboard`, `/md-dashboard`, `/committee-dashboard` | reporting dashboards | dashboard/service SQL | HTML |
| POST | `/loans/generate-share-link` | share intake | signed token/SQL | JSON |
| GET/POST | `/share-intake/{token}`, `/share-intake/{token}/start` | public intake start | token validation/DB | HTML/303 |
| POST | `/applications/{id}/client-link` | staff client link | token/DB | JSON |
| GET | `/client-access/{token}` | client entry | token/DB | HTML/303 + cookie |
| POST | `/client-form/signing/otp/start`, `/viewed`, `/otp/verify` | signing evidence | client session/signing service | JSON |
| GET/POST | `/client-form/apply/step/{step}` | client wizard | client session/SQL | HTML/303 |
| GET/POST | `/client-form/apply/documents/upload` | client upload | client session/DocumentService | HTML/303 |
| GET/POST | `/client-form/apply/guarantors/{guarantor_index}/step/{step}` | client guarantor | client session/SQL | HTML/303 |
| GET | `/client-form/success` | completion | client session | HTML/cookie cleanup |

## Mobile JSON API (`/api/v1/mobile`)

| Methods | Route families | Models/dependencies | Response |
|---|---|---|---|
| GET | `/me`, `/dashboard`, `/config`, `/faqs`, `/onboarding`, `/search` | Bearer `get_current_user`; `MobileUserResponse` where declared | JSON |
| GET/PATCH/DELETE | `/notifications`, `/notifications/{id}/read` | Bearer + DB | JSON |
| GET | `/queues/{queue_name}`, `/borrowers`, `/applications` | Bearer, role/org filters, query params | JSON |
| POST | `/borrowers`, `/applications` | `MobileBorrowerRequest`, `CreateApplicationRequest` | JSON 201 |
| GET | `/applications/{id}`, `/intake`, `/guarantors/{slot}`, `/documents`, `/ocr-fields`, `/visitation` | Bearer + DB | JSON |
| PUT | `/applications/{id}/intake/steps/{step}`, `/guarantors/{slot}/steps/{step}`, `/visitation` | save-step/visitation models | JSON |
| POST | `/applications/{id}/documents` | multipart `UploadFile` | JSON |
| POST | `/applications/{id}/ocr-review`, `/ocr-corrections` | OCR request models | JSON |
| POST | `/applications/{id}/visitation/signoff`, `/credit-review`, `/approve`, `/return` | role-specific request models | JSON |
| GET/PATCH | `/applications/{id}/approval-readiness`, `/audit`, `/bureau`, `/committee-votes`, `/audit-checklist` | Bearer + role/org checks | JSON |
| GET/POST | `/applications/{id}/crm-review`, `/executive-review`, `/committee-votes-full`, `/ed-review`, `/md-review` and related decision routes | Bearer + role checks | JSON |
| POST | `/applications/{id}/committee-vote`, `/committee-complete`, `/executive-approve`, `/ed-approve`, `/md-approve`, `/md-refer-board`, `/workflow/advance` | JSON/form + role checks | JSON |
| GET/POST | `/applications/{id}/repayment-schedule`, `/payments`; GET `/reports/par` | Bearer + servicing SQL | JSON |
| POST | `/generate-share-link` | Bearer Loan Officer | JSON |
| GET/POST | `/users` | System Admin role | JSON / 201 |
| POST | `/auth/forgot-password`, `/auth/reset-password` | JSON/form request fields | JSON |

## Router registration check

`workflow`, `visitation`, `documents`, `audit`, `guarantors`, and `ocr` each contain placeholder `APIRouter()` modules with no declared routes and are not registered. This is not a broken route by itself because active behavior lives in `loans/router.py` and `mobile.py`. The two missing browser receivers documented in the contract map are confirmed issues.
