-- Backfill Branch Manager ownership for intake already submitted before the
-- assignment-on-submit fix.
UPDATE loan_applications la
SET branch_manager_id = (
    SELECT u.id FROM users u
    WHERE u.org_id = la.org_id AND u.role = 'branch_manager' AND u.active = TRUE
    ORDER BY u.created_at ASC
    LIMIT 1
),
    updated_at = NOW()
WHERE la.stage = 'branch_manager_review'
  AND la.branch_manager_id IS NULL
  AND la.deleted_at IS NULL;
