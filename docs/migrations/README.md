# Migration V002 — tài khoản và phiên

## Cài mới và mốc11/11/2026

Database rỗng: tạo schema rồi chọn schema đó trong MySQL và chạy `SOURCE docs/migrations/schema-v013-clean.sql;` bằng đường dẫn thực trên máy. File chỉ có cấu trúc V013, không có dữ liệu/tài khoản và không DROP bảng; không dùng để sửa database đang có dữ liệu. Đã kiểm tra53bảng/625cột và replay13migration trên schema thử riêng.

Khởi tạo Super Admin một lần bằng auth JAR với profile bootstrap và cấu hình riêng: DB_URL/USERNAME/PASSWORD, JWT_SECRET, auth.bootstrap.enabled=true, auth.bootstrap.username, auth.bootstrap.password (12–72byte), spring.jpa.hibernate.ddl-auto=validate. Không bật bootstrap trong prod. Dừng tiến trình bootstrap, bỏ/tắt bootstrap, rồi chạy năm service với prod/validate và cấu hình production riêng. Không lưu mật khẩu thực vào source.

Database nguồn đã được áp dụng V011 ngày11/11 sau backup/verify, giữ nguyên dữ liệu/tài khoản. Máy khác cần kiểm `courses.duration` nullable để tạo khóa chỉ có expectedMinutes; không coi có cột expected_minutes là đã chạy đủ migration. [Bằng chứng](../reviews/2026-11-11-phase-5-completion.md). Phần migration database cũ bên dưới vẫn áp dụng.

Áp dụng [V002__account_security.sql](V002__account_security.sql) sau V001 và sau khi sao lưu. Thêm session_version, must_change_password, password_changed_at và account_security_events; không sửa mật khẩu hiện có. Database local đã áp dụng và đối chiếu hash mật khẩu. Chạy lại không reset phiên/cờ đổi mật khẩu. [Hướng dẫn](../improvements/phase-two-account-security.md).

# Migration mới: phân công chi nhánh

Áp dụng [V001__account_branch_assignments.sql](V001__account_branch_assignments.sql) sau khi sao lưu và trước khi chạy bản hỗ trợ nhiều chi nhánh. Thêm account_branches/account_branch_events, giữ accounts.branch_id làm chi nhánh chính; không xóa dữ liệu. Chạy lại không khôi phục phân công đã thu hồi. Database local đã áp dụng và kiểm tra. Xem [quy tắc](../improvements/phase-one-multi-branch.md).

# Existing MySQL database

The broad legacy migration below has not been executed against the user's database.
On 2026-11-04, the narrow [student contact migration](phase-three-student-contact.sql)
was applied to the current language_center_db after backing up students. It removes
uk_student_phone_branch and fills missing student codes; no student rows are deleted.
Stage 3 was also exercised against MySQL; automated Java tests still use in-memory H2.
For an empty database, the services can create their own tables with DDL_AUTO=update.
For existing data, follow the procedure below on a restored copy first.

1. Stop every old service, especially teaching: its old ORM mappings touch academic/auth
   tables. Back up schema/data and any document directory. Confirm the intended server:
   auth previously used port 3306, the other services used 3308. Consolidate data deliberately;
   matching database names on two MySQL instances do not mean they share records.
2. Select that database and run [preflight.sql](preflight.sql). Inspect constraints and
   conflicting class_id/school_class_id, recorded_by/recorded_by_account_id mappings.
3. Review and run [prepare-existing-schema.sql](prepare-existing-schema.sql) on the restored
   copy. It removes obsolete phone/global-code uniqueness, adds nullable migration columns,
   preserves legacy columns/rows and copies values where their meaning is known. The script
   requires DDL/routine privileges; MySQL DDL is not transactional. Its helper procedures
   are dropped on success; after an interrupted run, inspect and remove only those five
   `lc_*` procedures before re-running.
