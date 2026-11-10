# Yêu cầu và đối chiếu hiện trạng — G01

Ngày rà soát: **07/11/2026**. Nguồn yêu cầu: đề cương do người dùng cung cấp trong cuộc trao đổi. Mục tiêu là ứng dụng web quản lý trung tâm tiếng Anh, gồm học viên, giáo viên, khóa/lớp/lịch, phân lớp, học tập, học phí, tìm kiếm/thống kê và bộ tài liệu khóa luận.

## 1. Phạm vi và mức bằng chứng

- Giữ bốn role `SUPER_ADMIN` (SA), `ADMIN` (A), `STAFF` (S), `TEACHER` (T). Học viên là hồ sơ được quản lý, chưa phải actor có tài khoản trong sản phẩm hiện tại.
- Backend: Auth sở hữu nhân sự/tài khoản; Academic sở hữu học vụ; Teaching sở hữu bài kiểm tra/điểm/tài liệu; Finance sở hữu học phí/thu tiền; Gateway định tuyến. Hiện dùng chung MySQL/schema, không phải mỗi service có một database riêng.
- FE có route thật và API service cho các trang nêu dưới, dùng Ant Design/styled-components. Một số file mock/TeacherManagement cũ vẫn tồn tại; không dùng sự tồn tại của file đó làm bằng chứng màn hình đang chạy. `src/App.tsx` hiện đưa SA/A vào `AccountManagement fixedRole="TEACHER"`, S vào `TeacherDirectory`.
- G01 kiểm tra tĩnh source, DTO, controller/service, entity, migration, navigation và tài liệu/test đã có. **Không truy vấn schema/dữ liệu MySQL đang chạy, không chạy UI/E2E hoặc chạy lại test nghiệp vụ trong G01.** Các dòng “có” dưới đây là xác nhận bằng code, không phải chứng nhận runtime mới.
- Quy tắc mục tiêu và giả định dùng để code nằm ở [business-rules](business-rules.md). Mã TC dưới đây là ca nghiệm thu dự kiến, chưa được tính là test đã đạt.

## 2. Danh mục yêu cầu

### RQ01 — Quản lý học viên

**Yêu cầu:** lưu họ tên, ngày sinh, giới tính, liên hệ, trình độ và lớp theo học; thêm/sửa/xóa có kiểm soát, tìm kiếm, theo dõi lịch sử.

**Có trong code:** `academic-service/.../student/Student.java`, `StudentDto`, `StudentService`; FE `pages/StudentManagement`. Có mã học viên, người giám hộ, chi nhánh, lịch sử đăng ký/vòng đời. DTO bắt buộc tên/điện thoại; ngày sinh không ở tương lai; thông tin chưa biết có thể null. Xóa bị chặn khi có Enrollment hoặc Lead chuyển đổi tham chiếu.

**Thiếu/cần kiểm tra:** `currentLevel` đang là text; chưa có đánh giá đầu vào có cấu trúc và hồ sơ học tập tổng hợp. Khi thêm bảng mới phải mở rộng guard xóa học viên, không để mất đánh giá/kết quả.

**Nghiệm thu:** TC-RQ01-01 tạo/sửa/reload/tìm kiếm đúng; TC-RQ01-02 không xóa hồ sơ đã có nghiệp vụ; TC-RQ01-03 xem lớp, đầu vào, chuyên cần/kết quả theo đúng quyền. G05, G12, G17–G18.

### RQ02 — Quản lý giáo viên

**Yêu cầu:** họ tên, chuyên môn, liên hệ; tạo/xem/sửa, ngừng làm việc và phân công hợp lý.

**Có trong code:** Auth `entity/Teacher`, `dto/CreateUserRequest`, `service/AuthService`, `controller/AuthController`; FE `pages/AccountManagement`, `TeacherDirectory`. Tạo hồ sơ có bằng cấp/kinh nghiệm/chứng chỉ/ngày vào làm; tra cứu giáo viên khả dụng kiểm tra cả account.status và workStatus. Đã có khóa tài khoản/phân công chi nhánh; thu hồi chi nhánh kiểm tra lớp và buổi chưa xử lý qua `integration/BranchDirectory`.

**Thiếu/cần kiểm tra:** chưa có API quản trị cập nhật đầy đủ hồ sơ, version của Teacher hoặc lịch sử sửa hồ sơ; tự sửa `/me/profile` chỉ email/phone. Chưa có thao tác trạng thái làm việc/bàn giao tổng hợp. Tìm kiếm tài khoản quản trị hiện theo username, chưa hỗ trợ tên hồ sơ đầy đủ.

