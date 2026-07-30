"""Safely and resumably move legacy local files to authenticated Cloudinary assets.

Dry-run is the default. Use --execute only after reviewing the printed rows.
The local file is deleted only after Cloudinary verification and a committed
database update.
"""
from __future__ import annotations

import argparse
import mimetypes
import re
import sys
from pathlib import Path
from uuid import uuid4

import psycopg2

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from app.core.config import ROOT_DIR, settings
from app.services.cloud_storage_service import _configure_cloudinary, upload_to_cloudinary


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--execute", action="store_true")
    parser.add_argument("--limit", type=int, default=100)
    args = parser.parse_args()
    frontend_root = ROOT_DIR / "frontend"
    connection = psycopg2.connect(settings.DATABASE_URL)
    connection.autocommit = False
    try:
        with connection.cursor() as cursor:
            cursor.execute(
                """
                SELECT id, org_id, loan_id, doc_type, original_name, stored_path, mime_type
                FROM documents
                WHERE stored_path LIKE '/static/uploads/%%'
                  AND cloud_public_id IS NULL
                  AND deleted_at IS NULL
                ORDER BY uploaded_at, id
                LIMIT %s
                """,
                (args.limit,),
            )
            rows = cursor.fetchall()
        for document_id, org_id, loan_id, doc_type, original_name, stored_path, mime_type in rows:
            local_path = (frontend_root / stored_path.removeprefix("/")).resolve()
            upload_root = (frontend_root / "static" / "uploads").resolve()
            if upload_root not in local_path.parents or not local_path.is_file():
                print(f"SKIP {document_id}: local file missing or outside upload root")
                continue
            print(f"{'MIGRATE' if args.execute else 'WOULD MIGRATE'} {document_id}: {local_path}")
            if not args.execute:
                continue
            safe_type = re.sub(r"[^a-zA-Z0-9_.-]+", "_", doc_type or "other").strip("._") or "other"
            public_id = f"fieldcrm/{org_id}/{loan_id}/{safe_type}_{uuid4().hex}"
            detected_mime = mime_type or mimetypes.guess_type(original_name or local_path.name)[0] or ""
            result = upload_to_cloudinary(
                local_path.read_bytes(), detected_mime, public_id=public_id, overwrite=False
            )
            _configure_cloudinary()
            import cloudinary.api
            asset = cloudinary.api.resource(public_id, resource_type="image", type="authenticated")
            if asset.get("public_id") != public_id or int(asset.get("bytes") or 0) != local_path.stat().st_size:
                connection.rollback()
                print(f"FAILED {document_id}: Cloudinary verification mismatch")
                continue
            try:
                with connection.cursor() as cursor:
                    cursor.execute(
                        """
                        UPDATE documents
                        SET stored_path=%s, cloud_public_id=%s, cloud_preview_url=%s
                        WHERE id=%s AND stored_path=%s AND cloud_public_id IS NULL
                        """,
                        (f"cloudinary://{result.public_id}", result.public_id, result.preview_url,
                         document_id, stored_path),
                    )
                    if cursor.rowcount != 1:
                        raise RuntimeError("document changed during migration")
                connection.commit()
            except Exception:
                connection.rollback()
                raise
            local_path.unlink()
            print(f"DONE {document_id}")
        return 0
    finally:
        connection.close()


if __name__ == "__main__":
    raise SystemExit(main())
