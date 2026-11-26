# G1 — Quy chuẩn phân loại đào tạo

Chốt ngày 16/11/2026. Áp dụng thống nhất cho backend, web quản lý, website, mobile và chatbot khi hoàn tất G3–G8.

## Danh mục chuẩn

| Đối tượng học | Mã cố định |
|---|---|
| Mẫu giáo | PRESCHOOL |
| Tiểu học | PRIMARY |
| THCS | SECONDARY |
| THPT | HIGH_SCHOOL |
| Người lớn | ADULT |

| Chương trình | Mã cố định |
|---|---|
| Tiếng Anh tổng quát | GE |
| Tiếng Anh giao tiếp | COMMUNICATION |
| IELTS | IELTS |
| TOEIC | TOEIC |

Mã là định danh nghiệp vụ không đổi; tên hiển thị có thể điều chỉnh. Không nhập thêm mã tùy ý trong giao diện mới. Ngừng sử dụng bằng active=false, không xóa danh mục đã có tham chiếu. Danh mục chương trình cũ ngoài bốn mã được giữ nguyên để bảo toàn lịch sử, cần xác nhận cách xử lý trước khi dùng cho khóa mới.

## Quan hệ

`Chương trình → Khóa học → Lớp học`.

Mỗi khóa có một chương trình, nhiều đối tượng phù hợp; một đối tượng thuộc nhiều khóa. Trình độ là danh mục riêng theo chương trình, không dùng Mẫu giáo/THCS/Người lớn làm trình độ đầu vào.

Ví dụ IELTS Foundation: chương trình IELTS, đối tượng THCS + THPT, có các lớp tại những chi nhánh phù hợp. Không tạo hai khóa giống nhau chỉ vì hai nhóm đối tượng.

## Quy tắc dùng chung

- Không suy ra đối tượng từ tên khóa, mã khóa, trình độ hoặc ngày sinh của một số học viên đang học.
- Đối tượng phục vụ tìm kiếm/tư vấn. Chưa áp đặt tuổi tối thiểu/tối đa hoặc tự từ chối ghi danh theo tuổi. Nếu muốn ràng buộc độ tuổi sau này phải có quy tắc và ngoại lệ riêng được xác nhận.
- Không mặc định IELTS/TOEIC chỉ cho người lớn, hoặc mọi khóa GE chỉ cho trẻ em.
- Chương trình khóa phải thống nhất với chính sách đầu vào mới. Không viết lại các bản chính sách, kết quả đánh giá, lớp và ghi danh lịch sử khi đổi danh mục.
- Khóa đã phát sinh lớp/ghi danh không được tự đổi chương trình bằng một thao tác sửa thông thường; G3 sẽ thực thi cơ chế bảo vệ.
- Website có các lối tìm theo đối tượng và IELTS/TOEIC. Đây là hai trục lọc kết hợp được, không phải một enum trộn lứa tuổi với chứng chỉ.
- Thống kê tổng số khóa dùng COUNT DISTINCT theo khóa khi join nhiều đối tượng, tránh đếm trùng.
- Đánh giá hiện hành của học viên vẫn theo chương trình; đối tượng không thay thế chính sách đánh giá hoặc trình độ được xác nhận.
- Danh mục gốc do Superadmin quản lý. Quyền gắn phân loại cho khóa phải theo phạm vi chi nhánh và quyền nghiệp vụ được thực thi ở G3/G4.

## Trạng thái chuyển tiếp

G2 bổ sung schema và chương trình đã đối chiếu; trường type vẫn giữ nguyên. Các API/giao diện cũ còn hoạt động. Chưa tuyên bố bỏ ô nhập type hoặc hoàn tất kiểm soát đầu vào: đó là G3/G4. Không tạo nguồn phân loại mới riêng trong CMS, mobile hoặc chatbot.
