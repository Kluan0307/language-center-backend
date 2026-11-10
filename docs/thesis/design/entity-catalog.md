# Entity và thuộc tính JPA — G14

52 lớp @Entity ánh xạ 51 bảng khác nhau; ResultBarrier được ánh xạ ở hai service vào cùng một bảng. Hai bảng collection bổ sung thành53 bảng. Các DTO, controller, Spring framework và enum không được gán thành bảng/entity riêng. @MappedSuperclass BaseEntity chỉ đóng góp id, không phải bảng độc lập.

Scalar ID trong Class Diagram không phải object association JPA; một số scalar vẫn có FK do SQL migration. FK vật lý xem ERD/từ điển; quan hệ miền nghiệp vụ xem sơ đồ logic. Thuộc tính collection/quan hệ là Java, không thêm cột cùng tên.

## auth.Account

Bảng **accounts**; [source](../../../auth-service/src/main/java/com/example/auth/entity/Account.java).

| Thuộc tính Java | Kiểu | Mapping |
|---|---|---|
| id | Long | @Id; id |
| username | String | username |
| password | String | password |
| role | Role | role |
| status | Boolean | status |
| sessionVersion | long | session_version |
| mustChangePassword | boolean | must_change_password |
| passwordChangedAt | LocalDateTime | password_changed_at |
| securityEvents | java.util.List<AccountSecurityEvent> | OneToMany; không cột scalar riêng |
| createdAt | LocalDateTime | created_at |
| lastLogin | LocalDateTime | last_login |
| branchId | Long | branch_id |
| branchAssignments | java.util.List<AccountBranch> | OneToMany; không cột scalar riêng |
| branchEvents | java.util.List<AccountBranchEvent> | OneToMany; không cột scalar riêng |
## auth.AccountBranch

Bảng **account_branches**; [source](../../../auth-service/src/main/java/com/example/auth/entity/AccountBranch.java).

| Thuộc tính Java | Kiểu | Mapping |
|---|---|---|
| id | Long | @Id; id |
| account | Account | ManyToOne; account_id |
| branchId | Long | branch_id |
| status | boolean | status |
| primary | boolean | is_primary |
| assignedAt | LocalDateTime | assigned_at |
| assignedBy | Long | assigned_by |
| revokedAt | LocalDateTime | revoked_at |
## auth.AccountBranchEvent

Bảng **account_branch_events**; [source](../../../auth-service/src/main/java/com/example/auth/entity/AccountBranchEvent.java).

| Thuộc tính Java | Kiểu | Mapping |
|---|---|---|
| id | Long | @Id; id |
| account | Account | ManyToOne; account_id |
| branchId | Long | branch_id |
| action | String | action |
| occurredAt | LocalDateTime | occurred_at |
| performedBy | Long | performed_by |
## auth.AccountSecurityEvent

Bảng **account_security_events**; [source](../../../auth-service/src/main/java/com/example/auth/entity/AccountSecurityEvent.java).

| Thuộc tính Java | Kiểu | Mapping |
|---|---|---|
| id | Long | @Id; id |
| account | Account | ManyToOne; account_id |
| action | String | action |
| actorId | Long | actor_id |
| occurredAt | LocalDateTime | occurred_at |
| reason | String | reason |
## auth.Admin

Bảng **admins**; [source](../../../auth-service/src/main/java/com/example/auth/entity/Admin.java).

| Thuộc tính Java | Kiểu | Mapping |
|---|---|---|
| id | Long | @Id; id |
| account | Account | OneToOne; account_id |
| fullName | String | full_name |
| email | String | email |
| phone | String | phone |
| hireDate | LocalDate | hire_date |
| workStatus | String | work_status |
## auth.Staff

Bảng **staffs**; [source](../../../auth-service/src/main/java/com/example/auth/entity/Staff.java).

| Thuộc tính Java | Kiểu | Mapping |
|---|---|---|
| id | Long | @Id; id |
| account | Account | OneToOne; account_id |
| fullName | String | full_name |
| email | String | email |
| phone | String | phone |
| hireDate | LocalDate | hire_date |
| workStatus | String | work_status |
| position | String | position |
## auth.SuperAdmin

Bảng **super_admins**; [source](../../../auth-service/src/main/java/com/example/auth/entity/SuperAdmin.java).

| Thuộc tính Java | Kiểu | Mapping |
|---|---|---|
| id | Long | @Id; id |
| account | Account | OneToOne; account_id |
| fullName | String | full_name |
| email | String | email |
| phone | String | phone |
## auth.Teacher

Bảng **teachers**; [source](../../../auth-service/src/main/java/com/example/auth/entity/Teacher.java).

