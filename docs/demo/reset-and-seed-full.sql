USE language_center_db;
SET FOREIGN_KEY_CHECKS=0;
START TRANSACTION;
DELETE FROM `assessment_events`;
DELETE FROM `attendance_changes`;
DELETE FROM `attendances`;
DELETE FROM `class_admission_events`;
DELETE FROM `class_closure_events`;
DELETE FROM `class_closures`;
DELETE FROM `class_duration_plans`;
DELETE FROM `class_result_barriers`;
DELETE FROM `class_sessions`;
DELETE FROM `course_admission_levels`;
DELETE FROM `course_admission_policies`;
DELETE FROM `course_duration_plans`;
DELETE FROM `courses`;
DELETE FROM `document_events`;
DELETE FROM `documents`;
DELETE FROM `enrollment_admissions`;
DELETE FROM `enrollment_periods`;
DELETE FROM `enrollments`;
DELETE FROM `evaluation_policies`;
DELETE FROM `evaluation_policy_exams`;
DELETE FROM `evaluation_states`;
DELETE FROM `exam_candidates`;
DELETE FROM `exams`;
DELETE FROM `final_results`;
DELETE FROM `grade_changes`;
DELETE FROM `grades`;
DELETE FROM `lead_activities`;
DELETE FROM `leads`;
DELETE FROM `learning_events`;
DELETE FROM `payment_corrections`;
DELETE FROM `payments`;
DELETE FROM `placement_assessments`;
DELETE FROM `placement_exceptions`;
DELETE FROM `program_levels`;
DELETE FROM `programs`;
DELETE FROM `result_reopens`;
DELETE FROM `rooms`;
DELETE FROM `schedules`;
DELETE FROM `school_classes`;
DELETE FROM `session_events`;
DELETE FROM `students`;
DELETE FROM `tuition`;
DELETE FROM `tuition_adjustments`;

SET FOREIGN_KEY_CHECKS=1;
SET NAMES utf8mb4;

SET @b=(SELECT id FROM branches WHERE code='CN01' AND status='ACTIVE');
SET @actor=(SELECT id FROM accounts WHERE username='superadmin');
SET @staff=(SELECT id FROM accounts WHERE username='staff');
SET @teacher=(SELECT id FROM teachers WHERE account_id=(SELECT id FROM accounts WHERE username='teacher'));
SET @teacher_account=(SELECT account_id FROM teachers WHERE id=@teacher);
SET @teacher_name=(SELECT full_name FROM teachers WHERE id=@teacher);

INSERT INTO programs (id,code,name,active,version) VALUES
(1,'GE','Giao tiếp',1,0),
(2,'IELTS','IELTS',1,0),
(3,'TOEIC','TOEIC',1,0);

INSERT INTO program_levels (id,program_id,code,name,sort_order,active,version) VALUES
(1,1,'A1','A1',1,1,0),
(2,1,'A2','A2',2,1,0),
(3,1,'B1','B1',3,1,0),
(4,2,'BASIC','Cơ bản',1,1,0),
(5,2,'5.5','Mục tiêu 5.5',2,1,0),
(6,2,'6.5','Mục tiêu 6.5',3,1,0),
(7,3,'BASIC','Cơ bản',1,1,0),
(8,3,'450','Mục tiêu 450',2,1,0),
(9,3,'650','Mục tiêu 650',3,1,0);

INSERT INTO courses (id,branch_id,code,name,type,level,duration,duration_revision,expected_minutes,tuition_fee,max_students,status,content,entry_requirement) VALUES
(1,@b,'GT1','Giao tiếp 1','GE','A1',8,1,720,2000000,15,'OPEN','8 buổi, mỗi buổi 90 phút.','Nhận học viên mới; tư vấn trình độ trước khi đăng ký.'),
(2,@b,'GT2','Giao tiếp 2','GE','A2',8,1,720,2500000,15,'OPEN','8 buổi, mỗi buổi 90 phút.','Nhận học viên mới; tư vấn trình độ trước khi đăng ký.'),
(3,@b,'IELTS1','IELTS nền tảng','IELTS','BASIC',8,1,720,3500000,15,'OPEN','8 buổi, mỗi buổi 90 phút.','Nhận học viên mới; tư vấn trình độ trước khi đăng ký.'),
(4,@b,'TOEIC1','TOEIC nền tảng','TOEIC','BASIC',8,1,720,3000000,15,'OPEN','8 buổi, mỗi buổi 90 phút.','Nhận học viên mới; tư vấn trình độ trước khi đăng ký.');

INSERT INTO course_duration_plans (id,course_id,revision,expected_minutes,actor_id,actor_name,occurred_at,reason) VALUES
(1,1,1,720,@actor,'superadmin','2026-10-30 08:00:00','Dữ liệu mẫu: 8 buổi x 90 phút'),
(2,2,1,720,@actor,'superadmin','2026-10-30 08:00:00','Dữ liệu mẫu: 8 buổi x 90 phút'),
(3,3,1,720,@actor,'superadmin','2026-10-30 08:00:00','Dữ liệu mẫu: 8 buổi x 90 phút'),
(4,4,1,720,@actor,'superadmin','2026-10-30 08:00:00','Dữ liệu mẫu: 8 buổi x 90 phút');

INSERT INTO course_admission_policies (id,course_id,revision,mode,program_id,program_code,program_name,output_level_id,output_level_code,output_level_name,confirmed_by,confirmed_by_name,confirmed_at,reason) VALUES
(1,1,1,'OPEN_ENTRY',1,'GE','Giao tiếp',1,'A1','A1',@actor,'superadmin','2026-10-30 08:00:00','Lớp nhập môn, không bắt buộc kết quả đầu vào'),
(2,2,1,'OPEN_ENTRY',1,'GE','Giao tiếp',2,'A2','A2',@actor,'superadmin','2026-10-30 08:00:00','Lớp nhập môn, không bắt buộc kết quả đầu vào'),
(3,3,1,'OPEN_ENTRY',2,'IELTS','IELTS',4,'BASIC','Cơ bản',@actor,'superadmin','2026-10-30 08:00:00','Lớp nhập môn, không bắt buộc kết quả đầu vào'),
(4,4,1,'OPEN_ENTRY',3,'TOEIC','TOEIC',7,'BASIC','Cơ bản',@actor,'superadmin','2026-10-30 08:00:00','Lớp nhập môn, không bắt buộc kết quả đầu vào');

