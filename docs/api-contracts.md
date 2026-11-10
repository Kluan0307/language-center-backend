# Hợp đồng API và body mẫu

Đối chiếu và chuẩn hóa theo mã nguồn thực tế ngày **10/11/2026** (G16 đối chiếu lại). Toàn bộ method/path/query/quyền nằm ở [danh sách API](api-list.md).
Các path rút gọn dưới đây phải ghép với prefix của service tương ứng.

## 1. Quy ước chung

| Mục | Hợp đồng |
|---|---|
| Base URL frontend | `http://localhost:8080` |
| Header xác thực | `Authorization: Bearer <token>`; login không cần |
| JSON request | `Content-Type: application/json` |
| File upload | `multipart/form-data`; để client tự đặt boundary |
| Ngày | `yyyy-MM-dd` |
| Giờ lịch học | `HH:mm`, ví dụ `18:00` |
| Ngày giờ trả về | ISO LocalDateTime, ví dụ `2026-11-02T09:00:00`; nghiệp vụ dùng múi giờ Việt Nam |
| Phân trang | page=0, size=20 mặc định; size 1–200; chưa có query sort |
| Thành công | GET/POST/PUT/PATCH nghiệp vụ hiện trả 200; các DELETE được triển khai trả 204 |
| Chi nhánh | A/S/T dùng phân công hiện tại từ Auth, header X-Branch-Id chọn phạm vi; mặc định chi nhánh chính. SA chọn branchId khi tạo dữ liệu theo chi nhánh |

GET danh sách có phân trang trả:

```json
{
  "content": [],
  "page": 0,
  "size": 20,
  "totalElements": 0,
  "totalPages": 0
}
```

Các API chọn giáo viên, danh sách học viên/lịch của một lớp, lịch sử và tổng hợp điểm danh,
lịch sử sửa điểm trả mảng; sheet/báo cáo trả object riêng.

Lỗi nghiệp vụ thường trả `{"message":"..."}`; validation DTO trả map field → thông báo.
Các mã thường gặp: 400 dữ liệu/nghiệp vụ sai, 401 chưa đăng nhập/token sai, 403 thiếu quyền,
404 không tìm thấy hoặc ngoài phạm vi, 409 trùng/xung đột, 503 service phụ thuộc không sẵn sàng.
Upload quá giới hạn ở tầng multipart có thể trả 413. Lỗi framework không nhất thiết dùng cùng body nghiệp vụ.

**Các ID và ngày trong ví dụ chỉ minh họa**; thay bằng ID lấy từ response và thời gian thực tế.
Request PUT dùng đầy đủ Request DTO, không phải JSON Patch.

## 2. Auth — /api/v1/auth

### Login

POST `/login`:

```json
{"username":"<ten-dang-nhap>","password":"<mat-khau>"}
```

Response:

```json
{
  "token": "<jwt>",
  "accountId": 10,
  "profileId": 3,
  "branchId": 1,
  "username": "teacher01",
  "role": "TEACHER",
  "fullName": "Nguyễn Văn A"
}
```

Login và GET /me có thêm `branchIds: number[]`; branchId là chi nhánh chính. SA có branchId=null. Dùng `profileId` làm teacherId khi chọn giáo viên cho lớp.
Login và GET /me có mustChangePassword; UserResponse có passwordChangedAt. `GET /me` không trả password.

### CreateUserRequest

POST `/accounts`:

```json
{
  "username": "teacher01",
  "password": "<mat-khau-moi>",
  "role": "TEACHER",
  "fullName": "Nguyễn Văn A",
  "email": "teacher01@example.com",
  "phone": "0901234567",
  "branchId": 1,
  "hireDate": "2026-11-02",
  "workStatus": "ACTIVE",
  "specialization": "IELTS",
  "qualification": "Cử nhân",
  "experience": "3 năm",
  "certificate": "TESOL"
}
```

Bắt buộc: username, password, role, fullName.
Username dùng 3–50 ký tự chữ/số hoặc `_.@-`; password 8–72 ký tự và không quá 72 byte UTF-8.
fullName/email tối đa 100 ký tự, phone tối đa 15. Email nếu có phải đúng định dạng.
Các trường hồ sơ còn lại tùy chọn; Staff có thêm position.

SA chọn chi nhánh hoạt động; A chọn chi nhánh được phân công qua body/header, thiếu thì dùng chi nhánh chính.
API này không tạo SA; dùng bootstrap cho database tài khoản rỗng. Tài khoản mới có mustChangePassword=true và phải đổi mật khẩu trước khi thao tác nghiệp vụ.
Danh sách giáo viên chỉ trả tài khoản bật và workStatus=ACTIVE, mỗi phần tử:
`{profileId,accountId,fullName,branchId,branchIds}`.

## 3. Academic — /api/v1/academic

### BranchRequest

Dùng cho POST/PUT `/branches`:

```json
{"code":"CN03","name":"Chi nhánh Quận 3","address":"TP.HCM","phone":"0901234567"}
```

Bắt buộc code, name, phone. Không gửi branchId khi tạo Branch.
Đổi trạng thái bằng PATCH `/branches/{id}/toggle-status`, không body.

### CourseRequest

Dùng cho POST/PUT `/courses`:

```json
{
  "code": "ENG-A1",
  "name": "Tiếng Anh A1",
  "type": "GENERAL",
  "level": "A1",
  "duration": 24,
  "tuitionFee": 2500000,
  "content": "Nội dung khóa học",
  "entryRequirement": "Đầu vào A0",
  "maxStudents": 20,
  "branchId": 1
}
```

Bắt buộc code, name, duration>0, tuitionFee>0, maxStudents>0.
SA tạo khóa chung bằng branchId=null; chỉ SA sửa khóa chung.
S không tạo khóa học; PUT khóa của chi nhánh phải giữ nguyên tuitionFee.
PATCH `/courses/{id}/status`: `{"status":"CLOSED"}`.

### RoomRequest

Dùng cho POST/PUT `/rooms`:

```json
{
  "code": "P101",
  "name": "Phòng 101",
  "location": "Tầng 1",
  "capacity": 25,
  "equipment": "Máy chiếu",
  "branchId": 1
}
```

Bắt buộc code, name, capacity>0; SA chọn branchId khi tạo.
Không giảm sức chứa dưới maxSize của lớp đã gán.
PATCH `/rooms/{id}/status`: `{"status":"MAINTENANCE"}`.

### StudentRequest

Dùng cho POST/PUT `/students`:

```json
{
  "fullName": "Nguyễn Minh An",
  "dateOfBirth": "2015-05-20",
  "gender": "MALE",
  "email": "parent@example.com",
  "phone": "0901234567",
  "address": "TP.HCM",
  "contactPerson": "Nguyễn Văn Bình",
  "guardianName": "Nguyễn Văn Bình",
  "guardianPhone": "0901234567",
  "currentLevel": "A1",
  "branchId": 1
}
```

Bắt buộc fullName, phone; dateOfBirth không ở tương lai.
studentCode do server sinh, không gửi mã qua request. Phone được phép trùng.
contactPerson được giữ để tương thích; guardianName/guardianPhone là thông tin phụ huynh riêng.
PATCH trạng thái: `{"status":"PAUSED","reason":"Tạm dừng theo yêu cầu","version":0}`.
PAUSED/GRADUATED bị từ chối khi còn đăng ký STUDYING; GRADUATED còn chặn đăng ký RESERVED; cần bảo lưu/kết thúc từng đăng ký trước.
Hồ sơ phải STUDYING để ghi danh, học lại hoặc chuyển lớp. Tìm kiếm học viên có hỗ trợ studentCode.
GET chi tiết trả `{student,enrollments}`, không phải StudentDto phẳng.

### LeadRequest và chuyển đổi

POST/PUT `/leads`:

