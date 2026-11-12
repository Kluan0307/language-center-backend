-- Apply after V014, with a backup. No legacy comment is copied into public feedback.
SET @lc_sql=(SELECT IF(COUNT(*)=0,'ALTER TABLE grades ADD COLUMN student_comment VARCHAR(1000) NULL','SELECT 1') FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='grades' AND column_name='student_comment');
PREPARE lc_stmt FROM @lc_sql; EXECUTE lc_stmt; DEALLOCATE PREPARE lc_stmt;
CREATE TABLE IF NOT EXISTS grade_publications (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 exam_id BIGINT NOT NULL, class_id BIGINT, branch_id BIGINT, revision BIGINT NOT NULL,
 request_id VARCHAR(36) NOT NULL, fingerprint VARCHAR(64) NOT NULL, draft_fingerprint VARCHAR(64) NOT NULL,
 action VARCHAR(20) NOT NULL, reason VARCHAR(500) NOT NULL, published_by BIGINT, published_at DATETIME(6),
 exam_name VARCHAR(255), exam_type VARCHAR(255), exam_date DATE,
 max_score DECIMAL(7,2), weight DECIMAL(7,2),
 UNIQUE KEY uk_grade_publication_revision(exam_id,revision), UNIQUE KEY uk_grade_publication_request(request_id),
 CONSTRAINT fk_publication_exam FOREIGN KEY(exam_id) REFERENCES exams(id)
);
CREATE TABLE IF NOT EXISTS published_grades (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, publication_id BIGINT NOT NULL,
 student_id BIGINT NOT NULL, enrollment_id BIGINT NOT NULL, score DECIMAL(7,2), student_comment VARCHAR(1000),
 UNIQUE KEY uk_published_grade(publication_id,student_id,enrollment_id),
 KEY idx_published_owner(student_id,enrollment_id),
 CONSTRAINT fk_published_grade_publication FOREIGN KEY(publication_id) REFERENCES grade_publications(id)
);
