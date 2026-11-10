-- MySQL 8; back up accounts/profiles before applying. Safe to re-run.
-- Existing passwords, memberships and accounts are preserved.
SET @lc_stage2_sql = (SELECT IF(COUNT(*)=0,
  'ALTER TABLE accounts ADD COLUMN session_version BIGINT NOT NULL DEFAULT 0',
  'SELECT 1') FROM information_schema.columns
  WHERE table_schema=DATABASE() AND table_name='accounts' AND column_name='session_version');
PREPARE lc_stage2_stmt FROM @lc_stage2_sql;
EXECUTE lc_stage2_stmt;
DEALLOCATE PREPARE lc_stage2_stmt;
SET @lc_stage2_sql = (SELECT IF(COUNT(*)=0,
  'ALTER TABLE accounts ADD COLUMN must_change_password BIT NOT NULL DEFAULT 0',
  'SELECT 1') FROM information_schema.columns
  WHERE table_schema=DATABASE() AND table_name='accounts' AND column_name='must_change_password');
PREPARE lc_stage2_stmt FROM @lc_stage2_sql;
EXECUTE lc_stage2_stmt;
DEALLOCATE PREPARE lc_stage2_stmt;
SET @lc_stage2_sql = (SELECT IF(COUNT(*)=0,
  'ALTER TABLE accounts ADD COLUMN password_changed_at DATETIME(6) NULL',
  'SELECT 1') FROM information_schema.columns
  WHERE table_schema=DATABASE() AND table_name='accounts' AND column_name='password_changed_at');
PREPARE lc_stage2_stmt FROM @lc_stage2_sql;
EXECUTE lc_stage2_stmt;
DEALLOCATE PREPARE lc_stage2_stmt;
CREATE TABLE IF NOT EXISTS account_security_events (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  account_id BIGINT NOT NULL,
  actor_id BIGINT NOT NULL,
  action VARCHAR(40) NOT NULL,
  occurred_at DATETIME(6) NOT NULL,
  reason VARCHAR(255) NULL,
  CONSTRAINT fk_security_event_account FOREIGN KEY (account_id) REFERENCES accounts(id),
  INDEX idx_security_event_account_time (account_id,occurred_at)
);
