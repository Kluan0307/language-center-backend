# R1–R2 — Tra cứu nhân sự theo quyền

Ngày 13/11/2026. Đã triển khai source, kiểm thử hợp đồng/API/MCP và cập nhật web. R1 có [ma trận 120 câu hỏi](readonly-audit.md) và [JSON đối chiếu](readonly-scenarios.json). Đây là audit và kiểm thử tự động bằng dữ liệu tổng hợp; chưa phải nghiệm thu 120 prompt bằng Gemini thật. C7/R8 vẫn còn mở.

## Nguồn và quyền

| Nguồn đã đối chiếu | Quy tắc áp dụng |
|---|---|
| `auth-service/.../entity/Teacher.java` | Bằng cấp = `qualification`, chứng chỉ = `certificate`, chuyên môn = `specialization`, kinh nghiệm = `experience`; không suy diễn qua lại |
| `auth-service/.../entity/Staff.java` | Vị trí = `position`; ID của Staff và Teacher thuộc hai bảng khác nhau, phải giữ `kind` khi hỏi tiếp |
| `auth-service/.../entity/Account.java`, `AccountBranch.java` | Phạm vi lấy phân công còn hiệu lực; không coi `branch_id` cũ là một phân công hợp lệ khi membership đã bị thu hồi |
| `auth-service/.../security/SecurityConfig.java`, `service/TeacherProfileService.java` | Hồ sơ giáo viên của người khác dành cho SUPER_ADMIN/ADMIN; không mở quyền bằng cấp cho STAFF chỉ vì câu mẫu có yêu cầu |
| `auth-service/.../assistant/PersonnelController.java` | Kiểm lại account/role/status/password-change và phân công hiện hành; projection tối thiểu, không trả email/điện thoại/mật khẩu/token |
| `academic-service/.../schoolclass/SchoolClass.java`, `assistant/AcademicReadService.java` | `teacher_id` là Teacher profile ID. “Đang dạy khóa nào” trong R2 là **phụ trách lớp đang trong thời gian học**, không phải chuyên môn khai báo hoặc mọi buổi dạy thay |
| `assistant-service/.../mcp/SessionVerifier.java`, `AssistantMcpRuntime.java` | Revalidate phiên và scope trước/sau thực thi; bearer và scope do host giữ, không thuộc schema model |
| `assistant-service/.../chat/ChatService.java`, `ReferenceIndex.java` | Trùng tên phải chọn; lựa chọn được truy vấn lại bằng ID trong scope. Giữ kind theo dữ liệu server để tránh lẫn hai bảng nhân sự |

SA = SUPER_ADMIN, A = ADMIN, S = STAFF, T = TEACHER. SA chọn chi nhánh hoặc WHOLE_SYSTEM rõ ràng; A chọn ACTIVE_BRANCH/ALL_ASSIGNED trong phân công; S/T chỉ ACTIVE_BRANCH. Branch header không tự cấp quyền.

## Ma trận các nhóm tra cứu

