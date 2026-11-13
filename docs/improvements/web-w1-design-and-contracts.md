# W1 — Thiết kế luồng và nền giao diện web quản lý

Ngày: 13/11/2026. Phạm vi: đợt nâng cấp W1–W8 trong [lộ trình](web-management-upgrade-roadmap-2026-11-13.md). W1 chốt thiết kế; W2 hiện thực nền UI và trang OnlinePayments. Các wireframe dưới đây là đặc tả cho các giai đoạn sau, không phải tất cả đã được code.

## Căn cứ và giới hạn khảo sát

Đọc NAVIGATION/App, ManagementLayout, Sidebar, các trang học viên/học phí/phiếu thu/online/hỗ trợ/lịch, service phía web, AuthContext, useRemoteResource và controller/service online/support/BranchScope phía backend. Quyền route lấy từ cấu hình hiện có; quyền thao tác tài chính đối chiếu OnlineController. Không suy ra quyền ghi từ việc có menu.

Walkthrough các luồng dưới đây theo source và hợp đồng API; nghiệm thu trình duyệt W2 trên phản hồi GET giả lập, không phải chạy lại toàn bộ nghiệp vụ bằng database thật. Chưa xác nhận hồi quy end-to-end VNPAY/IPN, gửi hỗ trợ, nhập điểm hoặc thu trực tiếp trong đợt này. Không sửa backend, database, môi trường VNPAY hoặc mobile.

## Bản đồ luồng làm việc

| Luồng | Điểm vào và mục tiêu | Thao tác chính | Đích và đường quay lại |
|---|---|---|---|
| Tìm học viên | Học viên, tìm hồ sơ đúng chi nhánh | Xem hồ sơ, đăng ký và đầu vào | Đóng chi tiết về danh sách với bộ lọc; W5 bổ sung tổng quan học tập/tài chính |
| Thu trực tiếp | Học phí & công nợ, xác định khoản còn phải thu | Mở khoản, tạo phiếu thu theo quyền | Phiếu thu là chứng từ; trở về khoản học phí; W3 đồng bộ link hai chiều |
| Thanh toán online | Đơn thanh toán, xác định phiên và kết quả | Xem, mở khoản học phí/phiếu thu; quản lý đối soát có lý do | Đóng drawer giữ trạng thái URL; không đánh dấu đã đóng tiền ở client |
| Xử lý hỗ trợ | Hỗ trợ học viên, đọc nội dung và tham chiếu | Phản hồi, chuyển trạng thái, ghi chú nội bộ | Quay lại danh sách theo chi nhánh; W4 bổ sung tham chiếu UUID đúng kiểu |
| Điều phối lịch | Lịch học, chọn tuần/chi nhánh/lớp/phòng/giáo viên | Xem buổi; chỉ quyền phù hợp được thay đổi lịch | Giữ tuần và bộ lọc; W6 hoàn thiện chi tiết, điều phối và xung đột |
| Giảng dạy | Lớp của tôi/Lịch dạy của tôi | Xem buổi, điểm danh/nhập điểm lớp được phân công | Về lớp hoặc tuần đang xem, không chuyển sang trang tài chính |

Ba khái niệm phải dùng nhất quán: **khoản học phí** là nghĩa vụ và công nợ; **đơn thanh toán** là một lần thử/phiên giao dịch; **phiếu thu** là khoản thu đã ghi sổ. Không cộng tổng các đơn thành doanh thu. Return trên trình duyệt không tự xác nhận tiền; giữ nguyên kiểm chứng callback/query phía server.

## Quy tắc phạm vi và điều hướng

