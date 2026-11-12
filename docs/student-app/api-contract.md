# Draft API Student App v1

**Hiện thực G4:** [Lớp/lịch đã triển khai](phase-4-completion.md). Các phần chưa nêu trong báo cáo G3/G4 vẫn là dự kiến.

**Hiện thực G3:** [API đã triển khai](phase-3-completion.md). Các nhóm còn lại bên dưới vẫn là dự kiến; dùng hợp đồng G3 cho account/profile.

Đề xuất ngày11/11/2026, CHƯA IMPLEMENT. Không tăng số173 endpoint hiện hành. Dùng route theo service đang có để không thêm Gateway route/BFF: /auth, /academic, /teaching, /finance. Trừ auth login và provider webhook, mọi API student yêu cầu STUDENT đã kích hoạt và có profile; service tự kiểm JWT/sessionVersion/ownership.

## Quy ước chung

Base /api/v1. Danh sách trả {content,page,size,totalElements,totalPages}, page0-based,size mặc định20,max100, sort ổn định theo ngày rồi id. ID giữ kiểu hiện hành; app coi là định danh, không chuyển nhầm profileId/accountId/enrollmentId. Offset thời gian rõ ràng, businessTimezone Asia/Ho_Chi_Minh. Query ngày yyyy-MM-dd; from/to inclusive, bắt buộc cho lịch, cửa sổ tối đa92ngày; size lớn không thay cho tải các trang.

Đề xuất error envelope mới cho API student: {code,message,requestId,fieldErrors?}; không đổi breaking envelope API quản trị hiện có. 400 validation,401 hết phiên,403 role/profile/activation,404 resource ngoài sở hữu,409 version/idempotency/state,429 rate limit với Retry-After,503 dependency unavailable. G3 ánh xạ nhất quán từng service. Không gửi stacktrace/token/storage path. X-Request-Id để trace, requestId trong mutation để dedup là hai khái niệm khác nhau. RequestId intent UUID, version số nguyên>=0. POST create201+Location; mutation khác200, no-body204; webhook200 chỉ sau lưu bền event,503 nếu chưa lưu được.

## Auth và thiết bị

| Method | Path | Hợp đồng dự kiến |
|---|---|---|
| POST | /auth/login | Tái sử dụng body/response hiện hành; thêm STUDENT, mustChangePassword; không giả refresh token |
| GET | /auth/me | Mở rộng STUDENT; accountId,username,role,mustChangePassword; không lộ password/sessionVersion |
| POST | /auth/me/password | Dùng currentPassword,newPassword,confirmPassword hiện có; 204,revoke all |
| POST | /auth/me/logout-all | Tái dùng; 204,revoke all; app ghi nhãn đăng xuất tất cả thiết bị |
| PUT | /auth/student/me/devices/{installationId} | {pushToken,platform:ANDROID/IOS}; idempotent, token không trong logs |
| DELETE | /auth/student/me/devices/{installationId} | 204; chỉ owner, gỡ trước logout khi có thể |

Không public reset endpoint ở bản đầu. App recovery hiển thị số trung tâm cấu hình sẵn; nhân viên xác minh rồi reset trên web. Không cài email/OTP giả. Rate limit login/reset/cấp tạm và log đã che dữ liệu.

## Academic: chỉ dữ liệu của mình