| Thuộc tính Java | Kiểu | Mapping |
|---|---|---|
| version | long | @Version; version |
| id | Long | @Id; id |
| account | Account | OneToOne; account_id |
| fullName | String | full_name |
| email | String | email |
| phone | String | phone |
| specialization | String | specialization |
| qualification | String | qualification |
| experience | String | experience |
| certificate | String | certificate |
| hireDate | LocalDate | hire_date |
| workStatus | String | work_status |
## auth.TeacherProfileEvent

Bảng **teacher_profile_events**; [source](../../../auth-service/src/main/java/com/example/auth/entity/TeacherProfileEvent.java).

| Thuộc tính Java | Kiểu | Mapping |
|---|---|---|
| id | Long | @Id; id |
| teacherId | Long | teacher_id |
| action | String | action |
| beforeState | String | before_state |
| afterState | String | after_state |
| reason | String | reason |
| actorId | Long | actor_id |
| actorName | String | actor_name |
| teacherVersion | long | teacher_version |
| occurredAt | LocalDateTime | occurred_at |
## academic.ClassAdmissionEvent

Bảng **class_admission_events**; [source](../../../academic-service/src/main/java/com/example/academic/admission/ClassAdmissionEvent.java).

| Thuộc tính Java | Kiểu | Mapping |
|---|---|---|
| id | Long | @Id; id |
| classId | Long | class_id |
| oldPolicyId | Long | old_policy_id |
| newPolicyId | Long | new_policy_id |
| revision | long | revision |
| actorId | Long | actor_id |
| actorName | String | actor_name |
| occurredAt | LocalDateTime | occurred_at |
| reason | String | reason |
## academic.CourseAdmissionPolicy

Bảng **course_admission_policies**; [source](../../../academic-service/src/main/java/com/example/academic/admission/CourseAdmissionPolicy.java).

| Thuộc tính Java | Kiểu | Mapping |
|---|---|---|
| id | Long | @Id; id |
| courseId | Long | course_id |
| revision | long | revision |
| mode | Mode | mode |
| programId | Long | program_id |
| programCode | String | program_code |
| programName | String | program_name |
| outputLevelId | Long | output_level_id |
| outputLevelCode | String | output_level_code |
| outputLevelName | String | output_level_name |
| acceptedLevels | List<LevelSnapshot> | ElementCollection; không cột scalar riêng |
| confirmedBy | Long | confirmed_by |
| confirmedByName | String | confirmed_by_name |
| confirmedAt | LocalDateTime | confirmed_at |
| reason | String | reason |

Embeddable LevelSnapshot: levelId:Long, code:String, name:String. Không phải entity độc lập.

Collection tables: course_admission_levels.
## academic.Program

Bảng **programs**; [source](../../../academic-service/src/main/java/com/example/academic/admission/Program.java).

| Thuộc tính Java | Kiểu | Mapping |
|---|---|---|
| id | Long | @Id; id |
| version | long | @Version; version |
| code | String | code |
| name | String | name |
| active | boolean | active |
## academic.ProgramLevel

Bảng **program_levels**; [source](../../../academic-service/src/main/java/com/example/academic/admission/ProgramLevel.java).

| Thuộc tính Java | Kiểu | Mapping |
|---|---|---|
| id | Long | @Id; id |
| version | long | @Version; version |
| program | Program | ManyToOne; program_id |
| code | String | code |
| name | String | name |
| rank | int | sort_order |
| active | boolean | active |
## academic.AssessmentEvent

Bảng **assessment_events**; [source](../../../academic-service/src/main/java/com/example/academic/assessment/AssessmentEvent.java).

| Thuộc tính Java | Kiểu | Mapping |
|---|---|---|
| id | Long | @Id; id |
| assessmentId | Long | assessment_id |
| action | String | action |
| beforeState | String | before_state |
| afterState | String | after_state |
| reason | String | reason |
| actorId | Long | actor_id |
| actorName | String | actor_name |
| occurredAt | LocalDateTime | occurred_at |
| assessmentVersion | long | assessment_version |
## academic.PlacementAssessment

Bảng **placement_assessments**; [source](../../../academic-service/src/main/java/com/example/academic/assessment/PlacementAssessment.java).

