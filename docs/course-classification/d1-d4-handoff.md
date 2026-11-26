# D1–D4 — Đã làm mới dữ liệu local

Cập nhật sau đó ngày 17/11/2026: **D5–D8 đã hoàn thành phần local**, website đã xuất bản dữ liệu và cập nhật menu. Xem [bàn giao mới](d5-d8-handoff.md). Các mục “chờ D5” bên dưới ghi lại trạng thái tại thời điểm đóng D4.

Đã thực hiện tuần tự D1 → D2 → D3 → D4 theo yêu cầu ngày 16/11/2026, trên `language_center_db` ở localhost. Bộ dữ liệu mới lấy ngày 16/11/2026, múi giờ Asia/Ho_Chi_Minh làm mốc lịch. Chưa thực hiện D5–D8.

## D1 — Phạm vi đã áp dụng

- Kiểm kê 87 bảng: giữ 21 bảng, bổ sung 2 bảng, làm mới 64 bảng nghiệp vụ/phụ trợ. Danh sách từng bảng và số bản ghi cũ: `d1-data-plan.json`.
- Giữ nguyên **419 tài khoản**, mật khẩu, trạng thái tài khoản, role, mã nhân sự, hồ sơ nhân sự, bốn chi nhánh, memberships và lịch sử phân công; giữ cơ cấu công ty, hỗ trợ nội bộ, cài đặt cá nhân và danh mục chuẩn.
- Giữ bốn chương trình GE/COMMUNICATION/IELTS/TOEIC, năm nhóm đối tượng và bảy trình độ cũ; bổ sung tám trình độ phù hợp với khóa mới. Bộ sinh mã học viên tiếp tục tăng.
- Xóa dữ liệu học viên, tuyển sinh, khóa/lớp, lịch, điểm, thu phí và các dữ liệu phụ thuộc cũ. Các tài khoản STUDENT được liên kết lại bằng **account_id**, không bằng tên người.
- Xóa lịch sử phiên/bảo mật mẫu, thiết bị push, thông báo và metadata website cũ thuộc phạm vi làm mới; tăng session_version của mọi tài khoản. Không thay đổi khóa JWT, VNPAY hay biến môi trường.

## D2 — Backup và phục hồi thử

- Full backup chứa schema và dữ liệu nằm trong `storage/data-refresh/`, được Git bỏ qua vì có thông tin tài khoản. Đường dẫn cụ thể và SHA-256 nằm ở file local `storage/data-refresh/backup.json`; có thêm bản backup ngay trước apply.
- Đã phục hồi backup vào database tạm, đối chiếu số bản ghi cả 87 bảng và fingerprint các bảng được giữ.
- Cấu trúc user table được giữ nguyên khi làm mới local: **không TRUNCATE, không DROP/ALTER bảng**. DELETE theo thứ tự khóa ngoại, INSERT và các assertion chạy chung một transaction InnoDB.
- Chế độ mặc định của `refresh-run.cjs` chỉ diễn tập trên database riêng. Muốn thay local phải truyền `--apply-local`, có backup và rehearsal đúng checksum, nguồn chưa thay đổi, cùng precondition phiên tài khoản và khóa thực thi độc quyền.
- Đã cố ý gây lỗi CHECK ngay trước COMMIT trên database thử và đối chiếu lại toàn bộ số bản ghi cũ: rollback thành công. Thông báo CHECK failure trong bước probe là lỗi được chủ động tạo để thử cơ chế này.
- Không có service Java của dự án đang chạy lúc apply. Không gửi thông báo, push, email hoặc yêu cầu thanh toán ra bên ngoài.

## D3 — Bộ dữ liệu mới

| Dữ liệu | Số lượng |
|---|---:|
| Tài khoản giữ nguyên | 419 |
| Chi nhánh giữ nguyên | 4 |
| Chương trình / nhóm đối tượng | 4 / 5 |
| Trình độ sau bổ sung | 15 |
| Khóa học | 20 |
| Gán khóa–đối tượng | 33 |
| Phòng học | 12 |
| Lớp học | 40 |
| Lịch học định kỳ / buổi học | 80 / 320 |
| Học viên | 400 |
| Ghi danh / học phí | 432 / 432 |
| Phiếu thu | 298 |
| Đánh giá đầu vào xác nhận | 144 |
| Bản ghi điểm danh | 1.856 |
| Bài kiểm tra / lần công bố điểm | 36 / 36 |
| Điểm / điểm công bố cho học viên | 432 / 432 |
| Lead | 80 |
| Yêu cầu hỗ trợ học viên | 8 |
| Thông báo trong inbox | 400 |
| Bản xuất bản website / giao dịch VNPAY được seed | 0 / 0 |

Mỗi chi nhánh có 100 học viên, 20 lead, 3 phòng và 10 lớp: 8 đang học, 1 đã kết thúc lịch, 1 sắp mở. Có 100 ghi danh hiện tại và 8 lượt học trước đây mỗi chi nhánh. Các lớp đã kết thúc lịch **đang chờ trung tâm chốt kết quả**; không dựng kết quả tốt nghiệp hoặc hoàn thành giả.

20 khóa gồm 16 khóa chung và 4 khóa giao tiếp riêng chi nhánh. Khóa chung chỉ được tính một lần khi thống kê toàn hệ thống. Không tạo bản sao khóa theo từng đối tượng.

