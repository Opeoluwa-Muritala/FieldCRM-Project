-- FieldCRM Full Schema — Mainstreet Microfinance Bank
-- Drops all existing tables and recreates with normalized UUID-keyed schema.
-- Requires PostgreSQL 13+ with pgcrypto extension.

-- =============================================================================
-- EXTENSIONS
-- =============================================================================
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- =============================================================================
-- DROP OLD TABLES (cascade to remove dependencies)
-- =============================================================================
DROP TABLE IF EXISTS workflow_event CASCADE;
DROP TABLE IF EXISTS stage_data CASCADE;
DROP TABLE IF EXISTS document CASCADE;
DROP TABLE IF EXISTS promise_to_pay CASCADE;
DROP TABLE IF EXISTS repayment_record CASCADE;
DROP TABLE IF EXISTS repayment_schedule CASCADE;
DROP TABLE IF EXISTS communication_log CASCADE;
DROP TABLE IF EXISTS group_member CASCADE;
DROP TABLE IF EXISTS "group" CASCADE;
DROP TABLE IF EXISTS loan_application CASCADE;
DROP TABLE IF EXISTS borrower CASCADE;
DROP TABLE IF EXISTS branch CASCADE;
DROP TABLE IF EXISTS "user" CASCADE;
DROP TABLE IF EXISTS organisation CASCADE;

-- Also drop new-schema tables if they exist (idempotent reruns)
DROP TABLE IF EXISTS audit_entries CASCADE;
DROP TABLE IF EXISTS visitation_reports CASCADE;
DROP TABLE IF EXISTS workflow_events CASCADE;
DROP TABLE IF EXISTS ocr_fields CASCADE;
DROP TABLE IF EXISTS ocr_results CASCADE;
DROP TABLE IF EXISTS documents CASCADE;
DROP TABLE IF EXISTS pledged_items CASCADE;
DROP TABLE IF EXISTS guarantors CASCADE;
DROP TABLE IF EXISTS stage_data CASCADE;
DROP TABLE IF EXISTS loan_applications CASCADE;
DROP TABLE IF EXISTS users CASCADE;
DROP TABLE IF EXISTS organisations CASCADE;

-- Drop sequences if they exist
DROP SEQUENCE IF EXISTS loan_ref_seq CASCADE;