**Nghiệm thu:** TC-RQ02-01 cập nhật hồ sơ và stale version; TC-RQ02-02 đúng quyền toàn bộ phân công; TC-RQ02-03 ngừng khi còn việc bị chặn, bàn giao xong được; TC-RQ02-04 race ngừng/thu hồi với phân công không tạo trách nhiệm cho người không khả dụng. G02–G03, G18.

### RQ03 — Quản lý khóa học

**Yêu cầu:** tên/chương trình tiếng Anh, trình độ, thời lượng, học phí và điều kiện đào tạo.

**Có trong code:** Academic `course/Course`, `CourseDto`, `CourseService`; FE `pages/CourseManagement`. Có type/level/entryRequirement dạng text, duration integer, học phí, maxStudents, trạng thái và khóa chung/chi nhánh.

**Thiếu/cần kiểm tra:** chưa có thang trình độ có cấu trúc; đầu vào và đầu ra chưa tách rõ; duration không có đơn vị xác định trong DTO/UI. Không đủ căn cứ chuyển dữ liệu cũ thành giờ hoặc trình độ chuẩn.

**Nghiệm thu:** TC-RQ03-01 CRUD/trạng thái/quyền khóa chung; TC-RQ03-02 đầu vào khác mục tiêu đầu ra, không quy đổi thang ngầm; TC-RQ03-03 hiển thị phút/giờ và giữ nguyên dữ liệu cũ chưa xác minh. G04, G08.

### RQ04 — Quản lý lớp học và ghi danh

**Yêu cầu:** lớp gắn khóa, giáo viên, lịch/phòng, giới hạn sĩ số và danh sách học viên.

**Có trong code:** Academic `schoolclass/SchoolClassService`, `enrollment/EnrollmentService`, `EnrollmentPeriod`; FE `ClassManagement`, `EnrollmentManagement`. Có kiểm tra sức chứa, mã lớp, giáo viên, ghi danh trùng, trạng thái, chi nhánh/lịch; bảo lưu/học lại/chuyển cùng khóa và chi nhánh giữ billing gốc.

**Thiếu/cần kiểm tra:** ghi danh chưa đối chiếu đầu vào; trạng thái kết thúc lớp/đăng ký chưa gắn xác nhận kết quả. Cần giữ học phí/idempotency khi thêm các guard mới.

**Nghiệm thu:** TC-RQ04-01 danh sách/chi tiết/ghi danh đúng; TC-RQ04-02 lớp đầy/trùng/ngoài chi nhánh bị chặn; TC-RQ04-03 hai request chỗ cuối chỉ một thành công; TC-RQ04-04 resume/transfer giữ lịch sử và billing, áp dụng đúng chính sách đầu vào. G06–G07, G11, G17–G18.

### RQ05 — Phân lớp theo trình độ

**Yêu cầu:** dựa trên trình độ/đánh giá đầu vào để hỗ trợ chọn lớp phù hợp.

**Có trong code:** hồ sơ có text currentLevel; khóa có level/entryRequirement; FE chọn lớp thủ công. `EnrollmentService.check()` hiện kiểm tra học vụ, **không kiểm tra học lực đầu vào**.

**Thiếu:** danh mục chương trình/thang, đánh giá có nguồn/ngày/người ghi nhận, preview phù hợp/gợi ý lớp, căn cứ lúc ghi danh và luồng quản lý xác nhận ngoại lệ.

**Nghiệm thu:** TC-RQ05-01 đánh giá và lớp phù hợp; TC-RQ05-02 thiếu/sai thang trả lý do; TC-RQ05-03 xác nhận ngoại lệ đúng chủ thể/căn cứ, Staff không tự duyệt; TC-RQ05-04 approval cũ hoặc dùng đồng thời không tạo đăng ký sai/trùng. G04–G07, G18.

### RQ06 — Quản lý lịch và buổi học

**Yêu cầu:** lịch theo lớp, tránh trùng giáo viên/phòng/học viên; theo dõi buổi thực tế.

**Có trong code:** Academic `schedule`, `session/ClassSession`, `SessionPlanner`; FE `ScheduleManagement`. Có SCHEDULED/CANCELLED/COMPLETED, nguồn tuần, buổi bù, giáo viên thực tế, version/audit. Điểm danh lưu đủ sheet sẽ gọi `sessions.complete(session)`; không cần thêm một luồng hoàn tất buổi trùng chức năng.

**Thiếu/cần kiểm tra:** tổng giờ kế hoạch/đã xếp/đã hoàn tất chưa thành chức năng rõ; một buổi hoạt động/lớp/ngày là giới hạn hiện tại, không mở đa ca cùng ngày trong đợt này.

