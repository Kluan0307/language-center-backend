# Phụ lục B — Đề cương18 slide và20 câu hỏi

**Mốc hiện hành11/11:** [23route, Time-grid và chức năng](../../current-state.md), [bằng chứng335BE/96FE/86tích hợp và phục hồi](../../reviews/2026-11-11-phase-5-completion.md). Chọn đúng bộ dữ liệu và ngày khi demo; gói mới cần đọc qualification đi kèm. Khảo sát/mẫu khoa chưa được tự điền.

Đây là **nội dung để dựng slide**, chưa tạo file PPTX hoặc video bảo vệ. Điều chỉnh số slide/thời lượng theo quy định khoa. Dùng hình nguồn/SVG và ảnh thật ở chương4/6, không chiếu secret hay tài khoản nguồn.

## 1. Đề cương18 slide

1. Tên đề tài, sinh viên/GVHD/trường: [CẦN BỔ SUNG].
2. Bài toán từ đề cương; phân biệt giả định với số liệu khảo sát chưa có.
3. Mục tiêu và phạm vi: quản lý trung tâm tiếng Anh,4 role; không LMS/QR/portal đã triển khai.
4. Kiến trúc FE → Gateway →4 service → MySQL chung; Gateway routing, service xác thực.
5. Dữ liệu targetV013:53 bảng/625 cột,52 entity; metadata và từ điển làm nguồn.
6. RBAC/phân công: Admin/Teacher nhiều chi nhánh, Staff một; Teacher lớp chính khác dạy thay.
7. Luồng đầu vào: CONFIRMED assessment + accepted level set; ngoại lệ không bỏ qua blocker, không TTL7ngày.
8. Ghi danh/billing: AFTER_COMMIT đồng bộ + REQUIRES_NEW, FAILED/PENDING và retry cùng billing root.
9. Thu học phí: key/body cố định cho cùng intent, phiếu cũ200, khác nội dung409; journal điều chỉnh.
10. Giảng dạy: buổi/phút xác minh, roster theo khoảng học, điểm0 khác thiếu điểm.
11. Kết quả: preview/fingerprint, confirm theo enrollment, barrier chung, reopen toàn lớp/refs mới.
12. Nhân sự: Account lock phối hợp Auth/Academic, kiểm trách nhiệm mọi phân công và fail-closed503.
13. Kiểm tra:335 BE,82 Node FE;86acceptance tách20 API+8CRUD+38 API+12Edge+8SQL; không coverage100%.
14. Bằng chứng tiền/học vụ: A1M+2M, B nợ3M; A85PASS/B40FAIL và cả haiCOMPLETED, closureVERIFIED.
15. Demo: dữ liệu đã nghiệm thu/ảnh thật; nêu rõ thao tác nào là đọc và phần hiệu chỉnh tùy chọn.
16. Giới hạn: chưa khảo sát thực, chưa SLA/load/Internet/TLS; shared schema, durable outbox chưa có.
17. Công việc tiếp: G20 đóng gói/restore đúng release, diễn tập máy bảo vệ, hoàn thiện mẫu khoa.
18. Kết luận trong phạm vi bằng chứng, cảm ơn và hỏi đáp.

## 2. Câu hỏi và hướng trả lời theo code

### Câu1: Vì sao chia4 service?

Để tách trách nhiệm và thực hành ranh giới HTTP/transaction. Đổi lại tăng cấu hình/độ trễ/lỗi phụ thuộc; chưa chứng minh tốt hơn monolith ở quy mô này.

### Câu2: Có phải database-per-service?

Không. Dùng chung MySQL/schema, có khóa account và barrier SQL dùng chung; muốn tách database phải thiết kế lại giao thức phối hợp.

### Câu3: Gateway kiểm JWT không?

Gateway routing/CORS/correlation. Mỗi service kiểm JWT rồi Auth live session, role, assignment và object scope; không tin identity header do client tự gửi.

### Câu4: Thu tiền bị mất phản hồi xử lý thế nào?

Giữ requestKey/body, tra cứu hoặc retry cùng intent. Finance khóa tuition, tìm payments theo branch/key và kiểm toàn bộ nội dung trước trả cùng phiếu. G18-13 cắt kết nối sau commit thực và retry cùngID.

### Câu5: Có bảng receipts hay tuitions?

Không. Phiếu thu là payments, nghĩa vụ phí là tuition; đảo/hoàn là payment_corrections, điều chỉnh nghĩa vụ là tuition_adjustments.

### Câu6: Hai người tranh ghế cuối?

