# Giai đoạn 1 — Chốt hiện trạng và danh sách chỉnh sửa

Ngày rà soát: **11/11/2026**. Trạng thái: **Hoàn thành rà soát tĩnh giai đoạn 1**.

Nguồn yêu cầu: đề cương quản lý trung tâm ngoại ngữ và các yêu cầu bổ sung trong cuộc trao đổi: bốn role, nhiều chi nhánh, sidebar cố định, quản lý Admin, cột thao tác gọn, ngôn ngữ nghiệp vụ, dữ liệu mẫu nhất quán và lịch Time-grid.

## 1. Phạm vi và mức kết luận

- Đã đọc route/navigation, các phần FE mới sửa, controller/service liên quan, script dữ liệu mẫu, tài liệu yêu cầu và các bằng chứng kiểm tra đã có.
- Đã lập [inventory máy đọc được](2026-11-11-phase-1-inventory.json): 22 mục navigation có concrete page, 35 controller Java được liệt kê. Đây không phải số lượng API hoặc test.
- Theo đồ thị import tĩnh từ `src/main.tsx`, không có file mock reachable. Kết quả này không thay thế kiểm tra network runtime; thuật toán chỉ theo import/export tương đối và dynamic import chuỗi tĩnh.
- Không chạy lại backend/E2E, không truy vấn hay sửa database, không sửa nghiệp vụ, không xóa code cũ, không sửa gói release bất biến.
- Không coi lịch sử PASS ngày 10/11 là chứng nhận cho toàn bộ source ngày 11/11. Không kết luận có lỗi phân quyền/tính tiền nếu chưa tái hiện được.
- [requirements.md](../thesis/requirements.md) là baseline G01 ngày 07/11; các mục “thiếu” trong đó không tự động là thiếu hiện nay. Tài liệu này là mốc đối chiếu mới cho lộ trình 6 giai đoạn, không thay thế hồ sơ lịch sử.

## 2. Ma trận yêu cầu → FE → BE/dữ liệu → bằng chứng

| Mã | Yêu cầu | FE đang chạy | BE / dữ liệu đối chiếu | Kết luận hiện tại |
|---|---|---|---|---|
| RQ01 | Hồ sơ học viên, tìm kiếm, lớp đang học | `/students`, StudentManagement; tab hồ sơ/đăng ký/học tập/đầu vào/vòng đời | Academic `StudentController`, `LearningProfileController`; students/enrollments/enrollment_periods | Có triển khai. CRUD và hồ sơ có bằng chứng G17 cũ; cần kiểm lại UI mới và lịch sử chi tiết F01 |
| RQ02 | Giáo viên: liên hệ, chuyên môn, quản lý | `/teachers`: SA/Admin dùng AccountManagement; Staff dùng TeacherDirectory | Auth `TeacherProfileController`, `AuthController`; teachers/account_branches/teacher_profile_events | Có cập nhật/ngừng làm việc/bàn giao. TeacherManagement cũ không phải route thật |
| RQ03 | Khóa, học phí, trình độ, thời lượng | `/courses`, ProgramCatalog, AdmissionPanel, DurationPanel | Academic `CourseController`, `ProgramCatalogController`, `AdmissionPolicyController`, `DurationController` | Có danh mục và chính sách cấu trúc; vị trí danh mục khó tìm F02 |
| RQ04 | Lớp, phòng, sĩ số, đăng ký | `/classes`, `/rooms`, `/enrollment` | Academic `SchoolClassController`, `RoomController`, `EnrollmentController`; school_classes/rooms/enrollments | Có giới hạn sĩ số/đăng ký/bảo lưu/chuyển lớp. Nghiệm thu lại sau cập nhật, không tự mở rộng chuyển khác khóa/chi nhánh |
| RQ05 | Xếp lớp theo trình độ | StudentAssessments, EnrollmentPlacement và EnrollmentForm | `PlacementAssessmentController`: `/students/{studentId}/assessments`; `PlacementController`: POST `/enrollments/preview`, `/placement-exceptions` | Có đánh giá, xét điều kiện và ngoại lệ; không phải chức năng còn thiếu. Bộ demo chưa thể hiện F06 |
| RQ06 | Lịch học, lịch dạy, tránh trùng | `/schedule`, `/my-schedule` → ScheduleManagement → SessionManagement → SessionCalendar; lịch mẫu riêng cho quản lý | `SessionController`, `ScheduleController`; class_sessions/schedules | Time-grid đã có; còn UX, filter, query và kiểm thử tích hợp F03–F05/F09 |
| RQ07 | Chuyên cần, điểm, kết quả học tập | `/attendance`, `/exams`, `/grades`, LearningProfile | Academic attendance/duration/closure; Teaching exam/grade/evaluation/final result | Có quy tắc/chốt/mở lại/kết thúc lớp. Bộ demo thiếu phần cuối F07; chi tiết lịch sử bị giảm F01 |
| RQ08 | Học phí, đã đóng/còn nợ, lịch sử | `/tuition`, `/payments` | Finance `TuitionController`, `PaymentController`, `LedgerController`; tuition/payments/payment_corrections/tuition_adjustments | Có thu nhiều lần, điều chỉnh/đảo/hoàn. Chuyển khoản được nhân viên ghi nhận, không phải tích hợp ngân hàng |
| RQ09 | Tìm kiếm, thống kê học viên/doanh thu | `/dashboard`, `/reports`, bộ lọc từng danh sách | Academic `StatsController`; Finance `RevenueController` | Có chức năng và nghiệm thu báo cáo cũ. Cần đối soát dữ liệu/bản hiện tại F09 |
| RQ10 | Phân quyền, nhiều chi nhánh, dữ liệu nhất quán | NAVIGATION + ProtectedRoute; `/administrators`, `/accounts`, `/staff`, sidebar | Auth live account/session/branch assignment và guard tại service | Có triển khai. Menu/route không đủ để chứng minh quyền API; cần test role/branch/object F09 |
| RQ11 | Use Case, ERD, Class Diagram và khảo sát | docs/thesis, diagrams/design | Source/model và migration | Có sơ đồ. Khảo sát thực tế và đồng bộ tài liệu còn thiếu F10/F11 |
| RQ12 | Quy trình phát triển, kiểm thử, bàn giao | tests, scripts/acceptance, deployment, report | Test/release/backup/restore evidence | Có bằng chứng cho bản cũ; bản hiện tại cần qualification và gói mới F09/F11 |

