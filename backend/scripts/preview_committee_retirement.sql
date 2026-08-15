-- Read-only deployment preview for migration 023_retire_committee.sql.
-- MCC votes and finalized amounts are reported separately and are never modified.
SELECT COUNT(*) AS active_committee_users
FROM users WHERE role = 'committee' AND active = TRUE;

SELECT COUNT(*) AS legacy_committee_stage_dossiers
FROM loan_applications WHERE stage = 'committee_review' AND deleted_at IS NULL;

SELECT COUNT(*) AS preserved_mcc_votes FROM committee_votes;

SELECT COUNT(*) AS preserved_mcc_finalized_dossiers
FROM loan_applications WHERE mcc_finalized_at IS NOT NULL;
