import asyncio
from uuid import uuid4

import pytest
from fastapi import HTTPException

from app.domains.documents.direct_upload import DirectDocumentUploadService, _validate_metadata


class RecordingConnection:
    def __init__(self):
        self.calls = []

    async def execute(self, query, *args):
        self.calls.append((query, args))


def test_metadata_rejects_extension_mime_mismatch():
    with pytest.raises(HTTPException) as exc:
        _validate_metadata("statement.pdf", "image/png", 100)
    assert exc.value.status_code == 400


def test_authorization_is_tenant_and_application_scoped(monkeypatch):
    from app.domains.documents import direct_upload

    monkeypatch.setattr(direct_upload.settings, "CLOUDINARY_CLOUD_NAME", "tenant-cloud")
    monkeypatch.setattr(direct_upload.settings, "CLOUDINARY_API_KEY", "public-key")
    monkeypatch.setattr(direct_upload.settings, "CLOUDINARY_API_SECRET", "server-secret")
    monkeypatch.setattr(direct_upload, "_configure_cloudinary", lambda: None)
    conn = RecordingConnection()
    org_id, application_id, actor_id = uuid4(), uuid4(), uuid4()

    result = asyncio.run(DirectDocumentUploadService(conn).authorize(
        application_id=application_id,
        org_id=org_id,
        actor_id=actor_id,
        actor_role="account_officer",
        doc_type="bank_statement",
        original_name="statement.pdf",
        mime_type="application/pdf",
        size_bytes=100,
    ))

    assert str(org_id) in result["fields"]["public_id"]
    assert str(application_id) in result["fields"]["public_id"]
    assert result["fields"]["overwrite"] == "false"
    assert "server-secret" not in str(result)
    assert conn.calls[0][1][1:4] == (org_id, application_id, actor_id)


def test_all_direct_upload_routes_are_registered():
    from app.main import app

    paths = set(app.openapi()["paths"])
    assert {
        "/api/v1/mobile/applications/{application_id}/documents/upload-authorizations",
        "/api/v1/mobile/applications/{application_id}/documents/finalize",
        "/api/v1/applications/{application_id}/documents/upload-authorizations",
        "/api/v1/applications/{application_id}/documents/finalize",
    } <= paths
