-- Phase 4: draft-bound products, dynamic forms, contextual document readiness.
ALTER TABLE loan_products ADD COLUMN IF NOT EXISTS org_id UUID REFERENCES organisations(id) ON DELETE CASCADE;
ALTER TABLE loan_products ADD COLUMN IF NOT EXISTS configuration_version_id UUID REFERENCES configuration_versions(id) ON DELETE RESTRICT;
ALTER TABLE loan_products ADD COLUMN IF NOT EXISTS interest_parameters JSONB NOT NULL DEFAULT '{}'::jsonb;
ALTER TABLE loan_products ADD COLUMN IF NOT EXISTS guarantor_count INTEGER NOT NULL DEFAULT 0 CHECK (guarantor_count BETWEEN 0 AND 20);
ALTER TABLE loan_products ADD COLUMN IF NOT EXISTS collateral_rules JSONB NOT NULL DEFAULT '{}'::jsonb;
ALTER TABLE loan_products ADD COLUMN IF NOT EXISTS approval_limits JSONB NOT NULL DEFAULT '{}'::jsonb;
ALTER TABLE loan_products ADD COLUMN IF NOT EXISTS visit_requirements JSONB NOT NULL DEFAULT '{}'::jsonb;
ALTER TABLE loan_products ADD COLUMN IF NOT EXISTS credit_checks JSONB NOT NULL DEFAULT '{}'::jsonb;
ALTER TABLE loan_products ADD COLUMN IF NOT EXISTS sla_hours INTEGER CHECK (sla_hours BETWEEN 1 AND 8760);

CREATE TABLE product_section_requirements (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(), org_id UUID NOT NULL REFERENCES organisations(id) ON DELETE CASCADE,
  configuration_version_id UUID NOT NULL REFERENCES configuration_versions(id) ON DELETE RESTRICT,
  product_code TEXT NOT NULL REFERENCES loan_products(code) ON DELETE CASCADE,
  section_key TEXT NOT NULL CHECK (section_key IN ('personal_details','employment','business','financials','guarantors','collateral','documents','visits','credit_assessment')),
  requirement TEXT NOT NULL CHECK (requirement IN ('required','optional','hidden')),
  UNIQUE(configuration_version_id,product_code,section_key)
);

CREATE TABLE product_form_fields (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(), org_id UUID NOT NULL REFERENCES organisations(id) ON DELETE CASCADE,
  configuration_version_id UUID NOT NULL REFERENCES configuration_versions(id) ON DELETE RESTRICT,
  product_code TEXT NOT NULL REFERENCES loan_products(code) ON DELETE CASCADE,
  section_key TEXT NOT NULL, field_key TEXT NOT NULL, label TEXT NOT NULL,
  field_type TEXT NOT NULL CHECK (field_type IN ('text','number','currency','date','dropdown','checkbox','yes_no','photo','file','signature','gps')),
  requirement TEXT NOT NULL DEFAULT 'optional' CHECK (requirement IN ('required','optional','hidden')),
  options JSONB NOT NULL DEFAULT '[]'::jsonb, validation_rules JSONB NOT NULL DEFAULT '{}'::jsonb,
  visibility_condition JSONB NOT NULL DEFAULT '{}'::jsonb, help_text TEXT, display_order INTEGER NOT NULL DEFAULT 0,
  UNIQUE(configuration_version_id,product_code,field_key)
);

ALTER TABLE product_document_requirements ADD COLUMN IF NOT EXISTS org_id UUID REFERENCES organisations(id) ON DELETE CASCADE;
ALTER TABLE product_document_requirements ADD COLUMN IF NOT EXISTS configuration_version_id UUID REFERENCES configuration_versions(id) ON DELETE RESTRICT;
ALTER TABLE product_document_requirements ADD COLUMN IF NOT EXISTS display_name TEXT;

CREATE TABLE application_dynamic_values (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(), org_id UUID NOT NULL REFERENCES organisations(id) ON DELETE CASCADE,
  application_id UUID NOT NULL REFERENCES loan_applications(id) ON DELETE CASCADE,
  field_id UUID NOT NULL REFERENCES product_form_fields(id) ON DELETE RESTRICT,
  value_json JSONB NOT NULL, captured_by UUID NOT NULL REFERENCES users(id), captured_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  UNIQUE(application_id,field_id)
);

CREATE TABLE document_quality_assessments (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(), org_id UUID NOT NULL REFERENCES organisations(id) ON DELETE CASCADE,
  document_id UUID NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
  blur_score NUMERIC(5,4), lighting_score NUMERIC(5,4), crop_score NUMERIC(5,4), glare_score NUMERIC(5,4), readability_score NUMERIC(5,4),
  status TEXT NOT NULL CHECK(status IN ('passed','needs_review','rejected')), issues JSONB NOT NULL DEFAULT '[]'::jsonb,
  assessed_at TIMESTAMPTZ NOT NULL DEFAULT NOW(), UNIQUE(document_id)
);

ALTER TABLE product_section_requirements ENABLE ROW LEVEL SECURITY; ALTER TABLE product_section_requirements FORCE ROW LEVEL SECURITY;
ALTER TABLE product_form_fields ENABLE ROW LEVEL SECURITY; ALTER TABLE product_form_fields FORCE ROW LEVEL SECURITY;
ALTER TABLE application_dynamic_values ENABLE ROW LEVEL SECURITY; ALTER TABLE application_dynamic_values FORCE ROW LEVEL SECURITY;
ALTER TABLE document_quality_assessments ENABLE ROW LEVEL SECURITY; ALTER TABLE document_quality_assessments FORCE ROW LEVEL SECURITY;
CREATE POLICY product_sections_tenant ON product_section_requirements USING(org_id=NULLIF(current_setting('app.current_org_id',TRUE),'')::uuid) WITH CHECK(org_id=NULLIF(current_setting('app.current_org_id',TRUE),'')::uuid);
CREATE POLICY product_fields_tenant ON product_form_fields USING(org_id=NULLIF(current_setting('app.current_org_id',TRUE),'')::uuid) WITH CHECK(org_id=NULLIF(current_setting('app.current_org_id',TRUE),'')::uuid);
CREATE POLICY application_values_tenant ON application_dynamic_values USING(org_id=NULLIF(current_setting('app.current_org_id',TRUE),'')::uuid) WITH CHECK(org_id=NULLIF(current_setting('app.current_org_id',TRUE),'')::uuid);
CREATE POLICY document_quality_tenant ON document_quality_assessments USING(org_id=NULLIF(current_setting('app.current_org_id',TRUE),'')::uuid) WITH CHECK(org_id=NULLIF(current_setting('app.current_org_id',TRUE),'')::uuid);
