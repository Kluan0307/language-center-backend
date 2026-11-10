# Tài liệu tham khảo và nguồn kỹ thuật

Các trang dưới đã mở kiểm tra ngày **10/11/2026**. Chỉ dùng để giải thích khái niệm; phiên bản dependencies của ứng dụng lấy từ POM/package-lock/source, không suy theo trang "latest". Chưa định dạng IEEE/APA theo mẫu khoa; cần chuẩn hóa trước nộp. Không tạo tác giả, DOI, năm xuất bản hoặc khảo sát không có nguồn.

<a id="r1"></a>
## R1 — Oracle, JDK21 Documentation

[JDK21 Documentation](https://docs.oracle.com/en/java/javase/21/). Dùng làm nguồn nền tảng Java; không khẳng định21 là bản mới nhất hoặc các tính năng như Virtual Threads đã bật trong ứng dụng.

<a id="r2"></a>
## R2 — Spring Framework, Transaction-bound Events

[Transaction-bound Events](https://docs.spring.io/spring-framework/reference/data-access/transaction/event.html). Giải thích callback theo phase transaction, gồm AFTER_COMMIT. Giao thức REQUIRES_NEW/đồng bộ trong dự án được xác định riêng từ TuitionListener/TuitionDeliveryService.

<a id="r3"></a>
## R3 — MySQL8.0 Reference Manual, Locking Reads

[InnoDB Locking Reads](https://dev.mysql.com/doc/refman/8.0/en/innodb-locking-reads.html). Nguồn cho SELECT FOR UPDATE và transaction; không coi cú pháp này tự bảo đảm đúng mọi luồng nếu thiếu guard hoặc thứ tự khóa tương thích.

<a id="r4"></a>
## R4 — React, Quick Start

[React Quick Start](https://react.dev/learn). Nguồn về component/state và tương tác; phiên bản trang hiện hành không thay phiên bản package-lock của đồ án.

<a id="r5"></a>
## R5 — TypeScript, Handbook

[TypeScript Handbook](https://www.typescriptlang.org/docs/handbook/intro.html). Nguồn về kiểm tra kiểu; không thay validation dữ liệu HTTP tại runtime.

<a id="r6"></a>
## R6 — PlantUML, Sequence Diagram

[Sequence Diagram syntax and features](https://plantuml.com/sequence-diagram). Dùng cho ký pháp participant/message/nhánh; render thành công không tự xác minh nghiệp vụ.

## Nguồn nội bộ có thể kiểm tra

[POM](../../../pom.xml), [package.json](../../../../../language-center-web-frontend/package.json), [API](../../api-list.md), [metadata V013](../design/schema-v013.metadata.json), [truy vết](../traceability.md), [báo cáo nghiệm thu](../../reviews/2026-11-10-g17-g18-real-acceptance.md), [manifest](../../reviews/2026-11-10-g17-g18-evidence.json). Các log/ảnh là bằng chứng thực nghiệm của đồ án, không phải tài liệu khảo sát bên ngoài.
