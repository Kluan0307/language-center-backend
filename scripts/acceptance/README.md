# G17–G18: nghiệm thu ứng dụng thật

Xem [chuẩn bị, lệnh chạy, dữ liệu demo và giới hạn](../../deployment/acceptance-g17-g18.md).

```powershell
.\scripts\acceptance\run.ps1 -Documents storage/improvement6-documents
```

Chạy tại backend root sau khi build JAR và cài dependency frontend. Luôn tạo schema/file riêng; không reset dữ liệu nguồn. Mỗi phase lưu expected/actual thật. `summarize.mjs` yêu cầu đủ86 kiểm tra đạt; báo cáo và tài khoản thử nằm trong `storage/acceptance/<runId>` (ignored). Không chia sẻ private-session hoặc SQL dump.
