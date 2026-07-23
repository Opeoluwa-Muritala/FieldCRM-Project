-- Versioned signing subjects and immutable electronic evidence.
CREATE TABLE IF NOT EXISTS document_versions (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    application_id uuid NOT NULL REFERENCES loan_applications(id),
    subject_type varchar(32) NOT NULL CHECK (subject_type IN ('applicant_stage', 'guarantor')),
    subject_id varchar(255) NOT NULL,
    version_number integer NOT NULL CHECK (version_number > 0),
    payload jsonb NOT NULL DEFAULT '{}'::jsonb,
    payload_hash varchar(64),
    status varchar(16) NOT NULL DEFAULT 'draft' CHECK (status IN ('draft','sent','signed','superseded')),
    created_by uuid NOT NULL REFERENCES users(id),
    created_at timestamptz NOT NULL DEFAULT now(),
    frozen_at timestamptz,
    superseded_by uuid REFERENCES document_versions(id) DEFERRABLE INITIALLY DEFERRED,
    UNIQUE (application_id, subject_type, subject_id, version_number),
    CHECK ((status = 'draft' AND payload_hash IS NULL AND frozen_at IS NULL)
        OR (status <> 'draft' AND payload_hash IS NOT NULL))
);

CREATE INDEX IF NOT EXISTS ix_document_versions_subject
ON document_versions(application_id, subject_type, subject_id, version_number DESC);

CREATE UNIQUE INDEX IF NOT EXISTS ux_document_versions_current
ON document_versions(application_id, subject_type, subject_id)
WHERE status <> 'superseded';

CREATE TABLE IF NOT EXISTS field_edit_log (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    document_version_id uuid NOT NULL REFERENCES document_versions(id),
    field_name varchar(255) NOT NULL,
    old_value text,
    new_value text,
    edited_by uuid NOT NULL REFERENCES users(id),
    edited_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS signing_sessions (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    application_id uuid NOT NULL REFERENCES loan_applications(id),
    subject_type varchar(32) NOT NULL CHECK (subject_type IN ('applicant','guarantor')),
    subject_id varchar(255) NOT NULL,
    document_version_ids jsonb NOT NULL DEFAULT '[]'::jsonb,
    token_jti uuid NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    token_hash varchar(64) UNIQUE,
    issued_by uuid REFERENCES users(id),
    issued_at timestamptz NOT NULL DEFAULT now(),
    expires_at timestamptz NOT NULL,
    invalidated_at timestamptz,
    document_viewed_at timestamptz,
    redeemed_at timestamptz,
    session_expires_at timestamptz,
    used_at timestamptz
);

CREATE INDEX IF NOT EXISTS ix_signing_sessions_subject
ON signing_sessions(application_id, subject_type, subject_id, expires_at DESC);

CREATE TABLE IF NOT EXISTS signing_auth_sessions (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    signing_session_id uuid NOT NULL REFERENCES signing_sessions(id),
    auth_method varchar(24) NOT NULL CHECK (auth_method IN ('single_use_link','otp_sms','otp_email','voice_otp','assisted')),
    transaction_id varchar(255) NOT NULL UNIQUE,
    otp_digest varchar(255),
    expires_at timestamptz NOT NULL,
    verified_at timestamptz,
    used_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS signature_events (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    document_version_id uuid NOT NULL REFERENCES document_versions(id),
    application_id uuid NOT NULL REFERENCES loan_applications(id),
    subject_type varchar(16) NOT NULL CHECK (subject_type IN ('applicant','guarantor','witness')),
    subject_id varchar(255) NOT NULL,
    signer_identity_ref varchar(255) NOT NULL,
    auth_method varchar(24) NOT NULL CHECK (auth_method IN ('single_use_link','otp_sms','otp_email','voice_otp','assisted')),
    auth_transaction_id varchar(255) NOT NULL,
    signed_at timestamptz NOT NULL DEFAULT now(),
    ip_address inet,
    user_agent varchar(1024),
    consent_text_version varchar(64) NOT NULL,
    signature_image_ref varchar(1024),
    mark_type varchar(24) CHECK (mark_type IN ('drawn_signature','thumbprint','other_mark')),
    assistance_type varchar(48) CHECK (assistance_type IN ('self_read','read_aloud_by_staff','read_aloud_by_family_witness')),
    reader_witness_user_id uuid REFERENCES users(id),
    reader_witness_attestation_text text,
    reader_witness_signed_at timestamptz,
    payload_hash varchar(64) NOT NULL,
    pdf_sha256 varchar(64),
    witness_for_event_id uuid REFERENCES signature_events(id),
    UNIQUE (document_version_id, subject_type, subject_id)
);

CREATE TABLE IF NOT EXISTS signature_event_pdfs (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    signature_event_id uuid NOT NULL REFERENCES signature_events(id),
    pdf_sha256 varchar(64) NOT NULL,
    storage_ref varchar(1024) NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    UNIQUE(signature_event_id, pdf_sha256)
);

ALTER TABLE loan_applications ADD COLUMN IF NOT EXISTS assistance_required boolean NOT NULL DEFAULT false;
ALTER TABLE guarantors ADD COLUMN IF NOT EXISTS assistance_required boolean NOT NULL DEFAULT false;

REVOKE UPDATE, DELETE ON signature_events FROM fieldcrm_app;
GRANT SELECT, INSERT ON signature_events TO fieldcrm_app;
REVOKE UPDATE, DELETE ON signature_event_pdfs FROM fieldcrm_app;
GRANT SELECT, INSERT ON signature_event_pdfs TO fieldcrm_app;
DROP TRIGGER IF EXISTS signature_events_append_only ON signature_events;
CREATE TRIGGER signature_events_append_only BEFORE UPDATE OR DELETE ON signature_events
FOR EACH ROW EXECUTE FUNCTION public.prevent_history_mutation();
DROP TRIGGER IF EXISTS signature_event_pdfs_append_only ON signature_event_pdfs;
CREATE TRIGGER signature_event_pdfs_append_only BEFORE UPDATE OR DELETE ON signature_event_pdfs
FOR EACH ROW EXECUTE FUNCTION public.prevent_history_mutation();
