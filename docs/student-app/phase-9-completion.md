# Student App — G9: thông báo và trang chủ

G9 đã triển khai code inbox, trang chủ, tùy chọn, đăng ký thiết bị và adapter Expo Push. Người dùng xác nhận chưa có Expo/EAS + Firebase, yêu cầu hoàn thiện code và hướng dẫn cấu hình. **Push qua Expo/FCM/APNs và hành vi trên điện thoại thật chưa nghiệm thu.** VNPAY vẫn mặc định tắt; G9 không cần merchant ngân hàng để chạy.

## Chạy ngay khi chưa có EAS/Firebase

1. Backup database hiện tại, kiểm tra đã áp dụng tuần tự đến V017. Chạy [V018](../migrations/V018__student_notifications.sql) trên đúng schema hiện tại; không nạp clean dump hoặc xóa bảng. Migration thêm5 bảng, có thể replay. Lần triển khai này chỉ áp dụng vào database kiểm thử riêng, chưa áp dụng database nguồn.
2. Giữ DB/JWT/URL các service như trước, thêm vào `.env.local` backend:

   ```dotenv
   NOTIFICATION_JOBS_ENABLED=true
   NOTIFICATIONS_PUSH_ENABLED=false
   EXPO_PROJECT_ID=
   EXPO_PUSH_ACCESS_TOKEN=
   VNPAY_ENABLED=false
   ```

3. Build/restart Auth, Academic, Teaching, Finance và Gateway cùng phiên bản. Có thể dùng `mvn spring-boot:run` tại từng service; `application.properties` đọc `.env.local` theo cấu hình hiện hành. `scripts/run-service.ps1` cũng nhận các biến mới.
4. Student app: cài dependency đã cập nhật (`npm ci` nếu checkout mới), đặt gateway trong `EXPO_PUBLIC_API_BASE_URL`, chạy `npm run start:go` để tiếp tục dùng Expo Go. Inbox, badge, trang chủ, cài đặt vẫn dùng được; đăng ký push cần development build.
5. Web giữ giao diện nghiệp vụ hiện hành. Đổi lịch, công bố/thu hồi điểm, chia sẻ/ngừng chia sẻ tài liệu, phản hồi công khai, thu tiền/đảo/hoàn/điều chỉnh sẽ phát sự kiện tại backend. Không cần thêm thao tác “gửi thông báo” thủ công cho các sự kiện này.

Worker inbox chạy khoảng5 giây/lần; app kiểm unread khi vào phiên, foreground và mỗi30 giây khi đang hoạt động. Có nút tải lại. Đây là cập nhật theo polling, chưa phải WebSocket.

## Những gì học viên sử dụng

- Inbox có tất cả/chưa đọc/đã đọc, phân trang, số chưa đọc toàn tài khoản. Chi tiết có đánh dấu đã đọc và mở nội dung liên quan. Đọc lại không giảm count nhiều lần. Khi chỉ mở trang chi tiết, trạng thái chỉ đổi khi bấm đánh dấu hoặc mở nội dung.
- Deep link `/notifications/{id}` giữ đích qua login/đổi mật khẩu. Học viên khác không đọc hoặc đánh dấu được ID đoán; endpoint tài nguyên kiểm lại quyền khi mở. Nội dung đã bị thu hồi hoặc nằm ngoài thời kỳ học có thể không mở được dù thông báo lịch sử còn tồn tại.
- Trang chủ có lớp đang học/bảo lưu, badge, cập nhật lịch chưa đọc, tổng học phí G7 và buổi tiếp theo. Mỗi module có lỗi/retry riêng. Thiếu xác minh hoặc thiếu hóa đơn không hiển thị tổng công nợ0 giả, không lấy tổng từ một trang.
- Cài đặt lưu tùy chọn push và6 nhóm lịch/điểm/học phí/thanh toán/tài liệu/hỗ trợ. Tắt push hoặc từng nhóm **không xóa hay lọc mất inbox bắt buộc**. Khi dịch vụ push chưa bật, giao diện nói rõ và không giả đã đăng ký/gửi thành công.

## Nguồn sự kiện và tính nhất quán

Outbox dùng chung schema hiện hành, được ghi trong cùng transaction nghiệp vụ. Academic đọc các hàng đã commit và tạo inbox cùng transaction đánh dấu DONE/SKIPPED. Rollback không tạo thông báo; khóa event + unique(event,account,student) ngăn gửi inbox trùng khi retry/nhiều worker. Không phát thông báo từ điểm nháp hoặc ghi chú hỗ trợ nội bộ.

