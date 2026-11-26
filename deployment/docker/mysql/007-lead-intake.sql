-- TN1-TN2. Preserve leads, students and enrollments; do not auto-convert.
ALTER TABLE leads ADD COLUMN preferred_class_id bigint NULL;
ALTER TABLE leads ADD COLUMN preferred_time varchar(500) NULL;
ALTER TABLE leads ADD KEY idx_lead_preferred_class(preferred_class_id);
ALTER TABLE leads ADD CONSTRAINT fk_lead_preferred_class FOREIGN KEY(preferred_class_id) REFERENCES school_classes(id);
-- An existing learner may have multiple distinct consultation leads.
ALTER TABLE leads ADD KEY idx_lead_converted_student(converted_student_id);
ALTER TABLE leads DROP INDEX UKmnswa5bo24ajxyfwnnkxgdt88;
CREATE TABLE IF NOT EXISTS lead_intake_receipts (
 request_id varchar(36) NOT NULL,lead_id bigint NOT NULL,student_id bigint NOT NULL,enrollment_id bigint NULL,
 created_student bit NOT NULL,intent varchar(64) NOT NULL,actor_id bigint NOT NULL,created_at datetime(6) NOT NULL,reason varchar(500) NOT NULL,
 PRIMARY KEY(request_id),UNIQUE KEY uk_intake_lead(lead_id),
 CONSTRAINT fk_intake_lead FOREIGN KEY(lead_id) REFERENCES leads(id),
 CONSTRAINT fk_intake_student FOREIGN KEY(student_id) REFERENCES students(id),
 CONSTRAINT fk_intake_enrollment FOREIGN KEY(enrollment_id) REFERENCES enrollments(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
