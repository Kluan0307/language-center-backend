-- DS7-DS8. Preserve all original exam/grade/publication rows.
ALTER TABLE final_results ADD COLUMN grading_release_id bigint NULL;
ALTER TABLE grading_activities ADD COLUMN version bigint NOT NULL DEFAULT 0;
CREATE TABLE IF NOT EXISTS grading_legacy_imports (
 id bigint NOT NULL AUTO_INCREMENT, class_id bigint NOT NULL, request_id varchar(36) NOT NULL,
 fingerprint varchar(64) NOT NULL, intent varchar(64) NOT NULL, template_id varchar(60) NOT NULL,
 snapshot longtext NOT NULL, reason varchar(500) NOT NULL, actor_id bigint NOT NULL, created_at datetime(6) NOT NULL,
 PRIMARY KEY(id), UNIQUE KEY uk_grading_import_class(class_id), UNIQUE KEY uk_grading_import_request(request_id),
 CONSTRAINT fk_grading_import_binding FOREIGN KEY(class_id) REFERENCES grading_class_bindings(class_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS grading_completion_batches (
 request_id varchar(36) NOT NULL, class_id bigint NOT NULL, release_id bigint NOT NULL,
 intent varchar(64) NOT NULL, actor_id bigint NOT NULL, created_at datetime(6) NOT NULL,
 reason varchar(500) NOT NULL, snapshot longtext NOT NULL,
 PRIMARY KEY(request_id), KEY idx_grading_completion_class(class_id),
 CONSTRAINT fk_grading_batch_release FOREIGN KEY(release_id) REFERENCES grading_releases(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
