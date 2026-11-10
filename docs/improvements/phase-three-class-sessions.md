# Cải thiện 3 — Buổi học thực tế

Đã triển khai ngày 05/11/2026 trên backend và frontend React/Ant Design/styled-components. Đây là giai đoạn cải thiện sau MVP, khác với giai đoạn tuyển sinh số 3 của MVP.

## Cách sử dụng

1. Khởi động lại Auth, Academic, Teaching, Finance và Gateway bằng lệnh Maven như README; tải lại frontend.
2. Vào **Lịch học**: mặc định là từng buổi theo ngày, có bộ lọc lớp/ngày/trạng thái. Admin/Super Admin chọn **Mẫu sinh lịch** để quản lý lịch tuần.
3. Tạo lớp và lịch tuần sẽ sinh các buổi trong khoảng ngày của lớp. **Sinh buổi** chỉ bổ sung ngày nguồn còn thiếu, không ghi đè điều chỉnh đã có.
4. Admin/Super Admin chọn một buổi để đổi ngày/giờ/phòng hoặc giáo viên thực tế; bắt buộc ghi lý do. Hủy rồi chọn **Học bù** tạo một buổi riêng liên kết với buổi gốc.
5. Giáo viên vào lịch của mình và mở **Điểm danh** tại buổi thực tế. Sau khi lưu, buổi chuyển COMPLETED và giáo viên chỉ xem. Admin sửa phải có lý do và phiên bản mới nhất; lịch sử giữ trạng thái và người ghi trước đó.

## Quyền

| Thao tác | Super Admin | Admin | Staff | Teacher |
|---|---|---|---|---|
| Xem lịch | Theo chi nhánh chọn | Chi nhánh được giao | Chi nhánh được giao | Buổi thực tế được giao |
| Sinh/đổi/hủy/khôi phục/học bù | Có | Trong phạm vi | Không | Không |
| Ghi điểm danh lần đầu | Có | Trong phạm vi | Trong phạm vi | Giáo viên thực tế của buổi |
| Sửa buổi đã điểm danh | Lý do + version | Lý do + version | Không | Không |
| Xem lịch sử sửa điểm danh | Có | Trong phạm vi | Không | Không |

Giáo viên chính có thể đọc buổi dạy thay của lớp mình nhưng không ghi thay giáo viên thực tế. Dạy thay không cấp quyền mở danh sách liên hệ toàn lớp, đề thi, điểm số hay các buổi khác. Backend kiểm tra độc lập với sidebar.

## Quy tắc đã chốt

- Lịch tuần là mẫu; class_sessions là nguồn kiểm tra trùng lịch và điểm danh. Chỉ một buổi hoạt động/lớp/ngày để tương thích khóa điểm danh cũ.
- Đổi mẫu chỉ đồng bộ buổi REGULAR chưa chỉnh tay từ ngày mai. Giữ nguyên quá khứ, hôm nay, buổi đã điểm danh, đã chỉnh tay hoặc hủy thủ công.
- Buổi CANCELLED không chiếm phòng/giáo viên. Kiểm tra trùng giáo viên xuyên chi nhánh, phòng theo chi nhánh và học viên theo khoảng ghi danh thực tế. Khóa giao dịch ngăn hai yêu cầu cùng chiếm chỗ.
- Phòng phải đúng chi nhánh, đủ sức chứa và không bảo trì; giáo viên phải hoạt động và có phân công chi nhánh. Không thu hồi phân công khi còn buổi chưa hoàn tất được giao, kể cả dạy thay.
- Buổi đã điểm danh không đổi ngày/giờ/phòng/giáo viên và không hủy. Gửi lại cùng nội dung điểm danh là idempotent; thay nội dung cần quyền quản lý, lý do và version. Không điểm danh buổi tương lai, buổi hủy hoặc danh sách học viên rỗng.
- Một buổi REGULAR đã hủy có tối đa một buổi học bù; không tạo chuỗi học bù. Nếu học bù bị hủy, sửa/khôi phục chính buổi đó. Không khôi phục buổi gốc khi học bù còn hoạt động.
- Học bù được vượt ngày kết thúc dự kiến. effectiveEndDate phục vụ trạng thái lớp/thống kê; endDate dự kiến vẫn giữ nguyên.
- Đóng lớp hủy các buổi chưa điểm danh, giữ lịch sử đã hoàn tất. Mọi sửa lịch có audit trước/sau, tác giả và lý do.
- Giới hạn sinh lịch 3.660 ngày; không thiết kế cho lớp không có ngày kết thúc.

## Migration và dữ liệu cũ

Áp dụng [V003](../migrations/V003__class_sessions.sql) sau V001/V002, sao lưu và kiểm tra trùng thứ trong lịch tuần trước khi chạy. SQL MySQL có DDL nên không coi toàn bộ script là một transaction. Dừng bản cũ trước khi nâng cấp; phục hồi từ bản sao lưu nếu cần quay lại schema cũ.

Database local đã sao lưu rồi áp dụng; không tạo lại database và không sửa mật khẩu bốn tài khoản đang có. Điểm danh cũ liên kết với buổi LEGACY/COMPLETED; giữ ngày, trạng thái, người ghi. Giờ/phòng/giáo viên thực tế không biết để null, không suy diễn từ mẫu hiện tại. sessions_initialized ngăn chạy lại migration tự sinh lại kế hoạch sau khi người dùng đã sửa lịch.

Đã thử migration với điểm danh cũ và chạy lại hai lần: không đổi tác giả, trạng thái, version, buổi hủy hay buổi chỉnh tay.

## API

Thêm 9 endpoint sessions; xem [API list](../api-list.md) và [contracts](../api-contracts.md). Tại mốc hoàn thành giai đoạn 3: 100 API chính: Auth 14, Academic 64, Finance 8, Teaching 14.

## Kiểm thử và giới hạn

Có 18 kiểm thử tích hợp mới cho vòng đời buổi học, lịch bù, quyền dạy thay, khóa/sửa điểm danh và lịch sử. Kiểm tra MySQL qua gateway bao gồm hai giao dịch đặt trùng phòng và hai giao dịch đặt giáo viên xuyên chi nhánh. Giao diện kiểm tra Teacher/Admin, dạy thay, lưu/sửa điểm danh, đổi phòng, lịch sử, bộ lọc mẫu lịch, dashboard và màn hình di động.

Chưa có khoảng đệm di chuyển giữa chi nhánh; hiện chỉ cấm thời gian chồng lấn. Điểm danh kiểm tra ngày theo Asia/Ho_Chi_Minh, chưa khóa theo giờ bắt đầu. Không hỗ trợ hai buổi hoạt động cùng lớp trong một ngày. Chưa có thông báo tự động khi đổi lịch. Kiểm tra chức năng hiện tại không thay thế kiểm thử tải hay nghiệm thu dữ liệu sản xuất.

Giai đoạn tiếp theo: cải thiện 4 — vòng đời học viên, bảo lưu và danh sách dự thi theo ngày.
