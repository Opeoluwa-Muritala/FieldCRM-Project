-- Assign the active Team Lead for the application's branch when intake is completed.
-- Params: $1=loan_id, $2=org_id
UPDATE loan_applications
SET branch_manager_id = (
    SELECT id FROM users
    WHERE org_id = $2 AND role = 'branch_manager' AND active = TRUE
      AND branch_id = (SELECT branch_id FROM loan_applications WHERE id = $1 AND org_id = $2)
    ORDER BY created_at ASC
    LIMIT 1
),
    updated_at = NOW()
WHERE id = $1 AND org_id = $2 AND deleted_at IS NULL;
