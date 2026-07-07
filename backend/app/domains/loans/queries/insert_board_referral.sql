-- Insert a board referral for a loan.
-- Params: $1=loan_id, $2=org_id, $3=referred_by, $4=board_member_email, $5=board_member_name, $6=notes

INSERT INTO board_referrals (loan_id, org_id, referred_by, board_member_email, board_member_name, notes)
VALUES ($1, $2, $3, $4, $5, $6)
RETURNING id, loan_id, board_member_email, board_member_name, notes, status, created_at;
