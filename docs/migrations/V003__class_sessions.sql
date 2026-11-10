-- MySQL 8, after V001/V002. Back up academic tables before running.
-- Historic attendance is kept intact. Unknown historic times/rooms/teachers stay NULL.
SET @lc_stage3_sql=(SELECT IF(COUNT(*)=0,'ALTER TABLE school_classes ADD COLUMN session_end_date DATE NULL','SELECT 1') FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='school_classes' AND column_name='session_end_date');
PREPARE lc_stage3_stmt FROM @lc_stage3_sql; EXECUTE lc_stage3_stmt; DEALLOCATE PREPARE lc_stage3_stmt;
SET @lc_stage3_sql=(SELECT IF(COUNT(*)=0,'ALTER TABLE school_classes ADD COLUMN sessions_initialized BIT NOT NULL DEFAULT 0','SELECT 1') FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='school_classes' AND column_name='sessions_initialized');
PREPARE lc_stage3_stmt FROM @lc_stage3_sql; EXECUTE lc_stage3_stmt; DEALLOCATE PREPARE lc_stage3_stmt;
CREATE TABLE IF NOT EXISTS class_sessions (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 class_id BIGINT NOT NULL,
 session_date DATE NOT NULL,
 start_time TIME(6) NULL,
 end_time TIME(6) NULL,
 room_id BIGINT NULL,
 room_name VARCHAR(255) NULL,
 planned_teacher_id BIGINT NULL,
 planned_teacher_name VARCHAR(255) NULL,
 teacher_id BIGINT NULL,
 teacher_name VARCHAR(255) NULL,
 status VARCHAR(20) NOT NULL,
 kind VARCHAR(20) NOT NULL,
 source_date DATE NULL,
 source_schedule_id BIGINT NULL,
 makeup_for_id BIGINT NULL,
 makeup_session_id BIGINT NULL,
 customized BIT NOT NULL DEFAULT 0,
 reason VARCHAR(500) NULL,
 version BIGINT NOT NULL DEFAULT 0,
 created_at DATETIME(6) NOT NULL,
 updated_at DATETIME(6) NOT NULL,
 CONSTRAINT fk_session_class FOREIGN KEY(class_id) REFERENCES school_classes(id),
 CONSTRAINT uk_session_source UNIQUE(class_id,source_date),
 CONSTRAINT uk_session_makeup UNIQUE(makeup_for_id),
 INDEX idx_session_date_teacher(session_date,teacher_id,status),
 INDEX idx_session_date_room(session_date,room_id,status)
);
CREATE TABLE IF NOT EXISTS session_events (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 session_id BIGINT NOT NULL,
 action VARCHAR(40) NOT NULL,
 reason VARCHAR(500) NULL,
 before_state TEXT NULL,
 after_state TEXT NULL,
 actor_id BIGINT NOT NULL,
 actor_name VARCHAR(255) NOT NULL,
 occurred_at DATETIME(6) NOT NULL,
 CONSTRAINT fk_session_event_session FOREIGN KEY(session_id) REFERENCES class_sessions(id),
 INDEX idx_session_event(session_id,id)
);
CREATE TABLE IF NOT EXISTS attendance_changes (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 session_id BIGINT NOT NULL,
 enrollment_id BIGINT NOT NULL,
 student_name VARCHAR(255) NOT NULL,
 old_status VARCHAR(255) NULL,
 new_status VARCHAR(255) NOT NULL,
 old_permission BIT NULL,
 new_permission BIT NOT NULL,
 previous_recorded_by BIGINT NULL,
 previous_recorded_by_name VARCHAR(255) NULL,
 changed_by BIGINT NOT NULL,
 changed_by_name VARCHAR(255) NOT NULL,
 changed_at DATETIME(6) NOT NULL,
 reason VARCHAR(500) NOT NULL,
 CONSTRAINT fk_attendance_change_session FOREIGN KEY(session_id) REFERENCES class_sessions(id),
 INDEX idx_attendance_change_session(session_id,id)
);
SET @lc_stage3_sql=(SELECT IF(COUNT(*)=0,'ALTER TABLE attendances ADD COLUMN class_session_id BIGINT NULL','SELECT 1') FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='attendances' AND column_name='class_session_id');
PREPARE lc_stage3_stmt FROM @lc_stage3_sql; EXECUTE lc_stage3_stmt; DEALLOCATE PREPARE lc_stage3_stmt;
SET @lc_stage3_now=CONVERT_TZ(UTC_TIMESTAMP(),'+00:00','+07:00');
START TRANSACTION;
INSERT INTO class_sessions(class_id,session_date,planned_teacher_id,planned_teacher_name,status,kind,source_date,customized,reason,version,created_at,updated_at)
SELECT h.class_id,h.session_date,c.teacher_id,c.teacher_name,'COMPLETED','LEGACY',h.session_date,1,
 'Chuyển điểm danh lịch sử; chưa xác minh giờ/phòng/giáo viên thực tế',0,@lc_stage3_now,@lc_stage3_now
