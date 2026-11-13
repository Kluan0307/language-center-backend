# C1 — Công cụ và khoảng trống API

[Mục lục](README.md) · [Quyền và định nghĩa](c1-scope-and-policy.md). Tên công cụ và tham số dưới đây là **thiết kế**, chưa được đăng ký trong MCP/runtime. Không expose toàn bộ 238 API cho model.

## 1. Danh mục công cụ dự kiến

Scope do backend xác thực, không là trường tùy ý trong schema model. `page` bắt đầu 0, `size` mặc định 5, tối đa 20 cho câu trả lời; server count trước phân trang. ID là số dương; không nhận SQL, tên bảng, URL, biểu thức sort hoặc role.

| Tool / ý định | Tham số nghiệp vụ model được đề xuất | Nguồn API hiện có | Xử lý thêm |
|---|---|---|---|
| `search_courses` / SEARCH_COURSES | keyword, level, type, status, minTuition, maxTuition, maxInclusive, page, size | GET `/api/v1/academic/courses`, `/{id}` | GAP-01; khóa chung không lặp |
| `search_classes` / SEARCH_CLASSES | keyword, courseId, status, recruitingOnly, page, size | GET `/api/v1/academic/classes` hoặc `/classes/my` | GAP-02; teacherId của T do server ép |
| `find_students` / FIND_STUDENTS | keyword, classId?, page, size | GET `/api/v1/academic/students`; T dùng `/classes/{id}/students` hoặc `/roster?date=` | GAP-03; không gọi StudentController bằng quyền T |
| `get_student_classes` / GET_STUDENT_CLASSES | studentId, asOf? | GET `/students/{id}` có enrollments; GET `/enrollments/{id}/history` có periods | GAP-03; một truy vấn có scope, tránh N+1 và rò lớp khác cho T |
| `get_student_statistics` / GET_STUDENT_STATISTICS | metric=TOTAL_PROFILES/PROFILE_STUDYING/ACTIVE_IN_CLASS, asOf? | GET `/api/v1/academic/stats/overview` | GAP-04; tổng hồ sơ khác đang có lớp; SA/A/S, không T |
| `list_sessions` / LIST_SESSIONS | classId?, teacherId?, from, to, status?, page, size | GET `/api/v1/academic/sessions`, `/{id}` | Dùng buổi thật; GAP-02 projection/date bounds; T không tự chọn giáo viên khác |
| `get_session_attendance` / GET_SESSION_ATTENDANCE | sessionId | GET `/api/v1/academic/sessions/{id}`, `/attendances/sheet?sessionId=` | GAP-05; chỉ recorded=true, giữ quyền dạy thay theo buổi |
| `get_learning_results` / GET_LEARNING_RESULTS | studentId?, enrollmentId?, classId?, examId?, view=PUBLISHED/CURRENT, page, size | GET `/api/v1/teaching/grades?examId=`, `/students/{studentId}/enrollments/{enrollmentId}/published-grades` | GAP-05; chọn đúng endpoint theo role, không STAFF gọi grades |
| `search_tuitions` / SEARCH_TUITIONS | studentId?, classId?, overdue?, status?, page, size | GET `/api/v1/finance/tuitions` | Projection số tiền/trạng thái/billing root; không T; không tự cộng một trang thành tổng |
| `search_receipts` / SEARCH_RECEIPTS | studentId?, tuitionId?, classId?, page, size | GET `/api/v1/finance/payments`, `/{id}` | Xác minh trạng thái hiệu lực, hủy/hoàn trong service; GAP-06 aggregate scope |
| `list_priority_leads` / LIST_PRIORITY_LEADS | limit=5 | GET `/api/v1/academic/leads` hiện chưa xếp ưu tiên | GAP-06; thêm thứ tự v1, STAFF ép assignedTo |
| `get_usage_help` / USAGE_HELP | topic trong danh sách đã duyệt | Chưa có nguồn hướng dẫn runtime | GAP-07; tài liệu chọn lọc, không đọc toàn bộ docs/env |

Chín nhóm nghiệp vụ được tách thành 11 công cụ dữ liệu và một công cụ hướng dẫn để schema nhỏ, quyền rõ. Ý định nội bộ khác: CLARIFY, OUT_OF_SCOPE, UNSUPPORTED_CAPABILITY, UNSUPPORTED_ACTION; không được tạo thành công cụ truy cập tùy ý.

