CREATE TABLE IF NOT EXISTS website_edit_events (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 kind VARCHAR(20) NOT NULL, source_id BIGINT NOT NULL,
 actor_id BIGINT NOT NULL, actor_name VARCHAR(255) NOT NULL,
 occurred_at DATETIME(6) NOT NULL, state VARCHAR(20) NOT NULL,
 snapshot VARCHAR(16000) NOT NULL,
 INDEX idx_website_edit_source(kind,source_id,id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
