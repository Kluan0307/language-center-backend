# Verification

## 2026-11-04 — Stage 6 dashboards, reports and MVP acceptance

All backend modules were tested together: 43 tests passed, zero failures/errors/skips
(Auth 10, Academic 12, Finance 10, Teaching 8, Gateway 3). Frontend: 50 tests passed,
TypeScript/production build and full ESLint passed.

30 Edge/API checks passed with real MySQL and isolated service instances. Fixtures
spanned two branches and exercised Lead -> Student -> Enrollment -> Tuition -> Payment
-> Attendance -> Grade -> Document approval -> Staff download. Dashboard values were
matched against source APIs for all four roles. Teacher/Staff did not call forbidden
aggregate endpoints; direct report routes were blocked. Checks also covered weighted
global ratios, branch reconciliation, inclusive receipt-day boundaries, chronological
month keys, current debt independent of date range, invalid date ranges, downloaded
CSV contents, partial Finance failure/recovery and navigation at 390/768/1366/1600px.

Routes now load lazily. The entry JS is about 233 kB rather than a single 1.7 MB bundle;
this is not total transfer size. A shared Sidebar/Ant Design chunk remains about 727 kB
and still triggers Vite's chunk-size warning. No production deployment/load test is claimed.

Tracked test rows/files were removed and owned test services/browser were stopped;
existing user instances were preserved. See the [stage 6 guide](../../../language-center-web-frontend/docs/phase-six.md)
and [acceptance runbook](../../../language-center-web-frontend/docs/acceptance-guide.md).

## 2026-11-04 — Stage 5 attendance and teaching

Frontend: 44 tests passed; TypeScript/production build and full ESLint passed.
Replacing the mock attendance page removed its pre-existing set-state-in-effect error.
Academic: 12 tests passed; Teaching: 8 tests passed (H2/mock/HTTP stub), including a
new transaction test for concurrent document reviewers.

37 browser/API checks passed through isolated Gateway/Academic/Teaching instances
with real MySQL and a separate fixture document directory. Checks covered attendance
save/reload/history/summary, future-session rejection, transferred student history,
full-roster validation, initial grades including zero, teacher updates, administrative
correction reasons and audit, weighted evaluation, exam scoring-rule restrictions,
role/class permissions, multipart uploads, approval/rejection, course assignment,
downloaded bytes and two reviewers competing for one pending document.

The teacher's current enrollment roster is combined with existing grades in the UI
so historical grades remain visible after transfer while teacher edit actions disappear.
Document downloads use JWT and Blob URLs; pending/rejected documents stay hidden from
Staff. Upload POST is not automatically retried after an uncertain response.

Tracked database and file fixtures were cleaned; owned test processes were stopped.
Existing user services were preserved. See the [stage 5 guide](../../../language-center-web-frontend/docs/phase-five.md).

## 2026-11-04 — Stage 4 finance

Frontend: 37 tests passed, TypeScript/production build and lint on changed files passed.
Full lint retains the pre-existing AttendancePage set-state-in-effect issue.
Finance: 8 tests passed on H2, including split payments, idempotent retries, unchanged
payment metadata, role-restricted adjustment with audit, and concurrent cashiers.

22 Edge browser/API checks passed against real MySQL through isolated Gateway,
Academic and Finance instances (18080/18082/18084). Scenarios included partial cash
and bank-transfer collections, final settlement, discounts and overdue flags,
student/class receipt context, Staff adjustment denial, Teacher denial, branch
isolation and Super Admin filtering. A response was deliberately aborted after the
payment committed: reloading and replaying the persisted request returned the same
receipt without duplicate collection. A changed balance while the form was open
produced a definite rejection and no extra receipt.

Tracked fixture IDs were cleaned afterward and owned test processes were stopped;
existing user services were preserved. No database reset or schema migration was
needed. See the frontend [stage 4 guide](../../../language-center-web-frontend/docs/phase-four.md).

## 2026-11-04 — Stage 3 admissions

Academic tests: 12 passed, zero failures/errors/skips (H2). Frontend: 31 tests passed,
TypeScript and production build passed. New admissions files passed ESLint; the full
frontend lint still reports one pre-existing set-state-in-effect issue in the mock
AttendanceManagement/AttendancePage.tsx.

