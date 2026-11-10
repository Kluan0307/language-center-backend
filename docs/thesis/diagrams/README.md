# Nguồn sơ đồ và bản render — G13/G14/G15

Cập nhật10/11/2026: cả10Activity/Sequence đã sửa theo source và render lại. PlantUML nguồn UTF-8 có thể sửa; SVG render bằng PlantUML 1.2025.10, layout Smetana (không cần Graphviz). Mỗi file độc lập, không remote include.

## Use Case — G13

Đọc kèm [ma trận quyền](../role-permissions.md) và [đặc tả](../use-cases.md); đường nối nhóm không cấp mọi thao tác. Không dùng actor inheritance làm A có mọi quyền T.

| Sơ đồ | Nguồn | SVG | Caption gợi ý |
|---|---|---|---|
| Tổng quan bốn role và vận hành ngoài RBAC | [PlantUML](use-case-overview.puml) | [Xem SVG](use-case-overview.svg) | Phạm vi tham gia Use Case của Tổng quan bốn role và vận hành ngoài RBAC; quyền chi tiết theo thao tác/chi nhánh. |
| SUPER_ADMIN | [PlantUML](use-case-super-admin.puml) | [Xem SVG](use-case-super-admin.svg) | Phạm vi tham gia Use Case của SUPER_ADMIN; quyền chi tiết theo thao tác/chi nhánh. |
| ADMIN | [PlantUML](use-case-admin.puml) | [Xem SVG](use-case-admin.svg) | Phạm vi tham gia Use Case của ADMIN; quyền chi tiết theo thao tác/chi nhánh. |
| STAFF | [PlantUML](use-case-staff.puml) | [Xem SVG](use-case-staff.svg) | Phạm vi tham gia Use Case của STAFF; quyền chi tiết theo thao tác/chi nhánh. |
| TEACHER | [PlantUML](use-case-teacher.puml) | [Xem SVG](use-case-teacher.svg) | Phạm vi tham gia Use Case của TEACHER; quyền chi tiết theo thao tác/chi nhánh. |

## ERD và Class Diagram — G14

ERD vật lý lấy đúng FK/nullability/unique từ information_schema target V013; lớp/entity lấy source, không vẽ DTO thành bảng. ERD logic chỉ quan hệ nghiệp vụ, đường chấm không khẳng định FK. Đường trong Class Diagram có nghĩa khác, xem legend từng hình.

| Sơ đồ | Nguồn | SVG | Dùng trong báo cáo |
|---|---|---|---|
| class-academic-catalog | [PlantUML](class-academic-catalog.puml) | [SVG](class-academic-catalog.svg) | Chi tiết module; dùng khổ lớn hoặc phụ lục, không thu nhỏ toàn bộ cột vào một trang A4 |
| class-academic-learning | [PlantUML](class-academic-learning.puml) | [SVG](class-academic-learning.svg) | Chi tiết module; dùng khổ lớn hoặc phụ lục, không thu nhỏ toàn bộ cột vào một trang A4 |
| class-academic-placement | [PlantUML](class-academic-placement.puml) | [SVG](class-academic-placement.svg) | Chi tiết module; dùng khổ lớn hoặc phụ lục, không thu nhỏ toàn bộ cột vào một trang A4 |
| class-auth | [PlantUML](class-auth.puml) | [SVG](class-auth.svg) | Chi tiết module; dùng khổ lớn hoặc phụ lục, không thu nhỏ toàn bộ cột vào một trang A4 |
| class-finance | [PlantUML](class-finance.puml) | [SVG](class-finance.svg) | Chi tiết module; dùng khổ lớn hoặc phụ lục, không thu nhỏ toàn bộ cột vào một trang A4 |
| class-overview | [PlantUML](class-overview.puml) | [SVG](class-overview.svg) | Hình tổng quan; chi tiết thuộc module bên dưới |
| class-shared-coordination | [PlantUML](class-shared-coordination.puml) | [SVG](class-shared-coordination.svg) | Chi tiết module; dùng khổ lớn hoặc phụ lục, không thu nhỏ toàn bộ cột vào một trang A4 |
| class-teaching | [PlantUML](class-teaching.puml) | [SVG](class-teaching.svg) | Chi tiết module; dùng khổ lớn hoặc phụ lục, không thu nhỏ toàn bộ cột vào một trang A4 |
| erd-academic-catalog | [PlantUML](erd-academic-catalog.puml) | [SVG](erd-academic-catalog.svg) | Chi tiết module; dùng khổ lớn hoặc phụ lục, không thu nhỏ toàn bộ cột vào một trang A4 |
| erd-academic-learning | [PlantUML](erd-academic-learning.puml) | [SVG](erd-academic-learning.svg) | Chi tiết module; dùng khổ lớn hoặc phụ lục, không thu nhỏ toàn bộ cột vào một trang A4 |
| erd-academic-placement | [PlantUML](erd-academic-placement.puml) | [SVG](erd-academic-placement.svg) | Chi tiết module; dùng khổ lớn hoặc phụ lục, không thu nhỏ toàn bộ cột vào một trang A4 |
| erd-auth | [PlantUML](erd-auth.puml) | [SVG](erd-auth.svg) | Chi tiết module; dùng khổ lớn hoặc phụ lục, không thu nhỏ toàn bộ cột vào một trang A4 |
| erd-finance | [PlantUML](erd-finance.puml) | [SVG](erd-finance.svg) | Chi tiết module; dùng khổ lớn hoặc phụ lục, không thu nhỏ toàn bộ cột vào một trang A4 |
| erd-logical | [PlantUML](erd-logical.puml) | [SVG](erd-logical.svg) | Mô hình nghiệp vụ, không dùng làm cam kết FK |
| erd-physical-overview | [PlantUML](erd-physical-overview.puml) | [SVG](erd-physical-overview.svg) | Hình tổng quan; chi tiết thuộc module bên dưới |
| erd-shared-coordination | [PlantUML](erd-shared-coordination.puml) | [SVG](erd-shared-coordination.svg) | Chi tiết module; dùng khổ lớn hoặc phụ lục, không thu nhỏ toàn bộ cột vào một trang A4 |
| erd-teaching | [PlantUML](erd-teaching.puml) | [SVG](erd-teaching.svg) | Chi tiết module; dùng khổ lớn hoặc phụ lục, không thu nhỏ toàn bộ cột vào một trang A4 |

