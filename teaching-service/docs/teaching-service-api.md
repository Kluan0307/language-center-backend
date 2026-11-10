> Historical reference. See [current contracts](../../docs/api-contracts.md) and [decisions](../../docs/implementation-decisions.md).

# Teaching Service API Contract

## Base URL

`/api/v1/teaching`

## Auth

- Header: `Authorization: Bearer <token>`
- JWT cần cùng cấu trúc với auth-service: `sub`, `role`, `accountId`, `profileId`, `branchId`
- JWT secret và expiration phải đồng bộ với auth-service.

## Quy tắc security

- `SUPER_ADMIN`: truy cập tất cả chi nhánh
- `ADMIN`, `STAFF`: truy cập theo `branchId`
- `TEACHER`: chỉ truy cập lịch học và điểm danh thuộc profileId của mình

## Endpoint chính

### Room

| Method | Path | Role | Mô tả |
|---|---|---|---|
| GET | `/rooms` | SA/A/S/T | Danh sách phòng |
| GET | `/rooms/{roomId}` | SA/A/S/T | Chi tiết phòng |
| POST | `/rooms` | SA/A/S | Tạo phòng |
| PUT | `/rooms/{roomId}` | SA/A/S | Cập nhật phòng |
| PATCH | `/rooms/{roomId}/status` | SA/A/S | Đổi trạng thái |
| DELETE | `/rooms/{roomId}` | SA/A/S | Xóa phòng |

### Schedule

| Method | Path | Role | Mô tả |
|---|---|---|---|
| GET | `/schedules` | SA/A/S/T | Danh sách lịch |
| GET | `/schedules/my` | T | Lịch của giáo viên đăng nhập |
| POST | `/schedules` | SA/A | Tạo lịch |
| PUT | `/schedules/{scheduleId}` | SA/A | Cập nhật lịch |
| DELETE | `/schedules/{scheduleId}` | SA/A | Xóa lịch |
| POST | `/schedules/check-conflict` | SA/A | Kiểm tra xung đột |

### Attendance

| Method | Path | Role | Mô tả |
|---|---|---|---|
| GET | `/attendances/sheet` | SA/A/S/T | Sheet điểm danh theo ngày |
| POST | `/attendances/bulk` | SA/A/S/T | Điểm danh hàng loạt |
| GET | `/attendances/history` | SA/A/S/T | Lịch sử điểm danh |
| GET | `/attendances/summary` | SA/A/S/T | Tổng hợp điểm danh |

### Facility

| Method | Path | Role | Mô tả |
|---|---|---|---|
| GET | `/facilities` | SA/A/S | Danh sách thiết bị |
| GET | `/facilities/{facilityId}` | SA/A/S | Chi tiết thiết bị |
| POST | `/facilities` | SA/A/S | Tạo thiết bị |
| PUT | `/facilities/{facilityId}` | SA/A/S | Sửa thiết bị |
| PATCH | `/facilities/{facilityId}/status` | SA/A/S | Đổi trạng thái |

### Exam

| Method | Path | Role | Mô tả |
|---|---|---|---|
| GET | `/exams` | SA/A/S/T | Danh sách bài kiểm tra |
| GET | `/exams/{examId}` | SA/A/S/T | Chi tiết bài kiểm tra |
| POST | `/exams` | SA/A/S | Tạo bài kiểm tra |
| PUT | `/exams/{examId}` | SA/A/S | Sửa bài kiểm tra |
| PATCH | `/exams/{examId}/status` | SA/A/S | Đổi trạng thái |

### Document

| Method | Path | Role | Mô tả |
|---|---|---|---|
| GET | `/documents` | SA/A/S/T | Danh sách tài liệu |
| GET | `/documents/{documentId}` | SA/A/S/T | Chi tiết tài liệu |
| POST | `/documents` | SA/A/S/T | Upload tài liệu |
| PATCH | `/documents/{documentId}/review` | SA/A/S | Duyệt / từ chối |
| DELETE | `/documents/{documentId}` | SA/A/S | Xóa tài liệu |

## Response chuẩn

```json
{
  "message": "..."
}
```

## Mã lỗi chuẩn

- 400: dữ liệu không hợp lệ / nghiệp vụ sai
- 401: chưa đăng nhập / token sai
- 403: không đủ quyền
- 404: không tìm thấy dữ liệu
- 409: xung đột dữ liệu / duplicate / business rule