## 2. Những phần đã đối chiếu source

Đường dẫn dưới đây là bằng chứng khảo sát local, không phải nguồn dữ liệu đưa cho model:

| Source | Hiện trạng có ý nghĩa với trợ lý |
|---|---|
| [CourseController](../../academic-service/src/main/java/com/example/academic/course/CourseController.java), [CourseService](../../academic-service/src/main/java/com/example/academic/course/CourseService.java), [Course](../../academic-service/src/main/java/com/example/academic/course/Course.java) | Lọc keyword/code/name, status OPEN/CLOSED, type/level; chưa có khoảng tiền; khóa chung branchId=null; expectedMinutes và duration legacy khác nhau |
| [StudentController](../../academic-service/src/main/java/com/example/academic/student/StudentController.java), [StudentService](../../academic-service/src/main/java/com/example/academic/student/StudentService.java) | Chỉ SA/A/S; keyword cả tên/phone/email/code; chi tiết trả đăng ký nhưng chưa đủ chứng minh khoảng học ngày cụ thể; list chưa có allAssigned |
| [Student](../../academic-service/src/main/java/com/example/academic/student/Student.java), [Enrollment](../../academic-service/src/main/java/com/example/academic/enrollment/Enrollment.java) | Student STUDYING/PAUSED/GRADUATED khác Enrollment STUDYING/DROPPED/COMPLETED/TRANSFERRED/RESERVED |
| [SchoolClassController](../../academic-service/src/main/java/com/example/academic/schoolclass/SchoolClassController.java), [EnrollmentPeriod](../../academic-service/src/main/java/com/example/academic/enrollment/EnrollmentPeriod.java) | Roster theo ngày khác students hiện tại; cần dùng khoảng có hiệu lực cho asOf và teacher roster |
| [StatsService](../../academic-service/src/main/java/com/example/academic/stats/StatsService.java), [StatsRepository](../../academic-service/src/main/java/com/example/academic/stats/StatsRepository.java) | Đếm hồ sơ tại SQL; A có allAssigned; chưa có count distinct học viên đang có lớp; thống kê trạng thái không dùng thay cho khoảng học |
| [LeadService](../../academic-service/src/main/java/com/example/academic/lead/LeadService.java), [Lead](../../academic-service/src/main/java/com/example/academic/lead/Lead.java), [Query](../../academic-service/src/main/java/com/example/academic/common/Query.java) | Có followUp/active/assignee/allAssigned, nextContactAt/lastContactedAt; chưa có score; Query.page sort id DESC, không phải top tiềm năng; OVERDUE/TODAY có thể giao nhau |
| [SessionPlanner](../../academic-service/src/main/java/com/example/academic/session/SessionPlanner.java) | List ép actual teacher; detail cho actual/primary; allAssigned chỉ SA/A; không mở rộng dạy thay thành lớp chính |
| [AttendanceService](../../academic-service/src/main/java/com/example/academic/attendance/AttendanceService.java) | Sheet không ghi dữ liệu; dòng chưa lưu trả PRESENT kèm recorded=false; chặn ngày tương lai/buổi hủy; tổng hợp cả lớp phải theo quyền lớp |
| [GradeController](../../teaching-service/src/main/java/com/example/teaching/grade/GradeController.java), [GradeService](../../teaching-service/src/main/java/com/example/teaching/grade/GradeService.java), [ManagementProgressService](../../teaching-service/src/main/java/com/example/teaching/publication/ManagementProgressService.java) | Bảng điểm SA/A/T; công bố SA/A/S, có xác minh enrollment/student/branch và revision hiện hành; null khác 0 |
| [TuitionService](../../finance-service/src/main/java/com/example/finance/tuition/TuitionService.java), [PaymentController](../../finance-service/src/main/java/com/example/finance/payment/PaymentController.java), [RevenueController](../../finance-service/src/main/java/com/example/finance/stats/RevenueController.java) | Tuitions có allAssigned/overdue; payments chưa có allAssigned; revenue chỉ SA/A, chưa là tool bản đầu |
| [JwtAuthenticationFilter](../../academic-service/src/main/java/com/example/academic/security/JwtAuthenticationFilter.java), [BranchScope](../../academic-service/src/main/java/com/example/academic/security/BranchScope.java) | Xác minh phiên trực tiếp từ Auth, không chỉ claim JWT; kiểm mustChangePassword và phân công hiện tại; branch header không là chứng cứ quyền |