INSERT INTO rooms (id,branch_id,code,name,capacity,status,location,equipment) VALUES
(1,@b,'P1','Phòng 1',20,'AVAILABLE','Tầng 1','TV, bảng, loa'),
(2,@b,'P2','Phòng 2',20,'AVAILABLE','Tầng 1','Máy chiếu, bảng'),
(3,@b,'P3','Phòng 3',15,'AVAILABLE','Tầng 2','TV, điều hòa');

INSERT INTO school_classes (id,branch_id,code,name,current_size,max_size,start_date,end_date,status,status_manually_set,teacher_id,teacher_name,course_id,room_id,session_end_date,sessions_initialized,admission_policy_id,admission_revision,duration_revision,expected_minutes) VALUES
(1,@b,'GT1-A','Giao tiếp 1A',3,15,'2026-11-02','2026-11-25','ONGOING',0,@teacher,@teacher_name,1,1,'2026-11-25',1,1,1,1,720),
(2,@b,'GT2-A','Giao tiếp 2A',3,15,'2026-11-03','2026-11-26','ONGOING',0,@teacher,@teacher_name,2,2,'2026-11-26',1,2,1,1,720),
(3,@b,'IELTS-A','IELTS A',3,15,'2026-11-06','2026-11-29','ONGOING',0,@teacher,@teacher_name,3,3,'2026-11-29',1,3,1,1,720),
(4,@b,'TOEIC-A','TOEIC A',3,15,'2026-11-07','2026-11-29','ONGOING',0,@teacher,@teacher_name,4,1,'2026-11-29',1,4,1,1,720);

INSERT INTO class_duration_plans (id,class_id,course_id,course_revision,expected_minutes,actor_id,actor_name,occurred_at,reason,revision,source) VALUES
(1,1,1,1,720,@actor,'superadmin','2026-10-31 08:00:00','Theo khóa học',1,'COURSE'),
(2,2,2,1,720,@actor,'superadmin','2026-10-31 08:00:00','Theo khóa học',1,'COURSE'),
(3,3,3,1,720,@actor,'superadmin','2026-10-31 08:00:00','Theo khóa học',1,'COURSE'),
(4,4,4,1,720,@actor,'superadmin','2026-10-31 08:00:00','Theo khóa học',1,'COURSE');

INSERT INTO schedules (id,class_id,day_of_week,start_time,end_time) VALUES
(1,1,'MONDAY','18:00:00','19:30:00'),
(2,1,'WEDNESDAY','18:00:00','19:30:00'),
(3,2,'TUESDAY','18:00:00','19:30:00'),
(4,2,'THURSDAY','18:00:00','19:30:00'),
(5,3,'FRIDAY','08:00:00','09:30:00'),
(6,3,'SUNDAY','08:00:00','09:30:00'),
(7,4,'SATURDAY','10:00:00','11:30:00'),
(8,4,'SUNDAY','10:00:00','11:30:00');

