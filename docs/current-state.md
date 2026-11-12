# Hiện trạng Language Center — 11/11/2026

Đề tài: **Xây dựng hệ thống quản lý trung tâm ngoại ngữ**, tập trung trung tâm tiếng Anh. Đây là điểm đọc hiện hành; tài liệu ngày07–10/11 được giữ như bằng chứng lịch sử.

## Chức năng và kiến trúc

Bốn role vận hành web SUPER_ADMIN/ADMIN/STAFF/TEACHER; STUDENT dành cho app học viên (G3). Bốn service nghiệp vụ và Gateway, MySQL dùng chung schema với phân chia bảng sở hữu.184 API nghiệp vụ chính (Auth24,Academic120,Teaching28,Finance12),3alias,2endpoint chẩn đoán. Student G3/G4 bổ sung11API; bản release G6 trước đó giữ nguyên ở173API.

Đã có quản lý học viên/nhân sự/chi nhánh/chương trình/khóa/lớp/phòng, đánh giá đầu vào/ngoại lệ, ghi danh/bảo lưu/chuyển lớp, buổi học/điểm danh/dạy thay/học bù, điểm/quy tắc/chốt kết quả/đóng lớp, tài liệu, thu phí nhiều đợt/điều chỉnh/đảo/hoàn và báo cáo. Đã có tài khoản học viên và hồ sơ cá nhân ở Student App G3; đã có lớp/lịch/chi tiết buổi trên app ở G4; điểm/học phí mobile ở các giai đoạn tiếp theo. Chưa có thi online, QR hoặc tự xác nhận ngân hàng.

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






## Student App G5 — 12/11/2026

[Điểm công bố, kết quả chính thức và chuyên cần cá nhân](student-app/phase-5-completion.md). Hiện192API (Auth24/Academic122/Teaching34/Finance12); V015 chỉ áp dụng trên schema kiểm thử riêng, chưa nâng database nguồn.


## Student App G6 — 12/11/2026

[Tài liệu và hỗ trợ](student-app/phase-6-completion.md). Tổng207API (Auth24/Academic134/Teaching37/Finance12); V016 chưa áp dụng database nguồn. 407BE/43app/96web và56API/12UI G6 đạt.


## Student App G7 — 12/11/2026

[Học phí/công nợ/phiếu thu cá nhân](student-app/phase-7-completion.md). Tổng213API (Auth24/Academic135/Teaching37/Finance17); không migration mới sau V016. 428BE/48app/96web và52API/11UI G7 đạt. Database nguồn không đổi.


## Student App G8

Đã hiện thực orders/IPN/querydr và đối soát, UI app/web, V017; [phạm vi và nghiệm thu](student-app/phase-8-completion.md). Không tuyên bố đã tích hợp sandbox thật.


## Student App G9

Đã có inbox, tùy chọn, badge/home và adapter Expo, 8API mới, migration V018. [Phạm vi và evidence](student-app/phase-9-completion.md). Push thật/VNPAY thật/thiết bị vẫn là gate chưa nghiệm thu.
