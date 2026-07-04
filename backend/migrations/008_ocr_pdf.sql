-- Migration 008: OCR PDF tracking + Cloudinary cloud storage columns
-- Adds ocr_status tracking and Cloudinary metadata to documents.

-- Track OCR processing state on each uploaded document
ALTER TABLE documents ADD COLUMN IF NOT EXISTS ocr_status TEXT NOT NULL DEFAULT 'pending'
    CHECK (ocr_status IN ('pending','processing','done','failed','skipped'));

-- Cloudinary cloud storage metadata
ALTER TABLE documents ADD COLUMN IF NOT EXISTS cloud_public_id   TEXT;
ALTER TABLE documents ADD COLUMN IF NOT EXISTS cloud_preview_url TEXT;

-- Allow crm_review and executive_approval stages in mobile stage helper
-- (Already added in 007_servicing_roles.sql for loan_applications)

-- Add system_admin and crm to allowed roles if not already present
-- (007 already handles role CHECK, this ensures no-op on re-run)
DO $$
BEGIN
    -- Extend stage_data to record OCR source per extraction
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name='ocr_fields' AND column_name='page_number'
    ) THEN
        ALTER TABLE ocr_fields ADD COLUMN page_number INTEGER;
    END IF;
END
$$;
