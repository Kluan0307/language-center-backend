# Student App — Giai đoạn 7: học phí, công nợ và phiếu thu

Thực hiện ngày 12/11/2026, thuộc lộ trình mobile 10 giai đoạn. Hoàn thiện dữ liệu tài chính cá nhân; thanh toán online thuộc G8.

## Chạy và sử dụng

G7 không thêm bảng/cột, không có migration mới. Database cần các migration đến V016 của G6. Nếu database hiện tại chưa được nâng, backup và áp dụng tuần tự theo [hướng dẫn G6](phase-6-completion.md); không tạo lại database hoặc xóa dữ liệu. Lần triển khai này chỉ dùng schema kiểm thử riêng, database nguồn giữ nguyên.

Build/restart Academic và Finance, cập nhật app và web cùng phiên bản. Finance đã có services.academic.url; Academic đã có services.finance.url. Các script/env hiện hành cấp URL này, không cần thêm secret hoặc credentials thanh toán trong G7. Native dùng gateway LAN/HTTPS như trước, không dùng localhost của điện thoại.

App → Học phí: tổng quan toàn bộ hồ sơ, danh sách khoản thu, lọc trạng thái. Học tập → lớp → Học phí của đăng ký mở đúng khoản của cả chuỗi chuyển lớp. Mở khoản để xem giảm trừ, điều chỉnh, số còn phải đóng/đóng dư, các phiếu thu. Lịch sử phiếu thu có chi tiết tiền gốc, tiền đảo/hoàn và số còn hiệu lực. Có thể yêu cầu hỗ trợ từ khoản/phiếu đã xác minh; tài nguyên được gắn tự động, backend kiểm lại sở hữu.

Web → Hỗ trợ học viên: thông tin liên quan dùng nhãn Học phí/Phiếu thu và có liên kết mở hồ sơ tài chính theo quyền nhân sự hiện hành. Luồng thu tiền, điều chỉnh và đảo/hoàn trên web tiếp tục dùng API cũ; học viên không được gọi các API này.

G7 không có nút giả xác nhận đã trả tiền. Hướng dẫn trên app là liên hệ trung tâm để đóng hoặc xử lý tiền dư; giao dịch chỉ xuất hiện khi được ghi vào ledger thật.

## Nguồn và sở hữu

Academic trả studentId đã đối chiếu account/profile cùng các đăng ký do học viên sở hữu. Billing root được giải toàn chuỗi; null/self là gốc, root thiếu/khác chủ hoặc vòng lặp bị409, không suy từ email/SĐT hoặc lớp trùng tên. Đăng ký chuyển, hoàn thành, bảo lưu, bỏ học vẫn giữ quyền đọc lịch sử tài chính.

Finance dùng bearer hiện hành để lấy scope. Mỗi tuition phải có studentId đúng người đọc; khoản xác minh phải gắn đúng root, class và branch. Chuyển lớp không tạo quyền nhân sự hay khoản nợ mới: các enrollmentId trong chuỗi dùng một tuition gốc. Lọc enrollmentId khác chủ trả404. X-Branch-Id/studentId giả không đổi chủ sở hữu. Giá/nhãn của đăng ký không thay thế tiền đã được ghi trong Finance.

Tuition chưa liên kết hoặc nằm trên enrollment con thay vì root vẫn có thể được nhận diện là dữ liệu cần đối soát của chính học viên, nhưng không xuất ra số dư đã xác nhận và không cộng thành nợ lần thứ hai. Không lộ tên lớp/khóa của người khác qua liên kết sai. Metadata lớp/khóa đã xác minh lấy từ scope gốc; học phí gốc cũ null giữ fallback hiện hành của backend là amount.

## Cách tính và trạng thái

Nguồn netPaid là tổng phiếu PAID trừ payment_corrections (VOID/REFUND); phải khớp paidAmount lưu trên tuition. Không cập nhật hay tự sửa cache/ledger khi GET. Phiếu cũ PENDING/PARTIAL/FAILED/REFUNDED không có bằng chứng ledger đầy đủ được đánh dấu REVIEW_REQUIRED, không mặc định là tiền đã nhận hoặc tự suy hoàn tiền.

Phiếu PAID hợp lệ có trạng thái VALID; VOID toàn bộ → VOIDED; REFUND một phần → PARTIALLY_REFUNDED; hoàn đủ → REFUNDED. Phiếu gốc vẫn giữ nguyên số tiền, số phiếu và ngày; các lần xử lý là lịch sử riêng. VOID là đảo phiếu kế toán, khác nghiệp vụ hoàn tiền. Hiệu chỉnh âm/vượt tiền, sai liên kết tuition/branch hoặc thiếu đối ứng không tạo netAmount giả.

Với khoản đang hiệu lực, outstanding=max(totalAmount-netPaid,0), credit=max(netPaid-totalAmount,0). Quá hạn theo ngày Việt Nam, còn nợ và không CANCELLED. Có tiền dư không có nghĩa app tự cho hoàn; nhân sự xác minh và thực hiện theo quy trình hiện hành.

CANCELLED không được cộng vào cả bốn tổng đang hiệu lực; vẫn xem được phiếu/lịch sử. Số đã thu của khoản hủy là lịch sử, không mặc định đã hoàn. Chi tiết khoản hủy hiển thị outstanding/credit khả thu bằng0 và hướng dẫn liên hệ trung tâm.

Summary tính toàn bộ khoản của tài khoản bằng truy vấn theo chủ, độc lập trang/bộ lọc đang xem. Chỉ có grand totals khi coverageComplete: không khoản cần đối soát và không billing root thiếu tuition. Thiếu đồng bộ hoặc dữ liệu legacy/mismatch → grand totals=null, có reviewCount/pendingBillingRoots; không biến thành0đ hay cộng thiếu một trang. Tổng chỉ bao gồm khoản hiệu lực, cancelledCount riêng. Tài khoản thực sự không có đăng ký/tuition trả tổng0 hợp lệ.

