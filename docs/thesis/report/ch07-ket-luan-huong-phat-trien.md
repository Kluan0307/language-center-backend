# Chương 7 — Kết luận và hướng phát triển

## 7.1. Kết quả đạt được

Đồ án đã xây dựng ứng dụng web cho bốn role nội bộ, quản lý học viên/giáo viên/khóa/lớp/lịch, đánh giá đầu vào và phân lớp có ngoại lệ, quá trình học tập/kết quả, học phí/thu nhiều đợt và thống kê cơ bản. Tài liệu Use Case, ERD, Class và thiết kế động được đối chiếu với controller/DTO/service và schema target. [Ma trận truy vết](../traceability.md) chỉ rõ phần source/test cho RQ01–RQ12.

Các điểm kỹ thuật đã triển khai gồm billing root và retry học phí, requestKey bảo vệ cùng intent, khóa account khi phân công/ngừng Teacher, và barrier/snapshot/revision cho final theo enrollment. Nợ học phí, hoàn thành học vụ và đạt học lực được tách để tránh suy diễn trạng thái.

Bằng chứng hiện có:335 test backend,82 test Node frontend,86 kiểm tra API/MySQL/Edge/SQL cùng ops/smoke riêng. Trong bộ ca bắt buộc không phát hiện sai quyền/sai tiền/kết quả hoặc nhân đôi dữ liệu chưa được xử lý. Không suy các con số này thành coverage100%, an toàn tuyệt đối, hiệu năng sản xuất hoặc xác nhận toàn bộ20giai đoạn.

## 7.2. Hạn chế

Chưa có khảo sát thực địa/đánh giá người dùng để xác minh giả định G01-v1 và hiệu quả giảm công việc. Bản thảo chưa có thông tin cá nhân/mẫu khoa và chưa dàn trang thành bản nộp.

Các service chung schema và có khóa điều phối SQL, phụ thuộc Auth live-session/HTTP, nên chưa độc lập triển khai theo database-per-service. Delivery sau commit chưa có durable outbox/broker; cần theo dõi PENDING/FAILED và retry có chủ đích. Một số dữ liệu legacy thiếu phút/đánh giá cần xác minh thủ công, không được tự chuẩn hóa.

Chưa thử tải/Internet/TLS/khôi phục với mục tiêu RPO/RTO. Bộ release G20 cũ cần đóng gói/xác minh lại theo source và tài liệu mới. Chuyển khoản thủ công, chưa LMS/portal/QR/thi online hoặc chứng chỉ; các chức năng đó không được tính là đã làm.

## 7.3. Hướng hoàn thiện

Ưu tiên G20: build/manifest/restore schema+file và smoke đúng artifact cuối trên môi trường riêng; sau đó diễn tập trên máy bảo vệ và hoàn thiện bản nộp theo mẫu khoa. Khi triển khai thực, khảo sát đơn vị, chốt chính sách vận hành, đo tải và chuẩn bị sao lưu/giám sát/phục hồi theo yêu cầu cụ thể.

Mở rộng LMS, portal, thi online, QR hoặc cổng thanh toán chỉ sau khi xác định nhu cầu và hợp đồng dữ liệu/quyền phù hợp. Tách database cần thiết kế lại account/barrier phối hợp, không chỉ đổi DB_URL. Cải thiện giao diện dựa phản hồi đo được, thay vì khẳng định sẵn thân thiện với mọi đối tượng.

## Cập nhật sau nghiệm thu G20

G20 đã hoàn thành release local900artifact: kiểm tra86ca trên JAR/FEbuild, phục hồi dữ liệu/file và diễn tập4role trước/sau restore đạt. Các mục cần xác minh release nêu ở bản thảo trước đã có [bằng chứng G20](../../thesis-progress/G20.md); bản nộp, người trình bày/máy bảo vệ, Internet/TLS/load vẫn chưa được xác nhận. Không thay kết quả khảo sát còn thiếu bằng kết quả kiểm thử kỹ thuật.
