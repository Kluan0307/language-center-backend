# Mobile student — M7 và M8

Ngày hoàn thiện: 15/11/2026. Tiếp nối [M1–M3](mobile-ux-m1-m3.md), [M4–M6](mobile-ux-m4-m6.md).

## M7 — Hiệu năng, mạng yếu và giữ trạng thái

Đã triển khai `src/lib/query/client.ts`, `StudentQueryProvider`, hook `useStudentQuery` mới và thông báo trạng thái `QueryFeedback`.

| Nội dung | Quy tắc |
|---|---|
| Phạm vi dữ liệu | Cache trong RAM, riêng theo API client, tài khoản và trạng thái đổi mật khẩu bắt buộc; không lưu dữ liệu học viên ra AsyncStorage/tệp |
| Trùng truy vấn | Cùng đường dẫn và kiểu phân trang dùng chung yêu cầu đang chạy |
| Dữ liệu thông thường | Tái sử dụng trong 15 giây; mở lại sau đó kiểm tra mới |
| Dữ liệu nhạy với cập nhật | Học phí, điều kiện thanh toán, điểm, kết quả cuối khóa, chuyên cần, phiên bản hỗ trợ/cài đặt luôn kiểm tra lại khi màn được kích hoạt |
| Mạng/timeout/5xx | Giữ dữ liệu đã tải của cùng tài khoản; báo đang xem thông tin cũ và thời gian cập nhật gần nhất; có Thử cập nhật |
| 401/403/404, dữ liệu sai hoặc xung đột | Không giữ bản dữ liệu riêng tư cũ của truy vấn bị từ chối |
| Rời màn hình | Hủy truy vấn khi không còn màn đang dùng; phản hồi muộn không được ghi trở lại cache |
| Logout/đổi tài khoản/server | Tạo vùng cache mới, hủy/xóa vùng cũ; không cho tài khoản mới thấy dữ liệu tài khoản trước |
| Bộ nhớ | Thu hồi dữ liệu không dùng theo LRU, tối đa 40 mục không được bảo vệ bởi màn đang hoạt động/yêu cầu đang chạy; mục không dùng quá 5 phút được thu hồi khi dọn cache |
| Quay về từ nền | Làm mới truy vấn và xác thực lại `/auth/me` mà không đưa toàn app về loading; bản nháp được giữ khi mạng tạm lỗi |
| Phiên bị thu hồi/đổi role | Xóa phiên; yêu cầu đổi mật khẩu bắt buộc vẫn được áp dụng |
| Cập nhật tài chính/push | Vô hiệu cache liên quan để màn đang dùng tải lại; không tự phát lại lệnh ghi dữ liệu |

`Checkout` tiếp tục đọc điều kiện thanh toán mới trước khi tạo lệnh, chặn xác nhận khi điều kiện chỉ còn là dữ liệu cũ sau lỗi mạng. UUID, recovery, kiểm tra số dư và xác nhận bằng backend/IPN giữ nguyên.

Đây là khả năng đọc thông tin đã tải trong phiên đang mở khi mạng yếu, không phải chế độ đăng nhập offline hoặc tạo thanh toán offline. Sau khi đóng tiến trình, cache RAM không còn.

### Đo trước/sau

Kịch bản cố định: **lớp → điểm → lớp → chuyên cần → lớp**. Cùng dữ liệu tổng hợp và thao tác, dùng bundle M4–M6 làm mốc trước M7:

| Chỉ số | Trước | Sau |
|---|---:|---:|
| GET thông tin cùng một lớp | 5 | 1 |

Giảm 80% số truy vấn thông tin lớp trong kịch bản này. Không suy ra toàn bộ ứng dụng nhanh hơn 80%; chưa có số đo mạng/thiết bị thật. Không đưa bảng debug hoặc dữ liệu kiểm thử vào app nghiệp vụ.

## M8 — Nghiệm thu tự động và APK nội bộ

- Phiên bản **1.1.0**, Android **versionCode 2**, package `com.languagecenter.student`.
- APK ARM64: `language-center-student-app/artifacts/LanguageCenter-1.1.0-sandbox-arm64-20261115014613.apk`.
- Dung lượng 28.783.665 byte (~28,8 MB).
- SHA-256: `b8f3f20e9544e02d300cf65b854d92f3f35e95be1c6d56319f14900c9e892c80`.
- API đã đóng gói: `https://subsystem-handwork-subscript.ngrok-free.dev/api/v1`.
- Chữ ký xác minh hợp lệ và khớp APK 1.0.0 trước; có thể cài đè khi bản đang cài dùng cùng khóa đó.
- Manifest không debuggable; phiên bản Android và Expo khớp; JS và cấu hình nhúng cùng dùng HTTPS Gateway; không chứa endpoint/dữ liệu giả lập của bộ nghiệm thu.
- Đây là bản cài nội bộ ký bằng khóa debug của Android project hiện có, chưa phải bản phát hành Google Play.

