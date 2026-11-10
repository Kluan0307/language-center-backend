# Backend implementation decisions

This document reconciles the project plan, the XMI domain model and the backend.
Service ownership is authoritative: shared MySQL does not permit another service
to map or query the owner's tables except the documented teacher-account transaction lock below.

1. Account profiles belong to auth; operational data belongs to academic;
   exams, grades and documents belong to teaching; tuition and payments to finance.
2. Four roles only. Non-super users are restricted to live AccountBranch assignments; branchId is the primary branch. Header X-Branch-Id selects one assigned working branch.
   Super Admin inherits operational Admin permissions, but never bypasses business rules.
3. Students belong to one branch; teachers and admins may have multiple branch assignments. Staff currently have at most one. Cross-branch transfer
   is outside scope. Student phone is a contact, not a unique identity; student code is unique.
4. Staff see all branch leads. Only Admin/Super Admin set prices or change existing
   class teacher, room, dates and timetable. Student timetable conflicts are checked
   both on enrollment and on subsequent class/schedule changes.
5. A course with null branch is shared and can only be changed by Super Admin.
6. Progress of a class is UPCOMING/ONGOING/FINISHED; capacity is derived separately.
7. One attendance session per class/day in this release. Multiple slots on the same
   weekday are rejected. Only scheduled dates inside class dates are eligible.
   Enrollment active dates are retained; historical sheets include saved records.
8. Each enrollment has at most one tuition. Tuition is a price snapshot. Payments
   are immutable receipts, cannot exceed the balance and record their collector.
   Same-course transfers keep the original billing enrollment and price, including
   its receipts; no second tuition is created. Cross-course transfers and refunds
   require a dedicated future adjustment workflow and are rejected in this release.
9. Failed tuition delivery never rolls back enrollment. Delivery can be retried
   explicitly using the user's JWT; finance creation is idempotent by enrollment.
10. Grade updates retain the original author; administrative corrections require a reason.
    Documents use local private storage and authenticated downloads with approval checks.
11. JWT lifetime remains 24h with no refresh tokens. Academic/Teaching/Finance verify live account status, role and assignments through Auth /me on every authenticated request; Auth outages fail closed with 503.
12. Expense, Invoice, Promotion and Facility are deferred; their public APIs must
    not expose incomplete workflows. VNPay and the student portal remain out of scope.
13. Branch-owned course writes require an active branch. Branch is locked before course,
    consistently with class writes; Staff fee preservation is checked after refreshing the
    locked course so a stale request cannot undo a concurrent Admin price change.

## Delivery order

Stage 6 reporting clarifications (2026-11-04):

- Operations statistics are current snapshots; new-student count uses the current
  business month. Financial date filters apply only to PAID receipts, inclusively.
  Outstanding debt is current, not a reconstructed period-end balance.
- Global occupancy/conversion ratios use global numerators and denominators, never
  averages of branch percentages. Month keys are normalized to YYYY-MM and sorted.
- Teacher dashboards derive seven-day sessions from their full assigned class/schedule
  lists and class dates. No holiday/makeup calendar is invented; student counts are
  enrollment places across classes, not unique people.
- Staff dashboards do not call manager-only Finance Stats; Teacher dashboards do not
  call aggregate Stats. Failed sources show unknown values rather than fabricated zeroes.
- Report CSV includes scope/date semantics and protects spreadsheet cells from formula
  interpretation. Mobile navigation and route-level code splitting retain the shared
  sidebar/route authorization configuration.

Stage 5 clarifications (2026-11-04):

- Attendance UI uses the server's roster for the requested date and sends enrollment IDs
  for the entire sheet. It does not reconstruct historical attendance from current students.
  Default PRESENT rows are explicitly unsaved until confirmation.
- Grade UI joins current students with existing grades, preserving departed students'
  history and distinguishing zero from unentered grades. Teacher editing remains subject
  to current assignment and current enrollment; administrators only correct existing grades.
- Document review now locks the pending document before checking its status and writing
  the decision, returning a conflict for the losing reviewer instead of overwriting audit.
- Teacher course upload choices come from assigned classes; manager/Staff filters include
  the full accessible course catalog. Downloads remain authenticated, never public file URLs.
- No new historical exam roster, grade-entry deadline, document resubmission or exam-date
  policy was invented. These remain separate backend decisions if required later.

Stage 4 finance clarifications (2026-11-04):

- Tuition responses expose the latest adjustment actor, timestamp and reason. This is
  the latest adjustment snapshot, not a history of every adjustment.
- Payment idempotency compares note as well as tuition, amount, method and reference.
- The frontend persists the exact pending payment in localStorage scoped to accountId
  before POST and replays it unchanged after an uncertain result or reload. It clears
  the request only after a receipt is confirmed, or an explicitly discarded definite
  rejection. A request-key mismatch cannot be discarded through that rejection flow.
- Collection refreshes the current tuition before opening a new form; the server still
  rechecks the balance under lock. Follow-up reads are separate from POST success.
- Receipts remain immutable. Bank transfer is a manual collection record, not automatic
  bank reconciliation. Refunds and a full adjustment history remain deferred.

Stage 3 clarifications (2026-11-04):

