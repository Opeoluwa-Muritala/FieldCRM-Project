-- visitation/queries/list_visits_due_today.sql
-- Returns loans that need visitation reports for a given officer.
-- A visit is "due" if the loan has no visitation report yet and is in
-- an active stage (intake through branch_approval).
-- Params: $1=org_id, $2=officer_user_id

SELECT
    la.id AS loan_id,
    la.ref_no,
    la.applicant_name,
    la.amount,
    la.stage,
    la.created_at AS application_date
FROM loan_applications la
LEFT JOIN visitation_reports vr ON vr.loan_id = la.id
WHERE la.org_id     = $1
  AND la.created_by = $2
  AND la.deleted_at IS NULL
  AND la.stage IN ('intake', 'ocr_review', 'credit_review', 'branch_approval')
  AND vr.id IS NULL
ORDER BY la.created_at ASC
LIMIT 20;
