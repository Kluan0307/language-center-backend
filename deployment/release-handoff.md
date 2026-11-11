# Bàn giao và chạy bản release local

Gói gồm năm JAR trong backend, frontend đã build với API `/api/v1`, SQL V001–V013, công cụ vận hành và snapshot source/tài liệu. Source giữ cấu trúc tại `source/language-center`: đọc README và `language-center-backend/language-center-backend/docs/thesis/report/index.md` trong đó. Không có .env.local, mật khẩu thực, SQL dump hoặc node_modules.

Manifest release.json ghi mọi file/byte/SHA-256, Git revision và dirtyWorkingTree. Source snapshot/hash mô tả nội dung working tree thực; Git HEAD đơn lẻ không đủ khi dirty. Gói là snapshot bất biến trước lúc chạy kiểm chứng. **Kết quả kiểm chứng cuối nằm trong hồ sơ qualification riêng đi kèm**, có hash release.json; không sửa manifest sau kiểm thử để ghép kết quả tương lai vào gói đã chạy.

## Cập nhật snapshot11/11

Có trang Admin và Chương trình & trình độ, Time-grid và lịch sử trước/sau; source FE kèm tests/docs.335BE/96FE/86ca nghiệm thu source đã đạt ở giai đoạn5. Snapshot này chưa tự mang kết quả qualification tương lai; đọc báo cáo riêng kèm gói và kiểm đúng hash release.json.

## Khởi tạo database sạch

Có `docs/migrations/schema-v013-clean.sql`: cấu trúc53bảng/625cột, không có account/mật khẩu/dữ liệu; ID bắt đầu nhỏ. Chỉ chạy trên database rỗng đã chọn, không chạy lên database đang dùng. Sau đó bootstrap Super Admin một lần theo hướng dẫn cài mới trong source docs/migrations/README.md, dừng bootstrap và chạy prod/validate. Với database cũ, backup và đối chiếu migration thay vì chạy clean schema.

## Kiểm tra và chạy

Từ gói đã giải nén, dùng Node22+, Java21, MySQL8 và PowerShell:

```powershell
node scripts/verify-release.cjs .
.\scripts\start-release.ps1 -Release . -Service auth-service -EnvFile D:/language-center-config/runtime.env
```

Lặp lệnh ở terminal riêng cho academic-service, teaching-service, finance-service, api-gateway, dùng đúng URL/cổng trong env. Script foreground kiểm hash JAR, yêu cầu prod và SPRING_CONFIG_IMPORT rõ ràng để không nạp env dev. Dùng Ctrl+C ở terminal tương ứng để dừng. Cần schema đã migrate và documents ở đường dẫn tuyệt đối; ddl-auto của prod phải validate. Không tự migrate/reset DB nguồn để chạy release.

Env đặt riêng ngoài gói, theo mẫu deployment/production.env.example: DB_URL/USERNAME/PASSWORD, JWT_SECRET, SPRING_PROFILES_ACTIVE=prod, SPRING_CONFIG_IMPORT, CORS_ALLOWED_ORIGINS, DOCUMENT_STORAGE và các service URLs; SERVER_PORT theo từng service. Không chiếu env, token hoặc dump khi demo.

Log phải ở ngoài gói: đặt LOG_FILE hoặc LOGGING_FILE_NAME là đường dẫn tuyệt đối. Nếu chưa đặt, launcher dùng thư mục logs bên cạnh EnvFile. Không chạy thêm tiến trình ghi vào release sau khi đã tạo manifest.

Với bộ cổng nghiệm thu28080–28084, frontend build tĩnh có thể chạy:

```powershell
node scripts/serve-release.cjs . 25173 28080
```

Mở http://127.0.0.1:25173; server local phục vụ frontend trong gói, SPA fallback và proxy `/api/v1` cùng origin. Công cụ này bind loopback, phục vụ kiểm tra/demo local; không phải đã nghiệm thu Nginx/TLS/Internet. Nginx mẫu giữ trong deployment/nginx.conf cho cấu hình riêng khi có hạ tầng.

## Backup/restore và dữ liệu demo

```powershell
node scripts/operations.cjs backup --env D:/language-center-config/backup.env --documents D:/language-center-data/documents --writes-paused --output D:/language-center-backups/backup-new
node scripts/operations.cjs verify --backup D:/language-center-backups/backup-new
node scripts/operations.cjs rehearse --env D:/language-center-config/backup.env --backup D:/language-center-backups/backup-new
```

Chỉ dùng --writes-paused sau khi đã dừng mọi writer của database đích; chỉ tắt gateway không đủ. Rehearse luôn tạo schema lc_rehearsal_* mới và khôi phục file riêng, kiểm fingerprint/row counts/file rồi chạy migration trong gói. Backup không mã hóa, chứa dữ liệu/tài khoản: giữ riêng và kiểm quyền truy cập, không đưa vào gói public. Online-rehearsal không là điểm phục hồi production.

Đợt G20 dùng bản sao demo đã dừng JVM để tạo backup nhất quán, không dừng/xóa DB người dùng. Qualification ghi schema, backup/restore/file hash và chứng cứ runtime thực. Tài khoản thử được giữ local trong private-session.json của lượt thử, không có mật khẩu trong tài liệu. Chỉ bốn role SUPER_ADMIN/ADMIN/STAFF/TEACHER, không có portal học viên/kế toán riêng.

## Diễn tập và giới hạn bản nộp

Kịch bản/ảnh/nội dung slide trong snapshot source là bản thảo kỹ thuật. Đọc qualification đi kèm để biết lượt nào đã chạy trên đúng JAR/frontend của gói, ngày báo cáo và ID demo. Không coi mở dữ liệu đã hoàn thành là đã làm lại từng thao tác tạo/thu trực tiếp.

Trước bảo vệ cần thử trên máy đích, kiểm Java/MySQL/Edge/cổng, thời lượng, cửa sổ riêng cho từng role, font/ảnh và phương án xem bằng chứng khi lỗi. Mẫu khoa, thông tin sinh viên/GVHD và khảo sát thực địa còn theo checklist bản thảo. Release local đạt không đồng nghĩa bản nộp hoặc production đã sẵn sàng.
