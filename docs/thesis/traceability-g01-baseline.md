# Ma trận truy vết G01

Ngày: **07/11/2026**. Đọc cùng [requirements](requirements.md) và [business-rules](business-rules.md).

**Mức bằng chứng G01:** static code review. Tất cả mã `TC-RQxx-yy` dưới đây là **kịch bản nghiệm thu dự kiến**, chưa được triển khai/chạy trong G01. Tên test source cũ chỉ giúp tìm nơi mở rộng; không được gán một ca mới là đã đạt vì có file test đó.

## 1. Use case dự kiến

| Mã | Tên | Actor chính |
|---|---|---|
| UC01 | Đăng nhập, mật khẩu, phiên và liên hệ cá nhân | SA/A/S/T |
| UC02 | Chi nhánh và phân công nhân sự | SA; các role khác xem phạm vi được phép |
| UC03 | Quản lý học viên, trạng thái và lịch sử | SA/A/S |
| UC04 | Quản trị hồ sơ giáo viên | SA/A đủ toàn bộ phạm vi |
| UC05 | Bàn giao và ngừng làm việc giáo viên | SA/A đủ phạm vi |
| UC06 | Quản lý chương trình, khóa và thời lượng | SA; A/S theo từng quyền đã chốt |
| UC07 | Mở lớp, phòng, lịch và phân công | SA/A/S theo quyền hiện có |
| UC08 | Ghi nhận đánh giá đầu vào | SA/A/S theo chi nhánh |
| UC09 | Gợi ý lớp, ghi danh, bảo lưu/học lại/chuyển cùng khóa | SA/A/S |
| UC10 | Xác nhận ngoại lệ đầu vào | S/A đề nghị; SA/A duyệt |
| UC11 | Nghỉ/bù/dạy thay và điểm danh | SA/A/S/T theo từng hành vi |
| UC12 | Bài kiểm tra, nhập/sửa điểm và đánh giá tạm | SA/A/T theo quyền hiện có |
| UC13 | Xác nhận/mở lại kết quả và hoàn tất học vụ | SA/A; S hoàn tất đăng ký sau xác nhận |
| UC14 | Học phí, thu tiền, điều chỉnh/đảo/hoàn | SA/A/S theo quyền tài chính hiện có |
| UC15 | Tìm kiếm, dashboard, thống kê và báo cáo | Các role theo nguồn dữ liệu; báo cáo tổng hợp SA/A |
| UC16 | Tài liệu giảng dạy, duyệt và revision | SA/A/S/T theo quyền hiện có |
| UC17 | Backup/restore và bàn giao | Người vận hành được ủy quyền, không tạo role ứng dụng mới |

G13 sẽ đặc tả/vẽ Use Case đầy đủ; bảng này không thay thế bộ UML cuối cùng.

## 2. Yêu cầu → UI/API/dữ liệu/test

API hiện có dùng prefix `/api/v1/`. Nhóm đích **[mới]** chưa có trong G01; tên endpoint cuối cùng do giai đoạn cài đặt kiểm tra/đặc tả, không được hiểu như API đã chạy.