- Sidebar thể hiện quyền/phạm vi tài khoản. Header mặc định ghi **Phạm vi tài khoản**, không khẳng định đó là bộ lọc dữ liệu của mọi màn hình.
- Trang có scope cụ thể truyền `scopeLabel`, header ghi **Phạm vi danh sách**. OnlinePayments: SUPER_ADMIN xem toàn hệ thống hoặc chi nhánh được lọc; ADMIN/STAFF theo chi nhánh đang làm việc.
- Nhật ký online không nhận `branchId`: backend lọc theo toàn hệ thống hoặc tập chi nhánh được phân công. Khi chuyển tab phải đổi nhãn phạm vi, không gán bộ lọc của tab đơn cho nhật ký.
- ADMIN/STAFF đổi chi nhánh bằng BranchSwitcher hiện có. Không bổ sung quyền xem chi nhánh từ URL. Backend vẫn kiểm tra đối tượng và chi nhánh.
- URL online: `branchId`, `state`, `page` (từ 0), `size` (10/20/50), `tab` (orders/events), `eventPage`, `orderId` đầy đủ. Đóng chi tiết chỉ xóa orderId; lọc danh sách đưa page về 0; chuyển tab giữ bộ lọc của danh sách.
- Không đưa keyword vào URL rồi lọc riêng trang kết quả hiện tại để giả làm tìm kiếm toàn bộ dữ liệu. Truy vấn người/lớp/tham chiếu sẽ thực hiện server-side ở W3.
- useRemoteResource đã hủy bằng AbortController và khóa kết quả theo query/revision. Tiếp tục dùng, không hiển thị kết quả của scope trước trong lúc tải scope mới. AuthContext remount theo tài khoản/chi nhánh hiện hành.

## Hợp đồng API liên quan

Các đường dẫn dưới đây có tiền tố `/api/v1`; đây là inventory phục vụ nâng cấp giao diện, không thay tài liệu API chi tiết hiện có.

| Nhóm | API hiện tại dùng cho luồng | Quy tắc cần giữ |
|---|---|---|
| Phiên/tài khoản | `/auth/login`, `/auth/me`, `/auth/accounts`, `/auth/accounts/{id}/branches` | Xác minh phiên, đổi mật khẩu bắt buộc, phân công chi nhánh theo quyền |
| Chi nhánh | `/academic/branches`, `/academic/branches/{id}` | Tra nhãn scope; lỗi tra nhãn không được biến thành danh sách rỗng hợp lệ |
| Tuyển sinh | `/academic/leads`, `/academic/students`, `/academic/enrollments` | Phạm vi chi nhánh, version và lịch sử thay đổi hiện có |
| Học vụ | `/academic/courses`, `/academic/classes`, `/academic/rooms`, `/academic/sessions` | Không tự phát hiện xung đột bằng riêng các dòng của một trang |
| Giảng dạy | `/academic/attendances/sheet`, `/academic/attendances/bulk`, `/teaching/exams`, `/teaching/grades`, `/teaching/documents` | Teacher giới hạn lớp được phân công, version/lý do theo hợp đồng |
| Học phí | GET `/finance/tuitions`, GET/PUT `/finance/tuitions/{id}`, GET `/{id}/history` | Điều chỉnh theo quyền, giữ audit/recovery; không sửa số tiền bằng UI đơn online |
| Phiếu thu | GET/POST `/finance/payments`, GET `/finance/payments/{id}`, GET/POST `/{id}/corrections` | Không sửa/xóa chứng từ trực tiếp; không tạo phiếu chỉ vì đóng drawer hoặc Return |
| Đơn online | GET `/finance/payment-orders?page&size&state&tuitionId&branchId`, GET `/{id}` | UUID đầy đủ dùng trong API/link; ký hiệu GD-8-ký-tự chỉ để đọc nhanh, không phải mã duy nhất |
| Đối soát | POST `/finance/payment-orders/{id}/reconcile`, GET `/finance/payment-orders/reconciliations/{requestId}` | Chỉ SUPER_ADMIN/ADMIN; reason, expectedVersion, requestId cố định khi gửi lại/khôi phục |
| Bằng chứng online | GET `/finance/provider-events?page&size` | Đọc cho SUPER_ADMIN/ADMIN/STAFF; không hỗ trợ lọc chi nhánh ở endpoint hiện tại |
| Hỗ trợ | `/academic/support-requests` và các API phản hồi/trạng thái của trang SupportManagement | resourceId hiện là Long; không ép UUID thành số hoặc cắt chuỗi UUID |

### Khoảng trống hợp đồng và thứ tự xử lý

