# Chương 3 — Phân tích yêu cầu

## 3.1. Tác nhân và phạm vi

| Actor | Trách nhiệm và giới hạn |
|---|---|
| SUPER_ADMIN | Chi nhánh, phân công, danh mục chung; quản trị theo các guard nghiệp vụ, không bỏ qua kiểm toán/kết quả |
| ADMIN | Một hoặc nhiều chi nhánh được giao; duyệt ngoại lệ, quản lý đào tạo, tài chính và kết quả; hồ sơ Teacher chung cần đủ mọi phân công |
| STAFF | Một chi nhánh; Lead được giao, học viên/đánh giá/ghi danh/thu tiền, điểm danh ban đầu theo điều kiện; không quản lý điểm, duyệt ngoại lệ hoặc VOID/REFUND |
| TEACHER | Lớp chính và buổi thực tế được giao theo từng tài nguyên; bài/điểm và tài liệu sở hữu; không CRM, thu tiền, chốt/mở final |

Teacher dạy thay được điểm danh buổi nhưng không tự có quyền toàn bộ bảng điểm lớp. SA/Admin không nhập điểm ban đầu thay Teacher; sửa điểm đã có theo quyền/lý do và audit. Không thêm actor STUDENT hoặc ACCOUNTANT. Quyền chi tiết tại [ma trận](../role-permissions.md); người vận hành backup không phải role RBAC thứ năm.

## 3.2. Yêu cầu chức năng và dữ liệu đầu vào

| RQ | Nội dung cốt lõi |
|---|---|
| RQ01 | Hồ sơ học viên: tên, ngày sinh, giới tính, liên hệ, trình độ, lớp/khoảng học và lịch sử; xóa có kiểm soát |
| RQ02 | Giáo viên: liên hệ/chuyên môn/bằng cấp, tài khoản/phân công, lịch sử và trạng thái công tác/bàn giao |
| RQ03 | Chương trình/thang trình độ, khóa với học phí, đầu vào/đầu ra và phút xác minh |
| RQ04 | Lớp, giáo viên/phòng/lịch, sĩ số và ghi danh, bảo lưu/học lại/chuyển lớp |
| RQ05 | Đánh giá đầu vào có căn cứ, gợi ý/preview phù hợp, ngoại lệ học thuật có quản lý quyết định |
| RQ06 | Lịch tuần và buổi thực tế; xung đột, nghỉ/bù/dạy thay, điểm danh và thời lượng |
| RQ07 | Bài thi, điểm, quy tắc đánh giá, chuyên cần, kết quả tạm/chốt/mở, hồ sơ học tập và tài liệu |
| RQ08 | Nghĩa vụ phí, thu nhiều đợt, lịch sử và nợ; điều chỉnh/VOID/REFUND có lý do và chống trùng |
| RQ09 | Tìm kiếm, lọc/phân trang, số hồ sơ/lượt học và thống kê thu ròng theo phạm vi/kỳ |
| RQ10 | Quyền, phiên, scope/đối tượng, consistency/stale/race, lỗi và trải nghiệm |
| RQ11 | Use Case, ERD, Class và thiết kế động phù hợp implementation |
| RQ12 | Quy trình phát triển, kiểm tra, hướng dẫn chạy/sao lưu/phục hồi/bàn giao |

Các trường không biết có thể null theo DTO; không điền giả ngày sinh/trình độ. currentLevel text là dữ liệu cũ, không dùng tự cấp mức/chứng chỉ. Thông tin phí theo khóa được chụp vào billing; lịch sử thu không thay bằng một cờ đã đóng/chưa đóng.

## 3.3. Use Case và truy vết

Hệ thống có18nhóm Use Case trong [đặc tả](../use-cases.md), minh họa bởi5 hình Use Case trong [danh mục hình](../diagrams/README.md). UC11 gom buổi/điểm danh, UC12 bài/điểm/quy tắc, UC13 final/completion/closure, UC14 tài chính. Các nhóm không cấp mọi quyền cho mọi actor được nối vào hình; đọc từng thao tác và ngoại lệ.

[Ma trận RQ–UC–màn hình–API–dữ liệu–test](../traceability.md) là nơi truy vết hiện hành. Yêu cầu G01 lưu snapshot các thiếu sót ban đầu; báo cáo không dùng snapshot đó thay hiện trạng đã bổ sung G02–G18.

## 3.4. Các ràng buộc quan trọng

- Phân lớp so tập level được chấp nhận trong đúng chương trình; thiếu/sai đánh giá trả lý do. Ngoại lệ không bỏ qua sĩ số, lịch, chi nhánh hoặc trạng thái đối tượng.
- Một đánh giá CONFIRMED/student/program; thay thế giữ lịch sử. REQUESTED/APPROVED có thể INVALIDATED khi căn cứ thay đổi; không TTL cố định.
- Buổi học cần giờ/phút có nguồn xác minh; không tự đổi duration legacy thành giờ. Điểm0 khác chưa có điểm; thiếu bài bắt buộc có thể INSUFFICIENT.
- Chốt từng enrollment, mở lại toàn lớp vì nguồn dùng chung. COMPLETED không đồng nghĩa PASS; đóng lớp không bắt thanh toán hết nợ.
- Thông tin giao dịch gốc giữ; sửa sai bằng journal. Báo cáo thu trong kỳ khác doanh thu dồn tích; outstanding là snapshot hiện tại, không phải công nợ cuối kỳ tái dựng.

## 3.5. Yêu cầu phi chức năng và giới hạn đánh giá

Mục tiêu là dữ liệu nhất quán, quyền/phạm vi đúng, lỗi có thể xử lý và giữ lịch sử; các ca nghiệm thu cụ thể nằm ở chương 6. Chưa có ngưỡng SLA được đo/duyệt, không ghi API dưới500ms như kết quả đã chứng minh. Chưa có load test, pentest toàn diện, khảo sát người dùng, RPO/RTO hoặc hạ tầng production được nghiệm thu. Các nội dung đó là việc cần làm khi triển khai thực tế.
