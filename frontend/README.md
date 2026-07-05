# FieldCRM Web Frontend

This module contains the server-rendered web UI for FieldCRM. It provides the role-aware pages that the backend renders for loan officers, credit officers, branch managers, auditors, and system administrators.

## What this module does

- hosts the Jinja2 templates and shared page shells
- serves the CSS and JavaScript assets used by the web app
- renders role-specific dashboards and workflow screens
- works alongside the FastAPI backend rather than running independently

## Stack

- Jinja2 templates
- HTML/CSS/JavaScript
- FastAPI template and static-file integration

## Run the frontend

The frontend runs as part of the backend. From the repository root:

```powershell
python -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -r backend\requirements.txt
uvicorn app.main:app --app-dir backend --reload
```

Then open:

- http://127.0.0.1:8000/login
- http://127.0.0.1:8000/dashboard

## Module structure

```text
frontend/
|-- static/
|   |-- css/
|   |-- img/
|   `-- js/
`-- templates/
    |-- base/
    |-- components/
    |-- shared/
    |-- loan_officer/
    |-- credit_officer/
    |-- branch_manager/
    |-- auditor/
    `-- system_admin/
```

## Rendering model

The backend configures the template and static directories from the FastAPI app. The rendered view depends on:

- user role
- request device type
- current workflow stage for the selected application

## Shared conventions

- keep templates reusable and small
- keep business logic in Python rather than in templates
- reuse shared workflow pages across roles where possible
- test both desktop and mobile layouts after UI changes

## Verification

With the backend running, you can validate the web routes with:

```powershell
python backend\test_http.py
python backend\test_routes_render.py
```

The root README contains the full setup workflow and demo account details.
