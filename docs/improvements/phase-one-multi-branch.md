# Cải thiện giai đoạn 1 — Phân quyền nhiều chi nhánh

Đã triển khai trên nền sáu giai đoạn chức năng ban đầu. Đây là giai đoạn cải thiện 1,
khác với giai đoạn frontend 1 trong các tài liệu cũ.

## Mô hình và nguyên tắc

- Một tài khoản giữ một role chính: SUPER_ADMIN, ADMIN, STAFF hoặc TEACHER.
- `AccountBranch` xác định các chi nhánh được phân công. Cặp account/branch duy nhất;
  có trạng thái, chi nhánh chính, người/ngày phân công và ngày thu hồi.
- `accounts.branch_id` được giữ làm chi nhánh chính. Quyền truy cập lấy từ các phân công
  đang có hiệu lực, không lấy duy nhất từ trường này hay claim branchId của JWT.
- `AccountBranchEvent` ghi nối tiếp các lần phân công, thu hồi và thay đổi chi nhánh chính.
  Phân công lại không xóa lịch sử. MIGRATED đánh dấu dữ liệu chuyển từ hệ thống cũ.
- Chi nhánh chính phải thuộc danh sách đang phân công. Danh sách rỗng yêu cầu chi nhánh chính
  null; tài khoản đó không đăng nhập mới hoặc truy cập nghiệp vụ được cho đến khi phân công lại.
- Chi nhánh ngừng hoạt động vẫn đọc được lịch sử; các quy tắc chặn tạo/sửa nghiệp vụ tại
  chi nhánh ngừng hoạt động tiếp tục được áp dụng.

## Ma trận quyền

| Chức năng | SUPER_ADMIN | ADMIN | STAFF | TEACHER |
|---|---|---|---|---|
| Phạm vi chi nhánh | Toàn hệ thống | Một/nhiều được giao | Tối đa một được giao | Một/nhiều được giao |
| Tạo/sửa/bật tắt chi nhánh | Có | Không | Không | Không |
| Phân công/thu hồi chi nhánh nhân sự | Có | Không | Không | Không |
| Xem lịch sử phân công | Có | Không | Không | Không |
| Tạo Admin | Có | Không | Không | Không |
| Tạo Staff/Teacher | Có | Trong chi nhánh được giao | Không | Không |
| Khóa/mở tài khoản | Trừ SUPER_ADMIN | Staff/Teacher có toàn bộ phân công nằm trong phạm vi Admin | Không | Không |
| Lead, học viên, ghi danh, thu tiền | Có | Chi nhánh được giao | Chi nhánh được giao | Không |
| Khóa học và phòng | Quản lý toàn hệ thống | Theo quyền hiện có, trong phạm vi | Theo quyền hiện có | Đọc khóa học được phép |
| Đổi giáo viên/phòng/ngày/lịch lớp | Có | Chi nhánh được giao | Không | Không |
| Lớp và lịch giảng dạy | Toàn hệ thống | Chi nhánh được giao | Chi nhánh được giao | Chỉ lớp của mình trong chi nhánh được giao |
| Điểm danh | Có | Chi nhánh được giao | Chi nhánh được giao | Chỉ lớp của mình |
| Kiểm tra và điểm | Có | Quản lý/sửa theo quy tắc có lý do | Không | Chỉ lớp của mình |
| Duyệt tài liệu | Có | Chi nhánh được giao | Không; chỉ đọc APPROVED | Không; đăng/xem tài liệu của mình |
| Điều chỉnh học phí, báo cáo tài chính | Có | Chi nhánh được giao | Không | Không |
| Thống kê vận hành | Toàn hệ thống | Một/tất cả chi nhánh được giao | Chi nhánh được giao | Dashboard giảng dạy riêng |

Quyền ở bảng này luôn kết hợp điều kiện trạng thái và quy tắc nghiệp vụ của endpoint.
Admin không tự tăng quyền và không được khóa tài khoản giáo viên dùng chung với một chi
nhánh nằm ngoài phạm vi quản lý của mình.

## API và phạm vi yêu cầu

