-- Apply after V016 with a backup. No existing receipts or balances are changed.
CREATE TABLE IF NOT EXISTS payment_orders (
 id VARCHAR(36) NOT NULL PRIMARY KEY,account_id BIGINT NOT NULL,student_id BIGINT NOT NULL,tuition_id BIGINT NOT NULL,billing_root_id BIGINT NOT NULL,branch_id BIGINT NOT NULL,
 request_id VARCHAR(36) NOT NULL,fingerprint VARCHAR(64) NOT NULL,merchant_ref VARCHAR(40) NOT NULL,merchant_code VARCHAR(32) NOT NULL,provider VARCHAR(20) NOT NULL,environment VARCHAR(20) NOT NULL,
 amount DECIMAL(15,2) NOT NULL,currency VARCHAR(3) NOT NULL,state VARCHAR(20) NOT NULL,review_code VARCHAR(60),receipt_id BIGINT,
 created_at DATETIME(6) NOT NULL,expires_at DATETIME(6) NOT NULL,updated_at DATETIME(6) NOT NULL,client_ip VARCHAR(45) NOT NULL,version BIGINT NOT NULL DEFAULT 0,
 active_tuition_id BIGINT GENERATED ALWAYS AS (CASE WHEN state IN ('CREATED','PENDING') THEN tuition_id ELSE NULL END) STORED,
 UNIQUE KEY uk_order_intent(account_id,request_id),UNIQUE KEY uk_order_reference(merchant_ref),UNIQUE KEY uk_order_active_tuition(active_tuition_id),
 KEY idx_order_owner(account_id,student_id,created_at),KEY idx_order_branch(branch_id,state),KEY idx_order_expiry(state,expires_at),
 CONSTRAINT fk_order_tuition FOREIGN KEY(tuition_id) REFERENCES tuition(id),CONSTRAINT fk_order_receipt FOREIGN KEY(receipt_id) REFERENCES payments(id)
);
CREATE TABLE IF NOT EXISTS payment_provider_events (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,event_key VARCHAR(64) NOT NULL,source VARCHAR(12) NOT NULL,merchant_ref VARCHAR(40) NOT NULL,merchant_code VARCHAR(32) NOT NULL,
 transaction_no VARCHAR(32),amount DECIMAL(15,2) NOT NULL,outcome VARCHAR(12) NOT NULL,paid_at DATETIME(6),status VARCHAR(20) NOT NULL,result_code VARCHAR(60),order_id VARCHAR(36),branch_id BIGINT,
 received_at DATETIME(6) NOT NULL,processed_at DATETIME(6),version BIGINT NOT NULL DEFAULT 0,
 UNIQUE KEY uk_provider_event(event_key),KEY idx_event_pending(status,id),KEY idx_event_branch(branch_id,id),CONSTRAINT fk_event_order FOREIGN KEY(order_id) REFERENCES payment_orders(id)
);
CREATE TABLE IF NOT EXISTS payment_provider_transactions (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,merchant_code VARCHAR(32) NOT NULL,transaction_no VARCHAR(32) NOT NULL,order_id VARCHAR(36),amount DECIMAL(15,2) NOT NULL,
 paid_at DATETIME(6),state VARCHAR(20) NOT NULL,review_code VARCHAR(60),receipt_id BIGINT,created_at DATETIME(6) NOT NULL,version BIGINT NOT NULL DEFAULT 0,
 UNIQUE KEY uk_provider_transaction(merchant_code,transaction_no),UNIQUE KEY uk_transaction_receipt(receipt_id),KEY idx_transaction_order(order_id,state),
 CONSTRAINT fk_transaction_order FOREIGN KEY(order_id) REFERENCES payment_orders(id),CONSTRAINT fk_transaction_receipt FOREIGN KEY(receipt_id) REFERENCES payments(id)
);
CREATE TABLE IF NOT EXISTS payment_reconcile_operations (
 id VARCHAR(36) NOT NULL PRIMARY KEY,order_id VARCHAR(36) NOT NULL,actor_id BIGINT NOT NULL,branch_id BIGINT NOT NULL,fingerprint VARCHAR(64) NOT NULL,reason VARCHAR(500) NOT NULL,
 status VARCHAR(20) NOT NULL,event_id BIGINT,created_at DATETIME(6) NOT NULL,completed_at DATETIME(6),
 CONSTRAINT fk_reconcile_order FOREIGN KEY(order_id) REFERENCES payment_orders(id),CONSTRAINT fk_reconcile_event FOREIGN KEY(event_id) REFERENCES payment_provider_events(id)
);
CREATE TABLE IF NOT EXISTS payment_outbox (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,event_key VARCHAR(100) NOT NULL,type VARCHAR(40) NOT NULL,order_id VARCHAR(36) NOT NULL,student_id BIGINT NOT NULL,receipt_id BIGINT,
 created_at DATETIME(6) NOT NULL,delivered_at DATETIME(6),UNIQUE KEY uk_payment_outbox(event_key),KEY idx_outbox_pending(delivered_at,id),
 CONSTRAINT fk_outbox_order FOREIGN KEY(order_id) REFERENCES payment_orders(id)
);
-- Also support a table created earlier by local Hibernate update. Duplicate active
-- rows must be reconciled explicitly if the unique index cannot be installed.
SET @lc_sql=(SELECT IF(COUNT(*)=0,'ALTER TABLE payment_orders ADD COLUMN active_tuition_id BIGINT GENERATED ALWAYS AS (CASE WHEN state IN (''CREATED'',''PENDING'') THEN tuition_id ELSE NULL END) STORED','SELECT 1') FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='payment_orders' AND column_name='active_tuition_id');
PREPARE lc_stmt FROM @lc_sql; EXECUTE lc_stmt; DEALLOCATE PREPARE lc_stmt;
SET @lc_sql=(SELECT IF(COUNT(*)=0,'ALTER TABLE payment_orders ADD UNIQUE KEY uk_order_active_tuition(active_tuition_id)','SELECT 1') FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='payment_orders' AND index_name='uk_order_active_tuition');
PREPARE lc_stmt FROM @lc_sql; EXECUTE lc_stmt; DEALLOCATE PREPARE lc_stmt;
