# DCK6 — Vận hành Docker local

Ngày cập nhật: **25/11/2026**. Backend, MySQL, web quản lý và website chạy cùng Compose; app Student tiếp tục Expo/APK. Dữ liệu DCK5 đã được chuyển sang MySQL Docker: 419 tài khoản, 400 học viên, 20 khóa, 40 lớp. MySQL trên Windows vẫn là môi trường riêng.

## Lệnh dùng hằng ngày

Từ backend root (thư mục có `pom.xml`, `compose.yaml`, `scripts/`):

```powershell
# Docker Desktop phải mở, dùng Linux containers.
.\scripts\docker\lc.ps1 -Action doctor
.\scripts\docker\lc.ps1 -Action up
.\scripts\docker\lc.ps1 -Action status
.\scripts\docker\lc.ps1 -Action logs

# Dừng nhưng giữ container, hoặc xóa container/network và giữ volume.
.\scripts\docker\lc.ps1 -Action stop
.\scripts\docker\lc.ps1 -Action down
```

`up` mặc định chạy cả hai web và Assistant; AI chỉ hoạt động khi cấu hình provider/flag riêng. `ps` xem bảng trạng thái; `status` kiểm tra readiness, số bản ghi và URL thực tế, không in secret. Script nhận đường dẫn từ vị trí của chính nó nên có thể gọi bằng đường dẫn đầy đủ từ thư mục khác.

| Ứng dụng | URL mặc định |
|---|---|
| Web quản lý | http://localhost:15173 |
| Website | http://localhost:13000 |
| API Gateway | http://localhost:18080 |

`down` giữ `language-center_mysql_data` và `language-center_documents`. Không dùng `down -v` để dừng thông thường. Các script không xóa volume của stack chính.

## Cài mới từ các repo

Đặt backend, `language-center-web-frontend` và `language-center-website` cùng cấp như cấu trúc hiện tại, cài Node.js và Docker Desktop. Từ backend root:

```powershell
node scripts/docker/init-env.cjs
node scripts/docker/configure-web.cjs
.\scripts\docker\lc.ps1 -Action up -Build
```

Env Docker nằm tại `language-center/.env.docker.local`, được ignore. Lần đầu trên máy mới tạo schema rỗng; muốn có dữ liệu phải phục hồi backup hoặc chạy quy trình migration local DCK5 **khi đích vẫn rỗng**. Không chạy `migrate-local.cjs` để đồng bộ lặp lại dữ liệu đã dùng.

## Backup database và tài liệu

```powershell
.\scripts\docker\lc.ps1 -Action backup
.\scripts\docker\lc.ps1 -Action backups
```

Lệnh in mã như `ops-<timestamp>-<suffix>`. Backup được lưu trong thư mục ignored `storage/docker-backups/<mã>/`:

- `database.sql`: schema và dữ liệu ứng dụng, không dump user/grant hệ thống MySQL.
- `documents.tar`: nội dung volume tài liệu.
- `backup.json`: hash SQL/archive, số bảng/dòng, hash từng bảng/file, image ID và danh sách service đang chạy.

Để DB và file nhất quán, script tạm dừng các ứng dụng Docker đang chạy, giữ MySQL hoạt động; dùng snapshot InnoDB và so sánh hash trước/sau backup. Sau đó khởi động lại đúng các service đã dừng. Không dừng MySQL hoặc backend Maven của Windows.

Backup không chứa env/key provider; lưu riêng `.env.docker.local` ở nơi riêng tư khi chuyển máy. SQL chứa thông tin và password hash của tài khoản, không đưa vào Git hoặc báo cáo công khai. Công cụ hiện dành cho bộ dữ liệu local, giới hạn output mỗi lệnh 128 MiB; vượt giới hạn sẽ báo lỗi, không tạo backup hoàn chỉnh để restore.

## Thử phục hồi trước

Thay `<BACKUP_ID>` bằng mã được in khi backup:

```powershell
.\scripts\docker\lc.ps1 -Action rehearse -BackupId <BACKUP_ID>
```

Script kiểm checksum, phục hồi vào database `lc_dck6_restore_<timestamp>` và volume `lc-dck6-restore-<timestamp>_documents`, đối chiếu hash toàn bộ dữ liệu/file rồi dọn hai vùng tạm. Database và volume chính không bị ghi. Archive bị lỗi checksum, chứa đường dẫn vượt thư mục, symlink hoặc special file sẽ bị từ chối.

## Phục hồi stack chính

```powershell
.\scripts\docker\lc.ps1 -Action restore -BackupId <BACKUP_ID> -ReplaceData
```

Đây là thao tác thay dữ liệu **MySQL Docker và volume tài liệu**, không ghi database Windows. Cần chỉ định rõ mã backup và `-ReplaceData`. Trước khi thay dữ liệu, script tự thử phục hồi cô lập, tạo một safety backup mới, dừng ứng dụng rồi phục hồi DB/file và kiểm hash.

