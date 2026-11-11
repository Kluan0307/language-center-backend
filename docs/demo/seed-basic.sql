-- Run once after reset-business-data.sql. Does not modify accounts or assignments.
USE language_center_db;
SET NAMES utf8mb4;
START TRANSACTION;
SET @demo_branch = (SELECT id FROM branches WHERE code = 'CN01' AND status = 'ACTIVE' LIMIT 1);

INSERT INTO programs (code,name,active,version) VALUES
('GENERAL_ENGLISH','Tiếng Anh tổng quát',1,0),
('IELTS','Luyện thi IELTS',1,0);
SET @general = (SELECT id FROM programs WHERE code='GENERAL_ENGLISH');
SET @ielts = (SELECT id FROM programs WHERE code='IELTS');
INSERT INTO program_levels (program_id,code,name,sort_order,active,version) VALUES
(@general,'A1','Sơ cấp A1',1,1,0),
(@general,'A2','Sơ cấp A2',2,1,0),
(@general,'B1','Trung cấp B1',3,1,0),
(@general,'B2','Trung cấp B2',4,1,0),
(@ielts,'FOUNDATION','IELTS Foundation',1,1,0),
(@ielts,'TARGET_6_5','IELTS mục tiêu 6.5',2,1,0);

-- Legacy duration has no verified unit. Configure verified duration/admission plans in the UI.
INSERT INTO courses (branch_id,code,name,type,level,duration,duration_revision,expected_minutes,tuition_fee,max_students,status,content,entry_requirement) VALUES
(@demo_branch,'DEMO-GE-A1','Tiếng Anh giao tiếp A1','GENERAL_ENGLISH','A1',24,0,NULL,2500000,20,'OPEN','Làm quen phát âm, từ vựng và giao tiếp cơ bản.','Chưa yêu cầu chứng chỉ; đánh giá đầu vào trước khi xếp lớp.'),
(@demo_branch,'DEMO-GE-A2','Tiếng Anh giao tiếp A2','GENERAL_ENGLISH','A2',30,0,NULL,3000000,20,'OPEN','Giao tiếp trong tình huống thường ngày.','Cần đối chiếu trình độ đầu vào A1.'),
(@demo_branch,'DEMO-IELTS-F','IELTS Foundation','IELTS','FOUNDATION',36,0,NULL,4500000,15,'OPEN','Làm quen bốn kỹ năng và định dạng IELTS.','Đánh giá đầu vào theo chương trình IELTS.');
INSERT INTO rooms (branch_id,code,name,capacity,status,location,equipment) VALUES
(@demo_branch,'DEMO-P101','Phòng học 101',25,'AVAILABLE','Tầng 1','Máy chiếu, bảng trắng, loa'),
(@demo_branch,'DEMO-P102','Phòng học 102',20,'AVAILABLE','Tầng 1','TV, bảng trắng, điều hòa');
INSERT INTO students (branch_id,student_code,full_name,date_of_birth,gender,email,phone,address,registered_at,status,current_level,version) VALUES
(@demo_branch,'DEMO-HV001','Nguyễn Minh Anh','2004-03-15','FEMALE','minhanh@example.com','0901000001','Quận 1, TP.HCM',CURRENT_DATE,'STUDYING',NULL,0),
(@demo_branch,'DEMO-HV002','Trần Quốc Bảo','2003-07-20','MALE','quocbao@example.com','0901000002','Quận 3, TP.HCM',CURRENT_DATE,'STUDYING',NULL,0),
(@demo_branch,'DEMO-HV003','Lê Thu Hà','2005-11-08','FEMALE','thuha@example.com','0901000003','Bình Thạnh, TP.HCM',CURRENT_DATE,'STUDYING',NULL,0),
(@demo_branch,'DEMO-HV004','Phạm Hoàng Nam','2002-01-12','MALE','hoangnam@example.com','0901000004','Thủ Đức, TP.HCM',CURRENT_DATE,'STUDYING',NULL,0),
(@demo_branch,'DEMO-HV005','Võ Ngọc Linh','2004-09-25','FEMALE','ngoclinh@example.com','0901000005','Quận 7, TP.HCM',CURRENT_DATE,'STUDYING',NULL,0);
COMMIT;
SELECT 'Đã nạp dữ liệu mẫu tại CN01; tài khoản giữ nguyên.' AS result;