4. Resolve the script's diagnostic rows before enabling traffic:
   - Map each legacy Tuition to its actual Enrollment. Do not infer it automatically if
     a student has repeated/transferred registrations. Mark delivery CREATED only after
     checking that mapping; otherwise a retry could create another bill.
   - Reconcile orphan/cross-branch/overpaid payments and legacy PENDING/PARTIAL/REFUNDED
     records. The old API allowed edits and arbitrary status changes, so database values
     alone cannot prove the actual money received. Only PAID receipts count in this release.
   - Check historical tuition prices: legacy code did not snapshot enrollment fees. The
     preparation script uses the current course fee only as a provisional value.
   - Confirm missing enrolledAt dates from actual registration records. Create
     enrollment_periods for historical registrations after schema creation, using their
     known effective dates. Saved attendance remains visible even without old periods.
   - Complete old exam date/type/maxScore/weight from real records. No scoring rules are
     invented by the script.
   - Copy any retained document to private storage using a generated UUID key, then fill
     storage_key and branch/author metadata. Old external fileUrl values are not fetched.
   - Legacy teaching used a hard-coded attendance author; those historical audit values
     cannot be repaired reliably without external records.
   - Normalize room ACTIVE to AVAILABLE only when it means the room is usable; check
     status values and legacy global uniqueness before creating new branch-local codes.
5. On that restored copy, run the new services with DDL_AUTO=update to add their remaining
   owned columns/indexes and the new supporting tables. Review Hibernate DDL errors; stop
   if a required field cannot be made non-null. Verify unique tuition.enrollment_id,
   payments.receipt_no and (branch_id,request_key), grades(exam_id,student_id).
6. Backfill display snapshots/collector metadata from validated records. A one-time migration
   may join service-owned tables; application code must not use those joins across services.
7. Test login -> lead conversion -> enrollment -> tuition -> split payments -> attendance ->
   grades -> document approval/download via gateway. Also test branch isolation and concurrent
   enrollment/payment requests against MySQL. Then repeat the reviewed migration for the
   intended database and use DDL_AUTO=validate for normal operation.

No DROP TABLE, TRUNCATE or automatic database reset is part of this migration. Deferred
Expense/Promotion/Invoice/Facility tables may remain for archival purposes; their incomplete
public APIs have been removed.

## V003 — buổi học thực tế

Áp dụng [V003__class_sessions.sql](V003__class_sessions.sql) sau V001/V002 và sao lưu; local đã áp dụng và thử replay với điểm danh cũ. Không reset tài khoản/dữ liệu. [Hướng dẫn, tiền điều kiện và giới hạn](../improvements/phase-three-class-sessions.md).

## V004 — vòng đời và roster thi

Sao lưu rồi áp dụng [V004__learning_lifecycle.sql](V004__learning_lifecycle.sql) sau V003, trước khi chạy bản mới. Local đã áp dụng; giữ dữ liệu/tài khoản/điểm cũ. Không suy diễn lịch sử thiếu. [Chính sách và quay lại](../improvements/phase-four-learning-lifecycle.md).

## V005 — sổ tài chính

Sao lưu trước [V005__finance_ledger.sql](V005__finance_ledger.sql), sau V004. Thêm version/journal, không biến đổi phiếu cũ hay tự sửa paidAmount. Local đã áp dụng và thử replay sau các giao dịch. [Chính sách và phục hồi](../improvements/phase-five-finance-ledger.md).

## V006 — lead và revision tài liệu

Sau V005, backup SQL và file rồi chạy [V006](V006__lead_care_document_revisions.sql) thủ công. Local đã áp dụng/replay, giữ version, lịch hẹn, audit và quyết định/file hiện tại. [Quy trình và giới hạn](../improvements/phase-six-care-documents-experience.md).

## Kiểm tra trước phát hành

Dùng `scripts/operations.cjs` để backup, verify và restore sang schema rehearsal mới; replay V001–V006 trên bản sao trước khi nâng cấp database thật. Chạy bốn service bằng profile prod/validate. Xem [runbook giai đoạn 7](../improvements/phase-seven-deployment.md). Không tự DROP database diễn tập hoặc ghi đè schema nguồn.

## V007 — hồ sơ và vòng đời giáo viên

