# teaching-service

Kiểm tra, điểm và tài liệu. Cổng **8083**, prefix `/api/v1/teaching`; hiện có **14 API nghiệp vụ chính**.

## Tra cứu

- [Toàn bộ endpoint và quyền](../docs/api-list.md#4-teaching--14-api)
- [Body và response](../docs/api-contracts.md)
- [Hướng dẫn chạy/cấu hình toàn dự án](../README.md)
- [Migration dữ liệu cũ](../docs/migrations/README.md)

## Nhóm API

- Exam: 5 API; Grade và đánh giá: 4 API.
- Document: 5 API, gồm upload, review, metadata và download.
- Phòng, lịch và điểm danh dùng academic, không có CRUD tương ứng tại teaching.

Các path rút gọn ở trên ghép với prefix của service. Qua gateway dùng cổng 8080 và giữ nguyên path.

## Quyền và phụ thuộc

SA/A/T dùng Exam/Grade; A/SA chỉ sửa điểm đã có, bắt buộc lý do. S chỉ xem/tải tài liệu APPROVED; T chỉ tài liệu mình đăng.

Phụ thuộc: Academic để xác minh lớp, học viên và khóa được phân công.

## Chạy và kiểm thử

Chạy từ thư mục root, sau khi cấu hình `.env.local` theo README chính:

```powershell
.\scripts\run-service.ps1 teaching-service
mvn -f teaching-service/pom.xml test
```

Test dùng H2/mock/HTTP stub, không dùng MySQL hiện tại.

## Frontend giai đoạn 5

Đã nối bài kiểm tra, bảng điểm/lịch sử hiệu chỉnh và tài liệu upload/duyệt/download.
Điểm danh/lịch sử/chuyên cần dùng Academic, bao gồm Teacher theo lớp được phân công.
Xem [hướng dẫn và kết quả kiểm tra](../../../language-center-web-frontend/docs/phase-five.md).
Teaching có 8 test đạt, gồm hai người duyệt tài liệu đồng thời. Luồng giao diện đã được
kiểm tra trên MySQL bằng fixture riêng. Kho file phải được sao lưu cùng database.

## Cải thiện 4

Roster thi theo ngày, snapshot khi Teacher lưu điểm đầu tiên, giữ điểm cũ và chặn sửa ngày thi đã chấm. [Hướng dẫn/API](../docs/improvements/phase-four-learning-lifecycle.md).

## Cải thiện 6

Xem [lead, revision tài liệu và guard phân công](../docs/improvements/phase-six-care-documents-experience.md). API ghi mới yêu cầu version; áp dụng V006 trước khi chạy.

## G10 — quy tắc và preview

Package evaluation lưu policy revisions/state/rule snapshots, tính theo enrollment/ngày học/chuyên cần và nguồn Academic API. API lớp mới cho SA/A/T chính; SA/A mới xác nhận policy, report cũ SA/A trả DTO provisional mới. Exam.version, candidate.enrollmentId bổ sung; source writes lock state trước exam, không đổi thang bài trong currentpolicy ngầm. [G10](../docs/thesis-progress/G10.md), [V012](../docs/migrations/V012__class_evaluation_policies.sql), [hợp đồng](../docs/api-contracts.md). Không có finalCONFIRMED/autoAcademicCompletion.
