# Theo dõi học tập và chốt cuối khóa

Ngày 08/11/2026; dùng sau khi nâng đủ migration đến V013 và restart Academic/Teaching/FE.

1. Staff mở Học viên → Xem → Đánh giá đầu vào để xem chứng cứ trình độ. Tab Hồ sơ học tập có từng đăng ký, khoảng học/bảo lưu/chuyển lớp, bản ghi điểm danh và kết quả. Admin còn xem điểm tạm và liên kết bảng điểm; Teacher dùng bảng điểm lớp chính, không CRM học viên.
2. Trên lớp: kiểm tra kế hoạch phút, buổi hoàn tất/hủy/bù và điểm danh. Xử lý thiếu phút hoặc UNKNOWN bằng chứng cứ thực tế. Nếu giờ buổi COMPLETED cũ thiếu/sai, quản lý dùng Lịch buổi → Xem → Đối soát giờ đã học; cần version/lý do và mở lại kết quả nếu đang khóa.
3. Admin/Superadmin mở Bảng điểm → chọn lớp → Đánh giá tạm / quy tắc; kiểm tra policy, điểm bắt buộc, thang/trọng số và điều kiện chuyên cần. Chưa đủ dữ liệu không là đạt hoặc chưa đạt chính thức.
4. Tab Kết quả chính thức → Xem trước / chốt từng đăng ký → đọc blockers và nhập lý do. Chốt khóa nguồn toàn lớp. Nếu mất phản hồi, giao diện tra intent; tải lại trước khi lập thao tác mới. Lịch sử lưu snapshot/quy tắc/người chốt và các lần mở lại.
5. Theo liên kết Đồng bộ hoàn thành sang Đăng ký, chọn Đồng bộ hoàn thành học vụ và nhập lý do. Cả kết quả đạt/chưa đạt đều có thể hoàn tất quá trình học. Nếu Teaching lỗi, kết quả đã chốt còn nguyên, Academic không đổi; tải lại và gửi lại khi service trở lại. Không ghi danh hoặc tạo học phí mới.
6. Sau khi mọi đăng ký đang học/bảo lưu đã xử lý: Lớp → Xem → Đóng / mở lại lớp → Đóng lớp có đối soát. Hết ngày học và đóng lớp được hiển thị riêng. Terminal legacy thiếu chứng cứ không tự được xác nhận.
7. Khi cần sửa: Admin mở lại kết quả toàn lớp với lý do/phiên bản. Nếu lớp đã đóng, mở lớp với lý do. Sửa nguồn qua chức năng có quyền/audit, chốt lại từng đăng ký, đồng bộ refs mới và đóng lớp lại. Enrollment COMPLETED/học phí/lịch sử không tự đảo.

Các link giữ studentId/classId/enrollmentId và chi nhánh theo nơi gọi; bộ lọc chính học viên/lớp/ghi danh/học phí lưu URL để reload. Đổi chi nhánh hủy request cũ theo cơ chế AuthContext/remount. Bảng rộng cuộn ngang trong vùng bảng trên mobile; các form có kiểm tra dữ liệu, khóa lúc gửi và xác nhận bỏ dữ liệu chưa lưu.