[V007__teacher_profile_lifecycle.sql](V007__teacher_profile_lifecycle.sql) thêm teachers.version mặc định 0 và teacher_profile_events; giữ workStatus/account/snapshot cũ. Backup SQL/file trước áp dụng; đã apply/replay trên database restore riêng, không reset version/audit khi chạy lại. Database nguồn chưa áp dụng. Nâng Academic có endpoint responsibilities trước Auth mới. [G02/G03](../thesis-progress/G03.md).

## V008/V009 — chương trình, điều kiện và đánh giá đầu vào

1. Backup SQL/file, xác nhận database; restore và kiểm tra bản sao trước nâng cấp. Dừng Academic cũ trong cửa sổ bảo trì khi nâng schema chính.
2. Áp dụng V007 nếu chưa có, rồi [V008__programs_course_admission.sql](V008__programs_course_admission.sql), sau đó [V009__placement_assessments.sql](V009__placement_assessments.sql) bằng MySQL8 trên schema đã chọn. Đây là migration thủ công; Maven/Spring không tự chạy các file SQL này. Chạy cả file, không chỉ một đoạn CREATE.
3. V008 tạo programs/program_levels/course_admission_policies/course_admission_levels/class_admission_events; thêm school_classes.admission_policy_id nullable và admission_revision mặc định0. V009 tạo placement_assessments/assessment_events, current_key generated STORED unique cho một CONFIRMED/student/program.
4. Khởi động Academic mới với prod/validate; kiểm tra readiness và API mới qua gateway. Không seed chương trình/trình độ, không đổi Course.type/level/entryRequirement, Student.currentLevel hoặc Course.duration. SA tạo danh mục từ dữ liệu đã đối chiếu, SA/A xác minh từng khóa; quản lý áp dụng bản khóa cho lớp cũ tường minh.
5. Kiểm tra nguồn/ngày/người đánh giá rồi xác nhận nháp học viên; text cũ chưa đủ chứng cứ không được tạo CONFIRMED. Phân biệt đầu vào và đầu ra, không dùng rank để quy đổi thang.

Ngày **08/11/2026**, V001–V009 đã apply trên schema restore riêng, Academic chạy prod/validate và API/UI qua gateway đạt. Replay V008/V009 sau khi có dữ liệu giữ toàn bộ rows mới/audit và liên kết lớp. Database nguồn **chưa áp dụng V007/V008/V009** trong đợt này. DDL MySQL không có rollback transaction: nếu lỗi phải đối soát schema hoặc phục hồi backup đã kiểm chứng; không drop bảng mới đã có chứng cứ để hạ phiên bản. Backup này là online rehearsal chưa được chứng nhận điểm phục hồi production.

## V010 — chấp nhận đầu vào và ngoại lệ phân lớp

Sau backup SQL/file và kiểm chứng bản sao, áp dụng [V010__enrollment_placement.sql](V010__enrollment_placement.sql) sau V009, trước Academic mới. Thêm placement_exceptions và enrollment_admissions, index và FK bảo vệ lịch sử; không cập nhật enrollment/student/course cũ. History rỗng là LEGACY_UNVERIFIED, không tự coi là đủ điều kiện hoặc tự hủy đăng ký.

V010 đã apply/replay trên schema restore riêng ngày **08/11/2026** và Academic prod/validate chạy được. So sánh toàn rows đề nghị, snapshot và enrollment trước/sau replay không đổi. Database chính chưa nâng V007–V010. Maven/Spring không tự chạy các SQL này; cần áp dụng thủ công các migration còn thiếu theo thứ tự. DDL MySQL không rollback transaction; sao lưu/restore bản đã kiểm chứng nếu nâng cấp thất bại, không drop bảng đã có bằng chứng.

Khi nâng ứng dụng có legacy: đối soát chương trình/đầu vào khóa và áp dụng cho lớp, ghi nhận assessment thật hoặc đề nghị quản lý xác nhận ngoại lệ theo chứng cứ. Không tự seed mapping hoặc điền xác nhận hàng loạt để bỏ qua dữ liệu chưa rõ.

## V011 — kế hoạch phút đã xác minh

