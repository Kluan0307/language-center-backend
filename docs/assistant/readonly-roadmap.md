# Lộ trình nâng cấp chatbot chỉ tra cứu

**R7–R8 (13/11/2026):** R7 đã triển khai alias và hội thoại tiếp nối. R8: 120/120 ca host/MCP đạt; Gemini thật đạt 4 ca, timeout 1 ca, 115 ca chưa chạy nên **chưa đóng nghiệm thu R8**. Assistant 340 ca đạt (2 live opt-in bỏ qua), web 125 ca đạt; build/lint đạt. [Bàn giao, giới hạn và cách chạy lại](r7-r8-completion.md).

Cập nhật 13/11/2026 theo yêu cầu làm rõ của người dùng. Lộ trình này thay hướng mục 4–5 về bản nháp/xác nhận ghi dữ liệu và danh mục cũ có mutation. Phạm vi hiện tại: hiểu nhiều câu hỏi, lấy đúng thông tin, đếm/thống kê và hỏi tiếp theo quyền. Không có phụ thuộc phải hoàn thiện mutation trước.

**Hiện trạng sau R5–R6:** có 36 công cụ MCP chỉ đọc (SA/A: 35, S: 27, T: 16), gồm nhân sự, đào tạo, tài chính, chăm sóc/support và thống kê theo scope. [Bàn giao R5–R6, metric, quyền và cách chạy](r5-r6-completion.md) · [R3–R4](r3-r4-completion.md) · [R1–R2](r1-r2-completion.md) · [Audit 120 câu](readonly-audit.md). R7 đã triển khai; R8 còn nghiệm thu model thật; chưa có doanh thu dồn tích hoặc snapshot công nợ quá khứ. Nghiệm thu C7/model thật đầy đủ còn mở.

[120 kịch bản dễ đọc](readonly-scenarios.md) · [JSON phục vụ phát triển/eval](readonly-scenarios.json). Gồm 24 câu/role và 24 tình huống hỏi tiếp, quyền, dữ liệu lỗi. Không gắn nhãn đã hỗ trợ chỉ vì có câu mẫu.

## Ma trận quyền định hướng

| Role | Nhu cầu | Giới hạn |
|---|---|---|
| SUPER_ADMIN | Nhân sự, phân công, tổng số và tổng hợp nhiều chi nhánh | Chọn rõ phạm vi; role cao vẫn không hỏi ngoài trung tâm hoặc đọc secrets |
| ADMIN | Vận hành lớp, nhân sự, học viên và tài chính của chi nhánh quản lý | Chỉ ACTIVE_BRANCH/ALL_ASSIGNED hợp lệ; không xem ngoài phân công |
| STAFF | Tư vấn khóa/lớp, tìm học viên, công nợ, điểm công bố, chăm sóc khách | Lead theo assignee; nhân sự chỉ projection phục vụ vận hành được API cho phép; không có điểm nháp |
| TEACHER | Hồ sơ chính mình, lịch dạy, lớp/roster/điểm/điểm danh được giao | Không có thống kê toàn trung tâm, công nợ hay hồ sơ nhân sự khác; quyền buổi dạy thay không mở toàn bộ quyền lớp |

Câu mẫu không tự cấp quyền mới. Khi quyền API hiện tại hẹp hơn nhu cầu đề xuất, trả từ chối hoặc ghi backlog; không gọi bằng tài khoản admin chung. Staff xem bằng cấp công khai của giáo viên là nhu cầu cần đối chiếu projection/API, chưa khẳng định đã có quyền.

## Các giai đoạn thực hiện

| Mốc | Kết quả cần có | Ví dụ ưu tiên | Điều kiện hoàn thành |
|---|---|---|---|
| **R1** | Ma trận câu hỏi → ý định → API → field/metric → role/scope → trạng thái hỗ trợ | Ba ví dụ đếm giáo viên, bằng cấp, đang dạy khóa gì | Đối chiếu controller/service/security và khả năng dữ liệu, không dựa vào tên route |
| **R2** | Tra cứu hồ sơ, phân công và đếm nhân sự | “Chi nhánh 1 có bao nhiêu giáo viên?”, “ABC có bằng cấp gì?” | API/projection theo role, đếm distinct trước phân trang, trùng tên hỏi lại; không lộ credential |
| **R3** | Khóa, lớp, lịch và tài nguyên | Lớp còn chỗ, lịch giáo viên, khóa đang được giao, phòng không trùng lịch | Quan hệ giảng dạy thực tế và khoảng ngày/giờ rõ ràng; lọc tại server trước limit |
| **R4** | Hồ sơ học viên, roster, học tập | Bạn ấy học lớp nào, sĩ số, chuyên cần, điểm còn thiếu | Phân biệt hồ sơ/đăng ký/người có lớp, 0/null, recorded=false, bảo lưu/chuyển lớp và quyền dạy thay |
| **R5** | Tài chính và công việc chăm sóc | Nợ, nợ quá hạn, phiếu thu/hoàn, hẹn khách, hỗ trợ tồn | Billing root, trạng thái hủy/hoàn, phạm vi assignee; teacher bị từ chối tài chính |
| **R6** | Thống kê, đếm, nhóm và so sánh | Đếm nhân sự/lớp/phòng, thực thu theo kỳ, tổng hợp chi nhánh | Mỗi metric có định nghĩa, đơn vị, bộ lọc, thời điểm, nguồn SQL/API và oracle; không cộng page thành tổng |
| **R7** | Nhiều cách diễn đạt và nhiều lượt | “bao nhiêu/tổng số/đếm”, “bằng cấp/trình độ học vấn”, “còn cô ấy?” | Alias có dấu/không dấu, lịch sử định danh/bộ lọc, hỏi rõ khi mơ hồ, xóa ngữ cảnh khi quyền đổi |
| **R8** | Nghiệm thu và tài liệu sử dụng | Chạy toàn bộ ma trận 120 ca cùng các biến thể | API test, MCP/host test, UI test, Gemini thật được báo riêng; không có lỗi quyền/giả số liệu còn mở |