FROM (SELECT DISTINCT e.class_id,a.session_date FROM attendances a JOIN enrollments e ON e.id=a.enrollment_id WHERE a.class_session_id IS NULL) h
JOIN school_classes c ON c.id=h.class_id
WHERE NOT EXISTS(SELECT 1 FROM class_sessions s WHERE s.class_id=h.class_id AND s.source_date=h.session_date);
-- A partially started new application may already have generated the legacy source date.
UPDATE class_sessions s JOIN (SELECT DISTINCT e.class_id,a.session_date FROM attendances a JOIN enrollments e ON e.id=a.enrollment_id WHERE a.class_session_id IS NULL) h
 ON s.class_id=h.class_id AND s.source_date=h.session_date AND s.session_date=h.session_date
SET s.status='COMPLETED',s.kind='LEGACY',s.start_time=NULL,s.end_time=NULL,s.room_id=NULL,s.room_name=NULL,s.teacher_id=NULL,s.teacher_name=NULL,s.customized=1,
 s.reason='Chuyển điểm danh lịch sử; chưa xác minh giờ/phòng/giáo viên thực tế'
WHERE s.makeup_for_id IS NULL;
UPDATE attendances a JOIN enrollments e ON e.id=a.enrollment_id
JOIN class_sessions s ON s.class_id=e.class_id AND s.session_date=a.session_date AND s.status='COMPLETED'
SET a.class_session_id=s.id WHERE a.class_session_id IS NULL;

-- At most one weekly template per weekday; preflight rejects duplicates and ranges >3660 days.
INSERT INTO class_sessions(class_id,session_date,start_time,end_time,room_id,room_name,planned_teacher_id,planned_teacher_name,teacher_id,teacher_name,status,kind,source_date,source_schedule_id,customized,reason,version,created_at,updated_at)
WITH digits AS (SELECT 0 AS n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9),
numbers AS (SELECT a.n+10*b.n+100*c.n+1000*d.n AS n FROM digits a CROSS JOIN digits b CROSS JOIN digits c CROSS JOIN digits d)
SELECT c.id,DATE_ADD(c.start_date,INTERVAL n.n DAY),s.start_time,s.end_time,r.id,r.name,c.teacher_id,c.teacher_name,c.teacher_id,c.teacher_name,
 'SCHEDULED','REGULAR',DATE_ADD(c.start_date,INTERVAL n.n DAY),s.id,0,'Sinh từ lịch tuần khi chuyển đổi',0,@lc_stage3_now,@lc_stage3_now
FROM school_classes c JOIN schedules s ON s.class_id=c.id JOIN rooms r ON r.id=c.room_id CROSS JOIN numbers n
WHERE c.sessions_initialized=0 AND c.status<>'FINISHED' AND n.n<=3660 AND n.n<=DATEDIFF(c.end_date,c.start_date)
 AND ELT(WEEKDAY(DATE_ADD(c.start_date,INTERVAL n.n DAY))+1,'MONDAY','TUESDAY','WEDNESDAY','THURSDAY','FRIDAY','SATURDAY','SUNDAY')=s.day_of_week
 AND NOT EXISTS(SELECT 1 FROM class_sessions existing WHERE existing.class_id=c.id AND existing.source_date=DATE_ADD(c.start_date,INTERVAL n.n DAY));
INSERT INTO session_events(session_id,action,reason,after_state,actor_id,actor_name,occurred_at)
SELECT s.id,'MIGRATED',s.reason,CONCAT('date=',s.session_date,'; status=',s.status,'; kind=',s.kind),0,'MIGRATION',@lc_stage3_now
FROM class_sessions s WHERE NOT EXISTS(SELECT 1 FROM session_events e WHERE e.session_id=s.id);
UPDATE school_classes c LEFT JOIN (SELECT class_id,MAX(session_date) AS latest FROM class_sessions WHERE status<>'CANCELLED' GROUP BY class_id) dates ON dates.class_id=c.id SET c.session_end_date=dates.latest;
UPDATE school_classes SET sessions_initialized=1 WHERE sessions_initialized=0;
COMMIT;
SET @lc_stage3_sql=(SELECT IF(COUNT(*)=0,'ALTER TABLE attendances ADD CONSTRAINT fk_attendance_session FOREIGN KEY(class_session_id) REFERENCES class_sessions(id)','SELECT 1') FROM information_schema.key_column_usage WHERE table_schema=DATABASE() AND table_name='attendances' AND column_name='class_session_id' AND referenced_table_name='class_sessions');
PREPARE lc_stage3_stmt FROM @lc_stage3_sql; EXECUTE lc_stage3_stmt; DEALLOCATE PREPARE lc_stage3_stmt;
