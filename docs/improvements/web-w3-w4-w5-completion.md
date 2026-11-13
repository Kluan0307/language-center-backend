# Bàn giao W3–W5 — Tài chính, hỗ trợ và hồ sơ học viên

Ngày: 13/11/2026. Thuộc [lộ trình nâng cấp web W1–W8](web-management-upgrade-roadmap-2026-11-13.md). W3–W5 đã triển khai trong source và kiểm thử; W6–W8 chưa thực hiện. Chưa deploy hoặc chạy migration trên database MySQL đang dùng.

## W3 — Tài chính xuyên suốt

- Đơn online nhận thêm ngữ cảnh học viên, lớp gốc, khóa và chi nhánh từ snapshot khoản học phí. Tên là thông tin ghi nhận tại khoản thu; không giả làm tên hiện hành lấy trực tiếp từ hồ sơ.
- Tìm kiếm phía server theo tên học viên/lớp/khóa, UUID, mã tham chiếu, số giao dịch cổng và mã GD-… đang hiển thị. Lọc studentId, classId, tuitionId, receiptId, state và branchId trước khi đếm/phân trang. `%`/`_` được hiểu là ký tự tìm kiếm, không phải wildcard do người dùng chèn vào.
- Phiếu thu hỗ trợ tìm theo tên học viên/lớp ngoài số phiếu và mã giao dịch; bổ sung studentId/classId để tra trong hồ sơ đúng người. Metadata lấy theo lô, không gọi service khác cho từng dòng.
- Phiếu thu có `source` và `onlineOrderId`. ONLINE dựa vào đơn thực sự trỏ tới phiếu với cùng khoản/chi nhánh; không suy ra online chỉ vì paymentMethod=BANK_TRANSFER. DIRECT là thu trực tiếp; dấu hiệu phiếu online thiếu liên kết được giữ ở REVIEW_REQUIRED.
- Liên kết học phí ↔ đơn ↔ phiếu ↔ hồ sơ có đường quay lại chứa bộ lọc nguồn; đóng drawer chỉ bỏ ID chi tiết. Chuỗi returnTo chỉ giữ một cấp, tránh URL lồng vô hạn; chỉ nhận route nội bộ cho phép.
- Học phí có danh sách đơn liên quan; phiếu thu mở lại đơn nguồn. UI nêu rõ đảo/hoàn trên sổ thu không đồng nghĩa VNPAY đã chuyển tiền hoàn về ngân hàng.
- Sửa bộ lọc quá hạn của học phí: useListQuery trước đây chưa đọc/ghi `overdue`, nên checkbox không tồn tại qua URL. Đã có kiểm thử giữ và xóa lọc này.

Không thay cơ chế thu/đối soát/settlement: chữ ký, version, requestId/requestKey và khôi phục vẫn giữ nguyên. Không thêm thao tác tự xác nhận đã trả tiền hoặc cộng tiền các đơn thành doanh thu.

## W4 — Hỗ trợ gắn đúng giao dịch

- Thêm `resourceKey` nullable, tối đa 36 ký tự. Loại PAYMENT_ORDER dùng UUID đầy đủ trong resourceKey và không dùng resourceId; tham chiếu số cũ PROFILE/ENROLLMENT/SESSION/TUITION/RECEIPT vẫn giữ nguyên.
- Khi học viên tạo ticket gắn đơn, academic gọi `GET /finance/student/me/payment-orders/{uuid}` bằng token hiện tại. Finance kiểm cả tài khoản và học viên sở hữu; academic đối chiếu id/studentId, lấy chi nhánh từ bằng chứng đó. Không tin branchId do client tự truyền và không mượn tuitionId thay UUID.
- Dấu vân tay idempotency của ticket số cũ giữ nguyên. resourceKey chỉ tham gia intent mới khi được cung cấp; đổi tham chiếu dưới cùng requestId bị từ chối.
- Danh sách hỗ trợ tìm theo học viên/điện thoại/tiêu đề/mã giao dịch, lọc studentId/category/status/branchId. Hàng đợi mặc định cần xử lý, có lựa chọn tất cả. Server ưu tiên OPEN, IN_PROGRESS rồi RESOLVED/CLOSED.
- Chi tiết có liên kết tới đơn/khoản/phiếu/hồ sơ, hội thoại và lịch sử trạng thái. Mặc định phản hồi công khai; ghi chú nội bộ là lựa chọn riêng kèm cảnh báo rõ trước khi gửi.
- Lịch sử trạng thái lưu từ bản mới gồm người thực hiện, thời điểm, trước/sau và lý do. Chỉ StaffDetail có lịch sử này. StudentDetail tiếp tục loại bỏ ghi chú/lý do nội bộ. Không bịa lịch sử cho ticket cũ.
- Mobile: các nút cần hỗ trợ trong chi tiết giao dịch gửi PAYMENT_ORDER + resourceKey; ticket mở lại được giao dịch liên quan. Các nút hỗ trợ từ khoản học phí/phiếu vẫn dùng tham chiếu số.

