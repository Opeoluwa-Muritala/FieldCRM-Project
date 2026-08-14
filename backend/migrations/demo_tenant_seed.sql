-- Synthetic presenter tenant for the Vercel demo branch.
-- Deliberately excluded from the automatic production migration list.
-- Additive and idempotent: it never rewinds the application or deletes history.

INSERT INTO organisations (id,name,code,active) VALUES
('de000000-0000-4000-8000-000000000001','FieldCRM Demo Organisation','FIELDCRM-DEMO',TRUE)
ON CONFLICT (id) DO UPDATE SET name=EXCLUDED.name,active=TRUE;

INSERT INTO branches (id,org_id,name,code,active) VALUES
('de000000-0000-4000-8000-000000000101','de000000-0000-4000-8000-000000000001','Lagos Island Demo Branch','DEMO-LAG',TRUE),
('de000000-0000-4000-8000-000000000102','de000000-0000-4000-8000-000000000001','Ikeja Demo Branch','DEMO-IKJ',TRUE),
('de000000-0000-4000-8000-000000000103','de000000-0000-4000-8000-000000000001','Surulere Demo Branch','DEMO-SUR',TRUE)
ON CONFLICT (id) DO UPDATE SET name=EXCLUDED.name,active=TRUE;

-- These hashes cannot authenticate. Only the secret-protected presenter can
-- issue short-lived sessions for these accounts.
INSERT INTO users (id,org_id,full_name,email,password_hash,role,active,branch_id,deleted_at) VALUES
('de000000-0000-4000-8000-000000000201','de000000-0000-4000-8000-000000000001','Amaka Okafor (Demo)','amaka.okafor@demo.fieldcrm.invalid','!presenter-only!','account_officer',TRUE,'de000000-0000-4000-8000-000000000101',NULL),
('de000000-0000-4000-8000-000000000202','de000000-0000-4000-8000-000000000001','Tunde Balogun (Demo)','tunde.balogun@demo.fieldcrm.invalid','!presenter-only!','branch_manager',TRUE,'de000000-0000-4000-8000-000000000101',NULL),
('de000000-0000-4000-8000-000000000203','de000000-0000-4000-8000-000000000001','Ngozi Eze (Demo)','ngozi.eze@demo.fieldcrm.invalid','!presenter-only!','branch_supervisor',TRUE,'de000000-0000-4000-8000-000000000101',NULL),
('de000000-0000-4000-8000-000000000204','de000000-0000-4000-8000-000000000001','Kunle Adeyemi (Demo)','kunle.adeyemi@demo.fieldcrm.invalid','!presenter-only!','credit_analyst',TRUE,NULL,NULL),
('de000000-0000-4000-8000-000000000205','de000000-0000-4000-8000-000000000001','Bisi Akinola (Demo)','bisi.akinola@demo.fieldcrm.invalid','!presenter-only!','crm',TRUE,NULL,NULL),
('de000000-0000-4000-8000-000000000206','de000000-0000-4000-8000-000000000001','Chidi Nwosu (Demo)','chidi.nwosu@demo.fieldcrm.invalid','!presenter-only!','head_crm',TRUE,NULL,NULL),
('de000000-0000-4000-8000-000000000207','de000000-0000-4000-8000-000000000001','Amina Yusuf (Demo)','amina.yusuf@demo.fieldcrm.invalid','!presenter-only!','ed',TRUE,NULL,NULL),
('de000000-0000-4000-8000-000000000208','de000000-0000-4000-8000-000000000001','Femi Cole (Demo)','femi.cole@demo.fieldcrm.invalid','!presenter-only!','md',TRUE,NULL,NULL),
('de000000-0000-4000-8000-000000000209','de000000-0000-4000-8000-000000000001','Ifeoma Obi (Demo)','ifeoma.obi@demo.fieldcrm.invalid','!presenter-only!','auditor',TRUE,NULL,NULL),
('de000000-0000-4000-8000-000000000210','de000000-0000-4000-8000-000000000001','David Lawal (Demo)','david.lawal@demo.fieldcrm.invalid','!presenter-only!','legal',TRUE,NULL,NULL),
('de000000-0000-4000-8000-000000000211','de000000-0000-4000-8000-000000000001','System Administrator (Demo)','admin@demo.fieldcrm.invalid','!presenter-only!','system_admin',TRUE,NULL,NULL),
('de000000-0000-4000-8000-000000000212','de000000-0000-4000-8000-000000000001','Sade Bello (Other Branch Demo)','sade.bello@demo.fieldcrm.invalid','!presenter-only!','account_officer',TRUE,'de000000-0000-4000-8000-000000000102',NULL),
('de000000-0000-4000-8000-000000000213','de000000-0000-4000-8000-000000000001','Uche Okoro (Other Branch Demo)','uche.okoro@demo.fieldcrm.invalid','!presenter-only!','branch_manager',TRUE,'de000000-0000-4000-8000-000000000102',NULL)
ON CONFLICT (id) DO UPDATE SET full_name=EXCLUDED.full_name,role=EXCLUDED.role,
active=TRUE,branch_id=EXCLUDED.branch_id,deleted_at=NULL;

