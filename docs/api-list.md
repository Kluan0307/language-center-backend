# Danh sách API hiện có

Cập nhật và kiểm kê theo controller, cấu hình security và kiểm tra quyền trong service thực tế ngày **13/11/2026** (kiểm kê lại controller sau C4).

Có **254 cặp HTTP method + đường dẫn nghiệp vụ chính**: Auth 26, Academic 151, Finance 34, Teaching 40, Assistant 3. Đã tính 13 API trợ lý C2 và 3 API hội thoại C4, hai API work-queue W7, hai API payment-context trong phần cập nhật thanh toán và API điểm công bố W5. Student App G3–G9 bổ sung60API; các số173/179/184/192/207 trong phần lịch sử giữ đúng mốc tương ứng.
Ngoài ra có 3 alias học phí cũ và 2 endpoint chẩn đoán chỉ gọi trực tiếp service.
Gateway định tuyến, không bổ sung controller nghiệp vụ.

## Assistant C4 — 3 API hội thoại

| Method | Đường dẫn | Quyền | Input / kết quả |
|---|---|---|---|
| GET | `/api/v1/assistant/config` | SA/A/S/T | Trạng thái enabled, readOnly, role và hạn mức; không trả key |
| POST | `/api/v1/assistant/chat` | SA/A/S/T | Body message, conversationId?, requestId?, scopeMode?, selection?; header X-Branch-Id?; trả message/cards/choices/sources/scope |
| DELETE | `/api/v1/assistant/conversations/{id}` | SA/A/S/T | UUID hội thoại của phiên hiện tại; 204 |

[Body mẫu, lỗi, cấu hình và hạn mức C4](assistant/c4-completion.md). Chat/MCP tắt mặc định; không có endpoint MCP công khai. Chưa có UI C6, Gemini thật chưa nghiệm thu do timeout/503. Snapshot kiểm kê: `storage/api-inventory.json` sinh bằng `node scripts/inventory-api.cjs`.

C5 giữ nguyên 3 route, bổ sung rate limit theo account, giới hạn body thực, policy và audit; [hành vi hiện tại và kiểm thử C5](assistant/c5-completion.md).

## Cách đọc

- Gọi qua gateway: `http://localhost:8080` + đường dẫn đầy đủ trong bảng.
- Gọi trực tiếp: Auth 8081, Academic 8082, Teaching 8083, Finance 8084, Assistant 8086; giữ nguyên đường dẫn.
- **SA** = SUPER_ADMIN; **A** = ADMIN; **S** = STAFF; **T** = TEACHER.
- Mọi API nghiệp vụ trừ login và đúng hai GET callback VNPAY đều cần `Authorization: Bearer <token>`; callback kiểm chữ ký ngân hàng.
- A/S/T dùng phân công hiện tại từ Auth; `X-Branch-Id` chọn chi nhánh làm việc, thiếu header dùng chi nhánh chính. Query/body ngoài phân công bị từ chối. T chỉ thao tác lớp được giao.
  T vẫn được đọc chi nhánh của mình và khóa học trong phạm vi được phép.
- `page` mặc định 0, `size` mặc định 20 và nằm trong 1–200. Những bảng dưới ghi `page, size` là có phân trang.
- Trường query không có ghi “bắt buộc” là tùy chọn. `{id}` là path parameter.
- Tên body như `StudentRequest` được giải thích trong [hợp đồng và body mẫu](api-contracts.md).
- Quyền trong bảng là quyền thực tế sau cả security/controller/service; vẫn phải thỏa điều kiện nghiệp vụ.

## 1. Auth — 19 API vận hành (5 API Student G3 ở cuối tài liệu)

| Method | Đường dẫn | Quyền | Input / kết quả chính |
|---|---|---|---|
| POST | `/api/v1/auth/login` | Công khai | Body: username, password. Trả token và thông tin người dùng. |
| GET | `/api/v1/auth/me` | SA/A/S/T | Thông tin hiện tại, gồm mustChangePassword và passwordChangedAt. |
| POST | `/api/v1/auth/me/password` | SA/A/S/T | {currentPassword,newPassword,confirmPassword}; 204, thu hồi tất cả phiên. |
| POST | `/api/v1/auth/me/logout-all` | SA/A/S/T | Không body; 204, thu hồi tất cả phiên kể cả phiên hiện tại. |
| PATCH | `/api/v1/auth/me/profile` | SA/A/S/T | {email,phone}; chỉ sửa liên hệ của chính mình. |
| POST | `/api/v1/auth/accounts/{id}/reset-password` | SA/A | {temporaryPassword,confirmPassword,reason}; 204, bắt buộc đổi mật khẩu tạm. A phải quản lý toàn bộ phân công của S/T đó; không cấp lại cho chính mình/SA. |
| GET | `/api/v1/auth/accounts` | SA/A | Query: page, size, branchId, role, keyword. |
| POST | `/api/v1/auth/accounts` | SA/A | Body: CreateUserRequest. A tạo S/T tại chi nhánh được giao; SA được tạo thêm A. |
| PATCH | `/api/v1/auth/accounts/{id}/toggle-status` | SA/A | Không body. A chỉ khóa/mở S/T khi mọi phân công của tài khoản đều thuộc phạm vi A; không khóa SA. |
| GET | `/api/v1/auth/teachers` | SA/A/S | Query: branchId. Trả mảng giáo viên khả dụng, gồm profileId, accountId, fullName, branchId (chính), branchIds. |
| GET | `/api/v1/auth/teachers/{id}` | SA/A/S | id là **teacher profileId**; query branchId tùy chọn; kiểm tra giáo viên hoạt động và được phân công tại chi nhánh đó. |
| GET | `/api/v1/auth/accounts/{id}/branches` | SA | Phân công hiện tại và đã thu hồi. |
| PUT | `/api/v1/auth/accounts/{id}/branches` | SA | Body: {branchIds,primaryBranchId}; thay danh sách phân công. |
| GET | `/api/v1/auth/accounts/{id}/branches/history` | SA | Lịch sử các lần phân công/thu hồi/đổi chi nhánh chính. |

## 2. Academic — 114 API

### Buổi học thực tế — 9

| Method | Đường dẫn | Quyền | Input / kết quả |
|---|---|---|---|
| GET | /api/v1/academic/sessions | SA/A/S/T | page,size,branchId,classId,teacherId,status,from,to; Teacher bị giới hạn actual teacher |
| GET | /api/v1/academic/sessions/{id} | SA/A/S/T | Theo chi nhánh; T là actual hoặc primary teacher |
| GET | /api/v1/academic/sessions/{id}/history | SA/A/S/T | Cùng quyền đọc buổi; audit lịch trước/sau |
| POST | /api/v1/academic/sessions/generate | SA/A | {classId,from?,to?}; sinh ngày nguồn còn thiếu |
| PUT | /api/v1/academic/sessions/{id} | SA/A | SessionEdit, reason và version bắt buộc |
| POST | /api/v1/academic/sessions/{id}/cancel | SA/A | {reason,version} |
| POST | /api/v1/academic/sessions/{id}/restore | SA/A | {reason,version}; kiểm tra xung đột lại |
| POST | /api/v1/academic/sessions/{id}/makeup | SA/A | SessionEdit; buổi gốc REGULAR đã hủy |
| GET | /api/v1/academic/sessions/{id}/attendance-changes | SA/A | Lịch sử sửa điểm danh trong phạm vi |

