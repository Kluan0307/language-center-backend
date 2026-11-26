-- DS1-DS3: additive only. Existing exams, grades, publications and final results remain unchanged.
-- Apply before starting teaching-service with schema validation. No automatic class migration.
CREATE TABLE IF NOT EXISTS grading_templates (
 id varchar(60) NOT NULL, code varchar(30) NOT NULL, revision bigint NOT NULL,
 name varchar(160) NOT NULL, specification longtext NOT NULL,
 created_by bigint NOT NULL, created_at datetime(6) NOT NULL, reason varchar(500) NOT NULL,
 PRIMARY KEY(id), UNIQUE KEY uk_grading_template_revision(code,revision)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS grading_class_bindings (
 class_id bigint NOT NULL, branch_id bigint NOT NULL, course_id bigint NOT NULL,
 revision bigint NOT NULL, template_id varchar(60) NOT NULL, name varchar(160) NOT NULL,
 specification longtext NOT NULL, locked bit NOT NULL, assigned_by bigint NOT NULL,
 assigned_at datetime(6) NOT NULL, version bigint NOT NULL DEFAULT 0,
 PRIMARY KEY(class_id), CONSTRAINT fk_grading_binding_template FOREIGN KEY(template_id) REFERENCES grading_templates(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS grading_course_defaults (
 id varchar(80) NOT NULL, course_id bigint NOT NULL, branch_id bigint NOT NULL,
 template_id varchar(60) NOT NULL, revision bigint NOT NULL, version bigint NOT NULL DEFAULT 0,
 PRIMARY KEY(id), UNIQUE KEY uk_grading_course_branch(course_id,branch_id),
 CONSTRAINT fk_grading_course_template FOREIGN KEY(template_id) REFERENCES grading_templates(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS grading_activities (
 id bigint NOT NULL AUTO_INCREMENT, class_id bigint NOT NULL, name varchar(160) NOT NULL,
 assessment_date date NOT NULL, components text NOT NULL, roster_frozen_at datetime(6), created_by bigint NOT NULL,
 PRIMARY KEY(id), KEY idx_grading_activity_class(class_id),
 CONSTRAINT fk_grading_activity_binding FOREIGN KEY(class_id) REFERENCES grading_class_bindings(class_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS grading_candidates (
 activity_id bigint NOT NULL, student_id bigint NOT NULL, student_name varchar(255) NOT NULL, enrollment_id bigint NOT NULL,
 UNIQUE KEY uk_grading_candidate(activity_id,student_id),
 CONSTRAINT fk_grading_candidate_activity FOREIGN KEY(activity_id) REFERENCES grading_activities(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS grading_marks (
 id bigint NOT NULL AUTO_INCREMENT, activity_id bigint NOT NULL, student_id bigint NOT NULL,
 component varchar(40) NOT NULL, status varchar(30) NOT NULL, score decimal(7,2), level varchar(30),
 comment varchar(1000), entered_by bigint NOT NULL, updated_at datetime(6) NOT NULL, version bigint NOT NULL DEFAULT 0,
 PRIMARY KEY(id), UNIQUE KEY uk_grading_mark(activity_id,student_id,component),
 CONSTRAINT fk_grading_mark_activity FOREIGN KEY(activity_id) REFERENCES grading_activities(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS grading_events (
 id bigint NOT NULL AUTO_INCREMENT, class_id bigint, kind varchar(40) NOT NULL,
 actor_id bigint NOT NULL, occurred_at datetime(6) NOT NULL, reason varchar(500) NOT NULL,
 before_value longtext, after_value longtext NOT NULL,
 PRIMARY KEY(id), KEY idx_grading_event_class(class_id,id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