Tạo/phản hồi ticket không tự điều chỉnh học phí, ghi phiếu, đổi điểm hoặc trạng thái ngân hàng.

## W5 — Hồ sơ học viên

- Header nhận diện bằng họ tên, mã, trạng thái và chi nhánh; bỏ nút Xem thừa khi hồ sơ đã mở.
- Năm nhóm chính: Tổng quan, Học tập, Tài chính, Hỗ trợ, Lịch sử. Thông tin hồ sơ và tài khoản ứng dụng nằm trong Tổng quan; ghi danh/hồ sơ học tập/đầu vào nằm trong Học tập.
- Tổng quan cho biết liên hệ, lớp đang học và lịch sử ghi danh, có liên kết đúng học viên tới lớp/học phí/hỗ trợ.
- Tài chính tải theo studentId và chi nhánh: học phí, giao dịch online, phiếu thu có phân trang server. Không cộng các dòng của một trang làm tổng công nợ.
- Hỗ trợ chỉ tải yêu cầu của học viên đang mở, có lối sang hàng đợi kèm ngữ cảnh.
- Từng khối tải khi chọn tab; phần kết quả học tập chỉ gọi dữ liệu lớp khi chủ động mở một kết quả. Không gọi hai request cho mọi card ngay khi xem hồ sơ. Khi mở kết quả có bảng điểm đã công bố lấy từ snapshot publication hiện hành; không lấy điểm nháp, không lấy nhận xét nội bộ và loại các bản đã thu hồi. Điểm tạm và kết quả chính thức vẫn được phân biệt, không đổi thiếu điểm thành 0.
- Giữ đánh giá đầu vào riêng theo chương trình và thang trình độ, lịch sử bảo lưu/chuyển lớp, cùng công cụ cấp/khóa/reset tài khoản. STAFF không tải hoặc thấy công cụ tài khoản ứng dụng của quản lý; TEACHER vẫn không được vào trang quản lý hồ sơ này.
- Không thay logic ghi danh/billing root. Chuyển lớp vẫn dùng hợp đồng hiện có, không tạo khoản học phí mới từ trang tổng quan. Giữ form thêm/sửa hiện tại vì không có phụ thuộc mới cần tách wizard.

## Hợp đồng API cập nhật

Tiền tố `/api/v1`; các thay đổi thêm trường/param, giữ route cũ và tham chiếu số của app cũ.

| API | Bổ sung |
|---|---|
| GET `/finance/payment-orders` | keyword, studentId, classId, receiptId; giữ tuitionId/state/branchId/page/size. Metadata studentId/studentName/classId/className/courseName/branchId |
| GET `/finance/payment-orders/{id}` | Cùng metadata; Student own-view cũng thêm studentId/branchId để academic xác minh tham chiếu, không mở quyền của endpoint |
| GET `/finance/payments` | studentId, classId; keyword còn tìm theo tên học viên/lớp; source, onlineOrderId và ngữ cảnh học viên/lớp |
| GET `/finance/payments/{id}` | Cùng metadata và nguồn thu; số tiền/đảo/hoàn vẫn từ ledger |
| GET `/academic/support-requests` | studentId, keyword, queue=ACTIVE; giữ category/status/branchId/page/size |
| POST `/academic/student/me/support-requests` | resourceKey tùy chọn; PAYMENT_ORDER bắt buộc key UUID, không có resourceId |
| GET `/teaching/students/{studentId}/enrollments/{enrollmentId}/published-grades` | API mới cho SUPER_ADMIN/ADMIN/STAFF: xác minh học viên/đăng ký/chi nhánh qua academic, phân trang điểm publication hiện hành, giữ null khác điểm 0 |
| GET chi tiết hỗ trợ | Row thêm resourceKey; chỉ chi tiết dành cho nhân viên thêm statusHistory |

Ví dụ nội dung tạo ticket mới từ mobile:

```json
{
  "requestId": "8899aabb-ccdd-4eef-8123-445566778899",
  "category": "PAYMENT",
  "subject": "Kiểm tra giao dịch học phí",
  "description": "Tôi đã thao tác thanh toán và cần trung tâm kiểm tra kết quả.",
  "resourceType": "PAYMENT_ORDER",
  "resourceKey": "UUID-đầy-đủ-của-đơn-thuộc-tài-khoản"
}
```

resourceKey trong ví dụ là chỗ thay bằng UUID thật từ đơn đang xem. requestId là UUID mới của thao tác, khác resourceKey; retry phải giữ nguyên cả requestId và nội dung.

ADMIN/STAFF chọn chi nhánh đang làm việc bằng BranchSwitcher hiện có. SUPER_ADMIN lọc trực tiếp chi nhánh trên danh sách. Endpoint chi tiết vẫn kiểm quyền đối tượng/chi nhánh ở backend; tham số URL không tự cấp quyền.

## Cập nhật môi trường đang chạy