Ví dụ: MG1/MG2 cho Mẫu giáo, TH1/TH2 cho Tiểu học, CS1 cho THCS/THPT/Người lớn, GT1/GT2 cho giao tiếp, IL1–IL4 cho IELTS, TC1–TC4 cho TOEIC, GT-CN01–GT-CN04 cho giao tiếp tại từng cơ sở. Ma trận đầy đủ: `d3-seed-manifest.json`.

Khóa mới có program_id, audience_groups, classification_revision và policy đúng chương trình. `type` của tất cả khóa mới để NULL, cột cũ vẫn giữ trong schema. Nội dung và các điểm IELTS/TOEIC trong bộ mẫu là kiểm tra nội bộ, không phải chứng chỉ thi thật.

ID tài nguyên mới đều vượt MAX(id) của dữ liệu đã xóa. Ví dụ học viên cũ 1–400 được thay bằng hồ sơ 401–800; mã hiển thị **HV0401–HV0800**. Không ép reset AUTO_INCREMENT về 1; các bộ sinh ID/mã đã kiểm tra tiếp tục tăng hợp lệ.

### Tài khoản học viên dễ thử

| Username giữ nguyên | Hồ sơ mới | Mã học viên | Lớp hiện tại |
|---|---:|---|---|
| student1 | 401 | HV0401 | CN01-L01 — Mẫu giáo 1 |
| student2 | 402 | HV0402 | CN01-L02 — Tiểu học 1 |
| student3 | 403 | HV0403 | CN01-L03 — Tiếng Anh A1 |
| student4 | 404 | HV0404 | CN01-L04 — Giao tiếp cơ bản |
| student5 | 405 | HV0405 | CN01-L05 — IELTS 5.5; có thêm lượt học IELTS nền tảng trước đó |

Mật khẩu giữ nguyên như trước; đợt này không đặt lại mật khẩu. Username và mã học viên là hai định danh khác nhau. Toàn bộ ánh xạ 400 account → hồ sơ mới nằm trong manifest private ở `storage/data-refresh/manifest.json`.

## D4 — Đã áp dụng và đối soát

- Backup và rehearsal thành công trước khi thay local.
- 64 kiểm tra trên database thật đều đạt: FK, phân loại, snapshot, role/account/profile, chi nhánh, lịch phòng/giáo viên/học viên, sĩ số, thời lượng, đầu vào, periods, điểm danh, điểm công bố, học phí, phiếu thu, lead, thông báo và phạm vi website.
- Fingerprint chứng minh tài khoản/mật khẩu và các bảng giữ lại không bị đổi ngoài session_version/last_login đã xác định trước. Mọi phiên cũ đã bị thu hồi; cần đăng nhập lại.
- Audit phân loại mới: 20/20 khóa đủ chương trình và đối tượng; 10 khóa nhiều đối tượng; không mồ côi, trùng nhóm hoặc lệch chương trình. Vấn đề 16 khóa cũ thiếu đối tượng tại G8 đã được thay bằng bộ dữ liệu mới, không phải suy đoán lại từ `type` cũ.
- Mọi cột của 87 bảng được đối chiếu với inventory ban đầu; không thay cấu trúc. Counter tự tăng đã kiểm tra khớp, không để lại database diễn tập.
- Không có gọi model AI, thanh toán VNPAY hoặc kiểm thử UI/API trực tiếp trong D4. Các kiểm chứng đợt này là thao tác MySQL và đối chiếu dữ liệu/schema.

## Cách dùng và bước tiếp theo

Khởi động các backend như thường lệ, reload web/app và đăng nhập lại. Link lớp/học viên đang mở trước khi reset dùng ID cũ, cần mở lại từ danh sách mới. Không tiếp tục dùng token cũ.

**Website hiện chưa có dữ liệu xuất bản.** Đây là ranh giới D4. D5 sẽ soạn nội dung và xuất bản chương trình → chi nhánh → khóa → lớp tuyển sinh; D6 mới cập nhật menu theo nhóm. Không chèn dữ liệu công khai giả chỉ để tránh trang trống.

Để kiểm tra lại mà không làm mới dữ liệu:

```powershell
node scripts\identity\audit-course-classification.cjs --d4
node scripts\identity\refresh-final-audit.cjs
```

Hai script trên chỉ đọc MySQL và ghi báo cáo local. `refresh-final-audit.cjs` đối chiếu bộ mẫu ở ngày nghiệm thu; sau khi người dùng sửa/xóa hoặc phát sinh học phí, lịch, xuất bản mới, một số kỳ vọng seed có thể không còn đúng. Đây không phải công cụ theo dõi production tổng quát.

Không chạy lại `refresh-plan.cjs`/`refresh-backup.cjs` để “kiểm tra”: đây là các bước lập một đợt thay dữ liệu mới. Công cụ apply từ chối lặp lại bộ seed đã chạy vì fingerprint và session precondition đã thay đổi; không tự cộng dồn bộ mẫu.

Nếu cần phục hồi, dừng các service ghi dữ liệu, dùng đúng backup private đã kiểm SHA và phục hồi thử. Việc phục hồi toàn bộ phải đi kèm thu hồi phiên để token của bộ dữ liệu mới không được tái sử dụng. Không lấy SQL bộ mẫu cũ chạy chồng lên dữ liệu hiện tại.

### Báo cáo

- `d1-data-plan.json`: phạm vi từng bảng.
- `d2-backup-report.json`: backup và restore verification.
- `d3-seed-manifest.json`, `d3-rehearsal-report.json`: bộ mẫu và rehearsal/rollback probe.
- `d4-apply-report.json`, `d4-classification-audit.json`, `d4-final-audit.json`: kết quả sau apply.
