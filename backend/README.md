# FieldCRM Backend

This module contains the FastAPI backend for FieldCRM. It serves the role-aware web app, exposes the API used by the Android client, and owns the database access layer and workflow logic.

## What this module does

- serves the web routes and API endpoints
- manages authentication and authorization
- handles loan workflow state transitions
- stores and retrieves application, document, and audit data
- renders templates and serves static assets from the frontend module

## Stack

- Python 3.10+
- FastAPI + Uvicorn
- PostgreSQL
- asyncpg and psycopg2
- Pydantic
- Jinja2
- JWT-based auth

## Setup

Run the commands from the repository root.

```powershell
python -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -r backend\requirements.txt
```

Create backend/.env with values such as:

```env
DATABASE_URL=postgresql://postgres:postgres@localhost:5432/fieldcrm
JWT_SECRET_KEY=replace-with-a-long-random-secret
COOKIE_SECURE=false
```

Apply the schema and seed data:

```powershell
python backend\migrations\run_migration.py
```

Start the backend:

```powershell
uvicorn app.main:app --app-dir backend --reload
```

Useful URLs:

- http://127.0.0.1:8000/login
- http://127.0.0.1:8000/api/docs
- http://127.0.0.1:8000/api/redoc
- http://127.0.0.1:8000/api/v1/health

## Module layout

```text
backend/
|-- app/
|   |-- main.py
|   |-- config.py
|   |-- core/
|   |-- domains/
|   |-- schemas/
|   `-- services/
|-- migrations/
|-- alembic/
|-- requirements.txt
|-- test_http.py
`-- test_routes_render.py
```

## Demo and verification

The repository includes helper scripts for local workflow checks:

```powershell
python test_imports.py
python backend\test_http.py
python backend\test_routes_render.py
```

The seeded demo accounts use the password password123 and are documented in the repository root README.

## Development notes

- keep routers thin and push business rules into services
- keep database access in repositories and domain SQL files
- use parameterized SQL and avoid string interpolation for user input
- confirm new endpoints work in /api/docs after changes