Edge browser and real MySQL checks covered lead creation/care/conversion, idempotent
conversion, optional birth date and guardian editing, Staff student creation with a
shared family phone, enrollment with Finance offline, retry after Finance recovery,
canonical billing status after transfer, reservation/resumption/drop, class capacity,
two requests competing for one remaining seat, branch isolation and Teacher denial.
The test services ran on 18080/18082/18084; they were stopped after verification.
The user's existing 8080/8081/8082/5173 processes were preserved. Exact fixture IDs
were removed, and zero remaining fixture students/enrollments/tuition/leads/classes/
branches were confirmed.

Real schema inspection found the obsolete students.uk_student_phone_branch index.
After backing up students, phase-three-student-contact.sql removed that index and
filled any missing student codes. The broad legacy migration remains unexecuted.
See the frontend [stage 3 guide](../../../language-center-web-frontend/docs/phase-three.md).

## 2026-11-04 — Phase 2: courses, rooms, classes and timetables

Frontend: **25 tests passed** (7 new), production build passed, changed files passed lint.
Academic: **10 tests passed** on H2/mock (3 new). New cases cover inactive branch course
creation, a Staff request with a stale fee after Admin changes it under concurrency,
and consistent branch-before-course lock order.

**40 browser/API checks passed** against the user's running Gateway/Auth/Academic/MySQL:
course/room/class/timetable CRUD where supported, shared-course permissions, role and
branch isolation, teacher profile IDs, capacity, teacher/room conflicts, adjacent slots,
initial timetable creation, details, cascade deletion, class progress and room state.
An additional HTTP check against the newly built Academic instance on temporary port
18082 confirmed inactive branch course creation returns 400.

Only explicitly tracked fixtures were deleted afterward: course/room/class/schedule
tables are empty, four existing accounts and CN01 remain. Enrollment/attendance/finance
and teaching workflows were not exercised as full live workflows in this phase.
See [frontend phase 2](../../../language-center-web-frontend/docs/phase-two.md).

## 2026-11-04 — Phase 1: organization and accounts

Auth: **10 tests passed**, zero failures/errors/skips. The test configuration was
isolated from the developer MySQL database and uses H2. Four new mocked service tests
cover missing branch, public Super Admin creation, disabling Super Admin and cross-branch access.

Frontend: **18 tests passed**, production build passed and the changed files passed lint.
The full frontend lint still reports three existing issues in AttendancePage,
LeadUpdateModal and LeadsPage, outside this phase.

**25 browser checks passed** against real Gateway, Auth, Academic and MySQL:
branch creation/update/status changes, duplicate codes, inactive branch account rejection,
SA-created Teacher, Admin-created Staff, new account login, disabled login, branch scope,
role route guards, profile, logout, remembered/per-tab sessions and network-failure recovery.
Temporary test accounts and branches were removed; the four existing seed accounts and CN01 remain.
Other academic/finance/teaching flows and migration of historical data were not exercised in this phase.

See [frontend phase 1](../../../language-center-web-frontend/docs/phase-one.md).

## 2026-11-02 — Backend baseline

The repository root `mvn clean test` passed for all five modules. Two additional
gateway assertions were subsequently added and the gateway suite was run again.
Latest reports contain 29 successful tests, zero failures/errors/skips:

| Service | Tests |
|---|---:|
| auth-service | 6 |
| academic-service | 7 |
| finance-service | 6 |
| teaching-service | 7 |
| api-gateway | 3 |

Coverage includes authenticated/unauthenticated role access, disabled account tokens,
student timetable conflicts after scheduling changes, expired full classes, transferred
students in historical attendance, inclusive/exclusive enrollment periods, failed finance
delivery after enrollment commit, shared parent phone numbers, retained transfer billing,
grouped operational reports, over-collection, duplicate payment requests, two concurrent
cashiers, revenue aggregation, grade limits, administrative correction restrictions,
private document authorization/storage, forwarding JWT to academic, four gateway route
prefixes and browser CORS preflight.

Databases were in-memory H2; one HTTP contract test used a local JDK HTTP stub. No MySQL
database, real accounts or existing files in document storage were changed. MySQL migration
SQL and end-to-end execution against real running services have not been executed. These
remain deployment verification steps, particularly for reconciling historical financial data.

Java changes were formatted with google-java-format 1.28.0. `git diff --check` passed.
# Cải thiện giai đoạn 1 — nhiều chi nhánh (04/11/2026)

