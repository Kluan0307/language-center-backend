# Finance service

Spring Boot 4.1.1 / Java 21 / MySQL 8. Base URL: `http://localhost:8084/api/v1/finance`.

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

Khởi chạy app:

```powershell
.\mvnw.cmd spring-boot:run
```

Hoặc nếu đã chuẩn bị biến môi trường và muốn đọc config từ auth-service:

```powershell
.\scripts\run-local.ps1 -AuthProperties ..\auth-service\src\main\resources\application.properties
```

Database `language_center_db` phải tồn tại và tài khoản DB có quyền tạo/sửa bảng finance. Finance service hiện mới chỉ là shell Spring Boot, nên đây là README theo định hướng thiết kế API và service nghiệp vụ cho giai đoạn tiếp theo.

## Mục tiêu service

Finance service chịu trách nhiệm quản lý doanh thu và chi phí của trung tâm:

- học phí và thanh toán học viên
- khuyến mãi và giảm giá
- chi phí vận hành / mua sắm / tài chính
- hóa đơn và bút toán
- báo cáo tài chính theo chi nhánh

Dù source hiện tại chưa có entity/controller đầy đủ, kiến trúc mong đợi của service sẽ tập trung vào các module:

- `Tuition`
- `Payment`
- `Expense`
- `Promotion`
- `Invoice`
- `Refund`

Trong dự án hiện tại đã có nhắc đến các khái niệm như `Tuition`, `Payment`, `Expense`, `Promotion`. Đây là dấu hiệu rõ ràng finance-service sẽ xử lý nghiệp vụ thu chi và báo cáo kết nối từ academic-service.

## Hợp đồng chung

- Header: `Authorization: Bearer <token>`.
- Claims: `sub`, `role`, `accountId`, `profileId`, `branchId`.
- JWT phải hợp lệ và tương thích với auth-service.
- ADMIN, STAFF, ACCOUNTANT bị ép theo `branchId` trong JWT.
- SUPER_ADMIN có thể xem dữ liệu nhiều chi nhánh.
- Dữ liệu tài chính không được thao tác lẫn lộn giữa các chi nhánh.
- Lỗi format chuẩn: `{"message":"..."}`.
- 400: dữ liệu đầu vào hoặc nghiệp vụ sai.
- 401: token không hợp lệ.
- 403: không có quyền.
- 404: không tìm thấy hóa đơn / khoản thu / chi phí / khuyến mãi.
- 409: trùng dữ liệu hoặc xung đột giao dịch.

## Endpoint đề xuất

Mọi path dưới đây bắt đầu bằng `/api/v1/finance`.

### Tuition

| Method | Path | Role | Mô tả |
|---|---|---|---|
| GET | `/tuition` | SA/A/S | Liệt kê khoản học phí |
| GET | `/tuition/{tuitionId}` | SA/A/S | Chi tiết học phí |
| POST | `/tuition` | SA/A/S | Tạo khoản học phí |
| PATCH | `/tuition/{tuitionId}/status` | SA/A/S | Cập nhật trạng thái |

### Payment

| Method | Path | Role | Mô tả |
|---|---|---|---|
| GET | `/payments` | SA/A/S | Liệt kê thanh toán |
| GET | `/payments/{paymentId}` | SA/A/S | Chi tiết thanh toán |
| POST | `/payments` | SA/A/S | Ghi nhận thanh toán |
| PATCH | `/payments/{paymentId}/status` | SA/A/S | Cập nhật trạng thái thanh toán |

### Expense

| Method | Path | Role | Mô tả |
|---|---|---|---|
| GET | `/expenses` | SA/A/S | Danh sách chi phí |
| GET | `/expenses/{expenseId}` | SA/A/S | Chi tiết chi phí |
| POST | `/expenses` | SA/A/S | Tạo chi phí |
| PUT | `/expenses/{expenseId}` | SA/A/S | Cập nhật chi phí |
| PATCH | `/expenses/{expenseId}/status` | SA/A/S | Đổi trạng thái |

### Promotion

