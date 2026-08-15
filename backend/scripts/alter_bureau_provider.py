import asyncio
import os
import sys
from pathlib import Path
import asyncpg
from dotenv import load_dotenv

BACKEND_DIR = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(BACKEND_DIR))
load_dotenv(BACKEND_DIR / ".env")

async def main():
    db_url = os.getenv("DATABASE_URL")
    if not db_url:
        print("DATABASE_URL is not set!")
        sys.exit(1)

    print("Connecting to database...")
    conn = await asyncpg.connect(db_url)
    try:
        print("Adding provider column to bureau_submissions...")
        await conn.execute(
            "ALTER TABLE bureau_submissions ADD COLUMN IF NOT EXISTS provider TEXT;"
        )
        print("Setting default value 'creditregistry' on existing null provider columns...")
        await conn.execute(
            "UPDATE bureau_submissions SET provider = 'creditregistry' WHERE provider IS NULL;"
        )
        print("Database schema updated successfully!")
    finally:
        await conn.close()

if __name__ == "__main__":
    asyncio.run(main())
