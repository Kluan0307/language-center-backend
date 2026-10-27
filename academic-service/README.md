# Academic service

Spring Boot 4.1.1 / Java 21 / MySQL 8. Base URL: `http://localhost:8082/api/v1/academic`.

## Chạy local

Build trên Windows:

```powershell
.\mvnw.cmd clean compile
```

Trên Linux/macOS: `./mvnw clean compile`.

Cấu hình bằng biến môi trường: `DB_PASSWORD`, `JWT_SECRET` bắt buộc; `DB_USERNAME` mặc định root; `DB_URL` mặc định MySQL localhost:3306/language_center_db; `JWT_EXPIRATION` mặc định 86400000 ms. JWT_SECRET và JWT_EXPIRATION phải trùng auth-service. Không lưu secret hoặc mật khẩu mới trong mã nguồn.

Nếu đã đặt các biến môi trường:

```powershell
.\scripts\run-local.ps1
```

Để phát triển trên máy hiện tại, có thể đọc cấu hình auth có sẵn vào biến môi trường của tiến trình:

```powershell
.\scripts\run-local.ps1 -AuthProperties ..\auth-service\src\main\resources\application.properties
```

Script này chỉ đọc file được chỉ định, không sửa cấu hình hay dữ liệu của auth. Ứng dụng academic khi triển khai chỉ cần biến môi trường, không cần mã nguồn auth bên cạnh. Database language_center_db phải tồn tại và tài khoản DB có quyền tạo/sửa 9 bảng academic. Khi branches rỗng, ứng dụng tạo hai chi nhánh có id 1 và 2.

## Kiểm thử API

Import [Postman collection](docs/academic-service.postman_collection.json), đặt biến `token` bằng JWT từ auth-service và thay các ID theo dữ liệu thực tế. Body mẫu của từng endpoint có sẵn trong collection. DELETE thành công trả 204; các API còn lại trả 200.

Sau khi ứng dụng đang chạy, chạy bộ kiểm tra tích hợp (không phải unit test):

```powershell
.\scripts\verify-local.ps1 -AuthProperties ..\auth-service\src\main\resources\application.properties
```

Hoặc bỏ -AuthProperties khi đã có DB_PASSWORD/JWT_SECRET. Bộ kiểm tra ký JWT local từ JWT_SECRET, tạo hai chi nhánh riêng với mã SMOKEyyyyMMddHHmmss, kiểm tra 53 endpoint và các quy tắc nghiệp vụ. Script verify tự dọn dữ liệu của đúng lượt chạy trong finally; script smoke.ps1 riêng giữ fixture để chẩn đoán. CleanupSmoke.java chỉ xóa dữ liệu academic thuộc đúng prefix SMOKE được truyền vào.

Lần xác minh ngày 2026-10-27: Maven Wrapper clean compile thành công; app khởi động MySQL 8.0.46; đủ 9 bảng và seed 1–2; 230 kiểm tra API thành công, gồm hai request tranh một chỗ học. Bộ kiểm tra không tạo hoặc sửa tài khoản auth. Tài khoản ADMIN/STAFF/TEACHER có branchId=null sẽ bị từ chối 403 đúng đặc tả; để dùng token đăng nhập thật cần chuẩn bị tài khoản có chi nhánh bên auth.

## Hợp đồng chung

- Header: `Authorization: Bearer <token>`. Claims: sub, role, accountId, profileId, branchId; HS256 tương thích auth-service. Token hết hạn hoặc sai chữ ký trả 401.
- ADMIN, STAFF, TEACHER bị ép branchId theo token; không có branchId trả 403. SUPER_ADMIN lọc branchId tùy chọn, không lọc thì xem mọi chi nhánh.
- Truy cập bản ghi chi nhánh khác trả 404. TEACHER chỉ xem lớp/lịch của profileId trong token; ghi điểm danh lớp khác trong cùng chi nhánh trả 403.
- Danh sách phân trang: `{content,page,size,totalElements,totalPages}`; page từ 0, size 1–200, mặc định 20. SchoolClass.students/schedules và các báo cáo điểm danh trả danh sách hoặc sheet, không phân trang.
- Ngày yyyy-MM-dd, giờ HH:mm, ngày giờ ISO-8601; dayOfWeek dùng MONDAY…SUNDAY. Ngày nghiệp vụ dựa trên múi giờ JVM.
- Lỗi thường: `{"message":"..."}`; lỗi validation: map field → message. 400 dữ liệu/nghiệp vụ; 401 xác thực; 403 quyền; 404 không tìm thấy; 409 trùng/tham chiếu/xung đột.
- Chi tiết Student: `{student,enrollments}`. Chi tiết Class: `{schoolClass,schedules}`. Không trả entity JPA.

