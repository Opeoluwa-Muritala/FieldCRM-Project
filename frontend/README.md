# FieldCRM Web Frontend

This folder contains the server-rendered web interface for FieldCRM. It delivers role-aware dashboards and workflow pages to loan officers, credit officers, branch managers, auditors, and system administrators.

The frontend is built with Jinja2 templates, vanilla CSS, and vanilla JavaScript. It does not run independently; the FastAPI backend in `../backend` renders the templates and serves the static assets.

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
|   |-- css/                 Responsive application and role theme styles
|   |-- img/                 Logos and image assets
|   `-- js/                  Dashboard and mobile interaction scripts
`-- templates/
    |-- base/                Desktop, mobile, and shared layout shells
    |-- components/          Role-specific sidebars, tab bars, top bars, and reusable fragments
    |-- shared/              Shared workflow pages and forms
    |-- loan_officer/        Loan officer-specific pages
    |-- credit_officer/      Credit officer-specific pages
    |-- branch_manager/      Branch manager-specific pages
    |-- auditor/             Auditor-specific pages
    `-- system_admin/        System administrator-specific pages
```

## Running the Frontend

The frontend runs as part of the backend. From the repository root:

```powershell
python -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -r backend\requirements.txt
uvicorn app.main:app --app-dir backend --reload
```

Open the app in a browser:

- Login: `http://127.0.0.1:8000/login`
- Dashboard: `http://127.0.0.1:8000/dashboard`

The backend must be configured with a database and environment variables. See `../backend/README.md` for full setup details and demo accounts.

## How Rendering Works

FastAPI configures template and static directories in `backend/app/main.py`:

```python
templates = Jinja2Templates(directory="frontend/templates")
app.mount("/static", StaticFiles(directory="frontend/static"), name="static")
```

The backend chooses the rendered page by:

- user role (`Loan Officer`, `Credit Officer`, `Branch Manager`, `Auditor`, `System Admin`)
- request device type (desktop or mobile)
- workflow stage for a selected application

Mobile clients receive a mobile shell and role-specific tab bar, while desktop users receive a desktop shell with a role-specific sidebar.

## Template Conventions

- Use `templates/base/` for layout shells.
- Use `templates/components/` for navigation, headers, and reusable UI fragments.
- Use `templates/shared/` for workflow pages used by multiple roles.
- Use role-specific subfolders for views only one role should see.
- Keep business logic in Python, not in templates.

Typical page structure:

```jinja2
{% extends "base/desktop_shell.html" %}

{% block title %}Page title{% endblock %}

{% block content %}
    <!-- page content -->
{% endblock %}
```

## Styling

Key stylesheets:

| File | Purpose |
| --- | --- |
| `static/css/dashboard.css` | Shared desktop dashboard and workflow styles |
| `static/css/mobile.css` | Mobile shells, bottom tab bars, and responsive layouts |
| `static/css/role-themes.css` | Role-specific colors and visual branding |
| `static/css/login.css` | Login page styling |
| `static/css/borrowers.css` | Borrower and application detail styles |

Role information is passed to the DOM via a `data-role` attribute. Use this for conditional visual styling rather than hardcoding role logic in templates.

## JavaScript

- `static/js/dashboard.js` manages dashboard interactions, badge polling, guided tours, and shared behavior.
- `static/js/mobile.js` handles mobile-specific UI interactions.

The backend must remain the source of truth for authentication and authorization. JavaScript is only for presentation and client-side enhancements.

## Screen and Navigation Overview

The web frontend is organized around shared workflow screens and role-specific queue pages. Navigation is role-aware:

- desktop uses a left sidebar for core navigation
- mobile uses a bottom tab bar and simplified shell
- some workflow screens hide normal navigation until the task completes

### Core shared screens

| Screen | Route | Purpose |
| --- | --- | --- |
| Login | `/login` | User authentication and redirect to the requested page or dashboard |
| Dashboard | `/dashboard` | Role-specific home page with cards, queue summaries, and action shortcuts |
| Applications | `/applications` | Application list with stage, type, and search filters |
| New Application | `/applications/new` | Start a new loan application draft |
| Application Detail | `/applications/{id}` | Central workflow hub for a selected application |
| Pipeline | `/pipeline` | Stage-based application pipeline, primarily for branch managers |
| Borrowers | `/borrowers` | Loan and borrower list for users with loan review or audit access |
| Audit | `/audit` | Audit and compliance workflow entry page |
| Audit Trail | `/audit-trail` | Read-only audit history for auditors and system admins |
| Compliance Flags | `/compliance-flags` | Auditor/system admin flag review page |
| Users | `/users` | System admin user management |
| System Activity | `/system-activity` | System admin final-control activity and audit queue |

