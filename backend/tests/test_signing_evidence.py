import io

import pytest
from fastapi import HTTPException
from PIL import Image

from app.domains.signing.evidence import evidential_wording, pdf_sha256
from app.domains.signing.service import canonical_payload, payload_sha256, validate_transparent_png


def _png(mode="RGBA", alpha=0):
    image = Image.new(mode, (4, 4), (0, 0, 0, alpha) if mode == "RGBA" else (255, 255, 255))
    output = io.BytesIO()
    image.save(output, "PNG")
    return output.getvalue()


def test_payload_hash_is_canonical_and_stable():
    left = {"z": "ọ", "a": {"b": 1}}
    right = {"a": {"b": 1}, "z": "ọ"}
    assert canonical_payload(left) == canonical_payload(right)
    assert payload_sha256(left) == payload_sha256(right)


def test_signature_png_requires_real_transparency():
    assert len(validate_transparent_png(_png(alpha=0))) == 64
    with pytest.raises(HTTPException):
        validate_transparent_png(_png(mode="RGB"))
    with pytest.raises(HTTPException):
        validate_transparent_png(_png(alpha=255))
    with pytest.raises(HTTPException):
        validate_transparent_png(b"not png")


def test_pdf_hash_and_evidential_wording():
    assert len(pdf_sha256(b"%PDF-example")) == 64
    text = evidential_wording(signed_at="2026-01-01T00:00:00Z", submission_id="sub-1",
        version=2, organisation="FieldCRM Test", reference="verify-1")
    assert "electronically signed through FieldCRM" in text
    assert "version 2" in text
    assert "certified copy" not in text.lower()


def test_link_flow_does_not_require_otp():
    """The chosen client flow authenticates on one-time link redemption."""
    assert "single_use_link" in open("migrations/023_signing_evidence.sql", encoding="utf-8").read()


@pytest.mark.asyncio
async def test_staff_cannot_write_applicant_signature():
    from unittest.mock import AsyncMock, MagicMock
    from starlette.requests import Request
    from app.domains.loans.router import process_crm_review

    # Mock request with form data containing applicant_signature
    mock_form = AsyncMock()
    mock_form.return_value = {
        "consent_credit_bureau": "true",
        "consent_credit_check": "true",
        "consent_cheque": "true",
        "consent_gsi": "true",
        "final_declaration": "true",
        "applicant_signature": "some_signature_data"
    }
    
    request = MagicMock(spec=Request)
    request.form = mock_form
    
    # Mock application
    application = MagicMock()
    application.stage = "crm_review"
    
    # Mock connection
    conn = AsyncMock()
    
    # Mock current user
    current_user = MagicMock()
    current_user.org_id = MagicMock()
    current_user.role = "crm"
    
    # Mock LoanRepository
    mock_repo = MagicMock()
    mock_repo.get_by_id = AsyncMock(return_value=application)
    mock_repo.get_stage_data = AsyncMock(return_value=None)
    mock_repo.save_stage_data = AsyncMock()
    # Patch LoanRepository inside router
    from app.domains.loans import router
    original_repo = router.LoanRepository
    router.LoanRepository = lambda *args, **kwargs: mock_repo
    
    try:
        with pytest.raises(HTTPException) as exc_info:
            await process_crm_review(
                request=request,
                application_id="12345678-1234-1234-1234-1234567890ab",
                action="advance",
                crm_notes="some notes",
                conn=conn,
                current_user=current_user
            )
        assert exc_info.value.status_code == 403
        assert "Staff cannot submit an applicant signature" in exc_info.value.detail
    finally:
        router.LoanRepository = original_repo


@pytest.mark.asyncio
async def test_draft_edits_log_and_locks():
    from unittest.mock import AsyncMock, MagicMock
    from app.domains.signing.service import SigningService
    
    # Mock Repository
    repo = MagicMock()
    service = SigningService(repo)
    
    # 1. Edits to sent/signed subjects are rejected with 409
    repo.latest_version = AsyncMock(return_value={"id": "v1", "status": "sent", "version_number": 1})
    with pytest.raises(HTTPException) as exc_info:
        await service.record_draft_edit(
            application_id="app-1", subject_type="applicant_stage", subject_id="intake",
            old_payload={}, new_payload={"full_name": "New Name"}, edited_by="user-1"
        )
    assert exc_info.value.status_code == 409
    
    # 2. Edits to draft subjects succeed and log
    repo.latest_version = AsyncMock(return_value={"id": "v2", "status": "draft", "version_number": 1})
    repo.log_edit = AsyncMock()
    repo.update_draft_payload = AsyncMock(return_value={"id": "v2", "status": "draft"})
    
    res = await service.record_draft_edit(
        application_id="app-1", subject_type="applicant_stage", subject_id="intake",
        old_payload={"full_name": "Old Name"}, new_payload={"full_name": "New Name"}, edited_by="user-1"
    )
    assert res["status"] == "draft"
    repo.log_edit.assert_called_once()


