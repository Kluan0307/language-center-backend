# auth-service

Định danh và quản lý tài khoản. Cổng **8081**, prefix `/api/v1/auth`; hiện có **14 API nghiệp vụ chính**.

## Tra cứu

- [Toàn bộ endpoint và quyền](../docs/api-list.md#1-auth--14-api)
- [Body và response](../docs/api-contracts.md)
- [Hướng dẫn chạy/cấu hình toàn dự án](../README.md)
- [Migration dữ liệu cũ](../docs/migrations/README.md)

## Nhóm API

- POST /login — đăng nhập; GET /me — hồ sơ hiện tại.
- GET/POST /accounts — tài khoản có phân trang/tạo mới; PATCH /accounts/{id}/toggle-status.
- GET /teachers và /teachers/{id} — giáo viên khả dụng, ID là profileId.

Các path rút gọn ở trên ghép với prefix của service. Qua gateway dùng cổng 8080 và giữ nguyên path.

## Quyền và phụ thuộc

SA quản lý toàn chuỗi; A tạo/quản lý S/T trong các chi nhánh được giao; chỉ khóa toàn tài khoản khi mọi phân công của người đó thuộc phạm vi A. Danh sách giáo viên dành cho SA/A/S.

Phụ thuộc: Academic để xác minh chi nhánh khi tạo tài khoản.

## Chạy và kiểm thử

Chạy bằng Maven cài trên máy từ thư mục backend root:

```powershell
cd auth-service
mvn spring-boot:run
```

`application.properties` tự import `.env.local` từ thư mục service cha hoặc thư mục hiện tại.
Có thể dùng script tiện ích từ thư mục backend root:

```powershell
cd ..
.\scripts\run-service.ps1 -Service auth-service
```

Kiểm thử:

```powershell
mvn -f auth-service/pom.xml test
```

Test dùng H2/mock/HTTP stub, không dùng MySQL hiện tại.

## Phân công nhiều chi nhánh

Admin/Teacher có thể có nhiều AccountBranch; Staff tối đa một. SA quản lý qua GET/PUT accounts/{id}/branches và GET accounts/{id}/branches/history. Xem [hướng dẫn](../docs/improvements/phase-one-multi-branch.md).

## Cải thiện giai đoạn 2

Có POST me/password, POST me/logout-all, PATCH me/profile và POST accounts/{id}/reset-password. Tài khoản mới/cấp lại dùng mật khẩu tạm; đổi mật khẩu trước khi vào nghiệp vụ. Session version bảo đảm token cũ mất hiệu lực sau đổi/cấp lại mật khẩu, logout-all hoặc khóa tài khoản. Áp dụng V002 trước khi chạy. [Hợp đồng và hướng dẫn](../docs/improvements/phase-two-account-security.md).

## Cải thiện 6

Xem [lead, revision tài liệu và guard phân công](../docs/improvements/phase-six-care-documents-experience.md). API ghi mới yêu cầu version; áp dụng V006 trước khi chạy.

## G02–G03

Teacher profile quản trị dùng profileId ở /auth/teacher-profiles; account API vẫn accountId. Sửa hồ sơ/self-contact có version/audit; ngừng làm việc giữ khóa account và kiểm tra mọi trách nhiệm trước commit; khóa đăng nhập riêng. Áp dụng V007. [API](../docs/api-contracts.md) · [Bàn giao](../docs/thesis-progress/G03.md).
