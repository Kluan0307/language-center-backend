# Cài đặt và nghiệm thu — S4, S5

Cập nhật 14/11/2026. Hoàn thiện ba mục cuối sidebar: `/company`, `/help`, `/settings`. Tiếp nối [S1–S3](company-support-s1-s3.md).

## S4 — Cài đặt có hiệu lực thực tế

Trang Cài đặt gồm:

| Phần | Chức năng | Quyền |
|---|---|---|
| Tùy chọn cá nhân | Bảng gọn, số dòng 10/20/50, lịch tuần/ngày, chi nhánh mặc định | Bốn role web, chỉ tài khoản của mình |
| Tài khoản và bảo mật | Mở hồ sơ/liên hệ, đổi mật khẩu, đăng xuất tất cả thiết bị | Dùng luồng bảo mật hiện có |
| Thông tin trung tâm | Tên, logo HTTPS, địa chỉ, email và số liên hệ công khai | SUPER_ADMIN sửa; bốn role đọc thông tin công khai |

- Tùy chọn lưu trong database theo account ID; không dùng chung localStorage giữa các tài khoản. Logout/login hoặc refresh tải lại tùy chọn từ server.
- Bảng gọn áp dụng padding của bảng bằng styled-components. Số dòng áp dụng cho các danh sách dùng `useListQuery`; giá trị kích thước trang được chọn rõ trên URL vẫn được ưu tiên.
- Lịch mặc định áp dụng màn hình xem buổi/lịch dạy dùng time-grid tuần/ngày. Không thêm lựa chọn tháng vì thành phần lịch hiện tại không có chế độ đó. Trước khi người dùng lưu tùy chọn lần đầu, giữ lịch ngày mặc định trên màn hình nhỏ.
- Chi nhánh mặc định chỉ áp dụng lần đăng nhập kế tiếp, không đột ngột chuyển phạm vi đang làm việc. Chỉ chọn trong phân công hiện hành và chi nhánh active. Thu hồi phân công sẽ xóa lựa chọn mặc định và tăng version. SUPER_ADMIN giữ phạm vi toàn hệ thống, chọn chi nhánh tại các trang nghiệp vụ.
- Giữ tiếng Việt và múi giờ Việt Nam. Không hiển thị các công tắc ngôn ngữ/dark-mode chưa được triển khai.
- Tên/logo/liên hệ trung tâm hiển thị tại Tổng quan Công ty. Logo dùng URL HTTPS, không chấp nhận javascript/data URL và không tải file về backend. Chưa có chức năng upload logo.
- Version chống ghi đè giữa hai lần mở cài đặt. Chi nhánh mặc định và account ID do server kiểm tra; client không thể sửa tùy chọn của người khác.

## API và migration

Migration [V022](../migrations/V022__web_settings.sql) đã áp dụng local, bổ sung `user_preferences` và `company_profile`. Giữ nguyên tài khoản, mật khẩu, cơ cấu và bộ dữ liệu bốn chi nhánh. Không nạp tùy chọn cá nhân giả; mỗi người tự lưu lựa chọn của mình.

| Method | Route | Quyền |
|---|---|---|
| GET / PUT | `/api/v1/auth/settings/preferences` | Bốn role web; chính mình |
| GET | `/api/v1/auth/settings/company` | Bốn role web |
| PUT | `/api/v1/auth/settings/company` | SUPER_ADMIN |

STUDENT dùng mobile và bị từ chối các API cài đặt web trên.

Máy khác cần chạy V021 và V022 trước khi chạy Auth với `ddl-auto=validate`. Script kiểm tra migration có thể chạy lại:

```powershell
node scripts/identity/settings-migration.cjs --apply
```

Khởi động lại **auth-service**, tải lại web và đăng nhập lại. Cấu hình gateway, khóa API, VNPAY và file môi trường không đổi.

## S5 — Kiểm tra ba mục và giao diện

- Sidebar desktop/mobile có ba liên kết Công ty/Hỗ trợ/Cài đặt, khu vực riêng ở cuối và trạng thái đang chọn.
- Bảng Công ty/Hỗ trợ có cuộn ngang bên trong để không đẩy rộng trang trên điện thoại; modal/drawer dùng bố cục Ant Design hiện có.
- Xử lý tải dữ liệu, dữ liệu trống, lỗi API và từ chối thao tác không đủ quyền. Nếu tải tùy chọn lỗi, các trang nghiệp vụ vẫn dùng mặc định; form Cài đặt yêu cầu tải lại trước khi lưu.
- Dữ liệu tổ chức mẫu từ S1–S3 vẫn giữ nguyên: 9 đơn vị, 9 chức vụ, 22 phân công; không tự gán Chủ tịch/CEO.

Kết quả:

| Kiểm tra | Kết quả |
|---|---|
| Auth | 109/109 đạt, gồm lưu riêng tài khoản, version, thu hồi chi nhánh mặc định và quyền sửa thông tin công ty |
| Web | 126/126 đạt; thêm kiểm tra số dòng mặc định và URL ghi đè; ba liên kết sidebar theo role |
| Build/lint | TypeScript/Vite và lint các file mới/chỉnh sửa liên quan đạt |
| Auth + Academic + MySQL riêng | API thật về Công ty/Hỗ trợ/Cài đặt, không sửa dữ liệu dự án trong kiểm thử |
| Chrome headless | Desktop 1440×1000 và mobile 390×844; trang Cài đặt, bảng gọn, không tràn trang Hỗ trợ và ba mục footer hiện trong drawer |

Tổng bộ kiểm tra HTTP và Chrome có **35 tình huống đạt**. Có chụp ảnh desktop/mobile; đường dẫn nằm trong `storage/company-s45-http.json`. Đây là kiểm tra trình duyệt với viewport mobile, chưa thay thế trải nghiệm chạm trên điện thoại vật lý.

Tái kiểm tra (cần JAR Auth/Academic đã build, Chrome và các cổng riêng còn trống):

```powershell
node scripts/identity/verify-company-http.cjs --settings --browser
```

Script tạo bản sao MySQL, chạy service ở cổng 30181–30182, proxy/Vite 30280/30290 và Chrome 30222. Các tiến trình, database thử được dọn sau khi chạy. Cấu hình cài đặt trong lượt thử không ghi vào database dự án.

Bằng chứng: `storage/company-s45-auth-final.log`, `storage/company-s45-web-tests.log`, `storage/company-s45-web-build.log`, `storage/company-s45-web-lint.log`, `storage/company-s45-http.json`.
