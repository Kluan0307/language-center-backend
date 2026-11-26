# Công ty và hỗ trợ nội bộ — S1, S2, S3

**S4–S5 hoàn thành (14/11/2026):** cài đặt theo tài khoản, thông tin trung tâm và ba mục cuối sidebar đã nối đầy đủ. V022 đã áp dụng local. [Bàn giao và kiểm tra desktop/mobile](company-settings-s4-s5.md).

Cập nhật 14/11/2026. Đã triển khai backend Auth, web Ant Design theo cấu trúc trang hiện có, migration và cơ cấu mẫu. Cài đặt cá nhân/hệ thống thuộc S4, chưa triển khai trong đợt này.

## S1 — Dữ liệu và phân quyền

Migration [V021](../migrations/V021__company_internal_support.sql) bổ sung năm bảng: `organization_units`, `job_titles`, `organization_assignments`, `internal_support_tickets`, `internal_support_replies`. Đã kiểm tra migration chạy lại và áp dụng local; các bảng nghiệp vụ cũ được giữ nguyên.

- Đơn vị tổ chức có đơn vị cha và chi nhánh liên quan tùy chọn; không cho tạo vòng lặp.
- Chức vụ độc lập với role. Chỉ SUPER_ADMIN sửa đơn vị, chức vụ và phân công. Gán chức vụ CEO không tự cấp quyền quản trị cho người đó.
- Một tài khoản nhân sự có nhiều phân công chức vụ/đơn vị. Nhân sự thuộc đơn vị chi nhánh cần phân công chi nhánh hợp lệ. Không cho tự quản lý chính mình hoặc tạo vòng lặp quản lý.
- Không ngừng sử dụng đơn vị đang có đơn vị con/phân công active hoặc chức vụ đang có người đảm nhiệm active. Ngừng phân công trước, giữ các hàng lịch sử thay vì xóa vật lý.
- Version bảo vệ cập nhật cũ. Các thao tác tổ chức được ghi sự kiện bảo mật; không cung cấp endpoint xóa nhân sự.
- Danh bạ là projection dùng chung mới cho bốn role web: mã, họ tên, chi nhánh, đơn vị và chức vụ. Không trả username đăng nhập, mật khẩu, email/điện thoại cá nhân, bằng cấp, lịch sử bảo mật. API hồ sơ chi tiết vẫn theo quyền trước đây.

## S2 — Công ty

Trang `/company` có ba tab:

1. Tổng quan: tên tổ chức và các thẻ chi nhánh, địa chỉ, số liên hệ hiện có.
2. Cơ cấu: cây tổ chức, đơn vị, chức vụ/người đảm nhiệm và quản lý trực tiếp. SUPER_ADMIN có form thêm/sửa và ngừng sử dụng, nhân sự khác chỉ xem.
3. Nhân sự: phân trang, tìm mã/họ tên và lọc theo chi nhánh, đơn vị, chức vụ; mỗi người một hàng dù làm ở nhiều chi nhánh.

Cơ cấu mẫu được nạp idempotent bằng [company-organization.sql](../demo/company-organization.sql): 9 đơn vị, 9 chức vụ và 22 phân công từ tài khoản/phân công chi nhánh thực tế. Bao gồm Ban lãnh đạo, Đào tạo, Tuyển sinh/chăm sóc, Hành chính–kế toán và bốn cơ sở. Các chức vụ Chủ tịch, Giám đốc điều hành, Giám đốc đào tạo… chưa có người được xác nhận sẽ hiển thị “Chưa có người phụ trách”. Không tự coi superadmin là Chủ tịch/CEO.

Danh bạ công việc hiện có 19 người. Bộ chọn người trong form cơ cấu tải tối đa 100 nhân sự active ở bản đầu; danh bạ chính có phân trang server. Không có upload ảnh hoặc công bố liên hệ cá nhân trong S1–S3.

## S3 — Hỗ trợ

Trang `/help` tách khỏi `/support-requests` xử lý hỗ trợ học viên:

- Hướng dẫn theo role: tài khoản/chi nhánh, chatbot, lịch, điểm hoặc học phí.
- FAQ có tìm kiếm: mật khẩu, quyền chi nhánh, thanh toán, công bố điểm.
- Gửi yêu cầu nội bộ: nhóm ACCOUNT/USAGE/BUG/FEEDBACK, tiêu đề, mô tả, chi nhánh tùy chọn.
- Yêu cầu của tôi; hàng chờ xử lý cho ADMIN/SUPER_ADMIN; lọc trạng thái và phân trang.
- Drawer xem nội dung và trao đổi. ADMIN/SUPER_ADMIN có quyền xử lý cập nhật OPEN → IN_PROGRESS → RESOLVED qua phản hồi. Version cũ bị từ chối; nhân sự khác không đổi trạng thái và không đọc ticket của người khác.
- ADMIN chỉ xử lý ticket có chi nhánh trong phân công hiện hành. Ticket hệ thống không có chi nhánh do SUPER_ADMIN xử lý; tác giả vẫn được đọc/phản hồi ticket của mình.
- Mỗi phản hồi lưu tác giả, thời điểm và trạng thái sau thao tác, tạo lịch sử trao đổi. Chưa thêm email, file đính kèm, SLA hoặc tổng đài.

## API

Tất cả thuộc `/api/v1/auth`, dùng JWT hiện có. STUDENT bị từ chối các API dưới đây.

| Method | Route | Quyền |
|---|---|---|
| GET | `/company` | Bốn role web |
| GET | `/company/directory` | Bốn role; keyword, branchId, unitId, titleId, page, size |
| POST / PUT | `/company/units`, `/company/units/{id}` | SUPER_ADMIN |
| POST / PUT | `/company/titles`, `/company/titles/{id}` | SUPER_ADMIN |
| POST / PUT | `/company/assignments`, `/company/assignments/{id}` | SUPER_ADMIN |
| GET | `/internal-support/guides` | Bốn role |
| GET / POST | `/internal-support/tickets` | Bốn role; queue chỉ ADMIN/SUPER_ADMIN |
| GET | `/internal-support/tickets/{id}` | Chủ yêu cầu hoặc người xử lý hợp lệ |
| POST | `/internal-support/tickets/{id}/replies` | Như trên; thay status cần quyền xử lý |

## Khởi động và kiểm tra

Migration local đã áp dụng. Khởi động lại **auth-service**, tải lại web, mở Công ty/Hỗ trợ ở cuối sidebar. Gateway dùng tuyến Auth hiện có, không cần đổi cấu hình. Không thay mật khẩu hoặc dữ liệu học viên/lớp/học phí.

Máy khác: chạy V021 trước khi khởi động Auth ở chế độ validate. Muốn cơ cấu mẫu thì chạy script seed sau khi migration:

```powershell
node scripts/identity/company-migration.cjs --apply
node scripts/identity/seed-company.cjs --apply
```

Không gắn chức năng Cài đặt vào trang cá nhân tạm thời; mục đó tiếp tục chờ S4.

Kiểm chứng:

- Auth: 104 kiểm thử đạt, gồm 9 ca mới kiểm tra dữ liệu công bố, role, vòng lặp, ngừng sử dụng, sở hữu ticket và version.
- Web: 125/125 đạt; build TypeScript/Vite và lint file thay đổi đạt. Sidebar test kiểm tra thêm `/company` và `/help` cho bốn role.
- HTTP thật với Auth/Academic và bản sao MySQL: 21 kiểm tra đạt, gồm danh bạ công bố, lọc đơn vị, STUDENT/TEACHER bị chặn thao tác không có quyền, ADMIN ngoài chi nhánh không đọc ticket, cập nhật và version cũ. Database/processe thử đã dọn.
- Chưa nghiệm thu tương tác trên trình duyệt thật hoặc email/push; S1–S3 không triển khai các phần đó.

Bằng chứng: `storage/company-s123-final-tests.log`, `storage/company-s123-web-tests.log`, `storage/company-s123-web-build.log`, `storage/company-s123-web-lint.log`, `storage/company-s123-http.json`.
