# FieldCRM Web Frontend

This folder contains FieldCRM's server-rendered web interface. It provides
role-aware, responsive screens for loan officers, credit officers, branch
managers, auditors, and system administrators.

The frontend is built with Jinja2 templates, vanilla CSS, and vanilla
JavaScript. It does not run as a separate development server: the FastAPI
application in `../backend` renders the templates and serves the static files.

## Technology

- Jinja2 templates
- HTML5
- Vanilla CSS
- Vanilla JavaScript
- FastAPI static-file and template integration

## Folder Structure

```text
frontend/
|-- static/
|   |-- css/                 Shared, responsive, login, and role theme styles
|   |-- img/                 Logos and other image assets
|   `-- js/                  Dashboard and mobile interactions
`-- templates/
    |-- base/                Desktop, mobile, and shared page shells
    |-- components/          Role-specific sidebars, tab bars, and top bars
    |-- shared/              Shared workflow and form pages
    |-- loan_officer/        Loan officer pages
    |-- credit_officer/      Credit officer pages
    |-- branch_manager/      Branch manager pages
    |-- auditor/             Auditor pages
    `-- system_admin/        System administrator pages
```

## Run the Frontend

Set up and start the backend from the repository root:

```powershell
python -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -r backend\requirements.txt
uvicorn app.main:app --app-dir backend --reload
```

Then open:

- Login: `http://127.0.0.1:8000/login`
- Dashboard after login: `http://127.0.0.1:8000/dashboard`

The backend expects its database and environment variables to be configured.
See `../backend/README.md` for the full setup and seeded demo accounts.

## How Rendering Works

FastAPI configures these directories in `backend/app/main.py`:

```python
templates = Jinja2Templates(directory="frontend/templates")
app.mount("/static", StaticFiles(directory="frontend/static"), name="static")
```

Requests are rendered according to both the authenticated user's role and the
request's device type:

- Desktop requests use a desktop shell and role-specific sidebar.
- Mobile requests use a mobile shell, top bar, and role-specific tab bar.
- Shared workflow pages are reused where the process is common across roles.
- Role-specific dashboards and detail views isolate actions and information.

## Template Conventions

- Put reusable layout markup in `templates/base/`.
- Put reusable navigation in `templates/components/`.
- Put workflow pages used by multiple roles in `templates/shared/`.
- Put role-only views in the matching role directory.
- Extend the existing shells instead of copying full HTML page structures.
- Use Jinja variables supplied by the backend; do not place business logic in
  templates.

Typical page structure:

```jinja2
{% extends "base/desktop_shell.html" %}

{% block title %}Page title{% endblock %}

{% block content %}
    <!-- Page content -->
{% endblock %}
```

Check an existing page in the same role before adding a new one because some
shell blocks and context names differ between desktop and mobile views.

## Styling

The main stylesheets are:

| File | Purpose |
| --- | --- |
| `static/css/dashboard.css` | Core desktop layout, components, and workflow UI |
| `static/css/mobile.css` | Mobile shells and responsive behavior |
| `static/css/role-themes.css` | Accent colors and role-aware presentation |
| `static/css/login.css` | Login page |
| `static/css/borrowers.css` | Borrower-related views |

The authenticated role is added to the document as a `data-role` attribute.
Prefer extending this mechanism when adding role-specific visual treatment.

## JavaScript

- `static/js/dashboard.js` handles role shells, guided tours, generated forms,
  badge polling, UI effects, and common dashboard behavior.
- `static/js/mobile.js` contains mobile-specific behavior.

Keep server data and authorization decisions in the backend. Frontend checks
may improve presentation, but they must not be treated as access control.

## Screen Navigation

Navigation is role-aware. Desktop users navigate with the left sidebar, while
mobile web users navigate with the bottom tab bar. Workflow screens may hide
the normal navigation so the user completes or exits the current task first.

### Shared Entry and Core Screens

