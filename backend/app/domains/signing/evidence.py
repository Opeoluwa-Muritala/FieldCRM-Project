from __future__ import annotations

import hashlib
import html


def evidential_wording(*, signed_at, submission_id, version, organisation, reference) -> str:
    return (
        f"This document was completed and electronically signed through FieldCRM on "
        f"{html.escape(str(signed_at))}. It is a printable representation of electronic "
        f"record {html.escape(str(submission_id))}, version {html.escape(str(version))}. "
        f"The original electronic record, integrity hash and associated audit trail are "
        f"retained by {html.escape(str(organisation))}. Verification reference: "
        f"{html.escape(str(reference))}."
    )


def pdf_sha256(pdf_bytes: bytes) -> str:
    return hashlib.sha256(pdf_bytes).hexdigest()


async def link_pdf_evidence(conn, signature_event_ids, pdf_bytes: bytes, storage_ref: str) -> str:
    """Append PDF/hash links without mutating append-only signature events."""
    digest = pdf_sha256(pdf_bytes)
    async with conn.transaction():
        for event_id in signature_event_ids:
            await conn.execute(
                """INSERT INTO signature_event_pdfs(signature_event_id,pdf_sha256,storage_ref)
                   VALUES ($1,$2,$3) ON CONFLICT (signature_event_id,pdf_sha256) DO NOTHING""",
                event_id, digest, storage_ref,
            )
            await conn.execute(
                "UPDATE signature_events SET pdf_sha256 = $1 WHERE id = $2",
                digest, event_id
            )
    return digest
