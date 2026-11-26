# G2 — Schema và chuyển đổi dữ liệu

## Đã thực hiện tại MySQL local

- V026 thêm `audience_groups`, `course_audiences` và `courses.program_id` nullable.
- Tái sử dụng `programs`, không tạo bảng chương trình thứ hai. Thêm COMMUNICATION nếu thiếu, giữ tên/trạng thái/ID của các chương trình hiện có.
- Seed năm nhóm đối tượng với mã cố định, không ghi đè nhãn/trạng thái nếu đã tồn tại.
- Khóa ngoại programme và liên kết đối tượng dùng RESTRICT; không cascade xóa danh mục hoặc dữ liệu lịch sử.
- Thử migration trên database bản sao, chạy lặp, kiểm tra FK từ chối ID chương trình không tồn tại; sau đó áp dụng local.
- Đã gắn chương trình cho **16/16 khóa**: nguồn type cũ, chính sách hiện hành và metadata website không mâu thuẫn.
- **16/16 khóa chưa gắn đối tượng**, cần người quản lý xác nhận; không suy đoán từ tên.
- Không sửa type cũ, chính sách/phiên bản đầu vào hoặc số lượng tài khoản, học viên, lớp, ghi danh, học phí, thanh toán. Công cụ so sánh trạng thái bảo vệ trước/sau.

Backup đầy đủ nằm trong thư mục riêng `storage/course-classification/` đã được Git bỏ qua. Không đưa SQL backup vào tài liệu công khai. Báo cáo từng khóa: [g2-report.json](g2-report.json); backup cụ thể được ghi trong báo cáo.

## Ánh xạ

1. Đọc chương trình hiện hành từ policy có revision lớn nhất, chỉ tin policy đã xác nhận.
2. Đối chiếu loại cũ theo danh sách alias chính xác; không dùng chứa từ khóa hoặc tên khóa.
3. Đối chiếu metadata chương trình website nếu có và liên kết programme đã tồn tại.
4. Nếu có mâu thuẫn hoặc mã ngoài danh mục: để cần rà soát, không ghi đè liên kết.
5. Giữ nguyên liên kết hiện có. Không gắn đối tượng tự động trong đợt này.

## Công cụ

```powershell
node --test scripts/identity/course-classification-rules.test.cjs
node scripts/identity/course-classification.cjs
```

Lệnh thứ hai tạo backup riêng và chỉ thử trên bản sao, không áp dụng nguồn. Thêm `--apply` mới áp dụng schema và những ánh xạ chắc chắn lên **local language_center_db**. Công cụ chỉ chấp nhận localhost/127.0.0.1 và schema này, không dùng cho production. Mỗi lượt có backup/report riêng; database rehearsal được xóa khi kết thúc, không xóa database nguồn.

Migration SQL V026 chỉ tạo schema/danh mục; phần backfill phải đi qua công cụ đối chiếu. MySQL DDL không rollback như một transaction DML: cần backup trước, xử lý lỗi bằng bản sao và quy trình khôi phục, không chạy DROP để rollback tùy tiện. Chưa xóa type hoặc ép program_id NOT NULL vì còn ứng dụng cũ.

## Chưa làm trong G1–G2

- G3: API và validation chương trình/đối tượng, ràng buộc đổi chương trình, đối chiếu khi thay policy.
- G4: dropdown và chọn nhiều đối tượng trên web quản lý, duyệt 16 khóa còn thiếu đối tượng.
- G5–G7: đồng bộ website/CMS, mobile, chatbot và thống kê DISTINCT.
- G8: nghiệm thu xuyên suốt và chỉ bỏ type sau khi các bên không còn phụ thuộc.

Trong giai đoạn chuyển tiếp, chưa dùng program_id mới để thay thế các policy snapshot hiện có. Khóa tạo mới bằng API cũ có thể chưa có program_id/đối tượng; sẽ được chặn/chuẩn hóa sau khi triển khai hợp đồng G3. Nếu tiếp tục sửa dữ liệu trước G3, chạy lại báo cáo để phát hiện lệch; không tự sửa lịch sử cho khớp.

## Kiểm chứng

4 test ánh xạ và 41 test hồi quy backend trong năm suite đạt: WebsiteCatalogTest, WebsiteIntakeWorkflowTest, AdmissionPolicyWorkflowTest, CourseWriteRulesTest, AcademicServiceApplicationTests. Migration được chạy trên MySQL bản sao hai lượt trước khi áp dụng nguồn; liên kết chương trình không tồn tại bị FK từ chối. Báo cáo: g12-validation.json.
