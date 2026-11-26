-- Additive L6/L7. Does not create or publish sample content or modify existing leads/accounts.
CREATE TABLE IF NOT EXISTS website_inquiries (
 request_id VARCHAR(36) NOT NULL PRIMARY KEY,
 fingerprint VARCHAR(64) NOT NULL,
 phone_hash VARCHAR(64) NOT NULL,
 lead_id BIGINT NOT NULL,
 branch_id BIGINT NOT NULL,
 program_slug VARCHAR(120) NULL,
 course_slug VARCHAR(120) NULL,
 class_slug VARCHAR(120) NULL,
 consent_version VARCHAR(50) NOT NULL,
 created_at DATETIME(6) NOT NULL,
 INDEX idx_website_inquiry_phone(phone_hash,created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS website_teacher_profiles (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 version BIGINT NOT NULL DEFAULT 0,
 teacher_id BIGINT NOT NULL,
 slug VARCHAR(120) NOT NULL,
 state ENUM('DRAFT','PUBLISHED') NOT NULL DEFAULT 'DRAFT',
 biography VARCHAR(2000) NULL,
 teaching_focus VARCHAR(500) NULL,
 image_url VARCHAR(1000) NULL,
 show_qualification BIT NOT NULL DEFAULT b'0',
 updated_at DATETIME(6) NOT NULL,
 updated_by BIGINT NOT NULL,
 CONSTRAINT uk_website_teacher_source UNIQUE(teacher_id),
 CONSTRAINT uk_website_teacher_slug UNIQUE(slug)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
-- actor_id=0 / actor_name='Website' identifies public-origin lead activities; it is not an account.
