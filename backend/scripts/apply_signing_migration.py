"""Apply the signing evidence migration to the configured PostgreSQL database."""

from pathlib import Path

import psycopg2


backend_dir = Path(__file__).resolve().parents[1]
env_lines = (backend_dir / ".env").read_text(encoding="utf-8").splitlines()
database_url = next(
    line.split("=", 1)[1].strip().strip("\"'")
    for line in env_lines
    if line.startswith("DATABASE_URL=")
)
sql = (backend_dir / "migrations" / "023_signing_evidence.sql").read_text(
    encoding="utf-8"
)

with psycopg2.connect(database_url) as connection:
    with connection.cursor() as cursor:
        cursor.execute(sql)
        cursor.execute(
            """
            SELECT
                to_regclass('public.document_versions'),
                to_regclass('public.signing_sessions'),
                to_regclass('public.signing_auth_sessions')
            """
        )
        print(cursor.fetchone())
