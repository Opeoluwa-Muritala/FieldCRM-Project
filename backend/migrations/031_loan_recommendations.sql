-- migrations/031_loan_recommendations.sql
CREATE TABLE loan_recommendations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    application_id UUID NOT NULL REFERENCES loan_applications(id) ON DELETE CASCADE,
    submitted_by UUID NOT NULL REFERENCES users(id),
    role_at_submission TEXT NOT NULL,
    recommended_amount NUMERIC NOT NULL,
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX ix_loan_recommendations_application ON loan_recommendations (application_id);
