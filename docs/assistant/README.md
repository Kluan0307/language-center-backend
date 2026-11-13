# Trợ lý nghiệp vụ Language Center — Gemini + MCP

Ngày cập nhật: 13/11/2026. Phạm vi: web SUPER_ADMIN, ADMIN, STAFF, TEACHER; chỉ đọc và chỉ nghiệp vụ trung tâm. Giữ Ant Design + styled-components và quyền hiện hành. Đây là kế hoạch mới C1–C7, không thay thế các mốc G/mobile hoặc W/web trước đó.

## Tiến độ

| Mốc | Công việc | Trạng thái |
|---|---|---|
| C1 | Khảo sát source, phạm vi, ma trận quyền, định nghĩa, công cụ và bộ nghiệm thu | Hoàn thành tài liệu; chưa có runtime chatbot |
| C2 | API tra cứu còn thiếu và projection dữ liệu tối thiểu | Hoàn thành source và kiểm thử; [bàn giao C2](c2-completion.md) |
| C3 | MCP server chỉ đọc, schema và kiểm quyền công cụ | Hoàn thành; 24 test và JAR smoke đạt; [bàn giao C3](c3-completion.md) |
| C4 | Backend trợ lý Gemini, hội thoại và xử lý kết quả | Đã triển khai API và test; Gemini thật chưa nghiệm thu do timeout/503; [bàn giao C4](c4-completion.md) |
| C5 | Kiểm soát chủ đề, chống vượt quyền và giới hạn sử dụng | Đã triển khai kiểm soát và test đối kháng; [bàn giao C5](c5-completion.md) |
| C6 | Drawer chat web, kết quả và điều hướng | Đã triển khai UI bốn role; [bàn giao C6](c6-completion.md) |
| C7 | Nghiệm thu tích hợp, model thật, vận hành và bàn giao | Đang thực hiện; hồi quy API đạt, model thật gần nhất timeout; [tiến độ mục 1–3](expansion-1-3.md) |

## Tài liệu C1

- [Phạm vi, ma trận quyền và định nghĩa nghiệp vụ](c1-scope-and-policy.md).
- [Danh sách công cụ, đối chiếu source và backlog C2](c1-tools-and-api-gaps.md).
- [Kiến trúc, ranh giới tin cậy và hợp đồng hội thoại dự kiến](c1-architecture.md).
- [Bộ tình huống nghiệm thu và điều kiện PASS](c1-acceptance.md).
- [Các câu hỏi đầu vào có cấu trúc](acceptance-cases.json): dữ liệu kiểm thử tổng hợp, chưa phải kết quả chạy.

## Kết quả và giới hạn C1

Đã đọc controller/service/entity/security và các route web liên quan; không chỉ dựa vào danh sách API cũ. Những khác biệt quan trọng: Student.status không chứng minh đăng ký lớp có hiệu lực; điểm danh chưa lưu có giá trị mặc định PRESENT; điểm 0 khác chưa chấm; STAFF chỉ có điểm công bố; lead chưa có điểm tiềm năng và mặc định đang sắp theo id giảm dần.

Không thêm module/API/dependency/schema; không thay đổi quyền hoặc dữ liệu; không gọi Gemini, không kiểm tra key thật và không đọc giá trị `.env.local`. Không chạy lại Maven/web test vì C1 chỉ sửa tài liệu. Bằng chứng C1 là đường dẫn source đã đối chiếu, quy tắc cụ thể, kiểm tra liên kết local và JSON đọc được; chưa tuyên bố chatbot đã hoạt động hoặc các ca nghiệm thu đã PASS.

Ở mốc C1 chưa có code tiêu thụ `GEMINI_API_KEY` và `GEMINI_MODEL`. C4 đã thêm adapter; thử nghiệm chỉ dùng dữ liệu tổng hợp, chưa bật gửi dữ liệu vận hành thật. Hai cờ chat/MCP vẫn tắt mặc định.

## Thực hiện tiếp

[Khắc phục UI báo hết hạn hội thoại khi mới mở, cờ bật và cách restart](troubleshooting.md).

Cập nhật sau C6: đã đổi sang gemini-3.1-flash-lite theo lựa chọn người dùng và kiểm đạt bốn luồng Gemini + MCP thật trên Auth/C2 tổng hợp. Hồi quy mới: 124 test assistant, 121 frontend đạt, 1 live opt-in skip trong bộ mặc định. [Thời gian đo, lỗi quá tải model cũ và cấu hình hiện tại](troubleshooting.md). C7 còn nghiệm thu đầy đủ với API nghiệp vụ/MySQL thật.

C2 đã triển khai GAP-01 đến GAP-07: 13 GET mới, lọc tiền khóa học và aggregate phiếu thu tương thích ngược. 470 test ở academic/finance/teaching đạt, gồm 28 test C2 mới; log/tổng hợp local ở `storage/c2-*-tests.log` và `storage/c2-verification.json`. C3 có SDK MCP thật, 12 công cụ (T: 8), schema kín và kiểm lại Auth mỗi lần thực thi; [chi tiết](c3-completion.md). C4 bổ sung 3 API assistant, hội thoại RAM, chọn thực thể trùng tên, điều phối Gemini và câu trả lời từ nguồn; [cấu hình C4](c4-completion.md). C5 thêm rate limit, body thực, policy đối kháng, audit và kiểm thu hồi trước trả dữ liệu: [giới hạn C5](c5-completion.md). C6 đã có drawer web bốn role, 119 test web, 123 assistant và 10 gateway đạt, 1 live opt-in skip; [UI và cách chạy C6](c6-completion.md). Gemini thật còn timeout/503, chưa nghiệm thu MySQL + Gemini thật. Tiếp theo C7 nghiệm thu tích hợp trước khi mở dữ liệu thật.
