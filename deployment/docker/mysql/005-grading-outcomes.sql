-- DS4-DS6. Additive migration; do not modify existing marks or publications.
ALTER TABLE grading_activities ADD COLUMN retake_of bigint NULL;
ALTER TABLE grading_activities ADD COLUMN conversion_id bigint NULL;
ALTER TABLE grading_marks ADD COLUMN raw_score int NULL;
ALTER TABLE grading_marks ADD COLUMN conversion_id bigint NULL;
CREATE TABLE IF NOT EXISTS grading_conversions (
 id bigint NOT NULL AUTO_INCREMENT, code varchar(40) NOT NULL, revision bigint NOT NULL,
 name varchar(160) NOT NULL, reference varchar(1000) NOT NULL, mapping longtext NOT NULL,
 created_by bigint NOT NULL, created_at datetime(6) NOT NULL,
 PRIMARY KEY(id), UNIQUE KEY uk_grading_conversion_revision(code,revision)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS grading_releases (
 id bigint NOT NULL AUTO_INCREMENT, class_id bigint NOT NULL, revision bigint NOT NULL,
 state varchar(20) NOT NULL, action varchar(20) NOT NULL, request_id varchar(36) NOT NULL,
 intent varchar(64) NOT NULL, fingerprint varchar(64) NOT NULL, snapshot longtext NOT NULL,
 actor_id bigint NOT NULL, submitted_by bigint NOT NULL, reason varchar(500) NOT NULL,
 occurred_at datetime(6) NOT NULL,
 PRIMARY KEY(id), UNIQUE KEY uk_grading_release_revision(class_id,revision),
 UNIQUE KEY uk_grading_release_request(request_id),
 CONSTRAINT fk_grading_release_binding FOREIGN KEY(class_id) REFERENCES grading_class_bindings(class_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
