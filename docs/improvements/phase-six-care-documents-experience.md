# Cải thiện 6 — chăm sóc lead, tài liệu và trải nghiệm

Hoàn thành ngày 05/11/2026. Giữ cấu trúc service hiện có; frontend dùng Ant Design và styled-components.

## Lead và phân công

- Lead có version, lịch hẹn nextContactAt, lịch sử bất biến và người phụ trách. CALL/EMAIL/MESSAGE/IN_PERSON ghi lần liên hệ; NOTE và đổi trạng thái hành chính không tự tạo lần liên hệ.
- Staff đọc lead cùng chi nhánh nhưng chỉ sửa lead mình phụ trách. Admin/Super Admin quản lý trong phạm vi được phép; Teacher không vào CRM.
- Chuyển người phụ trách phải có lý do, chọn tài khoản ADMIN/STAFF đang hoạt động thuộc chi nhánh. Ghi chủ cũ/mới và giữ lịch hẹn. Người phụ trách cũ mất quyền sửa ngay.
- Auth chặn bỏ phân công chi nhánh của Admin/Staff còn lead mở; phải bàn giao trước. Khóa account vẫn được phép vì nhu cầu bảo mật. Kiểm tra bàn giao và bỏ chi nhánh dùng khóa tài khoản để tránh race.
- Bộ lọc OVERDUE/TODAY/UPCOMING/UNSCHEDULED chỉ xét lead mở. Thời gian nghiệp vụ UTC+7; quá hạn tính khi đọc/refresh. Chưa có gửi SMS/email/push hay scheduler nhắc tự động.
- Mọi thay đổi cần version đã đọc; xung đột trả 409. Chuyển thành học viên chỉ từ CONTACTED/CONSULTING và cần version. Retry lead đã chuyển trả cùng học viên, không tạo thêm.

## Tài liệu

- Chỉ người tải lên được gửi lại tài liệu REJECTED. Lý do bắt buộc; có thể sửa metadata hoặc thay file. Không đổi lớp/khóa học. Lần gửi lại tăng revision, về PENDING, xóa quyết định hiện tại.
- Review gửi version đang xem; không duyệt nhầm phiên bản mới khi người dùng đang mở phiên bản cũ.
- Giữ file và quyết định các revision trước. Admin/Super Admin theo phạm vi và Teacher sở hữu được xem lịch sử/tải bản cũ. Staff chỉ đọc/tải bản APPROVED hiện tại, không được lịch sử riêng.
- Audit không trả storageKey. Dữ liệu cũ chỉ baseline từ thuộc tính đã biết, không bịa lịch sử hoặc người duyệt.

## Hợp đồng API bổ sung

Base lead: /api/v1/academic/leads; tài liệu: /api/v1/teaching/documents.

| Method | Đường dẫn | Quyền / body |
|---|---|---|
| GET | /leads/assignees?branchId= | SA/A; danh sách người được giao |
| GET | /leads/{id}/history | SA/A/S cùng phạm vi |
| POST | /leads/{id}/interactions | SA/A/S phụ trách; channel, note, status, nextContactAt, version |
| PUT | /leads/{id}/assignment | SA/A; accountId, reason, version |
| GET | /documents/{id}/history | SA/A hoặc Teacher sở hữu |
| GET | /documents/{id}/revisions/{revision}/download | Như quyền lịch sử |
| POST | /documents/{id}/resubmit | Multipart title, description?, reason, version, file?; chủ sở hữu |

PUT lead thêm version; PATCH status cần status, note, version. POST /leads/{id}/convert bắt buộc JSON {"version":0}. PATCH review tài liệu thêm version cùng status/reason. nextContactAt null trong interaction xóa lịch hẹn; lịch mới phải ở tương lai. Lưu version mới từ response, không tái sử dụng version cũ cho thao tác khác. Khi mất phản hồi mạng sau ghi, tải lại lịch sử trước khi thử lại.

## UX và hiệu năng

Form lead/chăm sóc/bàn giao/upload/review/resubmit có xác nhận bỏ thay đổi và cảnh báo đóng/refresh tab khi bẩn/đang gửi; không chặn mọi điều hướng nội bộ BrowserRouter hoặc nút Back. Nút gửi khóa khi đang xử lý; phản hồi ghi chưa xác định yêu cầu kiểm tra lịch sử.

Đo Hibernate tại service, H2, 20 bản ghi có quan hệ khác nhau: danh sách lead 22 → 2 SQL; enrollment 42 → 2 SQL nhờ EntityGraph. Test giữ ngân sách <=3. Không bao gồm Auth middleware, không phải benchmark tải/độ trễ production. Tìm kiếm không phân biệt hoa thường ở cả bốn service.

Lazy route đã có từ trước và được giữ. JS production: 57 → 58 chunks; 1.814.627 → 1.827.217 byte do thêm tính năng; chunk lớn nhất giữ 760.673 byte (gzip 248.139). Cảnh báo >500 kB còn; không tuyên bố giảm bundle.

## Migration và vận hành

Sao lưu SQL và thư mục file trước nâng cấp. Sau V005, chạy docs/migrations/V006__lead_care_document_revisions.sql; thêm version/lịch hẹn/audit/revision, không reset tài khoản hoặc tạo lịch sử liên hệ giả. Migration là SQL chạy thủ công, không tự chạy qua Flyway. Local đã backup SQL, áp dụng và replay thành công, giữ version/lịch hẹn/file/quyết định. Khi triển khai nơi khác phải sao lưu cả file vật lý.

Khởi động lại Auth, Academic, Teaching và Finance (Query chung đã thay đổi); tải lại frontend. Không dùng code cũ để ghi dữ liệu sau khi bắt đầu tạo revision/audit mới; khi rollback phải khôi phục đồng bộ SQL và file từ backup.

## Xác minh và bước tiếp theo

166 backend tests, 72 frontend tests; ESLint, TypeScript/Vite build đạt. 41 kiểm tra MySQL/gateway/UI/concurrency/replay: bốn role, bàn giao, stale version, lịch hẹn, chuyển học viên/đăng ký/thu tiền/điểm danh/điểm thi, từ chối/gửi lại/duyệt file, tải bản cũ và chống lộ lịch sử. Bằng chứng local ở storage/improvement6-* (gitignore).

Giai đoạn 7 tiếp theo: chuẩn hóa môi trường triển khai, backup/restore SQL+file có diễn tập, migration trên bản sao dữ liệu thật, health/log/giám sát và nghiệm thu trước phát hành. Chưa xác minh tải production hoặc tự động hóa thông báo.
