# Teaching service

Spring Boot 4.1.1 / Java 21 / MySQL 8. Base URL: `http://localhost:8083/api/v1/teaching`.

## Chạy local

Build trên Windows:

```powershell
.\mvnw.cmd clean compile
```

Trên Linux/macOS:

```bash
./mvnw clean compile
```

Cấu hình bằng biến môi trường: `DB_PASSWORD`, `JWT_SECRET` bắt buộc; `DB_USERNAME` mặc định root; `DB_URL` mặc định MySQL localhost:3306/language_center_db; `JWT_EXPIRATION` mặc định 86400000 ms. JWT_SECRET và JWT_EXPIRATION phải trùng auth-service. Không lưu secret hoặc mật khẩu mới trong mã nguồn.

Nếu đã đặt biến môi trường:

```powershell
.\mvnw.cmd spring-boot:run
```

Hoặc dùng script khởi chạy nếu có trong dự án hoặc đọc thêm config từ auth-service:

```powershell
.\scripts\run-local.ps1 -AuthProperties ..\auth-service\src\main\resources\application.properties
```

Database `language_center_db` phải tồn tại và tài khoản DB có quyền tạo/sửa bảng teaching. Nếu chưa có dữ liệu, service sẽ chủ yếu tạo schema và dữ liệu theo mô hình học tập của trung tâm.

## Mục tiêu service

Teaching service chịu trách nhiệm quản lý các nghiệp vụ liên quan đến:

- lớp học và lịch học
- phòng học và cơ sở vật chất
- điểm danh học viên
- đề thi, bài kiểm tra và tài liệu giảng dạy
- quyền truy cập theo chi nhánh và giáo viên

Các entity hiện có trong code (đã quan sát từ source) cho thấy service đang tập trung vào các thực thể chính:

- `Room`
- `Schedule`
- `Attendance`
- `Facility`
- `Exam`
- `Document`

Các thực thể này thể hiện mô hình vận hành của trung tâm ngoại ngữ: phòng học, lịch dạy, điểm danh, thiết bị, bài kiểm tra, tài liệu giảng dạy.

## Hợp đồng chung

- Header: `Authorization: Bearer <token>`.
- Claims: `sub`, `role`, `accountId`, `profileId`, `branchId`.
- Token phải được ký bằng `HS256` tương thích với auth-service.
- ADMIN, STAFF, TEACHER bị ép theo branchId trong JWT.
- SUPER_ADMIN có thể truy cập nhiều chi nhánh.
- TEACHER chỉ được xem/điểm danh lớp thuộc profileId của mình.
- Mỗi request trả về JSON chuẩn; lỗi tuân thủ format: `{"message":"..."}`.
- 400: dữ liệu không hợp lệ hoặc nghiệp vụ sai.
- 401: không xác thực.
- 403: không có quyền.
- 404: không tìm thấy dữ liệu.
- 409: xung đột dữ liệu / trùng / ràng buộc nghiệp vụ.

## Endpoint đề xuất

Mọi path dưới đây bắt đầu bằng `/api/v1/teaching`.

### Room

| Method | Path | Role | Mô tả |
|---|---|---|---|
| GET | `/rooms` | SA/A/S/T | Liệt kê phòng học |
| GET | `/rooms/{roomId}` | SA/A/S/T | Chi tiết phòng học |
| POST | `/rooms` | SA/A/S | Tạo phòng học |
| PUT | `/rooms/{roomId}` | SA/A/S | Sửa phòng học |
| PATCH | `/rooms/{roomId}/status` | SA/A/S | Đổi trạng thái |
| DELETE | `/rooms/{roomId}` | SA/A/S | Xóa phòng học |

### Schedule

| Method | Path | Role | Mô tả |
|---|---|---|---|
| GET | `/schedules` | SA/A/S/T | Liệt kê lịch học |
| GET | `/schedules/my` | T | Lịch của giáo viên đang login |
| POST | `/schedules` | SA/A | Tạo lịch học |
| PUT | `/schedules/{scheduleId}` | SA/A | Sửa lịch học |
| DELETE | `/schedules/{scheduleId}` | SA/A | Xóa lịch học |
| POST | `/schedules/check-conflict` | SA/A | Kiểm tra trùng lịch |

### Attendance

| Method | Path | Role | Mô tả |
|---|---|---|---|
| GET | `/attendances/sheet` | SA/A/S/T | Xem sheet điểm danh theo ngày |
| POST | `/attendances/bulk` | SA/A/S/T | Điểm danh hàng loạt |
| GET | `/attendances/history` | SA/A/S/T | Lịch sử điểm danh |
| GET | `/attendances/summary` | SA/A/S/T | Tổng hợp điểm danh |

### Facility

