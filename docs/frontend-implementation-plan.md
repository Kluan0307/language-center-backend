# Cập nhật cải thiện giai đoạn 2

Đã triển khai đổi/cấp lại mật khẩu, phiên và liên hệ cá nhân. Hướng dẫn hiện hành: [Giai đoạn 2](improvements/phase-two-account-security.md). Phần kế hoạch gốc phía dưới là lịch sử.

# Cập nhật cải thiện giai đoạn 1

Đã triển khai mô hình nhiều chi nhánh, phân quyền hiện tại, bộ chọn chi nhánh và tổng hợp báo cáo. Quy tắc hiện hành: [Cải thiện giai đoạn 1](improvements/phase-one-multi-branch.md). Các mô tả giới hạn một chi nhánh trong kế hoạch gốc bên dưới đã được thay thế.

# Phân tích và kế hoạch frontend Language Center

> Bản phân tích ban đầu ngày 02/11/2026. Giai đoạn 1 đã được triển khai ngày 04/11/2026;
> xem [chức năng và kết quả mới nhất](../../../language-center-web-frontend/docs/phase-one.md).
> Các mô tả hiện trạng dưới đây là trước khi hoàn thiện sidebar và giai đoạn 1.

> Giai đoạn 2 cũng đã triển khai ngày 04/11/2026:
> [Khóa học, Phòng, Lớp và Lịch](../../../language-center-web-frontend/docs/phase-two.md).

> Giai đoạn 3 đã triển khai ngày 04/11/2026:
> [Tuyển sinh, học viên và ghi danh](../../../language-center-web-frontend/docs/phase-three.md).
> Giai đoạn 4 đã triển khai ngày 04/11/2026:
> [Học phí, phiếu thu và công nợ](../../../language-center-web-frontend/docs/phase-four.md).
> Giai đoạn 5 đã triển khai ngày 04/11/2026:
> [Điểm danh và giảng dạy](../../../language-center-web-frontend/docs/phase-five.md).
> Giai đoạn 6 đã triển khai ngày 04/11/2026:
> [Dashboard và báo cáo](../../../language-center-web-frontend/docs/phase-six.md).
> Sáu giai đoạn MVP đã triển khai. Tiếp theo: [demo và nghiệm thu](../../../language-center-web-frontend/docs/acceptance-guide.md).

Ngày đối chiếu: 02/11/2026. Frontend được đọc tại `../../language-center-web-frontend`
tính từ repository backend. Đây là kế hoạch triển khai, không phải xác nhận tính năng đã chạy E2E.
Đối chiếu với [API hiện có](api-list.md), [hợp đồng](api-contracts.md) và
[quyết định nghiệp vụ](implementation-decisions.md).

## Hiện trạng xác minh

- React, TypeScript, Vite, React Router, Ant Design, styled-components, Axios.
- 11 thư mục trang: Login, Overview và 9 nhóm nghiệp vụ.
- 9 nhóm đều dùng mock/local state: Lead, Student, Teacher, Staff, Course, Class,
  Room, Enrollment, Attendance. Tải lại trang không bảo toàn những thay đổi trong state.
- Có service login/me/accounts/create/toggle, nhưng hook accounts chưa được trang nào dùng.
- 13 route placeholder cùng render Overview: overview, dashboard, schedule, exams,
  grades, documents, tuition, reports, accounts, payments, stats, my-classes, my-schedule.
- Role chỉ có ADMIN/STAFF/TEACHER; chưa có SUPER_ADMIN hoặc ngữ cảnh chi nhánh.
- ProtectedRoute hỗ trợ allowedRoles nhưng App không truyền; người đăng nhập có thể
  mở trực tiếp mọi trang nghiệp vụ đang khai báo. Ẩn sidebar không bảo vệ route.
- `.env` trỏ `http://localhost:8081/api/v1`, cần đổi sang gateway 8080 khi tích hợp.
- Kiểm tra `tsc --noEmit -p tsconfig.app.json`: thành công. ESLint: 7 lỗi.
  Chưa chạy Vite production build hoặc kiểm thử trình duyệt/backend thật trong đợt phân tích này.

## Các trang theo role

Đề xuất dùng một ứng dụng và các trang dùng chung, kiểm soát cả route lẫn hành động.
Phạm vi dữ liệu cuối cùng luôn do backend xác minh.

