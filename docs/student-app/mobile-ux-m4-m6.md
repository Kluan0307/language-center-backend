# Trải nghiệm ứng dụng học viên — M4, M5, M6

Ngày: 15/11/2026. Tiếp nối [M1–M3](mobile-ux-m1-m3.md). Source: `language-center-student-app`.

## M4 — Học tập theo từng lớp

Luồng chính: Học tập → Lớp hiện tại/Lịch sử học → Tổng quan lớp → Điểm/Chuyên cần/Tài liệu. Giữ nguyên mã đăng ký khi mở màn con; tên lớp và khóa học hiển thị trong các màn kết quả. Có kéo cập nhật và phân trang khi cần.

- Card lớp chạm được toàn bộ; trạng thái Đang học/Bảo lưu và chi nhánh hiển thị rõ.
- Tổng quan ngắn gồm trạng thái, giáo viên, chi nhánh và thời gian. Nút chính mở lịch; các mục Điểm, Chuyên cần, Tài liệu, Học phí và Hỗ trợ có nhãn/icon riêng.
- Thời kỳ và lịch sử đăng ký được thu vào phần mở rộng; giữ nguyên ý nghĩa mốc kết thúc loại trừ.
- Điểm công bố bằng 0 vẫn là `0 / thang điểm`; chưa chấm và chưa xác nhận kết quả không suy ra không đạt.
- Điểm danh chưa có dữ liệu hiển thị Chưa cập nhật; vắng có phép vẫn là vắng. Tỷ lệ giữ quy tắc backend, không tự tính từ danh sách một trang.
- Tài liệu giữ bộ lọc lớp, download có header xác thực, chặn bấm lặp và không đổi cơ chế chia sẻ/tệp riêng tư.

## M5 — Học phí, thanh toán và phiếu thu

Luồng chính: Học phí → Khoản cần đóng → Chọn số tiền → Xác nhận → VNPAY → Kết quả backend → Phiếu thu.

- Hai mục chính: **Cần đóng** và **Phiếu thu**. Giao dịch online là trang riêng, không còn tầng tab lồng nhau.
- Tổng số dư toàn học viên chỉ xuất hiện ở phạm vi toàn bộ học phí, không gắn nhãn thành số dư của một lớp.
- Khi mở từ lớp, chỉ truy vấn khoản của đăng ký đó. Xem phiếu thu/giao dịch liên quan qua chi tiết khoản, bằng `tuitionId` đã được API hỗ trợ. Không giả lập bộ lọc phiếu thu theo lớp trên API chỉ hỗ trợ theo khoản.
- Chưa xác minh không hiển thị thành số dư 0. Số tiền giữ decimal string, dùng currency của API; tạo thanh toán trực tuyến chỉ hỗ trợ VND theo hợp đồng hiện có.
- Xác nhận hiển thị học viên, lớp, khóa học, số tiền và phương thức. Đóng một phần kiểm tra số tiền dương, không vượt số còn phải đóng.
- Giữ toàn bộ cơ chế khóa thao tác, UUID cố định, lưu intent theo account, recovery khi timeout, kiểm tra số dư mới và phục hồi giao dịch đang chờ.
- Return/đóng browser không xác nhận thanh toán. PENDING, CREATED, FAILED, EXPIRED, REVIEW_REQUIRED, SUCCEEDED có hướng dẫn riêng. SUCCEEDED chưa có `receiptId` hiển thị Đã ghi nhận thanh toán/Phiếu thu đang cập nhật và cho kiểm tra tiếp.
- Kết quả thành công có phiếu thu mở trực tiếp; kết quả cần đối soát dẫn tới yêu cầu hỗ trợ chứa UUID. Về lịch sử bỏ bộ lọc lớp cũ rõ ràng.
- Không sửa endpoint VNPAY, chữ ký, callback/IPN, tài khoản sandbox, SQL hoặc cấu hình `.env.local`.

## M6 — Thông báo, hỗ trợ và cá nhân

### Thông báo

- Danh sách gọn, ba lựa chọn Tất cả/Chưa đọc/Đã đọc, dấu chưa đọc và thời gian. Chạm thông báo mở trực tiếp nội dung đã ánh xạ bằng loại tài nguyên/ID hợp lệ.
- Ghi đã đọc chạy nền, không bắt đợi trước khi mở nội dung. Badge chỉ lấy số từ backend; lỗi ghi đã đọc không tạo trạng thái đọc giả.
- Thông báo không có tài nguyên hỗ trợ mở trang nội dung. Trang chi tiết ghi đã đọc khi xem; có thử lại nếu lưu thất bại. Mọi tài nguyên đích được API kiểm lại quyền.
- Giữ hộp thư khi push chưa cấu hình, bị từ chối hoặc không được môi trường hiện tại hỗ trợ.

### Hỗ trợ

- Loại tài nguyên: PROFILE, ENROLLMENT, SESSION, TUITION, RECEIPT và PAYMENT_ORDER. Form kiểm tra tham chiếu, tải tên thông tin đính kèm từ API dành cho học viên và điền loại/tiêu đề phù hợp.
- Không gửi resourceKey UUID vào resourceId số; không tin tên/hồ sơ do query string tự cung cấp. Tham chiếu sai không tự rơi về form không liên kết.
- Khi cần chọn lớp, lấy đủ phân trang. Khi đã mở từ lớp/buổi/khoản thu thì không bắt chọn lại.
- Yêu cầu mới có tiêu đề, mô tả, loại và thông tin liên quan; giữ validation backend. Chặn bấm lặp; khi chưa rõ kết quả, gửi lại cùng requestId hoặc tra cứu thao tác.
- Hội thoại phân biệt Bạn/Trung tâm; có phản hồi, trạng thái và liên kết tài nguyên. Đóng yêu cầu cần xác nhận; giữ version chống xung đột.
- Giữ ngữ cảnh hỗ trợ, phiếu thu/giao dịch, bộ lọc lớp và tab lịch sử qua đăng nhập/đổi mật khẩu bắt buộc; chỉ giữ query đã cho phép.

