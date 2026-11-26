-- Schema only; generated from local schema on 2026-11-24. No application rows.

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;
DROP TABLE IF EXISTS `account_branch_events`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `account_branch_events` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `account_id` bigint NOT NULL,
  `branch_id` bigint NOT NULL,
  `action` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  `occurred_at` datetime(6) NOT NULL,
  `performed_by` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_branch_event_account_time` (`account_id`,`occurred_at`),
  CONSTRAINT `fk_branch_event_account` FOREIGN KEY (`account_id`) REFERENCES `accounts` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `account_branches`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `account_branches` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `account_id` bigint NOT NULL,
  `branch_id` bigint NOT NULL,
  `status` bit(1) NOT NULL,
  `is_primary` bit(1) NOT NULL,
  `assigned_at` datetime(6) NOT NULL,
  `assigned_by` bigint DEFAULT NULL,
  `revoked_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_account_branch` (`account_id`,`branch_id`),
  KEY `idx_membership_branch_status` (`branch_id`,`status`),
  CONSTRAINT `fk_membership_account` FOREIGN KEY (`account_id`) REFERENCES `accounts` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `account_security_events`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `account_security_events` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `account_id` bigint NOT NULL,
  `actor_id` bigint NOT NULL,
  `action` varchar(40) COLLATE utf8mb4_unicode_ci NOT NULL,
  `occurred_at` datetime(6) NOT NULL,
  `reason` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_security_event_account_time` (`account_id`,`occurred_at`),
  CONSTRAINT `fk_security_event_account` FOREIGN KEY (`account_id`) REFERENCES `accounts` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `accounts`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `accounts` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `last_login` datetime(6) DEFAULT NULL,
  `password` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `role` enum('ADMIN','STAFF','STUDENT','SUPER_ADMIN','TEACHER') COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` bit(1) NOT NULL,
  `username` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `branch_id` bigint DEFAULT NULL,
  `session_version` bigint NOT NULL DEFAULT '0',
  `must_change_password` bit(1) NOT NULL DEFAULT b'0',
  `password_changed_at` datetime(6) DEFAULT NULL,
  `personnel_code` varchar(24) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKk8h1bgqoplx0rkngj01pm1rgp` (`username`),
  UNIQUE KEY `uk_account_personnel_code` (`personnel_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `admins`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `admins` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `email` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `full_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `phone` varchar(15) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `account_id` bigint NOT NULL,
  `hire_date` date DEFAULT NULL,
  `work_status` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `version` bigint NOT NULL DEFAULT '0',
  `end_date` date DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKd1mh9asac0y4phlp5y7m02ben` (`account_id`),
  CONSTRAINT `FKji7l5mi8tqv73veb5cd90hupx` FOREIGN KEY (`account_id`) REFERENCES `accounts` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `assessment_events`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `assessment_events` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `action` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `actor_id` bigint NOT NULL,
  `actor_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `after_state` longtext COLLATE utf8mb4_unicode_ci NOT NULL,
  `assessment_id` bigint NOT NULL,
  `assessment_version` bigint NOT NULL,
  `before_state` longtext COLLATE utf8mb4_unicode_ci NOT NULL,
  `occurred_at` datetime(6) NOT NULL,
  `reason` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_assessment_event` (`assessment_id`,`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `attendance_changes`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `attendance_changes` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `session_id` bigint NOT NULL,
  `enrollment_id` bigint NOT NULL,
  `student_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `old_status` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `new_status` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `old_permission` bit(1) DEFAULT NULL,
  `new_permission` bit(1) NOT NULL,
  `previous_recorded_by` bigint DEFAULT NULL,
  `previous_recorded_by_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `changed_by` bigint NOT NULL,
  `changed_by_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `changed_at` datetime(6) NOT NULL,
  `reason` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_attendance_change_session` (`session_id`,`id`),
  CONSTRAINT `fk_attendance_change_session` FOREIGN KEY (`session_id`) REFERENCES `class_sessions` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `attendances`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `attendances` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `has_permission` bit(1) NOT NULL,
  `recorded_by` bigint NOT NULL,
  `recorded_by_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `session_date` date NOT NULL,
  `status` enum('ABSENT','LATE','PRESENT') COLLATE utf8mb4_unicode_ci NOT NULL,
  `enrollment_id` bigint NOT NULL,
  `class_session_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_attendance_enrollment_date` (`enrollment_id`,`session_date`),
  KEY `fk_attendance_session` (`class_session_id`),
  CONSTRAINT `FK7j92ropr8c7lg7jyrwbkffisy` FOREIGN KEY (`enrollment_id`) REFERENCES `enrollments` (`id`),
  CONSTRAINT `fk_attendance_session` FOREIGN KEY (`class_session_id`) REFERENCES `class_sessions` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `audience_groups`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `audience_groups` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `version` bigint NOT NULL DEFAULT '0',
  `code` varchar(32) NOT NULL,
  `name` varchar(100) NOT NULL,
  `active` bit(1) NOT NULL DEFAULT b'1',
  `sort_order` int NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_audience_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `branches`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `branches` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `address` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `code` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `phone` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` enum('ACTIVE','INACTIVE') COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_branch_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `class_admission_events`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `class_admission_events` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `actor_id` bigint NOT NULL,
  `actor_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `class_id` bigint NOT NULL,
  `new_policy_id` bigint DEFAULT NULL,
  `occurred_at` datetime(6) NOT NULL,
  `old_policy_id` bigint DEFAULT NULL,
  `reason` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL,
  `revision` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_class_admission_event` (`class_id`,`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `class_closure_events`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `class_closure_events` (
  `request_id` varchar(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `actor_id` bigint DEFAULT NULL,
  `actor_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `changed_at` datetime(6) DEFAULT NULL,
  `class_id` bigint DEFAULT NULL,
  `closed` bit(1) NOT NULL,
  `expected_revision` bigint NOT NULL,
  `reason` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `revision` bigint NOT NULL,
  PRIMARY KEY (`request_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `class_closures`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `class_closures` (
  `class_id` bigint NOT NULL,
  `actor_id` bigint DEFAULT NULL,
  `actor_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `changed_at` datetime(6) DEFAULT NULL,
  `closed` bit(1) NOT NULL,
  `reason` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `result_revision` bigint NOT NULL,
  `revision` bigint NOT NULL,
  `source_revision` bigint NOT NULL,
  PRIMARY KEY (`class_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `class_duration_plans`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `class_duration_plans` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `actor_id` bigint NOT NULL,
  `actor_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `class_id` bigint NOT NULL,
  `course_id` bigint NOT NULL,
  `course_revision` bigint DEFAULT NULL,
  `expected_minutes` int DEFAULT NULL,
  `occurred_at` datetime(6) NOT NULL,
  `reason` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL,
  `revision` bigint NOT NULL,
  `source` enum('COURSE','CUSTOM','UNVERIFIED') COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_class_duration_revision` (`class_id`,`revision`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `class_result_barriers`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `class_result_barriers` (
  `class_id` bigint NOT NULL,
  `branch_id` bigint DEFAULT NULL,
  `confirmed_count` bigint NOT NULL,
  `result_revision` bigint NOT NULL,
  `source_revision` bigint NOT NULL,
  `token` varchar(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`class_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `class_sessions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `class_sessions` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `class_id` bigint NOT NULL,
  `session_date` date NOT NULL,
  `start_time` time(6) DEFAULT NULL,
  `end_time` time(6) DEFAULT NULL,
  `room_id` bigint DEFAULT NULL,
  `room_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `planned_teacher_id` bigint DEFAULT NULL,
  `planned_teacher_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `teacher_id` bigint DEFAULT NULL,
  `teacher_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `kind` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `source_date` date DEFAULT NULL,
  `source_schedule_id` bigint DEFAULT NULL,
  `makeup_for_id` bigint DEFAULT NULL,
  `makeup_session_id` bigint DEFAULT NULL,
  `customized` bit(1) NOT NULL DEFAULT b'0',
  `reason` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `version` bigint NOT NULL DEFAULT '0',
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_session_source` (`class_id`,`source_date`),
  UNIQUE KEY `uk_session_makeup` (`makeup_for_id`),
  KEY `idx_session_date_teacher` (`session_date`,`teacher_id`,`status`),
  KEY `idx_session_date_room` (`session_date`,`room_id`,`status`),
  CONSTRAINT `fk_session_class` FOREIGN KEY (`class_id`) REFERENCES `school_classes` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `company_profile`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `company_profile` (
  `id` bigint NOT NULL,
  `version` bigint NOT NULL DEFAULT '0',
  `name` varchar(120) COLLATE utf8mb4_unicode_ci NOT NULL,
  `logo_url` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `address` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `email` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `phone` varchar(30) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `course_admission_levels`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `course_admission_levels` (
  `policy_id` bigint NOT NULL,
  `code` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `level_id` bigint NOT NULL,
  `name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  UNIQUE KEY `uk_admission_level` (`policy_id`,`level_id`),
  CONSTRAINT `FKk8qnpumbvrcaenfwwp3dh1t25` FOREIGN KEY (`policy_id`) REFERENCES `course_admission_policies` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `course_admission_policies`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `course_admission_policies` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `confirmed_at` datetime(6) NOT NULL,
  `confirmed_by` bigint NOT NULL,
  `confirmed_by_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `course_id` bigint NOT NULL,
  `mode` enum('OPEN_ENTRY','REQUIRES_ASSESSMENT','UNVERIFIED') COLLATE utf8mb4_unicode_ci NOT NULL,
  `output_level_code` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `output_level_id` bigint NOT NULL,
  `output_level_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `program_code` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `program_id` bigint NOT NULL,
  `program_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `reason` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL,
  `revision` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_course_admission_revision` (`course_id`,`revision`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `course_audiences`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `course_audiences` (
  `course_id` bigint NOT NULL,
  `audience_group_id` bigint NOT NULL,
  UNIQUE KEY `uk_course_audience` (`course_id`,`audience_group_id`),
  KEY `fk_course_audience_group` (`audience_group_id`),
  CONSTRAINT `fk_course_audience_course` FOREIGN KEY (`course_id`) REFERENCES `courses` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_course_audience_group` FOREIGN KEY (`audience_group_id`) REFERENCES `audience_groups` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `course_duration_plans`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `course_duration_plans` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `actor_id` bigint NOT NULL,
  `actor_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `course_id` bigint NOT NULL,
  `expected_minutes` int NOT NULL,
  `occurred_at` datetime(6) NOT NULL,
  `reason` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL,
  `revision` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_course_duration_revision` (`course_id`,`revision`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `courses`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `courses` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `branch_id` bigint DEFAULT NULL,
  `code` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `content` text COLLATE utf8mb4_unicode_ci,
  `duration` int DEFAULT NULL,
  `entry_requirement` text COLLATE utf8mb4_unicode_ci,
  `level` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `max_students` int NOT NULL,
  `name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `scope_key` bigint GENERATED ALWAYS AS (coalesce(`branch_id`,0)) STORED,
  `status` enum('CLOSED','OPEN') COLLATE utf8mb4_unicode_ci NOT NULL,
  `tuition_fee` decimal(15,2) NOT NULL,
  `type` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `duration_revision` bigint NOT NULL,
  `expected_minutes` int DEFAULT NULL,
  `program_id` bigint DEFAULT NULL,
  `classification_revision` bigint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_course_code_scope` (`code`,`scope_key`),
  KEY `fk_course_program` (`program_id`),
  CONSTRAINT `fk_course_program` FOREIGN KEY (`program_id`) REFERENCES `programs` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `document_events`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `document_events` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `document_id` bigint NOT NULL,
  `revision` int NOT NULL,
  `action` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `storage_key` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `title` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `description` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `filename` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `file_size` bigint DEFAULT NULL,
  `reason` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `actor_id` bigint DEFAULT NULL,
  `actor_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `occurred_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_document_event` (`document_id`,`revision`,`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `document_sharing_events`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `document_sharing_events` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `actor_id` bigint DEFAULT NULL,
  `document_id` bigint DEFAULT NULL,
  `occurred_at` datetime(6) DEFAULT NULL,
  `reason` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL,
  `revision` int NOT NULL,
  `shared_to_students` bit(1) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `documents`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `documents` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `branch_id` bigint NOT NULL,
  `class_id` bigint DEFAULT NULL,
  `course_id` bigint DEFAULT NULL,
  `description` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `file_size` bigint DEFAULT NULL,
  `original_filename` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `reject_reason` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `reviewed_at` datetime(6) DEFAULT NULL,
  `reviewed_by` bigint DEFAULT NULL,
  `reviewed_by_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `status` enum('APPROVED','PENDING','REJECTED') COLLATE utf8mb4_unicode_ci NOT NULL,
  `storage_key` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `title` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `uploaded_at` datetime(6) NOT NULL,
  `uploaded_by` bigint NOT NULL,
  `uploaded_by_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `version` bigint NOT NULL,
  `revision` int NOT NULL DEFAULT '1',
  `last_submitted_at` datetime(6) DEFAULT NULL,
  `shared_at` datetime(6) DEFAULT NULL,
  `shared_to_students` bit(1) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_document_branch_status` (`branch_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `employment_events`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `employment_events` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `account_id` bigint NOT NULL,
  `actor_id` bigint NOT NULL,
  `before_state` varchar(1000) COLLATE utf8mb4_unicode_ci NOT NULL,
  `after_state` varchar(1000) COLLATE utf8mb4_unicode_ci NOT NULL,
  `reason` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `occurred_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_employment_account` (`account_id`,`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `enrollment_admissions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `enrollment_admissions` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `academic_status` varchar(40) COLLATE utf8mb4_unicode_ci NOT NULL,
  `accepted_at` datetime(6) NOT NULL,
  `actor_id` bigint NOT NULL,
  `actor_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `approval_id` bigint DEFAULT NULL,
  `assessment_id` bigint DEFAULT NULL,
  `assessment_snapshot` longtext COLLATE utf8mb4_unicode_ci NOT NULL,
  `assessment_version` bigint DEFAULT NULL,
  `class_id` bigint NOT NULL,
  `class_revision` bigint NOT NULL,
  `enrollment_id` bigint NOT NULL,
  `fingerprint` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `intent` enum('ENROLL','RESUME','TRANSFER') COLLATE utf8mb4_unicode_ci NOT NULL,
  `policy_id` bigint DEFAULT NULL,
  `policy_snapshot` longtext COLLATE utf8mb4_unicode_ci NOT NULL,
  `reused_decision_id` bigint DEFAULT NULL,
  `student_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `ix_enrollment_admission` (`enrollment_id`,`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `enrollment_periods`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `enrollment_periods` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `active_from` date NOT NULL,
  `active_until` date DEFAULT NULL,
  `enrollment_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_period_enrollment` (`enrollment_id`),
  CONSTRAINT `FKer49xt5e0tsqnjq2drx2rg1ph` FOREIGN KEY (`enrollment_id`) REFERENCES `enrollments` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `enrollments`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `enrollments` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `enrolled_at` datetime(6) NOT NULL,
  `study_status` enum('COMPLETED','DROPPED','RESERVED','STUDYING','TRANSFERRED') COLLATE utf8mb4_unicode_ci NOT NULL,
  `class_id` bigint NOT NULL,
  `student_id` bigint NOT NULL,
  `billing_enrollment_id` bigint DEFAULT NULL,
  `registered_by` bigint DEFAULT NULL,
  `registered_by_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `tuition_delivery` enum('CREATED','FAILED','PENDING') COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `tuition_delivery_error` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `tuition_fee` decimal(15,2) DEFAULT NULL,
  `version` bigint NOT NULL DEFAULT '0',
  `reserved_since` date DEFAULT NULL,
  `expected_return_date` date DEFAULT NULL,
  `completion_result_id` bigint DEFAULT NULL,
  `completion_result_revision` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_enrollment_class_status` (`class_id`,`study_status`),
  KEY `idx_enrollment_student_status` (`student_id`,`study_status`),
  CONSTRAINT `FK8kf1u1857xgo56xbfmnif2c51` FOREIGN KEY (`student_id`) REFERENCES `students` (`id`),
  CONSTRAINT `FKbyysfcms9vsjp968fx2jl9jvr` FOREIGN KEY (`class_id`) REFERENCES `school_classes` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `evaluation_policies`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `evaluation_policies` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `actor_id` bigint NOT NULL,
  `actor_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `branch_id` bigint NOT NULL,
  `class_id` bigint NOT NULL,
  `confirmed_at` datetime(6) NOT NULL,
  `course_id` bigint NOT NULL,
  `min_attendance_percent` decimal(5,2) DEFAULT NULL,
  `pass_percent` decimal(5,2) NOT NULL,
  `reason` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL,
  `revision` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_evaluation_policy_revision` (`class_id`,`revision`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `evaluation_policy_exams`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `evaluation_policy_exams` (
  `policy_id` bigint NOT NULL,
  `exam_date` date NOT NULL,
  `exam_id` bigint NOT NULL,
  `exam_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `exam_version` bigint NOT NULL,
  `max_score` decimal(7,2) NOT NULL,
  `required` bit(1) NOT NULL,
  `weight` decimal(7,2) NOT NULL,
  UNIQUE KEY `uk_evaluation_policy_exam` (`policy_id`,`exam_id`),
  CONSTRAINT `FKdbm9bcyxr6m7khl0fpi6l2ml6` FOREIGN KEY (`policy_id`) REFERENCES `evaluation_policies` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `evaluation_states`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `evaluation_states` (
  `class_id` bigint NOT NULL,
  `branch_id` bigint NOT NULL,
  `revision` bigint NOT NULL,
  `source_revision` bigint NOT NULL,
  PRIMARY KEY (`class_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `exam_candidates`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `exam_candidates` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `exam_id` bigint NOT NULL,
  `student_id` bigint NOT NULL,
  `student_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `source` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `enrollment_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_exam_candidate` (`exam_id`,`student_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `exams`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `exams` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `branch_id` bigint NOT NULL,
  `class_id` bigint NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `created_by` bigint DEFAULT NULL,
  `created_by_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `exam_date` date NOT NULL,
  `exam_type` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `max_score` decimal(7,2) NOT NULL,
  `name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `weight` decimal(7,2) NOT NULL,
  `roster_frozen_at` datetime(6) DEFAULT NULL,
  `roster_frozen_by` bigint DEFAULT NULL,
  `roster_frozen_by_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `version` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_exam_branch_class` (`branch_id`,`class_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `final_results`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `final_results` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `academic_source_revision` bigint NOT NULL,
  `branch_id` bigint DEFAULT NULL,
  `class_id` bigint DEFAULT NULL,
  `confirmed_at` datetime(6) DEFAULT NULL,
  `confirmed_by` bigint DEFAULT NULL,
  `confirmed_by_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `enrollment_id` bigint DEFAULT NULL,
  `fingerprint` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `outcome` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `policy_revision` bigint NOT NULL,
  `reason` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL,
  `reopen_reason` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `reopened_at` datetime(6) DEFAULT NULL,
  `reopened_by` bigint DEFAULT NULL,
  `reopened_by_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `request_id` varchar(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `revision` bigint NOT NULL,
  `snapshot` longtext COLLATE utf8mb4_unicode_ci NOT NULL,
  `state` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `student_id` bigint DEFAULT NULL,
  `teaching_source_revision` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKgp9m0djsiqy4sjy180mdlkt7g` (`request_id`),
  UNIQUE KEY `UKigquic5nddu1yvginnfstaqyj` (`enrollment_id`,`revision`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `grade_changes`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `grade_changes` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `grade_id` bigint DEFAULT NULL,
  `modified_at` datetime(6) DEFAULT NULL,
  `modified_by` bigint DEFAULT NULL,
  `modified_by_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `new_score` decimal(7,2) DEFAULT NULL,
  `old_score` decimal(7,2) DEFAULT NULL,
  `reason` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_grade_change_grade` (`grade_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `grade_publications`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `grade_publications` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `action` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `branch_id` bigint DEFAULT NULL,
  `class_id` bigint DEFAULT NULL,
  `draft_fingerprint` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `exam_date` date DEFAULT NULL,
  `exam_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `exam_type` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `fingerprint` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `max_score` decimal(7,2) DEFAULT NULL,
  `published_at` datetime(6) DEFAULT NULL,
  `published_by` bigint DEFAULT NULL,
  `reason` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL,
  `request_id` varchar(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `revision` bigint NOT NULL,
  `weight` decimal(7,2) DEFAULT NULL,
  `exam_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKef278hio931ai1yljjyn4y39u` (`exam_id`,`revision`),
  UNIQUE KEY `UK3kcndjklq9aqrj621tis27ks9` (`request_id`),
  CONSTRAINT `FK62cpvrdoverhvoo0x9e50l578` FOREIGN KEY (`exam_id`) REFERENCES `exams` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `grades`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `grades` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `branch_id` bigint NOT NULL,
  `comment` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `entered_at` datetime(6) NOT NULL,
  `entered_by` bigint NOT NULL,
  `entered_by_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `last_modified_at` datetime(6) DEFAULT NULL,
  `last_modified_by` bigint DEFAULT NULL,
  `last_modified_by_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `modification_reason` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `score` decimal(7,2) NOT NULL,
  `student_id` bigint NOT NULL,
  `student_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `version` bigint NOT NULL,
  `exam_id` bigint NOT NULL,
  `student_comment` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_grade_exam_student` (`exam_id`,`student_id`),
  CONSTRAINT `FK8mv6x74stqpbxncsci4gh2omn` FOREIGN KEY (`exam_id`) REFERENCES `exams` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `internal_support_replies`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `internal_support_replies` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `ticket_id` bigint NOT NULL,
  `actor_id` bigint NOT NULL,
  `message` varchar(2000) COLLATE utf8mb4_unicode_ci NOT NULL,
  `status_after` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `created_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_internal_replies` (`ticket_id`,`id`),
  KEY `IDX8eofj88mocvl5cc0dhanjol7d` (`ticket_id`,`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `internal_support_tickets`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `internal_support_tickets` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `version` bigint NOT NULL DEFAULT '0',
  `owner_id` bigint NOT NULL,
  `branch_id` bigint DEFAULT NULL,
  `category` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  `title` varchar(120) COLLATE utf8mb4_unicode_ci NOT NULL,
  `description` varchar(3000) COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_internal_owner_branch` (`owner_id`,`branch_id`),
  KEY `IDXrktbwdbregkge3xhro412cnom` (`owner_id`,`branch_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `job_titles`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `job_titles` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `version` bigint NOT NULL DEFAULT '0',
  `name` varchar(120) COLLATE utf8mb4_unicode_ci NOT NULL,
  `active` bit(1) NOT NULL DEFAULT b'1',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `lead_activities`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `lead_activities` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `lead_id` bigint NOT NULL,
  `action` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `note` varchar(2000) COLLATE utf8mb4_unicode_ci NOT NULL,
  `channel` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `old_status` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `new_status` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `old_assignee_id` bigint DEFAULT NULL,
  `old_assignee_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `assignee_id` bigint NOT NULL,
  `assignee_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `old_next_contact_at` datetime(6) DEFAULT NULL,
  `next_contact_at` datetime(6) DEFAULT NULL,
  `actor_id` bigint NOT NULL,
  `actor_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `occurred_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_lead_activity` (`lead_id`,`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `leads`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `leads` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `assigned_to` bigint NOT NULL,
  `assigned_to_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `branch_id` bigint NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `desired_level` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `email` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `full_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `last_contacted_at` datetime(6) DEFAULT NULL,
  `note` longtext COLLATE utf8mb4_unicode_ci,
  `phone` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `source` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `status` enum('CONSULTING','CONTACTED','CONVERTED','LOST','NEW') COLLATE utf8mb4_unicode_ci NOT NULL,
  `converted_student_id` bigint DEFAULT NULL,
  `interested_course_id` bigint DEFAULT NULL,
  `version` bigint NOT NULL DEFAULT '0',
  `next_contact_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKmnswa5bo24ajxyfwnnkxgdt88` (`converted_student_id`),
  KEY `FKi9bcrgl5vp7bsmsn4idx1o1qq` (`interested_course_id`),
  KEY `idx_lead_followup` (`branch_id`,`status`,`next_contact_at`),
  CONSTRAINT `FKi9bcrgl5vp7bsmsn4idx1o1qq` FOREIGN KEY (`interested_course_id`) REFERENCES `courses` (`id`),
  CONSTRAINT `FKmrhkinpqhxk4wmhkqcg9pxt5b` FOREIGN KEY (`converted_student_id`) REFERENCES `students` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `learning_events`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `learning_events` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `branch_id` bigint NOT NULL,
  `student_id` bigint NOT NULL,
  `enrollment_id` bigint DEFAULT NULL,
  `related_enrollment_id` bigint DEFAULT NULL,
  `action` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `old_status` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `new_status` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `reason` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL,
  `effective_date` date NOT NULL,
  `expected_return_date` date DEFAULT NULL,
  `actor_id` bigint NOT NULL,
  `actor_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `occurred_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_learning_student` (`student_id`,`id`),
  KEY `idx_learning_enrollment` (`enrollment_id`,`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `organization_assignments`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `organization_assignments` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `version` bigint NOT NULL DEFAULT '0',
  `account_id` bigint NOT NULL,
  `unit_id` bigint NOT NULL,
  `title_id` bigint NOT NULL,
  `manager_account_id` bigint DEFAULT NULL,
  `active` bit(1) NOT NULL DEFAULT b'1',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_org_assignment` (`account_id`,`unit_id`,`title_id`),
  UNIQUE KEY `UK31amyjcx9uo1ccif93y1ittm8` (`account_id`,`unit_id`,`title_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `organization_units`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `organization_units` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `version` bigint NOT NULL DEFAULT '0',
  `name` varchar(120) COLLATE utf8mb4_unicode_ci NOT NULL,
  `parent_id` bigint DEFAULT NULL,
  `branch_id` bigint DEFAULT NULL,
  `active` bit(1) NOT NULL DEFAULT b'1',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `payment_corrections`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `payment_corrections` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `payment_id` bigint NOT NULL,
  `tuition_id` bigint NOT NULL,
  `branch_id` bigint NOT NULL,
  `type` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `amount` decimal(15,2) NOT NULL,
  `payment_method` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `reference_code` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `reason` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL,
  `request_key` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `expected_version` bigint NOT NULL,
  `recorded_by` bigint NOT NULL,
  `recorded_by_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `created_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_correction_request` (`branch_id`,`request_key`),
  KEY `idx_correction_payment` (`payment_id`,`id`),
  KEY `idx_correction_tuition` (`tuition_id`,`id`),
  KEY `idx_correction_branch_date` (`branch_id`,`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `payment_orders`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `payment_orders` (
  `id` varchar(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `account_id` bigint NOT NULL,
  `amount` decimal(15,2) NOT NULL,
  `billing_root_id` bigint NOT NULL,
  `branch_id` bigint NOT NULL,
  `client_ip` varchar(45) COLLATE utf8mb4_unicode_ci NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `currency` varchar(3) COLLATE utf8mb4_unicode_ci NOT NULL,
  `environment` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `expires_at` datetime(6) NOT NULL,
  `fingerprint` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `merchant_code` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL,
  `merchant_ref` varchar(40) COLLATE utf8mb4_unicode_ci NOT NULL,
  `provider` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `receipt_id` bigint DEFAULT NULL,
  `request_id` varchar(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `review_code` varchar(60) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `state` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `student_id` bigint NOT NULL,
  `tuition_id` bigint NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `version` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKjkg906r6ljklysq6em0wf6asb` (`account_id`,`request_id`),
  UNIQUE KEY `UKaubp5djw4kl9ka2tb75e9xfk1` (`merchant_ref`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `payment_outbox`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `payment_outbox` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `delivered_at` datetime(6) DEFAULT NULL,
  `event_key` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `order_id` varchar(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `receipt_id` bigint DEFAULT NULL,
  `student_id` bigint NOT NULL,
  `type` varchar(40) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKgadni09oi9sjwnlcwn2xb4eeo` (`event_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `payment_provider_events`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `payment_provider_events` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `amount` decimal(15,2) NOT NULL,
  `branch_id` bigint DEFAULT NULL,
  `event_key` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `merchant_code` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL,
  `merchant_ref` varchar(40) COLLATE utf8mb4_unicode_ci NOT NULL,
  `order_id` varchar(36) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `outcome` varchar(12) COLLATE utf8mb4_unicode_ci NOT NULL,
  `paid_at` datetime(6) DEFAULT NULL,
  `processed_at` datetime(6) DEFAULT NULL,
  `received_at` datetime(6) NOT NULL,
  `result_code` varchar(60) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `source` varchar(12) COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `transaction_no` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `version` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK50s8jqwl82g5b2o6fdpaij370` (`event_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `payment_provider_transactions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `payment_provider_transactions` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `amount` decimal(15,2) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `merchant_code` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL,
  `order_id` varchar(36) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `paid_at` datetime(6) DEFAULT NULL,
  `receipt_id` bigint DEFAULT NULL,
  `review_code` varchar(60) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `state` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `transaction_no` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL,
  `version` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKmkssbsl7vjnxvhx346e9smbcj` (`merchant_code`,`transaction_no`),
  UNIQUE KEY `UKdmwmc02fjh6r99o9310aknfco` (`receipt_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `payment_reconcile_operations`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `payment_reconcile_operations` (
  `id` varchar(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `actor_id` bigint NOT NULL,
  `branch_id` bigint NOT NULL,
  `completed_at` datetime(6) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `event_id` bigint DEFAULT NULL,
  `fingerprint` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `order_id` varchar(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `reason` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `payments`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `payments` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `amount` decimal(15,2) NOT NULL,
  `branch_id` bigint NOT NULL,
  `note` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `paid_at` datetime(6) NOT NULL,
  `payment_method` enum('BANK_TRANSFER','CARD','CASH','WALLET') COLLATE utf8mb4_unicode_ci NOT NULL,
  `receipt_no` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `recorded_by` bigint DEFAULT NULL,
  `recorded_by_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `reference_code` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `request_key` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `status` enum('FAILED','PAID','PARTIAL','PENDING','REFUNDED') COLLATE utf8mb4_unicode_ci NOT NULL,
  `tuition_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payment_receipt` (`receipt_no`),
  UNIQUE KEY `uk_payment_request` (`branch_id`,`request_key`),
  KEY `idx_payment_tuition` (`tuition_id`),
  KEY `idx_payment_branch_date` (`branch_id`,`paid_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `personnel_code_numbers`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `personnel_code_numbers` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `placement_assessments`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `placement_assessments` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `assessed_on` date NOT NULL,
  `assessor_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `branch_id` bigint NOT NULL,
  `confirmed_at` datetime(6) DEFAULT NULL,
  `confirmed_by` bigint DEFAULT NULL,
  `confirmed_by_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `current_key` varchar(90) COLLATE utf8mb4_unicode_ci GENERATED ALWAYS AS ((case when (`status` = _utf8mb4'CONFIRMED') then concat(`student_id`,_utf8mb4':',`program_id`) else NULL end)) VIRTUAL,
  `level_code` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `level_id` bigint NOT NULL,
  `level_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `max_score` decimal(12,4) DEFAULT NULL,
  `method` enum('CERTIFICATE','INTERVIEW','OTHER','TEST') COLLATE utf8mb4_unicode_ci NOT NULL,
  `program_code` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `program_id` bigint NOT NULL,
  `program_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `recorded_by` bigint NOT NULL,
  `recorded_by_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `score` decimal(12,4) DEFAULT NULL,
  `source` varchar(2000) COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` enum('CONFIRMED','DRAFT','REVOKED','SUPERSEDED') COLLATE utf8mb4_unicode_ci NOT NULL,
  `superseded_by_id` bigint DEFAULT NULL,
  `supersedes_id` bigint DEFAULT NULL,
  `updated_at` datetime(6) NOT NULL,
  `version` bigint NOT NULL,
  `student_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_assessment_current` (`current_key`),
  KEY `idx_assessment_student_program` (`student_id`,`program_id`,`status`),
  CONSTRAINT `FKofh7it4ukquhdaverxyoktyye` FOREIGN KEY (`student_id`) REFERENCES `students` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `placement_exceptions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `placement_exceptions` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `academic_status` varchar(40) COLLATE utf8mb4_unicode_ci NOT NULL,
  `assessment_id` bigint DEFAULT NULL,
  `assessment_version` bigint DEFAULT NULL,
  `branch_id` bigint NOT NULL,
  `class_id` bigint NOT NULL,
  `class_revision` bigint NOT NULL,
  `consumed_at` datetime(6) DEFAULT NULL,
  `consumed_enrollment_id` bigint DEFAULT NULL,
  `context_snapshot` longtext COLLATE utf8mb4_unicode_ci NOT NULL,
  `decided_at` datetime(6) DEFAULT NULL,
  `decided_by` bigint DEFAULT NULL,
  `decided_by_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `decision_reason` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `fingerprint` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `intent` enum('ENROLL','RESUME','TRANSFER') COLLATE utf8mb4_unicode_ci NOT NULL,
  `policy_id` bigint DEFAULT NULL,
  `requested_at` datetime(6) NOT NULL,
  `requested_by` bigint NOT NULL,
  `requested_by_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `requested_reason` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL,
  `source_enrollment_id` bigint DEFAULT NULL,
  `source_version` bigint DEFAULT NULL,
  `status` enum('APPROVED','CONSUMED','INVALIDATED','REJECTED','REQUESTED') COLLATE utf8mb4_unicode_ci NOT NULL,
  `student_id` bigint NOT NULL,
  `version` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `ix_placement_exception_branch` (`branch_id`,`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `program_levels`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `program_levels` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `active` bit(1) NOT NULL,
  `code` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `sort_order` int NOT NULL,
  `version` bigint NOT NULL,
  `program_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_program_level_code` (`program_id`,`code`),
  CONSTRAINT `FK8agktfjamx7lee1r907wcw26u` FOREIGN KEY (`program_id`) REFERENCES `programs` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `programs`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `programs` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `active` bit(1) NOT NULL,
  `code` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `version` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_program_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `published_grades`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `published_grades` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `enrollment_id` bigint NOT NULL,
  `score` decimal(7,2) DEFAULT NULL,
  `student_comment` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `student_id` bigint NOT NULL,
  `publication_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK2kney3wurkrasflk6kg2kyafa` (`publication_id`,`student_id`,`enrollment_id`),
  CONSTRAINT `FKoyl22cw0fwpcwa68a02rj35u8` FOREIGN KEY (`publication_id`) REFERENCES `grade_publications` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `result_reopens`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `result_reopens` (
  `request_id` varchar(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `actor_id` bigint DEFAULT NULL,
  `actor_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `class_id` bigint DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `previous_revision` bigint NOT NULL,
  `reason` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`request_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `rooms`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `rooms` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `branch_id` bigint NOT NULL,
  `capacity` int NOT NULL,
  `code` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `equipment` text COLLATE utf8mb4_unicode_ci,
  `location` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` enum('AVAILABLE','MAINTENANCE') COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_room_code_branch` (`code`,`branch_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `schedules`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `schedules` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `day_of_week` enum('FRIDAY','MONDAY','SATURDAY','SUNDAY','THURSDAY','TUESDAY','WEDNESDAY') COLLATE utf8mb4_unicode_ci NOT NULL,
  `end_time` time NOT NULL,
  `start_time` time NOT NULL,
  `class_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_schedule_class_day` (`class_id`,`day_of_week`),
  CONSTRAINT `FKtba44f4h9r5v0ldweopn33hxq` FOREIGN KEY (`class_id`) REFERENCES `school_classes` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `school_classes`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `school_classes` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `branch_id` bigint NOT NULL,
  `code` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `current_size` int NOT NULL,
  `end_date` date NOT NULL,
  `max_size` int NOT NULL,
  `name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `start_date` date NOT NULL,
  `status` enum('FINISHED','FULL','ONGOING','UPCOMING') COLLATE utf8mb4_unicode_ci NOT NULL,
  `status_manually_set` bit(1) NOT NULL,
  `teacher_id` bigint NOT NULL,
  `teacher_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `course_id` bigint NOT NULL,
  `room_id` bigint NOT NULL,
  `session_end_date` date DEFAULT NULL,
  `sessions_initialized` bit(1) NOT NULL DEFAULT b'0',
  `admission_policy_id` bigint DEFAULT NULL,
  `admission_revision` bigint NOT NULL,
  `duration_revision` bigint NOT NULL,
  `expected_minutes` int DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_class_code_branch` (`code`,`branch_id`),
  KEY `FKaar2qgjj7oj1q40nr741qffc4` (`course_id`),
  KEY `FK2iektjpygodribrx6k6t4rdpv` (`room_id`),
  CONSTRAINT `FK2iektjpygodribrx6k6t4rdpv` FOREIGN KEY (`room_id`) REFERENCES `rooms` (`id`),
  CONSTRAINT `FKaar2qgjj7oj1q40nr741qffc4` FOREIGN KEY (`course_id`) REFERENCES `courses` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `session_events`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `session_events` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `session_id` bigint NOT NULL,
  `action` varchar(40) COLLATE utf8mb4_unicode_ci NOT NULL,
  `reason` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `before_state` text COLLATE utf8mb4_unicode_ci,
  `after_state` text COLLATE utf8mb4_unicode_ci,
  `actor_id` bigint NOT NULL,
  `actor_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `occurred_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_session_event` (`session_id`,`id`),
  CONSTRAINT `fk_session_event_session` FOREIGN KEY (`session_id`) REFERENCES `class_sessions` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `staffs`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `staffs` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `email` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `full_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `phone` varchar(15) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `account_id` bigint NOT NULL,
  `hire_date` date DEFAULT NULL,
  `position` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `work_status` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `version` bigint NOT NULL DEFAULT '0',
  `end_date` date DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKk0fv4fbde24uqula08y9vril2` (`account_id`),
  CONSTRAINT `FK76xya5sjr57jknmc1fq46qc4x` FOREIGN KEY (`account_id`) REFERENCES `accounts` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `student_account_provisionings`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `student_account_provisionings` (
  `request_id` varchar(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `account_id` bigint NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `created_by` bigint NOT NULL,
  `expected_student_version` bigint NOT NULL,
  `initial_password_hash` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `operation_count` bigint NOT NULL,
  `student_id` bigint NOT NULL,
  `username` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `version` bigint NOT NULL,
  PRIMARY KEY (`request_id`),
  UNIQUE KEY `uk_student_provision_student` (`student_id`),
  UNIQUE KEY `uk_student_provision_account` (`account_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `student_code_numbers`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `student_code_numbers` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `student_notification_outbox`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `student_notification_outbox` (
  `id` varchar(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `body` varchar(300) COLLATE utf8mb4_unicode_ci NOT NULL,
  `branch_id` bigint DEFAULT NULL,
  `category` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `class_id` bigint DEFAULT NULL,
  `course_id` bigint DEFAULT NULL,
  `delivered_at` datetime(6) DEFAULT NULL,
  `enrollment_id` bigint DEFAULT NULL,
  `occurred_at` datetime(6) NOT NULL,
  `previous_date` date DEFAULT NULL,
  `recipient_count` int NOT NULL,
  `resource_id` varchar(80) COLLATE utf8mb4_unicode_ci NOT NULL,
  `resource_type` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `session_date` date DEFAULT NULL,
  `source_key` varchar(160) COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` varchar(12) COLLATE utf8mb4_unicode_ci NOT NULL,
  `student_id` bigint DEFAULT NULL,
  `title` varchar(120) COLLATE utf8mb4_unicode_ci NOT NULL,
  `type` varchar(40) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKexvwrpgfhyd2bk6h740hg2qc0` (`source_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `student_notification_preferences`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `student_notification_preferences` (
  `account_id` bigint NOT NULL,
  `documents` bit(1) NOT NULL,
  `grades` bit(1) NOT NULL,
  `payment` bit(1) NOT NULL,
  `push_enabled` bit(1) NOT NULL,
  `schedule` bit(1) NOT NULL,
  `support` bit(1) NOT NULL,
  `tuition` bit(1) NOT NULL,
  `version` bigint NOT NULL,
  PRIMARY KEY (`account_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `student_notifications`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `student_notifications` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `account_id` bigint NOT NULL,
  `body` varchar(300) COLLATE utf8mb4_unicode_ci NOT NULL,
  `category` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `event_id` varchar(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `read_at` datetime(6) DEFAULT NULL,
  `resource_id` varchar(80) COLLATE utf8mb4_unicode_ci NOT NULL,
  `resource_type` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `student_id` bigint NOT NULL,
  `title` varchar(120) COLLATE utf8mb4_unicode_ci NOT NULL,
  `type` varchar(40) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKs3hefqhvgk0rlppvgpp38mc58` (`event_id`,`account_id`,`student_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `student_push_deliveries`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `student_push_deliveries` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `attempted_at` datetime(6) DEFAULT NULL,
  `binding_version` bigint NOT NULL,
  `installation_id` varchar(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `notification_id` bigint NOT NULL,
  `receipt_checked_at` datetime(6) DEFAULT NULL,
  `result_code` varchar(60) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `ticket_id` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK4p2wl7qh8hojmg5xkmyd8gvxq` (`notification_id`,`installation_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `student_push_devices`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `student_push_devices` (
  `installation_id` varchar(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `account_id` bigint NOT NULL,
  `binding_version` bigint NOT NULL,
  `enabled` bit(1) NOT NULL,
  `platform` varchar(10) COLLATE utf8mb4_unicode_ci NOT NULL,
  `project_id` varchar(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `push_token` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `session_version` bigint NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  PRIMARY KEY (`installation_id`),
  UNIQUE KEY `UKhhm9388we9gur18uv73wfdvcc` (`push_token`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `student_support_messages`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `student_support_messages` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `actor_id` bigint DEFAULT NULL,
  `actor_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `internal_note` bit(1) NOT NULL,
  `message` varchar(2000) COLLATE utf8mb4_unicode_ci NOT NULL,
  `sender` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `request_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_support_message` (`request_id`,`id`),
  CONSTRAINT `FKqbhryd7mbcnx86e3teluo0n8q` FOREIGN KEY (`request_id`) REFERENCES `student_support_requests` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `student_support_operations`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `student_support_operations` (
  `id` varchar(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `action` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `actor_id` bigint NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `fingerprint` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `request_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FK6f8i5f2wxa5y3i7j3od6uogh1` (`request_id`),
  CONSTRAINT `FK6f8i5f2wxa5y3i7j3od6uogh1` FOREIGN KEY (`request_id`) REFERENCES `student_support_requests` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `student_support_requests`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `student_support_requests` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `branch_id` bigint NOT NULL,
  `category` enum('ATTENDANCE','GRADE','LEAVE','MAKEUP','OTHER','PAYMENT','PROFILE') COLLATE utf8mb4_unicode_ci NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `resource_id` bigint DEFAULT NULL,
  `resource_type` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `status` enum('CLOSED','IN_PROGRESS','OPEN','RESOLVED') COLLATE utf8mb4_unicode_ci NOT NULL,
  `student_id` bigint NOT NULL,
  `subject` varchar(120) COLLATE utf8mb4_unicode_ci NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `version` bigint NOT NULL,
  `resource_key` varchar(36) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_support_owner` (`student_id`,`branch_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `student_support_status_events`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `student_support_status_events` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `actor_id` bigint NOT NULL,
  `actor_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `previous_status` enum('CLOSED','IN_PROGRESS','OPEN','RESOLVED') COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `reason` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL,
  `request_id` bigint NOT NULL,
  `status` enum('CLOSED','IN_PROGRESS','OPEN','RESOLVED') COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_support_status_request` (`request_id`,`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `students`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `students` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `address` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `branch_id` bigint NOT NULL,
  `contact_person` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `current_level` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `date_of_birth` date DEFAULT NULL,
  `email` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `full_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `gender` enum('FEMALE','MALE','OTHER') COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `phone` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `registered_at` date NOT NULL,
  `status` enum('GRADUATED','PAUSED','STUDYING') COLLATE utf8mb4_unicode_ci NOT NULL,
  `guardian_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `guardian_phone` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `student_code` varchar(40) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `version` bigint NOT NULL DEFAULT '0',
  `account_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKcgcf3r5xk73o0etbduc1qxnol` (`student_code`),
  UNIQUE KEY `UKd8r092m3ykhh2vyn2jgeafsr3` (`account_id`),
  KEY `idx_student_phone_branch` (`phone`,`branch_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `super_admins`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `super_admins` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `email` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `full_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `phone` varchar(15) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `account_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKp4f72x4wtciiblbq6vfs0xq3o` (`account_id`),
  CONSTRAINT `FKp6orxeyhfsi614sh6sgb0q9o5` FOREIGN KEY (`account_id`) REFERENCES `accounts` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `teacher_profile_events`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `teacher_profile_events` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `action` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `actor_id` bigint NOT NULL,
  `actor_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `after_state` longtext COLLATE utf8mb4_unicode_ci NOT NULL,
  `before_state` longtext COLLATE utf8mb4_unicode_ci NOT NULL,
  `occurred_at` datetime(6) NOT NULL,
  `reason` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `teacher_id` bigint NOT NULL,
  `teacher_version` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_teacher_event` (`teacher_id`,`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `teachers`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `teachers` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `certificate` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `email` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `experience` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `full_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `hire_date` date DEFAULT NULL,
  `phone` varchar(15) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `qualification` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `specialization` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `work_status` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `account_id` bigint NOT NULL,
  `version` bigint NOT NULL,
  `end_date` date DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKfwk5qk8ju90rx58169r9dj8td` (`account_id`),
  CONSTRAINT `FKnywk18vp2rdv32e94ofevfphl` FOREIGN KEY (`account_id`) REFERENCES `accounts` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `tuition`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tuition` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `adjusted_at` datetime(6) DEFAULT NULL,
  `adjusted_by` bigint DEFAULT NULL,
  `adjusted_by_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `adjustment_reason` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `amount` decimal(15,2) NOT NULL,
  `branch_id` bigint NOT NULL,
  `class_id` bigint NOT NULL,
  `class_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `course_id` bigint DEFAULT NULL,
  `course_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `currency` varchar(10) COLLATE utf8mb4_unicode_ci NOT NULL,
  `discount_amount` decimal(15,2) DEFAULT NULL,
  `due_date` date NOT NULL,
  `enrollment_id` bigint DEFAULT NULL,
  `original_amount` decimal(15,2) DEFAULT NULL,
  `paid_amount` decimal(15,2) DEFAULT NULL,
  `status` enum('CANCELLED','OVERDUE','PAID','PARTIAL','PENDING','UNPAID') COLLATE utf8mb4_unicode_ci NOT NULL,
  `student_id` bigint NOT NULL,
  `student_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `version` bigint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tuition_enrollment` (`enrollment_id`),
  KEY `idx_tuition_branch_status` (`branch_id`,`status`),
  KEY `idx_tuition_branch_due` (`branch_id`,`due_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `tuition_adjustments`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tuition_adjustments` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tuition_id` bigint NOT NULL,
  `branch_id` bigint NOT NULL,
  `old_original` decimal(15,2) NOT NULL,
  `old_discount` decimal(15,2) NOT NULL,
  `old_amount` decimal(15,2) NOT NULL,
  `old_due_date` date NOT NULL,
  `new_original` decimal(15,2) NOT NULL,
  `new_discount` decimal(15,2) NOT NULL,
  `new_amount` decimal(15,2) NOT NULL,
  `new_due_date` date NOT NULL,
  `paid_at_change` decimal(15,2) NOT NULL,
  `reason` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL,
  `request_key` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `expected_version` bigint NOT NULL,
  `recorded_by` bigint NOT NULL,
  `recorded_by_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `created_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_adjustment_request` (`branch_id`,`request_key`),
  KEY `idx_adjustment_tuition` (`tuition_id`,`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `user_preferences`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_preferences` (
  `account_id` bigint NOT NULL,
  `version` bigint NOT NULL DEFAULT '0',
  `compact_tables` bit(1) NOT NULL DEFAULT b'0',
  `page_size` int NOT NULL DEFAULT '10',
  `calendar_view` varchar(10) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'week',
  `default_branch_id` bigint DEFAULT NULL,
  PRIMARY KEY (`account_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `website_edit_events`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `website_edit_events` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `actor_id` bigint NOT NULL,
  `actor_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `kind` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `occurred_at` datetime(6) NOT NULL,
  `snapshot` varchar(16000) COLLATE utf8mb4_unicode_ci NOT NULL,
  `source_id` bigint NOT NULL,
  `state` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  KEY `IDXeuysgt6ro2oa1mp9s1m4rsc56` (`kind`,`source_id`,`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `website_inquiries`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `website_inquiries` (
  `request_id` varchar(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `branch_id` bigint NOT NULL,
  `class_slug` varchar(120) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `consent_version` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `course_slug` varchar(120) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `fingerprint` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `lead_id` bigint NOT NULL,
  `phone_hash` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `program_slug` varchar(120) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`request_id`),
  KEY `idx_website_inquiry_phone` (`phone_hash`,`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `website_publications`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `website_publications` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `audience` varchar(2000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `image_url` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `kind` enum('BRANCH','CLASS','COURSE','PROGRAM') COLLATE utf8mb4_unicode_ci NOT NULL,
  `map_url` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `opening_hours` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `outcomes` varchar(4000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `program_id` bigint DEFAULT NULL,
  `show_tuition` bit(1) NOT NULL,
  `slug` varchar(120) COLLATE utf8mb4_unicode_ci NOT NULL,
  `source_id` bigint NOT NULL,
  `state` enum('DRAFT','PUBLISHED') COLLATE utf8mb4_unicode_ci NOT NULL,
  `summary` varchar(2000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `topics` varchar(4000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `updated_at` datetime(6) NOT NULL,
  `updated_by` bigint NOT NULL,
  `version` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_website_source` (`kind`,`source_id`),
  UNIQUE KEY `uk_website_slug` (`kind`,`slug`),
  KEY `idx_website_public` (`kind`,`state`,`source_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `website_teacher_profiles`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `website_teacher_profiles` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `biography` varchar(2000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `image_url` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `show_qualification` bit(1) NOT NULL,
  `slug` varchar(120) COLLATE utf8mb4_unicode_ci NOT NULL,
  `state` enum('DRAFT','PUBLISHED') COLLATE utf8mb4_unicode_ci NOT NULL,
  `teacher_id` bigint NOT NULL,
  `teaching_focus` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `updated_at` datetime(6) NOT NULL,
  `updated_by` bigint NOT NULL,
  `version` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_website_teacher_source` (`teacher_id`),
  UNIQUE KEY `uk_website_teacher_slug` (`slug`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

