# Hiện trạng Language Center — 11/11/2026

Đề tài: **Xây dựng hệ thống quản lý trung tâm ngoại ngữ**, tập trung trung tâm tiếng Anh. Đây là điểm đọc hiện hành; tài liệu ngày07–10/11 được giữ như bằng chứng lịch sử.

## Chức năng và kiến trúc

Bốn role SUPER_ADMIN/ADMIN/STAFF/TEACHER; bốn service nghiệp vụ và Gateway, MySQL dùng chung schema với phân chia bảng sở hữu.173 API nghiệp vụ chính (Auth19,Academic114,Teaching28,Finance12),3alias,2endpoint chẩn đoán. Kiểm kê controller11/11 không phát sinh API mới do cập nhật UI.

Đã có quản lý học viên/nhân sự/chi nhánh/chương trình/khóa/lớp/phòng, đánh giá đầu vào/ngoại lệ, ghi danh/bảo lưu/chuyển lớp, buổi học/điểm danh/dạy thay/học bù, điểm/quy tắc/chốt kết quả/đóng lớp, tài liệu, thu phí nhiều đợt/điều chỉnh/đảo/hoàn và báo cáo. Không có tài khoản học viên, thi online, QR hoặc tự xác nhận ngân hàng trong phạm vi hiện tại.

React19/TypeScript, Ant Design6, styled-components, FullCalendar6 Time-grid và múi giờ Asia/Ho_Chi_Minh. Lịch tuần/ngày/danh sách dùng buổi thực tế, lấy đủ trang, lọc giáo viên/phòng/lớp/chi nhánh/trạng thái; trạng thái/ngày lưu trên URL. Không bật kéo thả. Sidebar cố định, menu thao tác gọn; lịch sử có bảng trước/sau và cảnh báo khi không đọc được định dạng cũ.13file mock/trang cũ đã dọn, có archive local.

## 23 mục điều hướng hiện hành

| Route | Màn hình | Role |
|---|---|---|
| /dashboard | Tổng quan | SUPER_ADMIN, ADMIN, STAFF, TEACHER |
| /branches | Chi nhánh | SUPER_ADMIN |
| /administrators | Quản trị viên | SUPER_ADMIN |
| /accounts | Tài khoản | SUPER_ADMIN, ADMIN |
| /teachers | Giáo viên | SUPER_ADMIN, ADMIN, STAFF |
| /staff | Nhân viên | SUPER_ADMIN, ADMIN |
| /leads | Khách hàng tiềm năng | SUPER_ADMIN, ADMIN, STAFF |
| /students | Học viên | SUPER_ADMIN, ADMIN, STAFF |
| /programs | Chương trình & trình độ | SUPER_ADMIN |
| /courses | Khóa học | SUPER_ADMIN, ADMIN, STAFF |
| /classes | Lớp học | SUPER_ADMIN, ADMIN, STAFF |
| /rooms | Phòng học | SUPER_ADMIN, ADMIN, STAFF |
| /schedule | Lịch học | SUPER_ADMIN, ADMIN, STAFF |
| /enrollment | Đăng ký & xếp lớp | SUPER_ADMIN, ADMIN, STAFF |
| /my-classes | Lớp của tôi | TEACHER |
| /my-schedule | Lịch dạy của tôi | TEACHER |
| /attendance | Điểm danh | SUPER_ADMIN, ADMIN, STAFF, TEACHER |
| /exams | Bài kiểm tra | SUPER_ADMIN, ADMIN, TEACHER |
| /grades | Bảng điểm & đánh giá | SUPER_ADMIN, ADMIN, TEACHER |
| /documents | Tài liệu | SUPER_ADMIN, ADMIN, STAFF, TEACHER |
| /tuition | Học phí & công nợ | SUPER_ADMIN, ADMIN, STAFF |
| /payments | Phiếu thu | SUPER_ADMIN, ADMIN, STAFF |
| /reports | Báo cáo | SUPER_ADMIN, ADMIN |

Các trang profile/change-password và route403/404 không nằm trong số mục sidebar. Teacher dạy thay chỉ được cấp quyền buổi tương ứng; backend kiểm quyền cuối cùng. Không suy quyền chỉ từ menu.

## Bằng chứng và database

[Giai đoạn5](reviews/2026-11-11-phase-5-completion.md):335tests backend,96FE,12ops,86ca tích hợp thật,12UI bổ sung trước và12sau restore,19smoke mỗi lượt. Bản sao53bảng và2file tham chiếu phục hồi khớp fingerprint/số dòng. Không coi các nhóm kiểm thử là các chức năng độc lập hoặc coverage100%.

F13 đã xử lý: áp dụng V011 vào schema nguồn sau backup/verify, cột courses.duration cho phép NULL. Dữ liệu/tài khoản nguồn trước/sau khớp hash; không reset nguồn. Máy khác phải đối chiếu đủ migration, không suy đã cập nhật chỉ vì có cột mới.

[Bộ demo qua API](../scripts/demo-phase4/README.md):4khóa/4lớp,10học viên,25buổi, thực thu7triệu/nợ18triệu; các lớp tương lai từ16/11/2026 của lượt đã lưu. Bộ nghiệm thu G17/G18 là fixture khác, workshop180phút và số liệu riêng. Không trộn hai bộ khi đối chiếu báo cáo.

## Phần còn cần hoàn thiện

Khảo sát thực tế, thông tin sinh viên/GVHD, mẫu khoa và dàn trang bản nộp do người thực hiện cung cấp. PPTX/video/diễn tập có thời lượng trên máy bảo vệ chưa được chứng nhận. TLS/Internet/load và vận hành production cần nghiệm thu riêng.

Giai đoạn 6 đã hoàn thành nghiệm thu đúng gói. Xem [báo cáo bàn giao](reviews/2026-11-11-phase-6-completion.md) và hồ sơ qualification kèm ZIP. Hồ sơ cuối lưu ngoài snapshot bất biến và ràng buộc SHA256 manifest.

