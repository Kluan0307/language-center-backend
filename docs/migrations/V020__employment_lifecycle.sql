-- MySQL 8; stop Auth/Academic writers. Re-runnable, no demo data.
SET @exists=(SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='admins' AND COLUMN_NAME='version');
SET @ddl=IF(@exists=0,'ALTER TABLE admins ADD COLUMN version BIGINT NOT NULL DEFAULT 0','SELECT 1');
PREPARE employment_stmt FROM @ddl; EXECUTE employment_stmt; DEALLOCATE PREPARE employment_stmt;
SET @exists=(SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='staffs' AND COLUMN_NAME='version');
SET @ddl=IF(@exists=0,'ALTER TABLE staffs ADD COLUMN version BIGINT NOT NULL DEFAULT 0','SELECT 1');
PREPARE employment_stmt FROM @ddl; EXECUTE employment_stmt; DEALLOCATE PREPARE employment_stmt;
SET @exists=(SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='admins' AND COLUMN_NAME='end_date');
SET @ddl=IF(@exists=0,'ALTER TABLE admins ADD COLUMN end_date DATE NULL','SELECT 1');
PREPARE employment_stmt FROM @ddl; EXECUTE employment_stmt; DEALLOCATE PREPARE employment_stmt;
SET @exists=(SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='staffs' AND COLUMN_NAME='end_date');
SET @ddl=IF(@exists=0,'ALTER TABLE staffs ADD COLUMN end_date DATE NULL','SELECT 1');
PREPARE employment_stmt FROM @ddl; EXECUTE employment_stmt; DEALLOCATE PREPARE employment_stmt;
SET @exists=(SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='teachers' AND COLUMN_NAME='end_date');
SET @ddl=IF(@exists=0,'ALTER TABLE teachers ADD COLUMN end_date DATE NULL','SELECT 1');
PREPARE employment_stmt FROM @ddl; EXECUTE employment_stmt; DEALLOCATE PREPARE employment_stmt;
CREATE TABLE IF NOT EXISTS employment_events (id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,account_id BIGINT NOT NULL,actor_id BIGINT NOT NULL,before_state VARCHAR(1000) NOT NULL,after_state VARCHAR(1000) NOT NULL,reason VARCHAR(255) NOT NULL,occurred_at DATETIME(6) NOT NULL,INDEX idx_employment_account(account_id,id)) ENGINE=InnoDB;
UPDATE admins SET work_status='ACTIVE' WHERE work_status='WORKING';
-- Unknown historical end dates remain NULL; complete them through the employment form.
UPDATE staffs SET work_status='ACTIVE' WHERE work_status='WORKING';
UPDATE teachers SET work_status='ACTIVE' WHERE work_status='WORKING';
