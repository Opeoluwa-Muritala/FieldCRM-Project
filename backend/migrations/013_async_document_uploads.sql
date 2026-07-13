-- Asynchronous, private document-upload tracking for FieldCRM loan files.
-- Existing documents remain the source of truth after a job completes.

ALTER TABLE documents
  ADD COLUMN IF NOT EXISTS upload_status TEXT NOT NULL DEFAULT 'done'
    CHECK (upload_status IN ('pending', 'done', 'failed')),
  ADD COLUMN IF NOT EXISTS upload_error TEXT,
  ADD COLUMN IF NOT EXISTS cloud_public_id TEXT,
  ADD COLUMN IF NOT EXISTS cloud_preview_url TEXT;

CREATE TABLE IF NOT EXISTS document_upload_jobs (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    loan_id         UUID NOT NULL REFERENCES loan_applications(id) ON DELETE CASCADE,
    org_id          UUID NOT NULL REFERENCES organisations(id) ON DELETE CASCADE,
    uploaded_by     UUID NOT NULL REFERENCES users(id),
    doc_type        TEXT NOT NULL,
    target_field    TEXT,
    original_name   TEXT NOT NULL,
    mime_type       TEXT NOT NULL,
    status          TEXT NOT NULL DEFAULT 'pending'
                    CHECK (status IN ('pending', 'done', 'failed')),
    url             TEXT,
    public_id       TEXT,
    document_id     UUID REFERENCES documents(id) ON DELETE SET NULL,
    error           TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS ix_document_upload_jobs_loan_status
    ON document_upload_jobs (loan_id, status, created_at DESC);
CREATE INDEX IF NOT EXISTS ix_document_upload_jobs_user
    ON document_upload_jobs (uploaded_by, created_at DESC);
