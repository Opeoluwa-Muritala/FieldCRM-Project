ALTER TABLE visitation_reports
    ADD COLUMN IF NOT EXISTS visit_time TIME,
    ADD COLUMN IF NOT EXISTS relationship TEXT,
    ADD COLUMN IF NOT EXISTS visiting_officer_name TEXT,
    ADD COLUMN IF NOT EXISTS account_officer_name TEXT,
    ADD COLUMN IF NOT EXISTS visiting_officer_signature_data TEXT,
    ADD COLUMN IF NOT EXISTS account_officer_signature_data TEXT,
    ADD COLUMN IF NOT EXISTS gps_coordinates TEXT,
    ADD COLUMN IF NOT EXISTS site_photo_url TEXT,
    ADD COLUMN IF NOT EXISTS manager_signature_data TEXT,
    ADD COLUMN IF NOT EXISTS concurrence_return_reason TEXT;
