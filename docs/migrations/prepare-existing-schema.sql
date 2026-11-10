-- MySQL 8 only. Stop all old services and back up the database before applying.
-- This script preserves tables/rows and does not run automatically at app startup.
-- New finance keeps the existing table name `tuition`.
DELIMITER $$
CREATE PROCEDURE lc_add_column(IN tab VARCHAR(64),IN col VARCHAR(64),IN definition TEXT)
BEGIN
  IF EXISTS(SELECT 1 FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name=tab)
     AND NOT EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name=tab AND column_name=col) THEN
    SET @lc_sql=CONCAT('ALTER TABLE `',tab,'` ADD COLUMN `',col,'` ',definition);
    PREPARE lc_stmt FROM @lc_sql; EXECUTE lc_stmt; DEALLOCATE PREPARE lc_stmt;
  END IF;
END$$
CREATE PROCEDURE lc_relax_column(IN tab VARCHAR(64),IN col VARCHAR(64))
BEGIN
  DECLARE typ TEXT;
  SELECT MAX(column_type) INTO typ FROM information_schema.columns
    WHERE table_schema=DATABASE() AND table_name=tab AND column_name=col;
  IF typ IS NOT NULL THEN
    SET @lc_sql=CONCAT('ALTER TABLE `',tab,'` MODIFY COLUMN `',col,'` ',typ,' NULL');
    PREPARE lc_stmt FROM @lc_sql; EXECUTE lc_stmt; DEALLOCATE PREPARE lc_stmt;
  END IF;
END$$
CREATE PROCEDURE lc_run(IN tab VARCHAR(64),IN statement_text TEXT)
BEGIN
  IF EXISTS(SELECT 1 FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name=tab) THEN
    SET @lc_sql=statement_text; PREPARE lc_stmt FROM @lc_sql; EXECUTE lc_stmt; DEALLOCATE PREPARE lc_stmt;
  END IF;
END$$
CREATE PROCEDURE lc_copy_column(IN tab VARCHAR(64),IN old_col VARCHAR(64),IN new_col VARCHAR(64))
BEGIN
  IF EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name=tab AND column_name=old_col) THEN
    SET @lc_sql=CONCAT('UPDATE `',tab,'` SET `',new_col,'`=COALESCE(`',new_col,'`,`',old_col,'`)');
    PREPARE lc_stmt FROM @lc_sql; EXECUTE lc_stmt; DEALLOCATE PREPARE lc_stmt;
  END IF;
END$$
CREATE PROCEDURE lc_drop_old_indexes()
BEGIN
  DECLARE done INT DEFAULT 0; DECLARE tab VARCHAR(64); DECLARE idx VARCHAR(64);
  DECLARE indexes_cursor CURSOR FOR
    SELECT table_name,index_name FROM information_schema.statistics
    WHERE table_schema=DATABASE() AND non_unique=0 AND index_name<>'PRIMARY'
      AND table_name IN ('students','courses','rooms')
    GROUP BY table_name,index_name
    HAVING (table_name='students' AND GROUP_CONCAT(column_name ORDER BY seq_in_index) IN ('phone,branch_id','branch_id,phone'))
        OR (table_name IN ('courses','rooms') AND GROUP_CONCAT(column_name ORDER BY seq_in_index)='code');
  DECLARE CONTINUE HANDLER FOR NOT FOUND SET done=1;
  OPEN indexes_cursor;
  idx_loop: LOOP
    FETCH indexes_cursor INTO tab,idx; IF done THEN LEAVE idx_loop; END IF;
    SET @lc_sql=CONCAT('ALTER TABLE `',tab,'` DROP INDEX `',idx,'`');
    PREPARE lc_stmt FROM @lc_sql; EXECUTE lc_stmt; DEALLOCATE PREPARE lc_stmt;
  END LOOP;
  CLOSE indexes_cursor;
END$$
DELIMITER ;

CALL lc_drop_old_indexes();
CALL lc_add_column('students','student_code','VARCHAR(40) NULL');
CALL lc_add_column('students','guardian_name','VARCHAR(255) NULL');
CALL lc_add_column('students','guardian_phone','VARCHAR(255) NULL');
CALL lc_run('students','UPDATE students SET student_code=CONCAT(''HV-'',id) WHERE student_code IS NULL');

