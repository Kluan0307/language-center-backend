-- MySQL 8, additive after V007. No conversion of legacy Course/Student text or duration.
CREATE TABLE IF NOT EXISTS programs (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,version BIGINT NOT NULL DEFAULT 0,
 code VARCHAR(50) NOT NULL,name VARCHAR(255) NOT NULL,active BIT NOT NULL,
 CONSTRAINT uk_program_code UNIQUE(code)
);
CREATE TABLE IF NOT EXISTS program_levels (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,version BIGINT NOT NULL DEFAULT 0,
 program_id BIGINT NOT NULL,code VARCHAR(50) NOT NULL,name VARCHAR(255) NOT NULL,
 sort_order INT NOT NULL,active BIT NOT NULL,
 CONSTRAINT uk_program_level_code UNIQUE(program_id,code),
 CONSTRAINT fk_level_program FOREIGN KEY(program_id) REFERENCES programs(id)
);
CREATE TABLE IF NOT EXISTS course_admission_policies (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,course_id BIGINT NOT NULL,revision BIGINT NOT NULL,
 mode VARCHAR(40) NOT NULL,program_id BIGINT NOT NULL,program_code VARCHAR(50) NOT NULL,program_name VARCHAR(255) NOT NULL,
 output_level_id BIGINT NOT NULL,output_level_code VARCHAR(50) NOT NULL,output_level_name VARCHAR(255) NOT NULL,
 confirmed_by BIGINT NOT NULL,confirmed_by_name VARCHAR(255) NOT NULL,confirmed_at DATETIME(6) NOT NULL,reason VARCHAR(500) NOT NULL,
 CONSTRAINT uk_course_admission_revision UNIQUE(course_id,revision),
 CONSTRAINT fk_admission_course FOREIGN KEY(course_id) REFERENCES courses(id)
);
CREATE TABLE IF NOT EXISTS course_admission_levels (
 policy_id BIGINT NOT NULL,level_id BIGINT NOT NULL,code VARCHAR(50) NOT NULL,name VARCHAR(255) NOT NULL,
 CONSTRAINT uk_admission_level UNIQUE(policy_id,level_id),
 CONSTRAINT fk_admission_level_policy FOREIGN KEY(policy_id) REFERENCES course_admission_policies(id)
);
SET @lc_admission_sql=(SELECT IF(COUNT(*)=0,'ALTER TABLE school_classes ADD COLUMN admission_policy_id BIGINT NULL','SELECT 1') FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='school_classes' AND column_name='admission_policy_id');
PREPARE lc_admission_stmt FROM @lc_admission_sql;EXECUTE lc_admission_stmt;DEALLOCATE PREPARE lc_admission_stmt;
SET @lc_admission_sql=(SELECT IF(COUNT(*)=0,'ALTER TABLE school_classes ADD COLUMN admission_revision BIGINT NOT NULL DEFAULT 0','SELECT 1') FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='school_classes' AND column_name='admission_revision');
PREPARE lc_admission_stmt FROM @lc_admission_sql;EXECUTE lc_admission_stmt;DEALLOCATE PREPARE lc_admission_stmt;
CREATE TABLE IF NOT EXISTS class_admission_events (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,class_id BIGINT NOT NULL,
 old_policy_id BIGINT NULL,new_policy_id BIGINT NULL,revision BIGINT NOT NULL,
 actor_id BIGINT NOT NULL,actor_name VARCHAR(255) NOT NULL,occurred_at DATETIME(6) NOT NULL,reason VARCHAR(500) NOT NULL,
 INDEX idx_class_admission_event(class_id,id)
);