Điểm danh GET sheet thêm sessionId (ưu tiên); classId + sessionDate vẫn dùng cho client cũ nhưng phải khớp buổi đã tồn tại. POST bulk thêm sessionId,version,reason; chỉ SA/A sửa điểm danh đã hoàn tất. Dạy thay không cấp quyền các tài nguyên khác của lớp.

### Chi nhánh — 5

| Method | Đường dẫn | Quyền | Input / ghi chú |
|---|---|---|---|
| GET | `/api/v1/academic/branches` | SA/A/S/T | Query: page, size, keyword, branchId. |
| GET | `/api/v1/academic/branches/{id}` | SA/A/S/T | Chi tiết chi nhánh trong phạm vi. |
| POST | `/api/v1/academic/branches` | SA | Body: BranchRequest. |
| PUT | `/api/v1/academic/branches/{id}` | SA | Body: BranchRequest. |
| PATCH | `/api/v1/academic/branches/{id}/toggle-status` | SA | Không body; ACTIVE ↔ INACTIVE. |

### Khóa học — 5

| Method | Đường dẫn | Quyền | Input / ghi chú |
|---|---|---|---|
| GET | `/api/v1/academic/courses` | SA/A/S/T | Query: page, size, keyword, status, level, type, branchId. Bao gồm khóa chung. |
| GET | `/api/v1/academic/courses/{id}` | SA/A/S/T | Chi tiết khóa học. |
| POST | `/api/v1/academic/courses` | SA/A | Body: CourseRequest. SA tạo khóa chung bằng branchId=null. |
| PUT | `/api/v1/academic/courses/{id}` | SA/A/S | Body: CourseRequest. S phải giữ nguyên tuitionFee; chỉ SA sửa khóa chung. |
| PATCH | `/api/v1/academic/courses/{id}/status` | SA/A/S | Body: {status}. Chỉ SA đổi trạng thái khóa chung. |

### Phòng học — 6

| Method | Đường dẫn | Quyền | Input / ghi chú |
|---|---|---|---|
| GET | `/api/v1/academic/rooms` | SA/A/S | Query: page, size, keyword, status, branchId. |
| GET | `/api/v1/academic/rooms/{id}` | SA/A/S | Chi tiết phòng học. |
| POST | `/api/v1/academic/rooms` | SA/A/S | Body: RoomRequest. |
| PUT | `/api/v1/academic/rooms/{id}` | SA/A/S | Body: RoomRequest; sức chứa phải phù hợp lớp đã gán. |
| PATCH | `/api/v1/academic/rooms/{id}/status` | SA/A/S | Body: {status}. |
| DELETE | `/api/v1/academic/rooms/{id}` | SA/A/S | Không body; chặn nếu phòng đã gán lớp; thành công 204. |

### Học viên — 7

| Method | Đường dẫn | Quyền | Input / ghi chú |
|---|---|---|---|
| GET | `/api/v1/academic/students` | SA/A/S | Query: page, size, keyword, level, status, branchId. |
| GET | `/api/v1/academic/students/{id}` | SA/A/S | Trả {student, enrollments}. |
| GET | `/api/v1/academic/students/{id}/history` | SA/A/S | Sự kiện hồ sơ và đăng ký, lý do/tác giả. |
| POST | `/api/v1/academic/students` | SA/A/S | Body: StudentRequest; mã học viên do server sinh. |
| PUT | `/api/v1/academic/students/{id}` | SA/A/S | Body: StudentRequest; không chuyển chi nhánh qua PUT. |
| PATCH | `/api/v1/academic/students/{id}/status` | SA/A/S | Body: {status,reason,version}. |
| DELETE | `/api/v1/academic/students/{id}` | SA/A/S | Không body; chặn nếu có đăng ký hoặc Lead đang tham chiếu; thành công 204. |

### Khách hàng tiềm năng — 6

| Method | Đường dẫn | Quyền | Input / ghi chú |
|---|---|---|---|
| GET | `/api/v1/academic/leads` | SA/A/S | Query: page, size, keyword, status, assignedTo, branchId. |
| GET | `/api/v1/academic/leads/{id}` | SA/A/S | Chi tiết Lead, kèm học viên đã chuyển đổi nếu có. |
| POST | `/api/v1/academic/leads` | SA/A/S | Body: LeadRequest. Người tạo được ghi làm người phụ trách. |
| PUT | `/api/v1/academic/leads/{id}` | SA/A/S | Body: LeadRequest. Không sửa Lead CONVERTED. |
| PATCH | `/api/v1/academic/leads/{id}/status` | SA/A/S | Body: {status, note?}. Không đặt CONVERTED trực tiếp. |
| POST | `/api/v1/academic/leads/{id}/convert` | SA/A/S | Body: `{"version": 0}`; chỉ CONTACTED/CONSULTING; tạo Student trong cùng transaction. |

### Lớp học — 10

| Method | Đường dẫn | Quyền | Input / ghi chú |
|---|---|---|---|
| GET | `/api/v1/academic/classes` | SA/A/S/T | Query: page, size, keyword, status, courseId, teacherId, branchId. T bị ép teacherId. |
| GET | `/api/v1/academic/classes/my` | T | Query: page, size. Lớp của giáo viên đang đăng nhập. |
| GET | `/api/v1/academic/classes/{id}` | SA/A/S/T | Trả {schoolClass, schedules}; T chỉ lớp mình. |
| GET | `/api/v1/academic/classes/{id}/students` | SA/A/S/T | Mảng học viên có Enrollment STUDYING hiện tại; không dùng làm danh sách dự thi. |
| GET | `/api/v1/academic/classes/{id}/roster` | SA/A/S/T | date bắt buộc; id/fullName theo khoảng học tại ngày đó; T chỉ giáo viên chính. |
| GET | `/api/v1/academic/classes/{id}/schedules` | SA/A/S/T | Mảng lịch tuần của lớp. |
| POST | `/api/v1/academic/classes` | SA/A/S | Body: ClassRequest. S tạo lớp không kèm lịch. |
| PUT | `/api/v1/academic/classes/{id}` | SA/A/S | Body: ClassRequest. S không đổi teacher/room/course/dates; PUT không nhận schedules. |
| PATCH | `/api/v1/academic/classes/{id}/status` | SA/A | Body: {status}. S bị service từ chối; FULL không được đặt tay. |
| DELETE | `/api/v1/academic/classes/{id}` | SA/A/S | Không body; chặn nếu có đăng ký; thành công 204. |