| Method | Đường dẫn | Quyền | Nội dung |
|---|---|---|---|
| GET | `/api/v1/auth/accounts/{id}/branches` | SUPER_ADMIN | Phân công hiện tại và các liên kết đã thu hồi |
| PUT | `/api/v1/auth/accounts/{id}/branches` | SUPER_ADMIN | Thay toàn bộ danh sách đang phân công và chi nhánh chính |
| GET | `/api/v1/auth/accounts/{id}/branches/history` | SUPER_ADMIN | Các sự kiện phân công theo thời gian giảm dần |

Body PUT:

```json
{"branchIds":[11,12],"primaryBranchId":11}
```

Thu hồi toàn bộ: `{"branchIds":[],"primaryBranchId":null}`.
Staff có nhiều hơn một chi nhánh, danh sách trùng ID hoặc chi nhánh chính ngoài danh sách
đều trả 400. Chỉ được thêm mới phân công tới chi nhánh đang hoạt động.

- Login, GET /me và các response tài khoản có thêm `branchIds`; `branchId` là chi nhánh chính.
- GET teachers trả thêm `branchIds`; GET teachers/{profileId} nhận query `branchId` để kiểm tra
  khả dụng tại chi nhánh cụ thể, kể cả khi khác chi nhánh chính của giáo viên.
- Gửi `X-Branch-Id: 12` để chọn chi nhánh làm việc. Thiếu header thì dùng chi nhánh chính.
- Tham số branchId trong query/body phải nằm trong phân công; yêu cầu ngoài phạm vi bị từ chối,
  không còn bị lặng lẽ đổi sang chi nhánh chính. Header không được phân công trả 403;
  lookup bản ghi ngoài phạm vi trả 404. Header không phải số nguyên dương trả 400.
- Endpoint chi tiết kiểm tra chi nhánh của bản ghi và quyền lớp; biết ID không cấp thêm quyền.
- Tạo/sửa bản ghi không chuyển chi nhánh của dữ liệu cũ; các điều kiện cùng chi nhánh vẫn áp dụng.
- GET branches trả tất cả chi nhánh được phân công để dùng cho bộ chọn.
- Academic/Finance stats nhận `allAssigned=true`. ADMIN chỉ tổng hợp chi nhánh được giao;
  SUPER_ADMIN tổng hợp toàn hệ thống. Không kết hợp tham số này với branchId.
- Academic stats hỗ trợ groupByBranch trong phạm vi đã xác định. STAFF không được dùng allAssigned.
- Tỷ lệ lấp đầy/chuyển đổi tổng hợp dùng tổng tử số/mẫu số; không lấy trung bình các tỷ lệ chi nhánh.
- Danh sách nghiệp vụ thông thường dùng một chi nhánh mỗi lần. Không dùng allAssigned cho thao tác ghi.

Academic, Teaching và Finance kiểm tra chữ ký JWT rồi đọc phiên hiện tại từ Auth `/me`
mỗi request có token. Quyền/chi nhánh và trạng thái tài khoản lấy từ dữ liệu hiện tại.
Thu hồi phân công có hiệu lực ở yêu cầu tiếp theo; không cần đợi token hết hạn.
Nếu Auth không sẵn sàng, service trả 503 thay vì dùng quyền cũ trong token.
Điều này tăng số lời gọi Auth; cơ chế cache có thu hồi/version sẽ được cân nhắc khi tối ưu vận hành.
JWT vẫn có thời hạn, chưa bổ sung refresh token hay đăng xuất mọi thiết bị.

## Teacher nhiều chi nhánh

- Có thể được giao lớp ở từng chi nhánh đã phân công, bất kể chi nhánh chính.
- Kiểm tra trùng giáo viên xuyên chi nhánh theo lịch tuần và khoảng ngày lớp.
- Khóa tài khoản giáo viên trong giao dịch thay đổi lịch để tuần tự hóa các lần ghi khác chi nhánh.
  Việc này dùng JDBC khóa `accounts` thông qua `teachers` trong Academic: ngoại lệ hẹp của
  kiến trúc shared-database hiện tại, cần thay bằng API điều phối nếu tách database sau này.