- Backend: **70 tests**, 0 failures/errors/skips: Auth 17, Academic 20, Teaching 14,
  Finance 16, Gateway 3. Bao gồm kiểm tra phân công/thu hồi, chi nhánh chính, lịch sử,
  Staff tối đa một chi nhánh, Admin không tự tăng quyền và không khóa tài khoản dùng chung
  ngoài phạm vi; ba service kiểm tra token cũ, quyền hiện tại và Auth outage.
- Frontend: **52 tests**; TypeScript/production build và ESLint đạt.
- **39 kiểm tra API/Edge/MySQL thực tế** qua Gateway trên cổng kiểm tra riêng:
  A/B được giao, C bị chặn; đọc/sửa dữ liệu ngoài phạm vi; tạo nhân sự tại B;
  giáo viên đa chi nhánh và trùng giờ; giáo viên không truy cập lớp người khác;
  ghi danh → tạo học phí tại B; tổng hợp học viên/công nợ đúng phạm vi;
  thu hồi B có hiệu lực ngay với token cũ trên cả ba service và báo cáo tổng hợp;
  lịch sử phân công; CORS; chuyển chi nhánh, reload, form phân công, dashboard và menu di động.
- Migration V001 được áp dụng sau khi sao lưu trên local; ba tài khoản không phải SA
  được chuyển sang ba phân công chi nhánh 11. Chạy lại migration giữ nguyên 3 phân công
  và 3 sự kiện MIGRATED; không nhân đôi dữ liệu.
- Dữ liệu kiểm thử được theo dõi bằng ID và prefix riêng rồi dọn; kiểm tra còn 0 bản ghi
  kiểm thử. Bốn tài khoản superadmin/admin/staff/teacher ban đầu được giữ lại.
- Trình duyệt headless và các service kiểm tra dùng cổng 18080–18084 đã được dừng;
  không dùng hay dừng các service đang chạy của người dùng để thay bản kiểm tra.
- Artifact kiểm tra được giữ trong `storage/multibranch-results.json`, các log và ảnh
  `storage/multibranch-mobile.png` (gitignored). Vite vẫn cảnh báo shared chunk lớn;
  đây chưa phải kiểm thử tải hoặc xác nhận triển khai production.
- Kiểm tra giao diện phát hiện Vite đang giữ một bản transform trung gian sau nhiều lần
  ghi nhanh cùng tệp; làm mới module rồi đối chiếu lại dashboard cho đúng 2 học viên và
  1.000 VND công nợ trong dữ liệu thử. Production build đọc mã nguồn cuối cùng.

Hướng dẫn và các giới hạn: [Cải thiện giai đoạn 1](improvements/phase-one-multi-branch.md).

# Cải thiện giai đoạn 2 — tài khoản và phiên (05/11/2026)

- Backend: **92 tests**, không lỗi/thất bại; Auth 36, Academic 21, Teaching 15,
  Finance 17, Gateway 3. Đã kiểm tra đổi/cấp lại mật khẩu, mật khẩu hiện tại cũ,
  xác nhận và giới hạn byte UTF-8, phiên bản token, cùng username khác accountId,
  khóa/mở tài khoản, quyền cấp lại toàn phạm vi, cập nhật liên hệ bốn role và che thông tin log.
- Frontend: **55 tests**; ESLint và production build đạt. Kiểm tra bổ sung mật khẩu UTF-8,
  route bắt buộc đổi mật khẩu và xử lý lỗi PASSWORD_CHANGE_REQUIRED đúng phiên hiện tại.
- **45 kiểm tra API/Edge/MySQL thực tế**: mật khẩu tạm bị chặn ở Academic/Teaching/Finance;
  đổi/cấp lại mật khẩu và logout-all thu hồi token; mở khóa không khôi phục token cũ;
  reset không mở khóa; Admin không reset người dùng ngoài phạm vi/giáo viên dùng chung;
  liên hệ cá nhân không cho đổi role/chi nhánh/họ tên; các biểu mẫu, điều hướng, xác nhận,
  reset bởi Admin và giao diện di động hoạt động đúng.
- Đã đăng nhập kiểm tra cả bốn tài khoản có sẵn bằng mật khẩu hiện tại: vẫn hoạt động,
  không bị ép đổi mật khẩu do migration. Token của tài khoản thử bị thu hồi trên các service.