| Ý định | Tool → API chỉ đọc | Field/metric | Quyền / giới hạn |
|---|---|---|---|
| Tìm nhân sự | `find_personnel` → Auth `/assistant-query/personnel` | kind, personnelId, fullName/code, workStatus/accountStatus, branchIds; totalElements trước phân trang | SA/A giáo viên hoặc nhân viên; S chỉ giáo viên active tối thiểu; T không tìm người khác |
| Đếm giáo viên/nhân viên | `get_personnel_statistics` → Auth `/assistant-query/personnel-statistics` | DISTINCT_PERSONNEL, value, kind, activeOnly | SA/A; S chỉ giáo viên active. Không cộng số hàng/page hoặc cộng các chi nhánh gây trùng người |
| Bằng cấp/chứng chỉ/vị trí | `get_personnel_profile` → Auth `/assistant-query/personnel-profile` | qualification/certificate/specialization/experience hoặc position | SA/A trong scope; S/T không xem hồ sơ người khác |
| Hồ sơ chính mình | `get_my_personnel_profile` → Auth `/assistant-query/my-personnel-profile` | Account → Teacher; bằng cấp/chứng chỉ; danh sách chi nhánh của chính mình | Chỉ T, không nhận target ID. Danh sách phân công chính mình có thể gồm nhiều chi nhánh; không mở quyền đọc nghiệp vụ ngoài scope |
| Lớp/khóa được giao | `get_teacher_classes` → Academic `/assistant-query/teacher-classes` | teacherId, courseName, class code/name, startDate/endDate/status; totalElements | SA/A/S scoped; T chỉ lớp chính mình. currentOnly=true mặc định; false trả lịch sử phân công hiện đang lưu, không khôi phục lịch sử thay giáo viên |
| Khóa/lớp | `search_courses`, `search_classes` → Academic `/assistant-query/courses`, `/classes` | tuitionFee, level/type/status; currentSize/maxSize; recruitingOnly | Có query cơ bản; nhóm theo chi nhánh, phòng/giáo viên trống lịch, lọc khoảng khai giảng chưa đủ, tiếp R3/R6 |
| Học viên và thống kê | `find_students`, `get_student_classes`, `get_student_statistics` → Academic `/students`, `/student-classes`, `/student-statistics` dưới assistant-query | totalElements, quan hệ lớp/periods, TOTAL_PROFILES/ACTIVE_IN_CLASS | T bị giới hạn roster/lớp; thống kê toàn scope không cho T. Tổng hợp chuyên cần và nhiều điều kiện chưa đủ, tiếp R4/R6 |
| Lịch/điểm danh | `list_sessions`, `get_session_attendance` → Academic `/sessions`, `/attendance` dưới assistant-query | actual teacher, ngày/giờ/status; recorded/state | Buổi thực tế và quyền dạy thay; chưa có tool riêng lọc kind/substitute, aggregate vắng/chưa điểm danh |
| Điểm | `get_learning_results` → Teaching `/assistant-query/current-grades` hoặc `/published-grades` | score/null, maxScore, rosterState, view | S chỉ published; T current đúng phân công; aggregate/filter điểm chưa đủ |
| Học phí/phiếu thu | `search_tuitions`, `search_receipts` → Finance `/assistant-query/tuitions`, `/receipts` | balance, overdue, paidAmount, voided/refunded/availableAmount | SA/A/S; T bị chặn. Tổng kỳ/so sánh kỳ/nhóm chi nhánh chưa có tool; không lấy tổng một page |
| Chăm sóc | `list_priority_leads` → Academic `/assistant-query/priority-leads` | reason/thời gian hẹn/limit, chưa có điểm tiềm năng | S chính assignee. Lọc theo tên, người phụ trách khác và support chưa có tool |
| Hướng dẫn | `get_usage_help` → Academic `/assistant-query/usage-help` | Hai chủ đề cố định | Không mở đọc file/web tự do |

Đường dẫn đầy đủ bắt đầu `/api/v1/auth`, `/api/v1/academic`, `/api/v1/teaching` hoặc `/api/v1/finance`. [Catalog schema theo role](mcp-tool-catalog.json) xuất từ test MCP: 17 tool khác nhau; SA/A có 16, S có 15, T có 10. Kiểm kê toàn backend hiện có **259 API chính**, 3 alias, 2 diagnostic; R2 thêm 4 GET Auth và 1 GET Academic.

## Định nghĩa và hợp đồng R2

- Đếm distinct profile ID có đúng account role tương ứng và phân công ACTIVE trong scope. Một người làm hai chi nhánh tính một lần trong ALL_ASSIGNED/WHOLE_SYSTEM. Profile không có membership còn hiệu lực chỉ thuộc tổng toàn hệ thống.
- `activeOnly=true`: account mở và `workStatus=ACTIVE`. Không tự chuyển chuỗi legacy `WORKING` thành ACTIVE. `false` gồm mọi trạng thái. MCP/host mặc định false cho quản trị, true cho STAFF; REST trực tiếp mặc định true. “Đang hoạt động/đang làm việc” được host ép true, không phụ thuộc model chọn sai.
- Không có snapshot nhân sự quá khứ: câu đếm “tháng trước/năm ngoái” bị từ chối khả năng. Không dùng số hiện tại thay lịch sử. Đếm theo chuyên môn/chứng chỉ hoặc nhóm nhiều chiều chưa có bộ lọc tương ứng.
- `currentOnly=true`: lớp chưa FINISHED, đã bắt đầu và chưa qua ngày kết thúc hiệu lực, theo Asia/Ho_Chi_Minh. Kết quả đếm là số lớp; cùng một khóa có nhiều lớp vẫn hiện từng lớp, không giả thành số khóa distinct.
- Hồ sơ không có bằng cấp/chứng chỉ giữ null; giao diện hiển thị chưa xác định, không suy bằng cấp từ IELTS. `branchIds` của hồ sơ người khác chỉ hiện phần giao với scope; riêng hồ sơ bản thân T hiện phân công của chính mình.
- Tìm kiếm tên dùng LIKE có escape ký tự `%`, `_`, `!`, tham số bind. Mỗi trang tối đa 20. ID người trùng tên được kiểm lại ở trang 0 trước khi tiếp tục, tránh nhầm số thứ tự hay ID của bảng khác.
- Không có SQL/URL/role/bearer/branchId tùy ý trong schema model; phạm vi lấy từ phiên được xác thực. Không thêm mutation, migration, seed/reset hoặc đổi provider.

