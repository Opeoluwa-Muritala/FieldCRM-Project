# FieldCRM architecture overview

Evidence labels: **Confirmed** means directly supported by repository code; **Inferred** means strongly suggested by it; **Unverified** requires an external system.

## Executive summary

**Confirmed.** FieldCRM is one FastAPI application. Vercel's Python entry point is `backend.app.main:app`; FastAPI renders 108 Jinja templates from `frontend/templates` and mounts `frontend/static` at `/static`. There is no Node package, frontend build, React runtime, or independent static deployment.

The browser primarily uses ordinary same-origin links and HTML forms. Vanilla JavaScript provides progressive enhancement: navigation drawers, confirmation dialogs, loading states, document previews, wizard draft saves, client signing OTP, badge polling, and system-admin JSON actions. The backend returns HTML, redirects, JSON, streamed preview images, and redirects to signed Cloudinary downloads.

## Confirmed technology stack

| Item | Evidence and classification |
|---|---|
| Frontend | **Confirmed:** Jinja HTML, vanilla JS, CSS in `frontend/`; Montserrat is requested from Google Fonts. |
| Backend/API | **Confirmed:** FastAPI `>=0.110.0`, Pydantic settings/models, Python 3.12.11. |
| Package manager | **Confirmed:** pip-compatible `requirements.txt`; no JS package manager. |
| Database | **Confirmed:** PostgreSQL production configuration, SQLAlchemy async engine and hand-written SQL; SQLite/aiosqlite development fallback. |
| Authentication | **Confirmed:** JWT bearer or HttpOnly `session`/`__Host-session` cookie; PBKDF2-SHA256 and bcrypt verification. |
| Authorization | **Confirmed:** `get_current_user`, `RoleChecker`, organization IDs in repository queries, and client-session dependency. |
| Storage | **Confirmed:** authenticated Cloudinary in production; local `frontend/static/uploads` only outside production. |
| External APIs | **Confirmed:** QoreID, CreditRegistry, CRC, Youverify AML, Emailope-compatible JSON email gateway. |
| OCR/PDF | **Confirmed:** pytesseract, pdfplumber, pdf2image, Pillow; WeasyPrint. |
| Cache/rate limits | **Confirmed:** Redis optional for cache and required for production distributed rate limiting. |
| Analytics/logging/monitoring | **Confirmed:** standard Python logging only. **Unverified:** any Vercel-side analytics or monitoring. |
| Tests | **Confirmed:** pytest dependency and import/HTTP/render/responsive test scripts. No configured linter/type checker found. |

## Frontend structure and entry points

All feature templates inherit either `base.html` or the responsive `base/shell.html` through `base/desktop_shell.html` / `base/mobile_shell.html`. The responsive shell loads `dashboard.css`, `role-themes.css`, `dashboard.js`, `motion.js`, and `document-preview.js`. `base.html` loads `dashboard.css`, `motion.js`, and `document-preview.js`. Login adds `login.css`; borrower views add `borrowers.css`; motion styles are available in `motion.css`.

Template families inspected:

- `shared/`: login/reset, applications, borrower/search/audit/pipeline, staff/client application and guarantor wizards, documents, OCR, visitation, signing, repayment and settings.
- Role views: `loan_officer/`, `branch_manager/`, `branch_supervisor/`, `credit_analyst/`, `credit_officer/`, `crm/`, `committee/`, `executive/`, `legal/`, `auditor/`, and `system_admin/`.
- `components/`: role sidebars, mobile tab bars, MCC navigation and application flags.

There is no `fetch`-driven page bootstrap. **Confirmed:** FastAPI renders initial data into HTML. JavaScript then enhances it.

## Backend structure and entry points

`backend/app/main.py` creates `app = FastAPI(...)`, registers CORS and three custom middleware classes, configures Jinja/static paths, declares protected document delivery, registers auth/users/mobile routers under `/api/v1`, and registers the unprefixed loan/web router. Its lifespan initializes/disposes the database engine, Redis rate limiter and cache.

Major boundaries:

- Routers receive HTTP and enforce FastAPI validation/dependencies.
- Services implement authentication, user invitations, document handling, workflow decisions, external checks, OCR, email and PDFs.
- Repositories execute hand-written SQL through the shared SQLAlchemy/SQLite connection adapters.
- `DomainException` becomes JSON for APIs or browser redirect/template behavior through the later browser handler.

## Major confirmed user journeys

1. Login: `shared/login.html` → native `POST /login` → login rate limit → `AuthService.authenticate_user` → `UserRepository.get_by_email` → password verification → JWT → HttpOnly session cookie → 303 dashboard.
2. Staff application: dashboard/new application → server-rendered wizard → browser validation → form POST to `/applications/{id}/step/{step}` → auth/service/repository → database → 303 next step.
3. Document upload: file input → inline validation/progress XHR or native multipart → `UploadFile` route → `DocumentService.save_upload` → type/signature/size validation → Cloudinary → document/audit rows → redirect.
4. Preview/download: delegated document click → sequential `<img>` requests to protected preview → authorization and DB lookup → signed Cloudinary fetch proxied as PNG; download returns a signed-URL redirect.
5. Role workflow: queues/details → role-specific POST → `RoleChecker` → workflow/loan logic → transactional SQL/audit/notifications → redirect and rerender.
6. Client intake: signed share token → client-session cookie → client wizard and signing OTP → DB/signing evidence → success page.
7. Mobile: Android bearer token → `/api/v1/mobile/*` JSON endpoints → Pydantic models → role/organization-scoped data operations.

## Runtime boundaries

- **Browser runtime:** rendered HTML, CSS, vanilla JS, same-origin cookies and HTTP.
- **Vercel Python serverless runtime:** FastAPI request lifecycle, templates/static files, DB/external calls.
- **Database runtime:** pooled PostgreSQL in production; SQLite locally.
- **External runtime:** Redis, Cloudinary, email, identity/AML/credit APIs.
- **Build time:** Vercel installs root `requirements.txt` for Python 3.12.11.
- **Deployment state:** **Unverified** because no Vercel dashboard or deployment was queried.

## Findings

| Severity | Evidence | Location | Problem, impact, recommendation |
|---|---|---|---|
| High | **Confirmed** | `loan_officer/application_detail.html`, link-generation script | Calls `POST /applications/{id}/guarantor-link/{slot}`, but no route exists. Link generation fails (likely 404). Add/register the intended route and contract. |
| High | **Confirmed** | `shared/guarantor_sign.html`, form action | Posts to `/client-form/apply/guarantors/sign`, but no route exists. Guarantor signature submission cannot complete. Add/register the signing handler. |
| High | **Confirmed** | `DocumentService.save_upload` | OCR is launched with `asyncio.create_task`. A serverless instance can freeze/terminate after returning, so OCR may not finish. Use a durable queue or invoke OCR synchronously within limits. |
| Medium | **Confirmed** | `backend/app/domains/verification/service.py`, AML and credit service modules | Seed identities/names return mock outcomes before provider configuration. Production requests matching those values bypass real providers. Guard fixtures by non-production configuration or remove them. |
| Medium | **Confirmed** | deployment files | No `vercel.json` documents include/exclude rules, function duration, or explicit routing. Relative paths are robust, but bundling templates/static and time limits are **Unverified**. Add configuration only after checking actual Vercel project behavior. |
| Medium | **Confirmed** | OCR/PDF/document flows | OCR, image conversion, and PDF generation can be CPU/time intensive for serverless execution. Measure function duration and package size in preview. |
| Low | **Confirmed** | `SecurityHeadersMiddleware` | CSP permits inline scripts/styles and cdnjs; this is required by current templates but weakens CSP. Migrate inline scripts toward nonce/hash-based policy. |
| Informational | **Confirmed** | frontend | Most interactions are native forms, not `fetch`; this is intentional server-rendered architecture. |

## Inferred and unverified

- **Inferred:** Vercel packages sibling templates/static with the Python function because resolved paths point inside the repository.
- **Unverified:** production/preview domains, project root, install/build overrides, serverless duration, bundled files, environment values, Neon/Redis/Cloudinary reachability, and external-provider credentials/dashboard state.