INSERT INTO class_sessions (id,class_id,session_date,start_time,end_time,room_id,room_name,planned_teacher_id,planned_teacher_name,teacher_id,teacher_name,status,kind,source_date,source_schedule_id,customized,version,created_at,updated_at) VALUES
(1,1,'2026-11-02','18:00:00','19:30:00',1,'Phòng 1',@teacher,@teacher_name,@teacher,@teacher_name,'COMPLETED','REGULAR','2026-11-02',1,0,0,'2026-10-31 09:00:00','2026-10-31 09:00:00'),
(2,1,'2026-11-04','18:00:00','19:30:00',1,'Phòng 1',@teacher,@teacher_name,@teacher,@teacher_name,'COMPLETED','REGULAR','2026-11-04',2,0,0,'2026-10-31 09:00:00','2026-10-31 09:00:00'),
(3,1,'2026-11-09','18:00:00','19:30:00',1,'Phòng 1',@teacher,@teacher_name,@teacher,@teacher_name,'COMPLETED','REGULAR','2026-11-09',1,0,0,'2026-10-31 09:00:00','2026-10-31 09:00:00'),
(4,1,'2026-11-11','18:00:00','19:30:00',1,'Phòng 1',@teacher,@teacher_name,@teacher,@teacher_name,'SCHEDULED','REGULAR','2026-11-11',2,0,0,'2026-10-31 09:00:00','2026-10-31 09:00:00'),
(5,1,'2026-11-16','18:00:00','19:30:00',1,'Phòng 1',@teacher,@teacher_name,@teacher,@teacher_name,'SCHEDULED','REGULAR','2026-11-16',1,0,0,'2026-10-31 09:00:00','2026-10-31 09:00:00'),
(6,1,'2026-11-18','18:00:00','19:30:00',1,'Phòng 1',@teacher,@teacher_name,@teacher,@teacher_name,'SCHEDULED','REGULAR','2026-11-18',2,0,0,'2026-10-31 09:00:00','2026-10-31 09:00:00'),
(7,1,'2026-11-23','18:00:00','19:30:00',1,'Phòng 1',@teacher,@teacher_name,@teacher,@teacher_name,'SCHEDULED','REGULAR','2026-11-23',1,0,0,'2026-10-31 09:00:00','2026-10-31 09:00:00'),
(8,1,'2026-11-25','18:00:00','19:30:00',1,'Phòng 1',@teacher,@teacher_name,@teacher,@teacher_name,'SCHEDULED','REGULAR','2026-11-25',2,0,0,'2026-10-31 09:00:00','2026-10-31 09:00:00'),
(9,2,'2026-11-03','18:00:00','19:30:00',2,'Phòng 2',@teacher,@teacher_name,@teacher,@teacher_name,'COMPLETED','REGULAR','2026-11-03',3,0,0,'2026-10-31 09:00:00','2026-10-31 09:00:00'),
(10,2,'2026-11-05','18:00:00','19:30:00',2,'Phòng 2',@teacher,@teacher_name,@teacher,@teacher_name,'COMPLETED','REGULAR','2026-11-05',4,0,0,'2026-10-31 09:00:00','2026-10-31 09:00:00'),
(11,2,'2026-11-10','18:00:00','19:30:00',2,'Phòng 2',@teacher,@teacher_name,@teacher,@teacher_name,'COMPLETED','REGULAR','2026-11-10',3,0,0,'2026-10-31 09:00:00','2026-10-31 09:00:00'),
(12,2,'2026-11-12','18:00:00','19:30:00',2,'Phòng 2',@teacher,@teacher_name,@teacher,@teacher_name,'SCHEDULED','REGULAR','2026-11-12',4,0,0,'2026-10-31 09:00:00','2026-10-31 09:00:00'),
(13,2,'2026-11-17','18:00:00','19:30:00',2,'Phòng 2',@teacher,@teacher_name,@teacher,@teacher_name,'SCHEDULED','REGULAR','2026-11-17',3,0,0,'2026-10-31 09:00:00','2026-10-31 09:00:00'),
(14,2,'2026-11-19','18:00:00','19:30:00',2,'Phòng 2',@teacher,@teacher_name,@teacher,@teacher_name,'SCHEDULED','REGULAR','2026-11-19',4,0,0,'2026-10-31 09:00:00','2026-10-31 09:00:00'),
(15,2,'2026-11-24','18:00:00','19:30:00',2,'Phòng 2',@teacher,@teacher_name,@teacher,@teacher_name,'SCHEDULED','REGULAR','2026-11-24',3,0,0,'2026-10-31 09:00:00','2026-10-31 09:00:00'),
(16,2,'2026-11-26','18:00:00','19:30:00',2,'Phòng 2',@teacher,@teacher_name,@teacher,@teacher_name,'SCHEDULED','REGULAR','2026-11-26',4,0,0,'2026-10-31 09:00:00','2026-10-31 09:00:00'),
(17,3,'2026-11-06','08:00:00','09:30:00',3,'Phòng 3',@teacher,@teacher_name,@teacher,@teacher_name,'COMPLETED','REGULAR','2026-11-06',5,0,0,'2026-10-31 09:00:00','2026-10-31 09:00:00'),
(18,3,'2026-11-08','08:00:00','09:30:00',3,'Phòng 3',@teacher,@teacher_name,@teacher,@teacher_name,'COMPLETED','REGULAR','2026-11-08',6,0,0,'2026-10-31 09:00:00','2026-10-31 09:00:00'),
(19,3,'2026-11-13','08:00:00','09:30:00',3,'Phòng 3',@teacher,@teacher_name,@teacher,@teacher_name,'SCHEDULED','REGULAR','2026-11-13',5,0,0,'2026-10-31 09:00:00','2026-10-31 09:00:00'),
(20,3,'2026-11-15','08:00:00','09:30:00',3,'Phòng 3',@teacher,@teacher_name,@teacher,@teacher_name,'SCHEDULED','REGULAR','2026-11-15',6,0,0,'2026-10-31 09:00:00','2026-10-31 09:00:00'),
(21,3,'2026-11-20','08:00:00','09:30:00',3,'Phòng 3',@teacher,@teacher_name,@teacher,@teacher_name,'SCHEDULED','REGULAR','2026-11-20',5,0,0,'2026-10-31 09:00:00','2026-10-31 09:00:00'),
(22,3,'2026-11-22','08:00:00','09:30:00',3,'Phòng 3',@teacher,@teacher_name,@teacher,@teacher_name,'SCHEDULED','REGULAR','2026-11-22',6,0,0,'2026-10-31 09:00:00','2026-10-31 09:00:00'),
(23,3,'2026-11-27','08:00:00','09:30:00',3,'Phòng 3',@teacher,@teacher_name,@teacher,@teacher_name,'SCHEDULED','REGULAR','2026-11-27',5,0,0,'2026-10-31 09:00:00','2026-10-31 09:00:00'),
(24,3,'2026-11-29','08:00:00','09:30:00',3,'Phòng 3',@teacher,@teacher_name,@teacher,@teacher_name,'SCHEDULED','REGULAR','2026-11-29',6,0,0,'2026-10-31 09:00:00','2026-10-31 09:00:00'),
(25,4,'2026-11-07','10:00:00','11:30:00',1,'Phòng 1',@teacher,@teacher_name,@teacher,@teacher_name,'COMPLETED','REGULAR','2026-11-07',7,0,0,'2026-10-31 09:00:00','2026-10-31 09:00:00'),
(26,4,'2026-11-08','10:00:00','11:30:00',1,'Phòng 1',@teacher,@teacher_name,@teacher,@teacher_name,'COMPLETED','REGULAR','2026-11-08',8,0,0,'2026-10-31 09:00:00','2026-10-31 09:00:00'),
(27,4,'2026-11-14','10:00:00','11:30:00',1,'Phòng 1',@teacher,@teacher_name,@teacher,@teacher_name,'SCHEDULED','REGULAR','2026-11-14',7,0,0,'2026-10-31 09:00:00','2026-10-31 09:00:00'),
(28,4,'2026-11-15','10:00:00','11:30:00',1,'Phòng 1',@teacher,@teacher_name,@teacher,@teacher_name,'SCHEDULED','REGULAR','2026-11-15',8,0,0,'2026-10-31 09:00:00','2026-10-31 09:00:00'),
(29,4,'2026-11-21','10:00:00','11:30:00',1,'Phòng 1',@teacher,@teacher_name,@teacher,@teacher_name,'SCHEDULED','REGULAR','2026-11-21',7,0,0,'2026-10-31 09:00:00','2026-10-31 09:00:00'),
(30,4,'2026-11-22','10:00:00','11:30:00',1,'Phòng 1',@teacher,@teacher_name,@teacher,@teacher_name,'SCHEDULED','REGULAR','2026-11-22',8,0,0,'2026-10-31 09:00:00','2026-10-31 09:00:00'),
(31,4,'2026-11-28','10:00:00','11:30:00',1,'Phòng 1',@teacher,@teacher_name,@teacher,@teacher_name,'SCHEDULED','REGULAR','2026-11-28',7,0,0,'2026-10-31 09:00:00','2026-10-31 09:00:00'),
(32,4,'2026-11-29','10:00:00','11:30:00',1,'Phòng 1',@teacher,@teacher_name,@teacher,@teacher_name,'SCHEDULED','REGULAR','2026-11-29',8,0,0,'2026-10-31 09:00:00','2026-10-31 09:00:00');

