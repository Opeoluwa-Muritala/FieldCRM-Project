-- Insert or update a committee member's vote.
-- Params: $1=loan_id, $2=org_id, $3=member_id, $4=recommendation, $5=notes

INSERT INTO committee_votes (loan_id, org_id, member_id, recommendation, notes)
VALUES ($1, $2, $3, $4, $5)
ON CONFLICT (loan_id, member_id)
DO UPDATE SET
    recommendation = EXCLUDED.recommendation,
    notes          = EXCLUDED.notes,
    voted_at       = NOW()
RETURNING id, loan_id, member_id, recommendation, notes, voted_at;
