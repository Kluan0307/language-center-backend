# G5 — Điểm công bố và chuyên cần cá nhân

Triển khai ngày 12/11/2026. Thuộc lộ trình Student App 10 giai đoạn, độc lập với mốc G5 của bản web trước đây.

## Sử dụng

1. Backup database. Nếu chưa có V014, hoàn tất [hướng dẫn tài khoản](account-user-guide.md) trước.
2. Dừng ghi Academic/Teaching, áp dụng [V015](../migrations/V015__student_grade_publications.sql) vào database hiện có; không tạo lại database, không xóa dữ liệu. Maven không tự chạy file SQL này. Đã thử apply/replay trên MySQL riêng; database nguồn chưa được thay đổi.
3. Build/restart backend và web cùng phiên bản. App giữ cấu hình gateway `/api/v1` như G4.
4. Giáo viên chính của lớp nhập điểm ở web → Bảng điểm & đánh giá. Lần nhập đầu đóng băng danh sách dự thi. `Ghi chú nội bộ` được giữ riêng; `Nhận xét dành cho học viên` là trường mới, không tự sao chép ghi chú cũ.
5. Tại phần Công bố cho học viên, nhập lý do rồi Công bố điểm. Quản lý SUPER_ADMIN/ADMIN đúng phạm vi và giáo viên chính được công bố/thu hồi; STAFF không có quyền. Admin vẫn chỉ hiệu chỉnh điểm đã có theo quy tắc cũ.
6. App → Học tập → mở lớp → Xem điểm & kết quả / Xem chuyên cần. Có thể mở trực tiếp `/grades/{enrollmentId}` hoặc `/attendance/{enrollmentId}`, đích được giữ qua đăng nhập.

Ví dụ chạy trong MySQL client sau khi backup và chọn đúng database:

```sql
USE language_center_db;
SOURCE D:/hoctap/KLTN/language-center/language-center-backend/language-center-backend/docs/migrations/V015__student_grade_publications.sql;
```

DDL MySQL không rollback nguyên khối. Nếu schema đang được Hibernate tự cập nhật dở, cần kiểm tra cấu trúc trước khi áp dụng; không xóa bảng publication để hạ phiên bản. V015 dùng CREATE IF NOT EXISTS và kiểm tra cột để replay trên cấu trúc đã áp dụng thành công.

## Quy tắc điểm

Mỗi thao tác là một hàng append-only `grade_publications`, revision duy nhất theo exam và request UUID duy nhất. PUBLISH lưu từng `published_grades` cùng studentId + enrollmentId; RETRACT là revision mới không có điểm. Revision mới nhất quyết định quyền hiển thị. Không trả lịch sử bản đã thu hồi cho student. Không sửa/xóa bản công bố cũ khi sửa điểm nháp; không cho xóa bài có lịch sử công bố.

Publication khóa exam để tuần tự hóa với nhập/sửa điểm. Lệnh có `expectedRevision`, `draftFingerprint`, UUID và lý do; dữ liệu đổi phải tải lại trước khi xác nhận. Retry cùng intent không tạo bản mới; dùng lại UUID với intent khác bị409. Tra cứu request trả trạng thái hiện tại, không làm sống lại bản đã thu hồi. Web giữ nội dung/mã yêu cầu khi lỗi mạng và cho tra cứu hoặc gửi lại.

Danh sách dự thi phải đóng băng, có enrollmentId xác minh khớp roster Academic tại ngày thi. Legacy/ambiguous không đoán liên kết. Có bài chưa chấm thì snapshot giữ `score:null`, `NOT_GRADED`; điểm0 thật vẫn `GRADED`. Student chỉ xem bản công bố cho đúng student/profile/enrollment/class đã kiểm quyền qua Academic. Không có nhận xét nội bộ, người nhập/sửa, lý do công bố hoặc roster trong projection.

## Kết quả cuối khóa

Dùng kết quả chính thức đã xác nhận từ quy trình hiện hành; không tự tính trung bình hay suy PASS từ trạng thái COMPLETED. Projection chỉ có `state,outcome,revision,confirmedAt`. Chỉ trả PASS/FAIL khi bản mới nhất CONFIRMED khớp academic source revision, barrier token và teaching source revision hiện hành. Chưa có kết quả → PENDING; đã mở lại hoặc nguồn khác → UPDATING, không giữ kết luận cũ. Academic lỗi → lỗi API/503 để retry, không giả PENDING. Không so result revision từng học viên với counter kết quả toàn lớp.

Công bố/thu hồi chỉ thay quyền xem snapshot điểm, không thay source revision đánh giá hoặc tự xác nhận/mở lại kết quả chính thức.

## Chuyên cần

Chỉ buổi COMPLETED đến ngày hiện tại theo Asia/Ho_Chi_Minh và nằm trong period xác thực mới vào mẫu số. Cuối period là mốc loại trừ `[activeFrom,activeUntil)`. SCHEDULED/CANCELLED, buổi tương lai và học bù ngoài thời kỳ không tự tính.

PRESENT/LATE tính tham dự; ABSENT có phép vẫn là vắng. Buổi chưa có bản ghi liên kết đúng session + ngày là NOT_RECORDED, không mặc định PRESENT hoặc ABSENT. Bản ghi cũ không sessionId không được nối chỉ bằng ngày; hiển thị LEGACY_UNVERIFIED. Legacy enrollment thiếu period và bản ghi ngoài period không tạo tỷ lệ chính thức.