1. W3: dữ liệu đơn quản lý thiếu ngữ cảnh tên học viên/lớp thuận tiện; xác định DTO và truy vấn server trước khi thêm cột/tìm kiếm. API list hiện đã có tuitionId nhưng UI pilot chưa mở bộ lọc này. Không tạo API tổng doanh thu bằng tổng tiền một trang đơn.
2. W3: liên kết khoản học phí ↔ đơn ↔ phiếu và bộ lọc nguồn cần hợp đồng cụ thể, đảm bảo cùng phạm vi và billing root.
3. W4: tham chiếu hỗ trợ chỉ nhận Long; cần kiểu tham chiếu UUID bổ sung, validation và quyền sở hữu, giữ tương thích tham chiếu số.
4. W5–W7: chỉ bổ sung tổng quan/chỉ số khi có nguồn dữ liệu đầy đủ; trạng thái tải/lỗi không biểu diễn thành 0. Mỗi chỉ số cần link tới danh sách có đúng bộ lọc.

## Design brief đã chốt

Giữ Ant Design + styled-components, sidebar cố định hiện tại và FullCalendar. Màu chủ đạo #2563EB, chữ #1F2937, chữ phụ #64748B, nền #F8FAFC, đường viền #E2E8F0; trạng thái thành công xanh lá, cảnh báo hổ phách, lỗi đỏ. Trạng thái luôn có chữ, không chỉ có màu.

Chữ cơ bản 14px, H1 26px; control 36px; bo góc 8px (card 12px). Khoảng cách chính 8/12/16/24/32px. Main desktop padding 28px 32px; màn hẹp dùng 16px và menu hiện tại. Không tăng/giảm kích thước sidebar trong đợt này.

| Thành phần | Quyết định |
|---|---|
| PageHeader | Scope nhỏ phía trên, H1/mô tả ngắn, hành động của trang bên phải; wrap khi hẹp |
| Filters | Một vùng nhẹ, trường có nhãn truy cập; reset chỉ hiện khi có lọc; không đặt nút tài chính trong hàng lọc |
| TableActions | Giữ thành phần hiện có: một hành động thường dùng + menu thao tác phụ; xác nhận hành động nguy hiểm. Online pilot chỉ có Xem nên không thêm menu rỗng |
| Bảng | Số tiền căn phải, chữ số tabular; trạng thái bằng Tag; không wrap hàng nút; kéo ngang trong bảng thay vì kéo ngang cả trang |
| StateView | Trống có giải thích; lỗi dùng LoadError và Thử lại, không hiện thông báo trống cùng lỗi; loading không hiển thị dữ liệu scope cũ |
| DetailSection | Thông tin nghiệp vụ hiển thị trước; bằng chứng/mã kỹ thuật mở rộng được; không cắt dữ liệu API |
| Drawer/Form | Mặc định drawer 640px, Ant giới hạn theo viewport; lý do/version/recovery giữ nguyên; loading/disabled khi đang gửi; focus và đóng bằng bàn phím |

Theme Ant và styled-components dùng lại `Common/theme.ts`. Không cài thư viện UI mới. Shared header/styles ảnh hưởng các trang dùng ManagementLayout; nội dung của các trang khác sẽ di chuyển theo W3–W8, không thay tất cả bố cục trong W2.

## Bốn wireframe

Wireframe dùng tên trường, không phải dữ liệu thật hoặc số liệu mẫu chạy trong app. Dấu `…` là phần mở rộng trong thiết kế.

### Thanh toán trực tuyến — pilot W2

```text
Sidebar | Phạm vi danh sách: chi nhánh / toàn hệ thống         [Cập nhật]
        | Thanh toán trực tuyến
        | Mô tả mục đích tra cứu
        | [Đơn thanh toán] [Nhật ký xác thực]
        | ┌ Chi nhánh ─ Trạng thái ─ Xóa bộ lọc ───────────────┐
        | │ Tổng số từ server · bộ lọc đang áp dụng            │
        | │ Giao dịch | Tiền | Kết quả | Tạo lúc | MT | Xem   │
        | │                                    Phân trang     │
        | └───────────────────────────────────────────────────┘
        |                     Drawer: số tiền · kết quả · thông tin
        |                     → Khoản học phí → Phiếu thu (nếu có)
        |                     ▸ Mã/bằng chứng đầy đủ
        |                     Lý do · Đối soát (chỉ quản lý)
```

W3 sẽ bổ sung ngữ cảnh học viên/lớp và tìm kiếm bằng API, không đặt cột rỗng hoặc nhãn giả vào pilot.

### Học phí & công nợ — W3

