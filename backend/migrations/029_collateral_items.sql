-- migrations/029_collateral_items.sql
CREATE TABLE collateral_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    application_id UUID NOT NULL REFERENCES loan_applications(id) ON DELETE CASCADE,
    collateral_type TEXT NOT NULL CHECK (collateral_type IN ('property', 'equipment', 'inventory', 'cash')),
    narration TEXT NOT NULL,
    loan_based_price NUMERIC,
    face_value NUMERIC,
    force_sale_value NUMERIC GENERATED ALWAYS AS (
        CASE WHEN collateral_type = 'cash' THEN COALESCE(face_value, 0)
             ELSE COALESCE(loan_based_price, 0) * 0.70
        END
    ) STORED,
    created_by UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX ix_collateral_items_application ON collateral_items (application_id);

CREATE TABLE collateral_documents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    collateral_item_id UUID NOT NULL REFERENCES collateral_items(id) ON DELETE CASCADE,
    cloudinary_public_id TEXT NOT NULL,
    cloudinary_url TEXT NOT NULL,
    document_type TEXT NOT NULL,
    worth NUMERIC NOT NULL,
    uploaded_by UUID NOT NULL REFERENCES users(id),
    uploaded_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX ix_collateral_documents_item ON collateral_documents (collateral_item_id);
