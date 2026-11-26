# U7 — Nghiệm thu website local

Cập nhật **20/11/2026**. Phạm vi: đợt chuẩn hóa giao diện U1–U7 của website công khai. Kết quả số lượng và trạng thái cuối cùng được ghi tại [u7-acceptance.json](u7-acceptance.json).

## Công việc hoàn tất

- Kiểm tra catalog và tư vấn với **auth, academic, teaching, finance và API Gateway thật**, kết nối tới bản sao riêng của `language_center_db`.
- Đi hết luồng: chọn đối tượng/chương trình/chi nhánh → khóa → lớp → điền tư vấn → BFF → API → lead → nhân sự đúng chi nhánh đọc lại. Kiểm tra mismatch lớp–chi nhánh bị từ chối, khóa đóng/lớp đầy/chi nhánh ngừng hoạt động không còn công khai.
- Nghiệm thu trên Chrome và Edge với **12 trang × 8 chiều rộng = 96 tổ hợp mỗi trình duyệt**: 320/360/390/768/1024/1280/1366/1440px, bao gồm laptop 1366×768.
- Kiểm tra chữ/control, font tiếng Việt thực tế, nội dung dài, thiếu ảnh, không tràn ngang, menu bằng bàn phím, skip link, Escape trả focus, thứ tự tab trong form, required validation và reduced motion.
- Kiểm tra zoom **200% thật** bằng cài đặt page zoom của hồ sơ trình duyệt kiểm thử. Có thêm phép mô phỏng viewport/DPR để đối chiếu; hai loại kết quả được ghi riêng trong báo cáo chi tiết.
- Sửa lỗi focus form trên màn hình nhỏ: ô đang nhập được đưa vào vùng nhìn thấy dưới header; cập nhật khi `visualViewport` thay đổi. Tránh ô bị cắt khi tab hoặc khi vùng hiển thị thu nhỏ.
- Giữ cỡ chữ/control của U4–U6. Đã chạy lại hồi quy gửi form, consent, xử lý lỗi và requestId chống trùng sau sửa focus.
- Build production, TypeScript và ESLint toàn website đạt.

## Bằng chứng

Các báo cáo cuối cùng:

- [Chrome + service/MySQL thật](u7-real-chrome-validation.json).
- [Edge + service/MySQL thật](u7-real-edge-validation.json).
- [Tổng hợp và xác nhận dọn database tạm](u7-acceptance.json).
- Chi tiết đo giao diện/zoom và ảnh: `website-ui-u7-after/chrome/`, `website-ui-u7-after/edge/`.

Hash dump nguồn trước/sau phải giống nhau; các lần nghiệm thu chỉ ghi vào database bản sao. Script dừng service/trình duyệt kiểm thử và xóa database tạm khi kết thúc. Không thay mật khẩu hoặc seed lại dữ liệu nguồn.

Zoom được đặt trong hồ sơ trình duyệt tạo riêng, sau đó khôi phục giá trị ban đầu; không dùng hồ sơ Chrome/Edge cá nhân. Kiểm tra xác nhận zoom=2, DPR=2 và viewport CSS thực tế. Cách gọi setting được đối chiếu với [API settingsPrivate trong mã nguồn Chromium](https://chromium.googlesource.com/chromium/src/+/9080a481ba62a92f72b4a408040634e5e8dad728/chrome/common/extensions/api/settings_private.idl).

## Cách chạy lại

Build trong thư mục website:

```powershell
$env:WEBSITE_BUILD_DIR = '.next-ui-check'
npm run build
Remove-Item Env:WEBSITE_BUILD_DIR
```

Trong thư mục backend:

```powershell
$env:WEBSITE_BUILD_DIR = '.next-ui-check'
node scripts/website/verify-refresh.cjs --ui-u7
$env:UI_BROWSER = 'edge'
node scripts/website/verify-refresh.cjs --ui-u7
Remove-Item Env:UI_BROWSER
Remove-Item Env:WEBSITE_BUILD_DIR
```

Yêu cầu MySQL local đang chạy và đã có JAR của 5 service. Các cổng kiểm thử 31180–31184, 31200, 31227 phải trống. Script đọc cấu hình DB từ `.env.local`, không in secret. Bộ kiểm tra gắn với dữ liệu mẫu D1–D8 hiện tại; nếu chủ động đổi bộ dữ liệu, cần cập nhật kỳ vọng theo nghiệp vụ trước khi chạy lại.

Để chạy website bình thường, dùng `npm run dev` trong thư mục website và chạy backend local cần thiết để xem dữ liệu thật.

## Giới hạn còn cần kiểm tra khi có thiết bị

Chưa kiểm thử điện thoại thật và bàn phím ảo của iOS/Android; việc thu nhỏ viewport chỉ kiểm chứng phần xử lý resize. Chrome và Edge cùng dùng Chromium; Safari/Firefox chưa được kiểm thử. Đây là các mục kiểm tra bổ sung trước khi phát hành rộng rãi. Phạm vi local tự động của U7 đã có đầy đủ kết quả trong báo cáo tổng hợp.
