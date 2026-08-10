-- Transaction-led feasibility inputs and safe backfill for existing intake drafts.

CREATE TABLE IF NOT EXISTS cashflow_entries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    application_id UUID NOT NULL REFERENCES loan_applications(id) ON DELETE CASCADE,
    flow_direction TEXT NOT NULL CHECK (flow_direction IN ('inflow', 'outflow')),
    classification TEXT NOT NULL CHECK (classification IN ('operating', 'investing', 'financing', 'personal', 'transfer')),
    category TEXT NOT NULL,
    amount NUMERIC(15,2) NOT NULL CHECK (amount >= 0),
    frequency TEXT NOT NULL DEFAULT 'monthly' CHECK (frequency IN ('daily', 'weekly', 'biweekly', 'monthly', 'quarterly', 'annual', 'period_total', 'one_off')),
    period_months NUMERIC(7,2) NOT NULL DEFAULT 1 CHECK (period_months > 0),
    entry_date DATE,
    description TEXT,
    channel TEXT,
    source_type TEXT NOT NULL DEFAULT 'manual',
    source_reference TEXT,
    is_recurring BOOLEAN NOT NULL DEFAULT TRUE,
    verification_status TEXT NOT NULL DEFAULT 'declared' CHECK (verification_status IN ('declared', 'system_extracted', 'under_review', 'verified', 'rejected', 'excluded', 'stale')),
    captured_by UUID NOT NULL REFERENCES users(id),
    verified_by UUID REFERENCES users(id),
    verified_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS ix_cashflow_entries_application ON cashflow_entries(application_id);
CREATE INDEX IF NOT EXISTS ix_cashflow_entries_assessment ON cashflow_entries(application_id, flow_direction, classification, verification_status);
CREATE UNIQUE INDEX IF NOT EXISTS ux_cashflow_legacy_seed
    ON cashflow_entries(application_id, source_reference, flow_direction, category)
    WHERE source_type = 'legacy_pnl_seed';

