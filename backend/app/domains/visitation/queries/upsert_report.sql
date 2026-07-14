-- visitation/queries/upsert_report.sql
-- Creates or updates the officer visitation report for a loan.
-- Params: $1=loan_id, $2=org_id, $3=met_with,
--         $4=premises_description, $5=direction_from_branch,
--         $6=visit_date, $7=visit_time, $8=relationship, $9=business_condition,
--         $10=visiting_officer, $11=account_officer, $12/$13=signatures,
--         $14=gps_coordinates, $15=site_photo_url

INSERT INTO visitation_reports (
    loan_id,
    org_id,
    met_with,
    premises_description,
    direction_from_branch,
    visit_date,
    visit_time,
    relationship,
    business_condition,
    visiting_officer_name,
    account_officer_name,
    visiting_officer_signature,
    visiting_officer_signature_data,
    account_officer_signature_data,
    gps_coordinates,
    site_photo_url,
    status
)
VALUES ($1, $2, $3, $4, $5, $6::date, $7::time, $8, $9, $10, $11,
        COALESCE(NULLIF($12, ''), '') <> '', $12, $13, $14, $15, 'submitted')
ON CONFLICT (loan_id)
DO UPDATE SET
    met_with = EXCLUDED.met_with,
    premises_description = EXCLUDED.premises_description,
    direction_from_branch = EXCLUDED.direction_from_branch,
    visit_date = EXCLUDED.visit_date,
    visit_time = EXCLUDED.visit_time,
    relationship = EXCLUDED.relationship,
    business_condition = EXCLUDED.business_condition,
    visiting_officer_name = EXCLUDED.visiting_officer_name,
    account_officer_name = EXCLUDED.account_officer_name,
    visiting_officer_signature = EXCLUDED.visiting_officer_signature,
    visiting_officer_signature_data = EXCLUDED.visiting_officer_signature_data,
    account_officer_signature_data = EXCLUDED.account_officer_signature_data,
    gps_coordinates = COALESCE(EXCLUDED.gps_coordinates, visitation_reports.gps_coordinates),
    site_photo_url = COALESCE(EXCLUDED.site_photo_url, visitation_reports.site_photo_url),
    status = 'submitted',
    updated_at = NOW()
RETURNING
    id, loan_id, org_id, visit_date, met_with, premises_description,
    direction_from_branch, visit_time, relationship, business_condition, visiting_officer_id,
    visiting_officer_name, visiting_officer_signature, visiting_officer_signature_data,
    account_officer_id, account_officer_name, account_officer_signature_data,
    gps_coordinates, site_photo_url, manager_concurrence,
    manager_id, manager_notes, manager_concurred_at, status, created_at,
    updated_at;
