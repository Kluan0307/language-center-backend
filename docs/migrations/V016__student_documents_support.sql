-- After V015; back up first. Existing documents stay unshared.
SET @lc_sql=(SELECT IF(COUNT(*)=0,'ALTER TABLE documents ADD COLUMN shared_to_students BIT(1) NOT NULL DEFAULT b''0''','SELECT 1') FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='documents' AND column_name='shared_to_students');
PREPARE lc_stmt FROM @lc_sql; EXECUTE lc_stmt; DEALLOCATE PREPARE lc_stmt;
SET @lc_sql=(SELECT IF(COUNT(*)=0,'ALTER TABLE documents ADD COLUMN shared_at DATETIME(6) NULL','SELECT 1') FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='documents' AND column_name='shared_at');
PREPARE lc_stmt FROM @lc_sql; EXECUTE lc_stmt; DEALLOCATE PREPARE lc_stmt;
CREATE TABLE IF NOT EXISTS document_sharing_events(id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,document_id BIGINT,revision INT NOT NULL,shared_to_students BIT(1) NOT NULL,actor_id BIGINT,reason VARCHAR(500) NOT NULL,occurred_at DATETIME(6));
CREATE TABLE IF NOT EXISTS student_support_requests(
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,student_id BIGINT NOT NULL,branch_id BIGINT NOT NULL,category VARCHAR(20) NOT NULL,subject VARCHAR(120) NOT NULL,status VARCHAR(20) NOT NULL,
 resource_type VARCHAR(20),resource_id BIGINT,created_at DATETIME(6) NOT NULL,updated_at DATETIME(6) NOT NULL,version BIGINT NOT NULL DEFAULT 0,
 KEY idx_support_owner(student_id,branch_id,status),CONSTRAINT fk_support_student FOREIGN KEY(student_id) REFERENCES students(id),CONSTRAINT fk_support_branch FOREIGN KEY(branch_id) REFERENCES branches(id)
);
CREATE TABLE IF NOT EXISTS student_support_messages(
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,request_id BIGINT NOT NULL,message VARCHAR(2000) NOT NULL,internal_note BIT(1) NOT NULL,sender VARCHAR(20) NOT NULL,actor_id BIGINT,actor_name VARCHAR(255),created_at DATETIME(6) NOT NULL,
 KEY idx_support_message(request_id,id),CONSTRAINT fk_support_message FOREIGN KEY(request_id) REFERENCES student_support_requests(id)
);
CREATE TABLE IF NOT EXISTS student_support_operations(
 id VARCHAR(36) NOT NULL PRIMARY KEY,request_id BIGINT NOT NULL,actor_id BIGINT NOT NULL,fingerprint VARCHAR(64) NOT NULL,action VARCHAR(20) NOT NULL,created_at DATETIME(6) NOT NULL,
 CONSTRAINT fk_support_operation FOREIGN KEY(request_id) REFERENCES student_support_requests(id)
);
