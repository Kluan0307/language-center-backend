# G9 local qualification

Chỉ dùng database `lc_student_g9_<timestamp>` và cổng riêng. Push provider/VNPAY bị tắt trong runtime; device API dùng token cú pháp kiểm thử, không gửi qua Expo. Không sửa `.env.local` nguồn hoặc chạy migration trên database hiện tại qua bộ này.

Từ backend root, chạy tuần tự:

```powershell
node scripts/student-app/phase3/prepare.cjs --phase=9
mvn package
./scripts/student-app/phase3/runtime.ps1 -Action start -Phase 9
node scripts/student-app/phase4/live-api.cjs --phase=9
node scripts/student-app/phase5/live-api.cjs --phase=9
node scripts/student-app/phase6/live-api.cjs --phase=9
node scripts/student-app/phase9/live-api.cjs
./scripts/student-app/phase9/start-ui.ps1
node scripts/student-app/phase9/live-ui.cjs
node scripts/student-app/phase9/verify-sql.cjs
```

Dừng runtime/UI của run trước rồi chuẩn bị run mới trước khi seed/API lại. UI replay tự tạo sự kiện lịch mới bằng API và chờ worker commit; không reset readAt bằng SQL. G6 harness khi dùng phase9 kiểm Finance proof404 theo hợp đồng đã có từ G7. Không suy rằng fixture SQL đã kiểm toàn bộ writer học vụ.

Log app `typecheck`, `test`, Android export tại `storage/g9-app-typecheck.log`, `g9-app-tests.log`, `g9-android.log`; web lint/test/build tại `g9-web-*.log`. Summary kiểm các báo cáo/log và hash source/JAR:

```powershell
node scripts/student-app/phase9/summarize.cjs
./scripts/student-app/phase9/stop-ui.ps1
./scripts/student-app/phase3/runtime.ps1 -Action stop -Phase 9
```

Không chạy public Expo Push/VNPAY trong harness. Push protocol/permission/native app-killed vẫn cần credentials và thiết bị theo [hướng dẫn G9](../../../docs/student-app/phase-9-completion.md).