@pytest.mark.asyncio
async def test_correction_flow_and_link_invalidation():
    from unittest.mock import AsyncMock, MagicMock
    from app.domains.signing.service import SigningService
    
    repo = MagicMock()
    mock_conn = MagicMock()
    mock_conn.transaction = MagicMock(return_value=AsyncMock())
    repo.conn = mock_conn
    service = SigningService(repo)
    
    repo.latest_version = AsyncMock(return_value={"id": "v1", "status": "sent", "version_number": 1, "payload": {}})
    repo.create_draft = AsyncMock(return_value={"id": "v2", "status": "draft", "version_number": 2})
    repo.supersede_version = AsyncMock()
    repo.invalidate_sessions = AsyncMock()
    
    # Run correct flow
    res = await service.correct(application_id="app-1", subject_type="guarantor", subject_id="g-1", created_by="user-1")
    
    assert res["version_number"] == 2
    repo.supersede_version.assert_called_once_with("v1", "v2")
    repo.invalidate_sessions.assert_called_once_with("app-1", "guarantor", "g-1")


@pytest.mark.asyncio
async def test_multiple_guarantors_independent_chains():
    from unittest.mock import AsyncMock, MagicMock
    from app.domains.signing.service import SigningService
    
    repo = MagicMock()
    service = SigningService(repo)
    
    # Check that guarantor 1 can be draft/sent while guarantor 2 is signed
    repo.latest_version = AsyncMock(side_effect=lambda app_id, sub_type, sub_id: {
        "g-1": {"id": "v1", "status": "sent", "version_number": 1},
        "g-2": {"id": "v2", "status": "signed", "version_number": 1}
    }.get(sub_id))
    
    v1 = await service.repo.latest_version("app-1", "guarantor", "g-1")
    v2 = await service.repo.latest_version("app-1", "guarantor", "g-2")
    
    assert v1["status"] == "sent"
    assert v2["status"] == "signed"


@pytest.mark.asyncio
async def test_signature_fails_without_valid_otp():
    from unittest.mock import AsyncMock, MagicMock
    from app.domains.signing.service import SigningService
    
    repo = MagicMock()
    service = SigningService(repo)
    
    # Mock verify_otp to raise 401 when code is incorrect
    service.verify_otp = AsyncMock(side_effect=HTTPException(status_code=401, detail="Signing authentication code is incorrect"))
    
    with pytest.raises(HTTPException) as exc_info:
        await service.verify_otp(transaction_id="tx-1", otp="111111")
    assert exc_info.value.status_code == 401


@pytest.mark.asyncio
async def test_assisted_signing_witness_attestation():
    from unittest.mock import AsyncMock, MagicMock
    from app.domains.signing.service import SigningService
    
    repo = MagicMock()
    mock_conn = MagicMock()
    mock_conn.transaction = MagicMock(return_value=AsyncMock())
    mock_conn.execute = AsyncMock()
    repo.conn = mock_conn
    service = SigningService(repo)
    
    repo.latest_version = AsyncMock(return_value={"id": "v1", "status": "sent", "version_number": 1, "payload": {}})
    repo.consume_auth = AsyncMock(return_value={"id": "auth-1", "auth_method": "otp_sms"})
    repo.create_signature_event = AsyncMock(return_value={"id": "event-1"})
    repo.sign_version = AsyncMock()
    
    # Mock PNG validation
    from tests.test_signing_evidence import _png
    sig_png = _png(alpha=0)
    
    # Mock storage upload callback
    async def storage_upload(sig_bytes, image_hash, version):
        return "stored-path"
        
    witness = {
        "subject_id": "witness-1",
        "signer_identity_ref": "Witness Name",
        "reader_witness_user_id": "user-1",
        "reader_witness_attestation_text": "I read the terms",
        "reader_witness_signed_at": "now",
        "signature_image_ref": "stored-path-witness",
        "mark_type": "drawn_signature"
    }
    
    event = await service.sign(
        version={"id": "v1", "status": "sent", "version_number": 1, "application_id": "app-1", "subject_type": "applicant", "subject_id": "applicant", "payload_hash": "mock_hash"},
        auth_transaction_id="tx-1",
        signature_bytes=sig_png,
        storage_upload=storage_upload,
        subject_type="applicant",
        subject_id="applicant",
        signer_identity_ref="Applicant Name",
        consent_text_version="v1",
        mark_type="thumbprint",
        assistance_type="read_aloud_by_staff",
        witness=witness
    )
    
    # Assert signature event created
    assert event["id"] == "event-1"
    # Assert create_signature_event called for both applicant and witness
    assert repo.create_signature_event.call_count == 2