- Lead conversion is idempotent after CONVERTED; unknown birth dates remain null.
- Student profile status does not implicitly change enrollment history. PAUSED/GRADUATED
  requires no STUDYING enrollment; enroll/resume/transfer requires a STUDYING profile.
  Student writes use READ_COMMITTED and branch-before-student locks to see current enrollment state.
- A RESERVED registration prevents fresh enrollment into that same class. It can resume
  (with capacity/timetable checks) or close as DROPPED; terminal registrations stay terminal.
- Closing a registration before its future start creates an empty active period rather
  than an interval ending before it starts. Historical attendance periods remain half-open.
- Transfer DTOs show tuition delivery/error from the billing root, including list results.
  Delivery success means tuition exists, not that payment has been received.
- Shared family phones require removing any obsolete phone/branch unique index in old
  MySQL schemas; the narrow migration and its actual application are recorded separately.

Configuration and auth -> academic -> finance and integration -> teaching -> gateway.
Use targeted automated tests without the developer database. Schema changes are
documented separately; do not delete or reset existing databases automatically.

## Multi-branch improvement stage 1

See [rules and rollout](improvements/phase-one-multi-branch.md). Academic uses a narrow JDBC lock of the auth-owned teacher account in the shared MySQL transaction to serialize cross-branch timetable changes with membership revocation. This is an explicit shared-database coupling, not a general permission to join service-owned data. Replace it with coordinated ownership before splitting databases.

## Account security improvement stage 2 (2026-11-05)

Auth checks account ID and session version as well as username/status. JWT sv matches accounts.session_version; pre-migration tokens without sv mean version zero. Password change/reset, logout-all and locking increment the version. Login/password mutations lock the account row. New/reset accounts must change their temporary password before accessing business APIs. Self profile updates allow only email/phone. Admin password reset requires authority over all target memberships, like global account locking. Credential DTO diagnostics redact passwords/tokens. See [stage 2](improvements/phase-two-account-security.md).

## Cải thiện 3: nguồn lịch chuẩn

ClassSession thay lịch tuần làm nguồn kiểm tra xung đột/điểm danh. Mẫu tuần chỉ đồng bộ tương lai chưa chỉnh tay; các quyết định cũ dựa hoàn toàn trên lịch lặp được thay bằng [chính sách buổi thực tế](improvements/phase-three-class-sessions.md). Một buổi hoạt động/lớp/ngày; học bù riêng liên kết gốc, không chuỗi; chỉ quản lý sửa điểm danh hoàn tất với audit và optimistic version.

## Cải thiện 4

Quyền dự thi/nhập điểm dựa enrollment_periods tại examDate, sau đó dùng snapshot chốt khi Teacher lưu điểm đầu tiên; thay thế quyết định cũ dựa hoàn toàn vào Enrollment.STUDYING hiện tại. Bảo lưu là nghiệp vụ trên từng đăng ký, expectedReturnDate chỉ kế hoạch; không tự hết hạn/học lại. Trạng thái hồ sơ không tự đổi các đăng ký. [Quy tắc ngày, version, dữ liệu cũ](improvements/phase-four-learning-lifecycle.md).

## Cải thiện 5

Quy tắc cũ chặn học phí dưới số đã thu được thay bằng tiền dư credit. paidAmount là số thu ròng đang giữ, balance được chặn ở 0; journal VOID/REFUND giữ nguyên phiếu gốc. collected trên báo cáo đổi thành thu ròng theo ngày của từng giao dịch, công nợ/tiền dư là hiện tại. [Công thức, idempotency và giới hạn](improvements/phase-five-finance-ledger.md).

## Cập nhật hợp đồng giai đoạn cải thiện 6

[Quy tắc hiện hành](improvements/phase-six-care-documents-experience.md) thay thế ví dụ cũ: PUT lead/status/convert và review tài liệu cần version; convert có JSON body. Staff chỉ sửa lead được giao. NOTE/đổi trạng thái không ghi nhận liên hệ thực tế. Tài liệu gửi lại chỉ từ REJECTED bởi chủ sở hữu, tăng revision; lịch sử không dành cho Staff.

## G02–G03 (07/11/2026)

Đã thực hiện BR-01/02/03 cho hồ sơ/vòng đời Teacher theo giả định G01-v1: profileId riêng accountId, @Version/audit, quyền sửa mọi phân công và self-contact cùng version. Work status tách security lock; ngừng/thu hồi cần bàn giao lớp/buổi SCHEDULED, kể cả quá ngày. Auth và Academic phối hợp account-row lock hẹp đã có trong shared schema; live MySQL race đã kiểm tra, không phải distributed transaction. Profile inactive vẫn đọc qua management API riêng, lựa chọn xếp lịch vẫn yêu cầu khả dụng. [G02](thesis-progress/G02.md) · [G03](thesis-progress/G03.md).

## G11/G12 — 08/11/2026

Thực hiện BR-10 với shared class_result_barriers metadata, không join bảng nghiệp vụ chéo service. Final/Academic completion là hai bước, không distributed transaction. Token khác schema chặn chốt. Closure lưu epoch kết quả/nguồn và báo NEEDS_REVIEW nếu thay bằng chứng. [Chi tiết](thesis-progress/G11.md); chuyển sang database độc lập cần thiết kế lại coordination, không chỉ đổi URL.

