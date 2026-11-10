# Cải thiện 7 — triển khai và phục hồi

Giai đoạn này chuẩn hóa triển khai một máy chủ, phù hợp cấu trúc hiện tại: Java 21,
MySQL 8, năm tiến trình Spring Boot và frontend static sau reverse proxy. Không tự
publish ra Internet hoặc thay database đang dùng. Các lệnh dưới chạy từ backend root.

## Những phần đã bổ sung

| Phần | Hành vi |
|---|---|
| Profile `prod` | Hibernate `validate`, tắt SQL/bootstrap, shutdown mềm 30 giây, log xoay vòng |
| Cấu hình môi trường | DB/JWT/CORS và đường dẫn file nằm ngoài artifact; service bind loopback mặc định |
| Health | `/actuator/health/liveness`, `/actuator/health/readiness`; chỉ trả status, không trả chi tiết |
| Readiness | Bốn service kiểm tra DB; gateway kiểm tra trạng thái tiến trình của chính nó |
| Bảo vệ management | Không expose env, mappings, shutdown hoặc metrics công khai |
| Log request | Mã `X-Request-Id`, method, status, duration; không ghi body/token/query string |
| Backup | SQL transaction + file UUID + SHA-256 + số dòng và fingerprint dữ liệu |
| Diễn tập | Restore vào database mới sinh ngẫu nhiên, khôi phục file riêng, đối chiếu dữ liệu và replay V001–V006 |
| Release | Năm JAR + FE build + migration/runbook; manifest hash, không sao chép `.env.local` |

Profile production chặn khởi động khi bật DDL mutation, SQL logging, bootstrap hoặc
CORS wildcard. Teaching yêu cầu storage tuyệt đối, nằm ngoài thư mục release. Cần
phân quyền thư mục theo tài khoản chạy ứng dụng; không đặt document storage trong web root.

