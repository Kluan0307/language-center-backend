# Giai đoạn 2 — Lịch sử dễ đọc, danh mục rõ vị trí, dọn code cũ

Ngày: **11/11/2026**. Hoàn thành **F01, F02, F12** của [giai đoạn 1](2026-11-11-phase-1-current-state.md) trong phạm vi frontend.

## Thay đổi

- **F01:** thêm AuditComparison và auditDisplay; đưa lại lịch sử trước/sau cho hồ sơ giáo viên, buổi học và đánh giá đầu vào; đưa lại căn cứ điều kiện/đánh giá lúc đăng ký. Dùng nhãn tiếng Việt, giữ người thực hiện/thời điểm/lý do. JSON, Java record lồng nhau và chuỗi session được đọc có kiểm soát; định dạng không đọc được có cảnh báo, không dựng giá trị. Snapshot và audit gốc trong backend/database không đổi.
- **F02:** thêm `/programs` và menu **Học vụ → Chương trình & trình độ**, chỉ SUPER_ADMIN. ProgramCatalog dùng được trên trang riêng; liên kết từ Khóa học và Đánh giá đầu vào. Không mở rộng quyền quản lý cho Admin/Staff/Teacher.
- **F12:** kiểm tham chiếu source/tests trước khi dọn. Xóa 9 file `data/*.mock.ts` và 4 file TeacherManagement/StaffManagement cũ (index/styles). Đã sao lưu đối chiếu SHA256 ở `storage/legacy-ui-phase-2/manifest.json`; không xóa các mock/stub dùng cho tests.

## Xác minh

- Frontend: **89/89 Node tests**, ESLint và production build đạt. Có 4 test mới cho đọc dữ liệu audit JSON/Java/session, nested levels, null, giá trị không đọc được, duplicate fields, ngày và boolean. Ma trận navigation kiểm tra thêm quyền `/programs`.
- Edge với API giả lập: trang chương trình, bảng trình độ, mở form tạo; Admin bị chặn và không có menu; mở lịch sử buổi học và xem phòng trước/sau. [Kết quả](../../storage/phase-2-preview/result.json), [ảnh lịch sử](../../storage/phase-2-preview/history.png).
- Build vẫn có cảnh báo chunk lớn; không coi cảnh báo này là lỗi nghiệp vụ.
- Không sửa Java/API/migration hoặc database. Không gọi kiểm tra mock là nghiệm thu full-stack mới. Chuỗi Java lịch sử không có escaping chuẩn; dữ liệu bất thường có thể không đọc được và sẽ được cảnh báo thay vì đoán.

## Bước tiếp theo

Giai đoạn 3: **F04 → F03 → F05** — tách truy vấn/validation đúng chế độ lịch, thêm điều hướng ngày/bộ lọc và lưu URL, cải thiện cuộn và lịch dày. Bộ fixture và nghiệm thu backend thật giữ cho giai đoạn 4–5.
