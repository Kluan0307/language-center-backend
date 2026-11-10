# Thiết kế động đối chiếu implementation — G15

Cập nhật **10/11/2026**, sau review và nghiệm thu lại G17/G18. [Trạng thái/review](../../reviews/2026-11-10-plan-completion-review.md), [API](../../api-list.md), [hợp đồng](../../api-contracts.md). Nguồn chuẩn là controller/DTO/service hiện tại; các sơ đồ mô tả giao thức ứng dụng, không vẽ SQL giả như câu lệnh chạy thực tế.

## Kiến trúc và cách đọc

Bốn service nghiệp vụ và Gateway chạy riêng, nhưng dùng **chung schema MySQL**. Phần lớn dữ liệu được chia trách nhiệm logic; khóa account và class_result_barriers là ngoại lệ điều phối trực tiếp giữa service. Không gọi kiến trúc này là database-per-service. Mỗi transaction/connection là cục bộ; HTTP không làm hai service commit nguyên tử.

Gateway định tuyến, CORS và request correlation; SecurityConfig không giải mã JWT để phát header identity. Từng service kiểm chữ ký/hạn token và phiên sống từ Auth, rồi role, active assignments và đối tượng. Header X-Branch-Id là yêu cầu chọn phạm vi, không phải bằng chứng được phép. Admin/Teacher có thể nhiều chi nhánh; Staff tối đa một; quyền sửa hồ sơ Teacher chung cần bao phủ mọi phân công. Xem [ma trận quyền](../role-permissions.md).

| Luồng | Sơ đồ | Nguồn chính | Bằng chứng runtime đã có |
|---|---|---|---|
| Ghi danh/học phí | [Activity](../diagrams/activity-enrollment-tuition.svg), [Sequence](../diagrams/sequence-enrollment-tuition.svg) | EnrollmentService, TuitionListener, TuitionDeliveryService, TuitionService | G17-05/06, G18-09/27/28 |
| Thu và retry | [Activity](../diagrams/activity-payment-retry.svg), [Sequence](../diagrams/sequence-payment-retry.svg) | PaymentService, TuitionService, FinanceLedger | G17-07–09, G18-10–13/34–35 |
| Ngoại lệ đầu vào | [Activity](../diagrams/activity-placement-exception.svg), [Sequence](../diagrams/sequence-placement-exception.svg) | PlacementService, PlacementDto, EnrollmentService | G17-03–05, G18-36–38 |
| Bàn giao giáo viên | [Activity](../diagrams/activity-teacher-handover.svg), [Sequence](../diagrams/sequence-teacher-handover.svg) | TeacherProfileService, BranchDirectory, SessionPlanner, TeacherDirectory | G18-06/08/14/15/29 |
| Final/completion/closure | [Activity](../diagrams/activity-final-result-lock.svg), [Sequence](../diagrams/sequence-final-result-lock.svg) | FinalResultService, EvaluationService, ResultBarrierService, CompletionBridge, ClassClosureService | G17-12–17, G18-16–26, DB-02/03/06 |

Mã kiểm tra ở [bảng expected/actual](../../reviews/2026-11-10-g17-g18-real-acceptance.md); kết quả không thay cho chứng minh mọi lịch chạy đồng thời hoặc tải production.

## 1. Ghi danh và phân phối học phí

POST /api/v1/academic/enrollments nhận studentId, classId, approvalId tùy chọn. Academic Tx READ_COMMITTED: khóa branch → student → class; replay approval CONSUMED đúng context được kiểm trước các điều kiện tạo mới. Sau đó khóa course, kiểm đầu vào/sĩ số/lịch và barrier mutable; tạo enrollment, billing root, khoảng học, learning_events, admission/consume ngoại lệ trong cùng transaction.

EnrollmentCreated được publish trong transaction. Listener AFTER_COMMIT **đồng bộ**, không @Async; deliver mở REQUIRES_NEW, khóa/refresh enrollment billing root rồi POST /api/v1/finance/tuitions. Finance đọc GET /api/v1/academic/enrollments/{id}/billing để lấy giá/chủ thể gốc, tìm hoặc tạo tuition theo enrollment_id trong transaction riêng. CREATED hoặc FAILED được ghi trong transaction delivery. Enrollment đã commit không rollback vì Finance lỗi. Nếu không ghi được delivery, trạng thái có thể còn PENDING; nút retry là đường phục hồi có chủ đích.

Controller trả **200**, không 201. DTO tạo trước callback sau commit có thể chưa phản ánh trạng thái delivery cuối; FE phải đọc lại. POST /api/v1/academic/enrollments/{id}/tuition/retry dùng cùng billing root. Không có broker/outbox bền vững hoặc giao dịch phân tán. Chuyển lớp cùng khóa/chi nhánh giữ billing root, không mặc nhiên thu thêm học phí.