Prefix API Academic trong bảng là `/api/v1/academic`; danh sách đầy đủ ở [api-list](../api-list.md). Các controller được liệt kê trong inventory; bảng này chỉ chọn điểm đối chiếu, không khẳng định đã review từng dòng của 35 controller.

## 3. Các yêu cầu UI bổ sung đã có

| Yêu cầu | Hiện trạng | Giới hạn |
|---|---|---|
| Sidebar không đi theo cuộn trang | DesktopSlot + aside fixed; menu sidebar cuộn riêng | Có bằng chứng kiểm tra trình duyệt trước đây; cần kiểm lại trong hồi quy cuối |
| Super Admin quản lý Admin | Menu `/administrators`, fixedRole ADMIN, phân công chi nhánh | Có UI mock verification; không gọi đây là nghiệm thu full-stack mới |
| Cột thao tác gọn | TableActions dùng chung, giữ thao tác chính và menu phụ | Có tests callbacks/confirmation/link, browser mock; một số bảng chỉ 1–2 nút được giữ trực tiếp |
| Bỏ chữ kỹ thuật/thử nghiệm | Đã dọn text cũ/snapshot/ghi chú cập nhật/role tag lặp | Có hồi quy về chi tiết kiểm toán F01, cần thay phần raw bằng bảng nghiệp vụ |
| Weekly Time-grid | Week/day/list, múi giờ Việt Nam, lấy đủ trang, buổi thiếu giờ, quyền thao tác | Không kéo thả; đây là lựa chọn phạm vi đã nêu, không phải lỗi. Các thiếu sót còn lại F03–F05 |

## 4. Danh sách chỉnh sửa đã chốt

P1 = cần xong trước nghiệm thu/bảo vệ; P2 = hoàn thiện sử dụng/bảo trì. “Đã xác nhận tĩnh” chỉ kết luận từ source; chưa là lỗi runtime đã tái hiện mới.

