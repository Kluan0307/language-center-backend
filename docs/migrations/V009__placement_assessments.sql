-- MySQL 8 after V008. Legacy current_level stays unchanged and is not a confirmed assessment.
CREATE TABLE IF NOT EXISTS placement_assessments (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,version BIGINT NOT NULL DEFAULT 0,
 student_id BIGINT NOT NULL,branch_id BIGINT NOT NULL,
 program_id BIGINT NOT NULL,program_code VARCHAR(50) NOT NULL,program_name VARCHAR(255) NOT NULL,
 level_id BIGINT NOT NULL,level_code VARCHAR(50) NOT NULL,level_name VARCHAR(255) NOT NULL,
 method VARCHAR(20) NOT NULL,status VARCHAR(20) NOT NULL,
 assessed_on DATE NOT NULL,assessor_name VARCHAR(255) NOT NULL,source VARCHAR(2000) NOT NULL,
 score DECIMAL(12,4) NULL,max_score DECIMAL(12,4) NULL,
 recorded_by BIGINT NOT NULL,recorded_by_name VARCHAR(255) NOT NULL,
 confirmed_by BIGINT NULL,confirmed_by_name VARCHAR(255) NULL,confirmed_at DATETIME(6) NULL,
 supersedes_id BIGINT NULL,superseded_by_id BIGINT NULL,
 created_at DATETIME(6) NOT NULL,updated_at DATETIME(6) NOT NULL,
 current_key VARCHAR(90) GENERATED ALWAYS AS (CASE WHEN status='CONFIRMED' THEN CONCAT(student_id,':',program_id) ELSE NULL END) STORED,
 CONSTRAINT uk_assessment_current UNIQUE(current_key),
 CONSTRAINT fk_assessment_student FOREIGN KEY(student_id) REFERENCES students(id),
 INDEX idx_assessment_student_program(student_id,program_id,status)
);
CREATE TABLE IF NOT EXISTS assessment_events (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,assessment_id BIGINT NOT NULL,
 action VARCHAR(255) NOT NULL,before_state LONGTEXT NOT NULL,after_state LONGTEXT NOT NULL,reason VARCHAR(500) NULL,
 actor_id BIGINT NOT NULL,actor_name VARCHAR(255) NOT NULL,occurred_at DATETIME(6) NOT NULL,assessment_version BIGINT NOT NULL,
 INDEX idx_assessment_event(assessment_id,id)
);
