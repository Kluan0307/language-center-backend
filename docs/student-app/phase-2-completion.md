# Student App — giai đoạn 2

Ngày hoàn thiện:11/11/2026. Đã hoàn thành nền tảng source và kiểm chứng local; kiểm tra trên thiết bị Android thật còn chờ thiết bị. Chưa có nghiệp vụ STUDENT chạy thật, chưa thay backend/database hoặc release web.

## Đã triển khai

- Expo Router với stack và 5 tab: Trang chủ/Lịch học/Học tập/Học phí/Cá nhân; thêm welcome, hỗ trợ, thông báo, cài đặt, kích hoạt và404.
- Thành phần native Screen/Heading/Card/Button/Row/StateView; loading/empty/error/retry; safe area, vùng chạm, thanh tab có khoảng dưới theo thiết bị. Màu primary #2563EB, chữ/border theo theme web hiện hành.
- Source chia app/components/config/theme/lib/providers/types/tests; token UI tập trung để cập nhật sau. Chưa thêm thư viện UI nặng hoặc Ant Design DOM.
- API client kiểm cấu hình Gateway, bearer, JSON,204, timeout, abort, lỗi HTTP, mã lỗi an toàn. Không retry mutation tự động, chặn URL ngoài Gateway/traversal, không in token/server stack.
- SecureStore cho native; browser không lưu token vào localStorage. Khôi phục phiên qua /auth/me, chỉ nhận STUDENT hợp lệ, chặn phiên bắt đổi mật khẩu,401 dọn phiên;403 không tự đăng xuất. Login/save-token và reset được làm G3, không giả tài khoản học viên.
- env.example và README chạy/config/giới hạn. Địa chỉ server không mặc định localhost và không có secret; dependency cài theo expo install tương thích SDK58.

## Xem giao diện

Tại thư mục language-center-student-app chạy npm start; Expo Go tương thích hoặc development build. Màn hình welcome có **Khám phá giao diện** chỉ khi __DEV__. Bấm để xem5tab; có nhãn phát triển và không chứa dữ liệu học viên mẫu. Production không có nút preview và chưa đăng nhập được trước G3.

[Ảnh Trang chủ ở viewport390x844](figures/phase2-home.png). Đây là ảnh browser mobile viewport, không phải bằng chứng native device.

## Kiểm chứng

| Kiểm tra | Kết quả |
|---|---|
| npm run typecheck | PASS |
| npm test | 13/13; config, DTO identity, bearer, public request, ownership role boundary ở DTO,401/403,204, malformed JSON, network, timeout, cancellation, không retry mutation |
| npx expo install --check | Dependencies up to date |
| expo export --platform android | PASS; Hermes bundle; không phải APK cài thiết bị |
| Browser ở390x844 | 9/9, không exception runtime: welcome/preview/5tab/home-to-calendar/learning/tuition/notification/profile-exit/direct-link guard |
| Android device | adb devices không có thiết bị; chưa xác minh native back/keyboard/SecureStore runtime |
| Expo Doctor latest1.21.7 | Chưa chạy được do package thiếu @expo/env; thử dependency resolution còn lỗi boolish. Không ghi PASS, không thay code app để che lỗi công cụ |

Bằng chứng local: storage/student-phase2-ui.json, storage/student-phase2-home.png; build log trong app phase2-android-build.log (ignored). Script browser tái chạy từ backend: scripts/student-app/phase2-browser-check.cjs; cần Expo dev server18090 và Chromium/Edge debug19389 với profile riêng. Script không tự mở browser hay thay dữ liệu; report ở storage. Chỉ test preview, không test API business thật.

## Việc còn lại / chuyển sang G3

Npm đã báo5moderate khi cài; giữ lockfile và xem xét audit riêng, không force upgrade làm lệch SDK. Không có tuyên bố đã đủ production. Các màn hình nghiệp vụ đang hiển thị chưa kết nối; không dùng badge0/số tiền0/điểm0 để giả dữ liệu.

G3: role STUDENT tại mọi service, liên kết Academic Student với Auth Account có audit/unique/khôi phục cấp account, kiểm scope và session, web cấp/khóa/reset, app đăng nhập/đổi mật khẩu/hồ sơ. Khi có API phải thay placeholder bằng luồng thật và thêm test session race/account isolation. Gate thiết bị thật phải hoàn tất trước bàn giao cuối; khi nối thiết bị kiểm native tabs/back/safearea/fontscale/keyboard/persist401/logout.