- Thông báo trùng ở chi nhánh khác ẩn tên/mã lớp để tránh lộ dữ liệu ngoài phạm vi người thao tác.
- Không thu hồi chi nhánh của Teacher khi còn lớp UPCOMING/ONGOING tại đó. Cần đổi giáo viên
  hoặc kết thúc lớp trước. Lớp đã kết thúc không chặn thu hồi và không bị xóa lịch sử.
- Chưa tính thời gian di chuyển giữa các chi nhánh; lịch buổi học và dạy thay thuộc giai đoạn 3.

## Sử dụng giao diện

1. SUPER_ADMIN vào **Tài khoản / Giáo viên / Nhân viên**, chọn **Chi nhánh** ở dòng tài khoản.
2. Chọn danh sách chi nhánh và chi nhánh chính, kiểm tra lịch sử rồi lưu.
3. ADMIN/TEACHER chọn **Chi nhánh đang làm việc** trên sidebar, kể cả menu di động.
4. Xác nhận chuyển: các biểu mẫu/trạng thái trang được tạo lại, bỏ tham số URL cũ và tải dữ liệu
   theo phạm vi mới. Lựa chọn lưu trong sessionStorage riêng cho từng tài khoản/tab.
5. ADMIN có công tắc **Tất cả chi nhánh được giao** trên Tổng quan/Báo cáo. Các thao tác ghi vẫn
   dùng chi nhánh đang làm việc. Đánh giá lớp trên trang báo cáo vẫn theo chi nhánh đang làm việc.
6. Nếu bị thu hồi chi nhánh trong lúc đang mở trang, API chặn ngay; tải lại trang để cập nhật danh sách
   và chuyển về chi nhánh chính mới. Sidebar có thể còn dữ liệu phiên cũ cho đến khi làm mới.

## Migration và chạy

Áp dụng [V001](../migrations/V001__account_branch_assignments.sql) trước khi chạy bản mới.
Sao lưu database trước; migration thêm hai bảng và chuyển các liên kết accounts.branch_id hợp lệ,
không xóa dữ liệu. Chạy lại không khôi phục một phân công đã thu hồi.

Trên database local đã áp dụng migration và kiểm tra tài khoản sẵn có; bản sao lưu được giữ
trong `storage/before-multibranch-*.sql` (gitignored).

Khởi động Auth trước, rồi Academic, Teaching, Finance và Gateway; mỗi service một terminal:

```powershell
cd auth-service
mvn spring-boot:run
```

Các service cần khởi động lại để dùng code mới. Frontend dùng `npm run dev`.
Không cần tạo lại database hay tài khoản.

## Các giai đoạn cải thiện tiếp theo

| Giai đoạn | Trạng thái | Công việc |
|---|---|---|
| 1 — Nhiều chi nhánh | Đã triển khai | Phân công, phạm vi truy cập, bộ chọn, báo cáo và kiểm thử |
| 2 — Tài khoản | Đã triển khai; xem giai đoạn 2 | Đổi/cấp lại mật khẩu, hồ sơ cá nhân, chính sách phiên và tối ưu xác thực |
| 3 — Buổi học | Chưa triển khai | ClassSession, nghỉ/học bù, đổi phòng, dạy thay |
| 4 — Học viên | Chưa triển khai | Vòng đời hồ sơ/ghi danh, bảo lưu có thời hạn, danh sách dự thi theo ngày |
| 5 — Tài chính | Chưa triển khai | Lịch sử điều chỉnh đầy đủ, sửa sai/hoàn tiền, đối soát |
| 6 — Trải nghiệm | Chưa triển khai | Chăm sóc Lead, tài liệu gửi lại, trải nghiệm và hiệu năng |
| 7 — Triển khai | Chưa triển khai đầy đủ | Migration toàn hệ thống, môi trường, log, backup/restore và nghiệm thu |

Phần xác minh và kết quả kiểm thử được ghi trong [verification](../verification.md).

Giai đoạn cải thiện 2 đã hoàn thành: [Tài khoản và phiên](phase-two-account-security.md).