### Lịch học — 6

| Method | Đường dẫn | Quyền | Input / ghi chú |
|---|---|---|---|
| GET | `/api/v1/academic/schedules` | SA/A/S/T | Query: page, size, classId, dayOfWeek, teacherId, roomId, branchId. |
| GET | `/api/v1/academic/schedules/my` | T | Query: page, size. Lịch của giáo viên đang đăng nhập. |
| POST | `/api/v1/academic/schedules` | SA/A | Body: ScheduleRequest. |
| PUT | `/api/v1/academic/schedules/{id}` | SA/A | Body: ScheduleRequest; không đổi classId. |
| DELETE | `/api/v1/academic/schedules/{id}` | SA/A | Không body; thành công 204. |
| POST | `/api/v1/academic/schedules/check-conflict` | SA/A | Body: ConflictRequest. Xem trước xung đột; không ghi lịch. |

### Đăng ký và chuyển lớp — 8

| Method | Đường dẫn | Quyền | Input / ghi chú |
|---|---|---|---|
| GET | `/api/v1/academic/enrollments` | SA/A/S | Query: page, size, keyword, studyStatus, classId, studentId, branchId. |
| GET | `/api/v1/academic/enrollments/{id}` | SA/A/S | Có billingEnrollmentId, tuitionDelivery, tuitionDeliveryError. |
| GET | `/api/v1/academic/enrollments/{id}/history` | SA/A/S | {periods,events}; lịch sử theo khoảng học và thay đổi. |
| POST | `/api/v1/academic/enrollments` | SA/A/S | Body: {studentId, classId}; kiểm tra sĩ số và trùng lịch. |
| PATCH | `/api/v1/academic/enrollments/{id}/status` | SA/A/S | Body: {studyStatus,reason,version,expectedReturnDate?}; bảo lưu bắt buộc ngày dự kiến sau hôm nay; cùng RESERVED cập nhật ngày. |
| POST | `/api/v1/academic/enrollments/{id}/transfer` | SA/A/S | Body: {newClassId,reason,version}; STUDYING hoặc RESERVED, cùng khóa/chi nhánh, giữ học phí gốc. |
| GET | `/api/v1/academic/enrollments/{id}/billing` | SA/A/S | Snapshot học phí và đăng ký gốc; finance gọi với JWT người dùng. |
| POST | `/api/v1/academic/enrollments/{id}/tuition/retry` | SA/A/S | Không body; thử tạo lại học phí; trả {enrollmentId, status, error}. |

### Điểm danh — 4

| Method | Đường dẫn | Quyền | Input / ghi chú |
|---|---|---|---|
| GET | `/api/v1/academic/attendances/sheet` | SA/A/S/T | Query: sessionId hoặc classId + date; trả sheet theo buổi thực tế. |
| POST | `/api/v1/academic/attendances/bulk` | SA/A/S/T | Body: AttendanceBulk; gửi đầy đủ danh sách có hiệu lực tại ngày đó. |
| GET | `/api/v1/academic/attendances/history` | SA/A/S/T | Query: classId bắt buộc; from, to tùy chọn. Mảng tổng hợp theo ngày. |
| GET | `/api/v1/academic/attendances/summary` | SA/A/S/T | Query bắt buộc: classId. Mảng tổng hợp chuyên cần theo đăng ký. |

T ghi điểm danh buổi thực tế được giao; giáo viên chính đọc buổi dạy thay nhưng không ghi thay. Một lớp tối đa một slot trong cùng ngày trong tuần; ngày điểm danh phải hợp lệ, không ở tương lai.

### Thống kê vận hành — 1

| Method | Đường dẫn | Quyền | Input / ghi chú |
|---|---|---|---|
| GET | `/api/v1/academic/stats/overview` | SA/A/S | Query: branchId, groupByBranch=false, allAssigned=false. A dùng allAssigned=true để tổng hợp các chi nhánh được giao; không kết hợp branchId. Nhóm theo chi nhánh trong phạm vi đã xác định. |

### G04/G05 — chương trình, điều kiện và đánh giá đầu vào — 20 API

Các đường dẫn bên dưới có prefix `/api/v1/academic`. Danh mục là dữ liệu dùng chung; A/S/T chỉ đọc. Chính sách khóa chung chỉ SA sửa; khóa chi nhánh SA/A đúng phạm vi. Assessment chỉ SA/A/S đúng chi nhánh học viên; T không có quyền.

| Method | Đường dẫn sau prefix | Quyền | Body / kết quả |
|---|---|---|---|
| GET | `/programs` | SA/A/S/T | Mảng chương trình, cả ngừng dùng. |
| POST | `/programs` | SA | code,name,active. |
| PUT | `/programs/{id}` | SA | code,name,active,version; code không đổi. |
| GET | `/programs/{id}/levels` | SA/A/S/T | Mảng mức trình độ, rank chỉ để hiển thị. |
| POST | `/programs/{id}/levels` | SA | code,name,rank,active. |
| PUT | `/program-levels/{id}` | SA | code,name,rank,active,version; code/program không đổi. |
| GET | `/courses/{id}/admission` | SA/A/S/T theo phạm vi khóa | policy,canManage,legacyType/Level/EntryRequirement. |
| PUT | `/courses/{id}/admission` | SA/A như trên | expectedRevision,mode,programId,outputLevelId,acceptedLevelIds,reason. |
| GET | `/courses/{id}/admission/history` | Như GET khóa | Mảng các phiên bản, mới trước. |
| GET | `/classes/{id}/admission` | SA/A/S; T lớp mình | classId,revision,policy,canManage. |
| PUT | `/classes/{id}/admission` | SA/A đúng phạm vi lớp | expectedRevision,expectedCourseRevision,reason; áp dụng bản khóa mới nhất. |
| GET | `/classes/{id}/admission/history` | Như GET lớp | Các lần đổi liên kết chính sách. |
| GET | `/students/{studentId}/assessments` | SA/A/S đúng phạm vi | page,size,programId,status; PageResponse. |
| GET | `/students/{studentId}/assessments/current` | Như trên | Mảng CONFIRMED hiện hành, tối đa một/chương trình. |
| GET | `/students/{studentId}/assessments/{id}` | Như trên | AssessmentDto. |
| POST | `/students/{studentId}/assessments` | Như trên | Tạo nháp; AssessmentDraft. |
| PUT | `/students/{studentId}/assessments/{id}` | Như trên | AssessmentDraft + version,reason; chỉ nháp. |
| POST | `/students/{studentId}/assessments/{id}/confirm` | Như trên | version,reason,expectedCurrentId (null khi chưa có). |
| POST | `/students/{studentId}/assessments/{id}/revoke` | Như trên | version,reason; không phục hồi bản cũ. |
| GET | `/students/{studentId}/assessments/{id}/history` | Như trên | Mảng audit trước/sau, tác giả, lý do, version. |

