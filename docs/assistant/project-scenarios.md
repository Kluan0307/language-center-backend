# Bộ kịch bản mở rộng chatbot toàn dự án

**Tài liệu lịch sử:** đã được thay bằng [lộ trình chỉ tra cứu R1–R8](readonly-roadmap.md) và [120 kịch bản theo role](readonly-scenarios.md). Không còn yêu cầu thực hiện mục 4–5 về mutation trước khi mở rộng tra cứu.

Yêu cầu bổ sung ngày 13/11/2026, thực hiện sau mục 4–5. [Danh mục có cấu trúc](project-scenarios.json) gồm 108 mẫu câu thuộc 18 nhóm. Các câu là **kịch bản phát triển/kiểm thử**, chưa phải bằng chứng toàn bộ chức năng đã hoạt động. Thay placeholder bằng dữ liệu tổng hợp có oracle trước khi chạy.

## Cách biến kịch bản thành khả năng thực tế

1. Chốt ý nghĩa nghiệp vụ, quyền role/tài nguyên và trường hợp thiếu thông tin cần hỏi lại.
2. Kiểm API hiện có. Bổ sung API nếu chưa có nguồn dữ liệu hoặc định nghĩa đúng; lọc và tổng hợp trước phân trang.
3. Khai báo công cụ/schema MCP kín, giới hạn trường trả về và công cụ theo từng role.
4. Bổ sung ý định, biến thể diễn đạt và hội thoại tiếp nối; dùng câu mẫu để hướng dẫn model chọn công cụ.
5. Kiểm oracle, công cụ thực sự được gọi, từ chối vượt quyền, độ trễ và lỗi provider.
6. Chỉ chuyển trạng thái hỗ trợ sau khi triển khai và nghiệm thu. Số lượng câu mẫu không đồng nghĩa số chức năng đã có.

Không cần fine-tune để bổ sung các nghiệp vụ này. Dữ liệu vận hành cần lấy từ API tại thời điểm hỏi. Bộ câu mẫu hỗ trợ prompt/schema, phân loại ý định và eval; không dạy model ghi nhớ dữ liệu học viên.

## Thứ tự triển khai

| Đợt | Nhóm | Điều kiện |
|---|---|---|
| A | Khóa, lớp, học viên, lịch, điểm danh, điểm, học phí, lead | Tận dụng công cụ hiện có; nghiệm thu từng câu/role, từ đồng nghĩa và các lượt hỏi tiếp |
| B | Giáo viên, nhân viên, phòng và phân công chi nhánh | Bổ sung công cụ theo đúng API Auth/Academic; không truy cập bảng tài khoản bằng quyền chung |
| C | Công việc tồn, vắng nhiều, lịch trống | Định nghĩa khoảng ngày/giờ, threshold và thời điểm quan sát; thiếu lịch không chứng minh giáo viên sẵn sàng nhận dạy |
| D | Thống kê, đếm, so sánh | Phân biệt tổng hồ sơ/học viên có lớp; học phí phải thu/thực thu/hoàn tiền; ngày nghiệp vụ UTC+7; không cộng một trang thành tổng |
| E | Bản nháp và xác nhận thao tác | Hoàn thiện mục 4–5 trước, áp dụng whitelist từng thao tác, kiểm lại quyền/version, chống trùng và phục hồi kết quả chưa rõ |

## Các biến thể bắt buộc cho mỗi ca

- Có dấu/không dấu, câu ngắn, từ đồng nghĩa; lỗi chính tả chỉ được tự sửa khi không gây nhầm người/mã.
- Đủ thông tin, thiếu ngày/giờ, trùng tên, nhiều lớp, không có kết quả; hỏi “bạn ấy”, “lớp đó”, “còn dưới…”, “trang sau”.
- Hai tài khoản cùng role nhưng khác phân công; teacher chính/dạy thay; thay đổi quyền hoặc chi nhánh giữa lượt; bản nháp hết hạn.
- Số 0/null/chưa ghi nhận; lớp đầy/chưa khai giảng/kết thúc; bảo lưu/chuyển lớp; phiếu thu hủy/hoàn và đơn online đang chờ.
- Câu hỗn hợp, giả role, injection trong câu hỏi/dữ liệu; lỗi API, 429, timeout; lặp xác nhận và mất kết nối sau ghi.

Đối với thao tác ghi, model chỉ đề xuất bản nháp. Câu “tôi xác nhận” trong hội thoại không thay nút xác nhận gắn với một bản xem trước đã được server cấp và ràng buộc đúng người/phạm vi/nội dung.

## Tình trạng hiện tại

Mục 4–5 chưa được đánh dấu hoàn tất. Danh mục này không tự cấp quyền hoặc kích hoạt công cụ ghi. Các nhóm `EXISTING_TOOLS_NEEDS_ACCEPTANCE` chỉ có nguồn công cụ nền; cần kiểm từng câu, không phải tất cả biến thể đều đã được hỗ trợ. Các nhóm `PLANNED_REQUIRES_IMPLEMENTATION` còn phải triển khai API/host/UI trước khi nghiệm thu.
