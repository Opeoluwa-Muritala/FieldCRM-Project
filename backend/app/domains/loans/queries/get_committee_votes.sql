-- Get all committee votes for a loan.
-- Params: $1=loan_id, $2=org_id

SELECT
    cv.id,
    cv.member_id,
    cv.recommendation,
    cv.notes,
    cv.voted_at,
    u.full_name AS member_name
FROM committee_votes cv
JOIN users u ON u.id = cv.member_id
WHERE cv.loan_id = $1
  AND cv.org_id  = $2
ORDER BY cv.voted_at ASC;
