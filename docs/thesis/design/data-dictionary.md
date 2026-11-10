# Từ điển dữ liệu vật lý — V013

Sinh từ metadata MySQL trên bản sao V001–V013 ngày08/11/2026 và entity hiện tại. **53 bảng/625 cột**; không phải schema production đã nâng. [Nguồn JSON](schema-v013.metadata.json) · [Đối chiếu](schema-verification.md) · [Entity](entity-catalog.md). Không chứa dữ liệu nghiệp vụ.

PK/FK/UK/index/nullability/default ở đây là thực tế SQL, không suy từ annotation. Default `∅` là không có default tường minh hoặc SQL NULL theo thông tin information_schema; extra/generation được ghi riêng. Check nghiệp vụ, @Version và immutable qua service không đồng nghĩa DB CHECK/trigger.

## Danh mục

| Bảng | Owner logic | Cột | Mục đích |
|---|---|---|---|
| [account_branch_events](#account-branch-events) | auth | 6 | Lịch sử phân công/thu hồi/đổi chi nhánh chính. |
| [account_branches](#account-branches) | auth | 8 | Phân công nhiều chi nhánh; primary là lựa chọn làm việc, không thay tập quyền. |
| [account_security_events](#account-security-events) | auth | 6 | Audit bảo mật, reset/đổi mật khẩu/thu hồi phiên. |
| [accounts](#accounts) | auth | 11 | Tài khoản, role, trạng thái đăng nhập và phiên. |
| [admins](#admins) | auth | 7 | Hồ sơ ADMIN liên kết account. |
| [assessment_events](#assessment-events) | academic | 10 | Audit assessment trước/sau; không thay chứng cứ assessment. |
| [attendance_changes](#attendance-changes) | academic | 14 | Audit sửa điểm danh; giữ before/after và người thực hiện. |
| [attendances](#attendances) | academic | 8 | Điểm danh theo enrollment/buổi; legacy class_session_id nullable. |
| [branches](#branches) | academic | 7 | Chi nhánh Academic sở hữu; Auth chỉ tham chiếu ID. |
| [class_admission_events](#class-admission-events) | academic | 9 | Lịch sử áp dụng policy khóa cho lớp. |
| [class_closure_events](#class-closure-events) | academic | 9 | Intent/audit đóng mở lớp theo revision. |
| [class_closures](#class-closures) | academic | 9 | Trạng thái đóng/mở được đối soát, manual revision và epoch bằng chứng. |
| [class_duration_plans](#class-duration-plans) | academic | 11 | Lịch sử kế hoạch phút lớp, snapshot nguồn khóa/custom. |
| [class_result_barriers](#class-result-barriers) | shared-coordination | 6 | Metadata phối hợp hẹp Academic/Teaching trong schema dùng chung. |
| [class_sessions](#class-sessions) | academic | 22 | Buổi thực tế: trạng thái/ngày/giờ/actual/planned Teacher, liên kết nghỉ/bù. |
| [course_admission_levels](#course-admission-levels) | academic | 4 | ElementCollection snapshot tập mức đầu vào; không phải entity độc lập. |
| [course_admission_policies](#course-admission-policies) | academic | 14 | Phiên bản chính sách đầu vào và mục tiêu đầu ra của khóa. |
| [course_duration_plans](#course-duration-plans) | academic | 8 | Lịch sử xác nhận số phút khóa. |
| [courses](#courses) | academic | 15 | Khóa: giá, nội dung, trường duration cũ và expectedMinutes đã xác minh. |
| [document_events](#document-events) | teaching | 14 | Lịch sử duyệt/gửi lại và snapshot các revision/file cũ. |
| [documents](#documents) | teaching | 20 | Metadata file giảng dạy; file vật lý bên ngoài MySQL. |
| [enrollment_admissions](#enrollment-admissions) | academic | 18 | Snapshot căn cứ chấp nhận cho ENROLL/RESUME/TRANSFER. |
| [enrollment_periods](#enrollment-periods) | academic | 4 | Khoảng học [activeFrom, activeUntil); legacy có thể không có dòng. |
| [enrollments](#enrollments) | academic | 16 | Đăng ký học, trạng thái, giá snapshot, billing root và refs kết quả. |
| [evaluation_policies](#evaluation-policies) | teaching | 11 | Snapshot policy lớp, ngưỡng/chuyên cần và actor/reason. |
| [evaluation_policy_exams](#evaluation-policy-exams) | teaching | 8 | ElementCollection quy tắc bài của policy: required/weight/maxScore/date. |
| [evaluation_states](#evaluation-states) | teaching | 4 | Parent lock lớp, revision policy và teaching sourceRevision. |
| [exam_candidates](#exam-candidates) | teaching | 6 | Danh sách dự thi chụp theo ngày, enrollmentId có thể null ở legacy. |
| [exams](#exams) | teaching | 15 | Bài kiểm tra và freeze roster; thang/trọng số gợi ý. |
| [final_results](#final-results) | teaching | 23 | Phiên bản kết quả cuối theo enrollment; snapshot bất biến qua service. |
| [grade_changes](#grade-changes) | teaching | 8 | Audit hiệu chỉnh điểm, trước/sau/actor/reason. |
| [grades](#grades) | teaching | 15 | Điểm hiện tại theo exam/student; 0 khác chưa có grade; @Version. |
| [lead_activities](#lead-activities) | academic | 16 | Nhật ký chăm sóc, bàn giao, chuyển đổi; NOTE không là contact. |
| [leads](#leads) | academic | 18 | Tiếp nhận/quan tâm khóa/phụ trách/chuyển đổi thành học viên. |
| [learning_events](#learning-events) | academic | 14 | Lịch sử trạng thái học viên/đăng ký/chuyển/bảo lưu/hoàn thành. |
| [payment_corrections](#payment-corrections) | finance | 14 | Journal VOID/REFUND; giữ phiếu gốc. |
| [payments](#payments) | finance | 13 | Phiếu thu gốc, requestKey/receipt; không xóa bằng hoàn/đảo. |
| [placement_assessments](#placement-assessments) | academic | 27 | Đánh giá đầu vào có nguồn, trạng thái/current key và version. |
| [placement_exceptions](#placement-exceptions) | academic | 26 | Đề nghị/duyệt/tiêu thụ ngoại lệ học thuật với context fingerprint. |
| [program_levels](#program-levels) | academic | 7 | Mức trong chương trình; rank để hiển thị, không quy đổi tự động. |
| [programs](#programs) | academic | 5 | Chương trình/thang điểm cấu trúc. |
| [result_reopens](#result-reopens) | teaching | 7 | Intent/audit mở lại kết quả toàn lớp. |
| [rooms](#rooms) | academic | 8 | Phòng, sức chứa và tình trạng. |
| [schedules](#schedules) | academic | 5 | Lịch tuần mẫu; khác buổi học thực tế. |
| [school_classes](#school-classes) | academic | 20 | Lớp, khóa/phòng/Teacher, kế hoạch snapshot, sĩ số và lịch. |
| [session_events](#session-events) | academic | 9 | Snapshot trước/sau, lý do và người đổi buổi. |
| [staffs](#staffs) | auth | 8 | Hồ sơ STAFF liên kết account. |
| [students](#students) | academic | 16 | Hồ sơ học viên; currentLevel là text legacy riêng với assessment. |
| [super_admins](#super-admins) | auth | 5 | Hồ sơ SUPER_ADMIN liên kết account. |
| [teacher_profile_events](#teacher-profile-events) | auth | 10 | Snapshot/audit thay đổi hồ sơ Teacher; scalar teacherId. |
| [teachers](#teachers) | auth | 12 | Hồ sơ chuyên môn, version và trạng thái làm việc Teacher. |
| [tuition](#tuition) | finance | 21 | Khoản phải thu gắn enrollment billing root, số thu cached/version. |
| [tuition_adjustments](#tuition-adjustments) | finance | 18 | Journal thay học phí/hạn, requestKey/version/reason. |

<a id="account-branch-events"></a>

## account_branch_events

Lịch sử phân công/thu hồi/đổi chi nhánh chính. Owner logic: **auth**. Engine InnoDB; collation utf8mb4_unicode_ci.

| Cột | Kiểu SQL | Nullable | Default | Key / extra | Ý nghĩa / lưu ý |
|---|---|---|---|---|---|
| id | bigint | NO | ∅ | PRI; auto_increment | Ánh xạ AccountBranchEvent.id. |
| account_id | bigint | NO | ∅ | MUL | ID có FK SQL thực tế, xem danh sách bên dưới. |
| branch_id | bigint | NO | ∅ | — | ID scalar/logic; không có FK SQL trong schema này. |
| action | varchar(30) | NO | ∅ | — | Ánh xạ AccountBranchEvent.action. |
| occurred_at | datetime(6) | NO | ∅ | — | Ngày/giờ nghiệp vụ; timezone ứng dụng Asia/Ho_Chi_Minh. |
| performed_by | bigint | YES | ∅ | — | Ánh xạ AccountBranchEvent.performedBy. |

**Index/unique:**

- idx_branch_event_account_time: INDEX (account_id, occurred_at).
- PRIMARY: UNIQUE (id).

**FK SQL:**

- fk_branch_event_account: (account_id) → accounts(id); UPDATE NO ACTION, DELETE NO ACTION.

<a id="account-branches"></a>

## account_branches

Phân công nhiều chi nhánh; primary là lựa chọn làm việc, không thay tập quyền. Owner logic: **auth**. Engine InnoDB; collation utf8mb4_unicode_ci.

| Cột | Kiểu SQL | Nullable | Default | Key / extra | Ý nghĩa / lưu ý |
|---|---|---|---|---|---|
| id | bigint | NO | ∅ | PRI; auto_increment | Ánh xạ AccountBranch.id. |
| account_id | bigint | NO | ∅ | MUL | ID có FK SQL thực tế, xem danh sách bên dưới. |
| branch_id | bigint | NO | ∅ | MUL | ID scalar/logic; không có FK SQL trong schema này. |
| status | bit(1) | NO | ∅ | — | Ánh xạ AccountBranch.status. |
| is_primary | bit(1) | NO | ∅ | — | Ánh xạ AccountBranch.primary. |
| assigned_at | datetime(6) | NO | ∅ | — | Ngày/giờ nghiệp vụ; timezone ứng dụng Asia/Ho_Chi_Minh. |
| assigned_by | bigint | YES | ∅ | — | Ánh xạ AccountBranch.assignedBy. |
| revoked_at | datetime(6) | YES | ∅ | — | Ngày/giờ nghiệp vụ; timezone ứng dụng Asia/Ho_Chi_Minh. |

**Index/unique:**

- idx_membership_branch_status: INDEX (branch_id, status).
- PRIMARY: UNIQUE (id).
- uk_account_branch: UNIQUE (account_id, branch_id).

**FK SQL:**

- fk_membership_account: (account_id) → accounts(id); UPDATE NO ACTION, DELETE NO ACTION.

<a id="account-security-events"></a>

## account_security_events

Audit bảo mật, reset/đổi mật khẩu/thu hồi phiên. Owner logic: **auth**. Engine InnoDB; collation utf8mb4_unicode_ci.

| Cột | Kiểu SQL | Nullable | Default | Key / extra | Ý nghĩa / lưu ý |
|---|---|---|---|---|---|
| id | bigint | NO | ∅ | PRI; auto_increment | Ánh xạ AccountSecurityEvent.id. |
| account_id | bigint | NO | ∅ | MUL | ID có FK SQL thực tế, xem danh sách bên dưới. |
| actor_id | bigint | NO | ∅ | — | ID scalar/logic; không có FK SQL trong schema này. |
| action | varchar(40) | NO | ∅ | — | Ánh xạ AccountSecurityEvent.action. |
| occurred_at | datetime(6) | NO | ∅ | — | Ngày/giờ nghiệp vụ; timezone ứng dụng Asia/Ho_Chi_Minh. |
| reason | varchar(255) | YES | ∅ | — | Lý do nghiệp vụ/audit; validation tùy endpoint. |

**Index/unique:**

- idx_security_event_account_time: INDEX (account_id, occurred_at).
- PRIMARY: UNIQUE (id).

**FK SQL:**

- fk_security_event_account: (account_id) → accounts(id); UPDATE NO ACTION, DELETE NO ACTION.

<a id="accounts"></a>

## accounts

Tài khoản, role, trạng thái đăng nhập và phiên. Owner logic: **auth**. Engine InnoDB; collation utf8mb4_unicode_ci.

| Cột | Kiểu SQL | Nullable | Default | Key / extra | Ý nghĩa / lưu ý |
|---|---|---|---|---|---|
| id | bigint | NO | ∅ | PRI; auto_increment | Ánh xạ Account.id. |
| created_at | datetime(6) | YES | ∅ | — | Ngày/giờ nghiệp vụ; timezone ứng dụng Asia/Ho_Chi_Minh. |
| last_login | datetime(6) | YES | ∅ | — | Ánh xạ Account.lastLogin. |
| password | varchar(255) | NO | ∅ | — | Hash BCrypt; không mật khẩu rõ. |
| role | enum('ADMIN','STAFF','SUPER_ADMIN','TEACHER') | NO | ∅ | — | Ánh xạ Account.role. |
| status | bit(1) | NO | ∅ | — | Ánh xạ Account.status. |
| username | varchar(50) | NO | ∅ | UNI | Ánh xạ Account.username. |
| branch_id | bigint | YES | ∅ | — | ID scalar/logic; không có FK SQL trong schema này. |
| session_version | bigint | NO | 0 | — | Ánh xạ Account.sessionVersion. |
| must_change_password | bit(1) | NO | b'0' | — | Ánh xạ Account.mustChangePassword. |
| password_changed_at | datetime(6) | YES | ∅ | — | Ngày/giờ nghiệp vụ; timezone ứng dụng Asia/Ho_Chi_Minh. |

**Index/unique:**

- PRIMARY: UNIQUE (id).
- UKk8h1bgqoplx0rkngj01pm1rgp: UNIQUE (username).

**FK SQL:**

- Không có FK SQL; không suy ra FK từ tên cột hoặc ID qua API.

<a id="admins"></a>

## admins

Hồ sơ ADMIN liên kết account. Owner logic: **auth**. Engine InnoDB; collation utf8mb4_unicode_ci.

| Cột | Kiểu SQL | Nullable | Default | Key / extra | Ý nghĩa / lưu ý |
|---|---|---|---|---|---|
| id | bigint | NO | ∅ | PRI; auto_increment | Ánh xạ Admin.id. |
| email | varchar(100) | YES | ∅ | — | Ánh xạ Admin.email. |
| full_name | varchar(100) | NO | ∅ | — | Ánh xạ Admin.fullName. |
| phone | varchar(15) | YES | ∅ | — | Ánh xạ Admin.phone. |
| account_id | bigint | NO | ∅ | UNI | ID có FK SQL thực tế, xem danh sách bên dưới. |
| hire_date | date | YES | ∅ | — | Ngày/giờ nghiệp vụ; timezone ứng dụng Asia/Ho_Chi_Minh. |
| work_status | varchar(50) | YES | ∅ | — | Ánh xạ Admin.workStatus. |

**Index/unique:**

- PRIMARY: UNIQUE (id).
- UKd1mh9asac0y4phlp5y7m02ben: UNIQUE (account_id).

**FK SQL:**

- FKji7l5mi8tqv73veb5cd90hupx: (account_id) → accounts(id); UPDATE NO ACTION, DELETE NO ACTION.

<a id="assessment-events"></a>

## assessment_events

Audit assessment trước/sau; không thay chứng cứ assessment. Owner logic: **academic**. Engine InnoDB; collation utf8mb4_0900_ai_ci.

| Cột | Kiểu SQL | Nullable | Default | Key / extra | Ý nghĩa / lưu ý |
|---|---|---|---|---|---|
| id | bigint | NO | ∅ | PRI; auto_increment | Ánh xạ AssessmentEvent.id. |
| assessment_id | bigint | NO | ∅ | MUL | ID scalar/logic; không có FK SQL trong schema này. |
| action | varchar(255) | NO | ∅ | — | Ánh xạ AssessmentEvent.action. |
| before_state | longtext | NO | ∅ | — | Ảnh chụp/audit; không dùng dữ liệu hiện tại ghi đè lịch sử. |
| after_state | longtext | NO | ∅ | — | Ảnh chụp/audit; không dùng dữ liệu hiện tại ghi đè lịch sử. |
| reason | varchar(500) | YES | ∅ | — | Lý do nghiệp vụ/audit; validation tùy endpoint. |
| actor_id | bigint | NO | ∅ | — | ID scalar/logic; không có FK SQL trong schema này. |
| actor_name | varchar(255) | NO | ∅ | — | Ánh xạ AssessmentEvent.actorName. |
| occurred_at | datetime(6) | NO | ∅ | — | Ngày/giờ nghiệp vụ; timezone ứng dụng Asia/Ho_Chi_Minh. |
| assessment_version | bigint | NO | ∅ | — | Ánh xạ AssessmentEvent.assessmentVersion. |

**Index/unique:**

- idx_assessment_event: INDEX (assessment_id, id).
- PRIMARY: UNIQUE (id).

**FK SQL:**

- Không có FK SQL; không suy ra FK từ tên cột hoặc ID qua API.

<a id="attendance-changes"></a>

## attendance_changes

Audit sửa điểm danh; giữ before/after và người thực hiện. Owner logic: **academic**. Engine InnoDB; collation utf8mb4_unicode_ci.

| Cột | Kiểu SQL | Nullable | Default | Key / extra | Ý nghĩa / lưu ý |
|---|---|---|---|---|---|
| id | bigint | NO | ∅ | PRI; auto_increment | Xem đặc tả bảng và entity/collection tương ứng. |
| session_id | bigint | NO | ∅ | MUL | ID có FK SQL thực tế, xem danh sách bên dưới. |
| enrollment_id | bigint | NO | ∅ | — | ID scalar/logic; không có FK SQL trong schema này. |
| student_name | varchar(255) | NO | ∅ | — | Tên chụp theo nghiệp vụ, không FK định danh. |
| old_status | varchar(255) | YES | ∅ | — | Ánh xạ AttendanceChange.oldStatus. |
| new_status | varchar(255) | NO | ∅ | — | Ánh xạ AttendanceChange.newStatus. |
| old_permission | bit(1) | YES | ∅ | — | Ánh xạ AttendanceChange.oldPermission. |
| new_permission | bit(1) | NO | ∅ | — | Ánh xạ AttendanceChange.newPermission. |
| previous_recorded_by | bigint | YES | ∅ | — | Ánh xạ AttendanceChange.previousRecordedBy. |
| previous_recorded_by_name | varchar(255) | YES | ∅ | — | Tên chụp theo nghiệp vụ, không FK định danh. |
| changed_by | bigint | NO | ∅ | — | Ánh xạ AttendanceChange.changedBy. |
| changed_by_name | varchar(255) | NO | ∅ | — | Tên chụp theo nghiệp vụ, không FK định danh. |
| changed_at | datetime(6) | NO | ∅ | — | Ngày/giờ nghiệp vụ; timezone ứng dụng Asia/Ho_Chi_Minh. |
| reason | varchar(500) | NO | ∅ | — | Lý do nghiệp vụ/audit; validation tùy endpoint. |

**Index/unique:**

- idx_attendance_change_session: INDEX (session_id, id).
- PRIMARY: UNIQUE (id).

**FK SQL:**

- fk_attendance_change_session: (session_id) → class_sessions(id); UPDATE NO ACTION, DELETE NO ACTION.

<a id="attendances"></a>

## attendances

Điểm danh theo enrollment/buổi; legacy class_session_id nullable. Owner logic: **academic**. Engine InnoDB; collation utf8mb4_unicode_ci.

| Cột | Kiểu SQL | Nullable | Default | Key / extra | Ý nghĩa / lưu ý |
|---|---|---|---|---|---|
| id | bigint | NO | ∅ | PRI; auto_increment | Xem đặc tả bảng và entity/collection tương ứng. |
| has_permission | bit(1) | NO | ∅ | — | Ánh xạ Attendance.hasPermission. |
| recorded_by | bigint | NO | ∅ | — | Ánh xạ Attendance.recordedBy. |
| recorded_by_name | varchar(255) | NO | ∅ | — | Tên chụp theo nghiệp vụ, không FK định danh. |
| session_date | date | NO | ∅ | — | Ngày/giờ nghiệp vụ; timezone ứng dụng Asia/Ho_Chi_Minh. |
| status | enum('ABSENT','LATE','PRESENT') | NO | ∅ | — | Ánh xạ Attendance.status. |
| enrollment_id | bigint | NO | ∅ | MUL | ID có FK SQL thực tế, xem danh sách bên dưới. |
| class_session_id | bigint | YES | ∅ | MUL | ID có FK SQL thực tế, xem danh sách bên dưới. |

**Index/unique:**

- fk_attendance_session: INDEX (class_session_id).
- PRIMARY: UNIQUE (id).
- uk_attendance_enrollment_date: UNIQUE (enrollment_id, session_date).

**FK SQL:**

- FK7j92ropr8c7lg7jyrwbkffisy: (enrollment_id) → enrollments(id); UPDATE NO ACTION, DELETE NO ACTION.
- fk_attendance_session: (class_session_id) → class_sessions(id); UPDATE NO ACTION, DELETE NO ACTION.

<a id="branches"></a>

## branches

Chi nhánh Academic sở hữu; Auth chỉ tham chiếu ID. Owner logic: **academic**. Engine InnoDB; collation utf8mb4_unicode_ci.

| Cột | Kiểu SQL | Nullable | Default | Key / extra | Ý nghĩa / lưu ý |
|---|---|---|---|---|---|
| id | bigint | NO | ∅ | PRI; auto_increment | Xem đặc tả bảng và entity/collection tương ứng. |
| address | varchar(255) | YES | ∅ | — | Ánh xạ Branch.address. |
| code | varchar(255) | NO | ∅ | UNI | Ánh xạ Branch.code. |
| created_at | datetime(6) | NO | ∅ | — | Ngày/giờ nghiệp vụ; timezone ứng dụng Asia/Ho_Chi_Minh. |
| name | varchar(255) | NO | ∅ | — | Ánh xạ Branch.name. |
| phone | varchar(255) | NO | ∅ | — | Ánh xạ Branch.phone. |
| status | enum('ACTIVE','INACTIVE') | NO | ∅ | — | Ánh xạ Branch.status. |

**Index/unique:**

- PRIMARY: UNIQUE (id).
- uk_branch_code: UNIQUE (code).

**FK SQL:**

- Không có FK SQL; không suy ra FK từ tên cột hoặc ID qua API.

<a id="class-admission-events"></a>

## class_admission_events

Lịch sử áp dụng policy khóa cho lớp. Owner logic: **academic**. Engine InnoDB; collation utf8mb4_0900_ai_ci.

| Cột | Kiểu SQL | Nullable | Default | Key / extra | Ý nghĩa / lưu ý |
|---|---|---|---|---|---|
| id | bigint | NO | ∅ | PRI; auto_increment | Ánh xạ ClassAdmissionEvent.id. |
| class_id | bigint | NO | ∅ | MUL | ID scalar/logic; không có FK SQL trong schema này. |
| old_policy_id | bigint | YES | ∅ | — | ID scalar/logic; không có FK SQL trong schema này. |
| new_policy_id | bigint | YES | ∅ | — | ID scalar/logic; không có FK SQL trong schema này. |
| revision | bigint | NO | ∅ | — | Revision/epoch do service quản lý; không tự coi là @Version. |
| actor_id | bigint | NO | ∅ | — | ID scalar/logic; không có FK SQL trong schema này. |
| actor_name | varchar(255) | NO | ∅ | — | Ánh xạ ClassAdmissionEvent.actorName. |
| occurred_at | datetime(6) | NO | ∅ | — | Ngày/giờ nghiệp vụ; timezone ứng dụng Asia/Ho_Chi_Minh. |
| reason | varchar(500) | NO | ∅ | — | Lý do nghiệp vụ/audit; validation tùy endpoint. |

**Index/unique:**

- idx_class_admission_event: INDEX (class_id, id).
- PRIMARY: UNIQUE (id).

**FK SQL:**

- Không có FK SQL; không suy ra FK từ tên cột hoặc ID qua API.

<a id="class-closure-events"></a>

## class_closure_events

Intent/audit đóng mở lớp theo revision. Owner logic: **academic**. Engine InnoDB; collation utf8mb4_0900_ai_ci.

| Cột | Kiểu SQL | Nullable | Default | Key / extra | Ý nghĩa / lưu ý |
|---|---|---|---|---|---|
| request_id | varchar(36) | NO | ∅ | PRI | Idempotency intent theo scope/index; giữ khi retry cùng ý định. |
| class_id | bigint | NO | ∅ | MUL | ID scalar/logic; không có FK SQL trong schema này. |
| expected_revision | bigint | NO | ∅ | — | Revision/epoch do service quản lý; không tự coi là @Version. |
| revision | bigint | NO | ∅ | — | Revision/epoch do service quản lý; không tự coi là @Version. |
| closed | bit(1) | NO | ∅ | — | Ánh xạ ClassClosureEvent.closed. |
| actor_name | varchar(255) | NO | ∅ | — | Ánh xạ ClassClosureEvent.actorName. |
| actor_id | bigint | NO | ∅ | — | ID scalar/logic; không có FK SQL trong schema này. |
| changed_at | datetime(6) | NO | ∅ | — | Ngày/giờ nghiệp vụ; timezone ứng dụng Asia/Ho_Chi_Minh. |
| reason | varchar(500) | NO | ∅ | — | Lý do nghiệp vụ/audit; validation tùy endpoint. |

**Index/unique:**

- PRIMARY: UNIQUE (request_id).
- uk_closure_revision: UNIQUE (class_id, revision).

**FK SQL:**

- Không có FK SQL; không suy ra FK từ tên cột hoặc ID qua API.

<a id="class-closures"></a>

## class_closures

Trạng thái đóng/mở được đối soát, manual revision và epoch bằng chứng. Owner logic: **academic**. Engine InnoDB; collation utf8mb4_0900_ai_ci.

| Cột | Kiểu SQL | Nullable | Default | Key / extra | Ý nghĩa / lưu ý |
|---|---|---|---|---|---|
| class_id | bigint | NO | ∅ | PRI | ID scalar/logic; không có FK SQL trong schema này. |
| revision | bigint | NO | ∅ | — | Revision/epoch do service quản lý; không tự coi là @Version. |
| closed | bit(1) | NO | ∅ | — | Ánh xạ ClassClosure.closed. |
| actor_name | varchar(255) | NO | ∅ | — | Ánh xạ ClassClosure.actorName. |
| actor_id | bigint | NO | ∅ | — | ID scalar/logic; không có FK SQL trong schema này. |
| changed_at | datetime(6) | NO | ∅ | — | Ngày/giờ nghiệp vụ; timezone ứng dụng Asia/Ho_Chi_Minh. |
| reason | varchar(500) | NO | ∅ | — | Lý do nghiệp vụ/audit; validation tùy endpoint. |
| result_revision | bigint | NO | 0 | — | Revision/epoch do service quản lý; không tự coi là @Version. |
| source_revision | bigint | NO | 0 | — | Revision/epoch do service quản lý; không tự coi là @Version. |

**Index/unique:**

- PRIMARY: UNIQUE (class_id).

**FK SQL:**

- Không có FK SQL; không suy ra FK từ tên cột hoặc ID qua API.

<a id="class-duration-plans"></a>

## class_duration_plans

Lịch sử kế hoạch phút lớp, snapshot nguồn khóa/custom. Owner logic: **academic**. Engine InnoDB; collation utf8mb4_0900_ai_ci.

| Cột | Kiểu SQL | Nullable | Default | Key / extra | Ý nghĩa / lưu ý |
|---|---|---|---|---|---|
| id | bigint | NO | ∅ | PRI; auto_increment | Ánh xạ ClassDurationPlan.id. |
| class_id | bigint | NO | ∅ | MUL | ID có FK SQL thực tế, xem danh sách bên dưới. |
| revision | bigint | NO | ∅ | — | Revision/epoch do service quản lý; không tự coi là @Version. |
| expected_minutes | int | YES | ∅ | — | Số phút xác minh; null = chưa xác minh, không0. |
| course_id | bigint | NO | ∅ | — | ID scalar/logic; không có FK SQL trong schema này. |
| course_revision | bigint | YES | ∅ | — | Revision/epoch do service quản lý; không tự coi là @Version. |
| source | varchar(20) | NO | ∅ | — | Ánh xạ ClassDurationPlan.source. |
| actor_id | bigint | NO | ∅ | — | ID scalar/logic; không có FK SQL trong schema này. |
| actor_name | varchar(255) | NO | ∅ | — | Ánh xạ ClassDurationPlan.actorName. |
| occurred_at | datetime(6) | NO | ∅ | — | Ngày/giờ nghiệp vụ; timezone ứng dụng Asia/Ho_Chi_Minh. |
| reason | varchar(500) | NO | ∅ | — | Lý do nghiệp vụ/audit; validation tùy endpoint. |

**Index/unique:**

- PRIMARY: UNIQUE (id).
- uk_class_duration_revision: UNIQUE (class_id, revision).

**FK SQL:**

- fk_duration_class: (class_id) → school_classes(id); UPDATE NO ACTION, DELETE NO ACTION.

<a id="class-result-barriers"></a>

## class_result_barriers

Metadata phối hợp hẹp Academic/Teaching trong schema dùng chung. Owner logic: **shared-coordination**. Engine InnoDB; collation utf8mb4_0900_ai_ci.

| Cột | Kiểu SQL | Nullable | Default | Key / extra | Ý nghĩa / lưu ý |
|---|---|---|---|---|---|
| class_id | bigint | NO | ∅ | PRI | ID scalar/logic; không có FK SQL trong schema này. |
| branch_id | bigint | NO | ∅ | — | ID scalar/logic; không có FK SQL trong schema này. |
| token | varchar(36) | NO | ∅ | — | UUID kiểm cùng hạ tầng khóa; không JWT đăng nhập. |
| source_revision | bigint | NO | 0 | — | Revision/epoch do service quản lý; không tự coi là @Version. |
| result_revision | bigint | NO | 0 | — | Revision/epoch do service quản lý; không tự coi là @Version. |
| confirmed_count | bigint | NO | 0 | — | Ánh xạ ResultBarrier.confirmedCount. |

**Index/unique:**

- PRIMARY: UNIQUE (class_id).

**FK SQL:**

- Không có FK SQL; không suy ra FK từ tên cột hoặc ID qua API.

<a id="class-sessions"></a>

## class_sessions

Buổi thực tế: trạng thái/ngày/giờ/actual/planned Teacher, liên kết nghỉ/bù. Owner logic: **academic**. Engine InnoDB; collation utf8mb4_unicode_ci.

| Cột | Kiểu SQL | Nullable | Default | Key / extra | Ý nghĩa / lưu ý |
|---|---|---|---|---|---|
| id | bigint | NO | ∅ | PRI; auto_increment | Xem đặc tả bảng và entity/collection tương ứng. |
| class_id | bigint | NO | ∅ | MUL | ID có FK SQL thực tế, xem danh sách bên dưới. |
| session_date | date | NO | ∅ | MUL | Ngày/giờ nghiệp vụ; timezone ứng dụng Asia/Ho_Chi_Minh. |
| start_time | time(6) | YES | ∅ | — | Ánh xạ ClassSession.startTime. |
| end_time | time(6) | YES | ∅ | — | Ánh xạ ClassSession.endTime. |
| room_id | bigint | YES | ∅ | — | ID scalar/logic; không có FK SQL trong schema này. |
| room_name | varchar(255) | YES | ∅ | — | Ánh xạ ClassSession.roomName. |
| planned_teacher_id | bigint | YES | ∅ | — | ID scalar/logic; không có FK SQL trong schema này. |
| planned_teacher_name | varchar(255) | YES | ∅ | — | Tên chụp theo nghiệp vụ, không FK định danh. |
| teacher_id | bigint | YES | ∅ | — | ID scalar/logic; không có FK SQL trong schema này. |
| teacher_name | varchar(255) | YES | ∅ | — | Tên chụp theo nghiệp vụ, không FK định danh. |
| status | varchar(20) | NO | ∅ | — | Ánh xạ ClassSession.status. |
| kind | varchar(20) | NO | ∅ | — | Ánh xạ ClassSession.kind. |
| source_date | date | YES | ∅ | — | Ngày/giờ nghiệp vụ; timezone ứng dụng Asia/Ho_Chi_Minh. |
| source_schedule_id | bigint | YES | ∅ | — | ID scalar/logic; không có FK SQL trong schema này. |
| makeup_for_id | bigint | YES | ∅ | UNI | ID scalar/logic; không có FK SQL trong schema này. |
| makeup_session_id | bigint | YES | ∅ | — | ID scalar/logic; không có FK SQL trong schema này. |
| customized | bit(1) | NO | b'0' | — | Ánh xạ ClassSession.customized. |
| reason | varchar(500) | YES | ∅ | — | Lý do nghiệp vụ/audit; validation tùy endpoint. |
| version | bigint | NO | 0 | — | @Version JPA: kiểm ghi đè optimistic. |
| created_at | datetime(6) | NO | ∅ | — | Ngày/giờ nghiệp vụ; timezone ứng dụng Asia/Ho_Chi_Minh. |
| updated_at | datetime(6) | NO | ∅ | — | Ngày/giờ nghiệp vụ; timezone ứng dụng Asia/Ho_Chi_Minh. |

**Index/unique:**

- idx_session_date_room: INDEX (session_date, room_id, status).
- idx_session_date_teacher: INDEX (session_date, teacher_id, status).
- PRIMARY: UNIQUE (id).
- uk_session_makeup: UNIQUE (makeup_for_id).
- uk_session_source: UNIQUE (class_id, source_date).

**FK SQL:**

- fk_session_class: (class_id) → school_classes(id); UPDATE NO ACTION, DELETE NO ACTION.

<a id="course-admission-levels"></a>

## course_admission_levels

ElementCollection snapshot tập mức đầu vào; không phải entity độc lập. Owner logic: **academic**. Engine InnoDB; collation utf8mb4_0900_ai_ci.

| Cột | Kiểu SQL | Nullable | Default | Key / extra | Ý nghĩa / lưu ý |
|---|---|---|---|---|---|
| policy_id | bigint | NO | ∅ | PRI | ID có FK SQL thực tế, xem danh sách bên dưới. |
| level_id | bigint | NO | ∅ | PRI | ID scalar/logic; không có FK SQL trong schema này. |
| code | varchar(50) | NO | ∅ | — | Xem đặc tả bảng và entity/collection tương ứng. |
| name | varchar(255) | NO | ∅ | — | Xem đặc tả bảng và entity/collection tương ứng. |

**Index/unique:**

- uk_admission_level: UNIQUE (policy_id, level_id).

**FK SQL:**

- fk_admission_level_policy: (policy_id) → course_admission_policies(id); UPDATE NO ACTION, DELETE NO ACTION.

<a id="course-admission-policies"></a>

## course_admission_policies

Phiên bản chính sách đầu vào và mục tiêu đầu ra của khóa. Owner logic: **academic**. Engine InnoDB; collation utf8mb4_0900_ai_ci.

| Cột | Kiểu SQL | Nullable | Default | Key / extra | Ý nghĩa / lưu ý |
|---|---|---|---|---|---|
| id | bigint | NO | ∅ | PRI; auto_increment | Ánh xạ CourseAdmissionPolicy.id. |
| course_id | bigint | NO | ∅ | MUL | ID có FK SQL thực tế, xem danh sách bên dưới. |
| revision | bigint | NO | ∅ | — | Revision/epoch do service quản lý; không tự coi là @Version. |
| mode | varchar(40) | NO | ∅ | — | Ánh xạ CourseAdmissionPolicy.mode. |
| program_id | bigint | NO | ∅ | — | ID scalar/logic; không có FK SQL trong schema này. |
| program_code | varchar(50) | NO | ∅ | — | Ánh xạ CourseAdmissionPolicy.programCode. |
| program_name | varchar(255) | NO | ∅ | — | Ánh xạ CourseAdmissionPolicy.programName. |
| output_level_id | bigint | NO | ∅ | — | ID scalar/logic; không có FK SQL trong schema này. |
| output_level_code | varchar(50) | NO | ∅ | — | Ánh xạ CourseAdmissionPolicy.outputLevelCode. |
| output_level_name | varchar(255) | NO | ∅ | — | Ánh xạ CourseAdmissionPolicy.outputLevelName. |
| confirmed_by | bigint | NO | ∅ | — | Ánh xạ CourseAdmissionPolicy.confirmedBy. |
| confirmed_by_name | varchar(255) | NO | ∅ | — | Tên chụp theo nghiệp vụ, không FK định danh. |
| confirmed_at | datetime(6) | NO | ∅ | — | Ngày/giờ nghiệp vụ; timezone ứng dụng Asia/Ho_Chi_Minh. |
| reason | varchar(500) | NO | ∅ | — | Lý do nghiệp vụ/audit; validation tùy endpoint. |

**Index/unique:**

- PRIMARY: UNIQUE (id).
- uk_course_admission_revision: UNIQUE (course_id, revision).

**FK SQL:**

- fk_admission_course: (course_id) → courses(id); UPDATE NO ACTION, DELETE NO ACTION.

<a id="course-duration-plans"></a>

## course_duration_plans

Lịch sử xác nhận số phút khóa. Owner logic: **academic**. Engine InnoDB; collation utf8mb4_0900_ai_ci.

| Cột | Kiểu SQL | Nullable | Default | Key / extra | Ý nghĩa / lưu ý |
|---|---|---|---|---|---|
| id | bigint | NO | ∅ | PRI; auto_increment | Ánh xạ CourseDurationPlan.id. |
| course_id | bigint | NO | ∅ | MUL | ID có FK SQL thực tế, xem danh sách bên dưới. |
| revision | bigint | NO | ∅ | — | Revision/epoch do service quản lý; không tự coi là @Version. |
| expected_minutes | int | NO | ∅ | — | Số phút xác minh; null = chưa xác minh, không0. |
| actor_id | bigint | NO | ∅ | — | ID scalar/logic; không có FK SQL trong schema này. |
| actor_name | varchar(255) | NO | ∅ | — | Ánh xạ CourseDurationPlan.actorName. |
| occurred_at | datetime(6) | NO | ∅ | — | Ngày/giờ nghiệp vụ; timezone ứng dụng Asia/Ho_Chi_Minh. |
| reason | varchar(500) | NO | ∅ | — | Lý do nghiệp vụ/audit; validation tùy endpoint. |

**Index/unique:**

- PRIMARY: UNIQUE (id).
- uk_course_duration_revision: UNIQUE (course_id, revision).

**FK SQL:**

- fk_duration_course: (course_id) → courses(id); UPDATE NO ACTION, DELETE NO ACTION.

<a id="courses"></a>

## courses

Khóa: giá, nội dung, trường duration cũ và expectedMinutes đã xác minh. Owner logic: **academic**. Engine InnoDB; collation utf8mb4_unicode_ci.

| Cột | Kiểu SQL | Nullable | Default | Key / extra | Ý nghĩa / lưu ý |
|---|---|---|---|---|---|
| id | bigint | NO | ∅ | PRI; auto_increment | Xem đặc tả bảng và entity/collection tương ứng. |
| branch_id | bigint | YES | ∅ | — | ID scalar/logic; không có FK SQL trong schema này. |
| code | varchar(255) | NO | ∅ | MUL | Ánh xạ Course.code. |
| content | text | YES | ∅ | — | Ánh xạ Course.content. |
| duration | int | YES | ∅ | — | Giá trị legacy; không tự coi là giờ/buổi/phút. |
| entry_requirement | text | YES | ∅ | — | Ánh xạ Course.entryRequirement. |
| level | varchar(255) | YES | ∅ | — | Ánh xạ Course.level. |
| max_students | int | NO | ∅ | — | Ánh xạ Course.maxStudents. |
| name | varchar(255) | NO | ∅ | — | Ánh xạ Course.name. |
| scope_key | bigint | YES | ∅ | STORED GENERATED | Cột generated MySQL; coalesce(`branch_id`,0) |
| status | enum('CLOSED','OPEN') | NO | ∅ | — | Ánh xạ Course.status. |
| tuition_fee | decimal(15,2) | NO | ∅ | — | Ánh xạ Course.tuitionFee. |
| type | varchar(255) | YES | ∅ | — | Ánh xạ Course.type. |
| expected_minutes | int | YES | ∅ | — | Số phút xác minh; null = chưa xác minh, không0. |
| duration_revision | bigint | NO | 0 | — | Revision/epoch do service quản lý; không tự coi là @Version. |

**Index/unique:**

- PRIMARY: UNIQUE (id).
- uk_course_code_scope: UNIQUE (code, scope_key).

**FK SQL:**

- Không có FK SQL; không suy ra FK từ tên cột hoặc ID qua API.

<a id="document-events"></a>

## document_events

Lịch sử duyệt/gửi lại và snapshot các revision/file cũ. Owner logic: **teaching**. Engine InnoDB; collation utf8mb4_unicode_ci.

| Cột | Kiểu SQL | Nullable | Default | Key / extra | Ý nghĩa / lưu ý |
|---|---|---|---|---|---|
| id | bigint | NO | ∅ | PRI; auto_increment | Xem đặc tả bảng và entity/collection tương ứng. |
| document_id | bigint | NO | ∅ | MUL | ID scalar/logic; không có FK SQL trong schema này. |
| revision | int | NO | ∅ | — | Revision/epoch do service quản lý; không tự coi là @Version. |
| action | varchar(255) | NO | ∅ | — | Ánh xạ DocumentEvent.action. |
| status | varchar(255) | NO | ∅ | — | Ánh xạ DocumentEvent.status. |
| storage_key | varchar(255) | NO | ∅ | — | Khóa file nội bộ; không đường dẫn public từ input. |
| title | varchar(255) | NO | ∅ | — | Ánh xạ DocumentEvent.title. |
| description | varchar(1000) | YES | ∅ | — | Ánh xạ DocumentEvent.description. |
| filename | varchar(255) | YES | ∅ | — | Ánh xạ DocumentEvent.filename. |
| file_size | bigint | YES | ∅ | — | Ánh xạ DocumentEvent.fileSize. |
| reason | varchar(500) | YES | ∅ | — | Lý do nghiệp vụ/audit; validation tùy endpoint. |
| actor_id | bigint | YES | ∅ | — | ID scalar/logic; không có FK SQL trong schema này. |
| actor_name | varchar(255) | YES | ∅ | — | Ánh xạ DocumentEvent.actorName. |
| occurred_at | datetime(6) | NO | ∅ | — | Ngày/giờ nghiệp vụ; timezone ứng dụng Asia/Ho_Chi_Minh. |

**Index/unique:**

- idx_document_event: INDEX (document_id, revision, id).
- PRIMARY: UNIQUE (id).

**FK SQL:**

- Không có FK SQL; không suy ra FK từ tên cột hoặc ID qua API.

<a id="documents"></a>

## documents

Metadata file giảng dạy; file vật lý bên ngoài MySQL. Owner logic: **teaching**. Engine InnoDB; collation utf8mb4_unicode_ci.

| Cột | Kiểu SQL | Nullable | Default | Key / extra | Ý nghĩa / lưu ý |
|---|---|---|---|---|---|
| id | bigint | NO | ∅ | PRI; auto_increment | Xem đặc tả bảng và entity/collection tương ứng. |
| branch_id | bigint | NO | ∅ | MUL | ID scalar/logic; không có FK SQL trong schema này. |
| class_id | bigint | YES | ∅ | — | ID scalar/logic; không có FK SQL trong schema này. |
| course_id | bigint | YES | ∅ | — | ID scalar/logic; không có FK SQL trong schema này. |
| description | varchar(1000) | YES | ∅ | — | Ánh xạ Document.description. |
| file_size | bigint | YES | ∅ | — | Ánh xạ Document.fileSize. |
| original_filename | varchar(255) | YES | ∅ | — | Ánh xạ Document.originalFilename. |
| reject_reason | varchar(500) | YES | ∅ | — | Lý do nghiệp vụ/audit; validation tùy endpoint. |
| reviewed_at | datetime(6) | YES | ∅ | — | Ngày/giờ nghiệp vụ; timezone ứng dụng Asia/Ho_Chi_Minh. |
| reviewed_by | bigint | YES | ∅ | — | Ánh xạ Document.reviewedBy. |
| reviewed_by_name | varchar(255) | YES | ∅ | — | Tên chụp theo nghiệp vụ, không FK định danh. |
| status | enum('APPROVED','PENDING','REJECTED') | NO | ∅ | — | Ánh xạ Document.status. |
| storage_key | varchar(255) | NO | ∅ | — | Khóa file nội bộ; không đường dẫn public từ input. |
| title | varchar(255) | NO | ∅ | — | Ánh xạ Document.title. |
| uploaded_at | datetime(6) | NO | ∅ | — | Ngày/giờ nghiệp vụ; timezone ứng dụng Asia/Ho_Chi_Minh. |
| uploaded_by | bigint | NO | ∅ | — | Ánh xạ Document.uploadedBy. |
| uploaded_by_name | varchar(255) | YES | ∅ | — | Tên chụp theo nghiệp vụ, không FK định danh. |
| version | bigint | NO | ∅ | — | @Version JPA: kiểm ghi đè optimistic. |
| revision | int | NO | 1 | — | Revision/epoch do service quản lý; không tự coi là @Version. |
| last_submitted_at | datetime(6) | YES | ∅ | — | Ngày/giờ nghiệp vụ; timezone ứng dụng Asia/Ho_Chi_Minh. |

**Index/unique:**

- idx_document_branch_status: INDEX (branch_id, status).
- PRIMARY: UNIQUE (id).

**FK SQL:**

- Không có FK SQL; không suy ra FK từ tên cột hoặc ID qua API.

<a id="enrollment-admissions"></a>

## enrollment_admissions

Snapshot căn cứ chấp nhận cho ENROLL/RESUME/TRANSFER. Owner logic: **academic**. Engine InnoDB; collation utf8mb4_0900_ai_ci.

| Cột | Kiểu SQL | Nullable | Default | Key / extra | Ý nghĩa / lưu ý |
|---|---|---|---|---|---|
| id | bigint | NO | ∅ | PRI; auto_increment | Ánh xạ EnrollmentAdmission.id. |
| enrollment_id | bigint | NO | ∅ | MUL | ID có FK SQL thực tế, xem danh sách bên dưới. |
| student_id | bigint | NO | ∅ | — | ID scalar/logic; không có FK SQL trong schema này. |
| class_id | bigint | NO | ∅ | — | ID scalar/logic; không có FK SQL trong schema này. |
| policy_id | bigint | YES | ∅ | — | ID scalar/logic; không có FK SQL trong schema này. |
| class_revision | bigint | NO | ∅ | — | Revision/epoch do service quản lý; không tự coi là @Version. |
| assessment_id | bigint | YES | ∅ | — | ID scalar/logic; không có FK SQL trong schema này. |
| assessment_version | bigint | YES | ∅ | — | Ánh xạ EnrollmentAdmission.assessmentVersion. |
| approval_id | bigint | YES | ∅ | MUL | ID có FK SQL thực tế, xem danh sách bên dưới. |
| reused_decision_id | bigint | YES | ∅ | — | ID scalar/logic; không có FK SQL trong schema này. |
| intent | varchar(20) | NO | ∅ | — | Ánh xạ EnrollmentAdmission.intent. |
| academic_status | varchar(40) | NO | ∅ | — | Ánh xạ EnrollmentAdmission.academicStatus. |
| fingerprint | varchar(64) | NO | ∅ | — | Hash căn cứ/context; không bằng chứng khảo sát thật. |
| policy_snapshot | longtext | NO | ∅ | — | Ảnh chụp/audit; không dùng dữ liệu hiện tại ghi đè lịch sử. |
| assessment_snapshot | longtext | NO | ∅ | — | Ảnh chụp/audit; không dùng dữ liệu hiện tại ghi đè lịch sử. |
| actor_id | bigint | NO | ∅ | — | ID scalar/logic; không có FK SQL trong schema này. |
| actor_name | varchar(255) | NO | ∅ | — | Ánh xạ EnrollmentAdmission.actorName. |
| accepted_at | datetime(6) | NO | ∅ | — | Ngày/giờ nghiệp vụ; timezone ứng dụng Asia/Ho_Chi_Minh. |

**Index/unique:**

- fk_enrollment_admission_approval: INDEX (approval_id).
- ix_enrollment_admission: INDEX (enrollment_id, id).
- PRIMARY: UNIQUE (id).

**FK SQL:**

- fk_enrollment_admission_approval: (approval_id) → placement_exceptions(id); UPDATE NO ACTION, DELETE NO ACTION.
- fk_enrollment_admission_enrollment: (enrollment_id) → enrollments(id); UPDATE NO ACTION, DELETE NO ACTION.

<a id="enrollment-periods"></a>

## enrollment_periods

Khoảng học [activeFrom, activeUntil); legacy có thể không có dòng. Owner logic: **academic**. Engine InnoDB; collation utf8mb4_unicode_ci.

| Cột | Kiểu SQL | Nullable | Default | Key / extra | Ý nghĩa / lưu ý |
|---|---|---|---|---|---|
| id | bigint | NO | ∅ | PRI; auto_increment | Xem đặc tả bảng và entity/collection tương ứng. |
| active_from | date | NO | ∅ | — | Mốc bắt đầu có bao gồm ngày này. |
| active_until | date | YES | ∅ | — | Mốc kết thúc không bao gồm ngày này; null = còn mở. |
| enrollment_id | bigint | NO | ∅ | MUL | ID có FK SQL thực tế, xem danh sách bên dưới. |

**Index/unique:**

- idx_period_enrollment: INDEX (enrollment_id).
- PRIMARY: UNIQUE (id).

**FK SQL:**

- FKer49xt5e0tsqnjq2drx2rg1ph: (enrollment_id) → enrollments(id); UPDATE NO ACTION, DELETE NO ACTION.

<a id="enrollments"></a>

## enrollments

Đăng ký học, trạng thái, giá snapshot, billing root và refs kết quả. Owner logic: **academic**. Engine InnoDB; collation utf8mb4_unicode_ci.

| Cột | Kiểu SQL | Nullable | Default | Key / extra | Ý nghĩa / lưu ý |
|---|---|---|---|---|---|
| id | bigint | NO | ∅ | PRI; auto_increment | Xem đặc tả bảng và entity/collection tương ứng. |
| enrolled_at | datetime(6) | NO | ∅ | — | Ngày/giờ nghiệp vụ; timezone ứng dụng Asia/Ho_Chi_Minh. |
| study_status | enum('COMPLETED','DROPPED','RESERVED','STUDYING','TRANSFERRED') | NO | ∅ | — | Trạng thái quá trình học, tách PASS/FAIL. |
| class_id | bigint | NO | ∅ | MUL | ID có FK SQL thực tế, xem danh sách bên dưới. |
| student_id | bigint | NO | ∅ | MUL | ID có FK SQL thực tế, xem danh sách bên dưới. |
| billing_enrollment_id | bigint | YES | ∅ | — | ID đăng ký tính phí gốc; giữ khi chuyển cùng khóa. |
| registered_by | bigint | YES | ∅ | — | Ánh xạ Enrollment.registeredBy. |
| registered_by_name | varchar(255) | YES | ∅ | — | Tên chụp theo nghiệp vụ, không FK định danh. |
| tuition_delivery | enum('CREATED','FAILED','PENDING') | YES | ∅ | — | Ánh xạ Enrollment.tuitionDelivery. |
| tuition_delivery_error | varchar(255) | YES | ∅ | — | Ánh xạ Enrollment.tuitionDeliveryError. |
| tuition_fee | decimal(15,2) | YES | ∅ | — | Ánh xạ Enrollment.tuitionFee. |
| version | bigint | NO | 0 | — | @Version JPA: kiểm ghi đè optimistic. |
| reserved_since | date | YES | ∅ | — | Ngày/giờ nghiệp vụ; timezone ứng dụng Asia/Ho_Chi_Minh. |
| expected_return_date | date | YES | ∅ | — | Ngày/giờ nghiệp vụ; timezone ứng dụng Asia/Ho_Chi_Minh. |
| completion_result_id | bigint | YES | ∅ | — | Ref current Teaching khi đồng bộ hoàn thành; nullable ở legacy. |
| completion_result_revision | bigint | YES | ∅ | — | Revision/epoch do service quản lý; không tự coi là @Version. |

**Index/unique:**

- idx_enrollment_class_status: INDEX (class_id, study_status).
- idx_enrollment_student_status: INDEX (student_id, study_status).
- PRIMARY: UNIQUE (id).

**FK SQL:**

- FK8kf1u1857xgo56xbfmnif2c51: (student_id) → students(id); UPDATE NO ACTION, DELETE NO ACTION.
- FKbyysfcms9vsjp968fx2jl9jvr: (class_id) → school_classes(id); UPDATE NO ACTION, DELETE NO ACTION.

<a id="evaluation-policies"></a>

## evaluation_policies

Snapshot policy lớp, ngưỡng/chuyên cần và actor/reason. Owner logic: **teaching**. Engine InnoDB; collation utf8mb4_0900_ai_ci.

| Cột | Kiểu SQL | Nullable | Default | Key / extra | Ý nghĩa / lưu ý |
|---|---|---|---|---|---|
| id | bigint | NO | ∅ | PRI; auto_increment | Ánh xạ EvaluationPolicy.id. |
| class_id | bigint | NO | ∅ | MUL | ID scalar/logic; không có FK SQL trong schema này. |
| branch_id | bigint | NO | ∅ | — | ID scalar/logic; không có FK SQL trong schema này. |
| course_id | bigint | NO | ∅ | — | ID scalar/logic; không có FK SQL trong schema này. |
| revision | bigint | NO | ∅ | — | Revision/epoch do service quản lý; không tự coi là @Version. |
| pass_percent | decimal(5,2) | NO | ∅ | — | Ánh xạ EvaluationPolicy.passPercent. |
| min_attendance_percent | decimal(5,2) | YES | ∅ | — | Ánh xạ EvaluationPolicy.minAttendancePercent. |
| actor_id | bigint | NO | ∅ | — | ID scalar/logic; không có FK SQL trong schema này. |
| actor_name | varchar(255) | NO | ∅ | — | Ánh xạ EvaluationPolicy.actorName. |
| confirmed_at | datetime(6) | NO | ∅ | — | Ngày/giờ nghiệp vụ; timezone ứng dụng Asia/Ho_Chi_Minh. |
| reason | varchar(500) | NO | ∅ | — | Lý do nghiệp vụ/audit; validation tùy endpoint. |

**Index/unique:**

- PRIMARY: UNIQUE (id).
- uk_evaluation_policy_revision: UNIQUE (class_id, revision).

**FK SQL:**

- Không có FK SQL; không suy ra FK từ tên cột hoặc ID qua API.

<a id="evaluation-policy-exams"></a>

## evaluation_policy_exams

ElementCollection quy tắc bài của policy: required/weight/maxScore/date. Owner logic: **teaching**. Engine InnoDB; collation utf8mb4_0900_ai_ci.

| Cột | Kiểu SQL | Nullable | Default | Key / extra | Ý nghĩa / lưu ý |
|---|---|---|---|---|---|
| policy_id | bigint | NO | ∅ | PRI | ID có FK SQL thực tế, xem danh sách bên dưới. |
| exam_id | bigint | NO | ∅ | PRI | ID có FK SQL thực tế, xem danh sách bên dưới. |
| exam_version | bigint | NO | ∅ | — | Xem đặc tả bảng và entity/collection tương ứng. |
| exam_name | varchar(255) | NO | ∅ | — | Xem đặc tả bảng và entity/collection tương ứng. |
| exam_date | date | NO | ∅ | — | Ngày/giờ nghiệp vụ; timezone ứng dụng Asia/Ho_Chi_Minh. |
| required | bit(1) | NO | ∅ | — | Xem đặc tả bảng và entity/collection tương ứng. |
| weight | decimal(7,2) | NO | ∅ | — | Xem đặc tả bảng và entity/collection tương ứng. |
| max_score | decimal(7,2) | NO | ∅ | — | Xem đặc tả bảng và entity/collection tương ứng. |

**Index/unique:**

- fk_evaluation_exam: INDEX (exam_id).
- uk_evaluation_policy_exam: UNIQUE (policy_id, exam_id).

**FK SQL:**

- fk_evaluation_exam: (exam_id) → exams(id); UPDATE NO ACTION, DELETE NO ACTION.
- fk_evaluation_policy: (policy_id) → evaluation_policies(id); UPDATE NO ACTION, DELETE NO ACTION.

<a id="evaluation-states"></a>

## evaluation_states

Parent lock lớp, revision policy và teaching sourceRevision. Owner logic: **teaching**. Engine InnoDB; collation utf8mb4_0900_ai_ci.

| Cột | Kiểu SQL | Nullable | Default | Key / extra | Ý nghĩa / lưu ý |
|---|---|---|---|---|---|
| class_id | bigint | NO | ∅ | PRI | ID scalar/logic; không có FK SQL trong schema này. |
| branch_id | bigint | NO | ∅ | — | ID scalar/logic; không có FK SQL trong schema này. |
| revision | bigint | NO | 0 | — | Revision/epoch do service quản lý; không tự coi là @Version. |
| source_revision | bigint | NO | 0 | — | Revision/epoch do service quản lý; không tự coi là @Version. |

**Index/unique:**

- PRIMARY: UNIQUE (class_id).

**FK SQL:**

- Không có FK SQL; không suy ra FK từ tên cột hoặc ID qua API.

<a id="exam-candidates"></a>

## exam_candidates

Danh sách dự thi chụp theo ngày, enrollmentId có thể null ở legacy. Owner logic: **teaching**. Engine InnoDB; collation utf8mb4_unicode_ci.

| Cột | Kiểu SQL | Nullable | Default | Key / extra | Ý nghĩa / lưu ý |
|---|---|---|---|---|---|
| id | bigint | NO | ∅ | PRI; auto_increment | Xem đặc tả bảng và entity/collection tương ứng. |
| exam_id | bigint | NO | ∅ | MUL | ID scalar/logic; không có FK SQL trong schema này. |
| student_id | bigint | NO | ∅ | — | ID scalar/logic; không có FK SQL trong schema này. |
| student_name | varchar(255) | NO | ∅ | — | Tên chụp theo nghiệp vụ, không FK định danh. |
| source | varchar(255) | NO | ∅ | — | Ánh xạ ExamCandidate.source. |
| enrollment_id | bigint | YES | ∅ | — | ID scalar/logic; không có FK SQL trong schema này. |

**Index/unique:**

- PRIMARY: UNIQUE (id).
- uk_exam_candidate: UNIQUE (exam_id, student_id).

**FK SQL:**

- Không có FK SQL; không suy ra FK từ tên cột hoặc ID qua API.

<a id="exams"></a>

## exams

Bài kiểm tra và freeze roster; thang/trọng số gợi ý. Owner logic: **teaching**. Engine InnoDB; collation utf8mb4_unicode_ci.

| Cột | Kiểu SQL | Nullable | Default | Key / extra | Ý nghĩa / lưu ý |
|---|---|---|---|---|---|
| id | bigint | NO | ∅ | PRI; auto_increment | Xem đặc tả bảng và entity/collection tương ứng. |
| branch_id | bigint | NO | ∅ | MUL | ID scalar/logic; không có FK SQL trong schema này. |
| class_id | bigint | NO | ∅ | — | ID scalar/logic; không có FK SQL trong schema này. |
| created_at | datetime(6) | YES | ∅ | — | Ngày/giờ nghiệp vụ; timezone ứng dụng Asia/Ho_Chi_Minh. |
| created_by | bigint | YES | ∅ | — | Ánh xạ Exam.createdBy. |
| created_by_name | varchar(255) | YES | ∅ | — | Tên chụp theo nghiệp vụ, không FK định danh. |
| exam_date | date | NO | ∅ | — | Ngày/giờ nghiệp vụ; timezone ứng dụng Asia/Ho_Chi_Minh. |
| exam_type | varchar(255) | NO | ∅ | — | Ánh xạ Exam.examType. |
| max_score | decimal(7,2) | NO | ∅ | — | Ánh xạ Exam.maxScore. |
| name | varchar(255) | NO | ∅ | — | Ánh xạ Exam.name. |
| weight | decimal(7,2) | NO | ∅ | — | Ánh xạ Exam.weight. |
| roster_frozen_at | datetime(6) | YES | ∅ | — | Ngày/giờ nghiệp vụ; timezone ứng dụng Asia/Ho_Chi_Minh. |
| roster_frozen_by | bigint | YES | ∅ | — | Ánh xạ Exam.rosterFrozenBy. |
| roster_frozen_by_name | varchar(255) | YES | ∅ | — | Tên chụp theo nghiệp vụ, không FK định danh. |
| version | bigint | NO | 0 | — | @Version JPA: kiểm ghi đè optimistic. |

**Index/unique:**

- idx_exam_branch_class: INDEX (branch_id, class_id).
- PRIMARY: UNIQUE (id).

**FK SQL:**

- Không có FK SQL; không suy ra FK từ tên cột hoặc ID qua API.

<a id="final-results"></a>

## final_results

Phiên bản kết quả cuối theo enrollment; snapshot bất biến qua service. Owner logic: **teaching**. Engine InnoDB; collation utf8mb4_0900_ai_ci.

| Cột | Kiểu SQL | Nullable | Default | Key / extra | Ý nghĩa / lưu ý |
|---|---|---|---|---|---|
| id | bigint | NO | ∅ | PRI; auto_increment | Ánh xạ FinalResult.id. |
| class_id | bigint | NO | ∅ | MUL | ID scalar/logic; không có FK SQL trong schema này. |
| branch_id | bigint | NO | ∅ | — | ID scalar/logic; không có FK SQL trong schema này. |
| enrollment_id | bigint | NO | ∅ | MUL | ID scalar/logic; không có FK SQL trong schema này. |
| student_id | bigint | NO | ∅ | MUL | ID scalar/logic; không có FK SQL trong schema này. |
| revision | bigint | NO | ∅ | — | Revision/epoch do service quản lý; không tự coi là @Version. |
| policy_revision | bigint | NO | ∅ | — | Revision/epoch do service quản lý; không tự coi là @Version. |
| academic_source_revision | bigint | NO | ∅ | — | Revision/epoch do service quản lý; không tự coi là @Version. |
| teaching_source_revision | bigint | NO | ∅ | — | Revision/epoch do service quản lý; không tự coi là @Version. |
| request_id | varchar(36) | NO | ∅ | UNI | Idempotency intent theo scope/index; giữ khi retry cùng ý định. |
| fingerprint | varchar(64) | NO | ∅ | — | Hash căn cứ/context; không bằng chứng khảo sát thật. |
| outcome | varchar(20) | NO | ∅ | — | Ánh xạ FinalResult.outcome. |
| state | varchar(20) | NO | ∅ | — | Ánh xạ FinalResult.state. |
| reason | varchar(500) | NO | ∅ | — | Lý do nghiệp vụ/audit; validation tùy endpoint. |
| confirmed_by | bigint | NO | ∅ | — | Ánh xạ FinalResult.confirmedBy. |
| confirmed_by_name | varchar(255) | NO | ∅ | — | Tên chụp theo nghiệp vụ, không FK định danh. |
| confirmed_at | datetime(6) | NO | ∅ | — | Ngày/giờ nghiệp vụ; timezone ứng dụng Asia/Ho_Chi_Minh. |
| snapshot | longtext | NO | ∅ | — | Ảnh chụp/audit; không dùng dữ liệu hiện tại ghi đè lịch sử. |
| reopened_by | bigint | YES | ∅ | — | Ánh xạ FinalResult.reopenedBy. |
| reopened_by_name | varchar(255) | YES | ∅ | — | Tên chụp theo nghiệp vụ, không FK định danh. |
| reopened_at | datetime(6) | YES | ∅ | — | Ngày/giờ nghiệp vụ; timezone ứng dụng Asia/Ho_Chi_Minh. |
| reopen_reason | varchar(500) | YES | ∅ | — | Lý do nghiệp vụ/audit; validation tùy endpoint. |
| current_enrollment_id | bigint | YES | ∅ | UNI; STORED GENERATED | Cột generated MySQL; (case when (`state` = _utf8mb4\'CONFIRMED\') then `enrollment_id` else NULL end) |

**Index/unique:**

- idx_final_class: INDEX (class_id, id).
- idx_final_student: INDEX (student_id, id).
- PRIMARY: UNIQUE (id).
- uk_final_current: UNIQUE (current_enrollment_id).
- uk_final_request: UNIQUE (request_id).
- uk_final_revision: UNIQUE (enrollment_id, revision).

**FK SQL:**

- Không có FK SQL; không suy ra FK từ tên cột hoặc ID qua API.

<a id="grade-changes"></a>

## grade_changes

Audit hiệu chỉnh điểm, trước/sau/actor/reason. Owner logic: **teaching**. Engine InnoDB; collation utf8mb4_unicode_ci.

| Cột | Kiểu SQL | Nullable | Default | Key / extra | Ý nghĩa / lưu ý |
|---|---|---|---|---|---|
| id | bigint | NO | ∅ | PRI; auto_increment | Xem đặc tả bảng và entity/collection tương ứng. |
| grade_id | bigint | YES | ∅ | MUL | ID scalar/logic; không có FK SQL trong schema này. |
| modified_at | datetime(6) | YES | ∅ | — | Ngày/giờ nghiệp vụ; timezone ứng dụng Asia/Ho_Chi_Minh. |
| modified_by | bigint | YES | ∅ | — | Ánh xạ GradeChange.modifiedBy. |
| modified_by_name | varchar(255) | YES | ∅ | — | Tên chụp theo nghiệp vụ, không FK định danh. |
| new_score | decimal(7,2) | YES | ∅ | — | Ánh xạ GradeChange.newScore. |
| old_score | decimal(7,2) | YES | ∅ | — | Ánh xạ GradeChange.oldScore. |
| reason | varchar(500) | YES | ∅ | — | Lý do nghiệp vụ/audit; validation tùy endpoint. |

**Index/unique:**

- idx_grade_change_grade: INDEX (grade_id).
- PRIMARY: UNIQUE (id).

**FK SQL:**

- Không có FK SQL; không suy ra FK từ tên cột hoặc ID qua API.

<a id="grades"></a>

## grades

Điểm hiện tại theo exam/student; 0 khác chưa có grade; @Version. Owner logic: **teaching**. Engine InnoDB; collation utf8mb4_unicode_ci.

| Cột | Kiểu SQL | Nullable | Default | Key / extra | Ý nghĩa / lưu ý |
|---|---|---|---|---|---|
| id | bigint | NO | ∅ | PRI; auto_increment | Xem đặc tả bảng và entity/collection tương ứng. |
| branch_id | bigint | NO | ∅ | — | ID scalar/logic; không có FK SQL trong schema này. |
| comment | varchar(1000) | YES | ∅ | — | Ánh xạ Grade.comment. |
| entered_at | datetime(6) | NO | ∅ | — | Ngày/giờ nghiệp vụ; timezone ứng dụng Asia/Ho_Chi_Minh. |
| entered_by | bigint | NO | ∅ | — | Ánh xạ Grade.enteredBy. |
| entered_by_name | varchar(255) | YES | ∅ | — | Tên chụp theo nghiệp vụ, không FK định danh. |
| last_modified_at | datetime(6) | YES | ∅ | — | Ngày/giờ nghiệp vụ; timezone ứng dụng Asia/Ho_Chi_Minh. |
| last_modified_by | bigint | YES | ∅ | — | Ánh xạ Grade.lastModifiedBy. |
| last_modified_by_name | varchar(255) | YES | ∅ | — | Tên chụp theo nghiệp vụ, không FK định danh. |
| modification_reason | varchar(500) | YES | ∅ | — | Lý do nghiệp vụ/audit; validation tùy endpoint. |
| score | decimal(7,2) | NO | ∅ | — | Điểm 0 hợp lệ; không có dòng/chưa chấm khác0. |
| student_id | bigint | NO | ∅ | — | ID scalar/logic; không có FK SQL trong schema này. |
| student_name | varchar(255) | YES | ∅ | — | Tên chụp theo nghiệp vụ, không FK định danh. |
| version | bigint | NO | ∅ | — | @Version JPA: kiểm ghi đè optimistic. |
| exam_id | bigint | NO | ∅ | MUL | ID có FK SQL thực tế, xem danh sách bên dưới. |

**Index/unique:**

- PRIMARY: UNIQUE (id).
- uk_grade_exam_student: UNIQUE (exam_id, student_id).

**FK SQL:**

- FK8mv6x74stqpbxncsci4gh2omn: (exam_id) → exams(id); UPDATE NO ACTION, DELETE NO ACTION.

<a id="lead-activities"></a>

## lead_activities

Nhật ký chăm sóc, bàn giao, chuyển đổi; NOTE không là contact. Owner logic: **academic**. Engine InnoDB; collation utf8mb4_unicode_ci.

| Cột | Kiểu SQL | Nullable | Default | Key / extra | Ý nghĩa / lưu ý |
|---|---|---|---|---|---|
| id | bigint | NO | ∅ | PRI; auto_increment | Xem đặc tả bảng và entity/collection tương ứng. |
| lead_id | bigint | NO | ∅ | MUL | ID scalar/logic; không có FK SQL trong schema này. |
| action | varchar(255) | NO | ∅ | — | Ánh xạ LeadActivity.action. |
| note | varchar(2000) | NO | ∅ | — | Ánh xạ LeadActivity.note. |
| channel | varchar(255) | YES | ∅ | — | Ánh xạ LeadActivity.channel. |
| old_status | varchar(255) | YES | ∅ | — | Ánh xạ LeadActivity.oldStatus. |
| new_status | varchar(255) | NO | ∅ | — | Ánh xạ LeadActivity.newStatus. |
| old_assignee_id | bigint | YES | ∅ | — | ID scalar/logic; không có FK SQL trong schema này. |
| old_assignee_name | varchar(255) | YES | ∅ | — | Ánh xạ LeadActivity.oldAssigneeName. |
| assignee_id | bigint | NO | ∅ | — | ID scalar/logic; không có FK SQL trong schema này. |
| assignee_name | varchar(255) | NO | ∅ | — | Ánh xạ LeadActivity.assigneeName. |
| old_next_contact_at | datetime(6) | YES | ∅ | — | Ngày/giờ nghiệp vụ; timezone ứng dụng Asia/Ho_Chi_Minh. |
| next_contact_at | datetime(6) | YES | ∅ | — | Ngày/giờ nghiệp vụ; timezone ứng dụng Asia/Ho_Chi_Minh. |
| actor_id | bigint | NO | ∅ | — | ID scalar/logic; không có FK SQL trong schema này. |
| actor_name | varchar(255) | NO | ∅ | — | Ánh xạ LeadActivity.actorName. |
| occurred_at | datetime(6) | NO | ∅ | — | Ngày/giờ nghiệp vụ; timezone ứng dụng Asia/Ho_Chi_Minh. |

**Index/unique:**

- idx_lead_activity: INDEX (lead_id, id).
- PRIMARY: UNIQUE (id).

**FK SQL:**

- Không có FK SQL; không suy ra FK từ tên cột hoặc ID qua API.

<a id="leads"></a>

## leads

Tiếp nhận/quan tâm khóa/phụ trách/chuyển đổi thành học viên. Owner logic: **academic**. Engine InnoDB; collation utf8mb4_unicode_ci.

| Cột | Kiểu SQL | Nullable | Default | Key / extra | Ý nghĩa / lưu ý |
|---|---|---|---|---|---|
| id | bigint | NO | ∅ | PRI; auto_increment | Xem đặc tả bảng và entity/collection tương ứng. |
| assigned_to | bigint | NO | ∅ | — | Ánh xạ Lead.assignedTo. |
| assigned_to_name | varchar(255) | NO | ∅ | — | Ánh xạ Lead.assignedToName. |
| branch_id | bigint | NO | ∅ | MUL | ID scalar/logic; không có FK SQL trong schema này. |
| created_at | datetime(6) | NO | ∅ | — | Ngày/giờ nghiệp vụ; timezone ứng dụng Asia/Ho_Chi_Minh. |
| desired_level | varchar(255) | YES | ∅ | — | Ánh xạ Lead.desiredLevel. |
| email | varchar(255) | YES | ∅ | — | Ánh xạ Lead.email. |
| full_name | varchar(255) | NO | ∅ | — | Ánh xạ Lead.fullName. |
| last_contacted_at | datetime(6) | YES | ∅ | — | Ngày/giờ nghiệp vụ; timezone ứng dụng Asia/Ho_Chi_Minh. |
| note | longtext | YES | ∅ | — | Ánh xạ Lead.note. |
| phone | varchar(255) | NO | ∅ | — | Ánh xạ Lead.phone. |
| source | varchar(255) | YES | ∅ | — | Ánh xạ Lead.source. |
| status | enum('CONSULTING','CONTACTED','CONVERTED','LOST','NEW') | NO | ∅ | — | Ánh xạ Lead.status. |
| converted_student_id | bigint | YES | ∅ | UNI | ID có FK SQL thực tế, xem danh sách bên dưới. |
| interested_course_id | bigint | YES | ∅ | MUL | ID có FK SQL thực tế, xem danh sách bên dưới. |
| version | bigint | NO | 0 | — | @Version JPA: kiểm ghi đè optimistic. |
| next_contact_at | datetime(6) | YES | ∅ | — | Ngày/giờ nghiệp vụ; timezone ứng dụng Asia/Ho_Chi_Minh. |
| updated_at | datetime(6) | YES | ∅ | — | Ngày/giờ nghiệp vụ; timezone ứng dụng Asia/Ho_Chi_Minh. |

**Index/unique:**

- FKi9bcrgl5vp7bsmsn4idx1o1qq: INDEX (interested_course_id).
- idx_lead_followup: INDEX (branch_id, status, next_contact_at).
- PRIMARY: UNIQUE (id).
- UKmnswa5bo24ajxyfwnnkxgdt88: UNIQUE (converted_student_id).

**FK SQL:**

- FKi9bcrgl5vp7bsmsn4idx1o1qq: (interested_course_id) → courses(id); UPDATE NO ACTION, DELETE NO ACTION.
- FKmrhkinpqhxk4wmhkqcg9pxt5b: (converted_student_id) → students(id); UPDATE NO ACTION, DELETE NO ACTION.

<a id="learning-events"></a>

## learning_events

Lịch sử trạng thái học viên/đăng ký/chuyển/bảo lưu/hoàn thành. Owner logic: **academic**. Engine InnoDB; collation utf8mb4_unicode_ci.

| Cột | Kiểu SQL | Nullable | Default | Key / extra | Ý nghĩa / lưu ý |
|---|---|---|---|---|---|
| id | bigint | NO | ∅ | PRI; auto_increment | Xem đặc tả bảng và entity/collection tương ứng. |
| branch_id | bigint | NO | ∅ | — | ID scalar/logic; không có FK SQL trong schema này. |
| student_id | bigint | NO | ∅ | MUL | ID scalar/logic; không có FK SQL trong schema này. |
| enrollment_id | bigint | YES | ∅ | MUL | ID scalar/logic; không có FK SQL trong schema này. |
| related_enrollment_id | bigint | YES | ∅ | — | ID scalar/logic; không có FK SQL trong schema này. |
| action | varchar(255) | NO | ∅ | — | Ánh xạ LearningEvent.action. |
| old_status | varchar(255) | YES | ∅ | — | Ánh xạ LearningEvent.oldStatus. |
| new_status | varchar(255) | NO | ∅ | — | Ánh xạ LearningEvent.newStatus. |
| reason | varchar(500) | NO | ∅ | — | Lý do nghiệp vụ/audit; validation tùy endpoint. |
| effective_date | date | NO | ∅ | — | Ngày/giờ nghiệp vụ; timezone ứng dụng Asia/Ho_Chi_Minh. |
| expected_return_date | date | YES | ∅ | — | Ngày/giờ nghiệp vụ; timezone ứng dụng Asia/Ho_Chi_Minh. |
| actor_id | bigint | NO | ∅ | — | ID scalar/logic; không có FK SQL trong schema này. |
| actor_name | varchar(255) | NO | ∅ | — | Ánh xạ LearningEvent.actorName. |
| occurred_at | datetime(6) | NO | ∅ | — | Ngày/giờ nghiệp vụ; timezone ứng dụng Asia/Ho_Chi_Minh. |

**Index/unique:**

- idx_learning_enrollment: INDEX (enrollment_id, id).
- idx_learning_student: INDEX (student_id, id).
- PRIMARY: UNIQUE (id).

**FK SQL:**

- Không có FK SQL; không suy ra FK từ tên cột hoặc ID qua API.

<a id="payment-corrections"></a>

## payment_corrections

Journal VOID/REFUND; giữ phiếu gốc. Owner logic: **finance**. Engine InnoDB; collation utf8mb4_unicode_ci.

| Cột | Kiểu SQL | Nullable | Default | Key / extra | Ý nghĩa / lưu ý |
|---|---|---|---|---|---|
| id | bigint | NO | ∅ | PRI; auto_increment | Xem đặc tả bảng và entity/collection tương ứng. |
| payment_id | bigint | NO | ∅ | MUL | ID scalar/logic; không có FK SQL trong schema này. |
| tuition_id | bigint | NO | ∅ | MUL | ID scalar/logic; không có FK SQL trong schema này. |
| branch_id | bigint | NO | ∅ | MUL | ID scalar/logic; không có FK SQL trong schema này. |
| type | varchar(255) | NO | ∅ | — | Ánh xạ PaymentCorrection.type. |
| amount | decimal(15,2) | NO | ∅ | — | Ánh xạ PaymentCorrection.amount. |
| payment_method | varchar(255) | YES | ∅ | — | Ánh xạ PaymentCorrection.paymentMethod. |
| reference_code | varchar(100) | YES | ∅ | — | Ánh xạ PaymentCorrection.referenceCode. |
| reason | varchar(500) | NO | ∅ | — | Lý do nghiệp vụ/audit; validation tùy endpoint. |
| request_key | varchar(100) | NO | ∅ | — | Idempotency intent theo scope/index; giữ khi retry cùng ý định. |
| expected_version | bigint | NO | ∅ | — | Ánh xạ PaymentCorrection.expectedVersion. |
| recorded_by | bigint | NO | ∅ | — | Ánh xạ PaymentCorrection.recordedBy. |
| recorded_by_name | varchar(255) | NO | ∅ | — | Tên chụp theo nghiệp vụ, không FK định danh. |
| created_at | datetime(6) | NO | ∅ | — | Ngày/giờ nghiệp vụ; timezone ứng dụng Asia/Ho_Chi_Minh. |

**Index/unique:**

- idx_correction_branch_date: INDEX (branch_id, created_at).
- idx_correction_payment: INDEX (payment_id, id).
- idx_correction_tuition: INDEX (tuition_id, id).
- PRIMARY: UNIQUE (id).
- uk_correction_request: UNIQUE (branch_id, request_key).

**FK SQL:**

- Không có FK SQL; không suy ra FK từ tên cột hoặc ID qua API.

<a id="payments"></a>

## payments

Phiếu thu gốc, requestKey/receipt; không xóa bằng hoàn/đảo. Owner logic: **finance**. Engine InnoDB; collation utf8mb4_unicode_ci.

| Cột | Kiểu SQL | Nullable | Default | Key / extra | Ý nghĩa / lưu ý |
|---|---|---|---|---|---|
| id | bigint | NO | ∅ | PRI; auto_increment | Xem đặc tả bảng và entity/collection tương ứng. |
| amount | decimal(15,2) | NO | ∅ | — | Ánh xạ Payment.amount. |
| branch_id | bigint | NO | ∅ | MUL | ID scalar/logic; không có FK SQL trong schema này. |
| note | varchar(500) | YES | ∅ | — | Ánh xạ Payment.note. |
| paid_at | datetime(6) | NO | ∅ | — | Ngày/giờ nghiệp vụ; timezone ứng dụng Asia/Ho_Chi_Minh. |
| payment_method | enum('BANK_TRANSFER','CARD','CASH','WALLET') | NO | ∅ | — | Ánh xạ Payment.paymentMethod. |
| receipt_no | varchar(255) | YES | ∅ | UNI | Ánh xạ Payment.receiptNo. |
| recorded_by | bigint | YES | ∅ | — | Ánh xạ Payment.recordedBy. |
| recorded_by_name | varchar(255) | YES | ∅ | — | Tên chụp theo nghiệp vụ, không FK định danh. |
| reference_code | varchar(100) | YES | ∅ | — | Ánh xạ Payment.referenceCode. |
| request_key | varchar(100) | YES | ∅ | — | Idempotency intent theo scope/index; giữ khi retry cùng ý định. |
| status | enum('FAILED','PAID','PARTIAL','PENDING','REFUNDED') | NO | ∅ | — | Ánh xạ Payment.status. |
| tuition_id | bigint | NO | ∅ | MUL | ID scalar/logic; không có FK SQL trong schema này. |

**Index/unique:**

- idx_payment_branch_date: INDEX (branch_id, paid_at).
- idx_payment_tuition: INDEX (tuition_id).
- PRIMARY: UNIQUE (id).
- uk_payment_receipt: UNIQUE (receipt_no).
- uk_payment_request: UNIQUE (branch_id, request_key).

**FK SQL:**

- Không có FK SQL; không suy ra FK từ tên cột hoặc ID qua API.

<a id="placement-assessments"></a>

## placement_assessments

Đánh giá đầu vào có nguồn, trạng thái/current key và version. Owner logic: **academic**. Engine InnoDB; collation utf8mb4_0900_ai_ci.

| Cột | Kiểu SQL | Nullable | Default | Key / extra | Ý nghĩa / lưu ý |
|---|---|---|---|---|---|
| id | bigint | NO | ∅ | PRI; auto_increment | Ánh xạ PlacementAssessment.id. |
| version | bigint | NO | 0 | — | @Version JPA: kiểm ghi đè optimistic. |
| student_id | bigint | NO | ∅ | MUL | ID có FK SQL thực tế, xem danh sách bên dưới. |
| branch_id | bigint | NO | ∅ | — | ID scalar/logic; không có FK SQL trong schema này. |
| program_id | bigint | NO | ∅ | — | ID scalar/logic; không có FK SQL trong schema này. |
| program_code | varchar(50) | NO | ∅ | — | Ánh xạ PlacementAssessment.programCode. |
| program_name | varchar(255) | NO | ∅ | — | Ánh xạ PlacementAssessment.programName. |
| level_id | bigint | NO | ∅ | — | ID scalar/logic; không có FK SQL trong schema này. |
| level_code | varchar(50) | NO | ∅ | — | Ánh xạ PlacementAssessment.levelCode. |
| level_name | varchar(255) | NO | ∅ | — | Ánh xạ PlacementAssessment.levelName. |
| method | varchar(20) | NO | ∅ | — | Ánh xạ PlacementAssessment.method. |
| status | varchar(20) | NO | ∅ | — | Ánh xạ PlacementAssessment.status. |
| assessed_on | date | NO | ∅ | — | Ánh xạ PlacementAssessment.assessedOn. |
| assessor_name | varchar(255) | NO | ∅ | — | Ánh xạ PlacementAssessment.assessorName. |
| source | varchar(2000) | NO | ∅ | — | Ánh xạ PlacementAssessment.source. |
| score | decimal(12,4) | YES | ∅ | — | Điểm 0 hợp lệ; không có dòng/chưa chấm khác0. |
| max_score | decimal(12,4) | YES | ∅ | — | Ánh xạ PlacementAssessment.maxScore. |
| recorded_by | bigint | NO | ∅ | — | Ánh xạ PlacementAssessment.recordedBy. |
| recorded_by_name | varchar(255) | NO | ∅ | — | Tên chụp theo nghiệp vụ, không FK định danh. |
| confirmed_by | bigint | YES | ∅ | — | Ánh xạ PlacementAssessment.confirmedBy. |
| confirmed_by_name | varchar(255) | YES | ∅ | — | Tên chụp theo nghiệp vụ, không FK định danh. |
| confirmed_at | datetime(6) | YES | ∅ | — | Ngày/giờ nghiệp vụ; timezone ứng dụng Asia/Ho_Chi_Minh. |
| supersedes_id | bigint | YES | ∅ | — | ID scalar/logic; không có FK SQL trong schema này. |
| superseded_by_id | bigint | YES | ∅ | — | ID scalar/logic; không có FK SQL trong schema này. |
| created_at | datetime(6) | NO | ∅ | — | Ngày/giờ nghiệp vụ; timezone ứng dụng Asia/Ho_Chi_Minh. |
| updated_at | datetime(6) | NO | ∅ | — | Ngày/giờ nghiệp vụ; timezone ứng dụng Asia/Ho_Chi_Minh. |
| current_key | varchar(90) | YES | ∅ | UNI; STORED GENERATED | Cột generated MySQL; (case when (`status` = _utf8mb4\'CONFIRMED\') then concat(`student_id`,_utf8mb4\':\',`program_id`) else NULL end) |

**Index/unique:**

- idx_assessment_student_program: INDEX (student_id, program_id, status).
- PRIMARY: UNIQUE (id).
- uk_assessment_current: UNIQUE (current_key).

**FK SQL:**

- fk_assessment_student: (student_id) → students(id); UPDATE NO ACTION, DELETE NO ACTION.

<a id="placement-exceptions"></a>

## placement_exceptions

Đề nghị/duyệt/tiêu thụ ngoại lệ học thuật với context fingerprint. Owner logic: **academic**. Engine InnoDB; collation utf8mb4_0900_ai_ci.

| Cột | Kiểu SQL | Nullable | Default | Key / extra | Ý nghĩa / lưu ý |
|---|---|---|---|---|---|
| id | bigint | NO | ∅ | PRI; auto_increment | Ánh xạ PlacementException.id. |
| version | bigint | NO | 0 | — | @Version JPA: kiểm ghi đè optimistic. |
| student_id | bigint | NO | ∅ | MUL | ID có FK SQL thực tế, xem danh sách bên dưới. |
| class_id | bigint | NO | ∅ | MUL | ID có FK SQL thực tế, xem danh sách bên dưới. |
| branch_id | bigint | NO | ∅ | MUL | ID scalar/logic; không có FK SQL trong schema này. |
| intent | varchar(20) | NO | ∅ | — | Ánh xạ PlacementException.intent. |
| source_enrollment_id | bigint | YES | ∅ | MUL | ID có FK SQL thực tế, xem danh sách bên dưới. |
| source_version | bigint | YES | ∅ | — | Ánh xạ PlacementException.sourceVersion. |
| policy_id | bigint | YES | ∅ | — | ID scalar/logic; không có FK SQL trong schema này. |
| class_revision | bigint | NO | ∅ | — | Revision/epoch do service quản lý; không tự coi là @Version. |
| assessment_id | bigint | YES | ∅ | — | ID scalar/logic; không có FK SQL trong schema này. |
| assessment_version | bigint | YES | ∅ | — | Ánh xạ PlacementException.assessmentVersion. |
| context_snapshot | longtext | NO | ∅ | — | Ảnh chụp/audit; không dùng dữ liệu hiện tại ghi đè lịch sử. |
| fingerprint | varchar(64) | NO | ∅ | — | Hash căn cứ/context; không bằng chứng khảo sát thật. |
| academic_status | varchar(40) | NO | ∅ | — | Ánh xạ PlacementException.academicStatus. |
| requested_reason | varchar(500) | NO | ∅ | — | Lý do nghiệp vụ/audit; validation tùy endpoint. |
| requested_by | bigint | NO | ∅ | — | Ánh xạ PlacementException.requestedBy. |
| requested_by_name | varchar(255) | NO | ∅ | — | Tên chụp theo nghiệp vụ, không FK định danh. |
| requested_at | datetime(6) | NO | ∅ | — | Ngày/giờ nghiệp vụ; timezone ứng dụng Asia/Ho_Chi_Minh. |
| status | varchar(20) | NO | ∅ | — | Ánh xạ PlacementException.status. |
| decision_reason | varchar(500) | YES | ∅ | — | Lý do nghiệp vụ/audit; validation tùy endpoint. |
| decided_by | bigint | YES | ∅ | — | Ánh xạ PlacementException.decidedBy. |
| decided_by_name | varchar(255) | YES | ∅ | — | Tên chụp theo nghiệp vụ, không FK định danh. |
| decided_at | datetime(6) | YES | ∅ | — | Ngày/giờ nghiệp vụ; timezone ứng dụng Asia/Ho_Chi_Minh. |
| consumed_enrollment_id | bigint | YES | ∅ | MUL | ID có FK SQL thực tế, xem danh sách bên dưới. |
| consumed_at | datetime(6) | YES | ∅ | — | Ngày/giờ nghiệp vụ; timezone ứng dụng Asia/Ho_Chi_Minh. |

**Index/unique:**

- fk_placement_exception_class: INDEX (class_id).
- fk_placement_exception_consumed: INDEX (consumed_enrollment_id).
- fk_placement_exception_source: INDEX (source_enrollment_id).
- fk_placement_exception_student: INDEX (student_id).
- ix_placement_exception_branch: INDEX (branch_id, id).
- PRIMARY: UNIQUE (id).

**FK SQL:**

- fk_placement_exception_class: (class_id) → school_classes(id); UPDATE NO ACTION, DELETE NO ACTION.
- fk_placement_exception_consumed: (consumed_enrollment_id) → enrollments(id); UPDATE NO ACTION, DELETE NO ACTION.
- fk_placement_exception_source: (source_enrollment_id) → enrollments(id); UPDATE NO ACTION, DELETE NO ACTION.
- fk_placement_exception_student: (student_id) → students(id); UPDATE NO ACTION, DELETE NO ACTION.

<a id="program-levels"></a>

## program_levels

Mức trong chương trình; rank để hiển thị, không quy đổi tự động. Owner logic: **academic**. Engine InnoDB; collation utf8mb4_0900_ai_ci.

| Cột | Kiểu SQL | Nullable | Default | Key / extra | Ý nghĩa / lưu ý |
|---|---|---|---|---|---|
| id | bigint | NO | ∅ | PRI; auto_increment | Ánh xạ ProgramLevel.id. |
| version | bigint | NO | 0 | — | @Version JPA: kiểm ghi đè optimistic. |
| program_id | bigint | NO | ∅ | MUL | ID có FK SQL thực tế, xem danh sách bên dưới. |
| code | varchar(50) | NO | ∅ | — | Ánh xạ ProgramLevel.code. |
| name | varchar(255) | NO | ∅ | — | Ánh xạ ProgramLevel.name. |
| sort_order | int | NO | ∅ | — | Ánh xạ ProgramLevel.rank. |
| active | bit(1) | NO | ∅ | — | Ánh xạ ProgramLevel.active. |

**Index/unique:**

- PRIMARY: UNIQUE (id).
- uk_program_level_code: UNIQUE (program_id, code).

**FK SQL:**

- fk_level_program: (program_id) → programs(id); UPDATE NO ACTION, DELETE NO ACTION.

<a id="programs"></a>

## programs

Chương trình/thang điểm cấu trúc. Owner logic: **academic**. Engine InnoDB; collation utf8mb4_0900_ai_ci.

| Cột | Kiểu SQL | Nullable | Default | Key / extra | Ý nghĩa / lưu ý |
|---|---|---|---|---|---|
| id | bigint | NO | ∅ | PRI; auto_increment | Ánh xạ Program.id. |
| version | bigint | NO | 0 | — | @Version JPA: kiểm ghi đè optimistic. |
| code | varchar(50) | NO | ∅ | UNI | Ánh xạ Program.code. |
| name | varchar(255) | NO | ∅ | — | Ánh xạ Program.name. |
| active | bit(1) | NO | ∅ | — | Ánh xạ Program.active. |

**Index/unique:**

- PRIMARY: UNIQUE (id).
- uk_program_code: UNIQUE (code).

**FK SQL:**

- Không có FK SQL; không suy ra FK từ tên cột hoặc ID qua API.

<a id="result-reopens"></a>

## result_reopens

Intent/audit mở lại kết quả toàn lớp. Owner logic: **teaching**. Engine InnoDB; collation utf8mb4_0900_ai_ci.

| Cột | Kiểu SQL | Nullable | Default | Key / extra | Ý nghĩa / lưu ý |
|---|---|---|---|---|---|
| request_id | varchar(36) | NO | ∅ | PRI | Idempotency intent theo scope/index; giữ khi retry cùng ý định. |
| class_id | bigint | NO | ∅ | — | ID scalar/logic; không có FK SQL trong schema này. |
| previous_revision | bigint | NO | ∅ | — | Revision/epoch do service quản lý; không tự coi là @Version. |
| actor_id | bigint | NO | ∅ | — | ID scalar/logic; không có FK SQL trong schema này. |
| actor_name | varchar(255) | NO | ∅ | — | Ánh xạ ResultReopen.actorName. |
| created_at | datetime(6) | NO | ∅ | — | Ngày/giờ nghiệp vụ; timezone ứng dụng Asia/Ho_Chi_Minh. |
| reason | varchar(500) | NO | ∅ | — | Lý do nghiệp vụ/audit; validation tùy endpoint. |

**Index/unique:**

- PRIMARY: UNIQUE (request_id).

**FK SQL:**

- Không có FK SQL; không suy ra FK từ tên cột hoặc ID qua API.

<a id="rooms"></a>

## rooms

Phòng, sức chứa và tình trạng. Owner logic: **academic**. Engine InnoDB; collation utf8mb4_unicode_ci.

| Cột | Kiểu SQL | Nullable | Default | Key / extra | Ý nghĩa / lưu ý |
|---|---|---|---|---|---|
| id | bigint | NO | ∅ | PRI; auto_increment | Xem đặc tả bảng và entity/collection tương ứng. |
| branch_id | bigint | NO | ∅ | — | ID scalar/logic; không có FK SQL trong schema này. |
| capacity | int | NO | ∅ | — | Ánh xạ Room.capacity. |
| code | varchar(255) | NO | ∅ | MUL | Ánh xạ Room.code. |
| equipment | text | YES | ∅ | — | Ánh xạ Room.equipment. |
| location | varchar(255) | YES | ∅ | — | Ánh xạ Room.location. |
| name | varchar(255) | NO | ∅ | — | Ánh xạ Room.name. |
| status | enum('AVAILABLE','MAINTENANCE') | NO | ∅ | — | Ánh xạ Room.status. |

**Index/unique:**

- PRIMARY: UNIQUE (id).
- uk_room_code_branch: UNIQUE (code, branch_id).

**FK SQL:**

- Không có FK SQL; không suy ra FK từ tên cột hoặc ID qua API.

<a id="schedules"></a>

## schedules

Lịch tuần mẫu; khác buổi học thực tế. Owner logic: **academic**. Engine InnoDB; collation utf8mb4_unicode_ci.

| Cột | Kiểu SQL | Nullable | Default | Key / extra | Ý nghĩa / lưu ý |
|---|---|---|---|---|---|
| id | bigint | NO | ∅ | PRI; auto_increment | Xem đặc tả bảng và entity/collection tương ứng. |
| day_of_week | enum('FRIDAY','MONDAY','SATURDAY','SUNDAY','THURSDAY','TUESDAY','WEDNESDAY') | NO | ∅ | — | Ánh xạ Schedule.dayOfWeek. |
| end_time | time | NO | ∅ | — | Ánh xạ Schedule.endTime. |
| start_time | time | NO | ∅ | — | Ánh xạ Schedule.startTime. |
| class_id | bigint | NO | ∅ | MUL | ID có FK SQL thực tế, xem danh sách bên dưới. |

**Index/unique:**

- idx_schedule_class_day: INDEX (class_id, day_of_week).
- PRIMARY: UNIQUE (id).

**FK SQL:**

- FKtba44f4h9r5v0ldweopn33hxq: (class_id) → school_classes(id); UPDATE NO ACTION, DELETE NO ACTION.

<a id="school-classes"></a>

## school_classes

Lớp, khóa/phòng/Teacher, kế hoạch snapshot, sĩ số và lịch. Owner logic: **academic**. Engine InnoDB; collation utf8mb4_unicode_ci.

| Cột | Kiểu SQL | Nullable | Default | Key / extra | Ý nghĩa / lưu ý |
|---|---|---|---|---|---|
| id | bigint | NO | ∅ | PRI; auto_increment | Xem đặc tả bảng và entity/collection tương ứng. |
| branch_id | bigint | NO | ∅ | — | ID scalar/logic; không có FK SQL trong schema này. |
| code | varchar(255) | NO | ∅ | MUL | Ánh xạ SchoolClass.code. |
| current_size | int | NO | ∅ | — | Ánh xạ SchoolClass.currentSize. |
| end_date | date | NO | ∅ | — | Ngày/giờ nghiệp vụ; timezone ứng dụng Asia/Ho_Chi_Minh. |
| max_size | int | NO | ∅ | — | Ánh xạ SchoolClass.maxSize. |
| name | varchar(255) | NO | ∅ | — | Ánh xạ SchoolClass.name. |
| start_date | date | NO | ∅ | — | Ngày/giờ nghiệp vụ; timezone ứng dụng Asia/Ho_Chi_Minh. |
| status | enum('FINISHED','FULL','ONGOING','UPCOMING') | NO | ∅ | — | Trạng thái lưu; effective/dateStatus là dẫn xuất, closed khác hết ngày. |
| status_manually_set | bit(1) | NO | ∅ | — | Ánh xạ SchoolClass.statusManuallySet. |
| teacher_id | bigint | NO | ∅ | — | ID scalar/logic; không có FK SQL trong schema này. |
| teacher_name | varchar(255) | NO | ∅ | — | Tên chụp theo nghiệp vụ, không FK định danh. |
| course_id | bigint | NO | ∅ | MUL | ID có FK SQL thực tế, xem danh sách bên dưới. |
| room_id | bigint | NO | ∅ | MUL | ID có FK SQL thực tế, xem danh sách bên dưới. |
| session_end_date | date | YES | ∅ | — | Ngày/giờ nghiệp vụ; timezone ứng dụng Asia/Ho_Chi_Minh. |
| sessions_initialized | bit(1) | NO | b'0' | — | Ánh xạ SchoolClass.sessionsInitialized. |
| admission_policy_id | bigint | YES | ∅ | — | ID scalar/logic; không có FK SQL trong schema này. |
| admission_revision | bigint | NO | 0 | — | Revision/epoch do service quản lý; không tự coi là @Version. |
| expected_minutes | int | YES | ∅ | — | Số phút xác minh; null = chưa xác minh, không0. |
| duration_revision | bigint | NO | 0 | — | Revision/epoch do service quản lý; không tự coi là @Version. |

**Index/unique:**

- FK2iektjpygodribrx6k6t4rdpv: INDEX (room_id).
- FKaar2qgjj7oj1q40nr741qffc4: INDEX (course_id).
- PRIMARY: UNIQUE (id).
- uk_class_code_branch: UNIQUE (code, branch_id).

**FK SQL:**

- FK2iektjpygodribrx6k6t4rdpv: (room_id) → rooms(id); UPDATE NO ACTION, DELETE NO ACTION.
- FKaar2qgjj7oj1q40nr741qffc4: (course_id) → courses(id); UPDATE NO ACTION, DELETE NO ACTION.

<a id="session-events"></a>

## session_events

Snapshot trước/sau, lý do và người đổi buổi. Owner logic: **academic**. Engine InnoDB; collation utf8mb4_unicode_ci.

| Cột | Kiểu SQL | Nullable | Default | Key / extra | Ý nghĩa / lưu ý |
|---|---|---|---|---|---|
| id | bigint | NO | ∅ | PRI; auto_increment | Xem đặc tả bảng và entity/collection tương ứng. |
| session_id | bigint | NO | ∅ | MUL | ID có FK SQL thực tế, xem danh sách bên dưới. |
| action | varchar(40) | NO | ∅ | — | Ánh xạ SessionEvent.action. |
| reason | varchar(500) | YES | ∅ | — | Lý do nghiệp vụ/audit; validation tùy endpoint. |
| before_state | text | YES | ∅ | — | Ảnh chụp/audit; không dùng dữ liệu hiện tại ghi đè lịch sử. |
| after_state | text | YES | ∅ | — | Ảnh chụp/audit; không dùng dữ liệu hiện tại ghi đè lịch sử. |
| actor_id | bigint | NO | ∅ | — | ID scalar/logic; không có FK SQL trong schema này. |
| actor_name | varchar(255) | NO | ∅ | — | Ánh xạ SessionEvent.actorName. |
| occurred_at | datetime(6) | NO | ∅ | — | Ngày/giờ nghiệp vụ; timezone ứng dụng Asia/Ho_Chi_Minh. |

**Index/unique:**

- idx_session_event: INDEX (session_id, id).
- PRIMARY: UNIQUE (id).

**FK SQL:**

- fk_session_event_session: (session_id) → class_sessions(id); UPDATE NO ACTION, DELETE NO ACTION.

<a id="staffs"></a>

## staffs

Hồ sơ STAFF liên kết account. Owner logic: **auth**. Engine InnoDB; collation utf8mb4_unicode_ci.

| Cột | Kiểu SQL | Nullable | Default | Key / extra | Ý nghĩa / lưu ý |
|---|---|---|---|---|---|
| id | bigint | NO | ∅ | PRI; auto_increment | Ánh xạ Staff.id. |
| email | varchar(100) | YES | ∅ | — | Ánh xạ Staff.email. |
| full_name | varchar(100) | NO | ∅ | — | Ánh xạ Staff.fullName. |
| phone | varchar(15) | YES | ∅ | — | Ánh xạ Staff.phone. |
| account_id | bigint | NO | ∅ | UNI | ID có FK SQL thực tế, xem danh sách bên dưới. |
| hire_date | date | YES | ∅ | — | Ngày/giờ nghiệp vụ; timezone ứng dụng Asia/Ho_Chi_Minh. |
| position | varchar(100) | YES | ∅ | — | Ánh xạ Staff.position. |
| work_status | varchar(50) | YES | ∅ | — | Ánh xạ Staff.workStatus. |

**Index/unique:**

- PRIMARY: UNIQUE (id).
- UKk0fv4fbde24uqula08y9vril2: UNIQUE (account_id).

**FK SQL:**

- FK76xya5sjr57jknmc1fq46qc4x: (account_id) → accounts(id); UPDATE NO ACTION, DELETE NO ACTION.

<a id="students"></a>

## students

Hồ sơ học viên; currentLevel là text legacy riêng với assessment. Owner logic: **academic**. Engine InnoDB; collation utf8mb4_unicode_ci.

| Cột | Kiểu SQL | Nullable | Default | Key / extra | Ý nghĩa / lưu ý |
|---|---|---|---|---|---|
| id | bigint | NO | ∅ | PRI; auto_increment | Xem đặc tả bảng và entity/collection tương ứng. |
| address | varchar(255) | YES | ∅ | — | Ánh xạ Student.address. |
| branch_id | bigint | NO | ∅ | — | ID scalar/logic; không có FK SQL trong schema này. |
| contact_person | varchar(255) | YES | ∅ | — | Ánh xạ Student.contactPerson. |
| current_level | varchar(255) | YES | ∅ | — | Text legacy chưa xác minh; không thay assessment. |
| date_of_birth | date | YES | ∅ | — | Ánh xạ Student.dateOfBirth. |
| email | varchar(255) | YES | ∅ | — | Ánh xạ Student.email. |
| full_name | varchar(255) | NO | ∅ | — | Ánh xạ Student.fullName. |
| gender | enum('FEMALE','MALE','OTHER') | YES | ∅ | — | Ánh xạ Student.gender. |
| phone | varchar(255) | NO | ∅ | MUL | Ánh xạ Student.phone. |
| registered_at | date | NO | ∅ | — | Ngày/giờ nghiệp vụ; timezone ứng dụng Asia/Ho_Chi_Minh. |
| status | enum('GRADUATED','PAUSED','STUDYING') | NO | ∅ | — | Ánh xạ Student.status. |
| guardian_name | varchar(255) | YES | ∅ | — | Ánh xạ Student.guardianName. |
| guardian_phone | varchar(255) | YES | ∅ | — | Ánh xạ Student.guardianPhone. |
| student_code | varchar(40) | YES | ∅ | UNI | Ánh xạ Student.studentCode. |
| version | bigint | NO | 0 | — | @Version JPA: kiểm ghi đè optimistic. |

**Index/unique:**

- idx_student_phone_branch: INDEX (phone, branch_id).
- PRIMARY: UNIQUE (id).
- UKcgcf3r5xk73o0etbduc1qxnol: UNIQUE (student_code).

**FK SQL:**

- Không có FK SQL; không suy ra FK từ tên cột hoặc ID qua API.

<a id="super-admins"></a>

## super_admins

Hồ sơ SUPER_ADMIN liên kết account. Owner logic: **auth**. Engine InnoDB; collation utf8mb4_unicode_ci.

| Cột | Kiểu SQL | Nullable | Default | Key / extra | Ý nghĩa / lưu ý |
|---|---|---|---|---|---|
| id | bigint | NO | ∅ | PRI; auto_increment | Ánh xạ SuperAdmin.id. |
| email | varchar(100) | YES | ∅ | — | Ánh xạ SuperAdmin.email. |
| full_name | varchar(100) | NO | ∅ | — | Ánh xạ SuperAdmin.fullName. |
| phone | varchar(15) | YES | ∅ | — | Ánh xạ SuperAdmin.phone. |
| account_id | bigint | NO | ∅ | UNI | ID có FK SQL thực tế, xem danh sách bên dưới. |

**Index/unique:**

- PRIMARY: UNIQUE (id).
- UKp4f72x4wtciiblbq6vfs0xq3o: UNIQUE (account_id).

**FK SQL:**

- FKp6orxeyhfsi614sh6sgb0q9o5: (account_id) → accounts(id); UPDATE NO ACTION, DELETE NO ACTION.

<a id="teacher-profile-events"></a>

## teacher_profile_events

Snapshot/audit thay đổi hồ sơ Teacher; scalar teacherId. Owner logic: **auth**. Engine InnoDB; collation utf8mb4_0900_ai_ci.

| Cột | Kiểu SQL | Nullable | Default | Key / extra | Ý nghĩa / lưu ý |
|---|---|---|---|---|---|
| id | bigint | NO | ∅ | PRI; auto_increment | Ánh xạ TeacherProfileEvent.id. |
| teacher_id | bigint | NO | ∅ | MUL | ID scalar/logic; không có FK SQL trong schema này. |
| action | varchar(255) | NO | ∅ | — | Ánh xạ TeacherProfileEvent.action. |
| before_state | longtext | NO | ∅ | — | Ảnh chụp/audit; không dùng dữ liệu hiện tại ghi đè lịch sử. |
| after_state | longtext | NO | ∅ | — | Ảnh chụp/audit; không dùng dữ liệu hiện tại ghi đè lịch sử. |
| reason | varchar(500) | YES | ∅ | — | Lý do nghiệp vụ/audit; validation tùy endpoint. |
| actor_id | bigint | NO | ∅ | — | ID scalar/logic; không có FK SQL trong schema này. |
| actor_name | varchar(255) | NO | ∅ | — | Ánh xạ TeacherProfileEvent.actorName. |
| teacher_version | bigint | NO | ∅ | — | Ánh xạ TeacherProfileEvent.teacherVersion. |
| occurred_at | datetime(6) | NO | ∅ | — | Ngày/giờ nghiệp vụ; timezone ứng dụng Asia/Ho_Chi_Minh. |

**Index/unique:**

- idx_teacher_event: INDEX (teacher_id, id).
- PRIMARY: UNIQUE (id).

**FK SQL:**

- Không có FK SQL; không suy ra FK từ tên cột hoặc ID qua API.

<a id="teachers"></a>

## teachers

Hồ sơ chuyên môn, version và trạng thái làm việc Teacher. Owner logic: **auth**. Engine InnoDB; collation utf8mb4_unicode_ci.

| Cột | Kiểu SQL | Nullable | Default | Key / extra | Ý nghĩa / lưu ý |
|---|---|---|---|---|---|
| id | bigint | NO | ∅ | PRI; auto_increment | Ánh xạ Teacher.id. |
| certificate | varchar(255) | YES | ∅ | — | Ánh xạ Teacher.certificate. |
| email | varchar(100) | YES | ∅ | — | Ánh xạ Teacher.email. |
| experience | varchar(50) | YES | ∅ | — | Ánh xạ Teacher.experience. |
| full_name | varchar(100) | NO | ∅ | — | Ánh xạ Teacher.fullName. |
| hire_date | date | YES | ∅ | — | Ngày/giờ nghiệp vụ; timezone ứng dụng Asia/Ho_Chi_Minh. |
| phone | varchar(15) | YES | ∅ | — | Ánh xạ Teacher.phone. |
| qualification | varchar(100) | YES | ∅ | — | Ánh xạ Teacher.qualification. |
| specialization | varchar(100) | YES | ∅ | — | Ánh xạ Teacher.specialization. |
| work_status | varchar(50) | YES | ∅ | — | Ánh xạ Teacher.workStatus. |
| account_id | bigint | NO | ∅ | UNI | ID có FK SQL thực tế, xem danh sách bên dưới. |
| version | bigint | NO | 0 | — | @Version JPA: kiểm ghi đè optimistic. |

**Index/unique:**

- PRIMARY: UNIQUE (id).
- UKfwk5qk8ju90rx58169r9dj8td: UNIQUE (account_id).

**FK SQL:**

- FKnywk18vp2rdv32e94ofevfphl: (account_id) → accounts(id); UPDATE NO ACTION, DELETE NO ACTION.

<a id="tuition"></a>

## tuition

Khoản phải thu gắn enrollment billing root, số thu cached/version. Owner logic: **finance**. Engine InnoDB; collation utf8mb4_unicode_ci.

| Cột | Kiểu SQL | Nullable | Default | Key / extra | Ý nghĩa / lưu ý |
|---|---|---|---|---|---|
| id | bigint | NO | ∅ | PRI; auto_increment | Xem đặc tả bảng và entity/collection tương ứng. |
| adjusted_at | datetime(6) | YES | ∅ | — | Ngày/giờ nghiệp vụ; timezone ứng dụng Asia/Ho_Chi_Minh. |
| adjusted_by | bigint | YES | ∅ | — | Ánh xạ Tuition.adjustedBy. |
| adjusted_by_name | varchar(255) | YES | ∅ | — | Tên chụp theo nghiệp vụ, không FK định danh. |
| adjustment_reason | varchar(500) | YES | ∅ | — | Lý do nghiệp vụ/audit; validation tùy endpoint. |
| amount | decimal(15,2) | NO | ∅ | — | Ánh xạ Tuition.amount. |
| branch_id | bigint | NO | ∅ | MUL | ID scalar/logic; không có FK SQL trong schema này. |
| class_id | bigint | NO | ∅ | — | ID scalar/logic; không có FK SQL trong schema này. |
| class_name | varchar(255) | YES | ∅ | — | Ánh xạ Tuition.className. |
| course_id | bigint | YES | ∅ | — | ID scalar/logic; không có FK SQL trong schema này. |
| course_name | varchar(255) | YES | ∅ | — | Ánh xạ Tuition.courseName. |
| currency | varchar(10) | NO | ∅ | — | Ánh xạ Tuition.currency. |
| discount_amount | decimal(15,2) | YES | ∅ | — | Ánh xạ Tuition.discountAmount. |
| due_date | date | NO | ∅ | — | Ngày/giờ nghiệp vụ; timezone ứng dụng Asia/Ho_Chi_Minh. |
| enrollment_id | bigint | YES | ∅ | UNI | ID scalar/logic; không có FK SQL trong schema này. |
| original_amount | decimal(15,2) | YES | ∅ | — | Ánh xạ Tuition.originalAmount. |
| paid_amount | decimal(15,2) | YES | ∅ | — | Projection số thu ròng cached; đối chiếu journal khi cần. |
| status | enum('CANCELLED','OVERDUE','PAID','PARTIAL','PENDING','UNPAID') | NO | ∅ | — | Ánh xạ Tuition.status. |
| student_id | bigint | NO | ∅ | — | ID scalar/logic; không có FK SQL trong schema này. |
| student_name | varchar(255) | YES | ∅ | — | Tên chụp theo nghiệp vụ, không FK định danh. |
| version | bigint | NO | 0 | — | @Version JPA: kiểm ghi đè optimistic. |

**Index/unique:**

- idx_tuition_branch_due: INDEX (branch_id, due_date).
- idx_tuition_branch_status: INDEX (branch_id, status).
- PRIMARY: UNIQUE (id).
- uk_tuition_enrollment: UNIQUE (enrollment_id).

**FK SQL:**

- Không có FK SQL; không suy ra FK từ tên cột hoặc ID qua API.

<a id="tuition-adjustments"></a>

## tuition_adjustments

Journal thay học phí/hạn, requestKey/version/reason. Owner logic: **finance**. Engine InnoDB; collation utf8mb4_unicode_ci.

| Cột | Kiểu SQL | Nullable | Default | Key / extra | Ý nghĩa / lưu ý |
|---|---|---|---|---|---|
| id | bigint | NO | ∅ | PRI; auto_increment | Xem đặc tả bảng và entity/collection tương ứng. |
| tuition_id | bigint | NO | ∅ | MUL | ID scalar/logic; không có FK SQL trong schema này. |
| branch_id | bigint | NO | ∅ | MUL | ID scalar/logic; không có FK SQL trong schema này. |
| old_original | decimal(15,2) | NO | ∅ | — | Ánh xạ TuitionAdjustment.oldOriginal. |
| old_discount | decimal(15,2) | NO | ∅ | — | Ánh xạ TuitionAdjustment.oldDiscount. |
| old_amount | decimal(15,2) | NO | ∅ | — | Ánh xạ TuitionAdjustment.oldAmount. |
| old_due_date | date | NO | ∅ | — | Ngày/giờ nghiệp vụ; timezone ứng dụng Asia/Ho_Chi_Minh. |
| new_original | decimal(15,2) | NO | ∅ | — | Ánh xạ TuitionAdjustment.newOriginal. |
| new_discount | decimal(15,2) | NO | ∅ | — | Ánh xạ TuitionAdjustment.newDiscount. |
| new_amount | decimal(15,2) | NO | ∅ | — | Ánh xạ TuitionAdjustment.newAmount. |
| new_due_date | date | NO | ∅ | — | Ngày/giờ nghiệp vụ; timezone ứng dụng Asia/Ho_Chi_Minh. |
| paid_at_change | decimal(15,2) | NO | ∅ | — | Ánh xạ TuitionAdjustment.paidAtChange. |
| reason | varchar(500) | NO | ∅ | — | Lý do nghiệp vụ/audit; validation tùy endpoint. |
| request_key | varchar(100) | NO | ∅ | — | Idempotency intent theo scope/index; giữ khi retry cùng ý định. |
| expected_version | bigint | NO | ∅ | — | Ánh xạ TuitionAdjustment.expectedVersion. |
| recorded_by | bigint | NO | ∅ | — | Ánh xạ TuitionAdjustment.recordedBy. |
| recorded_by_name | varchar(255) | NO | ∅ | — | Tên chụp theo nghiệp vụ, không FK định danh. |
| created_at | datetime(6) | NO | ∅ | — | Ngày/giờ nghiệp vụ; timezone ứng dụng Asia/Ho_Chi_Minh. |

**Index/unique:**

- idx_adjustment_tuition: INDEX (tuition_id, id).
- PRIMARY: UNIQUE (id).
- uk_adjustment_request: UNIQUE (branch_id, request_key).

**FK SQL:**

- Không có FK SQL; không suy ra FK từ tên cột hoặc ID qua API.
