# Cải thiện giai đoạn 2 — Tài khoản và phiên đăng nhập

Triển khai ngày 05/11/2026, sau [giai đoạn 1 — nhiều chi nhánh](phase-one-multi-branch.md).

## Chức năng đã cập nhật

| Chức năng | Quyền và hành vi |
|---|---|
| Đổi mật khẩu cá nhân | Cả bốn role; yêu cầu mật khẩu hiện tại, mật khẩu mới và xác nhận |
| Cấp lại mật khẩu | SUPER_ADMIN cấp lại cho Admin/Staff/Teacher; ADMIN chỉ cấp lại cho Staff/Teacher có toàn bộ phân công trong phạm vi mình |
| Bắt buộc đổi mật khẩu | Tài khoản mới và tài khoản vừa được cấp lại mật khẩu phải đổi trước khi sử dụng nghiệp vụ |
| Đăng xuất tất cả phiên | Cả bốn role; kết thúc cả phiên hiện tại, giữ nguyên mật khẩu |
| Cập nhật hồ sơ | Người dùng tự sửa email/điện thoại của mình; các trường nhân sự, role, username, chi nhánh không được sửa qua API này |
| Khóa/mở tài khoản | Khóa làm hết hiệu lực token hiện có; mở lại không khôi phục token cũ |
| Ghi nhận thay đổi | account_security_events lưu thao tác, tài khoản, người thực hiện, thời gian và lý do cấp lại; không lưu mật khẩu/token |

Không cấp lại mật khẩu cho chính mình bằng chức năng quản trị; dùng đổi mật khẩu cá nhân.
Không dùng endpoint quản trị để cấp lại mật khẩu SUPER_ADMIN. Cấp lại mật khẩu cho tài khoản
đang khóa không làm tài khoản được mở khóa.

## API

Các đường dẫn có prefix `/api/v1/auth`, gọi qua Gateway 8080.

| Method | Path | Body | Kết quả |
|---|---|---|---|
| POST | `/me/password` | `{currentPassword,newPassword,confirmPassword}` | 204; tất cả phiên hết hiệu lực |
| POST | `/me/logout-all` | Không body | 204; tất cả phiên hết hiệu lực |
| POST | `/accounts/{id}/reset-password` | `{temporaryPassword,confirmPassword,reason}` | 204; mật khẩu tạm có hiệu lực, phiên cũ hết hiệu lực |
| PATCH | `/me/profile` | `{email,phone}` | 200 UserResponse mới |

Ví dụ đổi mật khẩu:

```json
{"currentPassword":"<mật khẩu hiện tại>","newPassword":"<mật khẩu mới>","confirmPassword":"<mật khẩu mới>"}
```

- Mật khẩu mới/tạm: ít nhất 8 ký tự, tối đa 72 byte UTF-8, không toàn khoảng trắng,
  khác mật khẩu đang dùng. Giữ nguyên khoảng trắng có ý nghĩa trong mật khẩu, không trim.
- Mật khẩu hiện tại có thể ngắn hơn 8 ký tự để hỗ trợ tài khoản cũ. Giới hạn byte vẫn áp dụng.
- Xác nhận phải khớp; lý do cấp lại sau khi trim dài 3–255 ký tự.
- Email tối đa 100 ký tự và đúng định dạng; điện thoại 6–15 ký tự gồm số và các ký tự `+() .-`.
  Chuỗi rỗng/null xóa thông tin liên hệ. Hai trường không gửi cũng được xử lý như null;
  frontend luôn gửi đủ cả hai. Họ tên/chuyên môn/ngày tuyển dụng do quản lý nhân sự phụ trách.
- Trả 400 khi sai dữ liệu hoặc mật khẩu hiện tại; 401 khi phiên không còn hiệu lực;
  403 khi thiếu quyền; 404 khi đối tượng nằm ngoài phạm vi quản lý.
- Login và UserResponse có thêm `mustChangePassword`; UserResponse có `passwordChangedAt`.

## Cơ chế phiên

`accounts.session_version` là số phiên bản hiện tại, khởi đầu bằng 0.
JWT mới có claim `sv`. Auth kiểm tra accountId, username, trạng thái và phiên bản trước
khi xác thực. Token có chữ ký hợp lệ nhưng phiên bản cũ vẫn bị từ chối.