| Screen | Route | How to open it | Main navigation |
| --- | --- | --- | --- |
| Login | `/login` | Open the site, visit `/`, or follow an authentication redirect | Successful login opens the requested `next` page or `/dashboard`. |
| Dashboard | `/dashboard` | Login, Home tab, or Dashboard sidebar item | Opens the role's queues and shortcuts. Logout returns to Login. |
| Applications | `/applications` | Dashboard links, application-related sidebar items, or mobile Upload/Applications tab | Select an application to open its detail/workflow; New Application opens `/applications/new`. |
| New Application | `/applications/new` | New Application sidebar item, mobile New tab, floating action button, or Applications page | Submit customer and loan type to create a draft and open intake Step 1. Intended for Loan Officers and System Admins. |
| Pipeline | `/pipeline` | Pipeline sidebar/tab or dashboard pipeline links | Select a stage/card to filter or open its applications. Branch Managers receive a branch-specific pipeline view. |
| Current Loans | `/borrowers` | Current Loans/Loans navigation for Branch Manager, Credit Officer, Auditor, or System Admin | Select a loan row to open Application Detail. |
| Compliance Audit | `/audit` | Legacy/shared audit navigation where exposed | Select an event to open the related Application Detail. |
| Logout | `/logout` | Header logout action or mobile Logout tab | Clears the session and opens Login. Inactivity logout preserves the current page as the post-login destination. |

### Application Detail and Workflow Screens

`{id}` below is the selected loan application ID.

| Screen | Route | How to open it | Main navigation |
| --- | --- | --- | --- |
| Application Detail | `/applications/{id}` | Select an application, loan, audit event, flag, or dashboard card | Desktop renders a role-specific workstation. Mobile redirects by stage: intake to Step 1, OCR to OCR Review, credit stage to Credit Review, and later stages to Approval Readiness. |
| Intake Wizard | `/applications/{id}/step/{1-9}` | Create a new application, open an intake-stage application on mobile, or choose an incomplete step from Loan Officer detail | Back opens the previous step. Save/continue opens the next step. Step 9 advances to OCR Review. |
| Guarantor Wizard | `/applications/{id}/guarantors/{1-2}/step/{1-8}` | Open a guarantor task from the intake wizard | Back opens the previous guarantor step. Completing Step 8 returns to application intake Step 3. |
| Document Upload | `/applications/{id}/documents/upload?type={type}` | Select an upload action from intake, application detail, guarantor, or credit review | Successful upload returns to Application Detail. |
| OCR Review | `/applications/{id}/ocr-review` | Finish intake, open an OCR-stage application, select an OCR exception, or choose Review Extracted Data | Verify advances to Credit Review. Other completion paths return to Application Detail. A return action opens Return Application. |
| Visitation Report | `/applications/{id}/visitation` | Visit Schedule, Visit Signoffs, Application Detail, or Approval Readiness | Submit/concur returns to Application Detail. The page also has Back to Application. |
| Credit Review | `/applications/{id}/credit-review` | Credit Officer review queue, a credit-stage application on mobile, or verified OCR Review | Submit a recommendation and return to Dashboard. Back to Profile opens Application Detail. |
| Approval Readiness | `/applications/{id}/approve` | Branch Manager Awaiting Me, System Admin control queue, later-stage mobile application, or Application Detail | Fix links open the relevant intake, OCR, or visitation screen. Approve returns to Dashboard. Return links open Return Application. |
| Return Application | `/applications/{id}/return` | Approval, OCR, or role-specific Application Detail actions | Submit returns the application to the returned stage and opens Dashboard. Cancel returns to Application Detail. |

### Loan Officer Screens

Desktop sidebar: Dashboard, My Queue, New Application, Drafts, Returned, Visit
Schedule, My Visitation Reports, Upload Form, and OCR Review Queue.

Mobile tabs: Home, My Queue, New, Upload, and Visits.

| Screen | Route | How to open it | Where items lead |
| --- | --- | --- | --- |
| Loan Officer Dashboard | `/dashboard` | Home/Dashboard | Application activity opens Application Detail; View All opens Applications; stage segments open filtered Applications; New action opens New Application. |
| My Queue | `/my-queue` | My Queue sidebar or mobile Queue tab | Select an item to open Application Detail. New/FAB opens New Application. |
| Drafts | `/applications?stage=intake` | Drafts sidebar item | Select an item to continue Application Detail or the mobile intake wizard. |
| Returned | `/applications?stage=returned` | Returned sidebar item | Select an item to review corrections from Application Detail. |
| Visit Schedule | `/visits` | Visit Schedule sidebar or mobile Visits tab | Select a visit to open its Visitation Report. |
| My Visitation Reports | `/applications` | Sidebar item | Select an application, then open its visitation task from Application Detail. |
| Upload Form | `/applications` | Sidebar item or mobile Upload tab | Select an application, then choose a document upload action. |
| OCR Review Queue | `/applications?stage=ocr_review` | OCR Review Queue sidebar item | Select an application to open its detail or mobile OCR Review. |

### Credit Officer Screens

Desktop sidebar: Dashboard, My Reviews, OCR Exceptions, and Current Loans.

Mobile tabs: Home, Reviews, OCR, Loans, and Logout.

