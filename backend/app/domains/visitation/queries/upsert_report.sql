-- visitation/queries/upsert_report.sql
-- Creates or updates the officer visitation report for a loan.
-- Params: $1=loan_id, $2=org_id, $3=met_with,
--         $4=premises_description, $5=direction_from_branch

INSERT INTO visitation_reports (
    loan_id,
    org_id,
    met_with,
    premises_description,
    direction_from_branch,
    status
)
VALUES ($1, $2, $3, $4, $5, 'submitted')
ON CONFLICT (loan_id)
DO UPDATE SET
    met_with = EXCLUDED.met_with,
    premises_description = EXCLUDED.premises_description,
    direction_from_branch = EXCLUDED.direction_from_branch,
    status = 'submitted',
    updated_at = NOW()
RETURNING
    id, loan_id, org_id, visit_date, met_with, premises_description,
    direction_from_branch, business_condition, visiting_officer_id,
    visiting_officer_signature, account_officer_id, manager_concurrence,
    manager_id, manager_notes, manager_concurred_at, status, created_at,
    updated_at;
