# Bàn giao chuẩn hóa dữ liệu — G4, G5, G6

**Dữ liệu mẫu hiện tại (14/11/2026):** 4 chi nhánh, 400 học viên, 80 lead, 16 lớp; admin1 phụ trách CN01–CN02, admin2 phụ trách CN03–CN04. Mật khẩu mẫu đã đổi thành `123456`. [Tài khoản, phân công và số liệu](../demo/four-branches.md).

Cập nhật 14/11/2026. Tiếp nối [G1–G3](identity-g1-g3.md). Không thay đổi role hoặc mở thêm quyền bằng mã nhân sự.

## G4 — Vòng đời nhân sự và phân công

Ba trạng thái độc lập:

| Trường | Ý nghĩa | Khi ngừng sử dụng |
|---|---|---|
| Tài khoản `status` | Khóa/mở đăng nhập vì quản trị hoặc bảo mật | Không xóa hồ sơ hoặc lịch sử |
| Hồ sơ `workStatus` | `ACTIVE` đang làm; `INACTIVE` ngừng làm | Kiểm tra trách nhiệm, ghi ngày kết thúc và thu hồi phiên |
| Phân công `account_branches.status` | Còn làm tại chi nhánh này | Giữ thời điểm gán/thu hồi và các sự kiện lịch sử |

- Thêm `endDate` cho admin, staff, teacher; thêm optimistic version cho admin/staff. Teacher tiếp tục dùng version hiện có.
- Ngày vào làm không vượt hôm nay. INACTIVE phải có ngày kết thúc không vượt hôm nay và không trước ngày vào làm. ACTIVE không có ngày kết thúc.
- API tạo nhân sự INACTIVE không truyền ngày kết thúc sẽ dùng ngày tạo thao tác; dữ liệu INACTIVE cũ không biết ngày kết thúc **không được migration tự bịa ngày**. Bổ sung qua form trước khi sửa hồ sơ cần kiểm tra ngày.
- Admin/staff ngừng làm cần bàn giao lead đang chăm sóc. Giáo viên ngừng làm cần xử lý lớp/buổi chưa hoàn tất theo cơ chế có sẵn.
- Thay đổi hồ sơ làm việc và thay đổi phân công thực tế thu hồi phiên. Nhân sự ngừng làm không đăng nhập mới được; kích hoạt làm việc lại không tự mở tài khoản đang khóa.
- Không cho xếp lớp/lịch/buổi ngoài khoảng làm việc đã biết của giáo viên. Nhân sự INACTIVE không xuất hiện trong danh sách nhận lead mới.
- Đổi phân công không nhân bản hồ sơ hoặc đổi mã. Giữ nguyên giới hạn STAFF một chi nhánh của dự án; ADMIN/TEACHER có thể nhiều chi nhánh theo phân công.
- Gán/thu hồi có hiệu lực ngay theo thời điểm thao tác hiện có. Chưa thêm lịch hẹn phân công trong tương lai hoặc một hệ thống tính lương/hợp đồng.

### API bổ sung

| Method | Route | Quyền |
|---|---|---|
| GET | `/api/v1/auth/accounts/{id}/employment` | SUPER_ADMIN; ADMIN quản lý toàn bộ phân công của STAFF đích |
| PATCH | `/api/v1/auth/accounts/{id}/employment` | Như trên; cần version, workStatus, hireDate/endDate và lý do |
| GET | `/api/v1/auth/accounts/{id}/employment/history` | Như trên |

ADMIN không sửa hồ sơ ADMIN khác. TEACHER tiếp tục dùng `/teacher-profiles/{id}`, `/work-status`, `/history`; thêm trường `endDate`, không đổi ID profile. Nhật ký admin/staff lưu trước/sau, lý do, người sửa và thời điểm trong `employment_events`. Giáo viên giữ nhật ký `teacher_profile_events`.

## G5 — Hồ sơ học viên và bảo toàn lịch sử