### Workflow screens by route

| Screen | Route | Action |
| --- | --- | --- |
| Intake Wizard | `/applications/{id}/step/{1-9}` | Loan officer intake wizard for draft applications |
| Guarantor Wizard | `/applications/{id}/guarantors/{slot}/step/{1-8}` | Guarantor intake and verification wizard |
| Document Upload | `/applications/{id}/documents/upload` | Upload files and trigger OCR processing |
| OCR Review | `/applications/{id}/ocr-review` | Verify and correct OCR-extracted documents |
| Credit Review | `/applications/{id}/credit-review` | Credit officer review, recommendation, and decision capture |
| Approval Readiness | `/applications/{id}/approve` | Branch manager approval readiness screen |
| Return Application | `/applications/{id}/return` | Return or reject application workflows |
| Visitation Report | `/applications/{id}/visitation` | Capture visits, GPS notes, and signoffs |

### Loan Officer flow

Loan officers focus on intake and field tasks:

1. Login → Dashboard
2. Dashboard → My Queue / Drafts / Applications
3. Create new application or resume draft
4. Complete intake steps and guarantor workflows
5. Upload documents and move the application to OCR review
6. View returned applications for correction

Desktop navigation uses sidebar items such as My Queue, New Application, Drafts, Returned, Visits, and OCR Review Queue. Mobile users access the same workflow through tab bar items and page-level actions.

### Credit Officer flow

Credit officers inspect application quality and OCR exceptions:

1. Login → Dashboard
2. Dashboard → My Reviews / OCR Exceptions / Current Loans
3. Open application detail or direct Credit Review screens
4. Validate OCR exceptions and submit recommendations

Credit officers access review queues from the sidebar or mobile tabs, then move to `/credit-review` or `/ocr-review` for task completion.

### Branch Manager flow

Branch managers approve applications and sign off visits:

1. Login → Dashboard
2. Dashboard → Awaiting Me / Pipeline / Visit Signoffs
3. Open application detail or Approval Readiness screens
4. Approve, return, or request corrections

The pipeline and signoff screens are the primary entry points for branch manager decisions.

### Auditor flow

Auditors review compliance and history:

1. Login → Dashboard
2. Dashboard → Compliance Flags / Audit Trail / Borrowers
3. Open flagged applications or audit events
4. Inspect details and verify workflow compliance

Auditors use the same shared application and detail pages, with audit-specific task access from `/audit`, `/audit-trail`, and `/compliance-flags`.

### System Admin flow

System admins monitor users and system activity:

1. Login → Dashboard
2. Dashboard → Users / System Activity / Audit Trail
3. Manage staff, review activity, and inspect queues

System administrators use user management and system activity pages to maintain org configuration and compliance.

## Navigation rules

- Role-restricted routes redirect unauthorized users to the login page or an error page.
- The `next` parameter preserves requested destinations across authentication.
- Desktop pages keep the sidebar active while workflow pages may hide it for focus.
- Mobile pages use a top bar and bottom tab bar for compact navigation.
- Application detail acts as a gateway for role-specific review screens.
- Submissions often return users to Dashboard or Application Detail depending on the workflow.

## Testing the frontend

With the backend running:

```powershell
python backend\test_http.py
python backend\test_routes_render.py
```

`test_routes_render.py` validates desktop and mobile rendering for seeded demo users.

## Development notes

- Keep templates small and reusable.
- Avoid embedding business logic in templates.
- Reuse shared workflow pages across roles where possible.
- Test both desktop and mobile views after layout changes.
- Reuse established classes and components before adding new variants.
- Store static assets under `static/` and reference them with `/static/...`.
- Restarting Uvicorn is normally unnecessary when `--reload` is enabled;
  template, CSS, and JavaScript changes can usually be refreshed directly.
