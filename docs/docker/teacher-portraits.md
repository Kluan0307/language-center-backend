# Ảnh chân dung giáo viên mẫu — 25/11/2026

Đã gắn 8 ảnh chân dung khác nhau cho 8 hồ sơ giáo viên đang công khai trong database Docker. Ảnh được tải lên Cloudinary qua API nhóm `teachers`, không sửa trực tiếp SQL. Giữ nguyên tài khoản, tên, chuyên môn, bằng cấp, phân công và trạng thái công bố.

Đây là **ảnh stock minh họa cho bộ dữ liệu mẫu, không phải ảnh thật của giáo viên mang tên trong hồ sơ**. Phần giới thiệu ghi rõ điều này; thẻ giáo viên trên website có nhãn “Ảnh minh họa”. Khi có ảnh thật được phép công khai, thay qua Nội dung website → Giáo viên. Khung ảnh riêng của giáo viên ưu tiên phần trên để không cắt mất mặt, không đổi cách hiển thị ảnh khóa học/chi nhánh.

Nguồn và giấy phép: [Pexels License](https://www.pexels.com/license/). Danh sách tác giả, URL nguồn và ánh xạ từng hồ sơ được lưu trong [báo cáo nhập](teacher-portraits.json). Người trong ảnh không được xác nhận là nhân viên hoặc người bảo trợ trung tâm.

- Website: http://localhost:13000/giao-vien
- Ảnh gốc: `storage/teacher-portraits/sources/`.
- Tiến độ, ID Cloudinary, hồ sơ trước/sau: `storage/teacher-portraits/import.json`.
- Script nhập dành riêng cho bộ demo hiện tại: `scripts/docker/seed-teacher-portraits.cjs`.
- Kiểm tra ảnh tải trong Chrome: `node scripts/docker/verify-teacher-portraits-ui.cjs`.
- [Ảnh màn hình desktop](screenshots/teacher-portraits-1440.png), [mobile](screenshots/teacher-portraits-390.png).

Backup trước khi nhập: `ops-1795599740594-79ee35`; backup trước khi build lại website: `ops-1795599976721-8115b4`. Không khôi phục toàn bộ database chỉ để gỡ ảnh: việc đó có thể làm mất thay đổi mới. Có thể dùng nội dung trước cập nhật trong manifest hoặc lịch sử CMS để sửa riêng từng hồ sơ. Ảnh vẫn nằm trong lịch sử phải được giữ theo quy tắc bảo vệ media hiện có.

Đã kiểm tra API danh sách/chi tiết của cả 8 hồ sơ, URL Cloudinary, checksum ảnh tải về và đối chiếu backup để xác nhận không thay đổi bảng nghiệp vụ ngoài media/hồ sơ công khai/nhật ký. Database MySQL riêng trên Windows không được cập nhật trong tác vụ này.