1. Dừng phiên academic-service, finance-service và teaching-service cũ bằng Ctrl+C ở terminal đang chạy. Không mở thêm bản thứ hai trên cùng cổng 8082/8083/8084.
2. Schema chỉ thêm: `student_support_requests.resource_key`, bảng `student_support_status_events` và index của lịch sử. Không xóa hoặc tạo lại database.
3. Dev đang dùng DDL_AUTO=update: Hibernate thêm cột/bảng khi khởi động academic. Nếu dùng DDL_AUTO=validate/none, áp dụng [SQL W4](../migrations/web-w4-support-order-links.sql) vào **database hiện có đã chọn**, sau backup, trước khi khởi động bản mới. Script không được thực thi trên MySQL trong đợt này.
4. Chạy lại ba service với cấu hình hiện có. Từ thư mục backend gốc, dùng ba terminal:

```powershell
cd .\academic-service
.\mvnw.cmd spring-boot:run
```

```powershell
cd .\finance-service
.\mvnw.cmd spring-boot:run
```

Thêm terminal teaching:

```powershell
cd .\teaching-service
.\mvnw.cmd spring-boot:run
```

Các application.properties hiện đều import `.env.local` của thư mục cha. Không cần thay VNPAY secret, Return/IPN hoặc dựng lại tài khoản. Auth/gateway vẫn cần chạy để sử dụng các chức năng tương ứng. API điểm mới nằm trong teaching, cần bản service mới để mở điểm từ hồ sơ.

5. Nạp bản frontend mới sau khi backend đã cập nhật. Kiểm tra lần lượt `/online-payments`, `/tuition`, `/payments`, `/support-requests`, `/students`. Không chạy frontend mới với backend cũ chưa hỗ trợ bộ lọc studentId của phiếu thu.
6. Mobile nguồn đã cập nhật; bản APK đang cài không tự đổi. Khi dùng Expo tải lại bundle mới; khi dùng APK cần build/cài bản mới để nút hỗ trợ gửi UUID trực tiếp. Chưa tạo APK mới trong đợt web W3–W5 này. Ticket và app cũ dùng tham chiếu số vẫn tương thích.

## Kết quả kiểm tra

| Kiểm tra | Kết quả |
|---|---|
| finance-service Maven/H2/mock/HTTP stub | **109/109 PASS**, có tìm kiếm trước phân trang, escape, mã GD, nguồn phiếu và chặn khác chi nhánh |
| academic-service Maven/H2/mock | **229/229 PASS**, có UUID hợp lệ/không hợp lệ/không sở hữu, queue, replay, nội bộ và lịch sử |
| teaching-service Maven/H2/mock | **97/97 PASS**, có chặn điểm nháp/nhận xét nội bộ, thu hồi công bố, sai học viên/đăng ký/chi nhánh/role |
| Web Node test | **105/105 PASS**, thêm query context, returnTo an toàn và overdue |
| Web TypeScript + Vite build | PASS; còn cảnh báo bundle >500kB từ nền hiện có |
| Mobile TypeScript | PASS |
| Mobile Node test | **81/81 PASS**, thêm kiểm tra resourceKey UUID tách resourceId số |
| Edge UI | **9 nhóm PASS**: metadata/filter; đơn→học phí→phiếu; ticket→đơn; giữ lọc hỗ trợ; tải lười và quyền STAFF; tài chính đúng học viên; kết quả mở theo yêu cầu; điểm công bố 0 khác thiếu điểm; drawer 390px |

Backend chạy trên database kiểm thử độc lập, không xóa dữ liệu MySQL người dùng. UI dùng GET/OPTIONS giả lập trong trình duyệt riêng, không gửi tiền hoặc phản hồi thật. Không coi kết quả này là nghiệm thu VNPAY/IPN live, Android APK mới hoặc thực thi migration MySQL production. Chưa chạy lại các suite của service không sửa (auth/gateway).

Log máy cục bộ: `storage/web-w345-{finance,academic,teaching}-tests.log`, `storage/web-w345-web-tests.log`, `storage/web-w345-mobile-{tests,typecheck}.log`, `storage/web-w345-build.log`, `storage/web-w345-evidence/report.json`.

Ảnh chỉ dùng dữ liệu giả lập để kiểm tra bố cục:

- [Đơn online](assets/web-w345/online-orders.png)
- [Liên kết từ học phí](assets/web-w345/tuition-links.png)
- [Chi tiết phiếu thu](assets/web-w345/receipt-detail.png)
- [Yêu cầu hỗ trợ](assets/web-w345/support-detail.png)
- [Tổng quan học viên](assets/web-w345/student-overview.png)
- [Tài chính trong hồ sơ](assets/web-w345/student-finance.png)
- [Điểm đã công bố trong hồ sơ](assets/web-w345/student-published-grades.png)
- [Hồ sơ trên màn hẹp](assets/web-w345/student-mobile.png)

Tiếp theo là **W6**, không gian học vụ/giảng dạy: lớp, buổi, điểm danh, kết quả và tài liệu theo đúng phân công.
