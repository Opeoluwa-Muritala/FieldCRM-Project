-- migrations/030_business_pnl.sql
CREATE TABLE business_pnl (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    application_id UUID NOT NULL UNIQUE REFERENCES loan_applications(id) ON DELETE CASCADE,
    revenue NUMERIC NOT NULL,
    expenses NUMERIC NOT NULL,
    net_profit NUMERIC GENERATED ALWAYS AS (revenue - expenses) STORED,
    period_label TEXT NOT NULL,
    created_by UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