Ưu tiên **R1 → R2** để đáp ứng ngay các ví dụ nhân sự. Tiếp theo R3/R4 → R5/R6 → R7/R8. Test quyền và khả năng hiểu câu phải bổ sung ngay ở từng mốc, không đợi đến R8. Mỗi mốc gồm source, test, cập nhật tài liệu và trạng thái các ca tương ứng.

## Những định nghĩa phải chốt

- Đếm giáo viên: distinct profileId; phân công chi nhánh phải còn hiệu lực. “Đang làm việc” phải xét trạng thái nhân sự/tài khoản theo quy tắc source. Tổng chuỗi không cộng trùng người làm nhiều chi nhánh.
- “ABC đang dạy môn gì”: trả khóa/chương trình từ phân công lớp trong thời gian được hỏi; chuyên môn khai trong hồ sơ là thông tin khác. Nêu rõ “phụ trách lớp” hay “có buổi dạy thực tế”.
- “Bằng cấp”: lấy qualification; chứng chỉ từ certificate; chuyên môn từ specialization, không tự suy bằng cấp từ chứng chỉ IELTS. Thiếu dữ liệu trả chưa cập nhật.
- “Lớp còn chỗ”: sức chứa trừ sĩ số theo định nghĩa hiện hành, chưa bảo đảm điều kiện ghi danh.
- “Vắng nhiều”: đề xuất mặc định ít nhất 3 buổi ABSENT đã ghi nhận trong 30 ngày; loại buổi hủy/tương lai và ngoài kỳ học hiệu lực. Nêu rõ tiêu chí trong câu trả lời.
- “Chưa chấm xong”: chỉ xét danh sách dự thi đủ điều kiện; null khác 0. Thiếu deadline thì không gọi là quá hạn.
- “Trống lịch”: chỉ kết luận không trùng trong khoảng xác định; chưa bao gồm giờ làm, nghỉ phép hoặc thời gian di chuyển nếu hệ thống chưa lưu.
- “Doanh thu/thực thu”: hỏi lại hoặc dùng metric đã định nghĩa rõ; đơn PENDING không thành phiếu thu, cần xử lý hủy/hoàn và kỳ ghi nhận. Không giả lập thống kê quá khứ khi thiếu lịch sử.

## Prompt thực hiện từng mốc

Dùng mẫu sau, thay Rn bằng mốc trong bảng:

> Thực hiện Rn trong docs/assistant/readonly-roadmap.md. Đọc source hiện tại và các ca Rn trong readonly-scenarios.json. Kiểm quyền/API/field trước khi thêm công cụ. Ưu tiên tái sử dụng API; bổ sung endpoint chỉ đọc nếu còn thiếu. Không thêm thao tác ghi, không đổi provider/billing, không ghi đè thay đổi giao diện của người dùng. Thực hiện backend/MCP/host/UI cần thiết, kiểm thử có oracle và cập nhật trạng thái từng ca bằng bằng chứng. Không đánh dấu model thật PASS bằng mock hoặc hạ tiêu chí; ghi rõ các phần chưa hoàn tất.

## Cách nghiệm thu

Mỗi câu phải kiểm cả ý định, công cụ/tham số thật sự gọi, danh tính, scope, kết quả so oracle và cách diễn đạt. Thử tối thiểu ba biến thể câu cho ý định quan trọng. Kiểm riêng hai người cùng role nhưng khác phân công. Tất cả ca quyền/mutation/secrets phải đạt; mục tiêu câu hợp lệ đúng hoặc hỏi rõ hợp lý ít nhất 90%, đồng thời không chấp nhận trả sai số tiền, count hoặc định danh để đạt tỷ lệ.

Gemini lỗi/timeout phải ghi kết quả chưa nghiệm thu, không tự retry nhiều lần để chọn một lượt đẹp. Bộ JSON đã có audit hợp đồng R1; execution vẫn NOT_RUN vì chưa chạy toàn bộ prompt bằng model thật. Kiểm thử tự động R2 được ghi riêng trong tài liệu bàn giao; không cần fine-tuning model.