```json
{
  "fullName": "Trần Minh Anh",
  "phone": "0901234568",
  "email": "anh@example.com",
  "source": "FACEBOOK",
  "interestedCourseId": 1,
  "desiredLevel": "A1",
  "note": "Quan tâm lớp buổi tối",
  "branchId": 1
}
```

Bắt buộc fullName, phone. Server đặt NEW và người phụ trách từ tài khoản đang thao tác.
PUT có note sẽ nối ghi chú; không phải thao tác xóa toàn bộ ghi chú cũ.

PATCH `/leads/{id}/status`:

```json
{"status":"CONTACTED","note":"Đã gọi tư vấn"}
```

POST `/leads/{id}/convert`:

```json
{"version":0}
```

Bắt buộc gửi JSON body chứa `version` hiện tại của Lead để kiểm soát cập nhật lạc quan (Optimistic Concurrency Control). Chỉ Lead ở trạng thái CONTACTED hoặc CONSULTING mới được phép chuyển đổi.
Response là LeadDto, có `convertedStudent`; lấy `convertedStudent.id` để đăng ký lớp.
Lead CONVERTED không được sửa. Chuyển đổi tạo Student và cập nhật Lead trong cùng transaction.
Gọi convert lại cho Lead đã chuyển đổi trả cùng convertedStudent, không tạo hồ sơ lần hai.

### ClassRequest

POST `/classes`:

```json
{
  "code": "A1-001",
  "name": "Tiếng Anh A1 buổi tối",
  "courseId": 1,
  "teacherId": 3,
  "teacherName": "Nguyễn Văn A",
  "roomId": 1,
  "startDate": "2026-11-02",
  "endDate": "2027-02-28",
  "maxSize": 20,
  "branchId": 1,
  "schedules": [
    {"dayOfWeek":"MONDAY","startTime":"18:00","endTime":"19:30"}
  ]
}
```

Bắt buộc code, name, courseId, teacherId, teacherName, roomId, startDate, endDate, maxSize>0.
teacherName vẫn là trường bắt buộc trong DTO hiện tại, nhưng tên lưu thực tế được lấy lại từ auth.
teacherId là profileId của giáo viên hoạt động cùng chi nhánh.
maxSize phải nằm trong sức chứa phòng/giới hạn khóa và không nhỏ hơn sĩ số hiện tại.

schedules là tùy chọn khi tạo; S phải bỏ trường này hoặc gửi mảng rỗng.
**PUT lớp không nhận schedules, kể cả []**; sửa lịch qua API schedules.
S sửa lớp phải giữ teacherId, roomId, courseId, startDate, endDate.
GET chi tiết trả `{schoolClass,schedules}`.
Response lớp có `full`; trạng thái trả về không dùng FULL làm tiến độ.

PATCH `/classes/{id}/status`: `{"status":"FINISHED"}`.
Chỉ A/SA thực hiện; UPCOMING/ONGOING nếu gửi phải phù hợp ngày lớp.

### ScheduleRequest và ConflictRequest

POST/PUT `/schedules`:

```json
{"classId":1,"dayOfWeek":"MONDAY","startTime":"18:00","endTime":"19:30"}
```

Bắt buộc đủ bốn trường. endTime phải sau startTime; một lớp tối đa một slot/ngày trong tuần.
PUT không chuyển lịch sang classId khác. Kiểm tra phòng, giáo viên và học viên bị trùng lịch.

POST `/schedules/check-conflict` cho lớp đã lưu:

```json
{"classId":1,"excludeScheduleId":2,"dayOfWeek":"MONDAY","startTime":"18:00","endTime":"19:30"}
```

excludeScheduleId là tùy chọn khi kiểm tra sửa lịch. Cho lớp chưa lưu:

```json
{
  "branchId": 1,
  "teacherId": 3,
  "roomId": 1,
  "startDate": "2026-11-02",
  "endDate": "2027-02-28",
  "dayOfWeek": "MONDAY",
  "startTime": "18:00",
  "endTime": "19:30"
}
```

Kết quả là mảng conflict; rỗng là chưa thấy xung đột theo dữ liệu hiện tại.
Preview không lưu; thao tác tạo/sửa vẫn kiểm tra lại trong transaction.

### Enrollment

POST `/enrollments`:

```json
{"studentId":1,"classId":1}
```

Học viên và lớp cùng chi nhánh, lớp chưa kết thúc/còn chỗ và không trùng lịch.
Server chặn tranh chỗ bằng khóa giao dịch và tự cập nhật sĩ số.

PATCH `/enrollments/{id}/status`: `{"studyStatus":"RESERVED","reason":"Nghỉ điều trị","version":0,"expectedReturnDate":"2026-12-01"}`.
Các chuyển trạng thái: STUDYING → DROPPED/COMPLETED/RESERVED; RESERVED → STUDYING/DROPPED.
Không ghi danh mới vào cùng lớp đang RESERVED; dùng học lại để giữ học phí gốc.
Khôi phục STUDYING phải kiểm tra lại chỗ/lịch. Không mở lại trạng thái kết thúc; TRANSFERRED chỉ qua API transfer.

POST `/enrollments/{id}/transfer`: `{"newClassId":2,"reason":"Học lại tại lớp khác","version":1}`.
Response là đăng ký mới. Chỉ cùng khóa/cùng chi nhánh, giữ billingEnrollmentId gốc và không tạo học phí thứ hai.

Sau commit đăng ký mới, academic thử tạo Tuition. Response POST ban đầu có thể là PENDING;
GET lại Enrollment để xem tuitionDelivery=PENDING/CREATED/FAILED và tuitionDeliveryError.
Danh sách/chi tiết đăng ký sau chuyển lớp lấy trạng thái delivery từ billingEnrollmentId gốc.
EnrollmentDto có registeredBy (accountId), registeredByName để hiển thị người ghi danh.
Finance lỗi không làm mất đăng ký. POST `/enrollments/{id}/tuition/retry` không body trả:

```json
{"enrollmentId":1,"status":"CREATED","error":null}
```

GET `/enrollments/{id}/billing` trả snapshot học phí của đăng ký gốc:
enrollmentId, branchId, studentId/studentName, classId/className, courseId/courseName, tuitionFee, dueDate.
Finance gọi API này bằng JWT của người dùng; giá không lấy từ browser.

### AttendanceBulk

GET sheet: `/attendances/sheet?classId=1&date=2026-11-02`.
Trả `{classId,className,sessionDate,records}`; mỗi row có enrollmentId, studentId,
studentName, currentLevel, status, hasPermission, saved, recordedBy, recordedByName.

POST `/attendances/bulk`:

```json
{
  "classId": 1,
  "sessionDate": "2026-11-02",
  "records": [
    {"enrollmentId":1,"status":"PRESENT","hasPermission":false},
    {"enrollmentId":2,"status":"ABSENT","hasPermission":true}
  ]
}
```

Gửi đúng và đủ enrollmentId có hiệu lực tại ngày được chọn; không lặp ID.
hasPermission mặc định false, chỉ lưu true khi ABSENT.
Không điểm danh tương lai hoặc tạo mới sheet ngoài ngày học hợp lệ.
Lịch sử tham gia dùng activeFrom bao gồm ngày đầu, activeUntil không bao gồm ngày kết thúc;
bản ghi điểm danh đã lưu luôn được giữ để xem/sửa lại dù học viên đã chuyển/nghỉ.
Row chưa lưu có saved=false; không được hiểu mặc định PRESENT là đã điểm danh.

History trả mảng theo ngày; summary trả mảng theo đăng ký, gồm cả đăng ký đã chuyển/nghỉ.
Tỷ lệ tham gia = (PRESENT + LATE) / số buổi đã lưu × 100, không có buổi thì 0.

### Stats