Academic khóa branch/student/class/course và kiểm lại số STUDYING trước tạo; G18-09 nhận200/409, SQL chỉ một enrollment. Không chỉ kiểm current_size ở frontend.

### Câu7: Khóa lạc quan và bi quan khác nhau thế nào trong dự án?

@Version/version phát hiện editor cũ; khóa DB gom check/ghi tài nguyên ngắn hạn. Policy/final/closure có revision/giao thức riêng, không phải mọi field version đều @Version.

### Câu8: Ngoại lệ có hạn7ngày không?

Không có expiresAt/TTL đó. Validity theo context/fingerprint của assessment/policy/class/source; approval không giữ chỗ và không bỏ qua lịch/sĩ số/chi nhánh.

### Câu9: JWT bị thu hồi theo iat so password_changed_at?

Không dùng mô tả đó. Auth dùng sessionVersion và trạng thái account/mustChangePassword; service kiểm phiên hiện tại. logout-all/đổi mật khẩu vô hiệu phiên cũ theo implementation.

### Câu10: Một nút chốt cả lớp?

Confirm theo classId/enrollmentId với requestId/fingerprint/reason, lưu một FinalResult. Vì nguồn chung, reopen áp dụng toàn lớp với expectedRevision và audit.

### Câu11: Fingerprint có chống sửa DB tuyệt đối?

Không. Giúp phát hiện preview cũ ở đường ứng dụng; snapshot/barrier bảo vệ luồng đã triển khai. Người có quyền SQL ngoài ứng dụng cần kiểm soát vận hành riêng.

### Câu12: Sau reopen có đóng lớp ngay được không?

Cần mở lại lớp nếu đóng, sửa nguồn, preview/chốt lại từng enrollment, cập nhật refs COMPLETED rồi đóng. Dùng refs cũ bị409.

### Câu13: FAIL có hoàn thành? Còn nợ có đóng lớp?

Hoàn thành học vụ khác đạt học lực; cả PASS/FAIL có thể COMPLETED nếu có final hợp lệ. Công nợ theo Finance riêng, không là điều kiện chặn đóng học thuật hiện tại.

### Câu14: Finance dừng lúc ghi danh?

Enrollment đã commit giữ nguyên. AFTER_COMMIT gọi delivery Tx riêng ghiFAILED/PENDING; sau khôi phục retry theo billing root tạo một bill. Đây không phải distributed transaction hoặc durable outbox.

### Câu15: Teacher ngừng công tác khi Academic lỗi?

Auth không xác minh được trách nhiệm trả503/giữ trạng thái. Còn CLASS chưaFINISHED hoặc SESSION SCHEDULED thì409, cần bàn giao; Account lock phối hợp phân công/thu hồi.

### Câu16: Teacher dạy thay có được sửa điểm toàn lớp?

Không suy vậy. Dạy thay có quyền buổi/điểm danh thực tế theo guard; điểm và nguồn toàn lớp còn theo Teacher chính và role của endpoint.

### Câu17: Bảo vệ dữ liệu khi xóa?

Student/Class có tham chiếu/lịch sử bị chặn theo service/constraint. Course dùng CLOSED, Teacher dùng INACTIVE; không bịa DELETE endpoint cho mọi đối tượng.

### Câu18: Đã kiểm thử tới đâu?

335 BE và82 Node FE trong regression cùng ngày;86 ca nghiệm thu thật bao gồm12Edge/8SQL. Không cộng chung như một độ bao phủ; chưa load/pentest/production TLS. Xem chương 6 và manifest.

### Câu19: Doanh thu báo cáo có phải lợi nhuận hoặc nợ cuối kỳ?

Không. collected là thu ròng trong kỳ theo journal; không có chi phí để tính lợi nhuận. outstanding là snapshot hiện tại, không tái dựng nợ cuối kỳ. Chọn đúng ngày/chi nhánh.

### Câu20: Còn gì trước khi nộp/triển khai?

Khảo sát/mẫu khoa/thông tin cá nhân, dàn trang/trích dẫn, diễn tập trên máy bảo vệ; G20 đóng gói và phục hồi đúng release. Internet/TLS/tải/backup theo mục tiêu thực phải nghiệm thu riêng.


Nguồn đối chiếu: [thiết kế động](../design/dynamic-workflows.md), [ma trận quyền](../role-permissions.md), [API](../../api-list.md), [kiểm thử](ch06-kiem-thu-danh-gia.md). Không trình bày lựa chọn của đồ án như một chuẩn áp dụng cho mọi trung tâm.
