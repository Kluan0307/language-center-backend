# academic-service

Vận hành trung tâm. Cổng **8082**, prefix `/api/v1/academic`; hiện có **55 API nghiệp vụ chính**.

## Tra cứu

- [Toàn bộ endpoint và quyền](../docs/api-list.md#2-academic--55-api)
- [Body và response](../docs/api-contracts.md)
- [Hướng dẫn chạy/cấu hình toàn dự án](../README.md)
- [Migration dữ liệu cũ](../docs/migrations/README.md)

## Nhóm API

- Chi nhánh 5; khóa học 5; phòng 6; học viên 6; Lead 6 API.
- Lớp 9; lịch 6; đăng ký/chuyển lớp/học phí retry 7 API.
- Điểm danh 4; thống kê vận hành 1 API.

Các path rút gọn ở trên ghép với prefix của service. Qua gateway dùng cổng 8080 và giữ nguyên path.

## Quyền và phụ thuộc

A/S/T bị ép chi nhánh từ token; T chỉ lớp được phân công. Staff không tạo khóa/đổi giá, sửa lịch hoặc đổi trạng thái lớp.

Phụ thuộc: Auth để xác minh giáo viên; Finance để tạo học phí sau commit.

## Chạy và kiểm thử

Chạy từ thư mục root, sau khi cấu hình `.env.local` theo README chính:

```powershell
.\scripts\run-service.ps1 academic-service
mvn -f academic-service/pom.xml test
```

Test dùng H2/mock/HTTP stub, không dùng MySQL hiện tại.


## Cải thiện 4

Vòng đời hồ sơ/ghi danh có version và lịch sử; bảo lưu có ngày dự kiến và học lại/chuyển lớp giữ root học phí. Thêm roster lớp theo ngày. [Quy tắc, API và V004](../docs/improvements/phase-four-learning-lifecycle.md).

## Cải thiện 6

Xem [lead, revision tài liệu và guard phân công](../docs/improvements/phase-six-care-documents-experience.md). API ghi mới yêu cầu version; áp dụng V006 trước khi chạy.

## G03 — trách nhiệm giáo viên

GET /api/v1/academic/teachers/{id}/responsibilities?branchId= chỉ SA/A, gồm lớp chưa kết thúc và mọi buổi SCHEDULED (cả quá ngày/dạy thay). Admin cần branchId thuộc phân công; SA có thể tìm toàn hệ thống. Mọi đường phân công giữ TeacherDirectory lock và xác nhận khả dụng sau lock. Upstream400 được trả là lỗi nghiệp vụ400 thay vì503. [Bàn giao và giới hạn shared DB](../docs/thesis-progress/G03.md).

## G04–G05 (08/11/2026)

Thêm packages admission/assessment và 20 API: [API](../docs/api-list.md), [hợp đồng](../docs/api-contracts.md), [G04](../docs/thesis-progress/G04.md), [G05](../docs/thesis-progress/G05.md). Danh mục toàn hệ thống do SA quản lý, điều kiện khóa do SA/A theo phạm vi; đánh giá do SA/A/S theo học viên. Các chuỗi legacy giữ nguyên.

Schema cần V008/V009 sau V007 trước prod/validate. Class save liên kết policy bất biến lúc tạo/đổi khóa; đổi policy khóa không tự sửa lớp. Student delete bảo vệ lịch sử assessment. Chưa enforce placement eligibility (G06/G07).

## G06/G07 — placement gate

Package placement dùng chung evaluate cho preview và lưu. EnrollmentService gọi gate ở enroll/transfer/resume, capture và consume cùng transaction, giữ tuition/billing hiện có. SessionPlanner.enrollmentConflict trả dữ liệu để preview không rollback-only. V010 cần trước prod/validate; không có cờ bypass hoặc auto-mapping legacy. [API](../docs/api-list.md), [G06](../docs/thesis-progress/G06.md), [G07](../docs/thesis-progress/G07.md).

## G08/G09

Package duration: immutable CourseDurationPlan/ClassDurationPlan, projection expectedMinutes/revision và DurationProgressService. 8API thêm, Academic107. Batch progress dùng2query cho tập lớp; clock phải phút chính xác, null là chưa biết. Course.duration legacy nullable và không suy ra đơn vị; expectedMinutes chuẩn1–600000. Manual FINISHED phải đủ căn cứ giờ và không còn SCHEDULED trước sync, không tự hủy buổi để đóng. [G08](../docs/thesis-progress/G08.md), [G09](../docs/thesis-progress/G09.md), [V011](../docs/migrations/V011__verified_duration_plans.sql). G10/G11 tiếp tục guard kết quả/enrollment.

## G10 — evaluation-context

GET /classes/{id}/evaluation-context chỉ SA/A/T lớp chính, includeAttendance tùy chọn; trả enrollment/period/sessionCOMPLETED/marks/duration, không tài chính. DatedRoster bổ sung enrollmentId/source cho freezeTeaching; legacyfallback/ambiguous không bịa mapping. Nâng Academic trước/cùng TeachingV012. [G10](../docs/thesis-progress/G10.md).