POST/PUT/confirm/revoke thành công trả 200. Mã trùng hoặc version/current reference cũ:409; validation:400; role:403; đối tượng ngoài phạm vi:404 theo convention hiện tại. Không có DELETE danh mục/chính sách/đánh giá. Xem [hợp đồng](api-contracts.md#g04g05--chương-trình-điều-kiện-và-đánh-giá-đầu-vào).

### G06/G07 — phù hợp và ngoại lệ phân lớp — 7 API

Prefix `/api/v1/academic`. Tất cả chỉ SA/A/S và đúng phạm vi học viên/lớp; Teacher không được cấp quyền CRM. Duyệt/từ chối chỉ SA/A.

| Method | Đường dẫn sau prefix | Input / kết quả |
|---|---|---|
| POST | `/enrollments/preview` | Context: studentId,classId,intent,sourceEnrollmentId; lý do học thuật + blockers cứng + fingerprint. |
| GET | `/enrollments/suggestions` | studentId,page,size; PageResponse lớp trong chi nhánh, giữ cả lớp không phù hợp và nguyên nhân. |
| GET | `/enrollments/{id}/admission-history` | Mảng căn cứ chấp nhận ghi danh/chuyển/học lại; rỗng là LEGACY_UNVERIFIED. |
| POST | `/placement-exceptions` | context,fingerprint,reason; tạo REQUESTED, không giữ chỗ. |
| GET | `/placement-exceptions` | studentId,classId,branchId,page,size tùy chọn; PageResponse theo quyền hiện tại. |
| GET | `/placement-exceptions/{id}` | Trạng thái hiệu lực, căn cứ gốc, quyết định, đăng ký đã sử dụng. |
| POST | `/placement-exceptions/{id}/decision` | SA/A: version,approve(boolean),reason. Stale409; căn cứ đổi trả200 trạng thái INVALIDATED. |

Ba API cũ nhận thêm approvalId tùy chọn: POST /enrollments, POST /enrollments/{id}/transfer, PATCH /enrollments/{id}/status khi học lại STUDYING. Không thêm cờ bypass. Thành công200. [Hợp đồng](api-contracts.md) · [G06](thesis-progress/G06.md) · [G07](thesis-progress/G07.md).

### G08/G09 — kế hoạch và tổng thời lượng — 8 API

Prefix `/api/v1/academic`. Mọi API cần đăng nhập. SA/A/S theo phạm vi; Teacher chỉ đọc khóa được phép và tổng kế hoạch của lớp phụ trách chính, không được xem toàn lớp chỉ vì dạy thay một buổi. SA/A xác nhận kế hoạch lớp; khóa chung chỉ SA sửa.

| Method | Đường dẫn sau prefix | Input / kết quả |
|---|---|---|
| GET | `/courses/{id}/duration-plan` | courseId,revision,expectedMinutes,legacyDuration,verified,canManage. |
| PUT | `/courses/{id}/duration-plan` | SA/A: expectedRevision,expectedMinutes,reason. |
| GET | `/courses/{id}/duration-history` | Mảng lịch sử kế hoạch khóa, mới trước. |
| GET | `/classes/{id}/duration-plan` | Kế hoạch riêng lớp + kế hoạch khóa hiện tại. |
| PUT | `/classes/{id}/duration-plan` | SA/A: expectedRevision,source,expectedMinutes hoặc expectedCourseRevision,reason. |
| GET | `/classes/{id}/duration-history` | Mảng các lần xác nhận kế hoạch lớp. |
| GET | `/classes/{id}/duration-progress` | Tổng toàn lớp, không giới hạn bởi bộ lọc ngày. |
| GET | `/classes/duration-progress` | Query classIds=1,2; 1–200 ID khác nhau, kiểm tra mọi lớp trước trả kết quả. |

POST/PUT khóa hiện có nhận expectedMinutes tùy chọn theo hợp đồng; không tăng số endpoint. GET course bổ sung expectedMinutes/durationRevision, duration cũ nullable. [Hợp đồng](api-contracts.md), [G08](thesis-progress/G08.md), [G09](thesis-progress/G09.md).

### G10 — căn cứ học vụ cho đánh giá — 1 API

| Method | Đường dẫn | Quyền / kết quả |
|---|---|---|
| GET | `/api/v1/academic/classes/{id}/evaluation-context` | SA/A/T lớp chính; includeAttendance=false mặc định. Enrollment/periods, session COMPLETED + marks khi yêu cầu, duration, observedAt. Không trả học phí/CRM. |

## 3. Finance — 12 API chính

| Method | Đường dẫn | Quyền | Input / ghi chú |
|---|---|---|---|
| GET | `/api/v1/finance/tuitions` | SA/A/S | Query: page, size, keyword, status, branchId, classId, studentId, overdue. |
| GET | `/api/v1/finance/tuitions/{id}` | SA/A/S | Học phí, số đã đóng, số còn nợ và cờ quá hạn. |
| POST | `/api/v1/finance/tuitions` | SA/A/S | Body: {enrollmentId}; lấy giá/chi nhánh từ academic; có chống tạo trùng. |
| PUT | `/api/v1/finance/tuitions/{id}` | SA/A | Body: TuitionAdjustment; bắt buộc reason,version,requestKey; giảm dưới thu ròng tạo credit. |
| GET | `/api/v1/finance/tuitions/{id}/history` | SA/A/S | {adjustments,corrections}; giá/hạn trước sau, lý do/tác giả. |
| GET | `/api/v1/finance/payments` | SA/A/S | Query: page, size, keyword, branchId, tuitionId. |
| GET | `/api/v1/finance/payments/{id}` | SA/A/S | Chi tiết phiếu thu, mã biên nhận và người thu. |
| POST | `/api/v1/finance/payments` | SA/A/S | Body: PaymentRequest; requestKey bắt buộc, kiểm tra số dư dưới khóa giao dịch. |
| GET | `/api/v1/finance/payments/{id}/corrections` | SA/A/S | Lịch sử đảo/hoàn của phiếu. |
| POST | `/api/v1/finance/payments/{id}/corrections` | SA/A | {type,amount,paymentMethod?,referenceCode?,reason,requestKey,version}; VOID toàn phiếu chưa hoàn; REFUND trong tiền dư. |
| GET | `/api/v1/finance/stats/reconciliation` | SA/A | page,size,branchId,allAssigned; so cachedPaid/ledgerPaid, difference, legacyPayments. |
| GET | `/api/v1/finance/stats/overview` | SA/A | Query: branchId, from, to, allAssigned=false. A dùng allAssigned=true để tổng hợp chi nhánh được giao; không kết hợp branchId. Trả grossCollected,voided,refunded,collected(thu ròng),outstanding,credit,byMonth/byCourse/byBranch. |

### 3 alias tương thích

| Method | Alias cũ | API chính |
|---|---|---|
| GET | `/api/v1/finance/tuition` | GET /api/v1/finance/tuitions |
| GET | `/api/v1/finance/tuition/{id}` | GET /api/v1/finance/tuitions/{id} |
| POST | `/api/v1/finance/tuition` | POST /api/v1/finance/tuitions |

Alias dùng cùng body, quyền và response mới; không giữ hợp đồng tạo học phí bằng studentId/classId/amount.
Không có PUT/PATCH/DELETE Payment hoặc API đặt Tuition thành PAID thủ công.

## 4. Teaching — 28 API

### Bài kiểm tra — 6

| Method | Đường dẫn | Quyền | Input / ghi chú |
|---|---|---|---|
| GET | `/api/v1/teaching/exams` | SA/A/T | Query: page, size, branchId, classId. T bắt buộc chọn classId của mình. |
| GET | `/api/v1/teaching/exams/{id}` | SA/A/T | Chi tiết bài kiểm tra; kiểm tra quyền với academic. |
| GET | `/api/v1/teaching/exams/{id}/roster` | SA/A/T | Danh sách theo examDate hoặc snapshot đã chốt; metadata frozen/frozenAt/frozenByName. |
| POST | `/api/v1/teaching/exams` | SA/A/T | Body: ExamRequest. |
| PUT | `/api/v1/teaching/exams/{id}` | SA/A/T | Body: ExamRequest; không đổi lớp; đã có điểm/chốt roster thì không đổi examDate/maxScore/weight. |
| DELETE | `/api/v1/teaching/exams/{id}` | SA/A/T | Chặn nếu đã có điểm; thành công 204. |

### Điểm và đánh giá — 4

| Method | Đường dẫn | Quyền | Input / ghi chú |
|---|---|---|---|
| GET | `/api/v1/teaching/grades` | SA/A/T | Query: examId bắt buộc; page, size tùy chọn. |
| PUT | `/api/v1/teaching/grades` | SA/A/T | Body: GradeRequest. T nhập/cập nhật; SA/A chỉ sửa điểm đã có, kèm reason. |
| GET | `/api/v1/teaching/grades/{id}/history` | SA/A/T | Mảng lịch sử thay đổi điểm; kiểm tra quyền lớp và chi nhánh. |
| GET | `/api/v1/teaching/reports/evaluations` | SA/A | Query bắt buộc: classId. Trả đánh giá tạm theo enrollment/policy, states/reasons/counts; passRate nullable, không mặc định50%. |

### Tài liệu — 5

| Method | Đường dẫn | Quyền | Input / ghi chú |
|---|---|---|---|
| GET | `/api/v1/teaching/documents` | SA/A/S/T | Query: page, size, branchId, classId, courseId, status. |
| GET | `/api/v1/teaching/documents/{id}` | SA/A/S/T | Metadata; cùng điều kiện hiển thị với danh sách. |
| GET | `/api/v1/teaching/documents/{id}/download` | SA/A/S/T | File dạng attachment; cùng điều kiện hiển thị với metadata. |
| POST | `/api/v1/teaching/documents` | SA/A/T | multipart/form-data: file, title, description?, classId hoặc courseId, branchId?. |
| PATCH | `/api/v1/teaching/documents/{id}/review` | SA/A | Body: {status, reason?}; chỉ duyệt PENDING, từ chối phải có reason. |

SA xem toàn chuỗi; A xem trong chi nhánh; S chỉ APPROVED; T chỉ tài liệu mình đăng.
T đăng theo khóa học phải có lớp đang được phân công thuộc khóa đó.

### G10 — quy tắc và kết quả tạm — 4 API

| Method | Đường dẫn | Quyền / input |
|---|---|---|
| GET | `/api/v1/teaching/classes/{classId}/evaluation-policy` | SA/A/T lớp chính; UNCONFIGURED/CONFIRMED/STALE_CLASS_CONTEXT, revision,canManage,policy. |
| PUT | `/api/v1/teaching/classes/{classId}/evaluation-policy` | SA/A; expectedRevision,passPercent,minAttendancePercent,exams,reason. |
| GET | `/api/v1/teaching/classes/{classId}/evaluation-policy/history` | SA/A/T lớp chính; các revision bất biến. |
| GET | `/api/v1/teaching/classes/{classId}/evaluation-preview` | SA/A/T lớp chính; kết quả tạm theo enrollment. |

GET reports/evaluations hiện có vẫn chỉ SA/A, cùng hợp đồng kết quả tạm mới. Xem [hợp đồng](api-contracts.md), [G10](thesis-progress/G10.md).

## 5. Endpoint chẩn đoán trực tiếp

Hai endpoint sau không nằm trong 161 API nghiệp vụ và không được gateway định tuyến:

| URL trực tiếp | Quyền | Ý nghĩa |
|---|---|---|
| GET http://localhost:8084/ | Công khai | Thông tin finance-service, status=UP. |
| GET http://localhost:8083/ | SA/A/S/T | Thông tin teaching-service, status=UP. |

Endpoint chẩn đoán trực tiếp chỉ trả thông tin service và trạng thái status=UP tối thiểu. Để kiểm tra sức khỏe đầy đủ kèm database connectivity, sử dụng các Actuator Health endpoints tại cổng từng service.

## 6. Những API chưa có

- Student Portal/tài khoản học viên, VNPay/payment gateway tích hợp trực tiếp, refresh token, khôi phục mật khẩu tự động qua email/OTP.
- GET /auth/accounts/{id}, cập nhật hồ sơ tài khoản ngoài các trường hồ sơ nhân sự đã hỗ trợ. Phân công lại Lead hiện đã có PUT /api/v1/academic/leads/{id}/assignment.
- Chuyển lớp xuyên khóa học hoặc xuyên chi nhánh tự động (chuyển lớp cùng khóa/chi nhánh đã hỗ trợ; chuyển khác khóa cần điều chỉnh học phí thủ công). Nghiệp vụ hoàn tiền (REFUND) và hủy phiếu (VOID) đã được hỗ trợ chính thức qua `POST /api/v1/finance/payments/{id}/corrections`.
- Expense, Invoice, Promotion, Facility.
- CRUD phòng/lịch/điểm danh dưới teaching: dùng các API academic tương ứng.
- Swagger/OpenAPI UI và Postman collection được đồng bộ tự động chưa được tích hợp.
  Collection academic cũ trong repo chỉ để tham khảo; danh sách này phản ánh controller hiện tại.

Chính sách mật khẩu/phiên và migration V002: [Cải thiện giai đoạn 2](improvements/phase-two-account-security.md).

## API bổ sung và hợp đồng giai đoạn 6


Base lead: /api/v1/academic/leads; tài liệu: /api/v1/teaching/documents.

| Method | Đường dẫn | Quyền / body |
|---|---|---|
| GET | /leads/assignees?branchId= | SA/A; danh sách người được giao |
| GET | /leads/{id}/history | SA/A/S cùng phạm vi |
| POST | /leads/{id}/interactions | SA/A/S phụ trách; channel, note, status, nextContactAt, version |
| PUT | /leads/{id}/assignment | SA/A; accountId, reason, version |
| GET | /documents/{id}/history | SA/A hoặc Teacher sở hữu |
| GET | /documents/{id}/revisions/{revision}/download | Như quyền lịch sử |
| POST | /documents/{id}/resubmit | Multipart title, description?, reason, version, file?; chủ sở hữu |

PUT lead thêm version; PATCH status cần status, note, version. POST /leads/{id}/convert bắt buộc JSON {"version":0}. PATCH review tài liệu thêm version cùng status/reason. nextContactAt null trong interaction xóa lịch hẹn; lịch mới phải ở tương lai. Lưu version mới từ response, không tái sử dụng version cũ cho thao tác khác. Khi mất phản hồi mạng sau ghi, tải lại lịch sử trước khi thử lại.


## Health vận hành (không tính vào 173 API nghiệp vụ)

Cả năm tiến trình có GET `/actuator/health`, `/actuator/health/liveness`, `/actuator/health/readiness` trực tiếp tại port service. Chỉ trả status; env/mappings/shutdown không expose. Reverse proxy mẫu không đưa Actuator ra ngoài. Readiness gateway chỉ kiểm tra chính gateway; bốn service có kiểm tra DB. [Vận hành](improvements/phase-seven-deployment.md).

## G02–G03 — quản trị hồ sơ và vòng đời giáo viên (07/11/2026)

[Lịch sử G02–G03: 161 API chính; từ G11–G12 đến nay: 173 API chính (Auth 19, Academic 114, Teaching 28, Finance 12)]. Giữ 3 alias học phí và các endpoint diagnostics/health ngoài số chính.

| Method | Path | Quyền và ý nghĩa |
|---|---|---|
| GET | /api/v1/auth/teacher-profiles/{id} | SA/A có giao phạm vi; id là teacher profileId; gồm canManage/version, thấy cả inactive |
| PUT | /api/v1/auth/teacher-profiles/{id} | SA hoặc A quản lý toàn bộ phân công đang hoạt động; cập nhật nhóm trường hồ sơ |
| GET | /api/v1/auth/teacher-profiles/{id}/history | Như quyền sửa; before/after text snapshot, actor/thời gian/action/version/reason |
| GET | /api/v1/auth/teacher-profiles/{id}/responsibilities | Như quyền sửa; CLASS hoặc SESSION, kể cả buổi quá ngày chưa xử lý |
| PATCH | /api/v1/auth/teacher-profiles/{id}/work-status | Như quyền sửa; {version,workStatus,reason}; INACTIVE chặn khi còn trách nhiệm |
| GET | /api/v1/academic/teachers/{id}/responsibilities?branchId= | SA/A; Admin phải chỉ định branch thuộc phân công; SA có thể bỏ branchId để rà cả legacy ngoài phân công |

GET teacher-profiles trả profileId/accountId/username/accountActive/branchIds/canManage/version cùng fullName/email/phone/specialization/qualification/experience/certificate/hireDate/workStatus. AccountId không thay cho profileId. Teacher không phân công chỉ SA sửa; partial Admin có thể xem profile nhưng canManage=false và không đọc lịch sử/bàn giao.

PUT body ví dụ (version lấy từ GET mới nhất):

```json
{"version":0,"fullName":"Nguyễn Văn A","email":"teacher@example.test","phone":"0901234567","specialization":"IELTS","qualification":"Thạc sĩ","experience":"3 năm","certificate":"Chứng chỉ giảng dạy","hireDate":null}
```

Các trường ngoài DTO không cấp quyền đổi role/account status/work status/branches. Work status đổi qua PATCH riêng:

```json
{"version":1,"workStatus":"INACTIVE","reason":"Đã bàn giao toàn bộ lớp và buổi chưa xử lý"}
```

workStatus chỉ ACTIVE/INACTIVE; reason 3–500 ký tự, version bắt buộc >=0. INACTIVE không khóa đăng nhập; ACTIVE không mở account đã khóa. Security lock hiện có vẫn thực hiện được khi còn lớp cần bàn giao. No-op cùng dữ liệu không tạo event thừa. 409 khi stale hoặc còn trách nhiệm, 403 khi thiếu quyền, 404 khi ngoài phạm vi theo convention hiện có, 503 khi không kiểm tra được Academic.

Responsibility gồm kind/id/branchId/classId/classCode/label/date/status/version; version của CLASS null vì dùng cơ chế class hiện có, SESSION dùng version buổi. Danh sách không giới hạn theo cửa sổ lịch mặc định; UI cho bàn giao từng mục qua ClassForm/SessionEditor và tải lại. Lịch sử đã hoàn tất không bị bulk rewrite. Member revocation dùng cùng nguồn kiểm tra.

Tìm account keyword hỗ trợ username hoặc họ tên Teacher. Teacher tự sửa /me/profile vẫn chỉ email/phone nhưng tăng Teacher.version và ghi CONTACT_UPDATED. Endpoint /auth/teachers giữ là danh sách/chi tiết khả dụng tối thiểu cho xếp lịch.

Áp dụng V007 trước Auth mới; nâng Academic trước hoặc cùng Auth để có endpoint kiểm tra mới. Cơ chế race phối hợp account-row lock của shared database, không phải distributed transaction. [Bàn giao G02](thesis-progress/G02.md), [G03](thesis-progress/G03.md).

## G11–G12 — kết quả cuối khóa và hồ sơ học tập (08/11/2026)

Tổng hiện tại **173 API nghiệp vụ**: Auth19, Academic114, Teaching28, Finance12; không tính 3 alias cũ, 2 diagnostics và health. G11/G12 thêm 6 Academic và 6 Teaching. SA/A/S/T vẫn chịu phạm vi chi nhánh; T chỉ lớp chính.

| Method | Đường dẫn | Quyền | Hợp đồng |
|---|---|---|---|
| GET | /api/v1/academic/classes/{id}/result-source | SA/A/S/T lớp mình | Metadata token/sourceRevision, phục vụ điều phối; không khóa từ HTTP đọc. |
| GET | /api/v1/academic/students/{id}/learning-profile | SA/A/S | Student + studies(enrollment,periods,recordedAttendance,resultVerification); Teacher403. Counts bản ghi không phải tỷ lệ chuyên cần đã xác minh. |
| GET | /api/v1/academic/classes/{id}/closure | SA/A/S/T lớp mình | revision,closed,verification,detail,history; LEGACY_UNVERIFIED hoặc NEEDS_REVIEW được giữ rõ. |
| POST | /api/v1/academic/classes/{id}/close | SA/A | requestId UUID,expectedRevision,reason; đủ phút, hết SCHEDULED/STUDYING/RESERVED và mọi COMPLETED khớp kết quả hiện hành. |
| POST | /api/v1/academic/classes/{id}/reopen | SA/A | Cùng body; phải mở lại kết quả toàn lớp trước; không mở enrollment terminal. |
| POST | /api/v1/academic/sessions/{id}/correct-time | SA/A | startTime,endTime HH:mm,version,reason; chỉ COMPLETED; lưu audit, khóa nguồn sau chốt. |
| GET | /api/v1/teaching/classes/{classId}/final-results | SA/A/S/T lớp mình | classId,resultRevision,results tối thiểu; không điểm/snapshot; thiếu entry là chưa chốt, không PASS giả. |
| POST | /api/v1/teaching/classes/{classId}/final-results/{enrollmentId}/preview | SA/A | Không body; evaluation,policy,duration,fingerprint,blockers,current,resultRevision. Không đủ điều kiện vẫn trả preview200 có blockers. |
| POST | /api/v1/teaching/classes/{classId}/final-results/{enrollmentId}/confirm | SA/A | requestId UUID,fingerprint,reason; trả Summary;409 nếu nguồn đổi/thiếu điều kiện/đã có current. |
| POST | /api/v1/teaching/classes/{classId}/final-results/reopen | SA/A | requestId UUID,expectedRevision=resultRevision,reason; mở toàn bộ current của lớp, giữ lịch sử. |
| GET | /api/v1/teaching/classes/{classId}/final-results/{enrollmentId}/history | SA/A/T lớp mình | Danh sách phiên bản và snapshot immutable; Staff403. |
| GET | /api/v1/teaching/classes/{classId}/final-results/requests/{requestId} | SA/A/S/T lớp mình | Tra cứu intent đã commit;404 nếu chưa thấy; trả trạng thái hiện tại kể cả REOPENED. |

PATCH /academic/enrollments/{id}/status nhận thêm finalResultId/finalResultRevision khi COMPLETED; kể cả cùng COMPLETED phải tham chiếu đúng current CONFIRMED. PASS hoặc FAIL đều được kết thúc quá trình học. INSUFFICIENT, nguồn sai, refs cũ, dependency lỗi đều chặn. Kết quả trả thêm completionResultId/completionResultRevision. Học phí/billing root không thay đổi. PATCH /classes/{id}/status không còn nhận FINISHED hoặc mở lại lớp đã đóng; dùng hai POST có lý do ở trên.

reason 3–500 ký tự. expectedRevision/version nguyên >=0. requestId cùng intent trả cùng kết quả, payload khác409; replay chốt cũ có thể trả state REOPENED nên luôn đọc state. Giữ cùng UUID/body khi retry, không tự tạo intent mới sau mất phản hồi. Phục hồi FE dùng requests/{requestId}; đồng bộ Academic là bước riêng, không transaction phân tán. [G11](thesis-progress/G11.md), [G12](thesis-progress/G12.md), [hướng dẫn](../deployment/learning-results-user-guide.md).


## Student App G3 — 11/11/2026

Đã triển khai6endpoint:5Auth quản lý tài khoản và1Academic hồ sơ cá nhân; tổng179API chính. STUDENT dùng app riêng, không cấp quyền vào API danh sách quản trị. Quyền SA/A/S/T của web giữ nguyên.

[Hợp đồng API thực tế và thay đổi](student-app/phase-3-completion.md#api-triển-khai-mới--6-endpoint) · [Migration/cách cấp tài khoản](student-app/account-user-guide.md). Dùng lại login,me,me/password,me/logout-all cho STUDENT. Student chỉ đọc academic/student/me/profile sau khi đổi mật khẩu tạm; không dùng studentId trong query để đổi chủ hồ sơ. Các API lịch/điểm/học phí/thông báo của Student trong draft G1 chưa triển khai.


## Student App G4 — 11/11/2026

Thêm5GET trong Academic: `/student/me/enrollments`, `/student/me/enrollments/{id}`, `/student/me/schedule`, `/student/me/sessions/{id}`, `/student/me/next-session` (đều dưới `/api/v1/academic`). Chỉ STUDENT đã đổi mật khẩu, chỉ dữ liệu chính mình. Tổng184API chính. [Hợp đồng và quy tắc thời kỳ/legacy](student-app/phase-4-completion.md). Nhóm API Student trước đó được giữ nguyên.



## Student App G5 — điểm và chuyên cần

Thêm8endpoint theo [hợp đồng, quyền, DTO và hướng dẫn V015](student-app/phase-5-completion.md#api-thực-hiện). `PUT /teaching/grades` nhận thêm `studentComment` độc lập với `comment` nội bộ. Chỉ snapshot đã công bố được trả cho học viên.


## Student App G6 — tài liệu và hỗ trợ

15API mới theo [bảng endpoint, DTO và quyền](student-app/phase-6-completion.md#api-đã-triển-khai). `DocumentDto` quản trị có thêm sharedToStudents/sharedAt. Không bật chia sẻ tài liệu cũ; student projection riêng, không trả entity/note nội bộ.


## Student App G7 — tài chính cá nhân

6API đọc mới theo [hợp đồng, ownership và phép tính](student-app/phase-7-completion.md#api-g7-đã-thực-hiện). Support G6 nhận thêm TUITION/RECEIPT với Finance proof. Không có API student ghi tiền, sửa học phí hoặc xác nhận đã thanh toán.


## Student App G8 — VNPAY sandbox

12 API mới hiện thực: [bảng method/path/quyền và DTO G8](student-app/phase-8-completion.md#api-đã-hiện-thực). Giao thức cục bộ đã kiểm chứng; VNPAY sandbox thật chưa nghiệm thu do chưa có merchant/HTTPS. Các endpoint đều có prefix `/api/v1/finance`.

| Method | Path | Quyền |
|---|---|---|
| GET | `/student/me/payment-config` | STUDENT |
| POST | `/student/me/payment-orders` | STUDENT |
| GET | `/student/me/payment-orders` | STUDENT |
| GET | `/student/me/payment-orders/{id}` | STUDENT, đúng chủ |
| GET | `/student/me/payment-orders/requests/{requestId}` | STUDENT, đúng chủ |
| GET | `/payment-orders` | SA/A/S, chi nhánh |
| GET | `/payment-orders/{id}` | SA/A/S, chi nhánh |
| POST | `/payment-orders/{id}/reconcile` | SA/A, lý do/UUID/version |
| GET | `/payment-orders/reconciliations/{requestId}` | SA/A |
| GET | `/provider-events` | SA/A/S, chi nhánh; unknown chỉ SA |
| GET | `/payment-providers/vnpay/ipn` | Public, HMAC-SHA512 |
| GET | `/payment-providers/vnpay/return` | Public, không ghi phiếu |


## Student App G9 — thông báo, tùy chọn và thiết bị

8API mới; [DTO, quyền và cấu hình](student-app/phase-9-completion.md#api-g9-thực-tế). Chỉ STUDENT đã kích hoạt và đúng chủ; push thật chưa nghiệm thu.

| Method | Path đầy đủ | Kết quả |
|---|---|---|
| GET | `/api/v1/academic/student/me/notifications` | page,size,unread?,category? |
| GET | `/api/v1/academic/student/me/notifications/unread-count` | count toàn tài khoản |
| GET | `/api/v1/academic/student/me/notifications/{id}` | Chi tiết đúng chủ |
| PUT | `/api/v1/academic/student/me/notifications/{id}/read` | 204 idempotent |
| GET | `/api/v1/academic/student/me/notification-preferences` | Tùy chọn,version,pushAvailable |
| PUT | `/api/v1/academic/student/me/notification-preferences` | Tùy chọn +version,409 khi stale |
| PUT | `/api/v1/auth/student/me/devices/{installationId}` | Token/platform/projectId, trả binding không chứa token |
| DELETE | `/api/v1/auth/student/me/devices/{installationId}` | 204, binding hiện thuộc account |


## Tối ưu Học phí và Thanh toán — giai đoạn 1–4

[Hợp đồng và kết quả kiểm thử](student-app/payment-flow-phases-1-4-completion.md). Chỉ STUDENT đúng chủ; các API cũ vẫn được giữ.

| Method | Path đầy đủ | Kết quả |
|---|---|---|
| GET | `/api/v1/finance/student/me/tuitions/{id}/payment-context` | Hành động PAY/RESUME/SUPPORT/NONE/UNAVAILABLE, số dư, đơn tóm tắt |
| GET | `/api/v1/finance/student/me/payment-contexts?tuitionIds=13,14` | Batch tối đa 20 id đã kiểm ownership |

Mở rộng GET tuitions bằng `status=DUE`; GET payment-orders bằng `summary=true` để bỏ URL ký khỏi danh sách. Lỗi POST tạo đơn có thêm code/requestRejected/orderId thuộc chủ khi phù hợp; giữ message cho client cũ.


## Bổ sung W5 — 13/11/2026

| Method | API | Quyền | Mục đích |
|---|---|---|---|
| GET | `/api/v1/teaching/students/{studentId}/enrollments/{enrollmentId}/published-grades?page=0&size=20` | SUPER_ADMIN, ADMIN, STAFF; kiểm học viên/đăng ký/chi nhánh | Chỉ xem điểm snapshot đã công bố hiện hành của đăng ký, không điểm nháp/nhận xét nội bộ |

Các filter/metadata mới của tài chính và hỗ trợ: [Bàn giao W3–W5](improvements/web-w3-w4-w5-completion.md). Không thay đường dẫn endpoint cũ.

## Cập nhật W7 — Hàng đợi và phạm vi đa chi nhánh (13/11/2026)

GET `/api/v1/academic/stats/work-queue` trả `{supportActive, leadsNew, businessDate, observedAt}`; GET `/api/v1/finance/stats/work-queue` trả `{tuitionOverdue, ordersReview, businessDate, observedAt}`. Cả hai yêu cầu SUPER_ADMIN/ADMIN/STAFF; query `branchId` tùy chọn và `allAssigned=false` mặc định. Đây là số đếm server theo toàn bộ tập hợp đủ quyền, trước phân trang.

`allAssigned=true`: ADMIN xem tập chi nhánh được phân công hiện hành (tập rỗng trả số 0/danh sách rỗng); SUPER_ADMIN xem toàn hệ thống. STAFF/TEACHER/STUDENT không được dùng chế độ này. Không kết hợp `branchId` và `allAssigned=true`; chi nhánh ngoài phân công bị từ chối. Không bật tổng hợp thì giữ cách chọn chi nhánh làm việc hiện tại.

Tham số tùy chọn tương thích ngược này cũng áp dụng GET quản lý `/api/v1/academic/sessions`, `/api/v1/academic/leads`, `/api/v1/academic/support-requests`, `/api/v1/finance/tuitions`, `/api/v1/finance/payment-orders`. Các kiểm tra quyền endpoint và quyền nghiệp vụ cũ vẫn áp dụng; không thay đổi endpoint student.

Hỗ trợ đang xử lý = OPEN/IN_PROGRESS; tư vấn mới = NEW; học phí quá hạn phải chưa hủy, hạn trước ngày nghiệp vụ và còn nợ; đơn cần rà soát = REVIEW_REQUIRED hoặc có reviewCode. UI dùng cùng điều kiện cho link đích và số đếm. `observedAt` là thời điểm server quan sát dữ liệu, không phải giờ render trên trình duyệt. Lỗi tải không tương đương số 0.

## C2 — Truy vấn trợ lý chỉ đọc (13/11/2026)

13 GET mới; [hợp đồng, projection và giới hạn đầy đủ](assistant/c2-completion.md). Đây là API backend chuẩn bị cho MCP, chưa có chat runtime.

| Method | Đường dẫn | Quyền |
|---|---|---|
| GET | /api/v1/academic/assistant-query/courses | SA/A/S/T |
| GET | /api/v1/academic/assistant-query/classes | SA/A/S/T; T lớp chính mình |
| GET | /api/v1/academic/assistant-query/students | SA/A/S; T chỉ roster lớp chính mình |
| GET | /api/v1/academic/assistant-query/student-classes | SA/A/S; T chỉ lớp chính mình |
| GET | /api/v1/academic/assistant-query/student-statistics | SA/A/S |
| GET | /api/v1/academic/assistant-query/sessions | SA/A/S/T; T actual teacher |
| GET | /api/v1/academic/assistant-query/attendance | SA/A/S/T theo quyền buổi |
| GET | /api/v1/academic/assistant-query/priority-leads | SA/A/S; S chỉ assignedTo chính mình |
| GET | /api/v1/academic/assistant-query/usage-help | SA/A/S/T; catalog cố định |
| GET | /api/v1/finance/assistant-query/tuitions | SA/A/S |
| GET | /api/v1/finance/assistant-query/receipts | SA/A/S |
| GET | /api/v1/teaching/assistant-query/current-grades | SA/A/T theo lớp chính |
| GET | /api/v1/teaching/assistant-query/published-grades | SA/A/S theo hồ sơ/đăng ký |

Scope mặc định ACTIVE_BRANCH, ALL_ASSIGNED chỉ A, WHOLE_SYSTEM chỉ SA; aggregate không kết hợp branchId. page=0,size=5, tối đa 20. asOf chỉ hôm nay ở C2; lịch từ 1–31 ngày. Không có mutation hoặc SQL tùy ý. API courses cũ thêm minTuition/maxTuition/maxInclusive=true/allAssigned=false; payments cũ thêm allAssigned=false; mặc định/hợp đồng cũ giữ nguyên.