| Method | Path | Query/body và response projection |
|---|---|---|
| GET | /academic/student/me/profile | {studentId,studentCode,fullName,dateOfBirth,gender,email,phone,address,status,branch:{id,name,publicPhone,address},version}; không ghi chú nội bộ |
| GET | /academic/student/me/enrollments | page,size,status?; {id,classId,classCode,className,courseName,programName,branch,status,startDate,endDate} |
| GET | /academic/student/me/enrollments/{id} | Projection trên + teacherDisplayName,publicEvents,periods; không roster, không ghi chú staff |
| GET | /academic/student/me/schedule | from,to,enrollmentId?,page,size; {sessionId,enrollmentId,classId,className,date,startTime,endTime,status,roomName,branch,teacherDisplayName,isMakeup,version}; một session không nhân đôi khi nối enrollment |
| GET | /academic/student/me/sessions/{id} | Chi tiết được phép theo period đăng ký; không attendance cả lớp |
| GET | /academic/student/me/enrollments/{id}/attendance | from?,to?,page,size; {sessionId,date,status:PRESENT/ABSENT/LATE/NOT_RECORDED/LEGACY_UNVERIFIED,hasPermission} |
| GET | /academic/student/me/enrollments/{id}/attendance-summary | {eligibleCompleted,present,late,absent,unrecorded,legacyUnverified,coverageComplete,participationPercent,ruleCode} |
| GET | /academic/student/me/support-requests | page,size,status?,category?; chỉ request mình |
| POST | /academic/student/me/support-requests | {requestId,category,subject,description,resourceType?,resourceId?}; subject5..120,description10..2000; 201 |
| GET | /academic/student/me/support-requests/{id} | {id,category,subject,status,createdAt,updatedAt,version,publicMessages}; không internalNote |
| POST | /academic/student/me/support-requests/{id}/messages | {requestId,version,message};1..2000, không CLOSED; public response |
| POST | /academic/student/me/support-requests/{id}/close | {requestId,version}; không sửa nghiệp vụ gốc |
| GET | /academic/student/me/notifications | page,size,unread?; {id,type,title,body,resourceType,resourceId,createdAt,readAt} |
| GET | /academic/student/me/notifications/unread-count | {count}; không suy từ trang đang tải |
| PUT | /academic/student/me/notifications/{id}/read | idempotent,204 |
| GET | /academic/student/me/notification-preferences | {pushEnabled,schedule,grades,tuition,payment,documents,support,version} |
| PUT | /academic/student/me/notification-preferences | Cùng fields +version;200; không xóa inbox bắt buộc khi tắt push |

## Teaching

| Method | Path | Projection |
|---|---|---|
| GET | /teaching/student/me/enrollments/{id}/grades | page,size; {examId,name,date,score,maxScore,studentComment,publicationRevision,publishedAt,gradeStatus}; chỉ published snapshot |
| GET | /teaching/student/me/enrollments/{id}/final-result | 200 {state:PENDING/CONFIRMED/UPDATING,result:null hoặc {outcome,publishedAt,summary}}; không lấy nhầm legacy/reopened |
| GET | /teaching/student/me/documents | enrollmentId?,page,size; {id,title,description,filename,mimeType,size,revision,sharedAt}; approved + share + ownership |
| GET | /teaching/student/me/documents/{id}/download | stream Content-Type,Content-Disposition; kiểm quyền lại, không trả storageKey; chặn sau retract/revoke |

## Finance

| Method | Path | Projection |
|---|---|---|
| GET | /finance/student/me/tuitions | page,size,status?,overdue?; {id,billingRootId,enrollmentLabel,currency,totalAmount,netPaid,outstanding,dueDate,status,version}; decimal string |
| GET | /finance/student/me/tuitions/{id} | Chi tiết + các điều chỉnh công khai; không internal notes |
| GET | /finance/student/me/receipts | tuitionId?,page,size; {id,receiptNo,paidAt,amount,netAmount,currency,method,state} |
| GET | /finance/student/me/receipts/{id} | +tuitionLabel,publicReference,corrections[{type,amount,date}]; không thông tin nhân sự riêng/secret provider |
| POST | /finance/student/me/payment-orders | {requestId,tuitionId,amount,currency:VND};201 {id,state,amount,currency,expiresAt,checkoutUrl}; URL do server provider adapter tạo và allowlist, không nhận returnUrl tùy ý |
| GET | /finance/student/me/payment-orders | page,size; gồm pending để khôi phục sau đóng app |
| GET | /finance/student/me/payment-orders/{id} | {id,state,amount,currency,expiresAt,receiptId?,publicMessage}; ownership; không raw provider payload |
| POST | /finance/payment-providers/{provider}/webhook | Public transport nhưng xác minh chữ ký theo nhà cung cấp, dedup, server-to-server; không student JWT; body cụ thể chốt G8 |

App return/deep link chỉ là tín hiệu đọc GET order; không có student POST mark-paid. Receipt download/PDF không nằm bắt buộc bản đầu, chi tiết biên nhận trên app là bắt buộc. Không tạo endpoint aggregate total mơ hồ từ một trang; tổng overview cần query server toàn bộ billing root nếu UI dùng.

## API web vận hành phải bổ sung cùng tính năng