| Screen | Route | How to open it | Where items lead |
| --- | --- | --- | --- |
| Credit Officer Dashboard | `/dashboard` | Home/Dashboard | Review items open Credit Review; OCR issue items open OCR Review. |
| My Reviews | `/my-reviews` | Reviews tab/sidebar or dashboard View All | Select an item to open Credit Review. |
| OCR Exceptions | `/ocr-exceptions` | OCR tab/sidebar or dashboard Open | Select an exception to open OCR Review for its application. |
| Current Loans | `/borrowers` | Loans tab or Current Loans sidebar | Select a loan to open Application Detail. |

### Branch Manager Screens

Desktop sidebar: Dashboard, Awaiting Me, Visit Signoffs, Pipeline, and Current
Loans.

Mobile tabs: Home, Awaiting, Signoffs, Pipeline, and Logout.

| Screen | Route | How to open it | Where items lead |
| --- | --- | --- | --- |
| Branch Manager Dashboard | `/dashboard` | Home/Dashboard | Priority approvals open Approval Readiness; pipeline summaries open Pipeline. |
| Awaiting Me | `/awaiting-me` | Awaiting tab/sidebar or dashboard Open Queue | Select an item to open Approval Readiness. |
| Visit Signoffs | `/pending-signoffs` | Signoffs tab/sidebar | Select an item to open its Visitation Report for concurrence. |
| Branch Pipeline | `/pipeline` | Pipeline tab/sidebar or dashboard Open | Select a stage to open filtered Applications. |
| Current Loans | `/borrowers` | Current Loans sidebar | Select a loan to open Application Detail. |

### Auditor Screens

Desktop sidebar: Dashboard, Compliance Flags, Audit Trail, and Current Loans.

Mobile tabs: Home, Flags, Audit, Loans, and Logout.

| Screen | Route | How to open it | Where items lead |
| --- | --- | --- | --- |
| Auditor Dashboard | `/dashboard` | Home/Dashboard | Compliance items open Application Detail; Audit Activity opens Audit Trail. |
| Compliance Flags | `/compliance-flags` | Flags tab/sidebar or dashboard Open Flags | Select a flag to open the related Application Detail. |
| Audit Trail | `/audit-trail` | Audit tab/sidebar or dashboard Audit Activity | Review immutable activity and open related applications where linked. |
| Current Loans | `/borrowers` | Loans tab or Current Loans sidebar | Select a loan to open Application Detail. |

### System Admin Screens

Desktop sidebar: Dashboard, Users, System Activity, and Audit Trail.

Mobile tabs: Home, Users, Activity, Audit, and Logout.

| Screen | Route | How to open it | Where items lead |
| --- | --- | --- | --- |
| System Admin Dashboard | `/dashboard` | Home/Dashboard | Final-control items open Approval Readiness; role counts open Users; activity shortcuts open System Activity. |
| Users | `/users` | Users tab/sidebar, Manage Users, or role-count cards | Manage organization users and inspect role totals; use global navigation to leave. |
| System Activity | `/system-activity` | Activity tab/sidebar or dashboard Open | Control-queue items open Approval Readiness. |
| Audit Trail | `/audit-trail` | Audit tab/sidebar | Uses the system activity presentation for read-only audit history and linked application controls. |
| Compliance Flags | `/compliance-flags` | Direct authorized route | Inspect flags and select one to open Application Detail. |

### Access and Back-Navigation Rules

- Unauthenticated page requests redirect to Login and preserve the requested
  URL in `next`.
- Role-only queue routes reject users without the required role.
- Browser Back follows normal browser history, but workflow buttons use the
  explicit destinations listed above.
- Mobile Application Detail is a routing gateway rather than a detail page.
- Desktop Application Detail remains open as the central workstation for most
  role-specific actions.
- Workflow submission commonly returns to Application Detail or Dashboard,
  depending on whether the action merely saves task data or advances ownership.

## Testing Changes

With the backend running and seeded:

```powershell
python backend\test_http.py
python backend\test_routes_render.py
```

`test_routes_render.py` logs in as each seeded role and verifies that desktop
and mobile shells render correctly. Also test changed pages manually at both
desktop and narrow mobile widths.

## Development Notes

- Keep the UI usable with keyboard navigation and visible focus states.
- Preserve the existing reduced-motion behavior for animations.
- Reuse established classes and components before adding new variants.
- Store static assets under `static/` and reference them with `/static/...`.
- Restarting Uvicorn is normally unnecessary when `--reload` is enabled;
  template, CSS, and JavaScript changes can usually be refreshed directly.
