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
    "004_notifications.sql",
    "024_branch_scoping.sql",
    "025_mobile_creation_idempotency.sql",
    "026_document_upload_intents.sql",
    "027_auth_refresh_sessions.sql",
    "028_business_locations.sql",
    "029_collateral_items.sql",
    "030_business_pnl.sql",
    "031_loan_recommendations.sql",
    "032_single_executive_role_holders.sql",
    "033_user_soft_delete.sql",
    "034_loan_products.sql",
    "035_refresh_tokens.sql",
    "036_offer_letter_product_configs.sql",
    "037_offer_letter_product_templates.sql",
    "038_feasibility_cashflow.sql",
    "039_collateral_valuation_policies.sql",
    "040_collateral_policy_trigger.sql",
    "041_recalculate_existing_collateral.sql",
    "042_core_banking.sql",
    "043_customer_identity.sql",
    "044_configuration_hub.sql",
    "045_configurable_products_forms.sql",
    "046_configurable_workflow_permissions.sql",
    "047_operational_work.sql",
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

        # Create migration history table if not exists
        cursor.execute(
            "CREATE TABLE IF NOT EXISTS migration_history ("
            "    filename VARCHAR(255) PRIMARY KEY,"
            "    applied_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()"
            ");"
        )
        conn.commit()

        # Check if core tables exist
        cursor.execute(
            "SELECT count(*) FROM information_schema.tables "
            "WHERE table_schema = 'public' AND table_name = 'users';"
        )
        users_exist = cursor.fetchone()[0] > 0

        cursor.execute(
            "SELECT count(*) FROM information_schema.tables "
            "WHERE table_schema = 'public' AND table_name = 'branches';"
        )
        branches_exist = cursor.fetchone()[0] > 0

        cursor.execute(
            "SELECT count(*) FROM information_schema.tables "
            "WHERE table_schema = 'public' AND table_name = 'notifications';"
        )
        notifications_exist = cursor.fetchone()[0] > 0

        # Check if history table is empty
        cursor.execute("SELECT count(*) FROM migration_history;")
        history_count = cursor.fetchone()[0]
        
        if history_count == 0 and users_exist:
            cursor.execute("INSERT INTO migration_history (filename) VALUES ('001_full_schema.sql') ON CONFLICT DO NOTHING;")
            cursor.execute("INSERT INTO migration_history (filename) VALUES ('002_ref_no_sequence.sql') ON CONFLICT DO NOTHING;")
            if notifications_exist:
                cursor.execute("INSERT INTO migration_history (filename) VALUES ('004_notifications.sql') ON CONFLICT DO NOTHING;")
            if branches_exist:
                cursor.execute("INSERT INTO migration_history (filename) VALUES ('024_branch_scoping.sql') ON CONFLICT DO NOTHING;")
            conn.commit()
            logger.info("Seeded migration history table selectively based on existing tables.")

        # Self-healing: if branches do not exist but 024 is in history, remove it to force re-run
        if not branches_exist:
            cursor.execute("DELETE FROM migration_history WHERE filename = '024_branch_scoping.sql';")
            conn.commit()

        # Fetch already applied migrations
        cursor.execute("SELECT filename FROM migration_history;")
        applied = {row[0] for row in cursor.fetchall()}

        for filename in MIGRATION_FILES:
            if filename in applied:
                logger.info("Skipping already applied migration: %s", filename)
                continue

            filepath = os.path.join(MIGRATION_DIR, filename)
            if not os.path.exists(filepath):
                logger.error("Migration file not found: %s", filepath)
                conn.rollback()
                sys.exit(1)

            logger.info("Running: %s", filename)
            with open(filepath, "r", encoding="utf-8") as f:
                sql = f.read()

            cursor.execute(sql)
            cursor.execute("INSERT INTO migration_history (filename) VALUES (%s);", (filename,))
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
        logger.info("Existing users: %d", user_count)

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