Điều chỉnh so với draft G1: bản đầu dùng bảng outbox chung, không mở HTTP internal endpoint hoặc thêm broker/service chỉ để chuyển sự kiện trong cùng database. Khi tách database các service cần adapter transport có xác thực máy–máy. Các entity/contract outbox trong ba service phải được nâng đồng bộ với V018.

| Nhóm | Sự kiện thực hiện |
|---|---|
| Lịch | Sửa, hủy, khôi phục, học bù, đồng bộ thay đổi lịch tuần, hiệu chỉnh giờ; không spam sinh lịch ban đầu/điểm danh hoàn tất |
| Điểm | Công bố, thu hồi công bố; xác nhận/mở lại kết quả cuối khóa |
| Tài liệu | Chủ động chia sẻ/ngừng chia sẻ; duyệt tài liệu chưa chia sẻ không tự gửi |
| Hỗ trợ | Nhân sự phản hồi công khai hoặc đổi trạng thái; nội dung ghi chú/lý do nội bộ không vào thông báo |
| Học phí | Tạo khoản, điều chỉnh, nhắc hạn |
| Thanh toán | Phiếu thu thực tế, đảo/hoàn; outbox online thành công/hết hạn/cần đối soát của G8 |

Người nhận lịch được giải theo thời kỳ hiệu lực trên ngày buổi, cùng ngày cũ nếu di chuyển; fallback legacy đang học hoặc attendance đúng buổi theo quy tắc hiện hành. Đăng ký trùng không tạo2 thông báo cho cùng học viên. Tài liệu theo lớp/khóa+chi nhánh, không gửi cho đăng ký phát sinh sau thời điểm sự kiện; lịch sử đã học được giữ, hủy trước khi học không được nhận. Inbox lưu cố định accountId+studentId khi delivery; người chưa có account được SKIPPED, không backfill toàn bộ lịch sử khi cấp account sau này.

Thông báo chỉ chứa nội dung chung, không tên lớp/điểm/số tiền/ghi chú nhạy cảm. Liên kết tài nguyên là type+ID, không URL tùy ý. Inbox lịch sử không khẳng định trạng thái hiện tại của tiền/điểm; phải đọc lại API nguồn.

Nhắc hạn: trước hạn3 ngày, đúng hạn, quá hạn1 ngày và7 ngày; worker kiểm tra mỗi giờ, khóa dedup theo tuitionId+dueDate+kỳ nhắc. Chỉ khoản gốc có sở hữu/class/branch khớp, còn nợ VND, không hủy và ledger/cache đã xác minh mới được nhắc. Không tự sửa ledger. Thông báo không kèm số dư đóng băng; mở chi tiết để xem giá trị hiện tại. Hạn đổi rồi trở lại cùng ngày/kỳ không tạo nhắc lặp.

Finance chuyển `payment_outbox` G8 chưa xử lý sang notification outbox bằng cùng source key. `payment_outbox.deliveredAt` ở đây nghĩa đã chuyển vào kênh inbox, không có nghĩa điện thoại đã nhận. Không tạo thanh toán giả để thử thông báo. Không backfill các audit cũ khác trước G9.

## Đăng ký thiết bị và push

Auth quản lý installation UUID ngẫu nhiên, token Expo, project, account, sessionVersion và bindingVersion. Cùng nội dung đăng ký giữ nguyên binding; đổi chủ/token/phiên tạo binding mới. DELETE của account cũ không vô hiệu binding đã thuộc account mới. Token duy nhất trên một installation, không trả token trong DTO hoặc log payload.

Academic chỉ tạo push job khi server bật và tùy chọn hiện tại cho phép. Trước lần gửi kiểm lại binding, project, account hoạt động/role/mustChangePassword/sessionVersion và liên kết student, kiểm lại tùy chọn và tuổi thông báo. App logout cố gỡ thiết bị rồi gọi logout-all hiện hành; sessionVersion vô hiệu các binding cũ. Token đã đăng ký được làm mới khi vào phiên nếu quyền OS vẫn được cấp, không tự hỏi quyền lúc khởi động.

