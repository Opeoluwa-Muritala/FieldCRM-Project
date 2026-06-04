# FieldCRM

FieldCRM is a loan workflow and document processing system for microfinance operations.

## Backend

The backend is a FastAPI app in `backend/app`. Runtime data access should use raw SQL through the domain repositories and SQL files under `backend/app/domains/*/queries`.

Local SQLite database files are generated artifacts and are intentionally ignored. Recreate local data with the migration and seed scripts when needed.

## Useful Commands

```powershell
pip install -r backend/requirements.txt
python backend/migrations/run_migration.py
uvicorn app.main:app --app-dir backend --reload
```

## Notes

Read `AGENTS.md` before making backend changes. It contains the project-specific raw SQL rules, domain structure, and data-access constraints.
