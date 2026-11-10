-- MySQL 8, after V004. Back up tuition/payments before applying.
-- Append-only journals; no receipt rewrite, guessed historic refunds or automatic balance repair.
SET @lc_v005_sql=(SELECT IF(COUNT(*)=0,'ALTER TABLE tuition ADD COLUMN version BIGINT NOT NULL DEFAULT 0','SELECT 1') FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='tuition' AND column_name='version');
PREPARE lc_v005_stmt FROM @lc_v005_sql;EXECUTE lc_v005_stmt;DEALLOCATE PREPARE lc_v005_stmt;
SET @lc_v005_sql=(SELECT IF(MAX(character_maximum_length)<500,'ALTER TABLE tuition MODIFY adjustment_reason VARCHAR(500) NULL','SELECT 1') FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='tuition' AND column_name='adjustment_reason');
PREPARE lc_v005_stmt FROM @lc_v005_sql;EXECUTE lc_v005_stmt;DEALLOCATE PREPARE lc_v005_stmt;
CREATE TABLE IF NOT EXISTS payment_corrections (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 payment_id BIGINT NOT NULL,tuition_id BIGINT NOT NULL,branch_id BIGINT NOT NULL,
 type VARCHAR(255) NOT NULL,amount DECIMAL(15,2) NOT NULL,payment_method VARCHAR(255) NULL,
 reference_code VARCHAR(100) NULL,reason VARCHAR(500) NOT NULL,request_key VARCHAR(100) NOT NULL,
 expected_version BIGINT NOT NULL,recorded_by BIGINT NOT NULL,recorded_by_name VARCHAR(255) NOT NULL,created_at DATETIME(6) NOT NULL,
 CONSTRAINT uk_correction_request UNIQUE(branch_id,request_key),
 INDEX idx_correction_payment(payment_id,id),INDEX idx_correction_tuition(tuition_id,id),INDEX idx_correction_branch_date(branch_id,created_at)
);
CREATE TABLE IF NOT EXISTS tuition_adjustments (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 tuition_id BIGINT NOT NULL,branch_id BIGINT NOT NULL,
 old_original DECIMAL(15,2) NOT NULL,old_discount DECIMAL(15,2) NOT NULL,old_amount DECIMAL(15,2) NOT NULL,old_due_date DATE NOT NULL,
 new_original DECIMAL(15,2) NOT NULL,new_discount DECIMAL(15,2) NOT NULL,new_amount DECIMAL(15,2) NOT NULL,new_due_date DATE NOT NULL,
 paid_at_change DECIMAL(15,2) NOT NULL,reason VARCHAR(500) NOT NULL,request_key VARCHAR(100) NOT NULL,
 expected_version BIGINT NOT NULL,recorded_by BIGINT NOT NULL,recorded_by_name VARCHAR(255) NOT NULL,created_at DATETIME(6) NOT NULL,
 CONSTRAINT uk_adjustment_request UNIQUE(branch_id,request_key),INDEX idx_adjustment_tuition(tuition_id,id)
);
SELECT t.id AS tuition_to_review,COALESCE(t.paid_amount,0) AS cached,
 COALESCE(p.gross,0)-COALESCE(c.removed,0) AS ledger
FROM tuition t LEFT JOIN (SELECT tuition_id,SUM(amount) AS gross FROM payments WHERE status='PAID' GROUP BY tuition_id) p ON p.tuition_id=t.id
LEFT JOIN (SELECT tuition_id,SUM(amount) AS removed FROM payment_corrections GROUP BY tuition_id) c ON c.tuition_id=t.id
WHERE COALESCE(t.paid_amount,0)<>COALESCE(p.gross,0)-COALESCE(c.removed,0);
SELECT status,COUNT(*) AS legacy_payments_to_review FROM payments WHERE status<>'PAID' GROUP BY status;