Payload push chỉ có lời nhắc chung cùng accountId+notificationId. App bỏ qua tap của account khác, không tin URL từ payload, tra inbox theo bearer rồi mới mở tài nguyên. Xóa last-response sau xử lý để không mở lại tap cũ. Không có thông tin điểm, tiền hoặc họ tên trên lockscreen.

Push qua Expo có trạng thái PENDING → SENDING → ACCEPTED → HANDED_OFF hoặc FAILED/UNKNOWN/SUPPRESSED. Ticket ACCEPTED chỉ là Expo chấp nhận, receipt HANDED_OFF là bàn giao cho FCM/APNs, **không phải điện thoại đã hiển thị hoặc người dùng đã đọc**. Kiểm receipt sau15 phút, trong cửa sổ24 giờ. DeviceNotRegistered vô hiệu đúng binding cũ, không khóa nhầm token vừa đăng ký lại.

Timeout/kết quả gửi không chắc chắn giữ UNKNOWN và không tự gửi lại để tránh push trùng; inbox vẫn còn. Tiến trình chết sau claim được đánh dấu UNKNOWN sau10 phút. Với lỗi cần xử lý vận hành, xem bảng delivery/outbox và sửa cấu hình; bản đầu chưa có trang quản trị replay push. Push đã nằm ở Expo/OS không thể thu hồi chắc chắn khi logout/offline; nội dung chung và kiểm account khi tap giới hạn việc lộ dữ liệu. Không tuyên bố exactly-once trên thiết bị.

## Cấu hình push Android khi có tài khoản

