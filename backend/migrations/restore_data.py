import os
import sys
import json
import psycopg2

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from run_migration import get_database_url

TABLE_ORDER = [
    "organisations",
    "branches",
    "users",
    "loan_applications",
    "visitation_reports",
    "documents",
    "checklist_items",
    "verification_checks",
    "bureau_submissions",
    "sanctions_checks",
    "offer_letters",
    "offer_letter_clause_sets",
    "notifications",
    "stage_data",
    "password_reset_tokens",
    "audit_entries",
    "document_versions",
    "signing_sessions",
    "signing_auth_sessions",
    "board_referrals",
    "signature_events",
    "signature_event_pdfs",
    "workflow_events",
    "migration_history"
]

def restore_database():
    backup_path = os.path.join(os.path.dirname(os.path.abspath(__file__)), "db_backup.json")
    if not os.path.exists(backup_path):
        print(f"Error: Backup file not found at {backup_path}")
        sys.exit(1)

    with open(backup_path, "r", encoding="utf-8") as f:
        backup_data = json.load(f)

    db_url = get_database_url()
    conn = psycopg2.connect(db_url)
    cursor = conn.cursor()

    try:
        # Determine tables in database to truncate
        cursor.execute(
            "SELECT table_name FROM information_schema.tables "
            "WHERE table_schema = 'public' AND table_type = 'BASE TABLE';"
        )
        existing_tables = {row[0] for row in cursor.fetchall()}
        
        tables_to_truncate = [t for t in backup_data.keys() if t in existing_tables]
        print(f"Truncating tables: {tables_to_truncate}")

        # Truncate all tables cascade
        if tables_to_truncate:
            truncate_sql = f"TRUNCATE TABLE {', '.join(tables_to_truncate)} CASCADE;"
            cursor.execute(truncate_sql)
            print("Truncation completed successfully.")

        # Determine insert order
        ordered_tables = [t for t in TABLE_ORDER if t in backup_data]
        # Append any backup tables that are not in the predefined list
        for t in backup_data:
            if t not in ordered_tables:
                ordered_tables.append(t)

        print("\nRestoring tables:")
        for table in ordered_tables:
            rows = backup_data[table]
            if not rows:
                print(f"  Skipping '{table}' (0 rows)")
                continue

            print(f"  Restoring '{table}' ({len(rows)} rows)...")
            for row in rows:
                col_names = list(row.keys())
                placeholders = ", ".join(["%s"] * len(col_names))
                cols_str = ", ".join(col_names)
                
                vals = []
                for col in col_names:
                    val = row[col]
                    if isinstance(val, (dict, list)):
                        val = json.dumps(val)
                    vals.append(val)
                
                cursor.execute(f"INSERT INTO {table} ({cols_str}) VALUES ({placeholders});", vals)

        conn.commit()
        print("\nDatabase successfully restored from backup!")

    except Exception as e:
        conn.rollback()
        print(f"Restore failed: {e}")
        sys.exit(1)
    finally:
        conn.close()

if __name__ == "__main__":
    restore_database()