INSERT INTO loan_applications (
 id,org_id,ref_no,customer_type,loan_type,stage,applicant_name,phone,amount,
 tenor_months,purpose,repayment_mode,created_by,current_owner_id,
 credit_officer_id,branch_manager_id,branch_id,assistance_required,created_at,updated_at
) VALUES (
 'de000000-0000-4000-8000-000000000301','de000000-0000-4000-8000-000000000001',
 'DEMO-2026-0001','new','corporate_sme','intake','Apex Foods & Retail Limited (Synthetic)',
 '08000000000',7500000,24,'Working capital and inventory expansion for a synthetic business',
 'direct_debit','de000000-0000-4000-8000-000000000201',
 'de000000-0000-4000-8000-000000000201','de000000-0000-4000-8000-000000000204',
 'de000000-0000-4000-8000-000000000202',
 'de000000-0000-4000-8000-000000000101',FALSE,NOW(),NOW()
) ON CONFLICT (id) DO NOTHING;

-- Credit Analyst writes are RLS-limited to their assigned dossier. Preserve
-- the current stage while repairing this stable assignment on repeat runs.
UPDATE loan_applications
SET credit_officer_id='de000000-0000-4000-8000-000000000204'
WHERE id='de000000-0000-4000-8000-000000000301'
  AND org_id='de000000-0000-4000-8000-000000000001';

INSERT INTO stage_data (id,loan_id,stage,data_json,saved_by,saved_at) VALUES (
 'de000000-0000-4000-8000-000000000401','de000000-0000-4000-8000-000000000301','intake',
 '{"business_name":"Apex Foods & Retail Limited (Synthetic)","business_type":"Limited liability company","business_address":"14 Demo Market Road, Lagos","years_in_business":"6","monthly_income":"1850000","monthly_expenses":"930000","loan_purpose":"Working capital and inventory expansion","demo_data":true}'::jsonb,
 'de000000-0000-4000-8000-000000000201',NOW()
) ON CONFLICT (id) DO NOTHING;

-- Guarantors provide details only; no guarantor or witness signatures exist.
INSERT INTO guarantors (
 id,loan_id,org_id,slot,full_name,relationship_to_client,phone,home_address,
 employment_type,monthly_salary,max_guarantee_amount,max_guarantee_amount_words,
 form_stage,signature_detected,witness_signature_detected
) VALUES
('de000000-0000-4000-8000-000000000501','de000000-0000-4000-8000-000000000301','de000000-0000-4000-8000-000000000001',1,'Grace Mensah (Synthetic)','Business associate','08000000001','22 Sample Avenue, Lagos','Self-employed',650000,3750000,'Three million seven hundred and fifty thousand naira','submitted',FALSE,FALSE),
('de000000-0000-4000-8000-000000000502','de000000-0000-4000-8000-000000000301','de000000-0000-4000-8000-000000000001',2,'Musa Ibrahim (Synthetic)','Supplier','08000000002','8 Example Close, Lagos','Employed',720000,3750000,'Three million seven hundred and fifty thousand naira','submitted',FALSE,FALSE)
ON CONFLICT (id) DO NOTHING;

INSERT INTO workflow_events (
 id,loan_id,org_id,event_type,from_stage,to_stage,triggered_by,triggered_role,notes,created_at
) VALUES (
 'de000000-0000-4000-8000-000000000601','de000000-0000-4000-8000-000000000301',
 'de000000-0000-4000-8000-000000000001','loan.created',NULL,'intake',
 'de000000-0000-4000-8000-000000000201','account_officer',
 'Synthetic demo application created by the Relationship Officer.',NOW()
) ON CONFLICT (id) DO NOTHING;
