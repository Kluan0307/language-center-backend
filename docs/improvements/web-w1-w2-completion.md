# Bàn giao W1–W2 — Web quản lý

Ngày: 13/11/2026. Thuộc [lộ trình nâng cấp web](web-management-upgrade-roadmap-2026-11-13.md). Đã triển khai W1–W2; chưa thực hiện W3–W8.

## W1 — Đã chốt

[Thiết kế và hợp đồng](web-w1-design-and-contracts.md) gồm bản đồ luồng làm việc, inventory route/quyền lấy từ NAVIGATION hiện tại, API liên quan, quy tắc phạm vi chi nhánh, phân biệt đơn thanh toán/phiếu thu/công nợ, token và bốn wireframe. Các thiếu hụt hợp đồng được chuyển rõ sang W3–W7, không chèn dữ liệu giả vào app để lấp cột.

## W2 — Đã code

Source nằm trong `language-center-web-frontend`:

| Thành phần | Thay đổi |
|---|---|
| `src/components/Common/theme.ts`, `src/App.tsx` | ConfigProvider dùng managementTheme tập trung, dùng lại theme cho styled-components; thống nhất màu, bảng, control và card |
| `src/components/Layout/ManagementLayout/PageHeader.tsx` | Tách header dùng chung, giữ tiêu đề/mô tả/hành động, wrap trên màn hẹp |
| `src/components/Layout/ManagementLayout/index.tsx` | Thêm scopeLabel tùy chọn, phân biệt phạm vi tài khoản với phạm vi danh sách; giữ tương thích các trang cũ |
| `ManagementLayout.styles.ts` | Khoảng cách, filter panel, focus, table alignment và khả năng đọc nội dung dài; giữ sidebar cố định hiện có |
| `src/components/Common/ManagementViews.tsx` | StateView cho trống và DetailSection cho nội dung nghiệp vụ/bằng chứng mở rộng; giữ LoadError hiện có cho lỗi và retry |
| `src/pages/OnlinePayments/query.ts` | Đọc/ghi query có validation, giữ bộ lọc và tab khi mở/đóng chi tiết; phân trang có giới hạn |
| `src/pages/OnlinePayments/index.tsx` | Trang mẫu mới: số tiền căn phải, trạng thái rõ, mã hiển thị gọn với ID đầy đủ trong chi tiết/tooltip, scope theo tab, loading/lỗi/trống riêng, chi tiết ưu tiên kết quả và thông tin nghiệp vụ |
| `tests/online-query.test.mjs` | 3 kiểm thử mới về điều hướng, query không hợp lệ và trạng thái độc lập giữa hai tab |

TableActions hiện có đã đáp ứng một hành động chính + menu phụ nên được giữ nguyên. Pilot chỉ có Xem, không thêm menu trống hoặc nhiều nút cùng cấp.

URL ví dụ để kiểm tra trên web đang chạy: `/online-payments?state=PENDING&page=0&size=20`. SUPER_ADMIN có thể thêm branchId hợp lệ. Mã rút gọn GD-… chỉ là nhãn; API và liên kết luôn dùng UUID đầy đủ.

Đối soát vẫn dùng requestId/expectedVersion/reason và cơ chế khôi phục hiện có. STAFF chỉ xem; không bổ sung nút tự ghi nhận thanh toán. Không sửa API, quyền phía server, schema hoặc dữ liệu tài chính trong đợt UI này.

## Kiểm tra

- `npm.cmd run build`: PASS, TypeScript và Vite. Còn cảnh báo bundle >500kB; không phải lỗi build và chưa xử lý tối ưu bundle trong W2.
- `npm.cmd test`: **99/99 PASS** (96 cũ + 3 mới).
- `git diff --check` trên các file đã sửa: không có lỗi whitespace; Git có thông báo chuyển LF/CRLF theo cấu hình Windows.
- Edge headless riêng: **7 nhóm kiểm tra PASS** — 1366px không tràn ngang body, scope khớp bộ lọc; đóng drawer giữ query; nhật ký không nhận nhầm scope của đơn; 390px không tràn ngang body; STAFF không có đối soát và URL branch giả không truyền vào list; Tab tới control/Escape đóng drawer; lỗi tải có retry thay vì danh sách trống.

Kiểm tra trình duyệt dùng phản hồi GET/OPTIONS giả lập và phiên giả chỉ trong profile riêng, không gọi ghi dữ liệu backend. Không phải nghiệm thu end-to-end với VNPAY thật hoặc database đang chạy. Các test này không được đưa vào runtime của app. Script và report máy cục bộ: `storage/web-w1-w2-ui.cjs`, `storage/web-w1-w2-evidence/report.json`.

Ảnh nghiệm thu chứa dữ liệu giả lập chỉ để đánh giá bố cục:

- [Danh sách desktop 1366px](assets/web-w2/online-payments-desktop.png)
- [Chi tiết giao dịch](assets/web-w2/online-payment-detail.png)
- [Màn hẹp 390px](assets/web-w2/online-payments-mobile.png)

## Tiếp theo

W3 sẽ bổ sung ngữ cảnh học viên/lớp, tìm kiếm phía server, liên kết hai chiều học phí–đơn online–phiếu thu và áp dụng mẫu UI cho hai trang tài chính còn lại. W4 xử lý tham chiếu UUID trong hỗ trợ. Các trang khác hiện nhận nền shared theme/header/filter, chưa được coi là đã nâng cấp toàn bộ UI/luồng theo W3–W8.
