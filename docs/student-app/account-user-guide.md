# Tài khoản học viên — hướng dẫn chạy giai đoạn 3

## Database và cập nhật backend

V014 đã được kiểm tra trên MySQL riêng, bao gồm chạy lại migration. **Chưa áp dụng vào database hiện tại của bạn**, không reset hoặc chèn tài khoản mẫu vào dữ liệu gốc.

1. Dừng các service trước khi cập nhật schema; backup database theo hướng dẫn vận hành hiện có, giữ file backup ngoài git.
2. Mở [V014__student_accounts.sql](../migrations/V014__student_accounts.sql) trong MySQL Workbench, chọn đúng schema hiện tại rồi chạy toàn bộ. Máy mới: chạy schema-v013-clean.sql trên database trống, sau đó V014. Không chạy clean schema lên database hiện hữu.
3. Build/restart cả Auth, Academic, Teaching, Finance và Gateway từ source hiện tại. Nếu dùng Maven, tại mỗi thư mục service chạy `mvn spring-boot:run` với .env.local/cấu hình hiện có. Phiên bản cũ của các service chưa nhận role STUDENT; không chỉ nâng riêng Auth.
4. Chạy web frontend bằng `npm run dev`. Bản ZIP release giai đoạn6 trước đây không chứa tính năng Student mới; giữ nó làm mốc lịch sử.

V014 thêm role STUDENT vào accounts.role, students.account_id nullable unique với FK restrict, bảng student_account_provisionings lưu ý định cấp tài khoản và phiên bản quản lý. Không thay mật khẩu/tài khoản hiện có. Trước rollback code phải xử lý các account STUDENT; không hạ enum hoặc xóa link/dữ liệu tự động.

## Cấp tài khoản trên web

- Đăng nhập SUPER_ADMIN hoặc ADMIN; vào Học viên -> Xem -> **Tài khoản ứng dụng**.
- Admin chỉ quản lý học viên thuộc chi nhánh đang được phân công. Staff/Teacher không có tab cấp/khóa/reset này.
- Xác minh chủ hồ sơ, chọn Cấp tài khoản; nhập username3–50ký tự Latin/số/./_/- và mật khẩu tạm ít nhất8ký tự, tối đa72byte UTF-8, xác nhận khớp. Không dùng mật khẩu chung cho tất cả học viên.
- Bàn giao thông tin qua kênh đã xác minh. Học viên phải đổi mật khẩu trước khi xem dữ liệu học tập.
- Nếu mất phản hồi, giữ modal và dùng Kiểm tra kết quả. Gửi lại giữ nguyên requestId và dữ liệu; không tạo yêu cầu khác để cố cấp thêm account.
- Khi cần hỗ trợ: Cấp lại mật khẩu có lý do, bắt đổi lại và thu hồi các phiên. Reset không tự mở khóa; Mở khóa là thao tác riêng. Khóa không đổi hồ sơ học tập/công nợ. Tài khoản có version chống cập nhật trên trạng thái cũ; gặp409 hãy đóng modal và tải lại trạng thái.

## Chạy app

Tại language-center-student-app:

```powershell
npm ci
Copy-Item .env.example .env.local
npm start
```

Sửa `.env.local` trước khi chạy:

```dotenv
EXPO_PUBLIC_API_BASE_URL=http://<IP-LAN-may-chu>:8080/api/v1
# Điện thoại liên hệ công khai của trung tâm, tùy chọn:
# EXPO_PUBLIC_CENTER_PHONE=<so-dien-thoai-trung-tam>
```

Dùng IP thật, không giữ dấu<> hoặc IP mẫu. localhost trên điện thoại là điện thoại. Gateway phải truy cập được từ LAN; nếu dùng profile prod mặc định loopback, cấu hình SERVER_ADDRESS cho Gateway phù hợp mạng thử nghiệm. Auth/các service còn lại có thể tiếp tục chỉ lắng nghe loopback. Triển khai thật dùng HTTPS.

Mở Expo Go tương thích SDK58 hoặc development build. Đăng nhập STUDENT vừa được cấp -> đổi mật khẩu tạm -> đăng nhập lại -> tab Cá nhân xem hồ sơ và chi nhánh. Tài khoản nhân sự bị từ chối trên app; STUDENT không đăng nhập web quản trị.

Khôi phục mật khẩu/cập nhật hồ sơ: màn hình Hỗ trợ hiển thị liên hệ chi nhánh khi đã đăng nhập; trước đăng nhập dùng số công khai được cấu hình hoặc liên hệ trực tiếp trung tâm. Chưa có OTP/email tự động. Chức năng gửi và theo dõi ticket thuộc G6; hiện không có nút gửi yêu cầu giả.

SecureStore dùng trên native. Browser preview chỉ giữ phiên trong bộ nhớ, refresh phải đăng nhập lại. Đăng xuất tất cả thiết bị gọi server revoke, sau đó xóa phiên/cache cục bộ; nếu offline chỉ xác nhận thoát trên máy này. Không lưu password, JWT secret hoặc DB password trong app.

## Giới hạn giai đoạn này

Tài khoản và hồ sơ đã nối API thật. Lớp/lịch, điểm/chuyên cần, tài liệu, học phí, thanh toán và thông báo được triển khai ở các giai đoạn sau. Nút Khám phá giao diện chỉ là preview ở development, không cấp quyền API.

Đã kiểm chứng browser kích thước mobile và xuất Hermes Android bundle; chưa có thiết bị Android kết nối để xác minh SecureStore/native back/keyboard. Bundle không phải APK. Không ghi nhận các phần này là đã nghiệm thu thiết bị thật.
