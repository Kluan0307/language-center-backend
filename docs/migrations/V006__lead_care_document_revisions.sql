-- MySQL 8, after V005. Back up leads/documents and document storage before applying.
SET @lc_v006_sql=(SELECT IF(COUNT(*)=0,'ALTER TABLE leads ADD COLUMN version BIGINT NOT NULL DEFAULT 0','SELECT 1') FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='leads' AND column_name='version');
PREPARE lc_v006_stmt FROM @lc_v006_sql;EXECUTE lc_v006_stmt;DEALLOCATE PREPARE lc_v006_stmt;
SET @lc_v006_sql=(SELECT IF(COUNT(*)=0,'ALTER TABLE leads ADD COLUMN next_contact_at DATETIME(6) NULL','SELECT 1') FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='leads' AND column_name='next_contact_at');
PREPARE lc_v006_stmt FROM @lc_v006_sql;EXECUTE lc_v006_stmt;DEALLOCATE PREPARE lc_v006_stmt;
SET @lc_v006_sql=(SELECT IF(COUNT(*)=0,'CREATE INDEX idx_lead_followup ON leads(branch_id,status,next_contact_at)','SELECT 1') FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='leads' AND index_name='idx_lead_followup');
PREPARE lc_v006_stmt FROM @lc_v006_sql;EXECUTE lc_v006_stmt;DEALLOCATE PREPARE lc_v006_stmt;
SET @lc_v006_sql=(SELECT IF(COUNT(*)=0,'ALTER TABLE documents ADD COLUMN revision INT NOT NULL DEFAULT 1','SELECT 1') FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='documents' AND column_name='revision');
PREPARE lc_v006_stmt FROM @lc_v006_sql;EXECUTE lc_v006_stmt;DEALLOCATE PREPARE lc_v006_stmt;
SET @lc_v006_sql=(SELECT IF(COUNT(*)=0,'ALTER TABLE documents ADD COLUMN last_submitted_at DATETIME(6) NULL','SELECT 1') FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='documents' AND column_name='last_submitted_at');
PREPARE lc_v006_stmt FROM @lc_v006_sql;EXECUTE lc_v006_stmt;DEALLOCATE PREPARE lc_v006_stmt;
UPDATE documents SET last_submitted_at=uploaded_at WHERE last_submitted_at IS NULL;
UPDATE documents SET version=0 WHERE version IS NULL;
SET @lc_v006_sql=(SELECT IF(COUNT(*)=0,'ALTER TABLE leads ADD COLUMN updated_at DATETIME(6) NULL','SELECT 1') FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='leads' AND column_name='updated_at');
PREPARE lc_v006_stmt FROM @lc_v006_sql;EXECUTE lc_v006_stmt;DEALLOCATE PREPARE lc_v006_stmt;
CREATE TABLE IF NOT EXISTS lead_activities (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,lead_id BIGINT NOT NULL,action VARCHAR(255) NOT NULL,note VARCHAR(2000) NOT NULL,channel VARCHAR(255) NULL,
 old_status VARCHAR(255) NULL,new_status VARCHAR(255) NOT NULL,old_assignee_id BIGINT NULL,old_assignee_name VARCHAR(255) NULL,
 assignee_id BIGINT NOT NULL,assignee_name VARCHAR(255) NOT NULL,old_next_contact_at DATETIME(6) NULL,next_contact_at DATETIME(6) NULL,
 actor_id BIGINT NOT NULL,actor_name VARCHAR(255) NOT NULL,occurred_at DATETIME(6) NOT NULL,INDEX idx_lead_activity(lead_id,id)
);
CREATE TABLE IF NOT EXISTS document_events (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,document_id BIGINT NOT NULL,revision INT NOT NULL,action VARCHAR(255) NOT NULL,status VARCHAR(255) NOT NULL,
 storage_key VARCHAR(255) NOT NULL,title VARCHAR(255) NOT NULL,description VARCHAR(1000) NULL,filename VARCHAR(255) NULL,file_size BIGINT NULL,
 reason VARCHAR(500) NULL,actor_id BIGINT NULL,actor_name VARCHAR(255) NULL,occurred_at DATETIME(6) NOT NULL,INDEX idx_document_event(document_id,revision,id)
);
-- Existing free-text lead notes remain intact; no contact history or old revisions are invented.
