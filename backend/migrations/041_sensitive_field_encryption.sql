-- Storage support for application-layer encrypted restricted identifiers.
-- Existing plaintext rows must be migrated with an authenticated maintenance
-- job before RLS_ENFORCED is declared fully deployed.
ALTER TABLE public.loan_applications
  ADD COLUMN IF NOT EXISTS bvn_lookup_hash text;

ALTER TABLE public.guarantors
  ADD COLUMN IF NOT EXISTS bvn_lookup_hash text,
  ADD COLUMN IF NOT EXISTS account_lookup_hash text;

CREATE INDEX IF NOT EXISTS ix_loan_bvn_lookup_hash
  ON public.loan_applications(org_id, bvn_lookup_hash)
  WHERE bvn_lookup_hash IS NOT NULL AND deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS ix_guarantor_bvn_lookup_hash
  ON public.guarantors(org_id, bvn_lookup_hash)
  WHERE bvn_lookup_hash IS NOT NULL;
CREATE INDEX IF NOT EXISTS ix_guarantor_account_lookup_hash
  ON public.guarantors(org_id, account_lookup_hash)
  WHERE account_lookup_hash IS NOT NULL;

COMMENT ON COLUMN public.loan_applications.bvn IS
  'Application-layer AES-256-GCM ciphertext (enc:v1 prefix); legacy rows may require migration.';
COMMENT ON COLUMN public.loan_applications.bvn_lookup_hash IS
  'Versioned keyed HMAC blind index for exact matching; never an unkeyed identifier hash.';