### Cá nhân và cài đặt

- Hồ sơ đầu trang chỉ hiển thị tên, mã học viên, chi nhánh, trạng thái. Ngày sinh/liên hệ/địa chỉ trong phần mở rộng.
- Nhóm Hỗ trợ/Liên hệ và Cài đặt thông báo/Đổi mật khẩu rõ ràng. Yêu cầu sửa hồ sơ liên kết PROFILE của học viên.
- Đăng xuất xác nhận rõ phạm vi **tất cả thiết bị**, giữ xử lý khi chưa xác nhận được thu hồi phiên từ backend.
- Cài đặt thể hiện hộp thông báo luôn hoạt động; tùy chọn push có lưu/bỏ thay đổi, chỉ xin quyền thiết bị khi người dùng thao tác. Switch và nhãn không tràn màn nhỏ.
- Trang liên hệ gọi đúng chi nhánh; giữ truy cập hỗ trợ khôi phục tài khoản khi chưa đăng nhập. Tránh lồng hai bộ xử lý bàn phím trong màn đổi mật khẩu.

## Cấu trúc triển khai

| Thành phần | Vai trò |
|---|---|
| `src/components/Details.tsx` | Badge, thông tin nhãn/giá trị, mở rộng chi tiết, phân trang |
| `src/features/learning/ClassContext.tsx` | Tên lớp trong điểm, chuyên cần, tài liệu và học phí theo lớp |
| `src/features/support/context.ts` | Kiểm tra, hiển thị và ánh xạ tài nguyên hỗ trợ |
| `src/features/notifications/useNoticeOpen.ts` | Mở thông báo và ghi đã đọc không chặn điều hướng |
| `src/features/online/Checkout.tsx`, `flow.ts` | Chọn/xác nhận tiền; giữ command/recovery; diễn giải trạng thái |
| `src/components/SessionGate.tsx`, `learning/utils.ts` | Giữ liên kết nội bộ hợp lệ qua bước đăng nhập |

Mọi màn tiếp tục dùng các API `student/me` hiện có. Backend kiểm tra ownership, trạng thái và version; UI không thay thế các kiểm tra này.

## Kiểm tra và bàn giao

Kiểm tra quy tắc bằng toàn bộ `tests/*.test.cjs`, TypeScript, bundle Expo web và Android/Hermes. Browser acceptance dùng Chrome headless với dữ liệu tổng hợp và HTTP server loopback riêng, không kết nối database hoặc gửi thanh toán tới VNPAY thật.

Script `scripts/student-app/verify-mobile-ux.cjs` nhận đường dẫn bundle web; truyền thêm `scripts/student-app/verify-mobile-m456.cjs` để chạy các tình huống M4–M6. Bundle kiểm thử dùng `http://127.0.0.1:18765/api/v1`, nằm trong `.expo/m456-web`/`.expo/m456-android`; không dùng bundle này làm bản phát hành cho điện thoại.

Chưa thay thế nghiệm thu APK thật: cần kiểm tra bàn phím, nút Back, TalkBack, cỡ chữ lớn, tải/chia sẻ tài liệu, push có Firebase/EAS và quay về app sau VNPAY. Chưa tạo APK mới trong M4–M6. M7 tối ưu hiệu năng/cache và M8 nghiệm thu/phát hành là phần tiếp theo.

### Kết quả đã chạy

| Kiểm tra | Kết quả |
|---|---|
| TypeScript | Đạt |
| Unit/regression tests của app | 92/92 đạt |
| Browser acceptance M4–M6 | 35/35 đạt, không có runtime exception |
| Browser regression M1–M3 | 23/23 đạt |
| Export web | Đạt, 1.047 module |
| Export Android/Hermes | Đạt, 1.375 module |
| `git diff --check` của mobile | Đạt |
| Thiết bị native, Firebase/push thật, VNPAY qua app thật | Chưa nghiệm thu trong đợt này |

Các tình huống bao gồm điểm 0/chưa chấm; chưa điểm danh; giữ bộ lọc lớp/tài liệu; xác nhận số tiền và chặn tiền vượt số dư; tạo một yêu cầu thanh toán và chờ backend xác nhận trước khi mở phiếu thu; giữ UUID yêu cầu đối soát qua đăng nhập; lỗi ghi đã đọc không chặn mở nội dung; gửi/phản hồi/đóng yêu cầu; hồ sơ thu gọn và đăng xuất cần xác nhận. Máy chủ giả lập chỉ tồn tại trong tiến trình kiểm thử, không nằm trong bundle nghiệp vụ.

Kết quả chi tiết: [M4–M6](validation/m456/result.json), [hồi quy M1–M3](validation/m456/regression-m123.json).

Ảnh dùng dữ liệu tổng hợp để review bố cục: [tổng quan lớp](validation/m456/class-hub-360.png), [điểm](validation/m456/grades-360.png), [chuyên cần](validation/m456/attendance-360.png), [xác nhận thanh toán](validation/m456/checkout-360.png), [phiếu thu](validation/m456/receipt-360.png), [lịch sử thu](validation/m456/finance-history-360.png), [thông báo](validation/m456/notifications-360.png), [cá nhân](validation/m456/profile-360.png), [hỗ trợ 320px](validation/m456/support-320.png).

Để xem mã mới: reload Metro đang chạy hoặc chạy `npm run start:go -- --port 8085` từ thư mục mobile. APK cài trước đây cần build/cài lại; chưa có APK M4–M6 mới được tạo.
