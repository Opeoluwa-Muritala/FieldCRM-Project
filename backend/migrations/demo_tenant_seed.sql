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

-- Populate every visible intake section. Restricted identifiers are added
-- separately by seed_demo_tenant.py so they are encrypted with deployment keys.
UPDATE stage_data SET data_json = data_json || $demo_intake$
{
  "full_name":"Adaeze Nwankwo (Synthetic)",
  "id_type":"National ID","id_number":"DEMO-NID-0001","id_expiry":"2031-12-31",
  "phone":"08000000000","marital_status":"Married","dob":"1988-06-18",
  "state_of_origin":"Anambra","lga":"Onitsha North",
  "home_address":"17 Illustration Crescent, Lekki, Lagos","landmark":"Demo Community Hall",
  "photo_url":"/static/img/fieldcrm-logo.png",
  "spouse_name":"Chinedu Nwankwo (Synthetic)","spouse_phone":"08000000003",
  "spouse_children":"2","spouse_dependants":"1",
  "spouse_business_address":"9 Sample Business Park, Lagos",
  "guarantor_1_name":"Grace Mensah (Synthetic)","guarantor_1_relationship":"Business associate",
  "guarantor_1_phone":"08000000001","guarantor_1_status":"Submitted",
  "guarantor_2_name":"Musa Ibrahim (Synthetic)","guarantor_2_relationship":"Supplier",
  "guarantor_2_phone":"08000000002","guarantor_2_status":"Submitted",
  "employment_type":"Self-employed","industry":"Food distribution and retail",
  "years_employed":"6","business_type":"Wholesale and retail distribution",
  "years_in_business":"6","monthly_sales":"1850000","monthly_turnover":"1720000",
  "business_location_address":["14 Demo Market Road","5 Example Warehouse Lane"],
  "business_location_city":["Lagos Island","Ikeja"],"business_location_state":["Lagos","Lagos"],
  "business_location_function":["retail_outlet","warehouse"],
  "cashflow_direction":["inflow","outflow","outflow"],
  "cashflow_classification":["operating","operating","personal"],
  "cashflow_category":["sales_revenue","inventory_purchase","household_withdrawal"],
  "cashflow_amount":["1850000","820000","210000"],
  "cashflow_frequency":["monthly","monthly","monthly"],
  "cashflow_period_months":["1","1","1"],
  "cashflow_description":["Retail and wholesale sales","Inventory replenishment","Household commitments"],
  "cashflow_channel":["bank_transfer","bank_transfer","cash"],
  "household_expenses":"210000","verified_other_income":"90000","dependants":"3",
  "maintenance_capex":"75000","inventory_value":"9800000","receivables_value":"1450000","payables_value":"620000",
  "pnl_period_label":"Latest 12 months","pnl_revenue":"22200000","pnl_expenses":"11160000",
  "facility_bank":["Demo Cooperative Finance"],"facility_amount":["650000"],
  "facility_payment":["85000"],"facility_frequency":["monthly"],
  "facility_tenor":["8"],"facility_status":["current"],"education":"Graduate",
  "loan_purpose":"Working Capital","loan_purpose_other":"",
  "amount":"7500000","amount_words":"Seven Million Five Hundred Thousand Naira Only","tenor":"24",
  "collateral_type":["property","inventory"],
  "collateral_market_value":["12000000","5000000"],
  "collateral_narration":["Synthetic commercial property used for the demonstration","Verified synthetic durable inventory"],
  "collateral_fsv":["8400000","2500000"],"repayment_mode":"direct_debit",
  "account_name":"Apex Foods & Retail Limited (Synthetic)","bank_name":"Mainstreet MFB (Demo)","sort_code":"000000",
  "pledge_date":"2026-08-14","pledge_borrower":"Apex Foods & Retail Limited (Synthetic)",
  "pledge_amount_figs":"7500000","pledge_amount_words":"Seven Million Five Hundred Thousand Naira Only",
  "pledge_location":"5 Example Warehouse Lane, Ikeja, Lagos","pledge_obligor":"Adaeze Nwankwo (Synthetic)",
  "pledge_item_name":["Packaged food inventory","Cold-room equipment"],
  "pledge_item_qty":["850","2"],"pledge_item_desc":["Assorted non-perishable inventory","Synthetic industrial cold-room units"],
  "pledge_item_val":["5000000","3200000"],
  "witness_name":"Demo Operations Witness","witness_address":"Lagos Island Demo Branch",
  "demo_data":true
}
$demo_intake$::jsonb, saved_at=NOW()
WHERE id='de000000-0000-4000-8000-000000000401';

