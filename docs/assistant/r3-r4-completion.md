# R3–R4 — Khóa, lớp, lịch và học tập

Cập nhật 13/11/2026. Đã triển khai backend, MCP/host, kết quả trên web và kiểm thử tự động bằng dữ liệu tổng hợp. Tiếp nối [R1–R2](r1-r2-completion.md). [Audit 120 câu hỏi](readonly-audit.md) được cập nhật theo khả năng hợp đồng thực tế; execution từng prompt vẫn NOT_RUN, không phải kết quả chạy Gemini thật.

## R3 đã bổ sung

| Nhu cầu | Tool / API | Quy tắc |
|---|---|---|
| Lớp còn chỗ, sắp khai giảng, đã kết thúc trong khoảng ngày | `search_classes` → Academic `/assistant-query/classes` | Có `classId`, `from/to`, tìm cả tên khóa; lọc trước limit. Khoảng ngày áp dụng ngày kết thúc hiệu lực khi FINISHED, ngày bắt đầu với trạng thái khác |
| Chỗ trống và sĩ số hiệu lực | Cùng API lớp | `remainingSeats=max(maxSize-currentSize,0)` khác `effectiveStudentCount` từ Enrollment/period/lớp đang diễn ra; không coi sĩ số quota là số người đang học hôm nay |
| Đầu vào khóa | `search_course_admissions` → `/course-admissions` | OPEN, policy đã xác nhận và revision mới nhất; `entryLevel` so mã trình độ được nhận, không nhầm `Course.level` hoặc trình độ đầu ra. OPEN_ENTRY không tự coi là đã đánh giá đạt mọi level |
| Lịch giáo viên theo tên | `find_personnel` rồi `list_sessions` | Trùng tên phải chọn; teacherId lấy Teacher profile ID. Teacher tự xem chỉ actual teacher chính mình |
| Học bù/dạy thay/buổi tiếp theo | `list_sessions` → `/sessions` | Thêm kind, substituteOnly, upcomingOnly; buổi tiếp theo lấy size=1 và sắp theo ngày/giờ, trong cửa sổ đã chọn tối đa 31 ngày |
| Phòng bảo trì hoặc không trùng lịch | `search_rooms` → `/rooms` | Trạng thái, sức chứa tối thiểu; muốn kiểm lịch phải có ngày/giờ cùng ngày; overlap `[start,end)`, buổi hủy không chặn. Buổi thiếu giờ được coi chưa đủ chắc chắn, chặn tài nguyên đó |
| Giáo viên không trùng lịch | `find_available_teachers` → `/available-teachers` | Account/role/workStatus ACTIVE và membership trong scope; xét xung đột buổi đã lưu ở mọi chi nhánh, không lộ chi tiết lịch ngoài scope |
| Đếm lớp/học viên theo chi nhánh | `get_academic_statistics` → `/academic-statistics` | Chỉ SA/A; metric CLASSES_ONGOING, CLASSES_FINISHED, ACTIVE_STUDENTS; count distinct tại SQL trước phân trang nhóm, thứ tự giảm dần theo số lượng. Câu so sánh nhiều chi nhánh yêu cầu chọn scope tổng hợp |

Phòng/giáo viên không trùng buổi đã lưu **không đồng nghĩa sẵn sàng xếp lịch**: chưa xét lịch chưa sinh, nghỉ phép, giờ làm, di chuyển. API trả cảnh báo và host giữ cảnh báo trên UI. Không sinh lịch hoặc thay đổi trạng thái khi tra cứu. Các nhóm thống kê chỉ có chi nhánh có dữ liệu khớp; chưa dựng thêm hàng chi nhánh số 0. Không dùng hiện trạng để giả số liệu năm trước. Khoảng ngày với FINISHED lọc theo ngày kết thúc lịch hiện tại; chưa có thời điểm đóng lớp thực tế để xác nhận lịch sử “đã kết thúc trong tháng”. ADMIN-10 được ghi PARTIAL_PLANNED_END_DATE_ONLY.