| Service/path prefix /api/v1 | Endpoint đề xuất | Quyền |
|---|---|---|
| auth | POST /student-accounts; GET /student-account-provisionings/{requestId} | SA/ADMIN; body studentId,expectedStudentVersion,username,requestId; scope được xác minh Academic; response không log password tạm |
| auth | GET /student-accounts?studentId=; POST /student-accounts/{accountId}/reset-password; PATCH /student-accounts/{accountId}/status | SA/ADMIN scope; reset/lock có reason/version; STAFF chỉ GET tình trạng tối thiểu qua academic students projection |
| teaching | POST /exams/{id}/student-publications; POST /exams/{id}/student-publications/retract | SA/ADMIN/TEACHER chính đúng phạm vi; {requestId,version,reason}; cập nhật snapshot theo domain |
| teaching | PATCH /documents/{id}/student-sharing | SA/ADMIN/TEACHER owner đúng scope, document APPROVED; {version,sharedToStudents}; ghi audit |
| academic | GET /support-requests; GET /support-requests/{id}; POST /support-requests/{id}/messages; PATCH /support-requests/{id}/status | SA/ADMIN/STAFF scope; public reply tách internal note; {requestId,version,...} |
| finance | GET /payment-orders; GET /payment-orders/{id}; POST /payment-orders/{id}/reconcile | SA/ADMIN; STAFF xem scope; reconcile cần xác minh server nhà cung cấp,reason,version,requestId; không nút mark-paid tùy ý |

Đây là contract draft đủ phân rã công việc, chưa OpenAPI sinh code. Trước mỗi giai đoạn implementation phải chốt DTO thực theo code đang có, cập nhật contract và test; không thay các endpoint quản trị cũ thành student API.



## Hợp đồng G5 đã hiện thực (12/11/2026)

[Projection và tám endpoint thực tế](phase-5-completion.md#api-thực-hiện) được ưu tiên cho các nhóm grades/final-result/attendance/publication so với draft G1 ở trên. Grades dùng `examName,examDate,revision,status`; final-result dùng trực tiếp `state,outcome,revision,confirmedAt`, không có object result lồng. Attendance có `summary` toàn đăng ký cùng page content; `source` phân biệt VERIFIED/LEGACY_UNVERIFIED. Các nhóm documents/finance/notifications/support vẫn thuộc các giai đoạn sau.


## Hợp đồng G6 đã hiện thực

[15API và DTO thực tế](phase-6-completion.md#api-đã-triển-khai) ưu tiên so với draft G1 cho documents/support. Detail ticket gồm request+messages; public message không có actorName/internalNote. Recovery dùng /support-requests/operations/{requestId}. Linked resource bản đầu PROFILE/ENROLLMENT/SESSION; chưa nhận TUITION/RECEIPT trước G7.


## Hợp đồng G7 đã hiện thực

[6API đọc mới và DTO](phase-7-completion.md#api-g7-đã-thực-hiện) ưu tiên so với draft G1 cho tuition/receipts. Có GET tuitions/summary; detail gồm tuition+adjustments hoặc receipt+tuition+corrections. Số tiền decimal string, số dư chưa xác minh=null, không trả ghi chú/actor/referenceCode nội bộ. Support G6 nhận thêm TUITION/RECEIPT có Finance proof; payment-orders/webhook chưa triển khai G7.


## Hợp đồng G8 đã hiện thực

[12 API và DTO G8](phase-8-completion.md#api-đã-hiện-thực) ưu tiên so với draft G1 cho payment-orders. Return không settlement, sai chữ ký không vào inbox; manager querydr có audit/recovery.


## Hợp đồng G9 đã hiện thực

[8 API thực tế và DTO](phase-9-completion.md#api-g9-thực-tế) ưu tiên so với draft G1. Có GET chi tiết notice, lọc category; preferences áp dụng push, inbox luôn lưu. Devices thuộc Auth, chỉ trả binding không trả token. Home tổng hợp API tại app.

## Tối ưu Học phí và Thanh toán — giai đoạn 1–4

[Hợp đồng bổ sung](payment-flow-phases-1-4-completion.md#api-và-hợp-đồng-bổ-sung): GET payment-context theo khoản, batch payment-contexts tối đa 20 id, `status=DUE`, danh sách đơn `summary=true`, metadata lớp/khóa và mã lỗi từ chối rõ ràng. Mặc định API cũ vẫn giữ khả năng tương thích; Return không xác nhận tiền.
