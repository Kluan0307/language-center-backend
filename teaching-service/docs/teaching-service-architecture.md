> Historical reference. See [current contracts](../../docs/api-contracts.md) and [decisions](../../docs/implementation-decisions.md).

# Teaching Service Architecture

## Mục tiêu

Teaching Service theo mô hình của academic-service, chia theo từng domain nghiệp vụ riêng biệt và dùng các lớp chuẩn:

- Entity
- Repository
- DTO / Request-Response
- Service
- Controller
- Common utilities
- Security

## Cấu trúc đề xuất

```text
teaching-service/
├── src/
│   ├── main/
│   │   ├── java/com/example/teaching/
│   │   │   ├── TeachingServiceApplication.java
│   │   │   ├── attendance/
│   │   │   │   ├── Attendance.java
│   │   │   │   ├── AttendanceController.java
│   │   │   │   ├── AttendanceDto.java
│   │   │   │   ├── AttendanceRepository.java
│   │   │   │   └── AttendanceService.java
│   │   │   ├── common/
│   │   │   │   ├── ApiException.java
│   │   │   │   ├── BaseEntity.java
│   │   │   │   ├── GlobalExceptionHandler.java
│   │   │   │   └── PageResponse.java
│   │   │   ├── document/
│   │   │   │   ├── Document.java
│   │   │   │   ├── DocumentController.java
│   │   │   │   ├── DocumentDto.java
│   │   │   │   ├── DocumentRepository.java
│   │   │   │   └── DocumentService.java
│   │   │   ├── exam/
│   │   │   │   ├── Exam.java
│   │   │   │   ├── ExamController.java
│   │   │   │   ├── ExamDto.java
│   │   │   │   ├── ExamRepository.java
│   │   │   │   └── ExamService.java
│   │   │   ├── facility/
│   │   │   │   ├── Facility.java
│   │   │   │   ├── FacilityController.java
│   │   │   │   ├── FacilityDto.java
│   │   │   │   ├── FacilityRepository.java
│   │   │   │   └── FacilityService.java
│   │   │   ├── room/
│   │   │   │   ├── Room.java
│   │   │   │   ├── RoomController.java
│   │   │   │   ├── RoomDto.java
│   │   │   │   ├── RoomRepository.java
│   │   │   │   └── RoomService.java
│   │   │   ├── schedule/
│   │   │   │   ├── Schedule.java
│   │   │   │   ├── ScheduleController.java
│   │   │   │   ├── ScheduleDto.java
│   │   │   │   ├── ScheduleRepository.java
│   │   │   │   ├── ScheduleService.java
│   │   │   │   └── ScheduleConflictService.java
│   │   │   ├── security/
│   │   │   │   ├── CurrentUser.java
│   │   │   │   ├── JwtAuthenticationFilter.java
│   │   │   │   ├── Role.java
│   │   │   │   └── SecurityConfig.java
│   │   │   └── ...
│   │   └── resources/
│   │       └── application.properties
│   └── test/java/com/example/teaching/
├── docs/
│   ├── teaching-service-architecture.md
│   ├── teaching-service-api.md
│   └── teaching-service-domain-model.md
├── README.md
├── pom.xml
└── mvnw
```

## Quy tắc mô hình

1. Tất cả domain theo package riêng, không gom chung trong `entity`.
2. `Repository` kế thừa `JpaRepository` và dùng query theo branchId, teacherId, roomId, schoolClassId.
3. `Service` kiểm tra quyền theo JWT: `SUPER_ADMIN`, `ADMIN`, `STAFF`, `TEACHER`.
4. `Controller` nhận `@RequestMapping("/api/v1/teaching")` và route theo domain.
5. `Dto` dùng pattern `Request` / `Response` hoặc record-based mapping.
6. `ApiException` thống nhất 400, 401, 403, 404, 409.

## Lưu ý

Cấu trúc này đã được chuẩn hóa theo phong cách của academic-service, trong khi vẫn giữ nguyên nghiệp vụ teaching-service hiện có.
