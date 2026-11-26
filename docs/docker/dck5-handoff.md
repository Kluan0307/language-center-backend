# DCK5 — Phục hồi dữ liệu MySQL và nghiệm thu stack Docker

Hoàn thành ngày **25/11/2026**. Đã chuyển nhất quán dữ liệu từ MySQL local trên Windows host vào volume MySQL riêng của Docker Compose, bảo toàn nguyên vẹn cơ sở dữ liệu gốc và nghiệm thu thành công toàn bộ stack 9 container.

Kết quả nghiệm thu: [Báo cáo migration](dck5-migration.json) · [Báo cáo nghiệm thu 17 checks](dck5-validation.json).

---

## 1. Kết quả chuyển đổi dữ liệu

- **Cơ sở dữ liệu nguồn (Windows MySQL)**: Hoàn toàn không bị sửa đổi (`sourceUnchanged: true`). Bản snapshot nhất quán được lấy qua transaction InnoDB, kiểm tra 87 bảng, 0 trigger/routine/event và 0 bảng non-InnoDB.
- **Diễn tập an toàn (`rehearsal`)**: Trước khi nạp vào database Docker chính, script `migrate-local.cjs` đã phục hồi thử nghiệm vào database tạm `lc_dck5_restore_<timestamp>` trong Docker MySQL và đối soát dấu vân tay SHA-256 (`rehearsalVerified: true`).
- **Nạp dữ liệu vào Docker MySQL**:
  - **87 bảng** dữ liệu.
  - **8.886 bản ghi** (419 tài khoản, 400 học viên, 1.856 lượt điểm danh, 432 kết quả học tập/ghi danh...).
  - Dấu vân tay băm SHA-256 khớp 100% với dữ liệu nguồn.
- **Bản lưu rollback**: Lưu trữ an toàn tại `storage/docker-backups/dck5-<timestamp>/` (bao gồm `docker-before.sql`, `source.sql`, `manifest.json`).

---

## 2. Trạng thái các dịch vụ sau phục hồi

Toàn bộ **9 container** đều khởi động lại thành công và đạt trạng thái `healthy`:

| Container | Image | Cổng ánh xạ host | Vai trò & Trạng thái |
|---|---|---|---|
| `language-center-mysql-1` | `mysql:8.0` | Không publish (nội bộ) | Lưu trữ 87 bảng dữ liệu (`healthy`) |
| `language-center-auth-service-1` | `language-center/auth-service:local` | 8081 nội bộ | Xác thực & Quản trị tài khoản (`healthy`) |
| `language-center-academic-service-1` | `language-center/academic-service:local` | 8082 nội bộ | Học vụ, lớp, môn, ghi danh (`healthy`) |
| `language-center-teaching-service-1` | `language-center/teaching-service:local` | 8083 nội bộ | Đánh giá, điểm số, tài liệu (`healthy`) |
| `language-center-finance-service-1` | `language-center/finance-service:local` | 8084 nội bộ | Sổ cái tài chính, thu học phí (`healthy`) |
| `language-center-assistant-service-1` | `language-center/assistant-service:local` | 8086 nội bộ | Trợ lý AI chatbot (`healthy`) |
| `language-center-api-gateway-1` | `language-center/api-gateway:local` | `127.0.0.1:18080` | Định tuyến & Bảo mật Gateway (`healthy`) |
| `language-center-web-admin-1` | `language-center/web-admin:local` | `127.0.0.1:15173` | Giao diện Quản trị Nginx (`healthy`) |
| `language-center-website-1` | `language-center/website:local` | `127.0.0.1:13000` | Website công khai Next.js (`healthy`) |

---

## 3. Nghiệm thu chức năng & Phân quyền (17/17 PASSED)

Kiểm thử tự động qua script `verify-dck5.cjs`:

1. **Khả năng hoạt động (Health/Readiness)**:
   - 9/9 container đạt `healthy`.
   - Gateway endpoint `/actuator/health/readiness` trả về `UP` (HTTP 200).
   - Actuator sensitive endpoints (`/actuator/env`) được bảo vệ an toàn (chặn 401/403/404).
2. **Đăng nhập trên dữ liệu thật (Authentication)**:
   - Superadmin: `superadmin` / `123456` → Đăng nhập thành công, nhận JWT.
   - Admin chi nhánh: `admin1` / `123456` → Đăng nhập thành công, nhận JWT.
   - Giáo viên: `teacher1` / `123456` → Đăng nhập thành công, nhận JWT.
   - Học viên: `hv0006` / `123456` → Đăng nhập thành công, nhận JWT.
3. **Phân quyền truy cập (RBAC Enforcement)**:
   - Request không kèm token vào `/api/v1/academic/leads` bị từ chối **401 Unauthorized** (kèm header `x-request-id`).
   - Giáo viên (`teacher1`) truy cập danh sách Leads bị từ chối **403 Forbidden**.
   - Học viên (`hv0006`) truy cập danh sách Leads bị từ chối **403 Forbidden**.
   - Quản trị viên (`admin1`) truy cập danh sách Leads thành công (HTTP 200).
   - Superadmin truy cập danh sách tài khoản hệ thống thành công (HTTP 200).
   - Giáo viên xem lịch giảng dạy cá nhân thành công (HTTP 200).
4. **Tích hợp Web & Website**:
   - Web Quản trị (`http://localhost:15173`) phục vụ trang đăng nhập SPA và proxy API trơn tru.
   - Website công khai (`http://localhost:13000`) hiển thị trang chủ và SSR kết nối Gateway catalog (`/api/v1/academic/public/catalog/`).

---

## 4. Cách kiểm tra lại nghiệm thu

Tại thư mục backend root:

```powershell
node scripts/docker/verify-dck5.cjs
```

Báo cáo kết quả chi tiết sẽ được tự động cập nhật tại `docs/docker/dck5-validation.json`.

---

## 5. Tiếp theo

Giai đoạn **DCK6** sẽ hoàn thiện bộ công cụ dòng lệnh tiện ích quản trị một chạm (up, down, status, logs, backup, restore) và tài liệu bàn giao vận hành hệ thống container.