-- =============================================================================
-- ORGANISATIONS (multi-tenant root)
-- =============================================================================
CREATE TABLE organisations (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        TEXT NOT NULL,
    code        TEXT NOT NULL UNIQUE,
    active      BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- =============================================================================
-- USERS
-- =============================================================================
CREATE TABLE users (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    org_id          UUID NOT NULL REFERENCES organisations(id),
    full_name       TEXT NOT NULL,
    email           TEXT NOT NULL,
    password_hash   TEXT NOT NULL,
    role            TEXT NOT NULL CHECK (role IN (
                        'loan_officer','credit_officer',
                        'branch_manager','auditor','system_admin'
                    )),
    active          BOOLEAN NOT NULL DEFAULT TRUE,
    last_login_at   TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (org_id, email)
);
CREATE INDEX ix_users_org_role ON users (org_id, role);

-- =============================================================================
-- LOAN APPLICATIONS
-- =============================================================================
CREATE TABLE loan_applications (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    org_id              UUID NOT NULL REFERENCES organisations(id),
    ref_no              TEXT NOT NULL,
    customer_type       TEXT NOT NULL CHECK (customer_type IN ('new','existing')),
    loan_type           TEXT NOT NULL CHECK (loan_type IN ('enterprise','msef','payee','other')),
    stage               TEXT NOT NULL DEFAULT 'intake' CHECK (stage IN (
                            'intake','ocr_review','credit_review',
                            'branch_approval','disbursement_ready',
                            'disbursed','returned','rejected'
                        )),
    applicant_name      TEXT NOT NULL,
    bvn                 TEXT,
    phone               TEXT,
    amount              NUMERIC(15,2) CHECK (amount > 0),
    tenor_months        INTEGER CHECK (tenor_months > 0),
    purpose             TEXT,
    repayment_mode      TEXT CHECK (repayment_mode IN (
                            'cheque','standing_order','direct_debit','cash_deposit'
                        )),
    created_by          UUID NOT NULL REFERENCES users(id),
    current_owner_id    UUID REFERENCES users(id),
    credit_officer_id   UUID REFERENCES users(id),
    branch_manager_id   UUID REFERENCES users(id),
    return_reason       TEXT,
    returned_at         TIMESTAMPTZ,
    approved_by         UUID REFERENCES users(id),
    approved_at         TIMESTAMPTZ,
    disbursed_at        TIMESTAMPTZ,
    deleted_at          TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (org_id, ref_no)
);
CREATE INDEX ix_loan_org_stage    ON loan_applications (org_id, stage)
    WHERE deleted_at IS NULL;
CREATE INDEX ix_loan_org_officer  ON loan_applications (org_id, created_by)
    WHERE deleted_at IS NULL;
CREATE INDEX ix_loan_org_updated  ON loan_applications (org_id, updated_at DESC)
    WHERE deleted_at IS NULL;

-- =============================================================================
-- STAGE DATA (JSON payload per workflow stage)
-- =============================================================================
CREATE TABLE stage_data (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    loan_id     UUID NOT NULL REFERENCES loan_applications(id) ON DELETE CASCADE,
    stage       TEXT NOT NULL,
    data_json   JSONB NOT NULL DEFAULT '{}',
    saved_by    UUID NOT NULL REFERENCES users(id),
    saved_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX ix_stage_data_loan  ON stage_data (loan_id, stage);

-- =============================================================================
-- GUARANTORS
-- =============================================================================
CREATE TABLE guarantors (
    id                          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    loan_id                     UUID NOT NULL REFERENCES loan_applications(id) ON DELETE CASCADE,
    org_id                      UUID NOT NULL REFERENCES organisations(id),
    slot                        INTEGER NOT NULL CHECK (slot IN (1,2)),
    full_name                   TEXT,
    relationship_to_client      TEXT,
    bvn                         TEXT,
    phone                       TEXT,
    home_address                TEXT,
    employment_type             TEXT,
    monthly_salary              NUMERIC(15,2),
    max_guarantee_amount        NUMERIC(15,2),
    max_guarantee_amount_words  TEXT,
    bank_name                   TEXT,
    account_number              TEXT,
    cheque_number               TEXT,
    form_stage                  TEXT NOT NULL DEFAULT 'draft' CHECK (form_stage IN (
                                    'draft','submitted','ocr_review',
                                    'verified','returned'
                                )),
    signature_detected          BOOLEAN NOT NULL DEFAULT FALSE,
    witness_signature_detected  BOOLEAN NOT NULL DEFAULT FALSE,
    created_at                  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at                  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (loan_id, slot)
);
CREATE INDEX ix_guarantor_loan ON guarantors (loan_id);

-- =============================================================================
-- PLEDGED ITEMS (collateral schedule)
-- =============================================================================
CREATE TABLE pledged_items (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    loan_id         UUID NOT NULL REFERENCES loan_applications(id) ON DELETE CASCADE,
    item_number     INTEGER NOT NULL,
    item_name       TEXT NOT NULL,
    serial_number   TEXT,
    description     TEXT,
    estimated_value NUMERIC(15,2) CHECK (estimated_value >= 0),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX ix_pledged_loan ON pledged_items (loan_id);

-- =============================================================================
-- DOCUMENTS
-- =============================================================================
CREATE TABLE documents (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    loan_id         UUID NOT NULL REFERENCES loan_applications(id) ON DELETE CASCADE,
    org_id          UUID NOT NULL REFERENCES organisations(id),
    guarantor_id    UUID REFERENCES guarantors(id),
    doc_type        TEXT NOT NULL,
    form_code       TEXT,
    original_name   TEXT NOT NULL,
    stored_path     TEXT NOT NULL,
    mime_type       TEXT NOT NULL,
    size_bytes      INTEGER NOT NULL,
    quality_status  TEXT NOT NULL DEFAULT 'pending' CHECK (quality_status IN (
                        'pending','clear','blurry','cropped','unreadable'
                    )),
    verified        BOOLEAN NOT NULL DEFAULT FALSE,
    verified_by     UUID REFERENCES users(id),
    verified_at     TIMESTAMPTZ,
    uploaded_by     UUID NOT NULL REFERENCES users(id),
    uploaded_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at      TIMESTAMPTZ
);
CREATE INDEX ix_doc_loan          ON documents (loan_id) WHERE deleted_at IS NULL;
CREATE INDEX ix_doc_loan_type     ON documents (loan_id, doc_type) WHERE deleted_at IS NULL;
CREATE INDEX ix_doc_unverified    ON documents (loan_id) WHERE verified = FALSE AND deleted_at IS NULL;

-- =============================================================================
-- OCR RESULTS
-- =============================================================================
CREATE TABLE ocr_results (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    document_id         UUID NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    loan_id             UUID NOT NULL REFERENCES loan_applications(id),
    form_type           TEXT NOT NULL CHECK (form_type IN (
                            'loan_application','guarantor','pledge_receipt'
                        )),
    overall_confidence  NUMERIC(5,2),
    raw_extraction      JSONB NOT NULL DEFAULT '{}',
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX ix_ocr_document ON ocr_results (document_id);
CREATE INDEX ix_ocr_loan     ON ocr_results (loan_id);

-- =============================================================================
-- OCR EXTRACTED FIELDS (one row per field)
-- =============================================================================
CREATE TABLE ocr_fields (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ocr_result_id   UUID NOT NULL REFERENCES ocr_results(id) ON DELETE CASCADE,
    loan_id         UUID NOT NULL REFERENCES loan_applications(id),
    field_name      TEXT NOT NULL,
    ocr_value       TEXT,
    corrected_value TEXT,
    final_value     TEXT GENERATED ALWAYS AS (
                        COALESCE(corrected_value, ocr_value)
                    ) STORED,
    confidence      NUMERIC(5,2),
    source          TEXT NOT NULL DEFAULT 'ocr' CHECK (source IN (
                        'ocr','manual','corrected','approved'
                    )),
    is_critical     BOOLEAN NOT NULL DEFAULT FALSE,
    verified        BOOLEAN NOT NULL DEFAULT FALSE,
    verified_by     UUID REFERENCES users(id),
    verified_at     TIMESTAMPTZ,
    corrected_by    UUID REFERENCES users(id),
    corrected_at    TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX ix_ocr_fields_result     ON ocr_fields (ocr_result_id);
CREATE INDEX ix_ocr_fields_loan       ON ocr_fields (loan_id);
CREATE INDEX ix_ocr_fields_low_conf   ON ocr_fields (loan_id)
    WHERE confidence < 70 AND verified = FALSE;
CREATE INDEX ix_ocr_fields_unverified ON ocr_fields (loan_id)
    WHERE is_critical = TRUE AND verified = FALSE;

-- =============================================================================
-- WORKFLOW EVENTS (immutable log)
-- =============================================================================
CREATE TABLE workflow_events (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    loan_id         UUID NOT NULL REFERENCES loan_applications(id),
    org_id          UUID NOT NULL REFERENCES organisations(id),
    event_type      TEXT NOT NULL,
    from_stage      TEXT,
    to_stage        TEXT,
    triggered_by    UUID NOT NULL REFERENCES users(id),
    triggered_role  TEXT NOT NULL,
    notes           TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX ix_workflow_loan       ON workflow_events (loan_id, created_at DESC);
CREATE INDEX ix_workflow_org_date   ON workflow_events (org_id, created_at DESC);

-- =============================================================================
-- VISITATION REPORTS
-- =============================================================================
CREATE TABLE visitation_reports (
    id                          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    loan_id                     UUID NOT NULL REFERENCES loan_applications(id),
    org_id                      UUID NOT NULL REFERENCES organisations(id),
    visit_date                  DATE,
    met_with                    TEXT,
    premises_description        TEXT,
    direction_from_branch       TEXT,
    business_condition          TEXT,
    visiting_officer_id         UUID REFERENCES users(id),
    visiting_officer_signature  BOOLEAN NOT NULL DEFAULT FALSE,
    account_officer_id          UUID REFERENCES users(id),
    manager_concurrence         BOOLEAN NOT NULL DEFAULT FALSE,
    manager_id                  UUID REFERENCES users(id),
    manager_notes               TEXT,
    manager_concurred_at        TIMESTAMPTZ,
    status                      TEXT NOT NULL DEFAULT 'pending' CHECK (status IN (
                                    'pending','submitted','concurred','returned'
                                )),
    created_at                  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at                  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE UNIQUE INDEX ix_visitation_loan ON visitation_reports (loan_id);

-- =============================================================================
-- AUDIT ENTRIES (append-only, never updated or deleted)
-- =============================================================================
CREATE TABLE audit_entries (
    id              BIGSERIAL PRIMARY KEY,
    org_id          UUID NOT NULL,
    entity_type     TEXT NOT NULL,
    entity_id       UUID NOT NULL,
    action          TEXT NOT NULL,
    user_id         UUID NOT NULL,
    user_role       TEXT NOT NULL,
    field_name      TEXT,
    old_value       TEXT,
    new_value       TEXT,
    source          TEXT,
    notes           TEXT,
    request_id      TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX ix_audit_entity    ON audit_entries (entity_type, entity_id, created_at DESC);
CREATE INDEX ix_audit_org_date  ON audit_entries (org_id, created_at DESC);
CREATE INDEX ix_audit_user      ON audit_entries (user_id, created_at DESC);
-- NOTE: REVOKE UPDATE, DELETE on audit_entries should be done by DBA
-- after creating the application DB role. Skipped here for Neon compatibility.