GET `/stats/overview?branchId=1&groupByBranch=false` trả `{overall,branches}`.
overall có studentsByStatus, studentsByLevel, newStudentsThisMonth, classesByStatus,
classOccupancyRate, leadsByStatus, leadConversionRate.
branches có dữ liệu khi groupByBranch=true trong phạm vi đã xác định; A dùng allAssigned=true để tổng hợp tất cả chi nhánh được phân công. Không kết hợp allAssigned và branchId; S không được dùng allAssigned.
Tỷ lệ lấp đầy loại lớp đã kết thúc; chuyển đổi Lead = CONVERTED / tổng Lead trong phạm vi.

## 4. Finance — /api/v1/finance

### Tạo Tuition

POST `/tuitions`:

```json
{"enrollmentId":1}
```

Lấy giá, chi nhánh và snapshot từ academic; gọi lại cùng đăng ký trả khoản học phí đã có.
Đăng ký chuyển lớp cùng khóa quy về đăng ký thanh toán gốc.
Học phí cũ chưa gắn enrollmentId phải được đối soát trước khi retry; server chặn tạo thêm khi phát hiện khoản cũ tương ứng.

TuitionDto có:
`id,enrollmentId,studentId,studentName,classId,className,courseId,courseName,
originalAmount,discountAmount,amount,paidAmount,balance,currency,dueDate,status,overdue,branchId,
adjustedBy,adjustedByName,adjustedAt,adjustmentReason,version,credit`.

- amount = originalAmount - discountAmount.
- paidAmount = tổng phiếu PAID − VOID − REFUND; balance = max(amount − paidAmount,0); credit = max(paidAmount − amount,0).
- overdue là cờ riêng, không thay thế trạng thái thanh toán.
- currency hiện được server đặt VND.

### TuitionAdjustment

PUT `/tuitions/{id}` chỉ A/SA:

```json
{
  "originalAmount": 2500000,
  "discountAmount": 200000,
  "dueDate": "2026-11-30",
  "reason": "Giảm học phí theo quyết định của quản lý",
  "version": 0,
  "requestKey": "adjustment-unique-key"
}
```

Bắt buộc đủ sáu trường; số tiền không âm, tối đa 2 chữ số thập phân.
Net không âm; nếu thấp hơn thu ròng thì tạo credit để hoàn riêng. Server lưu mọi lần điều chỉnh, giá/hạn trước sau, tác giả và lý do.
Không có API đặt trạng thái PAID hoặc hủy Tuition thủ công.

### PaymentRequest

POST `/payments`:

```json
{
  "tuitionId": 1,
  "amount": 1000000,
  "paymentMethod": "BANK_TRANSFER",
  "referenceCode": "BANK-20261102-001",
  "requestKey": "8a4588eb-1b31-469e-8cd4-789ca75ed2f9",
  "note": "Đóng đợt 1"
}
```

Bắt buộc tuitionId, amount>0, paymentMethod, requestKey. referenceCode/note tùy chọn.
Server đặt paidAt, status=PAID, branchId, receiptNo và người thu; không lấy các trường này từ client.
Phương thức thu mới chỉ CASH hoặc BANK_TRANSFER.

Mỗi ý định thu tiền có requestKey riêng; gửi lại **cùng key và cùng nội dung được đối chiếu**
để nhận lại phiếu thu. Không tạo key mới chỉ vì request trước timeout.
Đổi tuitionId/amount/paymentMethod/referenceCode/note dưới cùng key bị từ chối 409.
Khóa Tuition và tính sổ thu ròng (PAID trừ VOID/REFUND) trong cùng transaction để không thu vượt khi đồng thời.
Payment không có API sửa/xóa/đổi trạng thái.

### Doanh thu

GET `/stats/overview?branchId=1&from=2026-11-01&to=2026-11-30`, chỉ A/SA:

```json
{
  "collected": 1000000,
  "outstanding": 1300000,
  "byMonth": [{"key":"2026-11","amount":1000000}],
  "byCourse": [{"key":"1","amount":1000000}],
  "byBranch": [{"key":"1","amount":1000000}]
}
```

from/to gồm cả ngày đầu và cuối; query có thể bỏ một hoặc cả hai.
collected/byMonth/byCourse/byBranch là thu ròng trong khoảng ngày; grossCollected/voided/refunded tách các thành phần theo ngày phát sinh. outstanding và credit là số **hiện tại**.
Nhóm byMonth dùng key `YYYY-MM` và sắp theo thời gian. Từ/đến ngày bao gồm cả hai đầu;
ngày kết thúc được truy vấn đến trước 00:00 ngày kế tiếp. Không suy ra công nợ lịch sử từ outstanding.
key tháng dùng năm-tháng, không bảo đảm tháng có số 0 ở đầu; key khóa/chi nhánh là ID dạng chuỗi.
Không có bản ghi thì tổng là 0 và nhóm là mảng rỗng.

## 5. Teaching — /api/v1/teaching

### ExamRequest

POST/PUT `/exams`:

```json
{
  "classId": 1,
  "name": "Kiểm tra giữa kỳ",
  "examType": "MIDTERM",
  "examDate": "2026-11-16",
  "weight": 40,
  "maxScore": 10
}
```

Bắt buộc đủ sáu trường; examType hiện là String, không phải enum.
0 < weight ≤ 100; 0 < maxScore ≤ 10000; tối đa 2 chữ số thập phân.
T chỉ quản lý lớp được phân công; GET danh sách của T bắt buộc classId.
Đã có điểm thì không đổi maxScore/weight; không xóa bài kiểm tra đã có điểm.

### GradeRequest

PUT `/grades`:

```json
{"examId":1,"studentId":1,"score":8.5,"comment":"Hoàn thành tốt"}
```

Bắt buộc examId, studentId, score; 0 ≤ score ≤ maxScore của Exam.
T nhập/cập nhật cho học viên thuộc roster tại ngày thi của lớp được giao (kể cả người hiện đã bảo lưu/chuyển lớp); A/SA chỉ sửa điểm đã tồn tại:

```json
{"examId":1,"studentId":1,"score":9,"comment":"Đã kiểm tra lại","reason":"Sửa theo biên bản chấm phúc khảo"}
```

A/SA bắt buộc reason. enteredBy/enteredAt ban đầu không bị thay; lastModifiedBy/At và lịch sử được bổ sung.
GET `/grades/{id}/history` trả mảng:
`{oldScore,newScore,modifiedBy,modifiedByName,modifiedAt,reason}`.

GET `/reports/evaluations?classId=1`, chỉ A/SA, trả `{students,passRate}`.
Mỗi student có studentId, studentName, averagePercent, passed, gradedExams.
Điểm được chuẩn hóa theo maxScore và tính trung bình theo weight tương đối; không ép tổng weight=100.
G10 thay thế ngưỡng50% cố định bằng policy của lớp. Báo cáo theo enrollment, PASS/FAIL tạm hoặc INSUFFICIENT; chỉ PASS/FAIL vào mẫu số, không đủ dữ liệu trả passed=null. Xem hợp đồng G10 bên dưới.

### Upload và duyệt Document

POST `/documents` bằng multipart/form-data:

| Field | Bắt buộc | Ý nghĩa |
|---|---|---|
| file | Có | File không rỗng, tối đa 10 MB |
| title | Có | Tối đa 255 ký tự |
| description | Không | Tối đa 1000 ký tự |
| classId | Chọn một | Gắn lớp; không gửi cùng courseId |
| courseId | Chọn một | Gắn khóa; không gửi cùng classId |
| branchId | Khi SA đăng theo course | A/T dùng chi nhánh đang làm việc đã được phân công; đăng theo lớp suy ra chi nhánh từ lớp |

Định dạng hỗ trợ: pdf, doc, docx, xls, xlsx, ppt, pptx, png, jpg, jpeg, txt.
File lưu dưới khóa UUID riêng; metadata response không lộ storageKey hoặc đường dẫn filesystem.
T đăng theo course cần có lớp được phân công thuộc course đó. Trạng thái khởi tạo PENDING.

PATCH `/documents/{id}/review`:

