# Diễn tập và mở demo bản release đã phục hồi

Chạy từ backend root. Bộ G20 dùng frontend **build tĩnh trong release** và năm JAR cùng gói, không dùng Vite/source FE để thay bằng chứng release.

```powershell
$demoRun = (Get-Content storage/g20-restored-run.txt -Raw).Trim()
.\scripts\release-rehearsal\runtime.ps1 -Action start -Service all -RunDir $demoRun
node scripts/operations.cjs smoke --base-port 28080
```

Đợi đủ 5 service rồi chạy smoke; nếu chưa ready thì chờ log khởi động xong và chạy lại smoke. Mở **http://127.0.0.1:25173**. Script không thay `.env.local` hay database gốc; cần giữ cấu hình hiện tại, release và thư mục backup/file của lượt thử. Có thể xem runtime.json để biết release/database nào được dùng.

Tài khoản mới của bản sao nằm trong `$demoRun/private-session.json`, nhóm accounts.admin/staff/teacher và multiAdmin1 (Admin hai chi nhánh của lượt đã kiểm). Super Admin dùng thông tin bản sao nguồn. Chỉ đọc thông tin này local; không chiếu token/mật khẩu khi trình bày. Dữ liệu/ID chính nằm ở fixture.json; không dùng tên tài khoản của fixture mô phỏng cũ.

## Kịch bản trình bày

1. **Super Admin:** đăng nhập → Chi nhánh → tìm bằng fixture.prefix. Chỉ rõ chi nhánh A/B và phạm vi phân công; không đổi cấu hình hoặc xóa dữ liệu khi demo.
2. **Staff:** mở Student A, xem hồ sơ học tập/khoảng học/điểm danh và summary Đạt; mở học phí A có 2 phiếu 1M+2M, đã đóng đủ. Student B đã COMPLETED nhưng Chưa đạt chính thức và còn nợ 3M; đây là ba trạng thái khác nhau.
3. **Teacher:** dùng **Lịch dạy của tôi `/my-schedule`**, không `/schedule` của nhân viên; xem buổi đã điểm danh. Vào bảng điểm/kết quả của lớp chính, chỉ đọc final, không có nút chốt/mở lại.
4. **Admin:** mở lịch sử snapshot/ngưỡng60%, refs COMPLETED hiện hành và trạng thái đóng lớp có kiểm toán. Báo cáo → Tài chính, scope chi nhánh A: thu gốc/ròng 3M, đảo/hoàn0, nợ hiện tại3M. Bộ thử sinh ngày10/11/2026: chọn kỳ chứa ngày đó khi demo vào ngày khác.
5. Có thể dùng Admin nhiều chi nhánh chuyển A→B; chỉ người được phân công B mới có lựa chọn. Chuyển scope không cấp thêm quyền; 86ca nghiệm thu có giữ response A để kiểm dữ liệu cũ không ghi đè B.

Đây là trình bày dữ liệu đã nghiệm thu. Không bấm thu lại trên PAID hoặc tạo lại ngoại lệ CONSUMED. Nếu cần sửa nguồn trực tiếp: reopen final toàn lớp → mở lớp nếu đóng → sửa theo quyền → preview/chốt lại từng enrollment → đồng bộ refs COMPLETED → close. Kịch bản thay đổi trạng thái phải diễn tập riêng trên bản sao mới trước buổi bảo vệ.

## Trước buổi bảo vệ thật

- [ ] Dùng đúng máy/màn hình/mạng dự kiến, kiểm Java21/MySQL8/Node22+/Edge, cổng và quyền chạy PowerShell.
- [ ] Mở demo từ release và kiểm manifest; đảm bảo file documents/backup/config riêng còn đủ. Không copy riêng SQL mà bỏ file revision.
- [ ] Dùng cửa sổ/profile riêng khi cần giữ nhiều role đồng thời; diễn tập tự đăng nhập/CAPTCHA, không chiếu private-session.
- [ ] Người trình bày thực hành lời thoại và thời gian15–20phút theo quy định khoa. Các bước tự động đã kiểm chức năng không thay việc đo thời gian nói.
- [ ] Chuẩn bị ảnh/báo cáo expected/actual làm phương án khi lỗi; nói rõ đó là bằng chứng lưu, không giả là phản hồi đang chạy.
- [ ] Hoàn thiện mẫu khoa, thông tin sinh viên/GVHD, khảo sát và dàn trang bản nộp. Không tự thêm số liệu khảo sát hoặc SLA chưa đo.

Kết thúc:

```powershell
.\scripts\release-rehearsal\runtime.ps1 -Action stop -Service all -RunDir $demoRun
```

Các tiến trình thử được dừng sau nghiệm thu; schema/file/backup giữ nguyên. Internet/TLS/Nginx/load và máy bảo vệ chưa được chứng nhận bằng diễn tập local này. Xem [G20](../docs/thesis-progress/G20.md), [kịch bản và lời dẫn](../docs/thesis/report/kich-ban-demo.md).
