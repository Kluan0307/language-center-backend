> Historical reference. See [current contracts](../../docs/api-contracts.md) and [decisions](../../docs/implementation-decisions.md).

# Teaching Service Domain Model

## Core entities

### Room
- `id`
- `code`
- `name`
- `floor`
- `capacity`
- `status`
- `branchId`

### Schedule
- `id`
- `schoolClassId`
- `roomId`
- `teacherAccountId`
- `dayOfWeek`
- `startTime`
- `endTime`
- `branchId`

### Attendance
- `id`
- `sessionDate`
- `schoolClassId`
- `enrollmentId`
- `status`
- `hasPermission`
- `recordedByAccountId`

### Facility
- `id`
- `branchId`
- `roomId`
- `name`
- `type`
- `quantity`
- `condition`
- `purchaseDate`
- `status`

### Exam
- `id`
- `schoolClassId`
- `name`
- `type`
- `status`
- `startAt`
- `endAt`

### Document
- `id`
- `title`
- `fileUrl`
- `schoolClassId`
- `courseId`
- `uploadedByAccountId`
- `status`
- `reviewedByAccountId`
- `reviewedAt`
- `rejectReason`
- `fileType`
- `description`

## Mối quan hệ chính

- `Room` thuộc một chi nhánh (`branchId`)
- `Schedule` gắn với `SchoolClass`, `Room`, `TeacherAccount`
- `Attendance` liên kết với `Enrollment` và `Account`
- `Facility` thuộc phòng và chi nhánh
- `Exam` và `Document` có thể gắn với `schoolClassId` hoặc `courseId`

## Business rules

1. `Schedule` phải kiểm tra conflict theo giáo viên, phòng học và lớp học.
2. `Attendance` chỉ cho phép giáo viên ghi nhận lớp mình dạy.
3. `Document` cần review trước khi được công khai.
4. `Room` và `Facility` phải kiểm tra trạng thái hoạt động / bảo trì / hỏng.

## Gợi ý kỹ thuật

- Dùng `@ManyToOne` cho quan hệ với `Branch`, `SchoolClass`, `Account`, `Enrollment`.
- Dùng `@Enumerated(EnumType.STRING)` cho trạng thái.
- Dùng `ApiException` và `GlobalExceptionHandler` để chuẩn hóa lỗi theo academic-service.