| Method | Path | Role | Mô tả |
|---|---|---|---|
| GET | `/facilities` | SA/A/S | Danh sách thiết bị / cơ sở vật chất |
| GET | `/facilities/{facilityId}` | SA/A/S | Chi tiết thiết bị |
| POST | `/facilities` | SA/A/S | Tạo thiết bị |
| PUT | `/facilities/{facilityId}` | SA/A/S | Sửa thiết bị |
| PATCH | `/facilities/{facilityId}/status` | SA/A/S | Đổi trạng thái |

### Exam

| Method | Path | Role | Mô tả |
|---|---|---|---|
| GET | `/exams` | SA/A/S/T | Liệt kê bài kiểm tra |
| GET | `/exams/{examId}` | SA/A/S/T | Chi tiết bài kiểm tra |
| POST | `/exams` | SA/A/S | Tạo bài kiểm tra |
| PUT | `/exams/{examId}` | SA/A/S | Chỉnh sửa bài kiểm tra |
| PATCH | `/exams/{examId}/status` | SA/A/S | Đổi trạng thái |

### Document

| Method | Path | Role | Mô tả |
|---|---|---|---|
| GET | `/documents` | SA/A/S/T | Danh sách tài liệu |
| GET | `/documents/{documentId}` | SA/A/S/T | Chi tiết tài liệu |
| POST | `/documents` | SA/A/S/T | Tải lên tài liệu |
| PATCH | `/documents/{documentId}/review` | SA/A/S | Duyệt / từ chối tài liệu |
| DELETE | `/documents/{documentId}` | SA/A/S | Xóa tài liệu |

## Body mẫu

Dưới đây là các payload mẫu theo đúng mô hình entity đang có trong service. Các field dạng `...Id` nên được map sang entity relation trong controller/service layer.

### room

```json
{
  "code": "P101",
  "name": "Phòng 101",
  "floor": "Tầng 1",
  "capacity": 25,
  "status": "ACTIVE",
  "branchId": 1,
 
}
```

### schedule

```json
{
  "schoolClassId": 1,
  "roomId": 3,
  "teacherAccountId": 12,
  "dayOfWeek": "MONDAY",
  "startTime": "18:00:00",
  "endTime": "19:30:00"
}
```

### attendance

```json
{
  "sessionDate": "2026-10-27",
  "records": [
    {
      "enrollmentId": 1,
      "status": "PRESENT",
      "hasPermission": false,
      "recordedByAccountId": 45
    },
    {
      "enrollmentId": 2,
      "status": "ABSENT",
      "hasPermission": true,
      "recordedByAccountId": 45
    }
  ]
}
```

### facility

```json
{
  "branchId": 1,
  "roomId": 1,
  "name": "Máy chiếu projector",
  "type": "EQUIPMENT",
  "quantity": 1,
  "condition": "GOOD",
  "purchaseDate": "2026-01-15T09:00:00",
  "status": "ACTIVE"
}
```

### exam

```json
{
  "schoolClassId": 1,
  "name": "Kiểm tra giữa kỳ",
  "type": "MIDTERM",
  "status": "DRAFT",
  "startAt": "2026-10-15T09:00:00",
  "endAt": "2026-10-15T10:30:00"
}
```

### document

```json
{
  "title": "Lesson Plan Week 1",
  "fileUrl": "https://storage.example.com/teaching/lesson-plan-1.pdf",
  "schoolClassId": 1,
  "courseId": 1,
  "uploadedAt": "2026-10-20T08:30:00",
  "uploadedByAccountId": 45,
  "reviewedAt": "2026-10-21T09:00:00",
  "status": "PENDING",
  "reviewedByAccountId": 10,
  "rejectReason": null,
  "fileType": "PDF",
  "description": "Tài liệu cho buổi học đầu tiên"
}
```

### document review (nếu có review workflow)

```json
{
  "status": "APPROVED",
  "reviewedByAccountId": 10,
  "reviewedAt": "2026-10-21T09:00:00",
  "rejectReason": null
}
```

## Giả định triển khai

1. Teaching service dựa trên branch-based access control như academic-service.
2. `Room` thuộc chi nhánh, `Schedule` gắn với `SchoolClass`.
3. `Attendance` liên kết với `Enrollment` và `Account` để lưu người ghi nhận.
4. `Facility` thuộc phòng và chi nhánh; có trạng thái hoạt động / bảo trì / hỏng.
5. `Exam` và `Document` có thể gắn với `schoolClassId` hoặc `courseId` tùy nghiệp vụ.
6. Dữ liệu điểm danh cần kiểm tra quyền của giáo viên, chỉ cho phép ghi điểm danh lớp mình dạy.
7. `Schedule` cần kiểm tra xung đột lịch theo giáo viên, phòng học, lớp học.
8. Tài liệu giảng dạy nên có workflow review duyệt trước khi công khai.

## Giới hạn hiện tại

Service hiện mới chỉ có lớp entity và cấu hình Spring Boot, chưa có controller/service/repository đầy đủ như academic-service. README này được viết theo hướng spec nghiệp vụ và chuẩn hóa API để bạn tiến hành triển khai tiếp theo theo đúng mô hình microservice.
