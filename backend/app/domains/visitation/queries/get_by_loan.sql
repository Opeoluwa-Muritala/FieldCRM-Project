-- visitation/queries/get_by_loan.sql
-- Fetches a visitation report scoped to the current organisation.
-- Params: $1=loan_id, $2=org_id

SELECT
    id,
    loan_id,
    org_id,
    visit_date,
    met_with,
    premises_description,
    direction_from_branch,
    business_condition,
    visiting_officer_id,
    visiting_officer_signature,
    account_officer_id,
    manager_concurrence,
    manager_id,
    manager_notes,
    manager_concurred_at,
    status,
    created_at,
    updated_at
FROM visitation_reports
WHERE loan_id = $1
  AND org_id = $2;
