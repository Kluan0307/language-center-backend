# Chương 1 — Tổng quan và phương pháp khảo sát

## 1.1. Bài toán và mục tiêu

Đề cương yêu cầu số hóa quản lý học viên, giáo viên, khóa/lớp/lịch, học phí và thống kê của một trung tâm tiếng Anh. Khi hồ sơ, lịch và phiếu thu được lưu riêng, việc đối chiếu có thể mất thời gian hoặc phát sinh sai sót. Đây là vấn đề mục tiêu của thiết kế; **chưa có số liệu thực địa để khẳng định tần suất sai sót, quy mô thị trường hay mức giảm công việc**.

Sản phẩm hướng tới một chuỗi dữ liệu có kiểm soát: tiếp nhận → đánh giá đầu vào → xếp lớp → theo dõi học tập → chốt kết quả, song song với nghĩa vụ học phí/thu tiền/báo cáo. Mục tiêu đầu ra gồm ứng dụng web, SQL/migration, bộ UML/ERD, kiểm thử và bản thảo khóa luận. Mức đáp ứng từng yêu cầu được dẫn tại [ma trận truy vết](../traceability.md), không chỉ đánh giá bằng số màn hình hoặc API.

## 1.2. Phạm vi và đối tượng

Bốn vai trò nội bộ là SUPER_ADMIN, ADMIN, STAFF, TEACHER. Student là hồ sơ được quản lý; không có tài khoản/portal học viên. Admin và Teacher có thể được giao nhiều chi nhánh; Staff tối đa một, còn mỗi đối tượng học vụ có phạm vi riêng. Một trung tâm có thể bắt đầu với một chi nhánh và mở rộng phân công sau.

Phạm vi gồm CRM cơ bản, học viên/giáo viên, chương trình/trình độ, khóa/lớp/phòng/lịch, buổi thực tế/điểm danh, đánh giá/chốt/mở lại, tài liệu có duyệt, thu/điều chỉnh/đảo/hoàn học phí và thống kê. Chưa triển khai LMS, thi online, điểm danh QR, cổng thanh toán hoặc tự cấp chứng chỉ CEFR. Chuyển khoản hiện do nhân viên ghi nhận thủ công.

## 1.3. Phương pháp và mức bằng chứng

Nguồn yêu cầu là đề cương người thực hiện cung cấp, [requirements](../requirements.md), bộ quy tắc [G01-v1](../business-rules.md) và code đang chạy. Quy trình thực hiện gồm phân tích yêu cầu, mô hình hóa UC/ERD/Class/Activity/Sequence, cài đặt FE/BE, kiểm tra tự động và nghiệm thu riêng. Không bổ sung một tên trung tâm hoặc người được phỏng vấn khi chưa có chứng cứ.

[CẦN BỔ SUNG] Tên đơn vị, người khảo sát/người trả lời, vai trò, thời điểm, biểu mẫu và nhận xét có xác nhận. [survey.md](../survey.md) có bộ câu hỏi/biên bản trống. Khi có dữ liệu thực, đối chiếu giả định nào phù hợp hoặc cần đổi; không viết lại nguồn khảo sát như thể đã có từ đầu.

## 1.4. Các quy trình mục tiêu

| Quy trình | Luồng mục tiêu và ràng buộc |
|---|---|
| Tiếp nhận | Lead → chăm sóc → chuyển Student có ghi vết; replay chuyển đổi không tạo thêm học viên |
| Đầu vào | Xác nhận đánh giá theo chương trình; đối chiếu tập trình độ được chấp nhận, không suy từ text currentLevel |
| Ngoại lệ | REQUESTED → quản lý quyết định → tiêu thụ khi ghi danh; không được vượt sĩ số/lịch/chi nhánh, không TTL cố định |
| Học vụ | Enrollment và khoảng học lưu lịch sử bảo lưu/học lại/chuyển lớp; lịch tuần khác buổi thực tế |
| Giảng dạy | Điểm danh hợp lệ hoàn tất buổi; điểm/quy tắc tạo đánh giá tạm; thiếu dữ liệu là INSUFFICIENT |
| Xác nhận | Chốt từng enrollment; mở lại toàn lớp có kiểm toán; COMPLETED tham chiếu đúng final; đóng lớp xác minh riêng |
| Tài chính | Billing root giữ nghĩa vụ học phí; thu nhiều đợt; cùng intent retry không tạo phiếu thứ hai; điều chỉnh/VOID/REFUND lưu journal |
| Nhân sự | Kiểm trách nhiệm ở mọi phân công; ngừng khi đã bàn giao, từ chối an toàn khi không xác minh được |

Các quy trình là lựa chọn triển khai của đồ án. Nợ học phí không chặn COMPLETED/đóng học thuật; FAIL vẫn có thể đã học xong. Không mô tả các lựa chọn này là quy định bắt buộc của mọi trung tâm.

## 1.5. Tiêu chí đánh giá

Đánh giá theo tính đúng của hồ sơ/quyền/trạng thái, bảo toàn lịch sử, số tiền và refs kết quả, xử lý stale/race/outage, khả năng tái tạo dữ liệu, tính nhất quán tài liệu. Kết quả kiểm tra cụ thể ở [chương 6](ch06-kiem-thu-danh-gia.md). Chưa có nghiên cứu người dùng, số liệu tiết kiệm thời gian, tải đồng thời sản xuất hoặc vận hành Internet/TLS để kết luận các mục đó.