Health status dựa trên [Spring Boot Actuator](https://docs.spring.io/spring-boot/4.0/reference/actuator/endpoints.html).
Liveness không phụ thuộc DB, tránh restart hàng loạt khi MySQL gián đoạn. Readiness
của gateway không đại diện cho cả hệ thống: phải probe đủ năm service, sau đó smoke
qua gateway. Request ID đi từ gateway đến service nhận request; các HTTP gọi lồng
nội bộ chưa bảo đảm truyền tiếp cùng ID. Không coi log này là tracing phân tán đầy đủ.

## 1. Chuẩn bị môi trường

1. Java 21, Maven, Node 22+, MySQL 8 client (`mysql`, `mysqldump`) và server.
2. Tạo user ứng dụng chỉ có quyền runtime cần thiết (SELECT/INSERT/UPDATE/DELETE
   trên schema), tách user migration/backup. Không đưa root vào cấu hình production.
   Công cụ rehearsal cần tài khoản riêng có quyền CREATE DATABASE; tài khoản runtime không cần quyền đó.
3. Tạo thư mục dữ liệu bền vững, log và backup có ACL phù hợp, ngoài release.
4. Copy `deployment/production.env.example` sang nơi riêng và điền giá trị thật.
   Dùng một JWT secret đủ ít nhất 32 byte cho cả bốn service. Đặt đúng origin FE.
   `SPRING_CONFIG_IMPORT` phải được đặt rõ để không nạp `.env.local` của môi trường dev.
5. Có thể dùng file cấu hình thuộc tính Spring/secret manager thay vì env file.
   Script mẫu hiểu `KEY=value` nguyên văn, không hỗ trợ shell interpolation hoặc dotenv quoting.

Không đổi `.env.local` hiện có để chuyển môi trường. Không commit file secret, backup
SQL hoặc document. Backup hiện lưu plain SQL/file trên đĩa: cần ACL, mã hóa khi lưu
ngoài máy và chính sách lưu giữ của đơn vị triển khai; công cụ chưa tự mã hóa/upload.

## 2. Backup SQL và file đồng bộ

Trước nâng cấp production: đưa hệ thống vào bảo trì, dừng **mọi writer** (cả service
trực tiếp và job/tích hợp), đợi request hoàn tất. Chỉ chặn gateway chưa đủ nếu các
service khác vẫn nhận ghi. Giữ MySQL chạy; không chạy DDL trong lúc dump.

```powershell
node scripts/operations.cjs backup --env D:/language-center-config/production.env --documents D:/language-center-data/documents --writes-paused
```

Nếu dữ liệu cũ có file nằm ở nhiều thư mục, lặp `--documents <directory>` cho từng
thư mục. Cùng key khác nội dung bị từ chối. Folder không tồn tại, symlink, file
không có tên UUID hoặc backup đang có bị từ chối. Không tự ghi đè backup cũ.

`--writes-paused` là xác nhận của người vận hành; công cụ không tự dừng ứng dụng.
Chế độ `--online-rehearsal` phục vụ diễn tập khi chưa bảo trì, **không được dùng để
tuyên bố điểm phục hồi production**. Hai lần fingerprint/count giúp phát hiện thay
đổi nhưng không thay thế việc khóa mọi writer. Backup lỗi có `complete=false`, phải
giữ để điều tra hoặc người vận hành dọn riêng, không được mang đi restore.

Mặc định output ở `storage/backups/backup-...`. Có thể chỉ định thư mục mới bằng
`--output`. Đường dẫn MySQL client tùy chỉnh qua `MYSQL_BIN`; mật khẩu truyền bằng
environment của child process, không nằm trong command line hoặc log.

```powershell
node scripts/operations.cjs verify --backup D:/backups/backup-...
node scripts/operations.cjs rehearse --env D:/language-center-config/backup.env --backup D:/backups/backup-...
```

`verify` kiểm tra checksum/size/tính hoàn tất và đường dẫn. Chỉ restore **backup tin
cậy**; checksum không phải chữ ký xác thực nguồn SQL. `rehearse` không nhận tên
database đích: luôn tạo `lc_rehearsal_<timestamp>_<random>`, restore và so sánh toàn
bộ fingerprint/số dòng, kiểm tra mọi file của revision, copy file sang thư mục
restore riêng rồi chạy V001–V006 trên bản sao. Báo cáo `rehearsal.json` lưu database,
storage, hash migration và kết quả; không chứa mật khẩu/token. Database/file diễn
tập được giữ, không tự DROP/xóa. Chạy lặp lại tạo bản sao mới.

Hiện cơ chế fingerprint dùng dump chuẩn hóa trong bộ nhớ, phù hợp quy mô KLTN;
database lớn cần đổi sang hash streaming và đánh giá thời gian/RAM trước vận hành.

## 3. Migration và kiểm tra schema

Đọc [migration runbook](../migrations/README.md) trước. V001–V006 nâng cấp schema
đã có, không phải một baseline tạo mới toàn bộ database. Không tự chạy những file
`preflight`/`prepare-existing-schema` lên database thật mà chưa xem kết quả trên bản sao.

Trước nâng cấp: backup đã verify và restore rehearsal thành công; lưu release cũ,
manifest và hash SQL sẽ chạy. Trên bản sao, chạy migration theo thứ tự và khởi động
cả bốn service với `prod`/`validate`. Nếu schema validation thất bại, bổ sung migration
có chủ đích; không đổi production sang `update` để che lỗi.

SQL migration vẫn chạy thủ công, chưa có Flyway lịch sử tự động. Với schema hoàn
toàn mới, cần baseline riêng được nghiệm thu; không bật `update` trên database có
dữ liệu thật để bootstrap.

## 4. Build và đóng gói

```powershell
mvn verify
node --test scripts/operations.test.cjs
```

Tại repo frontend:

```powershell
npm ci
npm test
npm run lint
$env:VITE_API_BASE_URL='/api/v1'
npm run build
```

Quay lại backend:

```powershell
node scripts/package-release.cjs
```

Có thể truyền lần lượt đường dẫn repo FE và thư mục output mới. Công cụ chỉ đóng
gói artifact đã build, không thay thế test và không publish. Từ chối bundle còn URL
dev `http://localhost:8080/api/v1`. `release.json` ghi hash từng artifact, Git revision
và trạng thái dirty; dirty=true nghĩa là artifact có thay đổi chưa commit, không
được coi Git SHA là mô tả đầy đủ source. Chốt source/commit và lưu bằng chứng test
trước phát hành chính thức. `start-release.ps1` đối chiếu checksum JAR trước chạy;
manifest không thay thế chữ ký release hoặc quyền truy cập filesystem.

## 5. Chạy thử release

Mở terminal riêng, chạy Auth, Academic, Teaching, Finance, rồi Gateway. Ví dụ:

```powershell
.\scripts\start-release.ps1 -Release D:/releases/release-... -Service auth-service -EnvFile D:/language-center-config/production.env
```

Đổi `-Service` cho các tiến trình còn lại. Script chạy foreground và trả lỗi khi
service dừng bất thường. Khi vận hành lâu dài, dùng Windows service manager/systemd
của máy chủ, cấu hình restart/backoff và working directory đúng; chưa cài service
hoặc ghi thay đổi hệ điều hành trong giai đoạn này.

Nginx mẫu ở `deployment/nginx.conf`: FE và `/api/v1/` cùng origin, SPA fallback,
assets hash cache dài và HTML no-cache. Chỉ gateway được proxy; file vật lý và
Actuator không được proxy. Mẫu bind `127.0.0.1:8090` để preview cùng máy. Nginx phải
được khởi động với prefix là thư mục release và config `deployment/nginx.conf`.
Trước public cần domain/TLS/firewall và kiểm tra cấu hình `nginx -t`; chưa cài hoặc
cấu hình chứng chỉ do chưa có máy chủ/domain được chỉ định. Không dùng Vite dev
server làm web server production.

## 6. Giám sát và nghiệm thu

```powershell
node scripts/operations.cjs smoke
```

Smoke kiểm tra 10 probes, 5 management endpoints bị chặn và 4 business routes
không có token trả 401 với Request ID. `--base-port 17080` dùng cho bộ năm service
thử ở 17080–17084. Đây là kiểm tra vận hành không ghi dữ liệu, chưa thay thế
nghiệm thu đăng nhập/nghiệp vụ bằng cả bốn role.

Vận hành cần giám sát định kỳ cả năm readiness, dung lượng DB/file/log/backup,
backup gần nhất đã verify, thời gian HTTP và tỷ lệ 5xx. Log mẫu xoay 10MB/file,
14 ngày, tối đa 200MB/service; lưu/thu gom log tập trung tùy hạ tầng. Chưa có
Prometheus/Grafana, lịch backup tự động hay kênh gửi cảnh báo được cấu hình.

Nghiệm thu theo [các luồng hiện có](../verification.md): branch isolation và bốn
role; lead→học viên→ghi danh→học phí/thu tiền; nghỉ/bù/điểm danh; roster/điểm thi;
từ chối/gửi lại/duyệt/tải tài liệu. Trên staging cần cả lỗi service phụ thuộc,
timeout sau commit, thao tác đồng thời, hết hạn token, mobile và reload trang sâu.
Đối soát sổ thu/hoàn và file revision sau restore; không dùng dữ liệu người thật
để thử các giao dịch phá hủy. Chưa tuyên bố tải production hoặc RPO/RTO thực tế.

## 7. Khi cần quay lại

Dừng writer và giữ lại log/backup/snapshot lỗi. Không chạy release cũ ghi vào schema
mới khi chưa xác nhận tương thích. Có thể khôi phục sang database/storage **mới**,
đối soát và cấu hình release phù hợp trỏ sang chúng, smoke đầy đủ rồi mới mở lại
traffic. Không ghi đè database nguồn bằng script rehearsal. Giao dịch phát sinh
sau thời điểm backup cần được đối soát/phục hồi riêng; không thể giả định rollback
không mất dữ liệu. RPO/RTO phải được đo với backup đủ lớn và điều kiện máy chủ thật.

## Bằng chứng local

Ngày 05/11/2026 đã backup và restore bản sao 33 bảng, 4 file revision; replay
V001–V006 thành công và giữ dữ liệu bảo vệ. Chế độ backup là online rehearsal,
không phải điểm phục hồi production. Bằng chứng nằm ở `storage/improvement7-*`
và `storage/backups/improvement7-verified-snapshot/`, đều ngoài Git. Không xóa
fixture CARE trước đó hoặc database/file diễn tập.

Release local cuối có 83 artifact kèm manifest; cả năm tiến trình đã khởi động
profile prod trên cổng 17080–17084. 195 backend tests, 9 tests công cụ backup,
19 smoke checks và 16 kiểm tra đăng nhập/quyền đọc/CORS đạt. Production FE build
với `/api/v1` đạt; 72 FE tests từ giai đoạn 6 là baseline không thay đổi source FE.
Chưa chạy Nginx trên máy này vì chưa được cài; config chỉ là mẫu cần `nginx -t`
và nghiệm thu trên máy đích. Các tiến trình xác minh được dừng sau kiểm tra;
database/file restore và các release được giữ để tra cứu.