| Method | Path | Role | Mô tả |
|---|---|---|---|
| GET | `/promotions` | SA/A/S | Danh sách khuyến mãi |
| GET | `/promotions/{promotionId}` | SA/A/S | Chi tiết khuyến mãi |
| POST | `/promotions` | SA/A | Tạo khuyến mãi |
| PUT | `/promotions/{promotionId}` | SA/A | Sửa khuyến mãi |
| PATCH | `/promotions/{promotionId}/status` | SA/A | Đổi trạng thái |

### Invoice

| Method | Path | Role | Mô tả |
|---|---|---|---|
| GET | `/invoices` | SA/A/S | Danh sách hóa đơn |
| GET | `/invoices/{invoiceId}` | SA/A/S | Chi tiết hóa đơn |
| POST | `/invoices` | SA/A/S | Tạo hóa đơn |
| PATCH | `/invoices/{invoiceId}/status` | SA/A/S | Cập nhật trạng thái hóa đơn |

### Report / Stats

| Method | Path | Role | Mô tả |
|---|---|---|---|
| GET | `/stats/overview` | SA/A/S | Tổng quan tài chính |
| GET | `/stats/by-branch` | SA/A | Báo cáo theo chi nhánh |
| GET | `/stats/by-period` | SA/A/S | Báo cáo theo thời gian |

## Body mẫu

### tuition

```json
{
  "studentId": 1,
  "classId": 1,
  "amount": 2500000,
  "currency": "VND",
  "dueDate": "2026-10-31",
  "status": "PENDING",
  "branchId": 1
}
```

### payment

```json
{
  "tuitionId": 1,
  "amount": 2500000,
  "paymentMethod": "BANK_TRANSFER",
  "referenceCode": "PAY-20261027-001",
  "paidAt": "2026-10-27T10:00:00",
  "status": "PAID"
}
```

### expense

```json
{
  "category": "OPERATING_COST",
  "title": "Tiền điện tháng 10",
  "amount": 3500000,
  "currency": "VND",
  "branchId": 1,
  "description": "Hóa đơn điện",
  "status": "APPROVED"
}
```

### promotion

```json
{
  "code": "EARLYBIRD10",
  "name": "Giảm 10% cho học viên đăng ký sớm",
  "discountType": "PERCENT",
  "discountValue": 10,
  "startDate": "2026-10-01",
  "endDate": "2026-10-31",
  "branchId": 1,
  "status": "ACTIVE"
}
```

### invoice

```json
{
  "studentId": 1,
  "paymentId": 1,
  "invoiceNumber": "INV-20261027-001",
  "totalAmount": 2500000,
  "status": "PAID"
}
```

## Giả định triển khai

1. Finance service phải phụ thuộc vào academic-service để lấy thông tin học viên, lớp học, khóa học.
2. Tiền học phí được tính dựa trên khóa học / lớp / học viên, không lưu trữ logic tính toán rời rạc tại nhiều nơi.
3. `Payment` có trạng thái: `PENDING`, `PAID`, `FAILED`, `REFUNDED`, `PARTIAL`.
4. `Expense` có phân loại theo: `OPERATING_COST`, `TEACHER_SALARY`, `MARKETING`, `UTILITY`, `ASSET`.
5. `Promotion` áp dụng theo chi nhánh và có thời hạn hiệu lực.
6. Tất cả giao dịch tài chính cần có `branchId` hoặc được suy ra từ `StudentClass` / `Enrollment`.
7. Hóa đơn nên là immutable sau khi xác nhận, chỉ cập nhật trạng thái thay vì sửa nội dung lớn.
8. Báo cáo tài chính nên hỗ trợ cả tổng quan và phân tách theo chi nhánh, kỳ, học kỳ.

## Giới hạn hiện tại

Finance service trong repo hiện mới chỉ là project skeleton Spring Boot, chưa có controller, DTO, entity, repository đầy đủ như academic-service. Do đó README này được xây dựng theo mô hình spec API chuẩn để team có thể triển khai đúng ngay từ đầu và phù hợp với kiến trúc microservice đang dùng trong hệ thống đã có auth-service và academic-service.
