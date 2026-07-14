-- visitation/queries/get_by_loan.sql
-- Fetches a visitation report scoped to the current organisation.
-- Params: $1=loan_id, $2=org_id

SELECT
    id,
    loan_id,
    org_id,
    visit_date,
    visit_time,
    met_with,
    relationship,
    premises_description,
    direction_from_branch,
    business_condition,
    visiting_officer_id,
    visiting_officer_signature,
    visiting_officer_name,
    visiting_officer_signature_data AS visiting_officer_sig,
    account_officer_id,
    account_officer_name AS account_officer,
    account_officer_signature_data AS account_officer_sig,
    gps_coordinates,
    site_photo_url,
    manager_concurrence,
    manager_id,
    manager_notes,
    manager_signature_data AS bm_sig,
    concurrence_return_reason,
    manager_concurred_at,
    status,
    created_at,
    updated_at
FROM visitation_reports
WHERE loan_id = $1
  AND org_id = $2;