Đổi/cấp lại mật khẩu, đăng xuất tất cả phiên và khóa tài khoản đều tăng phiên bản.
Thao tác được khóa theo dòng tài khoản. Đăng nhập cũng khóa dòng trước khi kiểm tra mật khẩu,
tránh phát token phiên bản mới sau khi chỉ xác minh mật khẩu cũ trong một yêu cầu đồng thời.
Thay đổi mật khẩu kiểm tra lại phiên bản dưới khóa; hai yêu cầu từ cùng phiên cũ không cùng thành công.

Academic, Teaching và Finance tiếp tục gọi Auth `/me` theo từng yêu cầu như giai đoạn 1;
vì vậy token đã thu hồi bị chặn ở cả bốn service. Không có cache quyền cũ khi Auth lỗi;
service phụ thuộc trả 503. Chưa bổ sung refresh token hay danh sách phiên từng thiết bị.

Token trước migration không có `sv` được coi là phiên bản 0, chỉ hợp lệ khi tài khoản vẫn
ở phiên bản 0. Cách chuyển đổi này giữ phiên của tài khoản cũ cho đến lần thu hồi tiếp theo.
JWT vẫn hết hạn theo cấu hình hiện tại.

Khi `mustChangePassword=true`, chỉ cho phép:

- Đăng nhập, GET `/me`, POST `/me/password`, POST `/me/logout-all` và các yêu cầu CORS phù hợp.
- Các API nghiệp vụ/nhân sự khác trả 403 với `code=PASSWORD_CHANGE_REQUIRED`.
- Frontend chuyển mọi trang được bảo vệ về `/change-password`, không dựng sidebar hay gọi API nghiệp vụ.

Đổi mật khẩu thành công không cấp token thay thế: đăng nhập lại bằng mật khẩu mới.
Đăng xuất ở sidebar vẫn là kết thúc phiên phía trình duyệt; dùng **Đăng xuất tất cả phiên**
để vô hiệu hóa các token đã phát trên máy chủ.

## Giao diện và sử dụng

1. Nhấn tên người dùng trên sidebar để mở **Hồ sơ của tôi**.
2. Dùng **Cập nhật liên hệ**, **Đổi mật khẩu** hoặc **Đăng xuất tất cả phiên**.
3. Quản lý vào **Tài khoản / Nhân viên / Giáo viên → Cấp lại mật khẩu**; nhập mật khẩu tạm,
   xác nhận và lý do. Nút chỉ hiển thị khi người dùng có quyền quản lý toàn tài khoản đó.
4. Người được cấp tài khoản/mật khẩu tạm đăng nhập rồi được đưa tới trang đổi mật khẩu bắt buộc.
5. Sau đổi mật khẩu, đăng nhập lại. Các tab/thiết bị khác bị chặn ở yêu cầu tiếp theo hoặc
   khi quay lại tab và frontend kiểm tra phiên.

Nếu mạng lỗi sau khi gửi đổi/cấp lại mật khẩu, thao tác có thể đã được lưu. Giao diện không
tự gửi lại POST; kiểm tra đăng nhập bằng mật khẩu mới/tạm trước khi thực hiện lại.
Các DTO mật khẩu và token đã che thông tin trong `toString` để không lộ qua log chẩn đoán thông thường.

## Migration và khởi động

Sao lưu rồi áp dụng [V002__account_security.sql](../migrations/V002__account_security.sql)
trước khi chạy bản mới. Migration thêm ba cột tài khoản và bảng sự kiện, không sửa mật khẩu
hay phân công hiện có. Chạy lại không đưa phiên bản về 0 hoặc bỏ cờ bắt buộc đổi mật khẩu.

Database local đã áp dụng và đối chiếu giữ nguyên hash mật khẩu. Bốn tài khoản hiện tại
không bị ép đổi mật khẩu do migration; các tài khoản tạo mới sau cập nhật mới dùng chính sách tạm.

Khởi động lại Auth, Academic, Teaching, Finance và Gateway; mỗi service dùng `mvn spring-boot:run`
trong thư mục tương ứng. Frontend dùng `npm run dev` hoặc bản build mới.

## Phần tiếp theo

Giai đoạn cải thiện 3: buổi học cụ thể theo ngày, nghỉ/học bù, đổi phòng và giáo viên dạy thay.
Khôi phục mật khẩu tự động qua email/OTP và quản lý phiên từng thiết bị chưa nằm trong giai đoạn này.

Kết quả kiểm tra và giới hạn xem [verification](../verification.md).