```json
{"status":"APPROVED"}
```

Hoặc:

```json
{"status":"REJECTED","reason":"Cần cập nhật nội dung bài học"}
```

Chỉ A/SA duyệt tài liệu PENDING; REJECTED bắt buộc reason; không có API đưa tài liệu đã duyệt về PENDING.
S chỉ đọc/tải APPROVED; T chỉ đọc/tải tài liệu mình đăng; A theo chi nhánh, SA toàn hệ thống.
GET metadata và GET download kiểm tra cùng điều kiện hiển thị.
Download trả application/octet-stream với Content-Disposition attachment, không phải JSON hoặc public URL.

## 6. Tập trạng thái hiện tại

| Dữ liệu | Giá trị |
|---|---|
| Role | SUPER_ADMIN, ADMIN, STAFF, TEACHER |
| Branch | ACTIVE, INACTIVE |
| Course | OPEN, CLOSED |
| Room | AVAILABLE, MAINTENANCE |
| Student | STUDYING, PAUSED, GRADUATED |
| Gender | MALE, FEMALE, OTHER |
| Lead | NEW, CONTACTED, CONSULTING, CONVERTED, LOST |
| Class response | UPCOMING, ONGOING, FINISHED; full là trường riêng |
| Enrollment | STUDYING, DROPPED, COMPLETED, TRANSFERRED, RESERVED |
| Tuition delivery | PENDING, CREATED, FAILED |
| Attendance | PRESENT, ABSENT, LATE; có thêm hasPermission |
| Tuition response | UNPAID, PARTIAL, PAID; CANCELLED có thể xuất hiện từ dữ liệu cũ |
| Document | PENDING, APPROVED, REJECTED |

Class filter FULL, Tuition filter PENDING/OVERDUE là tương thích dữ liệu/cách gọi cũ; không phải trạng thái được đặt tay.
Enum Payment còn đọc các trạng thái/phương thức legacy, nhưng API mới chỉ ghi phiếu PAID bằng CASH/BANK_TRANSFER.
workStatus của hồ sơ và examType/type/level đang là String; không giả định có API danh mục enum cho chúng.

## 7. Lưu ý khi nối frontend

1. Dùng đúng wrapper: Student detail={student,enrollments}, Class detail={schoolClass,schedules}.
2. GET danh sách tài khoản hiện có phân trang; không còn là mảng UserResponse trực tiếp.
3. teacherId=profileId; các trường audit dùng accountId.
4. Tạo Tuition chỉ gửi enrollmentId; giá và chi nhánh do backend xác minh.
5. Lưu requestKey của khoản thu cho đến khi biết kết quả; chỉ tạo key mới cho khoản thu mới.
6. File upload dùng FormData; file download xử lý binary/Blob.
7. Hiển thị tuitionDelivery khi học phí chưa tạo thành công; không tự tạo lại Enrollment.
8. GET /me có thể thất bại khi tài khoản bị khóa; Academic/Teaching/Finance đọc phiên hiện tại từ Auth mỗi request, khóa tài khoản hoặc thu hồi phân công có hiệu lực ở yêu cầu tiếp theo.
9. Không có Swagger/OpenAPI UI hoặc collection Postman đồng bộ tự động ở phiên bản này.

## Cải thiện giai đoạn 1: nhiều chi nhánh

Hợp đồng đầy đủ cho ba API phân công mới, header, allAssigned và lịch sử: [Giai đoạn 1](improvements/phase-one-multi-branch.md).

## Cải thiện giai đoạn 2 — tài khoản và phiên

Bốn API mới, body, giới hạn mật khẩu và thu hồi phiên: [Hợp đồng giai đoạn 2](improvements/phase-two-account-security.md). JWT có sv; đổi/cấp lại mật khẩu, logout-all và khóa tài khoản tăng session_version. Các service dùng phiên hiện tại từ Auth. PASSWORD_CHANGE_REQUIRED là lỗi 403 yêu cầu đổi mật khẩu trước khi tiếp tục.

## Buổi học thực tế — cải thiện 3

GET /academic/sessions hỗ trợ from/to ISO yyyy-MM-dd, status SCHEDULED/CANCELLED/COMPLETED và phân trang. DTO trả id,version,classId,className,branchId,sessionDate,startTime,endTime,roomId/roomName,plannedTeacherId/Name,teacherId/Name,status,kind,sourceDate,makeupForId,makeupSessionId,customized,reason,classClosed,canAttend. kind là REGULAR/MAKEUP/LEGACY; các snapshot LEGACY chưa biết có thể null.

SessionEdit cho PUT /sessions/{id} và POST /sessions/{id}/makeup:

```json
{"sessionDate":"2026-11-10","startTime":"18:00","endTime":"19:30","roomId":1,"teacherId":2,"reason":"Đổi theo lịch giáo viên","version":0}
```

Version lấy từ GET mới nhất, không tự tăng phía client. HTTP 409 khi phiên bản cũ hoặc xung đột nghiệp vụ; không tự động gửi lại mutation với phiên bản mới. Hủy/khôi phục dùng {reason,version}; reason dài 3–500 ký tự.

GET /academic/attendances/sheet?sessionId=123 trả thêm sessionId,version,sessionStatus,startTime,endTime,teacherName,canEdit,correctionRequired,canViewClassHistory. Giáo viên dạy thay dùng sheet này, không gọi /classes/{id}/students.

POST /academic/attendances/bulk giữ classId,sessionDate,records và thêm sessionId,version,reason. Gửi đủ roster từ sheet, không gửi danh sách rỗng. Với buổi COMPLETED chỉ SA/A sửa, phải có version và reason; gửi lại nội dung giống hệt không ghi thêm audit. Mỗi record gồm enrollmentId,status(PRESENT/ABSENT/LATE),hasPermission. GET /sessions/{id}/attendance-changes trả oldStatus,newStatus,oldPermission,newPermission,previousRecordedByName,changedByName,changedAt,reason.

Chi tiết chính sách: [cải thiện 3](improvements/phase-three-class-sessions.md).


## Cải thiện 4 — vòng đời và roster thi

StudentDto thêm version; EnrollmentDto thêm version,reservedSince,expectedReturnDate. API đổi trạng thái/chuyển lớp bắt buộc reason 3–500 ký tự và version lấy từ GET mới nhất. Không gửi lại tự động khi HTTP 409.

GET /academic/enrollments/{id}/history trả {periods:[{id,activeFrom,activeUntil}],events:[...]}; ngày kết thúc là exclusive. GET /academic/students/{id}/history trả mảng events. Event gồm id,enrollmentId,relatedEnrollmentId,action,oldStatus,newStatus,reason,effectiveDate,expectedReturnDate,actorName,occurredAt.

GET /academic/classes/{id}/roster?date=2026-11-05 trả [{id,fullName}], không có điện thoại/email. Teaching dùng roster theo examDate thay danh sách STUDYING hiện tại.

GET /teaching/exams/{id}/roster trả {examId,examDate,frozen,frozenAt,frozenByName,students:[{id,fullName,source}]}; source là ENROLLMENT_PERIOD hoặc SAVED_GRADE. Teacher lưu điểm đầu tiên tự chốt toàn roster trong giao dịch giữ khóa Exam. Ngày thi tương lai không được nhập điểm. Ngày thi/thang/trọng số không đổi sau khi có điểm/chốt. Học viên rời lớp sau kỳ thi vẫn nhập điểm được nếu có trong roster.

RESERVED → RESERVED dùng để thay expectedReturnDate (không mở kỳ học). RESERVED → transfer cho phép chuyển khỏi lớp cũ đã kết thúc và tạo đăng ký STUDYING tại lớp đích, giữ root học phí. Hoàn thành đóng kỳ học vào ngày mai; bảo lưu/ngừng/chuyển đóng từ hôm nay. [Quy ước và giới hạn](improvements/phase-four-learning-lifecycle.md).


