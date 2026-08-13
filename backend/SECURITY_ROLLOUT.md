# FieldCRM security rollout

This rollout excludes MFA by product decision. Do not apply the RLS migration
through the application's runtime account; use a short-lived schema-owner
maintenance connection.

## Before deployment

1. Back up PostgreSQL and verify a restore in a separate database.
2. Generate independent 32-byte `FIELD_ENCRYPTION_KEY` and `FIELD_LOOKUP_KEY`
   values. Store them in the platform secret manager. Losing the encryption key
   makes protected values unrecoverable; exposing the lookup key permits offline
   guesses against blind indexes.
3. Create distinct login roles: a migration owner, `fieldcrm_app`, and
   `fieldcrm_worker`. Runtime roles must be `NOINHERIT NOBYPASSRLS` and must not
   own protected tables.
4. Apply migrations `040_rls_security_foundation.sql` and
   `041_sensitive_field_encryption.sql` as the migration owner.

## Encrypt legacy records

Run from `backend` with the maintenance database URL and both keys configured:

```powershell
python migrations/encrypt_existing_sensitive_fields.py
python migrations/encrypt_existing_sensitive_fields.py --apply
python migrations/encrypt_existing_sensitive_fields.py
```

The first and third commands are dry runs. The final dry run must report zero
rows/fields. The command is idempotent and never logs protected values. Keep the
backup until row counts and representative application workflows are verified.

## RLS validation and activation

1. Connect as `fieldcrm_app`; verify `rolsuper = false`, `rolbypassrls = false`,
   and that it does not own protected tables.
2. Test an officer, Team Lead, supervisor, CRM user, and auditor in two separate
   organisations. Confirm cross-organisation reads return zero rows, not errors.
3. Confirm Relationship Officers can mutate only their own `intake` records and
   assigned Team Leads can mutate only `branch_manager_review` records.
4. Confirm document access, audit insertion, applicant signing, and background
   jobs in staging before production traffic is switched.
5. Set `DATABASE_URL` to the `fieldcrm_app` pooled connection and set
   `RLS_ENFORCED=true`. Restart all application instances.

Rollback migration `040_rls_security_foundation.rollback.sql` is an emergency
availability measure, not a normal deployment step. If used, restrict traffic
until the authorization fault is corrected.

## Operational controls

- Keep database, Cloudinary, Redis, email, and signing credentials in a managed
  secret store with least-privilege service identities.
- Rotate session/JWT and infrastructure credentials after suspected exposure.
  Encrypted-field key rotation requires decrypt-and-re-encrypt migration support;
  do not replace `FIELD_ENCRYPTION_KEY` in place.
- Retain append-only audit logs outside the application account's update/delete
  privileges and export them to monitored, tamper-resistant storage.
- Alert on repeated access denials, cross-tenant probes, bulk downloads, unusual
  document previews, privileged-role changes, and encryption/decryption failures.
- Keep private documents outside public static paths and enforce retention and
  secure deletion schedules approved by legal/compliance.