| Thuộc tính Java | Kiểu | Mapping |
|---|---|---|
| id | Long | @Id; id |
| version | long | @Version; version |
| student | Student | ManyToOne; student_id |
| branchId | Long | branch_id |
| programId | Long | program_id |
| programCode | String | program_code |
| programName | String | program_name |
| levelId | Long | level_id |
| levelCode | String | level_code |
| levelName | String | level_name |
| method | Method | method |
| status | Status | status |
| assessedOn | LocalDate | assessed_on |
| assessorName | String | assessor_name |
| source | String | source |
| score | BigDecimal | score |
| maxScore | BigDecimal | max_score |
| recordedBy | Long | recorded_by |
| recordedByName | String | recorded_by_name |
| confirmedBy | Long | confirmed_by |
| confirmedByName | String | confirmed_by_name |
| confirmedAt | LocalDateTime | confirmed_at |
| supersedesId | Long | supersedes_id |
| supersededById | Long | superseded_by_id |
| createdAt | LocalDateTime | created_at |
| updatedAt | LocalDateTime | updated_at |
| currentKey | String | current_key |
## academic.Attendance

Bảng **attendances**; [source](../../../academic-service/src/main/java/com/example/academic/attendance/Attendance.java); extends BaseEntity (id kế thừa).

| Thuộc tính Java | Kiểu | Mapping |
|---|---|---|
| enrollment | Enrollment | ManyToOne; enrollment_id |
| classSession | com.example.academic.session.ClassSession | ManyToOne; class_session_id |
| sessionDate | LocalDate | session_date |
| status | Status | status |
| hasPermission | boolean | has_permission |
| recordedBy | Long | recorded_by |
| recordedByName | String | recorded_by_name |
## academic.AttendanceChange

Bảng **attendance_changes**; [source](../../../academic-service/src/main/java/com/example/academic/attendance/AttendanceChange.java); extends BaseEntity (id kế thừa).

| Thuộc tính Java | Kiểu | Mapping |
|---|---|---|
| session | ClassSession | ManyToOne; session_id |
| enrollmentId | Long | enrollment_id |
| studentName | String | student_name |
| oldStatus | Attendance.Status | old_status |
| newStatus | Attendance.Status | new_status |
| oldPermission | Boolean | old_permission |
| newPermission | boolean | new_permission |
| previousRecordedBy | Long | previous_recorded_by |
| previousRecordedByName | String | previous_recorded_by_name |
| changedBy | Long | changed_by |
| changedByName | String | changed_by_name |
| changedAt | LocalDateTime | changed_at |
| reason | String | reason |
## academic.Branch

Bảng **branches**; [source](../../../academic-service/src/main/java/com/example/academic/branch/Branch.java); extends BaseEntity (id kế thừa).

| Thuộc tính Java | Kiểu | Mapping |
|---|---|---|
| code | String | code |
| name | String | name |
| address | String | address |
| phone | String | phone |
| status | Status | status |
| createdAt | LocalDateTime | created_at |
## academic.Course

Bảng **courses**; [source](../../../academic-service/src/main/java/com/example/academic/course/Course.java); extends BaseEntity (id kế thừa).

| Thuộc tính Java | Kiểu | Mapping |
|---|---|---|
| code | String | code |
| name | String | name |
| type | String | type |
| level | String | level |
| duration | Integer | duration |
| expectedMinutes | Integer | expected_minutes |
| durationRevision | long | duration_revision |
| tuitionFee | BigDecimal | tuition_fee |
| content | String | content |
| entryRequirement | String | entry_requirement |
| maxStudents | Integer | max_students |
| status | Status | status |
| branchId | Long | branch_id |
| scopeKey | Long | scope_key |
## academic.ClassDurationPlan

Bảng **class_duration_plans**; [source](../../../academic-service/src/main/java/com/example/academic/duration/ClassDurationPlan.java).

| Thuộc tính Java | Kiểu | Mapping |
|---|---|---|
| id | Long | @Id; id |
| classId | Long | class_id |
| revision | long | revision |
| expectedMinutes | Integer | expected_minutes |
| courseId | Long | course_id |
| courseRevision | Long | course_revision |
| source | Source | source |
| actorId | Long | actor_id |
| actorName | String | actor_name |
| occurredAt | LocalDateTime | occurred_at |
| reason | String | reason |
## academic.CourseDurationPlan

Bảng **course_duration_plans**; [source](../../../academic-service/src/main/java/com/example/academic/duration/CourseDurationPlan.java).

| Thuộc tính Java | Kiểu | Mapping |
|---|---|---|
| id | Long | @Id; id |
| courseId | Long | course_id |
| revision | long | revision |
| expectedMinutes | Integer | expected_minutes |
| actorId | Long | actor_id |
| actorName | String | actor_name |
| occurredAt | LocalDateTime | occurred_at |
| reason | String | reason |
## academic.Enrollment

Bảng **enrollments**; [source](../../../academic-service/src/main/java/com/example/academic/enrollment/Enrollment.java); extends BaseEntity (id kế thừa).

