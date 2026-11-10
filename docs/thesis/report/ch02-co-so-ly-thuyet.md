# Chương 2 — Cơ sở kỹ thuật và công nghệ

## 2.1. Cách phân chia ứng dụng

Backend chia Auth, Academic, Teaching và Finance thành các tiến trình HTTP riêng; Gateway là điểm định tuyến cho FE. Mục đích trong đồ án là tách trách nhiệm và học cách xử lý ranh giới giao dịch. Sự tách tiến trình làm tăng cấu hình, độ trễ và đường lỗi; chưa có bằng chứng rằng cách chia này nhanh hơn hoặc phù hợp hơn mọi monolith.

Hiện các service **cùng schema MySQL**. Khóa account khi phân công giáo viên và class_result_barriers khi chốt/sửa nguồn là phụ thuộc SQL dùng chung. Vì vậy không khẳng định độc lập hoàn toàn, database-per-service hoặc có thể tách database mà không đổi giao thức. Chi tiết tại [thiết kế động](../design/dynamic-workflows.md).

## 2.2. Stack triển khai

| Phần | Cấu hình dự án / vai trò |
|---|---|
| Backend | Java 21, Spring Boot 4.1.1, Maven; controller/DTO/service/repository, JPA và JDBC ở một số khóa điều phối |
| Lưu trữ | MySQL8; DECIMAL/BigDecimal cho tiền; SQL/file storage là hai phần cần sao lưu |
| Frontend | React 19, TypeScript 6, Ant Design 6, styled-components 6; phiên bản khóa chính xác xem package-lock |
| HTTP/UI | Axios, React Router; CORS và correlation tại Gateway; thông báo lỗi và phạm vi ở service/FE |
| Kiểm tra | JUnit/Spring/H2/mock/stub ở BE; node --test cho FE; Edge CDP cho UI thật; PlantUML/Smetana cho hình |

Nguồn cấu hình: [POM](../../../pom.xml), [package.json](../../../../../language-center-web-frontend/package.json). Phiên bản là cấu hình repository được đọc, không phải tuyên bố phiên bản mới nhất. Không suy rằng Virtual Threads được bật chỉ vì dùng Java21.

## 2.3. Transaction, khóa và version

Transaction cục bộ gom các thay đổi dữ liệu của một thao tác. SELECT FOR UPDATE khóa hàng để giao dịch khác cùng tuân theo khóa phải chờ; cần thiết khi kiểm tra số chỗ hoặc số còn nợ rồi ghi kết quả. Tài liệu chính thức mô tả locking reads và phạm vi transaction của InnoDB [R3](tai-lieu-tham-khao.md#r3).

@Version phát hiện xung đột khi ghi entity; các DTO có version còn so phiên bản client gửi. Không phải mọi entity hay endpoint đều có @Version: policy/closure/final dùng revision và khóa riêng. Lead/Teacher/assessment là ví dụ stale editor; không giữ transaction DB suốt thời gian người dùng mở form.

AFTER_COMMIT gắn callback vào kết quả transaction theo cơ chế transaction-bound event của Spring [R2](tai-lieu-tham-khao.md#r2). Trong code, callback gọi đồng bộ TuitionDeliveryService.REQUIRES_NEW; Finance có transaction khác. Không có commit nguyên tử xuyên HTTP hoặc outbox bền vững. Nếu Finance lỗi, giữ enrollment và cho retry bằng billing root.

## 2.4. JWT và phân quyền

Service kiểm chữ ký/expiry rồi xác minh phiên hiện hành từ Auth; sessionVersion có thể thu hồi token. Đây không phải mô hình JWT hoàn toàn không trạng thái. Role xác định loại hành động; assignment và quyền sở hữu lớp/buổi xác định đối tượng được thao tác. UI ẩn nút hỗ trợ trải nghiệm, không thay guard server. Gateway hiện không phát identity header từ JWT.

## 2.5. Intent, fingerprint và snapshot

requestKey xác định intent thu tiền; server kiểm đúng branch/tuition và body trước replay. Mất phản hồi sau commit cần giữ key/body để tra cứu hoặc retry. Một key mới có nghĩa khác với lặp lại cùng intent; không gọi mọi POST là idempotent.

Fingerprint chốt là mã SHA-256 của các giá trị/revision nguồn. Server tính lại khi confirm để phát hiện preview cũ; snapshot lưu căn cứ quyết định tại thời điểm chốt. Snapshot không phải chữ ký số và không chống quản trị viên database sửa trực tiếp ngoài ứng dụng. Barrier chung điều phối các đường ghi của ứng dụng; mở lại có audit rồi chốt phiên bản mới giữ snapshot cũ.

## 2.6. Giao diện và mô hình hóa

React tổ chức giao diện theo component/state [R4](tai-lieu-tham-khao.md#r4); TypeScript hỗ trợ kiểm tra kiểu trước khi chạy nhưng không thay validation dữ liệu mạng [R5](tai-lieu-tham-khao.md#r5). Ant Design/styled-components bám cấu trúc FE có sẵn. Nhiều trang dùng bảng/form/filter; mobile được kiểm ở ca cụ thể, chưa có khảo sát usability để kết luận dễ dùng với mọi người.

PlantUML biểu diễn sequence bằng participant/message [R6](tai-lieu-tham-khao.md#r6). Hình render thành công chỉ xác nhận cú pháp; phải đối chiếu HTTP method/body, thứ tự khóa, transaction và nhánh lỗi với controller/service và kiểm thử thực tế.
