-- Additive only. Existing operational records remain PRIVATE until explicitly published.
-- Run manually when DDL_AUTO=validate/none; default development DDL_AUTO=update creates this table.
CREATE TABLE IF NOT EXISTS website_publications (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 version BIGINT NOT NULL DEFAULT 0,
 kind ENUM('PROGRAM','COURSE','BRANCH','CLASS') NOT NULL,
 source_id BIGINT NOT NULL,
 slug VARCHAR(120) NOT NULL,
 state ENUM('DRAFT','PUBLISHED') NOT NULL DEFAULT 'DRAFT',
 summary VARCHAR(2000) NULL,
 audience VARCHAR(2000) NULL,
 outcomes VARCHAR(4000) NULL,
 topics VARCHAR(4000) NULL,
 image_url VARCHAR(1000) NULL,
 opening_hours VARCHAR(500) NULL,
 map_url VARCHAR(1000) NULL,
 program_id BIGINT NULL,
 show_tuition BIT NOT NULL DEFAULT b'0',
 updated_at DATETIME(6) NOT NULL,
 updated_by BIGINT NOT NULL,
 CONSTRAINT uk_website_source UNIQUE(kind,source_id),
 CONSTRAINT uk_website_slug UNIQUE(kind,slug),
 INDEX idx_website_public(kind,state,source_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
-- source_id is polymorphic. Service validates source existence and suppresses deleted sources.
-- No INSERT/UPDATE of operational data and no automatic publication.