| Thuộc tính Java | Kiểu | Mapping |
|---|---|---|
| version | long | @Version; version |
| completionResultId | Long | completion_result_id |
| completionResultRevision | Long | completion_result_revision |
| reservedSince | java.time.LocalDate | reserved_since |
| expectedReturnDate | java.time.LocalDate | expected_return_date |
| billingEnrollmentId | Long | billing_enrollment_id |
| tuitionFee | java.math.BigDecimal | tuition_fee |
| registeredBy | Long | registered_by |
| registeredByName | String | registered_by_name |
| tuitionDelivery | TuitionDelivery | tuition_delivery |
| tuitionDeliveryError | String | tuition_delivery_error |
| student | Student | ManyToOne; student_id |
| schoolClass | SchoolClass | ManyToOne; class_id |
| enrolledAt | LocalDateTime | enrolled_at |
| studyStatus | StudyStatus | study_status |
## academic.EnrollmentPeriod

Bảng **enrollment_periods**; [source](../../../academic-service/src/main/java/com/example/academic/enrollment/EnrollmentPeriod.java); extends BaseEntity (id kế thừa).

| Thuộc tính Java | Kiểu | Mapping |
|---|---|---|
| enrollment | Enrollment | ManyToOne; enrollment_id |
| activeFrom | LocalDate | active_from |
| activeUntil | LocalDate | active_until |
## academic.Lead

Bảng **leads**; [source](../../../academic-service/src/main/java/com/example/academic/lead/Lead.java); extends BaseEntity (id kế thừa).

| Thuộc tính Java | Kiểu | Mapping |
|---|---|---|
| version | long | @Version; version |
| nextContactAt | LocalDateTime | next_contact_at |
| updatedAt | LocalDateTime | updated_at |
| fullName | String | full_name |
| phone | String | phone |
| email | String | email |
| source | String | source |
| interestedCourse | Course | ManyToOne; interested_course_id |
| desiredLevel | String | desired_level |
| status | Status | status |
| note | String | note |
| assignedTo | Long | assigned_to |
| assignedToName | String | assigned_to_name |
| convertedStudent | Student | OneToOne; converted_student_id |
| createdAt | LocalDateTime | created_at |
| lastContactedAt | LocalDateTime | last_contacted_at |
| branchId | Long | branch_id |
## academic.LeadActivity

Bảng **lead_activities**; [source](../../../academic-service/src/main/java/com/example/academic/lead/LeadActivity.java); extends BaseEntity (id kế thừa).

| Thuộc tính Java | Kiểu | Mapping |
|---|---|---|
| leadId | Long | lead_id |
| action | String | action |
| note | String | note |
| channel | String | channel |
| oldStatus | String | old_status |
| newStatus | String | new_status |
| oldAssigneeId | Long | old_assignee_id |
| oldAssigneeName | String | old_assignee_name |
| assigneeId | Long | assignee_id |
| assigneeName | String | assignee_name |
| oldNextContactAt | LocalDateTime | old_next_contact_at |
| nextContactAt | LocalDateTime | next_contact_at |
| actorId | Long | actor_id |
| actorName | String | actor_name |
| occurredAt | LocalDateTime | occurred_at |
## academic.LearningEvent

Bảng **learning_events**; [source](../../../academic-service/src/main/java/com/example/academic/lifecycle/LearningEvent.java); extends BaseEntity (id kế thừa).

| Thuộc tính Java | Kiểu | Mapping |
|---|---|---|
| branchId | Long | branch_id |
| studentId | Long | student_id |
| enrollmentId | Long | enrollment_id |
| relatedEnrollmentId | Long | related_enrollment_id |
| action | String | action |
| oldStatus | String | old_status |
| newStatus | String | new_status |
| reason | String | reason |
| effectiveDate | LocalDate | effective_date |
| expectedReturnDate | LocalDate | expected_return_date |
| actorId | Long | actor_id |
| actorName | String | actor_name |
| occurredAt | LocalDateTime | occurred_at |
## academic.EnrollmentAdmission

Bảng **enrollment_admissions**; [source](../../../academic-service/src/main/java/com/example/academic/placement/EnrollmentAdmission.java).

| Thuộc tính Java | Kiểu | Mapping |
|---|---|---|
| id | Long | @Id; id |
| enrollmentId | Long | enrollment_id |
| studentId | Long | student_id |
| classId | Long | class_id |
| policyId | Long | policy_id |
| classRevision | long | class_revision |
| assessmentId | Long | assessment_id |
| assessmentVersion | Long | assessment_version |
| approvalId | Long | approval_id |
| reusedDecisionId | Long | reused_decision_id |
| intent | PlacementDto.Intent | intent |
| academicStatus | String | academic_status |
| fingerprint | String | fingerprint |
| policySnapshot | String | policy_snapshot |
| assessmentSnapshot | String | assessment_snapshot |
| actorId | Long | actor_id |
| actorName | String | actor_name |
| acceptedAt | LocalDateTime | accepted_at |
## academic.PlacementException