| RQ / UC | Màn hình hiện tại hoặc đích | API/service và dữ liệu căn cứ | Quy tắc / test dự kiến | Giai đoạn, trạng thái G01 |
|---|---|---|---|---|
| RQ01 / UC03,08,09 | `/students`, `/enrollment`; tab đầu vào/học tập [mới] | `academic/students`, `students/{id}/history`, `enrollments`; Student, Enrollment, LearningEvent; Assessment [mới] | BR-01/05/06/11; TC-RQ01-01, TC-RQ01-02, TC-RQ01-03 | Có CRUD tĩnh; bổ sung G05/G12, nghiệm thu G17–18 |
| RQ02 / UC04,05 | `/teachers` → AccountManagement hoặc TeacherDirectory theo role | `auth/accounts`, `auth/teachers/{id}`, `accounts/{id}/branches`; Teacher/Account; quản trị update/version/audit [mới] | BR-01/02/03; TC-RQ02-01, TC-RQ02-02, TC-RQ02-03, TC-RQ02-04 | Thiếu update/offboarding; G02–03/G18 |
| RQ03 / UC06 | `/courses`, thông tin lớp | `academic/courses`; Course; Program/Level/admission policy/expectedMinutes [mới] | BR-04/08; TC-RQ03-01, TC-RQ03-02, TC-RQ03-03 | Có text/fee; thiếu cấu trúc/đơn vị; G04/G08 |
| RQ04 / UC07,09,13 | `/classes`, `/rooms`, `/enrollment` | `academic/classes`, `rooms`, `enrollments/.../status,transfer,billing`; SchoolClass, Enrollment, EnrollmentPeriod | BR-01/06/07/10; TC-RQ04-01, TC-RQ04-02, TC-RQ04-03, TC-RQ04-04 | Có luồng chính; bổ sung điều kiện/hoàn tất G06–07/G11, nghiệm thu G17–18 |
| RQ05 / UC08,09,10 | Student assessment, preview/gợi ý, duyệt ngoại lệ [mới] | Hiện `EnrollmentService.check`; ProgramLevel/Assessment/AdmissionSnapshot/Approval [mới] | BR-04/05/06/07; TC-RQ05-01, TC-RQ05-02, TC-RQ05-03, TC-RQ05-04 | Chưa có quy tắc học thuật; G04–07/G18 |
| RQ06 / UC07,11 | `/schedule`, `/my-schedule`, `/attendance`, chi tiết lớp | `academic/schedules`, `sessions`, `attendances`; ClassSession/SessionEvent/Attendance | BR-03/08; TC-RQ06-01, TC-RQ06-02, TC-RQ06-03 | Có buổi/audit; thêm tổng giờ G09, nghiệm thu G17–18 |
| RQ07 / UC11,12,13 | `/attendance`, `/exams`, `/grades`, `/reports`, hồ sơ học tập [mới] | `teaching/exams`, `grades`, `reports/evaluations`; Exam/ExamCandidate/Grade/GradeChange; policy/final result [mới] | BR-08/09/10; TC-RQ07-01, TC-RQ07-02, TC-RQ07-03, TC-RQ07-04, TC-RQ07-05 | Có điểm tạm; chưa chốt cuối; G09–12/G18 |
| RQ08 / UC09,14 | `/tuition`, `/payments`, billing enrollment | `finance/tuition`, `payments`, ledger và Academic tuition retry; Tuition/Payment/TuitionAdjustment/PaymentCorrection | BR-06/11; TC-RQ08-01, TC-RQ08-02, TC-RQ08-03 | Có luồng trong code, cần regression/đối soát G17–18 |
| RQ09 / UC15 | `/dashboard`, `/reports`, filter danh sách | `academic/stats/overview`, `finance/stats/overview`, `teaching/reports/evaluations` | BR-02/09/10/11; TC-RQ09-01, TC-RQ09-02, TC-RQ09-03 | Có báo cáo; đồng bộ semantic/tìm tên G02/G12, nghiệm thu G17–18 |
| RQ10 / UC01,02, các UC nghiệp vụ | ProtectedRoute/navigation/branch selector và form | Auth live `/me`, SecurityConfig/BranchScope/TeacherDirectory; version/lock/audit | BR-00/01 và các invariant tương ứng; TC-RQ10-01, TC-RQ10-02, TC-RQ10-03 | Có nền; kiểm tra theo G02–12/G18 |
| RQ11 / các UC, UML | `docs/domain-model.puml`, API/decisions, bộ thesis [mới] | Entity/migration/controllers/FE thực tế, không chỉ tài liệu cũ | BR-00/12; TC-RQ11-01, TC-RQ11-02, TC-RQ11-03 | Chưa đủ bộ cuối; G13–16 |
| RQ12 / quy trình, UC17 | README/demo/report/runbook | Test sources, verification, operations/package-release; baseline lịch sử | BR-00/12; TC-RQ12-01, TC-RQ12-02, TC-RQ12-03 | G01 có đặc tả/dàn ý; G17–20 mới nghiệm thu/bàn giao |

## 3. Nơi đọc code và mở rộng test

Đường dẫn dưới tương đối với backend root; FE là `../../language-center-web-frontend/` từ backend root.

| Nhóm | Nguồn implementation đã đọc | Nguồn test hiện có để tham khảo, không phải bằng chứng test mới |
|---|---|---|
| Nhân sự/quyền | `auth-service/src/main/java/com/example/auth/{controller,service,entity,dto,integration}`, Academic `security/TeacherDirectory.java` | Auth `AccountManagementUnitTest`, `BranchAssignmentsTest`, `AccountSecurityTest`; Academic `TeacherMembershipTest`; FE `multi-branch.test.mjs`, `account-security.test.mjs` |
| Học viên/ghi danh | Academic `student`, `enrollment`, `lifecycle`; FE `pages/StudentManagement`, `pages/EnrollmentManagement`, `services/admissionsService.ts` | `AdmissionTransactionTest`, `LearningLifecycleTest`, `AcademicRulesUnitTest`; FE `phase-three.test.mjs`, `learning-lifecycle.test.mjs` |
| Khóa/lớp/lịch/giờ | Academic `course`, `schoolclass`, `session`, `attendance`, `schedule`; FE `CourseManagement`, `ClassManagement`, `ScheduleManagement` | `CourseWriteRulesTest`, `SessionWorkflowTest`, `MultiBranchSecurityTest`; FE `phase-two.test.mjs`, `class-sessions.test.mjs` |
| Điểm/kết quả | Teaching `exam`, `grade`, `integration/AcademicDirectory`; Academic periods/attendance | `ExamRosterTest`, `TeachingRulesUnitTest`, `AcademicDirectoryUnitTest`; FE `phase-five.test.mjs`, `learning-lifecycle.test.mjs`; cần test mới G10–11 |
| Học phí/báo cáo | Finance `tuition`, `payment`, `ledger`, `stats/RevenueController`; Academic stats; FE Reports | `FinanceTransactionTest`, `FinanceLedgerWorkflowTest`, `RevenueReportTest`, `StatsQueryTest`; FE `finance-ledger.test.mjs`, `phase-six.test.mjs` |
| File/vận hành | Teaching `document`, service `operations`, `scripts/operations.cjs` và `package-release.cjs` | `DocumentRevisionWorkflowTest`, `OperationsHttpTest`, `scripts/operations.test.cjs`; FE `care-revisions.test.mjs` |