Đã đọc thêm App.tsx và config/navigation.ts của web: có route/guard theo role, pages lazy, ManagementLayout và useRemoteResource. C6 phải gắn vào layout có xác thực, không mở drawer ở login hoặc bypass guard. Link chi tiết phải dùng helper/query thực tế từng trang; không để model tự tạo đường dẫn.

## 3. Backlog C2 có thứ tự

| ID | Công việc cần làm | Điều kiện nghiệm thu C2 |
|---|---|---|
| GAP-01 | Thêm min/maxTuition và maxInclusive vào list khóa, kiểm min<=max, tiền không âm, BigDecimal; scope ACTIVE/ALL_ASSIGNED/WHOLE_SYSTEM có kiểm quyền | 4.999.999 được, 5.000.000 bị loại khi “dưới”; khóa chung một lần; không lọc sau phân trang; mặc định API cũ giữ nguyên |
| GAP-02 | Projection lớp và session tối thiểu, recruitingOnly tại server; giới hạn khoảng ngày công cụ; tổng hợp lớp đúng phân công | Class/session count trước limit; lớp dạy thay không lọt class-wide; session hủy/bù/actual teacher đúng; không N+1 bằng duyệt toàn database |
| GAP-03 | Query hồ sơ tối thiểu và lớp học viên theo asOf/period; thêm teacher search trong roster lớp chính mình; bổ sung aggregate scope có kiểm quyền | Cùng tên không tự chọn; nhiều lớp không đếm người lặp; transferred/reserved/future start được phân loại; T không thấy lớp/hồ sơ ngoài quyền |
| GAP-04 | Count distinct ACTIVE_IN_CLASS và metadata scope/asOf/observedAt; dùng lại tổng hồ sơ server | 1 học viên 2 lớp tính 1; Student STUDYING không lớp tính 0 active-in-class; kỳ thời gian không hỗ trợ phải hỏi lại, không lấy tổng hiện tại làm quá khứ |
| GAP-05 | Projection điểm danh/điểm an toàn và resolver tài nguyên theo role | recorded=false không thành có mặt; 0 không null; STAFF chỉ published; buổi tương lai/hủy trả trạng thái, không gọi sheet sai; substitute không lấy grades |
| GAP-06 | Lead priority query v1 + reason; aggregate scope payments nếu hỗ trợ; thống nhất projection tài chính | Server sort trước LIMIT; tie stable, loại LOST/CONVERTED, STAFF assignedTo; phiếu hủy/hoàn rõ; không double billing. Không thêm revenue tool |
| GAP-07 | Common response envelope/provenance và catalog hướng dẫn đã duyệt, mapping link an toàn | Chỉ trường cần thiết, không PII thừa; data/error/empty riêng; route whitelist theo role; nội dung hướng dẫn phải được chọn rõ, không scan file tự do |

Ưu tiên tái sử dụng GET hiện có và bổ sung query tùy chọn. Với teacher roster/asOf hoặc distinct stats cần query riêng, dùng endpoint GET cộng thêm trong academic; không nới StudentController cho T. Đường dẫn endpoint mới được chốt trong C2 sau khi kiểm collision với path `/{id}`. Không giả định các endpoint dự kiến đã tồn tại.

Nếu một công cụ chưa hỗ trợ tổng hợp nhiều chi nhánh, trả UNSUPPORTED_CAPABILITY hoặc đề nghị chọn một chi nhánh; không âm thầm trả chi nhánh hiện tại dưới nhãn ALL_ASSIGNED. Không dùng fan-out từ agent cộng các trang/count cục bộ để giả hỗ trợ aggregate.

## 4. Tương thích và kiểm tra

Không reset database, không đổi định nghĩa Student/Enrollment và billing root để phục vụ AI. Projection chỉ đọc không cần schema mới dự kiến; nếu C2 phát hiện cần index thì ghi migration cộng thêm riêng. Giữ API và mặc định cũ cho web/mobile, bổ sung test tương thích. Từ C3 tool annotations readOnly là mô tả; backend vẫn phải từ chối mutation và quyền không hợp lệ.