- Kiểm tra log Auth thực tế bằng chuỗi đánh dấu riêng: không thấy mật khẩu đánh dấu trong log;
  ChangePasswordRequest được hiển thị dạng REDACTED. Test DTO cũng kiểm tra LoginRequest,
  CreateUserRequest, ResetPasswordRequest và LoginResponse không in mật khẩu/token.
- Migration V002 đã áp dụng sau khi sao lưu auth tables, đối chiếu hash mật khẩu không đổi.
  Chạy lại giữ nguyên trạng thái phiên/cờ đổi mật khẩu và số sự kiện.
- MySQL ghi nhận các sự kiện PASSWORD_CHANGED, PASSWORD_RESET, SESSIONS_REVOKED,
  PROFILE_UPDATED, ACCOUNT_LOCKED và ACCOUNT_UNLOCKED của dữ liệu thử. Không ghi mật khẩu
  vào nội dung sự kiện. Sau khi kiểm tra đã dọn đúng ID/prefix; còn 0 tài khoản/chi nhánh/
  phân công/sự kiện kiểm thử. Bốn tài khoản ban đầu vẫn ở phiên bản 0 và không bị ép đổi mật khẩu.
- Các service kiểm thử 18080–18084 và Edge kiểm thử 9231 đã dừng. Các service và Vite của
  người dùng được giữ nguyên. Cần khởi động lại chúng để dùng đầy đủ code mới.
- Artifact: `storage/improvement2-results.json`, các log kiểm thử/build và ảnh
  `storage/improvement2-mobile.png` (gitignored). Shared frontend chunk vẫn có cảnh báo
  kích thước Vite; chưa thực hiện kiểm thử tải hay xác nhận production.

Hướng dẫn: [Cải thiện giai đoạn 2](improvements/phase-two-account-security.md).


## Cải thiện 3 — 05/11/2026

- Maven toàn bộ 5 module: **110 tests, 0 failure/error**, gồm 18 test SessionWorkflowTest mới.
- Frontend: **59 tests**, toàn bộ ESLint và TypeScript/Vite build thành công. Vite còn cảnh báo chunk chung lớn hơn 500 kB; chưa phải đánh giá hiệu năng tải.
- **44 kiểm tra MySQL/API/giao diện** qua các service riêng 18080–18084 và Edge riêng. Bao gồm quyền dạy thay, teacher/room collision xuyên chi nhánh, hai giao dịch đồng thời chỉ một thắng, học bù vượt ngày kết thúc, template giữ buổi tùy chỉnh, ghi/sửa/audit điểm danh, từ chối version cũ, migration LEGACY/replay, dashboard, lịch di động và không có uncaught browser error.
- Migration V003 đã sao lưu/applied; thử điểm danh lịch sử giữ trạng thái/tác giả, không bịa snapshot chưa biết. Chạy lại không đổi cancellation/customization/version.
- Đã xóa toàn bộ fixture theo danh sách ID và prefix SES, đối chiếu số dư fixture bằng 0. Hash mật khẩu, session_version và must_change_password của tài khoản cũ giữ nguyên. Dừng các tiến trình kiểm thử riêng, giữ service người dùng 8080–8084/Vite.
- Bằng chứng local trong storage/improvement3-results.json (đã bỏ token/password), log backend/frontend và improvement3-mobile.png; storage được gitignore. Không tuyên bố đã kiểm thử tải hoặc triển khai production.

Chi tiết và giới hạn: [giai đoạn 3](improvements/phase-three-class-sessions.md).

## Cải thiện 4 — 05/11/2026

