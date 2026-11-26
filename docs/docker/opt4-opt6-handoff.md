# OPT4–OPT6 — Vận hành và chốt bản local

Ngày 25/11/2026. Phạm vi bàn giao local; chưa phải cấu hình hosting production.

## OPT4: Theo dõi và backup

```powershell
node scripts/docker/observe.cjs
.\scripts\docker\lc.ps1 -Action status
.\scripts\docker\lc.ps1 -Action logs
```

Observation ghi CPU/RAM từng container, tỷ lệ lỗi và p50/p95 API vào `opt4-opt5-observation.json`. Đây là snapshot theo yêu cầu, chưa phải hệ thống cảnh báo liên tục. Gateway/backend đã có request ID để đối chiếu log. Không expose thêm actuator chứa thông tin cấu hình.

Đã đăng ký Windows Task Scheduler `LanguageCenter-DailyBackup` lúc 02:00, tài khoản hiện tại, quyền thường. Máy phải có phiên đăng nhập và Docker hoạt động. Task không chạy chồng; CLI cũng khóa thao tác backup/restore/update/rollback để ngăn tranh chấp. Log tại `storage/docker-backups/logs/`, dữ liệu SQL/tài liệu tại thư mục backup ignored như DCK6. Backup nhất quán có tạm dừng ứng dụng Docker rồi khởi động lại.

```powershell
# Chạy ngay một lần:
.\scripts\docker\scheduled-backup.ps1
# Xem trạng thái lịch:
Get-ScheduledTaskInfo -TaskName LanguageCenter-DailyBackup
# Đổi giờ, thêm bản sao ra ổ khác sau khi có đường dẫn thật:
.\scripts\docker\install-backup-task.ps1 -At 02:00 -CopyTo E:\LanguageCenterBackups
# Gỡ lịch:
.\scripts\docker\install-backup-task.ps1 -Remove
```

Đường dẫn E: chỉ là ví dụ. Người dùng chưa cung cấp nơi lưu ngoài máy; lịch hiện chỉ backup local. Tùy chọn CopyTo kiểm hash SQL, archive và manifest sau sao chép; cùng ổ hoặc cùng máy không bảo vệ được trường hợp mất máy. Chưa chạy thử sao chép ra thiết bị ngoài thực tế.

## OPT5: Quyết định cache

Đã đo 40 request cho mỗi endpoint, concurrency 4, có warm-up. Catalog Gateway p50 214 ms / p95 376 ms; Next BFF p50 208 ms / p95 284 ms. Không có lỗi trong 80 request. Chưa có dấu hiệu đủ để bổ sung Redis/cache; giữ catalog đọc dữ liệu mới để tránh sai hiển thị khi khóa đóng, lớp đầy hoặc chi nhánh ngừng hoạt động.

Chạy lại observation sau khi số liệu thay đổi. Đây không phải chứng nhận khả năng chịu tải. Nếu sau này cần cache, phải đo DB/request trước, giới hạn chỉ dữ liệu công khai và kiểm chứng invalidation khi chương trình/khóa/lớp/chi nhánh thay đổi. Chưa cache công nợ, số chỗ, quyền hoặc dữ liệu cá nhân.

## OPT6: Hồi quy và tài liệu

Đã chạy lại Chrome headless cho hai web ở 1440px/390px và kiểm tra chức năng/phân quyền qua Gateway. Bằng chứng tại `dck4-browser-validation.json` và `runtime-functional-validation.json`; ảnh chụp tại `docs/docker/screenshots/`. Kết quả thuộc lần chạy có timestamp trong báo cáo, không suy ra từ healthcheck.

Luồng demo: mở website xem khóa → đăng nhập web quản lý → xem học viên/lớp/lịch theo chi nhánh → kiểm tra teacher chỉ xem phạm vi của mình → student đọc ghi danh/học phí. Tài khoản demo còn dùng mật khẩu mẫu theo DCK5; đổi qua chức năng ứng dụng trước khi dùng ngoài môi trường demo.

Đã xuất schema hiện tại gồm 87 bảng, 944 cột, 32 FK vào `opt6-schema-metadata.json` và sơ đồ quan hệ vật lý `opt6-physical-erd.mmd`. Sơ đồ chỉ biểu diễn FK thật; không suy ra cardinality hoặc liên kết logic giữa service. Snapshot tài liệu ngày 08/11 thiếu 54 bảng được thêm sau đó; cần dùng bản mới này để cập nhật báo cáo/Class Diagram. Chạy lại bằng `node scripts/docker/document-schema.cjs`.

Chỉ mục tài liệu: DCK6 hướng dẫn chạy/backup/restore; OPT1–OPT3 bàn giao index/migration và tích hợp. Các thay đổi dữ liệu giữa Windows và Docker không tự đồng bộ.

Còn mở để nghiệm thu đầy đủ đề tài: E2E tạo/ghi dữ liệu qua toàn bộ web/mobile, thiết bị thật, provider thật, đối chiếu hoàn chỉnh UML/ERD với code và báo cáo khóa luận. Đợt này không viết lại tài liệu học thuật hoặc giả định các phần đó đã hoàn thành. Chưa tạo Git release/tag khi working tree vẫn có thay đổi của người dùng.
