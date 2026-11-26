# L10 — Gói triển khai và vận hành website

Ngày: 16/11/2026. Đã chuẩn bị và kiểm tra bản standalone local; **chưa triển khai Internet, chưa cấp chứng chỉ thật, chưa bật index**. Chưa có hosting/domain được xác nhận. Máy có Docker CLI/Compose nhưng Docker Engine chưa chạy, do đó image và HTTPS proxy chưa được nghiệm thu runtime.

## Thành phần

- Next `output:standalone`, tắt X-Powered-By; bổ sung nosniff, chống nhúng iframe, referrer và permissions headers.
- Dockerfile nhiều giai đoạn: npm ci/build, runtime Node 22 chạy bằng user node, không copy env riêng vào build context; public và static assets đi cùng release.
- Compose website + Caddy: chỉ xuất 80/443; Node 3000 nằm trong mạng container; chứng chỉ lưu ở volume bền vững, log xoay 10 MiB × 3.
- Caddy tự quản lý HTTPS khi domain trỏ đúng máy và mở 80/443; ghi đè header IP dành cho form và chặn body form quá 8 KiB. Không áp dụng header IP tin cậy này khi đi qua thêm CDN/proxy mà chưa xác định nguồn IP.
- `/api/health`: liveness. `/api/health?check=ready`: kiểm tra form key có cấu hình và gateway catalog truy cập được. Chỉ trả UP/DOWN, không có URL backend, key hay chi tiết database.
- Readiness không thay thế kiểm tra key khớp giữa hai server, nhân sự nhận lead hoặc nội dung đã duyệt. Docker đánh dấu unhealthy không tự sửa backend hỏng; cần theo dõi và xử lý nguyên nhân.
- `scripts/website/preflight.cjs` kiểm tra env production và smoke chỉ đọc, không gửi form hoặc tạo lead.
- Robots chặn index mặc định. Chỉ cho index khi có HTTPS origin hợp lệ và chủ động bật WEBSITE_INDEXABLE=true; API không thuộc phạm vi index.

## Lựa chọn triển khai

### VPS Linux + Docker

1. Chuẩn bị domain website và backend gateway ổn định. Không dùng ngrok cho production. DNS A/AAAA trỏ đúng VPS; không cấu hình AAAA nếu máy chưa hỗ trợ IPv6 thực tế.
2. Backend triển khai theo `deployment/production.env.example` và runbook `docs/improvements/phase-seven-deployment.md`. Giữ các service/DB trong mạng riêng; nếu website ở container thì địa chỉ gateway phải truy cập được từ container. `127.0.0.1` trong container không phải host. Không đổi tất cả service sang public chỉ để khắc phục kết nối.
3. Với backend cùng Docker network có thể dùng `http://gateway:8080/api/v1/academic/public/catalog/`; nếu backend ở host/nhà cung cấp khác thì dùng endpoint mạng riêng hoặc HTTPS đã cấu hình. Compose website không tự tạo các service backend.
4. Copy `language-center-website/deployment/website.env.example` vào nơi riêng ngoài checkout, ví dụ `/etc/language-center/website.env`. Điền WEBSITE_HOST, NEXT_PUBLIC_SITE_URL, WEBSITE_API_URL và WEBSITE_FORM_SECRET (khớp backend). Hạn chế quyền đọc file.
5. Từ backend checkout kiểm tra:

```sh
node scripts/website/preflight.cjs /etc/language-center/website.env
```

6. Từ thư mục website:

```sh
docker compose --env-file /etc/language-center/website.env -f deployment/compose.yaml config --quiet
docker compose --env-file /etc/language-center/website.env -f deployment/compose.yaml build
docker compose --env-file /etc/language-center/website.env -f deployment/compose.yaml up -d
docker compose --env-file /etc/language-center/website.env -f deployment/compose.yaml ps
```

7. Từ backend checkout:

```sh
node scripts/website/preflight.cjs --smoke https://TEN-MIEN-THAT
```

Chạy với WEBSITE_INDEXABLE=false cho staging; sau smoke và nghiệm thu L9 trên dữ liệu thật mới bật index. NEXT_PUBLIC_SITE_URL/contact và robots có dữ liệu tại build time: **phải rebuild** sau khi đổi domain/contact/index flag; đổi env runtime đơn thuần không đủ.

### Vercel + backend riêng