## Endpoint

Mọi path dưới đây bắt đầu bằng /api/v1/academic. SA = SUPER_ADMIN, A = ADMIN, S = STAFF, T = TEACHER. “SA/A/S” bao gồm quyền SUPER_ADMIN; khóa học chung chỉ SA được tạo hoặc sửa.

### Branch

| Method | Path | Role | Query / body |
|---|---|---|---|
| GET | `/branches` | SA/A/S/T | `page, size, keyword, branchId` |
| GET | `/branches/{branchId}` | SA/A/S/T | — |
| POST | `/branches` | SA | Body Branch mẫu bên dưới |
| PUT | `/branches/{branchId}` | SA | Body Branch mẫu bên dưới |
| PATCH | `/branches/{branchId}/toggle-status` | SA | — |

### Course

| Method | Path | Role | Query / body |
|---|---|---|---|
| GET | `/courses` | SA/A/S/T | `page, size, keyword, status, level, type, branchId` |
| GET | `/courses/{courseId}` | SA/A/S/T | — |
| POST | `/courses` | SA/A/S | Body Course mẫu bên dưới |
| PUT | `/courses/{courseId}` | SA/A/S | Body Course mẫu bên dưới |
| PATCH | `/courses/{courseId}/status` | SA/A/S | `{"status":"CLOSED"}` |

### Room

| Method | Path | Role | Query / body |
|---|---|---|---|
| GET | `/rooms` | SA/A/S | `page, size, keyword, status, branchId` |
| GET | `/rooms/{roomId}` | SA/A/S | — |
| POST | `/rooms` | SA/A/S | Body Room mẫu bên dưới |
| PUT | `/rooms/{roomId}` | SA/A/S | Body Room mẫu bên dưới |
| PATCH | `/rooms/{roomId}/status` | SA/A/S | `{"status":"MAINTENANCE"}` |
| DELETE | `/rooms/{roomId}` | SA/A/S | — |

### Student

| Method | Path | Role | Query / body |
|---|---|---|---|
| GET | `/students` | SA/A/S | `page, size, keyword, level, status, branchId` |
| GET | `/students/{studentId}` | SA/A/S | — |
| POST | `/students` | SA/A/S | Body Student mẫu bên dưới |
| PUT | `/students/{studentId}` | SA/A/S | Body Student mẫu bên dưới |
| PATCH | `/students/{studentId}/status` | SA/A/S | `{"status":"PAUSED"}` |
| DELETE | `/students/{studentId}` | SA/A/S | — |

### Lead

| Method | Path | Role | Query / body |
|---|---|---|---|
| GET | `/leads` | SA/A/S | `page, size, keyword, status, assignedTo, branchId` |
| GET | `/leads/{leadId}` | SA/A/S | — |
| POST | `/leads` | SA/A/S | Body Lead mẫu bên dưới |
| PUT | `/leads/{leadId}` | SA/A/S | Body Lead mẫu bên dưới |
| PATCH | `/leads/{leadId}/status` | SA/A/S | `{"status":"CONTACTED","note":"Đã gọi tư vấn"}` |
| POST | `/leads/{leadId}/convert` | SA/A/S | — |

### SchoolClass

| Method | Path | Role | Query / body |
|---|---|---|---|
| GET | `/classes` | SA/A/S/T | `page, size, keyword, status, courseId, teacherId, branchId` |
| GET | `/classes/my` | T | `page, size` |
| GET | `/classes/{classId}` | SA/A/S/T | — |
| GET | `/classes/{classId}/students` | SA/A/S/T | — |
| GET | `/classes/{classId}/schedules` | SA/A/S/T | — |
| POST | `/classes` | SA/A/S | Body SchoolClass mẫu bên dưới |
| PUT | `/classes/{classId}` | SA/A/S | Body SchoolClass mẫu bên dưới |
| PATCH | `/classes/{classId}/status` | SA/A/S | `{"status":"FINISHED"}` |
| DELETE | `/classes/{classId}` | SA/A/S | — |

### Schedule

