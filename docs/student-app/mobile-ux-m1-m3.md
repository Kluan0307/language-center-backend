# Tối ưu ứng dụng học viên — M1, M2, M3

Ngày: 15/11/2026. Phạm vi: React Native / Expo trong `language-center-student-app`.

## M1 — Luồng, dữ liệu và bố cục

Giữ năm tab: Trang chủ, Lịch học, Học tập, Học phí, Cá nhân. Chuông thông báo trên header, tiêu đề theo tab. Không thêm trang quản trị vào app học viên. Giữ toàn bộ đường dẫn stack hiện có để notification và payment return tiếp tục hoạt động.

| Nhu cầu | Luồng đã chốt | API hiện có | Trạng thái phải xử lý |
|---|---|---|---|
| Biết buổi tiếp theo | Trang chủ -> card buổi -> chi tiết | Academic `/student/me/next-session`, `/sessions/{id}` | Loading, chưa có lịch, lỗi riêng, lịch cũ cần xác minh |
| Biết thay đổi lịch | Trang chủ -> thông báo -> nội dung liên quan | Academic `/student/me/notifications?unread=true&category=SCHEDULE` | Không có thay đổi thì ẩn; lỗi không ngăn xem lịch; tối đa hai preview |
| Xem lịch cá nhân | Lịch học -> ngày / tuần danh sách / lưới giờ -> chi tiết | Academic `/student/me/schedule?from&to&enrollmentId` | Phân trang đầy đủ, hủy/bù/đổi giáo viên, giờ thiếu, lớp không còn truy cập |
| Lọc lớp | Lịch học -> bảng chọn lớp -> áp dụng ngay | Academic `/student/me/enrollments?group=ALL` | Loading, error/retry, không có lớp, bỏ lọc |
| Kiểm tra học phí | Trang chủ -> Học phí -> luồng hiện có | Finance `/student/me/tuitions/summary` | Chưa xác minh khác với số dư bằng 0; lỗi không suy thành đã đóng |
| Xem học tập | Trang chủ hoặc tab Học tập -> lớp | Luồng hiện có | Điểm/chuyên cần/tài liệu tiếp tục theo đúng lớp; tối ưu chi tiết ở M4 |

Mọi API đi qua Gateway `/api/v1`; URL trong source mobile giữ prefix `/academic` hoặc `/finance`. Backend kiểm tra ownership, không lấy account/student ID từ UI để truy cập người khác.

### Bố cục đã chốt

Trang chủ: lời chào và ngày -> buổi tiếp theo (hành động chính) -> thay đổi lịch cần chú ý -> học phí cần đóng/xác minh khi có -> liên kết lớp học. Bỏ request chỉ để đếm đăng ký và nhóm ba liên kết lặp tab. Thông báo chưa đọc vẫn có badge header. Kéo để cập nhật thay cho nút cập nhật toàn trang.

Lịch: chuyển tuần -> thanh ngày -> Ngày / Tuần / Lưới giờ -> lọc lớp + Hôm nay -> nội dung. Mặc định Ngày với ngày hiện tại; Tuần là danh sách nhóm theo ngày; Lưới giờ giữ tuần time-grid đã có. Chọn ngày trong Tuần chuyển sang Ngày; chọn ngày trong Lưới giờ giữ chế độ lưới và focus ngày đó. Chuyển tuần giữ thứ đang chọn; Hôm nay giữ bộ lọc và chế độ. Lịch sử và deep link vẫn mở đúng ngày/lớp.

Chi tiết buổi: tên lớp -> cảnh báo hủy nếu có -> ngày/giờ/trạng thái -> phòng, giáo viên thực tế, chi nhánh -> ghi chú đổi giáo viên/xác minh -> xem lớp hoặc xem lịch ngày đó.

### Quy tắc dữ liệu

- Ngày nghiệp vụ theo `Asia/Ho_Chi_Minh`; phép tính tuần theo lịch Thứ Hai–Chủ Nhật, không theo timezone thiết bị.
- Không suy ra nghỉ/vắng từ việc chưa có dữ liệu; không bỏ buổi đã hủy khỏi danh sách.
- Nhóm lịch sắp theo ngày rồi giờ, buổi chưa có giờ ở cuối ngày. Không sửa mảng dữ liệu API khi sắp xếp.
- `coverageComplete=false`, số dư null hoặc không hợp lệ luôn là cần xác minh; không hiển thị là không nợ. Decimal string được giữ nguyên; dùng currency từ API.
- Mỗi module lỗi có hành động riêng. Không đổi cơ chế VNPAY/IPN/receipt, session, ownership, push opt-in hoặc publication điểm.
- Lịch bù và giáo viên thay thế chỉ hiển thị theo dữ liệu backend; không tự suy ra đổi phòng khi chưa có trường xác nhận.

## M2 — Thành phần giao diện chung

- Tokens xanh/trắng hiện có; bổ sung màu nền trạng thái và vùng chạm chuẩn 48dp.
- `Screen`: safe area, bàn phím iOS, kéo để cập nhật tùy chọn; giữ tương thích các màn cũ.
- `SectionTitle`, `IconButton`, `SegmentedControl`, `InlineNotice`: hành động và trạng thái thống nhất, nhãn accessibility.
- `BottomSheet`: đóng bằng nền, nút hoặc Android back; safe area đáy; nội dung dài cuộn được.
- `Choice` và finance `Segments` tái sử dụng bộ thành phần; không thay đổi nghiệp vụ tài chính.
- Header đúng tên tab, badge thông báo có nhãn số chưa đọc. Giữ năm tab và tất cả route hiện có.

