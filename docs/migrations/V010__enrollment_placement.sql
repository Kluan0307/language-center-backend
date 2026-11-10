-- MySQL8 after V009; append-only acceptance and controlled placement exceptions.
-- No legacy enrollment is assigned invented evidence; empty history means LEGACY_UNVERIFIED.
CREATE TABLE IF NOT EXISTS placement_exceptions (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,version BIGINT NOT NULL DEFAULT 0,
 student_id BIGINT NOT NULL,class_id BIGINT NOT NULL,branch_id BIGINT NOT NULL,
 intent VARCHAR(20) NOT NULL,source_enrollment_id BIGINT NULL,source_version BIGINT NULL,policy_id BIGINT NULL,class_revision BIGINT NOT NULL,assessment_id BIGINT NULL,assessment_version BIGINT NULL,context_snapshot LONGTEXT NOT NULL,
 fingerprint VARCHAR(64) NOT NULL,academic_status VARCHAR(40) NOT NULL,
 requested_reason VARCHAR(500) NOT NULL,requested_by BIGINT NOT NULL,requested_by_name VARCHAR(255) NOT NULL,requested_at DATETIME(6) NOT NULL,
 status VARCHAR(20) NOT NULL,decision_reason VARCHAR(500) NULL,decided_by BIGINT NULL,decided_by_name VARCHAR(255) NULL,decided_at DATETIME(6) NULL,
 consumed_enrollment_id BIGINT NULL,consumed_at DATETIME(6) NULL,
 INDEX ix_placement_exception_branch(branch_id,id),
 CONSTRAINT fk_placement_exception_student FOREIGN KEY(student_id) REFERENCES students(id),
 CONSTRAINT fk_placement_exception_class FOREIGN KEY(class_id) REFERENCES school_classes(id),
 CONSTRAINT fk_placement_exception_source FOREIGN KEY(source_enrollment_id) REFERENCES enrollments(id),
 CONSTRAINT fk_placement_exception_consumed FOREIGN KEY(consumed_enrollment_id) REFERENCES enrollments(id)
);
CREATE TABLE IF NOT EXISTS enrollment_admissions (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,enrollment_id BIGINT NOT NULL,student_id BIGINT NOT NULL,class_id BIGINT NOT NULL,
 policy_id BIGINT NULL,class_revision BIGINT NOT NULL,assessment_id BIGINT NULL,assessment_version BIGINT NULL,approval_id BIGINT NULL,reused_decision_id BIGINT NULL,
 intent VARCHAR(20) NOT NULL,academic_status VARCHAR(40) NOT NULL,fingerprint VARCHAR(64) NOT NULL,
 policy_snapshot LONGTEXT NOT NULL,assessment_snapshot LONGTEXT NOT NULL,actor_id BIGINT NOT NULL,actor_name VARCHAR(255) NOT NULL,accepted_at DATETIME(6) NOT NULL,
 INDEX ix_enrollment_admission(enrollment_id,id),
 CONSTRAINT fk_enrollment_admission_enrollment FOREIGN KEY(enrollment_id) REFERENCES enrollments(id),
 CONSTRAINT fk_enrollment_admission_approval FOREIGN KEY(approval_id) REFERENCES placement_exceptions(id)
);
