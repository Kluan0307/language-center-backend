# Bộ demo nghiệp vụ — giai đoạn 4

Tạo **schema riêng** tên `lc_rehearsal_demo_g4_*`; không xóa, reset hay migrate database gốc. Dữ liệu nghiệp vụ được tạo qua gateway/API thật; chỉ cấu trúc bảng được sao chép và migrate. Super Admin được ứng dụng bootstrap một lần trước khi chạy profile prod/validate; hai chi nhánh khởi tạo bằng BranchSeed hiện có.

## Tạo lại bằng một lệnh

Từ backend root, sau khi cài dependency frontend:

```powershell
.\scripts\demo-phase4\run.ps1
```

Lệnh đóng gói backend (skipTests), tạo schema mới, áp dụng prepare-existing-schema + V001–V013, bootstrap, khởi động riêng 5 service và FE, tạo dữ liệu qua API, đối soát MySQL/UI, rồi dừng các tiến trình riêng. Dữ liệu/evidence vẫn được giữ. Có thể dùng `-SkipBuild` khi JAR hiện tại vừa được đóng gói và chưa đổi Java.

Workshop kết thúc dùng buổi **00:00–03:00 của ngày chạy** để thao tác điểm danh qua nghiệp vụ thật; chạy sau 03:00 giờ Việt Nam. Đây là bộ minh họa ngắn, không phải tuyên bố hoàn thành hoặc cấp chứng nhận CEFR qua ba giờ học. Các lớp tương lai chạy trong bốn tuần, tám buổi90phút.

Các cổng riêng: gateway28080, auth28081, academic28082, teaching28083, finance28084, FE25173, CDP29388. Nếu cổng có tiến trình khác, script dừng báo lỗi; không tắt tiến trình đó.

## Mở dữ liệu đã tạo, không tạo lại

```powershell
.\scripts\demo-phase4\open.ps1
```

Mở `http://127.0.0.1:25173`. Tài khoản ngắn: `superadmin`, `admin`, `staff`, `teacher`, `teacher2`, `staff2`. Mật khẩu được sinh riêng; xem `bootstrap.private.json` cho Super Admin và `private-session.json` cho các tài khoản còn lại tại thư mục được in ra. Không ghi mật khẩu vào tài liệu công khai.

Dừng demo:

```powershell
.\scripts\demo-phase4\open.ps1 -Action stop
```

Lượt mới nhất nằm trong `storage/demo-phase4-latest.txt`. Script chỉ quản lý runtime dưới `storage/acceptance` và database có prefix demo; không đổi `.env.local`.

## Dữ liệu và kịch bản

- 2 chi nhánh;6 tài khoản;3 chương trình;8 trình độ;4 khóa/lớp;3 phòng.
-10 học viên;8 đăng ký/học phí;25 buổi học;4 phiếu thu. ID bắt đầu nhỏ theo database mới, không tái đánh số hồ sơ đang dùng.
- **Ôn tập B1:** Minh Anh đủ đầu vào B1; Quốc Bảo A2 cần Admin duyệt ngoại lệ. Hai người được điểm danh đủ180phút; điểm lần đầu85/40 với ngưỡng60%, nên một đạt và một chưa đạt. Đã chốt, hoàn tất, đóng lớp; sau đó mở lại đúng thứ tự, sửa85→86 có audit, xác nhận lại và đóng lớp. Quốc Bảo vẫn còn nợ3triệu.
- **Giao tiếp A2:** yêu cầu A1; Thu Hà/Hoàng Nam đủ điều kiện. Minh Khang thiếu đánh giá, Minh Anh sai mức và Bảo Ngọc có đánh giá khác chương trình đều bị chặn ghi danh trực tiếp. Có ngoại lệ bị từ chối và yêu cầu để lại chờ duyệt.
- **IELTS A / TOEIC A:** lớp cơ bản OPEN_ENTRY, có học viên và lịch tuần ở hai chi nhánh; giữ chưa hoàn tất để xem lịch và thao tác thủ công.
- Thu Hà đóng1triệu/còn2triệu; Hoàng Nam đóng đủ3triệu; các khoản chưa đóng được giữ. Tổng toàn hệ thống: phải thu25triệu, thực thu7triệu, còn18triệu.
- Vào lịch và chọn tuần bắt đầu ở `nextMonday` trong `phase4-summary.json`; ở ngày tạo, lịch mặc định có thể chỉ thấy workshop đã hoàn tất.

Kết quả mong đợi định nghĩa trong `expected.json` và trước từng assertion:40 kiểm tra API,8 kiểm tra SQL/source,4 lượt đọc UI trên backend/MySQL thật. Đây là kiểm tra bộ fixture giai đoạn4, chưa thay thế toàn bộ regression giai đoạn5. Browser dùng phiên xác thực qua API, không gọi là kiểm thử đăng nhập UI.

## Bằng chứng và giới hạn

`phase4-summary.json`, `g4-core.json`, `g4-details.json`, `database-proof.json`, `browser-demo.json`, ảnh `demo-*.png`, `http.jsonl` và source-before/after được lưu trong run directory. Không chia sẻ file private/token hoặc SQL dump.

Đã phát hiện schema nguồn cần đối chiếu migration V011: cột legacy `courses.duration` còn NOT NULL có thể chặn tạo khóa chỉ có expectedMinutes. Bộ demo áp dụng migration đầy đủ trên bản riêng; script không sửa nguồn. Cần backup và chuẩn hóa migration nguồn trong đợt triển khai tiếp theo.
