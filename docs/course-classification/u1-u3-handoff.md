# Website U1–U3 — Font, điều hướng và trang chủ

Cập nhật **20/11/2026**. Phạm vi: `language-center-website`; giữ nguồn catalog CMS, logo và màu xanh hiện tại.

## Những phần đã thay đổi

| Giai đoạn | Kết quả |
|---|---|
| U1 | Nạp Be Vietnam Pro local bằng `next/font/local`, với bốn weight thật 400/500/600/700 và giấy phép OFL đi kèm. Bỏ font serif/italic xen kẽ, chuẩn hóa label 14px/500, nội dung input 16px/400 và placeholder. Tạo token màu, khoảng cách, heading, container và control. |
| U2 | Gỡ các luật header cũ khỏi globals, gom về `header.css`. Header desktop khoảng 126px, mobile 73px; menu có SVG chevron, vùng bấm tối thiểu 44px. Giữ active filter, Escape, focus và click ngoài. Đo chiều cao header bằng ResizeObserver để tính offset sticky/anchor. |
| U3 | Bỏ xuống dòng cưỡng ép của H1, giảm còn 60px desktop / 34px mobile nhỏ. Cân ảnh–chữ, đưa CTA lên sớm, giảm caption và chữ trang trí. Thu gọn thẻ chương trình, giữ nội dung CMS và ghi chú ảnh minh họa. |

Font dùng file TTF chính thức, không tải Google Fonts khi mở trang; bốn file khoảng 546KB trước nén truyền tải. Giấy phép và nguồn tác giả nằm tại `src/app/fonts/OFL.txt`. Chưa làm bộ font subset WOFF2 riêng.

## Kiểm chứng

- Production build và TypeScript đạt.
- ESLint các file vừa thay đổi đạt. Lint toàn website còn **10 lỗi, 1 cảnh báo** ở `consultation-form.tsx` và `course-catalog.tsx` có từ trước; không xem đây là lint toàn dự án đã đạt. Xử lý cùng U6/U4 tương ứng.
- **55 kiểm tra UI**: 12 chiều rộng 320/390/600/720/768/800/1024/1100/1101/1280/1366/1440px; laptop 1366×768; font tiếng Việt và ký hiệu ₫ ở bốn weight; menu desktop/mobile; Escape/focus/click ngoài; active filter; sticky học phí; chữ trường nhập liệu.
- **38 kiểm tra hồi quy** với API fixture riêng: catalog, bộ lọc, ngữ cảnh khóa/lớp/chi nhánh → tư vấn, bắt buộc đồng ý, retry không tạo trùng, xử lý API lỗi và màn hình 320/390px. Không ghi MySQL dự án.
- Không ghi nhận tràn ngang trong các trường hợp trên. Chrome mô phỏng không thay thế kiểm thử điện thoại thật, trình duyệt khác và zoom thực tế ở U7.

Ảnh sau sửa và kết quả được lưu cùng tài liệu này trong `website-ui-after/`, `website-ui-u123-result.json` và `website-ui-u123-regression.json`. Ảnh trước sửa vẫn ở `website-ui-before/`.

## Cách chạy và kiểm tra lại

Trong thư mục website:

```powershell
npm run dev
```

Mở `http://localhost:3000`. Backend academic/gateway cần chạy để hiện dữ liệu đã xuất bản.

Đã sửa lỗi tương tác khi dùng `http://127.0.0.1:3000`: Next dev chặn request HMR do hostname khác mặc định. `next.config.ts` bổ sung chính xác `allowedDevOrigins: ["127.0.0.1"]`; không mở wildcard. Cả bản production kiểm thử, phiên dev riêng và server dev cổng 3000 đều đạt bộ kiểm tra UI. Server 3000 đã được khởi động lại; các phiên kiểm thử riêng đã dừng. Cache dev cũ được giữ tại `.next/dev-before-u123-20261120-094618/`, không xóa source hoặc dữ liệu.

Trong thư mục backend, kiểm tra chỉ đọc trang:

```powershell
node scripts/website/verify-ui-u123.cjs
```

Kiểm tra build riêng, tránh ghi đè cache của phiên dev đang chạy:

```powershell
# Chạy trong thư mục website
$env:WEBSITE_BUILD_DIR = '.next-ui-check'
npm run build
Remove-Item Env:WEBSITE_BUILD_DIR
```

`.next-ui-check/` đã được loại khỏi Git và ESLint. Next có thể bổ sung đường dẫn type tương ứng vào tsconfig khi chạy build riêng; đây là file sinh tự động. Không đưa cache/build vào bản nộp source.

## Phần tiếp theo

**U4**: dàn lại danh sách, bộ lọc và thẻ khóa/giáo viên/chi nhánh; **U5**: bố cục chi tiết; **U6**: dàn lại form và các trạng thái; **U7**: nghiệm thu toàn website. Font/label/input dùng chung đã được áp dụng, nhưng chưa coi các bố cục U4–U6 là hoàn thành.

Không đổi dữ liệu, phân loại khóa, quyền truy cập hoặc cấu hình VNPAY trong U1–U3.