| Nhóm trang | SUPER_ADMIN | ADMIN | STAFF | TEACHER |
|---|---|---|---|---|
| Dashboard | Toàn chuỗi/theo chi nhánh | Chi nhánh | Vận hành chi nhánh | Lớp/lịch của mình |
| Chi nhánh | Tạo/sửa/bật tắt | Xem của mình | Thông tin của mình | Thông tin của mình |
| Tài khoản/nhân sự | Tạo A/S/T, khóa theo quy tắc | Tạo/khóa S/T cùng chi nhánh | Danh sách GV khả dụng | Hồ sơ bản thân |
| Lead và học viên | Theo phạm vi chọn | Chi nhánh | Chi nhánh | Không có trang danh bạ toàn chi nhánh |
| Khóa học | Quản lý cả khóa chung | Quản lý khóa chi nhánh | Xem/sửa phần được phép, không đổi giá/tạo mới | Xem liên quan |
| Phòng | Quản lý | Quản lý | Quản lý theo API | Không có trang quản lý |
| Lớp | Quản lý | Quản lý | Tạo/sửa giới hạn | Lớp của tôi và danh sách học viên trong lớp |
| Lịch | Xem/điều phối | Xem/điều phối | Xem | Lịch dạy của tôi |
| Đăng ký/xếp lớp | Thao tác | Thao tác | Thao tác | Không |
| Điểm danh | Theo phạm vi | Chi nhánh | Chi nhánh | Chỉ lớp mình |
| Học phí/phiếu thu | Thu tiền, điều chỉnh | Thu tiền, điều chỉnh | Thu tiền, không điều chỉnh học phí | Không |
| Báo cáo tài chính | Toàn chuỗi/chi nhánh | Chi nhánh | Không | Không |
| Kiểm tra/điểm | Quản lý; sửa điểm đã có kèm lý do | Tương tự trong chi nhánh | Không | Tạo bài, nhập/sửa điểm lớp mình |
| Đánh giá tổng hợp | Có | Có | Không | Chưa có quyền API evaluation |
| Tài liệu | Upload/duyệt/xem | Upload/duyệt/xem chi nhánh | Xem/tải APPROVED | Upload/xem tài liệu mình đăng |

Không cần menu riêng cho mọi chức năng: phiếu thu có thể là tab trong Tài chính;
điểm, kiểm tra, lịch và chuyên cần có thể là tab của chi tiết lớp.
Chi nhánh với A/S/T là thông tin cố định, không phải bộ chọn chuyển chi nhánh.
SA chọn chi nhánh trước thao tác tạo dữ liệu; chế độ toàn chuỗi dùng cho xem/báo cáo.

## Những điểm phải sửa khi tích hợp

1. Auth model: thêm SUPER_ADMIN, accountId, profileId, branchId. Teacher assignment
   dùng profileId, không dùng accountId. Accounts trả PageResponse, không phải mảng.
2. Không tạo user giả id=0 khi me thất bại. Phân biệt 401 với lỗi mạng/503;
   chỉ lỗi xác thực mới kết thúc phiên. Bảo vệ JSON.parse storage; đồng bộ trạng thái phiên.
3. Nút Remember me chưa quyết định nơi lưu phiên; CAPTCHA chỉ kiểm tra ở trình duyệt,
   không phải cơ chế chống tự động được server xác thực.
4. Types Student thêm mã/chi nhánh/người giám hộ; Enrollment thêm billing root,
   tuitionDelivery; Class dùng schedule có cấu trúc, tách progress và full.
   Teacher workStatus cần theo enum backend, không giữ WORKING/RESIGNED theo mock.
5. Lead convert gọi endpoint transaction, chỉ CONTACTED/CONSULTING; không tự sinh ID,
   push mockStudents hoặc gán ngày sinh giả 2005-01-01.
6. Enrollment TRANSFERRED là kết quả endpoint transfer với lớp đích, không là lựa chọn
   cập nhật status thông thường. Giữ cùng khóa/chi nhánh và học phí gốc.
7. Enrollment thành công nhưng tuition FAILED phải hiển thị rõ và có retry;
   không báo cả đăng ký thất bại rồi tạo lại.
8. Conflict helper đang so chuỗi ngày/giờ bằng nhau: bỏ sót giao nhau một phần và
   không xét khoảng ngày lớp. Dùng conflict API cho preview; lưu vẫn xử lý 409.
9. Attendance lấy roster theo ngày từ sheet API, không dựng từ STUDYING hiện tại.
   Cho xem lịch sử lớp đã kết thúc; không giới hạn dropdown vào ONGOING/FULL.
   Trường note ở giao diện chưa có trong AttendanceBulk; cần bỏ hoặc bổ sung hợp đồng riêng.
10. Course không có DELETE API; Teacher/Staff không có sửa/xóa hồ sơ API.
    Thay bằng trạng thái/khóa tài khoản khi phù hợp; không giữ nút hứa thao tác chưa được hỗ trợ.
11. Phân trang/lọc qua server, quy đổi page UI bắt đầu 1 sang API bắt đầu 0.
    Không tính tổng hệ thống bằng số dòng của một trang đã tải.
12. Ngày học là YYYY-MM-DD theo Asia/Ho_Chi_Minh; tránh cắt UTC ISO làm lệch ngày.
    Giá/thu tiền hiển thị VND; số dư luôn lấy lại từ server.

## Kiến trúc đề xuất

- Giữ React/TS/Vite và giao diện đang có; không cần viết lại toàn bộ.
- Layout chung chứa Sidebar, header, thông tin user và ngữ cảnh chi nhánh.
- Một cấu hình route/permission dùng để tạo menu và guard. Permission theo hành động
  và đối tượng: cùng role nhưng khóa chung, lớp mình, điểm đã có có quyền khác nhau.
