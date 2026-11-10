# Quy trình được phần mềm hỗ trợ — G13

Ngày08/11/2026. Mô tả **as-built** theo code G01–G12, không phải quy trình as-is tại trung tâm đã khảo sát. Quy trình thủ công hiện tại, thời gian thao tác, số người và tỷ lệ sai sót: **CẦN BỔ SUNG** bằng quan sát/phỏng vấn.

| Quy trình | Chủ thể/đầu vào | Các bước và kết quả | Điểm kiểm soát/ngoại lệ | UC |
|---|---|---|---|---|
| Tiếp nhận | Staff/manager; nhu cầu và liên hệ | Tạo lead → ghi contact thực tế → tư vấn → convert Student khi đủ trạng thái | Staff đúng người phụ trách; NOTE không là contact; không convert hai lần | UC18,03 |
| Xác định đầu vào | Staff/manager; nguồn đánh giá | DRAFT → confirm expectedCurrent → giữ bản thay thế/thu hồi và chứng cứ | Một current/chương trình; thiếu nguồn thì legacy chưa xác minh; không tự quy đổi | UC06,08 |
| Phân lớp và ghi danh | Staff/manager; Student và lớp | Preview/gợi ý → kiểm học thuật/giới hạn cứng → đề nghị ngoại lệ nếu cần → kiểm lại khi lưu | Preview không giữ chỗ; học thuật có thể ngoại lệ, quota/lịch/scope không thể | UC09,10 |
| Tạo phí và thu | Academic/Finance; enrollment billing root | Commit enrollment → after-commit thử tạo học phí → thu nhiều đợt/phiếu → journal điều chỉnh/đảo/hoàn | Hai service không atomic; Finance lỗi không xóa enrollment; retry cùng ý định, giữ root khi transfer | UC09,14 |
| Tổ chức giảng dạy | Manager/Teacher/Staff theo việc | Lớp và lịch mẫu → buổi thực tế → nghỉ/bù/dạy thay → điểm danh xác nhận buổi | Giáo viên thực tế khác kế hoạch; COMPLETED không sửa thông thường; thiếu giờ UNKNOWN | UC07,11 |
| Đánh giá | Teacher/manager; roster theo ngày | Bài và điểm → policy lớp được manager xác nhận → preview tạm | Missing khác0; không dùng danh sách STUDYING hôm nay thay roster ngày thi | UC12 |
| Chốt và hoàn tất | Manager; điểm/chuyên cần/phút đủ | Preview → confirm snapshot → đồng bộ COMPLETED riêng → đối soát đóng lớp | FAIL có thể học xong; INSUFFICIENT không chốt. Mở final toàn lớp trước sửa; class closed mở riêng; không đảo enrollment/tiền | UC13 |
| Nhân sự nghỉ/đổi nơi | Manager đủ mọi phân công | Đọc responsibilities → bàn giao từng lớp/buổi → recheck → INACTIVE/thu hồi | Không bulk rewrite lịch sử; lỗi nguồn fail closed; khóa account bảo mật là việc riêng | UC02,04,05 |
| Báo cáo | Người có quyền nguồn | Lọc kỳ/chi nhánh → tổng học viên/lượt học/thu ròng/nợ/credit và đánh giá | Phân biệt distinct student/enrollment; thu ròng theo giao dịch, không mặc định doanh thu kế toán | UC15 |

## Ranh giới cần ghi trong báo cáo

- Trạng thái profile Student, enrollment, session, provisional result, final result và class closure là các trục riêng. Không đặt một nút Hoàn thành rồi suy mọi trục cùng đạt.
- Một số khóa phối hợp dùng cùng MySQL schema (account khi bàn giao Teacher; class_result_barriers khi chốt). Business payload giữa service vẫn qua HTTP. Không gọi đây là distributed transaction, saga/outbox hoặc database-per-service đã triển khai.
- File giảng dạy nằm ngoài MySQL; snapshot metadata và file phải được backup cùng. Nhập phương thức BANK_TRANSFER/CARD/WALLET không có nghĩa đã tích hợp ngân hàng/payment gateway.
- Các nhánh này dùng đặc tả G01-v1; lịch thi lại/QR/e-learning/tự cấp chứng chỉ/tự tăng currentLevel không nằm trong phiên bản hiện tại.

[Đặc tả đầy đủ](use-cases.md) · [Quyền](role-permissions.md) · [Quy tắc](business-rules.md) · [Thiết kế động G15](design/dynamic-workflows.md). Đã hoàn thiện 10 sơ đồ Activity/Sequence chi tiết tại G15.
