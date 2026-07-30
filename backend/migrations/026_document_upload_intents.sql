-- One-time, tenant-scoped authorisations for direct browser/mobile uploads.
CREATE TABLE IF NOT EXISTS document_upload_intents (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organisations(id),
    application_id UUID NOT NULL REFERENCES loan_applications(id),
    actor_id UUID NOT NULL REFERENCES users(id),
    actor_role TEXT NOT NULL,
    document_type TEXT NOT NULL,
    form_code TEXT,
    original_name TEXT NOT NULL,
    mime_type TEXT NOT NULL,
    expected_size_bytes BIGINT NOT NULL CHECK (expected_size_bytes > 0),
    cloud_public_id TEXT NOT NULL UNIQUE,
    status TEXT NOT NULL DEFAULT 'pending'
        CHECK (status IN ('pending', 'finalized', 'expired')),
    document_id UUID REFERENCES documents(id),
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    finalized_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_document_upload_intents_pending
    ON document_upload_intents (expires_at)
    WHERE status = 'pending';