- Quy tắc sản phẩm: học viên có ngày sinh xác định và **chưa đủ 18 tuổi** cần tên và số điện thoại người giám hộ. Đúng sinh nhật 18 tuổi thì không còn bắt buộc.
- Không đoán tuổi khi chưa biết ngày sinh; chuyển lead thành học viên vẫn để trống ngày sinh nếu chưa thu thập.
- Khi đã nhập một thông tin người giám hộ, phải nhập đủ thông tin còn lại; ngày sinh tương lai bị từ chối.
- Số điện thoại cần 6–15 chữ số, cho phép dấu định dạng thông dụng. Hai anh chị em được dùng chung số phụ huynh; không gộp hồ sơ theo điện thoại.
- Email tùy chọn; thông tin trống được chuẩn hóa về null. Chuyên môn, bằng cấp, chứng chỉ giáo viên giữ các trường riêng.
- Học viên có tài khoản, đăng ký, lead chuyển đổi, đánh giá đầu vào, ngoại lệ xếp lớp hoặc lịch sử hỗ trợ/học tập/thông báo bị chặn xóa vật lý. Đổi trạng thái theo nghiệp vụ thay vì xóa lịch sử. Học phí/phiếu thu bình thường gắn đăng ký, nên đã được bảo vệ bởi kiểm tra đăng ký.
- Không bổ sung API xóa nhân sự; dùng trạng thái làm việc và khóa tài khoản. ID/mã không được đánh lại.

## G6 — Giao diện, chatbot và dữ liệu mẫu

Web giữ Ant Design và cấu trúc trang hiện tại:

- Danh sách tài khoản hiển thị riêng trạng thái tài khoản và làm việc, ngày kết thúc trong chi tiết.
- Menu thao tác admin/staff có “Hồ sơ làm việc”: ngày vào làm, trạng thái, ngày kết thúc, lý do, version và lịch sử.
- Hồ sơ giáo viên hiển thị mã/ngày kết thúc; form ngừng làm cho chọn ngày kết thúc.
- Form học viên yêu cầu người giám hộ theo ngày sinh và kiểm tra thông tin đi đôi.
- Chatbot hiểu mã HV/GV/NS và tìm bằng keyword nguyên mã. Phần số của mã không phải ID. NS không tự xác định ADMIN hay STAFF; quyền API vẫn quyết định dữ liệu được xem. Chi tiết ngày vào làm/ngày kết thúc chỉ được đưa vào projection có quyền chi tiết.

Ví dụ để thử sau khi khởi động lại:

- “GV007 có bằng cấp gì?”
- “Thông tin NS001.”
- “HV0003 đang học lớp nào?”
- “Chi nhánh 1 có bao nhiêu giáo viên đang hoạt động?”
- Giáo viên: “Lịch dạy của tôi tuần này.”

Không fine-tune hoặc gọi Gemini thật trong đợt này. Nghiệm thu model thật R8 còn hạn chế provider như tài liệu R7–R8; kiểm thử host không đồng nghĩa đã đo độ chính xác tất cả câu hỏi qua Gemini.

### Bộ mẫu đã nạp

| Nhóm | Số lượng |
|---|---:|
| Chi nhánh / phòng | 2 / 6 |
| Tài khoản | 41 |
| Superadmin / admin / staff / teacher | 1 / 2 / 4 / 4 |
| Học viên | 30 |
| Chương trình / khóa học / lớp | 3 / 6 / 8 |
| Buổi học | 64 |
| Đăng ký và khoản học phí | 28 mỗi loại |
| Phiếu thu tiền mặt | 20 |
| Đợt công bố điểm | 2 |

Có lớp đang học/sắp mở; học viên chưa đăng ký, đang học, bảo lưu; người giám hộ; đánh giá đầu vào; lịch sử điểm danh, điểm và chăm sóc/hỗ trợ. Không giả lập giao dịch VNPAY, tài liệu có file nhưng không tồn tại, hoặc kết quả tốt nghiệp chưa được xác nhận.

Giáo viên teacher1 và teacher4 được phân công hai chi nhánh. teacher1 dạy lớp đang học ở hai cơ sở; teacher4 có lớp sắp mở. Hai học viên HV0001/HV0002 dùng chung số liên hệ. HV0013 bảo lưu; HV0015/HV0030 chưa đăng ký. HV0003/HV0018 là học viên chưa đủ 18 tuổi có giám hộ.

