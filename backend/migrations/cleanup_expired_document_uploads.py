"""Expire stale direct-upload intents and remove any unfinalized Cloudinary asset."""
from __future__ import annotations

import argparse
import sys
from pathlib import Path

import psycopg2

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from app.core.config import settings
from app.services.cloud_storage_service import _configure_cloudinary


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--limit", type=int, default=100)
    args = parser.parse_args()
    connection = psycopg2.connect(settings.DATABASE_URL)
    connection.autocommit = False
    _configure_cloudinary()
    import cloudinary.uploader

    try:
        with connection.cursor() as cursor:
            cursor.execute(
                """
                SELECT id, cloud_public_id
                FROM document_upload_intents
                WHERE status IN ('pending', 'expired')
                  AND document_id IS NULL
                  AND expires_at < NOW() - INTERVAL '15 minutes'
                ORDER BY expires_at
                LIMIT %s
                FOR UPDATE SKIP LOCKED
                """,
                (args.limit,),
            )
            rows = cursor.fetchall()
            for intent_id, public_id in rows:
                # Cloudinary destroy is idempotent when the client never uploaded.
                cloudinary.uploader.destroy(
                    public_id, resource_type="image", type="authenticated", invalidate=True
                )
                cursor.execute(
                    "UPDATE document_upload_intents SET status='expired' WHERE id=%s AND status<>'finalized'",
                    (intent_id,),
                )
        connection.commit()
        print(f"Expired {len(rows)} upload intent(s).")
        return 0
    except Exception:
        connection.rollback()
        raise
    finally:
        connection.close()


if __name__ == "__main__":
    raise SystemExit(main())
