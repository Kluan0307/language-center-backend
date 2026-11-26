# Website U4–U6 — Danh sách, chi tiết và tư vấn

Hoàn thành **20/11/2026**, tiếp nối U1–U3. Phạm vi: website công khai `language-center-website`.

## Kết quả

| Giai đoạn | Đã thực hiện |
|---|---|
| U4 | Thu gọn tiêu đề còn tối đa 44px desktop/30px mobile; bỏ xuống dòng cưỡng ép. Sắp lại bộ lọc, nhóm bộ lọc phụ có thể thu gọn trên mobile và tự mở khi đang áp dụng. Căn thẻ khóa theo hàng giá/CTA; mô tả ngắn gọn, tên dài xuống dòng, thiếu ảnh không tạo khoảng trống. Đồng bộ danh sách chương trình, giáo viên, chi nhánh. |
| U5 | Cân cột nội dung/sidebar, các block đầu vào–mục tiêu–nội dung, học phí và lịch lớp. Thống nhất lịch/trạng thái ở cỡ chữ dễ đọc. Giữ khoảng cách dưới header khi sticky/đến anchor; thanh tư vấn mobile có safe area và khoảng đệm cuối trang. Áp dụng cho chi tiết khóa/chương trình/chi nhánh/giáo viên. |
| U6 | Đưa form lên sớm, tách nhóm liên hệ và nhu cầu học. Input/select 16px, label 14px; phân biệt trường bắt buộc/tùy chọn và nội dung trợ giúp. Giữ thông tin sau lỗi, thông báo đang gửi/thành công/lỗi, consent và chống gửi trùng. Đồng bộ breadcrumbs, footer, nội dung giới thiệu/chính sách và các trạng thái rỗng/lỗi. |

Các luật giao diện đã thay được gỡ khỏi `globals.css`; `pages.css` sở hữu bố cục các trang. Không chồng thêm một khối override cạnh tranh với luật cũ.

## Các điểm logic được sửa cùng giao diện

- Danh sách khóa/lớp phụ thuộc được gắn với query cụ thể và hủy request cũ khi đổi lựa chọn. Kết quả tải cũ không ghi đè lựa chọn mới; bỏ cách cập nhật state đồng bộ ngay trong effect.
- Đổi chi nhánh/chương trình sẽ bỏ khóa/lớp không còn phù hợp, giữ thông tin liên hệ đã nhập.
- Khi không tải được khóa/lớp, khách vẫn có thể gửi tư vấn theo chương trình/khóa đã xác định; có nút tải lại và giữ thông tin nhập.
- Giữ ngữ cảnh chi nhánh/đối tượng khi phân trang lớp; giữ chi nhánh khi mở khóa từ trang chi tiết chi nhánh.
- Danh sách chương trình đang lọc một mục không còn hiển thị phân trang của danh sách toàn bộ.
- Tách I/O khỏi JSX trong `CourseCatalog`; cùng thay đổi form đã giải quyết **10 lỗi lint và 1 cảnh báo** còn từ U1–U3.
- Giữ HTTP **404** cho hồ sơ/chi nhánh không công khai. Không dùng `loading.tsx` ở root vì việc stream sớm có thể gửi HTTP 200 trước khi phát hiện trang không tồn tại. Thông báo chuyển trang và trạng thái tải danh sách trong form vẫn hoạt động.

## Kiểm chứng

- Production build đạt, TypeScript `tsc --noEmit` đạt.
- **ESLint toàn website đạt**.
- **207 kiểm tra Chrome/API fixture đạt**: 38 kiểm tra hồi quy cùng 169 kiểm tra U4–U6.
- **77 tổ hợp**: 11 trang × 7 chiều rộng 320/390/768/1024/1280/1366/1440px, có laptop 1366×768. Bao gồm bốn loại chi tiết, danh sách, form, giới thiệu và chính sách.
- Kiểm tra riêng: bộ lọc mobile thu/mở và giữ giá trị, tên khóa dài vẫn căn hàng học phí, lớp phân trang giữ ngữ cảnh, anchor/sticky không bị che, CTA mobile không che footer, đổi nhánh giữ liên hệ, tải tùy chọn lỗi/tải lại.
- Luồng gửi fixture: thiếu consent không gửi; upstream đã nhận nhưng response lỗi thì lần gửi lại dùng cùng requestId; **2 lần gửi tạo 1 kết quả duy nhất**. Không ghi MySQL dự án.
- Không ghi nhận tràn ngang ở các trường hợp đã kiểm tra; không có exception chưa xử lý trên trình duyệt.

Kết quả: [website-ui-u456-result.json](website-ui-u456-result.json), [số đo từng trang](website-ui-u456-pages.json). Ảnh mới: `website-ui-u456-after/`. Các ảnh dùng dữ liệu **fixture cô lập**, không phải dữ liệu MySQL thật. Ảnh trước các thay đổi nằm trong `website-ui-before/` và `website-ui-after/` của U1–U3.

## Cách chạy

Trong thư mục website:

```powershell
npm run dev
```

Mở `http://localhost:3000`. Trong lúc nghiệm thu, API Gateway cổng 8080 không chạy. Cần chạy **academic-service và api-gateway** để xem catalog thật; website sẽ báo chưa tải được nếu backend chưa sẵn sàng. Không cấu hình dữ liệu fixture làm nguồn dự phòng cho ứng dụng.

Kiểm tra lại bộ nghiệm thu, sau khi build thư mục riêng:

```powershell
# Trong thư mục website
$env:WEBSITE_BUILD_DIR = '.next-ui-check'
npm run build
Remove-Item Env:WEBSITE_BUILD_DIR

# Trong thư mục backend
$env:WEBSITE_BUILD_DIR = '.next-ui-check'
node scripts/website/verify-l67.cjs --ui-u456
Remove-Item Env:WEBSITE_BUILD_DIR
```

Script mở API fixture/website/Chrome riêng, dừng tiến trình kiểm thử khi xong. Source ảnh và báo cáo trung gian nằm trong `tmp/website-l67/`.

## Còn lại U7

Nghiệm thu lại toàn luồng với backend/MySQL bản sao khi service sẵn sàng; kiểm tra zoom 200%, bàn phím toàn trang và trình duyệt/thiết bị thật. Các kiểm tra Chrome mô phỏng ở trên không thay thế những phần này. Chưa deploy.
