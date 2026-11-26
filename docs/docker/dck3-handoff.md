# DCK3 — Backend và MySQL bằng Docker Compose

Ngày triển khai: **24/11/2026**. Compose chuẩn được quản lý tại backend `compose.yaml`; file `language-center/compose.yaml` bên ngoài include file này để chạy từ thư mục tổng.

## Đã triển khai

- MySQL 8.0 + Auth, Academic, Teaching, Finance, Gateway; Assistant bật tùy chọn bằng profile `assistant`.
- Gateway publish **127.0.0.1:18080 → 8080** để cùng tồn tại với backend Maven cũ. MySQL và các service khác không publish cổng host.
- Network `database` là internal, chỉ MySQL và bốn service DB tham gia. Gateway/Assistant chỉ vào network `backend`.
- MySQL healthy trước khi bốn service DB chạy; Gateway/Assistant đợi bốn service đó healthy. Không biến các quan hệ gọi API hai chiều thành vòng `depends_on`.
- Volume `mysql_data` lưu DB; `documents` lưu tài liệu của Teaching. Container ứng dụng chạy UID 10001, root filesystem read-only, `/tmp` riêng; tài liệu vẫn ghi được qua volume.
- Giới hạn RAM/CPU, log tối đa 10 MiB × 3 và thời gian shutdown cho từng container.
- Phân phối biến riêng theo service: root DB password chỉ ở MySQL, DB/JWT chỉ ở bốn service dữ liệu, key Gemini chỉ ở Assistant, key VNPAY chỉ ở Finance.

Healthcheck/điều kiện phụ thuộc theo [Docker Compose startup order](https://docs.docker.com/compose/how-tos/startup-order/); Assistant dùng [Compose profile](https://docs.docker.com/compose/how-tos/profiles/).

## Database mới và dữ liệu hiện tại

`deployment/docker/mysql/001-schema.sql` là snapshot **87 bảng, không có bản ghi** từ schema local hiện tại. Các bộ đếm AUTO_INCREMENT ở mức bảng đã được bỏ để database mới khởi đầu sạch. [Manifest schema](schema-manifest.json) ghi số bảng và SHA-256.

MySQL official image chạy các script `/docker-entrypoint-initdb.d` khi volume dữ liệu còn mới, theo [hướng dẫn image MySQL](https://hub.docker.com/_/mysql). Script tiếp theo thu quyền tài khoản ứng dụng xuống SELECT/INSERT/UPDATE/DELETE. Root dùng cho init/backup/migration; không cấp root password cho ứng dụng.

Các file init **không chạy lại trên volume đã có dữ liệu**. Cập nhật schema sau này cần migration/restore có kiểm soát; không đổi Hibernate sang `update` để bỏ qua lỗi và không dùng `down -v` như lệnh dừng thông thường.

**Chưa nhập tài khoản, học viên, khóa/lớp hoặc dữ liệu MySQL đang dùng.** Docker DB mới có schema rỗng; các tài khoản cũ sẽ chưa đăng nhập được vào stack Docker cho tới bước chuyển dữ liệu DCK5. MySQL local cổng 3306/3308 hiện tại không bị thay đổi.

## Chạy và dừng

Đã tạo `language-center/.env.docker.local` với secret Docker riêng. File không được commit; không hiển thị secret trong báo cáo. Không dùng nguyên file `.env.local` của Maven cho Docker vì URL, storage và key giữa các môi trường khác nhau.

Tại backend root:

```powershell
# Lần đầu trên máy khác: sinh file cấu hình riêng, giữ nguyên nếu đã có
node scripts/docker/init-env.cjs

# Chạy core backend + MySQL bằng các image đã build ở DCK2
.\scripts\docker\compose.ps1 -Action up

# Chạy cả container Assistant (AI vẫn tắt cho tới khi cấu hình key/flags)
.\scripts\docker\compose.ps1 -Action up -Assistant

# Nếu cần build lại image từ source
.\scripts\docker\compose.ps1 -Action up -Assistant -Build

.\scripts\docker\compose.ps1 -Action ps
.\scripts\docker\compose.ps1 -Action logs
.\scripts\docker\compose.ps1 -Action down
```

Lệnh `down` của script dừng/xóa container và network nhưng **giữ volume**. `stop` giữ cả container. Khi muốn tắt Assistant: chạy `down`, sau đó `up` không có `-Assistant`.

Tại thư mục ngoài `language-center/`:

```powershell
docker compose --profile assistant up -d --wait
docker compose --profile assistant ps
docker compose --profile assistant down
```

Kiểm tra API: `http://localhost:18080/actuator/health/readiness`. Catalog rỗng: `http://localhost:18080/api/v1/academic/public/catalog/COURSE`.

`GATEWAY_HOST_PORT` thay được trong `.env.docker.local`. Mặc định bind loopback; kết nối từ điện thoại/LAN và URL các website sẽ cấu hình ở DCK4. Compose cố định DB nội bộ `mysql:3306/language_center_db`; biến DB_URL trong mẫu chỉ dành cho chạy service thủ công.

Nếu dùng `docker compose config`, ưu tiên `config --quiet`; bản render đầy đủ có thể chứa secret nội suy.

## Kiểm chứng

[Báo cáo tích hợp](dck3-validation.json) kiểm tra trên một project/volume tạm hoàn toàn riêng:

- Stack mặc định healthy, Assistant chưa chạy; bật profile thì đủ bảy container healthy.
- Đủ 87 bảng; không seed/import bản ghi ứng dụng.
- Gateway gọi Academic/Auth/Teaching/Finance/Assistant qua DNS nội bộ, giữ phản hồi xác thực.
- Tài khoản DB runtime bị từ chối CREATE TABLE.
- Teaching ghi tài liệu bằng UID 10001.
- Tạo lại container MySQL vẫn giữ dữ liệu thử; tạo lại Teaching vẫn giữ file thử.
- Project, network, volume và secret file thử nghiệm được dọn khi hoàn tất.

[Báo cáo stack chính](dck3-main-status.json) ghi health, cổng publish và trạng thái schema rỗng sau khi chạy bằng cấu hình chính. Không ghi nội dung secret.

Chạy lại kiểm thử cô lập:

```powershell
node scripts/docker/verify-compose.cjs
```

Script chỉ dùng `down --volumes` cho project thử sinh tự động dạng `lc-dck3-<timestamp>`, không dùng tên project `language-center`.

## Tiếp theo

**DCK4** ghép hai website, đồng bộ URL API/CORS và WEBSITE_FORM_SECRET giữa Next và Academic. **DCK5** backup/restore dữ liệu hiện tại vào MySQL Docker riêng, kiểm thử đăng nhập các role/nghiệp vụ và cấu hình provider khi cần. VNPAY/push/Gemini vẫn tắt trong cấu hình Docker mới.
