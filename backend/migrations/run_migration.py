"""
FieldCRM Database Migration Runner
Connects to Neon PostgreSQL and executes migration SQL files in order.
Usage: python backend/migrations/run_migration.py
"""
import os
import sys
import logging

logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(message)s")
logger = logging.getLogger("migration")

MIGRATION_DIR = os.path.dirname(os.path.abspath(__file__))
ENV_PATH = os.path.join(MIGRATION_DIR, "..", ".env")

MIGRATION_FILES = [
    "001_full_schema.sql",
    "002_ref_no_sequence.sql",
    "003_seed_demo.sql",
]


def get_database_url() -> str:
    """Read DATABASE_URL from backend/.env file."""
    if not os.path.exists(ENV_PATH):
        logger.error("No .env file found at %s", ENV_PATH)
        sys.exit(1)

    with open(ENV_PATH, "r", encoding="utf-8") as f:
        for line in f:
            line = line.strip()
            if line.startswith("DATABASE_URL="):
                url = line.split("=", 1)[1].strip().strip('"').strip("'")
                return url

    logger.error("DATABASE_URL not found in .env")
    sys.exit(1)


def run_migrations() -> None:
    """Execute all migration files against the database."""
    try:
        import psycopg2
    except ImportError:
        logger.error("psycopg2 not installed. Run: pip install psycopg2-binary")
        sys.exit(1)

    db_url = get_database_url()

    # Mask credentials in log output
    masked = db_url.split("@")[-1] if "@" in db_url else db_url
    logger.info("Connecting to database: ...@%s", masked)

    conn = None
    try:
        conn = psycopg2.connect(db_url)
        conn.autocommit = False
        cursor = conn.cursor()

        for filename in MIGRATION_FILES:
            filepath = os.path.join(MIGRATION_DIR, filename)
            if not os.path.exists(filepath):
                logger.error("Migration file not found: %s", filepath)
                conn.rollback()
                sys.exit(1)

            logger.info("Running: %s", filename)
            with open(filepath, "r", encoding="utf-8") as f:
                sql = f.read()

            # Execute each statement separately to handle multi-statement files
            # psycopg2 can handle multi-statement strings directly
            cursor.execute(sql)
            logger.info("  Completed: %s", filename)

        conn.commit()
        logger.info("All migrations completed successfully!")

        # Verify: list tables
        cursor.execute(
            "SELECT table_name FROM information_schema.tables "
            "WHERE table_schema = 'public' ORDER BY table_name"
        )
        tables = [row[0] for row in cursor.fetchall()]
        logger.info("Tables in database: %s", ", ".join(tables))

        # Verify: count users
        cursor.execute("SELECT count(*) FROM users")
        user_count = cursor.fetchone()[0]
        logger.info("Users seeded: %d", user_count)

        # List users
        cursor.execute("SELECT full_name, email, role FROM users ORDER BY role")
        for row in cursor.fetchall():
            logger.info("  User: %s | %s | %s", row[0], row[1], row[2])

    except Exception as e:
        logger.error("Migration failed: %s", e)
        if conn:
            conn.rollback()
        raise
    finally:
        if conn:
            conn.close()


if __name__ == "__main__":
    run_migrations()
