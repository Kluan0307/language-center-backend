# Mở rộng mục 1–3 — nghiệm thu, hội thoại và tra cứu theo role

Ngày 13/11/2026. Phạm vi theo ba mục đầu của bảng lộ trình: C7, hiểu câu hỏi/hội thoại tiếp nối, tra cứu phục vụ công việc. Không thêm thao tác ghi dữ liệu.

## Đã triển khai

- Câu “Còn dưới 3 triệu?” giữ bộ lọc khóa học trước, thay mức tiền, giữ nghĩa **dưới**, reset page về 0 và truy vấn mới. Kết quả cũ không được dùng làm dữ liệu mới.
- Sau tìm/chọn học viên, “Lịch tuần này của bạn ấy?” dùng đúng studentId đã xác minh → tra cứu các lớp → lấy lịch theo classId. Nhiều lớp phải chọn; khi chọn, server tải lại quan hệ lớp của đúng học viên, không chấp nhận ID khác.
- Backend chặn thay tham số ID khác với đối tượng đã chọn, kể cả ID đó đã xuất hiện trong kết quả trước. Đổi câu hỏi sang đối tượng mới vẫn phải xác định lại đối tượng.
- Hiểu thêm các cách nói: lớp còn chỗ, lớp sắp khai giảng, khách cần gọi/chăm sóc, hẹn liên hệ, nợ quá hạn/chưa đóng tiền.
- Lớp còn chỗ ép `recruitingOnly=true`; sắp khai giảng ép UPCOMING; nợ quá hạn ép `overdue=true` tại host trước API, không chỉ dựa vào prompt model.
- Hai câu chuẩn “Lớp nào còn chỗ và sắp khai giảng?” và “Những khoản nợ quá hạn?” có thể đi thẳng qua công cụ MCP đã kiểm quyền, không gọi Gemini. Câu có thêm điều kiện vẫn dùng model để tránh bỏ sót điều kiện.
- Các công cụ/API hiện có tiếp tục kiểm role, chi nhánh, tài nguyên và phiên. Không mở thêm quyền cho giáo viên hoặc cho phép ghi SQL.

## Các câu có thể thử

| Role | Ví dụ |
|---|---|
| SUPER_ADMIN | Tổng số học viên toàn hệ thống; chọn rõ WHOLE_SYSTEM |
| ADMIN | Lớp nào còn chỗ và sắp khai giảng? |
| STAFF | Những khoản nợ quá hạn?; Khách cần gọi hôm nay? |
| TEACHER | Tuần này tôi dạy những buổi nào?; Tìm học viên An trong lớp của tôi |
| Theo quyền hiện hành | Tìm/chọn học viên → Lịch tuần này của bạn ấy?; Khóa giao tiếp dưới 5 triệu → Còn dưới 3 triệu? |

Lớp còn chỗ chỉ phản ánh dữ liệu sĩ số hiện tại, chưa bảo đảm học viên đủ điều kiện ghi danh. Lịch theo lớp không tự chứng minh học viên đã tham dự mọi buổi; xem trạng thái quan hệ lớp và thời gian đăng ký khi đánh giá việc học.

## Nghiệm thu đã chạy và phần còn lại

Hồi quy API: Academic 249, Finance 118, Teaching 103 và Gateway 10 ca đạt trên môi trường test riêng. Assistant có 6 test mở rộng về giữ bộ lọc, lớp còn chỗ, nợ quá hạn, lịch học viên, chọn nhiều lớp và giới hạn nguồn dữ liệu; xem tổng kết cuối trong `storage/assistant-expansion-verify.log`. JAR được đóng gói cùng source đã kiểm.

Log tổng hợp API: `storage/assistant-expansion-tests.log`. Test model thật đã mở rộng bộ smoke lên 8 kịch bản; giữ bằng chứng các ca đã qua nếu provider lỗi. Hai lượt mới kết thúc MODEL_TIMEOUT, gồm một lượt đã qua khóa học và lịch dạy trước khi timeout ở thống kê. Bằng chứng cuối `storage/assistant-expansion-live.json` là BLOCKED, không phải PASS. Bốn ca model thật đã đạt trước đây vẫn có bằng chứng lịch sử tại `storage/gemini-flash-lite-verification.json`.

**C7 chưa hoàn tất**: chưa đạt bộ nghiệm thu đầy đủ với model ổn định và API nghiệp vụ/MySQL thật. Không đánh dấu C7 hoàn thành chỉ vì các test H2/mock đã đạt. Không đổi model, tăng timeout hay bật billing để làm test báo PASS.

**Mục 3 mới hoàn thiện nhóm tra cứu dựa trên API hiện có.** Phòng trống/giáo viên rảnh theo khoảng giờ, học viên vắng nhiều, buổi chưa điểm danh và bài chưa chấm xong còn cần API nghiệp vụ cùng định nghĩa/chỉ số cụ thể. Các câu này hiện trả UNSUPPORTED_CAPABILITY thay vì bịa kết quả hoặc gán là ngoài phạm vi. Chưa có lịch trống không đồng nghĩa giáo viên sẵn sàng nhận dạy.

## Triển khai

Restart assistant-service để nhận điều phối/policy mới, mở hội thoại mới trên web. Không cần migration hoặc thay đổi biến môi trường cho đợt mở rộng này. Icon BotMessageSquare và source frontend hiện tại không bị ghi đè.