## R4 đã bổ sung

| Nhu cầu | Tool / API | Quy tắc |
|---|---|---|
| Tìm học viên bảo lưu/chuyển lớp theo quan hệ | `find_students` → Academic `/students` dưới assistant-query | Có relationship, status hồ sơ, currentLevel, studentId; RESERVED khác PAUSED. Teacher vẫn phải có quan hệ hiệu lực với học viên trong lớp chính mình; bộ lọc không mở lớp của người khác |
| Hồ sơ học viên đã xác định | `get_student_profile` → `/student-profile` | Projection tối thiểu mã/tên/trình độ/trạng thái/chi nhánh, không số điện thoại/email/địa chỉ. Teacher chỉ hồ sơ trong roster hiệu lực |
| Số học viên hiệu lực của lớp hoặc nhiều lớp mình dạy | `get_roster_statistics` → `/roster-statistics` | COUNT DISTINCT studentId, Enrollment STUDYING và period hiệu lực, lớp đã bắt đầu/chưa kết thúc. Chọn classId ngoài quyền trả 404; lớp hợp lệ không người trả 0 |
| Điểm danh của một học viên, ai chưa ghi | `get_session_attendance` → `/attendance` | Thêm studentId, missingOnly; giữ NOT_RECORDED khác PRESENT/ABSENT. Buổi chưa bắt đầu hoặc đã hủy trả trạng thái phù hợp |
| Học viên vắng nhiều | `get_attendance_summary` → `/attendance-summary` | Tối thiểu 3 bản ghi ABSENT trong 30 ngày mặc định; có from/to và ngưỡng, tối đa 90 ngày. Chỉ buổi đã kết thúc, không hủy, thuộc kỳ học hiệu lực; ngày kết thúc period exclusive. Không đếm người chưa ghi là vắng. Bao gồm ABSENT có phép nếu nghiệp vụ lưu cùng trạng thái; chưa có bộ lọc nghỉ có phép riêng |
| Buổi còn thiếu điểm danh | `list_missing_attendance` → `/missing-attendance` | Buổi đã kết thúc, roster đúng kỳ học, còn bản ghi chưa lưu; giới hạn ngày hữu hạn. Teacher chỉ lớp chính mình hoặc buổi được giao, không suy quyền cả lớp từ dạy thay |
| Bài kiểm tra còn thiếu điểm | `list_learning_exams` → Teaching `/assistant-query/exams` | Count bài trước limit; missingCount dựa roster đã chốt; chưa chốt trả null. missingOnly chỉ bài đã đến ngày thi và có người chưa có điểm. Teacher chỉ lớp chính mình, S không có quyền tiến độ điểm nháp |
| Điểm 0 hoặc chưa chấm | `get_learning_results` → `/current-grades` | scoreFilter ALL/ZERO/NOT_GRADED và studentId; lọc tại SQL trước limit. NOT_GRADED yêu cầu roster chốt. Điểm 0 là đã chấm. Quyền published của STAFF giữ riêng, không thay bằng current |
| Bài kiểm tra trùng tên | `list_learning_exams` → chọn EXAM → kiểm lại examId | Không tự chọn một bài; FE hỗ trợ EXAM choice và giữ đúng ID/branch. Hỏi tiếp dùng ID được xác minh hoặc yêu cầu làm rõ |

Hệ thống **chưa lưu hạn chấm điểm**, nên ADMIN-18 (“đã đến hạn nhưng chưa chấm”) là UNSUPPORTED_GRADING_DEADLINE. Không đổi ngày thi thành deadline chấm. STAFF-12 vẫn DENIED_BY_CURRENT_POLICY: không có projection bằng cấp giáo viên công khai cho STAFF. Đây là giới hạn dữ liệu/quyền đã nêu từ R1, không tự cấp quyền mới để đạt câu mẫu.

## Tổ chức code và API