Nếu phục hồi/đối chiếu thất bại, script cố khôi phục safety backup; nếu bước đó cũng thất bại, giữ ứng dụng dừng và in mã backup để phục hồi thủ công. Chỉ khởi động lại sau khi dữ liệu hợp lệ. Nếu dữ liệu hợp lệ nhưng ứng dụng không healthy do image/schema không tương thích, giữ nguyên bộ dữ liệu đã phục hồi và xử lý phiên bản image phù hợp; không giả định restore dữ liệu là rollback image.

Backup chỉ hỗ trợ schema ứng dụng InnoDB không có routine/trigger/event/view. Công cụ sẽ từ chối schema ngoài phạm vi này để tránh bỏ sót object. Chỉ dùng backup do bộ công cụ tạo và đã kiểm chứng.

## Cập nhật và quay lại image

```powershell
# Cập nhật riêng web quản lý, hoặc nhiều service.
.\scripts\docker\lc.ps1 -Action update -Service web-admin
.\scripts\docker\lc.ps1 -Action update -Service web-admin,website

# Không chỉ định Service: cập nhật các application service đang chạy.
.\scripts\docker\lc.ps1 -Action update
```

`update` tạo backup DB/file trước, giữ image cũ bằng tag `rollback-<BACKUP_ID>`, build từ Compose hiện tại, tạo lại service đã chọn và đợi healthy. Build thất bại không thay container đang chạy; deploy thất bại sẽ khôi phục tag cũ và chạy lại. MySQL bị loại khỏi danh sách cập nhật tự động vì nâng phiên bản DB cần kế hoạch riêng.

Từ CL3, khi cập nhật/rollback backend, công cụ cũng khởi động lại các caller đang chạy theo manifest và website để tránh giữ IP/DNS cũ của container. Bước này có thể làm ngắt API ngắn trong lúc các service sẵn sàng lại.

```powershell
.\scripts\docker\lc.ps1 -Action rollback -BackupId <BACKUP_ID_CUA_LAN_UPDATE>
```

`rollback` dùng `release.json` của lần update, quay lại image cũ; không khôi phục dữ liệu. Khi bản code có thay đổi schema, cần dùng migration/restore phù hợp. Hibernate Docker vẫn `validate`, không tự cập nhật schema. Không prune image rollback/volume trước khi xác nhận bản mới ổn.

## Kiểm tra và bằng chứng

```powershell
# Kiểm tra chức năng trên dữ liệu mẫu đã chuyển; có cập nhật last_login ở Docker.
.\scripts\docker\lc.ps1 -Action verify

# Backup + phục hồi cô lập, từ chối dữ liệu lỗi, hash toàn bộ DB/file.
node scripts/docker/verify-operations.cjs

# Build/deploy/rollback hai web, down/up toàn stack, đối chiếu dữ liệu.
node scripts/docker/verify-lifecycle.cjs

# Smoke cấu hình/proxy, dùng được cả khi DB đã có dữ liệu.
node scripts/docker/verify-main.cjs
node scripts/docker/verify-web.cjs
```

`verify` dùng các tài khoản demo còn giữ từ DCK5 với mật khẩu mẫu; nếu đổi mật khẩu hoặc xóa demo, cập nhật cấu hình kiểm thử trước khi chạy. Phần kiểm tra lịch giáo viên/catalog yêu cầu HTTP 200 và cấu trúc response thực, không tính 404 là thành công. Báo cáo hiện hành không ghi đè bằng chứng lịch sử database rỗng của DCK3/DCK4.

Kết quả: [backup/restore](dck6-operations-validation.json), [update/restart](dck6-lifecycle-validation.json), [chức năng và phân quyền hiện hành](runtime-functional-validation.json), [trạng thái đang chạy](runtime-status.json). Hai script tích hợp có thể tạm ngắt các ứng dụng Docker trong lúc kiểm thử; không chạy song song hoặc trong lúc đang nhập liệu.

## Môi trường local và tích hợp ngoài

- Dữ liệu MySQL Windows và Docker là hai bản độc lập; các thay đổi phát sinh sau DCK5 không tự đồng bộ.
- VNPAY, push và Gemini không tự sao chép key từ `.env.local`. Cấu hình Docker hiện tắt các tích hợp này; healthy không có nghĩa nhà cung cấp ngoài đã hoạt động.
- Mobile dùng Gateway Docker cổng 18080; điện thoại thật cần IP LAN/bind/firewall phù hợp, xem [DCK4](dck4-handoff.md).
- Đây là bàn giao chạy local. Chưa triển khai hosting/TLS, backup định kỳ/off-machine, kiểm thử thiết bị thật hoặc gọi provider thật trong DCK6.


## Kết quả nghiệm thu DCK6
Đã đạt 13 kiểm tra backup/restore, 8 kiểm tra update/rollback/restart, 23 kiểm tra chức năng/phân quyền và 38 kiểm tra HTTP/web. Cả 9 container healthy sau nghiệm thu; dữ liệu ứng dụng vẫn đầy đủ. [Báo cáo tổng hợp](dck6-completion.json).