## Cải thiện 5 — sổ tài chính

POST /finance/payments/{id}/corrections:

```json
{"type":"REFUND","amount":200000,"paymentMethod":"BANK_TRANSFER","referenceCode":"BANK-REF-001","reason":"Hoàn tiền theo quyết định giảm phí","requestKey":"unique-refund-key","version":3}
```

VOID dùng amount toàn phiếu, bỏ paymentMethod/referenceCode; REFUND dùng CASH/BANK_TRANSFER và không vượt credit/phần phiếu còn hiệu lực. BANK_TRANSFER bắt buộc referenceCode. reason 3–500 ký tự, requestKey tối đa 100 ký tự, version lấy Tuition GET mới nhất. Retry giữ toàn bộ payload kể cả version cũ; server kiểm tra key đã áp dụng trước khi kiểm tra version hiện tại. 409 khi đổi payload dưới cùng key hoặc yêu cầu mới dùng version cũ.

PaymentDto thêm voidedAmount,refundedAmount,availableAmount; amount/status/recordedBy vẫn là phiếu gốc. Correction trả id,paymentId,tuitionId,branchId,type,amount,paymentMethod,referenceCode,reason,requestKey,expectedVersion,recordedBy,recordedByName,createdAt.

GET /finance/tuitions/{id}/history trả {adjustments,corrections}. Adjustment gồm oldOriginal/oldDiscount/oldAmount/oldDueDate,newOriginal/newDiscount/newAmount/newDueDate,paidAtChange,reason,recordedByName,createdAt và metadata id/requestKey/expectedVersion.

GET /finance/stats/reconciliation?page=0&size=20&branchId=1 trả PageResponse với tuitionId,studentName,branchId,cachedPaid,ledgerPaid,difference,legacyPayments; không sửa database. allAssigned tương tự overview. [Chính sách, ví dụ và giới hạn](improvements/phase-five-finance-ledger.md).


## Cập nhật hợp đồng giai đoạn cải thiện 6

[Quy tắc hiện hành](improvements/phase-six-care-documents-experience.md) thay thế ví dụ cũ: PUT lead/status/convert và review tài liệu cần version; convert có JSON body. Staff chỉ sửa lead được giao. NOTE/đổi trạng thái không ghi nhận liên hệ thực tế. Tài liệu gửi lại chỉ từ REJECTED bởi chủ sở hữu, tăng revision; lịch sử không dành cho Staff.

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

## G04/G05 — chương trình, điều kiện và đánh giá đầu vào

Cập nhật hiện hành: tổng **173 API chính** (Auth 19, Academic 114, Teaching 28, Finance 12), ngoài aliases/diagnostics/health. [Đầy đủ 173 endpoint](api-list.md).

### Danh mục và chính sách khóa/lớp

Program: id,version,code,name,active. Level thêm programId,rank. Mã uppercase, 1–50 ký tự chữ/số/chấm/gạch, không đổi sau tạo; tên 1–255, active bắt buộc, rank nguyên không âm. Version phải gửi khi sửa. Có thể ngừng dùng; không xóa lịch sử. Rank không phải phép so sánh khả năng học, không quy đổi IELTS/TOEIC/CEFR.

Khóa chưa cấu hình trả policy synthetic `{id:null,revision:0,mode:"UNVERIFIED",acceptedLevels:[]}`; giữ riêng text legacy. Xác minh mapping bằng thao tác quản trị chọn chương trình/đầu vào/đầu ra tường minh, có lý do:

```json
{"expectedRevision":0,"mode":"REQUIRES_ASSESSMENT","programId":1,"outputLevelId":2,"acceptedLevelIds":[1],"reason":"Đối chiếu giáo trình và chuẩn đầu vào"}
```

ID chỉ minh họa: lấy ID thực từ GET danh mục. OPEN_ENTRY cần acceptedLevelIds rỗng; REQUIRES_ASSESSMENT cần ít nhất một mức. Tất cả level phải active và cùng program active, không trùng. Không cho PUT UNVERIFIED. Mỗi lần xác minh tạo policy bất biến revision+1, lưu mã/tên trình độ/chương trình, tác giả/thời điểm/lý do. Danh mục đổi tên không viết lại snapshot.

ClassAdmission.revision là lần áp dụng của lớp, khác policy.revision của khóa. Lớp mới/đổi khóa được liên kết bản mới nhất; lớp legacy chưa mapping giữ null/revision0. Sửa khóa không đổi lớp cũ. PUT lớp dùng cả expectedRevision của lớp và expectedCourseRevision của khóa, lý do 3–500 ký tự; giữ class_admission_events. Không tự cập nhật các đăng ký/phiếu thu. Điều kiện phân lớp chưa được enforce ở G04/G05; G06/G07 phải dùng chính snapshot lớp.

### Hồ sơ đánh giá

AssessmentDraft:

```json
{"programId":1,"levelId":1,"method":"INTERVIEW","assessedOn":"2026-11-08","assessorName":"Người phỏng vấn","source":"Biên bản đã đối chiếu","score":null,"maxScore":null}
```

Method TEST/INTERVIEW/CERTIFICATE/OTHER. Ngày không ở tương lai theo Asia/Ho_Chi_Minh; assessorName 1–255 và source 1–2000 ký tự bắt buộc, không chỉ khoảng trắng. TEST phải có điểm và thang; phương thức khác có thể không dùng điểm, hoặc có cả hai. 0 <= score <= maxScore, maxScore > 0, tối đa 8 chữ số nguyên/4 thập phân. Không tự tính level từ điểm; người ghi nhận chọn kết quả thuộc chương trình dựa trên nguồn.

POST tạo DRAFT chưa hiệu lực. PUT chỉ DRAFT và cần version + reason3–500. Confirm cần version, reason, expectedCurrentId lấy từ GET/current; chưa có gửi null. DRAFT -> CONFIRMED; bản hiện hành trước đó -> SUPERSEDED, lưu supersedesId/supersededById hai chiều. Một CONFIRMED/student/program, được bảo vệ bằng khóa và unique generated current_key. Hai yêu cầu cùng version/current reference không cùng thắng.

Revoke DRAFT/CONFIRMED -> REVOKED, cần version/reason. Không khôi phục SUPERSEDED. Muốn đính chính kết quả đã chốt, tạo nháp thay thế rồi xác nhận. Nội dung bản đã xác nhận không PUT đè. DTO lưu program/level code/name snapshot, assessor/source, recordedBy/Name, confirmedBy/Name/At, createdAt/updatedAt và liên kết thay thế. History ghi beforeState/afterState dạng chuỗi snapshot DTO, action,reason,actorName,occurredAt,assessmentVersion; không hứa là JSON. Mọi trạng thái được giữ; học viên có lịch sử assessment không được xóa cứng (409).

Student.currentLevel vẫn là text legacy chưa xác minh, không tự sửa và không dùng thay GET/current. GET/current tách khỏi bộ lọc/phân trang history. Confirm không tự xét hết hạn chứng chỉ; nguồn/thời điểm được người vận hành kiểm tra theo giả định G01-v1. Chưa sinh đánh giá từ điểm cuối khóa hoặc tạo thi online.

Áp dụng V008 sau V007, V009 sau V008, trước khi chạy Academic mới bằng prod/validate. [G04](thesis-progress/G04.md), [G05](thesis-progress/G05.md).

## G06/G07 — kiểm tra và xác nhận ngoại lệ đầu vào (08/11/2026)

Tổng hiện tại **161 API chính**: Auth19, Academic108, Finance12, Teaching22; thêm7 API, mở rộng3 request ghi danh/chuyển/học lại. [Danh sách](api-list.md).

### Preview và gợi ý

```json
{"studentId":1,"classId":2,"intent":"ENROLL","sourceEnrollmentId":null}
```