- Import repo website, framework Next.js. Có vercel.json chọn region sin1; xác nhận region phù hợp tài khoản trước triển khai.
- Cấu hình env build/runtime theo mẫu, không đặt key dưới prefix NEXT_PUBLIC. WEBSITE_API_URL phải là gateway ổn định mà Vercel truy cập được; localhost và mạng nội bộ VPS không tự truy cập được từ Vercel.
- Preview giữ WEBSITE_INDEXABLE=false. Production domain HTTPS phải khớp NEXT_PUBLIC_SITE_URL, redeploy khi đổi.
- Không tự bật WEBSITE_CLIENT_IP_HEADER nếu chưa xác nhận nền tảng ghi đè header IP tin cậy. Giá trị mặc định trống vẫn có giới hạn tổng/điện thoại.
- Không chạy Docker/Caddy khi dùng hosting quản lý HTTPS sẵn. Smoke và nghiệm thu nguồn tư vấn vẫn bắt buộc.

## Giám sát

- Theo dõi liveness và readiness website mỗi 1–5 phút bằng công cụ monitoring của hosting hoặc scheduler riêng; status 503/non-zero là lỗi cần điều tra.
- Probe readiness từng backend, không chỉ gateway. Cấu hình hiện có ở backend không công khai chi tiết database.
- Chạy smoke sau mỗi release. Theo dõi 5xx, timeout, tỷ lệ lỗi form, dung lượng DB/file/log, hạn chứng chỉ, ngày backup và restore rehearsal gần nhất.
- Không log form body, phone/email hoặc env secret. Chưa có kênh cảnh báo bên ngoài được kết nối; chọn nhà cung cấp giám sát khi chốt hosting.

Ví dụ scheduler gọi công cụ chỉ đọc:

```sh
node /opt/language-center-backend/scripts/website/preflight.cjs --smoke https://TEN-MIEN-THAT
```

Exit code 0 là đạt, khác 0 cần kiểm tra. Scheduler/monitoring bên ngoài chịu trách nhiệm lưu kết quả và gửi cảnh báo; không có tác vụ gửi email/chat tự động nào được thiết lập trong đợt này.

## Database, backup và rollback

- Profile prod hiện dùng ddl-auto=validate; áp dụng đầy đủ migration đã duyệt đến V025 trước release, kiểm tra trên bản sao. Không reset/seed database production.
- Tái sử dụng `scripts/operations.cjs`: backup toàn schema nên bao gồm cả publications, inquiries, teacher profiles và edit events mới. Cần sao lưu cả tài liệu upload và cấu hình riêng theo chính sách lưu trữ.
- Backup nhất quán cần dừng tất cả writer/job như runbook hiện có; không gọi --writes-paused khi chưa thực sự dừng writer. Không tự chạy backup hoặc dừng dịch vụ đang dùng trong đợt này.

```sh
node scripts/operations.cjs backup --env /etc/language-center/backend.env --documents /var/lib/language-center/documents --output /secure-backups/RELEASE-TIMESTAMP --writes-paused
node scripts/operations.cjs verify --backup /secure-backups/RELEASE-TIMESTAMP
```

Thử restore bằng chế độ rehearse hiện có vào **schema mới**, không ghi đè schema đang vận hành. Mã hóa và kiểm soát truy cập backup, giữ bản ngoài máy chủ. Chốt lịch backup/retention/RPO/RTO sau khi có môi trường thật; chưa có lịch backup tự động nào được kích hoạt.

- Lưu image/release cũ và env version riêng. Website rollback về image đã kiểm thử; không tự hạ schema hoặc ghi đè DB bằng backup cũ. Nếu có thay đổi schema, đối soát khả năng tương thích trước khi rollback backend.
- Không chạy docker compose down -v trong vận hành: volume TLS là dữ liệu bền vững.

## Kết quả kiểm tra và phần còn mở

- TypeScript + production build standalone đạt.
- 3 unit test preflight đạt, 37 kiểm tra Chrome/standalone đạt; readiness trả 503 khi upstream lỗi, liveness vẫn UP, headers/robots/sitemap đúng ở staging.
- Compose được kiểm tra cấu trúc khi có CLI. Image/Caddy TLS runtime, DNS/domain, firewall, external monitoring, backup schedule và restore trên hạ tầng thật còn cần xác minh sau khi có hosting.
- L9 trên MySQL/APK và thao tác CMS thực tế vẫn là điều kiện trước khi đóng nghiệm thu vận hành. Không gọi gói cấu hình này là website đã online.