## 4. Bằng chứng và tình trạng

- **G01-E01:** đối chiếu source/route/entity/migration theo danh mục trên; xác nhận tĩnh, không gọi API runtime.
- **G01-E02:** baseline working tree + digest các file source/config/test/build được ghi trong `../thesis-progress/G01-evidence.json`; so sánh sau G01 để xác nhận không đổi code.
- **HIST-01:** [verification](../verification.md), phần cải thiện 6–7, ghi kết quả cũ; artifact local `storage/improvement7-results.json` có thông tin 195 BE / 72 FE baseline / 9 ops / 19 smoke / 16 role-CORS / restore 33 bảng và 4 file. Không chạy lại trong G01, không dùng để đánh dấu TC-RQ mới đã đạt.
- **PENDING:** runtime/schema thực tế ngày G01, khảo sát trung tâm, tính dễ sử dụng với người dùng thật, test tải và các chức năng mới G02–G12 chưa được xác minh. G17–G20 bổ sung bằng chứng sau khi cài đặt.

Giai đoạn sau cập nhật hàng RQ liên quan bằng link test report cụ thể, ngày/môi trường/expected/actual. Giữ rõ bằng chứng lịch sử và mới, không đổi một case planned thành passed chỉ bằng sửa Markdown.

## Bằng chứng cập nhật G02–G03

RQ02 / UC04–UC05: TeacherProfileController/Service/Event/V007 và TeacherProfilePanel/Form/WorkStatusForm đã cài đặt; TeacherResponsibilitiesController + BranchDirectory dùng cùng nguồn. TC-RQ02-01/02/03/04 có bằng chứng H2, MySQL/UI và race theo [G02](../thesis-progress/G02.md), [G03](../thesis-progress/G03.md). Phần tìm tên Teacher TC-RQ09-01 và quyền/stale thuộc TC-RQ10 đã kiểm tra cho phạm vi này; không đánh dấu cả RQ09/RQ10 toàn hệ thống đã hoàn tất. Assessment/admission/duration/final results vẫn planned G04–G12.

## Bổ sung bằng chứng G04/G05 — 08/11/2026

UC06/BR-04: ProgramCatalogService + AdmissionPolicyService, AdmissionPolicyWorkflowTest (12 ca), CourseAdmissionPanel/ClassAdmissionPanel/ProgramCatalog và API/UI MySQL. UC08/BR-05: PlacementAssessmentService, PlacementAssessmentWorkflowTest (15 ca), StudentAssessments/AssessmentForm, API/UI và race/replay MySQL. Xem [G04](../thesis-progress/G04.md) và [G05](../thesis-progress/G05.md). Không tự gán các TC dự kiến G01 đã đạt chỉ vì có test tên gần giống. UC09/UC10 về kiểm tra phù hợp/gợi ý/ngoại lệ vẫn chờ G06/G07; thời lượng/kết quả cuối khóa chờ G08–G11.

## G06/G07 đã triển khai — 08/11/2026

BR-06/BR-07 và UC09/UC10 đã có preview/gợi ý, kiểm tra lại khi lưu, snapshot và ngoại lệ theo role/chi nhánh, gồm transfer/resume/retry. 263 BE tests và 32 kiểm tra runtime được mô tả tại [G06](../thesis-progress/G06.md), [G07](../thesis-progress/G07.md). Các TC dự kiến ở G01 vẫn cần truy vết cụ thể, không tự gán toàn bộ đã đạt. T3 (G04–G07) hoàn thành phần triển khai; G08–G20 còn lại, không tuyên bố toàn đồ án hoàn tất.

## G08/G09 triển khai — 08/11/2026

BR-08 đã có kế hoạch phút/version/snapshot, tổng session thật, unknown và gate thời lượng đóng lớp. [G08](../thesis-progress/G08.md), [G09](../thesis-progress/G09.md) ghi26tests mới, 33runtimechecks và giới hạn. UC06/UC07/UC11 có bằng chứng liên quan, không tự đánh dấu toàn bộ TC G01 đã đạt. T4 còn G10–G12; kết quả cuối khóa và sửa nguồn ảnh hưởng kết quả thuộc G10/G11.

## G10 triển khai — 08/11/2026

BR-09/UC12 có policy lớp phiên bản, điểm chuẩn hóa trước rounding, required/optional/applicability, chuyên cần và trạng thái provisional. [G10](../thesis-progress/G10.md) có34tests mới và 30runtimechecks. Không đánh dấu các TC G01 dự kiến khác đã đạt; BR-10/final results còn G11 và hồ sơ tổng hợp G12. T4 chưa hoàn tất.
