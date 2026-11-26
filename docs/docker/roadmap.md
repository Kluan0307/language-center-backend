# Docker — DCK1 đến DCK6

Ngày bắt đầu: **24/11/2026**. Phạm vi ưu tiên: local, giữ nguyên cách chạy Maven/Expo hiện tại. Backend root là thư mục chứa `pom.xml`, `Dockerfile` và `scripts/`.

**DCK1–DCK6 đã hoàn thành và kiểm chứng cho môi trường local.** [Báo cáo DCK1–DCK2](dck1-dck2-validation.json) · [Báo cáo DCK3](dck3-completion.json) · [Bàn giao DCK4](dck4-handoff.md) · [Bàn giao DCK5](dck5-handoff.md).

| Giai đoạn | Công việc | Đầu ra |
|---|---|---|
| DCK1 | Khảo sát service, runtime, cổng, cấu hình, health, storage và các ứng dụng gọi API | Manifest `deployment/docker/services.json`, mẫu `.env.docker.example`, tài liệu kiến trúc và hợp đồng cấu hình |
| DCK2 | Đóng gói backend bằng Maven/JDK 21 → JRE 21, user riêng, profile Docker và readiness | Một Dockerfile chung build thành sáu image độc lập; script build và kiểm chứng |
| DCK3 | Compose backend + MySQL: network, health/dependencies, volume, log, profile Assistant | Một lệnh chạy backend; schema được chuẩn bị trước khi khởi động các service `validate` |
| DCK4 | Ghép React/Nginx, Next standalone; cấu hình URL cho browser và app Student | Hai website kết nối gateway; mobile tiếp tục Expo/APK |
| DCK5 | Backup/restore dữ liệu vào MySQL Docker riêng, nghiệm thu luồng và phân quyền | Báo cáo chạy thật, dữ liệu và file liên kết đúng, có đường quay lại môi trường cũ |
| DCK6 | Script up/down/log/backup/restore và bàn giao sử dụng | Người khác có thể cài/chạy theo tài liệu; kiểm tra restart/cập nhật image |

Trạng thái đóng từng giai đoạn và kiểm chứng DCK1–DCK2: [README Docker](README.md), [báo cáo cấu hình/probe](contract-validation.json), báo cáo image khi có tại `image-validation.json`.

## Ranh giới giai đoạn

- DCK1–DCK2 không tạo Compose, không chuyển database đang dùng và không seed dữ liệu.
- DCK3 chuẩn bị MySQL/schema riêng; profile container giữ `ddl-auto=validate`, vì vậy database rỗng chưa có schema sẽ không chạy được bốn service dữ liệu.
- DCK5 mới phục hồi dữ liệu hiện có và nghiệm thu nghiệp vụ trên bộ container đầy đủ.
- DCK4 dùng lại Dockerfile Next standalone từ L10, thêm React/Nginx và fragment `compose.web.yaml` để chạy local cùng backend. Compose/Caddy riêng của website vẫn dành cho kịch bản hosting trước đó, không dùng cùng stack local này.

Compose wrapper toàn hệ thống đã đặt ở thư mục `language-center/` bên ngoài; include `compose.yaml` và `compose.web.yaml` trong backend. Hai web dùng cổng 15173/13000 và Gateway 18080, mặc định loopback. Các Dockerfile/profile và hướng dẫn backend được quản lý ngay trong repo backend hiện tại. 9 container healthy, phục hồi thành công 87 bảng và 8.886 bản ghi từ MySQL local; 17/17 tiêu chí nghiệm thu xác thực và phân quyền RBAC đạt 100%. Tiếp tục DCK6.

DCK6 đã kiểm chứng backup/restore, cập nhật/rollback hai web và tạo lại toàn stack; dữ liệu được giữ nguyên. [Bàn giao DCK6](dck6-handoff.md) · [Báo cáo tổng hợp](dck6-completion.json).
