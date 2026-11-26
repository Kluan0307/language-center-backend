-- Stop Auth/Academic writers first. MySQL 8. Re-runnable; existing short codes stay unchanged.
-- No database switch: execute against the explicitly selected project database.
CREATE TABLE IF NOT EXISTS student_code_numbers (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS personnel_code_numbers (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY
) ENGINE=InnoDB;

SET @code_column_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS
 WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='accounts' AND COLUMN_NAME='personnel_code');
SET @code_ddl = IF(@code_column_exists=0,
 'ALTER TABLE accounts ADD COLUMN personnel_code VARCHAR(24) NULL', 'SELECT 1');
PREPARE code_stmt FROM @code_ddl; EXECUTE code_stmt; DEALLOCATE PREPARE code_stmt;

START TRANSACTION;
SET @hv_max = (SELECT COALESCE(MAX(CAST(SUBSTRING(student_code,3) AS UNSIGNED)),0)
 FROM students WHERE student_code REGEXP '^HV[0-9]+$');
SET @hv_max = GREATEST(@hv_max,(SELECT COALESCE(MAX(id),0) FROM student_code_numbers));
CREATE TEMPORARY TABLE student_code_backfill AS
 SELECT id, CONCAT('HV',IF(n<10000,LPAD(n,4,'0'),CAST(n AS CHAR))) AS new_code
 FROM (SELECT id,CAST(@hv_max+ROW_NUMBER() OVER(ORDER BY id) AS UNSIGNED) AS n FROM students
 WHERE student_code IS NULL OR student_code='' OR student_code LIKE 'HV-%') numbered;
UPDATE students s JOIN student_code_backfill b ON b.id=s.id SET s.student_code=b.new_code;
DROP TEMPORARY TABLE student_code_backfill;
INSERT IGNORE INTO student_code_numbers(id)
 SELECT MAX(CAST(SUBSTRING(student_code,3) AS UNSIGNED)) FROM students
 WHERE student_code REGEXP '^HV[0-9]+$'
 HAVING MAX(CAST(SUBSTRING(student_code,3) AS UNSIGNED))>0;

SET @person_max = (SELECT COALESCE(MAX(CAST(SUBSTRING(personnel_code,3) AS UNSIGNED)),0)
 FROM accounts WHERE personnel_code REGEXP '^(NS|GV)[0-9]+$');
SET @person_max = GREATEST(@person_max,(SELECT COALESCE(MAX(id),0) FROM personnel_code_numbers));
CREATE TEMPORARY TABLE personnel_code_backfill AS
 SELECT id,CONCAT(IF(role='TEACHER','GV','NS'),IF(n<1000,LPAD(n,3,'0'),CAST(n AS CHAR))) AS new_code
 FROM (SELECT id,role,CAST(@person_max+ROW_NUMBER() OVER(ORDER BY id) AS UNSIGNED) AS n FROM accounts
 WHERE role IN ('ADMIN','STAFF','TEACHER') AND (personnel_code IS NULL OR personnel_code='')) numbered;
UPDATE accounts a JOIN personnel_code_backfill b ON b.id=a.id SET a.personnel_code=b.new_code;
DROP TEMPORARY TABLE personnel_code_backfill;
INSERT IGNORE INTO personnel_code_numbers(id)
 SELECT MAX(CAST(SUBSTRING(personnel_code,3) AS UNSIGNED)) FROM accounts
 WHERE personnel_code REGEXP '^(NS|GV)[0-9]+$'
 HAVING MAX(CAST(SUBSTRING(personnel_code,3) AS UNSIGNED))>0;
COMMIT;

SET @code_index_exists = (SELECT COUNT(*) FROM information_schema.STATISTICS
 WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='accounts' AND COLUMN_NAME='personnel_code' AND NON_UNIQUE=0);
SET @code_ddl = IF(@code_index_exists=0,
 'CREATE UNIQUE INDEX uk_account_personnel_code ON accounts(personnel_code)', 'SELECT 1');
PREPARE code_stmt FROM @code_ddl; EXECUTE code_stmt; DEALLOCATE PREPARE code_stmt;