## 2. Thu tiền và cùng intent

POST /api/v1/finance/payments dùng CASH hoặc BANK_TRANSFER; chuyển khoản là ghi nhận thủ công. PaymentService Tx READ_COMMITTED khóa/refresh bảng **tuition**, kiểm scope, rồi tìm **payments** bằng branch_id/request_key. Replay phải khớp tuitionId, amount, paymentMethod, referenceCode và note; cùng key khác nội dung trả409.

Intent mới kiểm chưa CANCELLED, ledger đồng bộ và số thu dương không vượt còn nợ; lưu payments, receiptNo PT-UUID và cập nhật tuition.paid_amount qua FinanceLedger. Kết quả **200**. Không có bảng tuitions/receipts/collected_amount trong giao thức này. REFUND/VOID nằm ở payment_corrections và chỉ quản lý được thao tác; phiếu gốc giữ nguyên amount.

Mất phản hồi không có nghĩa chưa commit. FE phải giữ key/body, xác minh hoặc gửi lại cùng intent; không tự đổi key. Unique constraint và khóa cùng bảo vệ dữ liệu; lỗi/lock timeout có thể trả409. G18-13 đã dùng relay cắt socket sau upstream200, retry trả cùng payment ID.

## 3. Ngoại lệ phân lớp

Preview POST /api/v1/academic/enrollments/preview nhận Context gồm studentId, classId, intent (ENROLL/TRANSFER/RESUME), sourceEnrollmentId khi cần. Policy dùng tập trình độ được chấp nhận và đánh giá CONFIRMED theo chương trình; text currentLevel và điểm đầu vào không thay thế căn cứ có cấu trúc. Blockers về sĩ số/lịch/chi nhánh/trạng thái không được bỏ qua bằng ngoại lệ.

POST /api/v1/academic/placement-exceptions nhận **{context,fingerprint,reason}**, khóa context và kiểm fingerprint rồi tạo **REQUESTED** (200). Decision POST /api/v1/academic/placement-exceptions/{id}/decision nhận **{version,approve,reason}**; Admin/SA trong phạm vi, khóa branch/student/class/course rồi exception. Version cũ409; căn cứ đổi có thể trả200 với INVALIDATED; cần đọc trạng thái thay vì suy200 là APPROVED.

Không có expiresAt hay TTL7ngày. Khi authorize, phải đúng context, APPROVED và fingerprint hiện tại; kiểm lại blockers. Admission snapshot và CONSUMED/consumedEnrollmentId ghi cùng Tx enrollment. Retry CONSUMED đúng context trả enrollment cũ, không phải mọi retry đều409. Approval không giữ chỗ. Context đổi có thể được trình bày INVALIDATED khi đọc dù storedStatus vẫn khác; xem DTO.

## 4. Bàn giao và trạng thái Teacher

UI đọc GET /api/v1/auth/teacher-profiles/{id}/responsibilities; Auth gọi Academic GET /api/v1/academic/teachers/{teacherId}/responsibilities theo các chi nhánh cần kiểm. Phản hồi là **mảng Responsibility**, gồm CLASS có effectiveStatus khác FINISHED và SESSION SCHEDULED (cả quá khứ chưa xử lý).

Bàn giao dùng **PUT full ClassRequest** tại /api/v1/academic/classes/{id}; dạy thay dùng **PUT SessionEdit** tại /api/v1/academic/sessions/{id}, kèm version/reason. Không dùng PATCH phần teacherId như thể endpoint hỗ trợ cập nhật một trường.

PATCH /api/v1/auth/teacher-profiles/{id}/work-status nhận version, workStatus và reason. Auth khóa **Account trước Teacher**, kiểm quyền mọi phân công và version. Gọi Academic xác minh lại khi đổi INACTIVE; còn việc409; nguồn lỗi503, không thay trạng thái. Thành công lưu teacher_profile_events và tăng version Teacher. Academic phân công khóa account các giáo viên liên quan qua SessionPlanner/TeacherDirectory trước kiểm khả dụng; cùng schema là điều kiện để khóa này phối hợp với Auth/thu hồi chi nhánh.

## 5. Preview/chốt/mở lại kết quả

GET /api/v1/teaching/classes/{classId}/final-results là **summary đã lưu**, không tự tạo provisional hoặc trả toàn bộ điểm cho Staff. Tính tạm qua GET /api/v1/teaching/classes/{classId}/evaluation-preview. Preview chốt dùng POST /api/v1/teaching/classes/{classId}/final-results/{enrollmentId}/preview; confirm cùng prefix với /confirm, body **{requestId,fingerprint,reason}**. Chốt **một enrollment**, không một lần chốt toàn lớp.

