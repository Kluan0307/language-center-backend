# Version, lịch sử và bất biến dữ liệu

Ngày08/11/2026; đọc với [Use Case](../use-cases.md), [BR](../business-rules.md) và [từ điển](data-dictionary.md). SQL nullable/enum là giới hạn vật lý; trạng thái được service cho phép có thể hẹp hơn tập enum khai báo.

## Các trục trạng thái tách biệt

| Nguồn | Trạng thái/ý nghĩa chính | Quy tắc |
|---|---|---|
| Account | status, sessionVersion, mustChangePassword | Khóa bảo mật không đồng nghĩa Teacher nghỉ việc; thu hồi phiên kiểm Auth hiện tại |
| Teacher | ACTIVE/INACTIVE + account/branch availability | Phải bàn giao hết trách nhiệm; quyền sửa chung đủ mọi phân công |
| Student | STUDYING/PAUSED/GRADUATED theo enum | Hồ sơ không tự đổi tất cả đăng ký hoặc cấp trình độ từ final |
| Enrollment | STUDYING/RESERVED/COMPLETED/DROPPED/TRANSFERRED | Terminal không mở ngầm; reservation date không tự resume; giữ billing root khi transfer |
| EnrollmentPeriod | [activeFrom,activeUntil) | Ngày đầu tính, ngày cuối không tính; null until còn mở; không tự điền legacy |
| ClassSession | SCHEDULED/COMPLETED/CANCELLED; REGULAR/MAKEUP/LEGACY | Ngày qua không tự hoàn tất; chỉ nguồn xác nhận buổi mới cộng đã học |
| Assessment | DRAFT/CONFIRMED/SUPERSEDED/REVOKED | Một CONFIRMED/student/program bằng generated unique; revoke không khôi phục bản cũ |
| PlacementException | REQUESTED/APPROVED/REJECTED/CONSUMED/INVALIDATED | Validity khi đọc có thể khác stored status; approval không giữ chỗ; consume cùng transaction học vụ |
| Provisional evaluation | PASS/FAIL/INSUFFICIENT | Tính từ nguồn hiện tại; không phải FinalResult và không persisted thành enrollment status |
| FinalResult | outcome PASS/FAIL; state CONFIRMED/REOPENED | Một current/enrollment, snapshot bất biến; reopen toàn lớp vì nguồn dùng chung |
| SchoolClass/closure | stored progress, dateStatus, closed và verification | Hết ngày khác đóng thủ công; LEGACY_UNVERIFIED/NEEDS_REVIEW không được coi đã đối soát |
| Document | PENDING/APPROVED/REJECTED, revision | Resubmit REJECTED bởi chủ sở hữu; file cũ giữ qua history, không chỉ metadata bản mới |
| Tuition/Payment/journal | Projection phải thu/thu ròng/nợ/credit và phiếu gốc | VOID/REFUND ghi journal, không UPDATE amount phiếu gốc để xóa lịch sử; enum Payment legacy rộng hơn luồng tạo PAID |

## Optimistic version và revision thủ công

**13 lớp có @Version:** auth.Teacher, academic.Program, academic.ProgramLevel, academic.PlacementAssessment, academic.Enrollment, academic.Lead, academic.PlacementException, academic.ClassSession, academic.Student, teaching.Document, teaching.Exam, teaching.Grade, finance.Tuition. Không tự đánh dấu revision policy/plan/closure/result hoặc sessionVersion là @Version. Grade có @Version trong source hiện tại; việc một endpoint có yêu cầu client gửi version hay không phải đọc DTO/service, không suy từ annotation. Các bảng closure/final/policy dùng parent/pessimistic lock và expectedRevision/intent theo giao thức riêng.

## Snapshot/audit và scalar reference

Assessment giữ chương trình/mức/source; admission giữ context lúc chấp nhận; policy giữ tên/mức/thang/trọng số; duration giữ kế hoạch nguồn; final giữ policy/row/attendance/duration và các revision. Tên đã chụp không tự đổi theo hồ sơ hôm nay. ID actor trong lịch sử thường scalar, không tự nối FK khi tài khoản thay đổi.

Tính tiền dùng DECIMAL trong SQL và BigDecimal trong Java; giờ/phút không lấy duration chưa biết đơn vị để nhân ngầm. Unique intent và generated keys được ghi chính xác theo bảng; không gọi mọi request là idempotent nếu API không có hợp đồng đó. Shared account lock và result barrier là ngoại lệ phối hợp của cùng schema; HTTP không atomically commit hai service. File storage nằm ngoài SQL, backup cần cả hai.