**Nghiệm thu:** TC-RQ06-01 kiểm tra xung đột; TC-RQ06-02 nghỉ/bù/khôi phục có lịch sử; TC-RQ06-03 tổng giờ đúng, legacy thiếu giờ là chưa đủ dữ liệu. G03, G09, G17–G18.

### RQ07 — Theo dõi quá trình và kết quả học tập

**Yêu cầu:** điểm danh, bài kiểm tra/điểm/nhận xét, tiến độ và kết quả học tập.

**Có trong code:** Academic attendance/periods; Teaching `exam/ExamService`, `ExamCandidate`, `grade/GradeService`; FE Attendance/Exam/Grade/Reports. Có roster theo ngày và chốt khi nhập điểm, lịch sử sửa điểm, trọng số/thang điểm. `GradeService.evaluations()` chỉ tính các bài đã có điểm, passed >=50%; đây là điểm tạm, chưa phải xác nhận cuối khóa.

**Thiếu:** bài bắt buộc/quy tắc xác nhận theo lớp, lý do chưa đủ dữ liệu, kết quả chốt theo enrollment, mở lại có audit và kiểm soát các endpoint COMPLETED/FINISHED hiện có.

**Nghiệm thu:** TC-RQ07-01 điểm 0 khác chưa chấm, roster đúng ngày; TC-RQ07-02 thiếu bài chưa được chốt; TC-RQ07-03 chốt/mở lại/version/quyền đúng; TC-RQ07-04 sửa nguồn không làm đổi snapshot đã chốt; TC-RQ07-05 học xong nhưng không đạt vẫn phân biệt được với chưa học xong. G09–G12, G18.

### RQ08 — Học phí và thanh toán

**Yêu cầu:** mức học phí, đã đóng/chưa đóng/một phần, lịch sử thu và số còn nợ rõ ràng.

**Có trong code:** Finance tuition/payment/ledger; FE TuitionManagement/PaymentManagement. Billing gốc theo enrollment, tiền snapshot, requestKey, thu nhiều lần, journal điều chỉnh/đảo/hoàn, bảo toàn phiếu gốc; tạo học phí thất bại có retry độc lập với ghi danh.

**Thiếu/cần kiểm tra:** không thiếu module cơ bản theo đọc code; cần đối soát trên bộ dữ liệu nghiệm thu và regression khi sửa enrollment. Chuyển khoản là ghi nhận thủ công, chưa tự xác nhận từ ngân hàng.

**Nghiệm thu:** TC-RQ08-01 học phí 3 triệu, thu 1 rồi 2 triệu, phiếu/đã thu/nợ khớp; TC-RQ08-02 retry/timeout không nhân đôi; TC-RQ08-03 điều chỉnh/đảo/hoàn và dư đúng giới hạn, đúng quyền. G17–G18.

### RQ09 — Tìm kiếm, báo cáo và thống kê

**Yêu cầu:** tìm kiếm danh sách, số học viên/lớp, doanh thu cơ bản và công nợ.

**Có trong code:** Academic StatsController/StatsService; Finance `stats/RevenueController`; FE Overview/Reports/reportService và filter các danh sách. Finance báo cáo grossCollected, voided, refunded, collected ròng, outstanding/credit; outstanding là snapshot hiện tại, không phải công nợ tái dựng cuối kỳ.

**Thiếu/cần kiểm tra:** tìm theo tên giáo viên ở màn quản trị; số liệu học tập phải phân biệt tạm/chốt; nghiệm thu số đếm distinct học viên với số chỗ ghi danh và phạm vi/kỳ báo cáo.

**Nghiệm thu:** TC-RQ09-01 lọc/phân trang/phạm vi khớp; TC-RQ09-02 số thu gốc/đảo/hoàn/ròng và kỳ đúng expected; TC-RQ09-03 hồ sơ học tập/đạt tạm/đạt cuối không bị cộng lẫn. G02, G12, G17–G18.

### RQ10 — Phân quyền, nhất quán và trải nghiệm

**Yêu cầu:** role/phạm vi đúng, dữ liệu toàn vẹn, lỗi rõ, giao diện dùng được.

**Có trong code:** SecurityConfig/BranchScope/CurrentUser và Auth live session; FE navigation/ProtectedRoute/useRemoteResource. Có version/lock/audit ở nhiều nghiệp vụ, guard form ở một số luồng, health/prod/backup.

**Thiếu/cần kiểm tra:** Teacher profile chưa version; các nghiệp vụ mới cần guard; form chưa lưu chưa phủ mọi màn/điều hướng; chưa có kết quả usability khảo sát thật hoặc load test production.

