# Giai đoạn 3 — Hoàn thiện Time-grid

Ngày: **11/11/2026**. Hoàn thành F04, F03 và phần cải thiện/kiểm tra F05 của [giai đoạn 1](2026-11-11-phase-1-current-state.md).

## Nội dung đã thực hiện

- Một nguồn tải buổi học trong SessionManagement; SessionCalendar chỉ trình bày. Chế độ lịch lấy đủ trang trong khoảng ngày đang xem; danh sách thường dùng phân trang backend. Không còn chạy đồng thời truy vấn danh sách và truy vấn lịch.
- Khoảng ngày sai chỉ chặn danh sách, không gửi yêu cầu sessions và không hiện cảnh báo sai khi chuyển sang lịch.
- URL giữ display, calendarView, date, branchId, classId, teacherId, roomId, status, page, size, from/to. Reload và Back khôi phục trạng thái; bỏ/chọn filter không xóa ngày và chế độ còn lại. Ngày/ID/status/phân trang không hợp lệ có xử lý mặc định; date rỗng của bộ lọc danh sách được giữ.
- Thêm chọn ngày trực tiếp, filter giáo viên và phòng. Giáo viên dùng profileId, không nhầm accountId. Teacher không tải danh mục nhân sự/phòng toàn chi nhánh; lựa chọn phòng lấy từ buổi được phép xem.
- API sessions hiện không có roomId: khi chọn phòng, tải đủ khoảng dữ liệu được backend phân quyền, lọc trước rồi mới phân trang. Không gửi tham số giả và không lọc riêng 10 dòng đang hiển thị. Đây là phương án FE; khoảng danh sách rất rộng có thể cần API roomId ở đợt tối ưu sau.
- Giữ lựa chọn lớp/giáo viên/phòng từ dữ liệu buổi thực tế, kể cả dạy thay; ID đang chọn có nhãn dự phòng khi chưa tải được tên.
- Cuộn gần buổi không hủy đầu tiên; nếu chỉ còn buổi hủy thì dùng giờ của chúng. Giữ toàn bộ khung giờ phù hợp, buổi thiếu giờ ở hàng riêng, các buổi cùng giờ đặt cạnh nhau. Không bật kéo thả hoặc mở rộng quyền chỉnh lịch.

## Kiểm tra

- **96/96 tests FE**, lint và build đạt. Bổ sung 7 ca cho UTC range qua năm/nhuận, URL restore/clear/invalid, request isolation, ngày sai không gọi API và lọc phòng trước pagination.
- Edge với API giả lập: không có yêu cầu sessions size10 trong chế độ lịch, tải trang thứ hai, filter giáo viên đúng profileId, filter phòng, reload URL, Back, tuần/ngày, lỗi ngày riêng danh sách, Teacher mobile không tràn trang và không có filter giáo viên.
- [Kết quả browser](../../storage/phase-3-preview/result.json), [ảnh lọc lịch](../../storage/phase-3-preview/filtered.png), [ảnh mobile](../../storage/phase-3-preview/mobile.png).
- Browser đặt múi giờ America/Los_Angeles; lịch vẫn dùng Asia/Ho_Chi_Minh. Các tests date-range dùng phép tính UTC, không dựa múi giờ máy.
- Chưa chạy lại backend thật/MySQL, chưa chứng nhận accessibility hoặc mọi tình huống lịch cực dày. Build giữ cảnh báo chunk lớn đã có. Database và API không thay đổi.

## Bước tiếp theo

Giai đoạn 4: **F06/F07/F08** — tạo bộ demo trên schema riêng qua quy trình API, thể hiện đầu vào đủ/thiếu/sai và ngoại lệ, học tập đến kết quả chính thức/hoàn tất/đóng lớp; có số liệu mong đợi từ đầu. Không dùng fixture SQL xem UI làm bằng chứng nghiệm thu toàn vẹn nghiệp vụ.