Teaching xác minh class qua Academic; trong Tx READ_COMMITTED khóa **class_result_barriers → evaluation_states**. Đọc Academic result-source, token/sourceRevision phải khớp barrier; EvaluationService gọi evaluation-context (gồm enrollment/period/duration và attendance nếu policy yêu cầu), kết hợp policy/exams/grades/candidates tại Teaching. Không đọc trực tiếp bảng buổi học của Academic để tính thay API.

Blockers gồm lớp đóng, chưa có PASS/FAIL, studyStatus không hợp lệ, duration chưa timeReadyForClosure hoặc enrollment đã có current final. Không tự thêm quy tắc phải chấm toàn bộ lớp, không còn khiếu nại, mọi buổi kể cả CANCELLED phải COMPLETED. Duration service là nguồn xét đủ phút và buổi chưa xử lý. Fingerprint gồm revision học vụ/giảng dạy/kết quả/policy, row và duration.

Confirm kiểm replay requestId đúng class/enrollment/fingerprint/reason trước khi tạo mới. Intent mới tính lại blockers/fingerprint; nguồn đổi409. Lưu một FinalResult CONFIRMED, snapshot JSON, revision theo enrollment, actor/reason; tăng confirmed_count và result_revision. confirmed_count>0 khóa **nguồn chung của cả lớp**: sửa điểm **PUT /api/v1/teaching/grades**, attendance, bài/quy tắc và học vụ liên quan phải đi guard tương ứng.

POST /api/v1/teaching/classes/{classId}/final-results/reopen nhận **{requestId,expectedRevision,reason}**, khóa barrier, kiểm replay/revision; chuyển mọi CONFIRMED sang REOPENED, ghi result_reopens, reset count và tăng revision. Không ghi đè snapshot cũ. Nếu lớp đã đóng, cần mở lại lớp qua Academic có kiểm toán trước khi chốt mới; sửa nguồn rồi preview/chốt lại từng enrollment.

## 6. Hoàn thành đăng ký và đóng lớp

PATCH /api/v1/academic/enrollments/{id}/status với COMPLETED cần version, finalResultId, finalResultRevision, reason. CompletionBridge đọc Teaching summaries và kiểm current CONFIRMED đúng enrollment/revision cùng barrier/token/source. Dependency lỗi503 không cập nhật dở đăng ký. PASS và FAIL đều có thể COMPLETED; kết quả học tập khác trạng thái đã học xong. Nợ học phí không chặn thao tác này.

POST /api/v1/academic/classes/{id}/close cần requestId, expectedRevision, reason. Academic khóa branch/class/barrier, kiểm đủ giờ, **không còn STUDYING hoặc RESERVED**, và mọi COMPLETED có refs đúng kết quả hiện hành. Không có guard bắt trả hết nợ. Lưu FINISHED cùng class_closures/class_closure_events; closure.closed=true và verification=VERIFIED là bằng chứng đã đóng có kiểm toán, không có enum SchoolClass.CLOSED. FINISHED legacy có thể LEGACY_UNVERIFIED. Sau reopen kết quả phải chốt lại, đồng bộ refs rồi mới đóng; chỉ xem trạng thái theo ngày không thay được quy trình này.

## Nguồn và phạm vi xác minh

[EnrollmentService](../../../academic-service/src/main/java/com/example/academic/enrollment/EnrollmentService.java), [TuitionListener](../../../academic-service/src/main/java/com/example/academic/enrollment/TuitionListener.java), [TuitionDeliveryService](../../../academic-service/src/main/java/com/example/academic/enrollment/TuitionDeliveryService.java), [TuitionService](../../../finance-service/src/main/java/com/example/finance/tuition/TuitionService.java), [PaymentService](../../../finance-service/src/main/java/com/example/finance/payment/PaymentService.java), [PlacementService](../../../academic-service/src/main/java/com/example/academic/placement/PlacementService.java), [PlacementDto](../../../academic-service/src/main/java/com/example/academic/placement/PlacementDto.java), [TeacherProfileService](../../../auth-service/src/main/java/com/example/auth/service/TeacherProfileService.java), [FinalResultService](../../../teaching-service/src/main/java/com/example/teaching/results/FinalResultService.java), [EvaluationService](../../../teaching-service/src/main/java/com/example/teaching/evaluation/EvaluationService.java), [ClassClosureService](../../../academic-service/src/main/java/com/example/academic/results/ClassClosureService.java), [lifecycle/invariants](lifecycles-and-invariants.md).

Render/check/hash là kiểm tra hình thức; tính đúng của giao thức còn dựa đối chiếu source và bằng chứng runtime. Danh mục source/hash đối chiếu nằm ở dynamic-source-review.json. Không gọi kiểm tra tĩnh là chứng nhận an toàn production.
