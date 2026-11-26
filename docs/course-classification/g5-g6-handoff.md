# G5–G6 — Website/CMS và mobile học viên

Ngày 16/11/2026. Phát triển local, không deploy, không tự xuất bản hoặc gán nhóm cho dữ liệu hiện có.

## G5 — Website

- API GET `/api/v1/academic/public/audiences` trả mã và tên của nhóm chuẩn đang hoạt động; không trả hồ sơ học viên hay ID nội bộ.
- GET public catalog COURSE thêm `audience=PRESCHOOL|PRIMARY|SECONDARY|HIGH_SCHOOL|ADULT`; kết hợp được với chương trình, chi nhánh, từ khóa, học phí và phân trang.
- Điều kiện nhóm nằm trong EXISTS trước phân trang, không làm nhân đôi khóa có nhiều nhóm.
- Không tìm thấy theo nhóm nếu khóa chưa có nhóm đó; không suy ra từ tên/type cũ. Nhóm ngừng dùng không được liệt kê hoặc dùng để tìm khóa mới.
- Trang chủ có các lối chọn Mẫu giáo/Tiểu học/THCS/THPT/Người lớn từ catalog. IELTS/TOEIC chỉ xuất hiện dưới dạng lối theo chương trình khi chương trình đó đã công khai; dùng slug thật, không tự tạo URL giả.
- Danh sách khóa có bộ lọc đối tượng, giữ lựa chọn khi phân trang, hiển thị chương trình và nhóm phù hợp từ khóa gốc. Trang chi tiết cũng hiển thị cùng metadata.
- Khi chưa có khóa phù hợp, chuyển sang tư vấn giữ nhóm khách đã chọn dưới dạng **ghi chú có thể chỉnh sửa**. Đây là nhu cầu tư vấn, không phải tuổi/đối tượng hoặc trình độ đã xác nhận của học viên. Không dùng group để ghi vào desiredLevel.
- API public vẫn giữ nguyên điều kiện xuất bản, khóa/nhánh hoạt động, học phí được duyệt và lớp còn tuyển sinh.

## CMS

- Source picker bổ sung programId và classification từ bản ghi gốc cho khóa/lớp.
- Khi chọn nguồn khóa, chương trình được tự điền và chỉ đọc; không nhập một chương trình thứ hai độc lập với khóa.
- Hiển thị chương trình/nhóm nguồn và cảnh báo khi thiếu nhóm. Muốn sửa nhóm phải vào Quản lý khóa học.
- Trường mô tả đối tượng vẫn là nội dung biên tập bổ sung, không được dùng làm dữ liệu lọc hoặc phân loại.
- Việc biên tập/xuất bản vẫn theo quyền L8. Không mở API quản trị cho khách hoặc Student.

## G6 — Mobile

- API student/me/enrollments và enrollment detail thêm trường classification; giữ nguyên toàn bộ thuộc tính cũ.
- Phân loại chỉ đọc sau khi đã kiểm tra quyền sở hữu đăng ký của học viên.
- Trang chi tiết lớp trên mobile thêm Chương trình và Đối tượng phù hợp của khóa khi có dữ liệu; không thêm bộ lọc tuyển sinh vào app học viên.
- Với lớp có admissionPolicyId, tên/mã chương trình ưu tiên snapshot gắn với lớp; không dùng nhãn mới để ghi đè thông tin lịch sử. Nhóm phù hợp là metadata hiện tại của khóa, không phải đánh giá hay phân loại cá nhân học viên.
- Backend cũ chưa có classification: app mới ẩn phần bổ sung, không đoán nhãn. API mới thêm trường: APK cũ tiếp tục đọc các thuộc tính cũ như trước.
- Không thay luồng điểm, điểm danh, lịch học, học phí, phiếu thu, tài khoản hoặc điều kiện nhập học.
- Đã cập nhật mã nguồn mobile và kiểm tra TypeScript; **chưa build APK mới hoặc kiểm thử thiết bị thật** trong đợt này. APK đang cài cần được cập nhật để thấy giao diện bổ sung, dù vẫn dùng được API mới.

## Dữ liệu và chạy local

Không có migration mới. Cần V026/V027 từ G2–G4, rồi khởi động lại academic-service, website và web quản lý để nạp API mới. Reload app chạy bằng Expo để xem thay đổi mã nguồn.

16 khóa cũ vẫn cần quản lý xác nhận đối tượng. Vì vậy một số bộ lọc nhóm có thể chưa có kết quả dù danh sách tất cả khóa vẫn có dữ liệu; không phải hệ thống tự đoán nhầm hoặc bỏ khóa.

G7 chatbot/thống kê và G8 nghiệm thu toàn luồng còn lại. Không tuyên bố đã cập nhật khả năng chatbot trong G5–G6.

## Kiểm chứng

- 78 test backend đã chọn đạt: 75 test phân loại/public catalog/quyền/student/query budget và 3 test metadata tương thích/snapshot.
- Website TypeScript/production build đạt; CMS production build đạt; TypeScript mobile đạt.
- Chrome dùng HTTP fixture riêng để kiểm tra homepage, lọc nhóm không trùng khóa, các lựa chọn hiện hành, luồng tư vấn và kích thước 320/390px. Không chèn dữ liệu nhóm/lead thử vào MySQL local.
- Tổng cộng 38 kiểm tra Chrome đạt, bao gồm giữ nhóm quan tâm trong ghi chú tư vấn. Báo cáo: g56-validation.json.
- Kiểm thử APK thật và thao tác CMS trực quan đầy đủ chưa thực hiện. Kết quả automation không thay thế nghiệm thu dữ liệu thật ở G8.