INSERT INTO business_locations (id,application_id,address_line,city,state,function,created_by) VALUES
('de000000-0000-4000-8000-000000000701','de000000-0000-4000-8000-000000000301','14 Demo Market Road','Lagos Island','Lagos','retail_outlet','de000000-0000-4000-8000-000000000201'),
('de000000-0000-4000-8000-000000000702','de000000-0000-4000-8000-000000000301','5 Example Warehouse Lane','Ikeja','Lagos','warehouse','de000000-0000-4000-8000-000000000201')
ON CONFLICT (id) DO NOTHING;

INSERT INTO business_pnl (application_id,revenue,expenses,period_label,created_by)
VALUES ('de000000-0000-4000-8000-000000000301',22200000,11160000,'Latest 12 months','de000000-0000-4000-8000-000000000201')
ON CONFLICT (application_id) DO UPDATE SET revenue=EXCLUDED.revenue,expenses=EXCLUDED.expenses,period_label=EXCLUDED.period_label,updated_at=NOW();

INSERT INTO borrower_financial_profiles (
 application_id,essential_household_expenses,verified_other_income,dependants,
 inventory_value,receivables_value,payables_value,maintenance_capex,
 source_type,verification_status,captured_by
) VALUES (
 'de000000-0000-4000-8000-000000000301',210000,90000,3,9800000,1450000,620000,75000,
 'demo_seed','declared','de000000-0000-4000-8000-000000000201'
) ON CONFLICT (application_id) DO UPDATE SET
 essential_household_expenses=EXCLUDED.essential_household_expenses,
 verified_other_income=EXCLUDED.verified_other_income,dependants=EXCLUDED.dependants,
 inventory_value=EXCLUDED.inventory_value,receivables_value=EXCLUDED.receivables_value,
 payables_value=EXCLUDED.payables_value,maintenance_capex=EXCLUDED.maintenance_capex,updated_at=NOW();

INSERT INTO cashflow_entries (
 id,application_id,flow_direction,classification,category,amount,frequency,period_months,
 description,channel,source_type,source_reference,is_recurring,verification_status,captured_by
) VALUES
('de000000-0000-4000-8000-000000000711','de000000-0000-4000-8000-000000000301','inflow','operating','sales_revenue',1850000,'monthly',1,'Retail and wholesale sales','bank_transfer','manual','demo:cashflow:1',TRUE,'declared','de000000-0000-4000-8000-000000000201'),
('de000000-0000-4000-8000-000000000712','de000000-0000-4000-8000-000000000301','outflow','operating','inventory_purchase',820000,'monthly',1,'Inventory replenishment','bank_transfer','manual','demo:cashflow:2',TRUE,'declared','de000000-0000-4000-8000-000000000201'),
('de000000-0000-4000-8000-000000000713','de000000-0000-4000-8000-000000000301','outflow','personal','household_withdrawal',210000,'monthly',1,'Household commitments','cash','manual','demo:cashflow:3',TRUE,'declared','de000000-0000-4000-8000-000000000201')
ON CONFLICT (id) DO NOTHING;

INSERT INTO credit_obligations (
 id,application_id,lender_name,source_type,outstanding_balance,periodic_payment,
 payment_frequency,remaining_tenor_months,status,verification_status,source_reference,captured_by
) VALUES (
 'de000000-0000-4000-8000-000000000721','de000000-0000-4000-8000-000000000301',
 'Demo Cooperative Finance','declared',650000,85000,'monthly',8,'current','declared','demo:facility:1',
 'de000000-0000-4000-8000-000000000201'
) ON CONFLICT (id) DO NOTHING;

INSERT INTO collateral_items (
 id,application_id,collateral_type,narration,loan_based_price,face_value,
 force_sale_value,retention_rate,valuation_date,valuation_source,manual_review_required,created_by
) VALUES
('de000000-0000-4000-8000-000000000731','de000000-0000-4000-8000-000000000301','property','Synthetic commercial property used for the demonstration',12000000,8400000,8400000,0.7000,CURRENT_DATE,'demo_seed',TRUE,'de000000-0000-4000-8000-000000000201'),
('de000000-0000-4000-8000-000000000732','de000000-0000-4000-8000-000000000301','inventory','Verified synthetic durable inventory',5000000,2500000,2500000,0.5000,CURRENT_DATE,'demo_seed',TRUE,'de000000-0000-4000-8000-000000000201')
ON CONFLICT (id) DO NOTHING;

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