Bảng **placement_exceptions**; [source](../../../academic-service/src/main/java/com/example/academic/placement/PlacementException.java).

| Thuộc tính Java | Kiểu | Mapping |
|---|---|---|
| id | Long | @Id; id |
| version | long | @Version; version |
| studentId | Long | student_id |
| classId | Long | class_id |
| branchId | Long | branch_id |
| intent | PlacementDto.Intent | intent |
| sourceEnrollmentId | Long | source_enrollment_id |
| sourceVersion | Long | source_version |
| policyId | Long | policy_id |
| classRevision | long | class_revision |
| assessmentId | Long | assessment_id |
| assessmentVersion | Long | assessment_version |
| contextSnapshot | String | context_snapshot |
| fingerprint | String | fingerprint |
| academicStatus | String | academic_status |
| requestedReason | String | requested_reason |
| requestedBy | Long | requested_by |
| requestedByName | String | requested_by_name |
| requestedAt | LocalDateTime | requested_at |
| status | Status | status |
| decisionReason | String | decision_reason |
| decidedBy | Long | decided_by |
| decidedByName | String | decided_by_name |
| decidedAt | LocalDateTime | decided_at |
| consumedEnrollmentId | Long | consumed_enrollment_id |
| consumedAt | LocalDateTime | consumed_at |
## academic.ClassClosure

Bảng **class_closures**; [source](../../../academic-service/src/main/java/com/example/academic/results/ClassClosure.java).

| Thuộc tính Java | Kiểu | Mapping |
|---|---|---|
| classId | Long | @Id; class_id |
| revision | long | revision |
| resultRevision | long | result_revision |
| sourceRevision | long | source_revision |
| closed | boolean | closed |
| actorName | String | actor_name |
| actorId | Long | actor_id |
| changedAt | LocalDateTime | changed_at |
| reason | String | reason |
## academic.ClassClosureEvent

Bảng **class_closure_events**; [source](../../../academic-service/src/main/java/com/example/academic/results/ClassClosureEvent.java).

| Thuộc tính Java | Kiểu | Mapping |
|---|---|---|
| requestId | String | @Id; request_id |
| classId | Long | class_id |
| expectedRevision | long | expected_revision |
| revision | long | revision |
| closed | boolean | closed |
| actorName | String | actor_name |
| actorId | Long | actor_id |
| changedAt | LocalDateTime | changed_at |
| reason | String | reason |
## academic.ResultBarrier

Bảng **class_result_barriers**; [source](../../../academic-service/src/main/java/com/example/academic/results/ResultBarrier.java).

| Thuộc tính Java | Kiểu | Mapping |
|---|---|---|
| classId | Long | @Id; class_id |
| branchId | Long | branch_id |
| token | String | token |
| sourceRevision | long | source_revision |
| resultRevision | long | result_revision |
| confirmedCount | long | confirmed_count |
## academic.Room

Bảng **rooms**; [source](../../../academic-service/src/main/java/com/example/academic/room/Room.java); extends BaseEntity (id kế thừa).

| Thuộc tính Java | Kiểu | Mapping |
|---|---|---|
| code | String | code |
| name | String | name |
| location | String | location |
| capacity | Integer | capacity |
| equipment | String | equipment |
| status | Status | status |
| branchId | Long | branch_id |
## academic.Schedule

Bảng **schedules**; [source](../../../academic-service/src/main/java/com/example/academic/schedule/Schedule.java); extends BaseEntity (id kế thừa).

| Thuộc tính Java | Kiểu | Mapping |
|---|---|---|
| schoolClass | SchoolClass | ManyToOne; class_id |
| dayOfWeek | DayOfWeek | day_of_week |
| startTime | LocalTime | start_time |
| endTime | LocalTime | end_time |
## academic.SchoolClass

Bảng **school_classes**; [source](../../../academic-service/src/main/java/com/example/academic/schoolclass/SchoolClass.java); extends BaseEntity (id kế thừa).

| Thuộc tính Java | Kiểu | Mapping |
|---|---|---|
| expectedMinutes | Integer | expected_minutes |
| durationRevision | long | duration_revision |
| admissionPolicyId | Long | admission_policy_id |
| admissionRevision | long | admission_revision |
| code | String | code |
| name | String | name |
| course | Course | ManyToOne; course_id |
| teacherId | Long | teacher_id |
| teacherName | String | teacher_name |
| room | Room | ManyToOne; room_id |
| startDate | LocalDate | start_date |
| endDate | LocalDate | end_date |
| sessionEndDate | LocalDate | session_end_date |
| sessionsInitialized | boolean | sessions_initialized |
| maxSize | Integer | max_size |
| currentSize | Integer | current_size |
| status | Status | status |
| statusManuallySet | boolean | status_manually_set |
| branchId | Long | branch_id |
| schedules | List<Schedule> | OneToMany; không cột scalar riêng |
## academic.ClassSession

