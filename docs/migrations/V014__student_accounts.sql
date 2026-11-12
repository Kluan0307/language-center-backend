-- V014: STUDENT security association. Apply after V013; no data reset/seeding.
ALTER TABLE accounts MODIFY role ENUM('ADMIN','STAFF','SUPER_ADMIN','TEACHER','STUDENT') NOT NULL;
SET @lc_sql=(SELECT IF(COUNT(*)=0,'ALTER TABLE students ADD COLUMN account_id BIGINT NULL','SELECT 1') FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='students' AND column_name='account_id');
PREPARE lc_stmt FROM @lc_sql; EXECUTE lc_stmt; DEALLOCATE PREPARE lc_stmt;
SET @lc_sql=(SELECT IF(COUNT(*)=0,'ALTER TABLE students ADD UNIQUE KEY uk_student_account(account_id)','SELECT 1') FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='students' AND index_name='uk_student_account');
PREPARE lc_stmt FROM @lc_sql; EXECUTE lc_stmt; DEALLOCATE PREPARE lc_stmt;
SET @lc_sql=(SELECT IF(COUNT(*)=0,'ALTER TABLE students ADD CONSTRAINT fk_student_account FOREIGN KEY(account_id) REFERENCES accounts(id)','SELECT 1') FROM information_schema.table_constraints WHERE constraint_schema=DATABASE() AND table_name='students' AND constraint_name='fk_student_account');
PREPARE lc_stmt FROM @lc_sql; EXECUTE lc_stmt; DEALLOCATE PREPARE lc_stmt;
CREATE TABLE IF NOT EXISTS student_account_provisionings(
 request_id VARCHAR(36) PRIMARY KEY, student_id BIGINT NOT NULL, account_id BIGINT NOT NULL,
 username VARCHAR(50) NOT NULL, expected_student_version BIGINT NOT NULL,
 initial_password_hash VARCHAR(255) NOT NULL, created_by BIGINT NOT NULL, created_at DATETIME(6) NOT NULL,
 version BIGINT NOT NULL DEFAULT 0, operation_count BIGINT NOT NULL DEFAULT 0,
 UNIQUE KEY uk_student_provision_student(student_id), UNIQUE KEY uk_student_provision_account(account_id),
 CONSTRAINT fk_provision_student FOREIGN KEY(student_id) REFERENCES students(id),
 CONSTRAINT fk_provision_account FOREIGN KEY(account_id) REFERENCES accounts(id)
);
-- Also cover a table previously created by local Hibernate update (without FK mappings).
SET @lc_sql=(SELECT IF(COUNT(*)=0,'ALTER TABLE student_account_provisionings ADD CONSTRAINT fk_provision_student FOREIGN KEY(student_id) REFERENCES students(id)','SELECT 1') FROM information_schema.table_constraints WHERE constraint_schema=DATABASE() AND table_name='student_account_provisionings' AND constraint_name='fk_provision_student');
PREPARE lc_stmt FROM @lc_sql; EXECUTE lc_stmt; DEALLOCATE PREPARE lc_stmt;
SET @lc_sql=(SELECT IF(COUNT(*)=0,'ALTER TABLE student_account_provisionings ADD CONSTRAINT fk_provision_account FOREIGN KEY(account_id) REFERENCES accounts(id)','SELECT 1') FROM information_schema.table_constraints WHERE constraint_schema=DATABASE() AND table_name='student_account_provisionings' AND constraint_name='fk_provision_account');
PREPARE lc_stmt FROM @lc_sql; EXECUTE lc_stmt; DEALLOCATE PREPARE lc_stmt;
