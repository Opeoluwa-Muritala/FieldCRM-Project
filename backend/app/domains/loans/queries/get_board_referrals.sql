-- Get all board referrals for a loan.
-- Params: $1=loan_id, $2=org_id

SELECT
    br.id,
    br.board_member_email,
    br.board_member_name,
    br.notes,
    br.status,
    br.created_at,
    u.full_name AS referred_by_name
FROM board_referrals br
JOIN users u ON u.id = br.referred_by
WHERE br.loan_id = $1
  AND br.org_id  = $2
ORDER BY br.created_at DESC;
