-- MySQL 8; additive after V006. Preserve work status, accounts and historical teaching snapshots.
SET @lc_teacher_version_sql=(SELECT IF(COUNT(*)=0,'ALTER TABLE teachers ADD COLUMN version BIGINT NOT NULL DEFAULT 0','SELECT 1') FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='teachers' AND column_name='version');
PREPARE lc_teacher_version_stmt FROM @lc_teacher_version_sql;EXECUTE lc_teacher_version_stmt;DEALLOCATE PREPARE lc_teacher_version_stmt;
CREATE TABLE IF NOT EXISTS teacher_profile_events (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 teacher_id BIGINT NOT NULL,action VARCHAR(255) NOT NULL,
 before_state LONGTEXT NOT NULL,after_state LONGTEXT NOT NULL,reason VARCHAR(500) NULL,
 actor_id BIGINT NOT NULL,actor_name VARCHAR(255) NOT NULL,teacher_version BIGINT NOT NULL,
 occurred_at DATETIME(6) NOT NULL,
 INDEX idx_teacher_event(teacher_id,id)
);