Áp dụng [V011__verified_duration_plans.sql](V011__verified_duration_plans.sql) sau V010, sau backup SQL/file và kiểm chứng restore. Thêm courses.expected_minutes/duration_revision, school_classes.expected_minutes/duration_revision, course_duration_plans/class_duration_plans; cho phép courses.duration=null để khóa mới không bịa giá trị legacy. Không cập nhật số duration hiện có, không seed phút hoặc lịch sử xác minh cho dữ liệu cũ.

Ngày **08/11/2026**, V001–V011 đã diễn tập trên schema restore mới, Academic prod/validate và API/UI chạy được. Replay V011 giữ nguyên giá trị legacy, expectedMinutes/revision, lịch sử và session. Database chính chưa được nâng V011 trong đợt này; các V007–V010 còn thiếu phải áp dụng trước. Maven/Spring không tự thực thi các SQL này. DDL MySQL không rollback transaction; backup và kiểm chứng bản sao trước nâng, không drop bảng đã có audit để hạ phiên bản.

Đối soát: SA/A đọc giáo trình/kế hoạch thật, nhập số phút và lý do ở Xem khóa → Thời lượng; từng lớp cũ áp dụng bản khóa hoặc xác nhận kế hoạch riêng có lý do. Không dùng tổng lịch hiện có làm kế hoạch khóa tự động. Buổi cũ thiếu giờ vẫn UNKNOWN; không sửa gián tiếp bằng thay duration. Giữ các lớp đã terminal và chứng cứ cũ; việc sửa nguồn/chốt kết quả thuộc G11.

## V012 — quy tắc đánh giá lớp

Sau backup SQL/file và thử trên restore, áp dụng [V012__class_evaluation_policies.sql](V012__class_evaluation_policies.sql) sau V011. Thêm exams.version mặc định0, exam_candidates.enrollment_id nullable; tạo evaluation_states/evaluation_policies/evaluation_policy_exams thuộc Teaching. Không sinh policy50%, không đánh dấu bài cũ bắt buộc, không gán enrollment cho roster cũ hoặc sửa điểm.

Nâng Academic có evaluation-context và DatedRoster mapping mới trước hoặc cùng Teaching; cả hai cần restart, FE cập nhật cùng vì reports/evaluations đổi hợp đồng. Teacher nhập điểm mới freeze enrollmentId khi có nguồn duy nhất; legacy thiếu nguồn vẫn giữ để đối soát, không tự bịa.

V001–V012 apply trên schema restore mới ngày08/11/2026; prod/validate/API/UI và replay V012 giữ policy revisions/thresholds/required/grades/frozen mappings. Database gốc chưa nâng V012 trong đợt này, các migration còn thiếu trước đó phải áp dụng theo thứ tự. Maven/Spring không tự chạy các SQL migration này. DDL MySQL không rollback transaction; backup và phục hồi bản đã kiểm chứng nếu lỗi, không drop audit để hạ phiên bản.

## V013 — kết quả chính thức và hồ sơ học tập

Sau backup SQL/file và thử restore, áp dụng [V013](V013__final_results_learning_profile.sql) sau V012, lúc Academic/Teaching ngừng ghi. Thêm shared class_result_barriers, evaluation_states.source_revision, enrollments.completion_result_id/revision, final_results/result_reopens/class_closures/class_closure_events. MySQL generated unique current_enrollment_id đảm bảo tối đa một current CONFIRMED. Không backfill PASS, refs, actor hoặc bằng chứng đóng lớp cho terminal cũ.

Academic/Teaching hiện bắt buộc cùng schema MySQL cho hạ tầng khóa metadata này; không cấu hình hai database tách biệt rồi bỏ kiểm token. Nâng đồng thời hai service và FE. V013 đã apply/replay trên rehearsal, prod/validate và API/UI/race đạt; source database vẫn chưa nâng các migration sau V006. Maven không tự chạy SQL. DDL MySQL không rollback: giữ bản backup, không drop audit để hạ phiên bản. [G11](../thesis-progress/G11.md), [G12](../thesis-progress/G12.md).