Tổng summary lấy toàn đăng ký, độc lập trang/dải ngày đang xem. Thiếu điểm danh hoặc có dữ liệu cần xác minh → `coverageComplete:false,attendancePercent:null`. Mẫu số0 cũng trả null; không hiện0% giả. Không có endpoint student ghi điểm/chuyên cần.

## API thực hiện

Tất cả thêm prefix `/api/v1` và bearer hợp lệ. Nhân sự không dùng endpoint `/student/me`.

| Method | Path | Quyền / dữ liệu |
|---|---|---|
| GET | `/academic/student/me/enrollments/{id}/attendance` | STUDENT chủ sở hữu; page,size1..100,from?,to?; content + summary |
| GET | `/academic/student/me/enrollments/{id}/result-source` | STUDENT chủ sở hữu; classId,token,sourceRevision; không bootstrap/ghi |
| GET | `/teaching/student/me/enrollments/{id}/grades` | STUDENT chủ sở hữu; page,size1..100; snapshot hiện hành |
| GET | `/teaching/student/me/enrollments/{id}/final-result` | STUDENT chủ sở hữu; trạng thái kết quả hiện hành |
| GET | `/teaching/exams/{id}/student-publication` | SA/A/T chính; revision,published,publishedAt,candidates,graded,draftFingerprint,draftChanged |
| POST | `/teaching/exams/{id}/student-publications` | SA/A/T chính; requestId,expectedRevision,draftFingerprint,reason |
| POST | `/teaching/exams/{id}/student-publications/retract` | SA/A/T chính; cùng command |
| GET | `/teaching/exams/{id}/student-publication/requests/{requestId}` | SA/A/T chính; phục hồi command |

Grade projection: `examId,examName,examType,examDate,maxScore,weight,revision,publishedAt,score,status,studentComment`. Attendance: `sessionId,attendanceId,date,startTime,endTime,status,hasPermission,source`. Không coi trạng thái session đã diễn ra là bằng chứng học viên có mặt.

## Kiểm chứng và giới hạn

Bằng chứng tái tạo ở `scripts/student-app/phase5/`, con trỏ riêng `storage/student-phase5-latest.txt`. Prepare dùng schema V013 + V014/V015, replay migrations; `prod/validate` cho cả5process. G4 fixture setup chạy lại trên schema G5, sau đó G5 API/UI. Fixture enrollments/sessions/attendance/final states được seed SQL riêng để kiểm chứng read model; account provisioning và grade/publication commands chạy API thật. Đây không phải kiểm thử lại toàn bộ workflow enrollment/confirm/attendance writer.

Phát hiện và sửa lỗi MySQL REPEATABLE_READ ở lần bootstrap barrier: query khóa/current read nạp lại instance sau khi flush/detach cache, bỏ refresh dư có thể dùng snapshot cũ. Sửa cùng cơ chế ở Academic và Teaching; kiểm chứng lại trên schema trống, không che lỗi bằng seed barrier trước.

Báo cáo máy `summary.json` trong thư mục run ghi số kiểm tra và hash artifacts. Browser UI dùng Edge với viewport mobile và API/MySQL thật; đọc CAPTCHA canvas chỉ để tự động hóa login web, không phải kiểm thử CAPTCHA. Android Hermes bundle không thay thế nghiệm thu APK/thiết bị thật. Native keyboard/back/SecureStore/thiết bị vẫn là gate G10. G6 tiếp theo: tài liệu chia sẻ và yêu cầu hỗ trợ; học phí/online payment/push chưa thuộc G5.

### Kết quả nghiệm thu local

- Backend:390 tests (Auth61, Academic189, Teaching89, Finance43, Gateway8), không lỗi/skip. App39 tests; web96 tests.
- MySQL/gateway:63 kiểm tra setup/G4 và41 kiểm tra G5;5 kiểm tra SQL toàn vẹn. Race công bố cùng revision chỉ một commit, một409.
- UI Edge/API/MySQL thật:10 luồng gồm login/deep link, điểm0, kết quả reopened, attendance/legacy, chặn đăng ký người khác, web công bố/thu hồi, sửa nhận xét và công bố lại. Không có runtime exception. Ảnh: [điểm](figures/phase5-grades.png), [chuyên cần](figures/phase5-attendance.png), [công bố trên web](figures/phase5-web-publication.png).
- TypeScript, web lint/build và Android Hermes export đạt. Web còn cảnh báo chunk lớn như bản trước. Chưa kiểm chứng thiết bị Android thật/APK/iOS; không tuyên bố Expo Doctor đạt.
- Không thay đổi dữ liệu nguồn hoặc ZIP release web trước đây. Dừng runtime và UI riêng sau kiểm tra.

Tái tạo: `node scripts/student-app/phase3/prepare.cjs --phase=5` → build backend → `scripts/student-app/phase3/runtime.ps1 -Action start -Phase 5` → `node scripts/student-app/phase4/live-api.cjs --phase=5` → `node scripts/student-app/phase5/live-api.cjs` → `scripts/student-app/phase5/start-ui.ps1` → `node scripts/student-app/phase5/live-ui.cjs` → `node scripts/student-app/phase5/verify-sql.cjs`. Trước summarize chạy Maven test, app typecheck/test/export, web lint/test/build và lưu log phase5 tương ứng. Không chạy lại bước seed lên cùng run; prepare mới tạo schema riêng. `summarize.cjs` đối chiếu báo cáo, lưu hash5JAR/manifest source. Dừng bằng runtime.ps1 `-Action stop -Phase 5` và `phase5/stop-ui.ps1`.