INSERT INTO students (id,branch_id,student_code,full_name,date_of_birth,gender,email,phone,address,registered_at,status,current_level,version) VALUES
(1,@b,'HV01','Minh Anh','2002-03-15','FEMALE','hv1@example.com','0901000001','TP.HCM','2026-10-31','STUDYING',NULL,0),
(2,@b,'HV02','Quốc Bảo','2003-03-15','MALE','hv2@example.com','0901000002','TP.HCM','2026-10-31','STUDYING',NULL,0),
(3,@b,'HV03','Thu Hà','2004-03-15','FEMALE','hv3@example.com','0901000003','TP.HCM','2026-10-31','STUDYING',NULL,0),
(4,@b,'HV04','Hoàng Nam','2005-03-15','MALE','hv4@example.com','0901000004','TP.HCM','2026-10-31','STUDYING',NULL,0),
(5,@b,'HV05','Ngọc Linh','2002-03-15','FEMALE','hv5@example.com','0901000005','TP.HCM','2026-10-31','STUDYING',NULL,0),
(6,@b,'HV06','Gia Huy','2003-03-15','MALE','hv6@example.com','0901000006','TP.HCM','2026-10-31','STUDYING',NULL,0),
(7,@b,'HV07','Thanh Mai','2004-03-15','FEMALE','hv7@example.com','0901000007','TP.HCM','2026-10-31','STUDYING',NULL,0),
(8,@b,'HV08','Đức Anh','2005-03-15','MALE','hv8@example.com','0901000008','TP.HCM','2026-10-31','STUDYING',NULL,0),
(9,@b,'HV09','Bảo Ngọc','2002-03-15','FEMALE','hv9@example.com','0901000009','TP.HCM','2026-10-31','STUDYING',NULL,0),
(10,@b,'HV10','Tuấn Kiệt','2003-03-15','MALE','hv10@example.com','0901000010','TP.HCM','2026-10-31','STUDYING',NULL,0),
(11,@b,'HV11','Khánh Vy','2004-03-15','FEMALE','hv11@example.com','0901000011','TP.HCM','2026-10-31','STUDYING',NULL,0),
(12,@b,'HV12','Minh Khang','2005-03-15','MALE','hv12@example.com','0901000012','TP.HCM','2026-10-31','STUDYING',NULL,0);

INSERT INTO enrollments (id,class_id,student_id,enrolled_at,study_status,billing_enrollment_id,registered_by,registered_by_name,tuition_delivery,tuition_fee,version) VALUES
(1,1,1,'2026-10-31 10:00:00','STUDYING',1,@staff,'staff','CREATED',2000000,0),
(2,1,2,'2026-10-31 10:00:00','STUDYING',2,@staff,'staff','CREATED',2000000,0),
(3,1,3,'2026-10-31 10:00:00','STUDYING',3,@staff,'staff','CREATED',2000000,0),
(4,2,4,'2026-10-31 10:00:00','STUDYING',4,@staff,'staff','CREATED',2500000,0),
(5,2,5,'2026-10-31 10:00:00','STUDYING',5,@staff,'staff','CREATED',2500000,0),
(6,2,6,'2026-10-31 10:00:00','STUDYING',6,@staff,'staff','CREATED',2500000,0),
(7,3,7,'2026-10-31 10:00:00','STUDYING',7,@staff,'staff','CREATED',3500000,0),
(8,3,8,'2026-10-31 10:00:00','STUDYING',8,@staff,'staff','CREATED',3500000,0),
(9,3,9,'2026-10-31 10:00:00','STUDYING',9,@staff,'staff','CREATED',3500000,0),
(10,4,10,'2026-10-31 10:00:00','STUDYING',10,@staff,'staff','CREATED',3000000,0),
(11,4,11,'2026-10-31 10:00:00','STUDYING',11,@staff,'staff','CREATED',3000000,0),
(12,4,12,'2026-10-31 10:00:00','STUDYING',12,@staff,'staff','CREATED',3000000,0);

INSERT INTO enrollment_periods (id,enrollment_id,active_from,active_until) VALUES
(1,1,'2026-10-31',NULL),
(2,2,'2026-10-31',NULL),
(3,3,'2026-10-31',NULL),
(4,4,'2026-10-31',NULL),
(5,5,'2026-10-31',NULL),
(6,6,'2026-10-31',NULL),
(7,7,'2026-10-31',NULL),
(8,8,'2026-10-31',NULL),
(9,9,'2026-10-31',NULL),
(10,10,'2026-10-31',NULL),
(11,11,'2026-10-31',NULL),
(12,12,'2026-10-31',NULL);

INSERT INTO enrollment_admissions (id,enrollment_id,student_id,class_id,policy_id,class_revision,intent,academic_status,fingerprint,policy_snapshot,assessment_snapshot,actor_id,actor_name,accepted_at) VALUES
(1,1,1,1,1,1,'ENROLL','ELIGIBLE',SHA2('demo-enrollment-1',256),'DEMO: policy 1, revision 1, OPEN_ENTRY','NONE',@staff,'staff','2026-10-31 10:00:00'),
(2,2,2,1,1,1,'ENROLL','ELIGIBLE',SHA2('demo-enrollment-2',256),'DEMO: policy 1, revision 1, OPEN_ENTRY','NONE',@staff,'staff','2026-10-31 10:00:00'),
(3,3,3,1,1,1,'ENROLL','ELIGIBLE',SHA2('demo-enrollment-3',256),'DEMO: policy 1, revision 1, OPEN_ENTRY','NONE',@staff,'staff','2026-10-31 10:00:00'),
(4,4,4,2,2,1,'ENROLL','ELIGIBLE',SHA2('demo-enrollment-4',256),'DEMO: policy 2, revision 1, OPEN_ENTRY','NONE',@staff,'staff','2026-10-31 10:00:00'),
(5,5,5,2,2,1,'ENROLL','ELIGIBLE',SHA2('demo-enrollment-5',256),'DEMO: policy 2, revision 1, OPEN_ENTRY','NONE',@staff,'staff','2026-10-31 10:00:00'),
(6,6,6,2,2,1,'ENROLL','ELIGIBLE',SHA2('demo-enrollment-6',256),'DEMO: policy 2, revision 1, OPEN_ENTRY','NONE',@staff,'staff','2026-10-31 10:00:00'),
(7,7,7,3,3,1,'ENROLL','ELIGIBLE',SHA2('demo-enrollment-7',256),'DEMO: policy 3, revision 1, OPEN_ENTRY','NONE',@staff,'staff','2026-10-31 10:00:00'),
(8,8,8,3,3,1,'ENROLL','ELIGIBLE',SHA2('demo-enrollment-8',256),'DEMO: policy 3, revision 1, OPEN_ENTRY','NONE',@staff,'staff','2026-10-31 10:00:00'),
(9,9,9,3,3,1,'ENROLL','ELIGIBLE',SHA2('demo-enrollment-9',256),'DEMO: policy 3, revision 1, OPEN_ENTRY','NONE',@staff,'staff','2026-10-31 10:00:00'),
(10,10,10,4,4,1,'ENROLL','ELIGIBLE',SHA2('demo-enrollment-10',256),'DEMO: policy 4, revision 1, OPEN_ENTRY','NONE',@staff,'staff','2026-10-31 10:00:00'),
(11,11,11,4,4,1,'ENROLL','ELIGIBLE',SHA2('demo-enrollment-11',256),'DEMO: policy 4, revision 1, OPEN_ENTRY','NONE',@staff,'staff','2026-10-31 10:00:00'),
(12,12,12,4,4,1,'ENROLL','ELIGIBLE',SHA2('demo-enrollment-12',256),'DEMO: policy 4, revision 1, OPEN_ENTRY','NONE',@staff,'staff','2026-10-31 10:00:00');

