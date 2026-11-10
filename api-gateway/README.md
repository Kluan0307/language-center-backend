# api-gateway

Cổng **8080**, Spring Cloud Gateway reactive. Không có controller nghiệp vụ hoặc database.

| Prefix giữ nguyên | Đích mặc định |
|---|---|
| /api/v1/auth/** | http://localhost:8081 |
| /api/v1/academic/** | http://localhost:8082 |
| /api/v1/teaching/** | http://localhost:8083 |
| /api/v1/finance/** | http://localhost:8084 |

Đích đọc từ AUTH_URL/ACADEMIC_URL/TEACHING_URL/FINANCE_URL.
Gateway chuyển tiếp Authorization; từng service kiểm tra JWT và quyền nghiệp vụ.
CORS cho frontend http://localhost:5173 được xử lý tại gateway; Origin được bỏ trước khi chuyển xuống service để tránh header lặp.
Kết nối downstream timeout 3 giây, response timeout 15 giây; không lưu security context vào session.

Chạy từ thư mục root:

```powershell
.\scripts\run-service.ps1 api-gateway
mvn -f api-gateway/pom.xml test
```

Xem [README chính](../README.md), [danh sách API](../docs/api-list.md) và [hợp đồng](../docs/api-contracts.md).
Các endpoint GET / trực tiếp của finance/teaching không được định tuyến qua gateway.