Intent ENROLL/TRANSFER/RESUME. ENROLL không có source; TRANSFER/RESUME bắt buộc sourceEnrollmentId thuộc chính học viên. RESUME là học lại đúng lớp của enrollment RESERVED; TRANSFER chỉ khác lớp cùng khóa và chi nhánh, nguồn STUDYING/RESERVED. ID mẫu phải thay bằng ID thực.

Preview trả context, classCode/Name, branchId, studentName/legacyLevel, classRevision, policy snapshot của lớp, assessment hiện hành đúng chương trình (hoặc null), academicStatus, blockers[{code,message}], eligible, canRequestException, fingerprint, sourceVersion, reusedDecisionId, currentSize/maxSize. Trạng thái học thuật: ELIGIBLE, MISSING_ASSESSMENT, PROGRAM_MISMATCH (chỉ có kết quả chương trình khác), LEVEL_MISMATCH, UNVERIFIED_POLICY, REUSED_ACCEPTANCE. OPEN_ENTRY đã xác nhận không cần assessment; không lấy output/rank/currentLevel thay acceptedLevelIds.

Blockers cứng gồm BRANCH_MISMATCH/INACTIVE, STUDENT_INACTIVE, COURSE_CLOSED, CLASS_FINISHED/FULL, DUPLICATE_ENROLLMENT, SCHEDULE_CONFLICT, SOURCE_BRANCH/STATUS, TRANSFER_COURSE. Phạm vi/role không hợp lệ trả403/404 trước preview, không tiết lộ lớp ngoài quyền. Lịch đối chiếu buổi thực tế chưa hủy từ hôm nay theo SessionPlanner; lịch mẫu chỉ để tham khảo. Gợi ý phân trang các lớp cùng chi nhánh, không ẩn lớp đầy/kết thúc/thiếu căn cứ; không tuyên bố xếp hạng toàn bộ catalog theo độ phù hợp.

Preview không giữ chỗ. Khi lưu, cùng PlacementService.evaluate chạy dưới branch/student/class/course lock với căn cứ mới nhất; đầy/lịch/duplicate/stale bị chặn409 trước phát sự kiện tuition. Không có snapshot mới nếu giao dịch bị từ chối. Tính sĩ số từ enrollment STUDYING, không tin cờ full trên client.

### Ngoại lệ và phiên bản

POST placement-exceptions gửi context như trên, fingerprint lấy từ preview và reason3–500. Chỉ đề nghị khi lỗi là học thuật và không có blocker cứng. Lưu policyId/classRevision, assessmentId/version hoặc không có, sourceVersion, SHA256 semantic context và snapshot gốc, người/lý do/thời điểm. Có thể xem requestedPolicy/requestedAssessment trong ExceptionView; status của assessment có thể đã thay đổi sau đề nghị, assessmentVersion là version tại lúc đề nghị.

Quản lý POST decision body `{version:0,approve:true,reason:"Đã kiểm tra bằng chứng"}`. Staff/Teacher403; Admin đúng phạm vi được duyệt đề nghị do mình tạo theo BR-07. REQUESTED -> APPROVED/REJECTED, không ghi danh hay giữ chỗ tự động; mỗi quyết định có actor/time/reason, không sửa hoặc duyệt lại. Khi căn cứ đổi, status trả về INVALIDATED; storedStatus giữ trạng thái lưu. Đây có thể là trạng thái hiệu lực tính khi đọc để không ghi DB trong GET. Quyết định trên REQUESTED đã đổi căn cứ lưu INVALIDATED và trả200 để UI tải lại; phiên bản đề nghị cũ409. APPROVED cũ dù storedStatus chưa đổi vẫn không tiêu thụ được.

Fingerprint gồm student/version/branch/status, class/branch/course/date-range/admission revision+policyId, các CONFIRMED assessment ID/version, intent/source ID/version/status và reusedDecisionId. Vì vậy kết quả bị thay/thu hồi hoặc policy lớp mới/nguồn thay đổi không dùng xác nhận cũ. Sửa policy khóa chưa áp dụng cho lớp không ảnh hưởng snapshot lớp. Đổi tên danh mục không viết lại căn cứ. Quyền/phân công hiện tại được kiểm tra lại độc lập; sĩ số/lịch/khả dụng luôn xét mới, không được nới bởi approval.

### Sử dụng và phục hồi phản hồi mất

Gửi approvalId trong request ghi danh, chuyển hoặc học lại đúng intent/source. Chỉ APPROVED, fingerprint đúng và không blocker mới được chấp nhận. Consume -> CONSUMED, lưu consumedEnrollmentId/At và EnrollmentAdmission trong cùng transaction Academic. Hai request cùng approval trả cùng enrollment sau lần commit đầu, không tạo thêm enrollment/tuition. Retry được nhận diện trước kiểm tra source version/capacity mới vì bản ghi đã tạo có thể chính là lý do lớp vừa đầy. Vẫn kiểm tra quyền và đúng đối tượng/intent/source; trả dữ liệu đăng ký hiện tại, không phát lại hành động học lại/chuyển hoặc tuition.

FE khi mất phản hồi khóa gửi, kiểm tra approval GET/consumedEnrollmentId hoặc danh sách/detail enrollment trước khi gửi lại; form cập nhật trạng thái đối chiếu trạng thái/version đã lưu. Confirm không phải cờ miễn mọi lỗi. Có thể dùng GET đề nghị/học phí/history để kiểm chứng kết quả.

### Snapshot và dữ liệu cũ

EnrollmentAdmission append-only lưu policy/assessment ID/version/snapshot, academicStatus (ELIGIBLE/EXCEPTION/REUSED_ACCEPTANCE), classRevision, intent, approval/reusedDecision, actor/time. Snapshot audit hiện là chuỗi DTO, không hứa JSON. Chuyển cùng khóa giữ tuitionFee và billingEnrollmentId gốc, không phát tuition mới. Học lại giữ căn cứ đã chấp nhận nếu cùng policyId/classRevision: không bắt đánh giá lại chỉ vì trình độ hiện tại đổi; snapshot lần học lại giữ chứng cứ đánh giá gốc và trỏ reusedDecisionId. Policy đổi thì xét lại hoặc ngoại lệ. Enrollment cũ thiếu history vẫn đọc được, hiển thị LEGACY_UNVERIFIED; không tạo chứng cứ bịa, đổi giá hay hủy dữ liệu. Nếu quay lại mà thiếu căn cứ cần xét mới/ngoại lệ. Đề nghị/audit bảo vệ xóa cứng học viên/lớp.

Áp dụng V010 sau V009 trước Academic mới. [Migration](migrations/README.md), [G06](thesis-progress/G06.md), [G07](thesis-progress/G07.md).

## G08/G09 — thời lượng chuẩn và tiến độ lớp (08/11/2026)

Tổng **161 API chính**: Auth19, Academic108, Finance12, Teaching22. [8 API mới](api-list.md).

### Phút chuẩn, legacy và phiên bản

Course.expectedMinutes là số phút nguyên 1–600000 (giới hạn bảo vệ 10000 giờ/khóa), durationRevision bắt đầu0. Course.duration giữ nguyên số cũ chưa rõ đơn vị; null nếu khóa mới không có giá trị legacy. Không tự nhân24 thành giờ/phút, không lấy tổng lịch làm mục tiêu. expectedMinutes=null là UNVERIFIED. CourseDto thêm expectedMinutes/durationRevision; duration không còn bắt buộc trong request. FE tạo mới yêu cầu số phút chuẩn; client cũ chỉ gửi duration vẫn tạo khóa chưa xác minh để tương thích.

POST courses có expectedMinutes thì SA/A xác nhận bản1 khi tạo, lưu actor/time và lý do ghi nhận lúc tạo; không bịa giá trị duration. PUT course cơ bản không đổi expectedMinutes đã xác minh: bỏ trường này hoặc giữ nguyên; đổi qua API duration-plan có version/lý do. Legacy duration đã có không được sửa số; omitting không xóa giá trị cũ. Staff không được xác nhận thời lượng, kể cả qua PUT cơ bản. Khóa dùng chung chỉ SA quản lý.

