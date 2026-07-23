-- loans/queries/get_by_id.sql
-- Fetches a single loan with officer names joined.
-- Enforces org_id scoping on every read.
-- Params: $1=loan_id, $2=org_id

SELECT
    la.id,
    la.org_id,
    la.ref_no,
    la.customer_type,
    la.loan_type,
    la.stage,
    la.applicant_name,
    la.bvn,
    la.phone,
    la.amount,
    la.tenor_months,
    la.purpose,
    la.repayment_mode,
    la.assistance_required,
    la.created_by,
    la.current_owner_id,
    la.credit_officer_id,
    la.branch_manager_id,
    la.return_reason,
    la.returned_at,
    la.approved_by,
    la.approved_at,
    la.disbursed_at,
    la.ed_escalated_to_md,
    la.ed_approved_by,
    la.md_approved_by,
    la.md_approved_at,
    la.md_notes,
    la.created_at,
    la.updated_at,
    officer.full_name           AS created_by_name,
    officer.role                AS created_by_role,
    owner.full_name             AS current_owner_name,
    credit.full_name            AS credit_officer_name,
    manager.full_name           AS branch_manager_name
FROM loan_applications la
LEFT JOIN users officer  ON officer.id  = la.created_by
LEFT JOIN users owner    ON owner.id    = la.current_owner_id
LEFT JOIN users credit   ON credit.id   = la.credit_officer_id
LEFT JOIN users manager  ON manager.id  = la.branch_manager_id
WHERE la.id        = $1
  AND la.org_id    = $2
  AND la.deleted_at IS NULL;
