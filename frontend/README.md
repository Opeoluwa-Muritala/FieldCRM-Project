# FieldCRM web frontend

This directory is the server-rendered web user interface for FieldCRM. It contains templates, styling, browser JavaScript, image assets, and local development uploads. FastAPI in `../backend` renders it and serves `/static`; this is not a Node project and has no separate build or development server.

## What it provides

- Responsive, authenticated browser UI for loan origination, review, approval, disbursement, servicing, and administration.
- Desktop and mobile shells with role-specific sidebars and tab bars.
- Shared templates for login, password recovery, settings, notifications, search, borrowers, applications, workflow actions, documents, client intake, and reports.
- Role-specific dashboards, queues, application/detail views, shared CSS, browser-side dashboard behavior, and bank brand assets.

The backend selects templates from the current user's role and authorizes every route/action. Browser code presents data and submits forms; it does not decide permissions or advance workflow state.

## Screen catalogue

| Area | Screens represented |
| --- | --- |
| Shared/authentication | Login, forgot/reset password, notifications, settings, search, error, audit, pipeline, repayment, return/approve, loan view, applications, borrowers. |
| Account officer | Dashboard, work queue, application detail/wizard, document selector/upload, OCR-review queue, visits and visitation reports. |
| Branch manager/supervisor | Dashboard, application detail, awaiting concurrence, pending sign-offs, pipeline, supervisory review queue. |
| Credit analyst/officer | Dashboard, review queue, application detail, OCR exceptions. |
| CRM | Dashboard, CRM queue/review, disbursement, payment recording. |
| Committee/executive | Committee queue/review/MCC summary; executive, ED, and MD dashboards, queues, and decision pages. |
| Auditor/legal | Auditor dashboard, application detail, audit trail, compliance flags; legal queue and valuation. |
| System admin | Dashboard, application detail, users, system activity, interest presets. |
| External client intake | Share-link start, multi-step application and guarantor forms, upload, success, and error pages. |

## Layout

```text
frontend/
├── templates/
│   ├── base/              desktop, mobile, and shared shells
│   ├── components/        reusable navigation and application flags
│   ├── shared/            cross-role and client-intake pages
│   └── <role>/            role-specific screens and dashboards
└── static/
    ├── css/               login, dashboard, borrower, and role-theme styles
    ├── js/                browser-side UI behaviour
    ├── img/               logos and icon sprite
    └── uploads/           local document-storage fallback
```

`backend/app/main.py` mounts `static/` at `/static` and configures Jinja with `templates/`.

## Run and configure

Start the backend from the repository root:

```powershell
python -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -r backend\requirements.txt
uvicorn app.main:app --app-dir backend --reload
```

Open `http://127.0.0.1:8000/login` or `http://127.0.0.1:8000/dashboard`. Database, session security, email, external services, and document storage are configured in `backend/.env`; see [`../backend/README.md`](../backend/README.md).

## Rendering model

1. The backend authenticates the user and creates an HTTP-only session cookie.
2. A protected route resolves role and permitted workflow data.
3. FastAPI renders the appropriate Jinja template.
4. The browser loads CSS, JS, and assets from `/static`.
5. Forms return to the backend for validation, audit logging, document processing, and workflow transitions.

Unauthorized page requests are redirected to login or dashboard as appropriate; API calls return JSON errors.

## Development and verification

- Keep business and authorization rules in Python, not Jinja or JavaScript.
- Reuse base, component, and shared templates before creating role-specific markup.
- Update desktop and mobile navigation together for new top-level role screens.
- Preserve accessibility and responsive behavior; status must not rely on color alone.
- Treat `static/uploads/` as user data, not design assets.

Because the frontend is served through FastAPI, use its route checks:

```powershell
python backend\test_http.py
python backend\test_routes_render.py
python backend\test_responsive_smoke.py
```
