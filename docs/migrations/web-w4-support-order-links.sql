-- MySQL 8, additive migration. Select your existing database before running.
-- No DELETE/TRUNCATE and no changes to payment/tuition amounts.
-- Back up the database first. Run once before starting academic-service with DDL_AUTO=validate.
-- With the existing development DDL_AUTO=update Hibernate adds the same nullable column/table.

SET @w4_schema = DATABASE();
SET @w4_sql = IF(
  EXISTS(SELECT 1 FROM information_schema.columns
         WHERE table_schema=@w4_schema AND table_name='student_support_requests' AND column_name='resource_key'),
  'SELECT 1',
  'ALTER TABLE student_support_requests ADD COLUMN resource_key VARCHAR(36) NULL'
);
PREPARE w4_statement FROM @w4_sql;
EXECUTE w4_statement;
DEALLOCATE PREPARE w4_statement;

CREATE TABLE IF NOT EXISTS student_support_status_events (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  request_id BIGINT NOT NULL,
  previous_status VARCHAR(20) NULL,
  status VARCHAR(20) NOT NULL,
  actor_id BIGINT NOT NULL,
  actor_name VARCHAR(100) NOT NULL,
  reason VARCHAR(500) NOT NULL,
  created_at DATETIME(6) NOT NULL,
  INDEX idx_support_status_request (request_id,id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Old tickets keep their numeric resource_id and existing messages.
-- We do not invent historical state events for old tickets.
