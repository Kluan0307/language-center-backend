# G8 local protocol qualification

Không kết nối VNPAY thật. Chỉ dùng database `lc_student_g8_<timestamp>`, loopback ports28180–28184/29180 và keys sinh ngẫu nhiên trong storage; không dùng main database. Không đặt test credentials vào `.env.local`. `.env.local` chỉ cung cấp thông tin kết nối MySQL/JWT để tạo môi trường riêng.

Từ backend root, chạy tuần tự:

```powershell
node scripts/student-app/phase3/prepare.cjs --phase=8
mvn package
./scripts/student-app/phase3/runtime.ps1 -Action start -Phase 8
```

Console riêng, cùng root:

```powershell
node scripts/student-app/phase8/protocol-server.cjs
```

Console đầu:

```powershell
node scripts/student-app/phase4/live-api.cjs --phase=8
node scripts/student-app/phase7/live-api.cjs --phase=8
node scripts/student-app/phase8/live-api.cjs
./scripts/student-app/phase8/start-ui.ps1
node scripts/student-app/phase8/live-ui.cjs
node scripts/student-app/phase8/verify-sql.cjs
./scripts/student-app/phase8/stop-ui.ps1
./scripts/student-app/phase3/runtime.ps1 -Action stop -Phase 8
```

Dừng console protocol-server bằng Ctrl+C. Prepare run mới nếu chạy lại seeding/API; không áp dụng clean schema vào DB hiện tại. UI script có thể dùng đơn mới nếu đơn UI cũ đã kết thúc; ngân hàng mô phỏng trong bộ nhớ không tồn tại qua restart. Qualification UI dùng Edge CDP, không thay thiết bị native thật. Summary yêu cầu log typecheck/test/export app, lint/test/build web nằm trong `storage/g8-*.log`; chạy `node scripts/student-app/phase8/summarize.cjs` khi đã đủ evidence.

[Merchant/HTTPS và quy trình nghiệm thu thật](../../../docs/student-app/phase-8-completion.md).
