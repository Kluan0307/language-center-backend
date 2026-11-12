-- Apply after V017 with a backup. No historical business events are backfilled,
-- except existing pending G8 payment_outbox rows bridged by Finance after upgrade.
CREATE TABLE IF NOT EXISTS student_notification_outbox (
 id VARCHAR(36) NOT NULL PRIMARY KEY,source_key VARCHAR(160) NOT NULL,category VARCHAR(20) NOT NULL,type VARCHAR(40) NOT NULL,title VARCHAR(120) NOT NULL,body VARCHAR(300) NOT NULL,
 student_id BIGINT,enrollment_id BIGINT,class_id BIGINT,course_id BIGINT,branch_id BIGINT,resource_type VARCHAR(20) NOT NULL,resource_id VARCHAR(80) NOT NULL,
 session_date DATE,previous_date DATE,occurred_at DATETIME(6) NOT NULL,delivered_at DATETIME(6),status VARCHAR(12) NOT NULL,recipient_count INT NOT NULL DEFAULT 0,
 UNIQUE KEY uk_notification_source(source_key),KEY idx_notification_pending(status,occurred_at,id)
);
CREATE TABLE IF NOT EXISTS student_notifications (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,event_id VARCHAR(36) NOT NULL,account_id BIGINT NOT NULL,student_id BIGINT NOT NULL,category VARCHAR(20) NOT NULL,type VARCHAR(40) NOT NULL,
 title VARCHAR(120) NOT NULL,body VARCHAR(300) NOT NULL,resource_type VARCHAR(20) NOT NULL,resource_id VARCHAR(80) NOT NULL,created_at DATETIME(6) NOT NULL,read_at DATETIME(6),
 UNIQUE KEY uk_notification_recipient(event_id,account_id,student_id),KEY idx_notification_inbox(account_id,student_id,read_at,created_at,id),
 CONSTRAINT fk_notification_event FOREIGN KEY(event_id) REFERENCES student_notification_outbox(id)
);
CREATE TABLE IF NOT EXISTS student_notification_preferences (
 account_id BIGINT NOT NULL PRIMARY KEY,push_enabled BIT(1) NOT NULL DEFAULT b'0',schedule BIT(1) NOT NULL DEFAULT b'1',grades BIT(1) NOT NULL DEFAULT b'1',tuition BIT(1) NOT NULL DEFAULT b'1',payment BIT(1) NOT NULL DEFAULT b'1',documents BIT(1) NOT NULL DEFAULT b'1',support BIT(1) NOT NULL DEFAULT b'1',version BIGINT NOT NULL DEFAULT 0
);
CREATE TABLE IF NOT EXISTS student_push_devices (
 installation_id VARCHAR(36) NOT NULL PRIMARY KEY,account_id BIGINT NOT NULL,session_version BIGINT NOT NULL,push_token VARCHAR(255) NOT NULL,platform VARCHAR(10) NOT NULL,project_id VARCHAR(36) NOT NULL,
 enabled BIT(1) NOT NULL DEFAULT b'1',updated_at DATETIME(6) NOT NULL,binding_version BIGINT NOT NULL DEFAULT 0,
 UNIQUE KEY uk_student_push_token(push_token),KEY idx_student_device_account(account_id,enabled)
);
CREATE TABLE IF NOT EXISTS student_push_deliveries (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,notification_id BIGINT NOT NULL,installation_id VARCHAR(36) NOT NULL,binding_version BIGINT NOT NULL,status VARCHAR(20) NOT NULL,
 ticket_id VARCHAR(100),result_code VARCHAR(60),attempted_at DATETIME(6),receipt_checked_at DATETIME(6),
 UNIQUE KEY uk_student_push_delivery(notification_id,installation_id),KEY idx_student_push_pending(status,id),
 CONSTRAINT fk_student_push_notification FOREIGN KEY(notification_id) REFERENCES student_notifications(id),
 CONSTRAINT fk_student_push_installation FOREIGN KEY(installation_id) REFERENCES student_push_devices(installation_id)
);
