ALTER TABLE borrower_financial_profiles
ADD COLUMN IF NOT EXISTS analyst_metric_notes JSONB NOT NULL DEFAULT '{}'::jsonb;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'borrower_financial_profiles_analyst_metric_notes_object'
    ) THEN
        ALTER TABLE borrower_financial_profiles
        ADD CONSTRAINT borrower_financial_profiles_analyst_metric_notes_object
        CHECK (jsonb_typeof(analyst_metric_notes) = 'object');
    END IF;
END $$;