```json
{"expectedRevision":0,"expectedMinutes":90,"reason":"Đối chiếu giáo trình và kế hoạch 1 giờ 30 phút"}
```

PUT khóa tạo revision mới bất biến trong course_duration_plans, cập nhật projection Course. Lý do3–500; version cũ409. Không thay lớp đã mở. Academic từ chối JSON số lẻ cho các trường số nguyên (ACCEPT_FLOAT_AS_INT=false), ví dụ90.5 trả400 thay vì cắt thành90. Số tiền Decimal không đổi.

Lớp mới hoặc đổi khóa giữ expectedMinutes của khóa tại thời điểm đó và durationRevision riêng; nếu khóa chưa xác minh thì lưu snapshot UNVERIFIED, không tự đặt0. Lớp có sẵn trước V011 giữ null/revision0, không backfill. Xác minh riêng:

```json
{"expectedRevision":1,"source":"CUSTOM","expectedMinutes":180,"reason":"Xác nhận kế hoạch riêng của lớp"}
```

Hoặc source COURSE kèm expectedCourseRevision lấy từ GET mới nhất; không gửi expectedMinutes ghi đè. CUSTOM không nhận expectedCourseRevision. Không PUT source UNVERIFIED. Cả hai lưu class_duration_plans với revision, actor/time/reason, courseId/source courseRevision. Class plan khác course plan không tự là lỗi: có thể là kế hoạch đã chốt hoặc điều chỉnh có căn cứ. Class đã đóng thủ công không cho đổi plan; cần mở/đối soát theo chính sách hiện có. Staff/Teacher không xác nhận. Xóa lớp nháp chỉ được sau các guard lịch sử/enrollment hiện có; xóa kèm kế hoạch draft như admission draft, không xóa lịch sử lớp đã vận hành.

### Tổng thời lượng

Progress trả expectedMinutes, planRevision, scheduledMinutes, taughtMinutes, knownScheduledMinutes, knownTaughtMinutes, scheduledSessions (tổng buổi hoạt động SCHEDULED+COMPLETED), completedSessions, cancelledSessions, unfinishedSessions, overdueSessions, unknownActiveSessions, unknownCompletedSessions, invalidMakeupSessions, dataState, scheduledDifferenceMinutes, remainingTaughtMinutes, timeReadyForClosure và warnings[{code,message}]. Toàn bộ lớp, không theo cửa sổ lịch đang lọc.

- Số phút = end-start cùng ngày, giờ đã biết, end>start và chính xác đến phút. Buổi qua đêm/thiếu giờ/giây lẻ không được suy diễn hoặc làm tròn. Các API sửa/sinh lịch mới kiểm tra precision phút; UI cùng quy tắc.
- Đã xếp = SCHEDULED+COMPLETED có giờ hợp lệ; đã học = COMPLETED có giờ hợp lệ. CANCELLED không cộng, kể cả thiếu giờ. Điểm danh xác nhận là nguồn chuyển COMPLETED hiện có; ngày đã qua không tự chuyển. Sửa điểm danh không nhân đôi thời lượng; ngày/giờ/phòng/giáo viên buổi đã điểm danh vẫn bị khóa, không thêm đường sửa lịch sử tắt.
- MAKEUP hoạt động cần nguồn REGULAR cùng lớp đã CANCELLED, nguồn trỏ đúng child và chỉ một child hoạt động cho nguồn. Chỉ cộng buổi bù; liên kết sai không âm thầm cộng trùng. COMPLETED ở tương lai bị đánh dấu thiếu căn cứ.
- Có buổi hoạt động chưa đủ căn cứ thì scheduledMinutes=null, dataState=UNKNOWN. Nếu buổi hoàn tất thiếu căn cứ thì taughtMinutes=null. Các known* là phần cộng được, không được thay số null bằng0 hoặc dùng phần đã biết để tuyên bố đủ giờ. Unknown SCHEDULED không làm mất tổng COMPLETED đã biết, nhưng vẫn chặn đóng.
- scheduledDifferenceMinutes = đã xếp - kế hoạch khi đủ dữ liệu; remainingTaughtMinutes=max(0,kế hoạch-đã học). Null khi nguồn tương ứng chưa xác minh. Cảnh báo PLAN_UNVERIFIED, UNKNOWN_SESSION_MINUTES, INVALID_MAKEUP_LINK, FUTURE_COMPLETED, SCHEDULED_SHORTFALL/EXCESS, TAUGHT_SHORTFALL, UNFINISHED_SESSIONS, OVERDUE_SESSIONS.

Batch nạp tập lớp và projection session cho toàn danh sách trong2 truy vấn, không tải lazy course/từng lớp trong vòng lặp; kiểm tra toàn bộ scope rồi trả đúng thứ tự yêu cầu, không partial leak. Teacher dạy thay chỉ thấy giờ buổi được giao qua luồng session, không được tổng toàn lớp không phụ trách.

### Đóng lớp và bước sau

timeReadyForClosure=true khi kế hoạch đã xác minh, không buổi hoạt động thiếu căn cứ, taughtMinutes>=expectedMinutes và không còn SCHEDULED. PATCH class/status FINISHED kiểm tra lại dưới branch/class lock trước sync; không còn hành vi đóng để tự hủy các buổi chưa học. Cần hủy/khôi phục/học bù hoặc điều chỉnh kế hoạch có lý do. Đủ giờ chưa phải PASS hoặc đã chốt kết quả; kiểm tra kết quả/enrollment học vụ và snapshot cuối khóa ở G10/G11. dateStatus FINISHED vẫn chỉ là hết lịch, chưa được coi là xác nhận đóng học vụ.

Không tự chỉnh dữ liệu legacy thiếu giờ đã hoàn tất; giữ UNKNOWN và khóa sửa lịch sử. Cần đối soát và luồng sửa nguồn có kiểm soát của G11, không nhập0/ước lượng giờ để bỏ cảnh báo. [V011 và nâng cấp](migrations/README.md), [G08](thesis-progress/G08.md), [G09](thesis-progress/G09.md).

## G10 — quy tắc theo lớp và đánh giá tạm (08/11/2026)

Tổng **161 API chính** (Auth19, Academic108, Teaching22, Finance12), thêm5API. Hợp đồng report cũ được thay cùng FE: kết quả theo **enrollmentId**, passed nullable và trạng thái rõ, không còn chuẩn50% tự động. Client cũ phải cập nhật khi nâng cấp.

### Quy tắc đã xác nhận

```json
{"expectedRevision":0,"passPercent":70,"minAttendancePercent":null,"exams":[{"examId":1,"expectedExamVersion":0,"required":true,"weight":1},{"examId":2,"expectedExamVersion":0,"required":false,"weight":3}],"reason":"Đối chiếu chuẩn của lớp"}
```

ID/version chỉ minh họa, lấy từ GET exam/policy mới nhất. Chọn1–200bài khác nhau thuộc lớp. Pass percent0–100, attendance null là NOT_REQUIRED hoặc0–100 khi có yêu cầu. Weight dương <=100, tối đa2thập phân, là trọng số tương đối, không bắt tổng100. Rule snapshot giữ examId/version/name/date/maxScore/required/weight. Thang điểm từ bài thật, không đặt thang khác điểm đã nhập. exam.weight cũ là gợi ý khi cấu hình, trọng số trong policy mới là nguồn tính.

Chỉ SA/A đúng scope xác nhận, Teacher/Staff không được tự nới ngưỡng, bỏ bắt buộc hoặc sửa policy. Mỗi lần lưu tạo revision bất biến, actor/time/reason3–500; expectedRevision và expectedExamVersion stale409. State row theo classID, ghi policy/source exam/grade dùng lock state trước exam; INSERT IGNORE khởi tạo state an toàn trên MySQL8/H2MySQL. Không có JPA đọc bảng Academic hoặc transaction phân tán. Native state initialization là owned Teaching table, không khóa chéo service.