Bảng **class_sessions**; [source](../../../academic-service/src/main/java/com/example/academic/session/ClassSession.java); extends BaseEntity (id kế thừa).

| Thuộc tính Java | Kiểu | Mapping |
|---|---|---|
| schoolClass | SchoolClass | ManyToOne; class_id |
| sessionDate | LocalDate | session_date |
| startTime | LocalTime | start_time |
| endTime | LocalTime | end_time |
| roomId | Long | room_id |
| roomName | String | room_name |
| plannedTeacherId | Long | planned_teacher_id |
| plannedTeacherName | String | planned_teacher_name |
| teacherId | Long | teacher_id |
| teacherName | String | teacher_name |
| status | Status | status |
| kind | Kind | kind |
| sourceDate | LocalDate | source_date |
| sourceScheduleId | Long | source_schedule_id |
| makeupForId | Long | makeup_for_id |
| makeupSessionId | Long | makeup_session_id |
| customized | boolean | customized |
| reason | String | reason |
| version | long | @Version; version |
| createdAt | LocalDateTime | created_at |
| updatedAt | LocalDateTime | updated_at |
## academic.SessionEvent

Bảng **session_events**; [source](../../../academic-service/src/main/java/com/example/academic/session/SessionEvent.java); extends BaseEntity (id kế thừa).

| Thuộc tính Java | Kiểu | Mapping |
|---|---|---|
| session | ClassSession | ManyToOne; session_id |
| action | String | action |
| reason | String | reason |
| beforeState | String | before_state |
| afterState | String | after_state |
| actorId | Long | actor_id |
| actorName | String | actor_name |
| occurredAt | LocalDateTime | occurred_at |
## academic.Student

Bảng **students**; [source](../../../academic-service/src/main/java/com/example/academic/student/Student.java); extends BaseEntity (id kế thừa).

| Thuộc tính Java | Kiểu | Mapping |
|---|---|---|
| version | long | @Version; version |
| studentCode | String | student_code |
| guardianName | String | guardian_name |
| guardianPhone | String | guardian_phone |
| fullName | String | full_name |
| dateOfBirth | LocalDate | date_of_birth |
| gender | Gender | gender |
| email | String | email |
| phone | String | phone |
| address | String | address |
| contactPerson | String | contact_person |
| currentLevel | String | current_level |
| registeredAt | LocalDate | registered_at |
| status | Status | status |
| branchId | Long | branch_id |
## teaching.Document

Bảng **documents**; [source](../../../teaching-service/src/main/java/com/example/teaching/document/Document.java); extends BaseEntity (id kế thừa).

| Thuộc tính Java | Kiểu | Mapping |
|---|---|---|
| revision | int | revision |
| lastSubmittedAt | LocalDateTime | last_submitted_at |
| branchId | Long | branch_id |
| title | String | title |
| classId | Long | class_id |
| courseId | Long | course_id |
| storageKey | String | storage_key |
| originalFilename | String | original_filename |
| fileSize | Long | file_size |
| description | String | description |
| uploadedBy | Long | uploaded_by |
| uploadedByName | String | uploaded_by_name |
| uploadedAt | LocalDateTime | uploaded_at |
| status | Status | status |
| reviewedBy | Long | reviewed_by |
| reviewedByName | String | reviewed_by_name |
| reviewedAt | LocalDateTime | reviewed_at |
| rejectReason | String | reject_reason |
| version | Long | @Version; version |
## teaching.DocumentEvent

Bảng **document_events**; [source](../../../teaching-service/src/main/java/com/example/teaching/document/DocumentEvent.java); extends BaseEntity (id kế thừa).

| Thuộc tính Java | Kiểu | Mapping |
|---|---|---|
| documentId | Long | document_id |
| revision | int | revision |
| action | String | action |
| status | String | status |
| storageKey | String | storage_key |
| title | String | title |
| description | String | description |
| filename | String | filename |
| fileSize | Long | file_size |
| reason | String | reason |
| actorId | Long | actor_id |
| actorName | String | actor_name |
| occurredAt | LocalDateTime | occurred_at |
## teaching.EvaluationPolicy

Bảng **evaluation_policies**; [source](../../../teaching-service/src/main/java/com/example/teaching/evaluation/EvaluationPolicy.java).

