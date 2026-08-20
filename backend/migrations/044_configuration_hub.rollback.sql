DROP TRIGGER IF EXISTS configuration_versions_published_immutable ON configuration_versions;
DROP FUNCTION IF EXISTS protect_published_configuration();
DROP TRIGGER IF EXISTS configuration_change_log_immutable ON configuration_change_log;
DROP FUNCTION IF EXISTS reject_configuration_history_mutation();
ALTER TABLE loan_applications DROP COLUMN IF EXISTS originated_config_version_id;
DROP TABLE IF EXISTS configuration_change_log;
DROP TABLE IF EXISTS configuration_versions;
ALTER TABLE users DROP COLUMN IF EXISTS config_mfa_enabled;
ALTER TABLE users DROP COLUMN IF EXISTS config_mfa_secret_encrypted;
ALTER TABLE users DROP CONSTRAINT IF EXISTS users_role_check;
ALTER TABLE users ADD CONSTRAINT users_role_check CHECK (role IN (
  'account_officer','branch_manager','branch_supervisor','credit_analyst',
  'crm','head_crm','auditor','ed','md','system_admin','legal'
));
