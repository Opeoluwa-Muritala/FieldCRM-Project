# FieldCRM Backend

This folder contains the FieldCRM FastAPI application, database access layer,
business domains, PostgreSQL migrations, authentication, REST endpoints, and
server-rendered web routes.

It also serves the templates and static assets from `../frontend`.

## Technology

- Python 3
- FastAPI and Uvicorn
- PostgreSQL
- asyncpg and psycopg2
- Pydantic settings and schemas
- Jinja2
- JWT authentication
- SQL organized by business domain

## Folder Structure

```text
backend/
|-- app/
|   |-- main.py              Application entry point and web authentication
|   |-- config.py            Compatibility import for application settings
|   |-- core/                Database, security, middleware, and shared helpers
|   |-- domains/             Domain routers, services, repositories, and SQL
|   |-- schemas/             Shared request and response schemas
|   `-- services/            Cross-domain services
|-- migrations/              Ordered schema and demo-data SQL scripts
|-- alembic/                 Alembic migration environment
|-- requirements.txt         Python dependencies
|-- test_http.py             Basic running-server connectivity check
`-- test_routes_render.py    Role and device rendering checks
```

The domain pattern is:

```text
domains/<domain>/
|-- router.py
|-- service.py
|-- repository.py
|-- schemas.py               When domain-specific schemas are needed
`-- queries/                 SQL statements loaded by the repository
```

Routers should handle HTTP concerns, services should hold business rules, and
repositories should own database access.

## Prerequisites

- Python 3.10 or newer
- PostgreSQL
- A PowerShell terminal for the commands below

PostgreSQL is the supported path for the bundled schema and demo seed. The
application contains a SQLite compatibility layer, but the supplied migration
runner and SQL files are PostgreSQL-oriented.

## Setup

Run from the repository root.

### 1. Create a virtual environment

```powershell
python -m venv .venv
.\.venv\Scripts\Activate.ps1
```

### 2. Install dependencies

```powershell
pip install -r backend\requirements.txt
```

### 3. Configure the environment

Create or update `backend/.env`:

```env
DATABASE_URL=postgresql://postgres:postgres@localhost:5432/fieldcrm
JWT_SECRET_KEY=replace-with-a-long-random-secret
COOKIE_SECURE=false
```

Do not commit real credentials or production secrets.

Supported settings include:

| Variable | Purpose | Development default |
| --- | --- | --- |
| `DATABASE_URL` | Complete database connection URL | Local SQLite fallback |
| `POSTGRES_SERVER` | PostgreSQL host used to build a URL | Empty |
| `POSTGRES_USER` | PostgreSQL user | `postgres` |
| `POSTGRES_PASSWORD` | PostgreSQL password | `postgres` |
| `POSTGRES_DB` | PostgreSQL database | `fieldcrm` |
| `JWT_SECRET_KEY` | Token signing secret | Ephemeral generated value |
| `COOKIE_SECURE` | Restrict cookies to HTTPS | `false` |

Always set `JWT_SECRET_KEY` outside throwaway local development. The generated
fallback changes when the process restarts and invalidates existing sessions.

### 4. Create the schema and seed demo data

```powershell
python backend\migrations\run_migration.py
```

The runner executes:

1. `001_full_schema.sql`
2. `002_ref_no_sequence.sql`
3. `003_seed_demo.sql`

The runner reads `DATABASE_URL` directly from `backend/.env`.

### 5. Start the application

```powershell
uvicorn app.main:app --app-dir backend --reload
```

Open:

- Web login: `http://127.0.0.1:8000/login`
- OpenAPI docs: `http://127.0.0.1:8000/api/docs`
- ReDoc: `http://127.0.0.1:8000/api/redoc`
- Health check: `http://127.0.0.1:8000/api/v1/health`

## Demo Accounts

The seed migration creates these accounts with password `password123`:

| Role | Email |
| --- | --- |
| System Admin | `admin@mmfb.com` |
| Branch Manager | `adebayo@mmfb.com` |
| Loan Officer | `chidi@mmfb.com` |
| Credit Officer | `fatima@mmfb.com` |
| Auditor | `samuel@mmfb.com` |

These credentials are for local demo data only.

## Authentication

The backend supports:

- Browser login at `POST /login`, which sets an HTTP-only session cookie.
- API cookie login at `POST /api/v1/auth/login`.
- API bearer login at `POST /api/v1/auth/login-bearer`.
- Logout at `/logout` or `POST /api/v1/auth/logout`.

Protected browser routes redirect unauthenticated users to `/login`. Protected
API routes return JSON errors.

## Current Route Areas

- Authentication and user registration under `/api/v1/auth` and
  `/api/v1/users`
- Role dashboards and work queues
- Loan application creation and staged data capture
- Guarantor forms
- Document upload and OCR review pages
- Visitation reports and sign-off
- Credit review, approval, and return workflows
- Audit, compliance, users, and system activity views

Some domain router files are currently placeholders, and not every mobile
client endpoint is mounted yet. Treat the generated OpenAPI page as the source
of truth for currently exposed API endpoints.

## Checks

Start the application before running the HTTP checks.

```powershell
python backend\test_http.py
python backend\test_routes_render.py
```

The rendering test requires the seeded PostgreSQL data. A repository-level
import smoke test is also available:

```powershell
python test_imports.py
```

## Adding Backend Functionality

1. Add or update request/response schemas.
2. Put business rules in the domain service.
3. Put data access in the repository.
4. Add SQL under the domain's `queries/` directory.
5. Keep the router focused on validation, dependencies, and responses.
6. Add tests for success, authorization, validation, and failure paths.
7. Confirm the endpoint appears correctly in `/api/docs`.

Use parameterized SQL through the repository layer. Do not interpolate
untrusted values into SQL strings.

## Troubleshooting

If imports fail:

```powershell
.\.venv\Scripts\Activate.ps1
pip install -r backend\requirements.txt
```

If startup fails while initializing the pool, verify that `DATABASE_URL`
points to a reachable database and that migrations have run.

If login fails for all demo accounts, confirm that `003_seed_demo.sql`
completed successfully.

If browser pages have missing styles or templates, start Uvicorn from the
repository root with `--app-dir backend`; the application resolves frontend
paths relative to the repository structure.

