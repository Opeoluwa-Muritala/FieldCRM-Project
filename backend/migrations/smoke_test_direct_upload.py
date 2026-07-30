"""End-to-end smoke test for the mobile direct-upload endpoints.

The script creates a tiny authenticated Cloudinary PNG, finalizes it through
FastAPI, verifies the database row, and removes every test artifact afterward.
"""
from __future__ import annotations

import base64
import sys
from pathlib import Path

import httpx
import psycopg2
from fastapi.testclient import TestClient

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from app.core.config import settings
from app.core.security import create_access_token
from app.main import app
from app.services.cloud_storage_service import _configure_cloudinary


PNG = base64.b64decode(
    "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII="
)


def cleanup_previous_smoke_artifacts(db) -> None:
    with db.cursor() as cursor:
        cursor.execute(
            "SELECT id, cloud_public_id FROM documents WHERE doc_type='automated_smoke_test'"
        )
        stale = cursor.fetchall()
        for stale_id, _ in stale:
            cursor.execute("DELETE FROM ocr_jobs WHERE document_id=%s", (stale_id,))
            cursor.execute("DELETE FROM document_upload_intents WHERE document_id=%s", (stale_id,))
            cursor.execute("DELETE FROM documents WHERE id=%s", (stale_id,))
        cursor.execute(
            "DELETE FROM document_upload_intents WHERE document_type='automated_smoke_test'"
        )
    db.commit()
    if stale:
        _configure_cloudinary()
        import cloudinary.uploader
        for _, stale_public_id in stale:
            if stale_public_id:
                cloudinary.uploader.destroy(
                    stale_public_id, resource_type="image", type="authenticated", invalidate=True
                )


def main() -> int:
    db = psycopg2.connect(settings.DATABASE_URL)
    intent_id = document_id = public_id = None
    try:
        cleanup_previous_smoke_artifacts(db)
        with db.cursor() as cursor:
            cursor.execute(
                """
                SELECT u.id, u.org_id, a.id
                FROM users u
                JOIN loan_applications a ON a.org_id=u.org_id
                WHERE u.active=TRUE AND u.role='system_admin'
                ORDER BY a.created_at DESC
                LIMIT 1
                """
            )
            selected = cursor.fetchone()
        if not selected:
            raise RuntimeError("No active system administrator/application pair is available.")
        user_id, org_id, application_id = selected
        token = create_access_token(user_id, role="system_admin", org_id=org_id, session_type="mobile")
        headers = {"Authorization": f"Bearer {token}"}
        prefix = f"/api/v1/mobile/applications/{application_id}/documents"

        with TestClient(app) as client:
            auth_response = client.post(
                f"{prefix}/upload-authorizations",
                headers=headers,
                json={
                    "filename": "direct-upload-smoke.png",
                    "mime_type": "image/png",
                    "size_bytes": len(PNG),
                    "doc_type": "automated_smoke_test",
                },
            )
            auth_response.raise_for_status()
            authorization = auth_response.json()["authorization"]
            intent_id = authorization["intent_id"]
            cloud_response = httpx.post(
                authorization["upload_url"],
                data=authorization["fields"],
                files={"file": ("direct-upload-smoke.png", PNG, "image/png")},
                timeout=60,
            )
            cloud_response.raise_for_status()
            cloud = cloud_response.json()
            public_id = cloud["public_id"]
            finalize_response = client.post(
                f"{prefix}/finalize",
                headers=headers,
                json={
                    "intent_id": intent_id,
                    "public_id": public_id,
                    "version": cloud["version"],
                    "signature": cloud["signature"],
                },
            )
            finalize_response.raise_for_status()
            document_id = finalize_response.json()["document"]["id"]
            repeat_response = client.post(
                f"{prefix}/finalize",
                headers=headers,
                json={
                    "intent_id": intent_id,
                    "public_id": public_id,
                    "version": cloud["version"],
                    "signature": cloud["signature"],
                },
            )
            repeat_response.raise_for_status()
            assert repeat_response.json()["document"]["id"] == document_id

        with db.cursor() as cursor:
            cursor.execute(
                "SELECT stored_path, cloud_public_id FROM documents WHERE id=%s", (document_id,)
            )
            stored = cursor.fetchone()
        assert stored == (f"cloudinary://{public_id}", public_id)
        print("Direct upload authorization, Cloudinary upload, finalization, and idempotent replay passed.")
        return 0
    finally:
        db.rollback()
        if document_id:
            with db.cursor() as cursor:
                cursor.execute("DELETE FROM ocr_jobs WHERE document_id=%s", (document_id,))
                cursor.execute("DELETE FROM document_upload_intents WHERE id=%s", (intent_id,))
                cursor.execute("DELETE FROM documents WHERE id=%s", (document_id,))
            db.commit()
        elif intent_id:
            with db.cursor() as cursor:
                cursor.execute("DELETE FROM document_upload_intents WHERE id=%s", (intent_id,))
            db.commit()
        if public_id:
            _configure_cloudinary()
            import cloudinary.uploader
            cloudinary.uploader.destroy(
                public_id, resource_type="image", type="authenticated", invalidate=True
            )
        db.close()


if __name__ == "__main__":
    raise SystemExit(main())
