SET @g3_ddl=IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='courses' AND column_name='classification_revision'),'SELECT 1','ALTER TABLE courses ADD COLUMN classification_revision BIGINT NOT NULL DEFAULT 0');
PREPARE g3_stmt FROM @g3_ddl; EXECUTE g3_stmt; DEALLOCATE PREPARE g3_stmt;