### Kết quả kiểm tra

| Kiểm tra | Kết quả |
|---|---|
| TypeScript | Đạt |
| Tests app | 108/108 đạt |
| Hồi quy Trang chủ/Lịch học | 23/23 đạt |
| Hồi quy Học tập/Học phí/Hỗ trợ/Cá nhân | 35/35 đạt |
| M7 cache, mất mạng, giữ bản nháp, đổi tài khoản | 11/11 đạt |
| Android `assembleRelease` | Thành công |
| Chữ ký, phiên bản, cấu hình API và quét endpoint giả lập trong APK | Đạt |
| Cài/chạy trên điện thoại, TalkBack, push thật và VNPAY qua app thật | Chưa nghiệm thu; chưa có thiết bị ADB kết nối |

Browser tests chạy với HTTP server loopback và dữ liệu tổng hợp; không thêm dữ liệu vào database hoặc gửi thanh toán tới VNPAY. Script nghiệm thu: `scripts/student-app/verify-mobile-ux.cjs`, extension `verify-mobile-m78.cjs`, và `verify-apk.ps1`.

Chứng cứ: [đo trước](validation/m78/performance-before.json), [đo sau](validation/m78/performance-after.json), [11 tình huống M7](validation/m78/ui-m7.json), [35 hồi quy M4–M6](validation/m78/ui-m456.json), [23 hồi quy M1–M3](validation/m78/ui-m123.json), [manifest APK](validation/m78/apk-manifest.json), [xác minh APK](validation/m78/apk-verification.json). Ảnh dùng dữ liệu tổng hợp: [điểm đã tải khi mất mạng](validation/m78/cached-grades-offline.png), [giữ bản nháp khi quay về app](validation/m78/resume-keeps-draft.png).

### Điều kiện hiện tại để đăng nhập APK

Khi bàn giao, Gateway local `127.0.0.1:8080` chưa chạy; URL ngrok trả `404 / ERR_NGROK_3200`. Vì vậy bản APK đã build/xác minh nhưng chưa thể đăng nhập cho đến khi backend và tunnel hoạt động. `.env.local` của Expo vẫn giữ URL LAN phát triển, không bị ghi đè thành URL kiểm thử hoặc URL phát hành.

1. Bật MySQL và chạy các service trong những terminal riêng, từ thư mục backend chứa `scripts`:

   ```powershell
   .\scripts\run-service.ps1 -Service auth-service
   .\scripts\run-service.ps1 -Service academic-service
   .\scripts\run-service.ps1 -Service teaching-service
   .\scripts\run-service.ps1 -Service finance-service
   .\scripts\run-service.ps1 -Service api-gateway
   ```

   Mỗi lệnh chạy ở một terminal. Assistant-service không phải điều kiện để dùng app học viên.

2. Mở ngrok trỏ Gateway cổng 8080 và giữ đúng tên miền HTTPS đã đóng gói. Nếu dùng URL khác, phải build lại APK với URL mới; đổi `.env.local` trên máy không sửa APK đã cài.

3. Kiểm tra `/api/v1/auth/me` qua HTTPS: chưa đăng nhập thì HTTP 401 là phản hồi bình thường từ backend. `ERR_NGROK_3200` nghĩa tunnel chưa hoạt động.

4. Cài bản mới trên điện thoại Android ARM64:

   ```powershell
   adb install -r .\artifacts\LanguageCenter-1.1.0-sandbox-arm64-20261115014613.apk
   ```

   Chạy từ thư mục mobile, hoặc chép APK sang điện thoại và mở để cài. Bản cài này không cần Metro/Expo Go. Chưa tự động cài vì không có thiết bị kết nối.

5. Nghiệm thu thiết bị: đăng nhập → lịch/đổi tuần → điểm/chuyên cần/tài liệu → học phí/phiếu thu → hỗ trợ/thông báo → đưa app ra nền/quay lại → mất mạng/thử lại → logout/đổi tài khoản. Chỉ thử thanh toán sandbox sau khi callback/IPN hoạt động đúng. Push cần EAS/Firebase và quyền thiết bị.

## Trạng thái bàn giao

**M7 hoàn thành phần triển khai và kiểm thử. M8 hoàn thành build APK, xác minh tĩnh và nghiệm thu trên trình duyệt; nghiệm thu thiết bị/kết nối thực tế còn chờ backend/ngrok và điện thoại.** Không coi các bước chưa thực hiện là đã đạt.
