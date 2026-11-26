# Website Language Center — lộ trình đồng bộ hệ thống

Website Next.js trong language-center-website, phát triển local; chưa triển khai hosting.
Logo từ mobile app; màu xanh #2563EB đồng bộ web quản lý. Không sử dụng tài khoản quản trị trong website công khai.

Kế hoạch này thay thế cách đánh số L4–L5 trong bản khởi tạo. Theo yêu cầu cập nhật mới, form tư vấn thuộc L6, hosting thuộc L10.

| Giai đoạn | Công việc | Trạng thái |
|---|---|---|
| L1–L3 | Nền Next.js, trang chủ responsive, khung trang chương trình/chi nhánh/giới thiệu/tư vấn | Đã triển khai |
| L4 | Metadata liên kết nghiệp vụ; nháp/công khai, slug, đối tượng, đầu ra, ảnh, liên hệ và quy tắc học phí | Đã triển khai; không tự xuất bản dữ liệu hiện có |
| L5 | Public catalog API, phân trang/lọc, nguồn thật, tải/lỗi/404, sitemap động | Đã triển khai; hướng dẫn tại L4-L5 |
| L6 | Form tư vấn → lead, đồng ý liên hệ, chống spam/trùng, phân chi nhánh | Đã triển khai; hướng dẫn tại website-l6-l7.md |
| L7 | Giao diện tuyển sinh, ảnh đã duyệt, tìm chương trình/khóa/chi nhánh, hồ sơ giáo viên công khai | Đã triển khai; chưa tự xuất bản nội dung hiện có |
| L8 | Màn hình nội dung, xem trước/xuất bản, quyền biên tập theo chi nhánh, lịch sử | Đã triển khai |
| L9 | Kiểm thử website → tư vấn → học viên, nhiều chi nhánh/quyền, đồng bộ mobile | 106 test backend + 35 Chrome đạt; nghiệm thu MySQL/APK còn mở |
| L10 | Domain HTTPS, backend ổn định, SEO/index, giám sát, backup và vận hành | Đã chuẩn bị gói triển khai; 37 kiểm tra standalone đạt; hosting/domain/TLS thật và vận hành còn mở |

## Nguồn dữ liệu

- Chương trình: programs; khóa: courses; lớp: school_classes; chi nhánh: branches.
- website_publications chỉ lưu thông tin biên tập và quyền công khai; tên, giá, địa chỉ, thời lượng, lịch đọc từ nguồn gốc.
- Thông tin học viên, tài khoản, điểm, công nợ, phiếu thu và nhân sự nội bộ không nằm trong API website.
- Nội dung giới thiệu cứng ban đầu đã thay bằng catalog backend. Chưa xuất bản không hiển thị.
- Hotline/email toàn hệ thống vẫn từ biến cấu hình website; đồng bộ company_profile và quyền công khai có thể bổ sung L8.

## Vận hành

Xem tài liệu L4–L5 đi kèm về cấu hình, API và xuất bản. Khởi động lại academic-service, chạy gateway và website. Không cần reset database.
Thông tin marketing, ảnh và liên hệ phải được xác nhận trước khi công khai. Không tự đưa dữ liệu demo thành thông tin kinh doanh thật. Ảnh nhóm học hiện là ảnh minh họa Unsplash.