CREATE TABLE IF NOT EXISTS borrower_financial_profiles (
    application_id UUID PRIMARY KEY REFERENCES loan_applications(id) ON DELETE CASCADE,
    essential_household_expenses NUMERIC(15,2) NOT NULL DEFAULT 0 CHECK (essential_household_expenses >= 0),
    verified_other_income NUMERIC(15,2) NOT NULL DEFAULT 0 CHECK (verified_other_income >= 0),
    dependants INTEGER NOT NULL DEFAULT 0 CHECK (dependants >= 0),
    inventory_value NUMERIC(15,2) NOT NULL DEFAULT 0 CHECK (inventory_value >= 0),
    receivables_value NUMERIC(15,2) NOT NULL DEFAULT 0 CHECK (receivables_value >= 0),
    payables_value NUMERIC(15,2) NOT NULL DEFAULT 0 CHECK (payables_value >= 0),
    maintenance_capex NUMERIC(15,2) NOT NULL DEFAULT 0 CHECK (maintenance_capex >= 0),
    source_type TEXT NOT NULL DEFAULT 'manual',
    verification_status TEXT NOT NULL DEFAULT 'declared' CHECK (verification_status IN ('declared', 'under_review', 'verified', 'rejected', 'stale')),
    captured_by UUID NOT NULL REFERENCES users(id),
    verified_by UUID REFERENCES users(id),
    verified_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS credit_obligations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    application_id UUID NOT NULL REFERENCES loan_applications(id) ON DELETE CASCADE,
    lender_name TEXT NOT NULL,
    source_type TEXT NOT NULL DEFAULT 'declared',
    outstanding_balance NUMERIC(15,2) NOT NULL DEFAULT 0 CHECK (outstanding_balance >= 0),
    periodic_payment NUMERIC(15,2) NOT NULL DEFAULT 0 CHECK (periodic_payment >= 0),
    payment_frequency TEXT NOT NULL DEFAULT 'monthly' CHECK (payment_frequency IN ('daily', 'weekly', 'biweekly', 'monthly', 'quarterly', 'annual')),
    remaining_tenor_months INTEGER,
    status TEXT NOT NULL DEFAULT 'current',
    verification_status TEXT NOT NULL DEFAULT 'declared' CHECK (verification_status IN ('declared', 'system_extracted', 'under_review', 'verified', 'disputed', 'rejected', 'stale')),
    source_reference TEXT,
    captured_by UUID NOT NULL REFERENCES users(id),
    verified_by UUID REFERENCES users(id),
    verified_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS ix_credit_obligations_application ON credit_obligations(application_id);
CREATE UNIQUE INDEX IF NOT EXISTS ux_credit_obligations_source
    ON credit_obligations(application_id, source_reference)
    WHERE source_reference IS NOT NULL;

-- Existing draft P&L values become declared period totals. Period length is
-- inferred conservatively from the legacy free-text label and remains visible
-- for officer review rather than silently becoming verified evidence.
INSERT INTO cashflow_entries
    (application_id, flow_direction, classification, category, amount, frequency,
     period_months, description, source_type, source_reference, is_recurring,
     verification_status, captured_by)
SELECT
    bp.application_id,
    seeded.flow_direction,
    'operating',
    seeded.category,
    seeded.amount,
    'period_total',
    CASE
        WHEN lower(bp.period_label) LIKE '%annual%' OR lower(bp.period_label) LIKE '%year%'
             OR lower(bp.period_label) LIKE '%fy%' OR lower(bp.period_label) LIKE '%12 month%' THEN 12
        WHEN lower(bp.period_label) LIKE '%6 month%' OR lower(bp.period_label) LIKE '%half%' THEN 6
        WHEN lower(bp.period_label) LIKE '%quarter%' OR lower(bp.period_label) LIKE '%3 month%' THEN 3
        ELSE 1
    END,
    'Seeded from legacy P&L: ' || bp.period_label,
    'legacy_pnl_seed',
    'business_pnl:' || bp.id::text,
    TRUE,
    'declared',
    bp.created_by
FROM business_pnl bp
JOIN loan_applications la ON la.id = bp.application_id AND la.stage = 'intake'
CROSS JOIN LATERAL (
    VALUES ('inflow', 'sales_revenue', bp.revenue),
           ('outflow', 'operating_expenses', bp.expenses)
) AS seeded(flow_direction, category, amount)
WHERE seeded.amount IS NOT NULL
ON CONFLICT DO NOTHING;

-- Create a financial profile for every existing draft, preserving any values
-- already available in intake JSON without inventing missing household data.
WITH latest_intake AS (
    SELECT DISTINCT ON (sd.loan_id) sd.loan_id, sd.data_json, sd.saved_by
    FROM stage_data sd
    JOIN loan_applications la ON la.id = sd.loan_id AND la.stage = 'intake'
    WHERE sd.stage = 'intake'
    ORDER BY sd.loan_id, sd.saved_at DESC
)
INSERT INTO borrower_financial_profiles
    (application_id, essential_household_expenses, verified_other_income,
     dependants, inventory_value, receivables_value, payables_value,
     maintenance_capex, source_type, verification_status, captured_by)
SELECT
    loan_id,
    COALESCE(NULLIF(regexp_replace(COALESCE(data_json->>'household_expenses', ''), '[^0-9.-]', '', 'g'), '')::numeric, 0),
    COALESCE(NULLIF(regexp_replace(COALESCE(data_json->>'other_income', ''), '[^0-9.-]', '', 'g'), '')::numeric, 0),
    COALESCE(NULLIF(regexp_replace(COALESCE(data_json->>'dependants', ''), '[^0-9]', '', 'g'), '')::integer, 0),
    COALESCE(NULLIF(regexp_replace(COALESCE(data_json->>'inventory_value', ''), '[^0-9.-]', '', 'g'), '')::numeric, 0),
    COALESCE(NULLIF(regexp_replace(COALESCE(data_json->>'receivables_value', ''), '[^0-9.-]', '', 'g'), '')::numeric, 0),
    COALESCE(NULLIF(regexp_replace(COALESCE(data_json->>'payables_value', ''), '[^0-9.-]', '', 'g'), '')::numeric, 0),
    COALESCE(NULLIF(regexp_replace(COALESCE(data_json->>'maintenance_capex', ''), '[^0-9.-]', '', 'g'), '')::numeric, 0),
    'legacy_intake_seed',
    'declared',
    saved_by
FROM latest_intake
ON CONFLICT (application_id) DO NOTHING;

-- Salary-only drafts receive a declared recurring inflow when no business P&L
-- was available to seed. This prevents a destructive or fabricated backfill.
WITH latest_intake AS (
    SELECT DISTINCT ON (sd.loan_id) sd.loan_id, sd.data_json, sd.saved_by
    FROM stage_data sd
    JOIN loan_applications la ON la.id = sd.loan_id AND la.stage = 'intake'
    WHERE sd.stage = 'intake'
    ORDER BY sd.loan_id, sd.saved_at DESC
), salary_values AS (
    SELECT loan_id, saved_by,
           NULLIF(regexp_replace(COALESCE(data_json->>'monthly_salary', ''), '[^0-9.-]', '', 'g'), '')::numeric AS amount
    FROM latest_intake
)
INSERT INTO cashflow_entries
    (application_id, flow_direction, classification, category, amount, frequency,
     period_months, description, source_type, source_reference, is_recurring,
     verification_status, captured_by)
SELECT loan_id, 'inflow', 'operating', 'salary', amount, 'monthly', 1,
       'Seeded from existing draft salary declaration', 'legacy_salary_seed',
       'stage_data:intake:monthly_salary', TRUE, 'declared', saved_by
FROM salary_values sv
WHERE amount IS NOT NULL AND amount > 0
  AND NOT EXISTS (SELECT 1 FROM cashflow_entries ce WHERE ce.application_id = sv.loan_id AND ce.flow_direction = 'inflow')
ON CONFLICT DO NOTHING;

-- Preserve facilities already declared in step 5. Repeated form fields are
-- stored as JSON arrays, while older single-row drafts may contain scalars.
WITH latest_intake AS (
    SELECT DISTINCT ON (sd.loan_id) sd.loan_id, sd.data_json, sd.saved_by
    FROM stage_data sd
    JOIN loan_applications la ON la.id = sd.loan_id AND la.stage = 'intake'
    WHERE sd.stage = 'intake'
    ORDER BY sd.loan_id, sd.saved_at DESC
), normalized AS (
    SELECT loan_id, saved_by,
           CASE WHEN jsonb_typeof(data_json->'facility_bank') = 'array' THEN data_json->'facility_bank' ELSE jsonb_build_array(COALESCE(data_json->>'facility_bank', '')) END AS banks,
           CASE WHEN jsonb_typeof(data_json->'facility_amount') = 'array' THEN data_json->'facility_amount' ELSE jsonb_build_array(COALESCE(data_json->>'facility_amount', '0')) END AS balances,
           CASE WHEN jsonb_typeof(data_json->'facility_payment') = 'array' THEN data_json->'facility_payment' ELSE jsonb_build_array(COALESCE(data_json->>'facility_payment', '0')) END AS payments,
           CASE WHEN jsonb_typeof(data_json->'facility_frequency') = 'array' THEN data_json->'facility_frequency' ELSE jsonb_build_array(COALESCE(data_json->>'facility_frequency', 'monthly')) END AS frequencies,
           CASE WHEN jsonb_typeof(data_json->'facility_tenor') = 'array' THEN data_json->'facility_tenor' ELSE jsonb_build_array(COALESCE(data_json->>'facility_tenor', '')) END AS tenors,
           CASE WHEN jsonb_typeof(data_json->'facility_status') = 'array' THEN data_json->'facility_status' ELSE jsonb_build_array(COALESCE(data_json->>'facility_status', 'current')) END AS statuses
    FROM latest_intake
), expanded AS (
    SELECT n.*, bank.value AS lender_name, bank.ordinality,
           n.balances ->> (bank.ordinality::int - 1) AS balance_text,
           n.payments ->> (bank.ordinality::int - 1) AS payment_text,
           n.frequencies ->> (bank.ordinality::int - 1) AS frequency_text,
           n.tenors ->> (bank.ordinality::int - 1) AS tenor_text,
           n.statuses ->> (bank.ordinality::int - 1) AS status_text
    FROM normalized n
    CROSS JOIN LATERAL jsonb_array_elements_text(n.banks) WITH ORDINALITY AS bank(value, ordinality)
)
INSERT INTO credit_obligations
    (application_id, lender_name, source_type, outstanding_balance,
     periodic_payment, payment_frequency, remaining_tenor_months, status,
     verification_status, source_reference, captured_by)
SELECT loan_id, trim(lender_name), 'declared',
       COALESCE(NULLIF(regexp_replace(COALESCE(balance_text, ''), '[^0-9.-]', '', 'g'), '')::numeric, 0),
       COALESCE(NULLIF(regexp_replace(COALESCE(payment_text, ''), '[^0-9.-]', '', 'g'), '')::numeric, 0),
       CASE WHEN frequency_text IN ('daily','weekly','biweekly','monthly','quarterly','annual') THEN frequency_text ELSE 'monthly' END,
       NULLIF(regexp_replace(COALESCE(tenor_text, ''), '[^0-9]', '', 'g'), '')::integer,
       CASE WHEN status_text IN ('current','past_due','restructured','disputed') THEN status_text ELSE 'current' END,
       'declared',
       'stage_data:intake:facility:' || ordinality::text,
       saved_by
FROM expanded
WHERE trim(COALESCE(lender_name, '')) <> ''
ON CONFLICT DO NOTHING;