INSERT INTO tuition (id,branch_id,class_id,class_name,course_id,course_name,student_id,student_name,enrollment_id,amount,original_amount,discount_amount,paid_amount,currency,due_date,status,version) VALUES
(1,@b,1,'Giao tiếp 1A',1,'Giao tiếp 1',1,'Minh Anh',1,2000000,2000000,0,2000000,'VND','2026-11-20','PAID',0),
(2,@b,1,'Giao tiếp 1A',1,'Giao tiếp 1',2,'Quốc Bảo',2,2000000,2000000,0,1000000,'VND','2026-11-20','PARTIAL',0),
(3,@b,1,'Giao tiếp 1A',1,'Giao tiếp 1',3,'Thu Hà',3,2000000,2000000,0,0,'VND','2026-11-20','UNPAID',0),
(4,@b,2,'Giao tiếp 2A',2,'Giao tiếp 2',4,'Hoàng Nam',4,2500000,2500000,0,2500000,'VND','2026-11-20','PAID',0),
(5,@b,2,'Giao tiếp 2A',2,'Giao tiếp 2',5,'Ngọc Linh',5,2500000,2500000,0,1250000,'VND','2026-11-20','PARTIAL',0),
(6,@b,2,'Giao tiếp 2A',2,'Giao tiếp 2',6,'Gia Huy',6,2500000,2500000,0,0,'VND','2026-11-20','UNPAID',0),
(7,@b,3,'IELTS A',3,'IELTS nền tảng',7,'Thanh Mai',7,3500000,3500000,0,3500000,'VND','2026-11-20','PAID',0),
(8,@b,3,'IELTS A',3,'IELTS nền tảng',8,'Đức Anh',8,3500000,3500000,0,1750000,'VND','2026-11-20','PARTIAL',0),
(9,@b,3,'IELTS A',3,'IELTS nền tảng',9,'Bảo Ngọc',9,3500000,3500000,0,0,'VND','2026-11-20','UNPAID',0),
(10,@b,4,'TOEIC A',4,'TOEIC nền tảng',10,'Tuấn Kiệt',10,3000000,3000000,0,3000000,'VND','2026-11-20','PAID',0),
(11,@b,4,'TOEIC A',4,'TOEIC nền tảng',11,'Khánh Vy',11,3000000,3000000,0,1500000,'VND','2026-11-20','PARTIAL',0),
(12,@b,4,'TOEIC A',4,'TOEIC nền tảng',12,'Minh Khang',12,3000000,3000000,0,0,'VND','2026-11-20','UNPAID',0);

INSERT INTO payments (id,tuition_id,branch_id,amount,paid_at,payment_method,status,receipt_no,recorded_by,recorded_by_name,note,request_key) VALUES
(1,1,@b,2000000,'2026-11-01 09:00:00','CASH','PAID','PT01',@staff,'staff','Thu học phí mẫu','demo-payment-1'),
(2,2,@b,1000000,'2026-11-01 09:00:00','BANK_TRANSFER','PAID','PT02',@staff,'staff','Thu học phí mẫu','demo-payment-2'),
(3,4,@b,2500000,'2026-11-01 09:00:00','CASH','PAID','PT03',@staff,'staff','Thu học phí mẫu','demo-payment-3'),
(4,5,@b,1250000,'2026-11-01 09:00:00','BANK_TRANSFER','PAID','PT04',@staff,'staff','Thu học phí mẫu','demo-payment-4'),
(5,7,@b,3500000,'2026-11-01 09:00:00','CASH','PAID','PT05',@staff,'staff','Thu học phí mẫu','demo-payment-5'),
(6,8,@b,1750000,'2026-11-01 09:00:00','BANK_TRANSFER','PAID','PT06',@staff,'staff','Thu học phí mẫu','demo-payment-6'),
(7,10,@b,3000000,'2026-11-01 09:00:00','CASH','PAID','PT07',@staff,'staff','Thu học phí mẫu','demo-payment-7'),
(8,11,@b,1500000,'2026-11-01 09:00:00','BANK_TRANSFER','PAID','PT08',@staff,'staff','Thu học phí mẫu','demo-payment-8');