## Kiểm thử và giới hạn nghiệm thu

- Auth: bộ clean test 81/81 đạt; sau đó bổ sung và chạy 14/14 `PersonnelQueryTest`, gồm HTTP/JWT thật trong MockMvc, 401 khi thiếu token và 403 khi STAFF xem chi tiết.
- Academic: 251/251 đạt, gồm 18 `AssistantQueryTest`; kiểm teacher ownership, scope, lớp tương lai/đã kết thúc, total trước limit.
- Assistant: 141 ca, 140 đạt và 1 live opt-in bỏ qua; gồm 10 `PersonnelChatTest`, kiểm count 0, activeOnly, trùng tên, đổi kind, tự xem và từ chối sai quyền. Sau sửa phân trang lựa chọn, chạy lại 10/10 ca này đạt.
- Web: 123/123 test đạt, gồm render bằng cấp/chứng chỉ/chi nhánh bằng React; TypeScript + Vite build và ESLint ba file TypeScript thay đổi đạt. Hỗ trợ PERSONNEL choice, bằng cấp/chứng chỉ/vị trí, trạng thái và danh sách chi nhánh. Giữ icon BotMessageSquare hiện có.
- Không gọi Gemini thật trong R1–R2. Kết quả hợp đồng tổng hợp không chứng minh mọi biến thể tiếng Việt đều được model chọn đúng; cần nghiệm thu model và dữ liệu MySQL thật ở C7/R8. 120 kịch bản vẫn giữ execution NOT_RUN, tách với bằng chứng unit/integration.

Log local: `storage/r1-r2-auth-clean-tests.log`, `r1-r2-auth-http-tests.log`, `r1-r2-academic-assistant-retest.log` (Academic đạt; phần Assistant lần này phát hiện fixture cũ), `r1-r2-assistant-final-tests.log`, `r1-r2-assistant-selection-tests.log`, `r1-r2-web-test.log`, `r1-r2-web-build.log`. ESLint chạy trực tiếp trả exit code 0.

## Cách dùng sau cập nhật

Khởi động lại **auth-service, academic-service, assistant-service** bằng cách hiện đang dùng. Web dev tự tải lại; bản deploy cần build lại. Tạo hội thoại mới, chọn chi nhánh hợp lệ rồi thử:

- Quản trị: “Chi nhánh đang chọn có bao nhiêu giáo viên?”, “Có bao nhiêu nhân viên đang hoạt động?”, “Giáo viên An có bằng cấp gì?”, “Giáo viên này đang được giao những lớp nào?”.
- STAFF: “Có bao nhiêu giáo viên đang hoạt động?”, “Tìm giáo viên An”. Hỏi bằng cấp người khác sẽ bị từ chối theo quyền hiện tại.
- TEACHER: “Bằng cấp của tôi?”, “Tôi đang được phân công những chi nhánh nào?”, “Tôi đang dạy những khóa học nào?”.

Đổi chi nhánh/phạm vi trên UI trước khi hỏi tổng nhiều chi nhánh. Nếu cần tên giáo viên cụ thể và trùng tên, chọn đúng GV#ID do server trả. Lộ trình tiếp theo là R3/R4; các aggregate cao hơn ở R5/R6 vẫn là backlog đã ghi rõ trong ma trận.
