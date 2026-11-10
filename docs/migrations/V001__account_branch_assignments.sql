-- MySQL 8. Run once before starting the updated services; safe to re-run.
-- accounts.branch_id is retained as the primary branch for compatibility.
CREATE TABLE IF NOT EXISTS account_branches (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  account_id BIGINT NOT NULL,
  branch_id BIGINT NOT NULL,
  status BIT NOT NULL,
  is_primary BIT NOT NULL,
  assigned_at DATETIME(6) NOT NULL,
  assigned_by BIGINT NULL,
  revoked_at DATETIME(6) NULL,
  CONSTRAINT uk_account_branch UNIQUE (account_id, branch_id),
  CONSTRAINT fk_membership_account FOREIGN KEY (account_id) REFERENCES accounts(id),
  INDEX idx_membership_branch_status (branch_id, status)
);
CREATE TABLE IF NOT EXISTS account_branch_events (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  account_id BIGINT NOT NULL,
  branch_id BIGINT NOT NULL,
  action VARCHAR(30) NOT NULL,
  occurred_at DATETIME(6) NOT NULL,
  performed_by BIGINT NULL,
  CONSTRAINT fk_branch_event_account FOREIGN KEY (account_id) REFERENCES accounts(id),
  INDEX idx_branch_event_account_time (account_id, occurred_at)
);
START TRANSACTION;
INSERT INTO account_branches (account_id,branch_id,status,is_primary,assigned_at)
SELECT a.id,a.branch_id,1,1,COALESCE(a.created_at,NOW(6))
FROM accounts a JOIN branches b ON b.id=a.branch_id
WHERE a.role<>'SUPER_ADMIN' AND a.branch_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM account_branches ab WHERE ab.account_id=a.id);
INSERT INTO account_branch_events (account_id,branch_id,action,occurred_at)
SELECT ab.account_id,ab.branch_id,'MIGRATED',NOW(6)
FROM account_branches ab
WHERE ab.status=1 AND NOT EXISTS (
  SELECT 1 FROM account_branch_events e WHERE e.account_id=ab.account_id
);
COMMIT;