-- Old teaching mappings introduced competing columns on academic-owned tables.
CALL lc_add_column('enrollments','class_id','BIGINT NULL');
CALL lc_copy_column('enrollments','school_class_id','class_id');
CALL lc_relax_column('enrollments','school_class_id');
CALL lc_add_column('enrollments','study_status','VARCHAR(32) NULL');
CALL lc_copy_column('enrollments','status','study_status');
CALL lc_add_column('enrollments','billing_enrollment_id','BIGINT NULL');
CALL lc_add_column('enrollments','tuition_fee','DECIMAL(15,2) NULL');
CALL lc_add_column('enrollments','tuition_delivery','VARCHAR(32) NULL');
CALL lc_add_column('enrollments','enrolled_at','DATETIME NULL');
CALL lc_run('enrollments','UPDATE enrollments SET billing_enrollment_id=id WHERE billing_enrollment_id IS NULL');
CALL lc_run('enrollments','UPDATE enrollments e JOIN school_classes c ON c.id=e.class_id JOIN courses k ON k.id=c.course_id SET e.tuition_fee=k.tuition_fee WHERE e.tuition_fee IS NULL');
-- Above price fallback needs human review: old code did not retain the enrollment price.
CALL lc_run('enrollments','UPDATE enrollments SET tuition_delivery=''PENDING'' WHERE tuition_delivery IS NULL');
CALL lc_add_column('schedules','class_id','BIGINT NULL');
CALL lc_copy_column('schedules','school_class_id','class_id');
CALL lc_relax_column('schedules','school_class_id');
CALL lc_relax_column('schedules','room_id');
CALL lc_add_column('attendances','recorded_by','BIGINT NULL');
CALL lc_copy_column('attendances','recorded_by_account_id','recorded_by');
CALL lc_add_column('attendances','recorded_by_name','VARCHAR(255) NULL');
CALL lc_run('attendances','UPDATE attendances a JOIN accounts u ON u.id=a.recorded_by SET a.recorded_by_name=u.username WHERE a.recorded_by_name IS NULL');

CALL lc_add_column('tuition','enrollment_id','BIGINT NULL');
CALL lc_add_column('tuition','original_amount','DECIMAL(15,2) NULL');
CALL lc_add_column('tuition','discount_amount','DECIMAL(15,2) NULL');
CALL lc_add_column('tuition','paid_amount','DECIMAL(15,2) NULL');
CALL lc_run('tuition','UPDATE tuition SET original_amount=amount WHERE original_amount IS NULL');
CALL lc_run('tuition','UPDATE tuition SET discount_amount=0 WHERE discount_amount IS NULL');
CALL lc_run('tuition','UPDATE tuition t SET paid_amount=(SELECT COALESCE(SUM(p.amount),0) FROM payments p WHERE p.tuition_id=t.id AND p.status=''PAID'')');
CALL lc_add_column('payments','receipt_no','VARCHAR(255) NULL');
CALL lc_add_column('payments','request_key','VARCHAR(100) NULL');
CALL lc_run('payments','UPDATE payments SET receipt_no=CONCAT(''LEGACY-'',id), request_key=CONCAT(''legacy-'',id) WHERE receipt_no IS NULL');
-- Legacy enrollmentId mappings must be reconciled before delivery retry to avoid a second bill.

CALL lc_add_column('exams','class_id','BIGINT NULL');
CALL lc_copy_column('exams','school_class_id','class_id');
CALL lc_add_column('exams','branch_id','BIGINT NULL');
CALL lc_run('exams','UPDATE exams e JOIN school_classes c ON c.id=e.class_id SET e.branch_id=c.branch_id WHERE e.branch_id IS NULL');
CALL lc_add_column('exams','exam_type','VARCHAR(255) NULL');
CALL lc_copy_column('exams','type','exam_type');
CALL lc_add_column('exams','exam_date','DATE NULL');
CALL lc_add_column('exams','weight','DECIMAL(7,2) NULL');
CALL lc_add_column('exams','max_score','DECIMAL(7,2) NULL');
CALL lc_add_column('documents','class_id','BIGINT NULL');
CALL lc_copy_column('documents','school_class_id','class_id');
CALL lc_add_column('documents','branch_id','BIGINT NULL');
CALL lc_run('documents','UPDATE documents d JOIN school_classes c ON c.id=d.class_id SET d.branch_id=c.branch_id WHERE d.branch_id IS NULL');
CALL lc_add_column('documents','storage_key','VARCHAR(255) NULL');
CALL lc_add_column('documents','uploaded_by','BIGINT NULL');
CALL lc_copy_column('documents','uploaded_by_account_id','uploaded_by');
CALL lc_add_column('documents','reviewed_by','BIGINT NULL');
CALL lc_copy_column('documents','reviewed_by_account_id','reviewed_by');

-- Diagnostics: resolve every result before enabling traffic/tuition retry.
CALL lc_run('enrollments','SELECT id,class_id,study_status,enrolled_at,tuition_fee FROM enrollments WHERE class_id IS NULL OR study_status IS NULL OR enrolled_at IS NULL OR tuition_fee IS NULL');
CALL lc_run('tuition','SELECT id,student_id,class_id,amount,paid_amount FROM tuition WHERE enrollment_id IS NULL OR paid_amount>amount');
CALL lc_run('payments','SELECT p.id,p.branch_id,t.branch_id AS tuition_branch FROM payments p LEFT JOIN tuition t ON t.id=p.tuition_id WHERE t.id IS NULL OR p.branch_id<>t.branch_id');
CALL lc_run('exams','SELECT id FROM exams WHERE branch_id IS NULL OR class_id IS NULL OR exam_type IS NULL OR exam_date IS NULL OR weight IS NULL OR max_score IS NULL');
CALL lc_run('documents','SELECT id FROM documents WHERE branch_id IS NULL OR storage_key IS NULL OR uploaded_by IS NULL');

DROP PROCEDURE lc_drop_old_indexes;
DROP PROCEDURE lc_copy_column;
DROP PROCEDURE lc_run;
DROP PROCEDURE lc_relax_column;
DROP PROCEDURE lc_add_column;