## M3 — Trang chủ và lịch

- Trang chủ ưu tiên next-session, card chạm trực tiếp, lỗi độc lập và chỉ hiển thị học phí cần xử lý.
- Lịch có thanh ngày cuộn được trên màn hẹp, chuyển tuần có nhãn, nhóm danh sách theo ngày, bộ lọc lớp trong sheet.
- `SessionCard` có trạng thái bằng chữ/màu, phòng và giáo viên thực tế, ghi chú học bù/hủy/đổi giáo viên.
- Chi tiết buổi có bố cục ngắn, đường trở về lịch ngày đang xem và kéo để cập nhật.
- Hook truy vấn chỉ bổ sung trạng thái fetching cho refresh indicator. Không thêm cache lâu dài hoặc đổi chiến lược refresh tài chính ở M1–M3.

## Nghiệm thu và phần tiếp theo

- Typecheck; toàn bộ test hiện có; test quy tắc nhóm lịch, biên năm, timezone và số dư chưa xác minh.
- Export bundle Android/web để kiểm tra tích hợp module; kiểm tra giao diện trình duyệt nếu môi trường hỗ trợ.
- Nghiệm thu APK: điện thoại nhỏ, cỡ chữ lớn, TalkBack, Android back/modal, kéo refresh, mạng chậm/lỗi một service, chọn lớp và chuyển tuần, lịch hủy/bù, đổi tài khoản. Lưới giờ cuộn ngang có chủ đích; buổi quá ngắn vẫn có chế độ danh sách để truy cập thuận tiện.
- Chưa mặc nhiên tuyên bố đã nghiệm thu native hoặc tạo APK mới khi chỉ qua typecheck/test/export.
- M4 học tập; M5 tài chính; M6 thông báo/hỗ trợ/cá nhân; M7 hiệu năng/cache; M8 nghiệm thu và APK.

Tham khảo thiết kế: [VUS](https://student.vus.edu.vn/quy-dinh-khoa-hoc.html), [ILA Smart Learning](https://play.google.com/store/apps/details?hl=vi&id=com.ila.lms.prod), [Canvas Student](https://community.instructure.com/en/en/kb/articles/661624-how-do-i-use-the-canvas-app-on-my-ios-device), [Android accessibility](https://developer.android.com/guide/topics/ui/accessibility/apps). Đây là cơ sở tham khảo luồng; không sao chép nội dung/bài tập hoặc mở rộng thành LMS trong M1–M3.

## Kết quả triển khai M1–M3

| Kiểm tra | Kết quả |
|---|---|
| TypeScript `npm run typecheck` | Đạt |
| Toàn bộ `tests/*.test.cjs` | 86/86 đạt, gồm 5 test mới về quy tắc trình bày lịch/công nợ |
| Expo export web | Đạt, 1.044 module |
| Expo export Android/Hermes | Đạt, 1.372 module |
| Browser UI ở 320px và 360px | 23 tình huống đạt, không có runtime exception |
| Native trên điện thoại / APK mới | Chưa nghiệm thu / chưa tạo APK mới |

Đã phát hiện và sửa nhãn thanh tab bị cắt chữ ở màn hẹp: chiều cao thanh tab tính thêm cỡ chữ hệ thống, nhãn có line-height và không bị co chiều cao. Bộ lọc radio và chế độ xem có cả trạng thái accessibility native và ARIA cho bản web. Bộ chọn ngày và lưới tự đưa ngày được chọn vào vùng nhìn thấy. Tránh lồng hai bộ xử lý bàn phím trên màn đăng nhập.

Browser UI dùng HTTP server cục bộ và dữ liệu tổng hợp riêng, không kết nối backend thật, không chèn dữ liệu hoặc tạo giao dịch. Script: `scripts/student-app/verify-mobile-ux.cjs`. Script nhận đường dẫn bundle web đã export với API kiểm thử `http://127.0.0.1:18765/api/v1`, mở Chrome headless với profile riêng và đóng sau kiểm tra. Bundle kiểm thử nằm trong `.expo/m123-web` và `.expo/m123-android` của app; không dùng những bundle có API kiểm thử này để phát hành APK. Không sửa `.env.local`.

Kết quả: [23 tình huống](validation/m123/result.json). Ảnh minh họa từ dữ liệu kiểm thử: [Trang chủ 360px](validation/m123/home-360.png), [Lịch 360px](validation/m123/schedule-360.png), [Lịch 320px](validation/m123/schedule-320.png), [Bộ lọc lớp](validation/m123/class-filter-360.png).

Để xem thay đổi trong app phát triển, chạy Metro từ thư mục mobile bằng `npm run start:go -- --port 8085`, hoặc reload phiên Metro đang chạy. APK đã cài trước đó cần build và cài lại. Chưa thay đổi API, cơ sở dữ liệu hoặc cấu hình VNPAY.
