-- G2 additive schema; safe to replay. No inferred audiences, no rewriting policies.
CREATE TABLE IF NOT EXISTS audience_groups (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 version BIGINT NOT NULL DEFAULT 0,
 code VARCHAR(32) NOT NULL,
 name VARCHAR(100) NOT NULL,
 active BIT NOT NULL DEFAULT b'1',
 sort_order INT NOT NULL,
 CONSTRAINT uk_audience_code UNIQUE(code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
INSERT INTO audience_groups(code,name,sort_order) SELECT 'PRESCHOOL','Mẫu giáo',1 WHERE NOT EXISTS(SELECT 1 FROM audience_groups WHERE code='PRESCHOOL');
INSERT INTO audience_groups(code,name,sort_order) SELECT 'PRIMARY','Tiểu học',2 WHERE NOT EXISTS(SELECT 1 FROM audience_groups WHERE code='PRIMARY');
INSERT INTO audience_groups(code,name,sort_order) SELECT 'SECONDARY','THCS',3 WHERE NOT EXISTS(SELECT 1 FROM audience_groups WHERE code='SECONDARY');
INSERT INTO audience_groups(code,name,sort_order) SELECT 'HIGH_SCHOOL','THPT',4 WHERE NOT EXISTS(SELECT 1 FROM audience_groups WHERE code='HIGH_SCHOOL');
INSERT INTO audience_groups(code,name,sort_order) SELECT 'ADULT','Người lớn',5 WHERE NOT EXISTS(SELECT 1 FROM audience_groups WHERE code='ADULT');
INSERT INTO programs(code,name,active,version) SELECT 'GE','Tiếng Anh tổng quát',b'1',0 WHERE NOT EXISTS(SELECT 1 FROM programs WHERE code='GE');
INSERT INTO programs(code,name,active,version) SELECT 'COMMUNICATION','Tiếng Anh giao tiếp',b'1',0 WHERE NOT EXISTS(SELECT 1 FROM programs WHERE code='COMMUNICATION');
INSERT INTO programs(code,name,active,version) SELECT 'IELTS','IELTS',b'1',0 WHERE NOT EXISTS(SELECT 1 FROM programs WHERE code='IELTS');
INSERT INTO programs(code,name,active,version) SELECT 'TOEIC','TOEIC',b'1',0 WHERE NOT EXISTS(SELECT 1 FROM programs WHERE code='TOEIC');

SET @g2_ddl = IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='courses' AND column_name='program_id'), 'SELECT 1', 'ALTER TABLE courses ADD COLUMN program_id BIGINT NULL');
PREPARE g2_stmt FROM @g2_ddl; EXECUTE g2_stmt; DEALLOCATE PREPARE g2_stmt;
SET @g2_ddl = IF(EXISTS(SELECT 1 FROM information_schema.table_constraints WHERE constraint_schema=DATABASE() AND table_name='courses' AND constraint_name='fk_course_program'), 'SELECT 1', 'ALTER TABLE courses ADD CONSTRAINT fk_course_program FOREIGN KEY(program_id) REFERENCES programs(id) ON DELETE RESTRICT');
PREPARE g2_stmt FROM @g2_ddl; EXECUTE g2_stmt; DEALLOCATE PREPARE g2_stmt;
CREATE TABLE IF NOT EXISTS course_audiences (
 course_id BIGINT NOT NULL,
 audience_group_id BIGINT NOT NULL,
 CONSTRAINT uk_course_audience UNIQUE(course_id,audience_group_id),
 CONSTRAINT fk_course_audience_course FOREIGN KEY(course_id) REFERENCES courses(id) ON DELETE RESTRICT,
 CONSTRAINT fk_course_audience_group FOREIGN KEY(audience_group_id) REFERENCES audience_groups(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
-- program_id backfill is performed by the reviewed mapping tool, not a blind type/name UPDATE.