```text
Scope · Học phí & công nợ                              [Cập nhật]
Tìm học viên ─ Lớp ─ Trạng thái ─ Hạn đóng ─ Chi nhánh
Học viên | Lớp/khóa | Phải đóng | Đã đóng | Còn nợ | Hạn | Xem
Drawer: học viên · lớp · tình trạng xác minh
        Số phải đóng / đã đóng / còn lại (từ server)
        [Thu trực tiếp] theo quyền · [Lịch sử giao dịch online]
        Phiếu thu · điều chỉnh · lịch sử, lý do và người thực hiện
```

### Hồ sơ học viên — W5

```text
← Danh sách (giữ bộ lọc)     Họ tên · Mã · Chi nhánh · Trạng thái
[Tổng quan] [Học tập] [Đầu vào] [Tài chính] [Hỗ trợ] [Lịch sử]
Thông tin liên hệ           Lớp hiện tại · buổi sắp tới
Chuyên cần/điểm (có phạm vi) Khoản đến hạn (nguồn server)
Liên kết tới lớp / học phí / phiếu / hỗ trợ có ngữ cảnh học viên
```

Không tự coi trình độ text cũ là đầu vào đã xác minh; mỗi chương trình giữ kết quả hiện hành riêng.

### Tổng quan STAFF — W7

```text
Scope · Tổng quan                                      [Cập nhật]
Hôm nay / khoảng thời gian · Chi nhánh đang làm việc
Việc cần xử lý: hỗ trợ chờ / khoản quá hạn / tuyển sinh cần chăm sóc
Lịch buổi học                       Hàng đợi công việc
Danh sách rút gọn có đích mở         Trạng thái · đối tượng · Xem
Mỗi số đếm ghi nguồn và thời gian; lỗi dữ liệu hiện thử lại
```

STAFF không thấy công cụ đối soát/điều chỉnh chỉ dành cho quản lý. TEACHER dùng tổng quan giảng dạy riêng, không dùng bảng công nợ của STAFF.

## Phát hiện, bằng chứng và ưu tiên

| Phát hiện | Bằng chứng / mức xác nhận | Giải quyết |
|---|---|---|
| Header account scope dễ bị hiểu là bộ lọc thật | Source header cũ + browser pilot | W2 tách nhãn tài khoản/danh sách và scope từng tab |
| Đóng drawer online làm mất query | Source cũ setParams({}); test URL và browser | W2 patch từng khóa URL |
| Bằng chứng và UUID chiếm ưu tiên đầu trang | Source + screenshot bố cục | W2 rút gọn mã hiển thị, giữ UUID đầy đủ trong chi tiết |
| Lỗi list có thể đồng thời hiện bảng trống | Source cũ render table dù error | W2 ẩn bảng khi lỗi, hiển thị retry |
| Thiếu ngữ cảnh học viên/lớp trong online | Source DTO/UI; chưa nghiệm thu dữ liệu production | W3 cập nhật truy vấn/DTO và liên kết |
| Hỗ trợ UUID chưa tương thích resourceId | SupportService.Create và SupportRequest dùng Long | W4 nâng hợp đồng, không ép kiểu |
| Hủy request/giữ scope đã có | useRemoteResource và AuthContext | Tái sử dụng; không viết lại chỉ để đổi tên |

## Ma trận màn hình và role

Bảng dưới được trích từ NAVIGATION của source hiện tại. Đây là quyền **vào trang**, không có nghĩa mọi role được thực hiện mọi hành động. Backend tiếp tục là nơi quyết định quyền cuối cùng. ADMIN/STAFF bị giới hạn chi nhánh; TEACHER giới hạn lớp phân công. SUPER_ADMIN không được tự bỏ các kiểm chứng tài chính.

