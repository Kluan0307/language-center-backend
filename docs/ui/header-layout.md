# Thanh tài khoản và sidebar

Cập nhật ngày 26/11/2026 trên web quản lý và Docker web-admin.

- Sidebar dùng logo hiện có từ website trung tâm; bỏ khối avatar/tên/chức danh và bộ chọn chi nhánh cũ.
- Header cố định chứa bộ chọn chi nhánh và menu tài khoản: avatar, họ tên, chức danh thực tế (dự phòng nhãn role), hồ sơ, đổi mật khẩu và đăng xuất.
- Bộ chọn chi nhánh nằm bên trái header; avatar và thông tin tài khoản nằm bên phải. Đã bỏ chữ và biểu tượng “Không gian quản lý”.
- SUPER_ADMIN giữ phạm vi toàn hệ thống; các role còn lại dùng nguyên cơ chế chi nhánh, quyền và xác nhận chuyển hiện có.
- Desktop xếp cùng hàng. Màn hình nhỏ có nút mở menu, thông tin tài khoản và bộ chọn chi nhánh xuống hàng để không bị chèn lấn.
- Chiều cao header được đo để chừa đúng khoảng trống nội dung khi resize hoặc thông tin dài. Sidebar và header không cuộn theo trang; menu dài có cuộn riêng.
- Cụm cuối sidebar được gom vào nút hamburger **Thêm**: Nội dung website (SUPER_ADMIN/ADMIN), Công ty, Hỗ trợ, Cài đặt và Đăng xuất. Popup mở phía trên, thao tác cả dòng điều hướng được; chọn mục trên mobile sẽ đóng drawer.
- Build web đạt. [11 kiểm tra Chrome](header-layout.json) gồm cả bốn role, đổi chi nhánh, menu tài khoản, cố định khi cuộn và bố cục 320/390/900/1440 px. Không thay backend/schema/dữ liệu nghiệp vụ.

Mở http://localhost:15173 và tải lại trang để lấy bản giao diện mới. Ảnh kiểm chứng nằm trong `docs/ui/screenshots/header-admin-*.png`.
