-- Committee is not an operational FieldCRM role. Preserve historical rows while
-- preventing retired accounts and stages from remaining actionable.
-- This intentionally does not alter committee_votes or mcc_* columns: those
-- records belong to the ED/MD MCC feature, which remains operational.
UPDATE users
SET active = FALSE,
    updated_at = NOW()
WHERE role = 'committee'
  AND active = TRUE;

-- A legacy committee-stage dossier has already left CRM but has not received the
-- current Head CRM handoff. Return it to that controlled gate instead of skipping
-- directly to executive approval.
UPDATE loan_applications
SET stage = 'head_crm_review',
    updated_at = NOW()
WHERE stage = 'committee_review'
  AND deleted_at IS NULL;