INSERT INTO attendances (id,enrollment_id,class_session_id,session_date,status,has_permission,recorded_by,recorded_by_name) VALUES
(1,1,1,'2026-11-02','PRESENT',0,@teacher_account,@teacher_name),
(2,2,1,'2026-11-02','PRESENT',0,@teacher_account,@teacher_name),
(3,3,1,'2026-11-02','PRESENT',0,@teacher_account,@teacher_name),
(4,1,2,'2026-11-04','PRESENT',0,@teacher_account,@teacher_name),
(5,2,2,'2026-11-04','PRESENT',0,@teacher_account,@teacher_name),
(6,3,2,'2026-11-04','PRESENT',0,@teacher_account,@teacher_name),
(7,1,3,'2026-11-09','PRESENT',0,@teacher_account,@teacher_name),
(8,2,3,'2026-11-09','PRESENT',0,@teacher_account,@teacher_name),
(9,3,3,'2026-11-09','LATE',0,@teacher_account,@teacher_name),
(10,4,9,'2026-11-03','PRESENT',0,@teacher_account,@teacher_name),
(11,5,9,'2026-11-03','PRESENT',0,@teacher_account,@teacher_name),
(12,6,9,'2026-11-03','LATE',0,@teacher_account,@teacher_name),
(13,4,10,'2026-11-05','PRESENT',0,@teacher_account,@teacher_name),
(14,5,10,'2026-11-05','PRESENT',0,@teacher_account,@teacher_name),
(15,6,10,'2026-11-05','PRESENT',0,@teacher_account,@teacher_name),
(16,4,11,'2026-11-10','PRESENT',0,@teacher_account,@teacher_name),
(17,5,11,'2026-11-10','PRESENT',0,@teacher_account,@teacher_name),
(18,6,11,'2026-11-10','PRESENT',0,@teacher_account,@teacher_name),
(19,7,17,'2026-11-06','PRESENT',0,@teacher_account,@teacher_name),
(20,8,17,'2026-11-06','PRESENT',0,@teacher_account,@teacher_name),
(21,9,17,'2026-11-06','PRESENT',0,@teacher_account,@teacher_name),
(22,7,18,'2026-11-08','PRESENT',0,@teacher_account,@teacher_name),
(23,8,18,'2026-11-08','PRESENT',0,@teacher_account,@teacher_name),
(24,9,18,'2026-11-08','LATE',0,@teacher_account,@teacher_name),
(25,10,25,'2026-11-07','PRESENT',0,@teacher_account,@teacher_name),
(26,11,25,'2026-11-07','PRESENT',0,@teacher_account,@teacher_name),
(27,12,25,'2026-11-07','PRESENT',0,@teacher_account,@teacher_name),
(28,10,26,'2026-11-08','PRESENT',0,@teacher_account,@teacher_name),
(29,11,26,'2026-11-08','PRESENT',0,@teacher_account,@teacher_name),
(30,12,26,'2026-11-08','PRESENT',0,@teacher_account,@teacher_name);

INSERT INTO exams (id,branch_id,class_id,name,exam_date,exam_type,max_score,weight,created_at,created_by,created_by_name,version,roster_frozen_at,roster_frozen_by,roster_frozen_by_name) VALUES
(1,@b,1,'Bài 1','2026-11-10','QUIZ',10,40,'2026-11-01 08:00:00',@teacher_account,@teacher_name,0,'2026-11-10 07:00:00',@teacher_account,@teacher_name),
(2,@b,1,'Cuối khóa','2026-11-25','FINAL',10,60,'2026-11-01 08:00:00',@teacher_account,@teacher_name,0,NULL,NULL,NULL),
(3,@b,2,'Bài 1','2026-11-10','QUIZ',10,40,'2026-11-01 08:00:00',@teacher_account,@teacher_name,0,'2026-11-10 07:00:00',@teacher_account,@teacher_name),
(4,@b,2,'Cuối khóa','2026-11-26','FINAL',10,60,'2026-11-01 08:00:00',@teacher_account,@teacher_name,0,NULL,NULL,NULL),
(5,@b,3,'Bài 1','2026-11-10','QUIZ',10,40,'2026-11-01 08:00:00',@teacher_account,@teacher_name,0,'2026-11-10 07:00:00',@teacher_account,@teacher_name),
(6,@b,3,'Cuối khóa','2026-11-29','FINAL',10,60,'2026-11-01 08:00:00',@teacher_account,@teacher_name,0,NULL,NULL,NULL),
(7,@b,4,'Bài 1','2026-11-10','QUIZ',10,40,'2026-11-01 08:00:00',@teacher_account,@teacher_name,0,'2026-11-10 07:00:00',@teacher_account,@teacher_name),
(8,@b,4,'Cuối khóa','2026-11-29','FINAL',10,60,'2026-11-01 08:00:00',@teacher_account,@teacher_name,0,NULL,NULL,NULL);

INSERT INTO exam_candidates (id,exam_id,student_id,student_name,source,enrollment_id) VALUES
(1,1,1,'Minh Anh','ENROLLMENT_PERIOD',1),
(2,1,2,'Quốc Bảo','ENROLLMENT_PERIOD',2),
(3,1,3,'Thu Hà','ENROLLMENT_PERIOD',3),
(4,3,4,'Hoàng Nam','ENROLLMENT_PERIOD',4),
(5,3,5,'Ngọc Linh','ENROLLMENT_PERIOD',5),
(6,3,6,'Gia Huy','ENROLLMENT_PERIOD',6),
(7,5,7,'Thanh Mai','ENROLLMENT_PERIOD',7),
(8,5,8,'Đức Anh','ENROLLMENT_PERIOD',8),
(9,5,9,'Bảo Ngọc','ENROLLMENT_PERIOD',9),
(10,7,10,'Tuấn Kiệt','ENROLLMENT_PERIOD',10),
(11,7,11,'Khánh Vy','ENROLLMENT_PERIOD',11),
(12,7,12,'Minh Khang','ENROLLMENT_PERIOD',12);

INSERT INTO grades (id,branch_id,exam_id,student_id,student_name,score,comment,entered_at,entered_by,entered_by_name,version) VALUES
(1,@b,1,1,'Minh Anh',7,'Đạt','2026-11-10 20:00:00',@teacher_account,@teacher_name,0),
(2,@b,1,2,'Quốc Bảo',8,'Đạt','2026-11-10 20:00:00',@teacher_account,@teacher_name,0),
(3,@b,1,3,'Thu Hà',9,'Đạt','2026-11-10 20:00:00',@teacher_account,@teacher_name,0),
(4,@b,3,4,'Hoàng Nam',7,'Đạt','2026-11-10 20:00:00',@teacher_account,@teacher_name,0),
(5,@b,3,5,'Ngọc Linh',8,'Đạt','2026-11-10 20:00:00',@teacher_account,@teacher_name,0),
(6,@b,3,6,'Gia Huy',9,'Đạt','2026-11-10 20:00:00',@teacher_account,@teacher_name,0),
(7,@b,5,7,'Thanh Mai',7,'Đạt','2026-11-10 20:00:00',@teacher_account,@teacher_name,0),
(8,@b,5,8,'Đức Anh',8,'Đạt','2026-11-10 20:00:00',@teacher_account,@teacher_name,0),
(9,@b,5,9,'Bảo Ngọc',9,'Đạt','2026-11-10 20:00:00',@teacher_account,@teacher_name,0),
(10,@b,7,10,'Tuấn Kiệt',7,'Đạt','2026-11-10 20:00:00',@teacher_account,@teacher_name,0),
(11,@b,7,11,'Khánh Vy',8,'Đạt','2026-11-10 20:00:00',@teacher_account,@teacher_name,0),
(12,@b,7,12,'Minh Khang',9,'Đạt','2026-11-10 20:00:00',@teacher_account,@teacher_name,0);