Mọi số tiền API mới là decimal string, hai chữ số thập phân. App định dạng trực tiếp bằng chuỗi, không parseFloat/Number để cộng tiền, không làm tròn cents qua JavaScript. Currency bản đầu VND; tiền khác loại cần đối soát. ReceiptNo có thể null với phiếu cũ, app hiển thị mã nội bộ dễ tra cứu và vẫn giữ trạng thái xác minh.

## API G7 đã thực hiện

Tất cả thêm prefix `/api/v1`, bearer STUDENT đã kích hoạt. Nhân sự không gọi endpoint `/student/me`.

| Method | Path | Dữ liệu |
|---|---|---|
| GET | `/academic/student/me/billing-scopes` | studentId,registrations[{enrollmentId,billingRootId,classId,className,courseId,courseName,branchId}] |
| GET | `/finance/student/me/tuitions` | page,size1..100,status?,overdue?,enrollmentId?; PageResponse |
| GET | `/finance/student/me/tuitions/summary` | Tổng tài khoản, coverage,reviewCount,pendingBillingRoots,asOf; không dựa vào trang |
| GET | `/finance/student/me/tuitions/{id}` | tuition projection + adjustments công khai |
| GET | `/finance/student/me/receipts` | tuitionId?,page,size1..100; lịch sử phiếu kể cả đã đảo/hoàn |
| GET | `/finance/student/me/receipts/{id}` | receipt,tuition,corrections công khai |

Tuition projection: id,billingRootId,enrollmentIds,enrollmentLabel,courseName,branchId,currency,originalAmount,discountAmount,totalAmount,netPaid,outstanding,credit,dueDate,status,overdue,cancelled,verification,reviewCode,version. netPaid/outstanding/credit null nếu chưa xác minh; root/branch/nhãn không lấy từ liên kết ngoài sở hữu.

Receipt projection: id,tuitionId,receiptNo,paidAt,amount,netAmount,voidedAmount,refundedAmount,currency,method,state. Không trả note, referenceCode ngân hàng, recordedBy/name, requestKey, reason nội bộ. Public correction chỉ id,type,amount,date; adjustment chỉ số tiền/giảm trừ/hạn trước và sau/ngày, không lý do hoặc nhân sự.

Các API support của G6 nhận thêm resourceType=TUITION/RECEIPT. Academic lấy proof bằng endpoint Finance dành cho chính student, không dùng endpoint quản trị; chỉ gắn ticket khi tuition có branch đã xác minh. Không tự điều chỉnh tiền. Web link nhân sự vẫn kiểm quyền branch ở endpoint vận hành.

6API mới; tổng hiện tại213: Auth24, Academic135, Teaching37, Finance17. Không thêm payment-orders/webhook/checkout trong G7.

Deep link: `/tuition?enrollmentId=…`, `/tuitions/{id}`, `/receipts?tuitionId=…`, `/receipts/{id}`, `/support?category=PAYMENT&resourceType=TUITION|RECEIPT&resourceId=…`. Giữ đích qua đăng nhập/đổi mật khẩu bắt buộc, kiểm lại quyền lúc đọc.

## Kiểm chứng và giới hạn

- 428 tests backend,48 app,96 web; TypeScript, web lint/build và Android Hermes export đạt.
- MySQL riêng: schema đến V016, replay migrations và prod/validate;63 checks setup/G4,52 API G7,6 SQL integrity. Tạo tuition, ghi phiếu, điều chỉnh, VOID/REFUND chạy qua API vận hành thật. Enrollment/chuỗi chuyển lớp và CANCELLED/legacy fixture seed SQL riêng; không tuyên bố kiểm lại writer chuyển lớp/hủy bằng các fixture đó.
- 11 luồng Edge/API thật: giữ bộ lọc qua login, số tổng toàn tài khoản, lịch sử điều chỉnh, phiếu đảo/hoàn, đóng dư, yêu cầu hỗ trợ gắn phiếu, chặn ID người khác, trạng thái cần đối soát, lọc rỗng native và staff mở phiếu từ ticket.
- SQL đối chiếu net/cache, correction đúng phiếu/tuition/branch, không âm, tài nguyên ticket đúng học viên, các GET/UI không đổi version/paidAmount.
- [Tổng quan](figures/phase7-tuitions.png), [chi tiết học phí](figures/phase7-tuition-detail.png), [phiếu thu](figures/phase7-receipt.png), [đối soát](figures/phase7-review.png), [ticket tài chính trên web](figures/phase7-web-support.png).

Evidence: `storage/student-phase7-latest.txt` → summary.json, manifest/hash5JAR, API/UI/SQL reports. Scripts tại scripts/student-app/phase7. Prepare `--phase=7` → Maven package → runtime `-Action start -Phase 7` → phase4/live-api.cjs `--phase=7` → phase7/live-api.cjs → start-ui.ps1/live-ui.cjs → verify-sql.cjs/summarize.cjs. Cần log phase7 app typecheck/test/export và web lint/test/build trước summarize. Prepare run mới trước mỗi lần seed.

Database nguồn và ZIP release cũ giữ nguyên. Runtime/UI kiểm thử riêng được dừng sau nghiệm thu. Edge mobile viewport/Android bundle không thay nghiệm thu thiết bị thật; native device/back/keyboard/SecureStore ở G10. Không tuyên bố Expo Doctor đạt; cảnh báo chunk lớn web vẫn như trước. Tiếp theo G8 cần tích hợp nhà cung cấp thanh toán sandbox, callback xác thực và đối soát, không xác nhận tiền từ return URL.