1. Tạo tài khoản Expo/EAS, tạo/liên kết project với app. Có thể chạy `npx eas-cli login` rồi `npx eas-cli init` trong student app. Ghi nhận project UUID thực tế; không dùng UUID fixture trong test.
2. Chọn Android applicationId của bạn, tạo Firebase Android app trùng applicationId. Lấy `google-services.json`; cấu hình FCM v1 credentials trong EAS theo [hướng dẫn Expo](https://docs.expo.dev/push-notifications/push-notifications-setup/). Service-account private key của FCM cấp cho EAS/backend theo hướng dẫn, không đưa vào JS bundle/git.
3. `.env.local` của app:

   ```dotenv
   EXPO_PUBLIC_API_BASE_URL=https://<gateway>/api/v1
   EXPO_PUBLIC_EAS_PROJECT_ID=<project UUID thực tế>
   EXPO_ANDROID_PACKAGE=com.yourcenter.student
   EXPO_ANDROID_GOOGLE_SERVICES_JSON=C:/private/google-services.json
   ```

   `app.config.ts` nhận các giá trị trên. `eas.json` đã có profile development, `expo-dev-client`, `expo-notifications`, `expo-device` đã được thêm. Project ID là cấu hình công khai, khác access token server.
4. Backend `.env.local` dùng cùng UUID:

   ```dotenv
   NOTIFICATION_JOBS_ENABLED=true
   NOTIFICATIONS_PUSH_ENABLED=true
   EXPO_PROJECT_ID=<cùng project UUID với app>
   EXPO_PUSH_ACCESS_TOKEN=<token Expo nếu bật enhanced push security>
   ```

   Nếu không bật enhanced push security, để trống EXPO_PUSH_ACCESS_TOKEN. Auth và Academic phải nhận cùng project. Academic từ chối khởi động nếu bật push mà thiếu UUID hợp lệ. Build/restart service sau khi đổi cấu hình; không đặt access token trong biến EXPO_PUBLIC.
5. `npx eas-cli build --profile development --platform android`; cài development build lên điện thoại thật. Chạy `npm run start:dev`, kết nối gateway phù hợp. Expo Go trên Android không hỗ trợ remote push từ SDK 53; dùng Expo Go chỉ nghiệm thu inbox. [Tài liệu SDK 58](https://docs.expo.dev/versions/v58.0.0/sdk/notifications/).
6. App → Cài đặt thông báo → lưu bật push/nhóm cần nhận → Đăng ký nhận thông báo → cho phép OS. Thử thay đổi lịch/phản hồi công khai/thu tiền từ web. Kiểm inbox khi foreground/background/app đóng, ticket/receipt server và tap mở đúng tài nguyên. Thử từ chối quyền, đổi account, logout-all, khóa/reset account, tắt từng nhóm, token cũ. Chỉ sau khi đạt mới ghi nghiệm thu push thật.

Tham chiếu provider: [Gửi push/ticket/receipt](https://docs.expo.dev/push-notifications/sending-notifications/). iOS cần APNs/build/thiết bị riêng, chưa nghiệm thu.

## API G9 thực tế

Bearer STUDENT đã kích hoạt, không tin studentId/accountId do client cung cấp. Prefix `/api/v1`:

| Method | Path | Body/query và kết quả |
|---|---|---|
| GET | `/academic/student/me/notifications` | page,size1..100,unread?,category?; PageResponse |
| GET | `/academic/student/me/notifications/unread-count` | `{count}` toàn tài khoản |
| GET | `/academic/student/me/notifications/{id}` | DTO đúng chủ;404 ngoài quyền |
| PUT | `/academic/student/me/notifications/{id}/read` | 204, idempotent |
| GET | `/academic/student/me/notification-preferences` | pushEnabled,schedule,grades,tuition,payment,documents,support,version,pushAvailable |
| PUT | `/academic/student/me/notification-preferences` | 7 boolean+version;200;409 stale |
| PUT | `/auth/student/me/devices/{installationId}` | UUID path; `{pushToken,platform:"ANDROID"|"IOS",projectId UUID}`; `{installationId,version,enabled}` |
| DELETE | `/auth/student/me/devices/{installationId}` | 204, chỉ binding hiện thuộc account |

Notice DTO: id,category,type,title,body,resourceType,resourceId,createdAt,readAt; thời gian offset+07:00. Không trả actor/internalNote/provider secret/push token. Các nhóm category SCHEDULE/GRADES/TUITION/PAYMENT/DOCUMENTS/SUPPORT. Resource SESSION/GRADES(enrollment)/DOCUMENT/SUPPORT/TUITION/RECEIPT/PAYMENT_ORDER; app chỉ ánh xạ type và ID đã kiểm định thành route nội bộ.

8 API mới, tổng 233: Auth26, Academic141, Teaching37, Finance29. Không thêm endpoint home/BFF hoặc endpoint gửi thông báo tùy ý cho học viên. Tài liệu draft G1 được thay bằng hợp đồng này cho nhóm notification/device.

## Nghiệm thu và giới hạn

Kết quả local: **490 tests backend** (Auth68, Academic225, Teaching95, Finance94, Gateway8), **61 tests app**, **96 tests web**. TypeScript app, Android Hermes export, lint/build web đạt; web vẫn có cảnh báo chunk lớn hiện hữu. Có32test mới backend về inbox/rollback/concurrency/audience, device binding/session/project và nhắc hạn.

MySQL riêng: V018 replay2lần,5JAR chạy prod/validate;63kiểm tra setup,41hồi quy điểm/chuyên cần,56tài liệu/hỗ trợ,50API G9,11luồng UI và13đối chiếu SQL. Các lần thử trước không đạt được giữ trong thư mục kiểm thử/attempt reports, báo cáo cuối chỉ đánh dấu PASS sau khi chạy đúng hợp đồng và fixture ổn định.

Ảnh: [inbox](figures/phase9-inbox.png), [chi tiết](figures/phase9-notice.png), [cài đặt](figures/phase9-preferences.png), [trang chủ](figures/phase9-home.png), [phản hồi trên web](figures/phase9-web-support.png). Edge dùng mobile viewport và API thật, không thay kiểm thử điện thoại. Có kiểm tra chặn mạng riêng Finance rồi retry; home không biến công nợ chưa đầy đủ thành tổng tiền giả.

Evidence nằm tại `storage/student-phase9-latest.txt` → thư mục MySQL riêng; API/UI/SQL reports, source manifest và hash JAR. Scripts tại `scripts/student-app/phase9`; prepare/run dùng phase9. Các fixture học vụ dùng SQL có nhãn riêng; các nguồn công bố/chia sẻ/hỗ trợ/thu/đảo/điều chỉnh/đổi lịch đã gọi API nghiệp vụ thật. Không suy rằng mọi writer học vụ đã được E2E lại chỉ từ fixture.

Push provider, notification permission/tap/SecureStore trên điện thoại, Android app-killed và iOS chưa được nghiệm thu. VNPAY sandbox thật vẫn chờ merchant/HTTPS như G8. Database nguồn và release ZIP cũ không thay đổi. G10 tiếp theo là bàn giao/thiết bị/E2E và các gate môi trường còn lại.
