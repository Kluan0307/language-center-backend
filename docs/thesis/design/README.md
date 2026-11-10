# Mô hình thiết kế hệ thống — G14/G15

Ngày 09/11/2026; **as-built target V013**, không phải khảo sát thực tế và không khẳng định database đang dùng đã nâng xong.

| Tài liệu | Nội dung |
|---|---|
| [Thiết kế động & Luồng quan trọng](dynamic-workflows.md) | **G15**: Đặc tả 5 luồng cốt lõi: Actor, FE, Gateway, Local Tx, Khóa bi quan, Idempotency, Fail-Closed |
| [Từ điển dữ liệu](data-dictionary.md) | 53 bảng/625 cột: kiểu, nullable/default, PK/FK/index/unique/generated và ghi chú nghiệp vụ |
| [Danh mục entity](entity-catalog.md) | 52 lớp @Entity; Java fields/JPA, kế thừa và hai embeddable/collection tables |
| [Sơ đồ nguồn/SVG](../diagrams/README.md) | 32 sơ đồ canonical: 5 Use Case, 9 ERD, 8 Class, 5 Activity, 5 Sequence |
| [Đối chiếu schema](schema-verification.md) | Database hiện tại 33 bảng vs target 53; chênh 20 bảng/11 cột/1 nullable; các giới hạn kiểm chứng |
| [Version và vòng đời](lifecycles-and-invariants.md) | State riêng biệt, snapshot, 13 lớp @Version, manual revision và journal |
| [Metadata V013](schema-v013.metadata.json) | information_schema bản sao, không dòng nghiệp vụ |
| [Metadata hiện tại](schema-current.metadata.json) | information_schema ứng dụng hiện tại, chỉ đọc |
| [Mapping source](entity-mapping.json) | Owner, source path/hash, field/association/embeddable; không Java AST tổng quát |
| [Kết quả đối chiếu](model-validation.json) | Coverage và scalar FK/generation; kiểm tên cột, không chứng nhận mọi JPA/schema constraint |
| [Migration fingerprints](migration-manifest.json) | V001–V013 tại thời điểm lập tài liệu, không execution ledger |

## Kiến trúc và cách đọc

- Auth sở hữu logic 9 bảng, Academic 28, Teaching 11, Finance 4; metadata phối hợp chung 1 bảng. Tổng 53 bảng. API Gateway định tuyến/security/health, không có bảng nghiệp vụ trong mô hình này.
- Tất cả cùng MySQL schema trong cấu hình hiện tại. Owner logic không có nghĩa database-per-service; thông tin giữa service đi qua API/ID, với hai ngoại lệ khóa hẹp account và class_result_barriers đã có. Không vẽ payment gateway, LMS, học viên login hoặc DB riêng chưa triển khai.
- **ERD vật lý:** đường liền chỉ FK MySQL có thật; index không phải FK. Nullable/unique quyết định cardinality. Hai bảng collection không PK được thể hiện đúng, không tự thêm id.
- **Class Diagram:** đường object association chỉ JPA, thoi cho ElementCollection; scalar ID không được biến thành object association. Một scalar vẫn có thể có FK do migration. DTO/report/projection/filter/controller không là entity. Class tổng quan rút gọn fields; danh mục và diagram module giữ fields đầy đủ.
- **ERD logic:** mô tả nghiệp vụ theo liên kết ID/HTTP và scope, không dùng thay bản vẽ vật lý. Snapshot tên/mức/quy tắc có chủ đích tách nguồn đang thay đổi.
- **Activity & Sequence (G15):** Mô tả đúng ranh giới Local Transaction và tương tác HTTP độc lập. Không vẽ 2-Phase Commit (2PC) hay tự động retry ngầm nếu mã nguồn không có. Thể hiện chính sách Fail-Closed khi kiểm tra trách nhiệm giáo viên và cơ chế rào cản bất biến khi chốt kết quả.

Các SVG module đầy đủ khá lớn; chọn tổng quan làm hình chương thiết kế, dùng module/phụ lục và từ điển cho chi tiết, giữ source để phóng to/in khổ phù hợp. [Bàn giao G14](../../thesis-progress/G14.md) · [Bàn giao G15](../../thesis-progress/G15.md).