| Route | Màn hình / mục tiêu | SUPER_ADMIN | ADMIN | STAFF | TEACHER |
|---|---|---|---|---|---|
| `/dashboard` | Tổng quan — Theo dõi hoạt động trong phạm vi được phân quyền. | Có | Có | Có | Có |
| `/branches` | Chi nhánh — Tạo, cập nhật và bật/tắt chi nhánh. | Có | — | — | — |
| `/administrators` | Quản trị viên — Tạo và quản lý Admin, phân công nhiều chi nhánh và xem lịch sử phân công. | Có | — | — | — |
| `/accounts` | Tài khoản — Xem, tạo và khóa/mở tài khoản theo quyền. | Có | Có | — | — |
| `/teachers` | Giáo viên — Tra cứu giáo viên trong phạm vi được phép. | Có | Có | Có | — |
| `/staff` | Nhân viên — Tra cứu nhân viên; tạo và khóa/mở tài khoản ở mục Tài khoản. | Có | Có | — | — |
| `/leads` | Khách hàng tiềm năng — Chăm sóc Lead và chuyển đổi thành học viên. | Có | Có | Có | — |
| `/students` | Học viên — Quản lý hồ sơ và theo dõi lịch sử đăng ký của học viên. | Có | Có | Có | — |
| `/programs` | Chương trình & trình độ — Quản lý chương trình đào tạo và thang trình độ. | Có | — | — | — |
| `/courses` | Khóa học — Quản lý khóa học trong phạm vi được phép. | Có | Có | Có | — |
| `/classes` | Lớp học — Quản lý thông tin lớp và danh sách học viên. | Có | Có | Có | — |
| `/rooms` | Phòng học — Quản lý phòng, sức chứa và trạng thái sử dụng. | Có | Có | Có | — |
| `/schedule` | Lịch học — Xem và điều phối lịch, kiểm tra trùng giáo viên, phòng và học viên. | Có | Có | Có | — |
| `/enrollment` | Đăng ký & xếp lớp — Ghi danh, bảo lưu, học lại và chuyển lớp cùng khóa, cùng chi nhánh. | Có | Có | Có | — |
| `/my-classes` | Lớp của tôi — Xem các lớp được phân công, thông tin lớp và học viên trong lớp. | — | — | — | Có |
| `/my-schedule` | Lịch dạy của tôi — Xem lịch dạy của các lớp được phân công. | — | — | — | Có |
| `/attendance` | Điểm danh — Điểm danh theo ngày, xem lịch sử và tổng hợp chuyên cần. | Có | Có | Có | Có |
| `/exams` | Bài kiểm tra — Quản lý bài kiểm tra, thang điểm và trọng số. | Có | Có | — | Có |
| `/grades` | Bảng điểm & đánh giá — Xem điểm, lịch sử sửa điểm và đánh giá tổng hợp của lớp. | Có | Có | — | Có |
| `/support-requests` | Hỗ trợ học viên — Xử lý yêu cầu, phản hồi học viên và ghi chú nội bộ theo chi nhánh. | Có | Có | Có | — |
| `/documents` | Tài liệu — Đăng, duyệt và tải tài liệu trong phạm vi được phép. | Có | Có | Có | Có |
| `/tuition` | Học phí & công nợ — Theo dõi học phí, số đã đóng và số còn nợ. | Có | Có | Có | — |
| `/payments` | Phiếu thu — Thu tiền mặt/chuyển khoản, xem biên nhận; không sửa hoặc xóa phiếu thu. | Có | Có | Có | — |
| `/online-payments` | Thanh toán trực tuyến — Theo dõi đơn VNPAY sandbox và đối soát giao dịch đúng chi nhánh. | Có | Có | Có | — |
| `/reports` | Báo cáo — Xem doanh thu, công nợ và thống kê vận hành. | Có | Có | — | — |

Ngoài menu: `/login` công khai; `/profile` và `/change-password` theo phiên; `/forbidden`/404 là trạng thái; `/` và `/overview` chuyển về dashboard, `/stats` giữ guard theo App. Không thêm role STUDENT vào web quản lý.

## Checklist nghiệm thu từng màn hình sau W2

1. Scope của header, request và đối tượng chi tiết nhất quán; thay scope không ló dữ liệu cũ.
2. Tải, lỗi, trống, hết quyền là các trạng thái rõ ràng; có đường thử lại/quay lại.
3. Mở/đóng chi tiết và Back giữ bộ lọc; không rút ngắn ID dùng để gọi API.
4. Chỉ một hành động ưu tiên; thao tác nguy hiểm có xác nhận/lý do và giữ hợp đồng hiện có.
5. Kiểm tra 1366px, màn hẹp, keyboard/focus; số liệu không tổng hợp từ trang dữ liệu chưa đầy đủ.
6. Bổ sung kiểm thử nghiệp vụ khi thay API/logic ở W3 trở đi; không xem build UI là nghiệm thu tiền thật.