## Activity và Sequence Diagrams — G15

Đặc tả động 5 luồng nghiệp vụ cốt lõi, làm rõ Actor, Frontend, Gateway, Microservice, ranh giới Local Transaction, kiểm tra quyền, khóa bi quan/lạc quan, cơ chế chống trùng lặp (Idempotency) và chính sách xử lý lỗi/timeout thực tế. Đọc kèm [tài liệu luồng động](../design/dynamic-workflows.md).

| Nhóm luồng | Activity Diagram | Sequence Diagram | Nội dung chính |
|---|---|---|---|
| 1. Ghi danh & tạo học phí | [PlantUML](activity-enrollment-tuition.puml) · [SVG](activity-enrollment-tuition.svg) | [PlantUML](sequence-enrollment-tuition.puml) · [SVG](sequence-enrollment-tuition.svg) | Local Tx 1 tại Academic, sự kiện sau commit gọi Finance HTTP, trạng thái PENDING/CREATED/FAILED, nút thử lại thủ công (không distributed transaction) |
| 2. Thu tiền & chống trùng | [PlantUML](activity-payment-retry.puml) · [SVG](activity-payment-retry.svg) | [PlantUML](sequence-payment-retry.puml) · [SVG](sequence-payment-retry.svg) | Pessimistic write lock trên Tuition, kiểm tra idempotency qua `requestKey`, tính số dư còn nợ qua sổ cái, sinh receiptNo |
| 3. Ngoại lệ phân lớp | [PlantUML](activity-placement-exception.puml) · [SVG](activity-placement-exception.svg) | [PlantUML](sequence-placement-exception.puml) · [SVG](sequence-placement-exception.svg) | Quy trình 3 bước: Preview -> Request ngoại lệ (Staff) -> Decision (Admin) -> Tiêu thụ nguyên tử (Atomic Consume) khi ghi danh |
| 4. Bàn giao giáo viên | [PlantUML](activity-teacher-handover.puml) · [SVG](activity-teacher-handover.svg) | [PlantUML](sequence-teacher-handover.puml) · [SVG](sequence-teacher-handover.svg) | Kiểm tra trách nhiệm học vụ qua HTTP, khóa hàng Account, chính sách Fail-Closed khi Academic lỗi, kiểm soát race condition |
| 5. Chốt/mở kết quả cuối khóa | [PlantUML](activity-final-result-lock.puml) · [SVG](activity-final-result-lock.svg) | [PlantUML](sequence-final-result-lock.puml) · [SVG](sequence-final-result-lock.svg) | Rào cản `ResultBarrierService`, snapshot kết quả, chặn sửa đổi điểm (409 Conflict), mở lại có kiểm toán qua `result_reopens` |

## Tái sinh và kiểm tra

1. Khi schema/code thay đổi, lấy metadata mới theo quy trình **chỉ đọc**; không lấy dữ liệu nghiệp vụ hoặc sửa database để khớp tài liệu. Schema JSON hiện được chốt ở V013, source database thật còn thiếu nhiều thay đổi.
2. Tại backend root: `node scripts/generate-thesis-models.cjs`. Script chỉ đọc source/metadata và sinh từ điển/mapping/diagram, không kết nối MySQL hoặc sửa source ứng dụng. Sơ đồ Use Case và Activity/Sequence là nguồn biên tập thiết kế động.
3. Với10sơ đồ động: node scripts/render-dynamic-diagrams.cjs kiểm cú pháp/render và cập nhật đúng10entry trong manifest. Với các hình khác, render và cập nhật manifest tương ứng trước kiểm tra:
   - Cú pháp/kiểm tra: `& "C:\Program Files\Java\jdk-21\bin\java.exe" -jar storage/tools/plantuml-1.2025.10.jar -charset UTF-8 -checkonly "docs/thesis/diagrams/*.puml"`
   - Xuất SVG: `& "C:\Program Files\Java\jdk-21\bin\java.exe" -jar storage/tools/plantuml-1.2025.10.jar -charset UTF-8 -tsvg "docs/thesis/diagrams/*.puml"`
4. `node scripts/check-thesis-docs.cjs --report` kiểm tra liên kết, coverage bảng/cột/entity, ảnh/source tương ứng (32 sơ đồ canonical), hash source entity và mapping.

[Thiết kế và phạm vi](../design/README.md) · [Luồng động chi tiết](../design/dynamic-workflows.md) · [Domain model alias](../../domain-model.puml).