INSERT INTO placement_assessments (id,student_id,branch_id,program_id,program_code,program_name,level_id,level_code,level_name,method,score,max_score,assessed_on,assessor_name,source,status,recorded_by,recorded_by_name,confirmed_by,confirmed_by_name,confirmed_at,created_at,updated_at,version) VALUES
(1,1,@b,1,'GE','Giao tiếp',1,'A1','A1','INTERVIEW',NULL,NULL,'2026-10-31',@teacher_name,'Phỏng vấn mẫu HV1','CONFIRMED',@staff,'staff',@actor,'superadmin','2026-10-31 09:00:00','2026-10-31 08:30:00','2026-10-31 09:00:00',0),
(2,2,@b,1,'GE','Giao tiếp',1,'A1','A1','INTERVIEW',NULL,NULL,'2026-10-31',@teacher_name,'Phỏng vấn mẫu HV2','CONFIRMED',@staff,'staff',@actor,'superadmin','2026-10-31 09:00:00','2026-10-31 08:30:00','2026-10-31 09:00:00',0),
(3,3,@b,1,'GE','Giao tiếp',1,'A1','A1','INTERVIEW',NULL,NULL,'2026-10-31',@teacher_name,'Phỏng vấn mẫu HV3','CONFIRMED',@staff,'staff',@actor,'superadmin','2026-10-31 09:00:00','2026-10-31 08:30:00','2026-10-31 09:00:00',0),
(4,4,@b,1,'GE','Giao tiếp',2,'A2','A2','INTERVIEW',NULL,NULL,'2026-10-31',@teacher_name,'Phỏng vấn mẫu HV4','CONFIRMED',@staff,'staff',@actor,'superadmin','2026-10-31 09:00:00','2026-10-31 08:30:00','2026-10-31 09:00:00',0),
(5,5,@b,1,'GE','Giao tiếp',2,'A2','A2','INTERVIEW',NULL,NULL,'2026-10-31',@teacher_name,'Phỏng vấn mẫu HV5','CONFIRMED',@staff,'staff',@actor,'superadmin','2026-10-31 09:00:00','2026-10-31 08:30:00','2026-10-31 09:00:00',0),
(6,6,@b,1,'GE','Giao tiếp',2,'A2','A2','INTERVIEW',NULL,NULL,'2026-10-31',@teacher_name,'Phỏng vấn mẫu HV6','CONFIRMED',@staff,'staff',@actor,'superadmin','2026-10-31 09:00:00','2026-10-31 08:30:00','2026-10-31 09:00:00',0),
(7,7,@b,2,'IELTS','IELTS',4,'BASIC','Cơ bản','INTERVIEW',NULL,NULL,'2026-10-31',@teacher_name,'Phỏng vấn mẫu HV7','CONFIRMED',@staff,'staff',@actor,'superadmin','2026-10-31 09:00:00','2026-10-31 08:30:00','2026-10-31 09:00:00',0),
(8,8,@b,2,'IELTS','IELTS',4,'BASIC','Cơ bản','INTERVIEW',NULL,NULL,'2026-10-31',@teacher_name,'Phỏng vấn mẫu HV8','CONFIRMED',@staff,'staff',@actor,'superadmin','2026-10-31 09:00:00','2026-10-31 08:30:00','2026-10-31 09:00:00',0),
(9,9,@b,2,'IELTS','IELTS',4,'BASIC','Cơ bản','INTERVIEW',NULL,NULL,'2026-10-31',@teacher_name,'Phỏng vấn mẫu HV9','CONFIRMED',@staff,'staff',@actor,'superadmin','2026-10-31 09:00:00','2026-10-31 08:30:00','2026-10-31 09:00:00',0),
(10,10,@b,3,'TOEIC','TOEIC',7,'BASIC','Cơ bản','INTERVIEW',NULL,NULL,'2026-10-31',@teacher_name,'Phỏng vấn mẫu HV10','CONFIRMED',@staff,'staff',@actor,'superadmin','2026-10-31 09:00:00','2026-10-31 08:30:00','2026-10-31 09:00:00',0),
(11,11,@b,3,'TOEIC','TOEIC',7,'BASIC','Cơ bản','INTERVIEW',NULL,NULL,'2026-10-31',@teacher_name,'Phỏng vấn mẫu HV11','CONFIRMED',@staff,'staff',@actor,'superadmin','2026-10-31 09:00:00','2026-10-31 08:30:00','2026-10-31 09:00:00',0),
(12,12,@b,3,'TOEIC','TOEIC',7,'BASIC','Cơ bản','INTERVIEW',NULL,NULL,'2026-10-31',@teacher_name,'Phỏng vấn mẫu HV12','CONFIRMED',@staff,'staff',@actor,'superadmin','2026-10-31 09:00:00','2026-10-31 08:30:00','2026-10-31 09:00:00',0);