| ID | Ưu tiên / trạng thái | Dẫn chứng và ảnh hưởng | Công việc / tiêu chí hoàn thành | Giai đoạn |
|---|---|---|---|---|
| F01 | P1 / hồi quy UI xác nhận tĩnh | TeacherProfilePanel, SessionDetails, StudentAssessments và EnrollmentAdmissionHistory đã bỏ phần before/after hoặc căn cứ chi tiết; API vẫn lưu/trả dữ liệu | Đưa lại bảng so sánh có nhãn Việt, người/ngày/lý do. Đọc snapshot đúng định dạng; với dữ liệu không đọc được, báo rõ, không bịa giá trị và không mất audit gốc | 2 |
| F02 | P2 / UX xác nhận tĩnh | ProgramCatalog mở bằng nút chỉ SA trong CourseManagement | Tạo mục/đường dẫn rõ cho chương trình & trình độ, giữ quyền quản lý hiện có; có lối hướng dẫn từ màn hình đầu vào. Không tự cấp quyền sửa cho Staff/Admin | 2 |
| F03 | P2 / thiếu phần kế hoạch Time-grid | SessionCalendar chỉ nhận branchId/classId/status, toolbar chưa có chọn ngày; state ngày/view không ở URL | Thêm chọn ngày, filter giáo viên theo quyền/API, filter phòng theo phương án phù hợp; lưu ngày/view/filter URL, reload/back giữ lựa chọn | 3 |
| F04 | P2 / truy vấn thừa và trạng thái chéo xác nhận tĩnh | SessionManagement luôn gọi sessionService.list với khoảng ngày riêng; SessionCalendar gọi sessionService.all theo range riêng; cảnh báo `!valid` vẫn dùng range danh sách | Chỉ tải nguồn cần cho chế độ đang mở; tách lỗi/loading/validation/count đúng chế độ. Khoảng ngày sai của danh sách không làm cảnh báo sai trên lịch. Giữ lựa chọn lớp dạy thay và tải đủ trang | 3 |
| F05 | P2 / UX xác nhận tĩnh, mức ảnh hưởng cần thử thêm | scrollTime cố định 07:00; nút prev/next nhưng thiếu jump date; card ngắn/nhiều buổi cần thử | Cuộn hợp lý tới buổi học/giờ đang xem; nội dung chính đọc/chọn được khi dày. Không che mất buổi sớm/muộn, hủy hoặc thiếu giờ; kiểm bàn phím và mobile | 3 |
| F06 | P1 / khoảng trống fixture xác nhận SQL | reset-and-seed-full.sql tạo cả 4 policy OPEN_ENTRY | Có khóa yêu cầu đánh giá, đủ/sai/thiếu trình độ, sai chương trình và ngoại lệ đúng quyền; tạo qua nghiệp vụ, không gắn ELIGIBLE thủ công | 4 |
| F07 | P1 / khoảng trống fixture xác nhận SQL | Script xóa evaluation_policies/final_results/class_closures/learning_events nhưng không tạo lại dữ liệu tương ứng | Tạo kịch bản đủ/thiếu điểm, đạt/chưa đạt, chốt/mở lại/hoàn tất/đóng lớp và lịch sử. Kiểm số liệu ở API, UI, MySQL | 4 |
| F08 | P1 / fixture không có nguồn nghiệp vụ đầy đủ | enrollment_admissions dùng SHA2('demo-enrollment-N') và text DEMO thay căn cứ do service sinh; nhiều snapshot và lịch sử được chèn trực tiếp | Bộ nghiệm thu mới tạo qua API trên schema demo riêng. Giữ script SQL hiện tại có nhãn fixture xem UI, không dùng làm chứng nhận toàn vẹn nghiệp vụ | 4 |
| F09 | P1 / thiếu bằng chứng hiện hành | Time-grid/admin/table checks mới dùng mock; G20 runtime thật là snapshot 10/11 | Chạy 4 role + 2 branch trên FE/Gateway/services/MySQL thật, ca thành công/sai quyền/xung đột/race/timeout, đối soát báo cáo và restore. Lưu expected/actual và source digest mới | 5 |
| F10 | P1 / hồ sơ chưa đủ | survey.md chưa có bằng chứng thực địa; report còn mẫu trường/thông tin cá nhân/nguồn khảo sát | Người dùng cung cấp khảo sát thực; ghi rõ giả định chưa kiểm chứng. Hoàn thiện báo cáo theo mẫu khoa; không tạo lời phỏng vấn hoặc kết quả nghiệm thu giả | 6, chuẩn bị từ 1 |
| F11 | P1 / tài liệu/release lệch mốc | baseline requirements 07/11; traceability còn nói G20 chưa chốt trong khi G20.md có qualification local; ZIP 10/11 không chứa UI mới | Đồng bộ ma trận, hướng dẫn/API/ảnh/sơ đồ/báo cáo theo bản cuối. Giữ snapshot cũ, tạo release mới sau nghiệm thu thay vì sửa ZIP cũ | 6 |
| F12 | P2 / code tồn dư xác nhận đồ thị import | 9 file data/*.mock.ts; TeacherManagement/StaffManagement và styles không reachable từ main theo inventory | Kiểm cả tests/docs/scripts/import khác trước khi xóa; dọn hoặc lưu historical có nhãn rõ. Không gọi các trang này là UI thật đang dùng mock | 2 |

### Không kết luận là lỗi hoặc yêu cầu thiếu

- Chưa có học online, thi online, QR, app mobile, Student role, ngân hàng tự động: ngoài phạm vi bắt buộc của đề cương hiện tại.
- Không có thao tác xóa cứng dữ liệu đã có lịch sử: chủ ý bảo toàn dữ liệu; cần diễn đạt Use Case đúng. Không thêm DELETE chỉ để đủ chữ CRUD.
- Bốn lớp mẫu OPEN_ENTRY: không sai schema, nhưng không đủ chứng minh phân lớp theo trình độ; Giao tiếp 2 không nên gọi là “nhập môn không cần đầu vào” nếu demo tiến cấp.
- Lớp học xong nhưng FAIL có thể hoàn thành học thuật; nợ không mặc nhiên chặn hoàn thành: giữ quy tắc đã triển khai, không tự đổi vì tên trạng thái.
- Không có lỗi tính tiền hoặc hở quyền mới nào được chứng minh trong lượt rà soát này. Các rủi ro cần tái kiểm nằm trong F09.

## 5. Phân quyền dùng làm mốc nghiệm thu

| Role | Mốc cần giữ |
|---|---|
| Super Admin | Quản lý toàn hệ thống, Admin và phân công chi nhánh; danh mục chung; không bỏ audit khi sửa dữ liệu |
| Admin | Trong các chi nhánh được giao; quản lý nhân sự/học vụ/tài chính theo contract; không tự nâng quyền hoặc sửa phạm vi toàn hệ thống |
| Staff | Tác nghiệp trong chi nhánh được giao; không tự duyệt ngoại lệ, sửa học phí hoặc có quyền quản trị lịch vì UI đổi view |
| Teacher | Chỉ lớp/buổi được phân công; dạy thay cấp quyền đúng buổi; không truy cập tài chính/quản lý Admin |

Đây là mốc rà soát, không thay [bảng quyền chi tiết](../thesis/role-permissions.md). Backend phải kiểm tra role + chi nhánh + đối tượng, không dựa vào việc ẩn nút.

## 6. Bằng chứng hiện có và phần chưa chạy

| Bằng chứng | Phạm vi | Cách sử dụng |
|---|---|---|
| storage/g20-backend-tests.json | 335 tests Java, zero failures/errors/skipped ở mốc G20 | Bằng chứng cũ, không báo là đã chạy lại ở giai đoạn 1 |
| storage/acceptance/20261110-g20-3d704261/acceptance-summary.json | 86 ca runtime thật, PASSED cho snapshot 10/11 | Mốc regression, không tự gán PASS cho UI/fixture mới |
| storage/admin-preview/result.json | Tạo Admin/phân công/guard với mock | Bằng chứng riêng của UI |
| storage/table-actions-preview/result.json | Menu/confirm/hủy/link với mock | Bằng chứng riêng của UI |
| storage/timegrid-preview/result.json | Week/day/list, pagination, timezone, missing hours, teacher actions, mobile với mock | Chưa thay thế full-stack calendar acceptance |
| 85 tests FE, lint/build | Đã đạt ở lượt cập nhật Time-grid ngay trước giai đoạn 1 | Không chạy lại chỉ vì ghi tài liệu; không gọi 85 tests là 85 bài kiểm tra trình duyệt |

Hash của các file bằng chứng và fingerprint FE được lưu trong inventory. Không bao gồm mật khẩu, JWT, .env.local, dữ liệu cá nhân hoặc nội dung backup.

## 7. Thứ tự bàn giao tiếp theo

1. **Giai đoạn 2:** F01 → F02 → F12. Không xóa audit, không thay quyền, không reset database.
2. **Giai đoạn 3:** F04 → F03 → F05. Nghiệm thu calendar state/query và các role trước khi đổi thêm tương tác.
3. **Giai đoạn 4:** F06 → F07 → F08. Dùng schema demo riêng, fixture qua API, có kịch bản và số liệu mong đợi từ đầu.
4. **Giai đoạn 5:** F09. Lỗi phát hiện quay lại công việc tương ứng; chỉ đánh dấu đạt khi có bằng chứng.
5. **Giai đoạn 6:** F10/F11. Khảo sát do người dùng cung cấp; đồng bộ tài liệu và đóng release mới.

## 8. Tiêu chí đóng giai đoạn 1

- [x] Phân biệt yêu cầu gốc, yêu cầu UI bổ sung và phần ngoài phạm vi.
- [x] 12 nhóm yêu cầu có điểm đối chiếu FE/BE/dữ liệu/bằng chứng.
- [x] 22 navigation item có màn hình triển khai, không có fallback trong PAGES hiện tại.
- [x] Code mock được phân loại theo import graph; không nhầm file tồn tại với route thật.
- [x] 12 công việc có ID, ưu tiên, dẫn chứng, tiêu chí và giai đoạn nhận việc.
- [x] Bằng chứng cũ/mới/mock/runtime thật được tách rõ.
- [x] Không sửa nghiệp vụ hoặc dữ liệu, không tuyên bố đã đạt nghiệm thu runtime mới.