- Maven toàn bộ 5 module: **128 tests, 0 failure/error**, gồm 11 LearningLifecycleTest và 7 ExamRosterTest mới. Đóng gói backend thành công.
- Frontend: **63 tests**, ESLint và TypeScript/Vite build thành công. Cảnh báo chunk dùng chung >500 kB còn như trước; chưa đánh giá tải sản xuất.
- **41 kiểm tra MySQL/gateway/giao diện và replay**: lý do/version/ngày dự kiến bắt buộc; bảo lưu/học lại/chuyển từ lớp đã kết thúc; giữ billing root và đúng một học phí; roster kỳ thi quá khứ khi hiện đã bảo lưu; chốt cả roster khi chấm điểm; loại học viên vào muộn; cấm nhập trước ngày thi; giữ tác giả khi sửa điểm; chặn Teacher ngoài lớp và Staff tại teaching; hai quyết định vòng đời đồng thời chỉ một thành công. UI kiểm tra lý do, ngày dự kiến, lịch sử, cập nhật kế hoạch, học lại, roster và điểm cho người đã rời lớp. Không có uncaught browser error trong các luồng thử.
- V004 đã sao lưu và áp dụng local. Replay sau khi có version, ngày bảo lưu, audit, snapshot roster và điểm không thay dữ liệu đó. Không bịa tác giả/lý do lịch sử hay khoảng học bị thiếu.
- Đã xóa chính xác fixture LIF theo ID; đối chiếu 0 lớp/học viên/đăng ký/học phí/đề thi/roster/điểm/audit/tài khoản/chi nhánh thử còn lại. Mật khẩu, session_version, must_change_password tài khoản cũ giữ nguyên.
- Dừng riêng service 18080–18084 và Edge 9233; giữ service người dùng 8080–8084 và Vite. Cần khởi động lại Academic/Teaching để các tiến trình đang chạy nạp bản cập nhật.
- Bằng chứng local: storage/improvement4-results.json đã bỏ token/password; các log backend/frontend/package và improvement4-mobile.png. Storage được gitignore. Kiểm tra chức năng không thay thế thử tải/triển khai production.

[Hướng dẫn và giới hạn giai đoạn 4](improvements/phase-four-learning-lifecycle.md).


## Cải thiện 5 — 05/11/2026

- Maven toàn bộ 5 module: **145 tests, 0 failure/error**, gồm 17 FinanceLedgerWorkflowTest mới. Đóng gói backend thành công.
- Frontend: **67 tests**, ESLint và TypeScript/Vite build thành công. Cảnh báo chunk dùng chung lớn hơn 500 kB còn như trước; chưa có kiểm thử tải sản xuất.
- **39 kịch bản MySQL/gateway/UI/replay**: điều chỉnh dưới số thu tạo credit; hoàn một phần/giới hạn dư; đảo toàn phiếu và ngăn đảo phiếu đã hoàn; giữ phiếu/người thu gốc; retry key/version cũ, key collision; quyền Admin/Staff/Teacher/chi nhánh; báo cáo kỳ trước/kỳ hiện tại; đối soát; hoàn đồng thời chỉ một thắng; cùng ý định gửi đồng thời trả đúng một correction.
- CDP đã chặn phản hồi HTTP 200 sau khi database commit cho cả điều chỉnh và hoàn. Reload trình duyệt rồi gửi lại payload gốc không tạo thêm journal. UI kiểm tra lý do/mã chuyển hoàn bắt buộc, audit, chặn giao dịch mới khi yêu cầu cũ chưa xác nhận, Staff chỉ đọc lịch sử. Không có uncaught browser error trong các luồng nghiệp vụ được kiểm tra.
- V005 sao lưu/applied local và replay hai lần, gồm lần có fixture PENDING kế thừa: giữ nguyên version, journal, thời gian/số phiếu gốc và số dư; chỉ đưa chẩn đoán. Phiếu cũ chưa xác minh bị chặn ghi mới ngay cả khi tổng số khớp.
- Đã dọn chính xác fixture FIN theo ID/prefix. Đối chiếu 0 adjustment/correction/payment/tuition/enrollment/student/class/account/branch thử còn lại; mật khẩu, session_version và must_change_password tài khoản cũ được giữ.
- Dừng riêng service kiểm thử 18080–18084 và Edge 9234. Giữ các service người dùng 8080–8084/Vite. Khởi động lại finance-service và tải lại frontend để dùng bản mới.
- Bằng chứng local: storage/improvement5-results.json (đã bỏ token/password), log tests/lint/build/package và improvement5-mobile.png; storage được gitignore. Không tuyên bố đã đối soát ngân hàng, tự chuyển tiền hay triển khai production.

[Hướng dẫn và giới hạn giai đoạn 5](improvements/phase-five-finance-ledger.md).


## Cải thiện 6 — 05/11/2026

