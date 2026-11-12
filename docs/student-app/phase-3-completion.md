# Student App — giai đoạn 3: tài khoản và hồ sơ

Trạng thái triển khai: hoàn thành code backend/web/app và kiểm chứng local bằng API/UI thật. Thiết bị Android thật chưa được kết nối. Xem [hướng dẫn migration và sử dụng](account-user-guide.md).

## Thay đổi

- Bổ sung STUDENT vào Auth/Academic/Teaching/Finance; tách allowlist tài nguyên student và deny mặc định trên API vận hành. Không cấp quyền student bằng branchAssignments nhân sự; X-Branch-Id không đổi danh tính học viên.
- V014 bổ sung liên kết students.account_id unique/FK, bản ghi provisioning có requestId, version và hash mật khẩu ban đầu phục vụ replay; không lưu mật khẩu thô. AccountSecurityEvent ghi người cấp/reset/khóa. Không tự liên kết theo phone/email hoặc mở public signup.
- Web Học viên -> Tài khoản ứng dụng: SA/Admin đúng phạm vi cấp/tra cứu/khóa/mở/reset; giữ tab khi reload. Account STUDENT không lẫn vào danh sách tài khoản nhân sự; API tạo tài khoản chung không nhận STUDENT.
- Mobile login dùng API thật; xác minh /auth/me phải STUDENT trước lưu SecureStore. Đổi mật khẩu bắt buộc trước đọc dữ liệu, đổi xong đăng nhập lại. /auth/me ẩn email/phone trong phiên bắt đổi mật khẩu.
- Hồ sơ cá nhân lấy theo accountId đang đăng nhập, trả dữ liệu tối thiểu và liên hệ chi nhánh. Không nhận studentId để chọn người khác. Profile không cho student sửa trực tiếp hoặc xóa hồ sơ có account.
- Queue lưu/xóa token và generation chặn phản hồi cũ sau logout; kiểm phiên lại khi foreground;401 xóa phiên,503 cho thử lại/thoát cục bộ. Không có refresh token hoặc OTP giả. Reset giữ trạng thái khóa.

## Điều chỉnh thiết kế có chủ đích

G1 đề xuất provisioning nhiều bước giữa Auth và Academic. Thực tế đang dùng một schema MySQL và cùng transaction manager/data source, nên G3 dùng **một giao dịch DB** cho Account + accountId/version của Student + provisioning/audit. Auth có một coordinator JDBC hẹp chỉ viết association bảo mật và tăng Student.version; Academic tiếp tục sở hữu hồ sơ/nghiệp vụ. Không tạo JPA cascade chéo service hoặc sao chép hồ sơ sang Auth.

Student row lock là khóa điều phối khi tạo, kiểm scope theo branch thật từ Student; account/provision có unique DB và idempotency. Khác requestId cho cùng hồ sơ409; cùng requestId/payload trả cùng kết quả kể cả sau đổi mật khẩu nhờ hash ban đầu. Nếu tách database tương lai phải thay coordinator bằng internal command/saga và giữ nguyên hợp đồng ngoài; không giả đây là transaction phân tán.

API quản lý khóa/reset dùng optimistic version, không tự retry mutation. G3 dùng error envelope hiện hành của hệ thống để tránh đổi breaking toàn bộ API; draft envelope trong G1 chưa được tuyên bố áp dụng cho API cũ. Lỗi nghiệp vụ có message; code PASSWORD_CHANGE_REQUIRED giữ nguyên từ filter.

## API triển khai mới — 6 endpoint

| Method | Path | Quyền |
|---|---|---|
| GET | /api/v1/auth/student-accounts?studentId= | SA/ADMIN đúng chi nhánh; trả linked=false nếu chưa cấp |
| POST | /api/v1/auth/student-accounts | SA/ADMIN; requestId UUID,studentId,expectedStudentVersion,username,temporaryPassword,confirmPassword;201 |
| GET | /api/v1/auth/student-account-provisionings/{requestId} | SA/ADMIN đúng phạm vi hiện tại; tra cứu sau mất phản hồi |
| PATCH | /api/v1/auth/student-accounts/{accountId}/status | SA/ADMIN; active,version,reason |
| POST | /api/v1/auth/student-accounts/{accountId}/reset-password | SA/ADMIN; temporaryPassword,confirmPassword,version,reason |
| GET | /api/v1/academic/student/me/profile | STUDENT đã đổi mật khẩu; tự suy owner từ principal |

Dùng lại login,me,me/password,me/logout-all. Account view gồm studentId,studentVersion,linked,accountId,username,active,mustChangePassword,version; không hash/token/password. Version quản lý account không phải Student.version. App profile không chứa guardian, ghi chú nội bộ, roster hay dữ liệu học viên khác.

## Bằng chứng kiểm thử

Kết quả cuối được lưu tại storage/student-phase3-latest.txt trỏ tới database/run MySQL riêng. Report kết quả API/UI/SQL và hash JAR trong summary.json của lượt đó; không công khai fixture/secrets chứa thông tin đăng nhập thử nghiệm.

- Backend:356 tests (Auth61,Academic167,Teaching77,Finance43,Gateway8), không failure/error. Bao gồm deny API cũ, unlinked profile, cấp/replay, stale version, scope, đổi password và revoke.
- Mobile:22 tests; TypeScript và Android bundle export đạt. Có tests xác minh live identity, sai role, restore/error, logout offline, response cũ không khôi phục phiên.
- Web:96 tests, lint và production build đạt. Build có cảnh báo chunk size hiện có, không coi là lỗi nghiệp vụ.
- Nghiệm thu MySQL/API:63 checks nền và6 checks bản Auth cuối;12 luồng UI thật gồm mobile activation/profile/logout và web cấp/reset/khóa/mở. Không dùng mock để thay API nghiệp vụ ở lượt này.
- Migration V014 đã chạy hai lần trên schema trống V013, có54bảng; thêm lượt mô phỏng schema đã bị Hibernate tạo một phần và xác minh đủ3FK sau replay. Kiểm tra association/không orphan và race cùng requestId được lưu ở SQL/API report.

Các con số trên là kiểm tra đạt, không phải số chức năng độc lập hoặc coverage100%. UI automation đọc CAPTCHA canvas để đăng nhập web, không phải bài kiểm thử độ an toàn CAPTCHA.

## Triển khai và phần còn lại

Chưa áp dụng V014 vào database gốc; chưa thêm tài khoản học viên vào dữ liệu thật. Không reset/hạ enum/tự sửa release ZIP cũ. Chạy migration và restart đủ service theo hướng dẫn trước khi dùng source mới trên dữ liệu hiện tại.

G4 tiếp tục lớp và lịch cá nhân. Khi thêm API student phải mở đúng matcher trong service tương ứng, @PreAuthorize STUDENT và query ownership; không thêm STUDENT vào quyền API danh sách quản trị. G6 hoàn thiện ticket cập nhật hồ sơ/hỗ trợ; hiện dùng liên hệ chi nhánh sau xác minh. G10 cần kiểm chứng thiết bị thật, bản cài và cấu hình vận hành trước bàn giao cuối.


Ảnh UI kiểm chứng: [Hồ sơ trên app](figures/phase3-mobile-profile.png) · [Quản lý tài khoản trên web](figures/phase3-web-account.png).

