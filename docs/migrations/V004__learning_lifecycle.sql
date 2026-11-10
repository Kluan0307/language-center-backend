-- MySQL 8. Back up students/enrollments/exams/grades before applying after V003.
-- No historical reasons, users or missing enrollment periods are invented.
DROP PROCEDURE IF EXISTS lc_v004_column;
DELIMITER $$
CREATE PROCEDURE lc_v004_column(IN t VARCHAR(64),IN c VARCHAR(64),IN d VARCHAR(255))
BEGIN
 IF NOT EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name=t AND column_name=c) THEN
  SET @lc_v004_sql=CONCAT('ALTER TABLE `',t,'` ADD COLUMN `',c,'` ',d);
  PREPARE lc_v004_stmt FROM @lc_v004_sql;EXECUTE lc_v004_stmt;DEALLOCATE PREPARE lc_v004_stmt;
 END IF;
END$$
DELIMITER ;
CALL lc_v004_column('students','version','BIGINT NOT NULL DEFAULT 0');
CALL lc_v004_column('enrollments','version','BIGINT NOT NULL DEFAULT 0');
CALL lc_v004_column('enrollments','reserved_since','DATE NULL');
CALL lc_v004_column('enrollments','expected_return_date','DATE NULL');
CALL lc_v004_column('exams','roster_frozen_at','DATETIME(6) NULL');
CALL lc_v004_column('exams','roster_frozen_by','BIGINT NULL');
CALL lc_v004_column('exams','roster_frozen_by_name','VARCHAR(255) NULL');
DROP PROCEDURE lc_v004_column;
CREATE TABLE IF NOT EXISTS learning_events (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 branch_id BIGINT NOT NULL,
 student_id BIGINT NOT NULL,
 enrollment_id BIGINT NULL,
 related_enrollment_id BIGINT NULL,
 action VARCHAR(255) NOT NULL,
 old_status VARCHAR(255) NULL,
 new_status VARCHAR(255) NOT NULL,
 reason VARCHAR(500) NOT NULL,
 effective_date DATE NOT NULL,
 expected_return_date DATE NULL,
 actor_id BIGINT NOT NULL,
 actor_name VARCHAR(255) NOT NULL,
 occurred_at DATETIME(6) NOT NULL,
 INDEX idx_learning_student(student_id,id),
 INDEX idx_learning_enrollment(enrollment_id,id)
);
CREATE TABLE IF NOT EXISTS exam_candidates (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 exam_id BIGINT NOT NULL,
 student_id BIGINT NOT NULL,
 student_name VARCHAR(255) NOT NULL,
 source VARCHAR(255) NOT NULL,
 CONSTRAINT uk_exam_candidate UNIQUE(exam_id,student_id)
);
-- Existing grades remain untouched. Unfrozen legacy exams preview dated membership plus
-- already saved grade owners (SAVED_GRADE); their roster freezes on the next teacher save.
-- Old RESERVED rows can retain unknown reserved_since/expected_return_date as NULL.
SELECT 'legacy_reserved_missing_expected_date' AS review,COUNT(*) AS count FROM enrollments WHERE study_status='RESERVED' AND expected_return_date IS NULL;