**Nghiệm thu:** TC-RQ10-01 API trái quyền và ngoài phạm vi bị chặn; TC-RQ10-02 stale/race/timeout không làm sai dữ liệu; TC-RQ10-03 đổi chi nhánh/reload/mobile và trạng thái lỗi không lẫn dữ liệu. G02–G12, G18.

### RQ11 — Phân tích và thiết kế hệ thống

**Yêu cầu:** Use Case, ERD, Class Diagram và tài liệu giải thích thiết kế.

**Có trong repository:** `docs/domain-model.puml` ở mức khái quát, API/contracts/decisions/migrations/verification và hướng dẫn module.

**Thiếu/cần kiểm tra:** chưa thấy bộ Use Case/ERD/từ điển và thiết kế động hoàn chỉnh; tài liệu cũ còn đoạn mâu thuẫn về API count, hoàn tiền, health và version. Không kết luận tài liệu ngoài repository không tồn tại.

**Nghiệm thu:** TC-RQ11-01 sơ đồ/đặc tả và code/schema cuối khớp; TC-RQ11-02 đủ ràng buộc/luồng lỗi/quyền; TC-RQ11-03 link/source diagram hợp lệ và tham chiếu RQ đầy đủ. G13–G16.

### RQ12 — Quy trình phát triển, kiểm thử và báo cáo khóa luận

**Yêu cầu:** khảo sát, phân tích–thiết kế–cài đặt–kiểm thử; sản phẩm ổn định và báo cáo hoàn chỉnh.

**Có:** source BE/FE, test sources, kết quả lịch sử trong verification và operations/release tooling. Chưa có đủ đầu vào để xác nhận báo cáo nộp theo mẫu khoa hoặc khảo sát thật đã hoàn tất.

**Nghiệm thu:** TC-RQ12-01 test report có expected/actual/ngày/môi trường; TC-RQ12-02 demo và backup/restore schema cuối có bằng chứng; TC-RQ12-03 báo cáo/source/build đồng bộ, phần thiếu thông tin bắt buộc được bổ sung trước khi gọi là bản nộp. G01, G17–G20.

## 3. Ngoài phạm vi bắt buộc

E-learning, lớp online/video, thi trực tuyến/thi thử tự động, QR, portal học viên, mobile app, thanh toán qua cổng/ngân hàng tự động, refresh token/email OTP, kế toán chi phí/lương, chứng chỉ pháp lý, đa ca/lớp/ngày và chuyển khác khóa/chi nhánh. Các mục này chỉ trở thành yêu cầu mới khi có quyết định mở phạm vi; không phải lý do trì hoãn các tiêu chí cơ bản.

## 4. Hoàn thành sản phẩm

Từng RQ phải có tiêu chí và bằng chứng đạt, không chỉ có API. G01 chốt đặc tả; G02–G12 cài đặt, G13–G16 chốt tài liệu, G17–G18 nghiệm thu, G19 hoàn thiện báo cáo và G20 xác minh bản bàn giao. Không dùng lịch sử test cũ để đánh dấu các yêu cầu mới đã đạt.

## Cập nhật sau G02–G03 — 07/11/2026

Phần hiện trạng G01 phía trên được giữ như baseline lịch sử. RQ02 đã bổ sung quản trị detail/update/version/audit, tự sửa liên hệ tham gia version, trạng thái làm việc và bàn giao theo BR-01/02/03; phần tìm kiếm tên Teacher của RQ09 đã có. Các ca RQ02-01/02/03/04 đã được kiểm chứng trong phạm vi H2 + API/MySQL/UI/race được ghi ở bàn giao. RQ05/đơn vị thời lượng/kết quả cuối chưa triển khai. [G02](../thesis-progress/G02.md) · [G03](../thesis-progress/G03.md).

## Hiện trạng sau G13/G14 — 08/11/2026

Các phần thiếu/có trong từng RQ phía trên là baseline G01 và các bàn giao theo thời điểm, không tự coi là hiện trạng cuối. G02–G12 đã bổ sung assessment/policy, phân lớp/ngoại lệ, thời lượng, kết quả chính thức và hồ sơ tổng hợp. [Truy vết hiện tại](traceability.md) nối đủ RQ01–RQ12 tới18 nhóm Use Case và nguồn/bàn giao. [Thiết kế G14](design/README.md) đối chiếu source với schema bản sao V013. RQ11 còn G15/G16; RQ12, nghiệm thu cuối và khảo sát thực địa chưa hoàn tất. Không dùng việc có tài liệu hoặc test cũ để gán mọi TC-RQ dự kiến đã đạt.