| Thuộc tính Java | Kiểu | Mapping |
|---|---|---|
| id | Long | @Id; id |
| classId | Long | class_id |
| branchId | Long | branch_id |
| courseId | Long | course_id |
| revision | long | revision |
| passPercent | BigDecimal | pass_percent |
| minAttendancePercent | BigDecimal | min_attendance_percent |
| actorId | Long | actor_id |
| actorName | String | actor_name |
| confirmedAt | LocalDateTime | confirmed_at |
| reason | String | reason |
| rules | List<Rule> | ElementCollection; không cột scalar riêng |

Embeddable Rule: examId:Long, examVersion:long, examName:String, examDate:LocalDate, required:boolean, weight:BigDecimal, maxScore:BigDecimal. Không phải entity độc lập.

Collection tables: evaluation_policy_exams.
## teaching.EvaluationState

Bảng **evaluation_states**; [source](../../../teaching-service/src/main/java/com/example/teaching/evaluation/EvaluationState.java).

| Thuộc tính Java | Kiểu | Mapping |
|---|---|---|
| classId | Long | @Id; class_id |
| branchId | Long | branch_id |
| revision | long | revision |
| sourceRevision | long | source_revision |
## teaching.Exam

Bảng **exams**; [source](../../../teaching-service/src/main/java/com/example/teaching/exam/Exam.java); extends BaseEntity (id kế thừa).

| Thuộc tính Java | Kiểu | Mapping |
|---|---|---|
| version | long | @Version; version |
| branchId | Long | branch_id |
| classId | Long | class_id |
| name | String | name |
| examType | String | exam_type |
| examDate | LocalDate | exam_date |
| weight | BigDecimal | weight |
| maxScore | BigDecimal | max_score |
| rosterFrozenAt | LocalDateTime | roster_frozen_at |
| rosterFrozenBy | Long | roster_frozen_by |
| rosterFrozenByName | String | roster_frozen_by_name |
| createdBy | Long | created_by |
| createdByName | String | created_by_name |
| createdAt | LocalDateTime | created_at |
## teaching.ExamCandidate

Bảng **exam_candidates**; [source](../../../teaching-service/src/main/java/com/example/teaching/exam/ExamCandidate.java); extends BaseEntity (id kế thừa).

| Thuộc tính Java | Kiểu | Mapping |
|---|---|---|
| enrollmentId | Long | enrollment_id |
| examId | Long | exam_id |
| studentId | Long | student_id |
| studentName | String | student_name |
| source | String | source |
## teaching.Grade

Bảng **grades**; [source](../../../teaching-service/src/main/java/com/example/teaching/grade/Grade.java); extends BaseEntity (id kế thừa).

| Thuộc tính Java | Kiểu | Mapping |
|---|---|---|
| exam | Exam | ManyToOne; exam_id |
| branchId | Long | branch_id |
| studentId | Long | student_id |
| studentName | String | student_name |
| score | BigDecimal | score |
| enteredBy | Long | entered_by |
| enteredByName | String | entered_by_name |
| enteredAt | LocalDateTime | entered_at |
| lastModifiedBy | Long | last_modified_by |
| lastModifiedByName | String | last_modified_by_name |
| lastModifiedAt | LocalDateTime | last_modified_at |
| modificationReason | String | modification_reason |
| comment | String | comment |
| version | Long | @Version; version |
## teaching.GradeChange

Bảng **grade_changes**; [source](../../../teaching-service/src/main/java/com/example/teaching/grade/GradeChange.java); extends BaseEntity (id kế thừa).

| Thuộc tính Java | Kiểu | Mapping |
|---|---|---|
| gradeId | Long | grade_id |
| oldScore | BigDecimal | old_score |
| newScore | BigDecimal | new_score |
| modifiedBy | Long | modified_by |
| modifiedByName | String | modified_by_name |
| modifiedAt | LocalDateTime | modified_at |
| reason | String | reason |
## teaching.FinalResult

Bảng **final_results**; [source](../../../teaching-service/src/main/java/com/example/teaching/results/FinalResult.java).

| Thuộc tính Java | Kiểu | Mapping |
|---|---|---|
| id | Long | @Id; id |
| classId | Long | class_id |
| branchId | Long | branch_id |
| enrollmentId | Long | enrollment_id |
| studentId | Long | student_id |
| revision | long | revision |
| policyRevision | long | policy_revision |
| academicSourceRevision | long | academic_source_revision |
| teachingSourceRevision | long | teaching_source_revision |
| requestId | String | request_id |
| fingerprint | String | fingerprint |
| outcome | String | outcome |
| state | String | state |
| reason | String | reason |
| confirmedBy | Long | confirmed_by |
| confirmedByName | String | confirmed_by_name |
| confirmedAt | LocalDateTime | confirmed_at |
| snapshot | String | snapshot |
| reopenedBy | Long | reopened_by |
| reopenedByName | String | reopened_by_name |
| reopenedAt | LocalDateTime | reopened_at |
| reopenReason | String | reopen_reason |
## teaching.ResultBarrier