- Feature folders auth, organization, admissions, academics, attendance, finance, teaching.
- API modules riêng cho 4 service, đều qua một Axios client gateway.
- DTO request/response riêng; adapter chỉ khi UI cần cấu trúc khác. Không ép kiểu để che
  khác biệt hợp đồng. Chuẩn hóa PageResponse và lỗi validation/401/403/409/503.
- Đề xuất dùng TanStack Query cho dữ liệu server, key chứa branch/filter/page;
  mutation thành công làm mới dữ liệu liên quan. UI state của modal/form vẫn ở component.
- Dùng Ant Design Form/Table/Modal nhất quán, giữ theme/styled-components cho bố cục.
  Quyết định bổ sung thư viện khi thực thi; chưa cài dependency trong đợt phân tích này.

## Thứ tự triển khai và điều kiện nghiệm thu

| Đợt | Công việc | Hoàn thành khi |
|---|---|---|
| 0 — nền tảng | Sửa lint, gateway env, auth DTO, 4 role, guard, 403/404, layout, branch context, error/paging | Login/reload/logout 4 role; URL trái quyền bị chặn; đổi branch SA không lẫn dữ liệu |
| 1 — tổ chức | Chi nhánh, accounts, nhân sự khả dụng, khóa, phòng | SA tạo branch/Admin; Admin tạo Staff/Teacher; dữ liệu tồn tại sau reload; nút đúng quyền |
| 2 — tuyển sinh | Lead, Student và trang chi tiết; convert server | CONTACTED -> convert -> Student; không ID giả; hồ sơ/người giám hộ đúng |
| 3 — học vụ | Class list/detail tabs, lịch cấu trúc, conflict, enrollment, transfer/reserve/resume | Xung đột/sĩ số bị chặn; transfer giữ billing; tuition FAILED có đường retry |
| 4 — tài chính | Tuition list/detail, receipts, thu tiền, điều chỉnh theo role | requestKey ổn định khi retry; không thu trùng/vượt dư; Staff không sửa giá |
| 5 — giảng dạy | My classes/schedule, sheet/history/summary, exams/grades/history, documents/review/download | Teacher chỉ lớp mình; điểm danh theo roster ngày; grade audit và file visibility đúng |
| 6 — báo cáo và hoàn thiện | Dashboard theo role, finance reports, responsive, trạng thái lỗi/rỗng/loading, README FE | Chỉ số có API nguồn; không placeholder/mock trong luồng thật; build/lint/E2E đạt |

Triển khai mỗi đợt thành luồng chạy được từ giao diện tới database trước khi mở rộng.
Không cần chờ hoàn thành cả 84 API wrapper mới bắt đầu trang đầu tiên.

## Phụ thuộc và phần hoãn

- Sửa auth IllegalArgumentException còn trả 500 cho một số input nghiệp vụ sai.
- Chạy migration trên bản sao MySQL và smoke test cả 5 service qua gateway.
- Calendar MVP có thể hiển thị lịch tuần. Nếu hiển thị sự kiện theo khoảng ngày,
  cần ghép lịch với startDate/endDate lớp và tải đủ trang; ưu tiên API calendar range
  nếu dữ liệu lớn. Không khẳng định backend đã có endpoint này.
- Email/điện thoại và đổi/cấp lại mật khẩu đã có. Khôi phục tự động qua email/OTP và phân công lại Lead còn cần bổ sung.
- Teacher directory hiện chỉ trả giáo viên khả dụng, không đủ làm danh sách nhân sự nghỉ việc;
  màn quản lý dùng accounts role=TEACHER cho các trường API thực sự cung cấp.
- Hoãn portal học viên, VNPay, hoàn tiền, chuyển khác khóa/chi nhánh, expense/invoice/promotion.
- Không bổ sung chart nếu API chưa có chỉ số. Teacher dashboard lấy lớp/lịch của mình,
  không gọi stats overview dành cho SA/A/S.

## Kiểm thử các tình huống thực tế

- Staff nhập URL accounts/reports; Teacher nhập URL lớp người khác; token hết hạn khi đang lưu.
- SA đổi branch khi request trước chưa xong; form cũ không được lưu sang branch mới.
- Hai người giành chỗ cuối; preview hết xung đột nhưng lúc lưu đã có lịch khác.
- Siblings cùng số điện thoại; học viên bảo lưu rồi học lại; lịch sử trước/sau chuyển lớp.
- Finance ngừng lúc ghi danh, retry sau phục hồi; hai tab thu tiền; timeout sau server commit
  rồi gửi lại cùng requestKey. Không tạo requestKey mới chỉ vì client không nhận response.
- Teacher nhập điểm; Admin sửa với lý do; file PENDING/REJECTED không lộ cho Staff.
- Reload giữ dữ liệu thật; lỗi mạng không hiển thị thành công; loading không cho gửi lặp.

Kiểm tra bắt buộc: TypeScript app/node, lint, production build; test guard/adapter và các
luồng quan trọng bằng component/E2E phù hợp. Frontend có npm test, npm run lint và npm run build.
