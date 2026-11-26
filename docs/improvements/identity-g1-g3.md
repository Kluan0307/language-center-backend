# Chuẩn hóa mã và danh tính — G1, G2, G3

**Cập nhật G4–G6 (14/11/2026):** vòng đời nhân sự, ngày kết thúc, người giám hộ, bảo toàn lịch sử và bộ mẫu đã nạp. [Bàn giao, tài khoản mẫu và cách chạy](identity-g4-g6.md).

Cập nhật 14/11/2026. Phạm vi: mã ngắn, hồ sơ–tài khoản–role, kiểm soát khởi tạo dữ liệu. Chưa triển khai G4–G6 về vòng đời nhân sự, ngày kết thúc, bộ mẫu đầy đủ.

## G1 — Đối chiếu cấu trúc hiện tại

| Dữ liệu | Trước cập nhật | Quyết định |
|---|---|---|
| Khóa chính | `Long`, IDENTITY | Giữ nguyên; không đánh lại ID hoặc reset bộ đếm |
| Học viên | `students.student_code`, unique, sinh `HV-UUID` | Cấp `HV0001`; không đổi mã khi sửa hồ sơ |
| Admin, staff, teacher | Hồ sơ riêng, `account_id` unique trong từng bảng | Giữ bảng; mã dùng chung tại `accounts.personnel_code` |
| Phân công chi nhánh | `account_branches`, trạng thái, thời gian gán/thu hồi và lịch sử | Tận dụng, không thêm bản sao hồ sơ theo chi nhánh |
| Trạng thái | Tài khoản `status`; nhân sự `workStatus`; phân công `status` | Giữ các ý nghĩa riêng, chưa thay đổi hợp đồng ở G1–G3 |
| Giáo viên | Có chuyên môn, bằng cấp, chứng chỉ, ngày vào làm | Không thêm cột trùng nghĩa |
| Học viên | Có người giám hộ, người liên hệ | Kiểm tra điều kiện theo tuổi ở G5 |
| Bootstrap | Superadmin đã opt-in; chi nhánh mẫu trước đây tự tạo | Chi nhánh mẫu cũng phải opt-in |
| Chuyển role | Chưa có API chuyển hồ sơ/lịch sử an toàn | Chưa hỗ trợ chuyển role; chặn sửa trực tiếp qua entity |

STAFF hiện vẫn chỉ được gán một chi nhánh theo nghiệp vụ có sẵn; G1–G3 không tự mở thêm quyền hoặc thay đổi giới hạn này.

## G2 — Quy chuẩn và cấp mã

| Loại | Ví dụ | Phạm vi duy nhất |
|---|---|---|
| Học viên | `HV0001`, `HV10000` | Toàn hệ thống |
| Admin/staff | `NS001`, `NS1000` | Toàn hệ thống |
| Giáo viên | `GV001`, `GV1000` | Toàn hệ thống |
| Superadmin/student account | Không có mã nhân sự | Học viên có mã trên hồ sơ học viên |

- Backend cấp số bằng IDENTITY của `student_code_numbers` và `personnel_code_numbers`, trong cùng giao dịch tạo hồ sơ/tài khoản.
- Admin, staff và teacher dùng **chung dãy số nhân sự**: ví dụ NS001, GV002, NS003. Không đảm bảo từng tiền tố liên tục; đây là lựa chọn giảm phức tạp, không phải lỗi mất dữ liệu.
- Không dùng số lượng bản ghi hay tên/điện thoại để tính mã. Số đã cấp có thể bị bỏ trống khi rollback; không tái sử dụng hoặc xóa bảng cấp số trong quá trình vận hành.
- Bốn/ba chữ số là độ rộng tối thiểu, không cắt số khi vượt ngưỡng.
- ID kỹ thuật và mã hiển thị là hai trường độc lập. Giữ nguyên ID, quan hệ khóa ngoại, mã giao dịch và khóa idempotency.
- API tạo học viên và chuyển lead đều cấp mã; API sửa không nhận trường mã. API tạo tài khoản cấp mã nhân sự sau kiểm tra role/chi nhánh.
- Không tạo thêm bản ghi học viên/nhân sự khi chuyển chi nhánh. Một người dùng một hồ sơ theo mô hình tài khoản hiện tại; chưa có sổ danh tính độc lập để hợp nhất nhiều tài khoản của cùng người.

### API và giao diện

`personnelCode` là trường bổ sung trong login, `/auth/me`, danh sách/tạo/khóa tài khoản, lựa chọn giáo viên và hồ sơ giáo viên. Không thay thế `accountId`, `profileId` hay `studentCode`.

Danh sách tài khoản tìm được theo mã, username và tên admin/staff/teacher. Projection tra cứu nhân sự/management accounts trả mã đã lưu, không dựng `GV#id`/`NV#id` nữa; giữ nguyên role/scope/projection quyền. Trang tài khoản web có cột và chi tiết mã nhân sự. Phần đồng bộ UI/báo cáo toàn dự án và bộ mẫu vẫn thuộc G6.

