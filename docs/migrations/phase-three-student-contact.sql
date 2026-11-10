-- MySQL 8, run with the intended database selected. No student rows are deleted.
-- Existing Hibernate schemas retain this obsolete unique index after entity changes.
SET @lc_phone_index = (
  SELECT index_name FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'students'
    AND non_unique = 0 AND index_name = 'uk_student_phone_branch'
  GROUP BY index_name
  HAVING GROUP_CONCAT(column_name ORDER BY seq_in_index) = 'phone,branch_id'
  LIMIT 1
);
SET @lc_contact_sql = IF(@lc_phone_index IS NULL, 'SELECT 1',
  'ALTER TABLE students DROP INDEX uk_student_phone_branch');
PREPARE lc_contact_stmt FROM @lc_contact_sql;
EXECUTE lc_contact_stmt;
DEALLOCATE PREPARE lc_contact_stmt;
-- student_code already has a unique index from the entity mapping.
UPDATE students SET student_code = CONCAT('HV-', id)
WHERE student_code IS NULL OR TRIM(student_code) = '';
