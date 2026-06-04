-- visitation/queries/manager_signoff.sql
-- Records branch manager concurrence on a visitation report.
-- Only updates if the report is in 'submitted' status — prevents double signoff.
-- Params: $1=loan_id, $2=org_id, $3=manager_id, $4=notes, $5=decision (concurred|returned)

UPDATE visitation_reports
SET
    manager_concurrence     = ($5 = 'concurred'),
    manager_id              = $3,
    manager_notes           = $4,
    manager_concurred_at    = NOW(),
    status                  = $5,
    updated_at              = NOW()
WHERE loan_id   = $1
  AND org_id    = $2
  AND status    = 'submitted'
RETURNING
    id, loan_id, org_id, visit_date, met_with, premises_description,
    direction_from_branch, business_condition, visiting_officer_id,
    visiting_officer_signature, account_officer_id, manager_concurrence,
    manager_id, manager_notes, manager_concurred_at, status, created_at,
    updated_at;
