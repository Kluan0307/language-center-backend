-- MySQL8 after V010. Preserve legacy duration values; never infer their unit.
SET @lc_duration_sql=(SELECT IF(COUNT(*)=0,'ALTER TABLE courses ADD COLUMN expected_minutes INT NULL','SELECT 1') FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='courses' AND column_name='expected_minutes');
PREPARE lc_duration_stmt FROM @lc_duration_sql;EXECUTE lc_duration_stmt;DEALLOCATE PREPARE lc_duration_stmt;
SET @lc_duration_sql=(SELECT IF(COUNT(*)=0,'ALTER TABLE courses ADD COLUMN duration_revision BIGINT NOT NULL DEFAULT 0','SELECT 1') FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='courses' AND column_name='duration_revision');
PREPARE lc_duration_stmt FROM @lc_duration_sql;EXECUTE lc_duration_stmt;DEALLOCATE PREPARE lc_duration_stmt;
SET @lc_duration_sql=(SELECT IF(COUNT(*)=0,'ALTER TABLE school_classes ADD COLUMN expected_minutes INT NULL','SELECT 1') FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='school_classes' AND column_name='expected_minutes');
PREPARE lc_duration_stmt FROM @lc_duration_sql;EXECUTE lc_duration_stmt;DEALLOCATE PREPARE lc_duration_stmt;
SET @lc_duration_sql=(SELECT IF(COUNT(*)=0,'ALTER TABLE school_classes ADD COLUMN duration_revision BIGINT NOT NULL DEFAULT 0','SELECT 1') FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='school_classes' AND column_name='duration_revision');
PREPARE lc_duration_stmt FROM @lc_duration_sql;EXECUTE lc_duration_stmt;DEALLOCATE PREPARE lc_duration_stmt;
-- Permit new records without inventing a unitless legacy value. No existing value is changed.
ALTER TABLE courses MODIFY COLUMN duration INT NULL;
CREATE TABLE IF NOT EXISTS course_duration_plans (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,course_id BIGINT NOT NULL,revision BIGINT NOT NULL,expected_minutes INT NOT NULL,
 actor_id BIGINT NOT NULL,actor_name VARCHAR(255) NOT NULL,occurred_at DATETIME(6) NOT NULL,reason VARCHAR(500) NOT NULL,
 CONSTRAINT uk_course_duration_revision UNIQUE(course_id,revision),CONSTRAINT fk_duration_course FOREIGN KEY(course_id) REFERENCES courses(id)
);
CREATE TABLE IF NOT EXISTS class_duration_plans (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,class_id BIGINT NOT NULL,revision BIGINT NOT NULL,expected_minutes INT NULL,
 course_id BIGINT NOT NULL,course_revision BIGINT NULL,source VARCHAR(20) NOT NULL,
 actor_id BIGINT NOT NULL,actor_name VARCHAR(255) NOT NULL,occurred_at DATETIME(6) NOT NULL,reason VARCHAR(500) NOT NULL,
 CONSTRAINT uk_class_duration_revision UNIQUE(class_id,revision),CONSTRAINT fk_duration_class FOREIGN KEY(class_id) REFERENCES school_classes(id)
);