## G3 — Tài khoản, hồ sơ và role

- Không thêm endpoint chuyển role. Entity từ chối thay đổi role sau khi đã lưu; không cho đổi STAFF thành ADMIN bằng một lệnh cập nhật field rồi bỏ lại hồ sơ cũ.
- Login kiểm tra đúng một hồ sơ nhân sự/superadmin theo tài khoản và role tương ứng; học viên không được mang thêm hồ sơ nhân sự. Thiếu/sai/trùng hồ sơ thì từ chối đăng nhập.
- Khóa/mở tài khoản giữ nguyên mã, profile ID, phân công và lịch sử; quyền phiên vẫn theo cơ chế session version hiện có.
- Nếu sau này hỗ trợ thăng chức, phải thêm luồng chuyển hồ sơ, kiểm tra trách nhiệm, ghi lịch sử và thu hồi phiên trong một giao dịch. Với STAFF↔ADMIN, giữ `personnelCode` NS đã cấp. Giai đoạn này chưa hỗ trợ thao tác đó.
- ORM guard không thay thế kiểm soát quyền database: thao tác SQL trực tiếp có thể bỏ qua nghiệp vụ. Không sửa role, cấp mã hoặc tạo hồ sơ thủ công ngoài script đã kiểm tra.

## Migration và khởi động

Migration: [V019__short_business_codes.sql](../migrations/V019__short_business_codes.sql). Đã áp dụng cho `language_center_db` trên máy hiện tại. Hai bảng cấp số được thêm; không nạp dữ liệu mẫu/tài khoản. Trước đó các bảng dữ liệu đã được người dùng yêu cầu xóa sạch.

Máy khác: dừng Auth/Academic writers, chọn đúng database và chạy V019 trước khi khởi động bản mới. Migration MySQL 8 có thể chạy lại: giữ mã ngắn hiện có, chuyển mã HV-UUID/null/rỗng và cấp mã nhân sự còn thiếu, đặt mốc cấp số sau mã đã tồn tại. ID và quan hệ không đổi. Với dữ liệu thật cần lưu ánh xạ mã cũ trước khi đổi; DDL MySQL không rollback toàn bộ như DML. Không chạy migration đồng thời với tạo hồ sơ.

Nếu nạp SQL mẫu thủ công có mã, phải cập nhật mốc cấp số tương ứng (có thể chạy lại V019 sau khi nạp, trước khi mở ứng dụng). Tốt nhất tạo hồ sơ qua API.

- `ACADEMIC_SEED_BRANCHES_ENABLED=false` mặc định: không tự tạo CN01/CN02 khi database trống.
- Muốn chi nhánh mẫu trong môi trường phát triển: đặt `ACADEMIC_SEED_BRANCHES_ENABLED=true` có chủ đích; chỉ tạo khi chưa có chi nhánh.
- Superadmin bootstrap vẫn tắt mặc định và bật một lần theo hướng dẫn hiện có. Không tạo mật khẩu mặc định hoặc tự bật bootstrap trong lần cập nhật này.
- Khởi động lại Auth và Academic sau cập nhật; tải lại web. Chưa có tài khoản sau lần xóa dữ liệu thì cần bước bootstrap/tạo dữ liệu nền riêng.

## Kiểm thử

Đã bổ sung kiểm tra cấp mã đồng thời, rollback không tái sử dụng số, tạo/sửa học viên giữ mã, chuyển lead giữ mã, mã nhân sự khi khóa tài khoản, chặn đổi role, chặn login có hồ sơ role trùng. Các kiểm thử tự động dùng H2; migration được chạy riêng trên MySQL 8 với backfill, chạy lại và tăng độ rộng mã.

Log và báo cáo tại `storage/identity-final-tests.log`, `storage/identity-migration-result.json`, `storage/identity-web-build.log`, `storage/identity-web-tests.log`. Không coi kiểm thử H2 là nghiệm thu toàn bộ deployment MySQL.

Kết quả cuối: Auth **89/89**, Academic **273/273**, web **125/125** đạt. TypeScript/Vite build và lint các file web thay đổi đạt. Đã sửa fixture kiểm thử TODAY bị lệch ngày khi chạy sau nửa đêm; không sửa nghiệp vụ để làm test đạt. Migration MySQL đạt backfill, chạy lại và cấp số tiếp sau ngưỡng. Database dự án có 73 bảng sau migration; tất cả vẫn trống tại thời điểm xác minh.

## Giai đoạn tiếp theo

G4: chuẩn hóa trạng thái/ngày kết thúc/phân công theo thời gian. G5: hồ sơ, người giám hộ, kiểm tra dữ liệu và xóa/ngừng sử dụng. G6: giao diện toàn diện, chatbot, dữ liệu mẫu liên kết và nghiệm thu theo role. Chưa tự tạo dữ liệu mẫu trong G1–G3.