| Method | Path | Role | Query / body |
|---|---|---|---|
| GET | `/schedules` | SA/A/S/T | `page, size, classId, dayOfWeek, teacherId, roomId, branchId` |
| GET | `/schedules/my` | T | `page, size` |
| POST | `/schedules` | SA/A | Body Schedule mẫu bên dưới |
| PUT | `/schedules/{scheduleId}` | SA/A | Body Schedule mẫu bên dưới |
| DELETE | `/schedules/{scheduleId}` | SA/A | — |
| POST | `/schedules/check-conflict` | SA/A | Body Schedule mẫu bên dưới |

### Enrollment

| Method | Path | Role | Query / body |
|---|---|---|---|
| GET | `/enrollments` | SA/A/S | `page, size, keyword, studyStatus, classId, studentId, branchId` |
| GET | `/enrollments/{enrollmentId}` | SA/A/S | — |
| POST | `/enrollments` | SA/A/S | Body Enrollment mẫu bên dưới |
| PATCH | `/enrollments/{enrollmentId}/status` | SA/A/S | `{"studyStatus":"DROPPED"}` |
| POST | `/enrollments/{enrollmentId}/transfer` | SA/A/S | Body Enrollment mẫu bên dưới |

### Attendance

| Method | Path | Role | Query / body |
|---|---|---|---|
| GET | `/attendances/sheet?classId={classId}&date=2026-10-27` | SA/A/S/T | — |
| POST | `/attendances/bulk` | SA/A/S/T | Body [mẫu điểm danh](#body-mẫu) |
| GET | `/attendances/history?classId={classId}&from=2026-10-01&to=2026-10-27` | SA/A/S/T | — |
| GET | `/attendances/summary?classId={classId}` | SA/A/S/T | — |

### Stats

| Method | Path | Role | Query / body |
|---|---|---|---|
| GET | `/stats/overview` | SA/A/S | `branchId, groupByBranch` |

## Body mẫu

Các body tạo và PUT dùng cùng cấu trúc. POST Branch không nhận branchId. SUPER_ADMIN gửi branchId cho Room, Student, Lead, Class; Course branchId=null là khóa chung. Enrollment/Schedule/Attendance suy ra chi nhánh từ lớp, không nhận branchId độc lập.

### branch

```json
{
  "code": "CN03",
  "name": "Chi nhánh Quận 3",
  "address": "123 Nguyễn Đình Chiểu",
  "phone": "0901234567"
}
```

### course

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

### room

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

### student

```json
{
  "fullName": "Nguyễn Văn An",
  "dateOfBirth": "2005-06-15",
  "gender": "MALE",
  "email": "an@example.com",
  "phone": "0901234568",
  "address": "TP.HCM",
  "contactPerson": "Nguyễn Văn Bình",
  "currentLevel": "A1",
  "branchId": 1
}
```

### lead

```json
{
  "fullName": "Trần Minh Anh",
  "phone": "0901234569",
  "email": "anh@example.com",
  "source": "FACEBOOK",
  "interestedCourseId": 1,
  "desiredLevel": "A1",
  "note": "Quan tâm lớp buổi tối",
  "branchId": 1
}
```

### schoolclass

```json
{
  "code": "A1-001",
  "name": "Tiếng Anh A1 buổi tối",
  "courseId": 1,
  "teacherId": 1,
  "teacherName": "Nguyễn Thị Giáo",
  "roomId": 1,
  "startDate": "2026-10-01",
  "endDate": "2026-12-31",
  "maxSize": 20,
  "branchId": 1
}
```

### schedule

```json
{
  "classId": 1,
  "dayOfWeek": "MONDAY",
  "startTime": "18:00",
  "endTime": "19:30"
}
```

### enrollment

```json
{
  "studentId": 1,
  "classId": 1
}
```

### bulk

```json
{
  "classId": 1,
  "sessionDate": "2026-10-27",
  "records": [
    {
      "enrollmentId": 1,
      "status": "PRESENT",
      "hasPermission": false
    },
    {
      "enrollmentId": 2,
      "status": "ABSENT",
      "hasPermission": true
    }
  ]
}
```

## Giả định và quyết định triển khai

1. Giữ toàn bộ mặc định mục 10: Student thuộc một chi nhánh, có thể học nhiều lớp; Course null branch là khóa chung; tin teacherId/teacherName từ frontend; phone Student duy nhất trong chi nhánh; STAFF xem mọi Lead của chi nhánh; trạng thái lớp tính khi đọc; cho sửa điểm danh quá khứ; chặn xóa khi còn tham chiếu.
2. JWT hiện không có fullName. assignedToName và recordedByName lưu username từ sub. Các ID tham chiếu auth chỉ là Long, không có quan hệ JPA hoặc truy vấn bảng auth.
3. branchId của bản ghi không thay đổi qua PUT. ADMIN/STAFF/TEACHER bỏ qua branchId client; SUPER_ADMIN chọn chi nhánh khi tạo. Liên kết course/room/student/class đều phải đúng phạm vi.
4. Class có statusManuallySet để phân biệt trạng thái đặt tay. FULL khi đủ chỗ được ưu tiên khi cập nhật sĩ số; FULL còn chỗ quay về trạng thái theo ngày. Không đổi trạng thái FINISHED khi tính lại sĩ số. PATCH FULL yêu cầu lớp thực sự đủ chỗ.
5. SchoolClass.schedules là tùy chọn lúc tạo, chỉ SA/A được gửi lịch. STAFF tạo lớp không kèm lịch. PUT lớp không sửa danh sách lịch: dùng API schedules. Không được đổi classId của một Schedule qua PUT.
6. Check-conflict cho lớp đã lưu nhận classId, dayOfWeek, startTime, endTime và excludeScheduleId tùy chọn. Cho lớp chưa lưu, bỏ classId và gửi branchId, teacherId, roomId, startDate, endDate. Preview không lưu. Khung giờ liền kề được phép; trùng lịch trong cùng lớp bị chặn.
7. Chuyển lớp chỉ áp dụng Enrollment STUDYING, trả Enrollment mới; đăng ký cũ giữ TRANSFERRED. PATCH khôi phục STUDYING phải kiểm tra lại sĩ số, trùng đăng ký và lịch. TuitionClient chỉ log sau commit thành công, kể cả đăng ký mới do chuyển lớp.
8. Các thao tác đăng ký và thay đổi lịch/lớp được tuần tự theo chi nhánh, kết hợp khóa pessimistic trên Student/Class; dùng READ_COMMITTED và làm mới dữ liệu sau khi chờ khóa. Course có khóa khi kiểm tra giới hạn sức chứa để tránh đua với cập nhật khóa học.
9. Bulk điểm danh phải gửi đúng và đủ mọi Enrollment STUDYING của lớp, không lặp ID. hasPermission thiếu/null mặc định false; chỉ ABSENT mới lưu true. Không hạn chế ngày theo startDate/endDate hoặc lịch tuần vì đặc tả chỉ cấm ngày tương lai.
10. Summary chỉ liệt kê Enrollment hiện STUDYING. History giữ các bản ghi đã lưu, kể cả học viên chuyển/nghỉ sau này. Buổi chưa lưu không tính vào thống kê. Tỷ lệ tham gia = (PRESENT + LATE) / số buổi đã lưu × 100; không có buổi thì 0.
11. Tỷ lệ lấp đầy = tổng currentSize / tổng maxSize của mọi lớp trong phạm vi × 100. Tỷ lệ chuyển đổi = Lead CONVERTED / tổng Lead × 100. Làm tròn 2 chữ số; mẫu số 0 thì 0. Trình độ chưa đặt gom vào UNSPECIFIED.
12. Stats luôn trả {overall,branches}. branches có dữ liệu khi SUPER_ADMIN gửi groupByBranch=true; branchId nếu có giới hạn cả overall và branches. Role khác gửi groupByBranch=true trả 403.
13. Lead PUT nối note có timestamp nếu gửi note; PATCH status cập nhật lastContactedAt và nối note, không ghi đè. CONVERTED chỉ đạt qua API convert.
14. Branch.code duy nhất toàn hệ thống vì Branch không có branch cha. Course.code duy nhất trong phạm vi branch/null; generated scope_key giải quyết việc MySQL cho phép nhiều NULL trong UNIQUE.
15. Stats dùng DTO và repository truy vấn các bảng sở hữu, không có StatsEntity/bảng thứ 10.

## Giới hạn tích hợp hiện tại

Token được xác thực cục bộ theo chữ ký/hạn, nên trạng thái khóa tài khoản hoặc thay đổi vai trò tại auth không thu hồi ngay token đã phát. Chưa kiểm tra giáo viên qua mạng theo đúng mặc định của prompt. TuitionClient chưa tạo khoản thu thật. Academic được kiểm thử trực tiếp qua cổng 8082; cấu hình route gateway nằm ngoài phần triển khai này.