Bảng **class_result_barriers**; [source](../../../teaching-service/src/main/java/com/example/teaching/results/ResultBarrier.java).

| Thuộc tính Java | Kiểu | Mapping |
|---|---|---|
| classId | Long | @Id; class_id |
| branchId | Long | branch_id |
| token | String | token |
| sourceRevision | long | source_revision |
| resultRevision | long | result_revision |
| confirmedCount | long | confirmed_count |
## teaching.ResultReopen

Bảng **result_reopens**; [source](../../../teaching-service/src/main/java/com/example/teaching/results/ResultReopen.java).

| Thuộc tính Java | Kiểu | Mapping |
|---|---|---|
| requestId | String | @Id; request_id |
| classId | Long | class_id |
| previousRevision | long | previous_revision |
| actorId | Long | actor_id |
| actorName | String | actor_name |
| createdAt | LocalDateTime | created_at |
| reason | String | reason |
## finance.PaymentCorrection

Bảng **payment_corrections**; [source](../../../finance-service/src/main/java/com/example/finance/ledger/PaymentCorrection.java); extends BaseEntity (id kế thừa).

| Thuộc tính Java | Kiểu | Mapping |
|---|---|---|
| paymentId | Long | payment_id |
| tuitionId | Long | tuition_id |
| branchId | Long | branch_id |
| type | Type | type |
| amount | BigDecimal | amount |
| paymentMethod | Payment.PaymentMethod | payment_method |
| referenceCode | String | reference_code |
| reason | String | reason |
| requestKey | String | request_key |
| expectedVersion | Long | expected_version |
| recordedBy | Long | recorded_by |
| recordedByName | String | recorded_by_name |
| createdAt | LocalDateTime | created_at |
## finance.TuitionAdjustment

Bảng **tuition_adjustments**; [source](../../../finance-service/src/main/java/com/example/finance/ledger/TuitionAdjustment.java); extends BaseEntity (id kế thừa).

| Thuộc tính Java | Kiểu | Mapping |
|---|---|---|
| tuitionId | Long | tuition_id |
| branchId | Long | branch_id |
| oldOriginal | BigDecimal | old_original |
| oldDiscount | BigDecimal | old_discount |
| oldAmount | BigDecimal | old_amount |
| oldDueDate | LocalDate | old_due_date |
| newOriginal | BigDecimal | new_original |
| newDiscount | BigDecimal | new_discount |
| newAmount | BigDecimal | new_amount |
| newDueDate | LocalDate | new_due_date |
| paidAtChange | BigDecimal | paid_at_change |
| reason | String | reason |
| requestKey | String | request_key |
| expectedVersion | Long | expected_version |
| recordedBy | Long | recorded_by |
| recordedByName | String | recorded_by_name |
| createdAt | LocalDateTime | created_at |
## finance.Payment

Bảng **payments**; [source](../../../finance-service/src/main/java/com/example/finance/payment/Payment.java); extends BaseEntity (id kế thừa).

| Thuộc tính Java | Kiểu | Mapping |
|---|---|---|
| tuitionId | Long | tuition_id |
| amount | BigDecimal | amount |
| paymentMethod | PaymentMethod | payment_method |
| referenceCode | String | reference_code |
| receiptNo | String | receipt_no |
| requestKey | String | request_key |
| paidAt | LocalDateTime | paid_at |
| status | Status | status |
| branchId | Long | branch_id |
| recordedBy | Long | recorded_by |
| recordedByName | String | recorded_by_name |
| note | String | note |
## finance.Tuition

Bảng **tuition**; [source](../../../finance-service/src/main/java/com/example/finance/tuition/Tuition.java); extends BaseEntity (id kế thừa).

| Thuộc tính Java | Kiểu | Mapping |
|---|---|---|
| version | long | @Version; version |
| enrollmentId | Long | enrollment_id |
| studentId | Long | student_id |
| studentName | String | student_name |
| classId | Long | class_id |
| className | String | class_name |
| courseId | Long | course_id |
| courseName | String | course_name |
| originalAmount | BigDecimal | original_amount |
| discountAmount | BigDecimal | discount_amount |
| amount | BigDecimal | amount |
| paidAmount | BigDecimal | paid_amount |
| currency | String | currency |
| dueDate | LocalDate | due_date |
| status | Status | status |
| branchId | Long | branch_id |
| adjustedBy | Long | adjusted_by |
| adjustedByName | String | adjusted_by_name |
| adjustedAt | LocalDateTime | adjusted_at |
| adjustmentReason | String | adjustment_reason |