166 backend tests (0 failure/error); 72 FE tests; lint và build đạt. 41 kiểm tra MySQL/gateway/UI/concurrency/replay đạt. Query lead 22→2, enrollment 42→2 trên H2/service, không bao gồm Auth và không phải load test. Bundle tăng 12.590 byte với tính năng mới; chunk lớn nhất vẫn 760.673 byte. V006 replay giữ nguyên dữ liệu. [Phạm vi và giới hạn](improvements/phase-six-care-documents-experience.md).

Dữ liệu/file fixture CARE còn lại: bước dọn bị automatic approval review từ chối và đang chờ người dùng cho phép. Token/mật khẩu đã được bỏ khỏi file bằng chứng. Không ghi nhận cleanup hoàn tất.

## Cải thiện 7 — triển khai/khôi phục

Maven verify cả năm module và kiểm tra gateway sau sửa header: **195 backend tests, 0 failure/error**. Thêm 9 tests công cụ backup (checksum, incomplete, path traversal, ID và consistency flag). FE production build `/api/v1` đạt; 72 FE tests từ giai đoạn 6 vẫn là baseline, không có thay đổi source FE đợt này.

Backup/restore 33 bảng, 4 file revision; hash toàn bộ data, số dòng, checksum file và replay V001–V006 đạt trên schema mới, giữ database nguồn. Backup dùng online-rehearsal, chưa phải điểm phục hồi production. Có release manifest; source còn dirty nên chưa coi Git SHA là đủ để tái tạo artifact. Nginx chưa có trên máy; chưa xác minh Nginx/TLS/domain hoặc tải production. Fixture CARE và các database diễn tập được giữ nguyên. [Runbook và giới hạn](improvements/phase-seven-deployment.md).

Release cuối chạy thực tế cả năm tiến trình trên cổng 17080–17084 với profile prod,
bốn service qua Hibernate validate trên database restore. **19 smoke checks** đạt:
10 probes UP/status-only, 5 management paths bị chặn, 4 business routes không token
trả 401 và có đúng một Request ID. Phát hiện/sửa header correlation bị lặp khi proxy,
có test hồi quy. **16 kiểm tra role/CORS** đạt: đăng nhập/identity/scoped read cả bốn
role, Teacher bị chặn CRM, Staff bị chặn endpoint quản lý, origin cấu hình được chấp
nhận và origin lạ bị từ chối. Các kiểm tra này không thay thế browser E2E nghiệp vụ.

## G02–G03 — 07/11/2026

211 backend tests, 0 failure/error: Auth54, Academic73, Finance40, Teaching36, Gateway8. Clean verify toàn backend đạt 210 tests; sau sửa upstream400, Academic 73 tests đạt và package thành công (một lần repackage bị khóa JAR Windows đã xử lý bằng dừng riêng JVM thử). FE72 tests, lint/build cuối đạt.

37 kiểm tra duy nhất trên MySQL/API/UI/race/replay: quyền nhiều chi nhánh, stale/self-contact version, audit/tìm tên, chặn ngừng/thu hồi khi còn việc, bàn giao giữ COMPLETED/attendance, independent login lock, 4 race ngừng/phân công và 1 race revoke/phân công, outage Academic fail-closed, UI dirty guard/read-only/bàn giao/ngừng/mobile, whitelist/validation, V007 replay giữ toàn bộ version/profile/workStatus/audit. Có screenshot và không có uncaught browser runtime exception trong luồng thử.

Chỉ dùng database/file restore riêng với prod/validate; source chưa áp dụng V007 hoặc sửa tài khoản/dữ liệu gốc. Backup online rehearsal, không gọi là backup production đủ điều kiện. [G02](thesis-progress/G02.md) · [G03](thesis-progress/G03.md).

## G04/G05 — 08/11/2026

238 backend tests tổng hợp, 0 failure/error: Auth54, Academic100, Finance40, Teaching36, Gateway8. Academic chạy test riêng (100), các service khác chạy regression cùng lệnh; package toàn backend đạt với skipTests sau đó, không gọi package là một lần test mới. Thêm 27 ca nghiệp vụ (12 policy +15 assessment). FE72 tests, ops9 tests đã đạt; npm run lint và npm run build cuối đạt. Vite có cảnh báo bundle lớn hơn500kB; chưa tối ưu chia bundle trong phạm vi G04/G05. Health19 qua cổng riêng đạt.