### Tài khoản mẫu

Mật khẩu chung cho **dữ liệu mẫu**: `Demo@123456`.

| Username | Role / phạm vi |
|---|---|
| `superadmin` | SUPER_ADMIN |
| `admin1` | ADMIN CN01 |
| `admin2` | ADMIN CN01, CN02; chi nhánh chính CN02 |
| `staff1`, `staff2` | STAFF CN01 |
| `staff3`, `staff4` | STAFF CN02 |
| `teacher1`, `teacher4` | TEACHER CN01, CN02 |
| `teacher2` | TEACHER CN01 |
| `teacher3` | TEACHER CN02 |
| `hv0001` … `hv0030` | STUDENT, liên kết đúng hồ sơ |

Dùng `hv0003` hoặc `hv0018` để xem điểm công bố và công nợ; `hv0001` để xem phiếu thu đã đóng đủ. Số tiếp theo do ứng dụng cấp là HV0031 và NS011/GV011 theo role tạo mới; ID kỹ thuật có dãy riêng.

### Migration và chạy lại fixture

Đã áp dụng [V020](../migrations/V020__employment_lifecycle.sql) tại database local. Máy khác cần chạy V019 rồi V020 trước khi dùng chế độ validate; dừng service ghi dữ liệu trong lúc migration/nạp mẫu. Không đổi `.env.local`, model, VNPAY hoặc mật khẩu database trong đợt này.

```powershell
# Chỉ kiểm tra schema/fixture trên database thử, không sửa database dự án:
node scripts/identity/load-demo.cjs

# Xác minh cả bộ mẫu đang có trong database dự án, không nạp lại:
node scripts/identity/load-demo.cjs --verify-local

# Chỉ dùng khi database dự án hoàn toàn trống:
node scripts/identity/load-demo.cjs --apply-local
```

Script từ chối ghi đè database có dữ liệu. Không chạy lại `--apply-local` sau khi đã nạp. [SQL fixture](../demo/seed-identity-full.sql) và [manifest](../demo/identity-demo-manifest.json) dùng ngày sinh/lịch/quan hệ rõ ràng. Khi cần bộ mẫu theo ngày hiện tại, `build-demo.cjs` tạo lại SQL/manifest; cần Java và thư viện BCrypt từ Maven cache. Không dùng các script reset/seed cũ để trộn vào bộ này.

Khởi động lại Auth, Academic, Teaching, Finance và Assistant nếu đang chạy bản cũ; gateway giữ cấu hình hiện có. Tải lại web và đăng nhập lại.

## Bằng chứng kiểm tra

- Auth: **95** ca đạt; Academic: **280** ca đạt; Assistant: **343** ca, **341 đạt và 2 live opt-in bỏ qua**.
- Web: **125** ca đạt; TypeScript/Vite build và lint các file thay đổi đạt.
- MySQL fixture: kiểm tra role–hồ sơ–chi nhánh, lịch trùng, ngày giảng dạy, số dư học phí/phiếu thu, điểm danh, ứng viên/điểm công bố và kế hoạch thời lượng.
- Bốn service thật, MySQL riêng: **23 kiểm tra đạt**, gồm đăng nhập theo role, phạm vi chi nhánh, hai chi nhánh của cùng giáo viên, quyền sở hữu học viên, điểm công bố và fingerprint, phiếu thu, nghỉ việc, version cũ và không nhận lead sau khi nghỉ. Không gọi nhà cung cấp AI/thanh toán.
- Tiến trình và database kiểm thử được dọn sau khi chạy. Không coi kiểm thử HTTP trực tiếp là kiểm thử trình duyệt hoặc APK.

Log/báo cáo: `storage/identity-g456-final-tests.log`, `storage/identity-g456-academic-final.log`, `storage/identity-g456-http-verification.json`, `storage/identity-g456-demo-verification.json`, `storage/identity-g456-web-tests.log`.