INSERT INTO assessment_events (id,assessment_id,assessment_version,action,actor_id,actor_name,before_state,after_state,occurred_at,reason) VALUES
(1,1,0,'CONFIRMED',@actor,'superadmin','DRAFT','CONFIRMED - dữ liệu mẫu','2026-10-31 09:00:00','Kết quả phỏng vấn mẫu'),
(2,2,0,'CONFIRMED',@actor,'superadmin','DRAFT','CONFIRMED - dữ liệu mẫu','2026-10-31 09:00:00','Kết quả phỏng vấn mẫu'),
(3,3,0,'CONFIRMED',@actor,'superadmin','DRAFT','CONFIRMED - dữ liệu mẫu','2026-10-31 09:00:00','Kết quả phỏng vấn mẫu'),
(4,4,0,'CONFIRMED',@actor,'superadmin','DRAFT','CONFIRMED - dữ liệu mẫu','2026-10-31 09:00:00','Kết quả phỏng vấn mẫu'),
(5,5,0,'CONFIRMED',@actor,'superadmin','DRAFT','CONFIRMED - dữ liệu mẫu','2026-10-31 09:00:00','Kết quả phỏng vấn mẫu'),
(6,6,0,'CONFIRMED',@actor,'superadmin','DRAFT','CONFIRMED - dữ liệu mẫu','2026-10-31 09:00:00','Kết quả phỏng vấn mẫu'),
(7,7,0,'CONFIRMED',@actor,'superadmin','DRAFT','CONFIRMED - dữ liệu mẫu','2026-10-31 09:00:00','Kết quả phỏng vấn mẫu'),
(8,8,0,'CONFIRMED',@actor,'superadmin','DRAFT','CONFIRMED - dữ liệu mẫu','2026-10-31 09:00:00','Kết quả phỏng vấn mẫu'),
(9,9,0,'CONFIRMED',@actor,'superadmin','DRAFT','CONFIRMED - dữ liệu mẫu','2026-10-31 09:00:00','Kết quả phỏng vấn mẫu'),
(10,10,0,'CONFIRMED',@actor,'superadmin','DRAFT','CONFIRMED - dữ liệu mẫu','2026-10-31 09:00:00','Kết quả phỏng vấn mẫu'),
(11,11,0,'CONFIRMED',@actor,'superadmin','DRAFT','CONFIRMED - dữ liệu mẫu','2026-10-31 09:00:00','Kết quả phỏng vấn mẫu'),
(12,12,0,'CONFIRMED',@actor,'superadmin','DRAFT','CONFIRMED - dữ liệu mẫu','2026-10-31 09:00:00','Kết quả phỏng vấn mẫu');

INSERT INTO leads (id,branch_id,assigned_to,assigned_to_name,full_name,phone,email,source,status,interested_course_id,created_at,updated_at,next_contact_at,note,version) VALUES
(1,@b,@staff,'staff','Lan Anh','0912000001','lead1@example.com','WEBSITE','NEW',1,'2026-11-10 08:00:00','2026-11-10 08:00:00','2026-11-12 09:00:00','Quan tâm khóa học',0),
(2,@b,@staff,'staff','Hải Nam','0912000002','lead2@example.com','WEBSITE','CONTACTED',2,'2026-11-10 08:00:00','2026-11-10 08:00:00','2026-11-12 09:00:00','Quan tâm khóa học',0),
(3,@b,@staff,'staff','Bích Ngọc','0912000003','lead3@example.com','WEBSITE','CONSULTING',3,'2026-11-10 08:00:00','2026-11-10 08:00:00','2026-11-12 09:00:00','Quan tâm khóa học',0),
(4,@b,@staff,'staff','Anh Tuấn','0912000004','lead4@example.com','WEBSITE','NEW',4,'2026-11-10 08:00:00','2026-11-10 08:00:00','2026-11-12 09:00:00','Quan tâm khóa học',0);

COMMIT;

-- Keep future IDs short as well. Existing account/profile IDs stay unchanged.
ALTER TABLE `assessment_events` AUTO_INCREMENT=1;
ALTER TABLE `attendance_changes` AUTO_INCREMENT=1;
ALTER TABLE `attendances` AUTO_INCREMENT=1;
ALTER TABLE `class_admission_events` AUTO_INCREMENT=1;
ALTER TABLE `class_closure_events` AUTO_INCREMENT=1;
ALTER TABLE `class_closures` AUTO_INCREMENT=1;
ALTER TABLE `class_duration_plans` AUTO_INCREMENT=1;
ALTER TABLE `class_result_barriers` AUTO_INCREMENT=1;
ALTER TABLE `class_sessions` AUTO_INCREMENT=1;
ALTER TABLE `course_admission_levels` AUTO_INCREMENT=1;
ALTER TABLE `course_admission_policies` AUTO_INCREMENT=1;
ALTER TABLE `course_duration_plans` AUTO_INCREMENT=1;
ALTER TABLE `courses` AUTO_INCREMENT=1;
ALTER TABLE `document_events` AUTO_INCREMENT=1;
ALTER TABLE `documents` AUTO_INCREMENT=1;
ALTER TABLE `enrollment_admissions` AUTO_INCREMENT=1;
ALTER TABLE `enrollment_periods` AUTO_INCREMENT=1;
ALTER TABLE `enrollments` AUTO_INCREMENT=1;
ALTER TABLE `evaluation_policies` AUTO_INCREMENT=1;
ALTER TABLE `evaluation_policy_exams` AUTO_INCREMENT=1;
ALTER TABLE `evaluation_states` AUTO_INCREMENT=1;
ALTER TABLE `exam_candidates` AUTO_INCREMENT=1;
ALTER TABLE `exams` AUTO_INCREMENT=1;
ALTER TABLE `final_results` AUTO_INCREMENT=1;
ALTER TABLE `grade_changes` AUTO_INCREMENT=1;
ALTER TABLE `grades` AUTO_INCREMENT=1;
ALTER TABLE `lead_activities` AUTO_INCREMENT=1;
ALTER TABLE `leads` AUTO_INCREMENT=1;
ALTER TABLE `learning_events` AUTO_INCREMENT=1;
ALTER TABLE `payment_corrections` AUTO_INCREMENT=1;
ALTER TABLE `payments` AUTO_INCREMENT=1;
ALTER TABLE `placement_assessments` AUTO_INCREMENT=1;
ALTER TABLE `placement_exceptions` AUTO_INCREMENT=1;
ALTER TABLE `program_levels` AUTO_INCREMENT=1;
ALTER TABLE `programs` AUTO_INCREMENT=1;
ALTER TABLE `result_reopens` AUTO_INCREMENT=1;
ALTER TABLE `rooms` AUTO_INCREMENT=1;
ALTER TABLE `schedules` AUTO_INCREMENT=1;
ALTER TABLE `school_classes` AUTO_INCREMENT=1;
ALTER TABLE `session_events` AUTO_INCREMENT=1;
ALTER TABLE `students` AUTO_INCREMENT=1;
ALTER TABLE `tuition` AUTO_INCREMENT=1;
ALTER TABLE `tuition_adjustments` AUTO_INCREMENT=1;