36 ca API/UI/race/replay duy nhất trong storage/g04-g05-results.json (fixture riêng trên schema restore). UI kiểm tra danh mục, dirty guard, policy khóa, áp dụng lớp, nháp/xác nhận/thay thế/thu hồi/history, mobile390 và không uncaught runtime exception. Một số lần automation đầu dùng sai kỳ vọng400 thay409, wrapper StudentDto.Detail hoặc selector trùng bảng; đã sửa kịch bản và chạy lại phần liên quan. Không tính các lần thất bại là pass. Chi tiết lệnh/log và giới hạn ở [G04](thesis-progress/G04.md)/[G05](thesis-progress/G05.md).

## G06/G07 — 08/11/2026

263 backend tests,0 failure/error (auth-service:54, academic-service:125, finance-service:40, teaching-service:36, api-gateway:8), trong đó25 ca placement mới. FE72, ops9, health19 đạt; lint/build cuối đạt. 32 API/UI/race/replay duy nhất; [chi tiết G06](thesis-progress/G06.md), [G07](thesis-progress/G07.md). Migration V010 chỉ trên restore; database nguồn chưa nâng. Đã sửa rollback-only khi preview lịch trùng bằng method trả dữ liệu không ném exception; có kiểm thử commit ngoài transaction test và MySQL200/409 tương ứng. Tất cả thử concurrency là ca chủ đích, không phải load test.

## G08/G09 — 08/11/2026

289 backend tests tổng hợp0failure/error (Academic151, Auth54, Finance40, Teaching36, Gateway8), FE76, ops9, health19; lint/build đạt. 33 ca API/UI/race/replay duy nhất. Hibernate statistics xác nhận batch3lớp dùng2query. Hai90phút cộng180; không tính lịch đã qua là đã dạy, hủy/restore/edit/bù đúng; legacy thiếu giờ UNKNOWN/null; đóng lớp chưa đủ bị chặn. [G08](thesis-progress/G08.md), [G09](thesis-progress/G09.md). V011 chỉ apply trên restore, không nâng database nguồn. Không phải load test hoặc chứng nhận phục hồi production.

## G10 — 08/11/2026

323 backend tests,0failure/error (auth-service:54, academic-service:153, finance-service:40, teaching-service:68, api-gateway:8), FE79, ops9, health19 đạt; lint/build đạt. 30 API/UI/race/replay duy nhất. [G10](thesis-progress/G10.md) ghi công thức/rounding, required/optional, roster/reserved/transfer, attendanceunknown/excused, role/stale/race và UI. V012 chỉ trênrestore, sourceDB nguyên. Không chốt kết quả cuối khóa hoặc khẳng định snapshot xuyên HTTP nguyên tử.

## G11/G12 — 08/11/2026

335 backend tests tổng hợp,0failure/error: Auth54,Academic159,Teaching74,Finance40,Gateway8. FE82, lint/build đạt; ops9/health19. 51 ca runtime duy nhất API/UI/race/CRUD/fault/integrity; V013 apply/replay +prodvalidate trên schema restore. Dừng Teaching thật trả503 và rollback, restart retry được. Snapshot policy/full evidence immutable; nguồn đua với final một200/một409; generated unique current. Source SQL row counts/data fingerprint không đổi. [G11](thesis-progress/G11.md) và [G12](thesis-progress/G12.md) ghi lệnh/log, giới hạn browser/CRUD và triển khai. Chưa load test hoặc nghiệm thu production.


## G13/G14 — kiểm chứng tài liệu 08/11/2026

18 nhóm UC/RQ01–12 và22 sơ đồ canonical đã kiểm cú pháp/render SVG với PlantUML1.2025.10/Smetana; alias domain-model cũng kiểm/render. Bộ kiểm tĩnh scripts/check-thesis-docs.cjs kiểm link/anchor, hash source/SVG, đầy đủ entity/table/column và thuộc tính thật xuất hiện trong SVG (tránh SQL type có ngoặc bị renderer hiểu thành method). Metadata chỉ đọc xác nhận target53bảng/625cột,52entityclasses và current33bảng/388cột; không chạy DDL hoặc truy vấn dòng nghiệp vụ. [G13](thesis-progress/G13.md), [G14](thesis-progress/G14.md) ghi giới hạn. Không chạy lại335BE/82FE hoặc51runtimechecks cũ; không giả khảo sát thực tế đã có.