Exam.version thêm vào DTO; roster freeze/cosmetic edit có thể tăng version, UI tải lại khi cấu hình bị stale. Metadata date/scale/legacyweight của bài thuộc policy hiện hành không được sửa ngầm qua Exam API, cả Teacher/Admin. Muốn thay cấu trúc quản lý tạo revision mới; guard grades/frozen roster cũ vẫn chặn đổi ngày/thang khi có điểm. Exam từng có policy không xóa cứng vì giữ lịch sử. Grade admin correction vẫn chỉ sửa điểm đã có, cần lý do và giữ GradeChange; G10 không cấp quyền quản lý nhập điểm ban đầu.

Chưa policy -> UNCONFIGURED, không tự lấy50%/coi bài cũ là bắt buộc. Lớp đổi course trước khi ghi danh khiến policy cũ STALE_CLASS_CONTEXT; preview xét như chưa có quy tắc cho context mới đến khi quản lý xác nhận lại. Quy tắc theo class/course cụ thể, không tự áp cho mọi lớp cùng chương trình.

### Theo đăng ký và ngày học

Academic evaluation-context trả các enrollment kể cả RESERVED/TRANSFERRED/DROPPED/COMPLETED với khoảng [activeFrom,activeUntil), không dựa STUDYING hôm nay; periods thiếu giữ rỗng, không suy ra từ currentLevel/enrolledAt. DatedRoster thêm enrollmentId/source, legacy fallback/ambiguous trả enrollmentId null. Grade entry freeze mới lưu mã enrollment ở exam_candidates, giữ liên kết sau chuyển/bảo lưu cùng ngày; không backfill mapping roster cũ.

Với bài chưa freeze, áp dụng theo khoảng học; cùng student có nhiều enrollment cùng ngày không được gán điểm hai lần. Với frozen roster, dùng enrollmentId đã ghi; legacy null chỉ suy ra tạm nếu khoảng học cho duy nhất một enrollment, nếu thiếu/mâu thuẫn trả UNKNOWN_ROSTER. Bài trước khi vào hoặc sau khi đã chuyển không áp sai cho enrollment cũ. Điểm ngoài policy/khoảng học hoặc chưa gắn được báo UNUSED_GRADES, không tự cộng.

### Kết quả tạm và công thức

Report gồm classId/policyRevision/policyStatus, students theo enrollmentId+studentId, passRate, assessableCount/insufficientCount/passedCount/failedCount, generatedAt, duration và warnings. Row có averagePercent hiển thị2số, unroundedAveragePercent20số, graded/applicable/missingRequired, attendance, từng examResult và reasons[{code,message}].

Điểm tạm = sum((score/maxScore)*100*policyWeight)/sum(policyWeight) trên các bài **áp dụng đã chấm hợp lệ**. Chia trung gian20chữ số, so ngưỡng trước làm tròn2số. Ví dụ99.99/200=49.995% hiển thị50.00 nhưng chưa đạt ngưỡng50. Điểm0 là đã chấm, null là chưa chấm. Bài tùy chọn chưa chấm không cho0 vào mẫu số; vẫn cần ít nhất một bài áp dụng có điểm. Bài bắt buộc thiếu hoặc chưa đến ngày phải báo chưa đủ dữ liệu. Score ngoài thang/future grade không được cộng âm thầm.

- PASS: đủ căn cứ và đạt ngưỡng điểm/chuyên cần đã cấu hình; FE ghi **Đạt tạm**.
- FAIL: đủ căn cứ nhưng điểm/chuyên cần dưới ngưỡng; FE ghi **Chưa đạt tạm**.
- INSUFFICIENT: thiếu policy/period/roster/bài bắt buộc/nguồn điểm/chuyên cần; passed=null, không gọi FAIL.

passRate là PASS/(PASS+FAIL), loại INSUFFICIENT khỏi mẫu số và hiện riêng số chưa đủ; khi không có đăng ký đủ dữ liệu trả null, không0. Không gọi đây là tỷ lệ đạt cuối khóa. averagePercent=null khi chưa có bài hợp lệ, không0.

### Chuyên cần

Chỉ tải marks/sessions khi policy yêu cầu. Mẫu số là session COMPLETED thuộc khoảng học, PRESENT/LATE là tham dự; ABSENT kể cả có phép vẫn là vắng và theo dõi excusedAbsences riêng. Buổi thiếu giờ/giờ sai hoặc ở tương lai, thiếu/nhầm mark, legacy date không gắn được duy nhất session, khoảng học không đủ hoặc mẫu số0 -> UNKNOWN. Saved attendance ngoài khoảng hiện tại được báo chưa rõ nguồn thay vì bỏ vắng để làm đẹp tỷ lệ. Không có điều kiện -> NOT_REQUIRED, percent=null, không mặc định100%. Ngưỡng0 vẫn đòi nguồn chuyên cần đã biết; nếu không yêu cầu phải dùng null.

Academic đọc snapshot REPEATABLE_READ của học vụ; Teaching đọc snapshot REPEATABLE_READ của policy/grades/roster và gọi API Academic, không tuyên bố hai snapshot là transaction nguyên tử. Dữ liệu provisional có thể đổi sau preview. Academic lỗi không trả0/PASS thay lỗi; FE hiển thị lỗi và tải lại. Context chỉ tên/đăng ký/khoảng học/attendance/duration, không tài chính hoặc CRM; Teacher dạy thay không được xem toàn lớp không phụ trách.

G10 không tạo CONFIRMED/PASS cuối khóa, không đổi enrollment COMPLETED hoặc class FINISHED. Duration đủ chưa quyết định PASS; G11 phải khóa/version/snapshot phối hợp nguồn, chốt/mở lại và đồng bộ học vụ có kiểm soát. [V012](migrations/V012__class_evaluation_policies.sql), [G10](thesis-progress/G10.md).

## G11/G12: xác nhận và đồng bộ kết quả

PATCH /academic/enrollments/{id}/status nhận thêm finalResultId/finalResultRevision khi COMPLETED; kể cả cùng COMPLETED phải tham chiếu đúng current CONFIRMED. PASS hoặc FAIL đều được kết thúc quá trình học. INSUFFICIENT, nguồn sai, refs cũ, dependency lỗi đều chặn. Kết quả trả thêm completionResultId/completionResultRevision. Học phí/billing root không thay đổi. PATCH /classes/{id}/status không còn nhận FINISHED hoặc mở lại lớp đã đóng; dùng hai POST có lý do ở trên.

reason 3–500 ký tự. expectedRevision/version nguyên >=0. requestId cùng intent trả cùng kết quả, payload khác409; replay chốt cũ có thể trả state REOPENED nên luôn đọc state. Giữ cùng UUID/body khi retry, không tự tạo intent mới sau mất phản hồi. Phục hồi FE dùng requests/{requestId}; đồng bộ Academic là bước riêng, không transaction phân tán. [G11](thesis-progress/G11.md), [G12](thesis-progress/G12.md), [hướng dẫn](../deployment/learning-results-user-guide.md).


Ví dụ chốt: POST /teaching/classes/42/final-results/99/confirm với {"requestId":"d35fba8e-7393-4c65-9b59-a1570dff6d32","fingerprint":"<SHA256 từ preview mới nhất>","reason":"Đã đối soát điểm, chuyên cần và thời lượng"}. Không tự tính fingerprint ở FE.

Snapshot lưu policy đầy đủ tại thời điểm chốt, row điểm/chuyên cần/bài áp dụng và duration; revision quy tắc và revision nguồn lưu cạnh kết quả. Phiên bản trước khi bổ sung full policy trong rehearsal vẫn hiển thị thiếu snapshot quy tắc, không suy diễn từ policy hôm nay.