- Academic: `AcademicInsights` giữ truy vấn cố định và Request có kiểu; `AcademicInsightController` giữ routes. Chuyển handler classes/students/sessions sang service mới nhưng giữ nguyên đường dẫn và mặc định tương thích. `AcademicReadService` tiếp tục phục vụ luồng cũ và hỗ trợ bộ lọc điểm danh.
- Teaching: mở rộng `TeachingReadService`/controller; `ExamService.require` vẫn kiểm quyền khi đọc điểm một bài. Truy vấn danh sách bài cho teacher kiểm school_classes.teacher_id hiện hành tại SQL trước limit.
- MCP: 26 tool khác nhau; SA/A thấy 25, STAFF 22, TEACHER 16. Schema không nhận scope/role/bearer/SQL/URL; vẫn revalidate Auth, giới hạn call và deadline. Không thay model/provider/billing.
- Web: hiển thị sĩ số hiệu lực, chỗ trống, sức chứa, chương trình/đầu vào, số vắng/thiếu điểm danh/thiếu điểm và nhãn chi nhánh; thêm EXAM choice. Giữ BotMessageSquare và cấu trúc Ant Design/styled-components.
- Thêm **9 GET** (8 Academic, 1 Teaching), tổng backend **268 API chính**, 3 alias, 2 diagnostic. Không có migration, seed/reset hoặc thao tác ghi.
- Availability giáo viên và danh sách bài của teacher dùng các bảng trong MySQL dùng chung hiện tại; không giả định dịch vụ đã tách database. Nếu tách DB sau này cần read model hoặc API nội bộ phù hợp, không dùng model tự nối dữ liệu.

## Kiểm thử

- Academic: **264/264** test đạt; gồm **31 AssistantQueryTest**, kiểm khoảng ngày, period exclusive, người học distinct, maintenance, giờ tiếp giáp/thiếu giờ, dạy thay và xung đột giáo viên ở chi nhánh khác. Sau sửa classId ngoài quyền, chạy lại 31 ca đạt.
- Teaching: **106/106** test đạt; gồm **9 AssistantQueryTest**, kiểm điểm 0/null, roster chưa chốt, missingCount và teacher ownership.
- Assistant: bộ toàn service cuối cùng **155 ca, 154 đạt, 1 live opt-in skip**. AcademicExpansionTest có **14 ca**, gồm chọn bài trùng tên bằng câu trả lời và hỏi scope tổng hợp.
- Web: **124/124** test đạt; TypeScript/Vite build và ESLint các file TypeScript thay đổi đạt. Chưa chạy app thật với Gemini/MySQL trong đợt này.

Log: `storage/r3-r4-business-tests.log`, `r3-r4-query-final-tests.log`, `r3-r4-assistant-final-tests.log`, `r3-r4-web-tests.log`, `r3-r4-web-build.log`. H2/fixture kiểm quy tắc và hợp đồng; không chứng minh toàn bộ 120 câu/biến thể đều được model nhận đúng. Nghiệm thu Gemini thật và dữ liệu vận hành tiếp ở C7/R8.

## Khởi động và thử

Khởi động lại **academic-service, teaching-service, assistant-service**; Auth/Gateway giữ cách cấu hình hiện có. Web dev tự tải lại, bản triển khai cần build lại. Mở hội thoại mới và chọn scope hợp lệ.

- “Lớp nào còn chỗ và sắp khai giảng trong 7 ngày tới?”
- “Lịch tuần này của giáo viên An?”
- “Phòng nào không có lịch ngày 2026-11-16 từ 18:00 đến 20:00?”
- “Học viên nào đang bảo lưu?”
- “Học viên nào vắng ít nhất 3 buổi trong 30 ngày?”
- Teacher: “Tổng số học viên distinct trong các lớp tôi phụ trách?”, “Tuần này có buổi học bù nào của tôi?”, “Có bao nhiêu bài kiểm tra của tôi còn thiếu điểm?”.

R5/R6 còn phần tài chính/chăm sóc/thống kê nâng cao; R7 mở rộng diễn đạt/hỏi tiếp và R8 nghiệm thu toàn bộ. Không cần fine-tuning để sử dụng các query mới.
