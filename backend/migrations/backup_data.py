import os
import sys
import json
import psycopg2
from uuid import UUID
from datetime import datetime, date
from decimal import Decimal

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from run_migration import get_database_url

class DateTimeEncoder(json.JSONEncoder):
    def default(self, obj):
        if isinstance(obj, (datetime, date)):
            return obj.isoformat()
        if isinstance(obj, UUID):
            return str(obj)
        if isinstance(obj, Decimal):
            return float(obj)
        return super().default(obj)

def backup_database():
    db_url = get_database_url()
    conn = psycopg2.connect(db_url)
    cursor = conn.cursor()

    try:
        # Get all table names in public schema
        cursor.execute(
            "SELECT table_name FROM information_schema.tables "
            "WHERE table_schema = 'public' AND table_type = 'BASE TABLE';"
        )
        tables = [row[0] for row in cursor.fetchall()]
        print(f"Found tables to backup: {tables}")

        backup_data = {}
        for table in tables:
            # Query column names
            cursor.execute(
                f"SELECT column_name FROM information_schema.columns "
                f"WHERE table_name = '{table}' ORDER BY ordinal_position;"
            )
            columns = [row[0] for row in cursor.fetchall()]

            # Fetch all rows
            cursor.execute(f"SELECT * FROM {table};")
            rows = cursor.fetchall()
            
            table_rows = []
            for row in rows:
                table_rows.append(dict(zip(columns, row)))
            
            backup_data[table] = table_rows
            print(f"  Backed up table '{table}': {len(table_rows)} rows")

        # Save to JSON
        backup_path = os.path.join(os.path.dirname(os.path.abspath(__file__)), "db_backup.json")
        with open(backup_path, "w", encoding="utf-8") as f:
            json.dump(backup_data, f, cls=DateTimeEncoder, indent=2)
        
        print(f"\nDatabase successfully backed up to: {backup_path}")

    except Exception as e:
        print(f"Backup failed: {e}")
        sys.exit(1)
    finally:
        conn.close()

if __name__ == "__main__":
    backup_database()
